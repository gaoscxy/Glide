package com.gaos.glide.glide.cache;

import android.os.Build;
import android.util.LruCache;

import com.gaos.glide.glide.resource.Value;

/**
 * 内存缓存 继承LRUCache
 * LRUCache自带put get等方法 所以不用自己写了
 */
public class MemoryCache extends LruCache<String, Value> {

    public MemoryCache(int maxSize) {
        super(maxSize);
    }

    /**
     * bitmap的大小
     * @param key
     * @param value
     * @return
     */
    @Override
    protected int sizeOf(String key, Value value) {
        int sdkInt = Build.VERSION.SDK_INT;

        if(sdkInt>=Build.VERSION_CODES.KITKAT){
            return value.getmBitmap().getAllocationByteCount();
        }
        return value.getmBitmap().getByteCount();
    }
}
