package cnt4004.server;

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

    public UDPKnockPortListener(InetSocketAddress socketAddress) throws SocketException {
        this.socket = new DatagramSocket(socketAddress);
        this.socket.setReceiveBufferSize(MAX_BUFFER);
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

                    System.out.println("Got packet " + packetWrapper.getID());

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
