package com.nightcity.tcivilization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightcity.tcivilization.api.ActionCheck;
import com.nightcity.tcivilization.api.ActionScope;
import com.nightcity.tcivilization.api.CivPermissions;
import com.nightcity.tcivilization.api.MonsterPolicy;
import com.nightcity.tcivilization.api.Notification;
import com.nightcity.tcivilization.api.Severity;
import com.nightcity.tcivilization.api.TcNotifications;
import com.nightcity.tcivilization.api.TcThreads;
import com.nightcity.tcivilization.api.TrackerApi;
import com.nightcity.tcivilization.api.TrackerPin;
import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import com.nightcity.tcivilization.client.CommandWheelScreen;
import com.nightcity.tcivilization.client.api.CivPanelTabRegistry;
import com.nightcity.tcivilization.client.api.CivPanelTabSpec;
import com.nightcity.tcivilization.client.api.CivTabGroup;
import com.nightcity.tcivilization.client.api.CivTabPage;
import com.nightcity.tcivilization.client.api.DeepLink;
import com.nightcity.tcivilization.client.api.DeepLinkRegistry;
import com.nightcity.tcivilization.client.api.HudLevel;
import com.nightcity.tcivilization.client.api.HudRect;
import com.nightcity.tcivilization.client.api.TcSkin;
import com.nightcity.tcivilization.command.TcCommands;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiRegistriesAndCommandsTest {
    private static CivTabPage dummyPage() {
        return new CivTabPage() {
            @Override
            public void init(com.nightcity.tcivilization.client.api.CivTabContext ctx, HudRect area) {
            }

            @Override
            public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            }
        };
    }

    @Test
    void neutralSkinMeetsAccessibilityContrastAndColorblindModes() {
        assertTrue(TcSkin.NEUTRAL_PALETTE.meetsContrastRequirements(),
                "tcivilization:skin/neutral must satisfy >=4.5:1 text and >=3.0:1 warning/critical contrast");
        for (TcSkin.ColorblindMode mode : TcSkin.ColorblindMode.values()) {
            assertNotNull(TcSkin.NEUTRAL_PALETTE.withColorblindMode(mode));
        }
    }

    @Test
    void civPanelTabRegistryEnforcesTwoTabsPerModLimitAndDeterministicOrder() {
        CivPanelTabRegistry.Impl reg = new CivPanelTabRegistry.Impl();
        Identifier icon = Identifier.fromNamespaceAndPath("tcivilization", "icon");

        reg.register(new CivPanelTabSpec(Identifier.fromNamespaceAndPath("mod_b", "world_tab"), CivTabGroup.WORLD, 10, icon, Optional.empty(), c -> true, Optional.empty(), c -> dummyPage()));
        reg.register(new CivPanelTabSpec(Identifier.fromNamespaceAndPath("mod_a", "first"), CivTabGroup.PERSONAL, 20, icon, Optional.empty(), c -> true, Optional.empty(), c -> dummyPage()));
        reg.register(new CivPanelTabSpec(Identifier.fromNamespaceAndPath("mod_a", "second"), CivTabGroup.ECONOMY, 5, icon, Optional.empty(), c -> true, Optional.empty(), c -> dummyPage()));

        boolean prevStrict = TcThreads.isStrictMode();
        try {
            TcThreads.setStrictMode(true);
            assertThrows(IllegalStateException.class, () -> reg.register(new CivPanelTabSpec(
                    Identifier.fromNamespaceAndPath("mod_a", "third_illegal"), CivTabGroup.WORLD, 1, icon, Optional.empty(), c -> true, Optional.empty(), c -> dummyPage()
            )));

            TcThreads.setStrictMode(false);
            reg.register(new CivPanelTabSpec(
                    Identifier.fromNamespaceAndPath("mod_a", "third_ignored"), CivTabGroup.WORLD, 1, icon, Optional.empty(), c -> true, Optional.empty(), c -> dummyPage()
            ));
            List<CivPanelTabSpec> ordered = reg.orderedTabs();
            assertEquals(3, ordered.size());
            assertEquals(CivTabGroup.PERSONAL, ordered.get(0).group());
            assertEquals(CivTabGroup.ECONOMY, ordered.get(1).group());
            assertEquals(CivTabGroup.WORLD, ordered.get(2).group());
        } finally {
            TcThreads.setStrictMode(prevStrict);
        }
    }

    @Test
    void commandWheelDeadzoneSectorSelectionHudLevelsAndDeepLinks() {
        assertEquals(-1, CommandWheelScreen.selectSector(5.0D, -5.0D, CommandWheelScreen.DEFAULT_DEADZONE));
        assertEquals(0, CommandWheelScreen.selectSector(0.0D, -50.0D, CommandWheelScreen.DEFAULT_DEADZONE)); // top
        assertEquals(2, CommandWheelScreen.selectSector(50.0D, 0.0D, CommandWheelScreen.DEFAULT_DEADZONE));  // right
        assertEquals(4, CommandWheelScreen.selectSector(0.0D, 50.0D, CommandWheelScreen.DEFAULT_DEADZONE));  // bottom
        assertEquals(6, CommandWheelScreen.selectSector(-50.0D, 0.0D, CommandWheelScreen.DEFAULT_DEADZONE)); // left

        assertEquals(HudLevel.STANDARD, HudLevel.MINIMAL.next());
        assertEquals(HudLevel.EXPANDED, HudLevel.STANDARD.next());
        assertEquals(HudLevel.MINIMAL, HudLevel.EXPANDED.next());

        DeepLinkRegistry.Impl dlReg = DeepLinkRegistry.global();
        dlReg.clearForTests();
        dlReg.register("tctechnology", link -> "tech/kiln".equals(link.path()) && "1".equals(link.query().get("era")));

        DeepLink parsed = DeepLink.parse("tc:tctechnology/tech/kiln?era=1").orElseThrow();
        assertEquals("tctechnology", parsed.modId());
        assertEquals("tech/kiln", parsed.path());
        assertTrue(dlReg.isResolvable("tc:tctechnology/tech/kiln?era=1"));
        assertTrue(dlReg.open("tc:tctechnology/tech/kiln?era=1"));
        assertFalse(dlReg.isResolvable("tc:uninstalled/foo"));
    }

    @Test
    void notificationsMergeWithinTenSecondsAndRateLimitInfoToTwelvePerMinute() {
        TcNotifications.clearForTests();
        UUID player = UUID.randomUUID();
        Notification merged = new Notification(
                TcNotifications.CATEGORY_CIVILIZATION, Severity.INFO, Component.literal("Test"),
                null, null, null, "same_key", 0);

        assertTrue(TcNotifications.recordForPlayer(player, merged, 1_000L));
        assertTrue(TcNotifications.recordForPlayer(player, merged, 5_000L));
        assertEquals(1, TcNotifications.history(player).size());
        assertEquals(2, TcNotifications.history(player).getFirst().count());

        // After >10,000 ms, same mergeKey creates a second entry
        assertTrue(TcNotifications.recordForPlayer(player, merged, 20_000L));
        assertEquals(2, TcNotifications.history(player).size());

        // Exhaust remaining 9 of 12/min INFO slots
        for (int i = 0; i < 9; i++) {
            Notification unique = new Notification(
                    TcNotifications.CATEGORY_CIVILIZATION, Severity.INFO, Component.literal("Info"),
                    null, null, null, "unique_" + i, 0);
            assertTrue(TcNotifications.recordForPlayer(player, unique, 21_000L + i));
        }
        Notification thirteenth = new Notification(
                TcNotifications.CATEGORY_CIVILIZATION, Severity.INFO, Component.literal("Overflow"),
                null, null, null, "overflow", 0);
        assertFalse(TcNotifications.recordForPlayer(player, thirteenth, 25_000L));

        // CRITICAL bypasses INFO rate limit
        Notification critical = new Notification(
                TcNotifications.CATEGORY_SYSTEM, Severity.CRITICAL, Component.literal("Critical"),
                null, null, null, "crit", 0);
        assertTrue(TcNotifications.recordForPlayer(player, critical, 25_001L));
    }

    @Test
    void trackerPinsPermissionsVanillaPolicyAndCommandDurationsWork() {
        TrackerApi.Impl tracker = new TrackerApi.Impl();
        UUID player = UUID.randomUUID();
        Identifier pinType = Identifier.fromNamespaceAndPath("test", "tech");
        for (int i = 0; i < 3; i++) {
            assertTrue(tracker.pin(player, new TrackerPin(pinType, "goal_" + i, Component.literal("G" + i))));
        }
        assertFalse(tracker.pin(player, new TrackerPin(pinType, "goal_overflow", Component.literal("Overflow"))));
        assertEquals(TrackerApi.MAX_PINS_PER_PLAYER, tracker.pins(player).size());

        CivPermissions.clearForTests();
        CivPermissions.setFeatureDisabled(Identifier.fromNamespaceAndPath("test", "disabled_op"), true);

        VanillaPolicyService.Impl policy = new VanillaPolicyService.Impl();
        assertTrue(policy.owner(VanillaSystem.VILLAGERS).isEmpty());
        policy.registerOwner(VanillaSystem.VILLAGERS, "tcsettlement", 100);
        assertEquals(Optional.of("tcsettlement"), policy.owner(VanillaSystem.VILLAGERS));
        policy.registerOwner(VanillaSystem.WORLDGEN, "tcwildlands", 100);
        assertTrue(policy.isPlanetMode(null));

        assertEquals(4_800_000L, TcCommands.parseSimulateDurationTicks("200d"));
        assertEquals(72_000L, TcCommands.parseSimulateDurationTicks("72000t"));
        assertEquals(72_000L, TcCommands.parseSimulateDurationTicks("3"));
        assertThrows(IllegalArgumentException.class, () -> TcCommands.parseSimulateDurationTicks("0d"));
        assertThrows(IllegalArgumentException.class, () -> TcCommands.parseSimulateDurationTicks("-10t"));
    }

    @Test
    void languageFilesAreValidJsonWithMatchingKeySets() throws Exception {
        JsonObject en;
        JsonObject zh;
        try (InputStream enIn = getClass().getResourceAsStream("/assets/tcivilization/lang/en_us.json");
             InputStream zhIn = getClass().getResourceAsStream("/assets/tcivilization/lang/zh_cn.json")) {
            assertNotNull(enIn, "en_us.json must be on classpath");
            assertNotNull(zhIn, "zh_cn.json must be on classpath");
            en = JsonParser.parseReader(new InputStreamReader(enIn, StandardCharsets.UTF_8)).getAsJsonObject();
            zh = JsonParser.parseReader(new InputStreamReader(zhIn, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        assertEquals(en.keySet(), zh.keySet(), "en_us.json and zh_cn.json must have symmetric translation keys");
    }
}
