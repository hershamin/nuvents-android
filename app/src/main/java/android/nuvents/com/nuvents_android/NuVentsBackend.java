package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 4/28/15.
 */

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URISyntaxException;

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

    public static BitmapDescriptor getMarkerIcon(String snippet) {
        Log.i("print", "IMAGE");
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