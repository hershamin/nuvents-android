package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 4/28/15.
 */

import org.json.simple.JSONObject;

public interface NuVentsBackendDelegate {

    // Resources ready
    public void nuventsServerDidSyncResources();                            // Resources are synced with device

    // Client Request
    public void nuventsServerDidReceiveNearbyEvent(JSONObject event);       // Got nearby event
    public void nuventsServerDidReceiveEventDetail(JSONObject event);       // Got event detail

    // Connection Status
    //public void nuventsServerDidGetNewData(String channel, Object data);    // Got new data from any WS event
            // CANNOT implement in Android
    public void nuventsServerDidConnect();                                  // Connected
    public void nuventsServerDidDisconnect();                               // Disconnected
    public void nuventsServerDidRespondToPing(String response);             // Got ping response

    // Client Request Status & Other errors
    public void nuventsServerDidReceiveError(String type, String error);    // Error
    public void nuventsServerDidReceiveStatus(String type, String status);  // Status

}
