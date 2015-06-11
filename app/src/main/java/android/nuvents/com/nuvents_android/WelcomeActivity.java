package android.nuvents.com.nuvents_android;

import android.content.Intent;
import android.location.Location;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.os.Handler;
import android.view.MenuItem;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.model.LatLng;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import io.fabric.sdk.android.Fabric;
import org.json.simple.JSONObject;

import java.net.URISyntaxException;

public class WelcomeActivity extends ActionBarActivity implements NuVentsBackendDelegate {

    private static int SPLASH_TIME_OUT = 2500; // Splash screen timeout

    public NuVentsBackend api;
    public boolean serverConn = false;
    public boolean haveLoc = false;
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
        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                // Look for nearby events if server connection is established
                GlobalVariables.currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                if (serverConn) {
                    requestNearbyEvents();
                }
                haveLoc = true;
            }
        };
        MyLocation myLocation = new MyLocation();
        myLocation.getLocation(this, locationResult);

        // Picker view button
        pickerButton = (ImageButton) findViewById(R.id.pickerButton);
        pickerButton.setOnClickListener(pickerButtonPressed);
        pickerButton.setVisibility(View.INVISIBLE); // hide picker button

        // Hide splash screen after delay in specified milli seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /*
                *   Showing splash scren (ImageView) with a timer. This is useful
                *       when showing app logo/company
                */
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView splashScreen = (ImageView) findViewById(R.id.splashScreen);
                        splashScreen.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }, SPLASH_TIME_OUT);

    }

    // Request nearby events
    void requestNearbyEvents() {
        LatLng loc = GlobalVariables.currentLoc; // Get current location
        api.getNearbyEvents(loc, 5000); // Search within 5000 meters
        runOnUiThread(new Runnable() { // Set picker button visible
            @Override
            public void run() {
                pickerButton.setVisibility(View.VISIBLE);
            }
        });
    }

    // Picker Activity button pressed
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

    // Send event website response code
    public static void sendWebRespCode(String website, String statusCode){
        GlobalVariables.api.sendWebsiteCode(website, statusCode);
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
        // Look for nearby events if location fix is established
        if (haveLoc) {
            requestNearbyEvents();
        }
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
