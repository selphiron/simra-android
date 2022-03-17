package de.tuberlin.mcc.simra.app.entities;

import android.os.Parcel;
import android.os.Parcelable;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadLeg;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class SimraRoad extends Road implements Parcelable {
    public int[] safetyScoreSegmentValues;
    public int[] simraSurfaceQualitySegmentValues;
    public int[] osmSurfaceQualitySegmentValues;

    public SimraRoad() {
        super();
    }

    public SimraRoad(ArrayList<GeoPoint> waypoints) {
        super(waypoints);
    }


    // Parcelable implementation

    @Override
    public int describeContents() {
        return super.describeContents();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeIntArray(safetyScoreSegmentValues);
        out.writeIntArray(simraSurfaceQualitySegmentValues);
        out.writeIntArray(osmSurfaceQualitySegmentValues);
    }

    public static final Creator<SimraRoad> CREATOR = new Creator<SimraRoad>() {
        @Override
        public SimraRoad createFromParcel(Parcel source) {
            return new SimraRoad(source);
        }

        @Override
        public SimraRoad[] newArray(int size) {
            return new SimraRoad[size];
        }
    };

    private SimraRoad(Parcel in) {
        this.mStatus = in.readInt();
        this.mLength = in.readDouble();
        this.mDuration = in.readDouble();
        this.mNodes = in.readArrayList(RoadNode.class.getClassLoader());
        this.mLegs = in.readArrayList(RoadLeg.class.getClassLoader());
        this.mRouteHigh = in.readArrayList(GeoPoint.class.getClassLoader());
        this.mBoundingBox = in.readParcelable(BoundingBox.class.getClassLoader());
        this.safetyScoreSegmentValues = in.createIntArray();
        this.simraSurfaceQualitySegmentValues = in.createIntArray();
        this.osmSurfaceQualitySegmentValues = in.createIntArray();
    }
}
