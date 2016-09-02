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
        },3000);
/*        RotateAnimation rotateAnimation = new RotateAnimation(0,1080,RotateAnimation.RELATIVE_TO_SELF,0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        rotateAnimation.setDuration(1000);
        rotateAnimation.setRepeatMode(Animation.RESTART);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        mLoadingView.startAnimation(rotateAnimation);*/
    }
}
