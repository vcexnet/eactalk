package com.eacpay.eactalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityContactDetailBinding;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.eactalk.fragment.main.MessageFragment.MyLikeList;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;

public class ContactDetail extends BRActivity {
    ActivityContactDetailBinding binding;
    ContactItem contactItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.contact_info));

        Intent intent = getIntent();
        String itemString = intent.getStringExtra("item");
        contactItem = new Gson().fromJson(itemString, ContactItem.class);

        binding.contactDetailName.setText(contactItem.name);
        binding.contactDetailAddress.setText(contactItem.address);

        binding.contactDetailCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyUtils.copy(getApplicationContext(), contactItem.address);
                Toast.makeText(ContactDetail.this, getString(R.string.copy_success_) + contactItem.address, Toast.LENGTH_SHORT).show();
            }
        });

        binding.contactDetailEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.contactDetailName.isEnabled()) {
                    binding.contactDetailName.setEnabled(false);
                    binding.contactDetailAddress.setEnabled(false);
                    binding.contactDetailEdit.setText(getString(R.string.edit));

                    contactItem.originalName = contactItem.name;
                    contactItem.name = binding.contactDetailName.getText().toString();
                    contactItem.address = binding.contactDetailAddress.getText().toString();
                    BRSharedPrefs.saveContactItem(ContactDetail.this, contactItem);
                    Toast.makeText(ContactDetail.this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                } else {
                    binding.contactDetailName.setEnabled(true);
                    binding.contactDetailAddress.setEnabled(true);
                    binding.contactDetailEdit.setText(getString(R.string.save));
                }
            }
        });

        binding.contactDetailDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.removeContact(ContactDetail.this, contactItem.name);
                Toast.makeText(ContactDetail.this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        Bundle bundle = new Bundle();
        bundle.putString("item", itemString);
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .add(R.id.contact_detail_list, MyLikeList.class, bundle)
                .commit();
    }
}
