package android.nuvents.com.nuvents_android;

import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class DetailView extends ActionBarActivity {

    JSONObject json; // Event variable to be passed
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);

        // Collect vars
        json = GlobalVariables.tempJson;

        loadPartialView();

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

    // Load detail view
    private void loadDetailView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String baseURL = NuVentsBackend.getResourcePath("tmp", "tmp"); // Base URL: resources dir
                baseURL = baseURL.replace("tmp/tmp", "");
                String fileURL = NuVentsBackend.getResourcePath("detailView", "html"); // Detail view html
                String htmlStr = getStringFromFile(fileURL);
                webView.loadDataWithBaseURL(baseURL, htmlStr, "text/html", null, null);
            }
        });
    }

    // Load partial view
    private void loadPartialView() {
        // Add views if not present to the hierarchy
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Add webview
                if (webView == null) {
                    webView = new WebView(getApplicationContext());
                    webView.setWebViewClient(new UIWebView());
                    webView.getSettings().setJavaScriptEnabled(true);
                    setContentView(webView);
                }
                String baseURL = NuVentsBackend.getResourcePath("tmp", "tmp"); // Base URL: resources dir
                baseURL = baseURL.replace("tmp/tmp", "");
                String fileURL = NuVentsBackend.getResourcePath("partialView", "html"); // Partial view html
                String htmlStr = getStringFromFile(fileURL);
                webView.loadDataWithBaseURL(baseURL, htmlStr, "text/html", null, null);
            }
        });
    }

    // Helper method to get string from file
    private String getStringFromFile(String filePath) {
        StringBuilder output = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    // Webview delegate methods
    private class UIWebView extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.contains("openDetailView://")) {
                loadDetailView();
                return true;
            } else if (url.contains("closeDetailView://")) {
                loadPartialView();
                return true;
            } else if (url.contains("closePartialView://")) {
                finish();
                return true;
            } else {
                return false;
            }
        }
    }

}
