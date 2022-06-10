package com.eacpay.eactalk.fragment.main.MessageFragment;

import android.content.Intent;
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
import com.eacpay.eactalk.HomeDetail;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.eactalk.fragment.main.HomeFragment.HomeItem;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.entities.BRTransactionEntity;
import com.eacpay.presenter.entities.TxItem;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.sqlite.TransactionDataSource;
import com.eacpay.tools.util.BRCurrency;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.wallet.BRWalletManager;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

public class MyMessageList extends Fragment implements TransactionDataSource.OnTxAddedListener, BRWalletManager.OnBalanceChanged {
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
    }

    @Override
    public void onResume() {
        super.onResume();
        getData();
    }

    private void getData() {
        if (!isVisible()) {
            return;
        }
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

                final List<BRTransactionEntity> dbList = TransactionDataSource.getInstance(getActivity()).getAllTransactions();
                final List<ContactItem> contactList = BRSharedPrefs.getContactList(getActivity());

                long took = (System.currentTimeMillis() - start);
                if (took > 500)
                    Timber.d("updateTxList: took: %s", took);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myMessageAdapter.setItems(items, dbList, contactList);
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
            final TxItem item = items.get(position);
            holder.binding.itemMyMessageTime.setText(MyUtils.formatTimeL(item.getTimeStamp()));
            holder.binding.itemMyMessageTitle.setText(item.parseComment(getActivity()));
            if (item.isRead()) {
                holder.binding.itemMyMessageTitle.setTextColor(getResources().getColor(R.color.c_999, null));
                holder.binding.itemMyMessageLayout.setBackgroundResource(R.drawable.b_white_10);
            } else {
                holder.binding.itemMyMessageTitle.setTextColor(getResources().getColor(R.color.c_333, null));
                holder.binding.itemMyMessageLayout.setBackgroundResource(R.drawable.b_ff34c4_10);
            }
            if (item.contactName != null && !item.contactName.equals("")) {
                holder.binding.itemMyMessageFrom.setVisibility(View.VISIBLE);
                holder.binding.itemMyMessageFrom.setText(getString(R.string.nickname_) + item.contactName);
            }

            String iso = BRSharedPrefs.getPreferredLTC(getActivity()) ? "EAC" : BRSharedPrefs.getIso(getContext());
            String amount = BRCurrency.getFormattedCurrencyString(getActivity(), iso, item.getAmount(getActivity()));
            String fee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getFee())));

            boolean received = item.getSent() == 0;
            holder.binding.itemMyMessageIcon.setImageResource(received ? R.mipmap.mine_receive : R.mipmap.mine_send);

            holder.binding.itemMyMessageAmount.setText((received ? getString(R.string.received_) : getString(R.string.sended_)) + amount + (item.getFee() == -1 ? "" : String.format(getString(R.string.Transaction_fee), fee)));
            holder.binding.itemMyMessageAddress.setText(received ? (getString(R.string.from_) + item.getFrom()[0]) : (getString(R.string.send_) + item.getTo()[0]));

            final int itemPosition = position;
            holder.binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    BRAnimator.showTransactionPager(getActivity(), items, itemPosition);
                    TransactionDataSource.getInstance(getActivity()).setRead(item.getBlockHeight());
                    item.setRead(true);
//                    notifyDataSetChanged();
                    Intent intent = new Intent(getActivity(), HomeDetail.class);
                    intent.putExtra("item", HomeItem.parseFromTxItem(getActivity(), item));
                    startActivity(intent);
                    Log.e(TAG, "onClick: " + new Gson().toJson(item));
                }
            });
        }

        @Override
        public int getItemCount() {
            if (items == null) {
                return 0;
            }
            return items.size();
        }

        public void setItems(List<TxItem> items, List<BRTransactionEntity> dbList, List<ContactItem> contactList) {
            if (items == null) {
                items = new ArrayList<>();
            }
            if (dbList == null) {
                dbList = new ArrayList<>();
            }
            if (contactList == null) {
                contactList = new ArrayList<>();
            }
            for (int i = 0; i < items.size(); i++) {
                TxItem item = items.get(i);
                for (int j = 0; j < dbList.size(); j++) {
                    BRTransactionEntity entity = dbList.get(j);
                    if (item.getBlockHeight() == entity.getBlockheight()) {
                        items.get(i).setRead(entity.isRead());
                        break;
                    }
                }

                for (int j = 0; j < contactList.size(); j++) {
                    ContactItem contactItem = contactList.get(j);
                    if (contactItem.address.equals(item.getFrom()[0])) {
                        item.contactName = contactItem.name;
                    }
                }
            }
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
