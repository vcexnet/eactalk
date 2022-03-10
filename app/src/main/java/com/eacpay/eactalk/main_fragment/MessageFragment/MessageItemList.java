package com.eacpay.eactalk.main_fragment.MessageFragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eacpay.databinding.FragmentMessageItemListBinding;
import com.eacpay.tools.manager.TxManager;
import com.eacpay.tools.sqlite.TransactionDataSource;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.wallet.BRPeerManager;
import com.eacpay.wallet.BRWalletManager;

public class MessageItemList extends Fragment implements TransactionDataSource.OnTxAddedListener, BRPeerManager.OnTxStatusUpdate, BRWalletManager.OnBalanceChanged {
    private static final String TAG = "oldfeel";
    FragmentMessageItemListBinding binding;
    String title = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessageItemListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        title = getArguments().getString("title");
        if (title.equals("我的消息")) {
            TxManager.getInstance().init(getActivity(), binding.addressItemList);

            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "run: updateTxList by message item list onActivityCreated");
                    TxManager.getInstance().updateTxList(getActivity());
                }
            });
        } else {

        }
    }

    @Override
    public void onTxAdded() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "run: updateTxList by message item list onTxAdded");
                TxManager.getInstance().updateTxList(getActivity());
            }
        });
        BRWalletManager.getInstance().refreshBalance(getActivity());
    }

    @Override
    public void onStatusUpdate() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "run: updateTxList by message item list onStatusUpdate");
                TxManager.getInstance().updateTxList(getActivity());
            }
        });
    }

    @Override
    public void onBalanceChanged(long balance) {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "run: updateTxList by message item list onBalanceChanged");
                TxManager.getInstance().updateTxList(getActivity());
            }
        });
    }
}
