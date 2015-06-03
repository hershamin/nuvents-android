package android.nuvents.com.nuvents_android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.fabric.sdk.android.Fabric;
import org.json.simple.JSONObject;

import java.net.URISyntaxException;
import java.util.Map;


public class WelcomeActivity extends ActionBarActivity implements NuVentsBackendDelegate {

    public NuVentsBackend api;
    public boolean serverConn = false;
    public boolean initialLoc = false;
    Point size = new Point();
    ImageButton mapListViewBtn;
    ImageView statusBarImg;
    ImageView navBarImg;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_welcome);

        // Init vars
        getWindowManager().getDefaultDisplay().getSize(size); // Get window size
        mapListViewBtn = (ImageButton) findViewById(R.id.mapListViewBtn);
        statusBarImg = (ImageView) findViewById(R.id.statusBarImg);
        navBarImg = (ImageView) findViewById(R.id.navBarImg);
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new UIWebView());

        // MapView
        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Init mapview/listview btn
        mapListViewBtn.setOnClickListener(listViewBtnPressed);
        webView.setVisibility(View.INVISIBLE);

        // Init NuVents backend API
        try {
            String filesDir = getApplicationContext().getFilesDir().getPath();
            api = new NuVentsBackend(this, GlobalVariables.server, "test", filesDir);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    // List view button pressed
    ImageButton.OnClickListener listViewBtnPressed = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            // UI Setup
            webView.setVisibility(View.VISIBLE);
            myLocBtn.setVisibility(View.INVISIBLE);
            Bitmap mapListImg = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("mapView", "icon", false));
            mapListViewBtn.setImageBitmap(mapListImg);
            mapListViewBtn.setOnClickListener(null);
            mapListViewBtn.setOnClickListener(mapViewBtnPressed);

            // Load webview
            /*String baseURL = NuVentsBackend.getResourcePath("tmp", "tmp", false);
            baseURL = baseURL.replace("tmp/tmp", "");
            String fileURL = NuVentsBackend.getResourcePath("listView", "html", false);
            StringBuilder htmlStr = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(fileURL)));
                String line;
                while ((line = br.readLine()) != null) {
                    htmlStr.append(line);
                }
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            webView.loadDataWithBaseURL("file://" + baseURL, htmlStr.toString(), "text/html", null, null);*/
            if (webView.getTitle().isEmpty()) {
                webView.loadUrl("http://storage.googleapis.com/nuvents-resources/listViewTest.html");
            } else {
                // Convert hashmap to json
                Map<String, JSONObject> eventMap = GlobalVariables.eventJson;
                JSONObject eventsJson = new JSONObject();
                for (String key : eventMap.keySet()) {
                    eventsJson.put(key, eventMap.get(key));
                }
                // Send events to listview
                webView.loadUrl("javascript:setEvents(" + eventsJson + ")");
                String searchText = searchField.getText().toString();
                webView.loadUrl("javascript:searchByTitle('" + searchText + "')");
            }
        }
    };

    // Map view button pressed
    ImageButton.OnClickListener mapViewBtnPressed = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            // UI Setup
            webView.setVisibility(View.INVISIBLE);
            myLocBtn.setVisibility(View.VISIBLE);
            Bitmap mapListImg = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("listView", "icon", false));
            mapListViewBtn.setImageBitmap(mapListImg);
            mapListViewBtn.setOnClickListener(null);
            mapListViewBtn.setOnClickListener(listViewBtnPressed);
        }
    };

    // Webview delegate methods
    private class UIWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.contains("opendetailview://")) {
                String eid = url.split("//")[1];
                openDetailView(eid);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Convert hashmap to json
            Map<String, JSONObject> eventMap = GlobalVariables.eventJson;
            JSONObject eventsJson = new JSONObject();
            for (String key : eventMap.keySet()) {
                eventsJson.put(key, eventMap.get(key));
            }
            // Send events to listview
            view.loadUrl("javascript:setEvents(" + eventsJson + ")");
            String searchText = searchField.getText().toString();
            webView.loadUrl("javascript:searchByTitle('" + searchText + "')");
            super.onPageFinished(view, url);
        }
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

        // Icons
        final Bitmap myLoc = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("myLocation", "icon", false));
        final Bitmap mapList = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("listView", "icon", false));
        final Bitmap statusBar = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("statusBar", "icon", false));
        final Bitmap navBar = BitmapFactory.decodeFile(NuVentsBackend.getResourcePath("navBar", "icon", false));

        // Add views to hierarchy
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusBarImg.setImageBitmap(statusBar); // Status Bar img
                navBarImg.setImageBitmap(navBar); // Nav Bar img
                myLocBtn.setImageBitmap(myLoc); // My Location btn
                mapListViewBtn.setImageBitmap(mapList); // Map/List View btn
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

    // Open Detail View
    public void openDetailView(final String eid) {
        api.getEventDetail(eid, new JSONCallable() {
            @Override
            public void json(JSONObject jsonData) {
                // Merge event summary & detail
                JSONObject summary = GlobalVariables.eventJson.get(eid);
                for (Object summ : summary.keySet()) {
                    jsonData.put(summ, summary.get(summ));
                }
                // Present detail view
                Intent detailView = new Intent(getApplicationContext(), DetailView.class);
                GlobalVariables.tempJson = jsonData;
                startActivity(detailView);
            }
        });
    }

    // MARK: NuVents Backend Methods
    @Override
    public void nuventsServerDidReceiveNearbyEvent(JSONObject event) {
        // Add to global vars
        GlobalVariables.eventJson.put((String)event.get("eid"), event);
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
