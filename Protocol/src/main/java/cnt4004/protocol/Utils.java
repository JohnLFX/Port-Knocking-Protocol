package cnt4004.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Utils {

    /**
     * The maximum port number that is possible to generate, inclusive.
     * 65535 is the maximum value permitted by UDP.
     */
    private static final int MAX_PORT_NUMBER = 65535;

    /**
     * The minimum port number that is possible to generate, inclusive.
     * Ports 0-1024 are usually reserved in most operating systems.
     */
    private static final int MIN_PORT_NUMBER = 1025;

    private Utils() {
    }

    /**
     * The md5 hash algorithm
     */
    private static final MessageDigest HASH_ALGORITHM;

    static {
        try {
            HASH_ALGORITHM = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // Every machine should have the MD5 algorithm...
            // But if not, crash the program
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates {@code portCount} ports using the current unix time in minutes and a port secret.
     *
     * @param portSecret The port secret
     * @param portCount  The amount of ports to generate
     * @return A list of {@code portCount} distinct ports
     */
    public synchronized static List<Integer> getPorts(String portSecret, int portCount) {

        if (portSecret == null)
            throw new IllegalArgumentException("Port secret cannot be null");

        if (portCount < 0)
            throw new IllegalArgumentException("Port count must not be negative");

        long unixMinuteTime = Instant.now().getEpochSecond() / 60;

        byte[] md5 = HASH_ALGORITHM.digest((portSecret + unixMinuteTime).getBytes(StandardCharsets.UTF_8));

        long a = md5[0] * 256 * md5[1] + 256 * 256 * md5[2] + 256 * 256 * 256 * md5[3];
        long b = md5[4] * 256 * md5[5] + 256 * 256 * md5[6] + 256 * 256 * 256 * md5[7];
        long result = a ^ b;

        Random random = new Random(result);

        List<Integer> ports = new ArrayList<>();

        while (ports.size() != portCount) {

            int port = random.nextInt(MAX_PORT_NUMBER + 1);

            if (validPort(port) && !ports.contains(port))
                ports.add(port);

        }

        return ports;

    }

    /**
     * Determines if {@code port} is within {@link Utils#MIN_PORT_NUMBER} and {@link Utils#MAX_PORT_NUMBER}
     * @param port The port to check
     * @return True if it is valid, false otherwise
     */
    private static boolean validPort(int port) {
        return port > MIN_PORT_NUMBER && port < MAX_PORT_NUMBER;
    }

}
