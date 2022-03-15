package com.eacpay.eactalk.main_fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.eacpay.databinding.FragmentMainHomeBinding;
import com.eacpay.databinding.TabHomeBinding;
import com.eacpay.eactalk.main_fragment.HomeFragment.HomeList;
import com.eacpay.tools.manager.SyncManager;
import com.eacpay.wallet.BRPeerManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Home extends Fragment {
    private static final String TAG = "oldfeel";
    String[] titles = new String[]{"深喉爆料", "昭告天下", "情感广场", "树洞吐槽"};
    String[] addresses = new String[]{"epJ9S6gVFjigZtHvRdK8VDFEJYh8JE16vC", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    FragmentMainHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyAdapter adapter = new MyAdapter(getActivity());
        binding.mainHomeViewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.mainHomeTabs, binding.mainHomeViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles[position]);

                if (position < titles.length - 1) {
                    TabHomeBinding tabHomeBinding = TabHomeBinding.inflate(LayoutInflater.from(getContext()), binding.mainHomeTabs, false);
                    tabHomeBinding.tabHomeTitle.setText(titles[position]);
                    tab.setCustomView(tabHomeBinding.getRoot());
                }

            }
        }).attach();
        for (int i = 0; i < binding.mainHomeTabs.getTabCount(); i++) {
//            LinearLayout linearLayout = (LinearLayout) binding.mainHomeTabs.getChildAt(i);
//            linearLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
//            linearLayout.setDividerDrawable(ContextCompat.getDrawable(getContext(), R.drawable.layout_divider_vertical));
        }

        SyncManager.getInstance().setBinding(binding);
        BRPeerManager.setOnSyncFinished(new BRPeerManager.OnSyncSucceeded() {
            @Override
            public void onFinished() {
                if (isVisible()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.mainHomeSyncView.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    class MyAdapter extends FragmentStateAdapter {

        public MyAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            HomeList addressItemList = new HomeList();
            Bundle args = new Bundle();
            args.putString("address", addresses[position]);
            addressItemList.setArguments(args);
            return addressItemList;
        }

        @Override
        public int getItemCount() {
            return 4;
        }

    }
}
