package com.eacpay.eactalk.ipfs;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.google.gson.Gson;

import java.nio.ByteBuffer;

public class IpfsDataFetcher implements DataFetcher<ByteBuffer> {
    private static final String TAG = "oldfeel";
    private final String model;

    IpfsDataFetcher(String model) {
        this.model = model;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super ByteBuffer> callback) {
        Log.e(TAG, "loadData: " + model);
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (IpfsManager.getInstance().getIpfs() == null);
        try {
            byte[] lsData = IpfsManager.getInstance().getIpfs().newRequest("ls")
                    .withArgument(model.substring(5))
                    .send();

            IpfsLs ipfsLs = new Gson().fromJson(new String(lsData), IpfsLs.class);

            if (ipfsLs.isDir()) {
                IpfsLink link = ipfsLs.Objects.get(0).Links.get(0);
                ipfsDownload(callback, link.Hash);
            } else {
                ipfsDownload(callback, model.substring(5));
            }

        } catch (Exception e) {
            e.printStackTrace();
            ipfsDownload(callback, model.substring(5));
        }
    }

    private void ipfsDownload(DataCallback<? super ByteBuffer> callback, String cid) {
        try {
            final byte[] imageData = IpfsManager.getInstance().getIpfs().newRequest("cat")
                    .withArgument(cid)
                    .send();
            ByteBuffer byteBuffer = ByteBuffer.wrap(imageData);
            callback.onDataReady(byteBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanup() {
        // Intentionally empty only because we're not opening an InputStream or another I/O resource!
    }

    @Override
    public void cancel() {
        // Intentionally empty.
    }

    @NonNull
    @Override
    public Class<ByteBuffer> getDataClass() {
        return ByteBuffer.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
