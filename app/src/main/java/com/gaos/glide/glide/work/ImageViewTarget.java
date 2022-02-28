package com.gaos.glide.glide.work;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.gaos.glide.glide.binding.inter.LifecycleListener;


public class ImageViewTarget implements LifecycleListener {

    private static final String TAG = "ImageViewTarget.class";
    public ImageViewTarget(ImageView view) {
        this.view = view;
    }
    private ImageView view;

    @Override
    public void onStart() {
        Log.i(TAG, "onStart: ImageViewTarget 做自己的具体业务逻辑处理 ....");
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop: ImageViewTarget 做自己的具体业务逻辑处理 ....");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ImageViewTarget 做自己的具体业务逻辑处理 ....");
    }

    public void setResource(Bitmap resource) {
        view.setImageBitmap(resource);
    }
}
