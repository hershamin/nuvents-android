package com.nuvents.android1;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;

import java.util.Locale;


public class DetailActivity extends ActionBarActivity {

    WebView webView;
    TextView titleText;
    ImageButton backButton;
    ImageButton mapButton;
    JSONObject event = GlobalVariables.tempJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Load webview
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new UIWebView());
        webView.loadUrl("file:///android_asset/detailView.html");

        // Init variables
        titleText = (TextView) findViewById(R.id.titleText);
        backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(backButtonPressed);
        mapButton = (ImageButton) findViewById(R.id.mapButton);
        mapButton.setOnClickListener(mapButtonPressed);

        // Record hit on event website by issuing a http get request
        String urlString = GlobalVariables.tempJson.get("website").toString();
        new IssueHttpGet().execute(urlString); // Call class declared at the bottom of this file

    }

    // Back button pressed
    ImageButton.OnClickListener backButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    // Map button pressed
    ImageButton.OnClickListener mapButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openMapsApp();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail_view, menu);
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

    // Open event in calendar app
    private void openCalendarApp() {
        Intent calIntent = new Intent(Intent.ACTION_INSERT);
        calIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        calIntent.setData(CalendarContract.Events.CONTENT_URI);
        String eventStartStr = ((JSONObject)event.get("time")).get("start").toString();
        calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, Long.parseLong(eventStartStr) * 1000);
        String eventEndStr = ((JSONObject)event.get("time")).get("end").toString();
        calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, Long.parseLong(eventEndStr) * 1000);
        calIntent.putExtra(CalendarContract.Events.TITLE, event.get("title").toString());
        calIntent.putExtra(CalendarContract.Events.DESCRIPTION, event.get("description").toString());
        calIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.get("address").toString());
        startActivity(calIntent);
    }

    // Open location in maps app
    private void openMapsApp() {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%s", Float.parseFloat(event.get("latitude").toString()),
                Float.parseFloat(event.get("longitude").toString()), event.get("address").toString());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    // Webview delegate methods
    private class UIWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.contains("closedetailview://")) {
                finish();
                return true;
            } else if (url.contains("opendirections://")) {
                openMapsApp();
                return true;
            } else if (url.contains("opencalendar://")) {
                openCalendarApp();
                return true;
            } else if (url.contains("file://")) { // File url
                return false; // Do not override
            } else { // Any other link
                // Open in chrome
                Uri webPage = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
                startActivity(intent);
                return true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Calculate distance between current location and event location
            LatLng eventLoc = new LatLng(Double.parseDouble(event.get("latitude").toString()),
                    Double.parseDouble(event.get("longitude").toString()));
            LatLng currentLoc = GlobalVariables.currentLoc;
            double dist = GMapCamera.distanceBetween(eventLoc, currentLoc);

            event.put("distance", dist);
            view.loadUrl("javascript:setEvent(" + event.toString() + ")");

            // Native nav-bar stuff. Add the label of the event to the nav-bar
            float rawDist = Float.parseFloat(event.get("distance").toString());
            float distMi = rawDist * (float)0.000621371; // Distance in miles
            String numText = "" + (Math.round(distMi*10.0)/10.0); // Round
            if (numText.endsWith(".0")) { numText = numText.split("\\.")[0]; } // Remove trailing zero
            final String labelText = numText + " Miles Away!";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    titleText.setText(labelText);
                }
            });

            super.onPageFinished(view, url);
        }
    }

    // Class to record hit using HTTP get request
    private class IssueHttpGet extends AsyncTask<String, Integer, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            JSONObject obj = new JSONObject();
            try {
                String urlString = params[0];
                HttpGet httpReq = new HttpGet(urlString);
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse resp = httpClient.execute(httpReq);
                int statusCode = resp.getStatusLine().getStatusCode();
                obj.put("link", urlString);
                obj.put("code", statusCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return obj;
        }
        @Override
        protected void onPostExecute(JSONObject status) {
            WelcomeActivity.sendWebRespCode(status.get("link").toString(), status.get("code").toString());
            super.onPostExecute(status);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

}