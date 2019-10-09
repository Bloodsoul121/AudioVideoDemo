package com.example.avd;

import android.app.Application;

public class MainApplication extends Application {

    private static MainApplication myApplication = null;

    public static MainApplication getApplication() {
        return myApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
    }

}
