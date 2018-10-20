package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class AckPacket implements Packet {

    private UUID nonce;
    private short sequence;

    public AckPacket() {
    }

    public AckPacket(UUID nonce, short sequence) {
        this.nonce = nonce;
        this.sequence = sequence;
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
