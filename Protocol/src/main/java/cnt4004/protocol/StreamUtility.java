package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

class StreamUtility {

    private StreamUtility() {
    }

    static void writeUUID(DataOutputStream out, UUID uuid) throws IOException {
        if (uuid == null) {
            // TODO Determine sentinel values
            out.writeLong(Long.MIN_VALUE);
            out.writeLong(Long.MAX_VALUE);
        } else {
            out.writeLong(uuid.getMostSignificantBits());
            out.writeLong(uuid.getLeastSignificantBits());
        }
    }

    static UUID readUUID(DataInputStream in) throws IOException {
        long mostSignificant = in.readLong();
        long leastSignificant = in.readLong();

        if (mostSignificant == Long.MIN_VALUE && leastSignificant == Long.MAX_VALUE) {
            return null;
        } else {
            return new UUID(mostSignificant, leastSignificant);
        }
    }

}
