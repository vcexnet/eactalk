package com.eacpay.eactalk.fragment.dialog;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eacpay.databinding.DialogShowImageBinding;

public class ShowImage extends DialogFragment {
    DialogShowImageBinding binding;
    Uri imageUri;

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        binding = DialogShowImageBinding.inflate(getLayoutInflater());
        final AlertDialog dialog = builder.setView(binding.getRoot())
                .create();

        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (imageUri != null) {
            binding.showImageView.setImageURI(imageUri);
        }
        binding.showImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });
        return dialog;
    }
}
