//
//package com.eacpay.presenter.activities.intro;
//
//import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.graphics.Point;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//
//import com.eacpay.BuildConfig;
//import com.eacpay.R;
//
//import com.eacpay.eactalk.MainActivity;
//import com.eacpay.eactalk.SetPin;
//import com.eacpay.presenter.activities.SetPinActivity;
//import com.eacpay.presenter.activities.util.BRActivity;
//import com.eacpay.presenter.customviews.BRText;
//import com.eacpay.tools.animation.BRAnimator;
//import com.eacpay.tools.security.BRKeyStore;
//import com.eacpay.tools.security.PostAuth;
//import com.eacpay.tools.security.SmartValidator;
//import com.eacpay.tools.threads.BRExecutor;
//import com.eacpay.tools.util.Utils;
//import com.eacpay.wallet.BRWalletManager;
//import com.platform.APIClient;
//
//import java.io.Serializable;
//import java.util.Locale;
//
//import timber.log.Timber;
//
//public class IntroActivity extends BRActivity implements Serializable {
//    public Button newWalletButton;
//    public Button recoverWalletButton;
//    public static IntroActivity introActivity;
//    public static boolean appVisible = false;
//    private static IntroActivity app;
//    private BRText versionText;
//
//
//    public static IntroActivity getApp() {
//        return app;
//    }
//
//    public static final Point screenParametersPoint = new Point();
//
//    @Override
//    protected void onRestart() {
//        super.onRestart();  // Always call the superclass method first
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_intro);
//        newWalletButton = (Button) findViewById(R.id.button_new_wallet);
//        recoverWalletButton = (Button) findViewById(R.id.button_recover_wallet);
//        versionText = findViewById(R.id.version_text);
//        setListeners();
//        updateBundles();
////        SyncManager.getInstance().updateAlarms(this);
//
//        if (!BuildConfig.DEBUG && BRKeyStore.AUTH_DURATION_SEC != 300) {
//            RuntimeException ex = new RuntimeException("onCreate: AUTH_DURATION_SEC should be 300");
//            Timber.e(ex);
//            throw ex;
//        }
//        introActivity = this;
//
//        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
//
//        PackageInfo pInfo = null;
//        try {
//            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            Timber.e(e);
//        }
//        String verName = pInfo != null ? pInfo.versionName : " ";
//        versionText.setText(String.format(Locale.US, "%1$s", verName));
//
//        if (Utils.isEmulatorOrDebug(this))
//            Utils.printPhoneSpecs();
//
//        byte[] masterPubKey = BRKeyStore.getMasterPublicKey(this);
//        boolean isFirstAddressCorrect = false;
//        if (masterPubKey != null && masterPubKey.length != 0) {
//            isFirstAddressCorrect = SmartValidator.checkFirstAddress(this, masterPubKey);
//        }
//        if (!isFirstAddressCorrect) {
//            BRWalletManager.getInstance().wipeWalletButKeystore(this);
//        }
//
//        PostAuth.getInstance().onCanaryCheck(this, false);
//    }
//
//    private void updateBundles() {
//        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
//            @Override
//            public void run() {
//                Thread.currentThread().setName("updateBundle");
//                final long startTime = System.currentTimeMillis();
//                APIClient apiClient = APIClient.getInstance(IntroActivity.this);
//                apiClient.updateBundle();
//                long endTime = System.currentTimeMillis();
//                Timber.d("updateBundle DONE in %sms", endTime - startTime);
//            }
//        });
//    }
//
//
//    private void setListeners() {
//        newWalletButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!BRAnimator.isClickAllowed()) return;
//                MainActivity bApp = MainActivity.getApp();
//                if (bApp != null) bApp.finish();
//                Intent intent = new Intent(IntroActivity.this, SetPin.class);
//                startActivity(intent);
//                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//            }
//        });
//
//        recoverWalletButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!BRAnimator.isClickAllowed()) return;
//                MainActivity bApp = MainActivity.getApp();
//                if (bApp != null) bApp.finish();
//                Intent intent = new Intent(IntroActivity.this, RecoverActivity.class);
//                startActivity(intent);
//                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//            }
//        });
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        appVisible = true;
//        app = this;
//
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        appVisible = false;
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//    }
//
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//
//    }
//
//}
