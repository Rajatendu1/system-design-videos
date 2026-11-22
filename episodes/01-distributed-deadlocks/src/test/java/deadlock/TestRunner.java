package deadlock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Zero-dependency test runner: every edge case narrated in the episode,
 * runnable with nothing but a JDK. Prints PASS/FAIL per test and exits
 * non-zero if anything fails.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        noDeadlockReturnsClean();
        twoNodeCycleDetectedWithPath();
        threeNodeCycleDetected();
        foreignCycleSkippedByInitiatorFilter();
        youngestVictimIsDeterministic();
        timestampTieBrokenDeterministically();
        twoIndependentCyclesFound();
        selfCycleDoesNotCrashTheSearch();
        staleSnapshotVictimNotLiveIsNotCancelled();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** A busy graph with no cycle must return clean. The detector must not cry wolf. */
    static void noDeadlockReturnsClean() {
        Node node = new Node(1);
        TransactionId t1 = new TransactionId(1, 1000L, 1);
        TransactionId t2 = new TransactionId(1, 1001L, 2);
        TransactionId t3 = new TransactionId(1, 1002L, 3);
        node.addWait(t1, t2, "a"); // t1 -> t2 -> t3, a chain, not a cycle
        node.addWait(t2, t3, "b");

        DeadlockDetector detector = new DeadlockDetector(GlobalWaitGraph.collectFrom(List.of(node)));
        check("no deadlock returns clean",
                detector.findCycle(t1).isEmpty()
                        && detector.findCycle(t2).isEmpty()
                        && detector.findCyclesInitiatedAt(1).isEmpty());
    }

    /** The episode's two transfers: the exact cycle is returned, start at both ends. */
    static void twoNodeCycleDetectedWithPath() {
        Node nodeA = new Node(1);
        Node nodeB = new Node(2);
        TransactionId t1 = new TransactionId(1, 1000L, 1);
        TransactionId t2 = new TransactionId(2, 1005L, 2);
        nodeA.addWait(t2, t1, "hello");
        nodeB.addWait(t1, t2, "world");

        DeadlockDetector detector = new DeadlockDetector(GlobalWaitGraph.collectFrom(List.of(nodeA, nodeB)));
        Optional<List<TransactionId>> cycle = detector.findCycle(t1);

        check("two-node cycle detected with path",
                cycle.isPresent()
                        && cycle.get().size() == 3
                        && cycle.get().get(0).equals(t1)
                        && cycle.get().get(2).equals(t1)
                        && cycle.get().contains(t2));
    }

    /** A longer loop across three shards, so viewers see the search travel. */
    static void threeNodeCycleDetected() {
        Node nodeA = new Node(1);
        Node nodeB = new Node(2);
        Node nodeC = new Node(3);
        TransactionId t1 = new TransactionId(1, 1000L, 1);
        TransactionId t2 = new TransactionId(2, 1001L, 2);
        TransactionId t3 = new TransactionId(3, 1002L, 3);
        nodeA.addWait(t1, t2, "a");
        nodeB.addWait(t2, t3, "b");
        nodeC.addWait(t3, t1, "c");

        DeadlockDetector detector = new DeadlockDetector(GlobalWaitGraph.collectFrom(List.of(nodeA, nodeB, nodeC)));
        Optional<List<TransactionId>> cycle = detector.findCycle(t1);

        check("three-node cycle detected",
                cycle.isPresent() && cycle.get().size() == 4 && cycle.get().contains(t3));
    }

    /**
     * A cycle among transactions started on other nodes is ignored, matching
     * Citus's initiator filter: the node that started a transaction owns its
     * detection.
     */
    static void foreignCycleSkippedByInitiatorFilter() {
        Node nodeA = new Node(1);
        Node nodeB = new Node(2);
        TransactionId foreign1 = new TransactionId(9, 1000L, 1);
        TransactionId foreign2 = new TransactionId(8, 1001L, 2);
        nodeA.addWait(foreign1, foreign2, "a");
        nodeB.addWait(foreign2, foreign1, "b");

        DeadlockDetector detector = new DeadlockDetector(GlobalWaitGraph.collectFrom(List.of(nodeA, nodeB)));
        check("foreign cycle skipped by initiator filter",
                detector.findCyclesInitiatedAt(1).isEmpty()
                        && detector.findCyclesInitiatedAt(9).size() == 1);
    }

    /** Same graph, repeated runs, same victim. Deterministic by timestamp. */
    static void youngestVictimIsDeterministic() {
        TransactionId older = new TransactionId(1, 1000L, 1);
        TransactionId younger = new TransactionId(2, 1005L, 2);
        List<TransactionId> cycle = List.of(older, younger, older);
        VictimPicker picker = new VictimPicker(Set.of(older, younger));

        Optional<TransactionId> first = picker.pickVictim(cycle);
        Optional<TransactionId> second = picker.pickVictim(cycle);

        check("youngest victim, every time",
                first.isPresent() && first.get().equals(younger) && first.equals(second));
    }

    /** Equal timestamps cannot make the pick unstable: the id ordering breaks the tie. */
    static void timestampTieBrokenDeterministically() {
        TransactionId a = new TransactionId(1, 1000L, 1);
        TransactionId b = new TransactionId(2, 1000L, 2); // same timestamp as a
        List<TransactionId> cycle = List.of(a, b, a);
        VictimPicker picker = new VictimPicker(Set.of(a, b));

        Optional<TransactionId> victim = picker.pickVictim(cycle);

        check("timestamp tie broken deterministically",
                victim.isPresent() && victim.get().equals(b)); // total order picks one, always
    }

    /** Independent cycles in one global graph are found one per initiating transaction. */
    static void twoIndependentCyclesFound() {
        Node nodeA = new Node(1);
        Node nodeB = new Node(2);
        TransactionId t1 = new TransactionId(1, 1000L, 1);
        TransactionId t2 = new TransactionId(2, 1001L, 2);
        TransactionId t3 = new TransactionId(1, 1002L, 3);
        TransactionId t4 = new TransactionId(2, 1003L, 4);
        nodeA.addWait(t1, t2, "a");
        nodeA.addWait(t3, t4, "c");
        nodeB.addWait(t2, t1, "b");
        nodeB.addWait(t4, t3, "d");

        DeadlockDetector detector = new DeadlockDetector(GlobalWaitGraph.collectFrom(List.of(nodeA, nodeB)));
        check("two independent cycles found", detector.findCyclesInitiatedAt(1).size() == 2);
    }

    /**
     * A one-node cycle cannot crash the search. Citus's own code comments
     * allow for a single-transaction self-deadlock case.
     */
    static void selfCycleDoesNotCrashTheSearch() {
        Node node = new Node(1);
        TransactionId t1 = new TransactionId(1, 1000L, 1);
        node.addWait(t1, t1, "a"); // waits for itself

        DeadlockDetector detector = new DeadlockDetector(GlobalWaitGraph.collectFrom(List.of(node)));
        Optional<List<TransactionId>> cycle = detector.findCycle(t1);

        check("self-cycle handled without crashing",
                cycle.isPresent() && cycle.get().size() == 2);
    }

    /**
     * The graph is a stitched snapshot: if the chosen victim is no longer
     * associated with a live backend, there is nothing to cancel. This is a
     * victim-still-exists check, not a revalidation of the whole cycle.
     */
    static void staleSnapshotVictimNotLiveIsNotCancelled() {
        TransactionId older = new TransactionId(1, 1000L, 1);
        TransactionId younger = new TransactionId(2, 1005L, 2);
        List<TransactionId> cycle = List.of(older, younger, older);

        VictimPicker nobodyAlive = new VictimPicker(Set.of());
        VictimPicker onlyOlderAlive = new VictimPicker(Set.of(older));

        check("stale snapshot: dead victim is not cancelled",
                nobodyAlive.pickVictim(cycle).isEmpty()
                        && onlyOlderAlive.pickVictim(cycle).get().equals(older));
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
