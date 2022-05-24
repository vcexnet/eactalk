package com.eacpay.eactalk.ipfs;

import com.eacpay.eactalk.utils.MyUtils;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IpfsItem {
    public String ipfs;
    public IpfsLs ipfsLs; // 执行 ipfs ls 返回的结果
    public byte[] data;
    public String mimeType;
    public int confirmations;
    public boolean loadOK;
    public boolean isLoading;
    public int imageErrCode; // 0: 非 ipfs; 1: 非 dir; 2: dir 中的文件不是视频/图片; 3: 图片加载成功; 4: 加载超时
    public String address;

    public boolean isImage() {
        if (isDir()) {
            return MyUtils.isImageByName(ipfsLs.getFirstName());
        }
        if (isFile()) {
            return mimeType != null && mimeType.startsWith("image");
        }
        return false;
    }

    public boolean isVideo() {
        if (isDir()) {
            return MyUtils.isVideoByName(ipfsLs.getFirstName());
        }
        if (isFile()) {
            return mimeType != null && mimeType.startsWith("video");
        }
        return false;
    }

    public boolean isDir() {
        return ipfsLs != null && ipfsLs.isDir();
    }

    public boolean isFile() {
        return data != null;
    }

    public boolean isParsed() {
        return ipfsLs != null || data != null;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getName().equals("data");
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).create();
        return gson.toJson(this);
    }
}
