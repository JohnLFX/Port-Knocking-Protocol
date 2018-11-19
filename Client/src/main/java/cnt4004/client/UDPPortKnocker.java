package cnt4004.client;

import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.RSAIO;
import cnt4004.protocol.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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

        LOGGER.info("Client identifier: " + clientIdentifier);

        Path keyPairPath = Paths.get(config.getProperty("client-keypair-path", "client_keypair.txt"));
        KeyPair clientKeyPair;

        if (Files.notExists(keyPairPath)) {

            LOGGER.info("Generating new keypair for " + keyPairPath);

            clientKeyPair = RSAIO.generateKeyPair();
            RSAIO.save(keyPairPath, clientKeyPair);

        } else {

            LOGGER.info("Loading keypair from " + keyPairPath);
            clientKeyPair = RSAIO.load(keyPairPath);

        }

        ProtocolMap.initializeSignature(null, clientKeyPair.getPrivate());

        DatagramSocket socket = new DatagramSocket();

        List<Integer> ports = Utils.getPorts(portSecret, portCount);

        if (ports == null || ports.isEmpty()) {
            LOGGER.error("No ports generated");
            return;
        }

        Iterator<Integer> portIterator = ports.iterator();

        LOGGER.info("Sending knock sequence on ports: " + ports);
        KnockPacket knockPacket = new KnockPacket(clientIdentifier, Instant.now(), (byte) 0, (byte) (ports.size() - 1));

        byte sequenceID = 0;

        while (portIterator.hasNext()) {

            knockPacket.setSequence(sequenceID++);
            knockPacket.setTimestamp(Instant.now());
            byte[] payload = ProtocolMap.encodePacket(knockPacket);

            int destinationPort = portIterator.next();

            LOGGER.info("Sending knock packet (" + knockPacket + ") to " + serverAddress + " on port " + destinationPort);

            Formatter formatter = new Formatter();
            for (byte b : payload)
                formatter.format("%02x", b);

            LOGGER.debug("Payload hex: " + formatter.toString());

            socket.send(new DatagramPacket(payload, payload.length, serverAddress, destinationPort));

        }

        LOGGER.info("Knock sequence complete");

    }

}
