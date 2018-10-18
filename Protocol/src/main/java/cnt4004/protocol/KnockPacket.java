package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class KnockPacket implements Packet {

    private UUID nonce;

    public UUID getNonce() {
        return nonce;
    }

    public void setNonce(UUID nonce) {
        this.nonce = nonce;
    }

    @Override
    public byte getID() {
        return 0;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        nonce = new UUID(in.readLong(), in.readLong());
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(nonce.getMostSignificantBits());
        out.writeLong(nonce.getLeastSignificantBits());
    }

}
