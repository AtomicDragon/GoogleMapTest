package Modules;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by Atomic on 1/19/2018.
 */

public interface RoadFinderListener {
    void onRoadFinderStart();
    void onRoadFinderSuccess(List<LatLng> route);
}
