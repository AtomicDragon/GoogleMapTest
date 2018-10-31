package Modules;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Used as a holder for routes used for directions
 * Created by Atomic on 1/19/2018.
 */
public class Route {
    private long totalDistance;
    private int totalDuration;
    private String endAddress;
    private LatLng endLocation;
    private String startAddress;
    private LatLng startLocation;
    private ArrayList<Waypoint> waypoints;
    private ArrayList<Step> steps;
    private ArrayList<LatLng> points;

    public Route()
    {
        this.totalDistance = 0;
        this.totalDuration = 0;
        this.endAddress = "";
        this.endLocation = new LatLng(0,0);
        this.startAddress = "";
        this.startLocation = new LatLng(0,0);
        this.steps = new ArrayList<Step>();
        this.waypoints = new ArrayList<Waypoint>();
        this.points = new ArrayList<LatLng>();
    }

    public void addPoints(List<LatLng> list)
    {
        points.addAll(list);
    }

    public void addWaypoints(Waypoint waypoint){waypoints.add(waypoint);}

    public void addStep(Step step){steps.add(step);}

    public long getDistance() {
        return totalDistance;
    }

    public void setDistance(long distance) {
        this.totalDistance = distance;
    }

    public int getDuration() {
        return totalDuration;
    }

    public void setDuration(int duration) {
        this.totalDuration = duration;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public LatLng getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(LatLng endLocation) {
        this.endLocation = endLocation;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public LatLng getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(LatLng startLocation) {
        this.startLocation = startLocation;
    }

    public ArrayList<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(ArrayList<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public ArrayList<Step> getSteps() {
        return steps;
    }

    public void setSteps(ArrayList<Step> steps) {
        this.steps = steps;
    }

    public ArrayList<LatLng> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }
}
