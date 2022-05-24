package com.eacpay.eactalk.fragment.main.HomeFragment;

import android.content.Context;

import com.eacpay.R;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.eactalk.ipfs.IpfsDataFetcher;
import com.eacpay.eactalk.ipfs.IpfsItem;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.eactalk.utils.SensitiveWord;
import com.eacpay.presenter.entities.TxItem;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;

import java.util.List;

public class HomeItem extends IpfsItem {
    private static final String TAG = "oldfeel";
    public String type;
    public double value;
    public int spent;
    public String txid;
    public int vout;
    public int time;
    public int height;
    public String sender;
    public String hash;
    public String txcomment;
    public OutputsItem[] outputs;
    public long sent;

    public String contactName;

    /**
     * 将我的消息转换成首页数据
     *
     * @param context
     * @param item
     * @return
     */
    public static String parseFromTxItem(Context context, TxItem item) {
        HomeItem homeItem = new HomeItem();
        homeItem.ipfs = item.getIPFS();
        homeItem.time = (int) item.getTimeStamp();
        homeItem.value = item.getAmount(context).intValue();
        homeItem.sender = item.getFrom()[0];
        homeItem.txcomment = item.getTxComment();
        homeItem.txid = item.getTxHashHexReversed();
        homeItem.contactName = searchContactName(context, homeItem);
        homeItem.confirmations = -1; // 我的消息中没有 confirmations 字段,设为 -1 用来跟首页数据区分.
        homeItem.address = item.getTo()[0];
        homeItem.sent = item.getSent();
        return new Gson().toJson(homeItem);
    }

    public static String searchContactName(Context context, HomeItem homeItem) {
        List<ContactItem> list = BRSharedPrefs.getContactList(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).address.equals(homeItem.sender)) {
                homeItem.contactName = list.get(i).name;
                return list.get(i).name;
            }
        }
        return "";
    }

    public boolean isIpfs() {
        return ipfs != null && !ipfs.equals("");
    }

    public void setIpfsItem(IpfsItem ipfsItem) {
        this.ipfs = ipfsItem.ipfs;
        this.ipfsLs = ipfsItem.ipfsLs;
        this.data = ipfsItem.data;
        this.mimeType = ipfsItem.mimeType;
        this.confirmations = ipfsItem.confirmations;
        this.loadOK = ipfsItem.loadOK;
        this.imageErrCode = ipfsItem.imageErrCode;
    }

    public String parseComment(Context context) {
        String result = txcomment;
        if (txcomment != null && txcomment.startsWith(IpfsDataFetcher.ENCRYPT_PREFIX)) {
            result = context.getString(R.string.E__) + MyUtils.decrypt(txcomment.replace(IpfsDataFetcher.ENCRYPT_PREFIX, ""), address);
        }
        result = SensitiveWord.replace(context, result);
        return result;
    }

    public String getTarget() {
        if (outputs != null) {
            return outputs[0].address;
        }
        return "";
    }
}