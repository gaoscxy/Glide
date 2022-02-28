package com.gaos.glide.glide.cache.disk.my;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;


import com.gaos.glide.glide.cache.disk.DiskLruCache;
import com.gaos.glide.glide.resource.Value;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * 1、单例模式
 * 2、文件路径，DiskLruCache
 * 3、get
 * 4、put
 */
public class DiskBitmapCache implements BitmapCache{

    private DiskLruCache diskLruCache;
    private static volatile DiskBitmapCache instance;
    private int maxDiskSize = 50 * 1024 *1024;
    private String imageCachePath = "image";
    private File file;
    private Context context;
    //单例
    public static DiskBitmapCache getInstance(Context context) {
        if (instance == null) {
            synchronized (DiskBitmapCache.class) {
                if (instance == null) {
                    instance = new DiskBitmapCache(context);
                }
            }
        }
        return instance;
    }

    //创建构造方法时，对DiskLruCache初始化，open

    private DiskBitmapCache(Context context){
        File file = getImageCacheFile(context,imageCachePath);
        this.file = file;
        if(!file.exists()){
            file.mkdirs();
        }
        this.context = context;
        //第一个传文件夹（私有目录的文件夹），app版本号，对应值数量，单个文件支持的最大大小
        try {

            diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //获取版本号
    private int getAppVersion(Context context) {
        //获取包管理器
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //返回版本号
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }




    //获取图片文件
    private File getImageCacheFile(Context context, String imageCachePath) {
        String path;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            path = context.getExternalCacheDir().getPath();
        } else {
            path = context.getCacheDir().getPath();
        }
        return new File(path + File.separator + imageCachePath);
    }


    /**
     *
     * @param key
     * @param value
     */
    @Override
    public void put(String key, Value value) {
        DiskLruCache.Editor editor =null;
        OutputStream outputStream = null;
        try {
            if(diskLruCache.isClosed()){
                diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
            }
            editor = diskLruCache.edit(key);
            if(editor!=null) {
                outputStream = editor.newOutputStream(0);
                Bitmap bitmap = value.getmBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
           e.printStackTrace();
        }finally {
            try {
                if(editor!=null) {
                    editor.commit();
                }
                if(diskLruCache.isClosed()){
                    diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
                }
                diskLruCache.close();
                if(outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Value get(String key) {
        Value value = new Value();
        InputStream inputStream = null;
        try {
            if(diskLruCache.isClosed()){
                diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
            }
            DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
            if(snapshot==null){
                return null;
            }
             inputStream = snapshot.getInputStream(0);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            value.setmBitmap(bitmap);
            value.setKey(key);
            return value;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public void remove(String key) {
        try {
            diskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
