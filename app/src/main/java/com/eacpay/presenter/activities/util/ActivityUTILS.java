package com.eacpay.presenter.activities.util;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

import com.eacpay.R;
import com.eacpay.eactalk.InputWord;
import com.eacpay.presenter.activities.DisabledActivity;
import com.eacpay.presenter.activities.SetPinActivity;

import java.util.List;

import timber.log.Timber;

public class ActivityUTILS {

    private static void setStatusBarColor(Activity app, int color) {
        if (app == null) return;
        Window window = app.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(app.getColor(color));
    }

    //return true if the app does need to show the disabled wallet screen
    public static boolean isAppSafe(Activity app) {
        return app instanceof SetPinActivity || app instanceof InputWord;
    }

    public static void showWalletDisabled(Activity app) {
        Intent intent = new Intent(app, DisabledActivity.class);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
        Timber.d("showWalletDisabled: %s", app.getClass().getName());
    }

    public static boolean isLast(Activity app) {
        ActivityManager mngr = (ActivityManager) app.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);

        return taskList.get(0).numActivities == 1 &&
                taskList.get(0).topActivity.getClassName().equals(app.getClass().getName());
    }

    public static boolean isMainThread() {
        boolean isMain = Looper.myLooper() == Looper.getMainLooper();
        if (isMain) {
            Timber.d("IS MAIN UI THREAD!");
        }
        return isMain;
    }
}
