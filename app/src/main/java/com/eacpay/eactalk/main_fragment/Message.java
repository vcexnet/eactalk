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

import com.eacpay.databinding.FragmentMainMessageBinding;
import com.eacpay.eactalk.main_fragment.MessageFragment.MyLikeList;
import com.eacpay.eactalk.main_fragment.MessageFragment.MyMessageList;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Message extends Fragment {
    FragmentMainMessageBinding binding;
    String[] titles = new String[]{"我的关注", "我的消息"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyAdapter adapter = new MyAdapter(getActivity());
        binding.mainMessageViewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.mainMessageTabs, binding.mainMessageViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles[position]);
            }
        }).attach();
    }

    class MyAdapter extends FragmentStateAdapter {

        public MyAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 1) {
                MyMessageList messageItemList = new MyMessageList();
                return messageItemList;
            }
            MyLikeList myLikeList = new MyLikeList();
            return myLikeList;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
