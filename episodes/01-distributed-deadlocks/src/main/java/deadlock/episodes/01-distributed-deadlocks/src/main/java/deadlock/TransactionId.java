package deadlock;

import java.util.Objects;

/**
 * A distributed transaction identifier, mirroring Citus's DistributedTransactionId.
 *
 * Every transaction is started on one node (the initiator), and that node is
 * responsible for detecting the deadlocks the transaction participates in.
 * The logical timestamp orders transactions for victim selection; the sequence
 * makes the id unique and the ordering total, so ties are deterministic.
 */
public final class TransactionId implements Comparable<TransactionId> {

    private final int initiatorNodeId;
    private final long timestamp;
    private final int sequence;

    public TransactionId(int initiatorNodeId, long timestamp, int sequence) {
        this.initiatorNodeId = initiatorNodeId;
        this.timestamp = timestamp;
        this.sequence = sequence;
    }

    public int initiatorNodeId() {
        return initiatorNodeId;
    }

    public long timestamp() {
        return timestamp;
    }

    /** Older transaction first; ties broken by initiator node, then sequence. */
    @Override
    public int compareTo(TransactionId other) {
        int byTime = Long.compare(this.timestamp, other.timestamp);
        if (byTime != 0) {
            return byTime;
        }
        int byNode = Integer.compare(this.initiatorNodeId, other.initiatorNodeId);
        if (byNode != 0) {
            return byNode;
        }
        return Integer.compare(this.sequence, other.sequence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionId)) {
            return false;
        }
        TransactionId that = (TransactionId) o;
        return initiatorNodeId == that.initiatorNodeId
                && timestamp == that.timestamp
                && sequence == that.sequence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initiatorNodeId, timestamp, sequence);
    }

    @Override
    public String toString() {
        return "T" + sequence + "(node " + initiatorNodeId + ", t=" + timestamp + ")";
    }
}
