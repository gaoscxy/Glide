package com.gaos.glide.glide.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;


import com.gaos.glide.glide.request.RequestOptions;
import com.gaos.glide.glide.resource.Key;
import com.gaos.glide.glide.resource.Value;
import com.gaos.glide.glide.util.MainThreadExecutor;
import com.gaos.glide.glide.util.ThreadPoolManager;
import com.gaos.glide.glide.work.ImageViewTarget;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class EngineJob implements Runnable{
    private String path;
    private ResponseListener responseListener;
    private Context context;
    RequestOptions requestOptions;
    private ImageViewTarget imageViewTarget;
    public Value loadResource(String path, ResponseListener responseListener, Context context, ImageViewTarget imageViewTarget, RequestOptions requestOptions){
        //执行线程，线程池
        this.path = path;
        this.responseListener = responseListener;
        this.context = context;
        this.imageViewTarget = imageViewTarget;
        this.requestOptions = requestOptions;
        Uri uri = Uri.parse(path);
        if("HTTP".equalsIgnoreCase(uri.getScheme())||"HTTPS".equalsIgnoreCase(uri.getScheme())){
            ThreadPoolManager.getInstance().execute(this);
        }else{
            //区分本地还是网络
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            Value value = new Value();
            value.setmBitmap(bitmap);
            String key = new Key(path).getKey();
            value.setKey(key);
            imageViewTarget.setResource(bitmap);
            responseListener.responseSuccess(key,value);
        }
        return null;
    }
    @Override
    public void run() {
        //第三方Glide做了很多步，这里就一步做了
        //这里就去做接口请求，由子线程直接转为主线程
        final Bitmap bitmap = getImageBitmap(path);
        Executor executor = new MainThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Value value = new Value();
                Log.w("EngineJob","1111--我是来自于服务器获取的数据");
                value.setmBitmap(bitmap);
                String key = new Key(path).getKey();
                responseListener.responseSuccess(key,value);

                imageViewTarget.setResource(bitmap);
            }
        });

    }


    private Bitmap getImageBitmap(String url){
        Bitmap bitmap=null;
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL imageUrl = new URL(url);
            //要用到HttpURLConnection
            conn = (HttpURLConnection) imageUrl.openConnection();
            conn.connect();
             is = conn.getInputStream();
            //Bitmap工厂类，流转化成Bitmap
            bitmap = BitmapFactory.decodeStream(is);
            Matrix matrix = new Matrix();
            bitmap =  Bitmap.createBitmap(bitmap,0,0,requestOptions.getWidth(), requestOptions.getHeight(), matrix,true);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(is!=null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(conn!=null) {
                conn.disconnect();
            }
        }
        return bitmap;
    }
}
