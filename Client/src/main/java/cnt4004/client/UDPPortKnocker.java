package cnt4004.client;

import cnt4004.protocol.NoncePacket;
import cnt4004.protocol.ProtocolMap;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        sharedSecret = "the-secret";

        List<Integer> portSequence = IntStream.range(2, args.length)
                .mapToObj(i -> Integer.parseInt(args[i]))
                .collect(Collectors.toList());

        ProtocolMap.initializeHMAC(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));

        DatagramSocket socket = new DatagramSocket();

        byte[] payload = ProtocolMap.generatePayload(new NoncePacket(null));

        System.out.println(String.format("%040x", new BigInteger(1, payload)));

        socket.send(new DatagramPacket(payload, payload.length, serverAddress, portSequence.get(0)));

        System.out.println("Sent");

    }

}
