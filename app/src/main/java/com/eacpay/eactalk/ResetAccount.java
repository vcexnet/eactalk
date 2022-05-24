package com.eacpay.eactalk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityResetAccountBinding;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.animation.BRAnimator;

public class ResetAccount extends BRActivity {
    ActivityResetAccountBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.start_recovery_account);

        binding.resetAccountNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                Intent intent = new Intent(ResetAccount.this, InputWord.class);
                intent.putExtra("restore", true);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                if (!ResetAccount.this.isDestroyed()) finish();
            }
        });
    }
}
