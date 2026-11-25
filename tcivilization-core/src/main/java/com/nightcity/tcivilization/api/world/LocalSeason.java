package com.nightcity.tcivilization.api.world;

/**
 * Locally resolved season at a world position and calendar day (10 §5.12.1, §12.1).
 */
public record LocalSeason(
        Season astronomical,
        SeasonRegime regime,
        String labelKey,
        float progress,
        boolean southernHemisphere
) {
    public Season effectiveSeason() {
        if (regime == SeasonRegime.MONSOON) {
            if (labelKey != null && labelKey.endsWith(".wet")) {
                return Season.WET;
            }
            if (labelKey != null && labelKey.endsWith(".dry")) {
                return Season.DRY;
            }
        }
        return astronomical;
    }
}
