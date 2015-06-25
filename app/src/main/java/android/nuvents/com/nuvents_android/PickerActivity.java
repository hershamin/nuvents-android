package android.nuvents.com.nuvents_android;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.simple.JSONObject;

import java.util.Map;


public class PickerActivity extends ActionBarActivity {

    WebView webView;

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
            } else {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Convert hashmap to json
            Map<String, JSONObject> eventMap = GlobalVariables.eventJson;
            // Send to webview
            view.loadUrl("javascript:setEventCount(" + eventMap.keySet().size() + ")");
            // Get image url from assets
            String imgURL = "file:///android_asset/catViewBack.png";
            // Send to webview
            view.loadUrl("javascript:setImgUrl(\"" + imgURL + "\")");
            super.onPageFinished(view, url);
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
