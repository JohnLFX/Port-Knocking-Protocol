package cnt4004.server.network;

import cnt4004.protocol.Packet;
import cnt4004.protocol.ProtocolMap;
import cnt4004.protocol.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static cnt4004.protocol.ProtocolMap.MAX_BUFFER;

public class UDPKnockPortListener implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPKnockPortListener.class);

    private final InetAddress bindAddress;
    private final PacketConsumer packetConsumer;
    private final String portSecret;
    private final int portCount;
    private final int portGenOffset;
    private int prevTimeout;

    public UDPKnockPortListener(PacketConsumer packetConsumer, InetAddress bindAddress, String portSecret, int portCount, int portGenOffset) {
        this.bindAddress = bindAddress;
        this.packetConsumer = packetConsumer;
        this.portSecret = portSecret;
        this.portCount = portCount;
        this.portGenOffset = portGenOffset;
    }

    @Override
    public void run() {

        byte[] buffer = new byte[MAX_BUFFER];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        //TODO Test case of already bound ports
        while (true) {

            try {

                DatagramSocket socket = new DatagramSocket(Utils.getPorts(portSecret, portCount).get(portGenOffset), bindAddress);
                socket.setReceiveBufferSize(MAX_BUFFER);

                LOGGER.debug("Listening on " + socket.getLocalSocketAddress());

                int timeout = calculateTimeout();

                do {

                    try {

                        LOGGER.debug("Socket timeout: " + TimeUnit.MILLISECONDS.toSeconds(timeout) + " seconds");

                        prevTimeout = timeout;
                        socket.setSoTimeout(timeout);
                        socket.receive(packet);

                        Packet packetWrapper = ProtocolMap.decodePayload(packet.getData());

                        if (packetWrapper != null) {

                            packetConsumer.queuePacket(packetWrapper, packet.getSocketAddress(), socket.getLocalSocketAddress());

                        }

                    } catch (SocketTimeoutException e) {
                        /* Do nothing */
                    } catch (IOException e) {
                        LOGGER.debug("IO Exception", e);
                    }

                    timeout = calculateTimeout();

                } while (timeout < prevTimeout);

            } catch (SocketException e) {
                LOGGER.warn("Socket exception", e);
            }

        }

    }

    private static int calculateTimeout() {
        // Calculate time until the next minute
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + 1);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return (int) (c.getTimeInMillis() - System.currentTimeMillis());
    }

}
