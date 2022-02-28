package com.gaos.glide.glide.cache.disk.my;


import com.gaos.glide.glide.resource.Value;

public interface BitmapCache {
    /**
     * 对path 加密后的字符串 sha算法加密
     *
     * @param key
     * @param value
     */
    void put(String key, Value value);
    Value get(String key);
    void remove(String key);
}
