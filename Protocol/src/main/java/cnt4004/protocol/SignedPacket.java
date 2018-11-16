package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class SignedPacket implements Packet {

    private String clientIdentifier;

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        if (clientIdentifier.length() > 4)
            throw new IllegalArgumentException("Client identifier is too long, max is 4 characters");

        this.clientIdentifier = clientIdentifier;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        byte[] buffer = new byte[4];

        int result = in.read(buffer);

        if (result != -1)
            clientIdentifier = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(buffer)).toString();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(StandardCharsets.US_ASCII.encode(clientIdentifier).array());
    }

    @Override
    public int length() {
        return 4;
    }

}
