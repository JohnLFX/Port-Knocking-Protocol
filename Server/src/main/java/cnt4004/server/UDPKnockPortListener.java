package cnt4004.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

public class UDPKnockPortListener implements Runnable {

    private static final byte[] MAGIC = new byte[]{
            '7', 'S', 'z', 'C', 'L', 'C', 'g', 'c'
    };

    private static final int MAX_BUFFER = 1000; // TODO Determine buffer

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

                DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));

                byte[] receivedMagic = new byte[MAGIC.length];
                int read = in.read(receivedMagic, 0, MAGIC.length);

                if (read != MAGIC.length || !Arrays.equals(receivedMagic, MAGIC)) {
                    in.close();
                    continue;
                }

                int packetID = in.read(); // Packet ID (byte)

                switch (packetID) {

                    case 0:
                        System.out.println("Knock packet");
                        break;
                    case 1:
                        System.out.println("Nonce packet");
                        break;
                    default:
                        break;
                }

                in.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
