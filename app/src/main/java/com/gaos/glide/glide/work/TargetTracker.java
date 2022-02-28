package com.gaos.glide.glide.work;

import android.util.Log;

import com.gaos.glide.glide.binding.inter.LifecycleListener;


public class TargetTracker implements LifecycleListener {

    private static final String TAG = "TargetTracker";

    public TargetTracker() {

    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart: TargetTracker 做自己的具体业务逻辑处理 ....");
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop: TargetTracker 做自己的具体业务逻辑处理 ....");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: TargetTracker 做自己的具体业务逻辑处理 ....");
    }
}
