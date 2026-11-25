package com.nightcity.wildlands.prospect;

import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.ProficiencyBridge;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.ProspectMethod;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import com.nightcity.tcivilization.api.world.event.RockIdentifiedEvent;
import com.nightcity.wildlands.WildlandsConfig;
import com.nightcity.wildlands.network.WildlandsPayloads;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles player-placed artificial block tracking, rock-tap identification, river panning,
 * obsidian portal hint tracking, and deep_threshold 60-tick sneak transitions (10 §5.10.5, §5.16.2, §9).
 */
public final class ProspectingService {
    private static final Identifier SURVEY_PRECISION = WildlandsCatalog.wl("precision/survey");
    private static final Identifier SURVEY_RANGE = WildlandsCatalog.wl("range/survey");
    private static final Identifier ROCK_TAP_SOURCE = WildlandsCatalog.wl("rock_tap");
    private static final Identifier PANNING_SOURCE = WildlandsCatalog.wl("panning");

    private static final Map<Long, BitSet> ARTIFICIAL_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TAP_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PAN_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> REACH_PANNING_BUDGET = new ConcurrentHashMap<>();
    private static final Set<UUID> PORTAL_HINT_SHOWN = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> CIV_IDENTIFIED_ROCKS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> DEEP_SNEAK_TICKS = new ConcurrentHashMap<>();

    private ProspectingService() {
    }

    private static long chunkKey(Level level, BlockPos pos) {
        long dimHash = level.dimension().identifier().hashCode() & 0xFFFFL;
        long cx = (pos.getX() >> 4) & 0xFFFFFFL;
        long cz = (pos.getZ() >> 4) & 0xFFFFFFL;
        return (dimHash << 48) | (cx << 24) | cz;
    }

    private static int localBitIndex(BlockPos pos) {
        int lx = pos.getX() & 15;
        int lz = pos.getZ() & 15;
        int ly = Math.clamp(pos.getY() - PlanetCoordinates.Y_MIN, 0, 2047);
        return (ly << 8) | (lz << 4) | lx;
    }

    public static void markPlayerPlaced(Level level, BlockPos pos) {
        BitSet bits = ARTIFICIAL_BLOCKS.computeIfAbsent(chunkKey(level, pos), k -> new BitSet(4096));
        synchronized (bits) {
            bits.set(localBitIndex(pos));
        }
    }

    public static void clearPlayerPlaced(Level level, BlockPos pos) {
        BitSet bits = ARTIFICIAL_BLOCKS.get(chunkKey(level, pos));
        if (bits != null) {
            synchronized (bits) {
                bits.clear(localBitIndex(pos));
            }
        }
    }

    public static boolean isPlayerPlaced(Level level, BlockPos pos) {
        BitSet bits = ARTIFICIAL_BLOCKS.get(chunkKey(level, pos));
        if (bits == null) {
            return false;
        }
        synchronized (bits) {
            return bits.get(localBitIndex(pos));
        }
    }

    public static boolean shouldShowPortalIgnitionHint(UUID playerUuid) {
        return PORTAL_HINT_SHOWN.add(playerUuid);
    }

    /**
     * Performs rock-tap identification when sneaking and right-clicking a natural rock/ore with a pickaxe (10 §9.2).
     */
    public static boolean tryRockTap(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack heldStack) {
        if (isPlayerPlaced(level, pos)) {
            return false;
        }
        long now = level.getGameTime();
        long cooldownUntil = TAP_COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L);
        if (now < cooldownUntil) {
            return false;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        WildlandsCatalog.RockSpec rockSpec = WildlandsCatalog.ROCK_BY_ID.get(blockId);
        if (rockSpec == null && blockId.getPath().endsWith("_ore")) {
            String path = blockId.getPath();
            for (WildlandsCatalog.RockSpec r : WildlandsCatalog.ROCKS) {
                if (path.startsWith(r.hostPrefix() + "_")) {
                    rockSpec = r;
                    break;
                }
            }
        }
        if (rockSpec == null) {
            return false;
        }

        TAP_COOLDOWN_UNTIL.put(player.getUUID(), now + 40L);
        heldStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        double precMult = ProficiencyBridge.multiplier(player, "surveying", SURVEY_PRECISION);
        double rangeMult = ProficiencyBridge.multiplier(player, "surveying", SURVEY_RANGE);
        double pIdentify = Math.min(1.0, (0.88 - rockSpec.identifyDifficulty() * 0.45) * precMult);
        boolean identified = player.getRandom().nextDouble() <= pIdentify;
        int hintRadius = (int) Math.clamp(Math.round(24.0 * rangeMult), 8, 39);

        String hint = "";
        PlanetModel model = PlanetRuntime.model();
        if (identified && model != null) {
            for (Deposit d : model.deposits()) {
                int dx = PlanetCoordinates.wrappedDx(pos.getX(), d.center().getX());
                int dz = d.center().getZ() - pos.getZ();
                int dy = d.center().getY() - pos.getY();
                if (dx * dx + dy * dy + dz * dz <= (d.radiusXZ() + hintRadius) * (d.radiusXZ() + hintRadius)) {
                    hint = d.mineral().getPath();
                    TcWorldApis.geo().markDiscovered(d.id(), Civilization.PLAYER_CIV_ID, Optional.of(player), ProspectMethod.ROCK_TAP);
                    break;
                }
            }
        }

        if (identified) {
            boolean firstCiv = CIV_IDENTIFIED_ROCKS.add(rockSpec.id());
            ProficiencyBridge.add(player, "surveying", firstCiv ? 12 : 3, ROCK_TAP_SOURCE);
            NeoForge.EVENT_BUS.post(new RockIdentifiedEvent(player, rockSpec.id(), firstCiv, ProspectMethod.ROCK_TAP));
        }

        String msgText = identified
                ? (hint.isEmpty() ? rockSpec.id().getPath() : (rockSpec.id().getPath() + " · " + hint))
                : "unidentified";
        PacketDistributor.sendToPlayer(player, new WildlandsPayloads.RockTapResultPayload(rockSpec.id(), identified, msgText));
        player.sendOverlayMessage(Component.translatable(
                identified ? "msg.tcwildlands.rock_tap.identified" : "msg.tcwildlands.rock_tap.unknown",
                Component.translatable("block." + rockSpec.id().getNamespace() + "." + rockSpec.id().getPath()),
                hint.isEmpty() ? Component.translatable("msg.tcwildlands.rock_tap.no_ore") : Component.literal(hint)
        ));
        return true;
    }

    private static final java.util.Set<Identifier> PLACER_MINERALS = java.util.Set.of(
            WildlandsCatalog.wl("raw_native_gold"),
            WildlandsCatalog.wl("raw_cassiterite"),
            WildlandsCatalog.wl("raw_ilmenite"),
            WildlandsCatalog.wl("raw_rutile"),
            WildlandsCatalog.wl("raw_monazite"),
            WildlandsCatalog.wl("raw_diamond")
    );

    /**
     * Computes downstream placer panning hit probability in [0, 0.65] at (blockX, blockZ) from upstream
     * heavy-mineral deposits in the same drainage basin; returns 0.0 when no upstream placer source exists.
     */
    public static double computeDownstreamPlacerProbability(PlanetModel model, int blockX, int blockZ) {
        int l0Idx = PlanetModel.l0Index(PlanetModel.blockToL0X(blockX), PlanetModel.blockToL0Z(blockZ));
        int basinId = model.rawBasin()[l0Idx] & 0xFFFF;
        double siteAltM = model.sampleBicubicAltitudeMeters(blockX, blockZ);
        double bestProb = 0.0;
        BlockPos here = new BlockPos(blockX, 64, blockZ);
        for (Deposit dep : model.deposits()) {
            WildlandsCatalog.MineralSpec mSpec = WildlandsCatalog.MINERAL_BY_ID.get(dep.mineral());
            Identifier rawId = mSpec != null ? mSpec.rawItemId() : WildlandsCatalog.wl("raw_" + dep.mineral().getPath());
            if (!PLACER_MINERALS.contains(rawId)) {
                continue;
            }
            int depL0 = PlanetModel.l0Index(PlanetModel.blockToL0X(dep.center().getX()), PlanetModel.blockToL0Z(dep.center().getZ()));
            int depBasin = model.rawBasin()[depL0] & 0xFFFF;
            if (depBasin != basinId) {
                continue;
            }
            double depAltM = model.sampleBicubicAltitudeMeters(dep.center().getX(), dep.center().getZ());
            if (depAltM + 5.0 < siteAltM) {
                continue; // Strictly downstream or co-level
            }
            double dist = com.nightcity.wildlands.planet.PlanetCoordinates.wrappedDistance(dep.center(), here);
            if (dist <= 12_000.0) {
                double p = 0.60 * Math.exp(-dist / 3_500.0);
                if (p > bestProb) {
                    bestProb = p;
                }
            }
        }
        return bestProb;
    }

    public static Optional<Identifier> rollPanningAt(PlanetModel model, int blockX, int blockZ, long reachId, RandomLike rng) {
        int remaining = REACH_PANNING_BUDGET.getOrDefault(reachId, 64);
        if (remaining <= 0) {
            return Optional.empty();
        }
        int l0Idx = PlanetModel.l0Index(PlanetModel.blockToL0X(blockX), PlanetModel.blockToL0Z(blockZ));
        int basinId = model.rawBasin()[l0Idx] & 0xFFFF;
        double siteAltM = model.sampleBicubicAltitudeMeters(blockX, blockZ);
        BlockPos here = new BlockPos(blockX, 64, blockZ);
        Identifier bestRaw = null;
        double bestProb = 0.0;
        for (Deposit dep : model.deposits()) {
            WildlandsCatalog.MineralSpec mSpec = WildlandsCatalog.MINERAL_BY_ID.get(dep.mineral());
            Identifier rawId = mSpec != null ? mSpec.rawItemId() : WildlandsCatalog.wl("raw_" + dep.mineral().getPath());
            if (!PLACER_MINERALS.contains(rawId)) {
                continue;
            }
            int depL0 = PlanetModel.l0Index(PlanetModel.blockToL0X(dep.center().getX()), PlanetModel.blockToL0Z(dep.center().getZ()));
            if ((model.rawBasin()[depL0] & 0xFFFF) != basinId) {
                continue;
            }
            if (model.sampleBicubicAltitudeMeters(dep.center().getX(), dep.center().getZ()) + 5.0 < siteAltM) {
                continue;
            }
            double dist = com.nightcity.wildlands.planet.PlanetCoordinates.wrappedDistance(dep.center(), here);
            if (dist <= 12_000.0) {
                double p = 0.60 * Math.exp(-dist / 3_500.0);
                if (p > bestProb) {
                    bestProb = p;
                    bestRaw = rawId;
                }
            }
        }
        if (bestRaw == null || rng.nextDouble() > bestProb) {
            return Optional.empty();
        }
        REACH_PANNING_BUDGET.put(reachId, remaining - 1);
        return Optional.of(bestRaw);
    }

    /**
     * Performs bowl panning in shallow river water with a 64-hit budget per river reach (10 §9.3, AC-WLD-05).
     */
    public static Optional<Identifier> rollPanning(long reachId, RandomLike rng) {
        int remaining = REACH_PANNING_BUDGET.getOrDefault(reachId, 64);
        if (remaining <= 0) {
            return Optional.empty();
        }
        if (rng.nextDouble() > 0.42) {
            return Optional.empty();
        }
        REACH_PANNING_BUDGET.put(reachId, remaining - 1);
        List<Identifier> heavyMinerals = List.copyOf(PLACER_MINERALS);
        return Optional.of(heavyMinerals.get(rng.nextInt(heavyMinerals.size())));
    }

    public static int remainingPanningBudget(long reachId) {
        return REACH_PANNING_BUDGET.getOrDefault(reachId, 64);
    }

    public static void restorePanningBudgetOnFlood(long reachId) {
        REACH_PANNING_BUDGET.merge(reachId, 32, (oldVal, add) -> Math.min(64, oldVal + add));
    }

    public static void clearForTests() {
        ARTIFICIAL_BLOCKS.clear();
        TAP_COOLDOWN_UNTIL.clear();
        PAN_COOLDOWN_UNTIL.clear();
        REACH_PANNING_BUDGET.clear();
        PORTAL_HINT_SHOWN.clear();
        CIV_IDENTIFIED_ROCKS.clear();
        DEEP_SNEAK_TICKS.clear();
    }

    public interface RandomLike {
        double nextDouble();
        int nextInt(int bound);
    }

    public static boolean tryBowlPanning(ServerPlayer player, ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        if (now < PAN_COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L)) {
            return false;
        }
        PAN_COOLDOWN_UNTIL.put(player.getUUID(), now + 40L);
        long reachId = ((long) (pos.getX() >> 6) << 32) | ((pos.getZ() >> 6) & 0xFFFF_FFFFL);
        PlanetModel activeModel = PlanetRuntime.model();
        RandomLike rng = new RandomLike() {
            @Override
            public double nextDouble() {
                return level.getRandom().nextDouble();
            }

            @Override
            public int nextInt(int bound) {
                return level.getRandom().nextInt(bound);
            }
        };
        Optional<Identifier> found = activeModel != null
                ? rollPanningAt(activeModel, pos.getX(), pos.getZ(), reachId, rng)
                : rollPanning(reachId, new RandomLike() {
            @Override public double nextDouble() { return player.getRandom().nextDouble(); }
            @Override public int nextInt(int bound) { return player.getRandom().nextInt(bound); }
        });
        if (found.isPresent()) {
            Item item = BuiltInRegistries.ITEM.getValue(found.get());
            ItemStack stack = new ItemStack(item, 1);
            if (!player.getInventory().add(stack)) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack);
            }
            ProficiencyBridge.add(player, "surveying", 4, PANNING_SOURCE);
            player.sendOverlayMessage(Component.translatable("msg.tcwildlands.panning.hit", Component.translatable(item.getDescriptionId())));
        } else {
            player.sendOverlayMessage(Component.translatable("msg.tcwildlands.panning.miss"));
        }
        return true;
    }

    /**
     * Tracks 60-tick sneaking on tcwildlands:deep_threshold to travel between Overworld and Deep Earth (10 §5.16.2, §9.5).
     */
    public static void tickDeepThresholdSneak(ServerPlayer player) {
        if (!PlanetRuntime.isDeepEarthActive()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos under = player.blockPosition().below();
        Identifier underId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(under).getBlock());
        boolean onThreshold = "tcwildlands".equals(underId.getNamespace()) && "deep_threshold".equals(underId.getPath());
        if (!onThreshold || !player.isShiftKeyDown()) {
            DEEP_SNEAK_TICKS.remove(player.getUUID());
            return;
        }
        int needed = WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.DEEP_THRESHOLD_SNEAK_TICKS.getAsInt() : 60;
        int ticks = DEEP_SNEAK_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (ticks < needed) {
            return;
        }
        DEEP_SNEAK_TICKS.remove(player.getUUID());
        MinecraftServer server = level.getServer();
        if (level.dimension() == Level.OVERWORLD) {
            ServerLevel deep = server.getLevel(Level.NETHER);
            if (deep != null) {
                int tx = Math.floorDiv(player.getBlockX(), 8);
                int tz = Math.floorDiv(player.getBlockZ(), 8);
                int ty = 120;
                ensureSafePlatform(deep, new BlockPos(tx, ty, tz));
                player.teleportTo(deep, tx + 0.5, ty + 1.0, tz + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
            }
        } else if (level.dimension() == Level.NETHER) {
            ServerLevel overworld = server.overworld();
            int tx = PlanetCoordinates.wrapX(player.getBlockX() * 8);
            int tz = PlanetCoordinates.clampZ(player.getBlockZ() * 8);
            int ty = Math.max(PlanetCoordinates.SEA_LEVEL + 1, TcWorldApis.planet().surfaceY(tx, tz) + 1);
            player.teleportTo(overworld, tx + 0.5, ty, tz + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
        }
    }

    private static void ensureSafePlatform(ServerLevel deep, BlockPos center) {
        var basalt = BuiltInRegistries.BLOCK.getValue(WildlandsCatalog.wl("basalt_rock")).defaultBlockState();
        var threshold = BuiltInRegistries.BLOCK.getValue(WildlandsCatalog.wl("deep_threshold")).defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos base = center.offset(dx, 0, dz);
                deep.setBlockAndUpdate(base, (dx == 0 && dz == 0) ? threshold : basalt);
                deep.setBlockAndUpdate(base.above(), Blocks.AIR.defaultBlockState());
                deep.setBlockAndUpdate(base.above(2), Blocks.AIR.defaultBlockState());
            }
        }
    }
}
