package Modules;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mai Thanh Hiep on 4/3/2016.
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
