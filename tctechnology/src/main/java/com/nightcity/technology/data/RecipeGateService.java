package com.nightcity.technology.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nightcity.technology.TechConfig;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.UUID;

public final class RecipeGateService extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final FileToIdConverter LISTER = FileToIdConverter.json("tc_recipe_gate");

    public record GateRule(
            Identifier tech,
            Optional<String> whenModAbsent,
            List<Identifier> recipes,
            List<Identifier> items,
            List<Identifier> blocks
    ) {
    }

    private volatile Map<Identifier, Identifier> recipeToTech = Map.of();
    private volatile Map<Identifier, Identifier> itemToTech = Map.of();
    private volatile Map<Identifier, Identifier> blockToTech = Map.of();
    private volatile Map<Identifier, GateRule> fallbackRecipeRules = Map.of();
    private volatile Set<Identifier> removedVanillaRecipes = defaultRemovedVanillaRecipes();

    public RecipeGateService() {
        super(TechDefinitionLoader.JSON_CODEC, LISTER);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        loadFromJsonMap(objects);
    }

    public void loadFromJsonMap(Map<Identifier, JsonElement> objects) {
        Map<Identifier, Identifier> rMap = new HashMap<>();
        Map<Identifier, Identifier> iMap = new HashMap<>();
        Map<Identifier, Identifier> bMap = new HashMap<>();
        Map<Identifier, GateRule> fbMap = new HashMap<>();
        Set<Identifier> removed = new HashSet<>(defaultRemovedVanillaRecipes());

        for (Map.Entry<Identifier, JsonElement> entry : objects.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getValue().getAsJsonObject();
            if (obj.has("removed_recipes") && obj.get("removed_recipes").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("removed_recipes")) {
                    removed.add(Identifier.parse(el.getAsString()));
                }
                continue;
            }
            if (!obj.has("tech")) {
                continue;
            }
            Identifier techId = Identifier.parse(obj.get("tech").getAsString());
            Optional<String> whenAbsent = obj.has("when_mod_absent")
                    ? Optional.of(obj.get("when_mod_absent").getAsString())
                    : Optional.empty();
            List<Identifier> recipes = readIdList(obj, "recipes");
            List<Identifier> items = readIdList(obj, "items");
            List<Identifier> blocks = readIdList(obj, "blocks");
            GateRule rule = new GateRule(techId, whenAbsent, recipes, items, blocks);

            if (whenAbsent.isPresent() || entry.getKey().getPath().startsWith("fallback/")) {
                for (Identifier r : recipes) {
                    fbMap.put(r, rule);
                }
            } else {
                for (Identifier r : recipes) {
                    rMap.put(r, techId);
                }
                for (Identifier it : items) {
                    iMap.put(it, techId);
                }
                for (Identifier bl : blocks) {
                    bMap.put(bl, techId);
                }
            }
        }
        this.recipeToTech = Collections.unmodifiableMap(rMap);
        this.itemToTech = Collections.unmodifiableMap(iMap);
        this.blockToTech = Collections.unmodifiableMap(bMap);
        this.fallbackRecipeRules = Collections.unmodifiableMap(fbMap);
        this.removedVanillaRecipes = Collections.unmodifiableSet(removed);
    }

    public void registerInlineRecipeGate(Identifier recipeId, Identifier requiredTech) {
        Map<Identifier, Identifier> mutable = new HashMap<>(recipeToTech);
        mutable.put(recipeId, requiredTech);
        this.recipeToTech = Collections.unmodifiableMap(mutable);
    }

    public boolean isRecipeUnlocked(
            UUID civ,
            Identifier recipeId,
            BiPredicate<UUID, Identifier> isTechUnlocked,
            Predicate<String> isModLoaded,
            boolean enableFallbackStubs,
            boolean replaceCampfire
    ) {
        if (!replaceCampfire && ("minecraft:campfire".equals(recipeId.toString()) || "minecraft:soul_campfire".equals(recipeId.toString()))) {
            return true;
        }
        if (removedVanillaRecipes.contains(recipeId)) {
            if (!enableFallbackStubs) {
                return false;
            }
            GateRule fb = fallbackRecipeRules.get(recipeId);
            if (fb != null && (fb.whenModAbsent().isEmpty() || !isModLoaded.test(fb.whenModAbsent().get()))) {
                return isTechUnlocked.test(civ, fb.tech());
            }
            return false;
        }
        Identifier directGate = recipeToTech.get(recipeId);
        if (directGate != null) {
            return isTechUnlocked.test(civ, directGate);
        }
        GateRule fb = fallbackRecipeRules.get(recipeId);
        if (fb != null && (fb.whenModAbsent().isEmpty() || !isModLoaded.test(fb.whenModAbsent().get()))) {
            return isTechUnlocked.test(civ, fb.tech());
        }
        return true;
    }

    public boolean isRecipeUnlocked(UUID civ, Identifier recipeId, BiPredicate<UUID, Identifier> isTechUnlocked) {
        boolean stubs = !TechConfig.SERVER_SPEC.isLoaded() || TechConfig.ENABLE_STUBS.get();
        boolean campfire = !TechConfig.SERVER_SPEC.isLoaded() || TechConfig.REPLACE_VANILLA_CAMPFIRE.get();
        Predicate<String> modCheck = m -> ModList.get() != null && ModList.get().isLoaded(m);
        return isRecipeUnlocked(civ, recipeId, isTechUnlocked, modCheck, stubs, campfire);
    }

    public boolean isItemUnlocked(UUID civ, Identifier itemId, BiPredicate<UUID, Identifier> isTechUnlocked) {
        Identifier req = itemToTech.get(itemId);
        return req == null || isTechUnlocked.test(civ, req);
    }

    public boolean isBlockUnlocked(UUID civ, Identifier blockId, BiPredicate<UUID, Identifier> isTechUnlocked) {
        Identifier req = blockToTech.get(blockId);
        return req == null || isTechUnlocked.test(civ, req);
    }

    public Optional<Identifier> requiredTechForRecipe(Identifier recipeId) {
        Identifier direct = recipeToTech.get(recipeId);
        if (direct != null) {
            return Optional.of(direct);
        }
        GateRule fb = fallbackRecipeRules.get(recipeId);
        return fb != null ? Optional.of(fb.tech()) : Optional.empty();
    }

    public static Set<Identifier> defaultRemovedVanillaRecipes() {
        Set<Identifier> set = new HashSet<>();
        String[] mats = {"wooden", "stone", "copper", "iron", "golden", "diamond"};
        String[] kinds = {"pickaxe", "axe", "shovel", "hoe", "sword"};
        for (String m : mats) {
            for (String k : kinds) {
                set.add(Identifier.withDefaultNamespace(m + "_" + k));
            }
        }
        set.add(Identifier.withDefaultNamespace("bow"));
        set.add(Identifier.withDefaultNamespace("crossbow"));
        set.add(Identifier.withDefaultNamespace("campfire"));
        set.add(Identifier.withDefaultNamespace("soul_campfire"));
        return Collections.unmodifiableSet(set);
    }

    private static List<Identifier> readIdList(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return List.of();
        }
        List<Identifier> out = new java.util.ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray(key)) {
            out.add(Identifier.parse(el.getAsString()));
        }
        return List.copyOf(out);
    }
}
