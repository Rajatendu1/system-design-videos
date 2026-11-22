package deadlock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The running example of the episode, end to end.
 *
 * Two transfers, two shards, from Citus's engineering FAQ:
 *   transaction T1 updates 'hello' on node A, then 'world' on node B
 *   transaction T2 updates 'world' on node B, then 'hello' on node A
 * Each node sees one session waiting on a row the other holds. Neither
 * node's local detector can fire. Only the assembled global graph shows
 * the cycle.
 */
public final class Simulation {

    public static void main(String[] args) {
        Node nodeA = new Node(1);
        Node nodeB = new Node(2);

        TransactionId t1 = new TransactionId(1, 1000L, 1); // started on node A, older
        TransactionId t2 = new TransactionId(2, 1005L, 2); // started on node B, younger

        nodeA.addWait(t2, t1, "hello"); // T2 wants 'hello', held by T1 on node A
        nodeB.addWait(t1, t2, "world"); // T1 wants 'world', held by T2 on node B

        GlobalWaitGraph graph = GlobalWaitGraph.collectFrom(List.of(nodeA, nodeB));
        DeadlockDetector detector = new DeadlockDetector(graph);

        List<List<TransactionId>> cycles = detector.findCyclesInitiatedAt(1);
        if (cycles.isEmpty()) {
            System.out.println("No distributed deadlock found.");
            return;
        }

        List<TransactionId> cycle = cycles.get(0);
        System.out.println("Distributed deadlock found:");
        for (int i = 0; i < cycle.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + cycle.get(i));
        }

        VictimPicker picker = new VictimPicker(Set.of(t1, t2));
        Optional<TransactionId> victim = picker.pickVictim(cycle);
        victim.ifPresentOrElse(
                v -> System.out.println("Cancelling the youngest transaction: " + v),
                () -> System.out.println("Cycle found, but nobody on it is still running."));
    }
}
