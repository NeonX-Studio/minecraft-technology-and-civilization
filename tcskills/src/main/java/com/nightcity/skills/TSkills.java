package com.nightcity.skills;

import com.nightcity.skills.api.GainFlag;
import com.nightcity.skills.api.GainResult;
import com.nightcity.skills.api.Skill;
import com.nightcity.skills.compat.tcivilization.TcivilizationBridge;
import com.nightcity.skills.data.TalentLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Mod(TSkills.MOD_ID)
public final class TSkills {
    public static final String MOD_ID = "tcskills";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredItem<Item> REFLECTION_TOKEN = ITEMS.registerSimpleItem("reflection_token");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> new ItemStack(REFLECTION_TOKEN.get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private static final TalentLoader TALENT_LOADER = new TalentLoader();
    private static final Map<UUID, PlayerSkillsProfile> MEMORY_PROFILES = new ConcurrentHashMap<>();
    private static final com.nightcity.skills.api.SkillsApi API_IMPL = new SkillsApiImpl();

    static {
        for (String s : List.of("ui.skill_level_up", "ui.growth_level_up", "ui.talent_commit", "ui.specialty_select", "ui.respec")) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, s);
            SOUNDS.register(s, () -> SoundEvent.createVariableRangeEvent(id));
        }
    }

    public TSkills(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SYNCED, SkillsConfig.SERVER_SPEC, "tcskills-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, SkillsConfig.CLIENT_SPEC, "tcskills-client.toml");
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(new SkillsEvents());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TcivilizationBridge::init);
    }

    public static TalentLoader talents() {
        return TALENT_LOADER;
    }

    public static com.nightcity.skills.api.SkillsApi api() {
        return API_IMPL;
    }

    public static PlayerSkillsProfile resolveProfile(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.overworld() != null) {
            return SkillsSavedData.get(server.overworld()).profile(playerId);
        }
        return MEMORY_PROFILES.computeIfAbsent(playerId, PlayerSkillsProfile::empty);
    }

    public static void storeProfile(PlayerSkillsProfile profile) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.overworld() != null) {
            SkillsSavedData.get(server.overworld()).putProfile(profile);
        } else {
            MEMORY_PROFILES.put(profile.playerId(), profile);
        }
    }

    private static final class SkillsApiImpl implements com.nightcity.skills.api.SkillsApi {
        private static Predicate<String> modCheck() {
            return m -> ModList.get() != null && ModList.get().isLoaded(m);
        }

        @Override
        public int level(Player player, Skill skill) {
            return level(player.getUUID(), skill);
        }

        @Override
        public int level(UUID playerId, Skill skill) {
            return skill.isActive(modCheck()) ? resolveProfile(playerId).skillLevel(skill) : 0;
        }

        @Override
        public long xp(UUID playerId, Skill skill) {
            return resolveProfile(playerId).skillExperience(skill.shortId());
        }

        @Override
        public long reserveXp(UUID playerId, Skill skill) {
            return resolveProfile(playerId).reserveXp(skill);
        }

        @Override
        public int growthLevel(Player player) {
            return resolveProfile(player.getUUID()).growthLevel();
        }

        @Override
        public int freePoints(Player player) {
            return resolveProfile(player.getUUID()).skillPoints();
        }

        @Override
        public List<Skill> specialties(Player player) {
            return resolveProfile(player.getUUID()).specialties().stream()
                    .map(Skill::resolve)
                    .flatMap(Optional::stream)
                    .toList();
        }

        @Override
        public boolean isSpecialty(Player player, Skill skill) {
            return resolveProfile(player.getUUID()).isSpecialty(skill);
        }

        @Override
        public boolean hasTalent(Player player, Identifier talentId) {
            var def = TALENT_LOADER.snapshot().byId().get(talentId);
            PlayerSkillsProfile p = resolveProfile(player.getUUID());
            return def != null ? p.isTalentActive(def, modCheck()) : p.hasTalent(talentId.toString());
        }

        @Override
        public double multiplier(Player player, Skill skill, Identifier effect) {
            return multiplier(player.getUUID(), skill, effect);
        }

        @Override
        public double multiplier(UUID playerId, Skill skill, Identifier effect) {
            return resolveProfile(playerId).multiplier(skill, effect.toString(), TALENT_LOADER.snapshot(), modCheck());
        }

        @Override
        public boolean hasOp(Player player, Identifier op) {
            return !Float.isNaN(opValue(player, op, Float.NaN));
        }

        @Override
        public float opValue(Player player, Identifier op, float fallback) {
            return resolveProfile(player.getUUID()).opValue(op.toString(), fallback, TALENT_LOADER.snapshot(), modCheck());
        }

        @Override
        public GainResult grant(ServerPlayer player, Skill skill, int amount, Identifier source, Set<GainFlag> flags) {
            ServerLevel level = (ServerLevel) player.level();
            return SkillsSavedData.get(level).addProficiency(player.getUUID(), skill.shortId(), amount, flags).gainResult();
        }

        @Override
        public GainResult grantOffline(UUID playerId, Skill skill, int amount, Identifier source) {
            var outcome = resolveProfile(playerId).gainProficiency(skill.shortId(), amount, Set.of(), TALENT_LOADER.snapshot(), modCheck());
            storeProfile(outcome.profile());
            return outcome.result();
        }

        @Override
        public @Nullable Object rawProfile(UUID playerId) {
            return resolveProfile(playerId);
        }
    }
}
