package com.nightcity.technology.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.technology.TTechnology;
import com.nightcity.technology.compat.tcivilization.TcivilizationBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TTechnology.MOD_ID, dist = Dist.CLIENT)
public final class TechnologyClient {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TTechnology.MOD_ID, "controls"));

    private static KeyMapping openTechTreeKey;

    public TechnologyClient(IEventBus modBus) {
        modBus.addListener(TechnologyClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(TechnologyClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        openTechTreeKey = new KeyMapping(
                "key.tctechnology.tech_tree",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_U,
                CATEGORY
        );
        event.register(openTechTreeKey);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (openTechTreeKey == null) {
            return;
        }
        while (openTechTreeKey.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.canInterruptScreen()) {
                if (!TcivilizationBridge.tryOpenTechTab()) {
                    mc.setScreenAndShow(new TechTreeScreen());
                }
            }
        }
    }
}
