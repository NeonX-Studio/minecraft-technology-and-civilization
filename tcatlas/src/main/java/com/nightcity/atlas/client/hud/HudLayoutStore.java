package com.nightcity.atlas.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightcity.atlas.api.HudAnchor;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudWidget;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudLayoutStore {
    public static final int FORMAT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record WidgetPlacement(
            HudAnchor anchor,
            int offsetX,
            int offsetY,
            float scale,
            boolean enabled
    ) {
        public static final Codec<WidgetPlacement> CODEC = RecordCodecBuilder.create(i -> i.group(
                HudAnchor.CODEC.fieldOf("anchor").forGetter(WidgetPlacement::anchor),
                Codec.INT.optionalFieldOf("offset_x", 0).forGetter(WidgetPlacement::offsetX),
                Codec.INT.optionalFieldOf("offset_y", 0).forGetter(WidgetPlacement::offsetY),
                Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(WidgetPlacement::scale),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(WidgetPlacement::enabled)
        ).apply(i, (anchor, ox, oy, sc, en) -> new WidgetPlacement(anchor, ox, oy, Math.clamp(sc, 0.5f, 2.0f), en)));

        public static WidgetPlacement defaultFor(HudWidget widget) {
            return new WidgetPlacement(widget.defaultAnchor(), widget.defaultOffsetX(), widget.defaultOffsetY(), 1.0f, true);
        }

        public WidgetPlacement movedTo(int screenX, int screenY, int widgetWidth, int widgetHeight, int screenWidth, int screenHeight, int snapGrid) {
            int snappedX = snapGrid > 1 ? Math.round((float) screenX / snapGrid) * snapGrid : screenX;
            int snappedY = snapGrid > 1 ? Math.round((float) screenY / snapGrid) * snapGrid : screenY;
            int scaledW = Math.round(widgetWidth * scale);
            int scaledH = Math.round(widgetHeight * scale);
            HudAnchor nearest = HudAnchor.nearest(snappedX, snappedY, scaledW, scaledH, screenWidth, screenHeight);
            int anchorBaseX = Math.round((screenWidth - scaledW) * nearest.normX());
            int anchorBaseY = Math.round((screenHeight - scaledH) * nearest.normY());
            return new WidgetPlacement(nearest, snappedX - anchorBaseX, snappedY - anchorBaseY, scale, enabled);
        }
    }

    public record SavedLayoutFile(
            int formatVersion,
            HudLevel hudLevel,
            Map<Identifier, WidgetPlacement> placements
    ) {
        public static final Codec<SavedLayoutFile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("formatVersion", FORMAT_VERSION).forGetter(SavedLayoutFile::formatVersion),
                HudLevel.CODEC.optionalFieldOf("hudLevel", HudLevel.STANDARD).forGetter(SavedLayoutFile::hudLevel),
                Codec.unboundedMap(Identifier.CODEC, WidgetPlacement.CODEC).optionalFieldOf("placements", Map.of()).forGetter(SavedLayoutFile::placements)
        ).apply(i, SavedLayoutFile::new));
    }

    private HudLevel hudLevel = HudLevel.STANDARD;
    private final Map<Identifier, WidgetPlacement> placements = new LinkedHashMap<>();

    public HudLevel hudLevel() {
        return hudLevel;
    }

    public void setHudLevel(HudLevel hudLevel) {
        this.hudLevel = hudLevel;
    }

    public WidgetPlacement placementFor(HudWidget widget) {
        return placements.computeIfAbsent(widget.id(), id -> WidgetPlacement.defaultFor(widget));
    }

    public void setPlacement(Identifier widgetId, WidgetPlacement placement) {
        placements.put(widgetId, placement);
    }

    public void resetToDefaults(Iterable<HudWidget> widgets) {
        placements.clear();
        for (HudWidget w : widgets) {
            placements.put(w.id(), WidgetPlacement.defaultFor(w));
        }
    }

    public Map<Identifier, WidgetPlacement> placements() {
        return Collections.unmodifiableMap(placements);
    }

    public void save(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        SavedLayoutFile snapshot = new SavedLayoutFile(FORMAT_VERSION, hudLevel, Map.copyOf(placements));
        JsonElement encoded = SavedLayoutFile.CODEC.encodeStart(JsonOps.INSTANCE, snapshot).getOrThrow();
        Files.writeString(file, GSON.toJson(encoded), StandardCharsets.UTF_8);
    }

    public void load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement json = JsonParser.parseString(raw);
        SavedLayoutFile decoded = SavedLayoutFile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        this.hudLevel = decoded.hudLevel();
        this.placements.clear();
        this.placements.putAll(decoded.placements());
    }
}
