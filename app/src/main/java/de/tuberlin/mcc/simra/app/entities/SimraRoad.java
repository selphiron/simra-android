package de.tuberlin.mcc.simra.app.entities;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class SimraRoad extends Road {
   public int[] safetyScoreSegmentValues;
   public int[] simraSurfaceQualitySegmentValues;
   public int[] osmSurfaceQualitySegmentValues;

   public SimraRoad() {
      super();
   }

   public SimraRoad(ArrayList<GeoPoint> waypoints) {
      super(waypoints);
   }
}
