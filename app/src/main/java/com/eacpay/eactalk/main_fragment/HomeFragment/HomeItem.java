package com.eacpay.eactalk.main_fragment.HomeFragment;

import com.eacpay.eactalk.ipfs.IpfsItem;

public class HomeItem extends IpfsItem {
    public String type;
    public String address;
    public int value;
    public boolean spent;
    public String txid;
    public int vout;
    public int time;
    public int height;
    public String sender;
    public String hash;
    public String txcomment;
    public OutputsItem[] outputs;

    public boolean isIpfs() {
        return ipfs != null && !ipfs.equals("");
    }
}