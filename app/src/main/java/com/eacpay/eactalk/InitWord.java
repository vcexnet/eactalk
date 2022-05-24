package com.eacpay.eactalk;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.eacpay.R;
import com.eacpay.databinding.ActivityInitWordBinding;
import com.eacpay.eactalk.fragment.pager_item.InitWordItem;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.security.BRKeyStore;
import com.eacpay.tools.security.PostAuth;
import com.eacpay.tools.util.BRConstants;

import timber.log.Timber;

public class InitWord extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityInitWordBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInitWordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.set_key));

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        binding.initWordViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.initWordPre.setEnabled(position != 0);
//                binding.initWordNext.setEnabled(position != 11);
                binding.initWordPosition.setText((position + 1) + "/" + 12);
            }
        });

        binding.initWordPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int current = binding.initWordViewPager.getCurrentItem();
                if (current > 0) {
                    current--;
                }
                binding.initWordViewPager.setCurrentItem(current);
            }
        });

        binding.initWordNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int current = binding.initWordViewPager.getCurrentItem();
                if (current < 11) {
                    current++;
                    binding.initWordViewPager.setCurrentItem(current);
                } else {
                    PostAuth.getInstance().onPhraseProveAuth(InitWord.this, false);
                }
            }
        });

        String cleanPhrase = getIntent().getStringExtra("phrase");
        if (cleanPhrase == null) {
            cleanPhrase = createPhrase();
        }
        Log.e(TAG, "onCreate: cleanPhrase " + cleanPhrase);
        String[] words = cleanPhrase.split(" ");
        MyAdapter adapter = new MyAdapter(this);
        adapter.setWords(words);
        binding.initWordViewPager.setAdapter(adapter);
    }

    private String createPhrase() {
        String cleanPhrase = null;
        try {
            byte[] raw = BRKeyStore.getPhrase(this, BRConstants.SHOW_PHRASE_REQUEST_CODE);
            if (raw == null) {
                NullPointerException ex = new NullPointerException("onPhraseCheckAuth: getPhrase = null");
                Timber.e(ex);
                throw ex;
            }
            cleanPhrase = new String(raw);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cleanPhrase;
    }

    class MyAdapter extends FragmentStateAdapter {
        String[] words;

        public MyAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            InitWordItem fragment = new InitWordItem();
            Bundle bundle = new Bundle();
            bundle.putString("text", words[position]);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return words.length;
        }

        public void setWords(String[] words) {
            this.words = words;
        }
    }
}
