package com.eacpay.eactalk;

import static com.eacpay.presenter.activities.SetPinActivity.introSetPitActivity;
import static com.eacpay.presenter.activities.intro.IntroActivity.introActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.eacpay.R;
import com.eacpay.presenter.activities.ReEnterPinActivity;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.fragments.FragmentManage;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.manager.InternetManager;
import com.eacpay.tools.manager.SyncManager;
import com.eacpay.tools.manager.TxManager;
import com.eacpay.tools.security.BitcoinUrlHandler;
import com.eacpay.tools.sqlite.TransactionDataSource;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRConstants;
import com.eacpay.wallet.BRPeerManager;
import com.eacpay.wallet.BRWalletManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.platform.APIClient;

import timber.log.Timber;

public class MainActivity extends BRActivity implements BRWalletManager.OnBalanceChanged,
        BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener,
        TransactionDataSource.OnTxAddedListener, FragmentManage.OnNameChanged, InternetManager.ConnectionReceiverListener {

    private static final String TAG = "oldfeel";

    public static final Point screenParametersPoint = new Point();

    private InternetManager mConnectionReceiver;

    public static boolean appVisible = false;
    private String savedFragmentTag;

    private static MainActivity app;

    public static MainActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        BRSharedPrefs.addIsoChangedListener(this);

        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        BRAnimator.init(this);

        if (introSetPitActivity != null) introSetPitActivity.finish();
        if (introActivity != null) introActivity.finish();
        if (ReEnterPinActivity.reEnterPinActivity != null)
            ReEnterPinActivity.reEnterPinActivity.finish();

        if (!BRSharedPrefs.getGreetingsShown(MainActivity.this))
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BRAnimator.showGreetingsMessage(MainActivity.this);
                    BRSharedPrefs.putGreetingsShown(MainActivity.this, true);
                }
            }, 1000);


        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_container);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);

        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.getNavController());
    }

    private void setUrlHandler(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;
        String scheme = data.getScheme();
        if (scheme != null && (scheme.startsWith("earthcoin") || scheme.startsWith("bitid"))) {
            String str = intent.getDataString();
            BitcoinUrlHandler.processRequest(this, str);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setUrlHandler(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        app = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        if (BRConstants.PLATFORM_ON)
            APIClient.getInstance(this).updatePlatform();

        setupNetworking();

        if (!BRWalletManager.getInstance().isCreated()) {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BRWalletManager.getInstance().initWallet(MainActivity.this);
                }
            });
        }

        BRWalletManager.getInstance().refreshBalance(this);

        BRAnimator.showFragmentByTag(this, savedFragmentTag);
        savedFragmentTag = null;
        TxManager.getInstance().onResume(MainActivity.this);

    }

    private void setupNetworking() {
        if (mConnectionReceiver == null) mConnectionReceiver = InternetManager.getInstance();
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, mNetworkStateFilter);
        InternetManager.addConnectionListener(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        saveVisibleFragment();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mConnectionReceiver);

        //sync the kv stores
        if (BRConstants.PLATFORM_ON) {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    APIClient.getInstance(MainActivity.this).syncKvStore();
                }
            });
        }

    }

    private void saveVisibleFragment() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            return;
        }
        savedFragmentTag = getFragmentManager().getBackStackEntryAt(0).getName();
    }

    @Override
    public void onBalanceChanged(final long balance) {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onStatusUpdate() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(MainActivity.this);
            }
        });

    }

    @Override
    public void onIsoChanged(String iso) {
    }

    @Override
    public void onTxAdded() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(MainActivity.this);
            }
        });
        BRWalletManager.getInstance().refreshBalance(MainActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BRAnimator.openScanner(this, BRConstants.SCANNER_REQUEST);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onNameChanged(String name) {
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (isConnected) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(MainActivity.this));
                    Timber.d("Sync Progress: %s", progress);
                    if (progress < 1 && progress > 0) {
                        SyncManager.getInstance().startSyncingProgressThread();
                    }
                }
            });

        } else {
            SyncManager.getInstance().stopSyncingProgressThread();
        }

    }
}
