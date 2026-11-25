package com.nightcity.tcivilization.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nightcity.tcivilization.TCivilization;
import com.nightcity.tcivilization.client.api.CeremonyOverlay;
import com.nightcity.tcivilization.client.api.CivPanel;
import com.nightcity.tcivilization.client.api.CivPanelTabRegistry;
import com.nightcity.tcivilization.client.api.CivPanelTabSpec;
import com.nightcity.tcivilization.client.api.CivTabGroup;
import com.nightcity.tcivilization.client.api.CommandWheelRegistry;
import com.nightcity.tcivilization.client.api.DeepLinkRegistry;
import com.nightcity.tcivilization.client.api.HudWidgetRegistry;
import com.nightcity.tcivilization.client.api.PersonSectionRegistry;
import com.nightcity.tcivilization.client.api.RegisterCivTabsEvent;
import com.nightcity.tcivilization.client.api.RegisterDeepLinksEvent;
import com.nightcity.tcivilization.client.api.RegisterHudWidgetsEvent;
import com.nightcity.tcivilization.client.api.RegisterPersonSectionsEvent;
import com.nightcity.tcivilization.client.api.RegisterTargetInfoEvent;
import com.nightcity.tcivilization.client.api.TargetInfo;
import com.nightcity.tcivilization.client.api.TargetInfoProvider;
import com.nightcity.tcivilization.client.api.WheelContext;
import com.nightcity.tcivilization.client.api.WheelOption;
import com.nightcity.tcivilization.client.api.WheelProvider;
import com.nightcity.tcivilization.client.api.WheelSlotHint;
import com.nightcity.tcivilization.client.api.WheelTarget;
import com.nightcity.tcivilization.client.renderer.PersonRenderer;
import com.nightcity.tcivilization.person.PersonEntity;
import com.nightcity.tcivilization.registry.TcRegistries;
import com.nightcity.tcivilization.world.PlaceholderBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * Client-only initialization: registers J, Z, conditional H fallback, HUD layer, built-in tabs, and PersonRenderer.
 */
public final class TcClientInit {
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "main"));

    @Nullable
    private static KeyMapping civPanelKey;
    @Nullable
    private static KeyMapping commandWheelKey;
    @Nullable
    private static KeyMapping hudLevelFallbackKey;

    private static boolean builtinsRegistered = false;

    private TcClientInit() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(TcClientInit::onRegisterKeyMappings);
        modBus.addListener(TcClientInit::onRegisterGuiLayers);
        modBus.addListener(TcClientInit::onRegisterLayerDefinitions);
        modBus.addListener(TcClientInit::onRegisterRenderers);
        modBus.addListener((FMLClientSetupEvent event) -> registerClientShells(modBus));
        NeoForge.EVENT_BUS.addListener(TcClientInit::onClientTickPost);
    }

    private static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                com.nightcity.tcivilization.client.model.PersonModelLocations.LOD0,
                com.nightcity.tcivilization.client.model.PersonLod0ModelGeometry::createBodyLayer);
        event.registerLayerDefinition(
                com.nightcity.tcivilization.client.model.PersonModelLocations.LOD1,
                com.nightcity.tcivilization.client.model.PersonLod1ModelGeometry::createBodyLayer);
        event.registerLayerDefinition(
                com.nightcity.tcivilization.client.model.PersonModelLocations.LOD2,
                com.nightcity.tcivilization.client.model.PersonLod2ModelGeometry::createBodyLayer);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        civPanelKey = new KeyMapping("key.tcivilization.civ_panel", KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_J, KEY_CATEGORY);
        commandWheelKey = new KeyMapping("key.tcivilization.command_wheel", KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, KEY_CATEGORY);
        event.register(civPanelKey);
        event.register(commandWheelKey);

        if (!ModList.get().isLoaded("tcatlas")) {
            hudLevelFallbackKey = new KeyMapping("key.tcivilization.hud_level", KeyConflictContext.IN_GAME, InputConstants.Type.KEYBOARD, InputConstants.KEY_H, KEY_CATEGORY);
            event.register(hudLevelFallbackKey);
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, TcHudLayer.LAYER_ID, TcHudLayer.INSTANCE);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TcRegistries.PERSON_ENTITY.get(), PersonRenderer::new);
    }

    public static synchronized void registerClientShells(IEventBus modBus) {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;

        CivPanelTabRegistry.global().register(new CivPanelTabSpec(
                CivPanel.TAB_OVERVIEW,
                CivTabGroup.CIVILIZATION,
                0,
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "textures/gui/icon/overview.png"),
                Optional.empty(),
                ctx -> true,
                Optional.empty(),
                CivPanelScreen::createOverviewPage
        ));
        CivPanelTabRegistry.global().register(new CivPanelTabSpec(
                CivPanel.TAB_CHRONICLE,
                CivTabGroup.KNOWLEDGE,
                900,
                Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "textures/gui/icon/chronicle.png"),
                Optional.empty(),
                ctx -> true,
                Optional.empty(),
                CivPanelScreen::createChroniclePage
        ));

        TcHudLayer.registerBuiltinWidgets(HudWidgetRegistry.global());

        DeepLinkRegistry.global().register(TCivilization.MOD_ID, link -> {
            if ("chronicle".equals(link.path())) {
                CivPanel.open(CivPanel.TAB_CHRONICLE);
                return true;
            }
            CivPanel.open(CivPanel.TAB_OVERVIEW);
            return true;
        });

        TargetInfoProvider.Registry.global().register((hit, level) -> {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof PersonEntity pe) {
                return Optional.of(new TargetInfo(
                        pe.getDisplayName(),
                        "T&C Core",
                        List.of(Component.translatable("gui.tcivilization.person.target_hint")),
                        Optional.empty()
                ));
            }
            if (hit instanceof BlockHitResult bhr && level.getBlockEntity(bhr.getBlockPos()) instanceof PlaceholderBlockEntity pbe) {
                return Optional.of(new TargetInfo(
                        Component.translatable("block.tcivilization.placeholder"),
                        "T&C Core",
                        List.of(Component.translatable("block.tcivilization.placeholder.inspect", pbe.originalId().toString())),
                        Optional.empty()
                ));
            }
            return Optional.empty();
        });

        CommandWheelRegistry.global().register(new WheelProvider() {
            private final Identifier id = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "core_wheel");

            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public List<WheelOption> options(WheelContext ctx) {
                return defaultWheelOptions();
            }

            @Override
            public List<WheelOption> predict(com.nightcity.tcivilization.client.api.ClientWheelContext ctx) {
                return defaultWheelOptions();
            }

            @Override
            public void execute(ServerPlayer player, WheelTarget target, Identifier optionId) {
            }

            private List<WheelOption> defaultWheelOptions() {
                return List.of(new WheelOption(
                        Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "open_civ_panel"),
                        Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "textures/gui/icon/overview.png"),
                        Component.translatable("gui.tcivilization.civ_panel.title"),
                        50,
                        true,
                        Optional.empty(),
                        false,
                        WheelSlotHint.PRIMARY_TOP
                ));
            }
        });

        TCivilization.postModBusEvent(modBus, new RegisterCivTabsEvent(CivPanelTabRegistry.global()));
        TCivilization.postModBusEvent(modBus, new RegisterHudWidgetsEvent(HudWidgetRegistry.global()));
        TCivilization.postModBusEvent(modBus, new RegisterPersonSectionsEvent(PersonSectionRegistry.global()));
        TCivilization.postModBusEvent(modBus, new RegisterTargetInfoEvent(TargetInfoProvider.Registry.global()));
        TCivilization.postModBusEvent(modBus, new RegisterDeepLinksEvent(DeepLinkRegistry.global()));
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        CeremonyOverlay.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        while (civPanelKey != null && civPanelKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                CivPanel.openDefault();
            }
        }
        while (commandWheelKey != null && commandWheelKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                WheelTarget target = resolveClientWheelTarget(mc);
                boolean hasPersonal = CivPanelTabRegistry.global().orderedTabs().stream()
                        .anyMatch(t -> t.group() == CivTabGroup.PERSONAL);
                mc.setScreenAndShow(new CommandWheelScreen(target, hasPersonal));
            }
        }
        while (hudLevelFallbackKey != null && hudLevelFallbackKey.consumeClick()) {
            if (mc.canInterruptScreen()) {
                String label = TcHudLayer.cycleUnifiedLevel();
                mc.player.sendOverlayMessage(Component.translatable("hud.tcivilization.level_changed", label));
            }
        }
    }

    private static WheelTarget resolveClientWheelTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult ehr) {
            WheelTarget.Kind kind = ehr.getEntity() instanceof PersonEntity ? WheelTarget.Kind.PERSON : WheelTarget.Kind.ENTITY;
            return new WheelTarget(kind, Optional.of(ehr.getEntity().getUUID()), Optional.empty(), ehr.getEntity().getDisplayName().getString());
        }
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK && mc.level != null) {
            String blockName = mc.level.getBlockState(bhr.getBlockPos()).getBlock().getName().getString();
            return new WheelTarget(WheelTarget.Kind.BLOCK, Optional.empty(), Optional.of(bhr.getBlockPos()), blockName);
        }
        return WheelTarget.none();
    }
}
