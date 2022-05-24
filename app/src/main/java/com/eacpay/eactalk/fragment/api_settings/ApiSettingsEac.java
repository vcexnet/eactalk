package com.eacpay.eactalk.fragment.api_settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eacpay.R;
import com.eacpay.databinding.FragmentApiSettingsEacBinding;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.wallet.BRPeerManager;

public class ApiSettingsEac extends Fragment {
    FragmentApiSettingsEacBinding binding;

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
    }
}
