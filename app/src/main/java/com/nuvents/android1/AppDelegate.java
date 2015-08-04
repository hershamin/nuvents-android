package com.nuvents.android1;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by hersh on 7/29/15.
 */
public class AppDelegate extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Called on app start
        // Begin NuVents backend connection
        NuVentsEndpoint.sharedEndpoint(getApplicationContext()).connect();
        // Initiate fabricIO if in release build
        if (!BuildConfig.DEBUG) { Fabric.with(this, new Crashlytics()); }
    }

}
