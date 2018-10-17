package cnt4004.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPKnockServer {

    private final InetAddress bindAddress;
    private final List<Integer> portSequence;
    private final ConcurrentMap<UUID, KnockSession> sessions = new ConcurrentHashMap<>();
    private ExecutorService portListeners = null;

    public UDPKnockServer(InetAddress bindAddress, List<Integer> portSequence) throws SocketException {
        this.bindAddress = bindAddress;
        this.portSequence = Collections.unmodifiableList(portSequence);
        bindPorts();
    }

    public void bindPorts() throws SocketException {

        if (portListeners != null)
            throw new IllegalStateException("Already bound");

        portListeners = Executors.newFixedThreadPool(portSequence.size());

        for (int port : portSequence) {

            InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, port);

            portListeners.execute(new UDPKnockPortListener(socketAddress));

            System.out.println("Listening on " + socketAddress);

        }

    }

    /**
     * Creates a new KnockSession
     *
     * @return The new KnockSession. Returns null if there is no more room allocated for a new session.
     */
    public KnockSession createSession() {

        KnockSession session = new KnockSession();

        // The probability of replacing an existing session is negligible
        sessions.put(session.getNonce(), session);

        return session;

    }

}

class KnockSession {

    private final UUID nonce;

    KnockSession() {
        this.nonce = UUID.randomUUID();
    }

    public UUID getNonce() {
        return nonce;
    }

}
