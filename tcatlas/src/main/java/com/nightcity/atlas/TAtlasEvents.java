package com.nightcity.atlas;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.nightcity.atlas.compat.wildlands.WildlandsTerrainBridge;
import com.nightcity.atlas.data.AtlasSavedData;
import com.nightcity.atlas.map.ChunkTerrainSummary;
import com.nightcity.atlas.map.SurveyAtlasStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TAtlasEvents {
    private TAtlasEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcatlas")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource())))
                .then(Commands.literal("hud")
                        .then(Commands.literal("cycle").executes(context -> {
                            var next = TAtlas.hud().cycleLevel();
                            context.getSource().sendSuccess(() -> Component.literal("[tcatlas] HUD: " + next.name()), false);
                            return 1;
                        })))
                .then(Commands.literal("debug")
                        .then(Commands.literal("minimap")
                                .executes(ctx -> {
                                    boolean next = !AtlasConfig.isMinimapDebugEnabled();
                                    AtlasConfig.setRuntimeDebugMinimapOverride(next);
                                    ctx.getSource().sendSuccess(() -> Component.literal("[tcatlas] Debug minimap: " + (next ? "ON" : "OFF")), false);
                                    return 1;
                                })
                                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                                    boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                    AtlasConfig.setRuntimeDebugMinimapOverride(val);
                                    ctx.getSource().sendSuccess(() -> Component.literal("[tcatlas] Debug minimap: " + (val ? "ON" : "OFF")), false);
                                    return 1;
                                }))))
                .then(Commands.literal("tier")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 10)).executes(ctx -> {
                            int tier = IntegerArgumentType.getInteger(ctx, "level");
                            TAtlas.surveyStore().setStandaloneTier(tier);
                            if (ctx.getSource().getPlayer() != null) {
                                ServerPlayer sp = ctx.getSource().getPlayer();
                                long nowSec = sp.level().getGameTime() / 20L;
                                long day = sp.level().getOverworldClockTime() / 24000L;
                                boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(sp.level().dimension().identifier());
                                TAtlas.waypoints().upgradeMemoryMarksIfEligible(sp.getUUID(), tier, day, nowSec, wrapX);
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("[tcatlas] Cartography tier set to T" + tier), true);
                            return 1;
                        }))));
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        TAtlas.surveyStore().setWorldSeed(overworld.getSeed());
        if (AtlasConfig.SERVER_SPEC.isLoaded()) {
            TAtlas.surveyStore().setMemoryDecayConfig(AtlasConfig.MEMORY_DECAY.get(), AtlasConfig.DECAY_SPEED.get());
            TAtlas.waypoints().setWaypointHardCap(AtlasConfig.WAYPOINT_HARD_CAP.get());
            TAtlas.waypoints().setMaxDeathPoints(AtlasConfig.DEATH_POINT_COUNT.get());
        }
        AtlasSavedData saved = AtlasSavedData.get(overworld);
        // VO-UI-01: First world load sets minecraft:locator_bar = false when replace_locator_bar = true (30 §6)
        boolean replaceLocator = !AtlasConfig.SERVER_SPEC.isLoaded() || AtlasConfig.REPLACE_LOCATOR_BAR.get();
        if (replaceLocator && !saved.isLocatorBarRuleApplied()) {
            overworld.getGameRules().set(GameRules.LOCATOR_BAR, false, server);
            saved.markLocatorBarRuleApplied();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel overworld = server.overworld();
        AtlasSavedData saved = AtlasSavedData.get(overworld);
        long nowSeconds = overworld.getGameTime() / 20L;
        TAtlas.waypoints().pruneExpiredPings(nowSeconds);

        boolean hideCoords = AtlasConfig.SERVER_SPEC.isLoaded() && AtlasConfig.HIDE_VANILLA_COORDINATES.get();
        int tier = TAtlas.surveyStore().standaloneTier();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            saved.trackPlayer(player.getUUID());
            ServerLevel level = player.level();
            Identifier dim = level.dimension().identifier();
            boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
            recordVisibleChunks(player, level, dim, nowSeconds, wrapX);
            TAtlas.waypoints().checkAndHandleArrival(player.getUUID(), dim, player.getX(), player.getZ(), 8.0, wrapX);

            // VO-UI-02: Per-player entity event 22/23 for F3 coordinates when hide_vanilla_coordinates = true
            if (hideCoords) {
                boolean isOp = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
                boolean exempt = player.isCreative() || player.isSpectator() || isOp || tier >= 10;
                level.broadcastEntityEvent(player, (byte) (exempt ? 23 : 22));
            }
        }
        saved.setDirty();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.level();
        Identifier dim = level.dimension().identifier();
        boolean wrapX = WildlandsTerrainBridge.isEastWestWrapActive(dim);
        long currentDay = level.getOverworldClockTime() / 24000L;
        TAtlas.waypoints().recordDeathPoint(player.getUUID(), dim, player.blockPosition(), 16, currentDay, wrapX);
        AtlasSavedData.get(level.getServer().overworld()).setDirty();
    }

    private static void recordVisibleChunks(ServerPlayer player, ServerLevel level, Identifier dim, long nowSeconds, boolean wrapX) {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        boolean underground = level.getBrightness(LightLayer.SKY, eye) < 4 && !level.canSeeSky(eye);
        int rSee = underground ? 1 : 3;
        int pcx = player.chunkPosition().x();
        int pcz = player.chunkPosition().z();
        SurveyAtlasStore store = TAtlas.surveyStore();

        for (int dz = -rSee; dz <= rSee; dz++) {
            for (int dx = -rSee; dx <= rSee; dx++) {
                if (dx * dx + dz * dz > rSee * rSee) {
                    continue;
                }
                int cx = pcx + dx;
                int cz = pcz + dz;
                store.recordPersonalSighting(player.getUUID(), dim, cx, cz, nowSeconds, wrapX);
                int canonicalCx = store.canonicalChunkX(dim, cx, wrapX);
                if (dx == 0 && dz == 0) {
                    ChunkAccess loaded = level.getChunkSource().getChunk(canonicalCx, cz, ChunkStatus.FULL, false);
                    if (loaded != null) {
                        store.putSummary(dim, canonicalCx, cz, ChunkTerrainSummary.fromLoadedChunk(level, loaded), wrapX);
                    }
                }
            }
        }
    }

    private static int sendStatus(CommandSourceStack source) {
        int tier = TAtlas.surveyStore().standaloneTier();
        int known = TAtlas.surveyStore().knownArchiveChunkCount(SurveyAtlasStore.OVERWORLD_ID);
        int wps = TAtlas.waypoints().allWaypointsSnapshot().size();
        source.sendSuccess(() -> Component.translatable("command.tcatlas.status"), false);
        source.sendSuccess(() -> Component.literal("[tcatlas] Tier=T" + tier + " | ArchiveChunks=" + known + " | Waypoints=" + wps
                + " | WildlandsWrap=" + WildlandsTerrainBridge.isEastWestWrapActive(SurveyAtlasStore.OVERWORLD_ID)), false);
        return 1;
    }
}
