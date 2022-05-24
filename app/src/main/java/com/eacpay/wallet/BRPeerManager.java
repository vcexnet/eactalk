package com.eacpay.wallet;

import android.content.Context;
import android.util.Log;

import com.eacpay.EacApp;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.presenter.entities.BlockEntity;
import com.eacpay.presenter.entities.PeerEntity;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.sqlite.MerkleBlockDataSource;
import com.eacpay.tools.sqlite.PeerDataSource;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.TrustedNode;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class BRPeerManager {
    private static final String TAG = "oldfeel";
    private static BRPeerManager instance;

    private static List<OnTxStatusUpdate> statusUpdateListeners;
    private static OnSyncSucceeded onSyncFinished;

    private static MyService myService;

    public static void setMyService(MyService service) {
        myService = service;
    }

    private BRPeerManager() {
        statusUpdateListeners = new ArrayList<>();
    }

    public static BRPeerManager getInstance() {
        if (instance == null) {
            instance = new BRPeerManager();
        }
        return instance;
    }

    /**
     * void BRPeerManagerSetCallbacks(BRPeerManager *manager, void *info,
     * void (*syncStarted)(void *info),
     * void (*syncSucceeded)(void *info),
     * void (*syncFailed)(void *info, BRPeerManagerError error),
     * void (*txStatusUpdate)(void *info),
     * void (*saveBlocks)(void *info, const BRMerkleBlock blocks[], size_t count),
     * void (*savePeers)(void *info, const BRPeer peers[], size_t count),
     * int (*networkIsReachable)(void *info))
     */

    public static void syncStarted() {
        Timber.d("syncStarted: %s", Thread.currentThread().getName());
//        BRPeerManager.getInstance().refreshConnection();
        Context ctx = EacApp.getBreadContext();
        int startHeight = BRSharedPrefs.getStartHeight(ctx);
        int lastHeight = BRSharedPrefs.getLastBlockHeight(ctx);
        if (startHeight > lastHeight) BRSharedPrefs.putStartHeight(ctx, lastHeight);
        Log.e(TAG, "syncStarted: syncStarted startSyncingProgressThread");
        if (myService != null) {
            myService.startEac();
        }
    }

    public static void syncSucceeded() {
        Timber.d("syncSucceeded");
        final Context app = EacApp.getBreadContext();
        if (app == null) return;
        BRSharedPrefs.putLastSyncTime(app, System.currentTimeMillis());
        BRSharedPrefs.putAllowSpend(app, true);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BRSharedPrefs.putStartHeight(app, getCurrentBlockHeight());
            }
        });
        if (onSyncFinished != null) onSyncFinished.onFinished();
    }

    public static void syncFailed() {
        Timber.d("syncFailed");
        Context ctx = EacApp.getBreadContext();
        if (ctx == null) return;
        Timber.d("Network Not Available, showing not connected bar");

        if (onSyncFinished != null) onSyncFinished.onFinished();
    }

    public static void txStatusUpdate() {
        Timber.d("txStatusUpdate");

        for (OnTxStatusUpdate listener : statusUpdateListeners) {
            if (listener != null) listener.onStatusUpdate();
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateLastBlockHeight(getCurrentBlockHeight());
            }
        });
    }

    public static void saveBlocks(final BlockEntity[] blockEntities, final boolean replace) {
        Timber.d("saveBlocks: %s", blockEntities.length);

        final Context ctx = EacApp.getBreadContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (replace) MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
                MerkleBlockDataSource.getInstance(ctx).putMerkleBlocks(blockEntities);
            }
        });

    }

    public static void savePeers(final PeerEntity[] peerEntities, final boolean replace) {
        Timber.d("savePeers: %s", peerEntities.length);
        final Context ctx = EacApp.getBreadContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (replace) PeerDataSource.getInstance(ctx).deleteAllPeers();
                PeerDataSource.getInstance(ctx).putPeers(peerEntities);
            }
        });
    }

    public static boolean networkIsReachable() {
        Timber.d("networkIsReachable");
        return BRWalletManager.getInstance().isNetworkAvailable(EacApp.getBreadContext());
    }

    public static void deleteBlocks() {
        Timber.d("deleteBlocks");
        final Context ctx = EacApp.getBreadContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
            }
        });
    }

    public static void deletePeers() {
        Timber.d("deletePeers");
        final Context ctx = EacApp.getBreadContext();
        if (ctx == null) return;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                PeerDataSource.getInstance(ctx).deleteAllPeers();
            }
        });
    }

    public void updateFixedPeer(Context ctx) {
        String node = BRSharedPrefs.getTrustNode(ctx);
        String host = TrustedNode.getNodeHost(node);
        int port = TrustedNode.getNodePort(node);
        boolean success = setFixedPeer(host, port);
        if (!success) {
            Timber.i("updateFixedPeer: Failed to updateFixedPeer with input: %s", node);
        } else {
            Timber.d("updateFixedPeer: succeeded");
        }
        connect();
    }

    public void networkChanged(boolean isOnline) {
        if (isOnline)
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BRPeerManager.getInstance().connect();
                }
            });
    }

    public void addStatusUpdateListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            return;
        }
        if (!statusUpdateListeners.contains(listener))
            statusUpdateListeners.add(listener);
    }

    public void removeListener(OnTxStatusUpdate listener) {
        if (statusUpdateListeners == null) {
            return;
        }
        statusUpdateListeners.remove(listener);
    }

    public static void setOnSyncFinished(OnSyncSucceeded listener) {
        onSyncFinished = listener;
    }

    public interface OnTxStatusUpdate {
        void onStatusUpdate();
    }

    public interface OnSyncSucceeded {
        void onFinished();
    }

    public static void updateLastBlockHeight(int blockHeight) {
        final Context ctx = EacApp.getBreadContext();
        if (ctx == null) return;
        BRSharedPrefs.putLastBlockHeight(ctx, blockHeight);
    }

    public native String getCurrentPeerName();

    public native void create(int earliestKeyTime, int blockCount, int peerCount);

    public native void connect();

    public native void putPeer(byte[] peerAddress, byte[] peerPort, byte[] peerTimeStamp);

    public native void createPeerArrayWithCount(int count);

    public native void putBlock(byte[] block, int blockHeight);

    public native void createBlockArrayWithCount(int count);

    public native static double syncProgress(int startHeight);

    public native static int getCurrentBlockHeight();

    public native static int getRelayCount(byte[] hash);

    public native boolean setFixedPeer(String node, int port);

    public native static int getEstimatedBlockHeight();

    public native boolean isCreated();

    public native boolean isConnected();

    public native void peerManagerFreeEverything();

    public native long getLastBlockTimestamp();

    public native void rescan();
}