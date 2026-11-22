# Episode 1: How a distributed database hunts deadlocks with DFS

Two transfers wait on locks held across two workers. Their combined waits-for
graph contains a cycle that neither local fragment reveals.

## What this code is

A plain-Java teaching simulator inspired by the Citus detector, not a port of
PostgreSQL or Citus. It assumes fixed, exclusive-lock waits: there are no lock
modes, wait-queue priorities, network failures or real database backends.
PostgreSQL also handles soft wait-queue edges and can reorder queues instead
of aborting a transaction. This model does not implement that machinery.

| This code | Related Citus implementation |
| --- | --- |
| `Node.localWaitEdges()` | per-node wait edges, represented here as supplied data |
| `GlobalWaitGraph.collectFrom()` | global edge collection and adjacency-list construction |
| `DeadlockDetector.findCycle()` | iterative DFS with depth-tagged work items and path tracking |
| `DeadlockDetector.findCyclesInitiatedAt()` | search roots restricted to local-initiator transactions |
| `VictimPicker.pickVictim()` | youngest eligible participant, with an added deterministic ID tie-break |

The simulator receives an explicit set of eligible live backends. In Citus,
association checks use real local backend state and restrict candidates to
transactions initiated on that node. This is not a globally youngest-victim
guarantee. Supply only locally eligible transactions to model one detector.
The simulation starts both transfers on one coordinator, while their locks
live on different workers. Its cancellation is printed, not executed.

## Run it

Install a JDK (11 or later), then from this directory:

```sh
sh run.sh
```

The shell invocation is deliberate: the repository stores `run.sh` without
an executable bit. No third-party Java dependencies are needed.
Expected output: 9 tests pass, then the simulation returns `T1 -> T2 -> T1`
and selects `T2`, the younger eligible participant.

## The tests

1. No cycle in an acyclic graph.
2. Exact path for a two-node cycle.
3. A longer, three-node cycle.
4. No local search roots when all transactions have foreign initiators.
5. Repeatable youngest eligible victim.
6. Equal timestamps resolved by our explicit ID ordering, an addition to Citus.
7. Two independent cycles found from local roots. The collector may return
   the same cycle from different roots; it does not promise unique reporting.
8. A synthetic self-loop as an algorithm robustness test, not evidence that
   a particular SQL statement can deadlock with itself.
9. No victim when the eligible-live set is empty, and a remaining eligible
   participant selected when another is absent. This is not cycle revalidation.

## Honest limits

- A cycle means deadlock under this fixed exclusive-lock model. Real lock
  managers have more rules. Breaking one cycle does not clear every cycle.
- PostgreSQL's default `deadlock_timeout` is one second. Citus's maintenance
  daemon derives its normal check interval from that setting and a factor
  whose default is two, with faster rechecks after a detection. These are
  scheduling settings, not guaranteed deadlock lifetimes or response times.
- Citus assembles wait edges from nodes at different times. A backend still
  existing does not prove that the collected cycle is still present.
- The initiator filter is not proof of cluster-wide exactly-once reporting.
- Detection here returns paths; it does not perform rollback, release locks,
  retry transactions, or simulate the distributed protocol.

## Sources

- [Citus detector](https://github.com/citusdata/citus/blob/main/src/backend/distributed/transaction/distributed_deadlock_detection.c)
- [Citus maintenance daemon](https://github.com/citusdata/citus/blob/main/src/backend/distributed/utils/maintenanced.c)
- [Citus deadlock FAQ](https://www.citusdata.com/blog/2017/08/31/databases-and-distributed-deadlocks-a-faq/)
- [PostgreSQL detector](https://github.com/postgres/postgres/blob/master/src/backend/storage/lmgr/deadlock.c)
- [PostgreSQL lock manager README](https://raw.githubusercontent.com/postgres/postgres/master/src/backend/storage/lmgr/README)
