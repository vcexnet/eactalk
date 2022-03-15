package com.eacpay.eactalk.main_fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.eacpay.R;
import com.eacpay.databinding.ActivityHomeDetailBinding;
import com.eacpay.eactalk.ipfs.IpfsItem;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.ipfs.OnIpfsLoadComplete;
import com.eacpay.eactalk.main_fragment.HomeFragment.HomeItem;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.util.BRActivity;
import com.google.gson.Gson;

import java.io.File;

public class HomeDetail extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityHomeDetailBinding binding;
    HomeItem homeItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("消息详情");

        Intent intent = getIntent();
        String itemString = intent.getStringExtra("item");
        homeItem = new Gson().fromJson(itemString, HomeItem.class);

        binding.homeDetailStatus.setText("状态: " + homeItem.confirmations + "个确认");
        binding.homeDetailFrom.setText("来自: " + homeItem.sender);
        binding.homeDetailTime.setText("日期: " + MyUtils.formatTimeLA(homeItem.time));

        binding.homeDetailReceive.setText("已接受: Є" + homeItem.value);
        binding.homeDetailId.setText("ID: " + homeItem.hash);

        binding.homeDetailContent.setText(homeItem.txcomment);

        showIpfs();

        binding.homeDetailLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        IpfsManager.getInstance().loadStart(homeItem, new OnIpfsLoadComplete() {
            @Override
            public void onLoad(IpfsItem ipfsItem, int position) {
                homeItem = (HomeItem) ipfsItem;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showIpfs();
                    }
                });
            }
        });
    }

    private void showIpfs() {
        if (homeItem.isShowImage()) {
            binding.homeDetailImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load("ipfs:" + homeItem.ipfs)
                    .placeholder(R.drawable.image_default)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(16)))
                    .into(binding.homeDetailImage);
        } else {
            binding.homeDetailImage.setVisibility(View.GONE);
        }
        if (homeItem.isIpfs()) {
            binding.homeDetailFileOpen.setVisibility(View.VISIBLE);
            binding.homeDetailFileOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fileOpen();
                }
            });
        } else {
            binding.homeDetailFileOpen.setVisibility(View.GONE);
        }
    }

    private void fileOpen() {
        binding.homeDetailFileDownloading.setVisibility(View.VISIBLE);

        new Thread() {
            Uri uri = null;
            String mimeType = null;

            @Override
            public void run() {
                do {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (IpfsManager.getInstance().getIpfs() == null || !homeItem.isParse());
                if (homeItem.isDir()) {
                    try {
                        byte[] data = IpfsManager.getInstance().getIpfs().newRequest("cat")
                                .withArgument(homeItem.ipfsLs.getFirstIpfsCid())
                                .send();

                        mimeType = MyUtils.guessMimeType(data);
                        File file = MyUtils.saveBytesToFile(data, homeItem.ipfsLs.getFirstName());
                        uri = FileProvider.getUriForFile(HomeDetail.this, getApplicationContext().getPackageName() + ".provider", file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (homeItem.isFile()) {
                    try {
                        byte[] data = IpfsManager.getInstance().getIpfs().newRequest("cat")
                                .withArgument(homeItem.ipfs)
                                .send();

                        mimeType = MyUtils.guessMimeType(data);
                        File file = null;
                        if (mimeType.startsWith("image")) {
                            file = MyUtils.saveBytesToFile(data, homeItem.ipfs + ".jpg");
                        }
                        if (mimeType.startsWith("video")) {
                            file = MyUtils.saveBytesToFile(data, homeItem.ipfs + ".mp4");
                        }
                        if (file != null) {
                            uri = Uri.fromFile(file);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (uri != null && mimeType != null) {
                    Log.e(TAG, "run: uri is " + uri.getPath() + " mimeType is " + mimeType);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MimeTypeMap mime = MimeTypeMap.getSingleton();
                            String fileName = uri.getPath();
                            String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
                            String type = mime.getMimeTypeFromExtension(ext);
                            Log.e(TAG, "run: type is " + type);

                            binding.homeDetailFileDownloading.setVisibility(View.GONE);
                            Intent openIntent = new Intent(Intent.ACTION_VIEW);
                            openIntent.setDataAndType(uri, type);
                            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(openIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }.start();
    }
}
