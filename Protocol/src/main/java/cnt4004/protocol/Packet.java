package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Packet interface, used for stream serializing
 */
public interface Packet {

    /**
     * The unique ID of the packet.
     * Two different packets cannot share the same ID.
     *
     * @return Unique ID
     */
    byte getID();

    /**
     * Deserialize an input stream and populate the packet fields
     * @param in The data input stream
     * @throws IOException Exception reading from stream
     */
    void read(DataInputStream in) throws IOException;

    /**
     * Serializes the packet fields to an output stream
     * @param out The data output stream
     * @throws IOException Exception writing to stream
     */
    void write(DataOutputStream out) throws IOException;

    /**
     * The amount of bytes the packet fields
     * requires for reading and writing to a stream
     * @return The amount of bytes used by the packet fields
     */
    int length();

}
