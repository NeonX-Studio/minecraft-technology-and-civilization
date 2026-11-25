package com.nightcity.tcivilization.api.world;

import java.util.Arrays;

/**
 * 12-month climatological normals and Koppen-Geiger classification (10 §5.7, §12.1).
 */
public record ClimateNormals(
        float[] tempMonthlyC,
        float[] precipMonthlyMm,
        String koppen,
        float annualMeanC,
        float annualPrecipMm,
        float seasonality
) {
    public ClimateNormals {
        tempMonthlyC = tempMonthlyC == null ? new float[12] : Arrays.copyOf(tempMonthlyC, tempMonthlyC.length);
        precipMonthlyMm = precipMonthlyMm == null ? new float[12] : Arrays.copyOf(precipMonthlyMm, precipMonthlyMm.length);
        koppen = koppen == null ? "Cfb" : koppen;
    }
}
