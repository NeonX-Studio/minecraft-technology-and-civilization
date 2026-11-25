package com.nightcity.atlas.compat;

import net.neoforged.fml.ModList;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AtlasCompat {
    private static final Set<String> ACTIVE_BRIDGES = ConcurrentHashMap.newKeySet();

    private AtlasCompat() {
    }

    public static void initAll() {
        if (ModList.get().isLoaded("tcbeyond")) {
            com.nightcity.atlas.compat.tcbeyond.TcbeyondBridge.init();
            ACTIVE_BRIDGES.add("tcbeyond");
        }
        if (ModList.get().isLoaded("tccommons")) {
            com.nightcity.atlas.compat.tccommons.TccommonsBridge.init();
            ACTIVE_BRIDGES.add("tccommons");
        }
        if (ModList.get().isLoaded("tccultivation")) {
            com.nightcity.atlas.compat.tccultivation.TccultivationBridge.init();
            ACTIVE_BRIDGES.add("tccultivation");
        }
        if (ModList.get().isLoaded("tcdiscovery")) {
            com.nightcity.atlas.compat.tcdiscovery.TcdiscoveryBridge.init();
            ACTIVE_BRIDGES.add("tcdiscovery");
        }
        if (ModList.get().isLoaded("tcechoes")) {
            com.nightcity.atlas.compat.tcechoes.TcechoesBridge.init();
            ACTIVE_BRIDGES.add("tcechoes");
        }
        if (ModList.get().isLoaded("tcecology")) {
            com.nightcity.atlas.compat.tcecology.TcecologyBridge.init();
            ACTIVE_BRIDGES.add("tcecology");
        }
        if (ModList.get().isLoaded("tcexchange")) {
            com.nightcity.atlas.compat.tcexchange.TcexchangeBridge.init();
            ACTIVE_BRIDGES.add("tcexchange");
        }
        if (ModList.get().isLoaded("tcfrontiers")) {
            com.nightcity.atlas.compat.tcfrontiers.TcfrontiersBridge.init();
            ACTIVE_BRIDGES.add("tcfrontiers");
        }
        if (ModList.get().isLoaded("tcindustry")) {
            com.nightcity.atlas.compat.tcindustry.TcindustryBridge.init();
            ACTIVE_BRIDGES.add("tcindustry");
        }
        if (ModList.get().isLoaded("tcivilization")) {
            com.nightcity.atlas.compat.tcivilization.TcivilizationBridge.init();
            ACTIVE_BRIDGES.add("tcivilization");
        }
        if (ModList.get().isLoaded("tcnations")) {
            com.nightcity.atlas.compat.tcnations.TcnationsBridge.init();
            ACTIVE_BRIDGES.add("tcnations");
        }
        if (ModList.get().isLoaded("tcnetworks")) {
            com.nightcity.atlas.compat.tcnetworks.TcnetworksBridge.init();
            ACTIVE_BRIDGES.add("tcnetworks");
        }
        if (ModList.get().isLoaded("tcorbit")) {
            com.nightcity.atlas.compat.tcorbit.TcorbitBridge.init();
            ACTIVE_BRIDGES.add("tcorbit");
        }
        if (ModList.get().isLoaded("tcsettlements")) {
            com.nightcity.atlas.compat.tcsettlements.TcsettlementsBridge.init();
            ACTIVE_BRIDGES.add("tcsettlements");
        }
        if (ModList.get().isLoaded("tcskills")) {
            com.nightcity.atlas.compat.tcskills.TcskillsBridge.init();
            ACTIVE_BRIDGES.add("tcskills");
        }
        if (ModList.get().isLoaded("tctechnology")) {
            com.nightcity.atlas.compat.tctechnology.TctechnologyBridge.init();
            ACTIVE_BRIDGES.add("tctechnology");
        }
        if (ModList.get().isLoaded("tcwildlands")) {
            com.nightcity.atlas.compat.tcwildlands.TcwildlandsBridge.init();
            ACTIVE_BRIDGES.add("tcwildlands");
        }
    }

    public static Set<String> activeBridges() {
        return Collections.unmodifiableSet(ACTIVE_BRIDGES);
    }
}
