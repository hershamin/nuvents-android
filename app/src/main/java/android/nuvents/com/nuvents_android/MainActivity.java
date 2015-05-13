package android.nuvents.com.nuvents_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import org.json.simple.JSONObject;

import java.net.URISyntaxException;


public class MainActivity extends ActionBarActivity implements OnMapReadyCallback, NuVentsBackendDelegate {

    public NuVentsBackend api;
    public boolean serverConn = false;
    public boolean initialLoc = false;
    Point size = new Point();
    ImageButton myLocBtn;
    ImageButton listViewBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init vars
        getWindowManager().getDefaultDisplay().getSize(size); // Get window size

        // MapView
        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        try {
            String filesDir = getApplicationContext().getFilesDir().getPath();
            api = new NuVentsBackend(this, GlobalVariables.server, "test", filesDir);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    // My location button pressed
    ImageButton.OnClickListener myLocBtnPressed = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            GoogleMap mapView = GlobalVariables.mapView;
            Location loc = mapView.getMyLocation();
            mapView.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(),
                    loc.getLongitude()), 15));
        }
    };

    // List view button pressed
    ImageButton.OnClickListener listViewBtnPressed = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent detailView = new Intent(getApplicationContext(), ListView.class);
            startActivity(detailView);
        }
    };

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng austin = new LatLng(30.2766, -97.734);

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
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

    // NuVents server resources sync complete
    @Override
    public void nuventsServerDidSyncResources() {
        final JSONObject config= GlobalVariables.config; // get config

        // Icons
        final Bitmap myLocImg = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("myLocation", "icon"));
        final Bitmap listViewImg = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("listView", "icon"));

        // Add views to hierarchy
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RelativeLayout relL = (RelativeLayout)findViewById(R.id.mapViewLayout); // Get layout

                // My Location Button
                if (myLocBtn == null) {
                    myLocBtn = new ImageButton(getApplicationContext());
                    myLocBtn.setOnClickListener(myLocBtnPressed);
                    relL.addView(myLocBtn);
                }
                myLocBtn.setImageBitmap(myLocImg);
                myLocBtn.setBackgroundDrawable(null);
                myLocBtn.setX(Float.parseFloat((String) config.get("myLocBtnX")) * size.x);
                myLocBtn.setY(Float.parseFloat((String) config.get("myLocBtnY")) * size.y);

                // List View Button
                if (listViewBtn == null) {
                    listViewBtn = new ImageButton(getApplicationContext());
                    listViewBtn.setOnClickListener(listViewBtnPressed);
                    relL.addView(listViewBtn);
                }
                listViewBtn.setImageBitmap(listViewImg);
                listViewBtn.setBackgroundDrawable(null);
                listViewBtn.setX(Float.parseFloat((String) config.get("listViewBtnX")) * size.x);
                listViewBtn.setY(Float.parseFloat((String) config.get("listViewBtnY")) * size.y);

            }
        });
    }

    // Google Maps did get my location
    GoogleMap.OnMyLocationChangeListener locationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            if (serverConn && !initialLoc) {
                GoogleMap mapView = GlobalVariables.mapView;
                LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 13));
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
            final String markerTitle = marker.getTitle();
            api.getEventDetail(marker.getTitle(), new JSONCallable() {
                @Override
                public void json(JSONObject jsonData) {
                    // Merge event summary & detail
                    JSONObject summary = GlobalVariables.eventJson.get(markerTitle);
                    for (Object summ : summary.keySet()) {
                        jsonData.put(summ, summary.get(summ));
                    }
                    // Present detail view
                    Intent detailView = new Intent(getApplicationContext(), DetailView.class);
                    GlobalVariables.tempJson = jsonData;
                    startActivity(detailView);
                }
            });
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
                GMapCamera.cameraChanged(cameraPosition, size); // Call clustering function
                GlobalVariables.prevCam = cameraPosition; // Make current position previous position
                cameraProcess = false;
            }
        }
    };

    // MARK: NuVents Backend Methods
    @Override
    public void nuventsServerDidReceiveNearbyEvent(JSONObject event) {
        // Add to global vars
        GlobalVariables.eventJson.put((String)event.get("eid"), event);
        // Build marker
        Double latitude = Double.parseDouble(event.get("latitude").toString());
        Double longitude = Double.parseDouble(event.get("longitude").toString());
        String mapSnippet = (String)event.get("title");
        Bitmap markerIcon = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath(mapSnippet, "marker"));
        final MarkerOptions markerOptions = new MarkerOptions().title((String)event.get("eid"))
                .snippet((String)event.get("title"))
                .position(new LatLng(latitude, longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(markerIcon));
        // Add to map & global variable
        final GoogleMap mapView = GlobalVariables.mapView;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Marker marker = mapView.addMarker(markerOptions);
                GlobalVariables.eventMarkers.add(marker);
                GMapCamera.clusterMarkers(mapView, mapView.getCameraPosition(), markerOptions.getTitle(), size);
            }
        });
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
