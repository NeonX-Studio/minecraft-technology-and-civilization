package com.nightcity.atlas;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TAtlasEvents {
    private TAtlasEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcatlas")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcatlas.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcatlas.status.details"), false);
        return 1;
    }
}
