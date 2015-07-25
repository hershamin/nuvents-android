package com.nuvents.android1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.simple.JSONObject;

import java.util.Map;

import info.hoang8f.android.segmented.SegmentedGroup;


public class MapActivity extends ActionBarActivity implements OnMapReadyCallback {

    ImageButton myLocBtn;
    ImageButton homeBtn;
    LinearLayout mainLinLay;
    Point size = new Point();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Init vars
        getWindowManager().getDefaultDisplay().getSize(size); // Get window size
        myLocBtn = (ImageButton) findViewById(R.id.myLocBtn);
        myLocBtn.setOnClickListener(myLocBtnPressed);
        homeBtn = (ImageButton) findViewById(R.id.homeBtn);
        homeBtn.setOnClickListener(homeBtnClicked);
        mainLinLay = (LinearLayout) findViewById(R.id.mainLinLay);

        // MapView
        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Check all button in segmented control
        SegmentedGroup sGroup = (SegmentedGroup) findViewById(R.id.segmentedCtrl);
        sGroup.setOnCheckedChangeListener(filterChanged);
        sGroup.check(R.id.allBtn);

    }

    // Called when filter type changed
    SegmentedGroup.OnCheckedChangeListener filterChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == R.id.allBtn) {
                GMapCamera.filterEventsByDate("all", size);
            } else if (checkedId == R.id.todayBtn) {
                GMapCamera.filterEventsByDate("today", size);
            } else if (checkedId == R.id.tomorrowBtn) {
                GMapCamera.filterEventsByDate("tomorrow", size);
            }
        }
    };

    // NuVents home button clicked, go to picker view
    ImageButton.OnClickListener homeBtnClicked = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Go to Picker Activity and dismiss anything above it
            Intent pickerView = new Intent(getApplicationContext(), PickerActivity.class);
            pickerView.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(pickerView);
        }
    };

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng currentLoc = GlobalVariables.currentLoc;

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 13));

        map.setOnMarkerClickListener(markerClickListener);
        map.setOnCameraChangeListener(cameraChangeListener);
        GlobalVariables.mapView = map;

        // Add markers to mapview
        Map<String, JSONObject> eventMap = GlobalVariables.eventJson;
        GlobalVariables.eventMarkers.clear(); // Clear markers global var
        for (String key : eventMap.keySet()) {
            JSONObject event = eventMap.get(key);
            // check if in requested category
            String category = event.get("marker").toString().toLowerCase();
            String reqCat = GlobalVariables.category;
            if (reqCat != "") {
                if (!category.contains(reqCat)) {
                    continue; // Not in requested category, continue
                }
            }
            // Build marker
            Double latitude = Double.parseDouble(event.get("latitude").toString());
            Double longitude = Double.parseDouble(event.get("longitude").toString());
            String mapSnippet = (String)event.get("marker");
            Bitmap markerIcon = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath(mapSnippet, "marker", false));
            markerIcon = NuVentsBackend.resizeImage(markerIcon, 85);
            final MarkerOptions markerOptions = new MarkerOptions().title((String)event.get("eid"))
                    .snippet(mapSnippet)
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

    // Google Maps Marker Click Listener
    GoogleMap.OnMarkerClickListener markerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            String markerTitle = marker.getTitle();
            WelcomeActivity.getEventDetail(markerTitle, new JSONCallable() {
                @Override
                public void json(JSONObject jsonData) {
                    GlobalVariables.tempJson = jsonData;
                    Intent detailView = new Intent(getApplicationContext(), DetailActivity.class);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
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
}