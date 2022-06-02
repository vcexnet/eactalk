package com.eacpay.eactalk.fragment.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.eacpay.R;
import com.eacpay.databinding.DialogReplyTipsBinding;

import java.util.Timer;
import java.util.TimerTask;

public class ReplyTips extends DialogFragment {
    DialogReplyTipsBinding binding;

    OnDialogOkListener onDialogOkListener;

    int timeLast = 3;

    public void setOnDialogOkListener(OnDialogOkListener onDialogOkListener) {
        this.onDialogOkListener = onDialogOkListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.my_dialog);
        binding = DialogReplyTipsBinding.inflate(getLayoutInflater());
        builder.setView(binding.getRoot())
                .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        if (onDialogOkListener != null) {
                            onDialogOkListener.onOk(null);
                        }
                        onDialogOkListener = null;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        onDialogOkListener = null;
                    }
                });
        binding.dialogReplyTipsClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });

        updateContent();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (timeLast <= 0) {
                    timer.cancel();
                    if (onDialogOkListener != null) {
                        onDialogOkListener.onOk(null);
                    }
                    if (getDialog() != null) {
                        getDialog().cancel();
                    }
                    return;
                }
                if (getDialog() == null || !getDialog().isShowing()) {
                    return;
                }
                timeLast--;
                updateContent();
            }
        }, 1000, 1000);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void updateContent() {
        if (getActivity() == null) {
            return;
        }
        binding.dialogReplyTipsContent.setText(Html.fromHtml(getActivity().getString(R.string.confirm_send_message, timeLast), HtmlCompat.FROM_HTML_MODE_LEGACY));
    }
}
