package com.eacpay.eactalk.main_fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eacpay.databinding.FragmentMainSendBinding;
import com.eacpay.eactalk.SendMessage;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.util.BRConstants;

public class Send extends Fragment {
    private static final String TAG = "oldfeel";
    FragmentMainSendBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainSendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        binding.sendNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SendMessage.class);
                intent.putExtra("type", "normal");
                startActivity(intent);
            }
        });

        binding.sendNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SendMessage.class);
                intent.putExtra("type", "notice");
                startActivity(intent);
            }
        });

        binding.sendQrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.openScanner(getActivity(), BRConstants.REQUEST_SEND_MESSAGE);
            }
        });
    }
}
