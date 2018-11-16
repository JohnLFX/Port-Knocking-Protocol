package cnt4004.server;

import cnt4004.protocol.KnockPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class KnockSession {

    private final SortedMap<KnockPacket, Integer> knockSequence;

    KnockSession() {
        this.knockSequence = new TreeMap<>();
    }

    public void addKnockPacket(KnockPacket packet, int knockedPort) {
        knockSequence.put(packet, knockedPort);
    }

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

    public List<Integer> getCurrentKnockSequence() {
        return new ArrayList<>(knockSequence.values());
    }

}
