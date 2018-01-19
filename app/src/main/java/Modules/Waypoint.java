package Modules;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Atomic on 1/19/2018.
 */

public class Waypoint {
    public String waypointAddress;
    public LatLng waypointLocation;

    public Waypoint(String s, LatLng l)
    {
        waypointAddress = s;
        waypointLocation = l;
    }
}
