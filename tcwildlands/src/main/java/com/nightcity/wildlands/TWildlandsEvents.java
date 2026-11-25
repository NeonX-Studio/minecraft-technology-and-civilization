package com.nightcity.wildlands;

import com.nightcity.tcivilization.TcServerContext;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.ProficiencyBridge;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.PrecipType;
import com.nightcity.tcivilization.api.world.ProspectMethod;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import com.nightcity.tcivilization.api.world.WeatherState;
import com.nightcity.wildlands.command.WildlandsCommands;
import com.nightcity.wildlands.data.WildlandsRegionLayers;
import com.nightcity.wildlands.network.WildlandsPayloads;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
import com.nightcity.wildlands.planet.StartSiteStore;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import com.nightcity.wildlands.prospect.ProspectingService;
import com.nightcity.wildlands.weather.RegionalWeatherEngine;
import com.nightcity.wildlands.worldgen.PlanetWrapManager;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TWildlandsEvents {
    private TWildlandsEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        WildlandsCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        PlanetRuntime.onServerStarting(server);
        TcServerContext ctx = TcServerContext.get(server);
        ctx.regions().pressureTracker().registerContributor(
                WildlandsRegionLayers.createHazardPressureContributor(() -> server.overworld().getOverworldClockTime())
        );
        PlanetModel m = PlanetRuntime.model();
        if (m != null) {
            RegionalWeatherEngine.seedInitialSystems(m.worldSeed());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PlanetRuntime.clear();
        RegionalWeatherEngine.reset();
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == Level.OVERWORLD) {
                PlanetRuntime.onServerStarting(serverLevel.getServer());
                if (PlanetRuntime.isPlanetActive() && !PlanetRuntime.isBorderInitialized()) {
                    PlanetWrapManager.initializeWorldBorderIfDefault(serverLevel);
                    PlanetRuntime.markBorderInitialized();
                }
            } else if (serverLevel.dimension() == Level.NETHER) {
                PlanetRuntime.refreshDeepEarthFlag(serverLevel.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void onCreateWorldSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        PlanetRuntime.onServerStarting(server);
        if (!PlanetRuntime.isPlanetActive()) {
            return;
        }
        BlockPos spawnPos = PlanetRuntime.resolveInitialWorldSpawn(server);
        net.minecraft.world.level.storage.LevelData.RespawnData respawnData =
                net.minecraft.world.level.storage.LevelData.RespawnData.of(serverLevel.dimension(), spawnPos, 0.0F, 0.0F);
        event.getSettings().setSpawn(respawnData);
        PlanetRuntime.syncServerWorldSpawn(server, spawnPos);
        com.mojang.logging.LogUtils.getLogger().info(
                "Selected habitable Wildlands global world spawn at ({}, {}, {})",
                spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()
        );
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean netherOwned = VanillaPolicyService.get().owner(VanillaSystem.NETHER).filter("tcwildlands"::equals).isPresent();
        if (netherOwned && PlanetRuntime.isPlanetActive() && PlanetRuntime.isDeepEarthActive()) {
            event.setCanceled(true);
            boolean showHint = !WildlandsConfig.SERVER_SPEC.isLoaded() || WildlandsConfig.DEEP_IGNITION_HINT.getAsBoolean();
            if (showHint) {
                BlockPos pos = event.getPos();
                for (ServerPlayer player : level.players()) {
                    if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0
                            && ProspectingService.shouldShowPortalIgnitionHint(player.getUUID())) {
                        player.sendSystemMessage(Component.translatable("msg.tcwildlands.portal_disabled"));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTravelDimension(EntityTravelToDimensionEvent event) {
        if (event.getDimension() == Level.NETHER && PlanetRuntime.isPlanetActive() && PlanetRuntime.isDeepEarthActive()) {
            boolean netherOwned = VanillaPolicyService.get().owner(VanillaSystem.NETHER).filter("tcwildlands"::equals).isPresent();
            Entity entity = event.getEntity();
            if (netherOwned && entity.level().getBlockState(entity.blockPosition()).is(Blocks.NETHER_PORTAL)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (level.dimension() == Level.OVERWORLD && PlanetRuntime.isPlanetActive() && PlanetCoordinates.isInMirrorBuffer(pos.getX())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("msg.tcwildlands.wrap_buffer"));
            }
            return;
        }
        if (event.getEntity() instanceof ServerPlayer) {
            ProspectingService.markPlayerPlaced(level, pos);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (level.dimension() == Level.OVERWORLD && PlanetRuntime.isPlanetActive() && PlanetCoordinates.isInMirrorBuffer(pos.getX())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("msg.tcwildlands.wrap_buffer"));
            return;
        }
        if (ProspectingService.isPlayerPlaced(level, pos)) {
            ProspectingService.clearPlayerPlaced(level, pos);
            return;
        }

        BlockState state = level.getBlockState(pos);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (WildlandsCatalog.ALL_ORE_BLOCK_PATHS.contains(blockId.getPath())) {
            ProficiencyBridge.add(player, "mining", 2, blockId);
            PlanetModel m = PlanetRuntime.model();
            if (m != null) {
                for (Deposit d : m.deposits()) {
                    int dx = PlanetCoordinates.wrappedDx(pos.getX(), d.center().getX());
                    int dz = d.center().getZ() - pos.getZ();
                    if (Math.abs(dx) <= d.radiusXZ() && Math.abs(dz) <= d.radiusXZ() && Math.abs(pos.getY() - d.center().getY()) <= d.radiusY()) {
                        TcWorldApis.geo().markDiscovered(d.id(), Civilization.PLAYER_CIV_ID, Optional.of(player), ProspectMethod.MINING);
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState clicked = level.getBlockState(pos);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(clicked.getBlock());

        if ("tcwildlands".equals(blockId.getNamespace()) && "loose_stones".equals(blockId.getPath())) {
            level.removeBlock(pos, false);
            var item = BuiltInRegistries.ITEM.getValue(WildlandsCatalog.wl("loose_rock"));
            int count = 1 + player.getRandom().nextInt(3);
            ItemStack stack = new ItemStack(item, count);
            if (!player.getInventory().add(stack)) {
                net.minecraft.world.level.block.Block.popResource(level, pos.above(), stack);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        ItemStack held = event.getItemStack();
        if (player.isShiftKeyDown() && held.is(ItemTags.PICKAXES)) {
            if (ProspectingService.tryRockTap(player, level, pos, held)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }

        if (held.is(Items.BOWL) && player.isInWater()) {
            String path = blockId.getPath();
            if ("gravel".equals(path) || "sand".equals(path) || "black_sand".equals(path) || path.startsWith("gravel_") || path.startsWith("sand_")) {
                if (ProspectingService.tryBowlPanning(player, level, pos)) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.level().getServer().overworld();
        long clockTime = overworld.getOverworldClockTime();
        CivCalendar.CalendarDate cal = CivCalendar.fromDayTime(clockTime);
        int doy = (int) Math.floorMod(cal.totalDays(), CivCalendar.YEAR_DAYS);
        PacketDistributor.sendToPlayer(player, new WildlandsPayloads.CalendarSyncPayload(
                cal.year(), doy, (byte) cal.seasonIndex(), PlanetRuntime.isPlanetActive(), PlanetRuntime.isDeepEarthActive()
        ));

        PlanetModel m = PlanetRuntime.model();
        if (PlanetRuntime.isPlanetActive() && m != null) {
            Optional<StartSiteStore.Selection> existing = PlanetRuntime.startSite();
            if (existing.isEmpty()) {
                boolean startUiEnabled = !WildlandsConfig.SERVER_SPEC.isLoaded() || WildlandsConfig.START_SITE_ENABLED.getAsBoolean();
                if (startUiEnabled) {
                    if (player.level() == overworld && (player.isInWater() || PlanetRuntime.isSubmergedOrOceanSpawn(m, player.blockPosition()))) {
                        BlockPos safeSpawn = PlanetRuntime.resolveInitialWorldSpawn(player.level().getServer());
                        player.teleportTo(overworld, safeSpawn.getX() + 0.5, safeSpawn.getY(), safeSpawn.getZ() + 0.5,
                                Set.of(), player.getYRot(), player.getXRot(), false);
                    }
                    PacketDistributor.sendToPlayer(player, new WildlandsPayloads.StartSiteOptionsPayload(
                            net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(player.permissions()), StartSiteStore.topFiveCandidates(m)
                    ));
                } else {
                    PlanetRuntime.chooseCandidateStartSite(player.level().getServer(), 0, player.getUUID(), "auto")
                            .ifPresent(sel -> player.teleportTo(overworld, sel.chosenX() + 0.5, sel.chosenY() + 1.0, sel.chosenZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!PlanetRuntime.isPlanetActive()) {
            return;
        }
        boolean localCmd = !WildlandsConfig.SERVER_SPEC.isLoaded() || WildlandsConfig.WEATHER_LOCAL_COMMAND.getAsBoolean();
        if (!localCmd) {
            return;
        }
        var nodes = event.getParseResults().getContext().getNodes();
        if (nodes.size() >= 2 && "weather".equals(nodes.get(0).getNode().getName())) {
            String sub = nodes.get(1).getNode().getName();
            BlockPos pos = BlockPos.containing(event.getParseResults().getContext().getSource().getPosition());
            PrecipType pt = "clear".equals(sub) ? PrecipType.NONE : PrecipType.RAIN;
            int conv = "thunder".equals(sub) ? 2 : 0;
            RegionalWeatherEngine.setWeatherOverride(pos, pt, conv, 6_000L, false);
            event.getParseResults().getContext().getSource().sendSuccess(
                    () -> Component.translatable("command.tcwildlands.weather.set", pt.name(), 6000, false), true
            );
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        long tick = server.getTickCount();
        long clockTime = overworld.getOverworldClockTime();

        if (PlanetRuntime.isPlanetActive()) {
            RegionalWeatherEngine.tickServer(server, overworld, clockTime, tick);

            for (Entity ent : overworld.getAllEntities()) {
                if (Math.abs(ent.getX()) >= PlanetWrapManager.X_MAX - 256 || Math.abs(ent.getZ()) >= PlanetWrapManager.POLAR_Z_LIMIT - 64) {
                    PlanetWrapManager.tickOverworldEntity(overworld, ent);
                }
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ProspectingService.tickDeepThresholdSneak(player);
            if (tick % 20L == 0L && PlanetRuntime.isPlanetActive()) {
                BlockPos p = player.blockPosition();
                WeatherState ws = TcWorldApis.weather().current(player.level(), p);
                float temp = TcWorldApis.climate().temperatureNow(overworld, p);
                int altM = (int) Math.clamp(Math.round(TcWorldApis.planet().altitudeMeters(p.getY())), Short.MIN_VALUE, Short.MAX_VALUE);
                PlanetModel m = PlanetRuntime.model();
                int bIdx = m != null ? m.biomeSpecAt(p.getX(), p.getZ()).index() : 48;
                PacketDistributor.sendToPlayer(player, new WildlandsPayloads.LocalClimatePayload(
                        (short) Math.round(temp * 10.0f),
                        (byte) ws.type().ordinal(),
                        (short) Math.round(ws.precipMmPerHour() * 100.0f),
                        (short) Math.round(ws.windDirDeg()),
                        (short) Math.round(ws.windSpeed() * 10.0f),
                        (short) altM,
                        bIdx,
                        (byte) 0,
                        (byte) 0,
                        (short) 0
                ));
            }
        }
    }
}



