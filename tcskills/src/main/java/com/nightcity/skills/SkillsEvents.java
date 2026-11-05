package com.nightcity.skills;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class SkillsEvents {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillsSavedData.get((ServerLevel) player.level()).profile(player.getUUID());
            player.sendSystemMessage(Component.translatable("command.tcskills.login"));
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tcskills")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource())))
                .then(Commands.literal("train")
                        .then(Commands.argument("skill", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(context -> train(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "skill"), 10))))
                .then(Commands.literal("learn")
                        .then(Commands.argument("talent", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(context -> learn(context.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(context, "talent"))))));
    }

    private int sendStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.tcskills.player_only"));
            return 0;
        }
        PlayerSkillsProfile profile = SkillsApi.getProfile((ServerLevel) player.level(), player.getUUID());
        source.sendSuccess(() -> Component.translatable("command.tcskills.status", profile.growthLevel(), profile.totalProficiency(), profile.skillPoints()), false);
        source.sendSuccess(() -> Component.translatable("command.tcskills.skills", profile.skills()), false);
        source.sendSuccess(() -> Component.translatable("command.tcskills.talents", profile.talents()), false);
        source.sendSuccess(() -> Component.translatable("command.tcskills.bonuses", formatBonuses(player, profile)), false);
        return 1;
    }

    private int train(CommandSourceStack source, String skill, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.tcskills.player_only"));
            return 0;
        }
        SkillsSavedData.GrowthResult result = SkillsApi.addProficiency((ServerLevel) player.level(), player.getUUID(), skill, amount);
        PlayerSkillsProfile profile = result.profile();
        source.sendSuccess(() -> Component.translatable("command.tcskills.skill_up", skill, profile.skillLevel(skill)), false);
        if (result.leveledUp()) {
            source.sendSuccess(() -> Component.translatable("command.tcskills.level_up", profile.growthLevel()), false);
        }
        return 1;
    }

    private int learn(CommandSourceStack source, String talent) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.tcskills.player_only"));
            return 0;
        }
        SkillsSavedData.TalentResult result = SkillsSavedData.get((ServerLevel) player.level()).learnTalent(player.getUUID(), talent);
        if (result == SkillsSavedData.TalentResult.LEARNED) {
            source.sendSuccess(() -> Component.translatable("command.tcskills.learned", talent), false);
        } else if (result == SkillsSavedData.TalentResult.ALREADY_KNOWN) {
            source.sendFailure(Component.translatable("command.tcskills.already_known", talent));
        } else {
            source.sendFailure(Component.translatable("command.tcskills.no_points"));
        }
        return result == SkillsSavedData.TalentResult.LEARNED ? 1 : 0;
    }

    private String formatBonuses(ServerPlayer player, PlayerSkillsProfile profile) {
        if (profile.skills().isEmpty()) {
            return "{}";
        }
        return profile.skills().keySet().stream()
                .sorted()
                .map(skill -> skill + " x" + String.format(java.util.Locale.ROOT, "%.2f", SkillsApi.getProficiencyMultiplier((ServerLevel) player.level(), player.getUUID(), skill)))
                .toList()
                .toString();
    }
}

