package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class KnockPacket implements Packet, Comparable<KnockPacket> {

    private UUID nonce;
    private short sequence;
    private short maxSequence;

    public KnockPacket() {
    }

    public KnockPacket(UUID nonce, short sequence, short maxSequence) {
        this.nonce = nonce;
        this.sequence = sequence;
        this.maxSequence = maxSequence;
    }

    public UUID getNonce() {
        return nonce;
    }

    public void setNonce(UUID nonce) {
        this.nonce = nonce;
    }

    public short getSequence() {
        return sequence;
    }

    public void setSequence(short sequence) {
        this.sequence = sequence;
    }

    public short getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(short maxSequence) {
        this.maxSequence = maxSequence;
    }

    @Override
    public byte getID() {
        return 0;
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
        sequence = in.readShort();
        maxSequence = in.readShort();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        if (nonce == null) {
            // TODO Determine sentinel values
            out.writeLong(Long.MIN_VALUE);
            out.writeLong(Long.MAX_VALUE);
        } else {
            out.writeLong(nonce.getMostSignificantBits());
            out.writeLong(nonce.getLeastSignificantBits());
        }
        out.writeShort(sequence);
        out.writeShort(maxSequence);
    }

    @Override
    public int length() {
        // 2 longs is 2 * 8 bytes
        // 1 short = 2 bytes
        return 8 + 8 + 2 + 2;
    }

    @Override
    public int compareTo(KnockPacket o) {
        return Short.compare(sequence, o.sequence);
    }

}
