package com.nuvents.android1;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
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
import android.widget.ProgressBar;

import io.fabric.sdk.android.Fabric;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Random;

public class WelcomeActivity extends ActionBarActivity implements NuVentsBackendDelegate {

    private static int SPLASH_TIME_OUT = 2500; // Splash screen timeout

    public NuVentsBackend api;
    public boolean serverConn = false;
    public boolean haveLoc = false;
    public ImageButton pickerButton;
    public ImageView backgroundImg;
    public ProgressBar activityIndicator;

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

        // Hide splash screen if override is set from picker activity
        Boolean splashOverride = getIntent().getBooleanExtra(PickerActivity.EXTRA_MESSAGE, false);
        if (splashOverride) {
            // This signifies refresh button is pressed from picker activity
            ImageView splashScreen = (ImageView) findViewById(R.id.splashScreen);
            splashScreen.setVisibility(View.INVISIBLE);
            GlobalVariables.eventJson.clear();
            MyLocation.LocationResult newLocRes = new MyLocation.LocationResult() {
                @Override
                public void gotLocation(Location location) {
                    GlobalVariables.currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    requestNearbyEvents();
                    nuventsServerDidSyncResources();
                }
            };
            MyLocation newLoc = new MyLocation();
            newLoc.getLocation(this, newLocRes);
        }

        // Init activity indicator
        activityIndicator = (ProgressBar) findViewById(R.id.activityIndicator);
        activityIndicator.setVisibility(View.VISIBLE);
        activityIndicator.setIndeterminate(true);

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

        // Background Images
        backgroundImg = (ImageView) findViewById(R.id.backgroundImg);
        setBackgroundImage(); // Call to set background image if exists

        new checkServerConn().execute(GlobalVariables.server); // Alert user if server is not reachable

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

    // Check for server connection & alert user if unreachable
    // Class to record hit using HTTP get request
    private class checkServerConn extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            Boolean serverConn;
            try {
                String urlString = params[0];
                HttpGet httpReq = new HttpGet(urlString);
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse resp = httpClient.execute(httpReq);
                serverConn = true; // Some kind of response is received, server is reachable
            } catch (Exception e) {
                serverConn = false; // Exception is thrown, server unreachable
                e.printStackTrace();
            }
            return serverConn;
        }
        @Override
        protected void onPostExecute(Boolean status) {
            if (!status) {
                // Server is unreachable alert user
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(WelcomeActivity.this);
                alertDialog.setTitle("Server connection error");
                alertDialog.setMessage("Either Airplane mode is on or Internet is not reachable");
                alertDialog.setNegativeButton("OK", null);
                alertDialog.show();
            }
            super.onPostExecute(status);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    // Request nearby events
    void requestNearbyEvents() {
        LatLng loc = GlobalVariables.currentLoc; // Get current location
        api.getNearbyEvents(loc, 10000, (float) System.currentTimeMillis() / (float) 1000.0); // Search within 10000 meters
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
        runOnUiThread(new Runnable() { // Set picker button visible & activity indicator invisible
            @Override
            public void run() {
                pickerButton.setVisibility(View.VISIBLE);
                activityIndicator.setVisibility(View.INVISIBLE);
            }
        });
        // Set Background Image
        setBackgroundImage();
    }

    // Set background image if it exists
    public void setBackgroundImage() {
        String imgDir = NuVentsBackend.getResourcePath("tmp", "welcomeViewImgs", false);
        imgDir = imgDir.replace("tmp","");
        File[] files = new File(imgDir).listFiles();
        if (files.length == 0) { return; } // Skip if directory is empty
        int randomInd = new Random().nextInt(files.length); // Pick random img to display
        final Bitmap bgImg = BitmapFactory.decodeFile(files[randomInd].getAbsolutePath());
        // Set background image
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                backgroundImg.setImageBitmap(bgImg);
            }
        });
    }

    // Send event website response code
    public static void sendWebRespCode(String website, String statusCode){
        GlobalVariables.api.sendWebsiteCode(website, statusCode);
    }

    // Send event request to add city
    public static void sendEventRequest(String request) {
        GlobalVariables.api.sendEventReq(request);
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
        // Update count in picker activity
        PickerActivity.updateEventCount();
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
        //
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
