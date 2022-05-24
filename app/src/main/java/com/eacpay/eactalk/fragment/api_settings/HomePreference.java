package com.eacpay.eactalk.fragment.api_settings;

import android.content.SharedPreferences;
import android.os.Bundle;

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
            MyUtils.log("home_day set default time");
            save("home_day", "15");
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
        earthCoinUrl.setSummary(sharedPreferences.getString("earth_coin_url", MyService.defaultEarthCoinUrl));
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
                    }
                });
                editDialog.show(getActivity().getSupportFragmentManager(), "earth_coin_url");
                return false;
            }
        });

        SwitchPreferenceCompat wifi = getPreferenceManager().findPreference("home_filter");
        wifi.setChecked(sharedPreferences.getBoolean("home_filter", true));
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
