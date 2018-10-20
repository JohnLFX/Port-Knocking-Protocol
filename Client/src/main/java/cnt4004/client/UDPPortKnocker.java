package cnt4004.client;

import cnt4004.protocol.*;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UDPPortKnocker {

    public static void main(String[] args) throws Exception {

        // Example: java -jar PortKnock.jar 127.0.0.1 sharedSecret 4000 5000 6000

        if (args.length == 0) {
            System.out.println("Error: Missing Host Name / IP Address in program arguments");
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

        ProtocolMap.initializeHMAC(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(3));

        System.out.println("Requesting Nonce...");

        Packet response = sendResponsePacket(new NoncePacket(null), socket,
                new InetSocketAddress(serverAddress, portSequence.get(0)));

        if (response == null) {
            System.err.println("Unknown packet received");
            return;
        }

        if (response.getID() != 1) {
            System.err.println("Received unexpected response packet for Nonce Request (ID " + response.getID() + ")");
            return;
        }

        NoncePacket noncePacket = (NoncePacket) response;

        System.out.println("Using Nonce " + noncePacket.getNonce());

        List<Object[]> payloads = new ArrayList<>(); // Ugly hack

        for (int i = 0; i < portSequence.size(); i++) {

            int port = portSequence.get(i);

            payloads.add(
                    new Object[]{
                            new KnockPacket(noncePacket.getNonce(), (short) i, (short) (portSequence.size() - 1)),
                            port
                    }
            );

        }

        Collections.shuffle(payloads);

        for (Object[] data : payloads) {

            KnockPacket knockPacket = (KnockPacket) data[0];

            do {

                System.out.println("Sending knock packet: " + knockPacket.getSequence());

                response = sendResponsePacket(knockPacket, socket, new InetSocketAddress(serverAddress, (int) data[1]));

            }
            while (!((response instanceof AckPacket) && ((AckPacket) response).getSequence() == knockPacket.getSequence()));

        }

    }

    private static Packet sendResponsePacket(Packet packet, DatagramSocket socket, SocketAddress severAddress) throws IOException {

        byte[] payload = ProtocolMap.generatePayload(packet);
        socket.send(new DatagramPacket(payload, payload.length, severAddress));

        payload = new byte[ProtocolMap.MAX_BUFFER];
        DatagramPacket response = new DatagramPacket(payload, payload.length);

        try {

            socket.receive(response);

        } catch (SocketTimeoutException e) {
            System.out.println("Timed out for packet ID " + packet.getID());
            return null;
        }

        return ProtocolMap.decodePayload(payload);

    }

}
