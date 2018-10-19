package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {

    byte getID();

    void read(DataInputStream in) throws IOException;

    void write(DataOutputStream out) throws IOException;

    int length();

}
