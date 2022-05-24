package com.eacpay.eactalk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.eacpay.R;
import com.eacpay.databinding.ActivityApiSettingsBinding;
import com.eacpay.eactalk.fragment.api_settings.ApiSettingsEac;
import com.eacpay.eactalk.fragment.api_settings.HomePreference;
import com.eacpay.eactalk.fragment.api_settings.IpfsPreference;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.presenter.activities.util.BRActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ApiSettings extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityApiSettingsBinding binding;
    String[] titles;
    MyService myService;
    boolean isBound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.node_settings));
        titles = new String[]{getString(R.string.home_url), getString(R.string.earth_coin_node), getString(R.string.ipfs_node)};

        binding.apiSettingsViewPager.setAdapter(new MyAdapter(this));
        new TabLayoutMediator(binding.apiSettingsTab, binding.apiSettingsViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles[position]);
            }
        }).attach();

//        binding.apiSettingsViewPager.setCurrentItem(2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(connection);
        isBound = false;
    }

    public MyService getMyService() {
        return myService;
    }

    public boolean isBound() {
        return isBound;
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyService.EacBinder binder = (MyService.EacBinder) iBinder;
            myService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    class MyAdapter extends FragmentStateAdapter {

        public MyAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new HomePreference();
                case 1:
                    return new ApiSettingsEac();
                case 2:
                    return new IpfsPreference();
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
