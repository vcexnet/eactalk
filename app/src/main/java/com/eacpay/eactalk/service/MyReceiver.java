package com.eacpay.eactalk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;

import com.blankj.utilcode.util.NetworkUtils;
import com.eacpay.tools.manager.BREventManager;
import com.eacpay.wallet.BRPeerManager;

public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "oldfeel";
    OnConnectChangedListener onConnectionChanged;

    public void setOnConnectionChanged(OnConnectChangedListener onConnectionChanged) {
        this.onConnectionChanged = onConnectionChanged;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent service = new Intent(context, MyService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } else if (intent.getAction().equals("EACTALK_CLOSE")) {
            Intent service = new Intent(context, MyService.class);
            service.setAction("EACTALK_CLOSE");
            context.startService(service);
        } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean connected = NetworkUtils.isConnected();
            BRPeerManager.getInstance().networkChanged(connected);
            BREventManager.getInstance().pushEvent(connected ? "reachability.isReachble" : "reachability.isNotReachable");
            if (onConnectionChanged != null) {
                onConnectionChanged.onConnectionChanged();
            }
        }
    }

    public interface OnConnectChangedListener {
        void onConnectionChanged();
    }
}
