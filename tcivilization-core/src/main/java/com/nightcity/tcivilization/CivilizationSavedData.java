package com.nightcity.tcivilization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.tcivilization.api.CivCalendar;
import com.nightcity.tcivilization.api.CivKind;
import com.nightcity.tcivilization.api.CivMember;
import com.nightcity.tcivilization.api.CivStats;
import com.nightcity.tcivilization.api.CivStatsView;
import com.nightcity.tcivilization.api.Civilization;
import com.nightcity.tcivilization.api.CivilizationApi;
import com.nightcity.tcivilization.api.EraWriteToken;
import com.nightcity.tcivilization.api.MemoryApi;
import com.nightcity.tcivilization.api.MilestoneRecord;
import com.nightcity.tcivilization.api.RegionApi;
import com.nightcity.tcivilization.api.SettlementRecord;
import com.nightcity.tcivilization.api.SettlementRegistry;
import com.nightcity.tcivilization.api.TcMigrators;
import com.nightcity.tcivilization.api.event.CivMemberChangedEvent;
import com.nightcity.tcivilization.api.event.CivilizationCreatedEvent;
import com.nightcity.tcivilization.api.event.SettlementFoundedEvent;
import com.nightcity.tcivilization.api.event.SettlementRemovedEvent;
import com.nightcity.tcivilization.api.event.SettlementTierChangedEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Global SavedData for tcivilization (formatVersion = 1) implementing CivilizationApi (03 §5.1, §5.9, §11).
 */
public final class CivilizationSavedData extends SavedData implements CivilizationApi {
    public static final int FORMAT_VERSION = 1;
    public static final Identifier FILE_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "civilization_state");

    private static boolean defaultMigratorsRegistered = false;

    static {
        registerDefaultMigrators();
    }

    public static synchronized void registerDefaultMigrators() {
        if (defaultMigratorsRegistered) {
            return;
        }
        defaultMigratorsRegistered = true;
        TcMigrators.register(FILE_ID, 0, (fromVer, tag) -> {
            CompoundTag migrated = new CompoundTag();
            migrated.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
            migrated.putLong("sequence", 0L);
            long legacyTicks = tag.getLongOr("world_ticks", 0L);
            int legacyLogins = tag.getIntOr("login_events", 0);
            int legacyPeak = tag.getIntOr("peak_players", 0);
            migrated.putInt("login_events", legacyLogins);

            CivStats stats = new CivStats();
            if (legacyTicks > 0L) {
                stats.set(CivStats.PLAY_TICKS, legacyTicks);
            }
            if (legacyPeak > 0) {
                stats.set(CivStats.PEAK_PLAYERS, legacyPeak);
            }
            Civilization playerCiv = new Civilization(
                    Civilization.PLAYER_CIV_ID, CivKind.PLAYER, Civilization.DEFAULT_NAME,
                    BannerPatternLayers.EMPTY, -1, 0L, Map.of(), List.of(), stats, Set.of()
            );
            ListTag civList = new ListTag();
            civList.add(Civilization.CODEC.encodeStart(NbtOps.INSTANCE, playerCiv).getOrThrow());
            migrated.put("civs", civList);
            return migrated;
        });
    }

    private record V1Payload(
            int formatVersion,
            long sequence,
            CivCalendar.State calendar,
            List<Civilization> civs,
            List<SettlementRecord> settlements,
            List<MilestoneRecord> milestones
    ) {
        static final Codec<V1Payload> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf(TcMigrators.FORMAT_VERSION_KEY).forGetter(V1Payload::formatVersion),
                Codec.LONG.optionalFieldOf("sequence", 0L).forGetter(V1Payload::sequence),
                CivCalendar.State.CODEC.optionalFieldOf("calendar", CivCalendar.State.initial()).forGetter(V1Payload::calendar),
                Civilization.CODEC.listOf().optionalFieldOf("civs", List.of()).forGetter(V1Payload::civs),
                SettlementRecord.CODEC.listOf().optionalFieldOf("settlements", List.of()).forGetter(V1Payload::settlements),
                MilestoneRecord.CODEC.listOf().optionalFieldOf("milestones", List.of()).forGetter(V1Payload::milestones)
        ).apply(i, V1Payload::new));
    }

    public static final Codec<CivilizationSavedData> CODEC = CompoundTag.CODEC.xmap(
            CivilizationSavedData::fromCompoundTag,
            CivilizationSavedData::toCompoundTag
    );

    public static final SavedDataType<CivilizationSavedData> TYPE = new SavedDataType<>(
            FILE_ID,
            CivilizationSavedData::new,
            CODEC
    );

    private final Map<UUID, Civilization> civilizations = new LinkedHashMap<>();
    private final Map<UUID, SettlementRecord> settlementMap = new LinkedHashMap<>();
    private final List<MilestoneRecord> milestoneList = new ArrayList<>();
    private final MemoryStore memoryStore;
    private final SettlementRegistry settlementRegistry = new SettlementRegistryImpl();
    private final TcEventBus eventBus;
    private CivCalendar.State calendarState = CivCalendar.State.initial();
    private long currentTick = 0L;

    public CivilizationSavedData() {
        this(TcEventBus.global());
    }

    public CivilizationSavedData(TcEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.memoryStore = new MemoryStore(this.eventBus, () -> this.currentTick);
        ensurePlayerCiv(0L);
    }

    public static CivilizationSavedData fromCompoundTag(CompoundTag rawTag) {
        return fromCompoundTag(rawTag, TcEventBus.global());
    }

    public static CivilizationSavedData fromCompoundTag(CompoundTag rawTag, TcEventBus bus) {
        CompoundTag migrated = TcMigrators.migrate(FILE_ID, FORMAT_VERSION, rawTag);
        V1Payload payload = V1Payload.CODEC.parse(NbtOps.INSTANCE, migrated)
                .getOrThrow(err -> new IllegalStateException("Failed decoding CivilizationSavedData v1: " + err));
        CivilizationSavedData data = new CivilizationSavedData(bus);
        data.calendarState = payload.calendar();
        bus.restoreSequence(payload.sequence());
        data.civilizations.clear();
        for (Civilization civ : payload.civs()) {
            civ.stats().setDirtyCallback(data::setDirty);
            data.civilizations.put(civ.id(), civ);
        }
        data.ensurePlayerCiv(0L);
        data.loginEventCount = migrated.getIntOr("login_events", 0);
        for (SettlementRecord sr : payload.settlements()) {
            data.settlementMap.put(sr.id(), sr);
        }
        data.milestoneList.addAll(payload.milestones());
        return data;
    }

    public CompoundTag toCompoundTag() {
        V1Payload payload = new V1Payload(
                FORMAT_VERSION,
                eventBus.currentSequence(),
                calendarState,
                List.copyOf(civilizations.values()),
                List.copyOf(settlementMap.values()),
                List.copyOf(milestoneList)
        );
        Tag encoded = V1Payload.CODEC.encodeStart(NbtOps.INSTANCE, payload)
                .getOrThrow(err -> new IllegalStateException("Failed encoding CivilizationSavedData v1: " + err));
        CompoundTag out = (CompoundTag) encoded;
        out.putInt("login_events", loginEventCount);
        return out;
    }

    public static CivilizationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private Civilization ensurePlayerCiv(long foundedTick) {
        return civilizations.computeIfAbsent(PLAYER_CIV_ID, id -> {
            Civilization created = Civilization.defaultPlayerCiv(foundedTick);
            created.stats().setDirtyCallback(this::setDirty);
            return created;
        });
    }

    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    public CivCalendar.State calendarState() {
        return calendarState;
    }

    public void updateCalendar(MinecraftServer server, long dayTime, boolean wildlandsLoaded, boolean publishFallbackSeason) {
        this.currentTick = dayTime;
        CivCalendar.State next = CivCalendar.advance(server, dayTime, calendarState, wildlandsLoaded, publishFallbackSeason, eventBus);
        if (!next.equals(calendarState)) {
            this.calendarState = next;
            setDirty();
        }
    }

    @Override
    public Civilization playerCiv() {
        return ensurePlayerCiv(currentTick);
    }

    @Override
    public Optional<Civilization> civ(UUID id) {
        return Optional.ofNullable(civilizations.get(id));
    }

    @Override
    public Optional<Civilization> civOf(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        Civilization pc = playerCiv();
        return pc.members().containsKey(player.getUUID()) ? Optional.of(pc) : Optional.empty();
    }

    @Override
    public OptionalInt era(UUID civ) {
        Civilization found = civilizations.get(civ);
        if (found == null || found.eraIndex() < 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(found.eraIndex());
    }

    @Override
    public void setEra(UUID civ, int era, EraWriteToken token) {
        Objects.requireNonNull(token, "EraWriteToken required");
        if (era < 0 || era > 10) {
            throw new IllegalArgumentException("Era out of [0, 10]: " + era);
        }
        Civilization current = civilizations.get(civ);
        if (current == null) {
            throw new IllegalArgumentException("Unknown civilization: " + civ);
        }
        Civilization updated = new Civilization(
                current.id(), current.kind(), current.name(), current.banner(),
                era, current.foundedTick(), current.members(), current.settlements(),
                current.stats(), current.flags()
        );
        updated.stats().bumpVersion();
        civilizations.put(civ, updated);
        setDirty();
    }

    @Override
    public Civilization createAiCivilization(String name, Identifier culture, BannerPatternLayers banner) {
        UUID id = UUID.randomUUID();
        CivStats stats = new CivStats();
        stats.setDirtyCallback(this::setDirty);
        Civilization ai = new Civilization(
                id, CivKind.AI, name, banner == null ? BannerPatternLayers.EMPTY : banner,
                -1, currentTick, Map.of(), List.of(), stats, Set.of()
        );
        civilizations.put(id, ai);
        setDirty();
        eventBus.enqueue(new CivilizationCreatedEvent(ai));
        return ai;
    }

    @Override
    public Civilization createHistoricCivilization(String name, Identifier culture, BannerPatternLayers banner, int endYear) {
        UUID id = UUID.randomUUID();
        CivStats stats = new CivStats();
        stats.setDirtyCallback(this::setDirty);
        Civilization historic = new Civilization(
                id, CivKind.HISTORIC, name, banner == null ? BannerPatternLayers.EMPTY : banner,
                -1, 0L, Map.of(), List.of(), stats, Set.of()
        );
        civilizations.put(id, historic);
        setDirty();
        eventBus.enqueue(new CivilizationCreatedEvent(historic));
        return historic;
    }

    public void renameCivilization(UUID civId, String newName) {
        Civilization current = civilizations.get(civId);
        if (current == null || current.kind() == CivKind.HISTORIC) {
            return;
        }
        Civilization updated = new Civilization(
                current.id(), current.kind(), newName, current.banner(),
                current.eraIndex(), current.foundedTick(), current.members(),
                current.settlements(), current.stats(), current.flags()
        );
        updated.stats().bumpVersion();
        civilizations.put(civId, updated);
        setDirty();
    }

    public CivMember joinPlayerCiv(UUID playerId, long tick) {
        Civilization pc = playerCiv();
        CivMember existing = pc.members().get(playerId);
        if (existing != null) {
            return existing;
        }
        Identifier role = pc.members().isEmpty() ? CivMember.ROLE_FOUNDER : CivMember.ROLE_MEMBER;
        CivMember member = new CivMember(playerId, role, tick, List.of(), 0L, Map.of());
        Map<UUID, CivMember> nextMembers = new LinkedHashMap<>(pc.members());
        nextMembers.put(playerId, member);
        Civilization updated = new Civilization(
                pc.id(), pc.kind(), pc.name(), pc.banner(), pc.eraIndex(),
                pc.members().isEmpty() ? tick : pc.foundedTick(),
                nextMembers, pc.settlements(), pc.stats(), pc.flags()
        );
        civilizations.put(PLAYER_CIV_ID, updated);
        setDirty();
        eventBus.enqueue(new CivMemberChangedEvent(PLAYER_CIV_ID, playerId, CivMemberChangedEvent.Change.JOINED, role));
        return member;
    }

    public boolean removeMember(UUID playerId, boolean kicked) {
        Civilization pc = playerCiv();
        CivMember removed = pc.members().get(playerId);
        if (removed == null) {
            return false;
        }
        Map<UUID, CivMember> nextMembers = new LinkedHashMap<>(pc.members());
        nextMembers.remove(playerId);
        Civilization updated = new Civilization(
                pc.id(), pc.kind(), pc.name(), pc.banner(), pc.eraIndex(),
                pc.foundedTick(), nextMembers, pc.settlements(), pc.stats(), pc.flags()
        );
        civilizations.put(PLAYER_CIV_ID, updated);
        setDirty();
        eventBus.enqueue(new CivMemberChangedEvent(
                PLAYER_CIV_ID, playerId,
                kicked ? CivMemberChangedEvent.Change.KICKED : CivMemberChangedEvent.Change.LEFT,
                removed.role()
        ));
        return true;
    }

    public void updateMember(CivMember updatedMember) {
        Civilization pc = playerCiv();
        Map<UUID, CivMember> nextMembers = new LinkedHashMap<>(pc.members());
        nextMembers.put(updatedMember.playerId(), updatedMember);
        Civilization updated = new Civilization(
                pc.id(), pc.kind(), pc.name(), pc.banner(), pc.eraIndex(),
                pc.foundedTick(), nextMembers, pc.settlements(), pc.stats(), pc.flags()
        );
        civilizations.put(PLAYER_CIV_ID, updated);
        setDirty();
    }

    @Override
    public CivStatsView stats(UUID civ) {
        Civilization found = civilizations.get(civ);
        if (found == null) {
            throw new IllegalArgumentException("Unknown civilization: " + civ);
        }
        return found.stats();
    }

    @Override
    public void setFlag(UUID civ, Identifier flag, boolean value) {
        Civilization found = civilizations.get(civ);
        if (found == null) {
            return;
        }
        Set<Identifier> nextFlags = new LinkedHashSet<>(found.flags());
        boolean changed = value ? nextFlags.add(flag) : nextFlags.remove(flag);
        if (changed) {
            Civilization updated = new Civilization(
                    found.id(), found.kind(), found.name(), found.banner(), found.eraIndex(),
                    found.foundedTick(), found.members(), found.settlements(), found.stats(), nextFlags
            );
            civilizations.put(civ, updated);
            setDirty();
        }
    }

    @Override
    public List<MilestoneRecord> milestones(UUID civ) {
        return milestoneList.stream().filter(m -> m.civ().equals(civ)).toList();
    }

    @Override
    public void recordMilestone(MilestoneRecord record) {
        Objects.requireNonNull(record, "record");
        boolean exists = milestoneList.stream()
                .anyMatch(m -> m.civ().equals(record.civ()) && m.id().equals(record.id()));
        if (!exists) {
            milestoneList.add(record);
            setDirty();
        }
    }

    @Override
    public SettlementRegistry settlements() {
        return settlementRegistry;
    }

    @Override
    public MemoryApi memories() {
        return memoryStore;
    }

    public MemoryStore memoryStore() {
        return memoryStore;
    }

    public List<Civilization> allCivilizations() {
        return List.copyOf(civilizations.values());
    }

    private final Map<UUID, Set<Long>> visitedRegionsByCiv = new LinkedHashMap<>();
    private int loginEventCount = 0;

    public boolean recordRegionVisit(UUID civId, com.nightcity.tcivilization.api.RegionCell cell) {
        Objects.requireNonNull(civId, "civId");
        Objects.requireNonNull(cell, "cell");
        Set<Long> visited = visitedRegionsByCiv.computeIfAbsent(civId, k -> new LinkedHashSet<>());
        if (!visited.add(cell.key())) {
            return false;
        }
        stats(civId).add(CivStats.REGIONS_VISITED, 1L);
        setDirty();
        eventBus.enqueue(new com.nightcity.tcivilization.api.event.RegionFirstVisitEvent(civId, cell, null));
        return true;
    }

    public int loginEvents() {
        return loginEventCount;
    }

    public void recordLogin(UUID playerId, long tick) {
        loginEventCount++;
        joinPlayerCiv(playerId, tick);
        setDirty();
    }

    public void recordSecond(MinecraftServer server) {
        int online = server.getPlayerList().getPlayerCount();
        if (online > 0) {
            CivStatsView st = stats(PLAYER_CIV_ID);
            st.add(CivStats.PLAY_TICKS, 20L * online);
            long prevPeak = st.get(CivStats.PEAK_PLAYERS);
            if (online > prevPeak) {
                st.set(CivStats.PEAK_PLAYERS, online);
            }
        }
        setDirty();
    }

    public long worldTicks() {
        return stats(PLAYER_CIV_ID).get(CivStats.PLAY_TICKS);
    }

    public int peakPlayers() {
        return (int) stats(PLAYER_CIV_ID).get(CivStats.PEAK_PLAYERS);
    }

    private final class SettlementRegistryImpl implements SettlementRegistry {
        @Override
        public SettlementRecord found(UUID civ, String name, GlobalPos core) {
            Objects.requireNonNull(civ, "civ");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(core, "core");
            Civilization owner = civilizations.get(civ);
            if (owner == null) {
                throw new IllegalArgumentException("Unknown civilization: " + civ);
            }
            int rx = RegionCoordinates.regionX(core.pos().getX());
            int rz = RegionCoordinates.regionZ(core.pos().getZ());
            long rKey = RegionApi.regionKey(rx, rz);
            SettlementRecord record = new SettlementRecord(
                    UUID.randomUUID(), civ, name, core, rKey, 0, 0, currentTick
            );
            settlementMap.put(record.id(), record);

            List<UUID> nextIds = new ArrayList<>(owner.settlements());
            nextIds.add(record.id());
            civilizations.put(civ, new Civilization(
                    owner.id(), owner.kind(), owner.name(), owner.banner(), owner.eraIndex(),
                    owner.foundedTick(), owner.members(), nextIds, owner.stats(), owner.flags()
            ));
            setDirty();
            eventBus.enqueue(new SettlementFoundedEvent(record));
            return record;
        }

        @Override
        public void setTier(UUID settlement, int tier) {
            SettlementRecord existing = settlementMap.get(settlement);
            if (existing == null) {
                return;
            }
            int clamped = Math.clamp(tier, 0, 6);
            if (existing.tier() == clamped) {
                return;
            }
            SettlementRecord updated = existing.withTier(clamped);
            settlementMap.put(settlement, updated);
            setDirty();
            eventBus.enqueue(new SettlementTierChangedEvent(updated, existing.tier(), clamped));
        }

        @Override
        public void setPopulation(UUID settlement, int population) {
            SettlementRecord existing = settlementMap.get(settlement);
            if (existing == null) {
                return;
            }
            settlementMap.put(settlement, existing.withPopulation(population));
            setDirty();
        }

        @Override
        public void remove(UUID settlement, Identifier reason) {
            SettlementRecord removed = settlementMap.remove(settlement);
            if (removed == null) {
                return;
            }
            Civilization owner = civilizations.get(removed.civ());
            if (owner != null) {
                List<UUID> nextIds = new ArrayList<>(owner.settlements());
                nextIds.remove(settlement);
                civilizations.put(owner.id(), new Civilization(
                        owner.id(), owner.kind(), owner.name(), owner.banner(), owner.eraIndex(),
                        owner.foundedTick(), owner.members(), nextIds, owner.stats(), owner.flags()
                ));
            }
            setDirty();
            eventBus.enqueue(new SettlementRemovedEvent(removed, reason));
        }

        @Override
        public Optional<SettlementRecord> get(UUID id) {
            return Optional.ofNullable(settlementMap.get(id));
        }

        @Override
        public List<SettlementRecord> inRegion(ResourceKey<Level> dim, long regionKey) {
            return settlementMap.values().stream()
                    .filter(s -> s.corePos().dimension().equals(dim) && s.regionKey() == regionKey)
                    .toList();
        }

        @Override
        public List<SettlementRecord> all() {
            return List.copyOf(settlementMap.values());
        }
    }
}

