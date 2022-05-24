package com.eacpay.eactalk.ipfs;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Loads an {@link InputStream} from a Base 64 encoded String.
 */
public final class IpfsModelLoader implements ModelLoader<IpfsItem, ByteBuffer> {
    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(IpfsItem model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model), new IpfsDataFetcher(model));
    }

    @Override
    public boolean handles(IpfsItem model) {
        return model.ipfs != null && !model.ipfs.equals("");
    }
}
