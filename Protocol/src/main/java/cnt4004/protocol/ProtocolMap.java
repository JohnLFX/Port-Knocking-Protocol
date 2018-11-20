package cnt4004.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
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
     */
    public static final int MAX_BUFFER = 700; // TODO Determine buffer

    //TODO Pooling or multi-thread support for signature processing
    private static Signature SIGNATURE_ALGORITHM;
    private static int SIGNATURE_LENGTH;
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

    public static synchronized void initializeSignature(Set<TrustedClient> trustedClients, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        TRUSTED_CLIENTS = new ConcurrentHashMap<>();

        if (trustedClients != null)
            trustedClients.forEach(client -> TRUSTED_CLIENTS.put(client.getIdentifier(), client));

        LOGGER.debug(TRUSTED_CLIENTS.toString());

        String algorithmName = "SHA256with" + privateKey.getAlgorithm();

        LOGGER.info("Signature algorithm to be used: " + algorithmName);

        SIGNATURE_ALGORITHM = Signature.getInstance(algorithmName);
        SIGNATURE_ALGORITHM.initSign(privateKey);

        // Perform a test signature to determine byte length
        // Also checks to see if signing works
        SIGNATURE_LENGTH = sign("test".getBytes()).length;

        LOGGER.debug("Initialized signature algorithm, signature length is " + SIGNATURE_LENGTH + " bytes");

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

            // Read signature

            byte[] signature = new byte[SIGNATURE_LENGTH];
            int result = in.read(signature);

            if (result != signature.length) {
                LOGGER.debug("Buffer underflow for signature, discarding packet " +
                        "(read " + result + ", expected " + SIGNATURE_LENGTH + ")");
                return null;
            }

            // Verify signature
            try {

                byte[] packetPayload = new byte[MAGIC.length + packet.length()];
                System.arraycopy(payload, 0, packetPayload, 0, packetPayload.length);

                if (!verifySignature(client.getPublicKey(), packetPayload, signature)) {

                    LOGGER.debug("Invalid signature, discarding packet");
                    return null;

                }

            } catch (InvalidKeyException | SignatureException e) {
                LOGGER.debug("Exception raised during signature verification", e);
                return null; // Discard packet
            }

            // Signature verified, update the max nonce
            client.setLargestNonceReceived(authenticatedPacket.getNonce());

            LOGGER.debug("Updating maximum nonce received for " + identifier + " to " + client.getLargestNonceReceived());

            //TODO Save to trusted clients flat file, IO thread perhaps

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

            byte[] signature;

            //TODO Reduce packet size by using HMAC with shared secret instead of RSA key-signing

            try {
                signature = sign(packetPayload);
            } catch (SignatureException e) {
                throw new IOException(e);
            }

            if (signature.length != SIGNATURE_LENGTH)
                throw new IOException("Expected signature length of " + SIGNATURE_LENGTH + ", actual is " + signature.length);

            byte[] payload = new byte[packetPayload.length + signature.length];
            System.arraycopy(packetPayload, 0, payload, 0, packetPayload.length);
            System.arraycopy(signature, 0, payload, packetPayload.length, signature.length);

            return payload;

        } else {

            return packetPayload;

        }

    }

    private static synchronized boolean verifySignature(PublicKey publicKey, byte[] data, byte[] signature) throws InvalidKeyException, SignatureException {
        SIGNATURE_ALGORITHM.initVerify(publicKey);
        SIGNATURE_ALGORITHM.update(data);
        return SIGNATURE_ALGORITHM.verify(signature);
    }

    private static synchronized byte[] sign(byte[] data) throws SignatureException {
        SIGNATURE_ALGORITHM.update(data);
        return SIGNATURE_ALGORITHM.sign();
    }

}
