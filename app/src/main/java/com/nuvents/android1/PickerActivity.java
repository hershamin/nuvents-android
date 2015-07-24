package com.nuvents.android1;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.gms.maps.model.LatLng;

import org.json.simple.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Random;


public class PickerActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.nuvents.android.SplashScreenOverride";

    WebView webView;
    public ProgressBar activityIndicator;
    ImageButton refreshBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        // Init vars
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new UIWebView());
        webView.loadUrl("file:///android_asset/pickerView.html");
        GlobalVariables.pickerWebView = webView;
        refreshBtn = (ImageButton) findViewById(R.id.refreshBtn);
        refreshBtn.setOnClickListener(refreshBtnPressed);

        // Init activity indicator
        activityIndicator = (ProgressBar) findViewById(R.id.activityIndicator);
        activityIndicator.setVisibility(View.VISIBLE);
        activityIndicator.setIndeterminate(true);

    }

    // Refresh btn pressed
    ImageButton.OnClickListener refreshBtnPressed = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Go to welcome activity to refresh nearby events
            goToWelcomeActivity();
        }
    };

    // Go to welcome activity without invoking splash screen
    private void goToWelcomeActivity() {
        Intent welcomeView = new Intent(getApplicationContext(), WelcomeActivity.class);
        welcomeView.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        welcomeView.putExtra(EXTRA_MESSAGE, true);
        startActivity(welcomeView);
    }

    // Webview delegate methods
    private class UIWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.contains("openmapview://")) {
                Intent mapView = new Intent(getApplicationContext(), MapActivity.class);
                GlobalVariables.category = ""; // Set category in global
                startActivity(mapView);
                return true;
            } else if (url.contains("openlistview://")) {
                Intent listView = new Intent(getApplicationContext(), ListActivity.class);
                GlobalVariables.category = ""; // Set category in global
                startActivity(listView);
                return true;
            } else if (url.contains("opencategoryview://")) {
                Intent categoryView = new Intent(getApplicationContext(), CategoryActivity.class);
                startActivity(categoryView);
                return true;
            } else if (url.contains("sendeventrequest://")) { // Send request to add city to backend
                String request = url.split("//")[1];
                WelcomeActivity.sendEventRequest(request);
                return true;
            } else if (url.contains("searchavailablecity://")) { // Search for events in available city
                String request = url.split("//")[1].replace("?","");
                GlobalVariables.eventReqLoc = request; // Set event request location
                goToWelcomeActivity();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Hide activity indicator
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activityIndicator.setVisibility(View.INVISIBLE);
                }
            });
            // Convert hashmap to json
            final Map<String, JSONObject> eventMap = GlobalVariables.eventJson;
            // Send to webview
            view.loadUrl("javascript:setEventCount(" + eventMap.keySet().size() + ")");
            // Hide refresh btn if event count is 0
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (eventMap.keySet().size() == 0) {
                        refreshBtn.setVisibility(View.INVISIBLE);
                    } else {
                        refreshBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
            // Get image url from welcome view backgrounds & send to webview
            String imgDir = NuVentsBackend.getResourcePath("tmp", "welcomeViewImgs", false);
            imgDir = imgDir.replace("tmp","");
            File[] files = new File(imgDir).listFiles();
            if (files.length > 0) { // proceed if images are available
                int randomInd = new Random().nextInt(files.length); // Pick random img to display
                // Send to webview
                view.loadUrl("javascript:setImgUrl(\"" + files[randomInd].getAbsolutePath() + "\")");
            }
            // Send current location coordinates to webview
            LatLng currentLoc = GlobalVariables.currentLoc;
            view.loadUrl("javascript:setLocation(\"" + currentLoc.latitude + "," +
                    currentLoc.longitude + "\")");
            // Send server url to webview
            view.loadUrl("javascript:setServer(\"" + GlobalVariables.server + "\")");
            super.onPageFinished(view, url);
        }
    }

    // Update event count
    public static void updateEventCount() {
        // Convert hashmap to json
        final Map<String, JSONObject> eventMap = GlobalVariables.eventJson;
        final WebView webView = GlobalVariables.pickerWebView;
        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:setEventCount(" + eventMap.keySet().size() + ")");
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_picker, menu);
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
