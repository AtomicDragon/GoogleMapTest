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

    //TODO: Configure if more than 25 waypoints
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

        ArrayList<Route> routes = new ArrayList<>();
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
        for (int i = 0; i < jsonRoutes.length(); i++) {
            JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
            Route route = new Route();

            JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
            for (int j=0; j< jsonLegs.length(); j++) {
                JSONObject jsonLeg = jsonLegs.getJSONObject(j);
                if(j==0) {
                    JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");
                    route.setStartAddress(jsonLeg.getString("start_address"));
                    route.setStartLocation(new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng")));
                }
                JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
                JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
                JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
                JSONArray jsonSteps = jsonLeg.getJSONArray("steps");
                for(int k=0; k < jsonSteps.length(); k++)
                {
                    JSONObject jsonStep = jsonSteps.getJSONObject(k);
                    String htmlInstruction = jsonStep.getString("html_instructions");
                    String stepDistanceText = jsonStep.getJSONObject("distance").getString("text");
                    long stepDistanceValue = jsonStep.getJSONObject("distance").getLong("value");
                    String stepDurationText = jsonStep.getJSONObject("duration").getString("text");
                    long stepDurationValue = jsonStep.getJSONObject("duration").getLong("value");
                    LatLng stepStartLocation = new LatLng(jsonStep.getJSONObject("start_location").getDouble("lat"),jsonStep.getJSONObject("start_location").getDouble("lng"));
                    LatLng stepEndLocation = new LatLng(jsonStep.getJSONObject("end_location").getDouble("lat"),jsonStep.getJSONObject("end_location").getDouble("lng"));
                    Step step = new Step(htmlInstruction, stepStartLocation, stepEndLocation, stepDistanceText, stepDistanceValue, stepDurationText, stepDurationValue);
                    route.addStep(step);

                    JSONObject jsonPolyline = jsonStep.getJSONObject("polyline");
                    List<LatLng> points = decodePolyLine(jsonPolyline.getString("points"));
                    route.addPoints(points);
                    Log.d(TAG, points.size()+" points added");
                }

                route.setDistance(jsonDistance.getInt("value"));
                route.setDuration(jsonDuration.getInt("value"));
                route.setEndAddress(jsonLeg.getString("end_address"));
                route.setEndLocation(new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng")));
                Waypoint waypoint = new Waypoint(jsonLeg.getString("end_address"), new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng")));
                route.addWaypoints(waypoint);
            }

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
