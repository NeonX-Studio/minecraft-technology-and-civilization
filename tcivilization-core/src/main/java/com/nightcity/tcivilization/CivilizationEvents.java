package com.nightcity.tcivilization;

import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.RegionCell;
import com.nightcity.tcivilization.api.RegionGridRegistry;
import com.nightcity.tcivilization.api.TcNotifications;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.command.TcCommands;
import com.nightcity.tcivilization.person.PersonEntity;
import com.nightcity.tcivilization.registry.TcRegistries;
import com.nightcity.tcivilization.sim.TcEventBus;
import com.nightcity.tcivilization.sim.TcProfiler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CivilizationEvents {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        TcThreads.bindMainThread(server.getRunningThread());
        TcServerContext.get(server);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        TcServerContext.shutdown(event.getServer());
        TcThreads.bindMainThread(null);
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD) {
            TcServerContext.get(serverLevel.getServer()).regions().flushAllSync();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.level().getServer().overworld();
        long clockTime = overworld.getOverworldClockTime();
        CivilizationSavedData data = CivilizationSavedData.get(overworld);
        if (!TCivilizationConfig.SPEC.isLoaded() || TCivilizationConfig.AUTO_JOIN_DEFAULT_CIV.getAsBoolean()) {
            data.recordLogin(player.getUUID(), clockTime);
        }
        trackPlayerRegionVisit(player, data);
        player.sendSystemMessage(Component.translatable("command.tcivilization.status"));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long startNanos = System.nanoTime();
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        long clockTime = overworld.getOverworldClockTime();
        CivilizationSavedData data = CivilizationSavedData.get(overworld);
        TcServerContext ctx = TcServerContext.get(server);

        int prevYear = data.calendarState().lastYear();
        boolean wildlandsLoaded = net.neoforged.fml.ModList.get().isLoaded("tcwildlands");
        data.updateCalendar(server, clockTime, wildlandsLoaded, true);
        if (data.calendarState().lastYear() > prevYear) {
            data.memoryStore().compactAnnual(clockTime);
        }
        if (server.getTickCount() % 20 == 0) {
            data.recordSecond(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                trackPlayerRegionVisit(player, data);
            }
        }

        List<BlockPos> playerPositions = server.getPlayerList().getPlayers().stream()
                .map(Entity::blockPosition)
                .toList();
        ctx.scheduler().tickServer(clockTime, playerPositions, rec -> {
            PersonEntity ent = TcRegistries.PERSON_ENTITY.get().create(overworld, EntitySpawnReason.EVENT);
            if (ent != null) {
                BlockPos p = rec.lastPos().pos();
                ent.snapTo(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D, 0.0F, 0.0F);
                overworld.addFreshEntity(ent);
            }
            return ent;
        });

        TcEventBus.global().flushTick();
        TcProfiler.global().recordSample(TCivilization.MOD_ID, System.nanoTime() - startNanos);
        TcProfiler.global().endTickAndMaybeExport();
    }

    @SubscribeEvent
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (TCivilizationConfig.SPEC.isLoaded() && TCivilizationConfig.FRIENDLY_FIRE.getAsBoolean()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer attacker) || attacker.isShiftKeyDown()) {
            return;
        }
        ServerLevel overworld = attacker.level().getServer().overworld();
        CivilizationSavedData data = CivilizationSavedData.get(overworld);
        if (!data.playerCiv().members().containsKey(attacker.getUUID())) {
            return;
        }
        Entity victim = event.getEntity();
        boolean sameCivTarget = (victim instanceof ServerPlayer targetPlayer
                && !targetPlayer.getUUID().equals(attacker.getUUID())
                && data.playerCiv().members().containsKey(targetPlayer.getUUID()))
                || (victim instanceof PersonEntity personEntity
                && Civilization.PLAYER_CIV_ID.equals(personEntity.civId()));
        if (sameCivTarget) {
            event.setCanceled(true);
            attacker.sendOverlayMessage(Component.translatable("chat.tcivilization.friendly_fire"));
        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }
        ServerLevel serverLevel = event.getLevel().getLevel();
        MonsterPolicy policy = VanillaPolicyService.get().effectiveMonsterPolicy(serverLevel);
        if (policy == MonsterPolicy.DISABLED) {
            event.setSpawnCancelled(true);
        } else if (policy == MonsterPolicy.RUINS_ONLY) {
            if (serverLevel.dimension() == Level.OVERWORLD && event.getY() >= 52.0D) {
                event.setSpawnCancelled(true);
            }
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        TcCommands.register(event.getDispatcher());
    }

    private static void trackPlayerRegionVisit(ServerPlayer player, CivilizationSavedData data) {
        String dim = player.level().dimension().identifier().toString();
        BlockPos pos = player.blockPosition();
        RegionGridRegistry.defaults().atBlock(dim, pos.getX(), pos.getZ())
                .ifPresent((RegionCell cell) -> data.recordRegionVisit(Civilization.PLAYER_CIV_ID, cell));
    }
}

