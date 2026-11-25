package com.nightcity.exchange.compat;

import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExchangeCompat {
    private static final Set<String> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private ExchangeCompat() {
    }

    public static void initAll() {
        if (ModList.get().isLoaded("tcatlas")) {
            com.nightcity.exchange.compat.tcatlas.TcatlasBridge.init();
            ACTIVE_BRIDGES.add("tcatlas");
        }
        if (ModList.get().isLoaded("tcbeyond")) {
            com.nightcity.exchange.compat.tcbeyond.TcbeyondBridge.init();
            ACTIVE_BRIDGES.add("tcbeyond");
        }
        if (ModList.get().isLoaded("tccommons")) {
            com.nightcity.exchange.compat.tccommons.TccommonsBridge.init();
            ACTIVE_BRIDGES.add("tccommons");
        }
        if (ModList.get().isLoaded("tccultivation")) {
            com.nightcity.exchange.compat.tccultivation.TccultivationBridge.init();
            ACTIVE_BRIDGES.add("tccultivation");
        }
        if (ModList.get().isLoaded("tcdiscovery")) {
            com.nightcity.exchange.compat.tcdiscovery.TcdiscoveryBridge.init();
            ACTIVE_BRIDGES.add("tcdiscovery");
        }
        if (ModList.get().isLoaded("tcechoes")) {
            com.nightcity.exchange.compat.tcechoes.TcechoesBridge.init();
            ACTIVE_BRIDGES.add("tcechoes");
        }
        if (ModList.get().isLoaded("tcecology")) {
            com.nightcity.exchange.compat.tcecology.TcecologyBridge.init();
            ACTIVE_BRIDGES.add("tcecology");
        }
        if (ModList.get().isLoaded("tcfrontiers")) {
            com.nightcity.exchange.compat.tcfrontiers.TcfrontiersBridge.init();
            ACTIVE_BRIDGES.add("tcfrontiers");
        }
        if (ModList.get().isLoaded("tcindustry")) {
            com.nightcity.exchange.compat.tcindustry.TcindustryBridge.init();
            ACTIVE_BRIDGES.add("tcindustry");
        }
        if (ModList.get().isLoaded("tcivilization")) {
            com.nightcity.exchange.compat.tcivilization.TcivilizationBridge.init();
            ACTIVE_BRIDGES.add("tcivilization");
        }
        if (ModList.get().isLoaded("tcnations")) {
            com.nightcity.exchange.compat.tcnations.TcnationsBridge.init();
            ACTIVE_BRIDGES.add("tcnations");
        }
        if (ModList.get().isLoaded("tcnetworks")) {
            com.nightcity.exchange.compat.tcnetworks.TcnetworksBridge.init();
            ACTIVE_BRIDGES.add("tcnetworks");
        }
        if (ModList.get().isLoaded("tcorbit")) {
            com.nightcity.exchange.compat.tcorbit.TcorbitBridge.init();
            ACTIVE_BRIDGES.add("tcorbit");
        }
        if (ModList.get().isLoaded("tcsettlements")) {
            com.nightcity.exchange.compat.tcsettlements.TcsettlementsBridge.init();
            ACTIVE_BRIDGES.add("tcsettlements");
        }
        if (ModList.get().isLoaded("tcskills")) {
            com.nightcity.exchange.compat.tcskills.TcskillsBridge.init();
            ACTIVE_BRIDGES.add("tcskills");
        }
        if (ModList.get().isLoaded("tctechnology")) {
            com.nightcity.exchange.compat.tctechnology.TctechnologyBridge.init();
            ACTIVE_BRIDGES.add("tctechnology");
        }
        if (ModList.get().isLoaded("tcwildlands")) {
            com.nightcity.exchange.compat.tcwildlands.TcwildlandsBridge.init();
            ACTIVE_BRIDGES.add("tcwildlands");
        }
    }

    public static Set<String> activeBridges() {
        return Collections.unmodifiableSet(ACTIVE_BRIDGES);
    }
}
