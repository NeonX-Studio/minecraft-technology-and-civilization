package com.nightcity.discovery.compat;

import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscoveryCompat {
    private static final Set<String> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private DiscoveryCompat() {
    }

    public static void initAll() {
        if (ModList.get().isLoaded("tcatlas")) {
            com.nightcity.discovery.compat.tcatlas.TcatlasBridge.init();
            ACTIVE_BRIDGES.add("tcatlas");
        }
        if (ModList.get().isLoaded("tcbeyond")) {
            com.nightcity.discovery.compat.tcbeyond.TcbeyondBridge.init();
            ACTIVE_BRIDGES.add("tcbeyond");
        }
        if (ModList.get().isLoaded("tccommons")) {
            com.nightcity.discovery.compat.tccommons.TccommonsBridge.init();
            ACTIVE_BRIDGES.add("tccommons");
        }
        if (ModList.get().isLoaded("tccultivation")) {
            com.nightcity.discovery.compat.tccultivation.TccultivationBridge.init();
            ACTIVE_BRIDGES.add("tccultivation");
        }
        if (ModList.get().isLoaded("tcechoes")) {
            com.nightcity.discovery.compat.tcechoes.TcechoesBridge.init();
            ACTIVE_BRIDGES.add("tcechoes");
        }
        if (ModList.get().isLoaded("tcecology")) {
            com.nightcity.discovery.compat.tcecology.TcecologyBridge.init();
            ACTIVE_BRIDGES.add("tcecology");
        }
        if (ModList.get().isLoaded("tcexchange")) {
            com.nightcity.discovery.compat.tcexchange.TcexchangeBridge.init();
            ACTIVE_BRIDGES.add("tcexchange");
        }
        if (ModList.get().isLoaded("tcfrontiers")) {
            com.nightcity.discovery.compat.tcfrontiers.TcfrontiersBridge.init();
            ACTIVE_BRIDGES.add("tcfrontiers");
        }
        if (ModList.get().isLoaded("tcindustry")) {
            com.nightcity.discovery.compat.tcindustry.TcindustryBridge.init();
            ACTIVE_BRIDGES.add("tcindustry");
        }
        if (ModList.get().isLoaded("tcivilization")) {
            com.nightcity.discovery.compat.tcivilization.TcivilizationBridge.init();
            ACTIVE_BRIDGES.add("tcivilization");
        }
        if (ModList.get().isLoaded("tcnations")) {
            com.nightcity.discovery.compat.tcnations.TcnationsBridge.init();
            ACTIVE_BRIDGES.add("tcnations");
        }
        if (ModList.get().isLoaded("tcnetworks")) {
            com.nightcity.discovery.compat.tcnetworks.TcnetworksBridge.init();
            ACTIVE_BRIDGES.add("tcnetworks");
        }
        if (ModList.get().isLoaded("tcorbit")) {
            com.nightcity.discovery.compat.tcorbit.TcorbitBridge.init();
            ACTIVE_BRIDGES.add("tcorbit");
        }
        if (ModList.get().isLoaded("tcsettlements")) {
            com.nightcity.discovery.compat.tcsettlements.TcsettlementsBridge.init();
            ACTIVE_BRIDGES.add("tcsettlements");
        }
        if (ModList.get().isLoaded("tcskills")) {
            com.nightcity.discovery.compat.tcskills.TcskillsBridge.init();
            ACTIVE_BRIDGES.add("tcskills");
        }
        if (ModList.get().isLoaded("tctechnology")) {
            com.nightcity.discovery.compat.tctechnology.TctechnologyBridge.init();
            ACTIVE_BRIDGES.add("tctechnology");
        }
        if (ModList.get().isLoaded("tcwildlands")) {
            com.nightcity.discovery.compat.tcwildlands.TcwildlandsBridge.init();
            ACTIVE_BRIDGES.add("tcwildlands");
        }
    }

    public static Set<String> activeBridges() {
        return Collections.unmodifiableSet(ACTIVE_BRIDGES);
    }
}
