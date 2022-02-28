package com.gaos.glide.glide.binding.inter;

import androidx.annotation.NonNull;

public interface LifeCycle {
    void addListener(@NonNull LifecycleListener listener);

    void removeListener(@NonNull LifecycleListener listener);
}
