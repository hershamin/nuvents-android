package com.nuvents.android1;

/**
 * Created by hersh on 4/28/15.
 */

import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.util.ByteArrayBuffer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

public class NuVentsBackend {

    private NuVentsBackendDelegate delegate;
    private String deviceID;
    private Socket nSocket;
    private static String filesDir;

    // Initialization called
    public NuVentsBackend(NuVentsBackendDelegate delegatePassed, String server, String devID, String fileDir) throws URISyntaxException {
        delegate = delegatePassed; // Assign delegate
        deviceID = devID; // Get device ID
        filesDir = fileDir; // Get files directory from application context
        // Socket connection handling
        nSocket = IO.socket(server);
        addSocketHandlingMethods();
        nSocket.connect();
    }

    // Sync resources with server
    private void syncResources(JSONObject jsonData) {

        // Get resources if not present on the internal file system or different
        JSONObject types = (JSONObject)jsonData.get("resource");
        for (Object type : types.keySet()) { // Resource types
            JSONObject resources = (JSONObject)types.get(type);
            for (Object resource : resources.keySet()) { // Resources

                String path = getResourcePath((String)resource, (String)type, true);
                File pathFile = new File(path);
                if (!pathFile.exists()) { // File does not exist
                    downloadFile(path, (String)resources.get(resource)); // Download from provided url
                } else {
                    JSONObject o1 = (JSONObject)jsonData.get("md5sum");
                    JSONObject o2 = (JSONObject)o1.get(type);
                    String md5sumWeb = (String)o2.get(resource);
                    String md5sumInt = getMD5SUM(path);
                    if (md5sumWeb != md5sumInt) { // MD5 sum does not match, redownload file
                       downloadFile(path, (String)resources.get(resource));
                    }
                }

            }
        }
        delegate.nuventsServerDidSyncResources();
    }

    // Function to download from web & save
    private void downloadFile(String filePath, String url) {
        try {
            URL urlU = new URL(url);
            File file = new File(filePath);
            URLConnection uConn = urlU.openConnection();

            InputStream is = uConn.getInputStream();
            BufferedInputStream bufferInStream = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(5000);
            int current = 0;
            while ((current = bufferInStream.read()) != -1) {
                baf.append((byte) current);
            }

            FileOutputStream fos =new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get MD5SUM of file
    private String getMD5SUM(String filePath) {

        String md5sum = "";
        try {
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
            md5sum += sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return md5sum;
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

    // Resize image based on width
    public static Bitmap resizeImage(Bitmap origImage, float width) {

        // Resize image to required width
        int oldWidth = origImage.getWidth();
        float scaleFactor = width / oldWidth;
        int newHeight = (int)(origImage.getHeight() * scaleFactor);
        int newWidth = (int)(oldWidth * scaleFactor);
        Bitmap newImage = Bitmap.createScaledBitmap(origImage, newWidth, newHeight, true);

        return newImage;
    }

    // Get resource from internal file system
    public static String getResourcePath(String resource, String type, Boolean override) {
        // Create directories if not present
        String mainDirS = filesDir + "/resources/" + type;
        File mainDir = new File(mainDirS);
        if (!mainDir.exists()) {
            mainDir.mkdirs();
        }
        // Return file path
        String filePath = mainDirS + "/" + resource;
        // Check if marker icon exists if not send a default one
        File filepathF = new File(filePath);
        if (!filepathF.exists() && type.equals("marker") && !override) {
            filePath = mainDirS + "/default";
        } // Only triggered if override is set to true
        return filePath;
    }

    // Get nearby events
    public void getNearbyEvents(LatLng location, float radius, float timestamp) {
        JSONObject obj = new JSONObject();
        obj.put("lat", "" + location.latitude);
        obj.put("lng", "" + location.longitude);
        obj.put("rad", "" + radius);
        obj.put("time", "" + timestamp);
        obj.put("did", deviceID);
        nSocket.emit("event:nearby", obj);
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

    // Get event detail
    public void getEventDetail(String eventID, final JSONCallable callback) {
        JSONObject obj = new JSONObject();
        obj.put("eid", eventID);
        obj.put("did", deviceID);
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

        // Resources status
        nSocket.on("resources:status", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String resp = (String)args[0];
                if (resp.contains("Error")) { // error status
                    delegate.nuventsServerDidReceiveError("Resources", resp);
                } else {
                    delegate.nuventsServerDidReceiveStatus("Resources", resp);
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
                delegate.nuventsServerDidConnect();
                JSONObject obj = new JSONObject();
                obj.put("did", deviceID);
                obj.put("dm", getDeviceHardware());
                nSocket.emit("device:initial", obj);
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