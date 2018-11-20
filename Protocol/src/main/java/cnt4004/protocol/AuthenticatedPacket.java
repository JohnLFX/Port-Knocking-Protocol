package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public abstract class AuthenticatedPacket implements Packet {

    private String clientIdentifier;
    private long nonce;
    private Instant timestamp;

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        if (clientIdentifier.length() > 4)
            throw new IllegalArgumentException("Client identifier is too long, max is 4 characters");

        this.clientIdentifier = clientIdentifier;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        if (nonce < 0)
            throw new IllegalArgumentException("Nonce cannot be negative");

        if (nonce > 4294967295L)
            throw new IllegalArgumentException("Maximum size is an unsigned 4-byte integer (2^32)");

        this.nonce = nonce;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        byte[] buffer = new byte[4];

        int result = in.read(buffer);

        if (result == -1)
            throw new EOFException();

        clientIdentifier = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(buffer)).toString();
        timestamp = Instant.ofEpochSecond((in.readInt() & 0x00000000FFFFFFFFL));
        nonce = (in.readInt() & 0x00000000FFFFFFFFL);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.write(StandardCharsets.US_ASCII.encode(clientIdentifier).array());
        out.writeInt((int) timestamp.getEpochSecond());
        out.writeInt((int) nonce);
    }

    @Override
    public int length() {
        return 4 + 4;
    }

}
