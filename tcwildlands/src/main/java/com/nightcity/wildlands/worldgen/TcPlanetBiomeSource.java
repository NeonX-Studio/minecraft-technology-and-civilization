package com.nightcity.wildlands.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
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
 * Planet BiomeSource (codec tcwildlands:planet) returning exclusively tcwildlands:* biomes
 * with seamless east-west periodicity at x = ±81,920 (10 §5.8, §5.9.1, VO-WG-07).
 */
public final class TcPlanetBiomeSource extends BiomeSource {
    public static final MapCodec<TcPlanetBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, TcPlanetBiomeSource::new));

    private final HolderGetter<Biome> biomeGetter;
    private final Map<Identifier, Holder<Biome>> resolvedBiomes = new HashMap<>();
    private final List<Holder<Biome>> surfaceHolders;
    private final Holder<Biome> fallbackHolder;

    public TcPlanetBiomeSource(HolderGetter<Biome> biomeGetter) {
        this.biomeGetter = biomeGetter;
        List<Holder<Biome>> list = new ArrayList<>(64);
        for (int i = 0; i < 64; i++) {
            WildlandsCatalog.BiomeSpec spec = WildlandsCatalog.BIOMES.get(i);
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, spec.id());
            Optional<Holder.Reference<Biome>> ref = biomeGetter.get(key);
            if (ref.isPresent()) {
                resolvedBiomes.put(spec.id(), ref.get());
                list.add(ref.get());
            }
        }
        this.fallbackHolder = !list.isEmpty()
                ? list.getFirst()
                : biomeGetter.getOrThrow(Biomes.PLAINS);
        this.surfaceHolders = list.isEmpty() ? List.of(this.fallbackHolder) : List.copyOf(list);
    }

    public HolderGetter<Biome> biomeGetter() {
        return biomeGetter;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return surfaceHolders.stream();
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return this::resolveNoiseBiome;
    }

    public Holder<Biome> resolveNoiseBiome(int quartX, int quartY, int quartZ) {
        int blockX = PlanetCoordinates.wrapX(QuartPos.toBlock(quartX));
        int blockZ = PlanetCoordinates.clampZ(QuartPos.toBlock(quartZ));
        PlanetModel model = PlanetRuntime.model();
        if (model != null) {
            WildlandsCatalog.BiomeSpec spec = model.biomeSpecAt(blockX, blockZ);
            Holder<Biome> h = resolvedBiomes.get(spec.id());
            if (h != null) {
                return h;
            }
        }
        // Deterministic latitude fallback before ServerStarting if queried during early bootstrap
        double lat = Math.abs(PlanetCoordinates.latLon(blockX, blockZ).latDeg());
        Identifier targetId;
        if (lat > 72.0) targetId = WildlandsCatalog.wl("polar_tundra");
        else if (lat > 52.0) targetId = WildlandsCatalog.wl("boreal_forest");
        else if (lat > 28.0) targetId = WildlandsCatalog.wl("temperate_broadleaf_forest");
        else targetId = WildlandsCatalog.wl("tropical_savanna");
        return resolvedBiomes.getOrDefault(targetId, fallbackHolder);
    }
}
