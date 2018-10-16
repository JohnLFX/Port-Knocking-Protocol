package cnt4004.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class Start {

    public static void main(String[] args) throws IOException {

        Properties config = new Properties();
        Path configFile = Paths.get("udpKnock.properties");

        if (Files.notExists(configFile)) {

            try (InputStream in = Start.class.getResourceAsStream("/settings.properties")) {
                System.out.println("Writing settings.properties to " + configFile);
                Files.copy(in, configFile);
            }

        }

        try (InputStream in = Files.newInputStream(configFile, StandardOpenOption.READ)) {
            System.out.println("Loading settings from " + configFile);
            config.load(in);
        }

        DatagramSocket receiveSocket = new DatagramSocket(Integer.parseInt(config.getProperty("port", "8392")));

        System.out.println("UDP Socket bound on " + receiveSocket.getLocalSocketAddress());

        new Thread(new UDPListener(receiveSocket, new UDPKnockCallback() {
            @Override
            public void onKnock() {
                System.out.println("I GOT KNOCKED BOI");
            }
        })).start();

    }

}
