package deadlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The global waits-for graph, assembled on the coordinator from every node's
 * local lock table. Mirrors Citus's BuildGlobalWaitGraph followed by
 * BuildAdjacencyListsForWaitGraph: collect local edges from all nodes, then
 * convert to adjacency lists for efficient search.
 *
 * Honest caveat, narrated in the episode: the edges are collected node by
 * node, so this graph is stitched together from lock tables read at slightly
 * different times. It is a good-enough snapshot, not a consistent cut.
 */
public final class GlobalWaitGraph {

    private final Map<TransactionId, List<TransactionId>> adjacency = new LinkedHashMap<>();
    private final int edgeCount;

    private GlobalWaitGraph(Map<TransactionId, List<TransactionId>> adjacency, int edgeCount) {
        this.adjacency.putAll(adjacency);
        this.edgeCount = edgeCount;
    }

    public static GlobalWaitGraph collectFrom(List<Node> nodes) {
        Map<TransactionId, List<TransactionId>> adjacency = new LinkedHashMap<>();
        int edgeCount = 0;
        for (Node node : nodes) {
            for (WaitEdge edge : node.localWaitEdges()) {
                adjacency.computeIfAbsent(edge.waiter(), k -> new ArrayList<>()).add(edge.holder());
                adjacency.computeIfAbsent(edge.holder(), k -> new ArrayList<>());
                edgeCount++;
            }
        }
        return new GlobalWaitGraph(adjacency, edgeCount);
    }

    /** Outgoing edges: the transactions this one is waiting for. */
    public List<TransactionId> waitsFor(TransactionId transaction) {
        return adjacency.getOrDefault(transaction, List.of());
    }

    /** Every transaction that waits or is waited upon. */
    public List<TransactionId> transactions() {
        return new ArrayList<>(adjacency.keySet());
    }

    /**
     * Search-depth guard based on Citus's edgeCount + 1 path-array allocation.
     * Visited-node handling bounds expansion in this DFS. This is not a claim
     * that every arbitrary walk, or the graph's total vertex count across
     * disconnected components, is bounded by edgeCount + 1.
     */
    public int maxSearchDepth() {
        return edgeCount + 1;
    }
}
