package com.gaos.glide;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.gaos.glide.glide.Glide;
import com.gaos.glide.glide.request.RequestOptions;



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String url = "https://pics2.baidu.com/feed/0e2442a7d933c89521daeaac15879cf9800200db.png?token=4313fc6d57e00a960846e4cd5c10cdda";
        RequestOptions requestOptions = new RequestOptions().override(300,300);
//        RequestOptions requestOptions = new RequestOptions();
        Glide.with(this).load(url).apply(requestOptions).into((ImageView) findViewById(R.id.iv_image));
    }
}