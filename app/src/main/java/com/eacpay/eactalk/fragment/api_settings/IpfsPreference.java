package com.eacpay.eactalk.fragment.api_settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.eacpay.R;
import com.eacpay.eactalk.ApiSettings;
import com.eacpay.eactalk.fragment.dialog.EditDialog;
import com.eacpay.eactalk.fragment.dialog.OnDialogOkListener;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.google.gson.Gson;

import java.io.File;

public class IpfsPreference extends PreferenceFragmentCompat {
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            MyUtils.log(s);
            MyUtils.log(new Gson().toJson(sharedPreferences.getAll()));
            updateUI();
        }
    };

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        MyUtils.log(intent.getData().toString());
                        File file = new File(intent.getData().getPath());
                        getPreferenceManager().findPreference("path").setSummary(file.getAbsolutePath());

                        save("path", file.getAbsolutePath());
                    }
                }
            });

    private void updateUI() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        DropDownPreference time = getPreferenceManager().findPreference("time");
        if (sharedPreferences.getString("time", "").equals("")) {
            MyUtils.log("ipfsTime set default time");
            save("time", "0");
            time.setValueIndex(time.getEntries().length - 1);
            time.setSummary(time.getEntries()[time.getEntries().length - 1]);
        } else {
            time.setSummary(time.getEntry());
        }

        SwitchPreferenceCompat wifi = getPreferenceManager().findPreference("wifi");
        wifi.setChecked(sharedPreferences.getBoolean("wifi", true));

        final Preference storage = getPreferenceManager().findPreference("storage");
        storage.setSummary(sharedPreferences.getString("storage", "10"));
        storage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditDialog editDialog = new EditDialog();
                final String maxStorage = sharedPreferences.getString("storage", "10");
                editDialog.setDefaultValue(maxStorage);
                editDialog.setTitle(getString(R.string.max_storage));
                editDialog.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                editDialog.setOnDialogOkListener(new OnDialogOkListener() {
                    @Override
                    public void onOk(Object object) {
                        save("storage", object.toString());
                        if (!maxStorage.equals(object.toString())) {
                            if (((ApiSettings) getActivity()) != null) {
                                ((ApiSettings) getActivity()).getMyService().restartIpfs();
                            }
                        }

                        storage.setSummary(object.toString());
                    }
                });
                editDialog.show(getActivity().getSupportFragmentManager(), "edit_dialog");
                return false;
            }
        });

        final Preference path = getPreferenceManager().findPreference("path");
        path.setSummary(sharedPreferences.getString("path", MyUtils.defaultIPFSPath()));
        path.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                if (!path.getSummary().equals("")) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(path.getSummary().toString()));
                }
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent = Intent.createChooser(intent, getString(R.string.select_ipfs_path));
                mStartForResult.launch(intent);
                return false;
            }
        });

        final Preference peerID = getPreferenceManager().findPreference("peerID");
        peerID.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MyUtils.copy(getActivity(), peerID.getSummary().toString());
                Toast.makeText(getActivity(), getString(R.string.copy_success_) + peerID.getSummary().toString(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference ipfsNode = getPreferenceManager().findPreference("ipfs_node");
        ipfsNode.setSummary(sharedPreferences.getString("ipfs_node", MyService.defaultIPFSNode));
        ipfsNode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditDialog editDialog = new EditDialog();
                final String ipfsNodeString = sharedPreferences.getString("ipfs_node", MyService.defaultIPFSNode);
                editDialog.setDefaultValue(ipfsNodeString);
                editDialog.setTitle(getString(R.string.default_node));
                editDialog.setOnDialogOkListener(new OnDialogOkListener() {
                    @Override
                    public void onOk(Object object) {
                        save("ipfs_node", object.toString());
                        if (!ipfsNodeString.equals(object.toString())) {
                            if (((ApiSettings) getActivity()) != null) {
                                ((ApiSettings) getActivity()).getMyService().restartIpfs();
                            }
                        }
                    }
                });
                editDialog.show(getActivity().getSupportFragmentManager(), "ipfs_node");
                return false;
            }
        });

        Preference ipfsGateway = getPreferenceManager().findPreference("ipfs_gateway");
        ipfsGateway.setSummary(sharedPreferences.getString("ipfs_gateway", MyService.defaultIPFSGateway));
        ipfsGateway.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditDialog editDialog = new EditDialog();
                final String ipfsGatewayString = sharedPreferences.getString("ipfs_gateway", MyService.defaultIPFSGateway);
                editDialog.setDefaultValue(ipfsGatewayString);
                editDialog.setTitle(getActivity().getString(R.string.default_gateway));
                editDialog.setOnDialogOkListener(new OnDialogOkListener() {
                    @Override
                    public void onOk(Object object) {
                        save("ipfs_gateway", object.toString());
                    }
                });
                editDialog.show(getActivity().getSupportFragmentManager(), "ipfs_gateway");
                return false;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peerID.setSummary(IpfsManager.getInstance().getPeerID() == null ? "" : IpfsManager.getInstance().getPeerID());
                        }
                    });

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.api_settings_ipfs, rootKey);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        updateUI();
    }

    @Override
    public void onDestroy() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }

    private void save(String key, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString(key, value);
        editor.commit();
    }

    private void setSummary(String key, String value) {
        getPreferenceManager().findPreference(key).setSummary(value);
    }
}
