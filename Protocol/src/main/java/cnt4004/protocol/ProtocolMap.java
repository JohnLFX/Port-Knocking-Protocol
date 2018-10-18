package cnt4004.protocol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ProtocolMap {

    // TODO Unmodifiable?
    public static final byte[] MAGIC = new byte[]{
            '7', 'S', 'z', 'C', 'L', 'C', 'g', 'c'
    };

    public static final int MAX_BUFFER = 1000; // TODO Determine buffer

    private static final Map<Byte, Constructor<? extends Packet>> PACKET_MAP = new HashMap<>();

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

}
