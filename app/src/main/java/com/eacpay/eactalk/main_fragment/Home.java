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
import androidx.viewpager2.widget.ViewPager2;

import com.eacpay.R;
import com.eacpay.eactalk.main_fragment.HomeFragment.AddressItemList;
import com.eacpay.tools.adapter.TransactionListAdapter;
import com.eacpay.tools.manager.TxManager;
import com.eacpay.wallet.BRPeerManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Home extends Fragment {
    private static final String TAG = "oldfeel";
    String[] titles = new String[]{"深喉爆料", "昭告天下", "情感广场", "树洞吐槽"};
    String[] addresses = new String[]{"epJ9S6gVFjigZtHvRdK8VDFEJYh8JE16vC", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    ViewPager2 viewPager2;
    TabLayout tabLayout;
    private View syncView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_home, container, false);
        viewPager2 = view.findViewById(R.id.main_home_view_pager);
        tabLayout = view.findViewById(R.id.main_home_tabs);

        syncView = view.findViewById(R.id.main_layout);
        TransactionListAdapter.SyncingHolder syncing = new TransactionListAdapter.SyncingHolder(syncView);

        TxManager.getInstance().syncingHolder = syncing;
        syncing.mainLayout.setBackgroundResource(R.drawable.tx_rounded);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyAdapter adapter = new MyAdapter(getActivity());
        viewPager2.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles[position]);
            }
        }).attach();

        BRPeerManager.setOnSyncFinished(new BRPeerManager.OnSyncSucceeded() {
            @Override
            public void onFinished() {
                if (isVisible() && syncView != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            syncView.setVisibility(View.GONE);
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
            AddressItemList addressItemList = new AddressItemList();
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
