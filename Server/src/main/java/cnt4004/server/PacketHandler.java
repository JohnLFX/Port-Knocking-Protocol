package cnt4004.server;

import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.NoncePacket;
import cnt4004.protocol.ProtocolMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.List;

public class PacketHandler {

    private final UDPKnockServer knockServer;

    public PacketHandler(UDPKnockServer knockServer) {
        this.knockServer = knockServer;
    }

    public void receivedNoncePacket(UDPKnockPortListener listener, DatagramPacket datagramPacket, NoncePacket packet) throws IOException {

        KnockSession session = knockServer.getSession(packet.getNonce());

        if (session == null) {

            session = knockServer.createSession();

            packet.setNonce(session.getNonce());

            byte[] payload = ProtocolMap.generatePayload(packet);

            listener.sendDatagramPacket(new DatagramPacket(payload, payload.length, datagramPacket.getSocketAddress()));

            System.out.println("Sending a new nonce for knock session to " + datagramPacket.getSocketAddress());

        } else {

            System.out.println("Received a duplicate nonce packet!");

        }

    }

    public void receivedKnockPacket(UDPKnockPortListener listener, DatagramPacket datagramPacket, KnockPacket packet) {

        KnockSession session = knockServer.getSession(packet.getNonce());

        if (session != null) {

            int knockedPort = listener.getPortBound();

            System.out.println("Got a knock from " + datagramPacket.getSocketAddress() + " on local port " + knockedPort);
            System.out.println("Sequence: " + packet.getSequence());
            System.out.println("Max Sequence: " + packet.getMaxSequence());

            // TODO ACK Packets

            session.addKnockPacket(packet, knockedPort);

            if (session.sequenceComplete()) {

                List<Integer> receivedSequence = session.getCurrentKnockSequence();

                System.out.println("Final received knock sequence: " + receivedSequence);

                if (receivedSequence.equals(knockServer.getPortSequence())) {

                    System.out.println("Correct knock sequence!");

                } else {

                    System.out.println("Incorrect knock sequence");

                }

                knockServer.removeSession(session);

            }

        } else {

            System.out.println("Possible playback attack: Denied knock packet from already-used nonce: " + packet.getNonce());

        }

    }

}
