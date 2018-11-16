package cnt4004.protocol;

import java.security.PublicKey;
import java.util.Objects;

public class TrustedClient {

    private String identifier;
    private PublicKey publicKey;
    private int currentNonce;

    public TrustedClient(String identifier, PublicKey publicKey, int currentNonce) {
        this.identifier = identifier;
        this.publicKey = publicKey;
        this.currentNonce = currentNonce;
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

    public int getCurrentNonce() {
        return currentNonce;
    }

    public void setCurrentNonce(int currentNonce) {
        this.currentNonce = currentNonce;
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
