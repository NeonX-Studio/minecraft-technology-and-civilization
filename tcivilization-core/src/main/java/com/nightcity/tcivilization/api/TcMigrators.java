package com.nightcity.tcivilization.api;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * Chained schema migration registry for SavedData and tc/ binary files (33 §8.1, 40 §9.1).
 */
public final class TcMigrators {
    public static final String FORMAT_VERSION_KEY = "formatVersion";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, NavigableMap<Integer, TcMigrator>> MIGRATORS = new ConcurrentHashMap<>();
    private static volatile Path migrationLogPath = Path.of("logs", "tc-migration.log");

    private TcMigrators() {
    }

    public static void setMigrationLogPath(Path path) {
        migrationLogPath = Objects.requireNonNull(path, "path");
    }

    public static void register(Identifier fileId, int fromVersion, TcMigrator migrator) {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(migrator, "migrator");
        if (fromVersion < 0) {
            throw new IllegalArgumentException("fromVersion must be >= 0: " + fromVersion);
        }
        NavigableMap<Integer, TcMigrator> chain = MIGRATORS.computeIfAbsent(fileId, k -> new TreeMap<>());
        synchronized (chain) {
            chain.put(fromVersion, migrator);
        }
    }

    public static void registerDynamic(Identifier fileId, int fromVersion, UnaryOperator<Dynamic<?>> operator) {
        Objects.requireNonNull(operator, "operator");
        register(fileId, fromVersion, (ver, tag) -> {
            Dynamic<Tag> input = new Dynamic<>(NbtOps.INSTANCE, tag.copy());
            Dynamic<?> result = operator.apply(input);
            Tag converted = result.convert(NbtOps.INSTANCE).getValue();
            if (converted instanceof CompoundTag compound) {
                return compound;
            }
            throw new IllegalStateException("Migrator did not produce a CompoundTag for " + fileId);
        });
    }

    public static void clearForTests(Identifier fileId) {
        MIGRATORS.remove(fileId);
    }

    public static CompoundTag migrate(Identifier fileId, int targetVersion, CompoundTag rawData) {
        Objects.requireNonNull(fileId, "fileId");
        Objects.requireNonNull(rawData, "rawData");
        int version = readFormatVersion(rawData);
        if (version > targetVersion) {
            String msg = "Save file " + fileId + " is from a newer formatVersion (" + version + " > " + targetVersion + ")";
            logFailure(fileId, version, targetVersion, new IllegalStateException(msg));
            throw new IllegalStateException(msg);
        }
        if (version == targetVersion) {
            CompoundTag copy = rawData.copy();
            copy.putInt(FORMAT_VERSION_KEY, targetVersion);
            return copy;
        }

        CompoundTag working = rawData.copy();
        NavigableMap<Integer, TcMigrator> chain = MIGRATORS.get(fileId);
        for (int v = version; v < targetVersion; v++) {
            TcMigrator step = null;
            if (chain != null) {
                synchronized (chain) {
                    step = chain.get(v);
                }
            }
            if (step != null) {
                try {
                    working = step.migrate(v, working.copy());
                } catch (RuntimeException ex) {
                    logFailure(fileId, v, targetVersion, ex);
                    throw ex;
                }
            }
            working.putInt(FORMAT_VERSION_KEY, v + 1);
        }
        return working;
    }

    public static int readFormatVersion(CompoundTag data) {
        if (data.contains(FORMAT_VERSION_KEY)) {
            return data.getIntOr(FORMAT_VERSION_KEY, 0);
        }
        if (data.contains("schemaVersion")) {
            return data.getIntOr("schemaVersion", 0);
        }
        return 0;
    }

    private static void logFailure(Identifier fileId, int fromVersion, int targetVersion, Throwable error) {
        LOGGER.error("Migration failed for {} ({} -> {}): {}", fileId, fromVersion, targetVersion, error.getMessage());
        try {
            Path log = migrationLogPath;
            if (log.getParent() != null) {
                Files.createDirectories(log.getParent());
            }
            String line = Instant.now() + " [" + fileId + "] " + fromVersion + " -> " + targetVersion + ": " + error + System.lineSeparator();
            Files.writeString(log, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
