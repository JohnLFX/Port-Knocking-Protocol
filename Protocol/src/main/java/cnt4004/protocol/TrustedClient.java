package cnt4004.protocol;

import java.security.PublicKey;
import java.util.Objects;

public class TrustedClient {

    private String identifier;
    private PublicKey publicKey;
    private long largestNonceReceived;

    public TrustedClient(String identifier, PublicKey publicKey, long largestNonceReceived) {
        this.identifier = identifier;
        this.publicKey = publicKey;
        this.largestNonceReceived = largestNonceReceived;
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

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
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
