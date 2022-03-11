package de.tuberlin.mcc.simra.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.adapter.AddressPair;
import de.tuberlin.mcc.simra.app.adapter.AutocompleteAdapter;
import de.tuberlin.mcc.simra.app.databinding.ActivityNavigationBinding;
import de.tuberlin.mcc.simra.app.services.SimraNavService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.Utils;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;

/**
 * Based on OSMNavigator MapActivity. License contained within application.
 * OSMBonusPack is used throughout this application, and OSMNavigator is also licensed under OSMBonuspack.
 * <a href="https://github.com/MKergall/osmbonuspack/blob/master/OSMNavigator/src/main/java/com/osmnavigator/MapActivity.java">MapActivity</a>
 */
public class NavigationActivity extends BaseActivity {

    private static final String TAG = "NavigationActivity_LOG";

    ActivityNavigationBinding binding;
    private GeocoderNominatim geocoderNominatim;

    private GeoPoint fromCoordinates, viaCoordinates, toCoordinates;

    private MapView mapView;
    private MapController mapController;

    private final int GEO_SEARCH = 10;

    public static Road[] mRoads;  //made static to pass between activities
    protected int mSelectedRoad;
    protected Polyline[] mRoadOverlays;
    protected FolderOverlay mRoadNodeMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set binding
        binding = ActivityNavigationBinding.inflate(LayoutInflater.from(this));

        setContentView(binding.getRoot());

        // action bar
        setSupportActionBar(binding.toolbar.toolbar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } catch (NullPointerException ignored) {
            Log.d(TAG, "NullPointerException");
        }
        binding.toolbar.toolbar.setTitle("");
        binding.toolbar.toolbar.setSubtitle("");
        binding.toolbar.toolbarTitle.setText(R.string.title_activity_navigation);
        binding.toolbar.backButton.setOnClickListener(v -> finish());

        // set geocoder
        geocoderNominatim = new GeocoderNominatim(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME);
        geocoderNominatim.setService("https://nominatim.openstreetmap.org/");

        // map config
        mapView = binding.map;
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setMultiTouchControls(true); // gesture zooming
        mapView.setFlingEnabled(true);
        mapView.setTilesScaledToDpi(true);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(ZOOM_LEVEL);

        // if opened from map selection, set data
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double lat = extras.getDouble("lat");
            double lon = extras.getDouble("lon");
            GeoPoint selectedPoint = new GeoPoint(lat, lon);
            try {
                String address = new GeocoderTask().execute(selectedPoint).get();
                boolean isStart = extras.getBoolean("isStart");
                if (isStart) {
                    binding.startLocation.setText(address);
                    fromCoordinates = selectedPoint;
                } else {
                    binding.destinationLocation.setText(address);
                    toCoordinates = selectedPoint;
                }
                updateSearchUiWithPoint(selectedPoint);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            SharedPreferences sharedPrefs = getSharedPreferences("simraPrefs", Context.MODE_PRIVATE);
            if (sharedPrefs.contains("lastLoc_latitude") & sharedPrefs.contains("lastLoc_longitude")) {
                GeoPoint lastLoc = new GeoPoint(Double.parseDouble(sharedPrefs.getString("lastLoc_latitude", "")),
                        Double.parseDouble(sharedPrefs.getString("lastLoc_longitude", "")));
                mapController.animateTo(lastLoc);
            }
        }

        // listener for fetching route button
        binding.startNavigationBtn.setOnClickListener(v -> {
            ArrayList<GeoPoint> pointList = new ArrayList<>();
            pointList.add(fromCoordinates);
            if (viaCoordinates != null)
                pointList.add(viaCoordinates);
            pointList.add(toCoordinates);
            new FetchRouteTask().execute(pointList);
        });

        // handlers for delayed geocoder fetching
        Handler startHandler = getAutocompleteHandler(binding.startLocation);
        Handler viaHandler = getAutocompleteHandler(binding.viaLocation);
        Handler toHandler = getAutocompleteHandler(binding.destinationLocation);

        // set up adapters and autocomplete views
        binding.startLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.startLocation.setOnItemClickListener((parent, view, position, id) -> {
            handleSuggestionClick(parent, position, 0);
        });
        binding.startLocation.addTextChangedListener(getAutocompleteTextWatcher(startHandler));

        binding.viaLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.viaLocation.setOnItemClickListener((parent, view, position, id) ->
                handleSuggestionClick(parent, position, 1));
        binding.viaLocation.addTextChangedListener(getAutocompleteTextWatcher(viaHandler));

        binding.destinationLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.destinationLocation.setOnItemClickListener((parent, view, position, id) ->
                handleSuggestionClick(parent, position, 2));
        binding.destinationLocation.addTextChangedListener(getAutocompleteTextWatcher(toHandler));

        // add road markers folder to map view
        mRoadNodeMarkers = new FolderOverlay();
        mRoadNodeMarkers.setName("Route Steps");
        mapView.getOverlays().add(mRoadNodeMarkers);
    }

    private void handleSuggestionClick(AdapterView<?> parent, int position, int fieldType) {
        Utils.hideKeyboard(this);
        AutocompleteAdapter adapter = (AutocompleteAdapter) parent.getAdapter();
        GeoPoint coordinates = Objects.requireNonNull(adapter.getItem(position)).getCoords();
        switch (fieldType) {
            case 0:
                fromCoordinates = coordinates;
                updateButtonEnabled();
                break;
            case 1:
                viaCoordinates = coordinates;
                break;
            case 2:
                toCoordinates = coordinates;
                updateButtonEnabled();
                break;
            default:
                break;
        }
        updateSearchUiWithPoint(coordinates);
    }

    private Handler getAutocompleteHandler(AutoCompleteTextView textView) {
        return new Handler(msg -> {
            if (msg.what == GEO_SEARCH && !TextUtils.isEmpty(textView.getText()))
                getAddresses(textView.getText().toString(), textView);
            return false;
        });
    }

    private void updateSearchUiWithPoint(GeoPoint point) {
        mapView.getOverlays().clear();
        List<GeoPoint> pointList = new ArrayList<>();
        if (fromCoordinates != null) {
            pointList.add(fromCoordinates);
            addMarker(fromCoordinates);
        }
        if (viaCoordinates != null) {
            pointList.add(viaCoordinates);
            addMarker(viaCoordinates);
        }
        if (toCoordinates != null) {
            pointList.add(toCoordinates);
            addMarker(toCoordinates);
        }
        if (pointList.size() > 1) {
            BoundingBox boundingBox = BoundingBox.fromGeoPointsSafe(pointList);
            mapView.zoomToBoundingBox(boundingBox, true);
        } else {
            mapController.animateTo(point);
        }
    }

    private void addMarker(GeoPoint point) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        setMarkerIcon(marker, R.drawable.ic_marker, R.color.distanceMarkerDanger);
        mapView.getOverlays().add(marker);
    }

    private TextWatcher getAutocompleteTextWatcher(Handler handler) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeMessages(GEO_SEARCH);
                handler.sendEmptyMessageDelayed(GEO_SEARCH, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    private void updateButtonEnabled() {
        binding.startNavigationBtn.setEnabled(fromCoordinates != null && toCoordinates != null);
    }

    private void getAddresses(String searchEntry, AutoCompleteTextView textView) {
        try {
            List<Address> results = new ReverseGeocoderTask().execute(searchEntry).get();
            List<AddressPair> resultPairs = new ArrayList<>();
            for (Address adr : results) {
                resultPairs.add(new AddressPair(new GeoPoint(adr.getLatitude(), adr.getLongitude()), addressToString(adr)));
            }
            AutocompleteAdapter adapter = (AutocompleteAdapter) textView.getAdapter();
            adapter.setSuggestions(resultPairs);
            adapter.notifyDataSetChanged();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class GeocoderTask extends AsyncTask<GeoPoint, Void, String> {
        @Override
        protected String doInBackground(GeoPoint... geoPoints) {
            List<Address> addresses;
            String addressForLocation = "";
            GeoPoint p = geoPoints[0];
            try {
                addresses = geocoderNominatim.getFromLocation(p.getLatitude(), p.getLongitude(), 1);
                if (addresses.size() == 0) {
                    Log.d(TAG, "getAddressFromLocation(): Couldn't find an address for input " + p);
                    // return coordinates if no address found
                    return p.toString();
                } else {
                    // Get address result from geocoding result
                    addressForLocation = addressToString(addresses.get(0));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return addressForLocation;
        }
    }

    private class ReverseGeocoderTask extends AsyncTask<String, Void, List<Address>> {
        @Override
        protected List<Address> doInBackground(String... strings) {
            String query = strings[0];
            try {
                return geocoderNominatim.getFromLocationName(query, 3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
    }

    private class FetchRouteTask extends AsyncTask<ArrayList<GeoPoint>, Void, Road[]> {
        SimraNavService navService;

        @Override
        protected Road[] doInBackground(ArrayList<GeoPoint>... arrayLists) {
            ArrayList<GeoPoint> routePoints = arrayLists[0];
            navService = new SimraNavService(NavigationActivity.this);
            return navService.getRoads(routePoints);
        }

        @Override
        protected void onPostExecute(Road[] roads) {
            mRoads = roads;
            updateUIWithRoads(roads);
        }

    }

    private void updateUIWithRoads(Road[] roads) {
        Log.d(TAG, "road count: " + roads.length);
        mRoadNodeMarkers.getItems().clear();
        List<Overlay> mapOverlays = mapView.getOverlays();
        if (mRoadOverlays != null) {
            for (Polyline mRoadOverlay : mRoadOverlays) mapOverlays.remove(mRoadOverlay);
            mRoadOverlays = null;
        }
        if (roads == null)
            return;
        if (roads[0].mStatus == Road.STATUS_TECHNICAL_ISSUE)
            Toast.makeText(mapView.getContext(), "Technical issue when getting the route", Toast.LENGTH_SHORT).show();
        else if (roads[0].mStatus > Road.STATUS_TECHNICAL_ISSUE) //functional issues
            Toast.makeText(mapView.getContext(), "No possible route here", Toast.LENGTH_SHORT).show();
        mRoadOverlays = new Polyline[roads.length];
        for (int i = 0; i < roads.length; i++) {
            Polyline roadPolyline = RoadManager.buildRoadOverlay(roads[i]);
            mRoadOverlays[i] = roadPolyline;
            Paint p = roadPolyline.getPaint();
            p.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
            String routeDesc = roads[i].getLengthDurationText(this, -1);
            roadPolyline.setTitle("Route" + " - " + routeDesc);
            roadPolyline.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView));
            roadPolyline.setRelatedObject(i);
            mapOverlays.add(1, roadPolyline);
            //we insert the road overlays at the "bottom", just above the MapEventsOverlay,
            //to avoid covering the other overlays.
        }
        selectRoad(0);
    }

    void selectRoad(int roadIndex) {
        mSelectedRoad = roadIndex;
        putRoadNodes(mRoads[roadIndex]);
        for (int i = 0; i < mRoadOverlays.length; i++) {
            Paint p = mRoadOverlays[i].getPaint();
            if (i == roadIndex)
                p.setColor(0x800000FF); //blue
            else
                p.setColor(0x90666666); //grey
        }
        mapView.invalidate();
    }

    private void setMarkerIcon(Marker marker, int iconResource, int color) {
        Drawable icon = ResourcesCompat.getDrawable(getResources(), iconResource, null);
        // set icon tint
        assert icon != null;
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(icon, getColor(color));
        marker.setIcon(icon);
    }

    private void putRoadNodes(Road road) {
        mRoadNodeMarkers.getItems().clear();
        int n = road.mNodes.size();
        MarkerInfoWindow infoWindow = new MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
        TypedArray iconIds = getResources().obtainTypedArray(R.array.direction_icons);
        for (int i = 0; i < n; i++) {
            RoadNode node = road.mNodes.get(i);
            String instructions = (node.mInstructions == null ? "" : node.mInstructions);
            Marker nodeMarker = new Marker(mapView);
            nodeMarker.setTitle(getString(R.string.step) + " " + (i + 1));
            nodeMarker.setSnippet(instructions);
            nodeMarker.setSubDescription(Road.getLengthDurationText(this, node.mLength, node.mDuration));
            nodeMarker.setPosition(node.mLocation);
            setMarkerIcon(nodeMarker, R.drawable.ic_circle, R.color.colorPrimaryDark);
            nodeMarker.setInfoWindow(infoWindow); //use a shared info window.
            int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
            if (iconId != R.drawable.ic_empty) {
                Drawable image = ResourcesCompat.getDrawable(getResources(), iconId, null);
                nodeMarker.setImage(image);
            }
            nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mRoadNodeMarkers.add(nodeMarker);
            mapView.getOverlays().add(mRoadNodeMarkers);
        }
        iconIds.recycle();
    }

    private String addressToString(Address address) {
        StringBuilder addressBuilder = new StringBuilder();
        int maxLines = address.getMaxAddressLineIndex();
        for (int i = 0; i <= maxLines; i++) {
            if (i != 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(address.getAddressLine(i));
        }
        return addressBuilder.toString();
    }
}