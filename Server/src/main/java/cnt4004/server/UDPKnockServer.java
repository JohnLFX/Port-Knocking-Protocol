package cnt4004.server;

import cnt4004.server.network.PacketConsumer;
import cnt4004.server.network.UDPKnockPortListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UDPKnockServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPKnockServer.class);

    private final InetAddress bindAddress;
    private final List<Integer> portSequence;
    private final ConcurrentMap<UUID, KnockSession> sessions = new ConcurrentHashMap<>(); // TODO Auto-expiring & limits
    private final PacketConsumer packetConsumer;
    private final String openCommand, closeCommand;
    private final Timer openTimer;
    private final int openTimeout;

    private ExecutorService networkExecutorService = null;
    private UDPKnockPortListener primaryListener = null;
    private boolean serviceOpen = false;

    public UDPKnockServer(InetAddress bindAddress, List<Integer> portSequence,
                          String openCommand, String closeCommand, int openTimeout) throws SocketException {
        this.bindAddress = bindAddress;
        if (portSequence.contains(null))
            throw new IllegalArgumentException("Port sequence cannot contain null elements");

        this.portSequence = Collections.unmodifiableList(portSequence);
        this.packetConsumer = new PacketConsumer(this);
        this.openCommand = openCommand;
        this.closeCommand = closeCommand;
        this.openTimer = new Timer();
        this.openTimeout = openTimeout;
        bindPorts();
    }

    public boolean isBound() {
        return networkExecutorService != null;
    }

    public void bindPorts() throws SocketException {

        if (isBound())
            throw new IllegalStateException("Already bound");

        List<Integer> distinctPorts = portSequence.stream().distinct().collect(Collectors.toList());

        int threadCount = distinctPorts.size() + 1;

        LOGGER.debug("Network thread count: " + threadCount);

        networkExecutorService = Executors.newFixedThreadPool(threadCount);

        for (int port : distinctPorts) {

            InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, port);

            if (primaryListener == null) {

                primaryListener = new UDPKnockPortListener(packetConsumer, socketAddress);
                networkExecutorService.execute(primaryListener);

            } else {

                networkExecutorService.execute(new UDPKnockPortListener(packetConsumer, socketAddress));

            }

            LOGGER.info("Bound on " + socketAddress);

        }

        networkExecutorService.submit(packetConsumer);

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

    public void sendDatagramPacket(DatagramPacket packet) throws IOException {
        if (isBound()) {
            primaryListener.sendDatagramPacket(packet);
        }
    }

    public synchronized void openTimedService() {
        if (serviceOpen)
            return;

        try {

            LOGGER.debug("Opening timed service!");

            Runtime.getRuntime().exec(openCommand.split(Pattern.quote(" ")));

            serviceOpen = true;

            openTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    closeService();
                }
            }, TimeUnit.SECONDS.toMillis(openTimeout));

        } catch (IOException e) {
            LOGGER.warn("Failed to execute the open command", e);
        }
    }

    private void closeService() {
        LOGGER.debug("Closing timed service!");
        try {
            Runtime.getRuntime().exec(closeCommand.split(Pattern.quote(" ")));
        } catch (IOException e) {
            LOGGER.warn("Failed to execute the close command", e);
        }
        serviceOpen = false;
    }

}

