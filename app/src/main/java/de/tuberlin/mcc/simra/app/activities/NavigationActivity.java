package de.tuberlin.mcc.simra.app.activities;

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

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.tuberlin.mcc.simra.app.BuildConfig;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.adapter.AutocompleteAdapter;
import de.tuberlin.mcc.simra.app.databinding.ActivityNavigationBinding;
import de.tuberlin.mcc.simra.app.util.BaseActivity;

public class NavigationActivity extends BaseActivity {

    private static final String TAG = "NavigationActivity_LOG";

    ActivityNavigationBinding binding;
    private GeocoderNominatim geocoderNominatim;

    private GeoPoint fromCoordinates;
    private GeoPoint viaCoordinates;
    private GeoPoint toCoordinates;

    private Handler startHandler;
    private Handler viaHandler;
    private Handler toHandler;

    private final int GEO_SEARCH = 10;

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
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        // handler for delayed geocoder fetching
        startHandler = new Handler(msg -> {
            if (msg.what == GEO_SEARCH && !TextUtils.isEmpty(binding.startLocation.getText()))
                getAddresses(binding.startLocation.getText().toString(), binding.startLocation);
            return false;
        });

        // listener for fetching route button
        binding.startNavigationBtn.setOnClickListener(v -> {
            ArrayList<GeoPoint> pointList = new ArrayList<>();
            pointList.add(fromCoordinates);
            if (viaCoordinates != null)
                pointList.add(viaCoordinates);
            pointList.add(toCoordinates);
            try {
                Road suggestedRoute = new FetchRouteTask().execute(pointList).get();

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        binding.startLocation.setAdapter(new AutocompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
        binding.startLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
        binding.startLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                startHandler.removeMessages(GEO_SEARCH);
                startHandler.sendEmptyMessageDelayed(GEO_SEARCH, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void getAddresses(String searchEntry, AutoCompleteTextView textView) {
        try {
            List<Address> results = new ReverseGeocoderTask().execute(searchEntry).get();
            List<String> resultStrings = new ArrayList<>();
            for (Address adr : results) {
                resultStrings.add(addressToString(adr));
            }
            AutocompleteAdapter adapter = (AutocompleteAdapter) textView.getAdapter();
            adapter.setSuggestions(resultStrings);
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

    private class FetchRouteTask extends AsyncTask<ArrayList<GeoPoint>, Void, Road> {
        GraphHopperRoadManager roadManager;

        @Override
        protected Road doInBackground(ArrayList<GeoPoint>... arrayLists) {
            ArrayList<GeoPoint> routePoints = arrayLists[0];
            // empty string as we use custom endpoint
            roadManager = new GraphHopperRoadManager("", false);
            roadManager.setService(BuildConfig.NAVIGATION_ENDPOINT);
            roadManager.addRequestOption("ch.disable=true");
            return roadManager.getRoad(routePoints);
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