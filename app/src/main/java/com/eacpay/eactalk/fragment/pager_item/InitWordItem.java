package com.eacpay.eactalk.fragment.pager_item;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eacpay.databinding.ItemInitWordBinding;

public class InitWordItem extends Fragment {
    ItemInitWordBinding itemInitWordBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        itemInitWordBinding = ItemInitWordBinding.inflate(inflater, container, false);
        return itemInitWordBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemInitWordBinding.itemInitWordText.setText(getArguments().getString("text"));
    }
}