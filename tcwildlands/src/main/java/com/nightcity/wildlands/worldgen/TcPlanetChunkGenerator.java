package com.nightcity.wildlands.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.wildlands.WildlandsConfig;
import com.nightcity.wildlands.block.OreGrade;
import com.nightcity.wildlands.block.WildlandsOreBlock;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetModel;
import com.nightcity.wildlands.planet.PlanetRuntime;
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
import net.minecraft.world.level.WorldGenLevel;
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
import org.jspecify.annotations.Nullable;

/**
 * Planet Overworld ChunkGenerator (codec tcwildlands:planet, Y -512..1024, sea level 63).
 * Synthesizes bicubic L1 terrain + periodic detail + 4 rock forms + 87 legal ore blocks
 * with zero vanilla stone, deepslate, or vanilla ores (10 §5.9, VO-WG-01..08, VO-WG-13).
 */
public final class TcPlanetChunkGenerator extends ChunkGenerator {
    public static final MapCodec<TcPlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, TcPlanetChunkGenerator::new));

    private final Map<Identifier, BlockState> stateCache = new ConcurrentHashMap<>();
    @Nullable
    private volatile PlanetModel boundModel;

    public TcPlanetChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public void bindModel(@Nullable PlanetModel model) {
        this.boundModel = model;
    }

    @Nullable
    private PlanetModel activeModel() {
        PlanetModel m = this.boundModel;
        return m != null ? m : PlanetRuntime.model();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    @Override
    public int getGenDepth() {
        return PlanetChunkSynthesizer.HEIGHT; // 1536
    }

    @Override
    public int getSeaLevel() {
        return PlanetCoordinates.SEA_LEVEL; // 63
    }

    @Override
    public int getMinY() {
        return PlanetCoordinates.Y_MIN; // -512
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
        PlanetModel model = activeModel();
        if (model == null) {
            return CompletableFuture.completedFuture(chunk);
        }
        int maxStep = WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.PLANET_MAX_STEP.getAsInt() : 3;
        PlanetChunkSynthesizer.SynthesizedChunk syn = PlanetChunkSynthesizer.synthesizeChunk(
                model, chunk.getPos().x(), chunk.getPos().z(), maxStep
        );

        int chunkMinY = Math.max(chunk.getMinY(), PlanetChunkSynthesizer.MIN_Y);
        int chunkMaxY = Math.min(chunk.getMaxY(), PlanetChunkSynthesizer.MAX_Y_INCLUSIVE);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int col2d = lz * 16 + lx;
                int topSolid = Math.max(syn.surfaceY()[col2d], syn.waterY()[col2d] == Integer.MIN_VALUE ? syn.surfaceY()[col2d] : syn.waterY()[col2d]);
                int upperBound = Math.min(chunkMaxY, topSolid + 1);
                for (int y = chunkMinY; y <= upperBound; y++) {
                    Identifier id = syn.blockAt(lx, y, lz);
                    if ("air".equals(id.getPath())) {
                        continue;
                    }
                    byte gradeNum = syn.gradeAt(lx, y, lz);
                    BlockState state = resolveBlockState(id, gradeNum);
                    if (!state.isAir()) {
                        mpos.set(lx, y, lz);
                        chunk.setBlockState(mpos, state, 0);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        PlanetModel model = activeModel();
        if (model == null) {
            return PlanetCoordinates.SEA_LEVEL + 1;
        }
        int maxStep = WildlandsConfig.SERVER_SPEC.isLoaded() ? WildlandsConfig.PLANET_MAX_STEP.getAsInt() : 3;
        int surfY = PlanetChunkSynthesizer.computeSurfaceY(model, x, z, maxStep);
        if (type == Heightmap.Types.OCEAN_FLOOR_WG || type == Heightmap.Types.OCEAN_FLOOR) {
            return surfY + 1;
        }
        int waterY = model.sampleWaterSurfaceY(x, z);
        return Math.max(surfY, waterY == Integer.MIN_VALUE ? surfY : waterY) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        int minY = height.getMinY();
        int len = height.getHeight();
        BlockState[] states = new BlockState[len];
        PlanetModel model = activeModel();
        int surfY = model != null ? PlanetChunkSynthesizer.computeSurfaceY(model, x, z, 3) : PlanetCoordinates.SEA_LEVEL + 1;
        BlockState gneiss = resolveBlockState(Identifier.fromNamespaceAndPath("tcwildlands", "gneiss"), (byte) 0);
        for (int i = 0; i < len; i++) {
            int y = minY + i;
            if (y <= minY + 4) {
                states[i] = Blocks.BEDROCK.defaultBlockState();
            } else if (y <= surfY) {
                states[i] = gneiss;
            } else if (y <= PlanetCoordinates.SEA_LEVEL) {
                states[i] = Blocks.WATER.defaultBlockState();
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos, net.minecraft.world.level.levelgen.densityfunction.SamplerContext context) {
        PlanetModel model = activeModel();
        if (model != null) {
            var ll = PlanetCoordinates.latLon(pos.getX(), pos.getZ());
            info.add(String.format("Wildlands Planet | lat=%.2f lon=%.2f alt=%.0fm", ll.latDeg(), ll.lonDeg(), PlanetCoordinates.altitudeMeters(pos.getY())));
        }
    }

    private BlockState resolveBlockState(Identifier id, byte gradeNum) {
        BlockState base = stateCache.computeIfAbsent(id, key -> {
            if ("tcwildlands".equals(key.getNamespace()) && "brine".equals(key.getPath())) {
                return Blocks.WATER.defaultBlockState();
            }
            Block b = BuiltInRegistries.BLOCK.getValue(key);
            if (b == Blocks.AIR && !"air".equals(key.getPath())) {
                Block fallback = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("tcwildlands", "gneiss"));
                return fallback != Blocks.AIR ? fallback.defaultBlockState() : Blocks.GRANITE.defaultBlockState();
            }
            return b.defaultBlockState();
        });
        if (gradeNum > 0 && base.hasProperty(WildlandsOreBlock.GRADE)) {
            OreGrade grade = switch (gradeNum) {
                case 1 -> OreGrade.POOR;
                case 3 -> OreGrade.RICH;
                default -> OreGrade.NORMAL;
            };
            return base.setValue(WildlandsOreBlock.GRADE, grade);
        }
        return base;
    }
}
