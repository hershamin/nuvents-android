package android.nuvents.com.nuvents_android;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.simple.JSONObject;

import java.util.Map;


public class ListActivity extends ActionBarActivity {

    WebView webView;
    EditText searchField;
    LinearLayout mainLinLay;

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
        webView.loadUrl(GlobalVariables.listView);
        searchField.addTextChangedListener(searchFieldWatcher);
        mainLinLay = (LinearLayout) findViewById(R.id.mainLinLay);
        mainLinLay.setOnTouchListener(screenTouchListener);
    }

    // Dismiss text field on clicks anywhere other than keyboard
    View.OnTouchListener screenTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent ev) {
            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            return false;
        }
    };

    // Search field changed value
    TextWatcher searchFieldWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean searchProcess = GlobalVariables.searchProc;
            if (!searchProcess) { // Search process free
                searchProcess = true;
                String searchText = searchField.getText().toString();
                webView.loadUrl("javascript:searchByTitle('" + searchText + "')");
                searchProcess = false;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            //
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
                eventsJson.put(key, eventMap.get(key));
            }
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
