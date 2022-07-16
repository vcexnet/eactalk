package com.eacpay.eactalk.fragment.main.HomeFragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.eacpay.R;
import com.eacpay.databinding.ItemHomeBinding;
import com.eacpay.databinding.RecyclerViewBinding;
import com.eacpay.eactalk.HomeDetail;
import com.eacpay.eactalk.fragment.main.Home;
import com.eacpay.eactalk.ipfs.IpfsItem;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

public class HomeList extends Fragment {
    private static final String TAG = "oldfeel";
    private MyAdapter myAdapter;
    private String address;
    OkHttpClient client = new OkHttpClient();
    RecyclerViewBinding binding;
    Home home;
    HomeTab homeTab;
    private String ipfsGateway;

    public void setHome(Home home) {
        this.home = home;
    }

    public void setHomeTab(HomeTab homeTab) {
        this.homeTab = homeTab;
    }

    public interface OnGetDataSuccess {
        void onSuccess(List<HomeItem> dataList);
    }

    OnGetDataSuccess onGetDataSuccess;

    public void setOnGetDataSuccess(OnGetDataSuccess onGetDataSuccess) {
        this.onGetDataSuccess = onGetDataSuccess;
    }

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
        ipfsGateway = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ipfs_gateway", MyService.defaultIPFSGateway);

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
        if (getActivity() == null || home == null || homeTab == null) {
            return;
        }
        binding.swiperLayout.setRefreshing(true);

        home.getData(homeTab, new OnGetDataSuccess() {

            @Override
            public void onSuccess(List<HomeItem> dataList) {
                if (getActivity() != null) {
                    myAdapter.setDataList(dataList);
                    binding.swiperLayout.setRefreshing(false);
                }
            }
        });
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private List<HomeItem> dataList = new ArrayList<>();
        Map<String, IpfsItem> map = new HashMap<>();

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemHomeBinding binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            MyViewHolder holder = new MyViewHolder(binding.getRoot());
            holder.setBinding(binding);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
            final HomeItem item = dataList.get(position);

            MyUtils.log("loadOK " + item.loadOK);
            if (item.ipfs != null && !item.ipfs.equals("")) {
                CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(getActivity());
                circularProgressDrawable.setStrokeWidth(5);
                circularProgressDrawable.setCenterRadius(30);
                circularProgressDrawable.start();

                if (map.containsKey(item.ipfs)) {
                    item.setIpfsItem(map.get(item.ipfs));
                }
                if (item.loadOK) {
                    showIpfsByImageErrCode(holder, item);
                } else {
                    Glide.with(getActivity())
                            .load(item)
                            .placeholder(circularProgressDrawable)
                            .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(16)))
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    final IpfsItem ipfsItem = (IpfsItem) model;
                                    MyUtils.log("Glide onLoadFailed " + new Gson().toJson(ipfsItem));
                                    ipfsItem.loadOK = true;
                                    item.setIpfsItem(ipfsItem);
                                    showIpfsByImageErrCode(holder, ipfsItem);
                                    map.put(ipfsItem.ipfs, ipfsItem);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    final IpfsItem ipfsItem = (IpfsItem) model;
                                    MyUtils.log("Glide onResourceReady " + new Gson().toJson(ipfsItem));
                                    holder.binding.itemHomeImage.setVisibility(View.VISIBLE);
                                    holder.binding.itemHomeFile.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(holder.binding.itemHomeImage);
                }
            } else {
                holder.binding.itemHomeImage.setVisibility(View.GONE);
                holder.binding.itemHomeFile.setVisibility(View.VISIBLE);
                holder.binding.itemHomeFile.setText(R.string.no_extra);
                holder.binding.itemHomeFile.setTextColor(getResources().getColor(R.color.c_999, null));
            }

            holder.binding.itemHomeTitle.setText(item.parseComment(getActivity()));
            holder.binding.itemHomeContent.setText(item.parseComment(getActivity()));
            holder.binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), HomeDetail.class);
                    intent.putExtra("item", item.toString());
                    startActivity(intent);
                }
            });
        }

        private void showIpfsByImageErrCode(MyViewHolder holder, final IpfsItem ipfsItem) {
            holder.binding.itemHomeImage.setVisibility(View.GONE);
            holder.binding.itemHomeFile.setVisibility(View.VISIBLE);
            if (ipfsItem.imageErrCode == 0) {
                holder.binding.itemHomeFile.setText(R.string.no_extra);
            } else if (ipfsItem.imageErrCode == 1 || ipfsItem.imageErrCode == 4) {
                holder.binding.itemHomeFile.setText(getString(R.string.extra_) + ipfsItem.ipfs);
                holder.binding.itemHomeFile.setTextColor(getResources().getColor(R.color.primary, null));
                holder.binding.itemHomeFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String txUrl = ipfsGateway + "/ipfs/" + ipfsItem.ipfs;
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(txUrl));
                        startActivity(browserIntent);
                    }
                });
            } else if (ipfsItem.imageErrCode == 2) {
                String fileName = ipfsItem.ipfsLs.getFirstName();
                holder.binding.itemHomeFile.setText(getString(R.string.extra_) + fileName);
            } else if (ipfsItem.imageErrCode == 3) {
                holder.binding.itemHomeImage.setVisibility(View.VISIBLE);
                holder.binding.itemHomeFile.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public void setDataList(List<HomeItem> dataList) {
            // if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("home_filter", true)) {
            //     dataList = filterList(dataList, 0);
            // }
            dataList = filterList(dataList, 0);

            Collections.sort(dataList, new Comparator<HomeItem>() {
                @Override
                public int compare(HomeItem t1, HomeItem t2) {
                    if (t1.value == t2.value) {
                        return t1.time > t2.time ? -1 : 1;
                    }
                    return t1.value > t2.value ? -1 : 1;
                }
            });

            this.dataList = dataList;
            notifyDataSetChanged();
        }

        private List<HomeItem> filterList(List<HomeItem> dataList, int position) {
            for (int i = position; i < dataList.size(); i++) {
                HomeItem item = dataList.get(i);
//                if (item.value < getMinEac(homeTab.address) || (getMinTime() > 0 && item.time < getMinTime())) {
//                    dataList.remove(i);
//                    return filterList(dataList, i);
//                }

                if (item.value < getMinEac(homeTab.address) || item.time < getMinTime()) {
                    dataList.remove(i);
                    return filterList(dataList, i);
                }
            }
            return dataList;
        }
    }

    private double getMinEac(String address) {
        if (address.equals(Home.addresses[0])) {
            return 50000;
        } else if (address.equals(Home.addresses[1])) {
            return 100000;
        } else if (address.equals(Home.addresses[2])) {
            return 1000;
        } else if (address.equals(Home.addresses[3])) {
            return 100;
        }
        return 0;
    }

    private long getMinTime() {
        String homeDayStr = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("home_day", "30");
        int homeDay = Integer.parseInt(homeDayStr);
        Calendar c = Calendar.getInstance();
        if (homeDay == 0) {
            c.setTimeInMillis(0);
        } else {
            c.add(Calendar.DATE, -homeDay);
        }
        return c.getTimeInMillis() / 1000;
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
