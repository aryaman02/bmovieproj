package com.example.kafka;

public class PartitionOffset {
    private int partitionID;
    private long offset;

    public PartitionOffset(int partitionID, long offset) {
        this.partitionID = partitionID;
        this.offset = offset;
    }

    public int getPartitionID() {
        return partitionID;
    }

    public void setPartitionID(int partitionID) {
        this.partitionID = partitionID;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
