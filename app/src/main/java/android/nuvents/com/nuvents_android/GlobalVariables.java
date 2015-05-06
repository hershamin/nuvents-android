package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 5/5/15.
 */

import android.app.Application;

public class GlobalVariables extends Application {

    // Global variable declaration
    public static String server = "http://repo.nuvents.com:1026/";

    private static GlobalVariables singleton;

    public static GlobalVariables getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }

}
