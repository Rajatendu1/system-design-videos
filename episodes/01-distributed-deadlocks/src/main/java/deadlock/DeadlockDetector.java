package deadlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Teaching version of the iterative DFS used by Citus's
 * CheckDeadlockForTransactionNode. Work items carry a depth and the search
 * records a path back to its root. The Java collections and return format
 * are our own. A cycle represents deadlock under this simulator's fixed,
 * exclusive-lock model, not every rule of a production lock manager.
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
     * Search from transactions whose initiator is the supplied local node,
     * matching Citus's root filter. This does not deduplicate cycles or
     * guarantee that exactly one node can report a cluster-wide cycle.
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
