package com.eacpay.eactalk.ipfs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ipfs.gomobile.android.IPFS;

public class IpfsManager {
    private static final String TAG = "oldfeel";
    public IPFS ipfs;
    private static IpfsManager ipfsInstance;
    private String peerID = "";
    public boolean isStop = false;
    ExecutorService threadPool = Executors.newCachedThreadPool();

    public static IpfsManager getInstance() {
        if (ipfsInstance == null) {
            ipfsInstance = new IpfsManager();
        }
        return ipfsInstance;
    }

    public void setIpfs(IPFS ipfs) {
        this.ipfs = ipfs;
        isStop = false;
    }

    public IPFS getIpfs() {
        return ipfs;
    }

    public void setPeerID(String peerID) {
        this.peerID = peerID;
    }

    public String getPeerID() {
        return peerID;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void stop() {
        isStop = true;
        if (ipfs != null) {
            try {
                ipfs.stop();
            } catch (IPFS.NodeStopException e) {
                e.printStackTrace();
            }
            ipfs = null;
        }
    }
}
