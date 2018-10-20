package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
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
        nonce = StreamUtility.readUUID(in);
        sequence = in.readShort();
        maxSequence = in.readShort();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        StreamUtility.writeUUID(out, nonce);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnockPacket packet = (KnockPacket) o;
        return sequence == packet.sequence &&
                maxSequence == packet.maxSequence &&
                Objects.equals(nonce, packet.nonce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, sequence, maxSequence);
    }

    @Override
    public int compareTo(KnockPacket o) {
        return Short.compare(sequence, o.sequence);
    }

}
