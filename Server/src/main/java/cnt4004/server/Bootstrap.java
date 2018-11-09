package cnt4004.server;

import cnt4004.protocol.ProtocolMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Bootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) throws Exception {

        Properties config = new Properties();
        Path configFile = Paths.get("server.properties");

        if (Files.notExists(configFile)) {

            try (InputStream in = Bootstrap.class.getResourceAsStream("/server.properties")) {
                LOGGER.info("Creating new configuration file: " + configFile);
                Files.copy(in, configFile);
            }

        }

        try (InputStream in = Files.newInputStream(configFile, StandardOpenOption.READ)) {
            LOGGER.info("Loading settings from " + configFile);
            config.load(in);
        }

        List<Integer> portKnockSequence = Arrays.stream(config.getProperty("knock-sequence").split(Pattern.quote(",")))
                .mapToInt(Integer::valueOf).boxed().collect(Collectors.toList());

        InetAddress bindAddress = InetAddress.getByName(config.getProperty("bind-address"));

        String secret = config.getProperty("shared-secret");

        LOGGER.debug("Shared secret: " + secret);

        ProtocolMap.initializeHMAC(new SecretKeySpec(secret.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));

        int openTimeout = Integer.parseInt(config.getProperty("open-timeout"));

        new KnockServer(bindAddress, portKnockSequence, openTimeout);

    }

}
