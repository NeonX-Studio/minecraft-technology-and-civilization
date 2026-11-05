package com.nightcity.settlements;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

public final class TSettlementsEvents {
    private TSettlementsEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcsettlements")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcsettlements.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcsettlements.status.details"), false);
        return 1;
    }
}



