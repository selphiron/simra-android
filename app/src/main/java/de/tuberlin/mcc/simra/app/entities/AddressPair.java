package de.tuberlin.mcc.simra.app.entities;

import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;

/**
 * Data class containing a String address and its corresponding location in GeoPoint form.
 */
public class AddressPair {

    private final String address;

    public GeoPoint getCoords() {
        return coords;
    }

    private final GeoPoint coords;

    public AddressPair(GeoPoint coordinates, String addressName) {
        this.address = addressName;
        this.coords = coordinates;
    }

    @NonNull
    @Override
    public String toString() {
        return address;
    }
}
