package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 5/5/15.
 */

import android.app.Application;
import android.webkit.WebView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalVariables extends Application {

    // Global constants
    public final static String server       = "http://backend.nuvents.com/";
    public final static String pickerView   = "http://storage.googleapis.com/nuvents-resources/pickerView.html";
    public final static String categoryView = "http://storage.googleapis.com/nuvents-resources/categoryView.html";
    public final static String listView     = "http://storage.googleapis.com/nuvents-resources/listView.html";
    public final static String detailView   = "http://storage.googleapis.com/nuvents-resources/detailView.html";
    public final static float zoomLevelMargin = (float)0.5; // User must change camera by indicated zoom level to trigger clustering
    public final static float zoomLevelClusteringLimit = (float)14.5; // Markers cannot resize if zoom level is above that
    public final static float nearbyEventsMargin= (float)5; // Events must be within specified meters to be combined
    public final static float clusteringMultiplier = (float)1; // Determins the clustering behaviour of markers

    // Global variables
    public static ArrayList<Marker> eventMarkers = new ArrayList<Marker>();
    public static Map<String, JSONObject> eventJson = new HashMap<String, JSONObject>();
    public static GoogleMap mapView;
    public static CameraPosition prevCam;
    public static boolean cameraProc = false; // true if camera process is busy
    public static boolean searchProc = false; // true if search process is busy
    public static JSONObject tempJson; // Temp event json to pass to detail view
    public static LatLng currentLoc; // Current location
    public static String category; // To set event category
    public static NuVentsBackend api; // NuVents backend API
    public static WebView pickerWebView; // Picker View Web View

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
