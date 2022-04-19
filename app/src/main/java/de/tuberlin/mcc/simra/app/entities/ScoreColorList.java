package de.tuberlin.mcc.simra.app.entities;

import android.content.Context;
import android.content.res.TypedArray;

import org.osmdroid.views.overlay.advancedpolyline.ColorMapping;

import de.tuberlin.mcc.simra.app.R;


/**
 * Class for mapping safety/surface quality scores to color within road segments.
 */
public class ScoreColorList implements ColorMapping {

    public enum ScoreType {NONE, SURFACE_SIMRA, SURFACE_OSM, SAFETY}

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

    /**
     * Get the color for a specific segment.
     *
     * @param pSegmentIndex index of the segment.
     * @return color in integer resource form
     */
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
                return context.getColor(R.color.simraBlue);
        }
    }

    /**
     * Get the corresponding color for a specific surface quality score.
     *
     * @param value the surface quality score
     * @return color in integer resource form
     */
    private int getSurfaceQualityColor(int value) {
        TypedArray colorScale = context.getResources().obtainTypedArray(R.array.scoreColorScale);
        if (value == 0)
            return colorScale.getColor(0, context.getColor(R.color.score1));
        int color = colorScale.getColor(value - 1, context.getColor(R.color.score1));
        colorScale.recycle();
        return color;
    }

    /**
     * Get the corresponding color for a specific safety score.
     *
     * @param value the safety score
     * @return color in integer resource form
     */
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
