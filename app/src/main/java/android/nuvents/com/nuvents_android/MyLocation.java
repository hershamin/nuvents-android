package android.nuvents.com.nuvents_android;

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;

/**
 * Created by hersh on 6/5/15.
 * Code from: http://stackoverflow.com/questions/3145089/what-is-the-simplest-and-most-robust-way-to-get-the-users-current-location-in-a/3145655#3145655
 *
 * Algorithm:
 *  1) Check what providers are enabled, Some may be disabled in the device or he application's manifest
 *  2) If any provider is available start location listeners and timeout timers (20s)
 *  3) If there is an update from location listener, use the provided value, stop listeners and timer
 *  4) If there is no update and the timer elapses, use the last known values
 *  5) Grab the last known values from available providers and choose the most recent
 */

public class MyLocation {
    Timer timer1;
    LocationManager lm;
    LocationResult locationResult;
    boolean gpsEnabled = false;
    boolean networkEnabled = false;

    public boolean getLocation(final Context context, LocationResult result) {
        // Use LocationResult callback class to pass location from MyLocation to user code
        locationResult = result;
        if (lm == null) {
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        // exceptions will be thrown if the provider is not permitted.
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {}
        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {}

        // don't start listeners if no provider is enabled, Alert user instead
        if (!gpsEnabled && !networkEnabled) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setMessage("Looks like gps and/or network location is disabled! Please " +
                    "go to settings and enabled them to search for events nearby!!");
            alertDialog.setPositiveButton("Open Location Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(locationIntent);
                }
            });
            alertDialog.setNegativeButton("Cancel", null);
            alertDialog.show();
            return false;
        }

        if (gpsEnabled) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGPS);
        }
        if (networkEnabled) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        }
        timer1 = new Timer();
        timer1.schedule(new GetLastLocation(), 20000);
        return true;
    }

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            timer1.cancel();
            locationResult.gotLocation(location);
            lm.removeUpdates(this);
            lm.removeUpdates(locationListenerNetwork);
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            timer1.cancel();
            locationResult.gotLocation(location);
            lm.removeUpdates(this);
            lm.removeUpdates(locationListenerGPS);
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    };

    class GetLastLocation extends TimerTask {
        @Override
        public void run(){
            lm.removeUpdates(locationListenerGPS);
            lm.removeUpdates(locationListenerNetwork);

            Location netLoc = null, gpsLoc = null;
            if (gpsEnabled) {
                gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (networkEnabled) {
                netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // If there are both values, use the latest one
            if (gpsLoc != null && netLoc != null) {
                if (gpsLoc.getTime() > netLoc.getTime()) {
                    locationResult.gotLocation(gpsLoc);
                } else {
                    locationResult.gotLocation(netLoc);
                }
                return;
            }

            if (gpsLoc != null) {
                locationResult.gotLocation(gpsLoc);
                return;
            }

            if (netLoc != null) {
                locationResult.gotLocation(netLoc);
                return;
            }
            locationResult.gotLocation(null);
        }
    }

    public static abstract class LocationResult {
        public abstract void gotLocation(Location location);
    }
}
