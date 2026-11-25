package com.nightcity.skills.data;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.nightcity.skills.TSkills;
import com.nightcity.skills.api.EffectCategory;
import com.nightcity.skills.api.Skill;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TalentLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            d -> d.convert(JsonOps.INSTANCE).getValue(),
            e -> new Dynamic<>(JsonOps.INSTANCE, e)
    );
    private static final FileToIdConverter LISTER = FileToIdConverter.json("tc_talent");

    public record TalentSnapshot(
            Map<Identifier, TalentDef> byId,
            Map<Skill, Map<TalentDef.Slot, TalentDef>> bySkillAndSlot,
            List<String> errors,
            List<String> warnings
    ) {
        public static final TalentSnapshot EMPTY = new TalentSnapshot(Map.of(), Map.of(), List.of(), List.of());
    }

    private volatile TalentSnapshot current = TalentSnapshot.EMPTY;

    public TalentLoader() {
        super(JSON_CODEC, LISTER);
    }

    public TalentSnapshot snapshot() {
        return current;
    }

    public void setSnapshotForTesting(TalentSnapshot snapshot) {
        this.current = snapshot;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> rawMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        TalentSnapshot compiled = compile(rawMap);
        this.current = compiled;
        LOGGER.info("[tcskills] Loaded {} talents ({} errors, {} warnings)",
                compiled.byId().size(), compiled.errors().size(), compiled.warnings().size());
    }

    public static TalentSnapshot compile(Map<Identifier, JsonElement> rawMap) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<Identifier, TalentDef> byId = new LinkedHashMap<>();
        Map<Skill, Map<TalentDef.Slot, TalentDef>> bySkillSlot = new EnumMap<>(Skill.class);

        List<Map.Entry<Identifier, JsonElement>> entries = new ArrayList<>(rawMap.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Identifier, JsonElement> entry : entries) {
            Identifier fileId = entry.getKey();
            Identifier canonicalId = Identifier.fromNamespaceAndPath(TSkills.MOD_ID, fileId.getPath());
            if (entry.getValue() == null || !entry.getValue().isJsonObject()) {
                errors.add(canonicalId + ": root JSON is not an object");
                continue;
            }
            DataResult<TalentDef.RawTalentJson> parsed = TalentDef.RawTalentJson.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
            if (parsed.error().isPresent() || parsed.result().isEmpty()) {
                errors.add(canonicalId + ": " + parsed.error().map(DataResult.Error::message).orElse("decode error"));
                continue;
            }
            TalentDef.RawTalentJson raw = parsed.result().get();
            if (raw.cost() != raw.slot().expectedCost() || raw.level() != raw.slot().expectedLevel()) {
                errors.add(canonicalId + ": cost/level mismatch for slot " + raw.slot().jsonKey());
                continue;
            }
            if (!validateModifierRange(raw.modifiers()) || (raw.fallback().isPresent() && !validateModifierRange(raw.fallback().get().modifiers()))) {
                errors.add(canonicalId + ": modifier amount outside (0, 0.25]");
                continue;
            }
            if (raw.skill() != Skill.ARCANA && raw.requiresMod().isPresent()) {
                if (raw.fallback().isEmpty() || raw.fallback().get().isEmpty()) {
                    errors.add(canonicalId + ": requires_mod '" + raw.requiresMod().get() + "' missing non-empty fallback");
                    continue;
                }
            }

            Map<TalentDef.Slot, TalentDef> slotMap = bySkillSlot.computeIfAbsent(raw.skill(), k -> new EnumMap<>(TalentDef.Slot.class));
            TalentDef existing = slotMap.get(raw.slot());
            if (existing != null && !existing.id().equals(canonicalId)) {
                errors.add(canonicalId + ": slot conflict on " + raw.skill().shortId() + "/" + raw.slot().jsonKey());
                continue;
            }

            TalentDef def = new TalentDef(
                    canonicalId, raw.skill(), raw.slot(), raw.cost(), raw.level(),
                    raw.eraGate(), raw.exclusiveWith(), List.copyOf(raw.modifiers()), List.copyOf(raw.ops()),
                    raw.requiresMod(), raw.fallback(), raw.icon()
            );
            slotMap.put(raw.slot(), def);
            byId.put(canonicalId, def);
        }

        // Headroom check per tree across every stackable query path under primaryEffect (05 §5.7.2 & §6.4)
        for (Map.Entry<Skill, Map<TalentDef.Slot, TalentDef>> treeEntry : bySkillSlot.entrySet()) {
            Skill skill = treeEntry.getKey();
            Optional<EffectCategory> catOpt = EffectCategory.fromEffectPath(skill.primaryEffect());
            if (catOpt.isEmpty()) {
                continue;
            }
            double headroom = catOpt.get().talentHeadroom();
            Map<TalentDef.Slot, TalentDef> slots = treeEntry.getValue();
            java.util.Set<String> candidateQueries = new java.util.LinkedHashSet<>();
            candidateQueries.add(skill.primaryEffect());
            for (TalentDef td : slots.values()) {
                for (TalentDef.Modifier m : td.modifiers()) {
                    if (EffectCategory.pathMatches(skill.primaryEffect(), m.effect().getPath())) {
                        candidateQueries.add(m.effect().getPath());
                    }
                }
                td.fallback().ifPresent(fb -> {
                    for (TalentDef.Modifier m : fb.modifiers()) {
                        if (EffectCategory.pathMatches(skill.primaryEffect(), m.effect().getPath())) {
                            candidateQueries.add(m.effect().getPath());
                        }
                    }
                });
            }
            double worstStack = 0.0;
            for (String queryPath : candidateQueries) {
                double baseSum = queryBonusOf(slots.get(TalentDef.Slot.T1), queryPath)
                        + queryBonusOf(slots.get(TalentDef.Slot.T2), queryPath)
                        + queryBonusOf(slots.get(TalentDef.Slot.T4), queryPath)
                        + queryBonusOf(slots.get(TalentDef.Slot.T5), queryPath)
                        + queryBonusOf(slots.get(TalentDef.Slot.C), queryPath);
                double maxBranch = Math.max(
                        queryBonusOf(slots.get(TalentDef.Slot.T3A), queryPath),
                        queryBonusOf(slots.get(TalentDef.Slot.T3B), queryPath)
                );
                worstStack = Math.max(worstStack, baseSum + maxBranch);
            }
            if (worstStack > headroom + 1e-5) {
                warnings.add(skill.shortId() + ": primary talent sum " + worstStack + " exceeds headroom " + headroom);
            }
        }

        Map<Skill, Map<TalentDef.Slot, TalentDef>> immutableBySkill = new EnumMap<>(Skill.class);
        for (Map.Entry<Skill, Map<TalentDef.Slot, TalentDef>> e : bySkillSlot.entrySet()) {
            immutableBySkill.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        return new TalentSnapshot(
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(immutableBySkill),
                List.copyOf(errors),
                List.copyOf(warnings)
        );
    }

    private static boolean validateModifierRange(List<TalentDef.Modifier> mods) {
        for (TalentDef.Modifier m : mods) {
            if (m.amount() <= 0.0f || m.amount() > 0.25001f || Float.isNaN(m.amount())) {
                return false;
            }
        }
        return true;
    }

    private static double queryBonusOf(TalentDef def, String queryPath) {
        if (def == null) {
            return 0.0;
        }
        double activeSum = sumMatchingQuery(def.modifiers(), queryPath);
        double fallbackSum = def.fallback().map(fb -> sumMatchingQuery(fb.modifiers(), queryPath)).orElse(0.0);
        return Math.max(activeSum, fallbackSum);
    }

    private static double sumMatchingQuery(List<TalentDef.Modifier> mods, String queryPath) {
        double total = 0.0;
        for (TalentDef.Modifier m : mods) {
            if (EffectCategory.pathMatches(m.effect().getPath(), queryPath)) {
                total += m.amount();
            }
        }
        return total;
    }
}
