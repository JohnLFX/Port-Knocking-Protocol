package cnt4004.server;

import cnt4004.protocol.KnockPacket;

import java.util.*;

public class KnockSession {

    private final UUID nonce;
    private final SortedMap<KnockPacket, Integer> knockSequence;

    KnockSession() {
        this.nonce = UUID.randomUUID();
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
        return Collections.unmodifiableList(new ArrayList<>(knockSequence.values()));
    }

    public UUID getNonce() {
        return nonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnockSession session = (KnockSession) o;
        return Objects.equals(nonce, session.nonce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce);
    }

}
