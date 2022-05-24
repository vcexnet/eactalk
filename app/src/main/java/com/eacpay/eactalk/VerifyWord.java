package com.eacpay.eactalk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.eacpay.R;
import com.eacpay.databinding.ActivityVerifyWordBinding;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.customviews.BRDialogView;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.animation.BRDialog;
import com.eacpay.tools.animation.SpringAnimator;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.security.SmartValidator;
import com.eacpay.tools.util.Bip39Reader;
import com.eacpay.tools.util.Utils;

import java.util.Locale;
import java.util.Random;

import timber.log.Timber;

public class VerifyWord extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityVerifyWordBinding binding;
    private final SparseArray<String> sparseArrayWords = new SparseArray<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyWordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.set_key_word));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        binding.verifyWordContent2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER) {
                    binding.verifyWordSubmit.performClick();
                    return true;
                }
                return false;
            }
        });

        binding.verifyWordSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        String cleanPhrase = null;

        cleanPhrase = getIntent().getExtras() == null ? null : getIntent().getStringExtra("phrase");

//        cleanPhrase = "到 挺 困 刑 实 苦 拜 芽 症 优 移 落";
//        cleanPhrase = "video tiger report bid suspect taxi mail argue naive layer metal surface";
        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new RuntimeException("VerifyWord: cleanPhrase is null");
        }

        String[] wordArray = cleanPhrase.split(" ");

        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    getString(R.string.Alert_keystore_generic_android), getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
            Timber.e(new IllegalArgumentException("Paper Key error. Problem with OS Keystore"));
        } else {
            randomWordsSetUp(wordArray);
        }

        binding.verifyWordContent1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateWord(binding.verifyWordContent1);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        binding.verifyWordContent2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validateWord(binding.verifyWordContent2);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void submit() {
        if (!BRAnimator.isClickAllowed()) return;

        if (isWordCorrect(true) && isWordCorrect(false)) {
            Utils.hideKeyboard(VerifyWord.this);
            BRSharedPrefs.putPhraseWroteDown(VerifyWord.this, true);
//            BRAnimator.showBreadSignal(VerifyWord.this, getString(R.string.Alerts_paperKeySet), getString(R.string.Alerts_paperKeySetSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
//                @Override
//                public void onComplete() {
//                    BRAnimator.startMainActivity(VerifyWord.this, false);
//                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//                    finishAffinity();
//                }
//            });

            new AlertDialog.Builder(VerifyWord.this, R.style.my_dialog)
                    .setTitle(R.string.set_complete)
                    .setMessage(R.string.set_key_word_success)
                    .setPositiveButton(R.string.i_known, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            BRAnimator.startMainActivity(VerifyWord.this, false);
                            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            finishAffinity();
                        }
                    })
                    .show();
        } else {

            if (!isWordCorrect(true)) {
                binding.verifyWordContent1.setTextColor(getColor(R.color.red_text));
                SpringAnimator.failShakeAnimation(VerifyWord.this, binding.verifyWordContent1);
            }

            if (!isWordCorrect(false)) {
                binding.verifyWordContent2.setTextColor(getColor(R.color.red_text));
                SpringAnimator.failShakeAnimation(VerifyWord.this, binding.verifyWordContent2);
            }
        }
    }

    private void randomWordsSetUp(String[] words) {
        final Random random = new Random();
        int n = random.nextInt(10) + 1;

        sparseArrayWords.append(n, words[n]);

        while (sparseArrayWords.get(n) != null) {
            n = random.nextInt(10) + 1;
        }

        sparseArrayWords.append(n, words[n]);

        binding.verifyWordTitle1.setText(String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word), (sparseArrayWords.keyAt(0) + 1)));
        binding.verifyWordTitle2.setText(String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word), (sparseArrayWords.keyAt(1) + 1)));
    }

    private boolean isWordCorrect(boolean first) {
        Log.e(TAG, "isWordCorrect: " + binding.verifyWordContent1.getText().toString() + " " + binding.verifyWordContent2.getText().toString());
        if (first) {
            String edit = Bip39Reader.cleanWord(binding.verifyWordContent1.getText().toString());
            Log.e(TAG, "isWordCorrect: " + SmartValidator.isWordValid(VerifyWord.this, edit) + " " + sparseArrayWords.get(sparseArrayWords.keyAt(0)));
            return SmartValidator.isWordValid(VerifyWord.this, edit) && edit.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(0)));
        } else {
            String edit = Bip39Reader.cleanWord(binding.verifyWordContent2.getText().toString());
            Log.e(TAG, "isWordCorrect: " + SmartValidator.isWordValid(VerifyWord.this, edit) + " " + sparseArrayWords.get(sparseArrayWords.keyAt(1)));
            return SmartValidator.isWordValid(VerifyWord.this, edit) && edit.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(1)));
        }
    }

    private void validateWord(EditText view) {
        String word = view.getText().toString();
        boolean valid = SmartValidator.isWordValid(this, word);
        view.setTextColor(getColor(valid ? R.color.light_gray : R.color.red_text));
//        if (!valid)
//            SpringAnimator.failShakeAnimation(this, view);
//        if (isWordCorrect(true)) {
//            checkMark1.setVisibility(View.VISIBLE);
//        } else {
//            checkMark1.setVisibility(View.INVISIBLE);
//        }
//
//        if (isWordCorrect(false)) {
//            checkMark2.setVisibility(View.VISIBLE);
//        } else {
//            checkMark2.setVisibility(View.INVISIBLE);
//        }
    }
}
