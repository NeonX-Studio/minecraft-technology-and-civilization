package com.nightcity.tcivilization.api.world;

/**
 * Geographic latitude (-90..90, positive North) and longitude (-180..180, positive East) in degrees (10 §5.1.2).
 */
public record LatLon(double latDeg, double lonDeg) {
}
