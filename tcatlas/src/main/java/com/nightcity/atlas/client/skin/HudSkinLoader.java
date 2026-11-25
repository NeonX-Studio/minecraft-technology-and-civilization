package com.nightcity.atlas.client.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.nightcity.atlas.TAtlas;
import com.nightcity.atlas.api.HudSkin;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HudSkinLoader extends SimpleJsonResourceReloadListener<JsonElement> {
    public static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            d -> d.convert(JsonOps.INSTANCE).getValue(),
            e -> new Dynamic<>(JsonOps.INSTANCE, e)
    );
    private static final FileToIdConverter LISTER = FileToIdConverter.json("tc_skins");
    public static final List<String> ERA_IDS = List.of(
            "stone", "neolithic", "bronze", "iron", "medieval", "renaissance",
            "industrial", "electric", "atomic", "information", "space"
    );

    private volatile Map<String, HudSkin> skins = loadFromClasspath();

    public HudSkinLoader() {
        super(JSON_CODEC, LISTER);
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> raw, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, HudSkin> parsed = compile(raw).skins();
        if (!parsed.isEmpty()) {
            this.skins = parsed;
        }
    }

    public HudSkin skinForEra(String eraId) {
        String normalized = eraId.contains(":") ? eraId.substring(eraId.indexOf(':') + 1) : eraId;
        return skins.getOrDefault(normalized, HudSkin.defaultStoneSkin());
    }

    public Map<String, HudSkin> allSkins() {
        return skins;
    }

    public record SkinCompileResult(Map<String, HudSkin> skins, List<String> errors) {
    }

    public static SkinCompileResult compile(Map<Identifier, JsonElement> raw) {
        Map<String, HudSkin> map = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<Identifier, JsonElement> entry : raw.entrySet()) {
            if (!TAtlas.MOD_ID.equals(entry.getKey().getNamespace())) {
                continue;
            }
            Optional<HudSkin> parsedOpt = parseFlexibleSkin(entry.getKey().getPath(), entry.getValue(), errors);
            if (parsedOpt.isEmpty()) {
                continue;
            }
            HudSkin skin = parsedOpt.get();
            Optional<String> wcagErr = validateAccessibility(skin);
            if (wcagErr.isPresent()) {
                errors.add(entry.getKey() + ": " + wcagErr.get());
                continue;
            }
            map.put(entry.getKey().getPath(), skin);
        }
        return new SkinCompileResult(Collections.unmodifiableMap(map), List.copyOf(errors));
    }

    private static Optional<HudSkin> parseFlexibleSkin(String eraName, JsonElement json, List<String> errors) {
        if (json.isJsonObject() && json.getAsJsonObject().has("palette")
                && json.getAsJsonObject().getAsJsonObject("palette").has("panel")) {
            try {
                var root = json.getAsJsonObject();
                var pal = root.getAsJsonObject("palette");
                int panel = parseHex(pal.get("panel").getAsString());
                int border = parseHex(pal.has("icon") ? pal.get("icon").getAsString() : pal.get("accent").getAsString());
                int text = parseHex(pal.get("text").getAsString());
                int muted = parseHex(pal.get("text_muted").getAsString());
                int accent = parseHex(pal.get("accent").getAsString());
                int pos = parseHex(pal.get("positive").getAsString());
                int warn = parseHex(pal.get("warning").getAsString());
                int crit = parseHex(pal.get("critical").getAsString());
                var frameObj = root.getAsJsonObject("frame");
                Identifier frameTex = Identifier.parse(frameObj.get("texture").getAsString());
                int slice = frameObj.has("nine_slice") ? frameObj.getAsJsonObject("nine_slice").get("left").getAsInt() : 6;
                var soundsObj = root.getAsJsonObject("sounds");
                Identifier sToast = Identifier.parse(soundsObj.get("open").getAsString());
                Identifier sAlert = Identifier.parse(soundsObj.get("click").getAsString());
                return Optional.of(new HudSkin(
                        Identifier.fromNamespaceAndPath("tctechnology", eraName),
                        new HudSkin.Palette(panel, border, text, muted, accent, pos, warn, crit),
                        frameTex, slice,
                        Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, "textures/gui/skin/" + eraName + "/mask.png"),
                        eraName, true, sToast, sAlert
                ));
            } catch (Exception e) {
                errors.add(eraName + ": " + e.getMessage());
                return Optional.empty();
            }
        }
        var res = HudSkin.CODEC.parse(JsonOps.INSTANCE, json);
        if (res.error().isPresent() || res.result().isEmpty()) {
            errors.add(eraName + ": " + res.error().map(e -> e.message()).orElse("decode error"));
            return Optional.empty();
        }
        return res.result();
    }

    private static int parseHex(String raw) {
        String s = raw.startsWith("#") ? raw.substring(1) : raw;
        long parsed = Long.parseUnsignedLong(s, 16);
        if (s.length() == 6) {
            parsed |= 0xFF000000L;
        }
        return (int) parsed;
    }

    public static Optional<String> validateAccessibility(HudSkin skin) {
        HudSkin.Palette p = skin.palette();
        double primaryContrast = HudSkin.contrastRatio(p.textPrimary(), p.bgFill());
        if (primaryContrast < 4.5) {
            return Optional.of("text_primary contrast " + primaryContrast + " < 4.5");
        }
        double secondaryContrast = HudSkin.contrastRatio(p.textSecondary(), p.bgFill());
        if (secondaryContrast < 3.0) {
            return Optional.of("text_secondary contrast " + secondaryContrast + " < 3.0");
        }
        if ((p.ok() & 0xFFFFFF) == (p.warn() & 0xFFFFFF) || (p.warn() & 0xFFFFFF) == (p.danger() & 0xFFFFFF)) {
            return Optional.of("ok/warn/danger status colors must be distinct");
        }
        return Optional.empty();
    }

    public static Map<String, HudSkin> loadFromClasspath() {
        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        ClassLoader cl = HudSkinLoader.class.getClassLoader();
        for (String era : ERA_IDS) {
            String resPath = "assets/tcatlas/tc_skins/" + era + ".json";
            try (InputStream in = cl.getResourceAsStream(resPath)) {
                if (in != null) {
                    JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    raw.put(Identifier.fromNamespaceAndPath(TAtlas.MOD_ID, era), el);
                }
            } catch (Exception ignored) {
            }
        }
        return compile(raw).skins();
    }
}
