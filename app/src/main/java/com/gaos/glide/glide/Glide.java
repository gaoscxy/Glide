package com.gaos.glide.glide;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Glide {
    /**
     * 步骤
     * 1、先画身体
     * 2、再找流程
     * 3、把原理搞清楚
     * 4、设计模式（建造者模式，工厂模式、门面模式）
     * 5、辅助类
     * 6、只写核心
     *
     * 细节
     *  生命周期的实现
     *  1、空fragment
     *  2、绑定生命周期，接口，
     *  3、适配androidX和FragmentActivity
     *
     */
    private static  volatile Glide glide;
    private final RequestManagerRetriever requestManagerRetriever;

    public Glide(Context context) {
        this.requestManagerRetriever = new RequestManagerRetriever();
    }

    public static RequestManager with(Activity activity){
        return getRetriever(activity).get(activity);
    }


    @NonNull
    private static RequestManagerRetriever getRetriever(@Nullable Context context) {
        return Glide.get(context).getRequestManagerRetriever();
    }

    public static  Glide get(Context context){
        if(glide==null){
            synchronized (Glide.class){
                if(glide==null){
                    glide=new Glide(context);
                }
            }
        }
        return glide;
    }


    @NonNull
    public RequestManagerRetriever getRequestManagerRetriever() {
        return requestManagerRetriever;
    }

}
