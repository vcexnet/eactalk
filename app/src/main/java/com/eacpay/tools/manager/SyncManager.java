package com.eacpay.tools.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.eacpay.databinding.FragmentMainHomeBinding;
import com.eacpay.eactalk.MainActivity;
import com.eacpay.tools.listeners.SyncReceiver;
import com.eacpay.tools.util.Utils;
import com.eacpay.wallet.BRPeerManager;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class SyncManager {
    private static final String TAG = "oldfeel";
    private static SyncManager instance;
    private static final long SYNC_PERIOD = TimeUnit.HOURS.toMillis(24);
    private static SyncProgressTask syncTask;
    public boolean running;
    private FragmentMainHomeBinding binding;

    public static SyncManager getInstance() {
        if (instance == null) instance = new SyncManager();
        return instance;
    }

    private SyncManager() {
    }

    private void createAlarm(Context app, long time) {
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(app, SyncReceiver.class);
        intent.setAction(SyncReceiver.SYNC_RECEIVER);//my custom string action name
        PendingIntent pendingIntent = PendingIntent.getService(app, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, time, time + TimeUnit.MINUTES.toMillis(1), pendingIntent);//first start will start asap
    }

    public synchronized void updateAlarms(Context app) {
        createAlarm(app, System.currentTimeMillis() + SYNC_PERIOD);
    }

    public synchronized void startSyncingProgressThread() {
        Timber.d("startSyncingProgressThread:%s", Thread.currentThread().getName());

        try {
            if (syncTask != null) {
                if (running) {
                    Timber.d("startSyncingProgressThread: syncTask.running == true, returning");
                    return;
                }
                syncTask.interrupt();
                syncTask = null;
            }
            syncTask = new SyncProgressTask();
            syncTask.start();
        } catch (IllegalThreadStateException ex) {
            Timber.e(ex);
        }
    }

    public synchronized void stopSyncingProgressThread() {
        Timber.d("stopSyncingProgressThread");
        final MainActivity ctx = MainActivity.getApp();
        if (ctx == null) {
            Timber.i("stopSyncingProgressThread: ctx is null");
            return;
        }
        try {
            if (syncTask != null) {
                syncTask.interrupt();
                syncTask = null;
            }
        } catch (Exception ex) {
            Timber.e(ex);
        }
    }

    public void setBinding(FragmentMainHomeBinding binding) {
        this.binding = binding;
    }

    private class SyncProgressTask extends Thread {
        public double progressStatus = 0;
        private MainActivity app;

        public SyncProgressTask() {
            progressStatus = 0;
        }

        @Override
        public void run() {
            if (running) return;
            try {
                app = MainActivity.getApp();
                progressStatus = 0;
                running = true;
                Timber.d("run: starting: %s", progressStatus);

                if (app != null) {
                    final long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (binding != null) {
                                binding.mainHomeSyncView.setVisibility(View.VISIBLE);
                                binding.mainHomeSyncProgress.setProgress((int) (progressStatus * 100));
                                binding.mainHomeSyncTime.setText("正在同步 " + Utils.formatTimeStamp(lastBlockTimeStamp, "yyyy/MM/dd HH:mm:ss"));
                            }
                        }
                    });
                }

                while (running) {
                    if (app != null) {
                        int startHeight = BRSharedPrefs.getStartHeight(app);
                        progressStatus = BRPeerManager.syncProgress(startHeight);
                        if (progressStatus == 1) {
                            running = false;
                            continue;
                        }
                        Log.e(TAG, "run: progressStatus" + progressStatus);
                        final long lastBlockTimeStamp = BRPeerManager.getInstance().getLastBlockTimestamp() * 1000;
                        app.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (TxManager.getInstance().currentPrompt != PromptManager.PromptItem.SYNCING) {
                                    Timber.d("run: currentPrompt != SYNCING, showPrompt(SYNCING) ....");
                                    TxManager.getInstance().showPrompt(app, PromptManager.PromptItem.SYNCING);
                                }

                                if (binding != null) {
                                    binding.mainHomeSyncView.setVisibility(View.VISIBLE);
                                    binding.mainHomeSyncProgress.setProgress((int) (progressStatus * 100));
                                    binding.mainHomeSyncTime.setText("正在同步 " + Utils.formatTimeStamp(lastBlockTimeStamp, "yyyy/MM/dd HH:mm:ss"));
                                }
                            }
                        });

                    } else {
                        app = MainActivity.getApp();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Timber.e(e, "run: Thread.sleep was Interrupted:%s", Thread.currentThread().getName());
                    }
                }
                Timber.d("run: SyncProgress task finished:%s", Thread.currentThread().getName());
            } finally {
                running = false;
                progressStatus = 0;
                if (app != null)
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TxManager.getInstance().hidePrompt(app, PromptManager.PromptItem.SYNCING);
                        }
                    });
            }
        }
    }
}
