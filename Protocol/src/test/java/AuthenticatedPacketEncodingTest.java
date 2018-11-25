import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

public class AuthenticatedPacketEncodingTest {

    @Test
    public void test() throws Exception {

        TrustedClient client = new TrustedClient("com1", "testKey", 0);

        KnockPacket packet = new KnockPacket(client.getIdentifier(), 0, (byte) 0, (byte) 0);

        ProtocolMap.setTrustedClients(new HashSet<>(Collections.singletonList(client)));

        byte[] payload = ProtocolMap.encodePacket(packet);
        ProtocolMap.decodePayload(payload);

        // No exceptions raised, test passed

    }

}
