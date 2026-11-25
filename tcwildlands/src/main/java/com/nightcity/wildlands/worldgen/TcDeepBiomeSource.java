package com.nightcity.wildlands.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.wildlands.planet.PeriodicNoise;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * Deep Earth BiomeSource (codec tcwildlands:deep_earth) assigning the 5 realistic deep-crust/upper-mantle
 * biomes across Y 0..256 (10 §5.16.3, VO-WG-09, VO-SP-10).
 */
public final class TcDeepBiomeSource extends BiomeSource {
    public static final MapCodec<TcDeepBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, TcDeepBiomeSource::new));

    private final HolderGetter<Biome> biomeGetter;
    private final Map<Identifier, Holder<Biome>> deepBiomes = new HashMap<>();
    private final List<Holder<Biome>> possible;
    private final Holder<Biome> fallback;

    public TcDeepBiomeSource(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        List<Holder<Biome>> list = new ArrayList<>(5);
        for (int i = 64; i < 69; i++) {
            WildlandsCatalog.BiomeSpec spec = WildlandsCatalog.BIOMES.get(i);
            Optional<Holder.Reference<Biome>> ref = biomeGetter.get(ResourceKey.create(Registries.BIOME, spec.id()));
            if (ref.isPresent()) {
                deepBiomes.put(spec.id(), ref.get());
                list.add(ref.get());
            }
        }
        this.fallback = !list.isEmpty() ? list.getFirst() : biomeGetter.getOrThrow(Biomes.BASALT_DELTAS);
        this.possible = list.isEmpty() ? List.of(this.fallback) : List.copyOf(list);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return possible.stream();
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return this::resolveNoiseBiome;
    }

    public Holder<Biome> resolveNoiseBiome(int quartX, int quartY, int quartZ) {
        int x = QuartPos.toBlock(quartX);
        int y = QuartPos.toBlock(quartY);
        int z = QuartPos.toBlock(quartZ);
        double n = PeriodicNoise.sample2D(0xDEE9_EA74L, x * 8, z * 8, 2048);

        Identifier id;
        if (y >= 160) {
            id = WildlandsCatalog.wl("deep_granite_batholith");
        } else if (y >= 80) {
            id = WildlandsCatalog.wl("deep_hydrothermal_caverns");
        } else if (y >= 24) {
            id = n > 0.35 ? WildlandsCatalog.wl("deep_mantle_plume") : WildlandsCatalog.wl("deep_magma_chambers");
        } else {
            id = n < -0.25 ? WildlandsCatalog.wl("deep_kimberlite_roots") : WildlandsCatalog.wl("deep_mantle_plume");
        }
        return deepBiomes.getOrDefault(id, fallback);
    }
}
