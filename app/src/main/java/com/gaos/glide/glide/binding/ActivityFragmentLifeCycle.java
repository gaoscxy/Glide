package com.gaos.glide.glide.binding;

import androidx.annotation.NonNull;

import com.gaos.glide.glide.binding.inter.LifeCycle;
import com.gaos.glide.glide.binding.inter.LifecycleListener;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * @author gaos
 */
public class ActivityFragmentLifeCycle implements LifeCycle {

    // 容器
    private final Set<LifecycleListener> lifecycleListeners =
            Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
    private boolean isStarted;  // 启动的标记
    private boolean isDestroyed; // 销毁的标记

    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.add(listener);

        if (isDestroyed) {
            listener.onDestroy();
        } else if (isStarted) {
            listener.onStart();
        } else {
            listener.onStop();  // 首次启动：会默认 onStop 先停止   然后再onStart
        }
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    void onStart() {
        isStarted = true;
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStart();
        }
    }

    void onStop() {
        isStarted = false;
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStop();
        }
    }

    void onDestroy() {
        isDestroyed = true;
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onDestroy();
        }
    }
}
