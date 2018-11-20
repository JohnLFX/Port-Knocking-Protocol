package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class KnockPacket extends AuthenticatedPacket implements Comparable<KnockPacket> {

    private byte sequence;
    private byte maxSequence;
    //TODO Vulnerability: What if mallory modifies the destination port in transport layer?

    public KnockPacket() {
    }

    public KnockPacket(String identifier, long nonce, byte sequence, byte maxSequence) {
        setClientIdentifier(identifier);
        setNonce(nonce);
        setMaxSequence(maxSequence);
        setSequence(sequence);
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
        super.read(in);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeByte(sequence);
        out.writeByte(maxSequence);
        super.write(out);
    }

    @Override
    public int length() {
        return super.length() + 2;
    }

    @Override
    public int compareTo(KnockPacket o) {
        return Short.compare(sequence, o.sequence);
    }

    @Override
    public String toString() {
        return "KnockPacket{" +
                "sequence=" + sequence +
                ", maxSequence=" + maxSequence +
                ", identifier=" + getClientIdentifier() +
                ", timestamp=" + getTimestamp().getEpochSecond() +
                '}';
    }
}
