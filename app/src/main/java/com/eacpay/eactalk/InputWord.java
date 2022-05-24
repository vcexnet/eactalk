package com.eacpay.eactalk;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.eacpay.R;
import com.eacpay.databinding.ActivityInputWordBinding;
import com.eacpay.presenter.activities.SetPinActivity;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.animation.SpringAnimator;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.security.AuthManager;
import com.eacpay.tools.security.PostAuth;
import com.eacpay.tools.security.SmartValidator;
import com.eacpay.tools.util.Utils;
import com.eacpay.wallet.BRWalletManager;

public class InputWord extends BRActivity {
    ActivityInputWordBinding binding;
    private boolean restore = false;
    private boolean resetPin = false;

//    private String debugPhrase = "到 挺 困 刑 实 苦 拜 芽 症 优 移 落";

    private final String debugPhrase = "怀 做 跨 焰 猛 况 余 狂 热 牲 姿 纤";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInputWordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.start_recovery_account));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        restore = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("restore");
        resetPin = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("resetPin");

//        if (restore) {
//            //change the labels
//            title.setText(getString(R.string.MenuViewController_recoverButton));
//            description.setText(getString(R.string.WipeWallet_instruction));
//        } else if (resetPin) {
//            //change the labels
//            title.setText(getString(R.string.RecoverWallet_header_reset_pin));
//            description.setText(getString(R.string.RecoverWallet_subheader_reset_pin));
//        }

        binding.inputWord12.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER) {
                    binding.inputWordSubmit.performClick();
                }
                return false;
            }
        });

        binding.inputWordSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

//        String[] words = debugPhrase.split(" ");
//        EditText[] ets = new EditText[]{
//                binding.inputWord1,
//                binding.inputWord2,
//                binding.inputWord3,
//                binding.inputWord4,
//                binding.inputWord5,
//                binding.inputWord6,
//                binding.inputWord7,
//                binding.inputWord8,
//                binding.inputWord9,
//                binding.inputWord10,
//                binding.inputWord11,
//                binding.inputWord12,
//        };
//        for (int i = 0; i < words.length; i++) {
//            ets[i].setText(words[i]);
//        }
    }

    private void submit() {
        if (!BRAnimator.isClickAllowed()) return;
        final Activity app = InputWord.this;
        String phraseToCheck = getPhrase();
        if (phraseToCheck == null) {
            return;
        }
        String cleanPhrase = SmartValidator.cleanPaperKey(app, phraseToCheck);
        if (SmartValidator.isPaperKeyValid(app, cleanPhrase)) {

            if (restore || resetPin) {
                if (SmartValidator.isPaperKeyCorrect(cleanPhrase, app)) {
                    Utils.hideKeyboard(app);
                    clearWords();

                    if (restore) {
                        new AlertDialog.Builder(InputWord.this, R.style.my_dialog)
                                .setTitle(R.string.remove_account)
                                .setMessage(R.string.confirm_remove_account)
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        BRWalletManager m = BRWalletManager.getInstance();
                                        m.wipeWalletButKeystore(app);
                                        m.wipeKeyStore(app);
                                        Intent intent = new Intent(app, StartActivity.class);
                                        finalizeIntent(intent);
                                    }
                                })
                                .show();
                    } else {
                        AuthManager.getInstance().setPinCode("", InputWord.this);
                        Intent intent = new Intent(app, SetPinActivity.class);
                        intent.putExtra("noPin", true);
                        finalizeIntent(intent);
                    }
                } else {
                    showFalseWord();
                }

            } else {
                Utils.hideKeyboard(app);
                BRWalletManager m = BRWalletManager.getInstance();
                m.wipeWalletButKeystore(app);
                m.wipeKeyStore(app);
                PostAuth.getInstance().setPhraseForKeyStore(cleanPhrase);
                BRSharedPrefs.putAllowSpend(app, false);
                //if this screen is shown then we did not upgrade to the new app, we installed it
                BRSharedPrefs.putGreetingsShown(app, true);
                PostAuth.getInstance().onRecoverWalletAuth(app, false);
            }

        } else {
            showFalseWord();
        }
    }

    private void showFalseWord() {
        new AlertDialog.Builder(InputWord.this, R.style.my_dialog)
                .setTitle(R.string.tips)
                .setMessage(R.string.tips_key_error)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    private void finalizeIntent(Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        startActivity(intent);
        if (!InputWord.this.isDestroyed()) finish();
        Activity app = MainActivity.getApp();
        if (app != null && !app.isDestroyed()) app.finish();
    }

    private String getPhrase() {
        boolean success = true;

        String w1 = binding.inputWord1.getText().toString().toLowerCase();
        String w2 = binding.inputWord2.getText().toString().toLowerCase();
        String w3 = binding.inputWord3.getText().toString().toLowerCase();
        String w4 = binding.inputWord4.getText().toString().toLowerCase();
        String w5 = binding.inputWord5.getText().toString().toLowerCase();
        String w6 = binding.inputWord6.getText().toString().toLowerCase();
        String w7 = binding.inputWord7.getText().toString().toLowerCase();
        String w8 = binding.inputWord8.getText().toString().toLowerCase();
        String w9 = binding.inputWord9.getText().toString().toLowerCase();
        String w10 = binding.inputWord10.getText().toString().toLowerCase();
        String w11 = binding.inputWord11.getText().toString().toLowerCase();
        String w12 = binding.inputWord12.getText().toString().toLowerCase();

        if (Utils.isNullOrEmpty(w1)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord1);
            success = false;
        }
        if (Utils.isNullOrEmpty(w2)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord2);
            success = false;
        }
        if (Utils.isNullOrEmpty(w3)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord3);
            success = false;
        }
        if (Utils.isNullOrEmpty(w4)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord4);
            success = false;
        }
        if (Utils.isNullOrEmpty(w5)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord5);
            success = false;
        }
        if (Utils.isNullOrEmpty(w6)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord6);
            success = false;
        }
        if (Utils.isNullOrEmpty(w7)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord7);
            success = false;
        }
        if (Utils.isNullOrEmpty(w8)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord8);
            success = false;
        }
        if (Utils.isNullOrEmpty(w9)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord9);
            success = false;
        }
        if (Utils.isNullOrEmpty(w10)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord10);
            success = false;
        }
        if (Utils.isNullOrEmpty(w11)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord11);
            success = false;
        }
        if (Utils.isNullOrEmpty(w12)) {
            SpringAnimator.failShakeAnimation(this, binding.inputWord12);
            success = false;
        }

        if (!success) return null;

        return w(w1) + " " + w(w2) + " " + w(w3) + " " + w(w4) + " " + w(w5) + " " + w(w6) + " " + w(w7) + " " + w(w8) + " " + w(w9) + " " + w(w10) + " " + w(w11) + " " + w(w12);
    }

    private String w(String word) {
        return word.replaceAll(" ", "");
    }

    private void clearWords() {
        binding.inputWord1.setText("");
        binding.inputWord2.setText("");
        binding.inputWord3.setText("");
        binding.inputWord4.setText("");
        binding.inputWord5.setText("");
        binding.inputWord6.setText("");
        binding.inputWord7.setText("");
        binding.inputWord8.setText("");
        binding.inputWord9.setText("");
        binding.inputWord10.setText("");
        binding.inputWord11.setText("");
        binding.inputWord12.setText("");
    }
}
