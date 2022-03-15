package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import android.content.res.TypedArray;

import org.osmdroid.views.overlay.advancedpolyline.ColorMapping;

import de.tuberlin.mcc.simra.app.R;


public class ScoreColorList implements ColorMapping {

    public enum ScoreType {SURFACE_SIMRA, SURFACE_OSM, SAFETY}

    private final Context context;

    private final int[] osmSurfaceValuesList, simraSurfaceQualityList, safetyScoreList;

    ScoreType selectedType;

    public ScoreColorList(SimraRoad road, ScoreType mappingType, Context context) {
        this.osmSurfaceValuesList = road.osmSurfaceQualitySegmentValues;
        this.simraSurfaceQualityList = road.simraSurfaceQualitySegmentValues;
        this.safetyScoreList = road.safetyScoreSegmentValues;
        this.selectedType = mappingType;
        this.context = context;
    }

    @Override
    public int getColorForIndex(int pSegmentIndex) {
        if (osmSurfaceValuesList == null || simraSurfaceQualityList == null || safetyScoreList == null) {
            return context.getColor(R.color.simraBlue);
        }
        switch (selectedType) {
            case SAFETY:
                return getSafetyScoreColor(safetyScoreList[pSegmentIndex]);
            case SURFACE_OSM:
                return getSurfaceQualityColor(osmSurfaceValuesList[pSegmentIndex]);
            case SURFACE_SIMRA:
                return getSurfaceQualityColor(simraSurfaceQualityList[pSegmentIndex]);
            default:
                return context.getColor(R.color.score1);
        }
    }

    private int getSurfaceQualityColor(int value) {
        TypedArray colorScale = context.getResources().obtainTypedArray(R.array.scoreColorScale);
        int color = colorScale.getColor(value - 1, context.getColor(R.color.score1));
        colorScale.recycle();
        return color;
    }

    private int getSafetyScoreColor(int value) {
        if (value <= 10) {
            return context.getColor(R.color.score1);
        } else if (value <= 25) {
            return context.getColor(R.color.score3);
        } else if (value <= 50) {
            return context.getColor(R.color.score4);
        } else {
            return context.getColor(R.color.score5);
        }
    }
}
