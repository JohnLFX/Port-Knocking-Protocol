package cnt4004.server;

import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import cnt4004.protocol.Utils;
import cnt4004.server.network.KnockPortListener;
import cnt4004.server.network.PacketConsumer;
import cnt4004.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class KnockServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnockServer.class);

    private final InetAddress bindAddress;
    private final ConcurrentMap<String, KnockSession> sessions = new ConcurrentHashMap<>();
    private final PacketConsumer packetConsumer;
    private final int serviceTimeout;
    private final String portSecret;
    private final int portCount;

    private Timer serviceTimer;
    private int serviceCounter;

    private ExecutorService networkExecutorService = null;
    private final List<KnockPortListener> portListeners = new ArrayList<>();
    private final AtomicBoolean serviceOpen = new AtomicBoolean(false);

    /**
     * Creates a new KnockServer instance
     *
     * @param bindAddress    The address to bind on
     * @param trustedClients The clients the server will trust
     * @param portSecret     The port secret
     * @param portCount      The amount of ports to use
     * @param openTimeout    The value to increase the timer with for every successful knock session
     */
    public KnockServer(InetAddress bindAddress,
                       Set<TrustedClient> trustedClients, String portSecret, int portCount,
                       int openTimeout) {

        this.bindAddress = bindAddress;
        this.packetConsumer = new PacketConsumer(this);
        this.serviceTimeout = openTimeout;
        this.portSecret = portSecret;
        this.portCount = portCount;
        bindPorts();

        LOGGER.debug("Initializing the service");
        ServiceManager.getInstance().initializeService();

        LOGGER.debug("Initializing the protocol module");
        ProtocolMap.setTrustedClients(trustedClients);

    }

    /**
     * Determines if the ports are bound
     *
     * @return If the ports are bound
     */
    private boolean isBound() {
        return networkExecutorService != null && !portListeners.isEmpty();
    }

    /**
     * Binds the ports
     */
    private void bindPorts() {

        if (isBound()) {

            LOGGER.debug("Clearing existing port bindings, shutting down network executor service");

            networkExecutorService.shutdownNow();
            portListeners.clear();

        }

        // One thread for each port and one thread for the packet consumer
        int threadCount = portCount + 1;

        LOGGER.debug("Network thread count: " + threadCount);

        // Create a new thread pool and spawn the threads
        networkExecutorService = Executors.newFixedThreadPool(threadCount);
        networkExecutorService.submit(packetConsumer);

        for (int offset = 0; offset < portCount; offset++) {

            KnockPortListener portListener = new KnockPortListener(packetConsumer, bindAddress, portSecret, portCount, offset);

            portListeners.add(portListener);
            networkExecutorService.execute(portListener);

        }

    }

    /**
     * Shuts down the Knock server. A knock server shutdown clears all sessions,
     * terminates all threads, shuts down the service, and unbinds the ports.
     */
    void shutdown() {
        sessions.clear();
        networkExecutorService.shutdownNow();
        portListeners.clear();
        closeService();
        ServiceManager.getInstance().shutdownService();
    }

    /**
     * Returns the KnockSession associated with an identifier
     *
     * @param identifier The identifier
     * @return The KnockSession associated with {@code identifier}
     */
    public KnockSession getSession(String identifier) {
        return sessions.computeIfAbsent(identifier, k -> new KnockSession());
    }

    /**
     * Removes a KnockSession associated with an identifier from memory
     * @param identifier The identifier
     */
    public void removeSession(String identifier) {
        sessions.remove(identifier);
    }

    /**
     * Returns the correct ports, in the right order, for the knock sequence
     * @return Ordered ports of the correct sequence
     */
    public List<Integer> getPorts() {
        return Utils.getPorts(portSecret, portCount);
    }

    /**
     * Returns the amount of ports the server uses for the knock sequence
     * @return The number of ports the server uses
     */
    public int getPortCount() {
        return portCount;
    }

    /**
     * Opens the timed service. If the service is already open,
     * then the counter increases by {@link KnockServer#serviceTimeout}
     */
    public void openTimedService() {

        serviceCounter += serviceTimeout;
        LOGGER.debug("Opening timed service! (Counter = " + serviceCounter + ")");

        // If the service is not open, start the timer and open the service
        if (serviceOpen.compareAndSet(false, true)) {

            ServiceManager.getInstance().openService();

            serviceTimer = new Timer();
            serviceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (serviceCounter-- <= 0) {
                        serviceTimer.cancel();
                        closeService();
                    }
                }
            }, 0, TimeUnit.SECONDS.toMillis(1)); // Every second

        }

    }

    /**
     * Closes the service and resets {@link KnockServer#serviceCounter} to 0
     */
    private void closeService() {
        if (serviceOpen.compareAndSet(true, false)) {
            LOGGER.debug("Closing timed service!");
            ServiceManager.getInstance().closeService();
            serviceCounter = 0;
        }
    }

}

