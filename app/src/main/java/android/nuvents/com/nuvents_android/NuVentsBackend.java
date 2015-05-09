package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 4/28/15.
 */

import android.os.Build;
import android.text.TextUtils;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.model.LatLng;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class NuVentsBackend {

    private NuVentsBackendDelegate delegate;
    private String deviceID;
    private Socket nSocket;

    // Initialization called
    public NuVentsBackend(NuVentsBackendDelegate delegatePassed, String server, String devID) throws URISyntaxException {
        delegate = delegatePassed; // Assign delegate
        deviceID = devID; // Get device ID
        // Socket connection handling
        nSocket = IO.socket(server);
        addSocketHandlingMethods();
        nSocket.connect();
    }

    // Sync resources with server
    private void syncResources() {
        JSONObject obj = new JSONObject();
        obj.put("did", deviceID);
        obj.put("dm", getDeviceHardware());
        nSocket.emit("device:initial", obj, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Object rawObj = JSONValue.parse((String) args[0]);
                JSONObject jsonData = (JSONObject) rawObj;
                GlobalVariables.resources = (JSONObject)jsonData.get("resources");
                // TODO: Sync resources
            }
        });
    }

    // Get MD5SUM of file
    private String getMD5SUM(String filePath) throws NoSuchAlgorithmException, FileNotFoundException
            , IOException {
        FileInputStream fis = new FileInputStream(filePath);
        char[] hexDigits = "0123456789abcdef".toCharArray();
        int read = 0;
        byte[] bytes = new byte[4096];

        MessageDigest digest = MessageDigest.getInstance("MD5");
        while ((read = fis.read(bytes)) != -1) {
            digest.update(bytes, 0, read);
        }
        byte[] messageDigest = digest.digest();

        StringBuilder sb = new StringBuilder(32);
        for (byte b : messageDigest) {
            sb.append(hexDigits[(b >> 4) & 0x0f]);
            sb.append(hexDigits[b & 0x0f]);
        }

        return sb.toString();
    }

    // Get device hardware type
    private String getDeviceHardware() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        if (manufacturer.equalsIgnoreCase("HTC")) {
            // make sure "HTC" is fully capitalized.
            return  "HTC " + model;
        }
        return capitalize(manufacturer) + " " + model;
    }

    // Helper function for getting device hardware type
    private String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        final char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (final char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }

    // Get resource from internal file system
    public static String getResourcePath(String resource, String type) {
        JSONObject resources = (JSONObject)GlobalVariables.resources.get(type);
        String[] fileNameTemp = resources.get(resource).toString().split("/");
        String fileName = type + "/" + fileNameTemp[fileNameTemp.length - 1];
        String filePath = "resources/" + fileName;
        return filePath;
    }

    // Get nearby events
    public void getNearbyEvents(LatLng location, float radius) {
        JSONObject obj = new JSONObject();
        obj.put("lat", "" + location.latitude);
        obj.put("lng", "" + location.longitude);
        obj.put("rad", "" + radius);
        obj.put("did", deviceID);
        nSocket.emit("event:nearby", obj);
    }

    // Get event detail
    public void getEventDetail(String eventID) {
        JSONObject obj = new JSONObject();
        obj.put("eid", eventID);
        obj.put("did", deviceID);
        nSocket.emit("event:detail", obj);
    }

    public void pingServer() { // Ping server for sanity check
        nSocket.emit("ping", deviceID);
    }

    // Socket handling methods
    private void addSocketHandlingMethods() {
        // Nearby Event Received
        nSocket.on("event:nearby", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Object rawObj = JSONValue.parse((String)args[0]);
                JSONObject obj = (JSONObject)rawObj;
                delegate.nuventsServerDidReceiveNearbyEvent(obj);
            }
        });

        // Nearby Event Error & Status
        nSocket.on("event:nearby:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    delegate.nuventsServerDidReceiveError("Event Nearby", resp);
                } else {
                    delegate.nuventsServerDidReceiveStatus("Event Nearby", resp);
                }
            }
        });

        // Detail Event Received
        nSocket.on("event:detail", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Object rawObj = JSONValue.parse((String)args[0]);
                JSONObject obj = (JSONObject)rawObj;
                delegate.nuventsServerDidReceiveEventDetail(obj);
            }
        });

        // Detail Event Error & Status
        nSocket.on("event:detail:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    delegate.nuventsServerDidReceiveError("Event Detail", resp);
                } else {
                    delegate.nuventsServerDidReceiveStatus("Event Detail", resp);
                }
            }
        });

        // Server ping response
        nSocket.on("pong", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                delegate.nuventsServerDidRespondToPing((String)args[0]);
            }
        });

        // Connection Status
        nSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                delegate.nuventsServerDidConnect();
                syncResources();
            }
        });
        nSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                delegate.nuventsServerDidDisconnect();
            }
        });
        nSocket.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                delegate.nuventsServerDidReceiveError("Connection", (String)args[0]);
            }
        });

        // On any socket event
        //  CANNOT implement in Android

    }

}