package deadlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A teaching model of one worker's local wait edges. The two-transfer
 * example splits its cycle across workers, so neither fragment shows that
 * cycle. This is not a full database lock table or lock manager.
 */
public final class Node {

    private final int nodeId;
    private final List<WaitEdge> localWaits = new ArrayList<>();

    public Node(int nodeId) {
        this.nodeId = nodeId;
    }

    public int nodeId() {
        return nodeId;
    }

    /** Record that waiter is blocked on a lock row held by holder on this node. */
    public Node addWait(TransactionId waiter, TransactionId holder, String resource) {
        localWaits.add(new WaitEdge(waiter, holder, resource));
        return this;
    }

    /** The local lock table's current waits-for edges, as the collector sees them. */
    public List<WaitEdge> localWaitEdges() {
        return Collections.unmodifiableList(localWaits);
    }
}
