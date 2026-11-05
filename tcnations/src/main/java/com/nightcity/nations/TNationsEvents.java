package com.nightcity.nations;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TNationsEvents {
    private TNationsEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcnations")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcnations.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcnations.status.details"), false);
        return 1;
    }
}
