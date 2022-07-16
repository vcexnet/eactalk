package com.eacpay.eactalk;

import static com.eacpay.eactalk.ipfs.IpfsDataFetcher.ENCRYPT_PREFIX;
import static com.eacpay.tools.security.BitcoinUrlHandler.getRequestFromString;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.blankj.utilcode.util.LanguageUtils;
import com.eacpay.R;
import com.eacpay.databinding.ActivitySendMessageBinding;
import com.eacpay.databinding.BottomSheetUploadBinding;
import com.eacpay.eactalk.fragment.dialog.OnDialogOkListener;
import com.eacpay.eactalk.fragment.dialog.ProgressDialog;
import com.eacpay.eactalk.fragment.dialog.ReplyTips;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.eactalk.utils.SensitiveWord;
import com.eacpay.presenter.activities.camera.ScanQRActivity;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.customviews.BRDialogView;
import com.eacpay.presenter.customviews.BRKeyboard;
import com.eacpay.presenter.entities.PaymentItem;
import com.eacpay.presenter.entities.RequestObject;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.animation.BRDialog;
import com.eacpay.tools.animation.SpringAnimator;
import com.eacpay.tools.manager.AnalyticsManager;
import com.eacpay.tools.manager.BRClipboardManager;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.manager.FeeManager;
import com.eacpay.tools.security.BRSender;
import com.eacpay.tools.security.BitcoinUrlHandler;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRConstants;
import com.eacpay.tools.util.BRCurrency;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.tools.util.Utils;
import com.eacpay.wallet.BRWalletManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import ipfs.gomobile.android.IPFS;
import timber.log.Timber;

public class SendMessage extends BRActivity {
    private static final String TAG = "oldfeel";
    String[] titles;
    // 深喉爆料/昭告天下/情感广场/树洞吐槽
    String[] addresses = new String[]{"epJ9S6gVFjigZtHvRdK8VDFEJYh8JE16vC", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    int[] minEac = new int[]{50000, 100000, 1000, 100};
    int noticeIndex = 1;
    ActivitySendMessageBinding binding;
    private String messageType;
    String fileName = null;
    byte[] fileData = null;
    String fileCid = null;
    int contentMaxLength = 256;
    boolean isReply = false;
    boolean isReplyOk = false;
    private boolean amountLabelOn = true;
    private String selectedIso = "EAC";
    private long curBalance;
    private StringBuilder amountBuilder = new StringBuilder(0);
    boolean isNotice = false;
    MyService eacService;
    boolean isBound;

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyService.EacBinder binder = (MyService.EacBinder) iBinder;
            eacService = binder.getService();
            isBound = true;
            if (!eacService.isEacConnect || !eacService.isEacSyncFinish) {
                showCannotSend();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };
    private ProgressDialog progressDialog;
    private boolean checkWord = true;

    private void showCannotSend() {
        Dialog dialog = new AlertDialog.Builder(this, R.style.my_dialog)
                .setTitle(getString(R.string.tips))
                .setMessage(R.string.tip_eac_syncing)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySendMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        titles = new String[]{getString(R.string.shbl), getString(R.string.zgtx), getString(R.string.qggc), getString(R.string.sdtc)};

        Intent intent = getIntent();
        messageType = intent.getStringExtra("type");
        if (messageType == null || messageType.equals("normal")) {
            binding.sendMessageNotice.setVisibility(View.GONE);
            setTitle(getString(R.string.send_normal));
        } else {
            binding.sendMessageNormal.setVisibility(View.GONE);
            setTitle(getString(R.string.send_notice));

            binding.sendMessagePaste.setVisibility(View.GONE);
            binding.sendMessageScan.setVisibility(View.GONE);
            binding.sendMessageAddress.setEnabled(false);
            binding.sendMessageName.setEnabled(false);
            isNotice = true;
        }

        String address = intent.getStringExtra("address");
        String name = intent.getStringExtra("name");
        String amount = intent.getStringExtra("amount");
        String title = intent.getStringExtra("title");
        isReply = intent.getBooleanExtra("isReply", false);

        if (address != null) {
            RequestObject obj = BitcoinUrlHandler.getRequestFromString(address);
            if (obj != null) {
                if (obj.address != null) {
                    address = obj.address.trim();
                }

                if (obj.label != null) {
                    name = obj.label;
                }

                if (obj.message != null) {
                    binding.sendMessageContent.setText(obj.message);
                }

                if (obj.amount != null) {
                    String iso = selectedIso;
                    //BigDecimal satoshiAmount = new BigDecimal(obj.amount).multiply(new BigDecimal(100000000));
                    BigDecimal satoshiAmount = new BigDecimal(obj.amount);
                    //amountBuilder = new StringBuilder(BRExchange.getAmountFromSatoshis(getActivity(), iso, satoshiAmount).toPlainString());
                    amountBuilder = new StringBuilder(satoshiAmount.toString());
                    updateText();
                }
            }
        }

        binding.sendMessageAddress.setText(address);
        binding.sendMessageName.setText(name);
        if (amount != null) {
            amountBuilder = new StringBuilder(amount);
        }
        if (title != null && !title.equals("")) {
            setTitle(title);
        }
        if (name != null && !name.equals("")) {
//            binding.sendMessageAddress.setEnabled(false);
            binding.sendMessageName.setEnabled(false);
        }
        if (address != null && !address.equals("")) {
            binding.sendMessagePaste.setVisibility(View.GONE);
            binding.sendMessageScan.setVisibility(View.GONE);
            binding.sendMessageAddress.setEnabled(false);
        }

        binding.sendMessageNoticeType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                amountBuilder = new StringBuilder();
                switch (i) {
                    case R.id.send_message_notice_zg:
                        binding.sendMessageAddress.setText(addresses[1]);
                        binding.sendMessageName.setText(titles[1]);
                        amountBuilder.append(minEac[1]);
                        noticeIndex = 1;
                        break;
                    case R.id.send_message_notice_bl:
                        binding.sendMessageAddress.setText(addresses[0]);
                        binding.sendMessageName.setText(titles[0]);
                        amountBuilder.append(minEac[0]);
                        noticeIndex = 0;
                        break;
                    case R.id.send_message_notice_qg:
                        binding.sendMessageAddress.setText(addresses[2]);
                        binding.sendMessageName.setText(titles[2]);
                        amountBuilder.append(minEac[2]);
                        noticeIndex = 2;
                        break;
                    case R.id.send_message_notice_sd:
                        binding.sendMessageAddress.setText(addresses[3]);
                        binding.sendMessageName.setText(titles[3]);
                        amountBuilder.append(minEac[3]);
                        noticeIndex = 3;
                        break;
                }
                updateText();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        binding.sendMessageFileUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileUpload();
            }
        });

        binding.sendMessageContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String content = binding.sendMessageContent.getText().toString();

                int length = content.getBytes().length;
                binding.sendMessageContentCount.setText(length + "/" + contentMaxLength);
                binding.sendMessageContentCount.setTextColor(length > contentMaxLength ? Color.RED : Color.BLACK);
            }
        });

        binding.sendMessagePaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                String bitcoinUrl = BRClipboardManager.getClipboard(SendMessage.this);
                if (Utils.isNullOrEmpty(bitcoinUrl) || !isInputValid(bitcoinUrl)) {
                    showClipboardError();
                    return;
                }
                RequestObject obj = getRequestFromString(bitcoinUrl);

                if (obj == null || obj.address == null) {
                    showClipboardError();
                    return;
                }
                String address = obj.address;
                final BRWalletManager wm = BRWalletManager.getInstance();

                if (BRWalletManager.validateAddress(address)) {
                    final String finalAddress = address;
                    final Activity app = SendMessage.this;
                    if (app == null) {
                        Log.e(TAG, "paste onClick: app is null");
                        return;
                    }
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (wm.addressContainedInWallet(finalAddress)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(SendMessage.this, "", getResources().getString(R.string.Send_containsAddress), getResources().getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                            }
                                        }, null, null, 0);
                                        BRClipboardManager.putClipboard(SendMessage.this, "");
                                    }
                                });

                            } else if (wm.addressIsUsed(finalAddress)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(SendMessage.this, getString(R.string.Send_UsedAddress_firstLine), getString(R.string.Send_UsedAddress_secondLIne), "Ignore", "Cancel", new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                                binding.sendMessageAddress.setText(finalAddress);
                                            }
                                        }, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                            }
                                        }, null, 0);
                                    }
                                });
                            } else {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.sendMessageAddress.setText(finalAddress);
                                    }
                                });
                            }
                        }
                    });
                } else {
                    showClipboardError();
                }
            }
        });

        binding.sendMessageScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStartForResult.launch(new Intent(SendMessage.this, ScanQRActivity.class));
            }
        });

        binding.sendMessageSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isReplyOk = false;
                if (LanguageUtils.getSystemLanguage().getLanguage().startsWith("zh")) {
                    checkWord = true;
                }
                submitSend();
            }
        });

        selectedIso = BRSharedPrefs.getPreferredLTC(this) ? "EAC" : BRSharedPrefs.getIso(this);
        binding.amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAmountEdit();
            }
        });

        binding.isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getIso(SendMessage.this))) {
                    selectedIso = "EAC";
                } else {
                    selectedIso = BRSharedPrefs.getIso(SendMessage.this);
                }
                updateText();
            }
        });

        binding.keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        binding.keyboard.setBRKeyboardColor(R.color.white);

        binding.keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });

        binding.feesSegment.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                FeeManager feeManager = FeeManager.getInstance();
                switch (checkedId) {
                    case R.id.regular_fee_but:
                        feeManager.setFeeType(FeeManager.REGULAR);
                        BRWalletManager.getInstance().setFeePerKb(feeManager.getFees().regular);
                        setFeeInformation(R.string.FeeSelector_regularTime, 0, 0, View.GONE);
                        break;
                    case R.id.economy_fee_but:
                        feeManager.setFeeType(FeeManager.ECONOMY);
                        BRWalletManager.getInstance().setFeePerKb(feeManager.getFees().economy);
                        setFeeInformation(R.string.FeeSelector_economyTime, R.string.FeeSelector_economyWarning, R.color.red_text, View.VISIBLE);
                        break;
                    case R.id.luxury_fee_but:
                        feeManager.setFeeType(FeeManager.LUXURY);
                        BRWalletManager.getInstance().setFeePerKb(feeManager.getFees().luxury);
                        setFeeInformation(R.string.FeeSelector_luxuryTime, R.string.FeeSelector_luxuryMessage, R.color.light_gray, View.VISIBLE);
                        break;
                }
                updateText();
            }
        });

        if (isNotice) { // 默认 昭告
            binding.sendMessageNoticeType.check(R.id.send_message_notice_bl);
            binding.sendMessageAddress.setText(addresses[0]);
            binding.sendMessageName.setText(titles[0]);
            noticeIndex = 0;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    showAmountEdit(); // 要获取 scale,延迟处理
                    amountBuilder = new StringBuilder();
                    amountBuilder.append(minEac[0]);
                    updateText();
                }
            }, 500);
        } else {
            updateText();
        }

        // ContentView is the root view of the layout of this activity/fragment
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        Rect r = new Rect();
                        binding.getRoot().getWindowVisibleDisplayFrame(r);
                        int screenHeight = binding.getRoot().getRootView().getHeight();

                        // r.bottom is the position above soft keypad or device button.
                        // if keypad is shown, the r.bottom is smaller than that before.
                        int keypadHeight = screenHeight - r.bottom;

                        Log.d(TAG, "keypadHeight = " + keypadHeight);

                        if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                            // keyboard is opened
                            if (!isKeyboardShowing) {
                                isKeyboardShowing = true;
                                onKeyboardVisibilityChanged(true);
                            }
                        } else {
                            // keyboard is closed
                            if (isKeyboardShowing) {
                                isKeyboardShowing = false;
                                onKeyboardVisibilityChanged(false);
                            }
                        }
                    }
                });

        showKeyboard(false);
    }

    private void showAmountEdit() {
        showKeyboard(true);
        if (amountLabelOn) { //only first time
            amountLabelOn = false;
            binding.amountEdit.setHint("0");
            binding.amountEdit.setTextSize(24);
            binding.balanceText.setVisibility(View.VISIBLE);
            binding.feeText.setVisibility(View.VISIBLE);
            binding.edit.setVisibility(View.VISIBLE);
            binding.isoText.setTextColor(getColor(R.color.almost_black));
            binding.isoText.setText(BRCurrency.getSymbolByIso(SendMessage.this, selectedIso));
            binding.isoText.setTextSize(28);
            final float scaleX = binding.amountEdit.getScaleX();
            binding.amountEdit.setScaleX(0);

            AutoTransition tr = new AutoTransition();
            tr.setInterpolator(new OvershootInterpolator());
            tr.addListener(new androidx.transition.Transition.TransitionListener() {
                @Override
                public void onTransitionStart(@NonNull androidx.transition.Transition transition) {

                }

                @Override
                public void onTransitionEnd(@NonNull androidx.transition.Transition transition) {
                    binding.amountEdit.requestLayout();
                    binding.amountEdit.animate().setDuration(100).scaleX(scaleX);
                }

                @Override
                public void onTransitionCancel(@NonNull androidx.transition.Transition transition) {

                }

                @Override
                public void onTransitionPause(@NonNull androidx.transition.Transition transition) {

                }

                @Override
                public void onTransitionResume(@NonNull androidx.transition.Transition transition) {

                }
            });

            ConstraintSet set = new ConstraintSet();
            set.clone(binding.amountLayout);
            TransitionManager.beginDelayedTransition(binding.amountLayout, tr);

            int px4 = Utils.getPixelsFromDps(SendMessage.this, 4);
            set.connect(binding.balanceText.getId(), ConstraintSet.TOP, binding.isoText.getId(), ConstraintSet.BOTTOM, px4);
            set.connect(binding.feeText.getId(), ConstraintSet.TOP, binding.balanceText.getId(), ConstraintSet.BOTTOM, px4);
            set.connect(binding.feeText.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4);
            set.connect(binding.isoText.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4);
            set.connect(binding.isoText.getId(), ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1);
            set.applyTo(binding.amountLayout);
        }
    }

    private void setFeeInformation(@StringRes int deliveryTime, @StringRes int warningStringId, @ColorRes int warningColorId, int visibility) {
        binding.feeDescription.setText(getString(R.string.FeeSelector_estimatedDeliver, getString(deliveryTime)));
        if (warningStringId != 0) {
            binding.warningText.setText(warningStringId);
        }
        if (warningColorId != 0) {
            binding.warningText.setTextColor(getResources().getColor(warningColorId, null));
        }
        binding.warningText.setVisibility(visibility);
    }

    private void handleClick(String key) {
        if (key == null) {
            Timber.d("handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || BRCurrency.getMaxDecimalPlaces(selectedIso) == 0)
            return;
        amountBuilder.append(".");
        updateText();
    }

    private void handleDigitClick(int dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= BRExchange.getMaxAmount(SendMessage.this, iso).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder();
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > BRCurrency.getMaxDecimalPlaces(iso))))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }
    }

    boolean isKeyboardShowing = false;

    void onKeyboardVisibilityChanged(boolean opened) {
        if (opened) {
            showKeyboard(false);
        }
    }

    private void updateText() {
        if (isNotice) {
            binding.sendMessageMinEac.setText(getString(R.string.min_eac, minEac[noticeIndex]));
        }
        String tmpAmount = amountBuilder.toString();
        setAmount();
        String iso = selectedIso;
        String currencySymbol = BRCurrency.getSymbolByIso(SendMessage.this, selectedIso);
        curBalance = BRWalletManager.getInstance().getBalance(SendMessage.this);
        if (!amountLabelOn)
            binding.isoText.setText(currencySymbol);
        binding.isoButton.setText(String.format("%s(%s)", BRCurrency.getCurrencyName(SendMessage.this, selectedIso), currencySymbol));
        //Balance depending on ISO
        long satoshis = (Utils.isNullOrEmpty(tmpAmount) || tmpAmount.equalsIgnoreCase(".")) ? 0 :
                (selectedIso.equalsIgnoreCase("eac") ?
                        BRExchange.getSatoshisForBitcoin(SendMessage.this, new BigDecimal(tmpAmount)).longValue() :
                        BRExchange.getSatoshisFromAmount(SendMessage.this, selectedIso, new BigDecimal(tmpAmount)).longValue());
        BigDecimal balanceForISO = BRExchange.getAmountFromSatoshis(SendMessage.this, iso, new BigDecimal(curBalance));
        Timber.d("updateText: balanceForISO: %s", balanceForISO);

        //formattedBalance
        String formattedBalance = BRCurrency.getFormattedCurrencyString(SendMessage.this, iso, balanceForISO);
        //Balance depending on ISO
        long fee;
        if (satoshis == 0) {
            fee = 0;
        } else {
            fee = BRWalletManager.getInstance().feeForTransactionAmount(satoshis);
            if (fee == 0) {
                Timber.i("updateText: fee is 0, trying the estimate");
                fee = BRWalletManager.getInstance().feeForTransaction(binding.sendMessageAddress.getText().toString(), satoshis);
            }
        }

        BigDecimal feeForISO = BRExchange.getAmountFromSatoshis(SendMessage.this, iso, new BigDecimal(curBalance == 0 ? 0 : fee));
        Timber.d("updateText: feeForISO: %s", feeForISO);
        //formattedBalance
        String aproxFee = BRCurrency.getFormattedCurrencyString(SendMessage.this, iso, feeForISO);
        Timber.d("updateText: aproxFee: %s", aproxFee);
        if (new BigDecimal((tmpAmount.isEmpty() || tmpAmount.equalsIgnoreCase(".")) ? "0" : tmpAmount).doubleValue() > balanceForISO.doubleValue()) {
            binding.balanceText.setTextColor(getColor(R.color.warning_color));
            binding.feeText.setTextColor(getColor(R.color.warning_color));
            binding.amountEdit.setTextColor(getColor(R.color.warning_color));
            if (!amountLabelOn)
                binding.isoText.setTextColor(getColor(R.color.warning_color));
        } else {
            binding.balanceText.setTextColor(getColor(R.color.light_gray));
            binding.feeText.setTextColor(getColor(R.color.light_gray));
            binding.amountEdit.setTextColor(getColor(R.color.almost_black));
            if (!amountLabelOn)
                binding.isoText.setTextColor(getColor(R.color.almost_black));
        }
        binding.balanceText.setText(getString(R.string.Send_balance, formattedBalance));
        binding.feeText.setText(String.format(getString(R.string.Send_fee), aproxFee));
//        binding.donate.setText(getString(R.string.Donate_title, currencySymbol));
//        binding.donate.setEnabled(curBalance >= BRConstants.DONATION_AMOUNT * 2);
        binding.amountLayout.requestLayout();
    }

    private void setAmount() {
        String tmpAmount = amountBuilder.toString();
        Log.e(TAG, "setAmount: tmpAmount " + tmpAmount);
        int divider = tmpAmount.length();
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".");
        }
        StringBuilder newAmount = new StringBuilder();
        for (int i = 0; i < tmpAmount.length(); i++) {
            newAmount.append(tmpAmount.charAt(i));
            if (divider > 3 && divider - 1 != i && divider > i && ((divider - i - 1) % 3 == 0)) {
                newAmount.append(",");
            }
        }
        Log.e(TAG, "setAmount: newAmount " + newAmount);
        binding.amountEdit.setText(newAmount.toString());
    }

    private void showKeyboard(boolean b) {
        if (!b) {
            binding.keyboardLayout.setVisibility(View.GONE);
            binding.feeButtonsLayout.setVisibility(View.GONE);
        } else {
            Utils.hideKeyboard(this);
            binding.feeButtonsLayout.setVisibility(View.VISIBLE);
            binding.keyboardLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean isInputValid(String input) {
        return input.matches("[a-zA-Z0-9]*");
    }

    private void showClipboardError() {
        BRDialog.showCustomDialog(SendMessage.this, getString(R.string.Send_emptyPasteboard), getResources().getString(R.string.Send_invalidAddressTitle), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(SendMessage.this, "");
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        String address = intent.getStringExtra("result");
                        binding.sendMessageAddress.setText(MyUtils.parseAddress(address));
                    }
                }
            });


    ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
        }
    });
    ActivityResultLauncher<String> videoSelect = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {
            if (uri == null) {
                return;
            }
            fileName = MyUtils.getFileName(SendMessage.this, uri);
            fileData = MyUtils.getFileBytes(SendMessage.this, uri);
            binding.sendMessageFileName.setText(getString(R.string.file_name_) + fileName);
        }
    });
    ActivityResultLauncher<String> fileSelect = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {
            if (uri == null) {
                return;
            }
            fileName = MyUtils.getFileName(SendMessage.this, uri);
            fileData = MyUtils.getFileBytes(SendMessage.this, uri);
            binding.sendMessageFileName.setText(getString(R.string.file_name_) + fileName);
        }
    });
    ActivityResultLauncher<String> imageSelect = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {
            if (uri == null) {
                return;
            }
            fileName = MyUtils.getFileName(SendMessage.this, uri);
            fileData = MyUtils.getFileBytes(SendMessage.this, uri);
            binding.sendMessageFileName.setText(getString(R.string.file_name_) + fileName);
        }
    });

    private void fileUpload() {
        BottomSheetUploadBinding uploadBinding = BottomSheetUploadBinding.inflate(getLayoutInflater());
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(uploadBinding.getRoot());
        dialog.show();
        // Remove default white color background
        FrameLayout bottomSheet = dialog.findViewById(R.id.design_bottom_sheet);
        bottomSheet.setBackground(null);

        uploadBinding.bottomSheetUploadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                videoSelect.launch("video/*");
            }
        });

        uploadBinding.bottomSheetUploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                fileSelect.launch("*/*");
            }
        });

        uploadBinding.bottomSheetUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                imageSelect.launch("image/*");
            }
        });

        uploadBinding.bottomSheetUploadCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.send_message, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == R.id.menu_send_message_send) {
//            isReplyOk = false;
//            submitSend();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private void submitSend() {
        Utils.hideKeyboard(SendMessage.this);

        String txIpfsid = "";
        if (fileCid != null && !fileCid.equals("")) {
            txIpfsid = fileCid;
        }
        String address = binding.sendMessageAddress.getText().toString();
        String amountStr = amountBuilder.toString();
        final String comment = binding.sendMessageName.getText().toString();
        String txComment = binding.sendMessageContent.getText().toString();

        // 检测敏感词
        if (checkWord) {
            Set<String> set = SensitiveWord.list(this, comment + " " + txComment);
            if (set.size() > 0) {
                String words = "";
                Iterator<String> it = set.iterator();
                while (it.hasNext()) {
                    words += " " + it.next();
                }
                new AlertDialog.Builder(this, R.style.my_dialog)
                        .setTitle(getString(R.string.tips))
                        .setMessage(getString(R.string.confirm_sensitive_word, words))
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                checkWord = false;
                                submitSend();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .show();
                return;
            }
        }
//        Set<String> set = SensitiveWord.list(this, comment);
//        if (set.size() > 0) {
//            new AlertDialog.Builder(this, R.style.my_dialog)
//                    .setTitle("提示")
//                    .setMessage("你发布的名字包含敏感字,请修改后重新提交")
//                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.cancel();
//                        }
//                    })
//                    .show();
//            return;
//        }
//        set = SensitiveWord.list(this, txComment);
//        if (set.size() > 0) {
//            new AlertDialog.Builder(this, R.style.my_dialog)
//                    .setTitle("提示")
//                    .setMessage("你发布的内容包含敏感字,请修改后重新提交")
//                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.cancel();
//                        }
//                    })
//                    .show();
//            return;
//        }

        // 加密
        if (binding.sendMessageNormalEncryptMessage.isChecked()) {
            txComment = MyUtils.encrypt(txComment, address);
            txComment = ENCRYPT_PREFIX + txComment;
        }

        String composedComment = (txIpfsid.length() > 0) ? (txIpfsid + "\n" + txComment) : txComment;

        BigDecimal bigAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) ? "0" : amountStr);
        BigDecimal satoshiAmount = selectedIso.equalsIgnoreCase("eac") ?
                BRExchange.getSatoshisForBitcoin(this, bigAmount) :
                BRExchange.getSatoshisFromAmount(this, selectedIso, bigAmount);

        if (address.isEmpty() || !BRWalletManager.validateAddress(address)) {
            SpringAnimator.failShakeAnimation(this, binding.sendMessageAddress);
            return;
        }
        if (satoshiAmount.longValue() > BRWalletManager.getInstance().getBalance(this)) {
            SpringAnimator.failShakeAnimation(this, binding.balanceText);
            SpringAnimator.failShakeAnimation(this, binding.feeText);
            Toast.makeText(this, R.string.insufficient_balance, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNotice && satoshiAmount.longValue() / 100000000 < minEac[noticeIndex]) {
            Toast.makeText(this, getString(R.string.not_less_than) + minEac[noticeIndex], Toast.LENGTH_SHORT).show();
            showAmountEdit();
            return;
        }

        if (txComment.getBytes().length > contentMaxLength) {
            Toast.makeText(this, R.string.message_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isReply && !isReplyOk) {
            ReplyTips replyTips = new ReplyTips();
            replyTips.setOnDialogOkListener(new OnDialogOkListener() {
                @Override
                public void onOk(Object obj) {
                    isReplyOk = true;
                    submitSend();
                }
            });

            replyTips.show(getSupportFragmentManager(), "reply_tips");
            return;
        }

        if (binding.sendMessageNormalEncryptFile.isChecked() && fileData != null) {
            fileData = MyUtils.encryptFile(fileData, address);
            fileName = ENCRYPT_PREFIX + fileName;
        }

        if (fileData != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    IPFS ipfs = IpfsManager.getInstance().getIpfs();
                    try {
                        fileCid = ipfs.addW(fileData, fileName);
                        fileData = null;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.sendMessageFileCid.setText("CID: " + fileCid);
                                submitSend();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return;
        }

        MyUtils.log("sendTransaction--------->");
        // 提交发送
        BRSender.getInstance().sendTransaction(SendMessage.this, new PaymentItem(new String[]{address}, null, satoshiAmount.longValue(), null, false, comment, composedComment));
        AnalyticsManager.logCustomEvent(BRConstants._20191105_DSL);
    }

    public void progressDialogShow() {
        progressDialog = new ProgressDialog();
        progressDialog.setTitle(getString(R.string.sending));
        progressDialog.show(getSupportFragmentManager(), "progress_dialog");
    }

    public void progressDialogDismiss() {
        if (progressDialog != null && progressDialog.getDialog() != null) {
            progressDialog.getDialog().cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        isBound = false;
    }

    @Override
    public void onBackPressed() {
        if (binding.keyboardLayout.getVisibility() == View.VISIBLE) {
            showKeyboard(false);
            return;
        }
        super.onBackPressed();
    }

    public void paySuccess() {
        progressDialogDismiss();
        if (isNotice) {
            new AlertDialog.Builder(this, R.style.my_dialog)
                    .setTitle(R.string.tips)
                    .setMessage(R.string.tip_send_notice_success)
                    .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Intent intent = new Intent();
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
//                            intent.setData(Uri.fromParts("package", getPackageName(), null));
//                            startActivity(intent);
//                            dialogInterface.cancel();
                            dialogInterface.cancel();
                            finish();
                        }
                    })
//                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.cancel();
//                        }
//                    })
                    .show();
        } else {
            new AlertDialog.Builder(this, R.style.my_dialog)
                    .setTitle(R.string.send_success)
                    .setCancelable(false)
                    .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            finish();
                        }
                    })
                    .show();
        }
    }
}
