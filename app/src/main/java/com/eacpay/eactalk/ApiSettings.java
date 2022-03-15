package com.eacpay.eactalk;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eacpay.R;
import com.eacpay.databinding.ActivityApiSettingsBinding;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.wallet.BRPeerManager;

import java.util.Timer;
import java.util.TimerTask;

import ipfs.gomobile.android.IPFS;
import ipfs.gomobile.android.RequestBuilder;

public class ApiSettings extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityApiSettingsBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("节点设置");

        binding.apiSettingsApiUrl.setText(BRSharedPrefs.getString(ApiSettings.this, "api_url"));
        binding.apiSettingsIpfsNode.setText(BRSharedPrefs.getString(ApiSettings.this, "ipfs_node"));

        boolean isAuto = BRSharedPrefs.getBoolean(ApiSettings.this, "eac_node_switch", true);
        binding.apiSettingsEacNodeSwitch.setChecked(isAuto);
        binding.apiSettingsEacNodeSwitchText.setText(isAuto ? "自动" : "手动");
        binding.apiSettingsEacNode.setText(BRSharedPrefs.getString(ApiSettings.this, "eac_node"));

        binding.apiSettingsSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRSharedPrefs.getString(ApiSettings.this, "ipfs_node", "/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3")
                        .equals(binding.apiSettingsIpfsNode.getText().toString())) {
                    BRSharedPrefs.putString(ApiSettings.this, "ipfs_node", binding.apiSettingsIpfsNode.getText().toString());
                    String ipfsNode = BRSharedPrefs.getString(ApiSettings.this, "ipfs_node", "/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3");

                    try {
                        byte[] bootStrapData = IpfsManager.getInstance().getIpfs().newRequest("bootstrap")
                                .withArgument("add")
                                .withArgument(ipfsNode)
                                .send();
                        Log.d(TAG, "onClick: bootStrapData " + new String(bootStrapData));
                    } catch (RequestBuilder.RequestBuilderException e) {
                        e.printStackTrace();
                    } catch (IPFS.ShellRequestException e) {
                        e.printStackTrace();
                    }

                }
                BRSharedPrefs.putString(ApiSettings.this, "api_url", binding.apiSettingsApiUrl.getText().toString());
                BRSharedPrefs.putBoolean(ApiSettings.this, "eac_node_switch", binding.apiSettingsEacNodeSwitch.isChecked());
                BRSharedPrefs.putString(ApiSettings.this, "eac_node", binding.apiSettingsEacNode.getText().toString());

                Toast.makeText(ApiSettings.this, "保存成功", Toast.LENGTH_SHORT).show();
            }
        });

        binding.apiSettingsEacNodeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                binding.apiSettingsEacNodeSwitchText.setText(b ? "自动" : "手动");
            }
        });

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(1);
            }
        };
        timer.schedule(timerTask, 2000, 2000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    final String peerName = BRPeerManager.getInstance().getCurrentPeerName();
                    final boolean isConnected = BRPeerManager.getInstance().isConnected();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "run: update eac node status");
                            binding.apiSettingsEacNodeStatus.setText(peerName + " "
                                    + (isConnected ? getString(R.string.NodeSelector_connected) : getString(R.string.NodeSelector_notConnected)));

                        }
                    });
                } while (true);
            }
        }).start();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            updateStatus();
        }
    };

    private void updateStatus() {
        Log.e(TAG, "updateStatus: ipfs");
        binding.apiSettingsIpfsNodeStatus.setText(IpfsManager.getInstance().getIpfsStatus());
    }
}
