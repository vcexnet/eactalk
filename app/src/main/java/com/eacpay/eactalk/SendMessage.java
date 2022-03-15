package com.eacpay.eactalk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eacpay.R;
import com.eacpay.databinding.ActivitySendMessageBinding;
import com.eacpay.databinding.BottomSheetUploadBinding;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.entities.PaymentItem;
import com.eacpay.tools.animation.SpringAnimator;
import com.eacpay.tools.manager.AnalyticsManager;
import com.eacpay.tools.security.BRSender;
import com.eacpay.tools.util.BRConstants;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.tools.util.Utils;
import com.eacpay.wallet.BRWalletManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;

import ipfs.gomobile.android.IPFS;

public class SendMessage extends BRActivity {
    private static final String TAG = "oldfeel";
    String[] titles = new String[]{"深喉爆料", "昭告天下", "情感广场", "树洞吐槽"};
    String[] addresses = new String[]{"epJ9S6gVFjigZtHvRdK8VDFEJYh8JE16vC", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    ActivitySendMessageBinding binding;
    private String messageType;
    String fileName = null;
    byte[] fileData = null;
    String fileCid = null;
    int contentMaxLength = 256;

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
                binding.sendMessageContentCount.setTextColor(length >= contentMaxLength ? Color.RED : Color.BLACK);
            }
        });

        binding.sendMessageAddress.setText("epJJ2UqtBU7cEEvAju9YnFCeDF1W75WE75");
        binding.sendMessageName.setText("oldfeel");
    }

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
            fileName = getFileName(uri);
            fileData = getFileBytes(uri);
        }
    });
    ActivityResultLauncher<String> fileSelect = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {
            if (uri == null) {
                return;
            }
            fileName = getFileName(uri);
            fileData = getFileBytes(uri);
        }
    });
    ActivityResultLauncher<String> imageSelect = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {
            if (uri == null) {
                return;
            }
            fileName = getFileName(uri);
            fileData = getFileBytes(uri);
        }
    });

    public byte[] getFileBytes(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getFileName(Uri returnUri) {
        Cursor returnCursor = getContentResolver().query(returnUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        fileName = returnCursor.getString(nameIndex);
        binding.sendMessageFileName.setText("文件名: " + fileName);
        return fileName;
    }

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
        if (binding.sendMessageContent.getText().toString().getBytes().length > contentMaxLength) {
            Toast.makeText(this, "消息太长了", Toast.LENGTH_SHORT).show();
            return;
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
        String txIpfsid = "";
        if (fileCid != null && !fileCid.equals("")) {
            txIpfsid = fileCid;
        }
        boolean allFilled = true;
        String address = binding.sendMessageAddress.getText().toString();
        String amountStr = binding.sendMessageAmount.getText().toString();
        String comment = binding.sendMessageName.getText().toString();
        String txComment = binding.sendMessageContent.getText().toString();
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
//            SpringAnimator.failShakeAnimation(this, balanceText);
//            SpringAnimator.failShakeAnimation(this, feeText);
            Toast.makeText(this, "余额不足", Toast.LENGTH_SHORT).show();
        }

        if (allFilled) {
            Log.e(TAG, "submitSend: allFilled start ");
            BRSender.getInstance().sendTransaction(SendMessage.this, new PaymentItem(new String[]{address}, null, satoshiAmount.longValue(), null, false, comment, composedComment));
            AnalyticsManager.logCustomEvent(BRConstants._20191105_DSL);
        }
    }
}
