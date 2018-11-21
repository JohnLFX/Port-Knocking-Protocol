package cnt4004.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class TrustedClient {

    public static final int MAC_LENGTH = 32;
    public static final String MAC_ALGORITHM = "HmacSHA256";

    private String identifier;
    private Mac macAlgorithm;
    private long largestNonceReceived;

    public TrustedClient(String identifier, String sharedSecret, long largestNonceReceived) throws NoSuchAlgorithmException, InvalidKeyException {
        this.identifier = identifier;
        this.largestNonceReceived = largestNonceReceived;

        this.macAlgorithm = Mac.getInstance(MAC_ALGORITHM);
        this.macAlgorithm.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));

        if (this.macAlgorithm.doFinal("test".getBytes()).length != MAC_LENGTH)
            throw new IllegalArgumentException("MAC algorithm does not generate the expected MAC_LENGTH");
    }

    public byte[] createMAC(byte[] payload) {
        return macAlgorithm.doFinal(payload);
    }

    public long getLargestNonceReceived() {
        return largestNonceReceived;
    }

    public void setLargestNonceReceived(long largestNonceReceived) {
        if (largestNonceReceived < 0)
            throw new IllegalArgumentException("Nonce cannot be negative");

        this.largestNonceReceived = largestNonceReceived;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustedClient that = (TrustedClient) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
