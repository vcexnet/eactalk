package com.eacpay.eactalk;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityStartBinding;
import com.eacpay.presenter.activities.intro.RecoverActivity;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.security.BRKeyStore;
import com.eacpay.tools.security.PostAuth;
import com.eacpay.tools.security.SmartValidator;
import com.eacpay.wallet.BRWalletManager;

public class StartActivity extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityStartBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartBinding.inflate(getLayoutInflater());

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(binding.getRoot());

        binding.satartCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isAgree = binding.startIsAgree.isChecked();
                if (!isAgree) {
                    Toast.makeText(StartActivity.this, R.string.agree_listen_privacy, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!BRAnimator.isClickAllowed()) return;
                MainActivity bApp = MainActivity.getApp();
                if (bApp != null) bApp.finish();
                Intent intent = new Intent(StartActivity.this, SetPin.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        binding.startRecover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                MainActivity bApp = MainActivity.getApp();
                if (bApp != null) bApp.finish();
                Intent intent = new Intent(StartActivity.this, RecoverActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        binding.startListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(Listen.class);
            }
        });

        byte[] masterPubKey = BRKeyStore.getMasterPublicKey(this);
        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = SmartValidator.checkFirstAddress(this, masterPubKey);
        }
        if (!isFirstAddressCorrect) {
            BRWalletManager.getInstance().wipeWalletButKeystore(this);
        }

        PostAuth.getInstance().onCanaryCheck(this, false);
    }
}
