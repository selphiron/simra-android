package de.tuberlin.mcc.simra.app.activities;

import static de.tuberlin.mcc.simra.app.util.Constants.ZOOM_LEVEL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.adapter.AutocompleteAdapter;
import de.tuberlin.mcc.simra.app.databinding.ActivityNavigationBinding;
import de.tuberlin.mcc.simra.app.entities.AddressPair;
import de.tuberlin.mcc.simra.app.entities.ScoreColorList;
import de.tuberlin.mcc.simra.app.entities.SimraRoad;
import de.tuberlin.mcc.simra.app.services.SimraNavService;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import de.tuberlin.mcc.simra.app.util.RouteUtils;
import de.tuberlin.mcc.simra.app.util.SharedPref;
import de.tuberlin.mcc.simra.app.util.Utils;

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

    public static SimraRoad[] mRoads;  //made static to pass between activities
    protected Polyline[] mRoadOverlays;
    protected FolderOverlay mRoadNodeMarkers;
    private RouteUtils routeUtils;

    public enum PointType {START, VIA, END}

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

        // init route utils
        routeUtils = new RouteUtils(mapView, mRoads, mRoadNodeMarkers, this, mRoadOverlays);

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
                updateSearchUiWithPoint(selectedPoint, address);
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
        binding.getRouteBtn.setOnClickListener(v -> {
            ArrayList<GeoPoint> pointList = new ArrayList<>();
            pointList.add(fromCoordinates);
            if (viaCoordinates != null)
                pointList.add(viaCoordinates);
            pointList.add(toCoordinates);
            new FetchRouteTask().execute(pointList);
        });

        // listener for route appearance selection button
        binding.routeVisualizerSelector.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.option_none) {
                    routeUtils.selectRoad(ScoreColorList.ScoreType.NONE);
                    return true;
                } else if (itemId == R.id.option_safety_score) {
                    routeUtils.selectRoad(ScoreColorList.ScoreType.SAFETY);
                    return true;
                } else if (itemId == R.id.option_surface_quality) {
                    boolean simraEnabled = SharedPref.Settings.Navigation.getSimraSurfaceQualityEnabled(this);
                    ScoreColorList.ScoreType surfaceType = ScoreColorList.ScoreType.SURFACE_OSM;
                    if (simraEnabled)
                        surfaceType = ScoreColorList.ScoreType.SURFACE_SIMRA;
                    routeUtils.selectRoad(surfaceType);
                    return true;
                } else return false;
            });
            popup.inflate(R.menu.menu_route_visualizer_options);
            popup.show();
        });

        // listener for route cancel button
        binding.cancelButton.setOnClickListener(v -> toggleButtons(false));

        // listener for starting navigation
        binding.startNavigationBtn.setOnClickListener(v -> {
            setResult(Activity.RESULT_OK, new Intent().putExtra("roads", mRoads));
            finish();
        });

        // handlers for delayed geocoder fetching
        Handler startHandler = getAutocompleteHandler(binding.startLocation);
        Handler viaHandler = getAutocompleteHandler(binding.viaLocation);
        Handler toHandler = getAutocompleteHandler(binding.destinationLocation);

        // set up adapters and autocomplete views
        binding.startLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.startLocation.setOnItemClickListener((parent, view, position, id) -> {
            handleSuggestionClick(parent, position, PointType.START);
        });
        binding.startLocation.addTextChangedListener(getAutocompleteTextWatcher(startHandler));

        binding.viaLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.viaLocation.setOnItemClickListener((parent, view, position, id) ->
                handleSuggestionClick(parent, position, PointType.VIA));
        binding.viaLocation.addTextChangedListener(getAutocompleteTextWatcher(viaHandler));

        binding.destinationLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.destinationLocation.setOnItemClickListener((parent, view, position, id) ->
                handleSuggestionClick(parent, position, PointType.END));
        binding.destinationLocation.addTextChangedListener(getAutocompleteTextWatcher(toHandler));

        // add road markers folder to map view
        mRoadNodeMarkers = new FolderOverlay();
        mRoadNodeMarkers.setName("Route Steps");
        mapView.getOverlays().add(mRoadNodeMarkers);
    }

    private void handleSuggestionClick(AdapterView<?> parent, int position, PointType pointType) {
        Utils.hideKeyboard(this);
        AutocompleteAdapter adapter = (AutocompleteAdapter) parent.getAdapter();
        AddressPair item = adapter.getItem(position);
        GeoPoint coordinates = Objects.requireNonNull(item).getCoords();
        switch (pointType) {
            case START:
                fromCoordinates = coordinates;
                updateButtonEnabled();
                break;
            case END:
                toCoordinates = coordinates;
                updateButtonEnabled();
                break;
            default:
                viaCoordinates = coordinates;
                break;
        }
        updateSearchUiWithPoint(coordinates, item.toString());
    }

    private Handler getAutocompleteHandler(AutoCompleteTextView textView) {
        return new Handler(msg -> {
            if (msg.what == GEO_SEARCH && !TextUtils.isEmpty(textView.getText()))
                getAddresses(textView.getText().toString(), textView);
            return false;
        });
    }

    private void updateSearchUiWithPoint(GeoPoint point, String address) {
        mapView.getOverlays().clear();
        List<GeoPoint> pointList = new ArrayList<>();
        if (fromCoordinates != null) {
            pointList.add(fromCoordinates);
            routeUtils.addMarker(fromCoordinates, PointType.START, address);
        }
        if (viaCoordinates != null) {
            pointList.add(viaCoordinates);
            routeUtils.addMarker(viaCoordinates, PointType.VIA, address);
        }
        if (toCoordinates != null) {
            pointList.add(toCoordinates);
            routeUtils.addMarker(toCoordinates, PointType.END, address);
        }
        if (pointList.size() > 1) {
            BoundingBox boundingBox = BoundingBox.fromGeoPointsSafe(pointList);
            mapView.zoomToBoundingBox(boundingBox, true);
        } else {
            mapController.animateTo(point);
        }
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
        binding.getRouteBtn.setEnabled(fromCoordinates != null && toCoordinates != null);
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

    private class FetchRouteTask extends AsyncTask<ArrayList<GeoPoint>, Void, SimraRoad[]> {
        SimraNavService navService;

        @Override
        protected SimraRoad[] doInBackground(ArrayList<GeoPoint>... arrayLists) {
            ArrayList<GeoPoint> routePoints = arrayLists[0];
            navService = new SimraNavService(NavigationActivity.this);
            return navService.getRoads(routePoints, false);
        }

        @Override
        protected void onPostExecute(SimraRoad[] roads) {
            mRoads = roads;
            // exit prematurely if error occurs
            if (roads[0].mStatus >= Road.STATUS_TECHNICAL_ISSUE || roads[0].mStatus == Road.STATUS_INVALID) {
                Toast.makeText(mapView.getContext(), getString(R.string.route_error), Toast.LENGTH_SHORT).show();
            } else {
                String[] durationTexts = routeUtils.updateUIWithRoads(roads);
                binding.durationText.setText(durationTexts[0]);
                toggleButtons(true);
            }
        }
    }

    private void toggleButtons(boolean routeFetched) {
        if (routeFetched) {
            binding.getRouteBtn.setVisibility(View.INVISIBLE);
            binding.startNavigationBtn.setVisibility(View.VISIBLE);
            binding.cancelButton.setVisibility(View.VISIBLE);
            binding.routeVisualizerSelector.setVisibility(View.VISIBLE);
        } else {
            binding.getRouteBtn.setVisibility(View.VISIBLE);
            binding.startNavigationBtn.setVisibility(View.GONE);
            binding.cancelButton.setVisibility(View.GONE);
            binding.routeVisualizerSelector.setVisibility(View.GONE);
            // reset route information values
            binding.durationText.setText(null);
            mRoadNodeMarkers.getItems().clear();
            List<Overlay> mapOverlays = mapView.getOverlays();
            if (mRoadOverlays != null) {
                for (Polyline mRoadOverlay : mRoadOverlays) mapOverlays.remove(mRoadOverlay);
                mRoadOverlays = null;
            }
        }
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