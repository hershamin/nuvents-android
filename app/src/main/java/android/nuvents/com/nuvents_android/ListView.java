package android.nuvents.com.nuvents_android;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;


public class ListView extends ActionBarActivity {

    Map<String, JSONObject> events;  // Events in the form of NSDict or Hashmap

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        // Collect vars
        events = GlobalVariables.eventJson;

        // Write event json to file /data
        String fileS = NuVentsBackend.getResourcePath("tmp", "tmp").replace("tmp/tmp", "") + "data";
        File file = new File(fileS);
        try {
            if (!file.exists()) file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(events.toString().replaceAll("=",":"));
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("EVENTS", events.toString().replaceAll("=",":"));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_view, menu);
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
