package deadlock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Chooses which transaction on a deadlock cycle gets cancelled.
 *
 * Mirrors Citus's policy: cancel the youngest transaction in the cycle. The
 * code comments in Citus give the reason: predictable results, and a young
 * transaction has usually done the least work, so it is the cheapest to
 * redo. Ties on the timestamp are broken by the full id ordering, so the
 * same graph always yields the same victim.
 *
 * Before cancelling, the victim must still map to a live backend process.
 * This is a "victim still exists" check, not a revalidation of the whole
 * cycle: the graph came from a stitched snapshot, and the cycle may have
 * partly resolved while it was being assembled.
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
