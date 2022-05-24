package com.eacpay.eactalk.ipfs;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.nio.ByteBuffer;

public class IpfsModelLoaderFactory implements ModelLoaderFactory<IpfsItem, ByteBuffer> {

    @Override
    public ModelLoader<IpfsItem, ByteBuffer> build(MultiModelLoaderFactory multiFactory) {
        return new IpfsModelLoader();
    }

    @Override
    public void teardown() {
        // Do nothing.
    }
}
