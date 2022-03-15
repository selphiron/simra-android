package de.tuberlin.mcc.simra.app.entities;

import org.osmdroid.bonuspack.routing.Road;

public class SimraRoad extends Road {
   public int[] safetyScoreSegmentValues;
   public int[] simraSurfaceQualitySegmentValues;
   public int[] osmSurfaceQualitySegmentValues;
}
