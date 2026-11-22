package deadlock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pick the youngest cycle participant in the supplied eligible-live set.
 * This set stands in for Citus's local backend association checks; callers
 * modelling one detector should include only locally eligible initiators.
 * Citus does not promise the globally youngest victim. Our total ID ordering
 * on equal timestamps is an explicit addition to its timestamp comparison.
 * Presence in this set is not revalidation of the collected cycle.
 */
public final class VictimPicker {

    private final Set<TransactionId> liveBackends;

    public VictimPicker(Set<TransactionId> liveBackends) {
        this.liveBackends = liveBackends;
    }

    /**
     * The youngest transaction on the cycle that is still backed by a live
     * process, or empty when nobody on the cycle can be cancelled any more.
     */
    public Optional<TransactionId> pickVictim(List<TransactionId> cycle) {
        TransactionId victim = null;
        for (TransactionId transaction : cycle) {
            if (!liveBackends.contains(transaction)) {
                continue;
            }
            // compareTo orders oldest first; the youngest is the greatest.
            if (victim == null || transaction.compareTo(victim) > 0) {
                victim = transaction;
            }
        }
        return Optional.ofNullable(victim);
    }
}
