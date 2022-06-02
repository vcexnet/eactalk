package com.eacpay.presenter.entities;


import android.content.Context;

import com.eacpay.eactalk.ipfs.IpfsDataFetcher;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.eactalk.utils.SensitiveWord;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.util.BRExchange;
import com.platform.entities.TxMetaData;

import java.math.BigDecimal;

public class TxItem {
    public static final String TAG = TxItem.class.getName();
    private long timeStamp;
    private int blockHeight;
    private byte[] txHash;
    private long sent;
    private long received;
    private long fee;
    private String[] to;
    private String[] from;
    public String txReversed;
    private long balanceAfterTx;
    private long[] outAmounts;
    private boolean isValid;
    private int txSize;
    public TxMetaData metaData;
    private String txComment;
    private String txIPFS;
    private boolean isRead;
    public String contactName;

    private TxItem() {
    }

    public TxItem(long timeStamp, int blockHeight, byte[] hash, String txReversed, long sent,
                  long received, long fee, String[] to, String[] from,
                  long balanceAfterTx, int txSize, long[] outAmounts, boolean isValid,
                  String txComposedComment) {
        this.timeStamp = timeStamp;
        this.blockHeight = blockHeight;
        this.txReversed = txReversed;
        this.txHash = hash;
        this.sent = sent;
        this.received = received;
        this.fee = fee;
        this.to = to;
        this.from = from;
        this.balanceAfterTx = balanceAfterTx;
        this.outAmounts = outAmounts;
        this.isValid = isValid;
        this.txSize = txSize;
        int pos = txComposedComment.indexOf("\n");
        this.txComment = (pos > 0) ? txComposedComment.substring(pos + 1) : txComposedComment;
        this.txIPFS = (pos > 0) ? txComposedComment.substring(0, pos) : "";
        if (!MyUtils.isIPFSCID(this.txIPFS)) {
            this.txComment = txComposedComment;
            this.txIPFS = "";
        }
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public long getFee() {
        return fee;
    }

    public int getTxSize() {
        return txSize;
    }

    public String[] getFrom() {
        return from;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public String getTxHashHexReversed() {
        return txReversed;
    }

    public long getReceived() {
        return received;
    }

    public long getSent() {
        return sent;
    }

    public static String getTAG() {
        return TAG;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String[] getTo() {
        return to;
    }

    public long getBalanceAfterTx() {
        return balanceAfterTx;
    }

    public long[] getOutAmounts() {
        return outAmounts;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getTxComment() {
        return txComment;
    }

    public String getIPFS() {
        return txIPFS;
    }

    public BigDecimal getAmount(Context context) {
        String iso = BRSharedPrefs.getPreferredLTC(context) ? "EAC" : BRSharedPrefs.getIso(context);
        BigDecimal txAmount = new BigDecimal(getReceived() - getSent()).abs();
        return BRExchange.getAmountFromSatoshis(context, iso, getFee() == -1 ? txAmount : txAmount.subtract(new BigDecimal(getFee())));
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String parseComment(Context context) {
        String result = txComment;
        if (txComment != null && txComment.startsWith(IpfsDataFetcher.ENCRYPT_PREFIX)) {
            result = "(密)" + MyUtils.decrypt(txComment.replace(IpfsDataFetcher.ENCRYPT_PREFIX, ""), getTo()[0]);
        }
        result = SensitiveWord.replace(context, result);
        return result;
    }
}
