import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;

public class AuthenticatedPacketEncodingTest {

    @Test
    public void test() throws Exception {

        SecretKeySpec sharedSecret = new SecretKeySpec("testKey".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        TrustedClient client = new TrustedClient("com1", sharedSecret, 0);

        KnockPacket packet = new KnockPacket(client.getIdentifier(), 0, (byte) 0, (byte) 0);

        ProtocolMap.initializeHMAC(new HashSet<>(Collections.singletonList(client)));

        byte[] payload = ProtocolMap.encodePacket(packet);
        ProtocolMap.decodePayload(payload);

        // No exceptions raised, test passed

    }

}
