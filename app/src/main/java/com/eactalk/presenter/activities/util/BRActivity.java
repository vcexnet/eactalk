package com.eactalk.presenter.activities.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

import com.eactalk.EactalkApp;
import com.eactalk.presenter.activities.DisabledActivity;
import com.eactalk.presenter.activities.intro.IntroActivity;
import com.eactalk.presenter.activities.intro.RecoverActivity;
import com.eactalk.presenter.activities.intro.WriteDownActivity;
import com.eactalk.tools.animation.BRAnimator;
import com.eactalk.tools.manager.BRApiManager;
import com.eactalk.tools.manager.InternetManager;
import com.eactalk.tools.security.AuthManager;
import com.eactalk.tools.security.BRKeyStore;
import com.eactalk.tools.security.BitcoinUrlHandler;
import com.eactalk.tools.security.PostAuth;
import com.eactalk.tools.threads.BRExecutor;
import com.eactalk.tools.util.BRConstants;
import com.eactalk.wallet.BRWalletManager;
import com.platform.HTTPServer;
import com.platform.tools.BRBitId;

import timber.log.Timber;
public class BRActivity extends Activity {

    static {
        System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EactalkApp.activityCounter.decrementAndGet();
        EactalkApp.onStop(this);
        EactalkApp.backgroundedTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        init(this);
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // 123 is the qrCode result
        switch (requestCode) {
            case BRConstants.PAY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPublishTxAuth(BRActivity.this, true);
                        }
                    });
                }
                break;
            case BRConstants.REQUEST_PHRASE_BITID:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onBitIDAuth(BRActivity.this, true);

                }
                break;

            case BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onPaymentProtocolRequest(this, true);
                }
                break;

            case BRConstants.CANARY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onCanaryCheck(this, true);
                } else {
                    finish();
                }
                break;

            case BRConstants.SHOW_PHRASE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onPhraseCheckAuth(this, true);
                }
                break;
            case BRConstants.PROVE_PHRASE_REQUEST:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onPhraseProveAuth(this, true);
                }
                break;
            case BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onRecoverWalletAuth(this, true);
                } else {
                    finish();
                }
                break;

            case BRConstants.SCANNER_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String result = data.getStringExtra("result");
                            if (BitcoinUrlHandler.isBitcoinUrl(result))
                                BitcoinUrlHandler.processRequest(BRActivity.this, result);
                            else if (BRBitId.isBitId(result))
                                BRBitId.signBitID(BRActivity.this, result, null);
                            else
                                Timber.i("onActivityResult: not earthcoin address NOR bitID");
                        }
                    }, 500);

                }
                break;
            case BRConstants.SCANNER_BCH_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String result = data.getStringExtra("result");
                            PostAuth.getInstance().onSendBch(BRActivity.this, true, result);
                        }
                    }, 500);

                }
                break;

            case BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onCreateWalletAuth(this, true);
                } else {
                    Timber.d("WARNING: resultCode != RESULT_OK");
                    BRWalletManager m = BRWalletManager.getInstance();
                    m.wipeWalletButKeystore(this);
                    finish();
                }
                break;
        }
    }

    public static void init(Activity app) {
        InternetManager.getInstance();
        if (!(app instanceof IntroActivity || app instanceof RecoverActivity || app instanceof WriteDownActivity))
            BRApiManager.getInstance().startTimer(app);
        //show wallet locked if it is
        if (!ActivityUTILS.isAppSafe(app))
            if (AuthManager.getInstance().isWalletDisabled(app))
                AuthManager.getInstance().setWalletDisabled(app);

        EactalkApp.activityCounter.incrementAndGet();
        EactalkApp.setBreadContext(app);
        //lock wallet if 3 minutes passed
        if (EactalkApp.backgroundedTime != 0 && (System.currentTimeMillis() - EactalkApp.backgroundedTime >= 180 * 1000) && !(app instanceof DisabledActivity)) {
            if (!BRKeyStore.getPinCode(app).isEmpty()) {
                BRAnimator.startBreadActivity(app, true);
            }
        }
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                HTTPServer.startServer();
            }
        });
        EactalkApp.backgroundedTime = System.currentTimeMillis();
    }
}
