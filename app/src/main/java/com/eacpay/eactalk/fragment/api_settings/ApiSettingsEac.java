package com.eacpay.eactalk.fragment.api_settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.eacpay.R;
import com.eacpay.databinding.FragmentApiSettingsEacBinding;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.wallet.BRPeerManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

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

public class ApiSettingsEac extends Fragment {
    FragmentApiSettingsEacBinding binding;
    private EacPeerInfoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentApiSettingsEacBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean isAuto = BRSharedPrefs.getBoolean(getActivity(), "eac_node_switch", true);
        binding.apiSettingsEacNodeSwitch.setChecked(isAuto);
        binding.apiSettingsEacNodeSwitchText.setText(isAuto ? getString(R.string.automatic) : getString(R.string.manual));
        binding.apiSettingsEacNode.setText(BRSharedPrefs.getString(getActivity(), "eac_node"));

        binding.apiSettingsEacNodeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                binding.apiSettingsEacNodeSwitchText.setText(b ? getString(R.string.automatic) : getString(R.string.manual));
            }
        });

        binding.apiSettingsEacSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putBoolean(getActivity(), "eac_node_switch", binding.apiSettingsEacNodeSwitch.isChecked());
                BRSharedPrefs.putString(getActivity(), "eac_node", binding.apiSettingsEacNode.getText().toString());

                Toast.makeText(getActivity(), getString(R.string.save_success), Toast.LENGTH_SHORT).show();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isRunning = true;
                while (isRunning) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    final String peerName = BRPeerManager.getInstance().getCurrentPeerName();
                    final boolean isConnected = BRPeerManager.getInstance().isConnected();
                    if (getActivity() == null) {
                        isRunning = false;
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.apiSettingsEacNodeStatus.setText(peerName + " "
                                    + (isConnected ? getString(R.string.NodeSelector_connected) : getString(R.string.NodeSelector_notConnected)));

                        }
                    });
                }
            }
        }).start();

        adapter = new EacPeerInfoAdapter();
        binding.apiSettingsEacPeerList.setAdapter(adapter);

        String earthCoinUrlString = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("earth_coin_url", MyService.defaultEarthCoinUrl);
        getPeerInfo(earthCoinUrlString);
    }

    private void getPeerInfo(String earthCoinUrlString) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(earthCoinUrlString + "/getpeerinfo")
                .build();
        MyUtils.log(earthCoinUrlString + "/getpeerinfo");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), R.string.get_peer_info_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                final List<EacPeerInfoItem> list = new Gson().fromJson(response.body().string(), new TypeToken<List<EacPeerInfoItem>>() {
                }.getType());

                if (getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setList(list);
                    }
                });
            }
        });

    }

    class EacPeerInfoAdapter extends BaseAdapter {
        List<EacPeerInfoItem> list = new ArrayList<>();

        @Override
        public int getCount() {
            return list.size() + 1;
        }

        @Override
        public EacPeerInfoItem getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = getLayoutInflater().inflate(R.layout.item_eac_peer_info, null);
            TextView textView = view.findViewById(R.id.eac_peer_info_text);

            if (i == list.size()) {
                textView.setText(R.string.eac_help);
            } else {
                EacPeerInfoItem item = getItem(i);
                textView.setText(item.addr + " | " + item.synced_headers + " | " + item.subver.replaceAll("/", ""));
            }
            return view;
        }

        public void setList(List<EacPeerInfoItem> list) {
            Collections.sort(list, new Comparator<EacPeerInfoItem>() {
                @Override
                public int compare(EacPeerInfoItem t1, EacPeerInfoItem t2) {
                    if (t1.subver.equals(t2.subver)) {
                        return t1.synced_headers > t2.synced_headers ? -1 : 1;
                    }
                    return t1.isLargeVersion(t2) ? -1 : 1;
                }
            });
            if (list.size() > 10) {
                list = list.subList(0, 10);
            }

            this.list = list;
            notifyDataSetChanged();
        }
    }
}
