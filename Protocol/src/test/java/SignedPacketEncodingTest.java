import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;

public class SignedPacketEncodingTest {

    @Test
    public void test() throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);

        KeyPair serverKeyPair = kpg.generateKeyPair();
        KeyPair clientKeyPair = kpg.generateKeyPair();

        TrustedClient client = new TrustedClient("com1", clientKeyPair.getPublic());

        KnockPacket packet = new KnockPacket(client.getIdentifier(), Instant.now(), (byte) 0, (byte) 0);

        // Encode it with client stuff
        ProtocolMap.initializeSignature(null, clientKeyPair.getPrivate());
        byte[] payload = ProtocolMap.encodePacket(packet);

        // Decode it with server stuff
        ProtocolMap.initializeSignature(new HashSet<>(Collections.singletonList(client)), serverKeyPair.getPrivate());
        ProtocolMap.decodePayload(payload);

        // No exceptions raised, test passed

    }

}
