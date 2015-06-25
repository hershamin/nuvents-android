package android.nuvents.com.nuvents_android;

import android.content.Intent;
import android.media.Image;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;


public class CategoryActivity extends ActionBarActivity {

    WebView webView;
    ImageButton homeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Init vars
        webView =(WebView) findViewById(R.id.webView);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWebViewClient(new UIWebView());
        webView.loadUrl("file:///android_asset/categoryView.html");
        homeBtn = (ImageButton) findViewById(R.id.homeBtn);
        homeBtn.setOnClickListener(homeBtnClicked);

    }

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
            if (url.contains("openmapview://")) {
                Intent mapView = new Intent(getApplicationContext(), MapActivity.class);
                GlobalVariables.category = url.split("//")[1];
                startActivity(mapView);
                return true;
            } else if (url.contains("openlistview://")) {
                Intent listView = new Intent(getApplicationContext(), ListActivity.class);
                GlobalVariables.category = url.split("//")[1];
                startActivity(listView);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //
            super.onPageFinished(view, url);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_category, menu);
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
