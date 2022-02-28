package com.gaos.glide.glide.resource;

import android.graphics.Bitmap;

public class Value {
    /**
     * 是一个字符串 LruCache无法识别特殊字符串，所以key不能直接已路径命名，用加密算法加密，转成key
     */
    private String key;
    private Bitmap mBitmap;
    /**
     * 回调，
     */
    private ValueCallback callBack;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Bitmap getmBitmap() {
        return mBitmap;
    }

    public void setmBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    public ValueCallback getCallBack() {
        return callBack;
    }

    public void setCallBack(ValueCallback callBack) {
        this.callBack = callBack;
    }

    /**
     * 回收 回调 释放Value 通过活动缓存 把当前Value移动到LRU内存缓存
     */
    public void recycle(){
        if(callBack != null){
            callBack.valueNonUseListener(key,this);
        }
    }
}
