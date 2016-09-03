package com.insta.download.loadingproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.insta.download.loadingproject.view.LoadingView;

public class MainActivity extends AppCompatActivity {

    private LoadingView mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLoadingView = (LoadingView) findViewById(R.id.loadingView);
        mLoadingView.start();
        mLoadingView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingView.finish();
            }
        },2000);
        mLoadingView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingView.start();
            }
        },3000);
        mLoadingView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingView.finish();
            }
        },3500);
        mLoadingView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingView.finish();
            }
        },4000);

    }
}
