package com.nightcity.ecology;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

public final class TEcologyEvents {
    private TEcologyEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcecology")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcecology.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcecology.status.details"), false);
        return 1;
    }
}



