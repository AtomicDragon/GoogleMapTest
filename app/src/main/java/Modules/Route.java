package Modules;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Used as a holder for routes used for directions
 * Created by Atomic on 1/19/2018.
 */
public class Route {
    public long distance;
    public int duration;
    public String endAddress;
    public LatLng endLocation;
    public String startAddress;
    public LatLng startLocation;
    public List<Waypoint> waypoints;

    public List<LatLng> points;

    public Route()
    {
        distance = 0;
        duration = 0;
        endAddress = "";
        endLocation = new LatLng(0,0);
        startAddress = "";
        startLocation = new LatLng(0,0);

        waypoints = new ArrayList<Waypoint>();
    }
}
