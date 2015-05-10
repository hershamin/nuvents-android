package android.nuvents.com.nuvents_android;

import android.graphics.Point;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import android.util.Log;

import org.json.simple.JSONObject;

import java.net.URISyntaxException;


public class MainActivity extends ActionBarActivity implements OnMapReadyCallback, NuVentsBackendDelegate {

    public NuVentsBackend api;
    public boolean serverConn = false;
    public boolean initialLoc = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        try {
            String filesDir = getApplicationContext().getFilesDir().getPath();
            api = new NuVentsBackend(this, GlobalVariables.server, "test", filesDir);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng austin = new LatLng(30.2766, -97.734);

        map.setMyLocationEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(austin, 9));

        map.setOnMyLocationChangeListener(locationChangeListener);
        map.setOnMarkerClickListener(markerClickListener);
        map.setOnCameraChangeListener(cameraChangeListener);
        GlobalVariables.mapView = map;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Google Maps did get my location
    GoogleMap.OnMyLocationChangeListener locationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            if (serverConn && !initialLoc) {
                GoogleMap mapView = GlobalVariables.mapView;
                LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                mapView.moveCamera(CameraUpdateFactory.newLatLng(loc));
                Projection projection = mapView.getProjection();
                LatLng topLeftCorner = projection.fromScreenLocation(new Point(0, 0));
                float dist = (float)GMapCamera.distanceBetween(loc, topLeftCorner);
                api.getNearbyEvents(loc, dist);
                initialLoc = true;
            }
        }
    };

    // Google Maps Marker Click Listener
    GoogleMap.OnMarkerClickListener markerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            Log.i("M TITLE", marker.getTitle());
            return true;
        }
    };

    // Google Maps Camera Change Listener
    GoogleMap.OnCameraChangeListener cameraChangeListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            boolean cameraProcess = GlobalVariables.cameraProc;
            if (!cameraProcess) { // Camera process free
                cameraProcess = true;
                Point size = new Point();
                Display display = getWindowManager().getDefaultDisplay();
                display.getSize(size);
                GMapCamera.cameraChanged(cameraPosition, size); // Call clustering function
                GlobalVariables.prevCam = cameraPosition; // Make current position previous position
                cameraProcess = false;
            }
        }
    };

    // MARK: NuVents Backend Methods
    @Override
    public void nuventsServerDidReceiveNearbyEvent(JSONObject event) {
        for (Object key : event.keySet()) {
            Log.i("EVENT", (String)key + ": " + event.get(key).toString());
        }
    }

    @Override
    public void nuventsServerDidReceiveEventDetail(JSONObject event) {
        //
    }

    @Override
    public void nuventsServerDidConnect() {
        Log.i("Server", "NuVents Backend connected");
        api.pingServer();
        serverConn = true;
    }

    @Override
    public void nuventsServerDidDisconnect() {
        Log.i("Server", "NuVents Backend disconnected");
    }

    @Override
    public void nuventsServerDidRespondToPing(String response) {
        Log.i("PING", "RESPONSE: " + response);
    }

    @Override
    public void nuventsServerDidReceiveError(String type, String error) {
        //
    }

    @Override
    public void nuventsServerDidReceiveStatus(String type, String status) {
        //
    }
}
