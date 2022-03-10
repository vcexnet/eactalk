package com.eacpay.eactalk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivitySendMessageBinding;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.entities.PaymentItem;
import com.eacpay.tools.animation.SpringAnimator;
import com.eacpay.tools.manager.AnalyticsManager;
import com.eacpay.tools.security.BRSender;
import com.eacpay.tools.util.BRConstants;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.tools.util.Utils;
import com.eacpay.wallet.BRWalletManager;

import java.math.BigDecimal;

public class SendMessage extends BRActivity {
    private static final String TAG = "oldfeel";
    String[] titles = new String[]{"深喉爆料", "昭告天下", "情感广场", "树洞吐槽"};
    String[] addresses = new String[]{"epJ9S6gVFjigZtHvRdK8VDFEJYh8JE16vC", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    ActivitySendMessageBinding binding;
    private String messageType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySendMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        messageType = intent.getStringExtra("type");
        if (messageType.equals("normal") || messageType.equals("qr")) {
            if (messageType.equals("qr")) {
                String content = intent.getStringExtra("content");
                binding.sendMessageAddress.setText(content);
            }
            binding.sendMessageNotice.setVisibility(View.GONE);
            setTitle("发布普通消息");
        } else {
            binding.sendMessageNormal.setVisibility(View.GONE);
            setTitle("发布公告消息");
        }

        binding.sendMessageNoticeType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.send_message_notice_zg:
                        binding.sendMessageAddress.setText(addresses[1]);
                        binding.sendMessageName.setText(titles[1]);
                        break;
                    case R.id.send_message_notice_bl:
                        binding.sendMessageAddress.setText(addresses[0]);
                        binding.sendMessageName.setText(titles[0]);
                        break;
                    case R.id.send_message_notice_qg:
                        binding.sendMessageAddress.setText(addresses[2]);
                        binding.sendMessageName.setText(titles[2]);
                        break;
                    case R.id.send_message_notice_sd:
                        binding.sendMessageAddress.setText(addresses[3]);
                        binding.sendMessageName.setText(titles[3]);
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_send_message_send) {
            submitSend();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void submitSend() {
        boolean allFilled = true;
        String address = binding.sendMessageAddress.getText().toString();
        String amountStr = binding.sendMessageAmount.getText().toString();
        String comment = binding.sendMessageName.getText().toString();
        String txComment = binding.sendMessageContent.getText().toString();
        String txIpfsid = "";
        String composedComment = (txIpfsid.length() > 0) ? (txIpfsid + "\n" + txComment) : txComment;
//        String selectedIso = BRSharedPrefs.getIso(this);
        String selectedIso = "EAC";

        //get amount in satoshis from any isos
        BigDecimal bigAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) ? "0" : amountStr);
        BigDecimal satoshiAmount = selectedIso.equalsIgnoreCase("eac") ?
                BRExchange.getSatoshisForBitcoin(this, bigAmount) :
                BRExchange.getSatoshisFromAmount(this, selectedIso, bigAmount);

        if (address.isEmpty() || !BRWalletManager.validateAddress(address)) {
            allFilled = false;
            SpringAnimator.failShakeAnimation(this, binding.sendMessageAddress);
        }
        if (amountStr.isEmpty()) {
            allFilled = false;
            SpringAnimator.failShakeAnimation(this, binding.sendMessageAmount);
        }
        if (satoshiAmount.longValue() > BRWalletManager.getInstance().getBalance(this)) {
            Log.e(TAG, "submitSend: satoshiAmount.longValue() " + satoshiAmount.longValue());
            Log.e(TAG, "submitSend: BRWalletManager.getInstance().getBalance(this) " + BRWalletManager.getInstance().getBalance(this));
//            SpringAnimator.failShakeAnimation(this, balanceText);
//            SpringAnimator.failShakeAnimation(this, feeText);
            Toast.makeText(this, "余额不足", Toast.LENGTH_SHORT).show();
        }

        if (allFilled) {
            BRSender.getInstance().sendTransaction(this, new PaymentItem(new String[]{address}, null, satoshiAmount.longValue(), null, false, comment, composedComment));
            AnalyticsManager.logCustomEvent(BRConstants._20191105_DSL);
        }
    }
}
