package com.nightcity.frontiers.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FrontiersRegistries {
    public static final String MOD_ID = "tcfrontiers";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    private static final List<String> BLOCK_PATHS = List.of(
        "watch_fire", "stake_barrier", "guard_post", "outpost_core", "supply_cache", "field_tent",
        "expedition_table", "bounty_board", "archery_target", "watchtower_platform", "alarm_bell", "signal_beacon",
        "arrow_slit", "portcullis", "gate_winch", "training_dummy", "armory_rack", "caltrops",
        "war_banner", "drawbridge_hinge", "murder_hole", "pitch_cauldron", "pavise", "gabion",
        "powder_magazine", "gun_platform", "sandbag", "barbed_wire", "earthwork_parapet", "pillbox_embrasure",
        "searchlight", "machine_gun_nest", "air_raid_siren", "blast_door", "anti_aircraft_gun", "rapid_response_console",
        "hazard_forecast_console", "emergency_beacon"
    );
    private static final List<String> BLOCK_ITEM_PATHS = List.of(
        "watch_fire", "stake_barrier", "guard_post", "outpost_core", "supply_cache", "field_tent",
        "expedition_table", "bounty_board", "archery_target", "watchtower_platform", "alarm_bell", "signal_beacon",
        "arrow_slit", "portcullis", "gate_winch", "training_dummy", "armory_rack", "caltrops",
        "war_banner", "drawbridge_hinge", "murder_hole", "pitch_cauldron", "pavise", "gabion",
        "powder_magazine", "gun_platform", "sandbag", "barbed_wire", "earthwork_parapet", "pillbox_embrasure",
        "searchlight", "machine_gun_nest", "air_raid_siren", "blast_door", "anti_aircraft_gun", "rapid_response_console",
        "hazard_forecast_console", "emergency_beacon"
    );
    private static final List<String> STANDALONE_ITEM_PATHS = List.of(
        "signal_horn", "bronze_dagger", "bronze_spear", "khopesh", "bronze_battle_axe", "sling",
        "sling_bullet", "javelin", "composite_bow", "bronze_arrow", "wicker_shield", "bronze_shield",
        "bronze_helmet", "linen_cuirass", "bronze_greaves", "ration_pack", "water_cask", "fodder_sack",
        "field_medical_kit", "bandit_insignia", "iron_sword", "iron_spear", "iron_battle_axe", "round_shield",
        "iron_helmet", "lamellar_cuirass", "lamellar_tassets", "hobnailed_boots", "crossbow", "crossbow_bolt",
        "iron_arrow", "siege_ladder", "siege_engine_frame", "stone_shot", "repair_kit", "longsword",
        "halberd", "pike", "mace", "warhammer", "longbow", "arbalest",
        "heater_shield", "bodkin_arrow", "mail_hauberk", "mail_chausses", "armet", "plate_cuirass",
        "plate_cuisses", "sabatons", "arquebus", "matchlock_musket", "flintlock_pistol", "rapier",
        "powder_flask", "lead_ball", "paper_cartridge", "cannonball", "morion", "pikeman_cuirass",
        "buff_coat", "rifled_musket", "breech_loading_rifle", "revolver", "bayonet", "cavalry_sabre",
        "metallic_cartridge", "artillery_shell", "firing_table", "bolt_action_rifle", "light_machine_gun", "hand_grenade",
        "mortar_shell", "steel_helmet", "assault_rifle", "submachine_gun", "intermediate_cartridge", "flak_jacket",
        "combat_boots", "carbine", "aramid_helmet", "plate_carrier", "night_vision_goggles"
    );
    private static final List<String> SOUND_PATHS = List.of(
        "item.signal_horn.blow", "entity.bowstring.release", "event.war_drum", "block.alarm_bell.ring", "entity.trebuchet.release", "entity.musket.fire",
        "entity.cannon.fire", "entity.machine_gun.fire", "block.air_raid_siren.wail"
    );

    public static final Map<String, DeferredBlock<Block>> REGISTERED_BLOCKS;
    public static final Map<String, DeferredItem<? extends Item>> REGISTERED_ITEMS;
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> REGISTERED_SOUNDS;
    public static final Map<String, List<Identifier>> APPENDIX_A_IDS;

    static {
        Map<String, DeferredBlock<Block>> bMap = new LinkedHashMap<>();
        for (String path : BLOCK_PATHS) {
            bMap.put(path, BLOCKS.registerSimpleBlock(path));
        }
        REGISTERED_BLOCKS = Collections.unmodifiableMap(bMap);

        Map<String, DeferredItem<? extends Item>> iMap = new LinkedHashMap<>();
        for (String path : BLOCK_ITEM_PATHS) {
            DeferredBlock<Block> block = bMap.get(path);
            if (block != null) {
                DeferredItem<BlockItem> bi = ITEMS.registerSimpleBlockItem(path, block);
                iMap.put(path, bi);
            }
        }
        for (String path : STANDALONE_ITEM_PATHS) {
            DeferredBlock<Block> block = bMap.get(path);
            if (block != null) {
                DeferredItem<BlockItem> bi = ITEMS.registerSimpleBlockItem(path, block);
                iMap.put(path, bi);
            } else {
                iMap.put(path, ITEMS.registerSimpleItem(path));
            }
        }
        REGISTERED_ITEMS = Collections.unmodifiableMap(iMap);

        Map<String, DeferredHolder<SoundEvent, SoundEvent>> sMap = new LinkedHashMap<>();
        for (String path : SOUND_PATHS) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
            sMap.put(path, SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id)));
        }
        REGISTERED_SOUNDS = Collections.unmodifiableMap(sMap);

        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("block", List.of(
                "tcfrontiers:watch_fire", "tcfrontiers:stake_barrier", "tcfrontiers:guard_post", "tcfrontiers:outpost_core", "tcfrontiers:supply_cache", "tcfrontiers:field_tent",
                "tcfrontiers:expedition_table", "tcfrontiers:bounty_board", "tcfrontiers:archery_target", "tcfrontiers:watchtower_platform", "tcfrontiers:alarm_bell", "tcfrontiers:signal_beacon",
                "tcfrontiers:arrow_slit", "tcfrontiers:portcullis", "tcfrontiers:gate_winch", "tcfrontiers:training_dummy", "tcfrontiers:armory_rack", "tcfrontiers:caltrops",
                "tcfrontiers:war_banner", "tcfrontiers:drawbridge_hinge", "tcfrontiers:murder_hole", "tcfrontiers:pitch_cauldron", "tcfrontiers:pavise", "tcfrontiers:gabion",
                "tcfrontiers:powder_magazine", "tcfrontiers:gun_platform", "tcfrontiers:sandbag", "tcfrontiers:barbed_wire", "tcfrontiers:earthwork_parapet", "tcfrontiers:pillbox_embrasure",
                "tcfrontiers:searchlight", "tcfrontiers:machine_gun_nest", "tcfrontiers:air_raid_siren", "tcfrontiers:blast_door", "tcfrontiers:anti_aircraft_gun", "tcfrontiers:rapid_response_console",
                "tcfrontiers:hazard_forecast_console", "tcfrontiers:emergency_beacon"
        ));
        map.put("item", List.of(
                "tcfrontiers:signal_horn", "tcfrontiers:bronze_dagger", "tcfrontiers:bronze_spear", "tcfrontiers:khopesh", "tcfrontiers:bronze_battle_axe", "tcfrontiers:sling",
                "tcfrontiers:sling_bullet", "tcfrontiers:javelin", "tcfrontiers:composite_bow", "tcfrontiers:bronze_arrow", "tcfrontiers:wicker_shield", "tcfrontiers:bronze_shield",
                "tcfrontiers:bronze_helmet", "tcfrontiers:linen_cuirass", "tcfrontiers:bronze_greaves", "tcfrontiers:ration_pack", "tcfrontiers:water_cask", "tcfrontiers:fodder_sack",
                "tcfrontiers:field_medical_kit", "tcfrontiers:bandit_insignia", "tcfrontiers:iron_sword", "tcfrontiers:iron_spear", "tcfrontiers:iron_battle_axe", "tcfrontiers:round_shield",
                "tcfrontiers:iron_helmet", "tcfrontiers:lamellar_cuirass", "tcfrontiers:lamellar_tassets", "tcfrontiers:hobnailed_boots", "tcfrontiers:crossbow", "tcfrontiers:crossbow_bolt",
                "tcfrontiers:iron_arrow", "tcfrontiers:siege_ladder", "tcfrontiers:siege_engine_frame", "tcfrontiers:stone_shot", "tcfrontiers:repair_kit", "tcfrontiers:longsword",
                "tcfrontiers:halberd", "tcfrontiers:pike", "tcfrontiers:mace", "tcfrontiers:warhammer", "tcfrontiers:longbow", "tcfrontiers:arbalest",
                "tcfrontiers:heater_shield", "tcfrontiers:bodkin_arrow", "tcfrontiers:mail_hauberk", "tcfrontiers:mail_chausses", "tcfrontiers:armet", "tcfrontiers:plate_cuirass",
                "tcfrontiers:plate_cuisses", "tcfrontiers:sabatons", "tcfrontiers:arquebus", "tcfrontiers:matchlock_musket", "tcfrontiers:flintlock_pistol", "tcfrontiers:rapier",
                "tcfrontiers:powder_flask", "tcfrontiers:lead_ball", "tcfrontiers:paper_cartridge", "tcfrontiers:cannonball", "tcfrontiers:morion", "tcfrontiers:pikeman_cuirass",
                "tcfrontiers:buff_coat", "tcfrontiers:rifled_musket", "tcfrontiers:breech_loading_rifle", "tcfrontiers:revolver", "tcfrontiers:bayonet", "tcfrontiers:cavalry_sabre",
                "tcfrontiers:metallic_cartridge", "tcfrontiers:artillery_shell", "tcfrontiers:firing_table", "tcfrontiers:bolt_action_rifle", "tcfrontiers:light_machine_gun", "tcfrontiers:hand_grenade",
                "tcfrontiers:mortar_shell", "tcfrontiers:steel_helmet", "tcfrontiers:assault_rifle", "tcfrontiers:submachine_gun", "tcfrontiers:intermediate_cartridge", "tcfrontiers:flak_jacket",
                "tcfrontiers:combat_boots", "tcfrontiers:carbine", "tcfrontiers:aramid_helmet", "tcfrontiers:plate_carrier", "tcfrontiers:night_vision_goggles"
        ));
        map.put("entity", List.of(
                "tcfrontiers:ranged_projectile", "tcfrontiers:ballistic_projectile", "tcfrontiers:siege_projectile", "tcfrontiers:thrown_grenade", "tcfrontiers:battering_ram", "tcfrontiers:siege_tower",
                "tcfrontiers:mangonel", "tcfrontiers:ballista", "tcfrontiers:trebuchet", "tcfrontiers:bombard", "tcfrontiers:field_cannon", "tcfrontiers:field_gun",
                "tcfrontiers:mortar"
        ));
        map.put("structure", List.of(
                "tcfrontiers:bandit_lair", "tcfrontiers:bandit_fort", "tcfrontiers:raider_camp", "tcfrontiers:pirate_cove", "tcfrontiers:deserter_camp"
        ));
        map.put("block_entity", List.of(
                "tcfrontiers:outpost_core", "tcfrontiers:supply_cache", "tcfrontiers:expedition_table", "tcfrontiers:bounty_board", "tcfrontiers:armory_rack", "tcfrontiers:alarm_bell",
                "tcfrontiers:signal_beacon", "tcfrontiers:gate_winch", "tcfrontiers:drawbridge_hinge", "tcfrontiers:war_banner", "tcfrontiers:powder_magazine", "tcfrontiers:searchlight",
                "tcfrontiers:machine_gun_nest", "tcfrontiers:anti_aircraft_gun", "tcfrontiers:rapid_response_console", "tcfrontiers:hazard_forecast_console", "tcfrontiers:emergency_beacon"
        ));
        map.put("menu", List.of(
                "tcfrontiers:outpost", "tcfrontiers:supply_cache", "tcfrontiers:expedition_loadout", "tcfrontiers:bounty_board", "tcfrontiers:armory_rack", "tcfrontiers:siege_engine"
        ));
        map.put("keybind", List.of(
                "tcfrontiers:reload"
        ));
        map.put("payload", List.of(
                "tcfrontiers:danger_sync", "tcfrontiers:threat_alert", "tcfrontiers:expedition_plan_update", "tcfrontiers:expedition_state_sync", "tcfrontiers:outpost_sync", "tcfrontiers:bounty_list_sync",
                "tcfrontiers:militia_order", "tcfrontiers:siege_engine_control", "tcfrontiers:weapon_action", "tcfrontiers:band_intel_sync"
        ));
        map.put("hud_widget", List.of(
                "tcfrontiers:expedition_supply", "tcfrontiers:danger_level", "tcfrontiers:ammo_counter"
        ));
        map.put("civ_tab", List.of(
                "tcfrontiers:expeditions", "tcfrontiers:defense"
        ));
        map.put("region_layer", List.of(
                "tcfrontiers:danger"
        ));
        map.put("sound", List.of(
                "tcfrontiers:item.signal_horn.blow", "tcfrontiers:entity.bowstring.release", "tcfrontiers:event.war_drum", "tcfrontiers:block.alarm_bell.ring", "tcfrontiers:entity.trebuchet.release", "tcfrontiers:entity.musket.fire",
                "tcfrontiers:entity.cannon.fire", "tcfrontiers:entity.machine_gun.fire", "tcfrontiers:block.air_raid_siren.wail"
        ));
        Map<String, List<Identifier>> idMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            idMap.put(entry.getKey(), entry.getValue().stream().map(Identifier::parse).toList());
        }
        APPENDIX_A_IDS = Collections.unmodifiableMap(idMap);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> new ItemStack(REGISTERED_ITEMS.getOrDefault("outpost_core", REGISTERED_ITEMS.values().iterator().next()).get()))
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private FrontiersRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
    }
}
