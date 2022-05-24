package com.eacpay.eactalk;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityAboutBinding;
import com.eacpay.presenter.activities.util.BRActivity;

public class About extends BRActivity {
    ActivityAboutBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle(getString(R.string.about));
    }
}
