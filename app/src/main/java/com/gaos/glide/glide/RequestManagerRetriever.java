package com.gaos.glide.glide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.gaos.glide.glide.binding.ApplicationLifecycle;
import com.gaos.glide.glide.binding.RequestManagerFragment;
import com.gaos.glide.glide.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * 专门管理RequestManager
 * @author gaos
 */
public class RequestManagerRetriever implements Handler.Callback {
    static final String FRAGMENT_TAG = "com.bumptech.glide.manager";

    @VisibleForTesting
    final Map<FragmentManager, RequestManagerFragment> pendingSupportRequestManagerFragments =
            new HashMap<>();

    private final Handler handler;

    public RequestManagerRetriever() {
        // 主线程
        handler = new Handler(Looper.getMainLooper(), this);
    }

    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 1; // androidx Fragmetn 空白

    private volatile RequestManager applicationManager;
    @NonNull
    private RequestManager getApplicationManager(@NonNull Context context) {
        if (applicationManager == null) {
            synchronized (this) {
                if (applicationManager == null) {

                    Glide glide = Glide.get(context.getApplicationContext());
                    applicationManager =  RequestManager.getInstance(new ApplicationLifecycle(), context.getApplicationContext());
                }
            }
        }
        return applicationManager;
    }

    // 参数 Context Activity FragmentActivity ... 调用了 此get函数【共用的】
    @NonNull
    public RequestManager get(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("你个货，传入的是 空的 context，要把你吊起来打...");
        } else if (Util.isOnMainThread() && !(context instanceof Application)) {
            if (context instanceof FragmentActivity) {
                return get((FragmentActivity) context); // 进入FragmentActivity的get函数
            } else if (context instanceof Activity) {
                return get((Activity) context); // 进入Activity的get函数
            } else if (context instanceof ContextWrapper && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
                return get(((ContextWrapper) context).getBaseContext()); // 继续递归寻找 匹配合适的
            }
        }

        // 若上面的判断都不满足，就会执行下面这句代码，同学们想知道Application作用域 就需要关心这句代码（红色区域）
        return getApplicationManager(context);
    }

    @NonNull
    public RequestManager get(@NonNull FragmentActivity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        } else {
            Util.assertNotDestroyed(activity);
            FragmentManager fm = activity.getSupportFragmentManager();
            return supportFragmentGet(activity, fm);
        }
    }

    @NonNull
    public RequestManager get(@NonNull Fragment fragment) { // androidx
        if (Util.isOnBackgroundThread()) {
            return get(fragment.getContext().getApplicationContext());
        } else {
            FragmentManager fm = fragment.getChildFragmentManager();
            return supportFragmentGet(fragment.getContext(), fm);
        }
    }

    @NonNull
    private RequestManager supportFragmentGet(
            @NonNull Context context,
            @NonNull FragmentManager fm) {

        // 1、从 FragmentManager 中获取 SupportRequestManagerFragment(空白)
        RequestManagerFragment current = getSupportRequestManagerFragment(fm);

        // 2、从该 空白Fragment 中获取 RequestManager
        RequestManager requestManager = current.getRequestManager();

        // 3、首次获取，则实例化 RequestManager
        if (requestManager == null) { // 【同学们注意：这样做的目的是为了  一个Activity或Fragment 只能有一个 RequestManager】

            // 3.1 实例化
            requestManager =  RequestManager.getInstance( current.getGlideLifecycle(), context);

            // 3.2 设置 Fragment 对应的 RequestManager    空白的Fragment<--->requestManager
            current.setRequestManager(requestManager);
        }
        return requestManager;
    }

    // 1、从 FragmentManager 中获取 SupportRequestManagerFragment
    @NonNull
    private RequestManagerFragment getSupportRequestManagerFragment(
            @NonNull final FragmentManager fm) {
        RequestManagerFragment current =
                (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);

        if (current == null) {

            //  1.2 尝试从【记录保存】中获取 Fragment
            current = pendingSupportRequestManagerFragments.get(fm);

            // 1.3 实例化 Fragment
            if (current == null) {

                // 1.3.1 创建对象 空白的Fragment
                current = new RequestManagerFragment();  // 重复创建

                // 1.3.2 【记录保存】映射关系 进行保存   第一个保障
                // 一个MainActivity == 一个空白的SupportRequestManagerFragment == 一个RequestManager
                pendingSupportRequestManagerFragments.put(fm, current);

                // 1.3.3 提交 Fragment 事务
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();

                // 1.3.4 post 一个消息
                handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        return current;
    }

    @Override
    public boolean handleMessage(Message message) {

        switch (message.what) {
            case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER: // 移除 【记录保存】  1.3.5 post 一个消息
                FragmentManager supportFm = (FragmentManager) message.obj;
                pendingSupportRequestManagerFragments.remove(supportFm); // 1.3.6 移除临时记录中的映射关系
                break;
            default:
                break;
        }

        return false;
    }
}