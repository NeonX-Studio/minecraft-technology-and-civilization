package com.nightcity.tcivilization.api;

/**
 * Optimistic concurrency contract for shared mutable objects (03 §11, 31 §16.3).
 */
public interface VersionedShared {
    long version();
}
