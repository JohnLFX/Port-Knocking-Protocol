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
        if (in.readBoolean()) {
            nonce = null;
        } else {
            nonce = new UUID(in.readLong(), in.readLong());
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        if (nonce == null) {
            out.writeBoolean(true);
        } else {
            out.writeBoolean(false);
            out.writeLong(nonce.getMostSignificantBits());
            out.writeLong(nonce.getLeastSignificantBits());
        }
    }

}
