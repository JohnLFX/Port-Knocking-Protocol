package cnt4004.server;

import cnt4004.protocol.KnockPacket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPKnockServer {

    private final InetAddress bindAddress;
    private final List<Integer> portSequence;
    private final ConcurrentMap<UUID, KnockSession> sessions = new ConcurrentHashMap<>(); // TODO Auto-expiring & limits
    private ExecutorService portListeners = null;
    private final PacketHandler packetHandler;

    public UDPKnockServer(InetAddress bindAddress, List<Integer> portSequence) throws SocketException {
        this.bindAddress = bindAddress;
        if (portSequence.contains(null))
            throw new IllegalArgumentException("Port sequence cannot contain null elements");

        this.portSequence = Collections.unmodifiableList(portSequence);
        this.packetHandler = new PacketHandler(this);
        bindPorts();
    }

    public void bindPorts() throws SocketException {

        if (portListeners != null)
            throw new IllegalStateException("Already bound");

        portListeners = Executors.newFixedThreadPool(portSequence.size());

        for (int port : portSequence) {

            InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, port);

            portListeners.execute(new UDPKnockPortListener(packetHandler, socketAddress));

            System.out.println("Listening on " + socketAddress);

        }

    }

    public short finalSequenceID() {
        return (short) portSequence.size();
    }

    public List<Integer> getPortSequence() {
        return portSequence;
    }

    public KnockSession getSession(UUID nonce) {
        if (nonce == null)
            return null;

        return sessions.get(nonce);
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

    public void removeSession(KnockSession session) {
        sessions.remove(session.getNonce());
    }

}

class KnockSession {

    private final UUID nonce;
    private final SortedMap<KnockPacket, Integer> knockSequence;

    KnockSession() {
        this.nonce = UUID.randomUUID();
        this.knockSequence = new TreeMap<>();
    }

    public void addKnockPacket(KnockPacket packet, int knockedPort) {
        knockSequence.put(packet, knockedPort);
    }

    public boolean sequenceComplete() {
        if (knockSequence.isEmpty())
            return false;

        short prevSequence = 0;
        boolean reachedMax = false;
        for (KnockPacket packet : knockSequence.keySet()) {
            if (packet.getSequence() != prevSequence++) {
                return false;
            }
            if (packet.getSequence() == packet.getMaxSequence()) {
                reachedMax = true;
            }
        }
        return reachedMax;
    }

    public List<Integer> getCurrentKnockSequence() {
        List<Integer> list = new ArrayList<>();
        // TODO Test if ordering is always right, and see knockSequence.values()
        for (Map.Entry<KnockPacket, Integer> en : knockSequence.entrySet()) {
            list.add(en.getValue());
        }
        return Collections.unmodifiableList(list);
    }

    public UUID getNonce() {
        return nonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnockSession session = (KnockSession) o;
        return Objects.equals(nonce, session.nonce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce);
    }

}
