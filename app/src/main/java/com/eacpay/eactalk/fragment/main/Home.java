package com.eacpay.eactalk.fragment.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.blankj.utilcode.util.CacheDiskUtils;
import com.blankj.utilcode.util.LanguageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.eacpay.R;
import com.eacpay.databinding.FragmentMainHomeBinding;
import com.eacpay.databinding.TabHomeBinding;
import com.eacpay.eactalk.HomeDetail;
import com.eacpay.eactalk.MainActivity;
import com.eacpay.eactalk.fragment.main.HomeFragment.HomeItem;
import com.eacpay.eactalk.fragment.main.HomeFragment.HomeList;
import com.eacpay.eactalk.fragment.main.HomeFragment.HomeTab;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Home extends Fragment {
    private static final String TAG = "oldfeel";
    String[] titles;
    public static String[] addresses = new String[]{"epJ9S6gVFjigZtHvRdK8VDFEJYh8JE16vC", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    //    String[] addresses = new String[]{"eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ", "ecTt5mii1LA1x2Mk7JfDXF8S8oFtzWhT3L", "epP4dE9tuoUUFNTpzragJg3UeRXEZExoLi", "eTszssBjz6617L6XNBEAUJyeMiLYpN5ijJ"};
    FragmentMainHomeBinding binding;
    List<HomeTab> homeTabList = new ArrayList<>();
    HomeItem[] bannerItemList = new HomeItem[]{null, null, null, null};

    OkHttpClient client = new OkHttpClient();
    private boolean isRunning = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titles = new String[]{getString(R.string.shbl), getString(R.string.zgtx), getString(R.string.qggc), getString(R.string.sdtc)};
        initHomeTab();
        MyAdapter adapter = new MyAdapter(getActivity());
        binding.mainHomeViewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.mainHomeTabs, binding.mainHomeViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles[position]);

                TabHomeBinding tabHomeBinding = TabHomeBinding.inflate(LayoutInflater.from(getContext()), binding.mainHomeTabs, false);
                tabHomeBinding.tabHomeTitle.setText(titles[position]);
                tab.setCustomView(tabHomeBinding.getRoot());
                if (position == titles.length - 1) {
                    tabHomeBinding.tabHomeDivider.setVisibility(View.GONE);
                }

            }
        }).attach();

//        SyncManager.getInstance().setBinding(binding);
//        BRPeerManager.setOnSyncFinished(new BRPeerManager.OnSyncSucceeded() {
//            @Override
//            public void onFinished() {
//                if (isVisible()) {
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            binding.mainHomeSyncView.setVisibility(View.GONE);
//                        }
//                    });
//                }
//            }
//        });

        List<String> bannerList = new ArrayList<>();
        if (LanguageUtils.getSystemLanguage().getLanguage().equals("zh")) {
            bannerList.add("https://eacpay.gitee.io/eactalkweb/1.jpg");
            bannerList.add("https://eacpay.gitee.io/eactalkweb/2.jpg");
            bannerList.add("https://eacpay.gitee.io/eactalkweb/3.jpg");
            bannerList.add("https://eacpay.gitee.io/eactalkweb/4.jpg");
        } else {
            bannerList.add("https://vcexnet.github.io/eactalkweb/1.jpg");
            bannerList.add("https://vcexnet.github.io/eactalkweb/2.jpg");
            bannerList.add("https://vcexnet.github.io/eactalkweb/3.jpg");
            bannerList.add("https://vcexnet.github.io/eactalkweb/4.jpg");
        }

        binding.mainHomeBanner.addBannerLifecycleObserver(this)
                .setIndicator(new CircleIndicator(getActivity()))
                .setAdapter(new BannerImageAdapter<String>(bannerList) {
                    @Override
                    public void onBindView(BannerImageHolder holder, String data, final int position, int size) {
                        holder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        Glide.with(holder.itemView)
                                .load(data)
                                .apply(RequestOptions.bitmapTransform(new RoundedCorners(30)))
                                .into(holder.imageView);
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (bannerItemList[position] != null) {
                                    Intent intent = new Intent(getActivity(), HomeDetail.class);
                                    intent.putExtra("item", bannerItemList[position].toString());
                                    startActivity(intent);
                                }
                            }
                        });
                    }
                });
        getBannerData();
    }

    private void getBannerData() {
        if (getActivity() == null) {
            return;
        }
        final String apiUrl = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("home_url", MyService.defaultHomeUrl) + "/addresshistory/egzQKymTBMJELYycmPRRqQ1csyHsf5aCop";
        if (!apiUrl.startsWith("http")) {
            Toast.makeText(getActivity(), getString(R.string.api_url_error) + apiUrl, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.e(TAG, "getData: " + apiUrl + " " + BRSharedPrefs.getLastBlockHeight(getActivity()));

        final String cacheKey = apiUrl + BRSharedPrefs.getLastBlockHeight(getActivity());

        String result = CacheDiskUtils.getInstance().getString(cacheKey);
        if (result != null && !result.equals("")) {
            parseBannerResult(result);
            return;
        }
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showBannerFailure();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showBannerFailure();
                    return;
                }
                final String result = response.body().string();
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CacheDiskUtils.getInstance().put(cacheKey, result);
                        parseBannerResult(result);
                    }
                });
            }
        });
    }

    private void showBannerFailure() {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void parseBannerResult(String result) {
        try {
            List<HomeItem> newBannerList = new Gson().fromJson(result, new TypeToken<List<HomeItem>>() {
            }.getType());
            for (int i = 0; i < newBannerList.size(); i++) {
                HomeItem item = newBannerList.get(i);
                if (item.value == 500001 && bannerItemList[0] == null) {
                    bannerItemList[0] = item;
                }
                if (item.value == 500002 && bannerItemList[1] == null) {
                    bannerItemList[1] = item;
                }
                if (item.value == 500003 && bannerItemList[2] == null) {
                    bannerItemList[2] = item;
                }
                if (item.value == 500004 && bannerItemList[3] == null) {
                    bannerItemList[3] = item;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        isRunning = false;
    }

    private void initStatus() {
        isRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (getActivity() != null) {
                        if (((MainActivity) getActivity()).getEacService() != null) {
                            ((MainActivity) getActivity()).getEacService().setBinding(binding);
                            ((MainActivity) getActivity()).getEacService().setBindingActivityActivity(getActivity());
                        } else {
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() != null) {
                                    if (((MainActivity) getActivity()).getEacService() != null) {
                                        boolean isConnected = ((MainActivity) getActivity()).getEacService().isEacConnect;
                                        binding.mainHomeEacStatus.setBackgroundResource(isConnected ? R.color.green_text : R.color.c_e41e1e);
                                    }
                                }
                                binding.mainHomeIpfsStatus.setBackgroundResource(IpfsManager.getInstance().getIpfs() == null ? R.color.c_e41e1e : R.color.green_text);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void initHomeTab() {
        homeTabList.clear();
        for (int i = 0; i < titles.length; i++) {
            HomeTab homeTab = new HomeTab();
            homeTab.name = titles[i];
            homeTab.address = addresses[i];
            getData(homeTab, null);
            homeTabList.add(homeTab);
        }
    }

    public void getData(final HomeTab homeTab, final HomeList.OnGetDataSuccess onGetDataSuccess) {
        if (getActivity() == null) {
            return;
        }
        final String apiUrl = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("home_url", MyService.defaultHomeUrl) + "/addresshistory/" + homeTab.address;
        if (!apiUrl.startsWith("http")) {
            Toast.makeText(getActivity(), getString(R.string.api_url_error) + apiUrl, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.e(TAG, "getData: " + apiUrl + " " + BRSharedPrefs.getLastBlockHeight(getActivity()));

        final String cacheKey = apiUrl + BRSharedPrefs.getLastBlockHeight(getActivity());

        String result = CacheDiskUtils.getInstance().getString(cacheKey);
        if (result != null && !result.equals("")) {
            parseDataResult(result, homeTab, onGetDataSuccess);
            return;
        }
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailure(onGetDataSuccess);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showFailure(onGetDataSuccess);
                    return;
                }
                final String result = response.body().string();
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CacheDiskUtils.getInstance().put(cacheKey, result);
                        parseDataResult(result, homeTab, onGetDataSuccess);
                    }
                });
            }
        });

    }

    private void parseDataResult(String result, HomeTab homeTab, HomeList.OnGetDataSuccess onGetDataSuccess) {
        try {
            List<HomeItem> dataList = new Gson().fromJson(result, new TypeToken<List<HomeItem>>() {
            }.getType());
            homeTab.list = dataList;
            for (int i = 0; i < dataList.size(); i++) {
                HomeItem item = dataList.get(i);
                if (item.value > homeTab.value) {
                    homeTab.value = item.value;
                }
            }
            if (onGetDataSuccess != null) {
                onGetDataSuccess.onSuccess(dataList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFailure(final HomeList.OnGetDataSuccess onGetDataSuccess) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), R.string.network_error, Toast.LENGTH_SHORT).show();
                if (onGetDataSuccess != null) {
                    onGetDataSuccess.onSuccess(new ArrayList<HomeItem>());
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
            HomeList homeList = new HomeList();
            homeList.setHome(Home.this);
            homeList.setHomeTab(homeTabList.get(position));
            Bundle args = new Bundle();
            args.putString("address", addresses[position]);
            homeList.setArguments(args);
            return homeList;
        }

        @Override
        public int getItemCount() {
            return 4;
        }

    }
}
