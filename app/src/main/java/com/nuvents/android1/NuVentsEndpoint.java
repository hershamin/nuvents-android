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
import java.util.ArrayList;
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
    JSONObject tempJson = new JSONObject();

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
    private boolean connected = false; // To keep track of server connection status
    private ArrayList<String> socketBuffer = new ArrayList<String>(); // To store failed to send socket events in buffer to retry on connection

    // Connect to backend
    public void connect() {
        addSocketHandlingMethods();
        nSocket.connect();
    }

    // Disconnect from backend
    public void disconnect() {
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
    public void getNearbyEvents(LatLng location, float radius) {
        JSONObject obj = new JSONObject();
        obj.put("lat", "" + location.latitude);
        obj.put("lng", "" + location.longitude);
        obj.put("rad", "" + radius);
        obj.put("time", "" + ((float) System.currentTimeMillis() / (float) 1000.0));
        obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
        nSocket.emit("event:nearby", obj);
        // Add to buffer
        String buffEntry = obj.toString();
        socketBuffer.add("event:nearby||" + buffEntry);
    }

    // Get event detail
    public void getEventDetail(String eventID, final JSONCallable callback) {
        JSONObject obj = new JSONObject();
        obj.put("eid", eventID);
        obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
        obj.put("time", "" + ((float) System.currentTimeMillis() / (float) 1000.0));
        nSocket.emit("event:detail", obj);
        // Add to buffer
        String buffEntry = obj.toString();
        socketBuffer.add("event:detail||" + buffEntry);
    }

    // Get resources from server
    private void getResourcesFromServer() {
        JSONObject obj = new JSONObject();
        obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
        obj.put("dm", NuVentsHelper.getDeviceHardware());
        nSocket.emit("resources", obj);
        // Add to buffer
        String buffEntry = obj.toString();
        socketBuffer.add("resources||" + buffEntry);
    }

    // Sync resources with server
    private void syncResources(JSONObject jsonData) {

        // Get resources if not present on the internal file system or different
        JSONObject types = (JSONObject)jsonData.get("resource");
        for (Object type : types.keySet()) { // Resource types
            JSONObject resources = (JSONObject)types.get(type);
            for (Object resource : resources.keySet()) { // Resources

                String path = NuVentsHelper.getResourcePath((String) resource, (String) type, applicationContext);
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
        Log.i("NuVents Endpoint", "Resources Sync Complete");
    }

    // Send request to retrieve missed messages from server
    private void retrieveMissedMessages() {
        JSONObject obj = new JSONObject();
        obj.put("did", NuVentsEndpoint.sharedEndpoint(applicationContext).udid);
        obj.put("dm", NuVentsHelper.getDeviceHardware());
        nSocket.emit("history", obj);
    }

    // Empty local buffer of socket message
    private void emptyLocalBuffer() {
        // Empty Local Buffer of socket messages
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
                // Acknowledge Server
                Ack ack = (Ack) args[args.length - 1];
                ack.call("Nearby Event Received");
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
                // Acknowledge Server
                Ack ack = (Ack) args[args.length - 1];
                ack.call("Nearby Event Status Received");
            }
        });

        // Event Detail Received
        nSocket.on("event:detail", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                JSONObject jsonData = (JSONObject)JSONValue.parse(resp);
                // Add to global variable
                NuVentsEndpoint.sharedEndpoint(applicationContext).tempJson = jsonData;
                // Notify views
                // TODO: Notify Views
                // Acknowledge Server
                Ack ack = (Ack) args[args.length - 1];
                ack.call("Event Detail Received");
            }
        });

        // Event Detail Error & Status
        nSocket.on("event:detail:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    Log.e("NuVents Endpoint", "Event Detail: " + resp);
                } else {
                    Log.e("NuVents Endpoint", "Event Detail Received");
                }
            }
        });

        // Resources received from server
        nSocket.on("resources", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    Log.e("NuVents Endpoint", "Resources: " + resp);
                } else {
                    Log.i("NuVents Endpoint", "Resources Received");
                    JSONObject jsonData = (JSONObject)JSONValue.parse(resp);
                    syncResources(jsonData); // Sync Resources
                }
                // Acknowledge Server
                Ack ack = (Ack) args[args.length - 1];
                ack.call("Resources Received");
            }
        });

        // Connection Status
        nSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                emptyLocalBuffer();
                retrieveMissedMessages();
                getResourcesFromServer();
                connected = true;
                Log.i("NuVents Endpoint", "Connected");
            }
        });
        nSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                connected = false;
                Log.i("NuVents Endpoint", "Disconnected");
                nSocket.off();
            }
        });
        nSocket.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                connected = false;
                Log.e("NuVents Endpoint", "Connection: " + (String)args[0]);
            }
        });
    }

}