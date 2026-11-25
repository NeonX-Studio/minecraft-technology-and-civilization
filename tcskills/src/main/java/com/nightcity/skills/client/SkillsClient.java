package com.nightcity.skills.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.skills.TSkills;
import com.nightcity.skills.compat.tcivilization.TcivilizationBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TSkills.MOD_ID, dist = Dist.CLIENT)
public final class SkillsClient {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TSkills.MOD_ID, "controls"));

    private static KeyMapping openSkillsKey;

    public SkillsClient(IEventBus modBus) {
        modBus.addListener(SkillsClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(SkillsClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        boolean standalone = ModList.get() == null || !ModList.get().isLoaded("tcivilization");
        int defaultKey = standalone ? InputConstants.KEY_J : InputConstants.UNKNOWN.getValue();
        openSkillsKey = new KeyMapping("key.tcskills.skills", InputConstants.Type.KEYBOARD, defaultKey, CATEGORY);
        event.register(openSkillsKey);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (openSkillsKey == null) {
            return;
        }
        while (openSkillsKey.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.canInterruptScreen()) {
                if (!TcivilizationBridge.tryOpenSkillsTab()) {
                    mc.setScreenAndShow(new SkillsPageScreen());
                }
            }
        }
    }
}
