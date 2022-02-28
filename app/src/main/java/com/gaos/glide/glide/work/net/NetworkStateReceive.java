package com.gaos.glide.glide.work.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.gaos.glide.glide.util.Util;

import java.util.Objects;

/**
 * 监听网络状态的广播接收者
 * 例如：可以收到 “网络不给力”
 *
 */
public class NetworkStateReceive extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (!Util.isWifiConnected()) {
                Toast.makeText(context, "网络不给力...", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
