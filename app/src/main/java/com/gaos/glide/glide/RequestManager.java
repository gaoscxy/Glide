package com.gaos.glide.glide;

import android.content.Context;
import android.net.Uri;
import android.util.Log;


import com.gaos.glide.glide.binding.inter.LifeCycle;
import com.gaos.glide.glide.binding.inter.LifecycleListener;
import com.gaos.glide.glide.request.RequestBuilder;
import com.gaos.glide.glide.work.DefaultConnectivity;
import com.gaos.glide.glide.work.TargetTracker;

import java.io.File;


public class RequestManager implements LifecycleListener {
    private static final String TAG = "RequestManager.class";
    private static RequestManager requestManager;




    // 伪代码模拟 ...

    // Glide 大量采用 两种方式


    private final TargetTracker targetTracker = new TargetTracker();

    // 第二种写法  Defatxxx 注册到  this.lifecycle.addListener
    private DefaultConnectivity defaultConnectivity;

    private LifeCycle lifecycle;

    private Context mContext;
    private RequestManager(LifeCycle lifecycle, Context context) {
        this.mContext = context;
        this.lifecycle = lifecycle;
        this.lifecycle.addListener(this); // 构造函数 已经给自己注册了【自己给自己绑定】
        defaultConnectivity = new DefaultConnectivity(context);
        this.lifecycle.addListener(defaultConnectivity); // 网络广播注册

    }



    public static RequestManager getInstance(LifeCycle lifecycle, Context context) {

        if (requestManager == null) {
            synchronized (RequestManager.class) {
                if (requestManager == null) {
                    requestManager = new RequestManager(lifecycle,context);
                }
            }
        }

        return requestManager;
    }






    // Activity/Fragment 可见时恢复请求 （onStart() ） 掉用函数
    @Override
    public void onStart() {
        Log.d(TAG, "开始执行生命周期业务 onStart: 运行队列 全部执行，等待队列 全部清空 ....");

        targetTracker.onStart();
        // defaultConnectivity.onStart(); 不需要
    }

    // Activity/Fragment 不可见时暂停请求 （onStop() ） 掉用函数
    @Override
    public void onStop() {
        Log.d(TAG, "开始执行生命周期业务 onStop: 运行队列 全部停止，把任务都加入到等待队列 ....");

        targetTracker.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "开始执行生命周期业务 onDestroy: 自己负责移除自己绑定的生命周期监听，释放操作 ....");
        this.lifecycle.removeListener(this); // 已经给自己销毁了 【自己给自己移除】

        targetTracker.onDestroy();

        this.lifecycle.removeListener(defaultConnectivity); // 网络广播注销
    }


    // 链式调度
    // 加载url
    public RequestBuilder load(String url) {

        return new RequestBuilder(mContext).load(url);
    }

    // 链式调度
    // 加载url
    public RequestBuilder load(Uri uri) {

        return new RequestBuilder(mContext).load(uri);
    }


    // 链式调度
    // 加载url
    public RequestBuilder load(File file) {

        return new RequestBuilder(mContext).load(file);
    }
}
