package blackdragonenterprises.googlemaptest;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.data.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import Modules.CustomMapTileProvider;
import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;
import Modules.Step;
import Modules.Waypoint;

/**
 * Makes a map and allows you to plot a course from point A to B.
 * Created by Atomic on 1/19/2018.
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback , DirectionFinderListener{

    private static final String TAG = "MapsActivity";

    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    private static final int DEFAULT_ZOOM = 15;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private static final long FASTEST_INTERVAL = 2000; /* 2 sec */
    private static final int NEXT_STEP_TRIGGER_DISTANCE = 40; /* 25 meters */
    private final LatLng mDefaultLocation = new LatLng(41.745037, -71.297715);

    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    private Button btnFindPath;
    private TextView txtDirections;
    private TextView txtNextStop;
    private TextView txtDistance;
    private TextView txtDuration;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Marker> waypointMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private LocationCallback mLocationCallback;

    private Route myRoute;
    private ArrayList<Step> mySteps;
    private ArrayList<Waypoint> myWaypoints;
    private LatLng nextStepLocation;
    private LatLng nextWaypointLocation;
    private Location previousLocation;
    private int stepNumber;
    private int waypointNumber;
    private long nextStopDistance;
    private long nextStopDuration;
    private long updateTime;

    /**
     * APPLICATION METHODS
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Reload saved instance
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnFindPath = findViewById(R.id.btnFindPath);
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequest();
            }
        });

        txtDirections = findViewById(R.id.directions_text_view);
        txtNextStop = findViewById(R.id.next_stop_text_view);
        txtDuration = findViewById(R.id.tvDuration);
        txtDistance =  findViewById(R.id.tvDistance);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mLocationPermissionGranted) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationPermissionGranted) {
            startLocationUpdates();
        }
    }

    /**
     * MAPPING METHODS
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        updateMapUi();
        addKMLLayer();
        //getLocationPermission();
        //getDeviceLocation();
        //startLocationUpdates();
        addOfflineTilesOverlay();
    }

    private void updateMapUi() {
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
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void addKMLLayer() {
        try {
            //Get file from storage
            File kmlFile = new File(Environment.getExternalStorageDirectory()+"/Android/data/blackdragonenterprises.googlemaptest/kml_file.kml");
            FileInputStream kmlInputStream= new FileInputStream(kmlFile);
            KmlLayer layer = new KmlLayer(mMap, kmlInputStream, getApplicationContext());
            //KmlLayer layer = new KmlLayer(mMap, R.raw.kml_file, getApplicationContext()); //Hard coded

            layer.addLayerToMap();
        }
        catch (IOException e) {
            Log.d(TAG, e.toString());}
        catch (XmlPullParserException e) {Log.d(TAG, e.toString());}
    }

    private void addOfflineTilesOverlay() {
        if(!isNetworkAvailable()) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NONE);

            //Get file path to Tiles
            File path = new File(Environment.getExternalStorageDirectory() + "/Android/data/blackdragonenterprises.googlemaptest");
            //Add map overlay using our custom tile provider
            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(path)));

            //Restrict map to bounds area (Boston)
            //TODO: Smart bounds based on what tiles you have?
            //mMap.setMinZoomPreference(9);
            mMap.setMaxZoomPreference(18);
            mMap.setLatLngBoundsForCameraTarget(new LatLngBounds(new LatLng(42.309942,-71.197414), new LatLng(42.423076,-70.971508)));

            CameraUpdate upd = CameraUpdateFactory.newLatLngZoom(new LatLng(42.357149, -71.049786), 5);
            mMap.moveCamera(upd);
        }

    }

    /**
     * Direction Services
     */

    private void sendRequest() {
        if (mLastKnownLocation != null)
        {
            //Close keyboard
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            String origin = mLastKnownLocation.getLatitude() + "," + mLastKnownLocation.getLongitude();
            String destination = "ProvinceTown Massachusetts";

            //Read waypoints from res file.
            InputStream inputStream = getResources().openRawResource(R.raw.waypoints);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            try {
                String eachline = bufferedReader.readLine();
                while (eachline != null) {
                    sb.append(eachline);
                    eachline = bufferedReader.readLine();
                    if (eachline != null) {
                        sb.append("|");
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, e.toString());
            }
            String waypoints = sb.toString();

            //Find route
            try {
                new DirectionFinder(this, origin, destination, waypoints).execute();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else
        {
            showErrorMessage("Your location is not available");
            getDeviceLocation();
        }
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
    public void onDirectionFinderSuccess(ArrayList<Route> routes) {
        progressDialog.dismiss();
        if(!routes.isEmpty())
        {
            polylinePaths = new ArrayList<>();
            originMarkers = new ArrayList<>();
            destinationMarkers = new ArrayList<>();
            waypointMarkers = new ArrayList<>();

            for (Route route : routes) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.getStartLocation(), 14));
                ((TextView) findViewById(R.id.tvDuration)).setText(convertDuration(route.getDuration()));
                ((TextView) findViewById(R.id.tvDistance)).setText(convertDistance(route.getDistance()));

                originMarkers.add(mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue))
                        .title(route.getStartAddress())
                        .position(route.getStartLocation())));
                destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green))
                        .title(route.getEndAddress())
                        .position(route.getEndLocation())));
                for (Waypoint waypoint : route.getWaypoints()) {
                    if (!waypoint.getWaypointAddress().equals(route.getEndAddress())) {
                        waypointMarkers.add(mMap.addMarker(new MarkerOptions()
                                .title(waypoint.getWaypointAddress())
                                .position(waypoint.getWaypointLocation())));
                    }
                }

                PolylineOptions polylineOptions = new PolylineOptions().
                        geodesic(true).
                        color(Color.BLUE).
                        width(10);

                for (int i = 0; i < route.getPoints().size(); i++)
                {
                    polylineOptions.add(route.getPoints().get(i));
                }
                polylinePaths.add(mMap.addPolyline(polylineOptions));


            }
            //Initialize for first step
            myRoute = routes.get(0);
            mySteps = myRoute.getSteps();
            myWaypoints = myRoute.getWaypoints();
            stepNumber = 0;
            waypointNumber =0;
            updateTime = System.currentTimeMillis();
            previousLocation = mLastKnownLocation;
            updateNextStep();
            updateNextWaypoint();

        }
        else
        {
            Log.d(TAG, "Could not find a route to the given destination");
        }

    }

    private void onLocationChanged(Location location)
    {
        String msg = "Updated Location: " + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude());
        Log.d(TAG, msg);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));

        if (nextStepLocation !=null)
        {
            //Check to see if you have reached the next location, and if you have go to next step.
            float results[] = new float[10];
            Location.distanceBetween(location.getLatitude(),location.getLongitude(), nextStepLocation.latitude, nextStepLocation.longitude, results);
            if(results[0] <= NEXT_STEP_TRIGGER_DISTANCE)
            {
                float waypointResults[] = new float[10];
                Location.distanceBetween(location.getLatitude(),location.getLongitude(), nextWaypointLocation.latitude, nextWaypointLocation.longitude, waypointResults);
                if(waypointResults[0] <= NEXT_STEP_TRIGGER_DISTANCE)
                {
                    waypointNumber++;
                    updateNextWaypoint();
                }
                stepNumber++;
                updateNextStep();
            }
            //If not, update distance to location
            else
            {
                nextStopDistance = (long)results[0];
                if (location.getSpeed() > 5) {
                    nextStopDuration = nextStopDuration - nextStopDistance / (long)location.getSpeed();
                }
                txtDistance.setText(convertDistance(nextStopDistance));
                txtDuration.setText(convertDuration(nextStopDuration));
                previousLocation = location;
            }
            updateTime = System.currentTimeMillis();
        }
    }

    private void updateNextWaypoint() {
        nextWaypointLocation = myWaypoints.get(waypointNumber).getWaypointLocation();
        String nextText = "Next Stop: "+myWaypoints.get(waypointNumber).getWaypointAddress();
        txtNextStop.setText(nextText);
    }

    private void updateNextStep()
    {
        if(mySteps.size() > stepNumber)
        {
            nextStepLocation = mySteps.get(stepNumber).getEndLocation();
            txtDirections.setText(Html.fromHtml(mySteps.get(stepNumber+1).getHtmlInstruction()));
            nextStopDistance = mySteps.get(stepNumber).getDistanceValue();
            nextStopDuration= mySteps.get(stepNumber).getDurationValue();
            txtDistance.setText(convertDistance(nextStopDistance));
            txtDuration.setText(convertDuration(nextStopDuration));
        }
    }

    private String convertDistance(long d)
    {
        if (d > 1609.34/4)
        {
            double dist = Math.round((d/1609.34)*100.0)/100.0;
            return dist +" mi";
        }
        else
        {
            double dist = Math.round(d*3.28);
            return dist+" ft";
        }
    }

    private String convertDuration(long totalSeconds)
    {
        long days = TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) - (days *24);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) - (TimeUnit.SECONDS.toHours(totalSeconds)* 60);

        String output = "";
        if (days!=0){output+= days + " days ";}
        if (hours!=0){output+= hours + " hours ";}
        if (minutes!=0 && days ==0){output+= minutes + " min";}
        return output;
    }

    /**
    * LOCATION SERVICES
    */

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

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void showErrorMessage(String message){
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .create()
                .show();
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
        updateMapUi();
    }

    private void getDeviceLocation() {
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
                    }
                    else {
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

    private void startLocationUpdates()
    {
        try
        {
            if(mLocationPermissionGranted)
            {
                LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(UPDATE_INTERVAL);
                mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        onLocationChanged(locationResult.getLastLocation());
                    }
                };
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            }
        }
        catch(SecurityException e)
        {
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

    private boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }
}


