package com.eacpay.eactalk;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityListenBinding;
import com.eacpay.presenter.activities.util.BRActivity;

public class Listen extends BRActivity {
    ActivityListenBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle(getString(R.string.listen_privacy));
    }
}
