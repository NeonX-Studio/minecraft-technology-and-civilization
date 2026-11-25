package com.nightcity.tcivilization.api.world;

import java.util.List;
import java.util.Optional;

/**
 * Survey reading produced by player or abstract NPC prospecting (10 §12.1).
 */
public record ProspectReading(
        ProspectMethod method,
        List<Anomaly> anomalies,
        Optional<StrataColumn> column,
        float noiseSigma,
        int costTicks,
        List<Long> newlyDiscoveredDeposits
) {
    public ProspectReading {
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        column = column == null ? Optional.empty() : column;
        newlyDiscoveredDeposits = newlyDiscoveredDeposits == null ? List.of() : List.copyOf(newlyDiscoveredDeposits);
    }
}
