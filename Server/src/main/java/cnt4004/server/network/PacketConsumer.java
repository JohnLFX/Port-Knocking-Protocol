package cnt4004.server.network;

import cnt4004.protocol.*;
import cnt4004.server.KnockSession;
import cnt4004.server.UDPKnockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PacketConsumer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacketConsumer.class);

    private final UDPKnockServer knockServer;

    //TODO Priority queue for knock sequences in progress?
    private final BlockingQueue<QueuedPacket> queue = new ArrayBlockingQueue<>(100);

    public PacketConsumer(UDPKnockServer knockServer) {
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
                    case 1:
                        receivedNoncePacket((NoncePacket) queuedPacket.packet, queuedPacket.clientAddress);
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

    private void receivedNoncePacket(NoncePacket packet, SocketAddress clientAddress) {

        if (packet.getNonce() == null) {

            KnockSession session = knockServer.createSession();

            packet.setNonce(session.getNonce());

            try {

                byte[] payload = ProtocolMap.generatePayload(packet);

                knockServer.sendDatagramPacket(new DatagramPacket(payload, payload.length, clientAddress));

                LOGGER.debug("Sending a new nonce (" + packet.getNonce() + ") for knock session to " + clientAddress);

            } catch (IOException e) {
                LOGGER.debug("Failed to send Nonce", e);
                knockServer.removeSession(session);
            }

        }

    }

    private void receivedKnockPacket(KnockPacket packet, SocketAddress clientAddress, SocketAddress localAddress) {

        KnockSession session = knockServer.getSession(packet.getNonce());

        if (session != null) {

            int knockedPort = ((InetSocketAddress) localAddress).getPort();

            LOGGER.debug("Got a knock from " + clientAddress + " on local port " + knockedPort
                    + " | Sequence: " + packet.getSequence()
                    + " | Max Sequence: " + packet.getMaxSequence());

            session.addKnockPacket(packet, knockedPort);

            try {

                byte[] ackPayload = ProtocolMap.generatePayload(new AckPacket(packet.getNonce(), packet.getSequence()));
                knockServer.sendDatagramPacket(new DatagramPacket(ackPayload, ackPayload.length, clientAddress));

            } catch (IOException e) {
                LOGGER.error("Failed to send ACK response packet to "
                        + clientAddress + " for sequence ID " + packet.getSequence(), e);
                knockServer.removeSession(session);
                return;
            }

            if (session.sequenceComplete()) {

                List<Integer> receivedSequence = session.getCurrentKnockSequence();

                LOGGER.debug("Final received knock sequence: " + receivedSequence);

                if (receivedSequence.equals(knockServer.getPortSequence())) {

                    LOGGER.debug("Correct knock sequence!");
                    knockServer.openTimedService();

                } else {

                    LOGGER.debug("Incorrect knock sequence");

                }

                knockServer.removeSession(session);

            }

        } else {

            LOGGER.debug("Possible playback attack: Denied knock packet from already-used nonce: " + packet.getNonce());

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
