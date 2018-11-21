package cnt4004.client;

import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import cnt4004.protocol.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

public class UDPPortKnocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPPortKnocker.class);

    public static void main(String[] args) throws Exception {

        Properties config = new Properties();
        Path configFile = Paths.get("client.properties");

        if (Files.notExists(configFile)) {

            try (InputStream in = UDPPortKnocker.class.getResourceAsStream("/client.properties")) {
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

        ProtocolMap.initializeHMAC(new HashSet<>(Collections.singletonList(new TrustedClient(clientIdentifier, sharedSecret, nonce))));

        DatagramSocket socket = new DatagramSocket();

        List<Integer> ports = Utils.getPorts(portSecret, portCount);

        if (ports == null || ports.isEmpty()) {
            LOGGER.error("No ports generated");
            return;
        }

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

        LOGGER.info("Knock sequence complete");

        config.setProperty("nonce", String.valueOf((nonce + ports.size())));

        LOGGER.info("Updating nonce in configuration to " + config.getProperty("nonce"));

        // Update the nonce in the configuration
        try (OutputStream out = Files.newOutputStream(configFile)) {
            config.store(out, null);
        }

    }

}
