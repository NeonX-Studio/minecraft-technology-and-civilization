package com.nightcity.echoes;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

public final class TEchoesEvents {
    private TEchoesEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcechoes")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcechoes.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tcechoes.status.details"), false);
        return 1;
    }
}



