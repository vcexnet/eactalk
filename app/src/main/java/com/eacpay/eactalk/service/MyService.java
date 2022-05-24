package com.eacpay.eactalk.service;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.NetworkUtils;
import com.eacpay.EacApp;
import com.eacpay.R;
import com.eacpay.databinding.FragmentMainHomeBinding;
import com.eacpay.eactalk.MainActivity;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRConstants;
import com.eacpay.tools.util.Utils;
import com.eacpay.wallet.BRPeerManager;
import com.eacpay.wallet.BRWalletManager;

import org.json.JSONObject;

import java.util.ArrayList;

import ipfs.gomobile.android.IPFS;

public class MyService extends Service {
    private static final String TAG = "oldfeel";
    public static final int NOTIFICATION_STATUS_ID = 123;
    public boolean isEacConnect;
    public boolean isEacSyncFinish;

    static {
        System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
    }

    IBinder binder = new EacBinder();
    MyReceiver myReceiver = new MyReceiver();
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    String statusIpfsText;
    String statusEacText;
    double statusEacProgress = 0;
    private FragmentMainHomeBinding binding;
    private FragmentActivity bindingActivity;
    private Thread ipfsThread;
    private boolean ipfsRunning;
    private boolean notificationRunning;

    private Thread eacThread;
    private boolean eacRunning;
    public static String defaultIPFSNode = "/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3";
    public static String defaultIPFSGateway = "http://171.221.247.27:85";
    public static String defaultHomeUrl = "http://apitest.eacpay.com:9000";
    public static String defaultEarthCoinUrl = "https://api.eacpay.com:9000";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        statusIpfsText = getString(R.string.ipfs_connecting);
        statusEacText = getString(R.string.eac_connecting);
        initBroadcastReceiver();
        initNotification();
        initListener();
        initEacStatus();
        initIpfsStatus();
    }

    private void initBroadcastReceiver() {
        IntentFilter filter = new IntentFilter("EACTALK_CLOSE");
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(myReceiver, filter);
    }

    private void initListener() {
        BRWalletManager.getInstance().addBalanceChangedListener(new BRWalletManager.OnBalanceChanged() {
            @Override
            public void onBalanceChanged(long balance) {

            }
        });

        BRPeerManager.getInstance().addStatusUpdateListener(new BRPeerManager.OnTxStatusUpdate() {
            @Override
            public void onStatusUpdate() {

            }
        });

        BRPeerManager.setMyService(this);
        BRPeerManager.setOnSyncFinished(new BRPeerManager.OnSyncSucceeded() {
            @Override
            public void onFinished() {
                isEacSyncFinish = true;
                statusEacProgress = 100;
                statusEacText = getString(R.string.eac_sync_complete);
                updateNotification();
            }
        });

        BRSharedPrefs.addIsoChangedListener(new BRSharedPrefs.OnIsoChangedListener() {
            @Override
            public void onIsoChanged(String iso) {

            }
        });

        myReceiver.setOnConnectionChanged(new MyReceiver.OnConnectChangedListener() {
            @Override
            public void onConnectionChanged() {
                if (NetworkUtils.isConnected()) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(getApplicationContext()));
                            if (progress < 1) {
                                startEac();
                            }
                        }
                    });

                    startIpfs();
                } else {
                    stopEac();
                    MyUtils.log("stop from onConnectionChanged");
                    stopIpfs();
                }
            }
        });
    }

    private void updateNotification() {
        if (binding != null && bindingActivity != null) {
            bindingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (statusEacProgress <= 0 || statusEacProgress >= 100) {
                        binding.mainHomeSyncView.setVisibility(View.GONE);
                    } else {
                        binding.mainHomeSyncView.setVisibility(View.VISIBLE);
                    }
                    binding.mainHomeSyncProgress.setProgress((int) statusEacProgress);
                    binding.mainHomeSyncTime.setText(statusEacText);
                }
            });
        }

        RemoteViews remoteViews = createRemoteViews();
        remoteViews.setTextViewText(R.id.status_ipfs, statusIpfsText);

        if (statusEacProgress <= 0 || statusEacProgress >= 100) {
            remoteViews.setViewVisibility(R.id.status_eac_progress, View.GONE);
        } else {
            remoteViews.setViewVisibility(R.id.status_eac_progress, View.VISIBLE);
        }
        remoteViews.setProgressBar(R.id.status_eac_progress, 100, (int) statusEacProgress, false);
        remoteViews.setTextViewText(R.id.status_eac_time, statusEacText);

        notificationBuilder.setCustomBigContentView(remoteViews);
        if (notificationRunning) {
            notificationManager.notify(NOTIFICATION_STATUS_ID, notificationBuilder.build());
        }
    }

    private void initIpfsStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    long ipfsTime = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("time", "0"));
                    if (ipfsTime == 0 || EacApp.isShow || (System.currentTimeMillis() - EacApp.backgroundedTime < ipfsTime * 60 * 1000)) {
                        startIpfs();
                    } else {
                        MyUtils.log("stop from " + EacApp.isShow + " " + (System.currentTimeMillis() - EacApp.backgroundedTime));
                        stopIpfs();
                    }
                    updateNotification();

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        startIpfs();
    }

    private void initEacStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isEacConnect = BRPeerManager.getInstance().isConnected();
                }
            }
        }).start();
        startEac();
    }

    private void initNotification() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("eactalk", "eactalk");
        } else {
            channelId = "eactalk";
        }

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        notificationBuilder
                .setSmallIcon(R.drawable.logo)
                .setCustomBigContentView(createRemoteViews())
                .setOngoing(true)
                .setContentTitle("EACTALK")
//                .setContentText("同步进度")
                .setContentIntent(pendingIntent)
                .setTicker("ticker")
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(NOTIFICATION_STATUS_ID, notificationBuilder.build());
        notificationRunning = true;
    }

    private RemoteViews createRemoteViews() {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_status);

        Intent closeButton = new Intent("EACTALK_CLOSE");
        closeButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent closeBroadcast = PendingIntent.getBroadcast(this, 1, closeButton, Intent.FILL_IN_ACTION);
        remoteViews.setOnClickPendingIntent(R.id.status_eac_close, closeBroadcast);
        return remoteViews;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    public synchronized void startIpfs() {
        // 是否仅在 wifi 状态下开启 ipfs
        boolean isIpfsWifi = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("wifi", true);
        if ((isIpfsWifi && !NetworkUtils.isWifiConnected()) || !NetworkUtils.isConnected()) {
            MyUtils.log("stop from startIpfs " + isIpfsWifi + " " + NetworkUtils.isWifiConnected() + " " + NetworkUtils.isConnected());
            stopIpfs();
            return;
        }
        if (ipfsThread != null) {
            if (ipfsRunning) {
                return;
            }
            ipfsThread.interrupt();
            ipfsThread = null;
            ipfsRunning = false;
        }

        ipfsThread = new Thread() {
            @Override
            public void run() {
                try {
                    IPFS ipfs = new IPFS(getApplicationContext());
                    ipfs.setBootstrap("[\n" +
                            "        \"/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ\",\n" +
                            "        \"/ip4/104.131.131.82/udp/4001/quic/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ\",\n" +
                            "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN\",\n" +
                            "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmQCU2EcMqAqQPR2i9bChDtGNJchTbq5TbXJJ16u19uLTa\",\n" +
                            "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmbLHAnMoJPWSCR5Zhtx6BHJX9KiKNN6tpvbUcqanj75Nb\",\n" +
                            "        \"/dnsaddr/bootstrap.libp2p.io/p2p/QmcZf59bWwK5XFi76CZX8cbJ4BhTzzA3gU1ZjYZcYW3dwt\",\n" +
                            "        \"/ip4/171.221.247.27/tcp/4001/p2p/12D3KooWEKb4mFUB6uTE4vR6GADtXbmzaHYoor1uLvEgedb2pUzk\",\n" +
                            "        \"/ip4/171.221.247.27/udp/1200/quic/p2p/12D3KooWEKb4mFUB6uTE4vR6GADtXbmzaHYoor1uLvEgedb2pUzk\",\n" +
                            "        \"/ip4/171.221.247.27/udp/4001/quic/p2p/12D3KooWEKb4mFUB6uTE4vR6GADtXbmzaHYoor1uLvEgedb2pUzk\",\n" +
                            "        \"/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3\",\n" +
                            "        \"/ip4/39.108.226.205/udp/4001/quic/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3\"\n" +
                            "    ]");

                    String storageMax = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("storage", "10");
                    ipfs.setConfigKey("Datastore", new JSONObject("{\n" +
                            "        \"BloomFilterSize\": 0,\n" +
                            "        \"GCPeriod\": \"1h\",\n" +
                            "        \"HashOnRead\": false,\n" +
                            "        \"Spec\": {\n" +
                            "            \"mounts\": [\n" +
                            "                {\n" +
                            "                    \"child\": {\n" +
                            "                        \"path\": \"blocks\",\n" +
                            "                        \"shardFunc\": \"/repo/flatfs/shard/v1/next-to-last/2\",\n" +
                            "                        \"sync\": true,\n" +
                            "                        \"type\": \"flatfs\"\n" +
                            "                    },\n" +
                            "                    \"mountpoint\": \"/blocks\",\n" +
                            "                    \"prefix\": \"flatfs.datastore\",\n" +
                            "                    \"type\": \"measure\"\n" +
                            "                },\n" +
                            "                {\n" +
                            "                    \"child\": {\n" +
                            "                        \"compression\": \"none\",\n" +
                            "                        \"path\": \"datastore\",\n" +
                            "                        \"type\": \"levelds\"\n" +
                            "                    },\n" +
                            "                    \"mountpoint\": \"/\",\n" +
                            "                    \"prefix\": \"leveldb.datastore\",\n" +
                            "                    \"type\": \"measure\"\n" +
                            "                }\n" +
                            "            ],\n" +
                            "            \"type\": \"mount\"\n" +
                            "        },\n" +
                            "        \"StorageGCWatermark\": 90,\n" +
                            "        \"StorageMax\": \"" + storageMax + "GB\"\n" +
                            "    }"));
                    ipfs.start();

                    IpfsManager.getInstance().setIpfs(ipfs);

                    ArrayList<JSONObject> jsonList = ipfs.newRequest("id").sendToJSONList();

                    IpfsManager.getInstance().setPeerID(jsonList.get(0).getString("ID"));

                    statusIpfsText = "IPFS: " + IpfsManager.getInstance().getPeerID();
                    updateNotification();

                    ipfsRunning = true;

                    String ipfsNode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ipfs_node", defaultIPFSNode);

                    byte[] bootStrapData = ipfs.newRequest("bootstrap")
                            .withArgument("add")
                            .withArgument(ipfsNode)
                            .send();

                    Log.d(TAG, "onClick: bootStrapData " + new String(bootStrapData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        ipfsThread.start();
    }

    public synchronized void stopIpfs() {
        MyUtils.log("stopIpfs");
        if (ipfsThread != null) {
            ipfsThread.interrupt();
            ipfsThread = null;
            ipfsRunning = false;
        }
        statusIpfsText = getApplicationContext().getString(R.string.ipfs_close);
        IpfsManager.getInstance().stop();
    }

    public void restartIpfs() {
        stopIpfs();
        startIpfs();
    }

    public synchronized void startEac() {
        if (eacThread != null) {
            if (eacRunning) {
                return;
            }
            eacThread.interrupt();
            eacThread = null;
            eacRunning = false;
        }
        isEacSyncFinish = false;
        eacThread = new Thread() {
            @Override
            public void run() {
                eacRunning = true;

                while (eacRunning) {
                    int startHeight = BRSharedPrefs.getStartHeight(getApplicationContext());
                    double progressStatus = BRPeerManager.syncProgress(startHeight);
                    if (progressStatus == 1) {
                        eacRunning = false;
                    } else {
                        long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;
                        statusEacProgress = progressStatus * 100;
                        statusEacText = getString(R.string.eac_syncing) + Utils.formatTimeStamp(lastBlockTimeStamp, "yyyy/MM/dd HH:mm:ss");
                    }
                    updateNotification();

                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        eacThread.start();
    }

    public synchronized void stopEac() {
        if (eacThread != null) {
            eacThread.interrupt();
            eacThread = null;
            eacRunning = false;
        }
    }

    public void setBinding(FragmentMainHomeBinding binding) {
        this.binding = binding;
    }

    public void setBindingActivityActivity(FragmentActivity bindingActivity) {
        this.bindingActivity = bindingActivity;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals("EACTALK_CLOSE")) {
            stopAll();
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopAll() {
        if (notificationRunning) {
            stopForeground(true);
            stopEac();
            stopIpfs();
            unregisterReceiver(myReceiver);
            notificationRunning = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAll();
    }

    public class EacBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }
}
