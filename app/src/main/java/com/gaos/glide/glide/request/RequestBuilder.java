package com.gaos.glide.glide.request;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.gaos.glide.glide.engine.Engine;
import com.gaos.glide.glide.work.ImageViewTarget;

import java.io.File;

public class RequestBuilder {
    //就是对类比于Request进行一个封装
    //路径
    // 请求路径
    private String url;

    // 上下文
    private Context context;
    // 占位图片
    private int resId;

    private RequestOptions requestOptions;

    // 回调对象
    private RequestListener requestListener;

    public RequestBuilder(Context context) {
        this.context = context;
    }

    public void into(ImageView imageView){
        //glide 99%在into方法实现的
        //不需要传imageView，而是传target
        final ImageViewTarget imageViewTarget = new ImageViewTarget(imageView);
        imageView.setImageResource(requestOptions.getResId());
        Engine.getInstance().into(requestOptions,imageViewTarget);

    }

    public RequestBuilder apply(RequestOptions requestOptions){
        this.requestOptions=requestOptions;
        return this;
    }

    public RequestBuilder load(String url){
        //真正load交给engine引擎
        Engine.getInstance().load(url,context);
        return this;
    }

    public RequestBuilder load(Uri uri){
        //真正load交给engine引擎
        Engine.getInstance().load(uri,context);
        return this;
    }

    public RequestBuilder load(File file){
        //真正load交给engine引擎
        Engine.getInstance().load(file,context);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public RequestListener getRequestListener() {
        return requestListener;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }
}
