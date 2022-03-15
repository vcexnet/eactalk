package com.eacpay.eactalk.main_fragment.MessageFragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eacpay.R;
import com.eacpay.databinding.ItemMyMessageBinding;
import com.eacpay.databinding.RecyclerViewBinding;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.entities.TxItem;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.sqlite.TransactionDataSource;
import com.eacpay.tools.util.BRCurrency;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.wallet.BRPeerManager;
import com.eacpay.wallet.BRWalletManager;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

public class MyMessageList extends Fragment implements TransactionDataSource.OnTxAddedListener, BRPeerManager.OnTxStatusUpdate, BRWalletManager.OnBalanceChanged {
    private static final String TAG = "oldfeel";
    RecyclerViewBinding binding;
    MyMessageAdapter myMessageAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RecyclerViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BRPeerManager.getInstance().addStatusUpdateListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        myMessageAdapter = new MyMessageAdapter();
        binding.recyclerView.setAdapter(myMessageAdapter);

        binding.swiperLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });

        getData();
    }

    private void getData() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.swiperLayout.setRefreshing(true);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                final TxItem[] arr = BRWalletManager.getInstance().getTransactions();
                final List<TxItem> items = arr == null ? null : new LinkedList<>(Arrays.asList(arr));

                long took = (System.currentTimeMillis() - start);
                if (took > 500)
                    Timber.d("updateTxList: took: %s", took);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myMessageAdapter.setItems(items);
                        binding.swiperLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onTxAdded() {
        getData();
    }

    @Override
    public void onStatusUpdate() {
        getData();
    }

    @Override
    public void onBalanceChanged(long balance) {
        getData();
    }

    class MyMessageAdapter extends RecyclerView.Adapter<MyMessageVH> {
        private List<TxItem> items = new ArrayList<>();

        @NonNull
        @Override
        public MyMessageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMyMessageBinding binding = ItemMyMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            MyMessageVH vh = new MyMessageVH(binding.getRoot());
            vh.setBinding(binding);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull MyMessageVH holder, int position) {
            TxItem item = items.get(position);
            Log.e(TAG, "onBindViewHolder: " + new Gson().toJson(item));
            holder.binding.itemMyMessageAddress.setText("到达: " + item.getTo()[0]);
            holder.binding.itemMyMessageTime.setText(MyUtils.formatTimeL(item.getTimeStamp()));
            holder.binding.itemMyMessageTitle.setText(item.getTxComment());

            String iso = BRSharedPrefs.getPreferredLTC(getActivity()) ? "EAC" : BRSharedPrefs.getIso(getContext());
            BigDecimal txAmount = new BigDecimal(item.getReceived() - item.getSent()).abs();
            String amount = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, item.getFee() == -1 ? txAmount : txAmount.subtract(new BigDecimal(item.getFee()))));
            String fee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getFee())));

            boolean received = item.getSent() == 0;
            holder.binding.itemMyMessageIcon.setImageResource(received ? R.drawable.mine_receive : R.drawable.mine_send);

            holder.binding.itemMyMessageAmount.setText((received ? "已接受: " : "已发送: ") + amount + (item.getFee() == -1 ? "" : String.format(getString(R.string.Transaction_fee), fee)));
        }

        @Override
        public int getItemCount() {
            if (items == null) {
                return 0;
            }
            return items.size();
        }

        public void setItems(List<TxItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }
    }

    class MyMessageVH extends RecyclerView.ViewHolder {
        public ItemMyMessageBinding binding;

        public MyMessageVH(@NonNull View itemView) {
            super(itemView);
        }

        public void setBinding(ItemMyMessageBinding binding) {
            this.binding = binding;
        }
    }
}
