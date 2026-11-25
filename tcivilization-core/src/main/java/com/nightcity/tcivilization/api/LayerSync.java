package com.nightcity.tcivilization.api;

/**
 * Synchronization policy for a RegionLayer (03 §5.2.3).
 */
public enum LayerSync {
    NONE,
    NEAR,
    ON_DEMAND,
    GLOBAL_SUMMARY
}
