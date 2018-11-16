import cnt4004.protocol.KnockPacket;
import org.junit.Before;
import org.junit.Test;

public class TestKnockPacket {

    private KnockPacket knockPacket;

    @Before
    public void setup() {
        this.knockPacket = new KnockPacket();
        knockPacket.setClientIdentifier("com1");
        knockPacket.setMaxSequence((short) 1);
        knockPacket.setSequence((short) 0);
        knockPacket.setNonce(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxNonce() {
        knockPacket.setNonce((int) Math.pow(2, 24) + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNonce() {
        knockPacket.setNonce(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSequence() {
        knockPacket.setSequence((short) (knockPacket.getMaxSequence() + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSequence() {
        knockPacket.setSequence((short) -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMaxSequence() {
        knockPacket.setMaxSequence((short) -1);
    }

}
