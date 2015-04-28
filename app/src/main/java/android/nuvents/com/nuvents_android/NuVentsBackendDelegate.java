package android.nuvents.com.nuvents_android;

/**
 * Created by hersh on 4/28/15.
 */

public interface NuVentsBackendDelegate {

    // Client Request
    public void nuventsServerDidReceiveNearbyEvent();
    public void nuventsServerDidReceiveEventDetail();

    // Connection Status
    public void nuventsServerDidGetNewData();
    public void nuventsServerDidConnect();
    public void nuventsServerDidDisconnect();
    public void nuventsServerDidRespondToPing();

    // Client Request Status & Other errors
    public void nuventsServerDidReceiveError();
    public void nuventsServerDidReceiveStatus();

}
