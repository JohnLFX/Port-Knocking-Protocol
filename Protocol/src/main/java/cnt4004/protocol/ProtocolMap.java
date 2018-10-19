package cnt4004.protocol;

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

public class ProtocolMap {

    private static final byte[] MAGIC = new byte[]{
            '7', 'S', 'z', 'C', 'L', 'C', 'g', 'c'
    };

    public static final int MAX_BUFFER = 100; // TODO Determine buffer

    private static final Map<Byte, Constructor<? extends Packet>> PACKET_MAP = new HashMap<>();

    private static Mac HMAC;
    private static int HMAC_LENGTH;

    static {
        try {
            PACKET_MAP.put((byte) 0, KnockPacket.class.getDeclaredConstructor());
            PACKET_MAP.put((byte) 1, NoncePacket.class.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Packet createFromID(byte id) {
        Constructor<? extends Packet> constructor = PACKET_MAP.get(id);
        if (constructor != null) {
            try {
                return constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace(); // Exception shouldn't happen anyways
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

        System.out.println("Initialized MAC: " + secretKeySpec.getAlgorithm() + " with output length of " + HMAC_LENGTH);

    }

    public static synchronized Packet decodePayload(byte[] payload) throws IOException {

        ByteArrayInputStream inBuffer = new ByteArrayInputStream(payload);
        DataInputStream in = new DataInputStream(inBuffer);

        byte[] receivedMagic = new byte[MAGIC.length];
        int read = in.read(receivedMagic, 0, MAGIC.length);

        if (read != MAGIC.length || !Arrays.equals(receivedMagic, MAGIC)) {
            System.out.println("Bad magic packet");
            return null;
        }

        byte packetID = in.readByte(); // Packet ID (byte)

        Packet packet = ProtocolMap.createFromID(packetID);

        if (packet == null) {
            System.out.println("Unknown packet ID: " + packetID);
            return null;
        }

        packet.read(in);

        byte[] parsedMAC = new byte[HMAC_LENGTH];
        in.readFully(parsedMAC);

        in.close();

        HMAC.update(payload, 0, MAGIC.length + 1 + packet.length());
        byte[] calculatedMAC = HMAC.doFinal();

        if (!Arrays.equals(parsedMAC, calculatedMAC)) {
            System.out.println("Bad MAC: " + Arrays.toString(parsedMAC) + " | " + Arrays.toString(calculatedMAC));
            return null;
        }

        return packet;

    }

    public static synchronized byte[] generatePayload(Packet packet) throws IOException {

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outputBuffer);

        out.write(MAGIC);
        out.writeByte(packet.getID());
        packet.write(out);

        out.close();

        byte[] packetPayload = outputBuffer.toByteArray();

        byte[] mac = HMAC.doFinal(packetPayload);

        System.out.println(Arrays.toString(mac));

        int payloadLength = packetPayload.length + mac.length;

        if (payloadLength > MAX_BUFFER)
            throw new IOException("Payload overflows buffer (Packet length: " + payloadLength + ", max buffer: " + MAX_BUFFER + ")");

        byte[] payload = new byte[payloadLength];

        System.arraycopy(packetPayload, 0, payload, 0, packetPayload.length);
        System.arraycopy(mac, 0, payload, packetPayload.length, mac.length);

        return payload;

    }

}
