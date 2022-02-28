package com.gaos.glide.glide.util;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {
    /**
     * 单例设计模式（饿汉式）
     * 单例首先私有化构造方法，然后饿汉式一开始就开始创建，并提供get方法
     */
    private static ThreadPoolManager mInstance = new ThreadPoolManager();

    public static ThreadPoolManager getInstance() {
        return mInstance;
    }

    private int corePoolSize;//核心线程池的数量，同时能够执行的线程数量
    private int maximumPoolSize;//最大线程池数量，表示当缓冲队列满的时候能继续容纳的等待任务的数量
    private long keepAliveTime = 1;//存活时间
    private TimeUnit unit = TimeUnit.HOURS;
    private ThreadPoolExecutor executor;

    private ThreadPoolManager() {
        /**
         * 给corePoolSize赋值：当前设备可用处理器核心数*2 + 1,能够让cpu的效率得到最大程度执行
         */
        corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 2;
        maximumPoolSize = corePoolSize; //虽然maximumPoolSize用不到，但是需要赋值，否则报错
        executor = new ThreadPoolExecutor(
                0, //当某个核心线程数
                Integer.MAX_VALUE, //先corePoolSize,然后new LinkedBlockingQueue<Runnable>(),然后maximumPoolSize,但是它的数量是包含了corePoolSize的
                60L, //表示的是maximumPoolSize当中等待任务的存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), //缓冲队列，用于存放等待任务，Linked的先进先出
                Executors.defaultThreadFactory(), //创建线程的工厂
                new ThreadPoolExecutor.AbortPolicy() //用来对超出maximumPoolSize的任务的处理策略
        );
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    /**
     * 执行任务
     */
    public void execute(Runnable runnable) {
        if (runnable == null) return;
        executor.execute(runnable);
    }

    /**
     * 从线程池中移除任务
     */
    public void remove(Runnable runnable) {
        if (runnable == null) return;
        executor.remove(runnable);
    }
}