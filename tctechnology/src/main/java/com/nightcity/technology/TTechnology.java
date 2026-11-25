package com.nightcity.technology;

import com.nightcity.technology.api.CostFactorProvider;
import com.nightcity.technology.api.DifficultyApi;
import com.nightcity.technology.api.EraDefinition;
import com.nightcity.technology.api.KnowledgeSourceType;
import com.nightcity.technology.api.TechApi;
import com.nightcity.technology.api.TechDefinition;
import com.nightcity.technology.api.TechEra;
import com.nightcity.technology.api.TechState;
import com.nightcity.technology.api.event.EraAdvancedEvent;
import com.nightcity.technology.api.event.KnowledgeThresholdReachedEvent;
import com.nightcity.technology.api.event.TechnologyUnlockedEvent;
import com.nightcity.technology.compat.tcivilization.TcivilizationBridge;
import com.nightcity.technology.data.CivTechState;
import com.nightcity.technology.data.RecipeGateService;
import com.nightcity.technology.data.TechDefinitionLoader;
import com.nightcity.technology.registry.TechnologyRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(TTechnology.MOD_ID)
public final class TTechnology {
    public static final String MOD_ID = "tctechnology";

    private static final TechDefinitionLoader LOADER = new TechDefinitionLoader();
    private static final RecipeGateService RECIPE_GATES = new RecipeGateService();
    private static final Map<Identifier, CostFactorProvider> COST_FACTORS = new LinkedHashMap<>();
    private static final Map<UUID, CivTechState> FALLBACK_MEMORY_CIVS = new ConcurrentHashMap<>();

    private static final DifficultyApi DIFFICULTY_API = TechConfig::resolveDifficulty;
    private static final TechApi TECH_API = new TechApiImpl();

    public TTechnology(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, TechConfig.SERVER_SPEC, "tctechnology-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, TechConfig.CLIENT_SPEC, "tctechnology-client.toml");
        TechnologyRegistries.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(TTechnologyEvents.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TcivilizationBridge::init);
    }

    public static TechDefinitionLoader loader() {
        return LOADER;
    }

    public static RecipeGateService recipeGates() {
        return RECIPE_GATES;
    }

    public static TechApi api() {
        return TECH_API;
    }

    public static DifficultyApi difficulty() {
        return DIFFICULTY_API;
    }

    public static CivTechState resolveCivState(UUID civId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.overworld() != null) {
            return TechnologySavedData.get(server.overworld()).civ(civId);
        }
        return FALLBACK_MEMORY_CIVS.computeIfAbsent(civId, CivTechState::new);
    }

    public static void markServerDirty() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.overworld() != null) {
            TechnologySavedData.get(server.overworld()).markModified();
        }
    }

    private static final class TechApiImpl implements TechApi {
        @Override
        public Optional<TechDefinition> definition(Identifier tech) {
            return Optional.ofNullable(LOADER.snapshot().definitions().get(tech));
        }

        @Override
        public Collection<TechDefinition> all() {
            return LOADER.snapshot().definitions().values();
        }

        @Override
        public TechState state(UUID civ, Identifier tech) {
            Optional<TechDefinition> def = definition(tech);
            if (def.isEmpty()) {
                return TechState.HIDDEN;
            }
            int limit = TechConfig.SERVER_SPEC.isLoaded() ? TechConfig.PRE_RESEARCH_LIMIT.get() : 3;
            return resolveCivState(civ).stateOf(def.get(), LOADER.snapshot().definitions(), limit);
        }

        @Override
        public boolean isUnlocked(UUID civ, Identifier tech) {
            return resolveCivState(civ).isUnlocked(tech);
        }

        @Override
        public boolean isRecipeUnlocked(UUID civ, Identifier recipe) {
            return RECIPE_GATES.isRecipeUnlocked(civ, recipe, this::isUnlocked);
        }

        @Override
        public boolean isItemUnlocked(UUID civ, Identifier itemId) {
            return RECIPE_GATES.isItemUnlocked(civ, itemId, this::isUnlocked);
        }

        @Override
        public Optional<TechEra> era(UUID civ) {
            return Optional.of(resolveCivState(civ).currentEra());
        }

        @Override
        public EraDefinition.EraProgress eraProgress(UUID civ) {
            CivTechState state = resolveCivState(civ);
            EraDefinition eraDef = LOADER.snapshot().eras().get(state.currentEra());
            boolean settlements = ModList.get() != null && ModList.get().isLoaded("tcsettlements");
            int effectivePop = Math.max(1, state.completed().size());
            return state.eraProgress(eraDef, effectivePop, settlements);
        }

        @Override
        public int knowledge(UUID civ, Identifier tech) {
            return resolveCivState(civ).knowledge(tech);
        }

        @Override
        public int cost(UUID civ, Identifier tech) {
            Optional<TechDefinition> def = definition(tech);
            if (def.isEmpty()) {
                return 0;
            }
            double factor = 1.0;
            for (CostFactorProvider provider : COST_FACTORS.values()) {
                factor *= Math.clamp(provider.factor(civ, tech), 0.5, 1.0);
            }
            return resolveCivState(civ).effectiveCost(def.get(), DIFFICULTY_API.current(), factor);
        }

        @Override
        public void addKnowledge(UUID civ, Identifier tech, KnowledgeSourceType type, int amount, Entity actor) {
            Optional<TechDefinition> def = definition(tech);
            if (def.isEmpty()) {
                return;
            }
            double actorMult = (actor == null || actor instanceof ServerPlayer) ? 1.0 : 0.25;
            CivTechState state = resolveCivState(civ);
            var outcome = state.addKnowledge(def.get(), type, amount, actorMult, DIFFICULTY_API.current(), 0.5, 0.5, 3.0);
            markServerDirty();
            if (outcome.thresholdJustReached()) {
                NeoForge.EVENT_BUS.post(new KnowledgeThresholdReachedEvent(civ, tech, outcome.insightRpAwarded()));
            }
        }

        @Override
        public void fireTrigger(ServerPlayer player, Identifier trigger, Map<String, String> context) {
            for (TechDefinition def : all()) {
                for (TechDefinition.KnowledgeSource src : def.sources()) {
                    if (src.trigger().equals(trigger)) {
                        addKnowledge(PLAYER_CIV_ID, def.id(), src.type(), src.amount(), player);
                    }
                }
            }
        }

        @Override
        public void addResearchPoints(UUID civ, int rp, Identifier reason) {
            resolveCivState(civ).addPoolRp(rp, 3.0);
            markServerDirty();
        }

        @Override
        public void grant(UUID civ, Identifier tech, Identifier reason) {
            CivTechState state = resolveCivState(civ);
            if (!state.isUnlocked(tech)) {
                state.completeTech(tech);
                markServerDirty();
                NeoForge.EVENT_BUS.post(new TechnologyUnlockedEvent(civ, tech, reason, 0L));
            }
        }

        @Override
        public boolean advanceEra(UUID civ, boolean ceremony) {
            CivTechState state = resolveCivState(civ);
            EraDefinition.EraProgress prog = eraProgress(civ);
            if (!prog.canDeclare() || prog.next().isEmpty()) {
                return false;
            }
            TechEra from = state.currentEra();
            TechEra to = prog.next().get();
            EraAdvancedEvent.Pre pre = new EraAdvancedEvent.Pre(civ, from, to, civ);
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) {
                return false;
            }
            if (!state.advanceToNextEra()) {
                return false;
            }
            markServerDirty();
            NeoForge.EVENT_BUS.post(new EraAdvancedEvent(civ, from, to, 0L, 1));
            return true;
        }

        @Override
        public float diffusionPressure(UUID civ, Identifier tech) {
            return 0.0f;
        }

        @Override
        public synchronized void registerCostFactor(CostFactorProvider provider) {
            COST_FACTORS.putIfAbsent(provider.id(), provider);
        }

        @Override
        public void invalidateCost(UUID civ, Identifier tech) {
        }
    }
}
