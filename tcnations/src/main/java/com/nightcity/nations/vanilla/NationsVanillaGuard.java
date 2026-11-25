package com.nightcity.nations.vanilla;

import com.mojang.logging.LogUtils;
import com.nightcity.nations.NationsConfig;
import com.nightcity.nations.TNations;
import com.nightcity.nations.data.NationsSavedData;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.slf4j.Logger;

/**
 * Nations-side DR-17 / VO-RD-01..02 / VO-ST-01 guard coordinated through VanillaPolicyService (30 §2.3, §3.2, §3.5, §4.3).
 */
public final class NationsVanillaGuard {
    public static final int FALLBACK_OWNER_PRIORITY = 70;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicLong BLOCKED_PATROLS = new AtomicLong();
    private static final AtomicLong BLOCKED_VILLAGE_ENTITIES = new AtomicLong();
    private static final AtomicLong BLOCKED_VILLAGE_STRUCTURES = new AtomicLong();

    private NationsVanillaGuard() {
    }

    public static void registerPolicyFallbacks() {
        if (!NationsConfig.isEnabled()) {
            return;
        }
        VanillaPolicyService policy = VanillaPolicyService.get();
        if (NationsConfig.isSuppressRaidsAndPatrols()) {
            policy.registerOwner(VanillaSystem.RAIDS, TNations.MOD_ID, FALLBACK_OWNER_PRIORITY);
            policy.registerOwner(VanillaSystem.PATROLS, TNations.MOD_ID, FALLBACK_OWNER_PRIORITY);
        }
        if (NationsConfig.isSuppressPlanetVillages()) {
            policy.registerOwner(VanillaSystem.VILLAGERS, TNations.MOD_ID, FALLBACK_OWNER_PRIORITY);
        }
    }

    public static void applyFirstLoadGameRules(MinecraftServer server, NationsSavedData savedData) {
        if (!NationsConfig.isEnabled() || !NationsConfig.isSuppressRaidsAndPatrols()) {
            return;
        }
        VanillaPolicyService policy = VanillaPolicyService.get();
        ServerLevel overworld = server.overworld();
        boolean shouldEnforce = policy.isPlanetMode(server)
                || policy.effectiveMonsterPolicy(overworld) == MonsterPolicy.RUINS_ONLY;
        if (!shouldEnforce) {
            return;
        }
        GameRules rules = overworld.getGameRules();
        if (isActiveOwnerOrFallback(policy, VanillaSystem.RAIDS)) {
            applyBooleanRuleOnce(server, rules, GameRules.RAIDS, "minecraft:raids", false, savedData);
        }
        if (isActiveOwnerOrFallback(policy, VanillaSystem.PATROLS)) {
            applyBooleanRuleOnce(server, rules, GameRules.SPAWN_PATROLS, "minecraft:spawn_patrols", false, savedData);
        }
    }

    private static void applyBooleanRuleOnce(
            MinecraftServer server,
            GameRules rules,
            GameRule<Boolean> rule,
            String ruleId,
            boolean targetValue,
            NationsSavedData savedData
    ) {
        Optional<Boolean> recorded = savedData.recordedGameruleValue(ruleId);
        if (recorded.isEmpty()) {
            rules.set(rule, targetValue, server);
            savedData.recordGameruleWrite(ruleId, targetValue);
            return;
        }
        boolean current = Boolean.TRUE.equals(rules.get(rule));
        if (current != recorded.get() && savedData.markAdminOverrideWarned(ruleId)) {
            LOGGER.warn("Admin modified gamerule {} to {} (Nations originally wrote {}); respecting admin setting.",
                    ruleId, current, recorded.get());
        }
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!NationsConfig.isEnabled() || event.isSpawnCancelled()) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        VanillaPolicyService policy = VanillaPolicyService.get();
        boolean planetMode = policy.isPlanetMode(level.getServer());
        MonsterPolicy monsterPolicy = policy.effectiveMonsterPolicy(level);
        Entity entity = event.getEntity();
        EntitySpawnReason reason = event.getSpawnType();

        if (shouldCancelPatrol(reason, entity instanceof Raider, planetMode, monsterPolicy, policy)) {
            event.setSpawnCancelled(true);
            BLOCKED_PATROLS.incrementAndGet();
            return;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (shouldCancelVanillaVillageEntity(typeId, reason, planetMode, policy)) {
            event.setSpawnCancelled(true);
            BLOCKED_VILLAGE_ENTITIES.incrementAndGet();
        }
    }

    public static boolean shouldCancelPatrol(
            EntitySpawnReason reason,
            boolean isRaider,
            boolean planetMode,
            MonsterPolicy monsterPolicy,
            VanillaPolicyService policy
    ) {
        if (!NationsConfig.isSuppressRaidsAndPatrols()) {
            return false;
        }
        if (reason != EntitySpawnReason.PATROL && !(isRaider && reason == EntitySpawnReason.NATURAL && planetMode)) {
            return false;
        }
        if (monsterPolicy == MonsterPolicy.RUINS_ONLY || planetMode) {
            Optional<String> owner = policy.owner(VanillaSystem.PATROLS);
            return owner.isEmpty() || TNations.MOD_ID.equals(owner.get()) || "tcivilization".equals(owner.get());
        }
        return false;
    }

    public static boolean shouldCancelVanillaVillageEntity(
            Identifier entityTypeId,
            EntitySpawnReason reason,
            boolean planetMode,
            VanillaPolicyService policy
    ) {
        if (!NationsConfig.isSuppressPlanetVillages() || !planetMode || entityTypeId == null) {
            return false;
        }
        if (reason != EntitySpawnReason.STRUCTURE && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return false;
        }
        String full = entityTypeId.toString();
        boolean isVillager = "minecraft:villager".equals(full) || "minecraft:zombie_villager".equals(full);
        if (!isVillager) {
            return false;
        }
        Optional<String> owner = policy.owner(VanillaSystem.VILLAGERS);
        return owner.isEmpty() || TNations.MOD_ID.equals(owner.get());
    }

    /**
     * Verifies whether a structure set / structure ID is suppressed under DR-17 / VO-ST-01 (30 §3.2).
     */
    public static boolean isStructureAllowed(Identifier structureOrSetId, boolean planetMode, VanillaPolicyService policy) {
        String path = structureOrSetId.getPath();
        boolean isVanillaVillage = "minecraft:villages".equals(structureOrSetId.toString())
                || ("minecraft".equals(structureOrSetId.getNamespace()) && path.startsWith("village"));
        if (isVanillaVillage) {
            boolean settlementsOwns = policy.owner(VanillaSystem.VILLAGERS).filter("tcsettlements"::equals).isPresent();
            if (planetMode || settlementsOwns) {
                BLOCKED_VILLAGE_STRUCTURES.incrementAndGet();
                return false;
            }
        }
        if (planetMode && "minecraft".equals(structureOrSetId.getNamespace())) {
            if (path.contains("pillager_outpost") || path.contains("woodland_mansion") || path.contains("mansion")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isActiveOwnerOrFallback(VanillaPolicyService policy, VanillaSystem system) {
        Optional<String> owner = policy.owner(system);
        return owner.isEmpty() || TNations.MOD_ID.equals(owner.get());
    }

    public static long blockedPatrols() {
        return BLOCKED_PATROLS.get();
    }

    public static long blockedVillageEntities() {
        return BLOCKED_VILLAGE_ENTITIES.get();
    }

    public static long blockedVillageStructures() {
        return BLOCKED_VILLAGE_STRUCTURES.get();
    }

    public static void resetCountersForTests() {
        BLOCKED_PATROLS.set(0L);
        BLOCKED_VILLAGE_ENTITIES.set(0L);
        BLOCKED_VILLAGE_STRUCTURES.set(0L);
    }
}
