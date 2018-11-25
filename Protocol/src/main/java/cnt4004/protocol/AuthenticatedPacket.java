package cnt4004.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Abstract class that represents an authenticated packet.
 * If a packet is authenticated, then the packet must have client identification, a nonce, and timestamp of transmission.
 * Authenticated packets are used for calculating the MAC
 */
public abstract class AuthenticatedPacket implements Packet {

    private String clientIdentifier;
    private long nonce;
    private Instant timestamp;

    /**
     * Creates a new authenticated packet with a default
     * nonce of 0, null clientIdentifier, and 0 seconds after epoch timestamp.
     */
    public AuthenticatedPacket() {
        timestamp = Instant.ofEpochSecond(0); // Beginning of time
    }

    /**
     * Client Identifier
     *
     * @return The client identifier
     */
    public String getClientIdentifier() {
        return clientIdentifier;
    }

    /**
     * Sets the client identifier
     * @param clientIdentifier The client identifier to use
     * @throws IllegalArgumentException If the client identifier is over 4 characters in length
     */
    public void setClientIdentifier(String clientIdentifier) {
        if (clientIdentifier.length() > 4)
            throw new IllegalArgumentException("Client identifier is too long, max is 4 characters");

        this.clientIdentifier = clientIdentifier;
    }

    /**
     * Nonce
     * @return The nonce
     */
    public long getNonce() {
        return nonce;
    }

    /**
     * Sets the nonce
     * @param nonce The nonce to use
     * @throws IllegalArgumentException If the nonce is negative or greater than 2^32
     */
    public void setNonce(long nonce) {
        if (nonce < 0)
            throw new IllegalArgumentException("Nonce cannot be negative");

        if (nonce > 4294967295L)
            throw new IllegalArgumentException("Maximum size is an unsigned 4-byte integer (2^32)");

        this.nonce = nonce;
    }

    /**
     * The timestamp is when the packet was transmitted
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for when the packet was transmitted
     * @param timestamp The timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        byte[] buffer = new byte[4];

        int result = in.read(buffer);

        if (result == -1)
            throw new EOFException();

        clientIdentifier = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(buffer)).toString();
        // Convert to unsigned 4-byte integer
        timestamp = Instant.ofEpochSecond((in.readInt() & 0x00000000FFFFFFFFL));
        nonce = (in.readInt() & 0x00000000FFFFFFFFL);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        // Use ASCII encoding, because 1 character is 1 byte
        out.write(StandardCharsets.US_ASCII.encode(clientIdentifier).array());
        out.writeInt((int) timestamp.getEpochSecond());
        out.writeInt((int) nonce);
    }

    @Override
    public int length() {
        return 12;
    }

}
