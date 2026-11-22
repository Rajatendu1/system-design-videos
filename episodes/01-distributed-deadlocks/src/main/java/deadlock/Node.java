package deadlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One database node (a shard server) with its own local lock table.
 *
 * A node only ever sees the waits that involve locks living on it. Neither
 * the cross-node cycle nor the other node's waits are visible here, which is
 * exactly why a purely local detector can never fire on a distributed
 * deadlock.
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
