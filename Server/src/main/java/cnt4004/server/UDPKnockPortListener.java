package cnt4004.server;

import cnt4004.protocol.KnockPacket;
import cnt4004.protocol.NoncePacket;
import cnt4004.protocol.Packet;
import cnt4004.protocol.ProtocolMap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import static cnt4004.protocol.ProtocolMap.MAX_BUFFER;

public class UDPKnockPortListener implements Runnable {

    private DatagramSocket socket;
    private final PacketHandler packetHandler;

    public UDPKnockPortListener(PacketHandler packetHandler, InetSocketAddress socketAddress) throws SocketException {
        this.socket = new DatagramSocket(socketAddress);
        this.socket.setReceiveBufferSize(MAX_BUFFER);
        this.packetHandler = packetHandler;
    }

    @Override
    public void run() {

        byte[] buffer = new byte[MAX_BUFFER];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (socket.isBound() && !socket.isClosed()) {

            try {

                socket.receive(packet);

                Packet packetWrapper = ProtocolMap.decodePayload(packet.getData());

                if (packetWrapper != null) {

                    switch (packetWrapper.getID()) {
                        case 0:
                            packetHandler.receivedKnockPacket(this, packet, (KnockPacket) packetWrapper);
                            break;
                        case 1:
                            packetHandler.receivedNoncePacket(this, packet, (NoncePacket) packetWrapper);
                            break;
                        default:
                            System.out.println("Got unknown packet ID: " + packetWrapper.getID());
                            break;
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public int getPortBound() {
        return socket.getLocalPort();
    }

    public void sendDatagramPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

}
