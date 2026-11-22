package deadlock;

import java.util.Objects;

/**
 * One edge in a waits-for graph: waiter is blocked until holder finishes.
 *
 * On a real node this edge comes from the local lock table: a transaction
 * waiting for a lock row held by another transaction. The resource label is
 * only there so the video can show which row the wait is on.
 */
public final class WaitEdge {

    private final TransactionId waiter;
    private final TransactionId holder;
    private final String resource;

    public WaitEdge(TransactionId waiter, TransactionId holder, String resource) {
        this.waiter = Objects.requireNonNull(waiter);
        this.holder = Objects.requireNonNull(holder);
        this.resource = Objects.requireNonNull(resource);
    }

    public TransactionId waiter() {
        return waiter;
    }

    public TransactionId holder() {
        return holder;
    }

    public String resource() {
        return resource;
    }

    @Override
    public String toString() {
        return waiter + " waits for " + holder + " on '" + resource + "'";
    }
}
