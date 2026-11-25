package com.nightcity.technology.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.technology.api.DifficultyApi;
import com.nightcity.technology.api.EraDefinition;
import com.nightcity.technology.api.KnowledgeSourceType;
import com.nightcity.technology.api.TechDefinition;
import com.nightcity.technology.api.TechEra;
import com.nightcity.technology.api.TechState;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CivTechState {
    public static final Codec<CivTechState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("civ_id").forGetter(CivTechState::civId),
            Codec.INT.optionalFieldOf("era", 0).forGetter(s -> s.currentEra.index()),
            Identifier.CODEC.listOf().optionalFieldOf("completed", List.of()).forGetter(s -> List.copyOf(s.completed)),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("progress", Map.of()).forGetter(CivTechState::progressMap),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("pool_spent", Map.of()).forGetter(CivTechState::poolSpentMap),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("knowledge", Map.of()).forGetter(CivTechState::knowledgeMap),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("daily_knowledge", Map.of()).forGetter(CivTechState::dailyKnowledgeMap),
            Identifier.CODEC.listOf().optionalFieldOf("insight_fired", List.of()).forGetter(s -> List.copyOf(s.insightFired)),
            Codec.INT.optionalFieldOf("pool", 0).forGetter(CivTechState::pool),
            Identifier.CODEC.listOf().optionalFieldOf("active_research", List.of()).forGetter(s -> List.copyOf(s.activeSiteProjects)),
            Identifier.CODEC.listOf().optionalFieldOf("milestones_done", List.of()).forGetter(s -> List.copyOf(s.completedMilestones))
    ).apply(i, CivTechState::new));

    private final UUID civId;
    private TechEra currentEra;
    private final Set<Identifier> completed;
    private final Map<Identifier, Integer> progress;
    private final Map<Identifier, Integer> poolSpent;
    private final Map<Identifier, Integer> knowledge;
    private final Map<String, Integer> dailyKnowledge;
    private final Set<Identifier> insightFired;
    private int pool;
    private final Set<Identifier> activeSiteProjects;
    private final Set<Identifier> completedMilestones;

    public CivTechState(UUID civId) {
        this(civId, 0, List.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(), 0, List.of(), List.of());
    }

    public CivTechState(
            UUID civId,
            int eraIndex,
            List<Identifier> completed,
            Map<Identifier, Integer> progress,
            Map<Identifier, Integer> poolSpent,
            Map<Identifier, Integer> knowledge,
            Map<String, Integer> dailyKnowledge,
            List<Identifier> insightFired,
            int pool,
            List<Identifier> activeSiteProjects,
            List<Identifier> completedMilestones
    ) {
        this.civId = civId;
        this.currentEra = TechEra.byIndex(eraIndex);
        this.completed = new LinkedHashSet<>(completed);
        this.progress = new LinkedHashMap<>(progress);
        this.poolSpent = new LinkedHashMap<>(poolSpent);
        this.knowledge = new LinkedHashMap<>(knowledge);
        this.dailyKnowledge = new LinkedHashMap<>(dailyKnowledge);
        this.insightFired = new LinkedHashSet<>(insightFired);
        this.pool = Math.max(0, pool);
        this.activeSiteProjects = new LinkedHashSet<>(activeSiteProjects);
        this.completedMilestones = new LinkedHashSet<>(completedMilestones);
    }

    public UUID civId() {
        return civId;
    }

    public TechEra currentEra() {
        return currentEra;
    }

    public int pool() {
        return pool;
    }

    public Set<Identifier> completed() {
        return Collections.unmodifiableSet(completed);
    }

    public Map<Identifier, Integer> progressMap() {
        return Collections.unmodifiableMap(progress);
    }

    public Map<Identifier, Integer> poolSpentMap() {
        return Collections.unmodifiableMap(poolSpent);
    }

    public Map<Identifier, Integer> knowledgeMap() {
        return Collections.unmodifiableMap(knowledge);
    }

    public Map<String, Integer> dailyKnowledgeMap() {
        return Collections.unmodifiableMap(dailyKnowledge);
    }

    public int knowledge(Identifier techId) {
        return knowledge.getOrDefault(techId, 0);
    }

    public int progress(Identifier techId) {
        return progress.getOrDefault(techId, 0);
    }

    public boolean isUnlocked(Identifier techId) {
        return completed.contains(techId);
    }

    public void resetDailyCaps() {
        dailyKnowledge.clear();
    }

    public int addPoolRp(int amount, double poolCapMultiplier) {
        if (amount <= 0) {
            return pool;
        }
        int cap = (int) Math.ceil(poolCapMultiplier * currentEra.averageRp());
        pool = Math.min(cap, pool + amount);
        return pool;
    }

    public record KnowledgeGainOutcome(int added, int newTotal, boolean thresholdJustReached, int insightRpAwarded) {
    }

    public KnowledgeGainOutcome addKnowledge(
            TechDefinition def,
            KnowledgeSourceType sourceType,
            int rawAmount,
            double actorMult,
            DifficultyApi.Difficulty difficulty,
            double farEraCapRatio,
            double defaultDailyCapRatio,
            double poolCapMultiplier
    ) {
        if (rawAmount <= 0) {
            return new KnowledgeGainOutcome(0, knowledge(def.id()), false, 0);
        }
        double sourceMult = sourceType.defaultMultiplier();
        if (sourceType == KnowledgeSourceType.ARTIFACT && completed.contains(Identifier.fromNamespaceAndPath("tctechnology", "bronze/artifact_study"))) {
            sourceMult = 1.0;
        }
        int computed = (int) Math.max(1, Math.round(rawAmount * sourceMult * actorMult * difficulty.knowledgeGain()));
        String dailyKey = def.id() + "|" + sourceType.name();
        int dailyCap = (int) Math.max(1, Math.ceil(def.threshold() * defaultDailyCapRatio));
        int usedToday = dailyKnowledge.getOrDefault(dailyKey, 0);
        int allowedToday = Math.max(0, dailyCap - usedToday);
        int delta = Math.min(computed, allowedToday);
        if (delta <= 0) {
            return new KnowledgeGainOutcome(0, knowledge(def.id()), false, 0);
        }

        boolean codifiedCrafts = completed.contains(Identifier.fromNamespaceAndPath("tctechnology", "medieval/codified_crafts"));
        int maxKnowledge = def.era().index() >= currentEra.index() + 2
                ? (int) Math.floor(def.threshold() * farEraCapRatio)
                : (int) Math.ceil(def.threshold() * (codifiedCrafts ? 1.5 : 1.0));
        int before = knowledge(def.id());
        int after = Math.min(Math.max(before, maxKnowledge), before + delta);
        int actualAdded = after - before;
        if (actualAdded > 0) {
            knowledge.put(def.id(), after);
            dailyKnowledge.put(dailyKey, usedToday + actualAdded);
        }

        boolean firedInsight = false;
        int insightRp = 0;
        if (after >= def.threshold() && !insightFired.contains(def.id())) {
            insightFired.add(def.id());
            firedInsight = true;
            insightRp = (int) Math.max(1, Math.round(def.rp() * difficulty.insightBonus()));
            addPoolRp(insightRp, poolCapMultiplier);
        }
        return new KnowledgeGainOutcome(actualAdded, after, firedInsight, insightRp);
    }

    public int effectiveCost(TechDefinition def, DifficultyApi.Difficulty difficulty, double externalFactor) {
        double hindsight = def.era().index() < currentEra.index()
                ? Math.max(0.50, Math.pow(0.80, currentEra.index() - def.era().index()))
                : 1.0;
        boolean ark = completed.contains(Identifier.fromNamespaceAndPath("tctechnology", "space/knowledge_ark"))
                && def.era().index() <= currentEra.index() - 2;
        double arkFactor = ark ? 0.5 : 1.0;
        double floorRatio = ark ? 0.25 : 0.40;
        int floor = (int) Math.ceil(floorRatio * def.rp() * difficulty.techCost());
        int raw = (int) Math.round(def.rp() * difficulty.techCost() * Math.clamp(externalFactor, 0.5, 1.0) * hindsight * arkFactor);
        return Math.max(1, Math.max(floor, raw));
    }

    public TechState stateOf(TechDefinition def, Map<Identifier, TechDefinition> graph, int preResearchLimit) {
        if (completed.contains(def.id())) {
            return TechState.COMPLETED;
        }
        int k = knowledge(def.id());
        if (activeSiteProjects.contains(def.id()) || progress(def.id()) > 0) {
            return TechState.RESEARCHING;
        }
        if (def.era().index() >= currentEra.index() + 2 && k <= 0) {
            return TechState.HIDDEN;
        }
        if (k <= 0) {
            return TechState.UNDISCOVERED;
        }
        if (k < def.threshold()) {
            return TechState.KNOWN;
        }
        for (Identifier prereq : def.prerequisites()) {
            if (!completed.contains(prereq)) {
                return TechState.KNOWN;
            }
        }
        if (def.era().index() > currentEra.index() + 1) {
            return TechState.KNOWN;
        }
        if (def.era().index() == currentEra.index() + 1) {
            int nextEraActiveOrDone = countNextEraPreResearch(graph);
            if (nextEraActiveOrDone >= preResearchLimit) {
                return TechState.KNOWN;
            }
        }
        return TechState.RESEARCHABLE;
    }

    public int countNextEraPreResearch(Map<Identifier, TechDefinition> graph) {
        int targetIdx = currentEra.index() + 1;
        int count = 0;
        for (TechDefinition d : graph.values()) {
            if (d.era().index() == targetIdx) {
                if (completed.contains(d.id()) || activeSiteProjects.contains(d.id()) || progress(d.id()) > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean assignActiveProject(TechDefinition def, Map<Identifier, TechDefinition> graph, int preResearchLimit) {
        TechState st = stateOf(def, graph, preResearchLimit);
        if (st != TechState.RESEARCHABLE && st != TechState.RESEARCHING) {
            return false;
        }
        activeSiteProjects.add(def.id());
        return true;
    }

    public boolean contributeSiteRp(
            TechDefinition def,
            int rpAmount,
            int cost,
            Map<Identifier, TechDefinition> graph,
            int preResearchLimit,
            double poolCapMultiplier
    ) {
        if (rpAmount <= 0 || completed.contains(def.id())) {
            return false;
        }
        TechState st = stateOf(def, graph, preResearchLimit);
        if (st != TechState.RESEARCHABLE && st != TechState.RESEARCHING) {
            return false;
        }
        int cur = progress(def.id());
        int updated = cur + rpAmount;
        if (updated >= cost) {
            int overflow = updated - cost;
            completeTech(def.id());
            if (overflow > 0) {
                addPoolRp(overflow, poolCapMultiplier);
            }
            return true;
        }
        progress.put(def.id(), updated);
        activeSiteProjects.add(def.id());
        return false;
    }

    public int investFromPool(
            TechDefinition def,
            int requestedRp,
            int cost,
            double maxPoolShare,
            Map<Identifier, TechDefinition> graph,
            int preResearchLimit
    ) {
        if (requestedRp <= 0 || pool <= 0 || completed.contains(def.id())) {
            return 0;
        }
        TechState st = stateOf(def, graph, preResearchLimit);
        if (st != TechState.RESEARCHABLE && st != TechState.RESEARCHING) {
            return 0;
        }
        int maxPoolAllowed = (int) Math.floor(cost * maxPoolShare);
        int alreadyFromPool = poolSpent.getOrDefault(def.id(), 0);
        int remainingPoolAllowance = Math.max(0, maxPoolAllowed - alreadyFromPool);
        int neededToComplete = Math.max(0, cost - progress(def.id()));
        int actual = Math.min(Math.min(requestedRp, pool), Math.min(remainingPoolAllowance, neededToComplete));
        if (actual <= 0) {
            return 0;
        }
        pool -= actual;
        poolSpent.put(def.id(), alreadyFromPool + actual);
        int newProg = progress(def.id()) + actual;
        if (newProg >= cost) {
            completeTech(def.id());
        } else {
            progress.put(def.id(), newProg);
        }
        return actual;
    }

    public void completeTech(Identifier techId) {
        completed.add(techId);
        progress.remove(techId);
        poolSpent.remove(techId);
        activeSiteProjects.remove(techId);
    }

    public void completeMilestone(Identifier milestoneId) {
        completedMilestones.add(milestoneId);
    }

    public EraDefinition.EraProgress eraProgress(EraDefinition eraDef, int effectivePopulation, boolean settlementsLoaded) {
        List<Identifier> ks = eraDef != null ? eraDef.keystones() : List.of();
        int done = 0;
        for (Identifier k : ks) {
            if (completed.contains(k)) {
                done++;
            }
        }
        boolean milestoneOk = eraDef == null || completedMilestones.contains(eraDef.milestone());
        int popReq = eraDef == null ? 6 : (settlementsLoaded ? eraDef.populationMin() : eraDef.fallbackPopulationMin());
        Optional<TechEra> nextEra = currentEra.next();
        boolean canDeclare = nextEra.isPresent() && done >= ks.size() && milestoneOk && effectivePopulation >= popReq;
        return new EraDefinition.EraProgress(currentEra, nextEra, done, ks.size(), milestoneOk, effectivePopulation, popReq, canDeclare);
    }

    public boolean advanceToNextEra() {
        Optional<TechEra> next = currentEra.next();
        if (next.isEmpty()) {
            return false;
        }
        currentEra = next.get();
        return true;
    }
}
