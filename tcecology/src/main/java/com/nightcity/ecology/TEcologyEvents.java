package com.nightcity.ecology;

import com.nightcity.ecology.spawn.BiomeWildlifeTable;
import com.nightcity.ecology.spawn.EcologySpawnManager;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class TEcologyEvents {
    private TEcologyEvents() {
    }

    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        EcologySpawnManager.onPotentialSpawns(event);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        EcologySpawnManager.onFinalizeSpawn(event);
    }

    @SubscribeEvent
    public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
        EcologySpawnManager.onPlayerSpawnPhantoms(event);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcecology")
                .then(Commands.literal("status").executes(context -> sendStatus(context.getSource()))));
    }

    private static int sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.tcecology.status"), false);
        ServerLevel level = source.getLevel();
        VanillaPolicyService policy = VanillaPolicyService.get();
        String wildlifeOwner = policy.owner(VanillaSystem.WILDLIFE_SPAWNS).orElse("vanilla");
        MonsterPolicy monsterPolicy = policy.effectiveMonsterPolicy(level);
        BlockPos pos = BlockPos.containing(source.getPosition());
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Identifier biomeId = biomeHolder.unwrapKey().map(k -> k.identifier()).orElse(Identifier.withDefaultNamespace("plains"));
        Biome.ClimateSettings climate = biomeHolder.value().getModifiedClimateSettings();
        BiomeWildlifeTable.BiomeEnvelopeProfile prof = BiomeWildlifeTable.classifyBiome(
                biomeId, climate.temperature(), climate.downfall()
        );
        List<BiomeWildlifeTable.ResolvedWildlifeSpawn> localSpawns =
                BiomeWildlifeTable.resolveSpawns(prof, EcologyConfig.isRelictSpeciesEnabled());

        String summary = String.format(
                Locale.ROOT,
                "Ecology P1: owner=%s, replaceWildlife=%s, species=%d, localBiome=%s (%.1fC, %.0fmm) -> %d species, monsterPolicy=%s, blockedMonsters=%d",
                wildlifeOwner,
                EcologyConfig.isReplaceVanillaWildlife(),
                BiomeWildlifeTable.allSpecies().size(),
                biomeId,
                prof.tempC(),
                prof.precipMm(),
                localSpawns.size(),
                monsterPolicy,
                EcologySpawnManager.blockedOutOfZoneMonsters()
        );
        source.sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }
}



