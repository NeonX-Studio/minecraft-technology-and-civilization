package com.nightcity.echoes.compat;

import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EchoesCompat {
    private static final Set<String> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private EchoesCompat() {
    }

    public static void initAll() {
        if (ModList.get().isLoaded("tcatlas")) {
            com.nightcity.echoes.compat.tcatlas.TcatlasBridge.init();
            ACTIVE_BRIDGES.add("tcatlas");
        }
        if (ModList.get().isLoaded("tcbeyond")) {
            com.nightcity.echoes.compat.tcbeyond.TcbeyondBridge.init();
            ACTIVE_BRIDGES.add("tcbeyond");
        }
        if (ModList.get().isLoaded("tccommons")) {
            com.nightcity.echoes.compat.tccommons.TccommonsBridge.init();
            ACTIVE_BRIDGES.add("tccommons");
        }
        if (ModList.get().isLoaded("tccultivation")) {
            com.nightcity.echoes.compat.tccultivation.TccultivationBridge.init();
            ACTIVE_BRIDGES.add("tccultivation");
        }
        if (ModList.get().isLoaded("tcdiscovery")) {
            com.nightcity.echoes.compat.tcdiscovery.TcdiscoveryBridge.init();
            ACTIVE_BRIDGES.add("tcdiscovery");
        }
        if (ModList.get().isLoaded("tcecology")) {
            com.nightcity.echoes.compat.tcecology.TcecologyBridge.init();
            ACTIVE_BRIDGES.add("tcecology");
        }
        if (ModList.get().isLoaded("tcexchange")) {
            com.nightcity.echoes.compat.tcexchange.TcexchangeBridge.init();
            ACTIVE_BRIDGES.add("tcexchange");
        }
        if (ModList.get().isLoaded("tcfrontiers")) {
            com.nightcity.echoes.compat.tcfrontiers.TcfrontiersBridge.init();
            ACTIVE_BRIDGES.add("tcfrontiers");
        }
        if (ModList.get().isLoaded("tcindustry")) {
            com.nightcity.echoes.compat.tcindustry.TcindustryBridge.init();
            ACTIVE_BRIDGES.add("tcindustry");
        }
        if (ModList.get().isLoaded("tcivilization")) {
            com.nightcity.echoes.compat.tcivilization.TcivilizationBridge.init();
            ACTIVE_BRIDGES.add("tcivilization");
        }
        if (ModList.get().isLoaded("tcnations")) {
            com.nightcity.echoes.compat.tcnations.TcnationsBridge.init();
            ACTIVE_BRIDGES.add("tcnations");
        }
        if (ModList.get().isLoaded("tcnetworks")) {
            com.nightcity.echoes.compat.tcnetworks.TcnetworksBridge.init();
            ACTIVE_BRIDGES.add("tcnetworks");
        }
        if (ModList.get().isLoaded("tcorbit")) {
            com.nightcity.echoes.compat.tcorbit.TcorbitBridge.init();
            ACTIVE_BRIDGES.add("tcorbit");
        }
        if (ModList.get().isLoaded("tcsettlements")) {
            com.nightcity.echoes.compat.tcsettlements.TcsettlementsBridge.init();
            ACTIVE_BRIDGES.add("tcsettlements");
        }
        if (ModList.get().isLoaded("tcskills")) {
            com.nightcity.echoes.compat.tcskills.TcskillsBridge.init();
            ACTIVE_BRIDGES.add("tcskills");
        }
        if (ModList.get().isLoaded("tctechnology")) {
            com.nightcity.echoes.compat.tctechnology.TctechnologyBridge.init();
            ACTIVE_BRIDGES.add("tctechnology");
        }
        if (ModList.get().isLoaded("tcwildlands")) {
            com.nightcity.echoes.compat.tcwildlands.TcwildlandsBridge.init();
            ACTIVE_BRIDGES.add("tcwildlands");
        }
    }

    public static Set<String> activeBridges() {
        return Collections.unmodifiableSet(ACTIVE_BRIDGES);
    }
}
