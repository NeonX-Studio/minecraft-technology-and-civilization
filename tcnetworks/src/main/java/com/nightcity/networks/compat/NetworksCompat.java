package com.nightcity.networks.compat;

import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworksCompat {
    private static final Set<String> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private NetworksCompat() {
    }

    public static void initAll() {
        if (ModList.get().isLoaded("tcatlas")) {
            com.nightcity.networks.compat.tcatlas.TcatlasBridge.init();
            ACTIVE_BRIDGES.add("tcatlas");
        }
        if (ModList.get().isLoaded("tcbeyond")) {
            com.nightcity.networks.compat.tcbeyond.TcbeyondBridge.init();
            ACTIVE_BRIDGES.add("tcbeyond");
        }
        if (ModList.get().isLoaded("tccommons")) {
            com.nightcity.networks.compat.tccommons.TccommonsBridge.init();
            ACTIVE_BRIDGES.add("tccommons");
        }
        if (ModList.get().isLoaded("tccultivation")) {
            com.nightcity.networks.compat.tccultivation.TccultivationBridge.init();
            ACTIVE_BRIDGES.add("tccultivation");
        }
        if (ModList.get().isLoaded("tcdiscovery")) {
            com.nightcity.networks.compat.tcdiscovery.TcdiscoveryBridge.init();
            ACTIVE_BRIDGES.add("tcdiscovery");
        }
        if (ModList.get().isLoaded("tcechoes")) {
            com.nightcity.networks.compat.tcechoes.TcechoesBridge.init();
            ACTIVE_BRIDGES.add("tcechoes");
        }
        if (ModList.get().isLoaded("tcecology")) {
            com.nightcity.networks.compat.tcecology.TcecologyBridge.init();
            ACTIVE_BRIDGES.add("tcecology");
        }
        if (ModList.get().isLoaded("tcexchange")) {
            com.nightcity.networks.compat.tcexchange.TcexchangeBridge.init();
            ACTIVE_BRIDGES.add("tcexchange");
        }
        if (ModList.get().isLoaded("tcfrontiers")) {
            com.nightcity.networks.compat.tcfrontiers.TcfrontiersBridge.init();
            ACTIVE_BRIDGES.add("tcfrontiers");
        }
        if (ModList.get().isLoaded("tcindustry")) {
            com.nightcity.networks.compat.tcindustry.TcindustryBridge.init();
            ACTIVE_BRIDGES.add("tcindustry");
        }
        if (ModList.get().isLoaded("tcivilization")) {
            com.nightcity.networks.compat.tcivilization.TcivilizationBridge.init();
            ACTIVE_BRIDGES.add("tcivilization");
        }
        if (ModList.get().isLoaded("tcnations")) {
            com.nightcity.networks.compat.tcnations.TcnationsBridge.init();
            ACTIVE_BRIDGES.add("tcnations");
        }
        if (ModList.get().isLoaded("tcorbit")) {
            com.nightcity.networks.compat.tcorbit.TcorbitBridge.init();
            ACTIVE_BRIDGES.add("tcorbit");
        }
        if (ModList.get().isLoaded("tcsettlements")) {
            com.nightcity.networks.compat.tcsettlements.TcsettlementsBridge.init();
            ACTIVE_BRIDGES.add("tcsettlements");
        }
        if (ModList.get().isLoaded("tcskills")) {
            com.nightcity.networks.compat.tcskills.TcskillsBridge.init();
            ACTIVE_BRIDGES.add("tcskills");
        }
        if (ModList.get().isLoaded("tctechnology")) {
            com.nightcity.networks.compat.tctechnology.TctechnologyBridge.init();
            ACTIVE_BRIDGES.add("tctechnology");
        }
        if (ModList.get().isLoaded("tcwildlands")) {
            com.nightcity.networks.compat.tcwildlands.TcwildlandsBridge.init();
            ACTIVE_BRIDGES.add("tcwildlands");
        }
    }

    public static Set<String> activeBridges() {
        return Collections.unmodifiableSet(ACTIVE_BRIDGES);
    }
}
