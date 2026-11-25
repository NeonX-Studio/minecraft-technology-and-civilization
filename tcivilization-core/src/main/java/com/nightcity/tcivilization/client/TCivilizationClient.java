package com.nightcity.tcivilization.client;

import com.nightcity.tcivilization.TCivilization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Client-only @Mod entrypoint ensuring client classes are never loaded on dedicated servers.
 */
@Mod(value = TCivilization.MOD_ID, dist = Dist.CLIENT)
public final class TCivilizationClient {
    public TCivilizationClient(IEventBus modBus) {
        TcClientInit.init(modBus);
    }
}
