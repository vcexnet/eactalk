package com.eacpay.eactalk.fragment.main.ContactFragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eacpay.R;
import com.eacpay.databinding.DialogContactCreateBinding;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;

import java.util.List;

public class ContactCreate extends DialogFragment {
    DialogContactCreateBinding binding;
    String address = "";

    public void setAddress(String address) {
        this.address = address;
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
        binding = DialogContactCreateBinding.inflate(getLayoutInflater());
        builder.setView(binding.getRoot())
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        save(dialogInterface);
                        if (onOKListener != null) {
                            onOKListener.onOk();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        binding.dialogContactCreateAddress.setText(address);
        binding.dialogContactCreateClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });
        return builder.create();
    }

    private void save(DialogInterface dialogInterface) {
        String name = binding.dialogContactCreateName.getText().toString();
        String address = binding.dialogContactCreateAddress.getText().toString();

        if (MyUtils.isEmpty(name) || MyUtils.isEmpty(address)) {
            Toast.makeText(getActivity(), getString(R.string.enter_complete_info), Toast.LENGTH_SHORT).show();
            return;
        }
        List<ContactItem> list = BRSharedPrefs.getContactList(getActivity());

        ContactItem item = new ContactItem();
        item.name = name;
        item.address = address;
        list.add(item);

        BRSharedPrefs.putString(getActivity(), "contact_list", new Gson().toJson(list));
        Toast.makeText(getActivity(), getString(R.string.save_success), Toast.LENGTH_SHORT).show();
        dialogInterface.cancel();
    }
}
