import cnt4004.protocol.KnockPacket;
import cnt4004.server.KnockSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class KnockSessionSequenceTest {

    private Constructor<KnockSession> constructor;
    private Random random;

    @Before
    public void init() throws Exception {
        constructor = KnockSession.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        random = new Random();
    }

    @Test
    public void test() throws Exception {

        KnockSession session = constructor.newInstance();

        Assert.assertNotNull(session.getNonce());
        Assert.assertTrue(session.getCurrentKnockSequence().isEmpty());
        Assert.assertFalse(session.sequenceComplete());

        List<Object[]> receivedPackets = new ArrayList<>();
        List<Integer> correctSequence = new ArrayList<>();

        short totalPackets = (short) (2 + random.nextInt(10));

        for (short i = 0; i < totalPackets; i++) {

            int port = random.nextInt();

            Assert.assertTrue(receivedPackets.add(new Object[]{
                    new KnockPacket(session.getNonce(), i, (short) (totalPackets - 1)),
                    port
            }));

            Assert.assertTrue(correctSequence.add(port));

        }

        Assert.assertEquals(correctSequence.size(), receivedPackets.size());

        // Simulate out-of-order arrival
        // (which is likely because its UDP and packets are processed with multiple threads)
        Collections.shuffle(receivedPackets);

        for (int i = 0; i < receivedPackets.size(); i++) {

            Object[] data = receivedPackets.get(i);

            session.addKnockPacket((KnockPacket) data[0], (int) data[1]);

            // Make sure the sequence is not complete while we are adding packets
            // Ignore the check for the last packet to be added, for now
            if ((i + 1) != receivedPackets.size())
                Assert.assertFalse("Packet sequence should be marked as incomplete", session.sequenceComplete());

        }

        Assert.assertTrue("Packet sequence should be marked as complete", session.sequenceComplete());

        Assert.assertEquals(correctSequence, session.getCurrentKnockSequence());

    }

}
