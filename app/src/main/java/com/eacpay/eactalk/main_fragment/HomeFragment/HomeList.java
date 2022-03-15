package com.eacpay.eactalk.main_fragment.HomeFragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.eacpay.R;
import com.eacpay.databinding.ItemHomeBinding;
import com.eacpay.databinding.RecyclerViewBinding;
import com.eacpay.eactalk.ipfs.IpfsItem;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.ipfs.OnIpfsLoadComplete;
import com.eacpay.eactalk.main_fragment.HomeDetail;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeList extends Fragment {
    private static final String TAG = "oldfeel";
    private MyAdapter myAdapter;
    private String address;
    OkHttpClient client = new OkHttpClient();
    RecyclerViewBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RecyclerViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        address = getArguments().getString("address");

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        myAdapter = new MyAdapter();
        binding.recyclerView.setAdapter(myAdapter);

        binding.swiperLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getData();
    }

    ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            Log.e(TAG, "onActivityResult: requestPermissionLauncher " + result);
        }
    });

    private void getData() {
        String apiUrl = BRSharedPrefs.getString(getActivity(), "api_url", "http://apitest.eacpay.com:9000/addresshistory/") + address;
        Log.e(TAG, "getData: api url " + apiUrl);
        if (!apiUrl.startsWith("http")) {
            Toast.makeText(getActivity(), "api url 错误 " + apiUrl, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.swiperLayout.setRefreshing(true);
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailure();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showFailure();
                    return;
                }
                final String result = response.body().string();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<HomeItem> dataList = new Gson().fromJson(result, new TypeToken<List<HomeItem>>() {
                            }.getType());
                            myAdapter.setDataList(dataList);
                            binding.swiperLayout.setRefreshing(false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    private void showFailure() {
        Looper.prepare();
        Toast.makeText(getActivity(), "网络连接失败", Toast.LENGTH_SHORT).show();
        binding.swiperLayout.setRefreshing(false);
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private List<HomeItem> dataList = new ArrayList<>();

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemHomeBinding binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            MyViewHolder holder = new MyViewHolder(binding.getRoot());
            holder.setBinding(binding);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            final HomeItem item = dataList.get(position);
            if (item.isDir()) { // 目录的 cid
                String fileName = item.ipfsLs.getFirstName();

                if (MyUtils.isImageByName(fileName)) {
                } else if (MyUtils.isVideoByName(fileName)) {
                } else {
                }

                holder.binding.itemHomeFile.setText("目录附件: " + fileName);
            } else if (item.isFile()) { // 文件的 cid
                if (item.mimeType != null && item.mimeType.startsWith("image")) {
                } else if (item.mimeType != null && item.mimeType.startsWith("video")) {
                } else {
                }

                holder.binding.itemHomeFile.setText("文件附件: " + item.mimeType + " ");
            } else { // 默认当做是图片处理
                loadImage(item, holder);
            }
            if (item.isShowImage()) {
                loadImage(item, holder);
            } else {
                holder.binding.itemHomeImage.setVisibility(View.GONE);
            }
            holder.binding.itemHomeTitle.setText(item.txcomment);
            holder.binding.itemHomeContent.setText(item.txcomment);
            holder.binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), HomeDetail.class);
                    Log.e(TAG, "onClick: " + item.toString());
                    intent.putExtra("item", item.toString());
                    startActivity(intent);
                }
            });
        }

        private void loadImage(HomeItem item, MyViewHolder holder) {
            if (item.ipfs != null && !item.ipfs.equals("")) {
                holder.binding.itemHomeImage.setVisibility(View.VISIBLE);
                Glide.with(getActivity())
                        .load("ipfs:" + item.ipfs)
                        .placeholder(R.drawable.image_default)
                        .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(16)))
                        .into(holder.binding.itemHomeImage);
            } else {
                holder.binding.itemHomeImage.setVisibility(View.GONE);
                holder.binding.itemHomeFile.setText("无附件");
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public void setDataList(List<HomeItem> dataList) {
            this.dataList = dataList;
            notifyDataSetChanged();

            IpfsManager.getInstance().loadStart(dataList, new OnIpfsLoadComplete() {
                @Override
                public void onLoad(final IpfsItem ipfsItem, final int position) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myAdapter.dataList.set(position, (HomeItem) ipfsItem);
                            myAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ItemHomeBinding binding;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setBinding(ItemHomeBinding binding) {
            this.binding = binding;
        }
    }
}
