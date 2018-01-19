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
 * Used to snap LatLng points to roads
 * Created by Atomic on 1/19/2018.
 */

public class RoadFinder {
    private static final String TAG = "RoadFinder";

    private static final String ROADS_URL_API = "https://roads.googleapis.com/v1/snapToRoads?";
    private static final String GOOGLE_API_KEY = "AIzaSyCCQF2XLyM9c1RVFW0MF-6AE20xPt3Fju8";

    private RoadFinderListener listener;
    private List<LatLng> list;

    public RoadFinder(RoadFinderListener listener, List<LatLng> list)
    {
        this.listener = listener;
        this.list = list;
    }

    public void execute() throws UnsupportedEncodingException {
        listener.onRoadFinderStart();
        new RoadFinder.DownloadRawData().execute(createRoadsUrl(list));
    }

    private String createRoadsUrl(List<LatLng> list) throws UnsupportedEncodingException {
        String points ="";
        for (int i= 0; i<100; i++)
        {
            LatLng point = list.get(i);
            points+=point.latitude+","+point.longitude;
            if(point != list.get(99))
            {
                points+="|";
            }
        }
        String url = ROADS_URL_API + "key=" + GOOGLE_API_KEY + "&path=" + points + "&interpolate=true" ;
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

        List<LatLng> points = new ArrayList<LatLng>();
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonSnappedPoints = jsonData.getJSONArray("snappedPoints");
        for (int i = 0; i < jsonSnappedPoints.length(); i++) {
            JSONObject point = jsonSnappedPoints.getJSONObject(i);
            JSONObject jsonLocation = point.getJSONObject("location");
            points.add(new LatLng(jsonLocation.getDouble("latitude"), jsonLocation.getDouble("longitude")));
        }

        listener.onRoadFinderSuccess(points);
    }
}
