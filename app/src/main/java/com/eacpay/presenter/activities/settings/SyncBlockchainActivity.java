package com.eacpay.presenter.activities.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.eacpay.R;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.customviews.BRDialogView;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.animation.BRDialog;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.wallet.BRPeerManager;


public class SyncBlockchainActivity extends BRActivity {
    private static final String TAG = SyncBlockchainActivity.class.getName();
    private Button scanButton;
    public static boolean appVisible = false;
    private static SyncBlockchainActivity app;

    public static SyncBlockchainActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_blockchain);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);
        //TODO: all views are using the layout of this button. Views should be refactored without it
        // Hiding until layouts are built.

        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRDialog.showCustomDialog(SyncBlockchainActivity.this, getString(R.string.ReScan_alertTitle),
                        getString(R.string.ReScan_footer), getString(R.string.ReScan_alertAction), getString(R.string.Button_cancel),
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRSharedPrefs.putStartHeight(SyncBlockchainActivity.this, 0);
                                        BRSharedPrefs.putAllowSpend(SyncBlockchainActivity.this, false);
                                        BRPeerManager.getInstance().rescan();
                                        BRAnimator.startMainActivity(SyncBlockchainActivity.this, false);

                                    }
                                });
                            }
                        }, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, 0);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
