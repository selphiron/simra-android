package de.tuberlin.mcc.simra.app.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.entities.SimraRoad;
import de.tuberlin.mcc.simra.app.util.SharedPref;

/**
 * Service Class containing methods to send requests to routing backend and parse responses.
 */
public class SimraNavService extends GraphHopperRoadManager {

    private static final String TAG = "SimraNavService_LOG";

    Context context;

    public SimraNavService(Context context) {
        super("false", false);
        this.setService(BuildConfig.NAVIGATION_ENDPOINT);
        this.context = context;
    }

    /**
     * Request a route from the backend.
     *
     * @param waypoints list of waypoints for which the route will be calculated
     * @return JSON response of the backend
     * @throws Exception generic exception (JSON parsing, network error)
     */
    public JSONObject fetchRoute(ArrayList<GeoPoint> waypoints) throws Exception {
        // fetch route
        URL navUrl = new URL(mServiceUrl + "routing/route");
        HttpURLConnection urlConnection = (HttpURLConnection) navUrl.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        String requestBodyString = getRequestBody(waypoints).toString();
        Log.d(TAG, "Sending request with body: " + requestBodyString);
        urlConnection.getOutputStream().write(requestBodyString.getBytes(StandardCharsets.UTF_8));
        // convert response to json object
        InputStream responseBody = urlConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseBody, StandardCharsets.UTF_8));
        StringBuilder responseStringBuilder = new StringBuilder();
        String responseString;
        while ((responseString = bufferedReader.readLine()) != null)
            responseStringBuilder.append(responseString);
        return new JSONObject(responseStringBuilder.toString());
    }

    /**
     * Returns the JSON request body for the routing request.
     *
     * @param waypoints list of waypoints for which the route will be calculated
     * @return JSON body
     * @throws JSONException error when constructing json
     */
    protected JSONObject getRequestBody(ArrayList<GeoPoint> waypoints) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("safety_weight", SharedPref.Settings.Navigation.getSafetyScoreWeighting(context));
        json.put("street_condition_weight", SharedPref.Settings.Navigation.getSurfaceQualityWeighting(context));
        json.put("use_simra_surface_quality", SharedPref.Settings.Navigation.getSimraSurfaceQualityEnabled(context));
        json.put("use_lit", SharedPref.Settings.Navigation.getUseLitStreets(context));
        json.put("avoid_accident_locations", SharedPref.Settings.Navigation.getAvoidRoadsWithAccidents(context));
        json.put("language", Locale.getDefault().getLanguage());
        JSONArray points = new JSONArray();
        for (GeoPoint point : waypoints) {
            JSONObject waypoint = new JSONObject();
            waypoint.put("lat", point.getLatitude());
            waypoint.put("lon", point.getLongitude());
            points.put(waypoint);
        }
        json.put("points", points);
        return json;
    }

    /**
     * Method for returning default road when request goes wrong.
     *
     * @param waypoints list of locations for route calculation
     * @return default road containing only selected locations (connected by straight line)
     */
    protected SimraRoad[] defaultRoad(ArrayList<GeoPoint> waypoints) {
        SimraRoad[] roads = new SimraRoad[1];
        roads[0] = new SimraRoad(waypoints);
        return roads;
    }


    /**
     * Return a list of calculated roads by the routing engine.
     *
     * @param waypoints    list of waypoints for which the route will be calculated
     * @param getAlternate whether alternate routes should be calculated
     * @return calculated road with safety and surface scores
     */
    @Override
    public SimraRoad[] getRoads(ArrayList<GeoPoint> waypoints, boolean getAlternate) {
        try {
            JSONObject jRoot = fetchRoute(waypoints);
            Log.d(TAG, "GraphHopper response: " + jRoot.toString());
            JSONArray jPaths = jRoot.optJSONArray("paths");
            if (jPaths == null || jPaths.length() == 0) {
                SimraRoad[] defaultRoad = this.defaultRoad(waypoints);
                defaultRoad[0].mStatus = STATUS_NO_ROUTE;
                return defaultRoad;
            }
            SimraRoad[] roads = new SimraRoad[jPaths.length()];
            // construct road paths
            for (int r = 0; r < jPaths.length(); r++) {
                JSONObject jPath = jPaths.getJSONObject(r);
                String route_geometry = jPath.getString("points");
                SimraRoad road = new SimraRoad();
                roads[r] = road;
                road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10, mWithElevation);
                JSONArray jInstructions = jPath.getJSONArray("instructions");
                int n = jInstructions.length();
                for (int i = 0; i < n; i++) {
                    JSONObject jInstruction = jInstructions.getJSONObject(i);
                    RoadNode node = new RoadNode();
                    JSONArray jInterval = jInstruction.getJSONArray("interval");
                    int positionIndex = jInterval.getInt(0);
                    node.mLocation = road.mRouteHigh.get(positionIndex);
                    node.mLength = jInstruction.getDouble("distance") / 1000.0;
                    node.mDuration = jInstruction.getInt("time") / 1000.0; //Segment duration in seconds.
                    int direction = jInstruction.getInt("sign");
                    node.mManeuverType = getManeuverCode(direction);
                    node.mInstructions = jInstruction.getString("text");
                    road.mNodes.add(node);
                }
                road.mLength = jPath.getDouble("distance") / 1000.0;
                road.mDuration = jPath.getInt("time") / 1000.0;
                JSONArray jBBox = jPath.getJSONArray("bbox");
                road.mBoundingBox = new BoundingBox(jBBox.getDouble(3), jBBox.getDouble(2),
                        jBBox.getDouble(1), jBBox.getDouble(0));
                road.mStatus = Road.STATUS_OK;
                road.buildLegs(waypoints);
                // add surface and safety score details to road
                JSONObject jDetails = jPath.getJSONObject("details");
                int segmentCount = road.mRouteHigh.size();
                road.safetyScoreSegmentValues = handleDetails(jDetails.getJSONArray("safety_score"), false, segmentCount);
                road.simraSurfaceQualitySegmentValues = handleDetails(jDetails.getJSONArray("simra_surface_quality"), false, segmentCount);
                road.osmSurfaceQualitySegmentValues = handleDetails(jDetails.getJSONArray("surface"), true, segmentCount);

                Log.d(BonusPackHelper.LOG_TAG, "GraphHopper.getRoads - finished");
            }
            return roads;
        } catch (Exception e) {
            e.printStackTrace();
            return defaultRoad(waypoints);
        }
    }

    /**
     * Method for converting route score details into integer array
     *
     * @param details    JSON array containing details
     * @param isSurface  whether the details are surface details
     * @param pathLength number of segments
     * @return integer array containing one score per road segment
     * @throws JSONException json parsing exception
     */
    private int[] handleDetails(JSONArray details, boolean isSurface, int pathLength) throws JSONException {
        int[] values = new int[pathLength - 1];
        for (int i = 0; i < details.length(); i++) {
            JSONArray detail = details.getJSONArray(i);
            // a detail entry is a 3-item array like such: [start, end, value]
            int startIndex = detail.getInt(0);
            int endIndex = detail.getInt(1);
            int value;
            if (isSurface) {
                String surfaceType = detail.getString(2);
                value = getSurfaceValue(surfaceType);
            } else {
                value = detail.getInt(2);
            }
            // add the given value to the values array using their respective start/end indices
            for (int j = startIndex; j < endIndex; j++) {
                // exit loop when we reach the end of the values array
                if (j == details.length())
                    break;
                values[j] = value;
            }
        }
        return values;
    }

    /**
     * returns a score value for a specific OSM surface type
     *
     * @param surfaceType the surface type
     * @return score ranging from 1 to 5
     */
    private int getSurfaceValue(String surfaceType) {
        switch (surfaceType) {
            case "paving_stones":
                return 2;
            case "fine_gravel":
            case "unpaved":
            case "compacted":
                return 3;
            case "cobblestone":
            case "gravel":
            case "ground":
                return 4;
            case "dirt":
            case "grass":
            case "sand":
            case "other":
                return 5;
            default:
                return 1;
        }
    }
}
