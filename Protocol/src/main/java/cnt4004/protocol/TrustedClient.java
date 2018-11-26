package cnt4004.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Objects;

/**
 * A TrustedClient is an object representing a client that the server trusts.
 * In other words, this object contains the associated shared secret and identifier.
 */
public class TrustedClient {

    /**
     * Path to the text file (trusted_clients.txt)
     */
    private static Path FLAT_FILE;

    /* Constants for the MAC algorithm */
    static final int MAC_LENGTH = 32;
    private static final String MAC_ALGORITHM = "HmacSHA256";

    private final String sharedSecret;
    private final String identifier;
    private final Mac macAlgorithm;
    private long largestNonceReceived;

    /**
     * Creates a new TrustedClient object. This method will initialize a local MAC algorithm.
     *
     * @param identifier           The identifier
     * @param sharedSecret         The shared secret
     * @param largestNonceReceived The largest known nonce
     * @throws NoSuchAlgorithmException If the MAC algorithm required by the protocol is not found on the system
     * @throws InvalidKeyException      If the shared secret cannot be used for the MAC
     * @throws IllegalArgumentException If the MAC length generated by the algorithm does not match {@link TrustedClient#MAC_LENGTH}
     */
    public TrustedClient(String identifier, String sharedSecret, long largestNonceReceived) throws NoSuchAlgorithmException, InvalidKeyException {
        this.identifier = identifier;
        this.largestNonceReceived = largestNonceReceived;
        this.sharedSecret = sharedSecret;

        this.macAlgorithm = Mac.getInstance(MAC_ALGORITHM);
        this.macAlgorithm.init(new SecretKeySpec(this.sharedSecret.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));

        if (this.macAlgorithm.doFinal("test".getBytes()).length != MAC_LENGTH)
            throw new IllegalArgumentException("MAC algorithm does not generate the expected MAC_LENGTH");
    }

    /**
     * Synchronized method for creating the MAC using the algorithm {@link TrustedClient#MAC_ALGORITHM}
     *
     * @param payload The payload to use for the MAC
     * @return The MAC. The length of the byte[] array is always {@link TrustedClient#MAC_LENGTH}
     */
    public synchronized byte[] createMAC(byte[] payload) {
        return macAlgorithm.doFinal(payload);
    }

    /**
     * Returns the largest known nonce received from this client
     *
     * @return The largest known nonce received from this client
     */
    public long getLargestNonceReceived() {
        return largestNonceReceived;
    }

    /**
     * Sets the largest known nonce received for this client
     *
     * @param largestNonceReceived The current largest known nonce
     * @throws IllegalArgumentException If {@code largestNonceReceived} is negative
     */
    public void setLargestNonceReceived(long largestNonceReceived) {
        if (largestNonceReceived < 0)
            throw new IllegalArgumentException("Nonce cannot be negative");

        this.largestNonceReceived = largestNonceReceived;
    }

    /**
     * The current identifier
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the flat file path that {@link TrustedClient#saveTrustedClients(Collection)} uses
     *
     * @param flatFile The path
     */
    public static void setFlatFile(Path flatFile) {
        FLAT_FILE = flatFile;
    }

    /**
     * Writes a collection of trusted clients to the path defined by {@link TrustedClient#setFlatFile(Path)}
     *
     * @param trustedClients Collection of {@link TrustedClient}
     * @throws IOException IO Exception occurred while writing to file
     */
    static void saveTrustedClients(Collection<TrustedClient> trustedClients) throws IOException {

        try (PrintWriter writer = new PrintWriter(Files.newOutputStream(FLAT_FILE))) {

            for (TrustedClient client : trustedClients) {

                writer.print(client.identifier);
                writer.print(' ');
                writer.print(client.sharedSecret);
                writer.print(' ');
                writer.print(client.largestNonceReceived);
                writer.println();

            }

        }

    }

    /*
    Override equals() and hashCode() to only consider the identifier,
     because the identifier should always be unique per client
     */

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
