package com.example.avd;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class MainApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = MainApplication.class.getSimpleName();
    private static MainApplication myApplication = null;

    public static MainApplication getContext() {
        return myApplication;
    }

    @Override
    public void onCreate() {
        // 程序创建的时候执行
        Log.d(TAG, "onCreate");
        super.onCreate();
        myApplication = this;
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        Log.d(TAG, "onTerminate");
        super.onTerminate();
        unregisterActivityLifecycleCallbacks(this);
    }

    @Override
    public void onLowMemory() {
        // 低内存的时候执行
        Log.d(TAG, "onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        // 程序在内存清理的时候执行
        Log.d(TAG, "onTrimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        ActivityLifeManager.getInstance().setCurrentActivity(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
