# Episode 1: How a distributed database hunts deadlocks with DFS

Two bank transfers freeze at the same moment on two different servers. Neither
server can see why. This episode builds the answer: the waits-for graph, the
depth-first search that finds the cycle, and the production design from
PostgreSQL and Citus that runs it every couple of seconds.

## What this code is

A self-contained simulator that mirrors Citus's real distributed deadlock
detector (`src/backend/distributed/transaction/distributed_deadlock_detection.c`):

| This code | Citus production code |
| --- | --- |
| `Node.localWaitEdges()` | the local lock table on each worker |
| `GlobalWaitGraph.collectFrom()` | `BuildGlobalWaitGraph()` + `BuildAdjacencyListsForWaitGraph()` |
| `DeadlockDetector.findCycle()` | `CheckDeadlockForTransactionNode()` (iterative DFS, explicit stack) |
| `DeadlockDetector.findCyclesInitiatedAt()` | the local-initiator filter (`initiatorNodeIdentifier == localGroupId`) |
| `VictimPicker.pickVictim()` | cancel the youngest transaction in the cycle |
| `VictimPicker`'s live-backend check | `AssociateDistributedTransactionWithBackendProc()` before cancelling |

## Run it

```sh
sh run.sh
```

Expected output: 9 tests pass, then the two-transfer simulation finds the
cycle `T1 -> T2 -> T1` and cancels `T2`, the youngest transaction.

## The edge cases (each one is a test)

1. **No deadlock** - a busy graph with no cycle returns clean.
2. **Two-node cycle** - the episode's two transfers; the exact cycle is returned.
3. **Three-node cycle** - a longer loop across three shards.
4. **Foreign cycle skipped** - cycles among transactions started on other nodes
   are ignored; the initiator node owns the detection.
5. **Youngest victim, every time** - deterministic by transaction timestamp.
6. **Timestamp tie broken deterministically** - the full id ordering breaks ties.
7. **Two cycles at once** - independent cycles, found one per pass.
8. **Self-cycle robustness** - a one-node cycle cannot crash the search.
9. **Stale snapshot** - a victim no longer backed by a live backend is not
   cancelled. This is a victim-still-exists check, not a revalidation of the
   whole cycle.

## Honest limits, narrated in the episode

- Detection is periodic, not instant. PostgreSQL checks after about a second
  of waiting; Citus rechecks roughly every two seconds by default.
- The global graph is stitched from lock tables collected node by node; a
  cycle found in it may already have partly resolved.
- Citus checks distributed transactions only; purely local cycles stay with
  each node's own PostgreSQL.

## Sources

- [Citus: distributed_deadlock_detection.c](https://github.com/citusdata/citus/blob/master/src/backend/distributed/transaction/distributed_deadlock_detection.c)
- [Citus: maintenanced.c](https://github.com/citusdata/citus/blob/master/src/backend/distributed/utils/maintenanced.c)
- [Citus engineering FAQ: Databases and Distributed Deadlocks](https://www.citusdata.com/blog/2017/08/31/databases-and-distributed-deadlocks-a-faq/)
- [PostgreSQL: deadlock.c](https://github.com/postgres/postgres/blob/master/src/backend/storage/lmgr/deadlock.c)
- [PostgreSQL lock manager README](https://raw.githubusercontent.com/postgres/postgres/master/src/backend/storage/lmgr/README)
