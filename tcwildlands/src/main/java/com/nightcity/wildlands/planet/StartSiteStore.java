package com.nightcity.wildlands.planet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Persists and loads <world>/tc/wildlands/start_site.json (10 §2.1, §10.5, §11.6).
 */
public final class StartSiteStore {
    public static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Selection(
            int formatVersion,
            int chosenX,
            int chosenY,
            int chosenZ,
            int score,
            int candidateIndex,
            @Nullable UUID chosenBy,
            String mode,
            long chosenAtGameTime,
            List<PlanetModel.StartSiteCandidate> candidates
    ) {
        public BlockPos spawnPos() {
            return new BlockPos(chosenX, chosenY, chosenZ);
        }
    }

    private StartSiteStore() {
    }

    public static Path resolvePath(Path worldRoot) {
        return worldRoot.resolve("tc").resolve("wildlands").resolve("start_site.json");
    }

    public static List<PlanetModel.StartSiteCandidate> topFiveCandidates(PlanetModel model) {
        List<PlanetModel.StartSiteCandidate> all = model.startSites();
        return all.subList(0, Math.min(5, all.size()));
    }

    public static void write(Path file, Selection selection) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", FORMAT_VERSION);

        JsonObject chosen = new JsonObject();
        chosen.addProperty("x", selection.chosenX());
        chosen.addProperty("y", selection.chosenY());
        chosen.addProperty("z", selection.chosenZ());
        chosen.addProperty("score", selection.score());
        chosen.addProperty("candidateIndex", selection.candidateIndex());
        root.add("chosen", chosen);

        if (selection.chosenBy() != null) {
            root.addProperty("chosenBy", selection.chosenBy().toString());
        }
        root.addProperty("mode", selection.mode());
        root.addProperty("chosenAtGameTime", selection.chosenAtGameTime());

        JsonArray arr = new JsonArray();
        for (PlanetModel.StartSiteCandidate c : selection.candidates()) {
            JsonObject item = new JsonObject();
            item.addProperty("x", c.blockX());
            item.addProperty("y", c.surfaceY());
            item.addProperty("z", c.blockZ());
            item.addProperty("score", c.score());
            item.addProperty("koppen", c.koppen());
            item.addProperty("biome", c.biome().toString());
            item.addProperty("rock", c.primaryRock().toString());
            item.addProperty("waterDistance", c.waterDistanceBlocks());
            arr.add(item);
        }
        root.add("candidates", arr);

        Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    public static Optional<Selection> readIfExists(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        int ver = root.has("formatVersion") ? root.get("formatVersion").getAsInt() : 1;
        JsonObject chosen = root.getAsJsonObject("chosen");
        int cx = chosen.get("x").getAsInt();
        int cy = chosen.has("y") ? chosen.get("y").getAsInt() : 66;
        int cz = chosen.get("z").getAsInt();
        int score = chosen.get("score").getAsInt();
        int idx = chosen.get("candidateIndex").getAsInt();
        UUID by = root.has("chosenBy") && !root.get("chosenBy").isJsonNull()
                ? UUID.fromString(root.get("chosenBy").getAsString())
                : null;
        String mode = root.has("mode") ? root.get("mode").getAsString() : "auto";
        long gameTime = root.has("chosenAtGameTime") ? root.get("chosenAtGameTime").getAsLong() : 0L;

        List<PlanetModel.StartSiteCandidate> candidates = new ArrayList<>();
        if (root.has("candidates") && root.get("candidates").isJsonArray()) {
            int cIdx = 0;
            for (JsonElement el : root.getAsJsonArray("candidates")) {
                JsonObject o = el.getAsJsonObject();
                candidates.add(new PlanetModel.StartSiteCandidate(
                        cIdx++,
                        o.get("x").getAsInt(),
                        o.get("z").getAsInt(),
                        o.has("y") ? o.get("y").getAsInt() : 66,
                        o.get("score").getAsInt(),
                        o.get("koppen").getAsString(),
                        Identifier.parse(o.get("biome").getAsString()),
                        o.has("rock") ? Identifier.parse(o.get("rock").getAsString()) : Identifier.withDefaultNamespace("granite"),
                        o.has("waterDistance") ? o.get("waterDistance").getAsInt() : 48
                ));
            }
        }
        return Optional.of(new Selection(ver, cx, cy, cz, score, idx, by, mode, gameTime, candidates));
    }
}
