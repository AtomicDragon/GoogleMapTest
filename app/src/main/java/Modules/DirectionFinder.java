package Modules;

        import android.os.AsyncTask;
        import android.util.Log;

        import com.google.android.gms.maps.model.LatLng;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.UnsupportedEncodingException;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.net.URLEncoder;
        import java.util.ArrayList;
        import java.util.List;

/**
 * Creates directions from point A to point B with stops along the way.
 * Created by Atomic on 1/19/2018.
 */
public class DirectionFinder {
    private static final String TAG = "DirectionFinder";

    private static final String DIRECTION_URL_API = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyCCQF2XLyM9c1RVFW0MF-6AE20xPt3Fju8";
    private DirectionFinderListener listener;
    private String origin;
    private String destination;
    private String waypoints;

    public DirectionFinder(DirectionFinderListener listener, String origin, String destination, String waypoints) {
        this.listener = listener;
        this.origin = origin;
        this.destination = destination;
        this.waypoints = waypoints;
    }

    public void execute() throws UnsupportedEncodingException {
        listener.onDirectionFinderStart();
        new DownloadRawData().execute(createDirectionsUrl());
    }

    private String createDirectionsUrl() throws UnsupportedEncodingException {
        String urlOrigin = URLEncoder.encode(origin, "utf-8");
        String urlDestination = URLEncoder.encode(destination, "utf-8");
        String urlWaypoints = URLEncoder.encode(waypoints, "utf-8");

        String url = DIRECTION_URL_API + "origin=" + urlOrigin + "&destination=" + urlDestination +"&waypoints="+urlWaypoints+"&optimizeWaypoints=true" + "&key=" + GOOGLE_API_KEY;
        Log.d(TAG, url);
        return url;
    }


    private class DownloadRawData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                parseJSon(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseJSon(String data) throws JSONException {
        if (data == null)
            return;

        List<Route> routes = new ArrayList<Route>();
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
        for (int i = 0; i < jsonRoutes.length(); i++) {
            JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
            Route route = new Route();

            JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
            for (i=0; i< jsonLegs.length(); i++) {
                JSONObject jsonLeg = jsonLegs.getJSONObject(i);
                if(i==0) {
                    JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");
                    route.startAddress = jsonLeg.getString("start_address");
                    route.startLocation = new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng"));
                }
                JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
                JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
                JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");

                route.distance+= jsonDistance.getInt("value");
                route.duration+= jsonDuration.getInt("value");
                route.endAddress = jsonLeg.getString("end_address");
                route.endLocation = new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng"));
                Waypoint stop = new Waypoint(jsonLeg.getString("end_address"), new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng")));
                route.waypoints.add(stop);
            }
            route.points = decodePolyLine(overview_polylineJson.getString("points"));

            routes.add(route);
        }

        listener.onDirectionFinderSuccess(routes);
    }

    private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }

        return decoded;
    }

}
