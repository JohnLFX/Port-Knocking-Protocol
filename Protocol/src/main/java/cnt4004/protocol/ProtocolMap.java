package cnt4004.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ProtocolMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolMap.class);

    private static final byte[] MAGIC = new byte[]{
            '7', 'S', 'z', 'C', 'L', 'C', 'g', 'c'
    };

    public static final int MAX_BUFFER = 100; // TODO Determine buffer

    private static final Map<Byte, Constructor<? extends Packet>> PACKET_MAP = new HashMap<>();

    private static Mac HMAC;
    private static int HMAC_LENGTH;

    private static final ReentrantLock HMAC_LOCK = new ReentrantLock();

    static {
        // Make sure that the byte is unique for each packet
        try {
            PACKET_MAP.put((byte) 0, KnockPacket.class.getDeclaredConstructor());
            PACKET_MAP.put((byte) 1, NoncePacket.class.getDeclaredConstructor());
            PACKET_MAP.put((byte) 2, AckPacket.class.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

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

    public static synchronized void initializeHMAC(SecretKeySpec secretKeySpec) throws InvalidKeyException, NoSuchAlgorithmException {

        if (HMAC != null)
            throw new IllegalStateException("Already initialized");

        HMAC = Mac.getInstance(secretKeySpec.getAlgorithm());
        HMAC.init(secretKeySpec);

        // Calculate HMAC length
        HMAC_LENGTH = HMAC.doFinal("test".getBytes()).length;

        LOGGER.debug("Initialized MAC: " + secretKeySpec.getAlgorithm() + " with output length of " + HMAC_LENGTH);

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

        byte packetID = in.readByte(); // Packet ID (byte)

        Packet packet = ProtocolMap.createFromID(packetID);

        if (packet == null) {
            LOGGER.debug("Unknown packet ID: " + packetID);
            return null;
        }

        packet.read(in);

        byte[] parsedMAC = new byte[HMAC_LENGTH];
        in.readFully(parsedMAC);

        in.close();

        byte[] calculatedMAC;

        HMAC_LOCK.lock();

        try {

            HMAC.update(payload, 0, MAGIC.length + 1 + packet.length());
            calculatedMAC = HMAC.doFinal();

        } finally {
            HMAC_LOCK.unlock();
        }

        if (!Arrays.equals(parsedMAC, calculatedMAC)) {
            LOGGER.debug("Bad MAC: " + Arrays.toString(parsedMAC) + " not equal to " + Arrays.toString(calculatedMAC));
            return null;
        }

        return packet;

    }

    public static byte[] generatePayload(Packet packet) throws IOException {

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outputBuffer);

        out.write(MAGIC);
        out.writeByte(packet.getID());
        packet.write(out);

        out.close();

        byte[] packetPayload = outputBuffer.toByteArray();
        byte[] mac;

        HMAC_LOCK.lock();

        try {
            mac = HMAC.doFinal(packetPayload);
        } finally {
            HMAC_LOCK.unlock();
        }

        int payloadLength = packetPayload.length + mac.length;

        if (payloadLength > MAX_BUFFER)
            throw new IOException("Payload overflows buffer (Packet length: " + payloadLength + ", max buffer: " + MAX_BUFFER + ")");

        byte[] payload = new byte[payloadLength];

        System.arraycopy(packetPayload, 0, payload, 0, packetPayload.length);
        System.arraycopy(mac, 0, payload, packetPayload.length, mac.length);

        return payload;

    }

}
