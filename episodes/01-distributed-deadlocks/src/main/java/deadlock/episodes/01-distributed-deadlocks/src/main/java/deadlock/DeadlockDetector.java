package deadlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The depth-first search at the heart of the episode.
 *
 * Mirrors Citus's CheckDeadlockForTransactionNode: an iterative DFS with an
 * explicit stack (never recursion, so a deep graph cannot blow the call
 * stack), starting from one transaction and asking a single question:
 * can I reach myself again?
 *
 * A path that loops back to the starting transaction is a deadlock cycle.
 * The returned path lists the transactions on the cycle in order.
 */
public final class DeadlockDetector {

    private final GlobalWaitGraph graph;

    public DeadlockDetector(GlobalWaitGraph graph) {
        this.graph = graph;
    }

    /**
     * Depth-first search for a path from start back to start.
     *
     * @return the cycle including start at both ends, or empty if start
     *         is not part of any deadlock
     */
    public Optional<List<TransactionId>> findCycle(TransactionId start) {
        int maxDepth = graph.maxSearchDepth();

        // The DFS stack: every entry is a transaction plus its depth.
        Deque<TransactionId> stack = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        // pathByDepth[d] is the transaction the search visited at depth d.
        List<TransactionId> pathByDepth = new ArrayList<>();
        Set<TransactionId> visited = new HashSet<>();

        stack.push(start);
        depths.push(0);

        while (!stack.isEmpty()) {
            TransactionId current = stack.pop();
            int depth = depths.pop();
            if (depth >= maxDepth) {
                continue;
            }

            // Keep the path array in sync with the stack's current depth.
            while (pathByDepth.size() > depth) {
                pathByDepth.remove(pathByDepth.size() - 1);
            }
            pathByDepth.add(current);

            if (depth > 0 && current.equals(start)) {
                // The search came home: start is on a cycle. Deadlock.
                return Optional.of(new ArrayList<>(pathByDepth));
            }

            if (!visited.add(current)) {
                continue;
            }

            for (TransactionId next : graph.waitsFor(current)) {
                stack.push(next);
                depths.push(depth + 1);
            }
        }
        return Optional.empty();
    }

    /**
     * Run the search for every transaction started on the local node, the
     * same initiator filter Citus applies: the node that started a
     * transaction owns the search for its deadlocks, so a cluster-wide
     * cycle is reported by exactly one node.
     */
    public List<List<TransactionId>> findCyclesInitiatedAt(int localNodeId) {
        List<List<TransactionId>> cycles = new ArrayList<>();
        for (TransactionId transaction : graph.transactions()) {
            if (transaction.initiatorNodeId() != localNodeId) {
                continue;
            }
            findCycle(transaction).ifPresent(cycles::add);
        }
        return cycles;
    }
}
