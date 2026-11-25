package com.nightcity.tcivilization.api;

import com.mojang.logging.LogUtils;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Core-hosted vanilla system ownership and monster policy resolver (03 §12.1, 30 §2.3).
 */
public interface VanillaPolicyService {
    static VanillaPolicyService get() {
        return Impl.INSTANCE;
    }

    boolean isPlanetMode(MinecraftServer server);

    boolean isDeepEarthMode(MinecraftServer server);

    boolean isThresholdMode(MinecraftServer server);

    MonsterPolicy effectiveMonsterPolicy(ServerLevel level);

    Optional<String> owner(VanillaSystem system);

    void registerOwner(VanillaSystem system, String modId, int priority);

    final class Impl implements VanillaPolicyService {
        static final Impl INSTANCE = new Impl();
        private static final Logger LOGGER = LogUtils.getLogger();

        private record OwnerEntry(String modId, int priority) {}

        private final Map<VanillaSystem, OwnerEntry> owners = new EnumMap<>(VanillaSystem.class);
        private final Map<ResourceKey<Level>, MonsterPolicy> overrides = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public boolean isPlanetMode(MinecraftServer server) {
            return owner(VanillaSystem.WORLDGEN).filter("tcwildlands"::equals).isPresent();
        }

        @Override
        public boolean isDeepEarthMode(MinecraftServer server) {
            return owner(VanillaSystem.NETHER).filter("tcwildlands"::equals).isPresent();
        }

        @Override
        public boolean isThresholdMode(MinecraftServer server) {
            return owner(VanillaSystem.END).filter("tcbeyond"::equals).isPresent();
        }

        @Override
        public MonsterPolicy effectiveMonsterPolicy(ServerLevel level) {
            MonsterPolicy override = overrides.get(level.dimension());
            if (override != null) {
                return override;
            }
            if (owner(VanillaSystem.MONSTER_SPAWNS).filter("tcbeyond"::equals).isPresent()) {
                return MonsterPolicy.BEYOND;
            }
            return isPlanetMode(level.getServer()) ? MonsterPolicy.RUINS_ONLY : MonsterPolicy.VANILLA;
        }

        @Override
        public synchronized Optional<String> owner(VanillaSystem system) {
            OwnerEntry entry = owners.get(system);
            return entry == null ? Optional.empty() : Optional.of(entry.modId());
        }

        @Override
        public synchronized void registerOwner(VanillaSystem system, String modId, int priority) {
            Objects.requireNonNull(system, "system");
            Objects.requireNonNull(modId, "modId");
            OwnerEntry current = owners.get(system);
            if (current != null) {
                if (current.priority() == priority && !current.modId().equals(modId)) {
                    if (TcThreads.isStrictMode()) {
                        throw new IllegalStateException("Conflicting priority " + priority + " on " + system + " between " + current.modId() + " and " + modId);
                    }
                    LOGGER.error("Conflicting priority {} on {} between {} and {}", priority, system, current.modId(), modId);
                    if (modId.compareTo(current.modId()) < 0) {
                        owners.put(system, new OwnerEntry(modId, priority));
                    }
                    return;
                }
                if (priority <= current.priority()) {
                    return;
                }
            }
            owners.put(system, new OwnerEntry(modId, priority));
        }

        public void setTemporaryPolicyOverride(ResourceKey<Level> dimension, MonsterPolicy policy) {
            if (policy == null) {
                overrides.remove(dimension);
            } else {
                overrides.put(dimension, policy);
            }
        }

        public synchronized void clearForTests() {
            owners.clear();
            overrides.clear();
        }
    }
}
