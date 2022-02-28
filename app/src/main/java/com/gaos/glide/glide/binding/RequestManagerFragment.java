package com.gaos.glide.glide.binding;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.gaos.glide.glide.RequestManager;

/**
 * 建立空的Fragment，实现生命周期
 */
public class RequestManagerFragment extends Fragment {

    private static final String TAG = "SupportRMFragment";
    private  ActivityFragmentLifeCycle lifecycle = null;
    @Nullable
    private RequestManager requestManager;

    public RequestManagerFragment() {
        this(new ActivityFragmentLifeCycle());
    }

    public RequestManagerFragment(@NonNull ActivityFragmentLifeCycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void setRequestManager(@Nullable RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    @NonNull
    public ActivityFragmentLifeCycle getGlideLifecycle() {
        return lifecycle;
    }

    @Nullable
    public RequestManager getRequestManager() {
        return requestManager;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // this.lifecycle.addListener(requestManager);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycle.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        lifecycle.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycle.onDestroy();
    }
}
