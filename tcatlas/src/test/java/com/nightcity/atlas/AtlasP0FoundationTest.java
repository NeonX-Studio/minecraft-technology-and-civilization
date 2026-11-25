package com.nightcity.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nightcity.atlas.api.HudAnchor;
import com.nightcity.atlas.api.HudLevel;
import com.nightcity.atlas.api.HudSkin;
import com.nightcity.atlas.client.hud.BuiltinHudWidgets;
import com.nightcity.atlas.client.hud.HudLayoutStore;
import com.nightcity.atlas.client.skin.HudSkinLoader;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AtlasP0FoundationTest {
    @Test
    void all11EraSkinsParseAndPassWcagContrast() throws IOException {
        Path skinDir = Path.of("src", "main", "resources", "assets", "tcatlas", "tc_skins");
        assertTrue(Files.isDirectory(skinDir));

        Map<Identifier, JsonElement> raw = new LinkedHashMap<>();
        for (String era : HudSkinLoader.ERA_IDS) {
            Path p = skinDir.resolve(era + ".json");
            assertTrue(Files.isRegularFile(p), "Missing skin file: " + p);
            raw.put(Identifier.fromNamespaceAndPath("tcatlas", era),
                    JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)));
        }
        HudSkinLoader.SkinCompileResult result = HudSkinLoader.compile(raw);
        assertTrue(result.errors().isEmpty(), () -> "Skin WCAG/Codec errors: " + result.errors());
        assertEquals(11, result.skins().size());

        for (HudSkin skin : result.skins().values()) {
            assertTrue(HudSkin.contrastRatio(skin.palette().textPrimary(), skin.palette().bgFill()) >= 4.5);
            assertTrue(HudSkin.contrastRatio(skin.palette().textSecondary(), skin.palette().bgFill()) >= 3.0);
        }
    }

    @Test
    void hudLevelCycleWidgetDragAndLayoutPersistenceAcrossRestarts(@TempDir Path tempDir) throws IOException {
        assertTrue(TAtlas.hud().widgets().size() >= 5, "At least 5 built-in widgets should be registered");

        HudLevel start = HudLevel.MINIMAL;
        assertEquals(HudLevel.STANDARD, start.next());
        assertEquals(HudLevel.TACTICAL, start.next().next());
        assertEquals(HudLevel.HIDDEN, start.next().next().next());
        assertEquals(HudLevel.MINIMAL, start.next().next().next().next());

        HudLayoutStore storeBeforeRestart = new HudLayoutStore();
        storeBeforeRestart.setHudLevel(HudLevel.TACTICAL);

        // Simulate dragging 2 widgets in Ctrl+H layout editor
        var w1Initial = storeBeforeRestart.placementFor(BuiltinHudWidgets.MINIMAP);
        var w1Dragged = w1Initial.movedTo(24, 36, 112, 112, 640, 360, 4);
        storeBeforeRestart.setPlacement(BuiltinHudWidgets.MINIMAP.id(), w1Dragged);

        var w2Initial = storeBeforeRestart.placementFor(BuiltinHudWidgets.STATUS_STRIP);
        var w2Dragged = w2Initial.movedTo(480, 292, 140, 20, 640, 360, 4);
        storeBeforeRestart.setPlacement(BuiltinHudWidgets.STATUS_STRIP.id(), w2Dragged);

        assertEquals(HudAnchor.TOP_LEFT, w1Dragged.anchor());
        assertEquals(HudAnchor.BOTTOM_RIGHT, w2Dragged.anchor());

        Path layoutFile = tempDir.resolve("tcatlas-hud-layout.json");
        storeBeforeRestart.save(layoutFile);
        assertTrue(Files.isRegularFile(layoutFile));

        // Simulate restart by loading into a fresh HudLayoutStore
        HudLayoutStore storeAfterRestart = new HudLayoutStore();
        storeAfterRestart.load(layoutFile);
        assertEquals(HudLevel.TACTICAL, storeAfterRestart.hudLevel());
        assertEquals(w1Dragged, storeAfterRestart.placementFor(BuiltinHudWidgets.MINIMAP));
        assertEquals(w2Dragged, storeAfterRestart.placementFor(BuiltinHudWidgets.STATUS_STRIP));
    }
}
