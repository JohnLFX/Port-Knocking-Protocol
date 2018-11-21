package cnt4004.server.network;

import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.Packet;
import cnt4004.server.KnockServer;
import cnt4004.server.KnockSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PacketConsumer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketConsumer.class);

    private final KnockServer knockServer;

    //TODO Priority queue for knock sequences in progress?
    private final BlockingQueue<QueuedPacket> queue = new ArrayBlockingQueue<>(100);

    public PacketConsumer(KnockServer knockServer) {
        this.knockServer = knockServer;
    }

    void queuePacket(Packet packet, SocketAddress clientAddress, SocketAddress localAddress) {
        queue.offer(new QueuedPacket(packet, clientAddress, localAddress));
    }

    @Override
    public void run() {

        LOGGER.info("Packet consumer started");

        try {

            //noinspection InfiniteLoopStatement
            while (true) {

                QueuedPacket queuedPacket = queue.take();

                switch (queuedPacket.packet.getID()) {

                    case 0:
                        receivedKnockPacket((KnockPacket) queuedPacket.packet, queuedPacket.clientAddress, queuedPacket.localAddress);
                        break;
                    default:
                        LOGGER.debug("Unknown packet ID: " + queuedPacket.packet.getID());
                        break;
                }

            }

        } catch (InterruptedException e) {
            LOGGER.info("Packet consumer thread interrupted");
        }

    }

    private void receivedKnockPacket(KnockPacket packet, SocketAddress clientAddress, SocketAddress localAddress) {

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
                knockServer.openTimedService();

            } else {

                LOGGER.debug("Incorrect knock sequence");

            }

            knockServer.removeSession(packet.getClientIdentifier());

        }

    }
}

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
