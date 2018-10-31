package Modules;

import com.google.android.gms.maps.model.LatLng;

/**
 * Used as a holder for steps of routes used for directions
 * Created by Atomic on 8/13/2018.
 */

public class Step {

    private String htmlInstruction;
    private LatLng startLocation;
    private LatLng endLocation;
    private String distanceText;
    private long distanceValue;
    private String durationText;
    private long durationValue;

    public Step(String htmlInstruction, LatLng startLocation, LatLng endLocation, String distanceText, long distanceValue, String durationText, long durationValue) {
        this.htmlInstruction = htmlInstruction;
        this.endLocation = endLocation;
        this.startLocation = startLocation;
        this.distanceText = distanceText;
        this.distanceValue = distanceValue;
        this.durationText = durationText;
        this.durationValue = durationValue;
    }

    public String getHtmlInstruction() {
        return htmlInstruction;
    }

    public void setHtmlInstruction(String htmlInstruction) {
        this.htmlInstruction = htmlInstruction;
    }

    public LatLng getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(LatLng endLocation) {
        this.endLocation = endLocation;
    }

    public LatLng getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(LatLng startLocation) {
        this.startLocation = startLocation;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    public long getDistanceValue() {
        return distanceValue;
    }

    public void setDistanceValue(long distanceValue) {
        this.distanceValue = distanceValue;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public long getDurationValue() {
        return durationValue;
    }

    public void setDurationValue(long durationValue) {
        this.durationValue = durationValue;
    }


}
