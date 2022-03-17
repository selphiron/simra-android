package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.PaintList;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.util.List;

import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.activities.NavigationActivity;
import de.tuberlin.mcc.simra.app.entities.ScoreColorList;
import de.tuberlin.mcc.simra.app.entities.SimraRoad;

public class RouteUtils {
    private static final String TAG = "RouteUtils_LOG";
    SimraRoad[] roads;
    MapView mapView;
    Context context;
    FolderOverlay mRoadNodeMarkers;
    Polyline[] mRoadOverlays;


    public RouteUtils(MapView mapView, SimraRoad[] roads, FolderOverlay roadNodeMarkers, Context context, Polyline[] roadOverlays) {
        this.mapView = mapView;
        this.mRoadNodeMarkers = roadNodeMarkers;
        this.roads = roads;
        this.context = context;
        this.mRoadOverlays = roadOverlays;
    }

    private void putRoadNodes(Road road) {
        mRoadNodeMarkers.getItems().clear();
        int n = road.mNodes.size();
        MarkerInfoWindow infoWindow = new MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
        TypedArray iconIds = context.getResources().obtainTypedArray(R.array.direction_icons);
        for (int i = 0; i < n; i++) {
            RoadNode node = road.mNodes.get(i);
            String instructions = (node.mInstructions == null ? "" : node.mInstructions);
            Marker nodeMarker = new Marker(mapView);
            nodeMarker.setTitle(context.getString(R.string.step) + " " + (i + 1));
            nodeMarker.setSnippet(instructions);
            nodeMarker.setSubDescription(Road.getLengthDurationText(context, node.mLength, node.mDuration));
            nodeMarker.setPosition(node.mLocation);
            setMarkerIcon(nodeMarker, R.drawable.ic_circle, R.color.colorPrimaryDark);
            nodeMarker.setInfoWindow(infoWindow); //use a shared info window.
            int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
            if (iconId != R.drawable.ic_empty) {
                Drawable image = ResourcesCompat.getDrawable(context.getResources(), iconId, null);
                nodeMarker.setImage(image);
            }
            nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mRoadNodeMarkers.add(nodeMarker);
            //add road markers below waypoints, above road polyline
            mapView.getOverlays().add(1, mRoadNodeMarkers);
        }
        iconIds.recycle();
    }

    public String[] updateUIWithRoads(Road[] roads) {
        Log.d(TAG, "road count: " + roads.length);
        mRoadNodeMarkers.getItems().clear();
        List<Overlay> mapOverlays = mapView.getOverlays();
        if (mRoadOverlays != null) {
            for (Polyline mRoadOverlay : mRoadOverlays) mapOverlays.remove(mRoadOverlay);
            mRoadOverlays = null;
        }
        mRoadOverlays = new Polyline[roads.length];
        String[] durations = new String[roads.length];
        for (int i = 0; i < roads.length; i++) {
            Polyline roadPolyline = RoadManager.buildRoadOverlay(roads[i]);
            mRoadOverlays[i] = roadPolyline;
            roadPolyline.getOutlinePaint().setStrokeWidth(20);
            String routeDesc = roads[i].getLengthDurationText(context, -1);
            durations[i] = routeDesc;
            roadPolyline.setTitle(context.getString(R.string.route) + " - " + routeDesc);
            roadPolyline.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView));
            roadPolyline.setRelatedObject(i);
            mapOverlays.add(0, roadPolyline);
        }
        selectRoad(ScoreColorList.ScoreType.NONE);
        return durations;
    }

    public void selectRoad(ScoreColorList.ScoreType scoreType) {
        mRoadOverlays[0].getOutlinePaintLists().clear();
        putRoadNodes(roads[0]);
        for (int i = 0; i < mRoadOverlays.length; i++) {
            Paint p = mRoadOverlays[i].getOutlinePaint();
            PaintList pList = new PolychromaticPaintList(
                    p,
                    new ScoreColorList(roads[0], scoreType, context),
                    false
            );
            if (i == 0)
                mRoadOverlays[0].getOutlinePaintLists().add(pList);
            else
                p.setColor(Color.GRAY);
        }
        mapView.invalidate();
    }

    public void setMarkerIcon(Marker marker, int iconResource, int color) {
        Drawable icon = ResourcesCompat.getDrawable(context.getResources(), iconResource, null);
        // set icon tint
        assert icon != null;
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(icon, context.getColor(color));
        marker.setIcon(icon);
    }

    public void addMarker(GeoPoint point, NavigationActivity.PointType markerType, String address) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(address);
        int color;
        switch (markerType) {
            case START:
                color = R.color.startGreen;
                break;
            case END:
                color = R.color.endRed;
                break;
            default:
                color = R.color.viaYellow;
        }
        setMarkerIcon(marker, R.drawable.ic_marker, color);
        mapView.getOverlays().add(marker);
    }
}
