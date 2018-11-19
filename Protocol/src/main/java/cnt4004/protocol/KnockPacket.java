package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;

public class KnockPacket extends SignedPacket implements Comparable<KnockPacket> {

    private byte sequence;
    private byte maxSequence;
    private Instant timestamp;
    //TODO Vulnerability: What if mallory modifies the destination port in transport layer?

    public KnockPacket() {
        timestamp = Instant.ofEpochSecond(0);
    }

    public KnockPacket(String clientIdentifier, Instant timestamp, byte sequence, byte maxSequence) {
        setClientIdentifier(clientIdentifier);
        setTimestamp(timestamp);
        setMaxSequence(maxSequence);
        setSequence(sequence);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public byte getSequence() {
        return sequence;
    }

    public void setSequence(byte sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("Sequence cannot be negative");

        if (sequence > maxSequence)
            throw new IllegalArgumentException("Sequence cannot be larger than max sequence");

        this.sequence = sequence;
    }

    public short getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(byte maxSequence) {
        if (maxSequence < sequence)
            throw new IllegalArgumentException("Max sequence cannot be less than sequence");

        this.maxSequence = maxSequence;
    }

    @Override
    public byte getID() {
        return 0;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        sequence = in.readByte();
        maxSequence = in.readByte();
        timestamp = Instant.ofEpochSecond((in.readInt() & 0x00000000FFFFFFFFL));
        super.read(in);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeByte(sequence);
        out.writeByte(maxSequence);
        out.writeInt((int) timestamp.getEpochSecond());
        super.write(out);
    }

    @Override
    public int length() {
        return super.length() + 6;
    }

    @Override
    public int compareTo(KnockPacket o) {
        return Short.compare(sequence, o.sequence);
    }
}
