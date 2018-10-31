package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Private utility class used for serializing fields
 * that are not included in the DataInputStream or DataOutputStream class
 */
class StreamUtility {

    // No instances
    private StreamUtility() {
    }

    /**
     * Serializes a UUID to an output stream. This method will write 16 bytes (2 longs)
     *
     * @param out  The DataOutputStream to write to
     * @param uuid The UUID to write
     * @throws IOException Exception while writing the UUID
     */
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

    /**
     * Deserialize a UUID from an input stream. This method will read 16 bytes (2 longs)
     * @param in The DataInputStream to read from
     * @return The read UUID
     * @throws IOException Exception while reading the UUID
     */
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
