package com.eacpay.eactalk.ipfs;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.eacpay.eactalk.utils.MyUtils;
import com.google.gson.Gson;

import java.nio.ByteBuffer;

public class IpfsDataFetcher implements DataFetcher<ByteBuffer> {
    public static String ENCRYPT_PREFIX = "E__";
    private static final String TAG = "oldfeel";
    private final IpfsItem model;
    boolean isLoading = true;
    int loadedTime = 0;

    IpfsDataFetcher(IpfsItem model) {
        this.model = model;
    }

    @Override
    public void loadData(Priority priority, final DataCallback<? super ByteBuffer> callback) {
        Log.e(TAG, "glide loadData: " + model.ipfs);
        isLoading = true;
        loadedTime = 0;

        while (IpfsManager.getInstance().getIpfs() == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        IpfsManager.getInstance().getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] lsData = IpfsManager.getInstance().getIpfs().newRequest("ls")
                            .withArgument(model.ipfs)
                            .send();

                    IpfsLs ipfsLs = new Gson().fromJson(new String(lsData), IpfsLs.class);
                    model.ipfsLs = ipfsLs;

                    if (ipfsLs.isDir()) {
                        IpfsLink link = ipfsLs.Objects.get(0).Links.get(0);
                        if (MyUtils.isVideoByName(link.Name) || MyUtils.isImageByName(link.Name)) {
                            ipfsDownload(callback, link.Hash);
                        } else {
                            model.imageErrCode = 2;
                            loadFailed(callback, "2: not image " + link.Name);
                        }
                    } else {
                        model.imageErrCode = 1;
                        loadFailed(callback, "1: not dir");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    loadFailed(callback, "1: not dir");
                }
                isLoading = false;
            }
        });

        while (isLoading && loadedTime < 5 * 1000) { // 加载 5 秒,
            try {
                loadedTime += 500;
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (model.imageErrCode == 0) {
            model.imageErrCode = 4;
            loadFailed(callback, "4: time out");
        }
    }

    private void loadFailed(DataCallback<? super ByteBuffer> callback, String s) {
        if (s == null) {
            return;
        }
        Exception exception = new Exception(s);
        if (callback != null) {
            callback.onLoadFailed(exception);
        }
    }

    private void ipfsDownload(DataCallback<? super ByteBuffer> callback, String cid) {
        try {
            byte[] imageData = IpfsManager.getInstance().getIpfs().newRequest("cat")
                    .withArgument(cid)
                    .send();

            if (model.ipfsLs.getFirstName().startsWith(ENCRYPT_PREFIX)) {
                imageData = MyUtils.decryptFile(imageData, model.address);
            }

            String mimeType = MyUtils.guessMimeType(imageData);
            model.mimeType = mimeType;
            if (mimeType.startsWith("image") || mimeType.startsWith("video")) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(imageData);
                model.imageErrCode = 3;
                callback.onDataReady(byteBuffer);
            } else {
                model.imageErrCode = 2;
                loadFailed(callback, "3: file not image");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.imageErrCode = 2;
            loadFailed(callback, "3: file not image");
        }
    }

    @Override
    public void cleanup() {
        // Intentionally empty only because we're not opening an InputStream or another I/O resource!
        MyUtils.log("clean up " + model.ipfs);
    }

    @Override
    public void cancel() {
        // Intentionally empty.
        MyUtils.log("cancel " + model.ipfs);
    }

    @NonNull
    @Override
    public Class<ByteBuffer> getDataClass() {
        return ByteBuffer.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
