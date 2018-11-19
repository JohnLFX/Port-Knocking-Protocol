import cnt4004.protocol.KnockPacket;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

public class TestKnockPacket {

    private KnockPacket knockPacket;

    @Before
    public void setup() {
        this.knockPacket = new KnockPacket();
        knockPacket.setClientIdentifier("com1");
        knockPacket.setMaxSequence((byte) 1);
        knockPacket.setSequence((byte) 0);
        knockPacket.setTimestamp(Instant.now());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSequence() {
        knockPacket.setSequence((byte) (knockPacket.getMaxSequence() + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSequence() {
        knockPacket.setSequence((byte) -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMaxSequence() {
        knockPacket.setMaxSequence((byte) -1);
    }

}
