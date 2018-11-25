package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A knock packet is used for sending a single knock to a specific UDP port.
 * This class contains the encoding and decoding for a Knock packet.
 * This class is comparable to another KnockPacket. The order compares {@link KnockPacket#getSequence()}
 * in ascending order.
 */
public class KnockPacket extends AuthenticatedPacket implements Comparable<KnockPacket> {

    /**
     * Sequence ID
     */
    private byte sequence;

    /**
     * Maximum sequence ID
     */
    private byte maxSequence;

    public KnockPacket() {
    }

    public KnockPacket(String identifier, long nonce, byte sequence, byte maxSequence) {
        setClientIdentifier(identifier);
        setNonce(nonce);
        setMaxSequence(maxSequence);
        setSequence(sequence);
    }

    /**
     * Sequence ID
     *
     * @return The sequence ID
     */
    public byte getSequence() {
        return sequence;
    }

    /**
     * Sets the sequence ID
     * @param sequence The new sequence ID
     * @throws IllegalArgumentException If the sequence ID is negative or larger than {@link KnockPacket#getMaxSequence()}}
     */
    public void setSequence(byte sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("Sequence cannot be negative");

        if (sequence > maxSequence)
            throw new IllegalArgumentException("Sequence cannot be larger than max sequence");

        this.sequence = sequence;
    }

    /**
     * Max sequence ID
     *
     * @return The max sequence ID
     */
    public byte getMaxSequence() {
        return maxSequence;
    }

    /**
     * Sets the maximum sequence ID
     * @param maxSequence The new maximum sequence ID
     * @throws IllegalArgumentException If the parameter is less than {@link KnockPacket#getSequence()}
     */
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
        return Byte.compare(sequence, o.sequence);
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
