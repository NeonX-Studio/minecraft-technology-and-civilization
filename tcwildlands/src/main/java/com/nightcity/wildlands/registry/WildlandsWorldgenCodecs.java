package com.nightcity.wildlands.registry;

import com.mojang.serialization.MapCodec;
import com.nightcity.wildlands.worldgen.TcDeepBiomeSource;
import com.nightcity.wildlands.worldgen.TcDeepEarthChunkGenerator;
import com.nightcity.wildlands.worldgen.TcPlanetBiomeSource;
import com.nightcity.wildlands.worldgen.TcPlanetChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers ChunkGenerator and BiomeSource codecs for tcwildlands:planet and tcwildlands:deep_earth.
 */
public final class WildlandsWorldgenCodecs {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, WildlandsRegistries.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, WildlandsRegistries.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<TcPlanetChunkGenerator>> PLANET_GENERATOR =
            CHUNK_GENERATORS.register("planet", () -> TcPlanetChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<TcDeepEarthChunkGenerator>> DEEP_EARTH_GENERATOR =
            CHUNK_GENERATORS.register("deep_earth", () -> TcDeepEarthChunkGenerator.CODEC);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<TcPlanetBiomeSource>> PLANET_BIOME_SOURCE =
            BIOME_SOURCES.register("planet", () -> TcPlanetBiomeSource.CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<TcDeepBiomeSource>> DEEP_EARTH_BIOME_SOURCE =
            BIOME_SOURCES.register("deep_earth", () -> TcDeepBiomeSource.CODEC);

    private WildlandsWorldgenCodecs() {
    }

    public static void register(IEventBus modBus) {
        CHUNK_GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
    }
}
