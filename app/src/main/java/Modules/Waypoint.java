package Modules;

import com.google.android.gms.maps.model.LatLng;

/**
 * Used to hold both the address and location of a waypoint
 * Created by Atomic on 1/19/2018.
 */

public class Waypoint {



    private String waypointAddress;
    private LatLng waypointLocation;

    public Waypoint(String s, LatLng l)
    {
        waypointAddress = s;
        waypointLocation = l;
    }

    public String getWaypointAddress() {
        return waypointAddress;
    }

    public void setWaypointAddress(String waypointAddress) {
        this.waypointAddress = waypointAddress;
    }

    public LatLng getWaypointLocation() {
        return waypointLocation;
    }

    public void setWaypointLocation(LatLng waypointLocation) {
        this.waypointLocation = waypointLocation;
    }
}
