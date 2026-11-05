package com.nightcity.skills;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TSkills.MOD_ID)
public final class TSkills {
    public static final String MOD_ID = "tcskills";

    public TSkills(IEventBus modBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new SkillsEvents());
    }
}
