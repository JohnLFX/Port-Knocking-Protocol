package cnt4004.server;

import cnt4004.protocol.ProtocolMap;

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

public class Start {

    public static void main(String[] args) throws Exception {

        Properties config = new Properties();
        Path configFile = Paths.get("udpKnock.properties");

        if (Files.notExists(configFile)) {

            try (InputStream in = Start.class.getResourceAsStream("/settings.properties")) {
                System.out.println("Writing settings.properties to " + configFile);
                Files.copy(in, configFile);
            }

        }

        try (InputStream in = Files.newInputStream(configFile, StandardOpenOption.READ)) {
            System.out.println("Loading settings from " + configFile);
            config.load(in);
        }

        List<Integer> portKnockSequence = Arrays.stream(config.getProperty("knock-sequence").split(Pattern.quote(",")))
                .mapToInt(Integer::valueOf).boxed().collect(Collectors.toList());

        // TODO Allow duplicate ports in future update
        if (portKnockSequence.size() != portKnockSequence.stream().distinct().count()) {
            System.out.println("Invalid port knock sequence in " + configFile + ": Sequence must contain only unique values");
            return;
        }

        InetAddress bindAddress = InetAddress.getLoopbackAddress(); //TODO Configurable

        String secret = config.getProperty("shared-secret");

        System.out.println("Secret: " + secret);

        ProtocolMap.initializeHMAC(new SecretKeySpec(secret.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));

        new UDPKnockServer(bindAddress, portKnockSequence, config.getProperty("open-command"), config.getProperty("close-command"));

    }

}
