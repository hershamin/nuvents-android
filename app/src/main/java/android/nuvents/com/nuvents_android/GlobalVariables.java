package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 5/5/15.
 */

import android.app.Application;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;

import org.json.simple.JSONObject;

import java.util.ArrayList;

public class GlobalVariables extends Application {

    // Global constants
    public final static String server = "http://repo.nuvents.com:1026/";
    public final static float zoomLevelMargin = (float)0.5; // User must change camera by indicated zoom level to trigger clustering
    public final static float zoomLevelClusteringLimit = (float)14.5; // Markers cannot resize if zoom level is above that
    public final static float nearbyEventsMargin= (float)5; // Events must be within specified meters to be combined
    public final static float clusteringMultiplier = (float)1; // Determins the clustering behaviour of markers

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
