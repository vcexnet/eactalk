package com.eacpay.eactalk;

import static com.eacpay.presenter.activities.SetPinActivity.introSetPitActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.eacpay.R;
import com.eacpay.databinding.BottomSheetSelectSendTypeBinding;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.HProgressDialogUtils;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.ReEnterPinActivity;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.fragments.FragmentManage;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.manager.TxManager;
import com.eacpay.tools.security.BitcoinUrlHandler;
import com.eacpay.tools.sqlite.TransactionDataSource;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRConstants;
import com.eacpay.wallet.BRPeerManager;
import com.eacpay.wallet.BRWalletManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationBarView;
import com.pgyer.pgyersdk.PgyerSDKManager;
import com.platform.APIClient;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.service.OnFileDownloadListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends BRActivity implements BRWalletManager.OnBalanceChanged,
        BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener,
        TransactionDataSource.OnTxAddedListener, FragmentManage.OnNameChanged {

    private static final String TAG = "oldfeel";

    public static final Point screenParametersPoint = new Point();

    public static boolean appVisible = false;
    private String savedFragmentTag;

    private static MainActivity app;
    private NavController controller;
    private BottomNavigationView bottomNavigationView;
    private String tagName;
    private String downloadUrl;
    private boolean isFirst = true;

    public static MainActivity getApp() {
        return app;
    }

    MyService eacService;
    boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().setOnTxStatusUpdate(this);
        BRSharedPrefs.addIsoChangedListener(this);

        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        BRAnimator.init(this);

        if (introSetPitActivity != null) introSetPitActivity.finish();
//        if (introActivity != null) introActivity.finish();
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

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_container);

        bottomNavigationView = findViewById(R.id.bottom_nav);

        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.getNavController());

        PgyerSDKManager.checkSoftwareUpdate(this);

        controller = navHostFragment.getNavController();
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_main_home:
                        controller.navigate(R.id.nav_main_home);
                        break;
                    case R.id.nav_main_message:
                        controller.navigate(R.id.nav_main_message);
                        break;
                    case R.id.nav_main_send:
                        selectSendType();
                        break;
                    case R.id.nav_main_contact:
                        controller.navigate(R.id.nav_main_contact);
                        break;
                    case R.id.nav_main_mine:
                        controller.navigate(R.id.nav_main_mine);
                        break;
                }
                return item.getItemId() != R.id.nav_main_send;
            }
        });

        updateBadge();

//        bottomNavigationView.setSelectedItemId(R.id.nav_main_mine);
//        openActivity(ApiSettings.class);

        if (BRSharedPrefs.getBoolean(this, "auto_update", false)) {
            autoUpdate();
        }
    }

    private void autoUpdate() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/vcexnet/eactalk/releases/latest")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MyUtils.log("getVersionInfoFromGithub fail");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.get_version_info_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                MyUtils.log("getVersionInfoFromGithub success");
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    tagName = jsonObject.getString("tag_name");
                    JSONArray assetsArray = jsonObject.getJSONArray("assets");
                    for (int i = 0; i < assetsArray.length(); i++) {
                        JSONObject assetsObject = assetsArray.getJSONObject(i);
                        if (assetsObject.getString("name").equals("eactalk.apk")) {
                            downloadUrl = assetsObject.getString("browser_download_url");
                            break;
                        }
                    }

                    if (tagName != null && !tagName.equals(AppUtils.getAppVersionName()) && downloadUrl != null) {
                        startUpdate();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startUpdate() {
        XUpdate.newBuild(MainActivity.this)
                .apkCacheDir(PathUtils.getExternalAppCachePath())
                .build()
                .download(downloadUrl, new OnFileDownloadListener() {
                    @Override
                    public void onStart() {
                        MyUtils.log("update onStart");
                        HProgressDialogUtils.showHorizontalProgressDialog(MainActivity.this, getString(R.string.updating), false);
                    }

                    @Override
                    public void onProgress(float progress, long total) {
                        MyUtils.log("update onProgress " + progress + " " + total);
                        HProgressDialogUtils.setProgress(Math.round(progress * 100));
                    }

                    @Override
                    public boolean onCompleted(File file) {
                        MyUtils.log("update onCompleted " + file.getPath());
                        HProgressDialogUtils.cancel();
                        ToastUtils.showLong(getString(R.string.apk_download_success) + file.getPath());
                        AppUtils.installApp(file);
                        return false;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        MyUtils.log("update onError " + throwable.getMessage());
                        HProgressDialogUtils.cancel();
                        if (!isFirst) {
                            ToastUtils.showLong(R.string.apk_download_fail);
                        }
                        if (isFirst) {
                            isFirst = false;
                            downloadUrl = "https://www.eacpay.com/download/eactalk.apk";
                            startUpdate();
                        }
                    }
                });
    }

    private void selectSendType() {
        BottomSheetSelectSendTypeBinding selectSendTypeBinding = BottomSheetSelectSendTypeBinding.inflate(getLayoutInflater());
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(selectSendTypeBinding.getRoot());
        dialog.show();
        // Remove default white color background
        FrameLayout bottomSheet = dialog.findViewById(R.id.design_bottom_sheet);
        bottomSheet.setBackground(null);

        selectSendTypeBinding.sendNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                Intent intent = new Intent(MainActivity.this, SendMessage.class);
                intent.putExtra("type", "normal");
                startActivity(intent);
            }
        });

        selectSendTypeBinding.sendNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                Intent intent = new Intent(MainActivity.this, SendMessage.class);
                intent.putExtra("type", "notice");
                startActivity(intent);
            }
        });

        selectSendTypeBinding.sendQrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                BRAnimator.openScanner(MainActivity.this, BRConstants.REQUEST_SEND_MESSAGE);
            }
        });

        selectSendTypeBinding.sendCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
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

        updateBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        saveVisibleFragment();
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyService.EacBinder binder = (MyService.EacBinder) iBinder;
            eacService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        isBound = false;
    }

    public MyService getEacService() {
        return eacService;
    }

    public boolean isBound() {
        return isBound;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BRPeerManager.getInstance().setOnTxStatusUpdate(null);
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

    long clickTime = System.currentTimeMillis();

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - clickTime > 2000) {
            Toast.makeText(this, getString(R.string.reclick_exit), Toast.LENGTH_SHORT).show();
            clickTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public void onStatusUpdate() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(MainActivity.this);
            }
        });

        updateBadge();
    }

    public void updateBadge() {
        if (bottomNavigationView == null) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                final long unReadCount = TransactionDataSource.getInstance(MainActivity.this).unReadCount();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_main_message);
                        if (unReadCount > 9) {
                            badge.clearNumber();
                            badge.setVisible(true);
                        } else if (unReadCount > 0) {
                            badge.setNumber((int) unReadCount);
                            badge.setVisible(true);
                        } else {
                            badge.setVisible(false);
                        }
                    }
                });
            }
        }.start();
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
                    BRAnimator.openScanner(this, BRConstants.REQUEST_SEND_MESSAGE);
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
}
