package com.eacpay.eactalk.fragment.main.MessageFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eacpay.R;
import com.eacpay.databinding.FragmentMyLikeBinding;
import com.eacpay.databinding.ItemMyLikeBinding;
import com.eacpay.eactalk.HomeDetail;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.eactalk.fragment.main.HomeFragment.HomeItem;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyLikeList extends Fragment {
    private static final String TAG = "oldfeel";
    FragmentMyLikeBinding binding;
    private MyLikeAdapter myLikeAdapter;
    OkHttpClient client = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyLikeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        myLikeAdapter = new MyLikeAdapter();
        binding.recyclerView.setAdapter(myLikeAdapter);

        binding.swiperLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });

        binding.myLikeReceiver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getData();
            }
        });

        binding.myLikeSend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
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
        if (getActivity() == null) {
            return;
        }

        binding.swiperLayout.setRefreshing(true);
        myLikeAdapter.clear();

        final boolean isReceiver = binding.myLikeReceiver.isChecked();
        final boolean isSend = binding.myLikeSend.isChecked();

        List<ContactItem> contactItems = new ArrayList<>();
        // 联系人详情
        if (getArguments() != null && getArguments().getString("item") != null) {
            ContactItem contactItem = new Gson().fromJson(getArguments().getString("item"), ContactItem.class);
            contactItems.add(contactItem);
        } else { // 我的关注
            contactItems = BRSharedPrefs.getContactList(getActivity());
        }
        for (int i = 0; i < contactItems.size(); i++) {
            String apiUrl = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("home_url", MyService.defaultHomeUrl) + "/addresshistory/" + contactItems.get(i).address;
            final String contactName = contactItems.get(i).name;
            if (!apiUrl.startsWith("http")) {
//                        Toast.makeText(getActivity(), "api url 错误 " + apiUrl, Toast.LENGTH_SHORT).show();
                continue;
            }

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (getActivity() == null) {
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.swiperLayout.setRefreshing(false);
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        return;
                    }

                    String result = response.body().string();
                    try {
                        final List<HomeItem> dataList = new Gson().fromJson(result, new TypeToken<List<HomeItem>>() {
                        }.getType());

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int j = 0; j < dataList.size(); j++) {
                                        dataList.get(j).contactName = contactName;
                                        if (dataList.get(j).type.equals("received") && isReceiver) {
                                            myLikeAdapter.addItem(dataList.get(j));
                                        }

                                        if (dataList.get(j).type.equals("sent") && isSend) {
                                            myLikeAdapter.addItem(dataList.get(j));
                                        }
                                    }
//                                    myLikeAdapter.addList(dataList);
                                    binding.swiperLayout.setRefreshing(false);
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    class MyLikeAdapter extends RecyclerView.Adapter<MyLikeVH> {
        List<HomeItem> list = new ArrayList<>();

        @NonNull
        @Override
        public MyLikeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMyLikeBinding binding = ItemMyLikeBinding.inflate(LayoutInflater.from(getActivity()), parent, false);
            MyLikeVH vh = new MyLikeVH(binding.getRoot());
            vh.setBinding(binding);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull MyLikeVH holder, int position) {
            final HomeItem item = list.get(position);
            holder.binding.itemMyLikeTitle.setText(item.parseComment(getActivity()));
            holder.binding.itemMyLikeName.setText(getString(R.string.nickname_) + item.contactName);
            holder.binding.itemMyLikeTime.setText(getString(R.string.time_) + MyUtils.formatTimeLA(item.time));
            holder.binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MyUtils.log(new Gson().toJson(item));
                    Intent intent = new Intent(getActivity(), HomeDetail.class);
                    intent.putExtra("item", item.toString());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        public void clear() {
            list.clear();
            notifyDataSetChanged();
        }

        public void addItem(HomeItem item) {
            list.add(item);

            Collections.sort(list, new Comparator<HomeItem>() {
                @Override
                public int compare(HomeItem t1, HomeItem t2) {
                    return t1.time > t2.time ? -1 : 1;
                }
            });

            notifyDataSetChanged();
        }

        public void addList(List<HomeItem> dataList) {
            list.addAll(dataList);

            Collections.sort(list, new Comparator<HomeItem>() {
                @Override
                public int compare(HomeItem t1, HomeItem t2) {
                    return t1.time > t2.time ? -1 : 1;
                }
            });

            notifyDataSetChanged();
        }
    }

    class MyLikeVH extends RecyclerView.ViewHolder {
        ItemMyLikeBinding binding;

        public MyLikeVH(@NonNull View itemView) {
            super(itemView);
        }

        public void setBinding(ItemMyLikeBinding binding) {
            this.binding = binding;
        }
    }
}
