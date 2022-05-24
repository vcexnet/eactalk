package com.eacpay.eactalk.fragment.main.ContactFragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eacpay.R;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ContactDelete extends DialogFragment {
    List<ContactItem> list = new ArrayList<>();

    public void setList(List<ContactItem> list) {
        this.list = list;
    }

    public interface OnOKListener {
        void onOk();
    }

    OnOKListener onOKListener;

    public void setOnOKListener(OnOKListener onOKListener) {
        this.onOKListener = onOKListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.my_dialog);
        builder.setTitle(getString(R.string.tips))
                .setMessage(R.string.confirm_delete_address)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        submit(dialog);
                        if (onOKListener != null) {
                            onOKListener.onOk();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }

    private void submit(DialogInterface dialog) {
        deleteSelected();

        BRSharedPrefs.putString(getActivity(), "contact_list", new Gson().toJson(list));
        Toast.makeText(getActivity(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show();

        dialog.cancel();
    }

    private void deleteSelected() {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).select) {
                list.remove(i);
                deleteSelected();
            }
        }
    }


}
