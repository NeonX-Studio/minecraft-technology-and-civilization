package com.nightcity.technology;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

public final class TTechnologyEvents {
    private TTechnologyEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tctechnology")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
        event.getDispatcher().register(Commands.literal("tctechnology")
                .then(Commands.literal("research").executes(context -> addResearch(context.getSource()))));
    }

    private static int sendStatus(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tctechnology.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tctechnology.status.details"), false);
        TechnologySavedData data = TechnologySavedData.get(source.getServer().overworld());
        source.sendSuccess(() -> Component.translatable("command.tctechnology.research", data.researchPoints()), false);
        return 1;
    }
    private static int addResearch(net.minecraft.commands.CommandSourceStack source) {
        TechnologySavedData data = TechnologySavedData.get(source.getServer().overworld());
        data.addResearch(1);
        source.sendSuccess(() -> Component.translatable("command.tctechnology.research.added", data.researchPoints()), false);
        return 1;
    }
}



