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
        nonce = StreamUtility.readUUID(in);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        StreamUtility.writeUUID(out, nonce);
    }

    @Override
    public int length() {
        return 16; // 2 longs is 2 * 8 bytes
    }

}
