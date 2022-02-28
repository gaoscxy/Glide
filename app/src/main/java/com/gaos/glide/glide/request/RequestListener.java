package com.gaos.glide.glide.request;

import android.graphics.Bitmap;

public interface RequestListener {

    boolean onSuccess(Bitmap bitmap);

    boolean onFaile();

}
