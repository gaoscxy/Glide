package com.gaos.glide.glide.request;

public class RequestOptions {
    /**
     * 需要什么功能，就在这里面加，在其他地方实现即可
     */
    private int width;
    private int height;
    private int resId;

    //1、你要实现glide有的方法
    //2、对你的设置项封装
    //3、根据需求来设置属性
    public RequestOptions override(int width,int height){
        this.width = width;
        this.height = height;
        return this;
    }

    public RequestOptions placeholder(int resId){
        this.resId = resId;
        return this;
    }

    public int getResId() {
        return resId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
