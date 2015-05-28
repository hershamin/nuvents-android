package android.nuvents.com.nuvents_android;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class DetailView extends ActionBarActivity {

    JSONObject json; // Event variable to be passed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);

        // Collect vars
        json = GlobalVariables.tempJson;

        // Load webview
        WebView webView = new WebView(getApplicationContext());
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new UIWebView());
        /*String baseURL = NuVentsBackend.getResourcePath("tmp", "tmp", false);
        baseURL = baseURL.replace("tmp/tmp", "");
        String fileURL = NuVentsBackend.getResourcePath("detailView", "html", false);
        String htmlStr = getStringFromFile(fileURL);
        webView.loadDataWithBaseURL("file://" + baseURL, htmlStr, "text/html", null, null);*/
        webView.loadUrl("http://storage.googleapis.com/nuvents-resources/detailViewTest.html");
        setContentView(webView);
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
            if (url.contains("closedetailview://")) {
                finish();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.loadUrl("javascript:setEvent(" + json.toString() + ")");
            super.onPageFinished(view, url);
        }
    }

}
