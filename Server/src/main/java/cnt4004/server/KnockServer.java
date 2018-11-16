package cnt4004.server;

import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.TrustedClient;
import cnt4004.protocol.Utils;
import cnt4004.server.network.PacketConsumer;
import cnt4004.server.network.UDPKnockPortListener;
import cnt4004.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.*;
import java.util.concurrent.*;

public class KnockServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnockServer.class);

    private final InetAddress bindAddress;
    private final ConcurrentMap<String, KnockSession> sessions = new ConcurrentHashMap<>(); // TODO Auto-expiring & limits
    private final PacketConsumer packetConsumer;
    private final int serviceTimeout;
    private final String portSecret;
    private final int portCount;

    private Timer serviceTimer;
    private int serviceCounter;

    private ExecutorService networkExecutorService = null;
    private final List<UDPKnockPortListener> portListeners = new ArrayList<>();
    private boolean serviceOpen = false;

    public KnockServer(InetAddress bindAddress,
                       Set<TrustedClient> trustedClients, PrivateKey serverKey, String portSecret, int portCount,
                       int openTimeout) throws SocketException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        this.bindAddress = bindAddress;
        this.packetConsumer = new PacketConsumer(this);
        this.serviceTimeout = openTimeout;
        this.portSecret = portSecret;
        this.portCount = portCount;
        bindPorts();

        LOGGER.debug("Initializing the service");
        ServiceManager.getInstance().initializeService();

        LOGGER.debug("Initializing the protocol module");
        ProtocolMap.initializeSignature(trustedClients, serverKey);

    }

    public boolean isBound() {
        return networkExecutorService != null && !portListeners.isEmpty();
    }

    public void bindPorts() throws SocketException {

        if (isBound()) {

            LOGGER.debug("Clearing existing port bindings, shutting down network executor service");

            networkExecutorService.shutdownNow();
            portListeners.clear();

        }

        int threadCount = portCount + 1;

        LOGGER.debug("Network thread count: " + threadCount);

        networkExecutorService = Executors.newFixedThreadPool(threadCount);
        networkExecutorService.submit(packetConsumer);

        for (int offset = 0; offset < portCount; offset++) {

            UDPKnockPortListener portListener = new UDPKnockPortListener(packetConsumer, bindAddress, portSecret, portCount, offset);

            if (portListeners.add(portListener)) {

                networkExecutorService.execute(portListener);

            } else {

                throw new SocketException("Failed to register port listener thread");

            }

        }

    }

    void shutdown() {
        sessions.clear();
        networkExecutorService.shutdownNow();
        portListeners.clear();
        closeService();
        ServiceManager.getInstance().shutdownService();
    }

    public KnockSession getSession(String identifier) {
        return sessions.computeIfAbsent(identifier, k -> new KnockSession());
    }

    public void removeSession(String identifier) {
        sessions.remove(identifier);
    }

    public List<Integer> getPorts() {
        return Utils.getPorts(portSecret, portCount);
    }

    public synchronized void openTimedService() {

        serviceCounter += serviceTimeout;
        LOGGER.debug("Opening timed service! (Counter = " + serviceCounter + ")");

        if (serviceOpen)
            return;

        serviceOpen = true;
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
        }, 0, TimeUnit.SECONDS.toMillis(1));

    }

    private void closeService() {
        LOGGER.debug("Closing timed service!");
        ServiceManager.getInstance().closeService();
        serviceCounter = 0;
        serviceOpen = false;
    }

}

