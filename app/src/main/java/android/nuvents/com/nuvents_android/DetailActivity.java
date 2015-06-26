package android.nuvents.com.nuvents_android;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;

import java.util.Locale;


public class DetailActivity extends ActionBarActivity {

    WebView webView;

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

        // Record hit on event website by issuing a http get request
        String urlString = GlobalVariables.tempJson.get("website").toString();
        new IssueHttpGet().execute(urlString); // Call class declared at the bottom of this file

    }

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

    // Open location in maps app
    private void openMapsApp(String lat, String lng) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", Float.parseFloat(lat),
                Float.parseFloat(lng), Float.parseFloat(lat), Float.parseFloat(lng));
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
                String loc = url.split("//")[1];
                String lat = loc.split(",")[0];
                String lng = loc.split(",")[1];
                openMapsApp(lat, lng);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            JSONObject event = GlobalVariables.tempJson;
            // Calculate distance between current location and event location
            LatLng eventLoc = new LatLng(Double.parseDouble(event.get("latitude").toString()),
                    Double.parseDouble(event.get("longitude").toString()));
            LatLng currentLoc = GlobalVariables.currentLoc;
            double dist = GMapCamera.distanceBetween(eventLoc, currentLoc);
            event.put("distance", dist);
            view.loadUrl("javascript:setEvent(" + event.toString() + ")");
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
