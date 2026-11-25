package com.nightcity.nations.seed;

import com.nightcity.nations.NationsConfig;
import com.nightcity.nations.TNations;
import com.nightcity.nations.api.EconomyCategory;
import com.nightcity.nations.api.Government;
import com.nightcity.nations.api.NationState;
import com.nightcity.nations.api.NationStatus;
import com.nightcity.nations.api.OpinionModifier;
import com.nightcity.nations.api.Personality;
import com.nightcity.nations.api.RelationRecord;
import com.nightcity.nations.api.event.NationsBorderChangedEvent;
import com.nightcity.nations.api.event.NationsCivFoundedEvent;
import com.nightcity.nations.compat.tcwildlands.TcwildlandsBridge;
import com.nightcity.nations.data.InfluenceLayerData;
import com.nightcity.nations.data.NationsInfluenceLayer;
import com.nightcity.nations.data.NationsSavedData;
import com.nightcity.tcivilization.RegionCoordinates;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.CivilizationApi;
import com.nightcity.tcivilization.api.RegionApi;
import com.nightcity.tcivilization.api.RegionHandle;
import com.nightcity.tcivilization.api.SettlementRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Deterministic P1 Step-N AI civilization seeder (23 §5.1, §5.2, §5.12.1, §6.1).
 * Writes initial distribution only; does not execute L3 AI decision loops.
 */
public final class NationSeeder {
    public static final long NATIONS_SEED_SALT = 0x4E4154494F4E53L; // "NATIONS"
    public static final ResourceKey<Level> OVERWORLD_DIM = ResourceKey.create(
            Registries.DIMENSION, Identifier.withDefaultNamespace("overworld")
    );
    public static final Identifier HISTORIC_FEUD_ID = Identifier.fromNamespaceAndPath(TNations.MOD_ID, "historic_feud");
    public static final Identifier HISTORIC_ALLIANCE_ID = Identifier.fromNamespaceAndPath(TNations.MOD_ID, "historic_alliance");

    private static final List<Identifier> BUILTIN_CULTURES = List.of(
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/river_valley"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/steppe"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/woodland"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/maritime"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/highland"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/oasis"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/monsoon"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "culture/boreal")
    );

    private static final List<Identifier> INITIAL_AGENDAS = List.of(
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "forest_keepers"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "merchant_princes"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "honor_bound"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "warrior_code"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "scholars"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "devout"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "expansionist"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "isolationist"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "seafarers"),
            Identifier.fromNamespaceAndPath(TNations.MOD_ID, "herders")
    );

    private static final String[] NAME_PREFIXES = {
            "Ash", "Val", "Kor", "Sol", "Mor", "Thal", "Kar", "Bel",
            "Nar", "Or", "Zan", "El", "Dar", "Mir", "Tor", "Fen"
    };
    private static final String[] NAME_SUFFIXES = {
            "ia", "on", "ar", "um", "en", "is", "eth", "or",
            "gard", "heim", "mark", "vale"
    };

    @FunctionalInterface
    public interface HabitabilityOracle {
        double habitability(int regionX, int regionZ);
    }

    public record NationBlueprint(
            int slotIndex,
            int echoesPolityId,
            String name,
            String capitalName,
            Identifier culture,
            Government government,
            Personality personality,
            Identifier agenda,
            int eraIndex,
            double population,
            double habitability,
            int regionX,
            int regionZ,
            long capitalRegionKey,
            BlockPos capitalPos,
            List<Long> holySites,
            boolean fromEchoes
    ) {}

    public record SeedPlan(
            long worldSeed,
            String source,
            int breathingRadiusBlocks,
            List<NationBlueprint> blueprints,
            Map<Long, EchoesHistorySnapshot.HistoricRelation> echoesRelations
    ) {}

    public record SeedResult(
            SeedPlan plan,
            List<NationState> nations,
            List<RelationRecord> relations,
            Map<Long, InfluenceLayerData> influences
    ) {}

    private NationSeeder() {
    }

    public static HabitabilityOracle serverOracle(ServerLevel overworld) {
        Objects.requireNonNull(overworld, "overworld");
        long seed = overworld.getSeed();
        HabitabilityOracle proceduralFallback = proceduralOracle(seed);
        return (rx, rz) -> {
            OptionalDouble wl = TcwildlandsBridge.queryRegionHabitability(rx, rz);
            if (wl.isPresent()) {
                return wl.getAsDouble();
            }
            try {
                return sampleBiomeGridHabitability(overworld, rx, rz);
            } catch (RuntimeException ignored) {
                return proceduralFallback.habitability(rx, rz);
            }
        };
    }

    public static double sampleBiomeGridHabitability(ServerLevel overworld, int regionX, int regionZ) {
        int baseX = regionX * RegionCoordinates.REGION_SIZE;
        int baseZ = regionZ * RegionCoordinates.REGION_SIZE;
        double weightSum = 0.0D;
        int oceanCells = 0;
        for (int gx = 0; gx < 8; gx++) {
            for (int gz = 0; gz < 8; gz++) {
                int bx = baseX + gx * 64 + 32;
                int bz = baseZ + gz * 64 + 32;
                Holder<Biome> holder = overworld.getUncachedNoiseBiome(
                        QuartPos.fromBlock(bx),
                        QuartPos.fromBlock(64),
                        QuartPos.fromBlock(bz)
                );
                String id = holder.unwrapKey().map(k -> k.identifier().getPath()).orElse("plains");
                double w = biomeHabitabilityWeight(id);
                if (w <= 0.0D) {
                    oceanCells++;
                }
                weightSum += w;
            }
        }
        if (oceanCells >= 32) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, weightSum / 64.0D));
    }

    public static double biomeHabitabilityWeight(String biomePath) {
        String p = biomePath.toLowerCase(Locale.ROOT);
        if (p.contains("ocean") || p.contains("sea") || p.contains("trench") || p.contains("reef")) {
            return 0.0D;
        }
        if (p.contains("plains") && !p.contains("snowy")
                || p.contains("forest") && !p.contains("taiga") && !p.contains("rainforest")
                || p.contains("meadow") || p.contains("grassland") || p.contains("floodplain")
                || p.contains("river") && !p.contains("frozen")) {
            return 1.0D;
        }
        if (p.contains("taiga") || p.contains("boreal") || p.contains("savanna") || p.contains("woodland") || p.contains("shrubland")) {
            return 0.7D;
        }
        if (p.contains("jungle") || p.contains("rainforest") || p.contains("swamp") || p.contains("marsh") || p.contains("mangrove") || p.contains("delta")) {
            return 0.5D;
        }
        return 0.2D;
    }

    /**
     * Deterministic multi-continent land/habitability field for headless unit testing.
     */
    public static HabitabilityOracle proceduralOracle(long worldSeed) {
        long salt = mix64(worldSeed ^ 0x574F524C44484142L);
        return (rx, rz) -> {
            double latFrac = StrictMath.abs(rz) / 80.0D;
            if (latFrac > 0.88D) {
                return 0.0D;
            }
            double nx = rx * 0.09D;
            double nz = rz * 0.09D;
            double continent = StrictMath.sin(nx + ((salt & 0xff) * 0.04D))
                    * StrictMath.cos(nz + (((salt >>> 8) & 0xff) * 0.04D))
                    + 0.45D * StrictMath.sin(nx * 2.3D - nz * 1.7D + (((salt >>> 16) & 0xff) * 0.03D));
            if (continent < -0.15D) {
                return 0.0D; // Ocean basin
            }
            double latClimate = StrictMath.max(0.15D, 1.0D - StrictMath.abs(latFrac - 0.28D) * 1.15D);
            double detail = ((mix64(salt ^ RegionCoordinates.key(rx, rz)) >>> 40) & 0xffL) / 255.0D;
            return Math.max(0.12D, Math.min(0.98D, 0.65D * latClimate + 0.35D * detail));
        };
    }

    public static double wrappedHorizontalDistance(BlockPos a, BlockPos b) {
        int dx = StrictMath.abs(a.getX() - b.getX());
        int wrappedDx = Math.min(dx, RegionCoordinates.WORLD_WIDTH - dx);
        long dz = (long) a.getZ() - b.getZ();
        return StrictMath.hypot(wrappedDx, dz);
    }

    public static SeedPlan computePlan(
            long worldSeed,
            BlockPos spawnPos,
            int playerStartEra,
            List<BlockPos> playerSettlementCores,
            NationsConfig.SeedingSettings settings,
            EchoesHistorySnapshot echoes,
            HabitabilityOracle habitability
    ) {
        Objects.requireNonNull(spawnPos, "spawnPos");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(habitability, "habitability");

        int targetCount = Math.max(0, settings.aiCivCount());
        int breathingRadius = settings.breathingRadiusForPlayerEra(playerStartEra);
        if (targetCount == 0) {
            return new SeedPlan(worldSeed, "disabled", breathingRadius, List.of(), Map.of());
        }

        int spawnExclusion = Math.max(settings.spawnExclusionRadius(), breathingRadius);
        List<BlockPos> playerCores = playerSettlementCores == null ? List.of() : List.copyOf(playerSettlementCores);
        SplitMix64 rng = new SplitMix64(worldSeed ^ NATIONS_SEED_SALT);
        List<NationBlueprint> blueprints = new ArrayList<>(targetCount);
        boolean usedEchoes = false;

        if (echoes != null && echoes.loaded()) {
            List<EchoesHistorySnapshot.PolityCandidate> survivors = new ArrayList<>();
            for (EchoesHistorySnapshot.PolityCandidate p : echoes.polities()) {
                if (p.survivor()) {
                    survivors.add(p);
                }
            }
            survivors.sort(Comparator.comparingDouble(EchoesHistorySnapshot.PolityCandidate::population).reversed()
                    .thenComparingInt(EchoesHistorySnapshot.PolityCandidate::polityId));

            int limit = Math.min(targetCount, survivors.size());
            for (int i = 0; i < limit; i++) {
                EchoesHistorySnapshot.PolityCandidate pol = survivors.get(i);
                int rx = clampRegionX(pol.capitalRegionX());
                int rz = clampRegionZ(pol.capitalRegionZ());
                BlockPos candidatePos = regionCenterPos(rx, rz);
                if (!isOutsidePlayerExclusion(candidatePos, spawnPos, spawnExclusion, playerCores, breathingRadius)
                        || !isSpacedFromExisting(candidatePos, blueprints, settings.capitalMinSpacing() * 0.5D)) {
                    Optional<RegionCoordinates> relocated = findNearestValidLandRegion(
                            rx, rz, spawnPos, spawnExclusion, playerCores, breathingRadius,
                            blueprints, settings.capitalMinSpacing() * 0.6D, habitability
                    );
                    if (relocated.isPresent()) {
                        rx = relocated.get().x();
                        rz = relocated.get().z();
                        candidatePos = regionCenterPos(rx, rz);
                    } else {
                        continue;
                    }
                }
                double hab = Math.max(0.25D, habitability.habitability(rx, rz));
                int cappedEra = Math.min(pol.eraIndex(), Math.max(0, playerStartEra) + settings.startEraLeadMax());
                Personality pers = rollPersonality(rng, pol.culture(), false);
                Government gov = chooseGovernment(cappedEra, rng);
                Identifier agenda = INITIAL_AGENDAS.get(rng.nextInt(INITIAL_AGENDAS.size()));
                double pop = Math.max(40.0D, StrictMath.rint(pol.population()));
                blueprints.add(new NationBlueprint(
                        blueprints.size(),
                        pol.polityId(),
                        pol.name(),
                        pol.name() + " Capital",
                        pol.culture(),
                        gov,
                        pers,
                        agenda,
                        cappedEra,
                        pop,
                        hab,
                        rx,
                        rz,
                        RegionCoordinates.key(rx, rz),
                        candidatePos,
                        pol.holySites(),
                        true
                ));
                usedEchoes = true;
            }
        }

        if (blueprints.size() < targetCount) {
            if (settings.guaranteedNeighbour() && !hasPeacefulNeighbourInRing(blueprints, spawnPos, spawnExclusion)) {
                placeGuaranteedNeighbour(
                        rng, spawnPos, spawnExclusion, playerStartEra, playerCores, breathingRadius,
                        settings, habitability, blueprints
                );
            }
            fillWithFallbackGenerator(
                    rng, targetCount, spawnPos, spawnExclusion, playerStartEra, playerCores, breathingRadius,
                    settings, habitability, blueprints
            );
        }

        String source = usedEchoes
                ? (blueprints.stream().allMatch(NationBlueprint::fromEchoes) ? "echoes" : "echoes+fallback")
                : "fallback";
        Map<Long, EchoesHistorySnapshot.HistoricRelation> rels =
                (echoes != null && echoes.loaded()) ? echoes.relations() : Map.of();
        return new SeedPlan(worldSeed, source, breathingRadius, List.copyOf(blueprints), rels);
    }

    public static SeedResult applyPlan(
            SeedPlan plan,
            CivilizationApi civApi,
            RegionApi regionApi,
            NationsSavedData savedData,
            BlockPos spawnPos,
            List<BlockPos> playerSettlementCores,
            long currentTick,
            boolean postEvents
    ) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(civApi, "civApi");
        Objects.requireNonNull(savedData, "savedData");

        List<NationState> createdStates = new ArrayList<>(plan.blueprints().size());
        Map<Integer, UUID> echoesIdToCivUuid = new LinkedHashMap<>();
        Map<UUID, Byte> civIndexMap = new LinkedHashMap<>();
        civIndexMap.put(Civilization.PLAYER_CIV_ID, (byte) 0);

        int idxCounter = 1;
        for (NationBlueprint bp : plan.blueprints()) {
            Civilization created = civApi.createAiCivilization(bp.name(), bp.culture(), BannerPatternLayers.EMPTY);
            UUID civId = created.id();
            civIndexMap.put(civId, (byte) Math.min(127, idxCounter++));
            if (bp.fromEchoes() && bp.echoesPolityId() >= 0) {
                echoesIdToCivUuid.put(bp.echoesPolityId(), civId);
            }

            SettlementRecord capitalSettlement = civApi.settlements().found(
                    civId,
                    bp.capitalName(),
                    GlobalPos.of(OVERWORLD_DIM, bp.capitalPos())
            );

            EnumMap<EconomyCategory, Double> stock = NationState.emptyStock();
            stock.put(EconomyCategory.FOOD, StrictMath.rint(bp.population() * 2.5D));
            stock.put(EconomyCategory.TIMBER, StrictMath.rint(bp.population() * 1.0D));
            stock.put(EconomyCategory.STONE, StrictMath.rint(bp.population() * 0.8D));

            NationState state = new NationState(
                    NationState.CURRENT_DATA_VERSION,
                    civId,
                    bp.name(),
                    bp.culture(),
                    bp.government(),
                    bp.personality(),
                    bp.agenda(),
                    Optional.empty(),
                    bp.eraIndex(),
                    bp.population(),
                    75.0D,
                    stock,
                    0.0D,
                    0.0D,
                    bp.capitalRegionKey(),
                    bp.capitalPos(),
                    List.of(capitalSettlement.id()),
                    bp.holySites(),
                    Optional.empty(),
                    currentTick,
                    NationStatus.ACTIVE
            );
            createdStates.add(state);
            if (postEvents) {
                NeoForge.EVENT_BUS.post(new NationsCivFoundedEvent(civId, bp.capitalPos(), NationsCivFoundedEvent.Cause.SEED));
            }
        }

        List<RelationRecord> relationRecords = buildInitialRelations(plan, echoesIdToCivUuid, currentTick);
        Map<Long, InfluenceLayerData> influences = computeInitialInfluenceMap(
                createdStates,
                civIndexMap,
                spawnPos,
                playerSettlementCores == null ? List.of() : playerSettlementCores,
                plan.breathingRadiusBlocks()
        );

        if (regionApi != null) {
            for (Map.Entry<Long, InfluenceLayerData> entry : influences.entrySet()) {
                long key = entry.getKey();
                int rx = (int) (key >> 32);
                int rz = (int) key;
                Optional<RegionHandle> handleOpt = regionApi.get(OVERWORLD_DIM, rx, rz);
                if (handleOpt.isPresent()) {
                    InfluenceLayerData value = entry.getValue();
                    handleOpt.get().setLayer(NationsInfluenceLayer.INSTANCE, value);
                    if (postEvents && value.ownerCivId().isPresent()) {
                        NeoForge.EVENT_BUS.post(new NationsBorderChangedEvent(
                                OVERWORLD_DIM, key, Optional.empty(), value.ownerCivId()
                        ));
                    }
                }
            }
        }

        savedData.recordSeeded(plan.worldSeed(), plan.source(), createdStates, relationRecords, influences);
        return new SeedResult(plan, createdStates, relationRecords, influences);
    }

    private static List<RelationRecord> buildInitialRelations(
            SeedPlan plan,
            Map<Integer, UUID> echoesIdToCivUuid,
            long currentTick
    ) {
        List<RelationRecord> out = new ArrayList<>();
        List<Map.Entry<Integer, UUID>> entries = new ArrayList<>(echoesIdToCivUuid.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                int polA = entries.get(i).getKey();
                int polB = entries.get(j).getKey();
                EchoesHistorySnapshot.HistoricRelation rel = Objects.requireNonNullElse(
                        plan.echoesRelations().get(EchoesHistorySnapshot.pairKey(polA, polB)),
                        EchoesHistorySnapshot.HistoricRelation.NEUTRAL
                );
                if (rel == EchoesHistorySnapshot.HistoricRelation.NEUTRAL) {
                    continue;
                }
                UUID civA = entries.get(i).getValue();
                UUID civB = entries.get(j).getValue();
                UUID first = civA.compareTo(civB) <= 0 ? civA : civB;
                UUID second = civA.compareTo(civB) <= 0 ? civB : civA;
                OpinionModifier mod = rel == EchoesHistorySnapshot.HistoricRelation.FEUD
                        ? new OpinionModifier(HISTORIC_FEUD_ID, -35.0D, currentTick, -1)
                        : new OpinionModifier(HISTORIC_ALLIANCE_ID, 30.0D, currentTick, -1);
                double trust = rel == EchoesHistorySnapshot.HistoricRelation.FEUD ? 30.0D : 65.0D;
                out.add(new RelationRecord(first, second, trust, 0.0D, 0.0D, List.of(mod), true, currentTick));
            }
        }
        return out;
    }

    public static Map<Long, InfluenceLayerData> computeInitialInfluenceMap(
            List<NationState> nations,
            Map<UUID, Byte> civIndexMap,
            BlockPos spawnPos,
            List<BlockPos> playerSettlementCores,
            int breathingRadiusBlocks
    ) {
        Map<Long, Map<UUID, Integer>> regionRawScores = new LinkedHashMap<>();
        int radiusCells = 4; // 4 * 512 = 2048 blocks around capital
        double lambda = 1500.0D;

        for (NationState nation : nations) {
            int capRx = (int) (nation.capitalRegionKey() >> 32);
            int capRz = (int) nation.capitalRegionKey();
            double eraMult = 1.0D + 0.12D * nation.eraIndex();
            double cultureMult = nation.culture().getPath().contains("steppe") || nation.agenda().getPath().equals("expansionist")
                    ? 1.05D
                    : 1.0D;
            double baseAmp = 40.0D * StrictMath.sqrt(Math.max(1.0D, nation.population())) * eraMult * cultureMult;

            for (int dz = -radiusCells; dz <= radiusCells; dz++) {
                int rz = capRz + dz;
                if (rz < -RegionCoordinates.REGION_HEIGHT / 2 || rz >= RegionCoordinates.REGION_HEIGHT / 2) {
                    continue;
                }
                for (int dx = -radiusCells; dx <= radiusCells; dx++) {
                    int rx = wrapRegionX(capRx + dx);
                    BlockPos cellCenter = regionCenterPos(rx, rz);
                    double dist = wrappedHorizontalDistance(nation.capitalPos(), cellCenter);
                    int inf = (int) StrictMath.rint(Math.min(1000.0D, baseAmp * StrictMath.exp(-dist / lambda)));
                    if (inf <= 0) {
                        continue;
                    }
                    long rKey = RegionCoordinates.key(rx, rz);
                    regionRawScores.computeIfAbsent(rKey, k -> new LinkedHashMap<>())
                            .merge(nation.civId(), inf, Integer::sum);
                }
            }
        }

        Map<Long, InfluenceLayerData> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Map<UUID, Integer>> e : regionRawScores.entrySet()) {
            long rKey = e.getKey();
            int rx = (int) (rKey >> 32);
            int rz = (int) rKey;
            BlockPos center = regionCenterPos(rx, rz);
            boolean inBreathingZone = isInsideBreathingZone(center, spawnPos, playerSettlementCores, breathingRadiusBlocks);
            InfluenceLayerData layerData = InfluenceLayerData.fromScores(
                    e.getValue(),
                    civIndexMap,
                    Civilization.PLAYER_CIV_ID,
                    inBreathingZone
            );
            if (!layerData.topEntries().isEmpty()) {
                result.put(rKey, layerData);
            }
        }
        return result;
    }

    private static boolean hasPeacefulNeighbourInRing(List<NationBlueprint> existing, BlockPos spawnPos, int innerRadius) {
        double outer = Math.max(4000.0D, innerRadius + 1500.0D);
        for (NationBlueprint bp : existing) {
            double d = wrappedHorizontalDistance(bp.capitalPos(), spawnPos);
            if (d >= innerRadius && d <= outer && bp.personality().aggression() <= 35) {
                return true;
            }
        }
        return false;
    }

    private static void placeGuaranteedNeighbour(
            SplitMix64 rng,
            BlockPos spawnPos,
            int innerRadius,
            int playerStartEra,
            List<BlockPos> playerCores,
            int breathingRadius,
            NationsConfig.SeedingSettings settings,
            HabitabilityOracle habitability,
            List<NationBlueprint> out
    ) {
        int spawnRx = RegionCoordinates.regionX(spawnPos.getX());
        int spawnRz = RegionCoordinates.regionZ(spawnPos.getZ());
        double[] outerCandidates = {
                Math.max(4000.0D, innerRadius + 1500.0D),
                Math.max(6000.0D, innerRadius + 3000.0D),
                Math.max(9000.0D, innerRadius + 5500.0D)
        };

        for (double outerRadius : outerCandidates) {
            int maxCellOffset = (int) StrictMath.ceil(outerRadius / RegionCoordinates.REGION_SIZE) + 1;
            List<RegionCandidate> ringCandidates = new ArrayList<>();
            for (int dz = -maxCellOffset; dz <= maxCellOffset; dz++) {
                int rz = spawnRz + dz;
                if (rz < -72 || rz >= 72) {
                    continue;
                }
                for (int dx = -maxCellOffset; dx <= maxCellOffset; dx++) {
                    int rx = wrapRegionX(spawnRx + dx);
                    BlockPos pos = regionCenterPos(rx, rz);
                    double dSpawn = wrappedHorizontalDistance(pos, spawnPos);
                    if (dSpawn < innerRadius || dSpawn > outerRadius) {
                        continue;
                    }
                    if (!isOutsidePlayerExclusion(pos, spawnPos, innerRadius, playerCores, breathingRadius)) {
                        continue;
                    }
                    if (!isSpacedFromExisting(pos, out, settings.capitalMinSpacing())) {
                        continue;
                    }
                    double h = habitability.habitability(rx, rz);
                    if (h > 0.05D) {
                        ringCandidates.add(new RegionCandidate(rx, rz, pos, h));
                    }
                }
            }
            if (ringCandidates.isEmpty()) {
                continue;
            }
            deterministicShuffle(ringCandidates, rng);
            RegionCandidate chosen = null;
            for (RegionCandidate cand : ringCandidates) {
                if (rng.nextDouble() < cand.habitability * cand.habitability) {
                    chosen = cand;
                    break;
                }
            }
            if (chosen == null) {
                ringCandidates.sort(Comparator.comparingDouble(RegionCandidate::habitability).reversed()
                        .thenComparingInt(RegionCandidate::rx)
                        .thenComparingInt(RegionCandidate::rz));
                chosen = ringCandidates.getFirst();
            }
            out.add(createFallbackBlueprint(out.size(), chosen, playerStartEra, settings, rng, true));
            return;
        }
    }

    private static void fillWithFallbackGenerator(
            SplitMix64 rng,
            int targetCount,
            BlockPos spawnPos,
            int spawnExclusion,
            int playerStartEra,
            List<BlockPos> playerCores,
            int breathingRadius,
            NationsConfig.SeedingSettings settings,
            HabitabilityOracle habitability,
            List<NationBlueprint> out
    ) {
        List<RegionCandidate> pool = new ArrayList<>();
        int spawnRx = RegionCoordinates.regionX(spawnPos.getX());
        int spawnRz = RegionCoordinates.regionZ(spawnPos.getZ());
        int searchHalfWidth = 80; // +-40,960 blocks around spawn longitude
        int searchHalfHeight = 64; // +-32,768 blocks latitude

        for (int dz = -searchHalfHeight; dz <= searchHalfHeight; dz++) {
            int rz = spawnRz + dz;
            if (rz < -72 || rz >= 72) {
                continue;
            }
            for (int dx = -searchHalfWidth; dx <= searchHalfWidth; dx++) {
                int rx = wrapRegionX(spawnRx + dx);
                BlockPos pos = regionCenterPos(rx, rz);
                if (!isOutsidePlayerExclusion(pos, spawnPos, spawnExclusion, playerCores, breathingRadius)) {
                    continue;
                }
                double h = habitability.habitability(rx, rz);
                if (h > 0.05D) {
                    pool.add(new RegionCandidate(rx, rz, pos, h));
                }
            }
        }
        deterministicShuffle(pool, rng);

        double spacing = settings.capitalMinSpacing();
        for (int relaxStep = 0; relaxStep <= 3 && out.size() < targetCount; relaxStep++) {
            for (RegionCandidate cand : pool) {
                if (out.size() >= targetCount) {
                    break;
                }
                if (!isSpacedFromExisting(cand.pos, out, spacing)) {
                    continue;
                }
                if (rng.nextDouble() < cand.habitability * cand.habitability) {
                    out.add(createFallbackBlueprint(out.size(), cand, playerStartEra, settings, rng, false));
                }
            }
            // Second pass at same spacing without stochastic rejection before relaxing spacing (NR-2)
            for (RegionCandidate cand : pool) {
                if (out.size() >= targetCount) {
                    break;
                }
                if (isSpacedFromExisting(cand.pos, out, spacing)) {
                    out.add(createFallbackBlueprint(out.size(), cand, playerStartEra, settings, rng, false));
                }
            }
            spacing *= 0.8D;
        }
    }

    private static NationBlueprint createFallbackBlueprint(
            int slotIndex,
            RegionCandidate cand,
            int playerStartEra,
            NationsConfig.SeedingSettings settings,
            SplitMix64 rng,
            boolean peacefulNeighbour
    ) {
        Identifier culture = BUILTIN_CULTURES.get(rng.nextInt(BUILTIN_CULTURES.size()));
        String name = generateCivName(rng, slotIndex);
        int baseEra = Math.max(0, playerStartEra);
        int rolledEra = rng.nextDouble() < 0.70D ? baseEra : baseEra + 1;
        int cappedEra = Math.min(10, Math.min(rolledEra, baseEra + settings.startEraLeadMax()));
        Government gov = chooseGovernment(cappedEra, rng);
        Personality personality = rollPersonality(rng, culture, peacefulNeighbour);
        Identifier agenda = peacefulNeighbour
                ? Identifier.fromNamespaceAndPath(TNations.MOD_ID, "honor_bound")
                : INITIAL_AGENDAS.get(rng.nextInt(INITIAL_AGENDAS.size()));
        double pop = StrictMath.rint(40.0D + 120.0D * cand.habitability);
        return new NationBlueprint(
                slotIndex,
                -1,
                name,
                name + " Hearth",
                culture,
                gov,
                personality,
                agenda,
                cappedEra,
                pop,
                cand.habitability,
                cand.rx,
                cand.rz,
                RegionCoordinates.key(cand.rx, cand.rz),
                cand.pos,
                List.of(),
                false
        );
    }

    private static String generateCivName(SplitMix64 rng, int slotIndex) {
        String prefix = NAME_PREFIXES[rng.nextInt(NAME_PREFIXES.length)];
        String suffix = NAME_SUFFIXES[rng.nextInt(NAME_SUFFIXES.length)];
        return prefix + suffix + "-" + (slotIndex + 1);
    }

    private static Government chooseGovernment(int eraIndex, SplitMix64 rng) {
        if (eraIndex <= 0) {
            return Government.TRIBE;
        }
        if (eraIndex == 1) {
            return rng.nextDouble() < 0.55D ? Government.TRIBE : Government.CHIEFDOM;
        }
        if (eraIndex <= 3) {
            return rng.nextDouble() < 0.5D ? Government.CITY_STATE : Government.MONARCHY;
        }
        return Government.MONARCHY;
    }

    private static Personality rollPersonality(SplitMix64 rng, Identifier culture, boolean peacefulCap) {
        int agg = peacefulCap ? (8 + rng.nextInt(28)) : (15 + rng.nextInt(76));
        int exp = 20 + rng.nextInt(66);
        int com = 20 + rng.nextInt(66);
        int sci = 20 + rng.nextInt(66);
        int hon = peacefulCap ? (55 + rng.nextInt(41)) : (25 + rng.nextInt(66));
        int xen = peacefulCap ? (45 + rng.nextInt(46)) : (20 + rng.nextInt(66));
        String path = culture.getPath();
        if (path.contains("steppe")) {
            exp += 12;
        } else if (path.contains("maritime")) {
            com += 15;
            xen += 10;
        } else if (path.contains("river_valley")) {
            sci += 10;
        }
        if (peacefulCap) {
            agg = Math.min(35, agg);
        }
        return new Personality(agg, exp, com, sci, hon, xen);
    }

    private static Optional<RegionCoordinates> findNearestValidLandRegion(
            int startRx,
            int startRz,
            BlockPos spawnPos,
            int spawnExclusion,
            List<BlockPos> playerCores,
            int breathingRadius,
            List<NationBlueprint> existing,
            double minSpacing,
            HabitabilityOracle habitability
    ) {
        for (int radius = 1; radius <= 24; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int rz = startRz + dz;
                if (rz < -72 || rz >= 72) {
                    continue;
                }
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int rx = wrapRegionX(startRx + dx);
                    BlockPos pos = regionCenterPos(rx, rz);
                    if (!isOutsidePlayerExclusion(pos, spawnPos, spawnExclusion, playerCores, breathingRadius)) {
                        continue;
                    }
                    if (!isSpacedFromExisting(pos, existing, minSpacing)) {
                        continue;
                    }
                    if (habitability.habitability(rx, rz) > 0.10D) {
                        return Optional.of(new RegionCoordinates(rx, rz));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isOutsidePlayerExclusion(
            BlockPos pos,
            BlockPos spawnPos,
            int spawnExclusion,
            List<BlockPos> playerCores,
            int breathingRadius
    ) {
        if (wrappedHorizontalDistance(pos, spawnPos) < spawnExclusion) {
            return false;
        }
        for (BlockPos core : playerCores) {
            if (wrappedHorizontalDistance(pos, core) < breathingRadius) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInsideBreathingZone(
            BlockPos pos,
            BlockPos spawnPos,
            List<BlockPos> playerCores,
            int breathingRadius
    ) {
        if (wrappedHorizontalDistance(pos, spawnPos) < breathingRadius) {
            return true;
        }
        for (BlockPos core : playerCores) {
            if (wrappedHorizontalDistance(pos, core) < breathingRadius) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpacedFromExisting(BlockPos pos, List<NationBlueprint> existing, double minSpacing) {
        for (NationBlueprint bp : existing) {
            if (wrappedHorizontalDistance(pos, bp.capitalPos()) < minSpacing) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos regionCenterPos(int rx, int rz) {
        return new BlockPos(rx * RegionCoordinates.REGION_SIZE + 256, 64, rz * RegionCoordinates.REGION_SIZE + 256);
    }

    private static int wrapRegionX(int rx) {
        int w = RegionCoordinates.REGION_WIDTH;
        return Math.floorMod(rx + w / 2, w) - w / 2;
    }

    private static int clampRegionX(int rx) {
        return wrapRegionX(rx);
    }

    private static int clampRegionZ(int rz) {
        return Math.max(-RegionCoordinates.REGION_HEIGHT / 2, Math.min(RegionCoordinates.REGION_HEIGHT / 2 - 1, rz));
    }

    private static <T> void deterministicShuffle(List<T> list, SplitMix64 rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    private record RegionCandidate(int rx, int rz, BlockPos pos, double habitability) {}

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static final class SplitMix64 {
        private long state;

        public SplitMix64(long seed) {
            this.state = seed;
        }

        public long nextLong() {
            long z = (state += 0x9E3779B97F4A7C15L);
            return mix64(z);
        }

        public int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be > 0");
            }
            return (int) Long.remainderUnsigned(nextLong(), bound);
        }

        public double nextDouble() {
            return (nextLong() >>> 11) * 0x1.0p-53;
        }
    }
}
