package cnt4004.client;

import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import cnt4004.protocol.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

/**
 * Port knocking client
 */
public class PortKnocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortKnocker.class);

    public static void main(String[] args) throws Exception {

        Properties config = new Properties();
        Path configFile = Paths.get("client.properties");

        if (Files.notExists(configFile)) {

            try (InputStream in = PortKnocker.class.getResourceAsStream("/client.properties")) {
                LOGGER.info("Creating new configuration file: " + configFile);
                Files.copy(in, configFile);
            }

        }

        try (InputStream in = Files.newInputStream(configFile, StandardOpenOption.READ)) {
            LOGGER.info("Loading settings from " + configFile);
            config.load(in);
        }

        InetAddress serverAddress = InetAddress.getByName(config.getProperty("server-address"));

        String portSecret = config.getProperty("port-secret");
        int portCount = Integer.parseInt(config.getProperty("ports", "3"));

        String clientIdentifier = config.getProperty("client-identifier");
        String sharedSecret = config.getProperty("client-shared-secret");
        long nonce = Long.parseLong(config.getProperty("nonce", "0"));

        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new NullPointerException("No client-shared-secret provided in " + configFile);

        LOGGER.info("Client identifier: " + clientIdentifier);

        ProtocolMap.setTrustedClients(new HashSet<>(Collections.singletonList(new TrustedClient(clientIdentifier, sharedSecret, nonce))));

        DatagramSocket socket = new DatagramSocket();

        List<Integer> ports = Utils.getPorts(portSecret, portCount);

        if (ports == null || ports.isEmpty()) {
            LOGGER.error("No ports generated");
            return;
        }

        // Socket address of the web server
        InetSocketAddress serviceAddress = new InetSocketAddress(serverAddress, Integer.parseInt(config.getProperty("service-port", "8080")));

        int attempts = 0;
        boolean serviceUp = false;

        // Do 3 attempts to send a knock sequence
        while (!serviceUp && attempts < 3) {

            Iterator<Integer> portIterator = ports.iterator();

            LOGGER.info("Sending knock sequence on ports: " + ports);
            KnockPacket knockPacket = new KnockPacket(clientIdentifier, nonce, (byte) 0, (byte) (ports.size() - 1));

            while (portIterator.hasNext()) {

                knockPacket.setTimestamp(Instant.now());
                byte[] payload = ProtocolMap.encodePacket(knockPacket);

                int destinationPort = portIterator.next();

                LOGGER.info("Sending knock packet (" + knockPacket + ") to " + serverAddress + " on port " + destinationPort);

                Formatter formatter = new Formatter();
                for (byte b : payload)
                    formatter.format("%02x", b);

                LOGGER.debug("Payload hex: " + formatter.toString());

                socket.send(new DatagramPacket(payload, payload.length, serverAddress, destinationPort));

                if (knockPacket.getSequence() < knockPacket.getMaxSequence()) {

                    knockPacket.setNonce(knockPacket.getNonce() + 1);
                    knockPacket.setSequence((byte) (knockPacket.getSequence() + 1));

                }

            }

            config.setProperty("nonce", String.valueOf((nonce + ports.size())));

            LOGGER.info("Updating nonce in configuration to " + config.getProperty("nonce"));

            // Update the nonce in the configuration
            try (OutputStream out = Files.newOutputStream(configFile)) {
                config.store(out, null);
            }

            LOGGER.info("Knock sequence complete");

            LOGGER.info("Checking if service is up...");
            serviceUp = tcpPing(serviceAddress);

            if (serviceUp) {
                LOGGER.info("Service is reachable!");
            } else {
                LOGGER.warn("Service is not reachable!");
                attempts++;
            }

        }

        socket.close();

        if (!serviceUp) {
            LOGGER.error("Failed to reach the service after " + attempts + " attempts.");
        }

    }

    /**
     * Checks to see if a TCP connection can be established on {@code address}
     *
     * @param address The remote host socket address
     * @return True if yes, false is no
     */
    private static boolean tcpPing(InetSocketAddress address) {
        try (Socket socket = new Socket()) {
            socket.connect(address);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
