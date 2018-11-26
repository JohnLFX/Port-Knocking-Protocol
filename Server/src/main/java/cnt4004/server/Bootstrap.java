package cnt4004.server;

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
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

/**
 * Entry point
 */
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

        // Read a set of trusted clients

        Set<TrustedClient> trustedClients = new HashSet<>();

        Path trustedClientsFile = Paths.get(config.getProperty("trusted-clients-path", "trusted_clients.txt"));

        if (Files.notExists(trustedClientsFile)) {

            LOGGER.info("Creating " + trustedClientsFile);
            Files.createFile(trustedClientsFile);

        }

        // Flat file for trusted clients, used for updating the nonce in the file
        TrustedClient.setFlatFile(trustedClientsFile);

        try (BufferedReader br = Files.newBufferedReader(trustedClientsFile)) {

            String line;

            while ((line = br.readLine()) != null) {

                // Remove extra whitespace
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty())
                    continue;

                int delimiterIndex = line.indexOf(' ');
                int keyDelimiterIndex = line.lastIndexOf(' ');

                String identifier = line.substring(0, delimiterIndex);
                String sharedSecret = line.substring(delimiterIndex + 1, keyDelimiterIndex);
                long nonce = Long.parseLong(line.substring(keyDelimiterIndex + 1));

                LOGGER.info("Load trusted client profile for " + identifier + ", max nonce = " + nonce);

                if (!trustedClients.add(new TrustedClient(identifier, sharedSecret, nonce)))
                    LOGGER.warn("Not adding duplicate trusted client: " + identifier);

            }

        }

        // Create a new Knock server instance
        KnockServer knockServer = new KnockServer(
                bindAddress,
                trustedClients,
                config.getProperty("port-secret"),
                Integer.parseInt(config.getProperty("ports", "3")),
                Integer.parseInt(config.getProperty("open-timeout", "10"))
        );

        Scanner scanner = new Scanner(System.in);

        boolean readingConsole = true;

        // Basic commands (only to cleanly stop)
        while (readingConsole && scanner.hasNextLine()) {

            String command = scanner.nextLine().toLowerCase();

            switch (command) {

                case "end":
                case "stop":
                case "quit":
                    readingConsole = false;
                    break;
                default:
                    LOGGER.info("Unknown command: \"" + command + "\"");
                    break;
            }

        }

        scanner.close();
        LOGGER.info("Shutting down knock server");

        knockServer.shutdown();

        LOGGER.info("Goodbye");
        System.exit(0);

    }

}
