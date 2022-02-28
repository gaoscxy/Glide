package com.gaos.glide.glide.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;


import com.gaos.glide.glide.cache.ActiveCache;
import com.gaos.glide.glide.cache.MemoryCache;
import com.gaos.glide.glide.cache.disk.my.DiskBitmapCache;
import com.gaos.glide.glide.request.RequestOptions;
import com.gaos.glide.glide.resource.Key;
import com.gaos.glide.glide.resource.Value;
import com.gaos.glide.glide.resource.ValueCallback;
import com.gaos.glide.glide.util.Tool;
import com.gaos.glide.glide.work.ImageViewTarget;

import java.io.File;

public class Engine implements ValueCallback,ResponseListener{
    private static final String TAG = "Engine";
    /**
     * 1、读缓存（1级，2级，3级）,读取逻辑
     * 2、分发给EngineJob
     */
    private Context glideContext;
    private String path;
    private String key;
    private ActiveCache activeCache; // 活动缓存
    private MemoryCache memoryCache; // 内存缓存
    private DiskBitmapCache diskLruCache; // 磁盘缓存
    private final int MEMORY_MAX_SIZE = 1024*1024*120;
    private static Engine engine;
    RequestOptions requestOptions;
    ImageViewTarget imageViewTarget;

    public static Engine getInstance(){
        if(engine==null){
            engine = new Engine();
        }
        return engine;
    }

    private Engine(){
        if (activeCache == null) {
            activeCache = new ActiveCache(this); // 回调给外界，Value资源不再使用了 设置监听
        }
        if (memoryCache == null) {
            memoryCache = new MemoryCache(MEMORY_MAX_SIZE); // 内存缓存
        }
    }


    public void load(String path,Context context){
        this.path = path;
        this.glideContext = context;
        this.key = new Key(path).getKey();
        diskLruCache = DiskBitmapCache.getInstance(glideContext);
    }

    public void load(Uri uri, Context context){
        this.path = uri.getPath();
        this.glideContext = context;
        this.key = new Key(path).getKey();
        diskLruCache = DiskBitmapCache.getInstance(glideContext);
    }

    public void load(File file, Context context){
        this.path = file.getAbsolutePath();
        this.glideContext = context;
        this.key = new Key(path).getKey();
        diskLruCache = DiskBitmapCache.getInstance(glideContext);
    }

    //读取缓存方法
    public void into(RequestOptions requestOptions, ImageViewTarget imageViewTarget){
        Tool.assertMainThread();
        this.requestOptions = requestOptions;
        //如果本地缓存有，就直接渲染
        Value value = cacheAction(imageViewTarget);//
        this.imageViewTarget = imageViewTarget;
        if(value!=null){
            Bitmap bitmap = value.getmBitmap();
            Matrix matrix = new Matrix();
            if(bitmap!=null) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, requestOptions.getWidth(), requestOptions.getHeight(), matrix, true);

            imageViewTarget.setResource(bitmap);
            }
        }
    }

    private Value cacheAction(ImageViewTarget imageViewTarget){
        //判断读取逻辑,1级缓存-活动缓存有，没有就找内存缓存，没有就去找磁盘缓存，找服务器
        Value value = activeCache.get(key);
        if(value!=null){
            Log.w(TAG,"1111--我是来自于活动缓存的数据");
            //发现活动缓存有，就可以在这里设置进去
            memoryCache.put(key,value);
            //-------------
            //1、发现活动缓存有，就放到内存缓存，然后移除活动缓存
            //2、服务器去获取
            //3、RequestBuilder
            //4、ImageViewTarget
            activeCache.deleteActive(key);
            return value;
        }
         value = memoryCache.get(key);
        if(value!=null){
            // 移动操作 剪切（内存--->活动）
            activeCache.put(key, value); // 把内存缓存中的元素，加入到活动缓存中...
            memoryCache.remove(key); // 移除内存缓存
            Log.w(TAG,"1111--我是来自于内存缓存的数据");
            return value;
        }
        value = diskLruCache.get(key);
        if(value!=null){
            Log.w(TAG,"1111--我是来自于磁盘缓存的数据");
            //磁盘缓存中有，就放到活动缓存，然后移除内存缓存
            activeCache.put(key,value);
            memoryCache.remove(key);
            //真正的glide  key（path,width,height,签名),UUID
            return value;
        }

        //服务器去找
        //--------------------
        new EngineJob().loadResource(path,this,glideContext,imageViewTarget,requestOptions);

        return null;
    }


    //写缓存


    @Override
    public void valueNonUseListener(String key, Value value) {

    }

    @Override
    public void responseSuccess(String key, Value value) {
        Log.d(TAG, "saveCache: >>>>>>>>>>>>>>>>>>>>>>>>>> 加载外置资源成功后 ，保存到缓存中， key:" + key + " value:" + value);
        value.setKey(key);
        if (diskLruCache != null) {
            activeCache.put(key, value);  //这个无所谓 自由控制了
            //这里不需要放内存缓存
            diskLruCache.put(key, value); // 保存到磁盘缓存中....
        }
    }

    @Override
    public void responseException(Exception e) {

    }
}
