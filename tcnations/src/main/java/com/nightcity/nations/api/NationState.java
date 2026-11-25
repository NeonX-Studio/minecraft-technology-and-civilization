package com.nightcity.nations.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

public record NationState(
        int dataVersion,
        UUID civId,
        String name,
        Identifier culture,
        Government government,
        Personality personality,
        Identifier agenda,
        Optional<Identifier> hiddenAgenda,
        int eraIndex,
        double population,
        double stability,
        EnumMap<EconomyCategory, Double> stock,
        double rpReserve,
        double exhaustion,
        long capitalRegionKey,
        BlockPos capitalPos,
        List<UUID> settlements,
        List<Long> holySites,
        Optional<UUID> leader,
        long foundedTick,
        NationStatus status
) {
    public static final int CURRENT_DATA_VERSION = 1;

    private static final Codec<EnumMap<EconomyCategory, Double>> STOCK_CODEC = Codec.unboundedMap(
            EconomyCategory.CODEC,
            Codec.DOUBLE
    ).xmap(NationState::toEnumMap, map -> {
        Map<EconomyCategory, Double> copy = new LinkedHashMap<>(map);
        return copy;
    });

    private record IdentityPart(
            int dataVersion,
            UUID civId,
            String name,
            Identifier culture,
            Government government,
            Personality personality,
            Identifier agenda,
            Optional<Identifier> hiddenAgenda,
            int eraIndex,
            double population,
            double stability
    ) {
        static final MapCodec<IdentityPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(IdentityPart::dataVersion),
                UUIDUtil.CODEC.fieldOf("civ_id").forGetter(IdentityPart::civId),
                Codec.STRING.optionalFieldOf("name", "Unknown Polity").forGetter(IdentityPart::name),
                Identifier.CODEC.fieldOf("culture").forGetter(IdentityPart::culture),
                Government.CODEC.optionalFieldOf("government", Government.TRIBE).forGetter(IdentityPart::government),
                Personality.CODEC.fieldOf("personality").forGetter(IdentityPart::personality),
                Identifier.CODEC.fieldOf("agenda").forGetter(IdentityPart::agenda),
                Identifier.CODEC.optionalFieldOf("hidden_agenda").forGetter(IdentityPart::hiddenAgenda),
                Codec.intRange(0, 10).optionalFieldOf("era_index", 0).forGetter(IdentityPart::eraIndex),
                Codec.DOUBLE.optionalFieldOf("population", 40.0D).forGetter(IdentityPart::population),
                Codec.DOUBLE.optionalFieldOf("stability", 75.0D).forGetter(IdentityPart::stability)
        ).apply(i, IdentityPart::new));
    }

    private record TerritoryPart(
            EnumMap<EconomyCategory, Double> stock,
            double rpReserve,
            double exhaustion,
            long capitalRegionKey,
            BlockPos capitalPos,
            List<UUID> settlements,
            List<Long> holySites,
            Optional<UUID> leader,
            long foundedTick,
            NationStatus status
    ) {
        static final MapCodec<TerritoryPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                STOCK_CODEC.optionalFieldOf("stock", emptyStock()).forGetter(TerritoryPart::stock),
                Codec.DOUBLE.optionalFieldOf("rp_reserve", 0.0D).forGetter(TerritoryPart::rpReserve),
                Codec.DOUBLE.optionalFieldOf("exhaustion", 0.0D).forGetter(TerritoryPart::exhaustion),
                Codec.LONG.fieldOf("capital_region_key").forGetter(TerritoryPart::capitalRegionKey),
                BlockPos.CODEC.fieldOf("capital_pos").forGetter(TerritoryPart::capitalPos),
                UUIDUtil.CODEC.listOf().optionalFieldOf("settlements", List.of()).forGetter(TerritoryPart::settlements),
                Codec.LONG.listOf().optionalFieldOf("holy_sites", List.of()).forGetter(TerritoryPart::holySites),
                UUIDUtil.CODEC.optionalFieldOf("leader").forGetter(TerritoryPart::leader),
                Codec.LONG.optionalFieldOf("founded_tick", 0L).forGetter(TerritoryPart::foundedTick),
                NationStatus.CODEC.optionalFieldOf("status", NationStatus.ACTIVE).forGetter(TerritoryPart::status)
        ).apply(i, TerritoryPart::new));
    }

    public static final Codec<NationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IdentityPart.MAP_CODEC.forGetter(s -> new IdentityPart(
                    s.dataVersion(), s.civId(), s.name(), s.culture(), s.government(), s.personality(),
                    s.agenda(), s.hiddenAgenda(), s.eraIndex(), s.population(), s.stability()
            )),
            TerritoryPart.MAP_CODEC.forGetter(s -> new TerritoryPart(
                    s.stock(), s.rpReserve(), s.exhaustion(), s.capitalRegionKey(), s.capitalPos(),
                    s.settlements(), s.holySites(), s.leader(), s.foundedTick(), s.status()
            ))
    ).apply(instance, (id, ter) -> new NationState(
            id.dataVersion(), id.civId(), id.name(), id.culture(), id.government(), id.personality(),
            id.agenda(), id.hiddenAgenda(), id.eraIndex(), id.population(), id.stability(),
            ter.stock(), ter.rpReserve(), ter.exhaustion(), ter.capitalRegionKey(), ter.capitalPos(),
            ter.settlements(), ter.holySites(), ter.leader(), ter.foundedTick(), ter.status()
    )));

    public NationState {
        Objects.requireNonNull(civId, "civId");
        if (name == null || name.isBlank()) {
            name = "Unknown Polity";
        } else if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        Objects.requireNonNull(culture, "culture");
        Objects.requireNonNull(government, "government");
        Objects.requireNonNull(personality, "personality");
        Objects.requireNonNull(agenda, "agenda");
        hiddenAgenda = hiddenAgenda == null ? Optional.empty() : hiddenAgenda;
        eraIndex = Math.max(0, Math.min(10, eraIndex));
        population = Math.max(0.0D, population);
        stability = Math.max(0.0D, Math.min(100.0D, stability));
        stock = stock == null ? emptyStock() : copyEnumMap(stock);
        Objects.requireNonNull(capitalPos, "capitalPos");
        settlements = settlements == null ? List.of() : List.copyOf(settlements);
        holySites = holySites == null ? List.of() : List.copyOf(holySites);
        leader = leader == null ? Optional.empty() : leader;
        status = status == null ? NationStatus.ACTIVE : status;
    }

    public static EnumMap<EconomyCategory, Double> emptyStock() {
        EnumMap<EconomyCategory, Double> map = new EnumMap<>(EconomyCategory.class);
        for (EconomyCategory cat : EconomyCategory.values()) {
            map.put(cat, 0.0D);
        }
        return map;
    }

    private static EnumMap<EconomyCategory, Double> toEnumMap(Map<EconomyCategory, Double> raw) {
        EnumMap<EconomyCategory, Double> out = emptyStock();
        if (raw != null) {
            out.putAll(raw);
        }
        return out;
    }

    private static EnumMap<EconomyCategory, Double> copyEnumMap(EnumMap<EconomyCategory, Double> src) {
        EnumMap<EconomyCategory, Double> copy = emptyStock();
        copy.putAll(src);
        return copy;
    }
}
