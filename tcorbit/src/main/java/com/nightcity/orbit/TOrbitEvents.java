package com.nightcity.orbit;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TOrbitEvents {
    private TOrbitEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcorbit")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcorbit.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcorbit.status.details"), false);
        return 1;
    }
}
