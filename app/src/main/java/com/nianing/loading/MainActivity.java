package com.nianing.loading;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


public class MainActivity extends AppCompatActivity {

    private LoadingView mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.nianing.loading.loadingproject.R.layout.activity_main);
        mLoadingView = (LoadingView) findViewById(com.nianing.loading.loadingproject.R.id.loadingView);
        mLoadingView.start();
        mLoadingView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoadingView.finish();
            }
        },2000);

    }
}
