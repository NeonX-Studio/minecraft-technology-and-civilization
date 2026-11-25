package com.nightcity.wildlands.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.wildlands.planet.PeriodicNoise;
import com.nightcity.wildlands.planet.WildlandsCatalog;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Deep Earth ChunkGenerator (codec tcwildlands:deep_earth) replacing minecraft:the_nether (10 §5.16, VO-WG-09).
 * Generates 5 realistic crust-mantle depth zones across Y = 0..255 with zero nether fortresses, bastions, or nether ores.
 */
public final class TcDeepEarthChunkGenerator extends ChunkGenerator {
    public static final MapCodec<TcDeepEarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, TcDeepEarthChunkGenerator::new));

    public static final int MIN_Y = 0;
    public static final int GEN_DEPTH = 256;

    private final Map<Identifier, BlockState> stateCache = new ConcurrentHashMap<>();

    public TcDeepEarthChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return 32;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    public static Identifier synthesizeDeepBlock(long seed, int x, int y, int z) {
        if (y <= 0 || y >= 255) {
            return WildlandsCatalog.mc("bedrock");
        }
        double cavern = PeriodicNoise.sample3D(seed ^ 0xDEE9_CA7EL, x * 8, y, z * 8, 256, 32);
        double lith = PeriodicNoise.sample3D(seed ^ 0xDEE9_B0C4L, x * 8, y, z * 8, 512, 48);

        if (y >= 240) {
            return lith > 0.45 ? WildlandsCatalog.wl("eclogite") : (lith > -0.1 ? WildlandsCatalog.wl("gneiss") : WildlandsCatalog.mc("granite"));
        }
        if (y >= 160) {
            if (cavern > 0.62) {
                return WildlandsCatalog.mc("air");
            }
            if (lith > 0.55) {
                return WildlandsCatalog.wl("pegmatite");
            }
            return lith > 0.0 ? WildlandsCatalog.mc("granite") : WildlandsCatalog.wl("gneiss");
        }
        if (y >= 80) {
            if (cavern > 0.42) {
                return y < 96 ? WildlandsCatalog.mc("water") : WildlandsCatalog.mc("air");
            }
            if (cavern > 0.36 && lith > 0.3) {
                return WildlandsCatalog.wl("massive_sulfide");
            }
            return lith > 0.2 ? WildlandsCatalog.wl("eclogite") : WildlandsCatalog.wl("gneiss");
        }
        if (y >= 20) {
            if (cavern > 0.38) {
                return y <= 46 ? WildlandsCatalog.mc("lava") : WildlandsCatalog.mc("air");
            }
            return lith > 0.3 ? WildlandsCatalog.wl("peridotite") : (lith > -0.2 ? WildlandsCatalog.wl("gabbro") : WildlandsCatalog.wl("basalt_rock"));
        }
        if (lith > 0.58) {
            return WildlandsCatalog.wl("kimberlite_diamond_ore");
        }
        if (lith > 0.35) {
            return WildlandsCatalog.wl("kimberlite");
        }
        return WildlandsCatalog.wl("peridotite");
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(
            ChunkAccess chunk,
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            BiomeManager biomeManager,
            WorldGenRegion region,
            java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biomes
    ) {
        long seed = 0x5EED_DEE9L;
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int lz = 0; lz < 16; lz++) {
            int wz = baseZ + lz;
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx;
                for (int y = 0; y < GEN_DEPTH; y++) {
                    Identifier id = synthesizeDeepBlock(seed, wx, y, wz);
                    if ("air".equals(id.getPath())) {
                        continue;
                    }
                    mpos.set(lx, y, lz);
                    chunk.setBlockState(mpos, resolve(id), 0);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 96;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] col = new BlockState[GEN_DEPTH];
        for (int y = 0; y < GEN_DEPTH; y++) {
            col[y] = resolve(synthesizeDeepBlock(0x5EED_DEE9L, x, y, z));
        }
        return new NoiseColumn(MIN_Y, col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos, net.minecraft.world.level.levelgen.densityfunction.SamplerContext context) {
        info.add("Wildlands Deep Earth | Y=" + pos.getY());
    }

    private BlockState resolve(Identifier id) {
        return stateCache.computeIfAbsent(id, key -> {
            Block b = BuiltInRegistries.BLOCK.getValue(key);
            return b != Blocks.AIR || "air".equals(key.getPath()) ? b.defaultBlockState() : Blocks.BASALT.defaultBlockState();
        });
    }
}
