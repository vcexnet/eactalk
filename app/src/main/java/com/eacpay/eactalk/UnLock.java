package com.eacpay.eactalk;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityUnLockBinding;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.animation.SpringAnimator;
import com.eacpay.tools.manager.AnalyticsManager;
import com.eacpay.tools.security.AuthManager;
import com.eacpay.tools.security.BRKeyStore;
import com.eacpay.tools.util.BRConstants;

public class UnLock extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityUnLockBinding binding;
    private String pin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUnLockBinding.inflate(getLayoutInflater());
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(binding.getRoot());

        binding.unLockContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String pin = binding.unLockContent.getText().toString();
                if (pin.length() >= 6) {
                    unLock();
                }
            }
        });

        binding.unLockContent.setFocusable(true);
        binding.unLockContent.setFocusableInTouchMode(true);
        binding.unLockContent.requestFocus();

        pin = BRKeyStore.getPinCode(this);
        if (pin.isEmpty() || (pin.length() != 6 && pin.length() != 4)) {
            Intent intent = new Intent(this, SetPin.class);
            intent.putExtra("noPin", true);
            startActivity(intent);
            if (!UnLock.this.isDestroyed()) finish();
            return;
        }
    }

    private void unLock() {
        if (AuthManager.getInstance().checkAuth(binding.unLockContent.getText().toString(), UnLock.this)) {
            AuthManager.getInstance().authSuccess(UnLock.this);
            unlockWallet();
            AnalyticsManager.logCustomEvent(BRConstants._20200217_DLWP);
        } else {
            AuthManager.getInstance().authFail(UnLock.this);
            showFailedToUnlock();
        }
    }

    private void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(UnLock.this, binding.unLockContent);
        binding.unLockContent.setText("");
        Toast.makeText(this, R.string.pin_error, Toast.LENGTH_SHORT).show();
    }

    private void unlockWallet() {
        Intent intent = new Intent(UnLock.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
        if (!UnLock.this.isDestroyed()) {
            UnLock.this.finish();
        }
    }
}
