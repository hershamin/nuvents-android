package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 5/5/15.
 */

import android.app.Application;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

public class GlobalVariables extends Application {

    // Global constants
    public final static String server = "http://repo.nuvents.com:1026/";

    // Global variables
    public static ArrayList<Marker> eventMarkers = new ArrayList<Marker>();
    public static GoogleMap mapView;
    public static CameraPosition prevCam;
    public static boolean cameraProc = false; // true if camera process is busy

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
