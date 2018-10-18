package cnt4004.client;

import cnt4004.protocol.NoncePacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cnt4004.protocol.ProtocolMap.MAGIC;
import static cnt4004.protocol.ProtocolMap.MAX_BUFFER;

public class UDPPortKnocker {

    public static void main(String[] args) throws Exception {

        // Example: java -jar PortKnock.jar 127.0.0.1 sharedSecret 4000 5000 6000

        if (args.length == 0) {
            System.out.println("Error: Missing HostName / IP Address in program arguments");
            return;
        }

        if (args.length == 1) {
            System.out.println("Error: Missing shared secret in program arguments");
            return;
        }

        if (args.length == 2) {
            System.out.println("Error: Missing port sequence in program arguments");
            return;
        }

        InetAddress serverAddress = InetAddress.getByName(args[0]);

        String sharedSecret = args[1];

        List<Integer> portSequence = IntStream.range(2, args.length)
                .mapToObj(i -> Integer.parseInt(args[i]))
                .collect(Collectors.toList());

        DatagramSocket socket = new DatagramSocket();

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outputBuffer);

        NoncePacket noncePacket = new NoncePacket(null);

        out.write(MAGIC);
        out.writeByte(noncePacket.getID());
        noncePacket.write(out);

        out.close();

        byte[] buffer = outputBuffer.toByteArray();

        if (buffer.length > MAX_BUFFER) {
            System.out.println("Buffer is too large");
            return;
        }

        socket.send(new DatagramPacket(buffer, buffer.length, serverAddress, portSequence.get(0)));

        System.out.println("Sent");

    }

}
