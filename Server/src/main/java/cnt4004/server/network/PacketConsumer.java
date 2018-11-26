package cnt4004.server.network;

import cnt4004.protocol.AuthenticatedPacket;
import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.Packet;
import cnt4004.protocol.TrustedClient;
import cnt4004.server.KnockServer;
import cnt4004.server.KnockSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketConsumer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketConsumer.class);

    /**
     * KnockServer instance
     */
    private final KnockServer knockServer;

    /**
     * Queue of pending packets to be processed
     */
    // Capacity is 1,000 packets can be queued at most. This does not mean that memory
    // has been allocated for 1,000 packets, but can dynamically grow to 1,000 packets.
    private final BlockingQueue<QueuedPacket> queue = new LinkedBlockingQueue<>(1000);

    public PacketConsumer(KnockServer knockServer) {
        this.knockServer = knockServer;
    }

    /**
     * Offers a packet to the queue. This method may drop the packet if the queue is full.
     *
     * @param packet        The packet to add
     * @param clientAddress The source address of the remote host (source fields in datagram)
     * @param localAddress  The local address the packet was received on
     */
    void queuePacket(Packet packet, SocketAddress clientAddress, SocketAddress localAddress) {
        queue.offer(new QueuedPacket(packet, clientAddress, localAddress));
    }

    @Override
    public void run() {

        LOGGER.info("Packet consumer started");

        try {

            //noinspection InfiniteLoopStatement
            while (true) {

                // take() is a blocking method that waits for a packet in the queue
                QueuedPacket queuedPacket = queue.take();

                if (queuedPacket.packet.getID() == 0) {
                    receivedKnockPacket((KnockPacket) queuedPacket.packet, queuedPacket.clientAddress, queuedPacket.localAddress);
                } else {
                    LOGGER.debug("Unknown packet ID: " + queuedPacket.packet.getID());
                }

            }

        } catch (InterruptedException e) {
            LOGGER.info("Packet consumer thread interrupted");
        }

    }

    /**
     * Processes a Knock Packet
     *
     * @param packet        The Knock packet
     * @param clientAddress The source address of the packet (datagram source address)
     * @param localAddress  The local address for which the packet was received on
     */
    private void receivedKnockPacket(KnockPacket packet, SocketAddress clientAddress, SocketAddress localAddress) {

        // Prevent the packet from allocating unnecessary memory
        if (packet.getMaxSequence() > (knockServer.getPortCount() - 1)) {
            LOGGER.debug("Discarding knock packet due to the maximum sequence number (" + packet.getMaxSequence()
                    + ") being greater than or equal to the port count (" + knockServer.getPortCount() + ")");
            return;
        }

        KnockSession session = knockServer.getSession(packet.getClientIdentifier());

        int knockedPort = ((InetSocketAddress) localAddress).getPort();

        LOGGER.debug("Got a knock from " + clientAddress + " on local port " + knockedPort
                + " | Sequence: " + packet.getSequence()
                + " | Max Sequence: " + packet.getMaxSequence());

        session.addKnockPacket(packet, knockedPort);

        if (session.sequenceComplete()) {

            List<Integer> receivedSequence = session.getCurrentKnockSequence();

            LOGGER.debug("Final received knock sequence: " + receivedSequence);

            if (receivedSequence.equals(knockServer.getPorts())) {

                LOGGER.debug("Correct knock sequence!");

                Set<TrustedClient> clientSet = knockServer.getTrustedClients();

                TrustedClient client = clientSet.stream()
                        .filter(c -> c.getIdentifier().equals(packet.getClientIdentifier()))
                        .findFirst().orElse(null);

                long largestNonce = session.getKnockSequence().keySet().stream()
                        .mapToLong(AuthenticatedPacket::getNonce)
                        .filter(p -> p >= 0).max().orElse(0);

                if (client != null && client.getLargestNonceReceived() < largestNonce) {

                    LOGGER.debug("Updating nonce for " + client.getIdentifier() + " to " + largestNonce);
                    client.setLargestNonceReceived(largestNonce);
                    try {
                        TrustedClient.saveTrustedClients(clientSet);
                    } catch (IOException e) {
                        LOGGER.error("Saving nonce", e);
                    }

                }

                knockServer.openTimedService();

            } else {

                LOGGER.debug("Incorrect knock sequence");

            }

            knockServer.removeSession(packet.getClientIdentifier());

        }

    }
}

/**
 * Represents a queued packet in the queue
 */
class QueuedPacket {
    final Packet packet;
    final SocketAddress clientAddress;
    final SocketAddress localAddress;

    QueuedPacket(Packet packet, SocketAddress clientAddress, SocketAddress localAddress) {
        this.packet = packet;
        this.clientAddress = clientAddress;
        this.localAddress = localAddress;
    }

}
