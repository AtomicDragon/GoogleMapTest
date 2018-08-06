package blackdragonenterprises.googlemaptest;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlPlacemark;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.RoadFinder;
import Modules.RoadFinderListener;
import Modules.Route;
import Modules.Waypoint;

/**
 * Makes a map and allows you to plot a course from point A to B.
 * Created by Atomic on 1/19/2018.
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback , DirectionFinderListener, RoadFinderListener{

    private static final String TAG = "MapsActivity";

    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    private static final int DEFAULT_ZOOM = 15;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private FusedLocationProviderClient mFusedLocationClient;
    private Button btnFindPath;
    private PlaceAutocompleteFragment etOriginFragment;
    private PlaceAutocompleteFragment etDestinationFragment;
    private Place etOrigin;
    private Place etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Marker> waypointMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    private boolean mLocationPermissionGranted;
    private static final boolean snapToRoads = false;
    private final LatLng mDefaultLocation = new LatLng(41.745037, -71.297715);
    private Location mLastKnownLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnFindPath = (Button) findViewById(R.id.btnFindPath);

//        etOriginFragment = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.etOrigin);
//        etOriginFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//                etOrigin = place;
//            }
//
//            @Override
//            public void onError(Status status) {
//
//            }
//        });
        etDestinationFragment = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.etDestination);
        etDestinationFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                etDestination = place;
            }

            @Override
            public void onError(Status status) {

            }
        });
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequest();
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        /*mMap.addMarker(new MarkerOptions()
                .title("Kent Street Pond")
                .position(new LatLng(41.753482, -71.297867)));*/

        try {
            //Get file from storage
//            File kmlFile = new File(Environment.getExternalStorageDirectory()+"/Android/data/blackdragonenterprises.googlemaptest/kml_file.kml");
//            FileInputStream kmlInputStream= new FileInputStream(kmlFile);
//            KmlLayer layer = new KmlLayer(mMap, kmlInputStream, getApplicationContext());
            //KmlLayer layer = new KmlLayer(mMap, R.raw.kml_file_edited, getApplicationContext()); //Hard coded
            KmlLayer layer = new KmlLayer(mMap, R.raw.pchtouralexevie, getApplicationContext()); //Hard coded

            layer.addLayerToMap();

            for(com.google.maps.android.data.kml.KmlContainer container : layer.getContainers()){
                for(com.google.maps.android.data.kml.KmlContainer nestedContainer : container.getContainers()){
                    for(KmlPlacemark placemark : nestedContainer.getPlacemarks()){
                        Log.i("MTAG", placemark.getStyleId());
                    }
                }
            }
        }
        catch (IOException e) {Log.d(TAG, e.toString());}
        catch (XmlPullParserException e) {Log.d(TAG, e.toString());}

        getLocationPermission();

        updateLocationUI();

        getDeviceLocation();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    private void sendRequest(){
        //Close keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        String origin = "";
        String destination ="";
        //Pull origin and destination data
//        String origin = etOrigin.getAddress().toString();
//        if(origin.toLowerCase().equals("current location"))
//        {
//            origin = mLastKnownLocation.getLatitude()+","+mLastKnownLocation.getLongitude();
//        }
        if (mLastKnownLocation != null) {
            origin = mLastKnownLocation.getLatitude()+","+mLastKnownLocation.getLongitude();
        }
        if (etDestination != null) {
            destination = etDestination.getAddress().toString();
        }
        //String waypoints = "32 Mallard Cove Barrington RI|Sowams School Barrington RI 02806|41 Linden Road Barrington RI";
        String waypoints = "";

        if(origin.isEmpty())
        {
            Toast.makeText(this, "Please enter origin address", Toast.LENGTH_SHORT).show();
            return;
        }
        if(destination.isEmpty())
        {
            Toast.makeText(this, "Please enter destination address", Toast.LENGTH_SHORT).show();
            return;
        }

        //Find route
        try {
            new DirectionFinder(this, origin, destination, waypoints).execute();
        }
        catch (UnsupportedEncodingException e)
        {e.printStackTrace();}
    }

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding path to destination", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }
        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }
        if (waypointMarkers != null) {
            for (Marker marker : destinationMarkers)
            {
                marker.remove();
            }
        }
        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();
        waypointMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 14));
            ((TextView) findViewById(R.id.tvDuration)).setText(calculateDuration(route.duration));
            ((TextView) findViewById(R.id.tvDistance)).setText(calculateDistance(route.distance));

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                    .title(route.endAddress)
                    .position(route.endLocation)));
            for (Waypoint waypoint : route.waypoints) {
                if (!waypoint.waypointAddress.equals(route.endAddress)) {
                    waypointMarkers.add(mMap.addMarker(new MarkerOptions()
                            .title(waypoint.waypointAddress)
                            .position(waypoint.waypointLocation)));
                }
            }

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));

            //Initialize Snap to Roads after finding Directions
            if(snapToRoads)
            {
                try {
                    new RoadFinder(this, route.points).execute();
                }
                catch (UnsupportedEncodingException e)
                {e.printStackTrace();}
            }
        }
    }

    @Override
    public void onRoadFinderStart()
    {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Snapping to roads", true);

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onRoadFinderSuccess(List<LatLng> points)
    {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        PolylineOptions polylineOptions = new PolylineOptions().
                geodesic(true).
                color(Color.BLUE).
                width(10);

        for (int i = 0; i < points.size(); i++)
            polylineOptions.add(points.get(i));

        polylinePaths.add(mMap.addPolyline(polylineOptions));
    }

    private String calculateDistance(long d)
    {
        double dist = Math.round((d/1609.34)*100.0)/100.0 ;
        return dist +" mi";
    }

    private String calculateDuration(int totalSeconds)
    {
        int days = totalSeconds / 86400;
        int hours = (totalSeconds - days * 86400) / 3600;
        int minutes = (totalSeconds - days * 86400 - hours * 3600) / 60;

        String output = "";
        if (days!=0){output+= days + " days ";}
        if (hours!=0){output+= hours + " hours ";}
        if (minutes!=0 && days ==0){output+= minutes + " min";}
        return output;
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                        if (location != null) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = location;
                            if(mCameraPosition != null){
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCameraPosition.target, mCameraPosition.zoom));
                            }
                            else {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    //TODO Style Polylines
    private void stylePolyline(Polyline polyline) {
        String type = "";
        // Get the data object stored with the polyline.
        if (polyline.getTag() != null) {
            type = polyline.getTag().toString();
        }

        switch (type) {
            // If no type is given, allow the API to use the default.
            case "A":
                // Use a custom bitmap as the cap at the start of the line.
                polyline.setStartCap(
                        new CustomCap(
                                BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 10));
                break;
            case "B":
                // Use a round cap at the start of the line.
                polyline.setStartCap(new RoundCap());
                break;
        }

        polyline.setEndCap(new RoundCap());
        polyline.setWidth(POLYLINE_STROKE_WIDTH_PX);
        polyline.setColor(COLOR_BLACK_ARGB);
        polyline.setJointType(JointType.ROUND);
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                showMessageOKCancel("You need to allow access to Location",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                    });
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
        else
        {
            mLocationPermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
            // other 'case' lines to check for other permissions this app might request.
        }
        updateLocationUI();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}
