package cnt4004.server;

import cnt4004.protocol.Packet;
import cnt4004.protocol.ProtocolMap;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

import static cnt4004.protocol.ProtocolMap.MAGIC;
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

                //TODO Closing stream?
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));

                byte[] receivedMagic = new byte[MAGIC.length];
                int read = in.read(receivedMagic, 0, MAGIC.length);

                if (read != MAGIC.length || !Arrays.equals(receivedMagic, MAGIC)) {
                    continue;
                }

                byte packetID = in.readByte(); // Packet ID (byte)

                Packet packetWrapper = ProtocolMap.createFromID(packetID);

                if (packetWrapper != null) {

                    packetWrapper.read(in);

                    System.out.println("Read packet ID " + packetID);

                } else {

                    throw new IOException("Unknown packet ID: " + packetID);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
