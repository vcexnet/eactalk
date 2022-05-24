package com.platform.interfaces;

import com.platform.kvstore.CompletionObject;
public interface KVStoreAdaptor {

    CompletionObject ver(String key);

    CompletionObject put(String key, byte[] value, long version);

    CompletionObject del(String key, long version);

    CompletionObject get(String key, long version);

    CompletionObject keys();

}
