package android.nuvents.com.nuvents_android;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;


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
        webView.loadUrl(GlobalVariables.detailView);

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
            JSONObject json = GlobalVariables.tempJson;
            view.loadUrl("javascript:setEvent(" + json.toString() + ")");
            super.onPageFinished(view, url);
        }
    }

    // Class to record hit using HTTP get request
    private class IssueHttpGet extends AsyncTask<String, Integer, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            int statusCode = 500;
            try {
                String urlString = params[0];
                HttpGet httpReq = new HttpGet(urlString);
                HttpClient httpClient = new DefaultHttpClient();
                HttpResponse resp = httpClient.execute(httpReq);
                statusCode = resp.getStatusLine().getStatusCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return statusCode;
        }
        @Override
        protected void onPostExecute(Integer statusCode) {
            Log.i("WEB", "" + statusCode); // TODO: send status code with link to backend
            super.onPostExecute(statusCode);
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

}
