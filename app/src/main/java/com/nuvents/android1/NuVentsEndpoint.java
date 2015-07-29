package com.nuvents.android1;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.google.android.gms.maps.model.LatLng;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hersh on 7/29/15.
 */

public class NuVentsEndpoint {

    // Singleton Initialization
    private static NuVentsEndpoint nInstance = null;
    public static NuVentsEndpoint sharedEndpoint(Context context) {
        applicationContext = context;
        if (nInstance == null) {
            nInstance = new NuVentsEndpoint();
        }
        return nInstance;
    }

    // Global Constants
    final String udid = Settings.Secure.getString(applicationContext.getContentResolver(),
            Settings.Secure.ANDROID_ID); // Unique Device ID

    // Global Variables
    Map<String, JSONObject> eventJSON = new HashMap<String, JSONObject>();

    // Internally used variables
    private static Context applicationContext;
    private static Socket nSocket;
    static { // The variable "BuildConfig.backend" is compiled during build, Set in build.gradle (Module: app)
        try {
            nSocket = IO.socket(BuildConfig.backend);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Connect to backend
    public void connect() {
        addSocketHandlingMethods();
        nSocket.connect();
    }

    // Disconnect from backend
    public void disconnect() {
        nSocket.off();
        nSocket.disconnect();
    }

    // Send event website response code
    public void sendWebsiteCode(String website, String code) {
        JSONObject obj = new JSONObject();
        obj.put("website", website);
        obj.put("respCode", code);
        nSocket.emit("event:website", obj);
    }

    // Send event request to add city
    public void sendEventReq(String request) {
        nSocket.emit("event:request", request);
    }

    // Get nearby events
    public void getNearbyEvents(LatLng location, float radius, float timestamp) {
        JSONObject obj = new JSONObject();
        obj.put("lat", "" + location.latitude);
        obj.put("lng", "" + location.longitude);
        obj.put("rad", "" + radius);
        obj.put("time", "" + timestamp);
        obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
        nSocket.emit("event:nearby", obj);
    }

    // Get event detail
    public void getEventDetail(String eventID, final JSONCallable callback) {
        JSONObject obj = new JSONObject();
        obj.put("eid", eventID);
        obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
        nSocket.emit("event:detail", obj, new Ack() {
            @Override
            public void call(Object... args) {
                String retStr = (String)args[0];
                if (!retStr.contains("Error")) {
                    Object rawObj = JSONValue.parse(retStr);
                    JSONObject obj = (JSONObject)rawObj;
                    callback.json(obj);
                } else {
                    // TODO: Handle ERROR
                }
            }
        });
    }

    // Sync resources with server
    private void syncResources(JSONObject jsonData) {

        // Get resources if not present on the internal file system or different
        JSONObject types = (JSONObject)jsonData.get("resource");
        for (Object type : types.keySet()) { // Resource types
            JSONObject resources = (JSONObject)types.get(type);
            for (Object resource : resources.keySet()) { // Resources

                String path = NuVentsHelper.getResourcePath((String) resource, (String) type, true, applicationContext);
                File pathFile = new File(path);
                if (!pathFile.exists()) { // File does not exist
                    NuVentsHelper.downloadFile(path, (String) resources.get(resource)); // Download from provided url
                } else {
                    JSONObject o1 = (JSONObject)jsonData.get("md5sum");
                    JSONObject o2 = (JSONObject)o1.get(type);
                    String md5sumWeb = (String)o2.get(resource);
                    String md5sumInt = NuVentsHelper.getMD5SUM(path);
                    if (md5sumWeb != md5sumInt) { // MD5 sum does not match, redownload file
                        NuVentsHelper.downloadFile(path, (String) resources.get(resource));
                    }
                }

            }
        }
    }

    // Socket handling methods
    private void addSocketHandlingMethods() {
        // Nearby Event Received
        nSocket.on("event:nearby", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Object rawObj = JSONValue.parse((String) args[0]);
                JSONObject obj = (JSONObject)rawObj;
                // Add to global vars
                NuVentsEndpoint.sharedEndpoint(applicationContext).eventJSON.put((String)obj.get("eid"), obj);
            }
        });

        // Nearby Event Error & Status
        nSocket.on("event:nearby:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    Log.e("NuVents Endpoint", "Event Nearby: " + resp);
                } else {
                    Log.i("NuVents Endpoint", "Event Nearby Received");
                }
            }
        });

        // Resources status
        nSocket.on("resources:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    Log.e("NuVents Endpoint", "Resources " + resp);
                } else {
                    Log.i("NuVents Endpoint", "Resources Received");
                }
            }
        });

        // Received resources from server
        nSocket.on("resources", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Object rawObj = JSONValue.parse((String)args[0]);
                JSONObject obj = (JSONObject)rawObj;
                syncResources(obj);
            }
        });

        // Connection Status
        nSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = new JSONObject();
                obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
                obj.put("dm", NuVentsHelper.getDeviceHardware());
                nSocket.emit("device:initial", obj);
                Log.i("NuVents Endpoint", "Connected");
            }
        });
        nSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i("NuVents Endpoint", "Disconnected");
            }
        });
        nSocket.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("NuVents Endpoint", "Connection: " + (String)args[0]);
            }
        });
    }

}