package com.nightcity.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.skills.api.EffectCategory;
import com.nightcity.skills.api.GainFlag;
import com.nightcity.skills.api.GainResult;
import com.nightcity.skills.api.Skill;
import com.nightcity.skills.data.SkillCurves;
import com.nightcity.skills.data.TalentDef;
import com.nightcity.skills.data.TalentLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public record PlayerSkillsProfile(
        UUID playerId,
        int totalProficiency,
        int skillPoints,
        int growthLevel,
        Map<String, Integer> skills,
        List<String> talents,
        List<String> specialties,
        int reflectionTokens,
        Map<String, Float> fractionalRemainders,
        Map<String, Integer> dailyTotals
) {
    public static final Codec<PlayerSkillsProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("player_id").forGetter(PlayerSkillsProfile::playerId),
            Codec.INT.optionalFieldOf("total_proficiency", 0).forGetter(PlayerSkillsProfile::totalProficiency),
            Codec.INT.optionalFieldOf("skill_points", 1).forGetter(PlayerSkillsProfile::skillPoints),
            Codec.INT.optionalFieldOf("growth_level", 1).forGetter(PlayerSkillsProfile::growthLevel),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("skills", Map.of()).forGetter(PlayerSkillsProfile::skills),
            Codec.STRING.listOf().optionalFieldOf("talents", List.of()).forGetter(PlayerSkillsProfile::talents),
            Codec.STRING.listOf().optionalFieldOf("specialties", List.of()).forGetter(PlayerSkillsProfile::specialties),
            Codec.INT.optionalFieldOf("reflection_tokens", 0).forGetter(PlayerSkillsProfile::reflectionTokens),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("fractional", Map.of()).forGetter(PlayerSkillsProfile::fractionalRemainders),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("daily_totals", Map.of()).forGetter(PlayerSkillsProfile::dailyTotals)
    ).apply(instance, PlayerSkillsProfile::new));

    public PlayerSkillsProfile(
            UUID playerId,
            int totalProficiency,
            int skillPoints,
            int growthLevel,
            Map<String, Integer> skills,
            List<String> talents
    ) {
        this(playerId, totalProficiency, skillPoints, growthLevel, skills, talents, List.of(), 0, Map.of(), Map.of());
    }

    public PlayerSkillsProfile {
        skills = Map.copyOf(skills);
        talents = List.copyOf(talents);
        specialties = List.copyOf(specialties);
        fractionalRemainders = Map.copyOf(fractionalRemainders);
        dailyTotals = Map.copyOf(dailyTotals);
    }

    public static PlayerSkillsProfile empty(UUID playerId) {
        return new PlayerSkillsProfile(playerId, 0, 1, 1, Map.of(), List.of(), List.of(), 0, Map.of(), Map.of());
    }

    public boolean isSpecialty(Skill skill) {
        return specialties.contains(skill.shortId());
    }

    public boolean isSpecialty(String skill) {
        return Skill.resolve(skill).map(this::isSpecialty).orElse(false);
    }

    public int skillExperience(String skill) {
        return Skill.resolve(skill).map(s -> skills.getOrDefault(s.shortId(), 0)).orElse(0);
    }

    public int skillLevel(Skill skill) {
        long xp = skills.getOrDefault(skill.shortId(), 0);
        return SkillCurves.levelForXp(xp, isSpecialty(skill));
    }

    public int skillLevel(String skill) {
        return Skill.resolve(skill).map(this::skillLevel).orElse(0);
    }

    public long reserveXp(Skill skill) {
        long xp = skills.getOrDefault(skill.shortId(), 0);
        return SkillCurves.reserveXp(xp, isSpecialty(skill));
    }

    public PlayerSkillsProfile addProficiency(String skillRaw, int amount) {
        return gainProficiency(skillRaw, amount, Set.of(GainFlag.ADMIN), TalentLoader.TalentSnapshot.EMPTY, m -> true).profile();
    }

    public record ProfileGainOutcome(PlayerSkillsProfile profile, GainResult result) {
    }

    public ProfileGainOutcome gainProficiency(
            String skillRaw,
            int requestedAmount,
            Set<GainFlag> flags,
            TalentLoader.TalentSnapshot talentSnapshot,
            Predicate<String> isModLoaded
    ) {
        Optional<Skill> skillOpt = Skill.resolve(skillRaw);
        if (skillOpt.isEmpty() || requestedAmount <= 0) {
            return new ProfileGainOutcome(this, new GainResult(GainResult.Status.DROPPED_INVALID, requestedAmount, 0, 0, 0));
        }
        Skill skill = skillOpt.get();
        if (!skill.isActive(isModLoaded)) {
            return new ProfileGainOutcome(this, new GainResult(GainResult.Status.DROPPED_EXCLUDED, requestedAmount, 0, 0, 0));
        }
        int clamped = Math.min(500, requestedAmount);
        int currentLvl = skillLevel(skill);
        boolean bypassLimits = flags.contains(GainFlag.MILESTONE) || flags.contains(GainFlag.ADMIN);

        double base = clamped;
        if (!bypassLimits) {
            base *= SkillCurves.bandMultiplier(clamped, currentLvl);
        }
        if (isSpecialty(skill)) {
            base *= 1.25;
        }
        double learningMult = multiplier(skill, "learning/" + skill.shortId(), talentSnapshot, isModLoaded);
        base *= learningMult;

        Map<String, Integer> updatedDaily = new HashMap<>(dailyTotals);
        if (!bypassLimits) {
            int usedToday = updatedDaily.getOrDefault(skill.shortId(), 0);
            int cap = SkillCurves.dailySoftCap(currentLvl);
            double preSoftCap = base;
            if (usedToday >= cap) {
                base = preSoftCap * 0.25;
            } else if (usedToday + preSoftCap > cap) {
                double fullPart = cap - usedToday;
                double overPart = (usedToday + preSoftCap) - cap;
                base = fullPart + overPart * 0.25;
            }
            updatedDaily.put(skill.shortId(), usedToday + (int) Math.ceil(preSoftCap));
        }

        Map<String, Float> updatedFrac = new HashMap<>(fractionalRemainders);
        float totalFrac = updatedFrac.getOrDefault(skill.shortId(), 0f) + (float) base;
        int gain = (int) Math.floor(totalFrac);
        updatedFrac.put(skill.shortId(), totalFrac - gain);

        Map<String, Integer> updatedSkills = new HashMap<>(skills);
        int oldXp = updatedSkills.getOrDefault(skill.shortId(), 0);
        int newXp = (int) Math.min(SkillCurves.maxTotalSkillXp(), (long) oldXp + gain);
        int credited = newXp - oldXp;
        updatedSkills.put(skill.shortId(), newXp);

        int newTotal = totalProficiency + credited;
        int newGrowth = SkillCurves.growthLevelForTotalGain(newTotal);
        int spentPoints = spentTalentPoints(talentSnapshot);
        int newFreePoints = Math.max(0, newGrowth - spentPoints);

        PlayerSkillsProfile updated = new PlayerSkillsProfile(
                playerId, newTotal, newFreePoints, newGrowth, updatedSkills, talents,
                specialties, reflectionTokens, updatedFrac, updatedDaily
        );
        GainResult.Status status = credited > 0 ? GainResult.Status.CREDITED : GainResult.Status.ACCUMULATED;
        return new ProfileGainOutcome(updated, new GainResult(status, requestedAmount, clamped, credited, updated.skillLevel(skill)));
    }

    public int spentTalentPoints(TalentLoader.TalentSnapshot snapshot) {
        int sum = 0;
        for (String tid : talents) {
            Identifier id = normalizeTalentId(tid);
            TalentDef def = snapshot.byId().get(id);
            sum += def != null ? def.cost() : 1;
        }
        return sum;
    }

    public boolean hasTalent(String talentRaw) {
        String norm = normalizeTalentString(talentRaw);
        for (String t : talents) {
            if (normalizeTalentString(t).equals(norm)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTalentActive(TalentDef def, Predicate<String> isModLoaded) {
        if (!hasTalent(def.id().toString())) {
            return false;
        }
        if (def.skill() == Skill.ARCANA && !isModLoaded.test("tcbeyond")) {
            return false;
        }
        if (def.slot() == TalentDef.Slot.C && !isSpecialty(def.skill())) {
            return false;
        }
        return skillLevel(def.skill()) >= def.level();
    }

    public double multiplier(
            Skill skill,
            String effectPath,
            TalentLoader.TalentSnapshot talentSnapshot,
            Predicate<String> isModLoaded
    ) {
        if (!skill.isActive(isModLoaded)) {
            return 1.0;
        }
        Optional<EffectCategory> catOpt = EffectCategory.fromEffectPath(effectPath);
        if (catOpt.isEmpty()) {
            return 1.0;
        }
        EffectCategory cat = catOpt.get();
        int lvl = skillLevel(skill);
        double coeff = cat.coeffPerLevel();
        double levelBonus = 0.0;
        if (EffectCategory.pathMatches(skill.primaryEffect(), effectPath)) {
            levelBonus = coeff * lvl;
        } else if (EffectCategory.pathMatches(skill.secondaryEffect(), effectPath)) {
            levelBonus = coeff * lvl * 0.5;
        }

        double talentBonus = 0.0;
        Map<TalentDef.Slot, TalentDef> tree = talentSnapshot.bySkillAndSlot().getOrDefault(skill, Map.of());
        for (TalentDef def : tree.values()) {
            if (!isTalentActive(def, isModLoaded)) {
                continue;
            }
            for (TalentDef.Modifier mod : def.effectiveModifiers(isModLoaded)) {
                if (EffectCategory.pathMatches(mod.effect().getPath(), effectPath)) {
                    talentBonus += mod.amount();
                }
            }
        }

        double clampedBonus = Math.clamp(levelBonus + talentBonus, 0.0, cat.maxBonusCap());
        return cat.positive() ? (1.0 + clampedBonus) : (1.0 - clampedBonus);
    }

    public float opValue(String opRaw, float fallbackValue, TalentLoader.TalentSnapshot snapshot, Predicate<String> isModLoaded) {
        String target = opRaw.startsWith("tcskills:") ? opRaw.substring("tcskills:".length()) : opRaw;
        if (!target.startsWith("op/")) {
            target = "op/" + target;
        }
        boolean found = false;
        float maxVal = fallbackValue;
        for (String tid : talents) {
            TalentDef def = snapshot.byId().get(normalizeTalentId(tid));
            if (def == null || !isTalentActive(def, isModLoaded)) {
                continue;
            }
            for (TalentDef.OpGrant grant : def.effectiveOps(isModLoaded)) {
                if (grant.op().getPath().equals(target)) {
                    maxVal = found ? Math.max(maxVal, grant.value()) : grant.value();
                    found = true;
                }
            }
        }
        return found ? maxVal : fallbackValue;
    }

    public PlayerSkillsProfile setSpecialty(Skill skill) {
        int unlockedSlots = SkillCurves.unlockedSpecialtySlots(growthLevel);
        if (unlockedSlots <= 0 || isSpecialty(skill)) {
            return this;
        }
        List<String> updated = new ArrayList<>(specialties);
        if (updated.size() < unlockedSlots) {
            updated.add(skill.shortId());
        } else {
            updated.set(unlockedSlots - 1, skill.shortId());
        }
        return new PlayerSkillsProfile(
                playerId, totalProficiency, skillPoints, growthLevel, skills, talents,
                updated, reflectionTokens, fractionalRemainders, dailyTotals
        );
    }

    public Optional<PlayerSkillsProfile> commitTalents(
            List<Identifier> orderedTalents,
            TalentLoader.TalentSnapshot snapshot
    ) {
        Set<String> nextTalents = new LinkedHashSet<>(talents.stream().map(PlayerSkillsProfile::normalizeTalentString).toList());
        int free = skillPoints;
        for (Identifier tid : orderedTalents) {
            TalentDef def = snapshot.byId().get(tid);
            if (def == null || nextTalents.contains(def.id().toString())) {
                return Optional.empty();
            }
            if (skillLevel(def.skill()) < def.level() || free < def.cost()) {
                return Optional.empty();
            }
            if (def.slot() == TalentDef.Slot.C && !isSpecialty(def.skill())) {
                return Optional.empty();
            }
            if (def.exclusiveWith().isPresent() && nextTalents.contains(def.exclusiveWith().get().toString())) {
                return Optional.empty();
            }
            if (!prereqSatisfied(def, nextTalents, snapshot)) {
                return Optional.empty();
            }
            free -= def.cost();
            nextTalents.add(def.id().toString());
        }
        return Optional.of(new PlayerSkillsProfile(
                playerId, totalProficiency, free, growthLevel, skills, List.copyOf(nextTalents),
                specialties, reflectionTokens, fractionalRemainders, dailyTotals
        ));
    }

    private static boolean prereqSatisfied(TalentDef def, Set<String> learned, TalentLoader.TalentSnapshot snapshot) {
        Map<TalentDef.Slot, TalentDef> tree = snapshot.bySkillAndSlot().getOrDefault(def.skill(), Map.of());
        return switch (def.slot()) {
            case T1 -> true;
            case T2 -> hasSlot(tree, learned, TalentDef.Slot.T1);
            case T3A, T3B -> hasSlot(tree, learned, TalentDef.Slot.T2);
            case T4 -> hasSlot(tree, learned, TalentDef.Slot.T3A) || hasSlot(tree, learned, TalentDef.Slot.T3B);
            case T5 -> hasSlot(tree, learned, TalentDef.Slot.T4);
            case C -> hasSlot(tree, learned, TalentDef.Slot.T5);
        };
    }

    private static boolean hasSlot(Map<TalentDef.Slot, TalentDef> tree, Set<String> learned, TalentDef.Slot slot) {
        TalentDef d = tree.get(slot);
        return d != null && learned.contains(d.id().toString());
    }

    public PlayerSkillsProfile learnTalent(String talent) {
        List<String> updatedTalents = new ArrayList<>(talents);
        updatedTalents.add(normalizeTalentString(talent));
        return new PlayerSkillsProfile(
                playerId, totalProficiency, Math.max(0, skillPoints - 1), growthLevel,
                skills, updatedTalents, specialties, reflectionTokens, fractionalRemainders, dailyTotals
        );
    }

    public PlayerSkillsProfile respecAll(TalentLoader.TalentSnapshot snapshot) {
        return new PlayerSkillsProfile(
                playerId, totalProficiency, growthLevel, growthLevel,
                skills, List.of(), specialties, reflectionTokens, fractionalRemainders, dailyTotals
        );
    }

    private static Identifier normalizeTalentId(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath("tcskills", raw);
    }

    private static String normalizeTalentString(String raw) {
        return normalizeTalentId(raw).toString();
    }
}
