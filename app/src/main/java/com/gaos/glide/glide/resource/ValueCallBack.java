package com.gaos.glide.glide.resource;

/**
 * 专门给Value，不再使用，的回调接口
 */
public interface ValueCallback {

    /**
     * 监听的方法（Value不再使用了）
     * @param key
     * @param value
     */
    void valueNonUseListener(String key, Value value);

}
