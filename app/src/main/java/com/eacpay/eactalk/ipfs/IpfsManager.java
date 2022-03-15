package com.eacpay.eactalk.ipfs;

import com.eacpay.eactalk.utils.MyUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import ipfs.gomobile.android.IPFS;
import timber.log.Timber;

public class IpfsManager {
    private static final String TAG = "oldfeel";
    public IPFS ipfs;
    public static IpfsManager ipfsInstance;
    private String peerID;
    SyncLoadTask syncTask;
    List<? extends IpfsItem> list;
    OnIpfsLoadComplete onIpfsLoadComplete;

    public static IpfsManager getInstance() {
        if (ipfsInstance == null) {
            ipfsInstance = new IpfsManager();
        }
        return ipfsInstance;
    }

    public void setIpfs(IPFS ipfs) {
        this.ipfs = ipfs;
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

    public String getIpfsStatus() {
        if (ipfs == null) {
            return "未连接";
        }
        if (peerID == null) {
            return "已连接";
        }
        return "已连接 本机 PeerID: " + peerID;
    }

    public synchronized void loadStart(IpfsItem ipfsItem, OnIpfsLoadComplete onIpfsLoadComplete) {
        List<IpfsItem> list = new ArrayList<>();
        list.add(ipfsItem);
        loadStart(list, onIpfsLoadComplete);
    }

    public synchronized void loadStart(List<? extends IpfsItem> list, OnIpfsLoadComplete onIpfsLoadComplete) {
        this.list = list;
        this.onIpfsLoadComplete = onIpfsLoadComplete;

        loadStop();

        syncTask = new SyncLoadTask();
        syncTask.start();
    }

    public synchronized void loadStop() {
        try {
            if (syncTask != null) {
                syncTask.interrupt();
                syncTask = null;
            }
        } catch (Exception ex) {
            Timber.e(ex);
        }

    }

    class SyncLoadTask extends Thread {

        @Override
        public void run() {
            if (list == null) {
                return;
            }
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (ipfs == null);

            for (int i = 0; i < list.size(); i++) {
                IpfsItem ipfsItem = list.get(i);
                if (ipfsItem.ipfs == null || ipfsItem.ipfs.equals("")) {
                    continue;
                }
                try {
                    byte[] lsData = IpfsManager.getInstance().getIpfs().newRequest("ls")
                            .withArgument(ipfsItem.ipfs)
                            .send();

                    ipfsItem.ipfsLs = new Gson().fromJson(new String(lsData), IpfsLs.class);

                    if (ipfsItem.ipfsLs.isDir()) {
                        if (onIpfsLoadComplete != null) {
                            onIpfsLoadComplete.onLoad(ipfsItem, i);
                        }
                    } else {
                        ipfsDownload(ipfsItem, i);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    ipfsDownload(ipfsItem, i);
                }
            }
        }

        private void ipfsDownload(IpfsItem ipfsItem, int i) {
            try {
                ipfsItem.data = IpfsManager.getInstance().getIpfs().newRequest("cat")
                        .withArgument(ipfsItem.ipfs)
                        .send();
                ipfsItem.mimeType = MyUtils.guessMimeType(ipfsItem.data);
                if (onIpfsLoadComplete != null) {
                    onIpfsLoadComplete.onLoad(ipfsItem, i);
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
}
