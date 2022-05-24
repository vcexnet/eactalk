package com.eacpay.eactalk;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LanguageUtils;
import com.eacpay.R;
import com.eacpay.databinding.ActivityHelpBinding;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.util.BRActivity;

public class Help extends BRActivity {
    ActivityHelpBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle(getString(R.string.help));

        MyUtils.log("LanguageUtils.getSystemLanguage(); " + LanguageUtils.getSystemLanguage());
        binding.helpView.getSettings().setJavaScriptEnabled(true);
        if (LanguageUtils.getSystemLanguage().toString().startsWith("zh_CN")) {
            binding.helpView.loadUrl("https://eacpay.com/sc/help.html");
        } else {
            binding.helpView.loadUrl("https://vcexnet.github.io/eacpayweb/sc/help.html");
        }
    }
}
