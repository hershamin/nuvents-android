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
        IO.Options opts = new IO.Options();
        opts.transports = new String[]{"websocket"};
        nSocket = IO.socket(server, opts);
        addSocketHandlingMethods();
        nSocket.connect();
    }

    public void pingServer() { // Ping server for sanity check
        nSocket.emit("ping", deviceID);
    }

    // Socket handling methods
    private void addSocketHandlingMethods() {

        // On any socket event
        //  CANNOT implement in Android

    }

}