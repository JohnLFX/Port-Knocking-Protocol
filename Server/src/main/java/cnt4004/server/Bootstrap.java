package cnt4004.server;

import cnt4004.protocol.RSAIO;
import cnt4004.protocol.TrustedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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

        InetAddress bindAddress = InetAddress.getByName(config.getProperty("bind-address"));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        Set<TrustedClient> trustedClients = new HashSet<>();

        Path keyPairPath = Paths.get(config.getProperty("server-keypair-path", "server_keypair.txt"));
        KeyPair serverKeyPair;

        if (Files.notExists(keyPairPath)) {

            LOGGER.info("Generating server key pair for " + keyPairPath);

            serverKeyPair = RSAIO.generateKeyPair();
            RSAIO.save(keyPairPath, serverKeyPair);

        } else {

            LOGGER.info("Loading server key pair from " + keyPairPath);
            serverKeyPair = RSAIO.load(keyPairPath);

        }

        Path trustedClientsFile = Paths.get(config.getProperty("trusted-clients-path", "trusted_clients.txt"));

        if (Files.notExists(trustedClientsFile)) {

            LOGGER.info("Creating " + trustedClientsFile);
            Files.createFile(trustedClientsFile);

        }

        try (BufferedReader br = Files.newBufferedReader(trustedClientsFile)) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty())
                    continue;

                int delimiterIndex = line.indexOf(' ');

                String identifier = line.substring(0, delimiterIndex);
                String encodedPublicKey = line.substring(delimiterIndex + 1);

                PublicKey publicKey = RSAIO.decodePublicKey(keyFactory, encodedPublicKey);

                LOGGER.info("Load trusted client profile for " + identifier);

                if (!trustedClients.add(new TrustedClient(identifier, publicKey)))
                    LOGGER.warn("Not adding duplicate trusted client: " + identifier);

            }

        }

        KnockServer knockServer = new KnockServer(
                bindAddress,
                trustedClients,
                serverKeyPair.getPrivate(),
                config.getProperty("port-secret"),
                Integer.parseInt(config.getProperty("ports", "3")),
                Integer.parseInt(config.getProperty("open-timeout", "10"))
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            LOGGER.info("Shutting down knock server");
            knockServer.shutdown();

        }));

    }

}
