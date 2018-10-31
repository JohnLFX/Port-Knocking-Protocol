package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * AckPacket
 * This packet is sent to acknowledge a received KnockPacket
 */
public class AckPacket implements Packet {

    /**
     * The nonce
     */
    private UUID nonce;

    /**
     * Sequence ID the Acknowledgement is for
     */
    private short sequence;

    public AckPacket() {
    }

    /**
     * Creates a new AckPacket
     *
     * @param nonce    The nonce
     * @param sequence The sequence ID
     */
    public AckPacket(UUID nonce, short sequence) {
        this.nonce = nonce;
        this.sequence = sequence;
    }

    /**
     * Nonce
     * @return The nonce
     */
    public UUID getNonce() {
        return nonce;
    }

    /**
     * Sets the nonce
     * @param nonce The new nonce value
     */
    public void setNonce(UUID nonce) {
        this.nonce = nonce;
    }

    /**
     * Sequence ID
     * @return Acknowledged Sequence ID for a KnockPacket
     */
    public short getSequence() {
        return sequence;
    }

    /**
     * Sets the acknowledged sequence ID
     * @param sequence The new sequence ID
     */
    public void setSequence(short sequence) {
        this.sequence = sequence;
    }

    @Override
    public byte getID() {
        return 2;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        nonce = StreamUtility.readUUID(in);
        sequence = in.readShort();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        StreamUtility.writeUUID(out, nonce);
        out.writeShort(sequence);
    }

    @Override
    public int length() {
        return 2 * 8 + 2;
    }

}
