package com.eacpay.eactalk.main_fragment.HomeFragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eacpay.R;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.platform.APIClient;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class AddressItemList extends Fragment {
    private static final String TAG = "oldfeel";
    SwipeRefreshLayout swipeRefreshLayout;
    ListView listView;
    private MyAdapter adapter;
    private String address;
    OkHttpClient client = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_address_item_list, container, false);
        swipeRefreshLayout = view.findViewById(R.id.address_item_swiper_layout);
        listView = view.findViewById(R.id.address_item_list);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        address = getArguments().getString("address");

        adapter = new MyAdapter();
        listView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });

        getData();
    }

    private static String urlGET(Context app, String myURL) {
        Request request = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get().build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Timber.i("urlGET: %s resp is null", myURL);
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Timber.i("urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            Timber.e(e);
        } finally {
            if (resp != null) resp.close();
        }
        return response;
    }

    private void getData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = urlGET(getActivity(), "http://apitest.eacpay.com:9000/addresshistory/" + address);
                Log.e(TAG, "getData: " + result);
            }
        }).start();

        swipeRefreshLayout.setRefreshing(true);

        Request request = new Request.Builder()
//                .url("https://blocks.deveac.com:5000/addresshistory/" + address)
                .url("http://apitest.eacpay.com:9000/addresshistory/" + address)
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
                        Log.e(TAG, "run: result " + result);
                        try {
                            List<AddressItemData> dataList = new Gson().fromJson(result, new TypeToken<List<AddressItemData>>() {
                            }.getType());
                            adapter.setDataList(dataList);
                            swipeRefreshLayout.setRefreshing(false);
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
        swipeRefreshLayout.setRefreshing(false);
    }

    class MyAdapter extends BaseAdapter {

        private List<AddressItemData> dataList = new ArrayList<>();

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public AddressItemData getItem(int i) {
            return dataList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = LayoutInflater.from(getActivity()).inflate(R.layout.address_item_view, null, false);
            TextView hash = view.findViewById(R.id.address_item_hash);
            TextView txcomment = view.findViewById(R.id.address_item_txcomment);
            TextView time = view.findViewById(R.id.address_item_time);
            AddressItemData item = getItem(i);
            hash.setText(item.hash);
            txcomment.setText(item.txcomment);
            time.setText(item.time + "");
            return view;
        }

        public void setDataList(List<AddressItemData> dataList) {
            this.dataList = dataList;
            notifyDataSetChanged();
        }
    }
}
