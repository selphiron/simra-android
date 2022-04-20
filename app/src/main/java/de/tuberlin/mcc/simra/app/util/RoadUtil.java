package de.tuberlin.mcc.simra.app.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;

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
import de.tuberlin.mcc.simra.app.entities.ScoreColorList;
import de.tuberlin.mcc.simra.app.entities.SimraRoad;

/**
 * Utility class containing methods to operate on the returned route.
 */
public class RoadUtil {
    MapView mapView;
    SimraRoad road;
    Polyline roadOverlay;
    FolderOverlay roadNodeMarkers;
    Context context;

    private static final String TAG = "RoadUtil_LOG";

    public RoadUtil(MapView mapView, SimraRoad road, Polyline roadOverlay, FolderOverlay roadNodeMarkers, Context context) {
        this.mapView = mapView;
        this.road = road;
        this.roadOverlay = roadOverlay;
        this.roadNodeMarkers = roadNodeMarkers;
        this.context = context;
    }

    /**
     * Draws the route with corresponding nodes containing instructions.
     *
     * @param scoreType the score type to display the route with (safety/surface quality)
     * @return the Polyline that was drawn
     */
    public Polyline drawRoute(ScoreColorList.ScoreType scoreType) {
        roadNodeMarkers.getItems().clear();
        List<Overlay> mapOverlays = mapView.getOverlays();
        if (roadOverlay != null) {
            mapOverlays.remove(roadOverlay);
            roadOverlay = null;
        }
        Polyline roadPolyline = RoadManager.buildRoadOverlay(road);
        roadOverlay = roadPolyline;
        roadPolyline.getOutlinePaint().setStrokeWidth(20);
        // set info window for road
        String routeDesc = road.getLengthDurationText(context, -1);
        roadPolyline.setTitle(context.getString(R.string.route) + " - " + routeDesc);
        roadPolyline.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView));
        roadPolyline.setRelatedObject(0);
        mapOverlays.add(0, roadPolyline);
        putRoadNodes();
        // paint the road
        paintRoad(scoreType);
        mapView.invalidate();
        return roadOverlay;
    }

    public void paintRoad(ScoreColorList.ScoreType scoreType) {
        Log.d(TAG, "overlay items: " + mapView.getOverlays().size());
        roadOverlay.getOutlinePaintLists().clear();
        Paint p = roadOverlay.getOutlinePaint();
        PaintList pList = new PolychromaticPaintList(
                p,
                new ScoreColorList(road, scoreType, context),
                false
        );
        roadOverlay.getOutlinePaintLists().add(pList);
        mapView.invalidate();
    }

    /**
     * Adds instruction nodes for the calculated route. Each node has a popup window that can be displayed on tap.
     */
    public void putRoadNodes() {
        roadNodeMarkers.getItems().clear();
        int n = road.mNodes.size();
        MarkerInfoWindow infoWindow = new MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
        TypedArray iconIds = context.getResources().obtainTypedArray(R.array.direction_icons);
        // add a node marker for each instruction
        for (int i = 0; i < n; i++) {
            RoadNode node = road.mNodes.get(i);
            String instructions = (node.mInstructions == null ? "" : node.mInstructions);
            // set marker with popup window containing navigation instructions
            Marker nodeMarker = new Marker(mapView);
            nodeMarker.setTitle(context.getString(R.string.step) + " " + (i + 1));
            nodeMarker.setSnippet(instructions);
            nodeMarker.setSubDescription(Road.getLengthDurationText(context, node.mLength, node.mDuration));
            nodeMarker.setPosition(node.mLocation);
            setMarkerIcon(nodeMarker, R.drawable.ic_circle, R.color.colorPrimaryDark, context);
            nodeMarker.setInfoWindow(infoWindow); //use a shared info window.
            int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
            if (iconId != R.drawable.ic_empty) {
                Drawable image = ResourcesCompat.getDrawable(context.getResources(), iconId, null);
                nodeMarker.setImage(image);
            }
            nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            roadNodeMarkers.add(nodeMarker);
            //add road markers below waypoints, above road polyline
            mapView.getOverlays().add(1, roadNodeMarkers);
        }
        iconIds.recycle();
    }

    /**
     * Sets the marker icon for a given marker
     *
     * @param marker       the marker in question
     * @param iconResource Resource ID for marker icon.
     * @param color        the color of the icon
     * @param ctx          generic context
     */
    public static void setMarkerIcon(Marker marker, int iconResource, int color, Context ctx) {
        Drawable icon = ResourcesCompat.getDrawable(ctx.getResources(), iconResource, null);
        // set icon tint
        assert icon != null;
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(icon, ctx.getColor(color));
        marker.setIcon(icon);
    }

    /**
     * Returns the instruction content for a given node of the route.
     *
     * @param node    the node in question
     * @param index   index of the node
     * @param context generic context
     * @return A pair containing a pair of an instruction title and the duration, and the corresponding navigation instruction image
     */
    public static Pair<Pair<String, String>, Drawable> getInstructionContent(RoadNode node, int index, Context context) {
        String step = context.getString(R.string.step) + " " + (index + 1);
        String instruction = node.mInstructions == null ? "" : node.mInstructions;
        String headerText = String.format("%s: %s", step, instruction);
        String durationText = Road.getLengthDurationText(context, node.mLength, node.mDuration);
        // icon
        TypedArray iconIds = context.getResources().obtainTypedArray(R.array.direction_icons);
        int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
        Drawable image = null;
        if (iconId != R.drawable.ic_empty) {
            image = ResourcesCompat.getDrawable(context.getResources(), iconId, null);
        }
        iconIds.recycle();
        return new Pair<>(new Pair<>(headerText, durationText), image);
    }

    /**
     * Returns the index of the next node that needs to be visited.
     *
     * @param lastVisitedNodeIndex index of the node that was last visited
     * @param road                 SimraRoad in which the node index needs to be found
     * @param currentLocation      current location of the user
     * @return node index
     */
    public static int getNextNodeIndex(int lastVisitedNodeIndex, SimraRoad road, GeoPoint currentLocation) {
        // init vars
        double distanceShortest = currentLocation.distanceToAsDouble(road.mNodes.get(0).mLocation);
        int closestIndex = 0;
        // find the node the user is currently the closest to
        for (int i = 0; i < road.mNodes.size(); i++) {
            RoadNode node = road.mNodes.get(i);
            double distance = currentLocation.distanceToAsDouble(node.mLocation);
            // assign new distance only if:
            // the node is the shortest distance
            // the next node in order
            // 20 m or less from the user
            if (
                    currentLocation.distanceToAsDouble(node.mLocation) < distanceShortest
                            && (i - 1 == lastVisitedNodeIndex)
                            && (distance <= 20)
            ) {
                distanceShortest = distance;
                closestIndex = i;
            }
        }
        // find node after closest and visited (this is the instruction that needs to be shown)
        int finalIndex = Math.max(closestIndex, lastVisitedNodeIndex);
        if (finalIndex == (road.mNodes.size() - 1)) {
            // the closest node ist the last one, indicates that no further instructions needed
            return -1;
        } else {
            return finalIndex;
        }
    }
}
