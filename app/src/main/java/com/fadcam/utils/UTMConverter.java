package com.fadcam.utils;

/**
 * Converts WGS84 latitude/longitude to UTM (Universal Transverse Mercator)
 * coordinates using the Krüger series formulas.
 *
 * <p>UTM divides Earth into 60 zones (each 6° wide) and provides easting
 * and northing in meters with mm-level accuracy within each zone.
 *
 * <p>Reference: J. H. L. Krüger (1912), WGS84 ellipsoid parameters.
 */
public final class UTMConverter {

    private static final double WGS84_A = 6378137.0;          // equatorial radius (m)
    private static final double WGS84_F = 1.0 / 298.257223563; // flattening
    private static final double K0 = 0.9996;                   // central meridian scale factor
    private static final double FALSE_EASTING = 500000.0;      // meters
    private static final double FALSE_NORTHING_SOUTH = 10000000.0;

    private UTMConverter() {}

    /**
     * Converts WGS84 lat/lon to a human-readable UTM string.
     *
     * @param lat Latitude in degrees (-90 to 90)
     * @param lon Longitude in degrees (-180 to 180)
     * @return Formatted string like "UTM Zone 42N — 372684m E, 3512334m N"
     */
    public static String latLonToUTM(double lat, double lon) {
        if (lat < -80 || lat > 84) return ""; // outside UTM coverage (use UPS instead)

        boolean isNorth = lat >= 0;
        int zone = (int) Math.floor((lon + 180.0) / 6.0) + 1;
        if (zone < 1) zone = 1;
        if (zone > 60) zone = 60;

        // Handle Norway exceptions (extended zone 32 for western Norway)
        if (lat >= 56 && lat < 64 && lon >= 3 && lon < 12) zone = 32;
        // Handle Svalbard exceptions (zones 31,33,35,37 extended; 32,34,36 not used)
        if (lat >= 72 && lat < 84) {
            if (lon >= 0 && lon < 9) zone = 31;
            else if (lon >= 9 && lon < 21) zone = 33;
            else if (lon >= 21 && lon < 33) zone = 35;
            else if (lon >= 33 && lon < 42) zone = 37;
        }

        double lon0 = Math.toRadians((zone - 1) * 6 - 180 + 3);
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);

        double[] result = utmFromLatLon(latRad, lonRad, lon0);
        double easting = result[0];
        double northing = result[1];

        // Apply false easting/northing
        easting += FALSE_EASTING;
        if (!isNorth) northing += FALSE_NORTHING_SOUTH;

        int eastingInt = (int) Math.round(easting);
        int northingInt = (int) Math.round(northing);

        return String.format("UTM Zone %d%s - %,dm E, %,dm N",
                zone, isNorth ? "N" : "S", eastingInt, northingInt);
    }

    /**
     * Krüger series conversion from lat/lon to UTM easting/northing (raw, before false offsets).
     * Accurate to ~1mm within 3000km of the central meridian.
     */
    private static double[] utmFromLatLon(double lat, double lon, double lon0) {
        double n = WGS84_F / (2.0 - WGS84_F);
        double A = WGS84_A / (1.0 + n) * (1.0 + n * n / 4.0 + n * n * n * n / 64.0);

        double[] alpha = new double[4];
        alpha[1] = 0.5 * n - 2.0 / 3.0 * n * n + 5.0 / 16.0 * n * n * n;
        alpha[2] = 13.0 / 48.0 * n * n - 3.0 / 5.0 * n * n * n;
        alpha[3] = 61.0 / 240.0 * n * n * n;

        double sinLat = Math.sin(lat);
        double t = Math.sinh(atanhSin(sinLat, n));
        double dLon = lon - lon0;
        double cosDLon = Math.cos(dLon);
        double sinDLon = Math.sin(dLon);

        double xi1 = Math.atan2(t, cosDLon);
        double eta1val = MathX.atanh(sinDLon / Math.sqrt(1 + t * t));

        // Krüger series: E = k₀A[η' + Σαⱼcos(2jξ')sinh(2jη')], N = k₀A[ξ' + Σαⱼsin(2jξ')cosh(2jη')]
        double easting = eta1val;
        double northing = xi1;
        for (int j = 1; j <= 3; j++) {
            easting += alpha[j] * Math.cos(2.0 * j * xi1) * Math.sinh(2.0 * j * eta1val);
            northing += alpha[j] * Math.sin(2.0 * j * xi1) * Math.cosh(2.0 * j * eta1val);
        }
        easting *= K0 * A;
        northing *= K0 * A;

        return new double[]{easting, northing};
    }

    /** sinh(atanh(sin φ) - 2√n/(1+n) * atanh(2√n/(1+n) * sin φ)) */
    private static double atanhSin(double sinLat, double nParam) {
        double sqrtN = Math.sqrt(nParam);
        double factor = 2.0 * sqrtN / (1.0 + nParam);
        return Math.sinh(
            MathX.atanh(sinLat) - factor * MathX.atanh(factor * sinLat)
        );
    }

    /** Lightweight hyperbolic math helpers. */
    private static final class MathX {
        static double atanh(double x) {
            // Clamp to domain [-1+ε, 1-ε] to avoid NaN at poles
            if (x >= 1.0) x = 0.9999999999;
            if (x <= -1.0) x = -0.9999999999;
            return 0.5 * Math.log((1.0 + x) / (1.0 - x));
        }
    }
}
