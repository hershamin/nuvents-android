package android.nuvents.com.nuvents_android;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;

/**
 * Created by hersh on 5/6/15.
 */

// Called for Google Maps Camera Change
public class GMapCamera {

    // Camera changed
    public static void cameraChanged(CameraPosition position) {
        GoogleMap mapView = GlobalVariables.mapView; // MapView
        Log.i("CAMERA", "CHANNNNGED");
    }

}
