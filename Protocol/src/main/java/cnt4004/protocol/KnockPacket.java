package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class KnockPacket extends SignedPacket implements Comparable<KnockPacket> {

    private short sequence;
    private short maxSequence;
    private int nonce;
    //TODO Vulnerability: What if mallory modifies the destination port in transport layer?

    public KnockPacket() {
    }

    public KnockPacket(String clientIdentifier, int nonce, short sequence, short maxSequence) {
        setClientIdentifier(clientIdentifier);
        setNonce(nonce);
        setMaxSequence(maxSequence);
        setSequence(sequence);
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        if (nonce > 16777216)
            throw new IllegalArgumentException("Maximum value for nonce is 16777216 (3 bytes unsigned)");

        if (nonce < 0)
            throw new IllegalArgumentException("Nonce cannot be negative");

        this.nonce = nonce;
    }

    public short getSequence() {
        return sequence;
    }

    public void setSequence(short sequence) {
        if (sequence < 0)
            throw new IllegalArgumentException("Sequence cannot be negative");

        if (sequence > maxSequence)
            throw new IllegalArgumentException("Sequence cannot be larger than max sequence");

        this.sequence = sequence;
    }

    public short getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(short maxSequence) {
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
        nonce = ((in.readByte() & 0xF) << 16) | ((in.readByte() & 0xFF) << 8) | (in.readByte() & 0xFF);
        sequence = in.readShort();
        maxSequence = in.readShort();
        super.read(in);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeByte((nonce >> 16) & 0xff);
        out.writeByte((nonce >> 8) & 0xff);
        out.writeByte(nonce & 0xff);
        out.writeShort(sequence);
        out.writeShort(maxSequence);
        super.write(out);
    }

    @Override
    public int length() {
        // 1 short = 2 bytes
        return 2 + 2 + 3 + super.length();
    }

    @Override
    public int compareTo(KnockPacket o) {
        return Short.compare(sequence, o.sequence);
    }
}
