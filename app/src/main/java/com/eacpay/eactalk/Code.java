package com.eacpay.eactalk;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.eacpay.R;
import com.eacpay.databinding.ActivityCodeBinding;
import com.eacpay.eactalk.utils.HProgressDialogUtils;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.service.OnFileDownloadListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Code extends BRActivity {
    ActivityCodeBinding binding;
    private String tagName;
    private String downloadUrl;
    boolean isFirst = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.git));

        binding.codeGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://github.com/vcexnet/eactalk");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        binding.codeGitee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://gitee.com/eacpay/eactalk");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        binding.codeUpdateAuto.setChecked(BRSharedPrefs.getBoolean(this, "auto_update", false));
        binding.codeUpdateAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                BRSharedPrefs.putBoolean(Code.this, "auto_update", b);
            }
        });

        binding.codeUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startUpdate();
            }
        });

        getVersionInfoFromGithub();
    }

    private void startUpdate() {
        if (downloadUrl == null) {
            Toast.makeText(this, R.string.update_url_error, Toast.LENGTH_SHORT).show();
            return;
        }
        MyUtils.log("update " + downloadUrl);
        XUpdate.newBuild(this)
                .apkCacheDir(PathUtils.getExternalAppCachePath())
                .build()
                .download(downloadUrl, new OnFileDownloadListener() {
                    @Override
                    public void onStart() {
                        MyUtils.log("update onStart");
                        HProgressDialogUtils.showHorizontalProgressDialog(Code.this, getString(R.string.download_progress), false);
                    }

                    @Override
                    public void onProgress(float progress, long total) {
                        MyUtils.log("update onProgress " + progress + " " + total);
                        HProgressDialogUtils.setProgress(Math.round(progress * 100));
                    }

                    @Override
                    public boolean onCompleted(File file) {
                        MyUtils.log("update onCompleted " + file.getPath());
                        HProgressDialogUtils.cancel();
                        ToastUtils.showLong(getString(R.string.apk_download_success) + file.getPath());
                        AppUtils.installApp(file);
                        return false;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        MyUtils.log("update onError " + throwable.getMessage());
                        HProgressDialogUtils.cancel();
                        if (!isFirst) {
                            ToastUtils.showLong(R.string.apk_download_fail);
                        }
                        if (isFirst) {
                            isFirst = false;
                            downloadUrl = "https://www.eacpay.com/download/eactalk.apk";
                            startUpdate();
                        }
                    }
                });
    }

    private void getVersionInfoFromGithub() {
        binding.codeUpdate.setEnabled(false);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/vcexnet/eactalk/releases/latest")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MyUtils.log("getVersionInfoFromGithub fail");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Code.this, R.string.get_version_info_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                MyUtils.log("getVersionInfoFromGithub success");
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    tagName = jsonObject.getString("tag_name");
                    JSONArray assetsArray = jsonObject.getJSONArray("assets");
                    for (int i = 0; i < assetsArray.length(); i++) {
                        JSONObject assetsObject = assetsArray.getJSONObject(i);
                        if (assetsObject.getString("name").equals("eactalk.apk")) {
                            downloadUrl = assetsObject.getString("browser_download_url");
                            break;
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (tagName != null && !tagName.equals(AppUtils.getAppVersionName())) {
                                binding.codeUpdate.setEnabled(true);
                            }
                            binding.codeVersionInfo.setText(getString(R.string.version_info, AppUtils.getAppVersionName(), tagName));
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
