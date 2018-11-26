package cnt4004.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProtocolMap {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolMap.class);

    /**
     * Magic header, used for identifying if a packet uses this protocol
     */
    private static final byte[] MAGIC = new byte[]{
            'P', 'K'
    };

    /**
     * The expected amount of bytes to read and write for a DatagramSocket
     * This value is used for the SO_RCVBUF datagram option/
     */
    public static final int MAX_BUFFER = 100;

    /**
     * A map of Trusted clients. Each client is indexed by its identifier.
     * The key is the client identifier.
     */
    private static ConcurrentMap<String, TrustedClient> TRUSTED_CLIENTS;

    /**
     * Packet ID mappings. Used for initializing new packet objects given a packet ID
     */
    private static final Map<Byte, Constructor<? extends Packet>> PACKET_MAP = new HashMap<>();

    static {
        // Make sure that the byte is unique for each packet
        try {

            PACKET_MAP.put((byte) 0, KnockPacket.class.getDeclaredConstructor());

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the trusted clients that the decoder will consider when verifying an {@link AuthenticatedPacket}
     *
     * @param trustedClients A set of trusted clients
     */
    public static void setTrustedClients(Set<TrustedClient> trustedClients) {

        TRUSTED_CLIENTS = new ConcurrentHashMap<>();

        if (trustedClients != null)
            trustedClients.forEach(client -> TRUSTED_CLIENTS.put(client.getIdentifier(), client));

    }

    /**
     * Creates a new Packet object given a packet ID
     *
     * @param id The packet ID
     * @return A new packet object associated with the id, or null if there is no packet object mapped
     */
    private static Packet createFromID(byte id) {
        Constructor<? extends Packet> constructor = PACKET_MAP.get(id);
        if (constructor != null) {
            try {
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Failed to create a new Packet object instance for ID " + id, e);
            }
        }
        return null;
    }

    /**
     * Decodes a byte[] payload to a Packet object
     *
     * @param payload The byte[] array to decode
     * @return A packet object deserialize from the payload, or null if the packet is corrupt.
     * @throws IOException Decoding error
     */
    public static Packet decodePayload(byte[] payload) throws IOException {

        if (payload == null)
            return null;

        ByteArrayInputStream inBuffer = new ByteArrayInputStream(payload);
        DataInputStream in = new DataInputStream(inBuffer);

        byte[] receivedMagic = new byte[MAGIC.length];
        int read = in.read(receivedMagic, 0, MAGIC.length);

        if (read != MAGIC.length || !Arrays.equals(receivedMagic, MAGIC)) {
            LOGGER.debug("Received a bad magic packet");
            return null;
        }

        Packet packet = createFromID((byte) 0);

        if (packet == null) {
            LOGGER.debug("Failed to create packet object (unknown ID?)");
            return null;
        }

        packet.read(in);

        // If the packet is supposed to be authenticated, check it
        if (packet instanceof AuthenticatedPacket) {

            AuthenticatedPacket authenticatedPacket = (AuthenticatedPacket) packet;

            // Identify the client
            String identifier = authenticatedPacket.getClientIdentifier();

            LOGGER.debug("Received client identifier for authenticated packet: " + identifier);

            TrustedClient client = TRUSTED_CLIENTS.get(identifier);

            // No trusted client for identifier
            if (client == null) {
                LOGGER.debug("No public key found for identifier (client not trusted): " + identifier);
                return null;
            }

            // Check for replay attack possibility
            if (authenticatedPacket.getNonce() <= client.getLargestNonceReceived()) {
                LOGGER.debug("Denied a possible replayed packet (or out-of-order)." +
                        " Received nonce vs. largest nonce received: "
                        + authenticatedPacket.getNonce() + " - " + client.getLargestNonceReceived());
                return null;
            }

            // Read MAC
            byte[] mac = new byte[TrustedClient.MAC_LENGTH];
            int result = in.read(mac);

            if (result != mac.length) {
                LOGGER.debug("Buffer underflow for MAC, discarding packet " +
                        "(read " + result + ", expected " + TrustedClient.MAC_LENGTH + ")");
                return null;
            }

            // Verify MAC
            byte[] packetPayload = new byte[MAGIC.length + packet.length()];
            System.arraycopy(payload, 0, packetPayload, 0, packetPayload.length);

            if (!Arrays.equals(mac, client.createMAC(packetPayload))) {

                LOGGER.debug("Invalid MAC, discarding packet");
                return null;

            }

        }

        return packet;

    }

    /**
     * Encodes a packet object into a byte[] payload
     *
     * @param packet The packet object to encode
     * @return The byte[] payload
     * @throws IOException Exception upon writing the payload
     */
    public static byte[] encodePacket(Packet packet) throws IOException {

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outputBuffer);

        out.write(MAGIC);
        packet.write(out);

        out.close();

        byte[] packetPayload = outputBuffer.toByteArray();

        if (packet instanceof AuthenticatedPacket) {

            TrustedClient client = TRUSTED_CLIENTS.get(((AuthenticatedPacket) packet).getClientIdentifier());

            if (client == null)
                throw new IOException("No trusted client found to generate MAC for given identifier");

            byte[] mac = client.createMAC(packetPayload);

            if (mac.length != TrustedClient.MAC_LENGTH)
                throw new IOException("Expected signature length of " + TrustedClient.MAC_LENGTH + ", actual is " + mac.length);

            // Concat final payload
            byte[] payload = new byte[packetPayload.length + mac.length];
            System.arraycopy(packetPayload, 0, payload, 0, packetPayload.length);
            System.arraycopy(mac, 0, payload, packetPayload.length, mac.length);

            return payload;

        } else {

            return packetPayload;

        }

    }

}
