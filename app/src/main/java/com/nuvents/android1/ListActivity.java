package com.nuvents.android1;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.google.android.gms.maps.model.LatLng;

import org.json.simple.JSONObject;

import java.util.Map;

import info.hoang8f.android.segmented.SegmentedGroup;


public class ListActivity extends ActionBarActivity {

    WebView webView;
    LinearLayout mainLinLay;
    ImageButton homeBtn;
    boolean webViewReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        // Init vars
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new UIWebView());
        webView.loadUrl("file:///android_asset/listView.html");
        mainLinLay = (LinearLayout) findViewById(R.id.mainLinLay);
        homeBtn = (ImageButton) findViewById(R.id.homeBtn);
        homeBtn.setOnClickListener(homeBtnClicked);

        // Check distance button in segmented control
        SegmentedGroup sGroup = (SegmentedGroup) findViewById(R.id.segmentedCtrl);
        sGroup.setOnCheckedChangeListener(filterChanged);
        sGroup.check(R.id.distanceBtn);

    }

    // Called when filter type changed
    SegmentedGroup.OnCheckedChangeListener filterChanged = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == R.id.distanceBtn) {
                if (webViewReady) {
                    webView.loadUrl("javascript:sortBy('distance')");
                }
            } else if (checkedId == R.id.timeBtn) {
                if (webViewReady) {
                    webView.loadUrl("javascript:sortBy('time.start')");
                }
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

    // Webview delegate methods
    private class UIWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.contains("opendetailview://")) {
                String eid = url.split("//")[1];
                WelcomeActivity.getEventDetail(eid, new JSONCallable() {
                    @Override
                    public void json(JSONObject jsonData) {
                        GlobalVariables.tempJson = jsonData;
                        Intent detailView = new Intent(getApplicationContext(), DetailActivity.class);
                        startActivity(detailView);
                    }
                });
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
                JSONObject event = eventMap.get(key);
                String category = event.get("marker").toString().toLowerCase();
                String reqCat = GlobalVariables.category;
                if (reqCat != "") {
                    if (!category.contains(reqCat)) {
                        continue; // Not in requested category, continue
                    }
                }
                // Calculate distance between current location and event location
                LatLng eventLoc = new LatLng(Double.parseDouble(event.get("latitude").toString()),
                        Double.parseDouble(event.get("longitude").toString()));
                LatLng currentLoc = GlobalVariables.currentLoc;
                double dist = GMapCamera.distanceBetween(eventLoc, currentLoc);
                event.put("distance", dist);
                eventsJson.put(key, event);
            }
            webViewReady = true;
            // Send events to listview
            view.loadUrl("javascript:setEvents(" + eventsJson + ")");
            super.onPageFinished(view, url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
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