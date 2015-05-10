package android.nuvents.com.nuvents_android;

import android.graphics.Point;
import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by hersh on 5/6/15.
 */

// Called for Google Maps Camera Change
public class GMapCamera {

    // Camera changed
    public static void cameraChanged(CameraPosition position, Point size) {
        GoogleMap mapView = GlobalVariables.mapView; // MapView
        // TODO: Remove markers not in view

        // Find zoom level difference
        float zoomDiff;
        CameraPosition prevLoc = GlobalVariables.prevCam;
        if (prevLoc == null) return; // Stop execution if previous location is not available
        if (prevLoc.zoom > position.zoom) {
            zoomDiff = prevLoc.zoom - position.zoom;
        } else {
            zoomDiff = position.zoom - prevLoc.zoom;
        }
        if (zoomDiff > GlobalVariables.zoomLevelMargin) {
            clusterMarkers(mapView, position, null, size);
        }
    }

    // Cluster markers
    public static void clusterMarkers(GoogleMap mapView, CameraPosition position, String specialEID, Point size) {
        ArrayList<Marker> markers = GlobalVariables.eventMarkers;

        if (position.zoom < GlobalVariables.zoomLevelClusteringLimit) {
            float clusteringConstant = position.zoom * GlobalVariables.clusteringMultiplier;
            // Used to determine the clustering of map markers.
            //  The lower the constant, the less clustering it has & vice-versa

            // Calculate tolerance
            Projection currentView = mapView.getProjection();
            LatLng topLeftCorner = currentView.fromScreenLocation(new Point(0, 0));
            LatLng topRightCorner = currentView.fromScreenLocation(new Point(Math.round(size.x/clusteringConstant), 0));
            float tolerance = (float)distanceBetween(topLeftCorner, topRightCorner);

            // Arrange Markers
            ArrayList<Integer> usedIndices = new ArrayList<Integer>();
            ArrayList<Integer> tempIndices = new ArrayList<Integer>();
            for (int i=0; i<markers.size(); i++) {
                if (!usedIndices.contains(i)) { // Index not used yet
                    for (int j=i+1; j<markers.size(); j++) {
                        Marker m1 = markers.get(i);
                        Marker m2 = markers.get(j);
                        LatLng m11 = m1.getPosition();
                        LatLng m22 = m2.getPosition();
                        float d12 = (float)distanceBetween(m11, m22);

                        if (d12 < tolerance) {
                            tempIndices.add(i);
                            tempIndices.add(j);
                            usedIndices.add(j);
                        }
                    }
                    // Remove similar objects
                    ArrayList<Integer> preSortArray = new ArrayList<Integer>();
                    preSortArray.addAll(tempIndices);
                    tempIndices.clear();
                    ArrayList<Integer> existingObjects = new ArrayList<Integer>();
                    for (int j=0; j<preSortArray.size(); j++) {
                        if (!existingObjects.contains(preSortArray.get(j))) {
                            existingObjects.add(preSortArray.get(j));
                            tempIndices.add(preSortArray.get(j));
                        }
                    }
                    if (tempIndices.size() != 0) {
                        // cluster, change icons
                        // Random marker in cluster or special marker to not resize
                        int randomIndex = new Random().nextInt(tempIndices.size());
                        // Make the rest of the markers smaller
                        for (int k=0; k<tempIndices.size(); k++) {
                            if (k != randomIndex && specialEID == null) {
                                Marker marker = markers.get(tempIndices.get(k));
                                marker.setIcon(BitmapDescriptorFactory.fromFile(
                                        NuVentsBackend.getResourcePath("cluster", "marker")
                                ));
                            } else if (specialEID != null && markers.get(tempIndices.get(k)).getTitle() == specialEID) {
                                // marker to keep bigger
                                Marker marker = markers.get(tempIndices.get(k));
                                marker.setIcon(BitmapDescriptorFactory.fromFile(
                                        NuVentsBackend.getResourcePath(marker.getSnippet(), "marker")
                                ));
                            } else {
                                // marker to keep bigger
                                Marker marker = markers.get(tempIndices.get(k));
                                marker.setIcon(BitmapDescriptorFactory.fromFile(
                                        NuVentsBackend.getResourcePath(marker.getSnippet(), "marker")
                                ));
                            }
                        }
                    }
                }
                usedIndices.add(i);
                tempIndices.clear();
            }
        } else {
            // Zoom level is greater than equal to ZoomLevel clustering limit
            //  Return all markers to original specs
            for (Marker marker : markers) {
                marker.setIcon(BitmapDescriptorFactory.fromFile(
                        NuVentsBackend.getResourcePath(marker.getSnippet(), "marker")
                ));
            }
        }
    }

    // Find distance between 2 GPS points
    public static double distanceBetween(LatLng ptA, LatLng ptB) {
        Location loc1 = new Location("l1");
        loc1.setLatitude(ptA.latitude);
        loc1.setLongitude(ptA.longitude);
        Location loc2 = new Location("l2");
        loc2.setLatitude(ptB.latitude);
        loc2.setLongitude(ptB.longitude);

        return (double) loc1.distanceTo(loc2);
    }

}
