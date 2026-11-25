package com.nightcity.tcivilization.client.api;

import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.client.CivPanelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Public client entry point to open the J Civilization Panel on a target tab (03 §5.10.1, §12.2).
 */
public final class CivPanel {
    public static final Identifier TAB_OVERVIEW = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "overview");
    public static final Identifier TAB_CHRONICLE = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "chronicle");

    private static volatile Identifier lastOpenedTab = TAB_OVERVIEW;

    private CivPanel() {
    }

    public static Identifier lastOpenedTab() {
        return lastOpenedTab;
    }

    public static void open(Identifier tabId) {
        Identifier target = tabId == null ? lastOpenedTab : tabId;
        lastOpenedTab = target;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreenAndShow(new CivPanelScreen(target));
            NeoForge.EVENT_BUS.post(new CivPanelOpenedEvent(target));
        }
    }

    public static void openDefault() {
        open(lastOpenedTab);
    }
}
