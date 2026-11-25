package com.nightcity.echoes.compat.tcnations;

import com.nightcity.echoes.EchoesConfig;
import com.nightcity.echoes.api.EchoesApi;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.Relation;
import com.nightcity.echoes.api.SurvivorPolitySnapshot;
import java.lang.reflect.Field;
import java.util.List;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Soft-dependency bridge exposing end-state surviving polities, relations, and holy sites for tcnations (18 §3.6, 23 §5.1.1).
 */
public final class TcnationsBridge {
    private static volatile boolean initialized;

    private TcnationsBridge() {
    }

    public static void init() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isNationsLoaded() {
        ModList list = ModList.get();
        return list != null && list.isLoaded("tcnations");
    }

    public static int resolveSurvivorCount() {
        if (isNationsLoaded()) {
            try {
                Class<?> cfgCls = Class.forName("com.nightcity.nations.NationsConfig");
                for (String fieldName : new String[]{"AI_CIV_COUNT", "aiCivCount"}) {
                    try {
                        Field f = cfgCls.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        Object val = f.get(null);
                        if (val instanceof ModConfigSpec.IntValue iv) {
                            return Math.clamp(iv.get(), 1, 32);
                        }
                        if (val instanceof Number num) {
                            return Math.clamp(num.intValue(), 1, 32);
                        }
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return EchoesConfig.defaultSurvivorCount();
    }

    public static List<Polity> survivors() {
        return EchoesApi.get().view().survivors();
    }

    public static List<SurvivorPolitySnapshot> survivorSnapshots() {
        return EchoesApi.get().view().survivorSnapshots();
    }

    public static Relation relationBetween(int polityA, int polityB) {
        return EchoesApi.get().view().relation(polityA, polityB);
    }

    public static List<Long> holySitesOf(int polityId) {
        return EchoesApi.get().view().holySites(polityId);
    }
}
