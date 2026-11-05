package com.nightcity.tcivilization;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

public final class CivilizationEvents {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CivilizationSavedData data = CivilizationSavedData.get((ServerLevel) player.level());
        data.recordLogin();
        player.sendSystemMessage(Component.translatable("command.tcivilization.status"));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        CivilizationSavedData.get(server.overworld()).recordSecond(server);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tcivilization")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private int sendStatus(CommandSourceStack source) {
        CivilizationSavedData data = CivilizationSavedData.get(source.getServer().overworld());
        source.sendSuccess(() -> Component.translatable("command.tcivilization.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcivilization.status.ticks", data.worldTicks()), false);
        source.sendSuccess(() -> Component.translatable("command.tcivilization.status.logins", data.loginEvents()), false);
        source.sendSuccess(() -> Component.translatable("command.tcivilization.status.peak", data.peakPlayers()), false);
        return 1;
    }
}
