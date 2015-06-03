package android.nuvents.com.nuvents_android;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.crashlytics.android.Crashlytics;
import android.util.Log;

import io.fabric.sdk.android.Fabric;
import org.json.simple.JSONObject;

import java.net.URISyntaxException;

public class WelcomeActivity extends ActionBarActivity implements NuVentsBackendDelegate {

    public NuVentsBackend api;
    public boolean serverConn = false;
    public boolean initialLoc = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_welcome);

        // Init NuVents backend API
        try {
            String filesDir = getApplicationContext().getFilesDir().getPath();
            api = new NuVentsBackend(this, GlobalVariables.server, "test", filesDir);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    // NuVents server resources sync complete
    @Override
    public void nuventsServerDidSyncResources() {
        //
    }

    // Open Detail View
    public void openDetailView(final String eid) {
        api.getEventDetail(eid, new JSONCallable() {
            @Override
            public void json(JSONObject jsonData) {
                // Merge event summary & detail
                JSONObject summary = GlobalVariables.eventJson.get(eid);
                for (Object summ : summary.keySet()) {
                    jsonData.put(summ, summary.get(summ));
                }
                // Present detail view
                Intent detailView = new Intent(getApplicationContext(), DetailView.class);
                GlobalVariables.tempJson = jsonData;
                startActivity(detailView);
            }
        });
    }

    // MARK: NuVents Backend Methods
    @Override
    public void nuventsServerDidReceiveNearbyEvent(JSONObject event) {
        // Add to global vars
        GlobalVariables.eventJson.put((String)event.get("eid"), event);
    }

    @Override
    public void nuventsServerDidConnect() {
        Log.i("Server", "NuVents Backend connected");
        api.pingServer();
        serverConn = true;
    }

    @Override
    public void nuventsServerDidDisconnect() {
        Log.i("Server", "NuVents Backend disconnected");
    }

    @Override
    public void nuventsServerDidRespondToPing(String response) {
        Log.i("PING", "RESPONSE: " + response);
    }

    @Override
    public void nuventsServerDidReceiveError(String type, String error) {
        //
    }

    @Override
    public void nuventsServerDidReceiveStatus(String type, String status) {
        //
    }
}
