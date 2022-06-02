package com.eacpay.eactalk.fragment.api_settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.eacpay.R;
import com.eacpay.eactalk.fragment.dialog.EditDialog;
import com.eacpay.eactalk.fragment.dialog.OnDialogOkListener;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomePreference extends PreferenceFragmentCompat {
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            MyUtils.log(s);
            MyUtils.log(new Gson().toJson(sharedPreferences.getAll()));
            updateUI();
        }
    };

    private void updateUI() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        DropDownPreference time = getPreferenceManager().findPreference("home_day");
        if (sharedPreferences.getString("home_day", "").equals("")) {
            save("home_day", "30");
            time.setValueIndex(1);
            time.setSummary(time.getEntries()[1]);
        } else {
            time.setSummary(time.getEntry());
        }

        Preference homeUrl = getPreferenceManager().findPreference("home_url");
        homeUrl.setSummary(sharedPreferences.getString("home_url", MyService.defaultHomeUrl));
        homeUrl.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditDialog editDialog = new EditDialog();
                final String str = sharedPreferences.getString("home_url", MyService.defaultHomeUrl);
                editDialog.setDefaultValue(str);
                editDialog.setTitle(getString(R.string.show_days));
                editDialog.setOnDialogOkListener(new OnDialogOkListener() {
                    @Override
                    public void onOk(Object object) {
                        save("home_url", object.toString());
                    }
                });
                editDialog.show(getActivity().getSupportFragmentManager(), "home_url");
                return false;
            }
        });

        Preference earthCoinUrl = getPreferenceManager().findPreference("earth_coin_url");
        String earthCoinUrlString = sharedPreferences.getString("earth_coin_url", MyService.defaultEarthCoinUrl);
        earthCoinUrl.setSummary(earthCoinUrlString);
        updateHeightAndHard(earthCoinUrlString);
        earthCoinUrl.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EditDialog editDialog = new EditDialog();
                final String str = sharedPreferences.getString("earth_coin_url", MyService.defaultHomeUrl);
                editDialog.setDefaultValue(str);
                editDialog.setTitle(getString(R.string.earth_coin_browser));
                editDialog.setOnDialogOkListener(new OnDialogOkListener() {
                    @Override
                    public void onOk(Object object) {
                        save("earth_coin_url", object.toString());
                        updateHeightAndHard(object.toString());
                    }
                });
                editDialog.show(getActivity().getSupportFragmentManager(), "earth_coin_url");
                return false;
            }
        });

        SwitchPreferenceCompat wifi = getPreferenceManager().findPreference("home_filter");
        wifi.setChecked(sharedPreferences.getBoolean("home_filter", true));
    }

    private void updateHeightAndHard(String earthCoinUrlString) {
        MyUtils.log("updateHeightAndHard: " + earthCoinUrlString);
        final Preference heightPre = getPreferenceManager().findPreference("eac_height");
        final Preference hardPre = getPreferenceManager().findPreference("eac_hard");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(earthCoinUrlString + "/getinfo")
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
                        Toast.makeText(getActivity(), R.string.get_height_hard_error, Toast.LENGTH_SHORT).show();
                    }
                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) {
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    final String eacHeight = jsonObject.getString("headers");
                    final String eacHard = new DecimalFormat("#.00").format(jsonObject.getDouble("difficulty") / 1000) + "KH/s";
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            heightPre.setSummary(eacHeight);
                            hardPre.setSummary(eacHard);
                        }
                    });
                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), R.string.parse_height_hard_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.api_settings_home, rootKey);

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
}
