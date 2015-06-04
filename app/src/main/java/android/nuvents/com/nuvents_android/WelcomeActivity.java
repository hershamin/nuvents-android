package android.nuvents.com.nuvents_android;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.LatLng;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import io.fabric.sdk.android.Fabric;
import org.json.simple.JSONObject;

import java.net.URISyntaxException;

public class WelcomeActivity extends ActionBarActivity implements NuVentsBackendDelegate {

    public NuVentsBackend api;
    public boolean serverConn = false;
    public LocationManager locationManager;
    public ImageButton pickerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_welcome);

        // Init NuVents backend API
        try {
            String filesDir = getApplicationContext().getFilesDir().getPath();
            String deviceID = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            api = new NuVentsBackend(this, GlobalVariables.server, deviceID, filesDir);
            GlobalVariables.api= api;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Location manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        MyLocationChanged locationChangeListener = new MyLocationChanged();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationChangeListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationChangeListener);

        // Picker view button
        pickerButton = (ImageButton) findViewById(R.id.pickerButton);
        pickerButton.setOnClickListener(pickerButtonPressed);

    }

    // got Device location
    class MyLocationChanged implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (serverConn) { // Only use when connected to server
                LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                api.getNearbyEvents(loc, 5000); // Search within 5000 meters
                GlobalVariables.currentLoc = loc; // Set current location
                locationManager.removeUpdates(this);
            }
        }
        @Override
        public void onProviderDisabled(String provider) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    // My location button pressed
    ImageButton.OnClickListener pickerButtonPressed = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent pickerIntent = new Intent(getApplicationContext(), PickerActivity.class);
            startActivity(pickerIntent);
        }
    };

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
        //
    }

    // Get event detail
    public static void getEventDetail(final String eid, final JSONCallable callback) {
        GlobalVariables.api.getEventDetail(eid, new JSONCallable() {
            @Override
            public void json(JSONObject jsonData) {
                // Merge event summary & detail
                JSONObject summary = GlobalVariables.eventJson.get(eid);
                for (Object summ : summary.keySet()) {
                    jsonData.put(summ, summary.get(summ));
                }
                callback.json(jsonData);
            }
        });
    }

    // MARK: NuVents Backend Methods
    @Override
    public void nuventsServerDidReceiveNearbyEvent(JSONObject event) {
        // Add to global vars
        GlobalVariables.eventJson.put((String) event.get("eid"), event);
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
