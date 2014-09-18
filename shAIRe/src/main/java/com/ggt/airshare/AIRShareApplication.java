package com.ggt.airshare;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

/**
 * AIRShare application. Starts singleton.
 */
public class AIRShareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.start(this);
    }

}
