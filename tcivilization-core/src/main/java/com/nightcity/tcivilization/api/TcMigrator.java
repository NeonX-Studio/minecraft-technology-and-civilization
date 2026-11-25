package com.nightcity.tcivilization.api;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;

/**
 * Stepwise schema migrator for SavedData and binary container metadata (40 §9.1).
 */
@FunctionalInterface
public interface TcMigrator {
    CompoundTag migrate(int fromVersion, CompoundTag data);

    default TcMigrator andThen(TcMigrator next) {
        Objects.requireNonNull(next, "next");
        return (fromVersion, data) -> {
            CompoundTag intermediate = this.migrate(fromVersion, data);
            return next.migrate(fromVersion + 1, intermediate);
        };
    }
}
