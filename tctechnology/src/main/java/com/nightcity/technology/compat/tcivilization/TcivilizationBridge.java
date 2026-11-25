package com.nightcity.technology.compat.tcivilization;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class TcivilizationBridge {
    private static volatile boolean initialized;

    private TcivilizationBridge() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("tcivilization")) {
            return;
        }
        initialized = true;
    }

    public static boolean tryOpenTechTab() {
        if (!ModList.get().isLoaded("tcivilization")) {
            return false;
        }
        try {
            Class<?> panelCls = Class.forName("com.nightcity.tcivilization.client.api.CivPanel");
            Method openMethod = panelCls.getMethod("open", Identifier.class);
            openMethod.invoke(null, Identifier.fromNamespaceAndPath("tctechnology", "tech_tree"));
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
