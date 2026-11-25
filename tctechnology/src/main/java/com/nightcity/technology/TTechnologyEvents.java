package com.nightcity.technology;

import com.nightcity.technology.data.TechDefinitionLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TTechnologyEvents {
    private TTechnologyEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "tech_definitions"), TTechnology.loader());
        event.addListener(Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "recipe_gates"), TTechnology.recipeGates());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tctechnology")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource())))
                .then(Commands.literal("research").executes(context -> addResearch(context.getSource())))
                .then(Commands.literal("selftest").executes(context -> runSelfTest(context.getSource()))));

        event.getDispatcher().register(Commands.literal("tcdebug")
                .then(Commands.literal("selftest")
                        .then(Commands.literal("tech").executes(context -> runSelfTest(context.getSource())))));
    }

    private static int sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tctechnology.status"), false);
        source.sendSuccess(() -> Component.translatable("command.tctechnology.status.details"), false);
        TechnologySavedData data = TechnologySavedData.get(source.getServer().overworld());
        source.sendSuccess(() -> Component.translatable("command.tctechnology.research", data.researchPoints()), false);
        return 1;
    }

    private static int addResearch(CommandSourceStack source) {
        TechnologySavedData data = TechnologySavedData.get(source.getServer().overworld());
        data.addResearch(1);
        source.sendSuccess(() -> Component.translatable("command.tctechnology.research.added", data.researchPoints()), false);
        return 1;
    }

    public static int runSelfTest(CommandSourceStack source) {
        TechDefinitionLoader.TechGraphSnapshot snap = TTechnology.loader().snapshot();
        if (snap.selfTestPassed()) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.tctechnology.selftest.pass",
                    snap.definitions().size(),
                    snap.fallbackCount()
            ), false);
            return 1;
        }
        source.sendFailure(Component.translatable(
                "commands.tctechnology.selftest.fail",
                snap.blockingErrorCount()
        ));
        return 0;
    }
}



