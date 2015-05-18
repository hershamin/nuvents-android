package android.nuvents.com.nuvents_android;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


public class DetailView extends ActionBarActivity {

    JSONObject json; // Event variable to be passed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);

        // Collect vars
        json = GlobalVariables.tempJson;

        // Write event json to file /data
        String fileS = NuVentsBackend.getResourcePath("tmp", "tmp").replace("tmp/tmp", "") + "data";
        File file = new File(fileS);
        try {
            if (!file.exists()) file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(json.toString());
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load webview
        WebView webView = new WebView(getApplicationContext());
        webView.setWebViewClient(new UIWebView());
        webView.getSettings().setJavaScriptEnabled(true);
        String baseURL = NuVentsBackend.getResourcePath("tmp", "tmp");
        baseURL = baseURL.replace("tmp/tmp", "");
        String fileURL = NuVentsBackend.getResourcePath("detailView", "html");
        String htmlStr = getStringFromFile(fileURL);
        webView.loadDataWithBaseURL("file://" + baseURL, htmlStr, "text/html", null, null);
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
            if (url.contains("closeDetailView://")) {
                finish();
                return true;
            } else {
                return false;
            }
        }
    }

}
