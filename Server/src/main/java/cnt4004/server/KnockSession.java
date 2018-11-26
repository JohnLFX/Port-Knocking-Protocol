package cnt4004.server;

import cnt4004.protocol.KnockPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class represents a KnockSession object.
 * A knock session contains the current sequence of Knock packets received by a client.
 */
public class KnockSession {

    /**
     * Sorted Map of Knock packets. The map is sorted based on the sequence IDs in the Knock packets.
     */
    private final SortedMap<KnockPacket, Integer> knockSequence;

    KnockSession() {
        this.knockSequence = new TreeMap<>();
    }

    /**
     * Adds a Knock packet, along with the port it has been received on, to the array.
     * This method replaces old entries automatically.
     *
     * @param packet      The KnockPacket received
     * @param knockedPort The port the packet was received on
     */
    public void addKnockPacket(KnockPacket packet, int knockedPort) {
        knockSequence.put(packet, knockedPort);
    }

    /**
     * Determines if the sequence is complete.
     * A complete knock sequence means that all sequence IDs have been received.
     * For example, if the maximum sequence ID is 2, then the sequence will be considered complete
     * when there are three packets in the array with sequence IDs 0, 1, and 2.
     * @return True if the knock sequence is complete, false otherwise
     */
    public boolean sequenceComplete() {
        if (knockSequence.isEmpty())
            return false;

        short prevSequence = 0;
        boolean reachedMax = false;
        for (KnockPacket packet : knockSequence.keySet()) {
            if (packet.getSequence() != prevSequence++) {
                return false;
            }
            if (packet.getSequence() == packet.getMaxSequence()) {
                reachedMax = true;
            }
        }
        return reachedMax;
    }

    /**
     * Returns an ordered list (based on sequence IDs) of the current received knock sequence
     * @return The current knock sequence
     */
    public List<Integer> getCurrentKnockSequence() {
        return new ArrayList<>(knockSequence.values());
    }

    /**
     * Gets the current knock sequence map
     *
     * @return The map
     */
    public SortedMap<KnockPacket, Integer> getKnockSequence() {
        return knockSequence;
    }

}
