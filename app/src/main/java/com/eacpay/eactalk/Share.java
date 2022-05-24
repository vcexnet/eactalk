package com.eacpay.eactalk;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityShareBinding;
import com.eacpay.presenter.activities.util.BRActivity;

import java.io.IOException;
import java.io.OutputStream;

public class Share extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityShareBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.invite_friends));

        binding.shareUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://www.eactalk.com/download");
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
//        binding.shareQrCode.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                saveQRCode();
//            }
//        });

        binding.shareQrCode.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                saveQRCode();
                return false;
            }
        });
    }

    private void saveQRCode() {
        Log.e(TAG, "saveQRCode: start ");
        try {
            Drawable drawable = getResources().getDrawable(R.drawable.qr_code, null);
            if (drawable == null) {
                Log.e(TAG, "saveQRCode: drawable is null");
                return;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri dataUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri fileUri = getContentResolver().insert(dataUri, values);

            if (fileUri == null) {
                Log.e(TAG, "saveQRCode: fileUri is null");
                return;
            }

            OutputStream outStream = getContentResolver().openOutputStream(fileUri);

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();

            sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", fileUri));
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
