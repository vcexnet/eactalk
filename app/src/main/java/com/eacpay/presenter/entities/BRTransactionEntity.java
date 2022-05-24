package com.eacpay.presenter.entities;

public class BRTransactionEntity {
    private final byte[] buff;
    private int blockheight;
    private long timestamp;
    private String txHash;
    private boolean isRead;

    public long getBlockheight() {
        return blockheight;
    }

    public void setBlockheight(int blockheight) {
        this.blockheight = blockheight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public BRTransactionEntity(byte[] txBuff, int blockheight, long timestamp, String txHash) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.buff = txBuff;
        this.txHash = txHash;
    }

    public BRTransactionEntity(byte[] txBuff, int blockheight, long timestamp, String txHash, boolean isRead) {
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.buff = txBuff;
        this.txHash = txHash;
        this.isRead = isRead;
    }

    public byte[] getBuff() {
        return buff;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isRead() {
        return isRead;
    }
}
