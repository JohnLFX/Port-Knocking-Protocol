package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class NoncePacket implements Packet {

    private UUID nonce;

    public NoncePacket() {
    }

    public NoncePacket(UUID nonce) {
        this.nonce = nonce;
    }

    public UUID getNonce() {
        return nonce;
    }

    public void setNonce(UUID nonce) {
        this.nonce = nonce;
    }

    @Override
    public byte getID() {
        return 1;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        long mostSignificant = in.readLong();
        long leastSignificant = in.readLong();

        if (mostSignificant == Long.MIN_VALUE && leastSignificant == Long.MAX_VALUE) {
            nonce = null;
        } else {
            nonce = new UUID(mostSignificant, leastSignificant);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        if (nonce == null) {
            // TODO Sentinel values
            out.writeLong(Long.MIN_VALUE);
            out.writeLong(Long.MAX_VALUE);
        } else {
            out.writeLong(nonce.getMostSignificantBits());
            out.writeLong(nonce.getLeastSignificantBits());
        }
    }

    @Override
    public int length() {
        return 16; // 2 longs is 2 * 8 bytes
    }

}
