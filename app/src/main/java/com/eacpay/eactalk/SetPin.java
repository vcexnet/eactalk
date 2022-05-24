package com.eacpay.eactalk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.eacpay.R;
import com.eacpay.databinding.ActivitySetPinBinding;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.interfaces.BROnSignalCompletion;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.security.AuthManager;
import com.eacpay.tools.security.PostAuth;

public class SetPin extends BRActivity {
    ActivitySetPinBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetPinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.set_pin));

        binding.setPinContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String pin = binding.setPinContent.getText().toString();
                if (pin.length() >= 6) {
                    setPinSuccess();
//                    Intent intent = new Intent(SetPin.this, ReEnterPin.class);
//                    intent.putExtra("pin", pin);
//                    intent.putExtra("noPin", getIntent().getBooleanExtra("noPin", false));
//                    startActivity(intent);
                }
            }
        });

        binding.setPinContent.setFocusable(true);
        binding.setPinContent.setFocusableInTouchMode(true);
        binding.setPinContent.requestFocus();
    }

    private void setPinSuccess() {
        binding.setPinContent.setEnabled(false);
        new AlertDialog.Builder(this, R.style.my_dialog)
                .setTitle(R.string.set_complete)
                .setMessage(R.string.set_pin_success)
                .setPositiveButton(R.string.i_known, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AuthManager.getInstance().setPinCode(binding.setPinContent.getText().toString(), SetPin.this);
                        if (getIntent().getBooleanExtra("noPin", false)) {
                            BRAnimator.startMainActivity(SetPin.this, false);
                        } else {
                            BRAnimator.showBreadSignal(SetPin.this, getString(R.string.Alerts_pinSet), getString(R.string.UpdatePin_createInstruction), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                @Override
                                public void onComplete() {
                                    PostAuth.getInstance().onCreateWalletAuth(SetPin.this, false);
                                }
                            });
                        }
                    }
                })
                .show();
    }
}
