package com.nightcity.settlements.compat;

import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SettlementsCompat {
    private static final Set<String> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private SettlementsCompat() {
    }

    public static void initAll() {
        if (ModList.get().isLoaded("tcatlas")) {
            com.nightcity.settlements.compat.tcatlas.TcatlasBridge.init();
            ACTIVE_BRIDGES.add("tcatlas");
        }
        if (ModList.get().isLoaded("tcbeyond")) {
            com.nightcity.settlements.compat.tcbeyond.TcbeyondBridge.init();
            ACTIVE_BRIDGES.add("tcbeyond");
        }
        if (ModList.get().isLoaded("tccommons")) {
            com.nightcity.settlements.compat.tccommons.TccommonsBridge.init();
            ACTIVE_BRIDGES.add("tccommons");
        }
        if (ModList.get().isLoaded("tccultivation")) {
            com.nightcity.settlements.compat.tccultivation.TccultivationBridge.init();
            ACTIVE_BRIDGES.add("tccultivation");
        }
        if (ModList.get().isLoaded("tcdiscovery")) {
            com.nightcity.settlements.compat.tcdiscovery.TcdiscoveryBridge.init();
            ACTIVE_BRIDGES.add("tcdiscovery");
        }
        if (ModList.get().isLoaded("tcechoes")) {
            com.nightcity.settlements.compat.tcechoes.TcechoesBridge.init();
            ACTIVE_BRIDGES.add("tcechoes");
        }
        if (ModList.get().isLoaded("tcecology")) {
            com.nightcity.settlements.compat.tcecology.TcecologyBridge.init();
            ACTIVE_BRIDGES.add("tcecology");
        }
        if (ModList.get().isLoaded("tcexchange")) {
            com.nightcity.settlements.compat.tcexchange.TcexchangeBridge.init();
            ACTIVE_BRIDGES.add("tcexchange");
        }
        if (ModList.get().isLoaded("tcfrontiers")) {
            com.nightcity.settlements.compat.tcfrontiers.TcfrontiersBridge.init();
            ACTIVE_BRIDGES.add("tcfrontiers");
        }
        if (ModList.get().isLoaded("tcindustry")) {
            com.nightcity.settlements.compat.tcindustry.TcindustryBridge.init();
            ACTIVE_BRIDGES.add("tcindustry");
        }
        if (ModList.get().isLoaded("tcivilization")) {
            com.nightcity.settlements.compat.tcivilization.TcivilizationBridge.init();
            ACTIVE_BRIDGES.add("tcivilization");
        }
        if (ModList.get().isLoaded("tcnations")) {
            com.nightcity.settlements.compat.tcnations.TcnationsBridge.init();
            ACTIVE_BRIDGES.add("tcnations");
        }
        if (ModList.get().isLoaded("tcnetworks")) {
            com.nightcity.settlements.compat.tcnetworks.TcnetworksBridge.init();
            ACTIVE_BRIDGES.add("tcnetworks");
        }
        if (ModList.get().isLoaded("tcorbit")) {
            com.nightcity.settlements.compat.tcorbit.TcorbitBridge.init();
            ACTIVE_BRIDGES.add("tcorbit");
        }
        if (ModList.get().isLoaded("tcskills")) {
            com.nightcity.settlements.compat.tcskills.TcskillsBridge.init();
            ACTIVE_BRIDGES.add("tcskills");
        }
        if (ModList.get().isLoaded("tctechnology")) {
            com.nightcity.settlements.compat.tctechnology.TctechnologyBridge.init();
            ACTIVE_BRIDGES.add("tctechnology");
        }
        if (ModList.get().isLoaded("tcwildlands")) {
            com.nightcity.settlements.compat.tcwildlands.TcwildlandsBridge.init();
            ACTIVE_BRIDGES.add("tcwildlands");
        }
    }

    public static Set<String> activeBridges() {
        return Collections.unmodifiableSet(ACTIVE_BRIDGES);
    }
}
