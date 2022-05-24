package com.eacpay.eactalk.fragment.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eacpay.R;

public class EditDialog extends DialogFragment {
    String title = "";
    private final int llPadding = 30;
    private int inputType = 0;
    private String defaultValue = "";
    OnDialogOkListener onDialogOkListener;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOnDialogOkListener(OnDialogOkListener onDialogOkListener) {
        this.onDialogOkListener = onDialogOkListener;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(llParams);

        final EditText editText = new EditText(getActivity());
        editText.setLayoutParams(llParams);
        editText.setText(defaultValue);
        if (inputType != 0) {
            editText.setInputType(inputType);
        }
        ll.addView(editText);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.my_dialog)
                .setTitle(title)
                .setView(ll)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (onDialogOkListener != null) {
                            onDialogOkListener.onOk(editText.getText().toString());
                        }
                        dialogInterface.cancel();
                    }
                });

        return builder.create();
    }
}
