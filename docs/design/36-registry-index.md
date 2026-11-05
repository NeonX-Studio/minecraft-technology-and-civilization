# 36 · 注册表索引 Registry Index

> **本文件由 `tools/design/aggregate.py` 自动生成，请勿手工编辑。**
> 生成时间：2026-11-05 13:14
> 修改来源：各模组章节末尾的附录 A-D；重新生成：`python tools/design/aggregate.py`

本章汇总全部模组的注册 ID、科技与技能挂钩，并给出契约校验结果。它是实现阶段查重、查归属、查时代的唯一索引；实现状态（设计 / 占位 / 已实现）将在 P0 起由构建脚本从代码注册表回填（见 [40 §8](40-roadmap.md)）。

## 1. 校验摘要

| 项目 | 数量 |
| --- | --- |
| 已解析章节 | 18 / 18 |
| 注册 ID（附录 A） | 2827 |
| 科技（附录 B） | 370 / 360 |
| 联动登记（附录 C） | 306 |
| 技能挂钩（附录 D） | 342 |
| 错误 | 0 |
| 警告 | 0 |
| 提示 | 0 |

## 2. 注册内容统计

### 2.1 模组 × 类型

| 模组 | block | item | entity | fluid | dimension | biome | structure | block_entity | menu | keybind | payload | hud_widget | civ_tab | region_layer | sound | 合计 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 文明核心 `tcivilization` | 1 | 2 | 1 |  |  |  |  | 1 | 3 | 3 | 20 | 4 | 2 | 1 | 15 | **53** |
| 科技核心 `tctechnology` | 7 | 34 | 3 |  |  |  |  | 7 | 6 | 1 | 7 | 2 | 1 |  | 17 | **85** |
| 技能 `tcskills` |  |  |  |  |  |  |  |  |  | 1 | 14 | 1 | 1 |  | 3 | **20** |
| 山河 `tcwildlands` | 219 | 46 | 2 | 3 | 2 | 69 | 15 | 2 |  |  | 15 | 1 | 1 | 3 | 3 | **381** |
| 生息 `tcecology` | 91 | 13 | 53 |  |  |  | 2 | 9 | 4 |  | 8 | 2 | 1 | 1 | 6 | **190** |
| 田园 `tccultivation` | 73 | 98 | 7 |  |  |  |  | 5 | 5 |  | 9 | 1 | 1 | 1 |  | **200** |
| 工坊 `tcindustry` | 249 | 212 |  | 71 |  |  |  | 28 | 12 | 1 | 13 | 1 | 1 | 1 | 32 | **621** |
| 通途 `tcnetworks` | 86 | 30 | 41 |  |  |  | 1 | 12 | 6 | 2 | 10 | 2 | 1 | 1 | 18 | **210** |
| 聚落 `tcsettlements` | 43 | 6 |  |  |  |  |  | 10 | 5 |  | 9 | 1 | 1 | 1 | 8 | **84** |
| 市集 `tcexchange` | 17 | 19 |  |  |  |  |  | 13 | 9 | 1 | 10 | 1 | 1 | 1 | 3 | **75** |
| 求知 `tcdiscovery` | 38 | 32 |  |  |  |  |  | 15 | 6 | 2 | 9 | 2 | 1 | 1 | 6 | **112** |
| 遗响 `tcechoes` | 11 | 23 |  |  |  |  | 25 | 5 | 3 | 1 | 10 | 1 | 1 | 1 | 3 | **84** |
| 契约 `tccommons` | 15 | 6 |  |  |  |  |  | 11 | 4 |  | 14 | 1 | 1 | 1 | 5 | **58** |
| 远征 `tcfrontiers` | 38 | 83 | 13 |  |  |  | 5 | 17 | 6 | 1 | 10 | 3 | 2 | 1 | 9 | **188** |
| 异界 `tcbeyond` | 23 | 22 | 4 |  | 3 | 7 | 11 | 15 | 5 |  | 8 | 1 | 1 | 1 | 4 | **105** |
| 舆图 `tcatlas` | 26 | 29 |  |  |  |  |  | 16 | 10 | 9 | 17 | 4 | 1 | 1 | 9 | **122** |
| 列国 `tcnations` | 7 | 6 |  |  |  |  | 9 | 5 | 4 | 1 | 10 | 2 | 1 | 1 | 5 | **51** |
| 寰宇 `tcorbit` | 113 | 13 | 6 | 2 | 3 | 10 |  | 8 | 9 | 3 | 11 | 2 | 1 | 1 | 6 | **188** |
| **合计** | **1057** | **674** | **130** | **76** | **8** | **86** | **68** | **179** | **97** | **26** | **204** | **32** | **20** | **18** | **152** | **2827** |

### 2.2 按时代分布（注册内容首次出现的时代）

| 模组 | — | E0 | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 | E10 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| CIV | 40 | 13 |  |  |  |  |  |  |  |  |  |  |
| TEC | 19 | 40 | 15 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 3 | 1 |
| SKL | 20 |  |  |  |  |  |  |  |  |  |  |  |
| WLD | 114 | 120 | 22 | 21 | 21 | 20 | 15 | 5 | 18 | 17 | 8 |  |
| ECO | 15 | 141 | 3 |  | 2 | 2 | 2 | 7 | 4 | 7 | 6 | 1 |
| CUL | 11 | 15 | 71 | 52 | 20 | 2 | 12 | 2 | 2 | 6 | 4 | 3 |
| IND | 17 |  | 47 | 59 | 66 | 47 | 23 | 89 | 155 | 73 | 32 | 13 |
| NET | 17 | 4 | 7 | 25 | 14 | 17 | 6 | 38 | 36 | 24 | 20 | 2 |
| SET | 18 | 13 | 12 | 9 | 11 | 4 | 3 | 6 | 3 | 2 | 2 | 1 |
| EXC | 15 | 1 | 3 | 8 | 14 | 8 | 11 | 5 | 3 | 3 | 2 | 2 |
| DIS | 21 | 4 | 6 | 12 | 15 | 13 | 18 | 8 | 4 | 5 | 4 | 2 |
| ECH | 1 | 68 | 3 | 3 | 6 |  |  |  | 3 |  |  |  |
| COM |  | 21 | 14 | 12 | 3 | 3 |  |  | 2 |  | 2 | 1 |
| FRO | 13 |  | 6 | 42 | 38 | 22 | 22 | 12 | 13 | 10 | 6 | 4 |
| BEY | 57 |  |  |  | 7 | 16 | 10 | 2 | 4 | 5 | 4 |  |
| ATL | 32 | 1 | 11 | 7 | 7 | 6 | 9 | 13 | 8 | 16 | 4 | 8 |
| NAT | 12 | 6 | 2 | 17 | 6 | 5 | 2 |  | 1 |  |  |  |
| ORB | 11 |  |  |  |  |  |  |  |  | 13 | 48 | 116 |

## 3. 科技

### 3.1 时代分布对照（目标来自 [01 第 2 节](01-eras-and-progression.md)）

| 时代 | 目标 | 实际 | 差值 | 平均 RP |
| --- | ---: | ---: | ---: | ---: |
| E0 `stone` | 18 | 19 | +1 | 21 |
| E1 `neolithic` | 26 | 35 | +9 | 60 |
| E2 `bronze` | 30 | 35 | +5 | 152 |
| E3 `iron` | 32 | 38 | +6 | 316 |
| E4 `medieval` | 36 | 38 | +2 | 644 |
| E5 `renaissance` | 36 | 36 | +0 | 1,206 |
| E6 `industrial` | 40 | 38 | -2 | 2,161 |
| E7 `electric` | 38 | 33 | -5 | 3,782 |
| E8 `atomic` | 36 | 32 | -4 | 5,991 |
| E9 `information` | 34 | 31 | -3 | 9,058 |
| E10 `space` | 34 | 35 | +1 | 13,771 |
| **合计** | **360** | **370** | **+10** | |

### 3.2 拥有者配额

| 模组 | 配额 | 实际 | E0 | E1 | E2 | E3 | E4 | E5 | E6 | E7 | E8 | E9 | E10 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 科技核心 `tctechnology` | 25 | 25 | 10 | 6 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| 山河 `tcwildlands` | 14 | 15 |  | 1 | 2 | 1 | 1 | 2 | 1 | 3 | 2 | 1 | 1 |
| 生息 `tcecology` | 14 | 15 | 2 | 2 | 1 | 1 | 1 | 1 | 1 | 1 | 2 | 2 | 1 |
| 田园 `tccultivation` | 33 | 33 | 1 | 5 | 5 | 4 | 4 | 2 | 4 | 3 | 2 | 2 | 1 |
| 工坊 `tcindustry` | 70 | 71 |  | 5 | 5 | 6 | 7 | 6 | 10 | 10 | 8 | 8 | 6 |
| 通途 `tcnetworks` | 36 | 37 | 1 | 3 | 3 | 4 | 4 | 3 | 4 | 4 | 5 | 5 | 1 |
| 聚落 `tcsettlements` | 28 | 30 | 2 | 4 | 3 | 5 | 3 | 3 | 4 | 2 | 2 | 1 | 1 |
| 市集 `tcexchange` | 18 | 19 |  | 1 | 3 | 2 | 3 | 4 | 2 | 1 | 1 | 1 | 1 |
| 求知 `tcdiscovery` | 26 | 26 | 1 | 2 | 3 | 3 | 4 | 5 | 3 | 2 | 1 | 1 | 1 |
| 遗响 `tcechoes` | 6 | 7 |  | 1 | 1 | 1 | 1 | 1 | 1 | 1 |  |  |  |
| 契约 `tccommons` | 18 | 18 | 1 | 2 | 3 | 3 | 2 | 1 | 2 | 1 | 1 | 1 | 1 |
| 远征 `tcfrontiers` | 20 | 20 |  | 1 | 3 | 4 | 3 | 3 | 2 | 1 | 1 | 1 | 1 |
| 异界 `tcbeyond` | 8 | 8 |  |  |  | 1 | 2 | 1 | 1 | 1 | 1 | 1 |  |
| 舆图 `tcatlas` | 12 | 14 | 1 | 1 | 1 | 1 | 1 | 2 | 1 | 1 | 2 | 1 | 2 |
| 列国 `tcnations` | 8 | 8 |  | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |  |  |
| 寰宇 `tcorbit` | 24 | 24 |  |  |  |  |  |  |  |  | 2 | 5 | 17 |

### 3.3 时代关键科技

| 科技 ID | 拥有者 | 状态 | 中文名 |
| --- | --- | --- | --- |
| `tctechnology:stone/fire_making` | tctechnology | 已定义 | 生火 |
| `tctechnology:stone/knapping` | tctechnology | 已定义 | 打制 |
| `tctechnology:stone/foraging_lore` | tctechnology | 已定义 | 采集知识 |
| `tctechnology:neolithic/cultivation` | tccultivation | 已定义 | 农耕 |
| `tctechnology:neolithic/animal_husbandry` | tccultivation | 已定义 | 畜牧 |
| `tctechnology:neolithic/pottery` | tcindustry | 已定义 | 制陶 |
| `tctechnology:neolithic/weaving` | tcindustry | 已定义 | 纺织 |
| `tctechnology:bronze/smelting` | tcindustry | 已定义 | 冶炼 |
| `tctechnology:bronze/alloying` | tcindustry | 已定义 | 合金 |
| `tctechnology:iron/bloomery` | tcindustry | 已定义 | 块炼铁 |
| `tctechnology:medieval/water_power` | tcindustry | 已定义 | 水力 |
| `tctechnology:industrial/steam_engine` | tcindustry | 已定义 | 蒸汽机 |
| `tctechnology:industrial/steelmaking` | tcindustry | 已定义 | 炼钢 |
| `tctechnology:industrial/machine_tools` | tcindustry | 已定义 | 机床 |
| `tctechnology:electric/power_grid` | tcindustry | 已定义 | 电网 |
| `tctechnology:electric/internal_combustion` | tcindustry | 已定义 | 内燃机 |
| `tctechnology:electric/industrial_chemistry` | tcindustry | 已定义 | 工业化学 |
| `tctechnology:atomic/nuclear_fission` | tcindustry | 已定义 | 核裂变 |
| `tctechnology:atomic/transistor` | tcindustry | 已定义 | 晶体管 |
| `tctechnology:information/integrated_circuit` | tcindustry | 已定义 | 集成电路 |
| `tctechnology:information/automation` | tcindustry | 已定义 | 自动化 |
| `tctechnology:bronze/the_wheel` | tcnetworks | 已定义 | 车轮 |
| `tctechnology:iron/road_building` | tcnetworks | 已定义 | 筑路 |
| `tctechnology:industrial/railway` | tcnetworks | 已定义 | 铁路 |
| `tctechnology:electric/telegraphy` | tcnetworks | 已定义 | 电报 |
| `tctechnology:atomic/powered_flight` | tcnetworks | 已定义 | 动力飞行 |
| `tctechnology:information/computer_networks` | tcnetworks | 已定义 | 计算机网络 |
| `tctechnology:stone/shelter_building` | tcsettlements | 已定义 | 窝棚建造 |
| `tctechnology:iron/masonry` | tcsettlements | 已定义 | 石工 |
| `tctechnology:iron/coinage` | tcexchange | 已定义 | 铸币 |
| `tctechnology:medieval/guilds` | tcexchange | 已定义 | 行会 |
| `tctechnology:renaissance/banking` | tcexchange | 已定义 | 银行 |
| `tctechnology:bronze/writing` | tcdiscovery | 已定义 | 文字 |
| `tctechnology:medieval/papermaking` | tcdiscovery | 已定义 | 造纸术 |
| `tctechnology:renaissance/printing_press` | tcdiscovery | 已定义 | 印刷术 |
| `tctechnology:renaissance/scientific_method` | tcdiscovery | 已定义 | 科学方法 |
| `tctechnology:medieval/magnetic_compass` | tcatlas | 已定义 | 磁罗盘 |
| `tctechnology:renaissance/celestial_navigation` | tcatlas | 已定义 | 天文导航 |
| `tctechnology:atomic/radar` | tcatlas | 已定义 | 雷达 |
| `tctechnology:information/rocketry` | tcorbit | 已定义 | 火箭技术 |
| `tctechnology:space/life_support` | tcorbit | 已定义 | 生命维持 |
| `tctechnology:space/orbital_assembly` | tcorbit | 已定义 | 轨道组装 |
| `tctechnology:space/in_situ_resources` | tcorbit | 已定义 | 原位资源利用 |
| `tctechnology:space/interplanetary_transfer` | tcorbit | 已定义 | 行星际转移 |

### 3.4 全部科技（按时代）

#### E0 `stone`（19 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:stone/wayfinding` | 识途 | ATL | 20 | 8 | stone/foraging_lore | 石堆、记忆标记（3 个） |
| `tctechnology:stone/tribal_council` | 部落议事 | COM | 15 | 8 | stone/fire_making | 部落议事形式、zone_sign、禁猎区、hunting_season |
| `tctechnology:stone/smoke_drying` | 烟熏与晾晒 | CUL | 20 | 10 | stone/fire_making | drying_rack；肉干、鱼干、熏制品、干果 |
| `tctechnology:stone/oral_tradition` | 口述传统 | DIS | 20 | 10 | stone/fire_making | 夜谈、长老托付 |
| `tctechnology:stone/game_tracking` | 追踪 | ECO | 20 | 10 | stone/foraging_lore | 痕迹方向箭头、粪便样本、tcecology:ecosystem_health HUD |
| `tctechnology:stone/snare_trapping` | 套索陷阱 | ECO | 25 | 10 | stone/game_tracking、stone/knapping | 套索、陷坑、鱼梁、捕获网 |
| `tctechnology:stone/raft_building` | 木筏制作 | NET | 20 | 10 | stone/knapping | 木筏、路标石堆 |
| `tctechnology:stone/band_organization` | 群居组织 | SET | 15 | 8 | stone/foraging_lore | job_board、自动岗位分配、守火人 |
| `tctechnology:stone/shelter_building` 🔑 | 窝棚建造 | SET | 20 | 10 | stone/knapping | thatch、thatch_slab、hide_cover、sleeping_mat、construction_stake、窝棚识别 |
| `tctechnology:stone/archery` | 弓箭 | TEC | 35 | 20 | stone/spear_hunting、stone/microliths | simple_bow、flint_arrow |
| `tctechnology:stone/basketry` | 编篮 | TEC | 15 | 12 | stone/foraging_lore | gathering_basket、storage_basket |
| `tctechnology:stone/bone_working` | 骨角加工 | TEC | 20 | 15 | stone/knapping | bone_harpoon、antler_billet、pressure_flaker |
| `tctechnology:stone/fire_making` 🔑 | 生火 | TEC | 20 | 12 | — | 关键科技；篝火坑满功能、火塘议事、bow_drill、原版火把 |
| `tctechnology:stone/foraging_lore` 🔑 | 采集知识 | TEC | 15 | 10 | — | 关键科技；tinder、cordage；纤维掉率 +15%；浆果/野生作物额外产出；可食用/有毒标注 |
| `tctechnology:stone/hafting` | 装柄 | TEC | 20 | 15 | stone/knapping、stone/foraging_lore | flint_axe、flint_spear |
| `tctechnology:stone/hearth_council` | 火塘议事 | TEC | 25 | 15 | stone/fire_making | 火塘 RP ×1.5；可用火塘 1 → 2 |
| `tctechnology:stone/knapping` 🔑 | 打制 | TEC | 15 | 10 | — | 关键科技；打制网格；flint_knife、flint_scraper、hand_axe、flint_axe_head、flint_spear_point |
| `tctechnology:stone/microliths` | 细石器 | TEC | 30 | 18 | stone/knapping、stone/bone_working | microlith、flint_arrowhead |
| `tctechnology:stone/spear_hunting` | 投矛狩猎 | TEC | 20 | 15 | stone/hafting | spear_thrower；投矛狩猎知识 |

#### E1 `neolithic`（35 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:neolithic/sketch_mapping` | 绘图 | ATL | 60 | 18 | stone/wayfinding | 兽皮图、炭笔、手绘地图、地图桌、路点旗、挂墙地图；8 个路点；路点导航控件 |
| `tctechnology:neolithic/communal_labor` | 共同劳役 | COM | 50 | 18 | stone/shelter_building、stone/tribal_council | works_marker、donation_box、covenant_chest、corvee、保护林、小型工程 |
| `tctechnology:neolithic/council_of_elders` | 长老会 | COM | 55 | 20 | stone/tribal_council | 长老会形式、council_seat、ballot_box |
| `tctechnology:neolithic/animal_husbandry` 🔑 | 畜牧 | CUL | 60 | 30 | stone/foraging_lore | 围养驯化、feed_trough、manure_heap、herding_crook、hay；原版拴绳与繁殖 |
| `tctechnology:neolithic/cultivation` 🔑 | 农耕 | CUL | 60 | 30 | stone/foraging_lore | 耕地条播、grain_pit、scarecrow、hedgerow、原版堆肥桶；农业页签；第一粮仓里程碑 |
| `tctechnology:neolithic/dairying` | 乳品 | CUL | 70 | 30 | neolithic/animal_husbandry | 挤奶上限可提升、churn、cheese_press、黄油、奶酪、凝乳酶 |
| `tctechnology:neolithic/fermentation` | 发酵 | CUL | 70 | 30 | neolithic/cultivation、neolithic/pottery | 密封容器（vessel）；啤酒、米酒、酸菜、酸奶 |
| `tctechnology:neolithic/seed_selection` | 选种 | CUL | 70 | 30 | neolithic/cultivation | seed_sorting_tray、品种命名 |
| `tctechnology:neolithic/sky_calendar` | 天象历法 | DIS | 70 | 25 | neolithic/tally_marks | calendar_stone、gnomon |
| `tctechnology:neolithic/tally_marks` | 计数刻符 | DIS | 60 | 20 | stone/oral_tradition | tally_stick、elder_seat |
| `tctechnology:neolithic/oral_epics` | 口传史诗 | ECH | 60 | 15 | stone/fire_making | relic_shrine；营火讲述冷却 2 日；象形文字破译 ×1 |
| `tctechnology:neolithic/controlled_burning` | 计划烧除 | ECO | 60 | 20 | stone/fire_making、stone/game_tracking | 计划烧除交互（5.16） |
| `tctechnology:neolithic/game_management` | 猎场轮休 | ECO | 60 | 20 | stone/snare_trapping | 舔盐块；地区“休猎”标记（NPC 不狩猎，W 衰减加快） |
| `tctechnology:neolithic/barter` | 物物交换 | EXC | 50 | 25 | neolithic/pottery | barter_mat、成篮交换、常设交换 |
| `tctechnology:neolithic/watch_and_ward` | 守望与戒备 | FRO | 60 | 15 | stone/knapping | 警戒火堆、号角、尖桩障碍、哨位、防务页签 |
| `tctechnology:neolithic/cold_hammering` | 冷锻铜 | IND | 60 | 15 | stone/knapping | 石砧冷锻：原版铜镐、铜斧、铜锹、铜锄（T1），copper_chisel、copper_sickle |
| `tctechnology:neolithic/pottery` 🔑 | 制陶 | IND | 60 | 15 | stone/fire_making | pottery_jar、large_storage_jar、wooden_chute、unfired_crucible、unfired_clay_mold（烧成 clay_crucible、clay_mold）；原版花盆、饰纹陶罐、陶瓦熔炉配方（§16） |
| `tctechnology:neolithic/tanning` | 鞣革 | IND | 50 | 12 | stone/knapping | tanning_pit；植物鞣皮革（§8.3） |
| `tctechnology:neolithic/updraft_kiln` | 升焰窑 | IND | 70 | 18 | neolithic/pottery | updraft_kiln、clay_lining；原版熔炉的工作台配方（§16） |
| `tctechnology:neolithic/weaving` 🔑 | 纺织 | IND | 60 | 15 | stone/foraging_lore | warp_weighted_loom；linen_cloth、wool_cloth；原版线 → 羊毛配方（§16） |
| `tctechnology:neolithic/gift_exchange` | 赠礼交往 | NAT | 60 | 16 | — | gift_bundle；贸易协定（回退形式）；赠礼好感加成 |
| `tctechnology:neolithic/dugout_canoe` | 独木舟 | NET | 55 | 25 | stone/raft_building | 独木舟 |
| `tctechnology:neolithic/pack_animals` | 驮兽 | NET | 70 | 30 | neolithic/animal_husbandry、neolithic/sledges | 驮鞍、驮队订单 |
| `tctechnology:neolithic/sledges` | 拖橇 | NET | 50 | 25 | stone/raft_building | 拖橇、货袋、货箱 |
| `tctechnology:neolithic/communal_rites` | 公共仪式 | SET | 45 | 12 | stone/band_organization | shrine、节庆指令 |
| `tctechnology:neolithic/herbal_healing` | 草药医术 | SET | 55 | 15 | stone/foraging_lore | herb_drying_rack、healer_mat、herbal_poultice、医者 |
| `tctechnology:neolithic/permanent_dwellings` | 定居房屋 | SET | 60 | 20 | stone/shelter_building | wattle、wattle_and_daub、granary、房屋与长屋类型 |
| `tctechnology:neolithic/well_digging` | 掘井 | SET | 50 | 15 | neolithic/permanent_dwellings | well、latrine |
| `tctechnology:neolithic/composite_tools` | 复合工具 | TEC | 70 | 35 | stone/microliths、stone/hafting | flint_sickle、stone_hoe |
| `tctechnology:neolithic/craft_lineage` | 技艺传承 | TEC | 80 | 40 | stone/hearth_council、stone/shelter_building | NPC 实践知识 ×0.25 → ×0.75；教师岗位产生 TEACHING 知识 |
| `tctechnology:neolithic/fire_keeping` | 保火 | TEC | 50 | 25 | stone/fire_making、stone/foraging_lore | firebrand；篝火坑余烬 1 → 3 MC 日；篝火坑燃料效率 +25% |
| `tctechnology:neolithic/flatbow` | 平弓 | TEC | 50 | 30 | stone/archery | flatbow；Frontiers 缺失时恢复原版弓配方 |
| `tctechnology:neolithic/flint_mining` | 燧石开采 | TEC | 60 | 30 | stone/bone_working、stone/knapping | antler_pick；软岩采掘；燧石结核 |
| `tctechnology:neolithic/ground_stone_tools` | 磨制石器 | TEC | 60 | 30 | stone/hafting | grinding_slab、stone_axe_blank、polished_axe_head、polished_stone_axe、stone_adze |
| `tctechnology:neolithic/seasonal_calendar` | 季节历 | WLD | 60 | 30 | stone/foraging_lore | 季节性规律预报（±2 日，5.13.5）；地理页签显示本地季节相位 |

#### E2 `bronze`（35 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:bronze/landmark_survey` | 地标测记 | ATL | 150 | 40 | neolithic/sketch_mapping、bronze/writing | 路标、界碑、地图碑、陶板地图；32 个路点；地标定位 |
| `tctechnology:bronze/code_of_law` | 法典 | COM | 160 | 45 | bronze/writing、neolithic/council_of_elders | council_table、law_code、law_stele、notice_board、遗产区、纪念碑、多项法律 |
| `tctechnology:bronze/kingship` | 王权 | COM | 150 | 40 | neolithic/council_of_elders | 王权形式 |
| `tctechnology:bronze/land_tenure` | 土地占有制 | COM | 140 | 40 | bronze/writing、neolithic/council_of_elders | 个人地块、land_deed、land_system、fallow_law、grazing_ban |
| `tctechnology:bronze/ard_plough` | 犁耕 | CUL | 150 | 45 | neolithic/cultivation、neolithic/animal_husbandry | yoke、ard、役畜 |
| `tctechnology:bronze/irrigation` | 灌溉 | CUL | 150 | 45 | neolithic/cultivation | irrigation_ditch、sluice_gate、paddy_field；配水 |
| `tctechnology:bronze/orchard_grafting` | 嫁接与果园 | CUL | 150 | 45 | neolithic/cultivation | 葡萄、橄榄、茶插条与苗；葡萄酒；抗性砧木 |
| `tctechnology:bronze/salting` | 腌制 | CUL | 140 | 45 | stone/smoke_drying | smokehouse、咸肉、咸鱼、鱼露、果酱、酱油、味噌 |
| `tctechnology:bronze/sericulture` | 蚕桑 | CUL | 150 | 45 | neolithic/cultivation、neolithic/weaving | mulberry_cutting、silkworm_tray、蚕茧 |
| `tctechnology:bronze/archives` | 档案 | DIS | 150 | 40 | bronze/writing | tablet_rack、dig_site_stake（3 × 3） |
| `tctechnology:bronze/numeracy` | 算术 | DIS | 140 | 35 | neolithic/tally_marks | balance_scale |
| `tctechnology:bronze/writing` 🔑 | 文字 | DIS | 195 | 50 | neolithic/tally_marks | stylus、wet_clay_tablet、clay_tablet、scribe_desk；方法阶段“记录” |
| `tctechnology:bronze/epigraphy` | 碑铭学 | ECH | 150 | 40 | bronze/writing | epigraphy_table；2 级比对；表意文字可破译 |
| `tctechnology:bronze/falconry` | 鹰猎 | ECO | 140 | 40 | neolithic/animal_husbandry、stone/game_tracking | 驯服游隼/金雕为猎鹰，标记并捕获小型猎物 |
| `tctechnology:bronze/caravan_trade` | 商队贸易 | EXC | 180 | 50 | neolithic/barter、neolithic/animal_husbandry | 驮队、商队规划 |
| `tctechnology:bronze/commodity_money` | 商品货币 | EXC | 140 | 45 | neolithic/barter | cowrie_shell、salt_cake、market_stall、merchant 职业 |
| `tctechnology:bronze/weights_and_measures` | 度量衡 | EXC | 160 | 45 | bronze/commodity_money、bronze/smelting | merchant_scale、hacksilver |
| `tctechnology:bronze/bronze_weaponry` | 青铜兵器 | FRO | 150 | 40 | bronze/alloying | 青铜匕首、青铜矛、镰形剑、青铜战斧、青铜盾、青铜盔、亚麻胸甲、青铜胫甲、投石索、标枪 |
| `tctechnology:bronze/composite_bow` | 复合弓 | FRO | 170 | 45 | neolithic/animal_husbandry、bronze/bronze_weaponry | 复合弓、青铜箭、箭靶、柳条盾 |
| `tctechnology:bronze/frontier_outposts` | 边疆前哨 | FRO | 140 | 35 | bronze/the_wheel | 前哨核心、补给箱、野营帐篷、远征桌、告示板、口粮包、水桶囊、草料袋、战地医疗包、远征页签 |
| `tctechnology:bronze/alloying` 🔑 | 合金 | IND | 180 | 45 | bronze/smelting | bronze_ingot、bronze_block；青铜镐、斧、锹、锄（T2）与 bronze_hammer、bronze_tongs、bronze_chisel、bronze_saw、bronze_sickle、bronze_shears；黄铜配比（brass_ingot、brass_block，需 E3 起的 zinc_concentrate） |
| `tctechnology:bronze/charcoal_burning` | 烧炭 | IND | 120 | 30 | neolithic/updraft_kiln | charcoal_clamp、charcoal_kiln；原版熔炉原木 → 木炭配方（§16） |
| `tctechnology:bronze/lost_wax_casting` | 失蜡铸造 | IND | 150 | 38 | bronze/alloying | wax_model；礼器、雕像与原版金工具（仅装饰，§16） |
| `tctechnology:bronze/potters_wheel` | 陶轮 | IND | 130 | 32 | neolithic/updraft_kiln | potters_wheel（拉坯 Q 基数 +10） |
| `tctechnology:bronze/smelting` 🔑 | 冶炼 | IND | 160 | 40 | neolithic/cold_hammering、bronze/charcoal_burning | crucible_furnace、casting_table、forge_hearth、forging_anvil、hand_bellows；tin_ingot、lead_ingot、silver_ingot、arsenical_bronze_ingot、tin_block、lead_block、silver_block；原版熔炉铜、金原矿低收率配方（§16） |
| `tctechnology:bronze/envoys` | 使节制度 | NAT | 150 | 40 | bronze/writing、neolithic/gift_exchange | envoy_seal、treaty_scroll、treaty_lectern、border_stone、tribute_chest、seat_of_power、white_flag；互不侵犯、边界、开放边境、朝贡条约 |
| `tctechnology:bronze/plank_boats` | 木板船 | NET | 140 | 40 | neolithic/dugout_canoe、bronze/smelting | 船体段、河船、木栈桥构件、货桶 |
| `tctechnology:bronze/sailing` | 航海帆 | NET | 170 | 50 | bronze/plank_boats、neolithic/weaving | 帆、桅杆、近岸帆船 |
| `tctechnology:bronze/the_wheel` 🔑 | 车轮 | NET | 160 | 45 | neolithic/sledges、neolithic/pack_animals | 木轮、手推车、牛车、挽具、夯具、夯土路、货栈、装卸台、调度板、车棚、雇工站、路线规划板、运单 |
| `tctechnology:bronze/census` | 人口普查 | SET | 140 | 35 | bronze/writing | census_desk、town_bell、个人记录统计 |
| `tctechnology:bronze/drainage` | 排水 | SET | 130 | 30 | neolithic/well_digging | drain_channel |
| `tctechnology:bronze/mudbrick_architecture` | 泥砖建筑 | SET | 160 | 40 | neolithic/permanent_dwellings、neolithic/pottery | drafting_table、building_plan、泥砖房屋材料等级 |
| `tctechnology:bronze/artifact_study` | 器物考察 | TEC | 150 | 45 | neolithic/craft_lineage、bronze/writing | ARTIFACT 倍率 0.25 → 1.0；逆向研究外来物品 |
| `tctechnology:bronze/prospecting` | 找矿 | WLD | 150 | 45 | stone/knapping | 敲击识别给出矿物类别（5.10.5）；ore_float、gossan 显示指示意义 |
| `tctechnology:bronze/timbered_shafts` | 木支护矿井 | WLD | 170 | 50 | bronze/prospecting | 配方 support/timber_prop（8.3）；地理页签显示矿井 max_span |

#### E3 `iron`（38 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:iron/road_survey` | 道路测量 | ATL | 320 | 90 | bronze/landmark_survey、iron/road_building | 卷轴地图、测绳、十字杆、测距轮、测桩、标杆；导线测量；小地图 r=64 |
| `tctechnology:iron/anomaly_lore` | 异象学 | BEY | 320 | 80 | iron/bloomery | 异象透镜、镇界粉笔、调谐台（研磨） |
| `tctechnology:iron/civic_engineering` | 市政工程 | COM | 300 | 70 | iron/masonry、neolithic/communal_labor | zoning_law、eminent_domain、工业区与住宅区、道路桥梁堤坝水渠城墙模板 |
| `tctechnology:iron/republic` | 共和 | COM | 340 | 75 | bronze/code_of_law | 共和形式、war_vote、open_border |
| `tctechnology:iron/taxation` | 赋税 | COM | 320 | 70 | bronze/land_tenure、bronze/code_of_law | treasury_chest、税吏、税与市场法令 |
| `tctechnology:iron/aquaculture` | 养鱼 | CUL | 300 | 70 | bronze/irrigation、neolithic/animal_husbandry | fish_pond_marker、鲤鱼、塘泥、桑基鱼塘 |
| `tctechnology:iron/crop_rotation` | 轮作 | CUL | 320 | 70 | bronze/ard_plough | 轮作计划、绿肥、compost_bin、agricultural_lime、raised_bed、rat_proof_bin、pasture_seed |
| `tctechnology:iron/selective_breeding` | 选育 | CUL | 320 | 70 | neolithic/seed_selection、neolithic/animal_husbandry | herd_book、EBV、品系命名、兔 |
| `tctechnology:iron/water_lifting` | 提水 | CUL | 320 | 70 | bronze/irrigation | shadoof、noria、chain_pump、diversion_weir、梯田 |
| `tctechnology:iron/geometry` | 几何学 | DIS | 300 | 65 | bronze/numeracy | armillary_sphere、圭表测地 |
| `tctechnology:iron/libraries` | 图书馆 | DIS | 320 | 70 | bronze/archives | bamboo_slips、parchment、scroll、ink、scroll_shelf；学者交流 |
| `tctechnology:iron/natural_philosophy` | 自然哲学 | DIS | 320 | 70 | bronze/writing、neolithic/sky_calendar | 假说板、experiment_bench、water_clock、scholars_table、streak_plate、标本 |
| `tctechnology:iron/historiography` | 史学 | ECH | 320 | 70 | bronze/epigraphy | 传闻误差 ×0.5；stele；编年史装订 |
| `tctechnology:iron/hunting_reserves` | 猎苑与禁伐林 | ECO | 300 | 70 | neolithic/game_management、bronze/writing | 保护界碑、巢箱、种群趋势箭头 |
| `tctechnology:iron/coinage` 🔑 | 铸币 | EXC | 320 | 70 | bronze/weights_and_measures、bronze/alloying | mint、coin_die、bronze_coin、silver_coin、gold_coin、coin_purse、文明货币（关键科技） |
| `tctechnology:iron/marketplace` | 市场 | EXC | 280 | 65 | bronze/commodity_money、bronze/writing | order_board、contract_scroll、credit_chit、订单与合同 |
| `tctechnology:iron/fortification` | 筑城术 | FRO | 340 | 85 | iron/masonry、neolithic/watch_and_ward | 瞭望台平台、警钟、烽燧、箭孔、闸门、绞盘、铁蒺藜、前哨 II 级 |
| `tctechnology:iron/iron_weaponry` | 铁制兵器 | FRO | 320 | 80 | iron/bloomery、bronze/bronze_weaponry | 铁剑、铁矛、铁战斧、圆盾、铁盔、札甲、札甲裙、钉靴、铁箭 |
| `tctechnology:iron/militia_drill` | 民兵操练 | FRO | 300 | 75 | iron/iron_weaponry | 弩、弩矢、训练假人、军械架、军旗、民兵组件 |
| `tctechnology:iron/siegecraft` | 攻城术 | FRO | 360 | 90 | iron/iron_weaponry、bronze/the_wheel | 攻城器械框架、攻城槌、攻城塔、投石机、弩炮、云梯、石弹、修理包 |
| `tctechnology:iron/bloomery` 🔑 | 块炼铁 | IND | 360 | 90 | bronze/smelting、bronze/alloying | bloomery、iron_bloom；原版铁锭、铁镐、铁斧、铁锹、铁锄（T3）、剪刀；iron_hammer、iron_tongs、iron_chisel、iron_saw、iron_file、iron_sickle、iron_plough_share、gold_pan；原版熔炉铁原矿低收率配方（§16） |
| `tctechnology:iron/carburizing` | 渗碳 | IND | 340 | 85 | iron/forge_welding | steel_ingot（泡钢）、steel_nugget、steel_block；钢镐、钢斧、钢锹、钢锄（T4）与 steel_hammer、steel_tongs、steel_chisel、steel_saw、steel_file、steel_sickle、steel_plough_share |
| `tctechnology:iron/forge_welding` | 锻焊 | IND | 300 | 75 | iron/bloomery | wrench、sluice_box；锻焊接长工件 |
| `tctechnology:iron/glassblowing` | 吹制玻璃 | IND | 300 | 75 | bronze/potters_wheel、bronze/smelting | glass_furnace、blowpipe；原版玻璃、玻璃板（吹制）与熔炉沙 → 玻璃配方（§16） |
| `tctechnology:iron/lime_mortar` | 石灰砂浆 | IND | 260 | 65 | bronze/charcoal_burning | lime_kiln、mixing_vat、clay_pipe；quicklime、lime_mortar、pozzolana、roman_concrete |
| `tctechnology:iron/rotary_quern` | 转磨 | IND | 260 | 65 | bronze/potters_wheel | millstone、hand_crank、treadwheel、animal_gin、wooden_shaft、lantern_gear、crown_gear、wooden_gear |
| `tctechnology:iron/military_command` | 军事指挥 | NAT | 320 | 86 | iron/road_building、bronze/writing | war_table、regiment_banner；军队编组与命令；防御同盟 |
| `tctechnology:iron/galleys` | 桨帆船 | NET | 300 | 65 | bronze/sailing | 桨帆船 |
| `tctechnology:iron/harbors` | 港口 | NET | 330 | 70 | iron/galleys、iron/masonry | 码头、港务所、系船柱、船台、港口环境 |
| `tctechnology:iron/relay_posts` | 驿传 | NET | 280 | 60 | iron/road_building、bronze/writing | 驿站、驿传信息延迟（5.12） |
| `tctechnology:iron/road_building` 🔑 | 筑路 | NET | 340 | 70 | bronze/the_wheel | 碎石路、石板路、路缘石、里程碑、路牌、水准仪、养路站、石拱桥识别 |
| `tctechnology:iron/aqueducts` | 引水渠 | SET | 380 | 90 | iron/masonry、bronze/drainage | cistern、aqueduct_channel、public_fountain |
| `tctechnology:iron/masonry` 🔑 | 石工 | SET | 320 | 80 | bronze/mudbrick_architecture | 石匠、砖石房屋材料等级 3、城的建筑要求 |
| `tctechnology:iron/public_baths` | 公共浴场 | SET | 280 | 70 | iron/aqueducts | bath_basin、浴场房间 |
| `tctechnology:iron/schooling` | 学校教育 | SET | 300 | 70 | bronze/census、bronze/writing | school_desk、teachers_lectern、教室房间、教师 |
| `tctechnology:iron/urban_planning` | 城市规划 | SET | 340 | 85 | iron/masonry、iron/road_building | planning_table、zoning_brush、house_number_plate |
| `tctechnology:iron/knowledge_exchange` | 知识互通 | TEC | 320 | 70 | bronze/artifact_study | 外交动作“交换学者”（diplomatic）；后备：扩散日上限 ×2、研究池上限 ×1.5 |
| `tctechnology:iron/hydrology` | 水文 | WLD | 320 | 70 | neolithic/seasonal_calendar、bronze/writing | 水文站曲线：上游降水与预计到达时间（5.14.1）；地理页签“水文”子页显示流量 |

#### E4 `medieval`（38 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:medieval/magnetic_compass` 🔑 | 磁罗盘 | ATL | 650 | 170 | iron/road_survey | 磁石针、磁罗盘、星盘、波特兰海图；罗盘条；小地图 r=128；64 个路点 |
| `tctechnology:medieval/spagyric_arts` | 炼化术 | BEY | 650 | 160 | iron/anomaly_lore | 蒸馏器、灵药、翠华护符 |
| `tctechnology:medieval/warding_rites` | 镇界仪轨 | BEY | 650 | 160 | iron/anomaly_lore、iron/masonry | 镇界石、仪式法阵、调谐台精炼与调谐 |
| `tctechnology:medieval/courts_of_justice` | 法庭 | COM | 600 | 100 | bronze/code_of_law、medieval/papermaking | courthouse_bench、法官、weapon_permit、copyright、patent_law |
| `tctechnology:medieval/town_charter` | 城镇特许状 | COM | 650 | 100 | iron/taxation、medieval/papermaking | town_charter、行会自治、guild_privilege |
| `tctechnology:medieval/heavy_plough` | 重犁 | CUL | 650 | 100 | bronze/ard_plough、iron/bloomery | heavy_plough（可耕黏土与草皮） |
| `tctechnology:medieval/ice_house` | 冰窖 | CUL | 600 | 100 | bronze/salting | ice_chest、冰窖储冰 |
| `tctechnology:medieval/polder_drainage` | 圩田排水 | CUL | 700 | 100 | iron/water_lifting、medieval/water_power | tile_drain、圩田、石膏改良盐碱 |
| `tctechnology:medieval/three_field_rotation` | 三圃制 | CUL | 650 | 100 | iron/crop_rotation | 三圃轮作模板、休耕放牧 |
| `tctechnology:medieval/alchemy` | 炼金术 | DIS | 600 | 100 | iron/natural_philosophy | alembic |
| `tctechnology:medieval/optics` | 光学 | DIS | 650 | 110 | iron/geometry | lens、hand_lens |
| `tctechnology:medieval/papermaking` 🔑 | 造纸术 | DIS | 845 | 140 | iron/libraries | paper_vat、pulp_stamper、paper_mould、scriptorium_desk、manuscript、quill；原版纸 |
| `tctechnology:medieval/universities` | 大学 | DIS | 700 | 120 | iron/natural_philosophy、iron/libraries | academy_lectern；方法阶段“经院” |
| `tctechnology:medieval/antiquarianism` | 好古学 | ECH | 650 | 110 | iron/historiography、iron/masonry | PROTECTED 遗产；古物市场；地区遗迹普查 |
| `tctechnology:medieval/coppicing` | 矮林作业 | ECO | 600 | 120 | iron/hunting_reserves | 萌桩、矮林细杆 |
| `tctechnology:medieval/bills_of_exchange` | 汇票 | EXC | 700 | 100 | iron/coinage、medieval/papermaking | counting_house、bill_of_exchange、跨货币兑换 |
| `tctechnology:medieval/guilds` 🔑 | 行会 | EXC | 650 | 100 | iron/marketplace、iron/coinage | guild_hall、guild_charter、guild_master 职业；Frontiers 缺失时原版弩的后备门槛（会所 8 格内制作）（关键科技） |
| `tctechnology:medieval/trade_fairs` | 集市 | EXC | 600 | 95 | iron/marketplace、iron/road_building | fair_banner |
| `tctechnology:medieval/castle_works` | 城堡工事 | FRO | 700 | 170 | iron/fortification、iron/siegecraft | 配重投石机、吊桥铰链、杀人孔、热油锅、大盾 |
| `tctechnology:medieval/heavy_armour` | 重甲 | FRO | 650 | 160 | iron/iron_weaponry、medieval/water_power | 锁子长衣、锁子护腿、全盔、板甲胸甲、板甲腿甲、铁靴、鸢形盾 |
| `tctechnology:medieval/polearms_and_longbow` | 长柄兵器与长弓 | FRO | 600 | 150 | iron/militia_drill | 长剑、长戟、长枪、钉头锤、战锤、长弓、重弩、锥头箭 |
| `tctechnology:medieval/blast_furnace` | 高炉 | IND | 760 | 190 | iron/bloomery、medieval/water_power | blast_furnace_controller（木炭高炉规格）、pig_iron、cast_iron_gear；zinc_ingot、zinc_block（竖罐炼锌） |
| `tctechnology:medieval/brickmaking` | 制砖 | IND | 560 | 140 | iron/lime_mortar | brick_kiln、chimney；unfired_brick、fire_clay、unfired_fire_brick、fire_bricks；原版砖的熔炉配方（§16） |
| `tctechnology:medieval/fulling_mill` | 漂洗坊 | IND | 560 | 140 | medieval/water_power、neolithic/weaving | fulling_mill、pulley、clutch、leather_belt；broadcloth、silk_cloth |
| `tctechnology:medieval/sawmill` | 水力锯木 | IND | 600 | 150 | medieval/water_power | sawmill（1 原木 → 6 木板） |
| `tctechnology:medieval/trip_hammer` | 水力锤 | IND | 640 | 160 | medieval/water_power、iron/forge_welding | trip_hammer、water_bellows；copper_plate、tin_plate、lead_plate、bronze_plate、brass_plate、brass_rod、iron_plate、iron_rod |
| `tctechnology:medieval/water_power` 🔑 | 水力 | IND | 700 | 175 | iron/rotary_quern、iron/forge_welding | undershot_waterwheel、overshot_waterwheel、sluice_gate、bucket_elevator、stamp_mill；辰砂研磨出原版红石（§8.6）；sail_cloth |
| `tctechnology:medieval/windmill` | 风车 | IND | 620 | 155 | medieval/water_power | post_windmill |
| `tctechnology:medieval/resident_embassies` | 常驻使馆 | NAT | 650 | 175 | bronze/envoys、medieval/papermaking | embassy_desk、使馆院落；研究交流、文化交流、互设使馆 |
| `tctechnology:medieval/canals` | 运河 | NET | 700 | 105 | iron/harbors | 运河驳船、船闸闸门、船闸控制器、纤道 |
| `tctechnology:medieval/cog_ships` | 柯克船 | NET | 650 | 100 | iron/galleys、iron/harbors | 柯克船 |
| `tctechnology:medieval/horse_collar` | 挽马颈圈 | NET | 600 | 95 | iron/road_building | 马车、驿车、辐条轮 |
| `tctechnology:medieval/warehousing` | 仓储 | NET | 620 | 100 | iron/harbors、iron/road_building、medieval/guilds | 仓库核心、货架、踏轮起重机 |
| `tctechnology:medieval/hospitals` | 医院 | SET | 650 | 150 | neolithic/herbal_healing、iron/public_baths | infirmary_bed、医院、医师 |
| `tctechnology:medieval/quarantine` | 检疫隔离 | SET | 550 | 130 | medieval/hospitals | quarantine_flag、rat_trap、隔离指令 |
| `tctechnology:medieval/timber_framing` | 木构架 | SET | 600 | 140 | iron/masonry | timber_frame、多层房屋 |
| `tctechnology:medieval/codified_crafts` | 工艺成法 | TEC | 650 | 100 | iron/knowledge_exchange、medieval/papermaking | 知识上限 ×1.5；超阈值知识折扣成本 ≤ 20% |
| `tctechnology:medieval/drainage_adits` | 排水平硐 | WLD | 620 | 95 | bronze/timbered_shafts、iron/hydrology | 平硐设计提示：平硐口低于工作面时不涌水（5.11.3） |

#### E5 `renaissance`（36 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:renaissance/celestial_navigation` 🔑 | 天文导航 | ATL | 1,200 | 300 | medieval/magnetic_compass | 六分仪、航海图、计程绳、地图集、海图桌、地球仪；经纬网；测深；偏角校正；128 个路点 |
| `tctechnology:renaissance/marine_chronometer` | 航海天文钟 | ATL | 1,500 | 360 | renaissance/celestial_navigation | 航海天文钟；经度测定 |
| `tctechnology:renaissance/threshold_attunement` | 界阈调谐 | BEY | 1,200 | 300 | medieval/warding_rites、medieval/magnetic_compass | 界钥、三种信物、渡界石 |
| `tctechnology:renaissance/parliament` | 议会 | COM | 1,200 | 140 | medieval/town_charter、renaissance/printing_press | 议会形式、academic_freedom、图书馆与大学模板 |
| `tctechnology:renaissance/columbian_exchange` | 哥伦布交换 | CUL | 1,200 | 140 | medieval/three_field_rotation、renaissance/celestial_navigation | acclimatization_garden、跨中心引种、诺福克四圃制、seed_drill、tree_tap 与枫糖 |
| `tctechnology:renaissance/guano` | 海鸟粪 | CUL | 1,100 | 140 | renaissance/columbian_exchange | 海鸟粪矿与 guano |
| `tctechnology:renaissance/curiosity_cabinets` | 珍奇柜 | DIS | 1,000 | 140 | medieval/universities | specimen_cabinet、display_case、mounted_specimen、收藏套组 |
| `tctechnology:renaissance/microscopy` | 显微术 | DIS | 1,100 | 150 | medieval/optics | compound_microscope |
| `tctechnology:renaissance/printing_press` 🔑 | 印刷术 | DIS | 1,560 | 210 | medieval/papermaking | printing_press、type_case、movable_type、printed_book |
| `tctechnology:renaissance/scientific_method` 🔑 | 科学方法 | DIS | 1,560 | 210 | medieval/universities、medieval/alchemy | thermometer、barometer、air_pump；方法阶段“实验” |
| `tctechnology:renaissance/telescope` | 望远镜 | DIS | 1,200 | 160 | medieval/optics | refracting_telescope；原版望远镜配方 |
| `tctechnology:renaissance/philology` | 古文字学 | ECH | 1,200 | 160 | medieval/antiquarianism、renaissance/printing_press | 音节与字母文字超过 M 40；双语碑刻 |
| `tctechnology:renaissance/natural_history` | 博物学 | ECO | 1,200 | 180 | renaissance/scientific_method、stone/game_tracking | 图鉴“精通”等级、种群估计 ±50%、观鸟掩体 |
| `tctechnology:renaissance/banking` 🔑 | 银行 | EXC | 1,200 | 140 | medieval/bills_of_exchange、medieval/guilds | bank_counter、bank_vault、banknote、存贷款；04 学术赞助的前置（关键科技） |
| `tctechnology:renaissance/double_entry_bookkeeping` | 复式记账 | EXC | 1,000 | 130 | medieval/guilds、medieval/papermaking | ledger、NPC 代管账本 |
| `tctechnology:renaissance/joint_stock_company` | 股份公司 | EXC | 1,500 | 150 | renaissance/banking、renaissance/double_entry_bookkeeping | share_certificate、trading_post、公司 |
| `tctechnology:renaissance/marine_insurance` | 海上保险 | EXC | 1,300 | 140 | renaissance/banking、renaissance/celestial_navigation | insurance_policy |
| `tctechnology:renaissance/colonial_expeditions` | 远洋远征 | FRO | 1,100 | 280 | bronze/frontier_outposts、renaissance/celestial_navigation | 跨海远征、船队补给线、海外前哨 |
| `tctechnology:renaissance/firearms` | 火器 | FRO | 1,200 | 300 | medieval/polearms_and_longbow、medieval/heavy_armour | 火绳枪、火绳滑膛枪、燧发手枪、刺剑、火药壶、铅弹、纸壳弹、高冠盔、防弹胸甲、牛皮外套 |
| `tctechnology:renaissance/gunpowder_artillery` | 火炮与棱堡 | FRO | 1,300 | 320 | medieval/castle_works | 射石炮、野战炮、实心炮弹、石笼、火药库、炮位、前哨 III 级 |
| `tctechnology:renaissance/gunpowder` | 火药 | IND | 1,200 | 300 | medieval/water_power、bronze/charcoal_burning | powder_mill、nitre_bed、lead_chamber、acid_carboy；saltpeter、sulfur、sulfuric_acid（铅室法）、nitric_acid（硝石法）；原版火药 |
| `tctechnology:renaissance/mine_drainage` | 矿井排水 | IND | 1,150 | 288 | medieval/windmill、medieval/water_power | pump、tower_windmill |
| `tctechnology:renaissance/optical_glass` | 光学玻璃 | IND | 1,200 | 300 | iron/glassblowing、medieval/brickmaking | optical_glass；炉温数字读数（§5.3） |
| `tctechnology:renaissance/porcelain` | 瓷器 | IND | 1,100 | 275 | medieval/brickmaking、bronze/potters_wheel | dragon_kiln；unfired_porcelain、porcelain |
| `tctechnology:renaissance/precision_brass` | 精密黄铜 | IND | 1,100 | 275 | bronze/alloying、medieval/trip_hammer | brass_gear、solder、maintenance_kit_iron |
| `tctechnology:renaissance/rolling_mill` | 轧制 | IND | 1,250 | 312 | medieval/trip_hammer | rolling_mill、wire_drawing_bench、iron_shaft |
| `tctechnology:renaissance/law_of_nations` | 万国法 | NAT | 1,200 | 324 | medieval/resident_embassies、renaissance/printing_press | 全面同盟；国际仲裁；“无端侵略”信誉惩罚生效 |
| `tctechnology:renaissance/caravel` | 卡拉维尔船 | NET | 1,100 | 135 | medieval/cog_ships、medieval/magnetic_compass | 卡拉维尔船、逆风航行 |
| `tctechnology:renaissance/galleon` | 盖伦船 | NET | 1,400 | 150 | renaissance/caravel、renaissance/celestial_navigation | 盖伦船、远洋定期航线 |
| `tctechnology:renaissance/wagonways` | 木轨道 | NET | 1,000 | 130 | medieval/horse_collar | 木轨、木轨矿车、铺轨器；原版矿车与铁轨配方 |
| `tctechnology:renaissance/anatomy` | 解剖学 | SET | 1,250 | 260 | medieval/hospitals、renaissance/scientific_method | surgery_table、surgical_kit、外科医生 |
| `tctechnology:renaissance/grammar_schools` | 文法学校 | SET | 1,100 | 240 | iron/schooling、renaissance/printing_press | 教育等级 2、强制入学指令 |
| `tctechnology:renaissance/street_lighting` | 街道照明 | SET | 1,000 | 220 | iron/urban_planning | oil_street_lamp |
| `tctechnology:renaissance/research_patronage` | 学术赞助 | TEC | 1,200 | 140 | medieval/codified_crafts、renaissance/banking | 研究地点赞助槽：货币 → 研究池 RP |
| `tctechnology:renaissance/meteorology` | 气象学 | WLD | 1,150 | 135 | iron/hydrology、renaissance/scientific_method | 天气预报 1 日（±25%，5.13.5） |
| `tctechnology:renaissance/stratigraphy` | 地层学 | WLD | 1,200 | 140 | bronze/prospecting、renaissance/scientific_method | 敲击识别给出地层名称与矿床类型；ProspectMethod.MAGNETOMETER 数据精度（12.1） |

#### E6 `industrial`（38 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:industrial/triangulation` | 三角测量 | ATL | 2,200 | 520 | renaissance/marine_chronometer、industrial/machine_tools | 经纬仪、平板仪、三角点、水准仪、水准尺、测链、地形图；等高线与地质图层；小地图 r=192 |
| `tctechnology:industrial/ember_engineering` | 余烬工程 | BEY | 2,200 | 550 | renaissance/threshold_attunement、industrial/steam_engine | 余烬坩埚 |
| `tctechnology:industrial/constitution` | 宪政 | COM | 2,200 | 190 | renaissance/parliament | 宪政形式、宪章权利、income_tax、bond_issue、铁路模板 |
| `tctechnology:industrial/labor_law` | 劳动法 | COM | 2,000 | 180 | industrial/constitution | 劳动与工业安全、排放标准、交通法规、自然保护区、抗震加固 |
| `tctechnology:industrial/canning` | 罐头 | CUL | 2,000 | 190 | medieval/ice_house、industrial/steelmaking | Industry canning_machine 的食物配方 |
| `tctechnology:industrial/mechanical_reaper` | 机械收割 | CUL | 2,200 | 190 | medieval/heavy_plough、industrial/machine_tools | reaper |
| `tctechnology:industrial/pedigree_breeding` | 系谱育种 | CUL | 2,200 | 190 | iron/selective_breeding、renaissance/scientific_method | 完整系谱、选择指数、杂交制种田、数值性状 |
| `tctechnology:industrial/soil_chemistry` | 土壤化学 | CUL | 2,200 | 190 | renaissance/guano、renaissance/scientific_method | soil_test_kit、bordeaux_mixture、土壤数值显示、工业磷钾肥 |
| `tctechnology:industrial/analytical_chemistry` | 分析化学 | DIS | 2,100 | 220 | renaissance/scientific_method、medieval/alchemy | lab_bench、fume_hood、analytical_balance |
| `tctechnology:industrial/photography` | 摄影术 | DIS | 2,000 | 210 | medieval/optics、industrial/analytical_chemistry | camera、photographic_plate |
| `tctechnology:industrial/scientific_journals` | 学术期刊 | DIS | 2,200 | 230 | renaissance/printing_press、renaissance/scientific_method | journal_issue、engineering_college_desk、学术会议；方法阶段“学科” |
| `tctechnology:industrial/archaeology` | 考古学 | ECH | 2,200 | 230 | renaissance/philology、renaissance/scientific_method | 17 的 9 × 9 发掘网格；古工业遗址修复；遗址普查 |
| `tctechnology:industrial/scientific_forestry` | 科学林业 | ECO | 2,100 | 260 | medieval/coppicing、renaissance/natural_history | 林分标桩、苗圃、鱼道、AAC |
| `tctechnology:industrial/central_banking` | 中央银行 | EXC | 2,200 | 190 | renaissance/banking、renaissance/double_entry_bookkeeping | 准备金率、最后贷款人、利率调整 |
| `tctechnology:industrial/stock_exchange` | 证券交易所 | EXC | 2,400 | 190 | renaissance/joint_stock_company、renaissance/printing_press | exchange_board、stock_ticker、bond |
| `tctechnology:industrial/field_fortification` | 野战工事 | FRO | 2,000 | 500 | renaissance/gunpowder_artillery、industrial/steelmaking | 沙袋、铁丝网、土木胸墙、后膛野战炮、炮弹、射表 |
| `tctechnology:industrial/rifled_arms` | 线膛枪械 | FRO | 2,200 | 550 | renaissance/firearms、industrial/machine_tools | 线膛枪、后装步枪、左轮手枪、刺刀、骑兵马刀、金属弹壳弹 |
| `tctechnology:industrial/coal_gas` | 煤气 | IND | 1,800 | 450 | industrial/coke_ironmaking | gas_producer、retort；coal_gas、coal_tar、wood_tar、pyroligneous_acid、ammonia_liquor；soda_ash、hydrochloric_acid（路布兰法）、superphosphate、ammonium_sulfate；ethanol |
| `tctechnology:industrial/coke_ironmaking` | 焦炭炼铁 | IND | 2,200 | 550 | medieval/blast_furnace | beehive_coke_oven、coke_oven_battery、hot_blast_stove、blowing_engine、coke；焦炭高炉规格（§6.1） |
| `tctechnology:industrial/factory_logistics` | 工厂物流 | IND | 1,800 | 450 | industrial/line_shafting | conveyor_belt、overhead_crane、canning_machine、cyclone_separator；tinplate、tin_can、canned_food |
| `tctechnology:industrial/line_shafting` | 天轴传动 | IND | 1,800 | 450 | industrial/steam_engine | steel_shaft、gearbox、flywheel、governor、slip_coupling |
| `tctechnology:industrial/machine_tools` 🔑 | 机床 | IND | 2,600 | 650 | industrial/steam_engine、renaissance/precision_brass、renaissance/rolling_mill | lathe、planer、milling_machine、drill_press、air_compressor、pneumatic_drill、air_tank、tank、compound_engine、lancashire_boiler、steel_gear；tools.diamond_tools_loot_only = false 时的钻石工具锻造配方（§16） |
| `tctechnology:industrial/mechanized_textiles` | 机械纺织 | IND | 2,000 | 500 | industrial/line_shafting、medieval/fulling_mill | spinning_mule、power_loom、punched_card；cotton_cloth |
| `tctechnology:industrial/portland_cement` | 波特兰水泥 | IND | 1,900 | 475 | medieval/brickmaking、industrial/coke_ironmaking | rotary_kiln、concrete_mixer；clinker、portland_cement、reinforced_concrete |
| `tctechnology:industrial/puddling` | 搅炼 | IND | 2,000 | 500 | industrial/coke_ironmaking | puddling_furnace（生铁 → 熟铁） |
| `tctechnology:industrial/steam_engine` 🔑 | 蒸汽机 | IND | 2,600 | 650 | renaissance/mine_drainage、renaissance/precision_brass、medieval/blast_furnace | haystack_boiler、cornish_boiler、newcomen_engine、watt_engine、condenser、cooling_tower、mechanical_stoker、iron_pipe、piston_assembly |
| `tctechnology:industrial/steelmaking` 🔑 | 炼钢 | IND | 2,800 | 700 | industrial/puddling、iron/carburizing | bessemer_converter、open_hearth_furnace、ladle、silica_bricks、steel_pipe、steel_plate、steel_rod；原版高炉的工作台配方（§16） |
| `tctechnology:industrial/general_staff` | 总参谋部 | NAT | 2,200 | 594 | iron/military_command、industrial/railway | 动员 1 MC 日；铁路调兵；资源战争理由 |
| `tctechnology:industrial/civil_engineering` | 土木工程 | NET | 2,100 | 190 | iron/road_building、industrial/steelmaking | 碎石沥青路、桁架桥段、隧道内衬、隧道洞门、维修工具包、包铁车轮、坩埚钢回退 |
| `tctechnology:industrial/railway` 🔑 | 铁路 | NET | 2,400 | 200 | renaissance/wagonways、industrial/steam_engine | 铁轨、蒸汽机车、棚车、漏斗车、罐车、客车、轮对、转向架、蒸汽动力总成、火车站、货运站、机车库、水塔、煤台、道岔扳道器、车挡、站台边缘、车票 |
| `tctechnology:industrial/railway_signalling` | 铁路信号 | NET | 2,000 | 185 | industrial/railway | 臂板信号、信号楼、道口、闭塞分区与时刻表 |
| `tctechnology:industrial/steamships` | 蒸汽船 | NET | 2,200 | 190 | renaissance/galleon、industrial/steam_engine | 蒸汽船、干船坞、蒸汽起重机 |
| `tctechnology:industrial/public_education` | 公共教育 | SET | 2,100 | 440 | renaissance/grammar_schools | classroom_blackboard、公立学校 |
| `tctechnology:industrial/sanitary_reform` | 卫生改革 | SET | 2,400 | 500 | iron/aqueducts、medieval/quarantine | sewer_pipe、sewage_outlet、standpipe |
| `tctechnology:industrial/tenement_housing` | 公寓楼 | SET | 2,200 | 450 | medieval/timber_framing、industrial/steam_engine | 公寓建筑类型 |
| `tctechnology:industrial/vaccination` | 疫苗接种 | SET | 2,000 | 420 | renaissance/anatomy | vaccination_station、vaccine_vial、护士、疫苗运动指令 |
| `tctechnology:industrial/patent_office` | 专利局 | TEC | 2,200 | 190 | renaissance/research_patronage、industrial/machine_tools | 每完成 1 项科技研究池 +5% 成本；扩散给 AI −50% |
| `tctechnology:industrial/economic_geology` | 经济地质 | WLD | 2,200 | 190 | renaissance/stratigraphy、industrial/steam_engine | 矿化提示给出八方位估计；GRAVIMETER、CORE_SAMPLE 精度；配方 support/steel_mine_arch |

#### E7 `electric`（33 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:electric/telegraphic_reporting` | 电报测报 | ATL | 3,800 | 880 | industrial/triangulation、electric/telegraphy | 报告站、无线电信标、无线电测向仪、态势板、野战电线；实时数据与天气图层；256 个路点 |
| `tctechnology:electric/resonance_theory` | 共鸣理论 | BEY | 3,800 | 950 | industrial/ember_engineering、electric/power_grid | 共鸣中继、共鸣门 |
| `tctechnology:electric/universal_suffrage` | 普选 | COM | 3,800 | 250 | industrial/constitution | 普选民主、联邦、polling_booth、禁飞区 |
| `tctechnology:electric/refrigeration` | 制冷 | CUL | 3,800 | 250 | industrial/canning、electric/power_grid | refrigerator、deep_freezer、冷链保鲜等级 |
| `tctechnology:electric/synthetic_fertilizer` | 合成氮肥 | CUL | 3,600 | 250 | industrial/soil_chemistry、electric/industrial_chemistry | 工业氮肥施用（c:fertilizers/nitrogen） |
| `tctechnology:electric/tractor_farming` | 拖拉机农业 | CUL | 3,800 | 250 | industrial/mechanical_reaper、electric/internal_combustion | combine、机械牵引 |
| `tctechnology:electric/research_institutes` | 研究所 | DIS | 3,800 | 320 | industrial/scientific_journals | research_director_desk、technical_report、oscilloscope；方法阶段“体制” |
| `tctechnology:electric/spectroscopy` | 光谱学 | DIS | 3,600 | 300 | industrial/photography、industrial/analytical_chemistry | spectroscope |
| `tctechnology:electric/heritage_conservation` | 遗产保护 | ECH | 3,800 | 320 | industrial/archaeology、electric/industrial_chemistry | conservation_bench |
| `tctechnology:electric/wildlife_conservation` | 野生动物保护 | ECO | 3,600 | 360 | industrial/scientific_forestry、renaissance/natural_history | 自然保护区规则、季度普查 ±25%、圈养繁育栏、动物通道 |
| `tctechnology:electric/wire_transfer` | 电汇 | EXC | 3,600 | 250 | industrial/central_banking、electric/telegraphy | wire_counter、cheque_book |
| `tctechnology:electric/machine_guns` | 机枪与钢盔 | FRO | 3,800 | 950 | industrial/rifled_arms、industrial/field_fortification | 栓动步枪、轻机枪、手榴弹、钢盔、迫击炮、迫击炮弹、碉堡射孔、探照灯、机枪巢、前哨 IV 级 |
| `tctechnology:electric/assembly_line` | 流水线 | IND | 3,400 | 850 | industrial/factory_logistics、electric/dynamo | sorter、relay_panel、control_cable、signal_converter、thermostat、pressure_switch、float_switch、helical_gear、precast_panel、maintenance_kit_steel |
| `tctechnology:electric/dynamo` | 发电机 | IND | 3,600 | 900 | industrial/steam_engine、industrial/machine_tools | dc_dynamo、electric_motor_small、electric_motor_medium、electric_motor_large、copper_wire、incandescent_lamp |
| `tctechnology:electric/electric_arc_furnace` | 电弧炉 | IND | 3,600 | 900 | electric/power_grid、industrial/steelmaking | electric_arc_furnace、graphite_electrode、magnesia_bricks、graphite_block、alumina_bricks；alloy_steel_ingot、alloy_steel_plate、alloy_steel_rod、stainless_steel_ingot、stainless_steel_plate、stainless_steel_block、nickel_ingot、chromium_ingot；合金钢镐、斧、锹、锄（T5） |
| `tctechnology:electric/electrochemistry` | 电化学 | IND | 3,400 | 850 | electric/dynamo | chlor_alkali_cell、hall_heroult_cell、lead_acid_battery、tailings_dam；lead_acid_cell、alumina、cryolite、aluminium_ingot、aluminium_plate、aluminium_block、magnesium_ingot、rubber、rubber_belt；chlorine、caustic_soda、hydrogen |
| `tctechnology:electric/hydroelectricity` | 水力发电 | IND | 3,600 | 900 | electric/power_grid、industrial/portland_cement | hydro_turbine、geothermal_well |
| `tctechnology:electric/industrial_chemistry` 🔑 | 工业化学 | IND | 4,400 | 1,100 | electric/electrochemistry、industrial/coal_gas | chemical_reactor、haber_reactor、air_separation_unit、flotation_cell、wastewater_plant、refrigeration_unit、gas_cylinder；接触法 sulfuric_acid、索尔维法 soda_ash、ammonia、nitric_acid（奥斯特瓦尔德法）、ammonium_nitrate、phosphoric_acid、hydrofluoric_acid、gypsum、oxygen、nitrogen、argon、liquid_oxygen；铬鞣（§8.3） |
| `tctechnology:electric/internal_combustion` 🔑 | 内燃机 | IND | 4,200 | 1,050 | electric/oil_refining、industrial/machine_tools | gas_engine、gasoline_engine、diesel_engine、stationary_engine、air_duct、chainsaw |
| `tctechnology:electric/oil_refining` | 炼油 | IND | 3,800 | 950 | industrial/coal_gas、industrial/steelmaking | distillation_tower、drilling_rig、pumpjack、wellhead、fluid_loading_arm；crude_oil、natural_gas、fuel_oil、diesel、gasoline、kerosene、naphtha、lubricating_oil、biodiesel、bitumen |
| `tctechnology:electric/power_grid` 🔑 | 电网 | IND | 4,500 | 1,125 | electric/dynamo | copper_cable、mv_line、hv_line、utility_pole、transmission_tower、pole_transformer、distribution_transformer、substation、step_up_station、circuit_breaker、power_meter、fuse、turbine_generator、water_tube_boiler、diesel_genset、electrostatic_precipitator |
| `tctechnology:electric/vacuum_tube` | 电子管 | IND | 3,800 | 950 | electric/dynamo、renaissance/optical_glass | vacuum_tube、circuit_board、tungsten_wire、tungsten_ingot |
| `tctechnology:electric/collective_security` | 集体安全 | NAT | 3,800 | 1,026 | renaissance/law_of_nations、electric/telegraphy | 集体安全公约；电报即时外交 |
| `tctechnology:electric/electric_traction` | 电力牵引 | NET | 4,000 | 260 | industrial/railway、electric/power_grid | 钢轨、色灯信号、接触网支柱、转辙机、牵引变电所、牵引电机、电力机车、有轨电车 |
| `tctechnology:electric/motor_vehicles` | 汽车 | NET | 3,900 | 255 | industrial/civil_engineering、electric/internal_combustion | 汽车、卡车、公共汽车、充气轮胎、内燃动力总成、沥青路、道路标线、红绿灯、公交站、卡车站、加油站、燃料罐、托盘、托盘货架、配送中心 |
| `tctechnology:electric/telegraphy` 🔑 | 电报 | NET | 3,600 | 240 | industrial/railway_signalling、iron/relay_posts | 电报线杆、电报局、电报键、电报纸条 |
| `tctechnology:electric/telephone` | 电话 | NET | 3,800 | 250 | electric/telegraphy | 电话交换机、电话机 |
| `tctechnology:electric/home_electrification` | 入户电气化 | SET | 3,600 | 720 | industrial/tenement_housing、electric/power_grid | electric_street_lamp、公用事业需求 |
| `tctechnology:electric/water_treatment` | 水处理 | SET | 3,800 | 760 | industrial/sanitary_reform、electric/industrial_chemistry | water_treatment_controller |
| `tctechnology:electric/corporate_rnd` | 企业研发 | TEC | 3,800 | 250 | industrial/patent_office、electric/power_grid | 生产线产值 1% 转为研究池 RP |
| `tctechnology:electric/petroleum_geology` | 石油地质 | WLD | 4,000 | 260 | industrial/economic_geology、electric/internal_combustion | SEISMIC 精度；地理页签显示储层圈闭 |
| `tctechnology:electric/seismology` | 地震学 | WLD | 3,600 | 240 | industrial/economic_geology、electric/telegraphy | 地震仪给出“N 日内 M ≥ 6 概率”（5.14.2） |
| `tctechnology:electric/synoptic_meteorology` | 天气图 | WLD | 3,800 | 250 | renaissance/meteorology、electric/telegraphy | 天气预报 2 日（±15%）；气旋路径预报 |

#### E8 `atomic`（32 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:atomic/aerial_photography` | 航空摄影 | ATL | 6,000 | 1,350 | industrial/triangulation、atomic/powered_flight | 航空相机、胶卷、显影罐、立体测图仪；SL5 |
| `tctechnology:atomic/radar` 🔑 | 雷达 | ATL | 6,500 | 1,450 | electric/telegraphic_reporting、electric/power_grid | 雷达天线、雷达站、雷达操作台；雷达图层；小地图 r=256 |
| `tctechnology:atomic/field_stabilization` | 场稳定 | BEY | 6,000 | 1,500 | electric/resonance_theory、atomic/nuclear_fission | 场稳定器、异象传感器、电弧炉与离心机配方 |
| `tctechnology:atomic/welfare_state` | 福利国家 | COM | 6,000 | 320 | electric/universal_suffrage | welfare、计划国家、planned_economy、gmo_policy |
| `tctechnology:atomic/agrochemicals` | 农药 | CUL | 5,600 | 320 | industrial/soil_chemistry、electric/industrial_chemistry | pesticide、herbicide、土壤熏蒸 |
| `tctechnology:atomic/green_revolution` | 绿色革命 | CUL | 6,000 | 320 | electric/synthetic_fertilizer、industrial/pedigree_breeding | 矮秆位点、npk_fertilizer、tube_well、sprinkler、seed_vault |
| `tctechnology:atomic/national_laboratories` | 国家实验室 | DIS | 6,000 | 430 | electric/research_institutes、electric/spectroscopy | national_lab_control、electron_microscope、cloud_chamber、radio_telescope_dish、microfilm_reel；方法阶段“大科学” |
| `tctechnology:atomic/ecotoxicology` | 生态毒理学 | ECO | 6,000 | 480 | renaissance/natural_history、electric/industrial_chemistry | 活检工具、组织样本、毒性分量显示 |
| `tctechnology:atomic/radio_telemetry` | 无线电遥测 | ECO | 5,800 | 480 | electric/wildlife_conservation、atomic/transistor | 无线电项圈、遥测接收站、红外相机、±10% |
| `tctechnology:atomic/international_monetary_system` | 国际货币体系 | EXC | 6,000 | 320 | electric/wire_transfer、industrial/stock_exchange | forex_board、payment_card、储备货币协定 |
| `tctechnology:atomic/air_defense` | 防空与现代步兵 | FRO | 6,000 | 1,500 | electric/machine_guns、atomic/radar | 突击步枪、冲锋枪、中间威力弹、防破片背心、作战靴、防空警报、防爆门、防空炮、空投补给 |
| `tctechnology:atomic/gas_turbine` | 燃气轮机 | IND | 5,600 | 1,400 | electric/internal_combustion、electric/electric_arc_furnace | gas_turbine、turbine_core |
| `tctechnology:atomic/nuclear_fission` 🔑 | 核裂变 | IND | 7,500 | 1,875 | atomic/uranium_enrichment | research_pile、graphite_water_reactor、pwr_reactor、steam_generator、control_rod_drive、eccs_pump、containment_dome、hydrogen_recombiner、spent_fuel_pool、dry_cask、decontamination_station、remote_manipulator、lead_cask；natural_uranium_rod、spent_fuel_assembly、vitrified_waste |
| `tctechnology:atomic/petrochemicals` | 石油化工 | IND | 5,500 | 1,375 | electric/oil_refining、electric/industrial_chemistry | catalytic_cracker、basic_oxygen_furnace、fgd_scrubber；ethylene、synthetic_rubber、liquid_hydrogen、liquefied_natural_gas、hypergolic_propellant |
| `tctechnology:atomic/plastics` | 塑料 | IND | 5,200 | 1,300 | atomic/petrochemicals | polyethylene、nylon_thread |
| `tctechnology:atomic/semiconductor_materials` | 半导体材料 | IND | 5,500 | 1,375 | electric/electrochemistry、electric/vacuum_tube | crystal_puller、float_glass_line、fluorescent_lamp；polysilicon、silicon_boule、silicon_wafer |
| `tctechnology:atomic/titanium_metallurgy` | 钛冶金 | IND | 5,200 | 1,300 | electric/electrochemistry、electric/electric_arc_furnace | flywheel_storage；titanium_ingot、titanium_alloy_ingot、titanium_plate、titanium_alloy_plate、al_li_alloy_ingot、al_li_alloy_plate、molybdenum_ingot、aluminium_wire |
| `tctechnology:atomic/transistor` 🔑 | 晶体管 | IND | 7,000 | 1,750 | atomic/semiconductor_materials | transistor、pid_controller |
| `tctechnology:atomic/uranium_enrichment` | 铀浓缩 | IND | 6,500 | 1,625 | electric/industrial_chemistry、electric/power_grid | uranium_mill、conversion_plant、gas_centrifuge、fuel_fabricator、stainless_pipe；yellowcake、uranium_tailings、uranium_hexafluoride、depleted_uranium、uo2_pellet、zircaloy_tube、zirconium_ingot、fuel_rod、fuel_assembly |
| `tctechnology:atomic/multilateral_institutions` | 多边机构 | NAT | 6,000 | 1,620 | electric/collective_security | 多边会议（同时与多个文明谈判）；太空合作条约（需 Orbit） |
| `tctechnology:atomic/centralized_traffic_control` | 调度集中 | NET | 6,200 | 330 | industrial/railway_signalling、electric/telephone | CTC 控制台、混凝土轨枕钢轨 |
| `tctechnology:atomic/highways` | 高速公路 | NET | 5,500 | 310 | electric/motor_vehicles、industrial/civil_engineering | 混凝土路、护栏、悬索桥主缆、半挂车、动物通道 |
| `tctechnology:atomic/oil_tankers` | 大型远洋船 | NET | 5,800 | 320 | industrial/steamships、electric/internal_combustion | 油轮、散货船、内燃机车、无线电台 |
| `tctechnology:atomic/pipelines` | 长输管道 | NET | 5,000 | 300 | industrial/civil_engineering、electric/industrial_chemistry | 管道、管道泵站、管道终端 |
| `tctechnology:atomic/powered_flight` 🔑 | 动力飞行 | NET | 6,500 | 330 | electric/motor_vehicles、electric/internal_combustion | 航空发动机、螺旋桨运输机、螺旋桨客机、机场核心、塔台、跑道灯、机库 |
| `tctechnology:atomic/inertial_guidance` | 惯性制导 | ORB | 6,500 | 350 | atomic/liquid_fuel_rockets、atomic/transistor | 航电单元 |
| `tctechnology:atomic/liquid_fuel_rockets` | 液体火箭 | ORB | 6,000 | 300 | atomic/powered_flight、electric/industrial_chemistry | 探空架、固体发动机、早期液体发动机、贮箱、分离器、头锥 |
| `tctechnology:atomic/antibiotics` | 抗生素 | SET | 6,000 | 1,200 | industrial/vaccination、electric/industrial_chemistry | clinic_cabinet、antibiotic_dose、医生 |
| `tctechnology:atomic/high_rise_housing` | 高层住宅 | SET | 6,500 | 1,300 | industrial/tenement_housing、industrial/steelmaking | 高层住宅建筑类型 |
| `tctechnology:atomic/big_science` | 大科学 | TEC | 6,000 | 320 | electric/corporate_rnd、atomic/nuclear_fission | 联合攻关：多地点同研一项，第 2 个起 60% |
| `tctechnology:atomic/geophysical_survey` | 地球物理勘探 | WLD | 6,000 | 320 | electric/seismology、atomic/powered_flight | AIRBORNE 精度；配方 support/rock_bolts；地震剖面显示 deep_conduit（5.16.1） |
| `tctechnology:atomic/volcanology` | 火山学 | WLD | 5,800 | 310 | electric/seismology | 火山监测给出 VEI 区间与喷发时间窗（5.14.3） |

#### E9 `information`（31 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:information/gis` | 地理信息系统 | ATL | 9,000 | 2,000 | atomic/aerial_photography、information/integrated_circuit | GIS 终端、全站仪；全部图层与分析；小地图 r=384；512 个路点 |
| `tctechnology:information/anomalous_synthesis` | 异质合成 | BEY | 9,000 | 2,200 | atomic/field_stabilization、information/integrated_circuit | 晶体合成器、合成晶体 |
| `tctechnology:information/digital_governance` | 数字治理 | COM | 9,000 | 400 | information/computer_networks、atomic/welfare_state | 数字民主、civic_terminal、organochlorine_ban |
| `tctechnology:information/genetic_engineering` | 基因工程 | CUL | 9,000 | 400 | atomic/green_revolution、atomic/transistor | gene_editing_station、转基因品种 |
| `tctechnology:information/precision_agriculture` | 精准农业 | CUL | 9,000 | 400 | atomic/agrochemicals、information/integrated_circuit | drip_line、field_sensor、综合防治、变量施肥 |
| `tctechnology:information/computational_science` | 计算科学 | DIS | 9,000 | 560 | atomic/national_laboratories、atomic/transistor | mainframe_terminal、mainframe_rack、data_tape；方法阶段“计算” |
| `tctechnology:information/ecosystem_restoration` | 生态修复 | ECO | 9,000 | 600 | electric/wildlife_conservation、atomic/ecotoxicology | 人工湿地、乡土种子混合物、毒性衰减加速 |
| `tctechnology:information/genetic_rescue` | 基因救援 | ECO | 9,500 | 600 | atomic/radio_telemetry、information/ecosystem_restoration | 基因库、液氮瓶、灭绝物种复原 |
| `tctechnology:information/electronic_payment` | 电子支付 | EXC | 9,000 | 400 | atomic/international_monetary_system、information/computer_networks | payment_terminal、即时清算 |
| `tctechnology:information/composite_armour` | 复合装甲与快速反应 | FRO | 9,000 | 2,200 | atomic/air_defense、information/computer_networks | 卡宾枪、芳纶头盔、插板防弹衣、夜视仪、快速反应调度台 |
| `tctechnology:information/automation` 🔑 | 自动化 | IND | 10,000 | 2,500 | information/integrated_circuit、electric/assembly_line | robot_arm、plc、control_room；engineering_tablet、maintenance_kit_precision |
| `tctechnology:information/cnc_machining` | 数控加工 | IND | 8,500 | 2,125 | information/automation、industrial/machine_tools | cnc_mill；tolerance_class 为精密级的机加工配方（§8.1） |
| `tctechnology:information/composites` | 复合材料 | IND | 8,000 | 2,000 | atomic/plastics | incinerator；fiberglass、heat_shield_tile |
| `tctechnology:information/integrated_circuit` 🔑 | 集成电路 | IND | 10,500 | 2,625 | atomic/transistor | lithography_stepper、cleanroom_panel、led_lamp；integrated_circuit |
| `tctechnology:information/lithium_battery` | 锂电池 | IND | 8,500 | 2,125 | electric/electrochemistry、atomic/plastics | lithium_battery_bank、fuel_cell；lithium_ingot、cobalt_ingot、lithium_cell、power_drill、angle_grinder；evaporation_pond 卤水提锂模式 |
| `tctechnology:information/photovoltaics` | 光伏 | IND | 8,500 | 2,125 | atomic/transistor | solar_panel、inverter；solar_cell |
| `tctechnology:information/smart_grid` | 智能电网 | IND | 9,500 | 2,375 | information/integrated_circuit、electric/power_grid | ehv_line、hvdc_line、converter_station、heat_recovery_boiler、scr_unit |
| `tctechnology:information/wind_turbine` | 风力发电 | IND | 8,000 | 2,000 | information/composites | wind_turbine；wind_turbine_blade |
| `tctechnology:information/automated_logistics` | 自动化物流 | NET | 9,800 | 420 | information/containerization、information/computer_networks、information/automation | 立体仓库、自动导引车、货运无人机 |
| `tctechnology:information/computer_networks` 🔑 | 计算机网络 | NET | 9,000 | 400 | electric/telephone、atomic/transistor | 交换机柜、光纤、路由器、调度终端、自动调度 |
| `tctechnology:information/containerization` | 集装箱化 | NET | 8,500 | 390 | medieval/warehousing、atomic/oil_tankers | 集装箱、集装箱船、集装箱平车、集装箱码头、龙门吊 |
| `tctechnology:information/high_speed_rail` | 高速铁路 | NET | 10,000 | 420 | electric/electric_traction、atomic/centralized_traffic_control | 无砟轨道、应答器、高速动车组 |
| `tctechnology:information/jet_airliners` | 喷气客机 | NET | 9,500 | 410 | atomic/powered_flight | 涡扇发动机、喷气客机、货运喷气机、直升机 |
| `tctechnology:information/communications_satellites` | 通信卫星 | ORB | 9,000 | 1,000 | information/rocketry、information/computer_networks | 通信载荷 |
| `tctechnology:information/cryogenic_propellants` | 低温推进剂 | ORB | 9,500 | 1,000 | information/rocketry、electric/industrial_chemistry | 低温贮箱、氢氧发动机、中型发射台 |
| `tctechnology:information/reentry_systems` | 再入与回收 | ORB | 8,500 | 900 | information/rocketry | 防热罩、降落伞包 |
| `tctechnology:information/remote_sensing` | 遥感 | ORB | 9,000 | 1,000 | information/rocketry、atomic/radar | 对地观测、科学、太阳监测载荷；遥感地区层 |
| `tctechnology:information/rocketry` 🔑 | 火箭技术 | ORB | 9,000 | 1,200 | atomic/inertial_guidance、information/integrated_circuit | 总装厂房、小型发射台、履带运输车、任务控制台、跟踪天线、煤油与自燃发动机、RCS、整流罩、卫星平台、太阳翼、信标载荷；近地轨道维度 |
| `tctechnology:information/epidemic_surveillance` | 疫情监测 | SET | 9,000 | 1,800 | atomic/antibiotics、information/computer_networks | health_monitor_terminal |
| `tctechnology:information/collaborative_research` | 协作研究 | TEC | 9,000 | 400 | atomic/big_science、information/computer_networks | 每个盟友/研究聚落研究产出 +5%，上限 +25% |
| `tctechnology:information/climate_modeling` | 气候模型 | WLD | 9,000 | 400 | electric/synoptic_meteorology、information/integrated_circuit | 天气预报 5 日（±8%）；SATELLITE 精度；地理页签显示气候变化预测（5.17） |

#### E10 `space`（35 项）

| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |
| --- | --- | --- | ---: | ---: | --- | --- |
| `tctechnology:space/satellite_imagery` | 卫星遥感 | ATL | 13,000 | 2,800 | information/gis、information/rocketry | 地面站、成像载荷；卫星影像；小地图 r=512 |
| `tctechnology:space/satellite_navigation` | 卫星导航 | ATL | 14,000 | 3,000 | space/satellite_imagery、atomic/radar | 导航载荷、GPS 接收机；坐标控件；路点不限 |
| `tctechnology:space/interstellar_charter` | 星际宪章 | COM | 13,000 | 500 | information/digital_governance | colony_charter、nuclear_thermal_ban、月面领地 |
| `tctechnology:space/controlled_environment_agriculture` | 受控环境农业 | CUL | 13,000 | 500 | information/precision_agriculture、space/life_support | grow_rack、nutrient_tank、soil_inoculant、外星土壤改良 |
| `tctechnology:space/space_science` | 空间科学 | DIS | 13,000 | 700 | information/computational_science、information/rocketry | space_research_console、space_telescope；方法阶段“空间” |
| `tctechnology:space/biosphere_engineering` | 生物圈工程 | ECO | 13,000 | 750 | information/ecosystem_restoration、space/life_support | 生物圈模板（Orbit）、生态缸 |
| `tctechnology:space/interplanetary_settlement` | 多星球结算 | EXC | 13,000 | 500 | information/electronic_payment、space/interplanetary_transfer | clearing_house、跨天体结算批次 |
| `tctechnology:space/hazard_forecasting` | 太空风险预报 | FRO | 13,000 | 3,200 | information/composite_armour、space/life_support | 太空风险预报台、应急信标、月面与火星远征规划 |
| `tctechnology:space/additive_manufacturing` | 增材制造 | IND | 12,000 | 3,000 | information/cnc_machining | additive_printer |
| `tctechnology:space/advanced_fission` | 先进裂变 | IND | 14,000 | 3,500 | atomic/nuclear_fission、information/automation | advanced_pwr_reactor、fast_reactor、compact_fission_reactor、reprocessing_plant；mox_fuel_assembly |
| `tctechnology:space/carbon_fiber` | 碳纤维 | IND | 11,000 | 2,750 | information/composites | carbon_fiber、carbon_fiber_panel |
| `tctechnology:space/fusion_experiment` | 聚变实验 | IND | 18,000 | 4,500 | space/advanced_fission、information/smart_grid | tokamak_controller；deuterium、tritium |
| `tctechnology:space/radioisotope_generator` | 同位素电源 | IND | 12,000 | 3,000 | atomic/nuclear_fission、information/integrated_circuit | rtg；pu238_heat_source |
| `tctechnology:space/space_solar` | 空间太阳能 | IND | 13,000 | 3,250 | information/photovoltaics | 耐辐照 solar_cell 配方（输入须为 Q4 电池片），供 Orbit 空间电源 #tcorbit:power/solar_panel |
| `tctechnology:space/maglev` | 磁浮 | NET | 13,000 | 500 | information/high_speed_rail | 磁浮导轨、磁浮列车 |
| `tctechnology:space/asteroid_mining` | 小行星采矿 | ORB | 16,000 | 2,600 | space/electric_propulsion、space/in_situ_resources | 小行星捕获任务 |
| `tctechnology:space/crewed_spaceflight` | 载人航天 | ORB | 13,000 | 1,800 | information/reentry_systems、information/cryogenic_propellants | 返回舱、座椅、逃逸塔、重型发射台、核心舱、舱壁、航天口粮 |
| `tctechnology:space/electric_propulsion` | 电推进 | ORB | 12,000 | 1,500 | information/communications_satellites | 离子推力器 |
| `tctechnology:space/extravehicular_activity` | 舱外活动 | ORB | 12,000 | 1,600 | space/life_support | 舱外航天服、套装接口、气闸 |
| `tctechnology:space/helium3_mining` | 氦-3 开采 | ORB | 14,000 | 2,200 | space/in_situ_resources | 氦-3 提取器 |
| `tctechnology:space/in_situ_resources` 🔑 | 原位资源利用 | ORB | 15,000 | 2,600 | space/lunar_outpost | ISRU 机器（5.11.2） |
| `tctechnology:space/interplanetary_transfer` 🔑 | 行星际转移 | ORB | 18,000 | 3,000 | space/orbital_assembly、space/life_support | 火星任务、深空天线；火星维度 |
| `tctechnology:space/life_support` 🔑 | 生命维持 | ORB | 14,000 | 2,000 | space/crewed_spaceflight | 生命维持单元与设备、居住舱、温室舱、乘员卧铺 |
| `tctechnology:space/lunar_landing` | 月面着陆 | ORB | 14,000 | 2,000 | space/rendezvous_docking、information/cryogenic_propellants | 着陆器、着陆腿、中继载荷；月球维度 |
| `tctechnology:space/lunar_outpost` | 月面前哨 | ORB | 14,000 | 2,200 | space/lunar_landing、space/life_support | 前哨核心、月面车、加压车、挖掘机、烧结机 |
| `tctechnology:space/mars_colonization` | 火星殖民 | ORB | 20,000 | 3,200 | space/interplanetary_transfer、space/in_situ_resources、space/methalox_propulsion | 高氯酸盐清洗机、殖民地信标 |
| `tctechnology:space/methalox_propulsion` | 甲烷发动机 | ORB | 13,000 | 1,600 | information/cryogenic_propellants、space/reusable_launch | 甲烷发动机 2 种 |
| `tctechnology:space/nuclear_thermal_propulsion` | 核热推进 | ORB | 17,000 | 2,800 | atomic/nuclear_fission、space/orbital_assembly | 核热发动机 |
| `tctechnology:space/orbital_assembly` 🔑 | 轨道组装 | ORB | 15,000 | 2,400 | space/rendezvous_docking、space/life_support | 实验舱、节点舱、桁架、补给站、船坞、机械臂、散热板 |
| `tctechnology:space/orbital_debris_management` | 太空碎片治理 | ORB | 12,000 | 1,500 | space/rendezvous_docking | 碎片收集载荷、惠普尔防护板 |
| `tctechnology:space/rendezvous_docking` | 交会对接 | ORB | 13,000 | 1,800 | space/crewed_spaceflight、information/automation | 对接口 |
| `tctechnology:space/reusable_launch` | 可复用火箭 | ORB | 13,000 | 1,800 | information/reentry_systems、information/automation | 助推器着陆腿、栅格舵、回收船、翻修套件 |
| `tctechnology:space/closed_habitats` | 封闭栖息地 | SET | 13,000 | 2,600 | atomic/high_rise_housing、space/life_support | habitat_bunk、太空住房 |
| `tctechnology:space/knowledge_ark` | 知识方舟 | TEC | 13,000 | 500 | information/collaborative_research、space/orbital_assembly | 落后 ≥ 2 时代的科技成本 ×0.5；终局完整科技谱系 |
| `tctechnology:space/planetary_geology` | 行星地质 | WLD | 13,000 | 500 | atomic/geophysical_survey、space/in_situ_resources | Orbit 月球、火星样本的矿物识别（3.17） |

## 4. 技能挂钩汇总

### 4.1 技能 × 模组（挂钩条数）

| 技能 | CIV | TEC | SKL | WLD | ECO | CUL | IND | NET | SET | EXC | DIS | ECH | COM | FRO | BEY | ATL | NAT | ORB | 合计 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `foraging` |  | 2 | 1 |  | 3 | 1 |  |  |  |  | 1 |  |  |  |  |  |  |  | **8** |
| `hunting` |  | 2 | 2 |  | 3 |  |  |  |  |  | 1 |  |  | 1 |  |  |  |  | **9** |
| `woodcutting` |  | 1 | 1 |  | 2 |  | 1 |  |  |  |  |  |  |  |  |  |  |  | **5** |
| `mining` |  | 1 | 1 | 8 |  |  | 1 |  |  |  |  |  |  |  |  |  |  | 1 | **12** |
| `farming` |  | 1 | 1 |  |  | 7 |  |  |  |  | 1 |  |  |  |  |  |  |  | **10** |
| `husbandry` |  |  | 4 |  | 3 | 5 |  | 1 |  |  |  |  |  |  |  |  |  |  | **13** |
| `cooking` |  | 1 | 1 |  |  | 3 |  |  |  |  |  |  |  |  |  |  |  |  | **5** |
| `crafting` |  | 5 | 1 |  |  |  | 2 |  |  |  | 3 |  |  |  |  |  |  |  | **11** |
| `smithing` |  |  | 2 |  |  |  | 6 |  |  |  |  |  |  | 1 |  |  |  |  | **9** |
| `engineering` |  |  | 1 |  | 1 | 2 | 6 | 3 |  |  | 1 |  |  | 1 |  | 1 |  | 3 | **19** |
| `construction` |  |  | 1 | 1 |  | 1 | 1 | 2 | 4 |  |  |  | 2 | 2 |  | 1 |  | 2 | **17** |
| `logistics` |  | 1 | 1 |  |  | 1 | 1 | 5 |  | 4 |  |  | 1 | 2 |  | 2 | 1 | 1 | **20** |
| `trade` |  | 1 | 1 |  |  | 1 |  |  |  | 9 |  |  | 1 | 1 |  | 1 | 1 |  | **16** |
| `diplomacy` | 1 | 1 | 1 |  |  |  |  |  |  | 3 | 1 |  | 4 | 1 |  | 1 | 7 |  | **20** |
| `leadership` | 2 | 2 | 3 |  | 1 |  | 1 |  | 8 | 3 | 2 |  | 6 | 2 |  |  | 2 | 2 | **34** |
| `scholarship` |  | 5 | 2 |  | 4 | 1 | 1 |  | 1 |  | 11 | 9 |  |  |  | 1 |  | 2 | **37** |
| `surveying` |  | 1 | 1 | 5 | 2 | 1 |  | 2 | 1 |  | 2 |  | 1 | 1 |  | 7 |  |  | **24** |
| `exploration` | 1 | 1 | 3 | 3 | 1 |  |  | 2 |  |  | 2 | 3 |  | 2 |  | 4 | 2 | 3 | **27** |
| `combat` |  |  | 2 |  | 1 |  |  |  |  |  |  |  |  | 4 |  |  | 1 |  | **8** |
| `medicine` |  |  | 2 |  | 1 | 1 |  |  | 3 |  | 1 |  |  | 1 |  |  |  |  | **9** |
| `aeronautics` |  |  | 1 |  |  |  | 1 | 3 |  |  |  |  |  |  |  | 1 |  | 10 | **16** |
| `arcana` |  |  |  |  |  |  |  |  |  |  |  |  |  |  | 13 |  |  |  | **13** |

## 5. 注册 ID 总表

### 5.1 文明核心 `tcivilization`（53 项，来源 [03-core-civilization.md](03-core-civilization.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcivilization:placeholder` | 已移除内容占位 | Removed Content Placeholder | — | 被删除方块的替身，右键显示原 ID（RM-P8） |
| item | `tcivilization:legacy_item` | 遗留物品 | Legacy Item | — | 被删除物品的替身，可分解为空 |
| item | `tcivilization:wood_ash` | 草木灰 | Wood Ash | E0 | 共享副产物（02 §3），加入 c:dusts/wood_ash；核心只注册不产出，由 Cultivation（篝火/熔炉，12 §8.2）与 Industry（炉窑，13 §5.3）产出；用于施肥与钾碱 |
| entity | `tcivilization:person` | 人物 | Person | E0 | 全家族共享类人 NPC，组件化（5.7） |
| block_entity | `tcivilization:placeholder` | 占位数据 | Placeholder Data | — | 保存原方块 ID 与 NBT |
| menu | `tcivilization:civ_panel` | 文明面板 | Civilization Panel | E0 | J 面板外壳，承载各模组页签 |
| menu | `tcivilization:command_wheel` | 指令轮盘 | Command Wheel | E0 | Z 指令轮盘覆盖层 |
| menu | `tcivilization:person_screen` | 人物检视 | Person Inspector | E0 | 人物检视外壳，承载各模组分节 |
| keybind | `tcivilization:civ_panel` | 打开文明面板 | Open Civilization Panel | — | key.tcivilization.civ_panel，默认 J |
| keybind | `tcivilization:command_wheel` | 指令轮盘 | Command Wheel | — | key.tcivilization.command_wheel，默认 Z，按住显示松开执行 |
| keybind | `tcivilization:hud_level` | 切换 HUD 级别 | Cycle HUD Level | — | key.tcivilization.hud_level，默认 H，仅 tcatlas 未安装时注册 |
| payload | `tcivilization:action_result` | 操作结果 | Action Result | — | S→C，ok/reason/撤销令牌（31 §16） |
| payload | `tcivilization:action_undo` | 操作撤销 | Action Undo | — | C→S，≤ 2 次/s |
| payload | `tcivilization:ceremony` | 仪式 | Ceremony | — | S→C，CeremonySpec，L4 仪式覆盖层 |
| payload | `tcivilization:civ_state_sync` | 文明状态同步 | Civilization State Sync | — | S→C |
| payload | `tcivilization:dialogue_choice` | 对话选择 | Dialogue Choice | — | C→S |
| payload | `tcivilization:dialogue_open` | 打开对话 | Dialogue Open | — | S→C |
| payload | `tcivilization:hud_data` | HUD 数据 | HUD Data | — | S→C |
| payload | `tcivilization:hud_subscribe` | HUD 订阅 | HUD Subscribe | — | C→S |
| payload | `tcivilization:notify` | 通知 | Notify | — | S→C，toast |
| payload | `tcivilization:panel_presence` | 面板在场 | Panel Presence | — | S→C，查看者与 30 s 软锁，≤ 1 次/s |
| payload | `tcivilization:panel_request` | 面板请求 | Panel Request | — | C→S |
| payload | `tcivilization:panel_snapshot` | 面板快照 | Panel Snapshot | — | S→C |
| payload | `tcivilization:person_inspect_request` | 人物检视请求 | Person Inspect Request | — | C→S |
| payload | `tcivilization:person_inspect_snapshot` | 人物检视快照 | Person Inspect Snapshot | — | S→C |
| payload | `tcivilization:region_layer_sync` | 地区层同步 | Region Layer Sync | — | S→C，3×3 地区 |
| payload | `tcivilization:tracker_pin` | 追踪固定 | Tracker Pin | — | C→S |
| payload | `tcivilization:tracker_sync` | 追踪同步 | Tracker Sync | — | S→C |
| payload | `tcivilization:wheel_execute` | 轮盘执行 | Wheel Execute | — | C→S |
| payload | `tcivilization:wheel_options` | 轮盘选项 | Wheel Options | — | S→C |
| payload | `tcivilization:wheel_query` | 轮盘查询 | Wheel Query | — | C→S |
| hud_widget | `tcivilization:alerts` | 告警 | Alerts | — | 极简级别，TOP_LEFT (4,22)，持续问题滚动条 |
| hud_widget | `tcivilization:date_season` | 日期与季节 | Date and Season | — | 标准级别，TOP_LEFT (4,4)，含时代徽章 |
| hud_widget | `tcivilization:pinned_tracker` | 追踪器 | Pinned Tracker | — | 标准级别，MIDDLE_RIGHT (-4,0)，最多 3 个 |
| hud_widget | `tcivilization:target_info` | 目标信息 | Target Info | — | 标准级别，TOP_CENTER (0,22)，Discovery 观察模式时隐藏 |
| civ_tab | `tcivilization:chronicle` | 编年史 | Chronicle | — | KNOWLEDGE 组，顺序 900，文明记忆 |
| civ_tab | `tcivilization:overview` | 概览 | Overview | — | CIV 组，顺序 0，承载各模组概览卡 |
| region_layer | `tcivilization:civ` | 文明层 | Civilization Layer | — | 发现位集、聚落索引、人物计数、压力 |
| sound | `tcivilization:event.ceremony` | 庆典 | Ceremony | — | 时代跃迁、重大功绩 |
| sound | `tcivilization:person.cheer` | 欢呼 | Person Cheer | E0 | 庆典、完工 |
| sound | `tcivilization:person.hurt` | 人物受伤 | Person Hurt | E0 |  |
| sound | `tcivilization:person.mourn` | 哀悼 | Person Mourn | E0 | 命名人物死亡 |
| sound | `tcivilization:person.voice` | 人物低语 | Person Voice | E0 | 无词语的拟声 |
| sound | `tcivilization:person.work.dig` | 挖掘 | Dig Work | E0 | 工作动作 |
| sound | `tcivilization:person.work.hammer` | 锤击 | Hammer Work | E0 | 工作动作 |
| sound | `tcivilization:person.work.hoe` | 锄地 | Hoe Work | E0 | 工作动作 |
| sound | `tcivilization:person.work.saw` | 锯木 | Saw Work | E0 | 工作动作 |
| sound | `tcivilization:ui.notify.critical` | 通知·危急 | Notify Critical | — | UI |
| sound | `tcivilization:ui.notify.info` | 通知·信息 | Notify Info | — | UI |
| sound | `tcivilization:ui.notify.success` | 通知·成功 | Notify Success | — | UI |
| sound | `tcivilization:ui.notify.warning` | 通知·警告 | Notify Warning | — | UI |
| sound | `tcivilization:ui.panel.open` | 面板打开 | Panel Open | — | UI |
| sound | `tcivilization:ui.wheel.open` | 轮盘打开 | Wheel Open | — | UI |

### 5.2 科技核心 `tctechnology`（85 项，来源 [04-core-technology.md](04-core-technology.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tctechnology:comm_station` | 通信站 | Communication Station | E9 | 全球通信网后备里程碑；仅 Orbit 缺失时可合成 |
| block | `tctechnology:fire_pit` | 篝火坑 | Fire Pit | E0 | 火塘研究地点、烤制、光源；替换原版营火配方 |
| block | `tctechnology:grinding_slab` | 砥石 | Grinding Slab | E1 | 磨制石器工作方块；neolithic/ground_stone_tools |
| block | `tctechnology:milestone_cache` | 物资窖 | Milestone Cache | — | 后备里程碑：提交物资、粮仓、方舟物资 |
| block | `tctechnology:milestone_marker` | 里程碑标记 | Milestone Marker | — | 后备里程碑：路线端点、贸易站、电网节点；拥有者模组缺失时才可合成 |
| block | `tctechnology:research_table` | 研究台 | Research Table | E1 | Discovery 缺失或 research_table_mode 允许时的研究地点 |
| block | `tctechnology:storage_basket` | 储物篮 | Storage Basket | E0 | 9 格自然物容器；stone/basketry |
| item | `tctechnology:antler_billet` | 鹿角软锤 | Antler Billet | E0 | stone/bone_working；过度剥落 ×0.7 |
| item | `tctechnology:antler_pick` | 鹿角镐 | Antler Pick | E1 | neolithic/flint_mining；只采软岩 |
| item | `tctechnology:bone_harpoon` | 骨鱼叉 | Bone Harpoon | E0 | stone/bone_working；带绳回收 |
| item | `tctechnology:bow_drill` | 弓钻 | Bow Drill | E0 | stone/fire_making；成功率 90% |
| item | `tctechnology:cordage` | 纤维绳 | Cordage | E0 | stone/foraging_lore；加入 #c:strings |
| item | `tctechnology:digging_stick` | 掘棍 | Digging Stick | E0 | 挖土、采根茎 |
| item | `tctechnology:fire_drill` | 钻火器 | Fire Drill | E0 | 钻木取火，成功率 20%/70% |
| item | `tctechnology:firebrand` | 火把束 | Firebrand | E1 | neolithic/fire_keeping；携火，免钻木点燃篝火坑 |
| item | `tctechnology:flatbow` | 平弓 | Flatbow | E1 | neolithic/flatbow；耐久 250 |
| item | `tctechnology:flint_arrow` | 燧石箭 | Flint Arrow | E0 | stone/archery；伤害 5 |
| item | `tctechnology:flint_arrowhead` | 燧石箭头 | Flint Arrowhead | E0 | stone/microliths |
| item | `tctechnology:flint_axe` | 燧石斧 | Flint Axe | E0 | stone/hafting |
| item | `tctechnology:flint_axe_head` | 燧石斧头 | Flint Axe Head | E0 | stone/knapping；装柄部件 |
| item | `tctechnology:flint_flake` | 燧石片 | Flint Flake | E0 | 粗打产物；简易切割 |
| item | `tctechnology:flint_knife` | 燧石刀 | Flint Knife | E0 | stone/knapping |
| item | `tctechnology:flint_scraper` | 刮削器 | Flint Scraper | E0 | stone/knapping；Industry 制革钩子 |
| item | `tctechnology:flint_sickle` | 燧石镰 | Flint Sickle | E1 | neolithic/composite_tools；3×3 收割 |
| item | `tctechnology:flint_spear` | 燧石矛 | Flint Spear | E0 | stone/hafting；近战 5 / 投掷 7 |
| item | `tctechnology:flint_spear_point` | 燧石矛头 | Flint Spear Point | E0 | stone/knapping；装柄部件 |
| item | `tctechnology:gathering_basket` | 采集篮 | Gathering Basket | E0 | stone/basketry；16 格自动收纳 |
| item | `tctechnology:hammerstone` | 锤石 | Hammerstone | E0 | 粗打与打制网格的基础工具；砾石 10% 掉落 |
| item | `tctechnology:hand_axe` | 手斧 | Hand Axe | E0 | stone/knapping；无柄砍木 |
| item | `tctechnology:microlith` | 细石叶 | Microlith | E0 | stone/microliths；复合工具部件 |
| item | `tctechnology:plant_fiber` | 植物纤维 | Plant Fiber | E0 | 破坏草类获得 |
| item | `tctechnology:polished_axe_head` | 磨制斧头 | Polished Axe Head | E1 | 砥石磨制产物 |
| item | `tctechnology:polished_stone_axe` | 磨制石斧 | Polished Stone Axe | E1 | 耐久 140 |
| item | `tctechnology:pressure_flaker` | 压剥器 | Pressure Flaker | E0 | stone/bone_working；细活配方必需 |
| item | `tctechnology:simple_bow` | 简易弓 | Simple Bow | E0 | stone/archery；耐久 120 |
| item | `tctechnology:spear_thrower` | 投矛器 | Spear Thrower | E0 | stone/spear_hunting；副手射程 ×1.6 |
| item | `tctechnology:stone_adze` | 石锛 | Stone Adze | E1 | 劈板：1 原木 → 2 木板 |
| item | `tctechnology:stone_axe_blank` | 石斧坯 | Stone Axe Blank | E1 | neolithic/ground_stone_tools |
| item | `tctechnology:stone_hoe` | 石锄 | Stone Hoe | E1 | neolithic/composite_tools；替代原版锄 |
| item | `tctechnology:tinder` | 火绒 | Tinder | E0 | stone/foraging_lore；生火材料 |
| item | `tctechnology:wooden_spear` | 削尖木矛 | Sharpened Wooden Spear | E0 | 无门槛近战/投掷 |
| entity | `tctechnology:flint_arrow` | 燧石箭 | Flint Arrow | E0 | 箭实体 |
| entity | `tctechnology:thrown_harpoon` | 投出的鱼叉 | Thrown Harpoon | E0 | 骨鱼叉投射物，8 格绳回收 |
| entity | `tctechnology:thrown_spear` | 投出的矛 | Thrown Spear | E0 | 木矛/燧石矛投射物，可拾回 |
| block_entity | `tctechnology:comm_station` | 通信站 | Communication Station | E9 | 联网站点登记 |
| block_entity | `tctechnology:fire_pit` | 篝火坑 | Fire Pit | E0 | 燃料、余烬计时、火塘议事状态 |
| block_entity | `tctechnology:grinding_slab` | 砥石 | Grinding Slab | E1 | 磨制进度 |
| block_entity | `tctechnology:milestone_cache` | 物资窖 | Milestone Cache | — | 已提交物资计数 |
| block_entity | `tctechnology:milestone_marker` | 里程碑标记 | Milestone Marker | — | 配对端点与验证状态 |
| block_entity | `tctechnology:research_table` | 研究台 | Research Table | E1 | 实现 ResearchSite；书籍槽、队列、主持者 |
| block_entity | `tctechnology:storage_basket` | 储物篮 | Storage Basket | E0 | 9 格库存 |
| menu | `tctechnology:fire_pit` | 篝火坑 | Fire Pit | E0 | 燃料与烤制槽 |
| menu | `tctechnology:gathering_basket` | 采集篮 | Gathering Basket | E0 | 16 格 |
| menu | `tctechnology:knapping` | 打制 | Knapping | E0 | 5×5 打制网格 |
| menu | `tctechnology:milestone_cache` | 物资窖 | Milestone Cache | — | 提交物资进度 |
| menu | `tctechnology:research_table` | 研究台 | Research Table | E1 | 项目、队列、书籍、赞助槽 |
| menu | `tctechnology:storage_basket` | 储物篮 | Storage Basket | E0 | 9 格 |
| keybind | `tctechnology:tech_tree` | 科技树 | Technology Tree | — | 按键映射名 key.tctechnology.tech_tree，默认 U，打开 J 文明面板的科技页签 |
| payload | `tctechnology:declare_era` | 宣布时代 | Declare Era | — | C→S，服务端复核条件 |
| payload | `tctechnology:era_advanced` | 时代已跃迁 | Era Advanced | — | S→全部 C；触发仪式覆盖层，Atlas 切换时代皮肤 |
| payload | `tctechnology:knapping_strike` | 打制敲击 | Knapping Strike | — | C→S，网格格子索引 |
| payload | `tctechnology:research_command` | 研究指令 | Research Command | — | C→S，设为研究、队列、研究池投入 |
| payload | `tctechnology:tech_defs_sync` | 科技定义同步 | Tech Definitions Sync | — | S→C，登录与重载时 |
| payload | `tctechnology:tech_delta` | 科技状态增量 | Tech Delta | — | S→C，每 20 tick 合并 |
| payload | `tctechnology:tech_state_sync` | 科技状态同步 | Tech State Sync | — | S→C，登录时完整状态 |
| hud_widget | `tctechnology:era_progress` | 时代进度 | Era Progress | — | 扩展层级，默认隐藏；关键科技与里程碑清单；hud.tctechnology.era_progress |
| hud_widget | `tctechnology:research_progress` | 研究进度 | Research Progress | — | 标准层级；当前研究与研究池 RP；hud.tctechnology.research_progress |
| civ_tab | `tctechnology:tech_tree` | 科技 | Technology | — | 文明分组，排序 100；本地化键 civtab.tctechnology.tech_tree |
| sound | `tctechnology:block.fire_pit.crackle` | 篝火噼啪声 | Fire Pit Crackle | E0 | 环境循环 |
| sound | `tctechnology:block.knapping.strike` | 打制敲击声 | Knapping Strike | E0 | 粗打与网格 |
| sound | `tctechnology:event.era_advance` | 时代跃迁乐句 | Era Advance Stinger | — | 仪式第 1 阶段 |
| sound | `tctechnology:event.tech_unlocked` | 科技完成 | Technology Unlocked | — | 完成通知 |
| sound | `tctechnology:item.fire_drill.spin` | 钻木声 | Fire Drill Spin | E0 | 钻火器与弓钻 |
| sound | `tctechnology:music.era.atomic` | 原子时代主题 | Atomic Age Theme | E8 | 音乐类别 |
| sound | `tctechnology:music.era.bronze` | 青铜时代主题 | Bronze Age Theme | E2 | 音乐类别（01 §9） |
| sound | `tctechnology:music.era.electric` | 电气时代主题 | Electric Age Theme | E7 | 音乐类别 |
| sound | `tctechnology:music.era.industrial` | 工业时代主题 | Industrial Age Theme | E6 | 音乐类别 |
| sound | `tctechnology:music.era.information` | 信息时代主题 | Information Age Theme | E9 | 音乐类别 |
| sound | `tctechnology:music.era.iron` | 铁器时代主题 | Iron Age Theme | E3 | 音乐类别 |
| sound | `tctechnology:music.era.medieval` | 中世纪主题 | Medieval Theme | E4 | 音乐类别 |
| sound | `tctechnology:music.era.neolithic` | 新石器时代主题 | Neolithic Theme | E1 | 音乐类别 |
| sound | `tctechnology:music.era.renaissance` | 文艺复兴主题 | Renaissance Theme | E5 | 音乐类别 |
| sound | `tctechnology:music.era.space` | 太空时代主题 | Space Age Theme | E10 | 音乐类别 |
| sound | `tctechnology:music.era.stone` | 石器时代主题 | Stone Age Theme | E0 | 音乐类别，3–5 首 |
| sound | `tctechnology:ui.insight` | 灵感 | Insight | — | 知识达阈值 |

### 5.3 技能 `tcskills`（20 项，来源 [05-skills.md](05-skills.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| keybind | `tcskills:skills` | 技能 | Skills | — | 语言键 key.tcskills.skills，分类 key.categories.tc；S0 默认 J，S1 默认未绑定（9.1） |
| payload | `tcskills:apprentice_request` | 拜师请求 | Apprentice Request | — | C→S |
| payload | `tcskills:goal_pin` | 追踪目标 | Goal Pin | — | C→S |
| payload | `tcskills:history_page` | 成长记录页 | History Page | — | S→C |
| payload | `tcskills:history_request` | 成长记录请求 | History Request | — | C→S |
| payload | `tcskills:proficiency_delta` | 熟练度增量 | Proficiency Delta | — | S→C，每 10 tick 合批 |
| payload | `tcskills:profile_request` | 档案请求 | Profile Request | — | C→S，只用于 S0 |
| payload | `tcskills:profile_snapshot` | 档案快照 | Profile Snapshot | — | S→C |
| payload | `tcskills:respec_request` | 重修请求 | Respec Request | — | C→S |
| payload | `tcskills:roster_request` | 名册请求 | Roster Request | — | C→S |
| payload | `tcskills:roster_snapshot` | 名册快照 | Roster Snapshot | — | S→C |
| payload | `tcskills:sense_highlight` | 感知高亮 | Sense Highlight | — | S→C，forage_sense / vein_sense |
| payload | `tcskills:specialty_set` | 设定专精 | Specialty Set | — | C→S |
| payload | `tcskills:talent_commit` | 天赋提交 | Talent Commit | — | C→S（12.6） |
| payload | `tcskills:talent_result` | 天赋操作结果 | Talent Result | — | S→C |
| hud_widget | `tcskills:proficiency_feed` | 熟练度动态 | Proficiency Feed | — | 标准等级，右中堆叠末尾；最近 5 秒有获得时出现，3 秒淡出（10.7） |
| civ_tab | `tcskills:skills` | 技能 | Skills | — | 文明面板“个人”分组，排序 50（S1） |
| sound | `tcskills:ui.growth_up` | 成长等级提升 | Growth Level Up | — | 界面音，类别 master |
| sound | `tcskills:ui.level_up` | 技能升级 | Skill Level Up | — | 界面音，类别 master |
| sound | `tcskills:ui.talent_learn` | 天赋学习 | Talent Learned | — | 界面音，类别 master |

### 5.4 山河 `tcwildlands`（381 项，来源 [10-wildlands.md](10-wildlands.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcwildlands:alluvium` | 冲积土 | Alluvium | E0 | 土壤与沉积物；河流沉积 |
| block | `tcwildlands:andesite_bricks` | 安山岩砖 | Andesite Bricks | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:andesite_chalcopyrite_ore` | 安山岩黄铜矿矿石 | Andesite Chalcopyrite Ore | E3 | 围岩 andesite；状态 grade |
| block | `tcwildlands:andesite_cobble` | 安山岩圆石 | Andesite Cobble | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:andesite_native_gold_ore` | 安山岩自然金矿石 | Andesite Native Gold Ore | E1 | 围岩 andesite；状态 grade |
| block | `tcwildlands:andesite_native_silver_ore` | 安山岩自然银矿石 | Andesite Native Silver Ore | E2 | 围岩 andesite；状态 grade |
| block | `tcwildlands:anthracite_seam` | 无烟煤层 | Anthracite Seam | E6 | 煤层；状态 grade |
| block | `tcwildlands:basalt_rock` | 玄武岩 | Basalt Rock | E0 | 喷出岩；原岩（6.1） |
| block | `tcwildlands:basalt_rock_amethyst_ore` | 玄武岩紫水晶矿石 | Basalt Rock Amethyst Ore | E2 | 围岩 basalt_rock；状态 grade |
| block | `tcwildlands:basalt_rock_bricks` | 玄武岩砖 | Basalt Rock Bricks | E0 | 建材形态 |
| block | `tcwildlands:basalt_rock_chalcopyrite_ore` | 玄武岩黄铜矿矿石 | Basalt Rock Chalcopyrite Ore | E3 | 围岩 basalt_rock；状态 grade |
| block | `tcwildlands:basalt_rock_cobble` | 玄武岩圆石 | Basalt Rock Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:basalt_rock_native_copper_ore` | 玄武岩自然铜矿石 | Basalt Rock Native Copper Ore | E1 | 围岩 basalt_rock；状态 grade |
| block | `tcwildlands:basalt_rock_sulfur_ore` | 玄武岩自然硫矿石 | Basalt Rock Sulfur Ore | E5 | 围岩 basalt_rock；状态 grade |
| block | `tcwildlands:bitumen_seep` | 沥青渗出 | Bitumen Seep | E1 | 油藏上方；产 raw_bitumen |
| block | `tcwildlands:bituminous_coal_seam` | 烟煤层 | Bituminous Coal Seam | E6 | 煤层；状态 grade |
| block | `tcwildlands:black_sand` | 黑砂 | Black Sand | E0 | 土壤与沉积物；受重力；淘洗得 raw_ilmenite |
| block | `tcwildlands:chalk` | 白垩 | Chalk | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:chalk_bricks` | 白垩砖 | Chalk Bricks | E0 | 建材形态 |
| block | `tcwildlands:chalk_cobble` | 白垩圆石 | Chalk Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:chalk_flint_ore` | 白垩燧石矿石 | Chalk Flint Ore | E0 | 围岩 chalk；状态 grade |
| block | `tcwildlands:chernozem` | 黑钙土 | Chernozem | E0 | 土壤与沉积物；草原土 |
| block | `tcwildlands:conglomerate` | 砾岩 | Conglomerate | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:conglomerate_bricks` | 砾岩砖 | Conglomerate Bricks | E0 | 建材形态 |
| block | `tcwildlands:conglomerate_cobble` | 砾岩圆石 | Conglomerate Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:conglomerate_native_copper_ore` | 砾岩自然铜矿石 | Conglomerate Native Copper Ore | E1 | 围岩 conglomerate；状态 grade |
| block | `tcwildlands:conglomerate_native_gold_ore` | 砾岩自然金矿石 | Conglomerate Native Gold Ore | E1 | 围岩 conglomerate；状态 grade |
| block | `tcwildlands:conglomerate_uraninite_ore` | 砾岩沥青铀矿矿石 | Conglomerate Uraninite Ore | E8 | 围岩 conglomerate；状态 grade |
| block | `tcwildlands:deep_threshold` | 深地门槛 | Deep Threshold | E8 | 方块实体；不可破坏；往返深地（5.16.2） |
| block | `tcwildlands:diorite_bricks` | 闪长岩砖 | Diorite Bricks | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:diorite_chalcopyrite_ore` | 闪长岩黄铜矿矿石 | Diorite Chalcopyrite Ore | E3 | 围岩 diorite；状态 grade |
| block | `tcwildlands:diorite_cobble` | 闪长岩圆石 | Diorite Cobble | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:diorite_molybdenite_ore` | 闪长岩辉钼矿矿石 | Diorite Molybdenite Ore | E8 | 围岩 diorite；状态 grade |
| block | `tcwildlands:dolomite` | 白云岩 | Dolomite | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:dolomite_bricks` | 白云岩砖 | Dolomite Bricks | E0 | 建材形态 |
| block | `tcwildlands:dolomite_calamine_ore` | 白云岩菱锌矿矿石 | Dolomite Calamine Ore | E3 | 围岩 dolomite；状态 grade |
| block | `tcwildlands:dolomite_cobble` | 白云岩圆石 | Dolomite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:dolomite_galena_ore` | 白云岩方铅矿矿石 | Dolomite Galena Ore | E2 | 围岩 dolomite；状态 grade |
| block | `tcwildlands:dolomite_sphalerite_ore` | 白云岩闪锌矿矿石 | Dolomite Sphalerite Ore | E4 | 围岩 dolomite；状态 grade |
| block | `tcwildlands:eclogite` | 榴辉岩 | Eclogite | E0 | 高压变质岩（附加）；原岩（6.1） |
| block | `tcwildlands:eclogite_bricks` | 榴辉岩砖 | Eclogite Bricks | E0 | 建材形态 |
| block | `tcwildlands:eclogite_cobble` | 榴辉岩圆石 | Eclogite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:eclogite_diamond_ore` | 榴辉岩金刚石矿石 | Eclogite Diamond Ore | E5 | 围岩 eclogite；状态 grade |
| block | `tcwildlands:eclogite_rutile_ore` | 榴辉岩金红石矿石 | Eclogite Rutile Ore | E8 | 围岩 eclogite；状态 grade |
| block | `tcwildlands:flood_silt` | 洪积淤泥 | Flood Silt | E0 | 土壤与沉积物；层状 1-3 层；一季后并入下方土壤 |
| block | `tcwildlands:fumarole` | 喷气孔 | Fumarole | E1 | 火山、地热；生成硫结壳 |
| block | `tcwildlands:gabbro` | 辉长岩 | Gabbro | E0 | 侵入岩；原岩（6.1） |
| block | `tcwildlands:gabbro_bricks` | 辉长岩砖 | Gabbro Bricks | E0 | 建材形态 |
| block | `tcwildlands:gabbro_chromite_ore` | 辉长岩铬铁矿矿石 | Gabbro Chromite Ore | E7 | 围岩 gabbro；状态 grade |
| block | `tcwildlands:gabbro_cobble` | 辉长岩圆石 | Gabbro Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:gabbro_ilmenite_ore` | 辉长岩钛铁矿矿石 | Gabbro Ilmenite Ore | E8 | 围岩 gabbro；状态 grade |
| block | `tcwildlands:gabbro_magnetite_ore` | 辉长岩磁铁矿矿石 | Gabbro Magnetite Ore | E3 | 围岩 gabbro；状态 grade |
| block | `tcwildlands:gabbro_pentlandite_ore` | 辉长岩镍黄铁矿矿石 | Gabbro Pentlandite Ore | E7 | 围岩 gabbro；状态 grade |
| block | `tcwildlands:gas_seep` | 气泡渗出 | Gas Seep | E7 | 气藏上方水底 |
| block | `tcwildlands:geyser` | 间歇泉口 | Geyser | E0 | 方块实体；周期喷发 |
| block | `tcwildlands:gneiss` | 片麻岩 | Gneiss | E0 | 变质岩；原岩（6.1） |
| block | `tcwildlands:gneiss_bricks` | 片麻岩砖 | Gneiss Bricks | E0 | 建材形态 |
| block | `tcwildlands:gneiss_cobaltite_ore` | 片麻岩辉砷钴矿矿石 | Gneiss Cobaltite Ore | E9 | 围岩 gneiss；状态 grade |
| block | `tcwildlands:gneiss_cobble` | 片麻岩圆石 | Gneiss Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:gneiss_graphite_ore` | 片麻岩石墨矿石 | Gneiss Graphite Ore | E5 | 围岩 gneiss；状态 grade |
| block | `tcwildlands:gneiss_magnetite_ore` | 片麻岩磁铁矿矿石 | Gneiss Magnetite Ore | E3 | 围岩 gneiss；状态 grade |
| block | `tcwildlands:gneiss_sapphire_ore` | 片麻岩蓝宝石矿石 | Gneiss Sapphire Ore | E4 | 围岩 gneiss；状态 grade |
| block | `tcwildlands:gossan` | 铁帽 | Gossan | E2 | 硫化物矿体上方风化帽 |
| block | `tcwildlands:granite_bricks` | 花岗岩砖 | Granite Bricks | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:granite_cassiterite_ore` | 花岗岩锡石矿石 | Granite Cassiterite Ore | E2 | 围岩 granite；状态 grade |
| block | `tcwildlands:granite_chalcopyrite_ore` | 花岗岩黄铜矿矿石 | Granite Chalcopyrite Ore | E3 | 围岩 granite；状态 grade |
| block | `tcwildlands:granite_cobble` | 花岗岩圆石 | Granite Cobble | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:granite_galena_ore` | 花岗岩方铅矿矿石 | Granite Galena Ore | E2 | 围岩 granite；状态 grade |
| block | `tcwildlands:granite_molybdenite_ore` | 花岗岩辉钼矿矿石 | Granite Molybdenite Ore | E8 | 围岩 granite；状态 grade |
| block | `tcwildlands:granite_monazite_ore` | 花岗岩独居石矿石 | Granite Monazite Ore | E9 | 围岩 granite；状态 grade |
| block | `tcwildlands:granite_uraninite_ore` | 花岗岩沥青铀矿矿石 | Granite Uraninite Ore | E8 | 围岩 granite；状态 grade |
| block | `tcwildlands:granite_wolframite_ore` | 花岗岩黑钨矿矿石 | Granite Wolframite Ore | E7 | 围岩 granite；状态 grade |
| block | `tcwildlands:gravel_cassiterite_ore` | 砾石锡石矿石 | Gravel Cassiterite Ore | E2 | 围岩 gravel；状态 grade；受重力（砂矿） |
| block | `tcwildlands:gravel_diamond_ore` | 砾石金刚石矿石 | Gravel Diamond Ore | E5 | 围岩 gravel；状态 grade；受重力（砂矿） |
| block | `tcwildlands:gravel_native_gold_ore` | 砾石自然金矿石 | Gravel Native Gold Ore | E1 | 围岩 gravel；状态 grade；受重力（砂矿） |
| block | `tcwildlands:gravel_ruby_ore` | 砾石红宝石矿石 | Gravel Ruby Ore | E4 | 围岩 gravel；状态 grade；受重力（砂矿） |
| block | `tcwildlands:gravel_sapphire_ore` | 砾石蓝宝石矿石 | Gravel Sapphire Ore | E4 | 围岩 gravel；状态 grade；受重力（砂矿） |
| block | `tcwildlands:gypsum_rock` | 石膏岩 | Gypsum Rock | E0 | 蒸发岩；原岩（6.1） |
| block | `tcwildlands:gypsum_rock_bricks` | 石膏岩砖 | Gypsum Rock Bricks | E0 | 建材形态 |
| block | `tcwildlands:gypsum_rock_cobble` | 石膏岩圆石 | Gypsum Rock Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:gypsum_rock_sulfur_ore` | 石膏岩自然硫矿石 | Gypsum Rock Sulfur Ore | E5 | 围岩 gypsum_rock；状态 grade |
| block | `tcwildlands:kaolinite` | 高岭土 | Kaolinite | E4 | 土壤与沉积物；掉落 raw_kaolinite ×4 |
| block | `tcwildlands:kimberlite` | 金伯利岩 | Kimberlite | E0 | 超基性岩（附加）；原岩（6.1） |
| block | `tcwildlands:kimberlite_bricks` | 金伯利岩砖 | Kimberlite Bricks | E0 | 建材形态 |
| block | `tcwildlands:kimberlite_cobble` | 金伯利岩圆石 | Kimberlite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:kimberlite_diamond_ore` | 金伯利岩金刚石矿石 | Kimberlite Diamond Ore | E5 | 围岩 kimberlite；状态 grade |
| block | `tcwildlands:laterite` | 红土 | Laterite | E0 | 土壤与沉积物；热带风化；放置后硬化 |
| block | `tcwildlands:laterite_bauxite_ore` | 红土铝土矿矿石 | Laterite Bauxite Ore | E7 | 围岩 laterite；状态 grade |
| block | `tcwildlands:lava_crust` | 熔岩壳 | Lava Crust | E0 | 1-3 MC 日后转为 basalt_rock |
| block | `tcwildlands:lignite_seam` | 褐煤层 | Lignite Seam | E4 | 煤层；状态 grade |
| block | `tcwildlands:limestone` | 石灰岩 | Limestone | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:limestone_bauxite_ore` | 石灰岩铝土矿矿石 | Limestone Bauxite Ore | E7 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_bricks` | 石灰岩砖 | Limestone Bricks | E0 | 建材形态 |
| block | `tcwildlands:limestone_calamine_ore` | 石灰岩菱锌矿矿石 | Limestone Calamine Ore | E3 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_cobble` | 石灰岩圆石 | Limestone Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:limestone_flint_ore` | 石灰岩燧石矿石 | Limestone Flint Ore | E0 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_galena_ore` | 石灰岩方铅矿矿石 | Limestone Galena Ore | E2 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_malachite_ore` | 石灰岩孔雀石矿石 | Limestone Malachite Ore | E2 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_native_silver_ore` | 石灰岩自然银矿石 | Limestone Native Silver Ore | E2 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_niter_ore` | 石灰岩硝石矿石 | Limestone Niter Ore | E5 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_phosphorite_ore` | 石灰岩磷块岩矿石 | Limestone Phosphorite Ore | E7 | 围岩 limestone；状态 grade |
| block | `tcwildlands:limestone_sphalerite_ore` | 石灰岩闪锌矿矿石 | Limestone Sphalerite Ore | E4 | 围岩 limestone；状态 grade |
| block | `tcwildlands:loess` | 黄土 | Loess | E0 | 土壤与沉积物；风成粉砂；坡面雨蚀（6.4） |
| block | `tcwildlands:loose_stones` | 松散石块 | Loose Stones | E0 | 地表特征；拾起 loose_rock |
| block | `tcwildlands:marble` | 大理岩 | Marble | E0 | 变质岩；原岩（6.1） |
| block | `tcwildlands:marble_bricks` | 大理岩砖 | Marble Bricks | E0 | 建材形态 |
| block | `tcwildlands:marble_cobble` | 大理岩圆石 | Marble Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:marble_graphite_ore` | 大理岩石墨矿石 | Marble Graphite Ore | E5 | 围岩 marble；状态 grade |
| block | `tcwildlands:marble_lapis_ore` | 大理岩青金石矿石 | Marble Lapis Ore | E2 | 围岩 marble；状态 grade |
| block | `tcwildlands:marble_magnetite_ore` | 大理岩磁铁矿矿石 | Marble Magnetite Ore | E3 | 围岩 marble；状态 grade |
| block | `tcwildlands:marble_ruby_ore` | 大理岩红宝石矿石 | Marble Ruby Ore | E4 | 围岩 marble；状态 grade |
| block | `tcwildlands:marl` | 泥灰岩土 | Marl | E0 | 土壤与沉积物；湖相钙质黏土 |
| block | `tcwildlands:massive_sulfide` | 块状硫化物 | Massive Sulfide | E3 | 混合矿石（5:3:2） |
| block | `tcwildlands:mud_bog_iron_ore` | 泥沼铁矿矿石 | Mud Bog Iron Ore | E3 | 围岩 mud；状态 grade |
| block | `tcwildlands:mudstone` | 泥岩 | Mudstone | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:mudstone_bricks` | 泥岩砖 | Mudstone Bricks | E0 | 建材形态 |
| block | `tcwildlands:mudstone_cobble` | 泥岩圆石 | Mudstone Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:mudstone_phosphorite_ore` | 泥岩磷块岩矿石 | Mudstone Phosphorite Ore | E7 | 围岩 mudstone；状态 grade |
| block | `tcwildlands:obsidian_bricks` | 黑曜石砖 | Obsidian Bricks | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:obsidian_cobble` | 黑曜石圆石 | Obsidian Cobble | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:ore_float` | 矿化漂砾 | Ore Float | E1 | 找矿标志；状态 mineral |
| block | `tcwildlands:peat` | 泥炭 | Peat | E1 | 土壤与沉积物；掉落 raw_peat；可燃 |
| block | `tcwildlands:peat_bog_iron_ore` | 泥炭沼铁矿矿石 | Peat Bog Iron Ore | E3 | 围岩 peat；状态 grade |
| block | `tcwildlands:pegmatite` | 伟晶岩 | Pegmatite | E0 | 侵入岩（附加）；原岩（6.1） |
| block | `tcwildlands:pegmatite_bricks` | 伟晶岩砖 | Pegmatite Bricks | E0 | 建材形态 |
| block | `tcwildlands:pegmatite_cassiterite_ore` | 伟晶岩锡石矿石 | Pegmatite Cassiterite Ore | E2 | 围岩 pegmatite；状态 grade |
| block | `tcwildlands:pegmatite_cobble` | 伟晶岩圆石 | Pegmatite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:pegmatite_emerald_ore` | 伟晶岩祖母绿矿石 | Pegmatite Emerald Ore | E4 | 围岩 pegmatite；状态 grade |
| block | `tcwildlands:pegmatite_spodumene_ore` | 伟晶岩锂辉石矿石 | Pegmatite Spodumene Ore | E9 | 围岩 pegmatite；状态 grade |
| block | `tcwildlands:pegmatite_wolframite_ore` | 伟晶岩黑钨矿矿石 | Pegmatite Wolframite Ore | E7 | 围岩 pegmatite；状态 grade |
| block | `tcwildlands:peridotite` | 橄榄岩 | Peridotite | E0 | 超基性岩；原岩（6.1） |
| block | `tcwildlands:peridotite_bricks` | 橄榄岩砖 | Peridotite Bricks | E0 | 建材形态 |
| block | `tcwildlands:peridotite_chromite_ore` | 橄榄岩铬铁矿矿石 | Peridotite Chromite Ore | E7 | 围岩 peridotite；状态 grade |
| block | `tcwildlands:peridotite_cobble` | 橄榄岩圆石 | Peridotite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:peridotite_jade_ore` | 橄榄岩玉矿石 | Peridotite Jade Ore | E1 | 围岩 peridotite；状态 grade |
| block | `tcwildlands:peridotite_pentlandite_ore` | 橄榄岩镍黄铁矿矿石 | Peridotite Pentlandite Ore | E7 | 围岩 peridotite；状态 grade |
| block | `tcwildlands:permafrost` | 冻土 | Permafrost | E0 | 土壤与沉积物；thawed 状态（5.12.3） |
| block | `tcwildlands:polished_basalt_rock` | 磨制玄武岩 | Polished Basalt Rock | E0 | 建材形态 |
| block | `tcwildlands:polished_chalk` | 磨制白垩 | Polished Chalk | E0 | 建材形态 |
| block | `tcwildlands:polished_conglomerate` | 磨制砾岩 | Polished Conglomerate | E0 | 建材形态 |
| block | `tcwildlands:polished_dolomite` | 磨制白云岩 | Polished Dolomite | E0 | 建材形态 |
| block | `tcwildlands:polished_eclogite` | 磨制榴辉岩 | Polished Eclogite | E0 | 建材形态 |
| block | `tcwildlands:polished_gabbro` | 磨制辉长岩 | Polished Gabbro | E0 | 建材形态 |
| block | `tcwildlands:polished_gneiss` | 磨制片麻岩 | Polished Gneiss | E0 | 建材形态 |
| block | `tcwildlands:polished_gypsum_rock` | 磨制石膏岩 | Polished Gypsum Rock | E0 | 建材形态 |
| block | `tcwildlands:polished_kimberlite` | 磨制金伯利岩 | Polished Kimberlite | E0 | 建材形态 |
| block | `tcwildlands:polished_limestone` | 磨制石灰岩 | Polished Limestone | E0 | 建材形态 |
| block | `tcwildlands:polished_marble` | 磨制大理岩 | Polished Marble | E0 | 建材形态 |
| block | `tcwildlands:polished_mudstone` | 磨制泥岩 | Polished Mudstone | E0 | 建材形态 |
| block | `tcwildlands:polished_obsidian` | 磨制黑曜石 | Polished Obsidian | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:polished_pegmatite` | 磨制伟晶岩 | Polished Pegmatite | E0 | 建材形态 |
| block | `tcwildlands:polished_peridotite` | 磨制橄榄岩 | Polished Peridotite | E0 | 建材形态 |
| block | `tcwildlands:polished_pumice` | 磨制浮岩 | Polished Pumice | E0 | 建材形态 |
| block | `tcwildlands:polished_quartzite` | 磨制石英岩 | Polished Quartzite | E0 | 建材形态 |
| block | `tcwildlands:polished_rhyolite` | 磨制流纹岩 | Polished Rhyolite | E0 | 建材形态 |
| block | `tcwildlands:polished_rock_salt` | 磨制岩盐 | Polished Rock Salt | E0 | 建材形态 |
| block | `tcwildlands:polished_sandstone_rock` | 磨制砂岩 | Polished Sandstone Rock | E0 | 建材形态 |
| block | `tcwildlands:polished_schist` | 磨制片岩 | Polished Schist | E0 | 建材形态 |
| block | `tcwildlands:polished_shale` | 磨制页岩 | Polished Shale | E0 | 建材形态 |
| block | `tcwildlands:polished_slate` | 磨制板岩 | Polished Slate | E0 | 建材形态 |
| block | `tcwildlands:pumice` | 浮岩 | Pumice | E0 | 火山碎屑岩；原岩（6.1） |
| block | `tcwildlands:pumice_bricks` | 浮岩砖 | Pumice Bricks | E0 | 建材形态 |
| block | `tcwildlands:pumice_cobble` | 浮岩圆石 | Pumice Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:quartz_sand` | 石英砂 | Quartz Sand | E3 | 土壤与沉积物；受重力；契约矿物 quartz_sand 以方块存在 |
| block | `tcwildlands:quartzite` | 石英岩 | Quartzite | E0 | 变质岩；原岩（6.1） |
| block | `tcwildlands:quartzite_bricks` | 石英岩砖 | Quartzite Bricks | E0 | 建材形态 |
| block | `tcwildlands:quartzite_cobble` | 石英岩圆石 | Quartzite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:quartzite_hematite_ore` | 石英岩赤铁矿矿石 | Quartzite Hematite Ore | E3 | 围岩 quartzite；状态 grade |
| block | `tcwildlands:rhyolite` | 流纹岩 | Rhyolite | E0 | 喷出岩；原岩（6.1） |
| block | `tcwildlands:rhyolite_bricks` | 流纹岩砖 | Rhyolite Bricks | E0 | 建材形态 |
| block | `tcwildlands:rhyolite_cobble` | 流纹岩圆石 | Rhyolite Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:rhyolite_native_silver_ore` | 流纹岩自然银矿石 | Rhyolite Native Silver Ore | E2 | 围岩 rhyolite；状态 grade |
| block | `tcwildlands:rock_bolts` | 锚杆 | Rock Bolts | E8 | 支护 +30 |
| block | `tcwildlands:rock_salt` | 岩盐 | Rock Salt | E0 | 蒸发岩；原岩（6.1） |
| block | `tcwildlands:rock_salt_bricks` | 岩盐砖 | Rock Salt Bricks | E0 | 建材形态 |
| block | `tcwildlands:rock_salt_cobble` | 岩盐圆石 | Rock Salt Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:rock_salt_sylvite_ore` | 岩盐钾盐矿石 | Rock Salt Sylvite Ore | E7 | 围岩 rock_salt；状态 grade |
| block | `tcwildlands:salt_crust` | 盐壳 | Salt Crust | E1 | 盐湖、盐滩；brine 蒸发 |
| block | `tcwildlands:sand_ilmenite_ore` | 沙钛铁矿矿石 | Sand Ilmenite Ore | E8 | 围岩 sand；状态 grade；受重力（砂矿） |
| block | `tcwildlands:sand_monazite_ore` | 沙独居石矿石 | Sand Monazite Ore | E9 | 围岩 sand；状态 grade；受重力（砂矿） |
| block | `tcwildlands:sand_rutile_ore` | 沙金红石矿石 | Sand Rutile Ore | E8 | 围岩 sand；状态 grade；受重力（砂矿） |
| block | `tcwildlands:sandstone_rock` | 砂岩 | Sandstone Rock | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:sandstone_rock_bricks` | 砂岩砖 | Sandstone Rock Bricks | E0 | 建材形态 |
| block | `tcwildlands:sandstone_rock_cinnabar_ore` | 砂岩辰砂矿石 | Sandstone Rock Cinnabar Ore | E4 | 围岩 sandstone_rock；状态 grade |
| block | `tcwildlands:sandstone_rock_cobble` | 砂岩圆石 | Sandstone Rock Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:sandstone_rock_hematite_ore` | 砂岩赤铁矿矿石 | Sandstone Rock Hematite Ore | E3 | 围岩 sandstone_rock；状态 grade |
| block | `tcwildlands:sandstone_rock_malachite_ore` | 砂岩孔雀石矿石 | Sandstone Rock Malachite Ore | E2 | 围岩 sandstone_rock；状态 grade |
| block | `tcwildlands:sandstone_rock_native_copper_ore` | 砂岩自然铜矿石 | Sandstone Rock Native Copper Ore | E1 | 围岩 sandstone_rock；状态 grade |
| block | `tcwildlands:sandstone_rock_niter_ore` | 砂岩硝石矿石 | Sandstone Rock Niter Ore | E5 | 围岩 sandstone_rock；状态 grade |
| block | `tcwildlands:sandstone_rock_uraninite_ore` | 砂岩沥青铀矿矿石 | Sandstone Rock Uraninite Ore | E8 | 围岩 sandstone_rock；状态 grade |
| block | `tcwildlands:saprolite` | 腐泥土 | Saprolite | E0 | 土壤与沉积物；原位风化岩 |
| block | `tcwildlands:schist` | 片岩 | Schist | E0 | 变质岩；原岩（6.1） |
| block | `tcwildlands:schist_bricks` | 片岩砖 | Schist Bricks | E0 | 建材形态 |
| block | `tcwildlands:schist_cobaltite_ore` | 片岩辉砷钴矿矿石 | Schist Cobaltite Ore | E9 | 围岩 schist；状态 grade |
| block | `tcwildlands:schist_cobble` | 片岩圆石 | Schist Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:schist_emerald_ore` | 片岩祖母绿矿石 | Schist Emerald Ore | E4 | 围岩 schist；状态 grade |
| block | `tcwildlands:schist_graphite_ore` | 片岩石墨矿石 | Schist Graphite Ore | E5 | 围岩 schist；状态 grade |
| block | `tcwildlands:schist_jade_ore` | 片岩玉矿石 | Schist Jade Ore | E1 | 围岩 schist；状态 grade |
| block | `tcwildlands:schist_native_gold_ore` | 片岩自然金矿石 | Schist Native Gold Ore | E1 | 围岩 schist；状态 grade |
| block | `tcwildlands:schist_rutile_ore` | 片岩金红石矿石 | Schist Rutile Ore | E8 | 围岩 schist；状态 grade |
| block | `tcwildlands:shale` | 页岩 | Shale | E0 | 沉积岩；原岩（6.1） |
| block | `tcwildlands:shale_bricks` | 页岩砖 | Shale Bricks | E0 | 建材形态 |
| block | `tcwildlands:shale_cobble` | 页岩圆石 | Shale Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:shale_hematite_ore` | 页岩赤铁矿矿石 | Shale Hematite Ore | E3 | 围岩 shale；状态 grade |
| block | `tcwildlands:shale_sphalerite_ore` | 页岩闪锌矿矿石 | Shale Sphalerite Ore | E4 | 围岩 shale；状态 grade |
| block | `tcwildlands:silt` | 粉砂 | Silt | E0 | 土壤与沉积物；湖相、河口细粒沉积 |
| block | `tcwildlands:slate` | 板岩 | Slate | E0 | 变质岩；原岩（6.1） |
| block | `tcwildlands:slate_bricks` | 板岩砖 | Slate Bricks | E0 | 建材形态 |
| block | `tcwildlands:slate_cobble` | 板岩圆石 | Slate Cobble | E0 | 原岩挖掘掉落 |
| block | `tcwildlands:slate_native_gold_ore` | 板岩自然金矿石 | Slate Native Gold Ore | E1 | 围岩 slate；状态 grade |
| block | `tcwildlands:steam_vent` | 蒸汽喷口 | Steam Vent | E8 | 深地；周期喷发 |
| block | `tcwildlands:steel_mine_arch` | 钢拱架 | Steel Mine Arch | E6 | 支护 +20 |
| block | `tcwildlands:terra_rossa` | 红色石灰土 | Terra Rossa | E0 | 土壤与沉积物；石灰岩风化 |
| block | `tcwildlands:till` | 冰碛土 | Till | E0 | 土壤与沉积物；冰川堆积；10% 掉落 loose_rock |
| block | `tcwildlands:timber_prop` | 木支柱 | Timber Prop | E2 | 支护 +10；配方 support/timber_prop |
| block | `tcwildlands:tuff_cinnabar_ore` | 凝灰岩辰砂矿石 | Tuff Cinnabar Ore | E4 | 围岩 tuff；状态 grade |
| block | `tcwildlands:tuff_cobble` | 凝灰岩圆石 | Tuff Cobble | E0 | 补齐原版缺失形态（7.1 第 2 条） |
| block | `tcwildlands:tuff_sulfur_ore` | 凝灰岩自然硫矿石 | Tuff Sulfur Ore | E5 | 围岩 tuff；状态 grade |
| block | `tcwildlands:volcanic_ash` | 火山灰 | Volcanic Ash | E0 | 土壤与沉积物；层状 1-8 层（5.14.3） |
| item | `tcwildlands:brine_bucket` | 卤水桶 | Brine Bucket | E1 |  |
| item | `tcwildlands:loose_rock` | 松散石块 | Loose Rock | E0 | 数据组件 tcwildlands:rock；4 个合成 _cobble |
| item | `tcwildlands:raw_amethyst` | 紫水晶原石 | Raw Amethyst | E2 | c:raw_materials/amethyst；切磨归 Industry |
| item | `tcwildlands:raw_anthracite` | 无烟煤 | Raw Anthracite | E6 | 燃料（6.6） |
| item | `tcwildlands:raw_bauxite` | 铝土矿原矿 | Raw Bauxite | E7 | c:raw_materials/bauxite |
| item | `tcwildlands:raw_bitumen` | 沥青 | Raw Bitumen | E1 | 来自 bitumen_seep |
| item | `tcwildlands:raw_bituminous_coal` | 烟煤 | Raw Bituminous Coal | E6 | 燃料（6.6） |
| item | `tcwildlands:raw_bog_iron` | 沼铁矿原矿 | Raw Bog Iron | E3 | c:raw_materials/bog_iron |
| item | `tcwildlands:raw_calamine` | 菱锌矿原矿 | Raw Calamine | E3 | c:raw_materials/calamine |
| item | `tcwildlands:raw_cassiterite` | 锡石原矿 | Raw Cassiterite | E2 | c:raw_materials/cassiterite |
| item | `tcwildlands:raw_chalcopyrite` | 黄铜矿原矿 | Raw Chalcopyrite | E3 | c:raw_materials/chalcopyrite |
| item | `tcwildlands:raw_chromite` | 铬铁矿原矿 | Raw Chromite | E7 | c:raw_materials/chromite |
| item | `tcwildlands:raw_cinnabar` | 辰砂原矿 | Raw Cinnabar | E4 | c:raw_materials/cinnabar |
| item | `tcwildlands:raw_cobaltite` | 辉砷钴矿原矿 | Raw Cobaltite | E9 | c:raw_materials/cobaltite |
| item | `tcwildlands:raw_diamond` | 金刚石原石 | Raw Diamond | E5 | c:raw_materials/diamond；切磨归 Industry |
| item | `tcwildlands:raw_emerald` | 祖母绿原石 | Raw Emerald | E4 | c:raw_materials/emerald；切磨归 Industry |
| item | `tcwildlands:raw_flint` | 燧石原矿 | Raw Flint | E0 | c:raw_materials/flint |
| item | `tcwildlands:raw_galena` | 方铅矿原矿 | Raw Galena | E2 | c:raw_materials/galena |
| item | `tcwildlands:raw_graphite` | 石墨原矿 | Raw Graphite | E5 | c:raw_materials/graphite |
| item | `tcwildlands:raw_hematite` | 赤铁矿原矿 | Raw Hematite | E3 | c:raw_materials/hematite |
| item | `tcwildlands:raw_ilmenite` | 钛铁矿原矿 | Raw Ilmenite | E8 | c:raw_materials/ilmenite |
| item | `tcwildlands:raw_jade` | 玉原石 | Raw Jade | E1 | c:raw_materials/jade；切磨归 Industry |
| item | `tcwildlands:raw_kaolinite` | 高岭土原矿 | Raw Kaolinite | E4 | c:raw_materials/kaolinite |
| item | `tcwildlands:raw_lapis` | 青金石原石 | Raw Lapis | E2 | c:raw_materials/lapis；切磨归 Industry |
| item | `tcwildlands:raw_lignite` | 褐煤 | Raw Lignite | E4 | 燃料（6.6） |
| item | `tcwildlands:raw_magnetite` | 磁铁矿原矿 | Raw Magnetite | E3 | c:raw_materials/magnetite |
| item | `tcwildlands:raw_malachite` | 孔雀石原矿 | Raw Malachite | E2 | c:raw_materials/malachite |
| item | `tcwildlands:raw_molybdenite` | 辉钼矿原矿 | Raw Molybdenite | E8 | c:raw_materials/molybdenite |
| item | `tcwildlands:raw_monazite` | 独居石原矿 | Raw Monazite | E9 | c:raw_materials/monazite |
| item | `tcwildlands:raw_native_copper` | 自然铜原矿 | Raw Native Copper | E1 | c:raw_materials/native_copper |
| item | `tcwildlands:raw_native_gold` | 自然金原矿 | Raw Native Gold | E1 | c:raw_materials/native_gold |
| item | `tcwildlands:raw_native_silver` | 自然银原矿 | Raw Native Silver | E2 | c:raw_materials/native_silver |
| item | `tcwildlands:raw_niter` | 硝石原矿 | Raw Niter | E5 | c:raw_materials/niter |
| item | `tcwildlands:raw_peat` | 泥炭 | Raw Peat | E1 | 燃料（6.6） |
| item | `tcwildlands:raw_pentlandite` | 镍黄铁矿原矿 | Raw Pentlandite | E7 | c:raw_materials/pentlandite |
| item | `tcwildlands:raw_phosphorite` | 磷块岩原矿 | Raw Phosphorite | E7 | c:raw_materials/phosphorite |
| item | `tcwildlands:raw_ruby` | 红宝石原石 | Raw Ruby | E4 | c:raw_materials/ruby；切磨归 Industry |
| item | `tcwildlands:raw_rutile` | 金红石原矿 | Raw Rutile | E8 | c:raw_materials/rutile |
| item | `tcwildlands:raw_sapphire` | 蓝宝石原石 | Raw Sapphire | E4 | c:raw_materials/sapphire；切磨归 Industry |
| item | `tcwildlands:raw_sphalerite` | 闪锌矿原矿 | Raw Sphalerite | E4 | c:raw_materials/sphalerite |
| item | `tcwildlands:raw_spodumene` | 锂辉石原矿 | Raw Spodumene | E9 | c:raw_materials/spodumene |
| item | `tcwildlands:raw_sulfur` | 自然硫原矿 | Raw Sulfur | E5 | c:raw_materials/sulfur |
| item | `tcwildlands:raw_sylvite` | 钾盐原矿 | Raw Sylvite | E7 | c:raw_materials/sylvite |
| item | `tcwildlands:raw_uraninite` | 沥青铀矿原矿 | Raw Uraninite | E8 | c:raw_materials/uraninite |
| item | `tcwildlands:raw_wolframite` | 黑钨矿原矿 | Raw Wolframite | E7 | c:raw_materials/wolframite |
| item | `tcwildlands:thermal_water_bucket` | 热水桶 | Thermal Water Bucket | E0 |  |
| entity | `tcwildlands:tornado` | 龙卷风 | Tornado | — | 环境实体，不可交互 |
| entity | `tcwildlands:volcanic_bomb` | 火山弹 | Volcanic Bomb | — | 抛射体（5.14.3） |
| fluid | `tcwildlands:brine` | 卤水 | Brine | E1 | 蒸发为 salt_crust；Industry 制盐、提锂 |
| fluid | `tcwildlands:floodwater` | 洪水 | Floodwater | — | 不可装桶；退去留下 flood_silt |
| fluid | `tcwildlands:thermal_water` | 热水 | Thermal Water | E0 | 40-95 °C；地热换热 |
| dimension | `tcwildlands:deep_earth` | 深地 | Deep Earth | — | 维度类型：minecraft:the_nether 使用（5.16） |
| dimension | `tcwildlands:earthlike` | 类地 | Earthlike | — | 维度类型：minecraft:overworld 使用（VO-WG-13） |
| biome | `tcwildlands:abyssal_trench` | 深海海沟 | Abyssal Trench | — | 组：海洋（6.3） |
| biome | `tcwildlands:alpine_glacier` | 高山冰川 | Alpine Glacier | — | 组：高地（6.3） |
| biome | `tcwildlands:alpine_meadow` | 高山草甸 | Alpine Meadow | — | 组：高地（6.3） |
| biome | `tcwildlands:alpine_rocky_peaks` | 高山裸岩 | Alpine Rocky Peaks | — | 组：高地（6.3） |
| biome | `tcwildlands:alpine_tundra` | 高山苔原 | Alpine Tundra | — | 组：高地（6.3） |
| biome | `tcwildlands:arid_badlands` | 恶地 | Arid Badlands | — | 组：干旱（6.3） |
| biome | `tcwildlands:arid_canyon` | 峡谷 | Arid Canyon | — | 组：干旱（6.3） |
| biome | `tcwildlands:arid_salt_flat` | 盐滩 | Arid Salt Flat | — | 组：干旱（6.3） |
| biome | `tcwildlands:arid_salt_lake` | 干旱盐湖 | Arid Salt Lake | — | 组：淡水（6.3） |
| biome | `tcwildlands:boreal_forest` | 北方针叶林 | Boreal Forest | — | 组：大陆（6.3） |
| biome | `tcwildlands:boreal_larch_forest` | 北方落叶松林 | Boreal Larch Forest | — | 组：大陆（6.3） |
| biome | `tcwildlands:boreal_peat_bog` | 北方泥炭沼 | Boreal Peat Bog | — | 组：湿地（6.3） |
| biome | `tcwildlands:cold_deep_ocean` | 寒带深海 | Cold Deep Ocean | — | 组：海洋（6.3） |
| biome | `tcwildlands:cold_desert` | 冷沙漠 | Cold Desert | — | 组：干旱（6.3） |
| biome | `tcwildlands:cold_fjord_coast` | 峡湾海岸 | Cold Fjord Coast | — | 组：海岸（6.3） |
| biome | `tcwildlands:cold_high_plateau` | 高寒高原 | Cold High Plateau | — | 组：高地（6.3） |
| biome | `tcwildlands:cold_lake` | 寒带湖泊 | Cold Lake | — | 组：淡水（6.3） |
| biome | `tcwildlands:cold_river` | 寒带河流 | Cold River | — | 组：淡水（6.3） |
| biome | `tcwildlands:cold_rocky_shore` | 寒带岩岸 | Cold Rocky Shore | — | 组：海岸（6.3） |
| biome | `tcwildlands:cold_shelf_sea` | 寒带陆架海 | Cold Shelf Sea | — | 组：海洋（6.3） |
| biome | `tcwildlands:cold_steppe` | 冷草原 | Cold Steppe | — | 组：干旱（6.3） |
| biome | `tcwildlands:deep_granite_batholith` | 深地花岗岩基 | Deep Granite Batholith | — | 组：深地（6.3） |
| biome | `tcwildlands:deep_hydrothermal_caverns` | 深地热液洞穴 | Deep Hydrothermal Caverns | — | 组：深地（6.3） |
| biome | `tcwildlands:deep_kimberlite_roots` | 深地金伯利岩根 | Deep Kimberlite Roots | — | 组：深地（6.3） |
| biome | `tcwildlands:deep_magma_chambers` | 深地岩浆房 | Deep Magma Chambers | — | 组：深地（6.3） |
| biome | `tcwildlands:deep_mantle_plume` | 深地地幔柱 | Deep Mantle Plume | — | 组：深地（6.3） |
| biome | `tcwildlands:hot_desert_oasis` | 沙漠绿洲 | Hot Desert Oasis | — | 组：干旱（6.3） |
| biome | `tcwildlands:hot_rocky_desert` | 热石漠 | Hot Rocky Desert | — | 组：干旱（6.3） |
| biome | `tcwildlands:hot_sand_desert` | 热沙漠 | Hot Sand Desert | — | 组：干旱（6.3） |
| biome | `tcwildlands:hot_steppe` | 热草原 | Hot Steppe | — | 组：干旱（6.3） |
| biome | `tcwildlands:humid_subtropical_forest` | 亚热带湿润林 | Humid Subtropical Forest | — | 组：温带（6.3） |
| biome | `tcwildlands:mediterranean_shrubland` | 地中海灌丛 | Mediterranean Shrubland | — | 组：温带（6.3） |
| biome | `tcwildlands:mediterranean_woodland` | 地中海林地 | Mediterranean Woodland | — | 组：温带（6.3） |
| biome | `tcwildlands:polar_deep_ocean` | 极地深海 | Polar Deep Ocean | — | 组：海洋（6.3） |
| biome | `tcwildlands:polar_ice_sheet` | 极地冰盖 | Polar Ice Sheet | — | 组：极地（6.3） |
| biome | `tcwildlands:polar_sea_ice` | 极地冰海 | Polar Sea Ice | — | 组：海洋（6.3） |
| biome | `tcwildlands:polar_tundra` | 极地苔原 | Polar Tundra | — | 组：极地（6.3） |
| biome | `tcwildlands:subtropical_karst_hills` | 亚热带喀斯特峰林 | Subtropical Karst Hills | — | 组：温带（6.3） |
| biome | `tcwildlands:subtropical_monsoon_forest` | 亚热带季风林 | Subtropical Monsoon Forest | — | 组：温带（6.3） |
| biome | `tcwildlands:temperate_broadleaf_forest` | 温带阔叶林 | Temperate Broadleaf Forest | — | 组：温带（6.3） |
| biome | `tcwildlands:temperate_deep_ocean` | 温带深海 | Temperate Deep Ocean | — | 组：海洋（6.3） |
| biome | `tcwildlands:temperate_delta` | 温带三角洲 | Temperate Delta | — | 组：淡水（6.3） |
| biome | `tcwildlands:temperate_floodplain` | 温带洪泛平原 | Temperate Floodplain | — | 组：淡水（6.3） |
| biome | `tcwildlands:temperate_grassland` | 温带草原 | Temperate Grassland | — | 组：大陆（6.3） |
| biome | `tcwildlands:temperate_lake` | 温带湖泊 | Temperate Lake | — | 组：淡水（6.3） |
| biome | `tcwildlands:temperate_marsh` | 温带沼泽 | Temperate Marsh | — | 组：湿地（6.3） |
| biome | `tcwildlands:temperate_mixed_forest` | 温带混交林 | Temperate Mixed Forest | — | 组：大陆（6.3） |
| biome | `tcwildlands:temperate_montane_forest` | 山地针叶林 | Temperate Montane Forest | — | 组：高地（6.3） |
| biome | `tcwildlands:temperate_rainforest` | 温带雨林 | Temperate Rainforest | — | 组：温带（6.3） |
| biome | `tcwildlands:temperate_river` | 温带河流 | Temperate River | — | 组：淡水（6.3） |
| biome | `tcwildlands:temperate_salt_marsh` | 温带盐沼 | Temperate Salt Marsh | — | 组：海岸（6.3） |
| biome | `tcwildlands:temperate_sandy_beach` | 温带沙滩 | Temperate Sandy Beach | — | 组：海岸（6.3） |
| biome | `tcwildlands:temperate_shelf_sea` | 温带陆架海 | Temperate Shelf Sea | — | 组：海洋（6.3） |
| biome | `tcwildlands:tropical_cloud_forest` | 热带云雾林 | Tropical Cloud Forest | — | 组：热带（6.3） |
| biome | `tcwildlands:tropical_coral_reef` | 热带珊瑚礁 | Tropical Coral Reef | — | 组：海洋（6.3） |
| biome | `tcwildlands:tropical_deep_ocean` | 热带深海 | Tropical Deep Ocean | — | 组：海洋（6.3） |
| biome | `tcwildlands:tropical_delta` | 热带三角洲 | Tropical Delta | — | 组：淡水（6.3） |
| biome | `tcwildlands:tropical_dry_forest` | 热带干旱林 | Tropical Dry Forest | — | 组：热带（6.3） |
| biome | `tcwildlands:tropical_floodplain` | 热带洪泛平原 | Tropical Floodplain | — | 组：淡水（6.3） |
| biome | `tcwildlands:tropical_mangrove_coast` | 热带红树林海岸 | Tropical Mangrove Coast | — | 组：海岸（6.3） |
| biome | `tcwildlands:tropical_monsoon_forest` | 热带季风林 | Tropical Monsoon Forest | — | 组：热带（6.3） |
| biome | `tcwildlands:tropical_rainforest` | 热带雨林 | Tropical Rainforest | — | 组：热带（6.3） |
| biome | `tcwildlands:tropical_river` | 热带河流 | Tropical River | — | 组：淡水（6.3） |
| biome | `tcwildlands:tropical_sandy_beach` | 热带沙滩 | Tropical Sandy Beach | — | 组：海岸（6.3） |
| biome | `tcwildlands:tropical_savanna` | 热带稀树草原 | Tropical Savanna | — | 组：热带（6.3） |
| biome | `tcwildlands:tropical_shelf_sea` | 热带陆架海 | Tropical Shelf Sea | — | 组：海洋（6.3） |
| biome | `tcwildlands:tropical_swamp` | 热带沼泽 | Tropical Swamp | — | 组：湿地（6.3） |
| biome | `tcwildlands:volcanic_geothermal_field` | 地热田 | Volcanic Geothermal Field | — | 组：火山（6.3） |
| biome | `tcwildlands:volcanic_lava_field` | 熔岩原 | Volcanic Lava Field | — | 组：火山（6.3） |
| structure | `tcwildlands:amethyst_geode` | 紫晶晶洞 | Amethyst Geode | — | 自然地貌（5.15） |
| structure | `tcwildlands:cenote` | 天坑 | Cenote | — | 自然地貌（5.15） |
| structure | `tcwildlands:deep_conduit` | 深地通道 | Deep Conduit | — | 自然地貌（5.15） |
| structure | `tcwildlands:fossil_bed` | 化石层 | Fossil Bed | — | 自然地貌（5.15） |
| structure | `tcwildlands:geyser_field` | 间歇泉田 | Geyser Field | — | 自然地貌（5.15） |
| structure | `tcwildlands:hoodoo_field` | 石林（土林） | Hoodoo Field | — | 自然地貌（5.15） |
| structure | `tcwildlands:hot_spring` | 温泉 | Hot Spring | — | 自然地貌（5.15） |
| structure | `tcwildlands:ice_cave` | 冰洞 | Ice Cave | — | 自然地貌（5.15） |
| structure | `tcwildlands:impact_crater` | 陨石坑 | Impact Crater | — | 自然地貌（5.15） |
| structure | `tcwildlands:karst_cave_system` | 喀斯特洞穴 | Karst Cave System | — | 自然地貌（5.15） |
| structure | `tcwildlands:kimberlite_pipe` | 金伯利岩管 | Kimberlite Pipe | — | 自然地貌（5.15） |
| structure | `tcwildlands:lava_tube` | 熔岩管 | Lava Tube | — | 自然地貌（5.15） |
| structure | `tcwildlands:natural_arch` | 天然拱 | Natural Arch | — | 自然地貌（5.15） |
| structure | `tcwildlands:sea_stack_cluster` | 海蚀柱群 | Sea Stack Cluster | — | 自然地貌（5.15） |
| structure | `tcwildlands:volcanic_vent` | 火山口 | Volcanic Vent | — | 自然地貌（5.15） |
| block_entity | `tcwildlands:deep_threshold` | 深地门槛 | Deep Threshold | — | 往返深地的计时与目标 |
| block_entity | `tcwildlands:geyser` | 间歇泉口 | Geyser | — | 喷发计时 |
| payload | `tcwildlands:calendar_sync` | 日历同步 | Calendar Sync | — | S→C；年、日、季节 |
| payload | `tcwildlands:flow_field` | 流场 | Flow Field | — | S→C；区块 L1 流场 |
| payload | `tcwildlands:hazard_state` | 灾害状态 | Hazard State | — | S→C；预警与进行中灾害 |
| payload | `tcwildlands:local_climate` | 本地气候 | Local Climate | — | S→C；HUD 控件数据 |
| payload | `tcwildlands:planet_preview` | 星球预览 | Planet Preview | — | S→C；10.4 |
| payload | `tcwildlands:planet_progress` | 星球进度 | Planet Progress | — | S→C；10.4，含追加阶段 |
| payload | `tcwildlands:prospect_result` | 勘探读数 | Prospect Result | — | S→C；ProspectReading 摘要 |
| payload | `tcwildlands:quake_shake` | 地震震动 | Quake Shake | — | S→C；震屏 |
| payload | `tcwildlands:region_summary` | 地区摘要 | Region Summary | — | S→C；地理页签快照 |
| payload | `tcwildlands:region_summary_request` | 地区摘要请求 | Region Summary Request | — | C→S；限速 4 次/秒 |
| payload | `tcwildlands:rock_tap_result` | 敲击结果 | Rock Tap Result | — | S→C；9.2 |
| payload | `tcwildlands:start_site_choice` | 出生地选择 | Start Site Choice | — | C→S；10.5 |
| payload | `tcwildlands:start_site_options` | 出生地候选 | Start Site Options | — | S→C；10.5 |
| payload | `tcwildlands:weather_cells` | 天气格 | Weather Cells | — | S→C；5 × 5 天气格 |
| payload | `tcwildlands:wrap_preload` | 接缝预载 | Wrap Preload | — | S→C；环绕接缝预取 |
| hud_widget | `tcwildlands:local_conditions` | 本地状况 | Local Conditions | — | 10.3 |
| civ_tab | `tcwildlands:geography` | 地理 | Geography | — | WORLD 组，order 600（10.2） |
| region_layer | `tcwildlands:climate` | 气候层 | Climate Layer | — | 11.4 |
| region_layer | `tcwildlands:geo` | 地质层 | Geology Layer | — | 11.4 |
| region_layer | `tcwildlands:hydro` | 水文层 | Hydrology Layer | — | 11.4 |
| sound | `tcwildlands:block.panning` | 淘洗 | Panning | — | 9.3 |
| sound | `tcwildlands:block.rock_tap` | 敲击岩石 | Rock Tap | — | 9.2 |
| sound | `tcwildlands:event.landslide` | 冒顶前兆 | Landslide Rumble | — | 5.10.6 前兆 |

### 5.5 生息 `tcecology`（190 项，来源 [11-ecology.md](11-ecology.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcecology:ash_leaves` | 白蜡树叶 | Ash Leaves | E0 | 树种 |
| block | `tcecology:ash_log` | 白蜡原木 | Ash Log | E0 | 可矮林 |
| block | `tcecology:ash_planks` | 白蜡木板 | Ash Planks | E0 | 木材套件 |
| block | `tcecology:ash_sapling` | 白蜡树苗 | Ash Sapling | E0 | 树种 |
| block | `tcecology:beaver_dam` | 河狸坝 | Beaver Dam | E0 | 河狸建造 |
| block | `tcecology:beech_leaves` | 山毛榉树叶 | Beech Leaves | E0 | 树种 |
| block | `tcecology:beech_log` | 山毛榉原木 | Beech Log | E0 | 树种 |
| block | `tcecology:beech_planks` | 山毛榉木板 | Beech Planks | E0 | 木材套件 |
| block | `tcecology:beech_sapling` | 山毛榉树苗 | Beech Sapling | E0 | 树种 |
| block | `tcecology:bird_blind` | 观鸟掩体 | Bird Blind | E5 | 隐蔽观察 |
| block | `tcecology:bramble` | 悬钩子 | Bramble | E0 | 灌丛阶段 |
| block | `tcecology:camera_trap` | 红外相机 | Camera Trap | E8 | 自动观察 |
| block | `tcecology:captive_breeding_pen` | 圈养繁育栏 | Captive Breeding Pen | E7 | 繁育与放归 |
| block | `tcecology:cattail` | 香蒲 | Cattail | E0 | 湿地 |
| block | `tcecology:cedar_leaves` | 雪松针叶 | Cedar Leaves | E0 | 树种 |
| block | `tcecology:cedar_log` | 雪松原木 | Cedar Log | E0 | 建筑造船 |
| block | `tcecology:cedar_planks` | 雪松木板 | Cedar Planks | E0 | 木材套件 |
| block | `tcecology:cedar_sapling` | 雪松树苗 | Cedar Sapling | E0 | 树种 |
| block | `tcecology:chestnut_leaves` | 栗树叶 | Chestnut Leaves | E0 | 树种 |
| block | `tcecology:chestnut_log` | 栗木原木 | Chestnut Log | E0 | 可矮林 |
| block | `tcecology:chestnut_planks` | 栗木木板 | Chestnut Planks | E0 | 木材套件 |
| block | `tcecology:chestnut_sapling` | 栗树苗 | Chestnut Sapling | E0 | 树种 |
| block | `tcecology:constructed_wetland` | 人工湿地 | Constructed Wetland | E9 | 水体净化 |
| block | `tcecology:coppice_stool` | 萌桩 | Coppice Stool | E4 | 矮林作业 |
| block | `tcecology:eucalyptus_leaves` | 桉树叶 | Eucalyptus Leaves | E0 | 树种 |
| block | `tcecology:eucalyptus_log` | 桉木原木 | Eucalyptus Log | E0 | 速生 |
| block | `tcecology:eucalyptus_planks` | 桉木木板 | Eucalyptus Planks | E0 | 木材套件 |
| block | `tcecology:eucalyptus_sapling` | 桉树苗 | Eucalyptus Sapling | E0 | 树种 |
| block | `tcecology:fireweed` | 柳兰 | Fireweed | E0 | 火烧地先锋 |
| block | `tcecology:fish_ladder` | 鱼道 | Fish Ladder | E6 | 恢复洄游 |
| block | `tcecology:fish_weir` | 鱼梁 | Fish Weir | E0 | 河道捕鱼 |
| block | `tcecology:forest_floor_moss` | 林地苔藓 | Forest Floor Moss | E0 | 原始林 |
| block | `tcecology:forest_stand_marker` | 林分标桩 | Forest Stand Marker | E6 | 蓄积与 AAC |
| block | `tcecology:gene_bank` | 基因库 | Gene Bank | E9 | 样本存储与复原 |
| block | `tcecology:green_foxtail` | 狗尾草 | Green Foxtail | E0 | 粟祖先 |
| block | `tcecology:hazel_shrub` | 榛灌丛 | Hazel Shrub | E0 | 灌丛阶段 |
| block | `tcecology:larch_leaves` | 落叶松针 | Larch Leaves | E0 | 秋季变黄脱落 |
| block | `tcecology:larch_log` | 落叶松原木 | Larch Log | E0 | 先锋树种 |
| block | `tcecology:larch_planks` | 落叶松木板 | Larch Planks | E0 | 木材套件 |
| block | `tcecology:larch_sapling` | 落叶松树苗 | Larch Sapling | E0 | 树种 |
| block | `tcecology:maple_leaves` | 槭树叶 | Maple Leaves | E0 | 树种 |
| block | `tcecology:maple_log` | 槭木原木 | Maple Log | E0 | 树种 |
| block | `tcecology:maple_planks` | 槭木木板 | Maple Planks | E0 | 木材套件 |
| block | `tcecology:maple_sapling` | 槭树苗 | Maple Sapling | E0 | 树种 |
| block | `tcecology:nest_box` | 巢箱 | Nest Box | E3 | 鸟类承载力 |
| block | `tcecology:nettle` | 荨麻 | Nettle | E0 | 纤维 |
| block | `tcecology:pine_leaves` | 松针 | Pine Leaves | E0 | 树种 |
| block | `tcecology:pine_log` | 松木原木 | Pine Log | E0 | 先锋树种 |
| block | `tcecology:pine_planks` | 松木木板 | Pine Planks | E0 | 木材套件 |
| block | `tcecology:pine_sapling` | 松树苗 | Pine Sapling | E0 | 树种 |
| block | `tcecology:pit_trap` | 陷坑 | Pit Trap | E0 | 中大型猎物陷阱 |
| block | `tcecology:reserve_marker` | 保护界碑 | Reserve Marker | E3 | 定义保护区 |
| block | `tcecology:rubber_tree_leaves` | 橡胶树叶 | Rubber Tree Leaves | E0 | 树种 |
| block | `tcecology:rubber_tree_log` | 橡胶树原木 | Rubber Tree Log | E0 | 可割胶 |
| block | `tcecology:rubber_tree_planks` | 橡胶木板 | Rubber Tree Planks | E0 | 木材套件 |
| block | `tcecology:rubber_tree_sapling` | 橡胶树苗 | Rubber Tree Sapling | E0 | 树种 |
| block | `tcecology:salt_lick` | 舔盐块 | Salt Lick | E1 | 吸引食草动物 |
| block | `tcecology:sea_beet` | 海甜菜 | Sea Beet | E0 | 甜菜祖先 |
| block | `tcecology:snare` | 套索 | Snare | E0 | 小型猎物陷阱 |
| block | `tcecology:teak_leaves` | 柚木树叶 | Teak Leaves | E0 | 树种 |
| block | `tcecology:teak_log` | 柚木原木 | Teak Log | E0 | 造船 |
| block | `tcecology:teak_planks` | 柚木木板 | Teak Planks | E0 | 木材套件 |
| block | `tcecology:teak_sapling` | 柚木树苗 | Teak Sapling | E0 | 树种 |
| block | `tcecology:telemetry_receiver` | 遥测接收站 | Telemetry Receiver | E8 | 项圈信号 |
| block | `tcecology:teosinte` | 大刍草 | Teosinte | E0 | 玉米祖先 |
| block | `tcecology:terrarium` | 生态缸 | Terrarium | E10 | 生物圈模板 |
| block | `tcecology:tree_nursery` | 苗圃 | Tree Nursery | E6 | 培育树苗 |
| block | `tcecology:walnut_leaves` | 胡桃树叶 | Walnut Leaves | E0 | 树种 |
| block | `tcecology:walnut_log` | 胡桃原木 | Walnut Log | E0 | 树种 |
| block | `tcecology:walnut_planks` | 胡桃木板 | Walnut Planks | E0 | 木材套件 |
| block | `tcecology:walnut_sapling` | 胡桃树苗 | Walnut Sapling | E0 | 树种 |
| block | `tcecology:wild_barley` | 野大麦 | Wild Barley | E0 | 作物祖先 |
| block | `tcecology:wild_bean` | 野生菜豆 | Wild Bean | E0 | 作物祖先 |
| block | `tcecology:wild_cabbage` | 野甘蓝 | Wild Cabbage | E0 | 作物祖先 |
| block | `tcecology:wild_carrot` | 野胡萝卜 | Wild Carrot | E0 | 作物祖先 |
| block | `tcecology:wild_cotton` | 野棉 | Wild Cotton | E0 | 作物祖先 |
| block | `tcecology:wild_einkorn` | 野生一粒小麦 | Wild Einkorn | E0 | 作物祖先 |
| block | `tcecology:wild_emmer` | 野生二粒小麦 | Wild Emmer | E0 | 小麦祖先 |
| block | `tcecology:wild_flax` | 野亚麻 | Wild Flax | E0 | 作物祖先 |
| block | `tcecology:wild_garlic` | 熊葱 | Wild Garlic | E0 | 林下 |
| block | `tcecology:wild_grape` | 野葡萄 | Wild Grape | E0 | 作物祖先 |
| block | `tcecology:wild_lentil` | 野生小扁豆 | Wild Lentil | E0 | 作物祖先 |
| block | `tcecology:wild_potato` | 野生马铃薯 | Wild Potato | E0 | 作物祖先 |
| block | `tcecology:wild_rice` | 普通野生稻 | Wild Rice | E0 | 作物祖先 |
| block | `tcecology:wild_sorghum` | 野高粱 | Wild Sorghum | E0 | 作物祖先 |
| block | `tcecology:wild_squash` | 野生南瓜 | Wild Squash | E0 | 作物祖先 |
| block | `tcecology:wildlife_crossing_marker` | 动物通道标志 | Wildlife Crossing Marker | E7 | 恢复连通 |
| block | `tcecology:willow_leaves` | 柳树叶 | Willow Leaves | E0 | 树种 |
| block | `tcecology:willow_log` | 柳木原木 | Willow Log | E0 | 可矮林 |
| block | `tcecology:willow_planks` | 柳木木板 | Willow Planks | E0 | 木材套件 |
| block | `tcecology:willow_sapling` | 柳树苗 | Willow Sapling | E0 | 树种 |
| item | `tcecology:antler` | 鹿角 | Antler | E0 | 骨角器原料 |
| item | `tcecology:biopsy_kit` | 活检工具 | Biopsy Kit | E8 | 取组织样本 |
| item | `tcecology:blubber` | 鲸脂 | Blubber | E5 | 炼油原料 |
| item | `tcecology:capture_net` | 捕获网 | Capture Net | E0 | 活捉 |
| item | `tcecology:coppice_pole` | 矮林细杆 | Coppice Pole | E4 | 燃料、编织 |
| item | `tcecology:fur_pelt` | 毛皮 | Fur Pelt | E0 | 带物种组件 |
| item | `tcecology:ivory` | 象牙 | Ivory | E0 | 贸易品 |
| item | `tcecology:latex` | 胶乳 | Latex | E6 | 橡胶原料 |
| item | `tcecology:liquid_nitrogen_flask` | 液氮瓶 | Liquid Nitrogen Flask | E9 | 基因库补给 |
| item | `tcecology:native_seed_mix` | 乡土种子混合物 | Native Seed Mix | E9 | 加速演替 |
| item | `tcecology:radio_collar` | 无线电项圈 | Radio Collar | E8 | 个体追踪 |
| item | `tcecology:spoor_sample` | 粪便样本 | Spoor Sample | E0 | 观察与实验 |
| item | `tcecology:tissue_sample` | 组织样本 | Tissue Sample | E8 | 基因救援 |
| entity | `tcecology:aurochs` | 原牛 | Aurochs | E0 | 孑遗，牛的祖先 |
| entity | `tcecology:barn_swallow` | 家燕 | Barn Swallow | E1 | 害虫控制 |
| entity | `tcecology:beaver` | 河狸 | Beaver | E0 | 关键种，筑坝 |
| entity | `tcecology:bezoar_ibex` | 野山羊 | Bezoar Ibex | E0 | 山羊祖先 |
| entity | `tcecology:bison` | 野牛 | Bison | E0 | 草原，关键种 |
| entity | `tcecology:brown_bear` | 棕熊 | Brown Bear | E0 | 大型捕食者 |
| entity | `tcecology:brown_rat` | 褐家鼠 | Brown Rat | E1 | 储粮害兽 |
| entity | `tcecology:brown_trout` | 褐鳟 | Brown Trout | E0 | 水质指示 |
| entity | `tcecology:common_carp` | 鲤鱼 | Common Carp | E0 | 静水 |
| entity | `tcecology:common_crane` | 灰鹤 | Common Crane | E0 | 迁飞 |
| entity | `tcecology:elephant` | 象 | Elephant | E0 | 象牙 |
| entity | `tcecology:european_eel` | 鳗鲡 | European Eel | E0 | 降海洄游 |
| entity | `tcecology:gazelle` | 瞪羚 | Gazelle | E0 | 荒漠草原 |
| entity | `tcecology:golden_eagle` | 金雕 | Golden Eagle | E0 | 鹰猎 |
| entity | `tcecology:great_auk` | 大海雀 | Great Auk | E0 | 孑遗 |
| entity | `tcecology:grey_heron` | 苍鹭 | Grey Heron | E0 | 湿地 |
| entity | `tcecology:grey_seal` | 灰海豹 | Grey Seal | E0 | 海岸 |
| entity | `tcecology:greylag_goose` | 灰雁 | Greylag Goose | E0 | 家鹅祖先，迁飞 |
| entity | `tcecology:grouse` | 松鸡 | Grouse | E0 | 北方林 |
| entity | `tcecology:guanaco` | 原驼 | Guanaco | E0 | 羊驼祖先 |
| entity | `tcecology:hedgehog` | 刺猬 | Hedgehog | E0 | 城市适应者 |
| entity | `tcecology:herring` | 鲱鱼 | Herring | E0 | 鱼群 |
| entity | `tcecology:island_dodo` | 岛鸠 | Island Dodo | E0 | 孑遗 |
| entity | `tcecology:lion` | 狮 | Lion | E0 | 顶级捕食者 |
| entity | `tcecology:lynx` | 猞猁 | Lynx | E0 | 捕食者 |
| entity | `tcecology:mallard` | 绿头鸭 | Mallard | E0 | 家鸭祖先 |
| entity | `tcecology:marsupial_wolf` | 袋狼样兽 | Marsupial Wolf | E0 | 孑遗 |
| entity | `tcecology:moose` | 驼鹿 | Moose | E0 | 北方林湿地 |
| entity | `tcecology:mouflon` | 盘羊 | Mouflon | E0 | 绵羊祖先 |
| entity | `tcecology:osprey` | 鱼鹰 | Osprey | E0 | DDT 指示 |
| entity | `tcecology:otter` | 水獭 | Otter | E0 | 污染指示种 |
| entity | `tcecology:passenger_pigeon` | 旅鸽 | Passenger Pigeon | E0 | 孑遗 |
| entity | `tcecology:peregrine_falcon` | 游隼 | Peregrine Falcon | E0 | 鹰猎，DDT 指示 |
| entity | `tcecology:pheasant` | 雉 | Pheasant | E0 | 林缘 |
| entity | `tcecology:pine_marten` | 松貂 | Pine Marten | E0 | 毛皮 |
| entity | `tcecology:red_deer` | 马鹿 | Red Deer | E0 | 温带林 |
| entity | `tcecology:red_junglefowl` | 原鸡 | Red Junglefowl | E0 | 鸡的祖先 |
| entity | `tcecology:red_squirrel` | 红松鼠 | Red Squirrel | E0 | 种子传播 |
| entity | `tcecology:reindeer` | 驯鹿 | Reindeer | E0 | 苔原，迁徙 |
| entity | `tcecology:right_whale` | 露脊鲸 | Right Whale | E0 | 捕鲸 |
| entity | `tcecology:roe_deer` | 狍 | Roe Deer | E0 | 林缘 |
| entity | `tcecology:saiga` | 高鼻羚羊 | Saiga | E0 | 干草原 |
| entity | `tcecology:sturgeon` | 鲟鱼 | Sturgeon | E0 | 洄游 |
| entity | `tcecology:tiger` | 虎 | Tiger | E0 | 顶级捕食者 |
| entity | `tcecology:wild_ass` | 野驴 | Wild Ass | E0 | 驴的祖先 |
| entity | `tcecology:wild_boar` | 野猪 | Wild Boar | E0 | 猪的祖先 |
| entity | `tcecology:wild_horse` | 野马 | Wild Horse | E0 | 马的祖先 |
| entity | `tcecology:wild_turkey` | 野火鸡 | Wild Turkey | E0 | 火鸡祖先 |
| entity | `tcecology:wild_water_buffalo` | 野水牛 | Wild Water Buffalo | E0 | 水牛祖先 |
| entity | `tcecology:wild_yak` | 野牦牛 | Wild Yak | E0 | 牦牛祖先 |
| entity | `tcecology:wildebeest` | 角马 | Wildebeest | E0 | 大迁徙 |
| entity | `tcecology:woolly_mammoth` | 猛犸象 | Woolly Mammoth | E0 | 孑遗 |
| entity | `tcecology:zebra` | 斑马 | Zebra | E0 | 稀树草原 |
| structure | `tcecology:animal_den` | 兽穴 | Animal Den | E0 | 熊、狼巢穴 |
| structure | `tcecology:beaver_lodge` | 河狸巢 | Beaver Lodge | E0 | 河湖 |
| block_entity | `tcecology:camera_trap` | 红外相机 | Camera Trap | E8 | 记录 |
| block_entity | `tcecology:captive_breeding_pen` | 圈养繁育栏 | Captive Breeding Pen | E7 | 个体与 Hdiv |
| block_entity | `tcecology:fish_weir` | 鱼梁 | Fish Weir | E0 | 3 格物品栏 |
| block_entity | `tcecology:forest_stand_marker` | 林分标桩 | Forest Stand Marker | E6 | 蓄积缓存 |
| block_entity | `tcecology:gene_bank` | 基因库 | Gene Bank | E9 | 样本 |
| block_entity | `tcecology:pit_trap` | 陷坑 | Pit Trap | E0 | 结算状态 |
| block_entity | `tcecology:snare` | 套索 | Snare | E0 | 结算状态 |
| block_entity | `tcecology:telemetry_receiver` | 遥测接收站 | Telemetry Receiver | E8 | 覆盖范围 |
| block_entity | `tcecology:tree_nursery` | 苗圃 | Tree Nursery | E6 | 培育进度 |
| menu | `tcecology:captive_breeding_pen` | 圈养繁育栏 | Captive Breeding Pen | E7 | 配对、放归 |
| menu | `tcecology:fish_weir` | 鱼梁 | Fish Weir | E0 | 收取渔获 |
| menu | `tcecology:gene_bank` | 基因库 | Gene Bank | E9 | 样本与复原 |
| menu | `tcecology:tree_nursery` | 苗圃 | Tree Nursery | E6 | 树苗培育 |
| payload | `tcecology:burn_confirm` | 计划烧除确认 | Burn Confirm | — | C2S |
| payload | `tcecology:codex_sighting` | 目击上报 | Codex Sighting | — | C2S |
| payload | `tcecology:codex_update` | 图鉴更新 | Codex Update | — | S2C |
| payload | `tcecology:collar_update` | 项圈位置 | Collar Update | — | S2C |
| payload | `tcecology:pen_action` | 繁育栏操作 | Pen Action | — | C2S |
| payload | `tcecology:reserve_edit` | 保护区编辑 | Reserve Edit | — | C2S |
| payload | `tcecology:track_request` | 痕迹请求 | Track Request | — | C2S |
| payload | `tcecology:track_sync` | 痕迹同步 | Track Sync | — | S2C |
| hud_widget | `tcecology:ecosystem_health` | 生态健康 | Ecosystem Health | E0 | 扩展等级，左上 |
| hud_widget | `tcecology:tracking` | 追踪 | Tracking | E0 | 标准等级，追踪姿态时出现 |
| civ_tab | `tcecology:ecology` | 生态 | Ecology | E0 | 文明面板 WORLD 组，排序 650 |
| region_layer | `tcecology:ecosystem` | 生态层 | Ecosystem Layer | — | 种群、兽群、土地覆盖、干扰、毒性、EHI |
| sound | `tcecology:ambient.crane_call` | 鹤唳 | Crane Call | — | 迁飞季 |
| sound | `tcecology:ambient.dawn_chorus` | 晨鸣合唱 | Dawn Chorus | — | 强度随鸟类丰度 |
| sound | `tcecology:ambient.deer_rut` | 鹿鸣 | Deer Rut | — | 秋季发情 |
| sound | `tcecology:ambient.frog_chorus` | 蛙鸣 | Frog Chorus | — | 湿地夜晚 |
| sound | `tcecology:ambient.wolf_howl` | 狼嚎 | Wolf Howl | — | 远处狼群 |
| sound | `tcecology:entity.generic_wild.flee` | 惊逃 | Wildlife Flee | — | 兽群受惊 |

### 5.6 田园 `tccultivation`（200 项，来源 [12-cultivation.md](12-cultivation.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tccultivation:acclimatization_garden` | 引种园 | Acclimatization Garden | E5 |  |
| block | `tccultivation:barley` | 大麦 | Barley | E1 | 作物 |
| block | `tccultivation:beetroot` | 甜菜根 | Beetroot | E1 | 作物；替换原版 |
| block | `tccultivation:cabbage` | 甘蓝 | Cabbage | E1 | 作物 |
| block | `tccultivation:carrot` | 胡萝卜 | Carrot | E1 | 作物；替换原版 |
| block | `tccultivation:cassava` | 木薯 | Cassava | E2 | 作物 |
| block | `tccultivation:chain_pump` | 龙骨车 | Chain Pump | E3 |  |
| block | `tccultivation:cheese_press` | 奶酪压榨机 | Cheese Press | E1 |  |
| block | `tccultivation:churn` | 搅乳器 | Butter Churn | E1 |  |
| block | `tccultivation:cocoa` | 可可 | Cocoa | E2 | 作物；替换原版 |
| block | `tccultivation:coffee` | 咖啡 | Coffee | E5 | 多年生 |
| block | `tccultivation:common_bean` | 菜豆 | Common Bean | E1 | 作物 |
| block | `tccultivation:compost_bin` | 堆肥箱 | Compost Bin | E3 |  |
| block | `tccultivation:cotton` | 棉花 | Cotton | E2 | 作物 |
| block | `tccultivation:deep_freezer` | 冷冻柜 | Deep Freezer | E7 |  |
| block | `tccultivation:diversion_weir` | 拦河堰 | Diversion Weir | E3 |  |
| block | `tccultivation:drip_line` | 滴灌带 | Drip Line | E9 |  |
| block | `tccultivation:drying_rack` | 晾架 | Drying Rack | E0 |  |
| block | `tccultivation:einkorn` | 一粒小麦 | Einkorn | E1 | 作物 |
| block | `tccultivation:emmer_wheat` | 二粒小麦 | Emmer Wheat | E1 | 作物 |
| block | `tccultivation:feed_trough` | 饲槽 | Feed Trough | E1 |  |
| block | `tccultivation:field_sensor` | 田间传感器 | Field Sensor | E9 |  |
| block | `tccultivation:fish_pond_marker` | 鱼塘标记 | Fish Pond Marker | E3 |  |
| block | `tccultivation:flax` | 亚麻 | Flax | E1 | 作物 |
| block | `tccultivation:foxtail_millet` | 粟 | Foxtail Millet | E1 | 作物 |
| block | `tccultivation:gene_editing_station` | 基因编辑台 | Gene Editing Station | E9 |  |
| block | `tccultivation:grain_pit` | 粮窖 | Grain Pit | E1 |  |
| block | `tccultivation:grape` | 葡萄 | Grape | E2 | 多年生 |
| block | `tccultivation:grow_rack` | 水培架 | Grow Rack | E10 |  |
| block | `tccultivation:guano_deposit` | 海鸟粪矿 | Guano Deposit | E5 | 世界生成 |
| block | `tccultivation:hedgerow` | 树篱 | Hedgerow | E1 | #tccultivation:hedgerows |
| block | `tccultivation:hemp` | 大麻 | Hemp | E1 | 作物 |
| block | `tccultivation:ice_chest` | 冰柜 | Ice Chest | E4 |  |
| block | `tccultivation:irrigation_ditch` | 灌渠 | Irrigation Ditch | E2 | lined 状态 |
| block | `tccultivation:lentil` | 小扁豆 | Lentil | E1 | 作物 |
| block | `tccultivation:maize` | 玉米 | Maize | E1 | 作物 |
| block | `tccultivation:manure_heap` | 粪堆 | Manure Heap | E1 |  |
| block | `tccultivation:melon_stem` | 西瓜茎 | Melon Stem | E2 | 作物；结原版西瓜 |
| block | `tccultivation:mulberry` | 桑 | Mulberry | E2 | 多年生 |
| block | `tccultivation:noria` | 筒车 | Noria | E3 |  |
| block | `tccultivation:nutrient_tank` | 营养液箱 | Nutrient Tank | E10 |  |
| block | `tccultivation:oats` | 燕麦 | Oats | E3 | 作物 |
| block | `tccultivation:olive` | 橄榄 | Olive | E2 | 多年生 |
| block | `tccultivation:onion` | 洋葱 | Onion | E2 | 作物 |
| block | `tccultivation:paddy_field` | 水田 | Paddy Field | E2 | 耕地变体 |
| block | `tccultivation:pea` | 豌豆 | Pea | E1 | 作物 |
| block | `tccultivation:potato` | 马铃薯 | Potato | E1 | 作物；替换原版 |
| block | `tccultivation:pumpkin_stem` | 南瓜茎 | Pumpkin Stem | E1 | 作物；结原版南瓜 |
| block | `tccultivation:raised_bed` | 高畦 | Raised Bed | E3 | 耕地变体 |
| block | `tccultivation:rat_proof_bin` | 防鼠粮仓 | Rat-proof Bin | E3 |  |
| block | `tccultivation:refrigerator` | 冰箱 | Refrigerator | E7 |  |
| block | `tccultivation:rice` | 稻 | Rice | E1 | 作物 |
| block | `tccultivation:rye` | 黑麦 | Rye | E2 | 作物 |
| block | `tccultivation:saline_soil` | 盐碱土 | Saline Soil | — | 退化土 |
| block | `tccultivation:scarecrow` | 稻草人 | Scarecrow | E1 | 鸟害 −50% |
| block | `tccultivation:seed_sorting_tray` | 选种台 | Seed Sorting Tray | E1 |  |
| block | `tccultivation:seed_vault` | 种质库 | Seed Vault | E8 |  |
| block | `tccultivation:shadoof` | 桔槔 | Shadoof | E3 |  |
| block | `tccultivation:silkworm_tray` | 蚕匾 | Silkworm Tray | E2 |  |
| block | `tccultivation:sluice_gate` | 水闸 | Sluice Gate | E2 |  |
| block | `tccultivation:smokehouse` | 烟熏房 | Smokehouse | E2 |  |
| block | `tccultivation:sorghum` | 高粱 | Sorghum | E1 | 作物 |
| block | `tccultivation:soybean` | 大豆 | Soybean | E2 | 作物 |
| block | `tccultivation:sprinkler` | 喷灌头 | Sprinkler | E8 |  |
| block | `tccultivation:sugar_cane` | 甘蔗 | Sugar Cane | E2 | 作物；替换原版 |
| block | `tccultivation:sunflower` | 向日葵 | Sunflower | E5 | 作物 |
| block | `tccultivation:sweet_berry_bush` | 甜浆果丛 | Sweet Berry Bush | E0 | 作物；替换原版 |
| block | `tccultivation:sweet_potato` | 甘薯 | Sweet Potato | E2 | 作物 |
| block | `tccultivation:tea` | 茶 | Tea | E2 | 多年生 |
| block | `tccultivation:tile_drain` | 暗管 | Tile Drain | E4 |  |
| block | `tccultivation:tree_tap` | 树液采集器 | Tree Tap | E5 | 装在 tcecology:maple_log |
| block | `tccultivation:tube_well` | 管井 | Tube Well | E8 |  |
| block | `tccultivation:wheat` | 小麦 | Wheat | E2 | 作物；替换原版 minecraft:wheat |
| item | `tccultivation:agricultural_lime` | 农用石灰 | Agricultural Lime | E3 |  |
| item | `tccultivation:barley` | 大麦 | Barley | E1 | 收获物 |
| item | `tccultivation:barley_seeds` | 大麦种子 | Barley Seeds | E1 |  |
| item | `tccultivation:beer` | 啤酒 | Beer | E1 |  |
| item | `tccultivation:bordeaux_mixture` | 波尔多液 | Bordeaux Mixture | E6 |  |
| item | `tccultivation:butter` | 黄油 | Butter | E1 |  |
| item | `tccultivation:cabbage` | 甘蓝 | Cabbage | E1 | 收获物 |
| item | `tccultivation:cabbage_seeds` | 甘蓝种子 | Cabbage Seeds | E1 |  |
| item | `tccultivation:carp` | 鲤鱼 | Carp | E3 |  |
| item | `tccultivation:cassava` | 木薯 | Cassava | E2 | 可种植；需熟制 |
| item | `tccultivation:cheese_wheel` | 奶酪轮 | Cheese Wheel | E1 | 可陈化 |
| item | `tccultivation:coffee` | 咖啡果 | Coffee Cherry | E5 | 收获物 |
| item | `tccultivation:coffee_beans` | 咖啡豆 | Coffee Beans | E5 | 特产 |
| item | `tccultivation:coffee_seedling` | 咖啡苗 | Coffee Seedling | E5 |  |
| item | `tccultivation:common_bean` | 菜豆 | Common Bean | E1 | 收获物 |
| item | `tccultivation:common_bean_seeds` | 菜豆种子 | Common Bean Seeds | E1 |  |
| item | `tccultivation:compost` | 堆肥 | Compost | E3 |  |
| item | `tccultivation:cooked_carp` | 熟鲤鱼 | Cooked Carp | E3 |  |
| item | `tccultivation:cooked_game_bird` | 熟野禽 | Cooked Game Bird | E0 |  |
| item | `tccultivation:cooked_venison` | 熟鹿肉 | Cooked Venison | E0 |  |
| item | `tccultivation:cotton` | 棉花 | Cotton | E2 | #c:crops/cotton |
| item | `tccultivation:cotton_seeds` | 棉籽 | Cotton Seeds | E2 |  |
| item | `tccultivation:cured_tea` | 成品茶 | Cured Tea | E2 | 特产 |
| item | `tccultivation:dough` | 面团 | Dough | E3 | 熔炉/烟熏炉 → 原版面包 |
| item | `tccultivation:dried_fish` | 鱼干 | Dried Fish | E0 |  |
| item | `tccultivation:dried_fruit` | 干果 | Dried Fruit | E0 |  |
| item | `tccultivation:dried_meat` | 肉干 | Dried Meat | E0 |  |
| item | `tccultivation:einkorn` | 一粒小麦 | Einkorn | E1 | 收获物 |
| item | `tccultivation:einkorn_seeds` | 一粒小麦种子 | Einkorn Seeds | E1 |  |
| item | `tccultivation:emmer_wheat` | 二粒小麦 | Emmer Wheat | E1 | 收获物（与方块同 ID，下同） |
| item | `tccultivation:emmer_wheat_seeds` | 二粒小麦种子 | Emmer Wheat Seeds | E1 | 带 seed_lot（下同） |
| item | `tccultivation:fish_sauce` | 鱼露 | Fish Sauce | E2 |  |
| item | `tccultivation:flax` | 亚麻 | Flax | E1 | #c:crops/flax |
| item | `tccultivation:flax_seeds` | 亚麻籽 | Flax Seeds | E1 |  |
| item | `tccultivation:flour` | 面粉 | Flour | E3 | Industry 石磨产物（tcindustry:milling 配方由本模组提供） |
| item | `tccultivation:foxtail_millet` | 粟 | Foxtail Millet | E1 | 收获物 |
| item | `tccultivation:foxtail_millet_seeds` | 粟种 | Foxtail Millet Seeds | E1 |  |
| item | `tccultivation:fruit_preserves` | 果酱 | Fruit Preserves | E2 |  |
| item | `tccultivation:game_bird` | 生野禽 | Raw Game Bird | E0 |  |
| item | `tccultivation:grape` | 葡萄 | Grape | E2 | 收获物 |
| item | `tccultivation:grape_cutting` | 葡萄插条 | Grape Cutting | E2 |  |
| item | `tccultivation:guano` | 海鸟粪 | Guano | E5 |  |
| item | `tccultivation:hay` | 干草 | Hay | E1 | 9 个 ↔ minecraft:hay_block；tcnetworks:fodder |
| item | `tccultivation:hemp` | 大麻 | Hemp | E1 | #c:crops/hemp |
| item | `tccultivation:hemp_seeds` | 大麻籽 | Hemp Seeds | E1 |  |
| item | `tccultivation:herbicide` | 除草剂 | Herbicide | E8 |  |
| item | `tccultivation:herd_book` | 畜群登记簿 | Herd Book | E3 |  |
| item | `tccultivation:herding_crook` | 牧杖 | Herding Crook | E1 |  |
| item | `tccultivation:implement` | 农具 | Farm Implement | E2-E7 | 放置为同名实体；组件 implement_kind 取 ard / heavy_plough / seed_drill / reaper / combine（6.5） |
| item | `tccultivation:lentil` | 小扁豆 | Lentil | E1 | 收获物 |
| item | `tccultivation:lentil_seeds` | 小扁豆种子 | Lentil Seeds | E1 |  |
| item | `tccultivation:maize` | 玉米 | Maize | E1 | 收获物 |
| item | `tccultivation:maize_seeds` | 玉米种子 | Maize Seeds | E1 |  |
| item | `tccultivation:manure` | 粪肥 | Manure | E1 |  |
| item | `tccultivation:maple_sap` | 枫树汁 | Maple Sap | E5 |  |
| item | `tccultivation:maple_syrup` | 枫糖浆 | Maple Syrup | E5 |  |
| item | `tccultivation:miso` | 味噌 | Miso | E2 |  |
| item | `tccultivation:mulberry` | 桑叶 | Mulberry Leaves | E2 | 蚕饲料 |
| item | `tccultivation:mulberry_cutting` | 桑插条 | Mulberry Cutting | E2 |  |
| item | `tccultivation:npk_fertilizer` | 复合肥 | NPK Fertilizer | E8 |  |
| item | `tccultivation:oats` | 燕麦 | Oats | E3 | 收获物 |
| item | `tccultivation:oats_seeds` | 燕麦种子 | Oat Seeds | E3 |  |
| item | `tccultivation:olive` | 橄榄 | Olive | E2 | #tccultivation:oilseeds |
| item | `tccultivation:olive_sapling` | 橄榄苗 | Olive Sapling | E2 |  |
| item | `tccultivation:onion` | 洋葱 | Onion | E2 | 可种植 |
| item | `tccultivation:pasture_seed` | 牧草种 | Pasture Seed | E3 |  |
| item | `tccultivation:pea` | 豌豆 | Pea | E1 | 收获物 |
| item | `tccultivation:pea_seeds` | 豌豆种子 | Pea Seeds | E1 |  |
| item | `tccultivation:pesticide` | 杀虫剂 | Pesticide | E8 |  |
| item | `tccultivation:pond_silt` | 塘泥 | Pond Silt | E3 | 肥料 |
| item | `tccultivation:rennet` | 凝乳酶 | Rennet | E1 |  |
| item | `tccultivation:rice` | 稻谷 | Rice | E1 | 收获物 |
| item | `tccultivation:rice_seeds` | 稻种 | Rice Seeds | E1 |  |
| item | `tccultivation:rice_wine` | 米酒 | Rice Wine | E1 |  |
| item | `tccultivation:rye` | 黑麦 | Rye | E2 | 收获物 |
| item | `tccultivation:rye_seeds` | 黑麦种子 | Rye Seeds | E2 |  |
| item | `tccultivation:salted_fish` | 咸鱼 | Salted Fish | E2 |  |
| item | `tccultivation:salted_meat` | 咸肉 | Salted Meat | E2 |  |
| item | `tccultivation:sauerkraut` | 酸菜 | Sauerkraut | E1 |  |
| item | `tccultivation:silk_cocoon` | 蚕茧 | Silk Cocoon | E2 | #c:silk_cocoons |
| item | `tccultivation:smoked_fish` | 熏鱼 | Smoked Fish | E0 |  |
| item | `tccultivation:smoked_meat` | 熏肉 | Smoked Meat | E0 |  |
| item | `tccultivation:soil_inoculant` | 土壤菌剂 | Soil Inoculant | E10 |  |
| item | `tccultivation:soil_test_kit` | 土壤速测箱 | Soil Test Kit | E6 |  |
| item | `tccultivation:sorghum` | 高粱 | Sorghum | E1 | 收获物 |
| item | `tccultivation:sorghum_seeds` | 高粱种子 | Sorghum Seeds | E1 |  |
| item | `tccultivation:soy_sauce` | 酱油 | Soy Sauce | E2 |  |
| item | `tccultivation:soybean` | 大豆 | Soybean | E2 | 收获物 |
| item | `tccultivation:soybean_seeds` | 大豆种子 | Soybean Seeds | E2 |  |
| item | `tccultivation:spoiled_food` | 腐败食物 | Spoiled Food | E0 | 可堆肥 |
| item | `tccultivation:sunflower` | 向日葵盘 | Sunflower Head | E5 | 可种植；#tccultivation:oilseeds |
| item | `tccultivation:sweet_potato` | 甘薯 | Sweet Potato | E2 | 可种植 |
| item | `tccultivation:tea` | 茶鲜叶 | Tea Leaves | E2 | 收获物 |
| item | `tccultivation:tea_cutting` | 茶插条 | Tea Cutting | E2 |  |
| item | `tccultivation:venison` | 生鹿肉 | Raw Venison | E0 | 野味 |
| item | `tccultivation:wine` | 葡萄酒 | Wine | E2 | 可陈化；特产 |
| item | `tccultivation:yogurt` | 酸奶 | Yogurt | E1 |  |
| item | `tccultivation:yoke` | 轭 | Yoke | E2 |  |
| entity | `tccultivation:duck` | 家鸭 | Domestic Duck | E2 |  |
| entity | `tccultivation:goat` | 家山羊 | Domestic Goat | E1 | 原版山羊保持野生 |
| entity | `tccultivation:goose` | 家鹅 | Domestic Goose | E2 |  |
| entity | `tccultivation:implement` | 农具挂件 | Farm Implement | E2 | 组件 implement_kind |
| entity | `tccultivation:turkey` | 火鸡 | Turkey | E2 |  |
| entity | `tccultivation:water_buffalo` | 水牛 | Water Buffalo | E1 |  |
| entity | `tccultivation:yak` | 牦牛 | Yak | E1 |  |
| block_entity | `tccultivation:crop` | 作物 | Crop | E0 | 全部作物方块共用 |
| block_entity | `tccultivation:husbandry_station` | 农牧站点 | Husbandry Station | E1 | feed_trough、seed_sorting_tray、acclimatization_garden、gene_editing_station、field_sensor、grow_rack、nutrient_tank |
| block_entity | `tccultivation:processor` | 加工设施 | Processor | E0 | drying_rack、smokehouse、churn、cheese_press、compost_bin、manure_heap、silkworm_tray、tree_tap |
| block_entity | `tccultivation:storage` | 储藏容器 | Storage | E1 | grain_pit、rat_proof_bin、ice_chest、refrigerator、deep_freezer、seed_vault |
| block_entity | `tccultivation:water_works` | 水利设施 | Water Works | E2 | sluice_gate、diversion_weir、noria、chain_pump、tube_well、sprinkler |
| menu | `tccultivation:feed_trough` | 饲槽 | Feed Trough | E1 | 9 格 |
| menu | `tccultivation:gene_editing` | 基因编辑 | Gene Editing | E9 | 5.4.6 |
| menu | `tccultivation:processing` | 加工 | Processing | E0 | 晾架、烟熏房、乳品、蚕匾 |
| menu | `tccultivation:seed_sorting` | 选种台 | Seed Sorting | E1 | 10.3 |
| menu | `tccultivation:storage` | 储藏 | Storage | E1 | 27/54 格，显示新鲜度 |
| payload | `tccultivation:allocate_water` | 配水 | Allocate Water | — | C→S，共享对象 |
| payload | `tccultivation:animal_inspect` | 牲畜检视 | Animal Inspect | — | S→C |
| payload | `tccultivation:field_status` | 农田状态 | Field Status | — | S→C |
| payload | `tccultivation:herd_action` | 畜群操作 | Herd Action | — | C→S |
| payload | `tccultivation:hitch` | 挂接农具 | Hitch Implement | — | C→S |
| payload | `tccultivation:name_variety` | 命名品种 | Name Variety | — | C→S |
| payload | `tccultivation:seed_select` | 选种 | Seed Select | — | C→S |
| payload | `tccultivation:set_rotation` | 设定轮作 | Set Rotation | — | C→S，共享对象 |
| payload | `tccultivation:soil_probe` | 土壤探查 | Soil Probe | — | S→C |
| hud_widget | `tccultivation:field_status` | 农田状态 | Field Status | E1 | 右中，标准级 |
| civ_tab | `tccultivation:agriculture` | 农业 | Agriculture | E1 | 经济组，排序 400 |
| region_layer | `tccultivation:agri` | 农业层 | Agriculture Layer | — | 11.3 |

### 5.7 工坊 `tcindustry`（621 项，来源 [13-industry.md](13-industry.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcindustry:additive_printer` | 增材打印机 | Additive Printer | E10 | 增材制造 |
| block | `tcindustry:advanced_pwr_reactor` | 先进压水堆 | Advanced PWR Reactor | E10 | 非能动安全 |
| block | `tcindustry:air_compressor` | 空压机 | Air Compressor | E6 | 压缩空气、充气瓶 |
| block | `tcindustry:air_duct` | 进气风道 | Air Duct | E7 | 发动机进排气 |
| block | `tcindustry:air_separation_unit` | 空分装置 | Air Separation Unit | E7 | 氧、氮、氩与液化 |
| block | `tcindustry:alumina_bricks` | 高铝砖 | Alumina Bricks | E7 | 炉衬 1,680 °C |
| block | `tcindustry:aluminium_block` | 铝块 | Block of Aluminium | E7 | 储存 |
| block | `tcindustry:animal_gin` | 畜力绞盘 | Animal Gin | E3 | 畜力动力 |
| block | `tcindustry:basic_oxygen_furnace` | 氧气顶吹转炉 | Basic Oxygen Furnace | E8 | 大宗炼钢 |
| block | `tcindustry:beehive_coke_oven` | 蜂窝焦炉 | Beehive Coke Oven | E6 | 炼焦 |
| block | `tcindustry:bessemer_converter` | 贝塞麦转炉 | Bessemer Converter | E6 | 炼钢 |
| block | `tcindustry:biodiesel` | 生物柴油（流体方块） | Biodiesel | E7 | 可装桶流体 |
| block | `tcindustry:blast_furnace_controller` | 高炉控制器 | Blast Furnace Controller | E4 | 多方块控制器 |
| block | `tcindustry:bloomery` | 块炼炉 | Bloomery | E3 | 块炼铁 |
| block | `tcindustry:blowing_engine` | 鼓风机 | Blowing Engine | E6 | 蒸汽鼓风 |
| block | `tcindustry:brass_block` | 黄铜块 | Block of Brass | E3 | 储存 |
| block | `tcindustry:brick_kiln` | 砖窑 | Brick Kiln | E4 | 多方块控制器 |
| block | `tcindustry:broken_shaft` | 断轴 | Broken Shaft | E3 | 过载断裂后的残件 |
| block | `tcindustry:bronze_block` | 青铜块 | Block of Bronze | E2 | 储存 |
| block | `tcindustry:bucket_elevator` | 斗式提升机 | Bucket Elevator | E4 | 垂直输送 |
| block | `tcindustry:canning_machine` | 罐装机 | Canning Machine | E6 | 罐头 |
| block | `tcindustry:cast_iron_gear` | 铸铁齿轮 | Cast Iron Gear | E4 | 传动 |
| block | `tcindustry:casting_table` | 铸造台 | Casting Table | E2 | 浇铸 |
| block | `tcindustry:catalytic_cracker` | 催化裂化装置 | Catalytic Cracker | E8 | 裂化、乙烯 |
| block | `tcindustry:charcoal_clamp` | 炭堆 | Charcoal Clamp | E2 | 烧炭 |
| block | `tcindustry:charcoal_kiln` | 炭窑 | Charcoal Kiln | E2 | 烧炭 |
| block | `tcindustry:chemical_reactor` | 化学反应釜 | Chemical Reactor | E7 | 通用化工 |
| block | `tcindustry:chimney` | 烟囱 | Chimney | E4 | 排烟扩散 |
| block | `tcindustry:chlor_alkali_cell` | 氯碱电解槽 | Chlor-Alkali Cell | E7 | 氯、烧碱、氢 |
| block | `tcindustry:circuit_breaker` | 断路器 | Circuit Breaker | E7 | 保护与切负荷 |
| block | `tcindustry:clay_lining` | 黏土炉衬 | Clay Lining | E1 | 低温炉衬 |
| block | `tcindustry:clay_pipe` | 陶管 | Clay Pipe | E3 | 低压水管 |
| block | `tcindustry:cleanroom_panel` | 洁净室墙板 | Cleanroom Panel | E9 | 洁净室结构 |
| block | `tcindustry:clutch` | 离合器 | Clutch | E4 | 断开传动 |
| block | `tcindustry:cnc_mill` | 数控铣床 | CNC Mill | E9 | 数控加工 |
| block | `tcindustry:coal_tar` | 煤焦油（流体方块） | Coal Tar | E6 | 可装桶流体 |
| block | `tcindustry:coke_oven_battery` | 焦炉组 | Coke Oven Battery | E6 | 炼焦并回收煤气 |
| block | `tcindustry:compact_fission_reactor` | 紧凑型裂变堆 | Compact Fission Reactor | E10 | Orbit 电源 |
| block | `tcindustry:compound_engine` | 复式蒸汽机 | Compound Engine | E6 | 多级膨胀 |
| block | `tcindustry:concrete_mixer` | 混凝土搅拌机 | Concrete Mixer | E6 | 取代原版混凝土粉合成 |
| block | `tcindustry:condenser` | 冷凝器 | Condenser | E6 | 回收凝结水 |
| block | `tcindustry:containment_dome` | 安全壳 | Containment Dome | E8 | 反应堆安全 |
| block | `tcindustry:control_cable` | 控制电缆 | Control Cable | E7 | 16 通道信号 |
| block | `tcindustry:control_rod_drive` | 控制棒驱动机构 | Control Rod Drive | E8 | 反应堆部件 |
| block | `tcindustry:control_room` | 控制室 | Control Room | E9 | 集中监控 |
| block | `tcindustry:conversion_plant` | 转化厂 | Conversion Plant | E8 | 六氟化铀 |
| block | `tcindustry:converter_station` | 换流站 | Converter Station | E9 | 交直流转换 |
| block | `tcindustry:conveyor_belt` | 传送带 | Conveyor Belt | E6 | 水平输送 |
| block | `tcindustry:cooling_tower` | 冷却塔 | Cooling Tower | E6 | 散热，降低热污染 |
| block | `tcindustry:copper_cable` | 铜电缆 | Copper Cable | E7 | 绝缘电缆 |
| block | `tcindustry:copper_wire` | 铜线 | Copper Wire | E7 | 低压导线 |
| block | `tcindustry:cornish_boiler` | 康沃尔锅炉 | Cornish Boiler | E6 | 中压蒸汽 |
| block | `tcindustry:crown_gear` | 冠状齿轮 | Crown Gear | E3 | 直角传动 |
| block | `tcindustry:crucible_furnace` | 坩埚炉 | Crucible Furnace | E2 | 熔炼 |
| block | `tcindustry:crude_oil` | 原油（流体方块） | Crude Oil | E7 | 可装桶流体 |
| block | `tcindustry:crystal_puller` | 单晶炉 | Crystal Puller | E8 | 单晶硅 |
| block | `tcindustry:cyclone_separator` | 旋风除尘器 | Cyclone Separator | E6 | 除尘 |
| block | `tcindustry:dc_dynamo` | 直流发电机 | DC Dynamo | E7 | 发电 |
| block | `tcindustry:decontamination_station` | 去污站 | Decontamination Station | E8 | 清理放射性碎片 |
| block | `tcindustry:diesel` | 柴油（流体方块） | Diesel | E7 | 可装桶流体 |
| block | `tcindustry:diesel_engine` | 柴油机 | Diesel Engine | E7 | 内燃，供 Networks |
| block | `tcindustry:diesel_genset` | 柴油发电机组 | Diesel Genset | E7 | 发电 |
| block | `tcindustry:distillation_tower` | 蒸馏塔 | Distillation Tower | E7 | 原油蒸馏 |
| block | `tcindustry:distribution_transformer` | 配电变压器 | Distribution Transformer | E7 | 降压 |
| block | `tcindustry:dragon_kiln` | 龙窑 | Dragon Kiln | E5 | 瓷器 |
| block | `tcindustry:drill_press` | 钻床 | Drill Press | E6 | 机加工 |
| block | `tcindustry:drilling_rig` | 钻机 | Drilling Rig | E7 | 钻井 |
| block | `tcindustry:dry_cask` | 干式贮存罐 | Dry Cask | E8 | 乏燃料长期储存 |
| block | `tcindustry:eccs_pump` | 应急堆芯冷却泵 | ECCS Pump | E8 | 反应堆安全 |
| block | `tcindustry:ehv_line` | 超高压线 | EHV Line | E9 | 输电 |
| block | `tcindustry:electric_arc_furnace` | 电弧炉 | Electric Arc Furnace | E7 | 合金钢、不锈钢 |
| block | `tcindustry:electric_motor_large` | 大型电动机 | Large Electric Motor | E7 | 电驱动 |
| block | `tcindustry:electric_motor_medium` | 中型电动机 | Medium Electric Motor | E7 | 电驱动 |
| block | `tcindustry:electric_motor_small` | 小型电动机 | Small Electric Motor | E7 | 电驱动 |
| block | `tcindustry:electrostatic_precipitator` | 静电除尘器 | Electrostatic Precipitator | E7 | 除尘 |
| block | `tcindustry:ethanol` | 乙醇（流体方块） | Ethanol | E6 | 可装桶流体 |
| block | `tcindustry:evaporation_pond` | 蒸发池 | Evaporation Pond | E1 | 晒盐；E9 提锂 |
| block | `tcindustry:fast_reactor` | 钠冷快堆 | Fast Reactor | E10 | MOX 燃料 |
| block | `tcindustry:fgd_scrubber` | 烟气脱硫塔 | FGD Scrubber | E8 | 脱硫 |
| block | `tcindustry:fire_bricks` | 耐火砖 | Fire Bricks | E4 | 炉衬 1,550 °C（§5.3.3） |
| block | `tcindustry:float_glass_line` | 浮法玻璃线 | Float Glass Line | E8 | 平板玻璃 |
| block | `tcindustry:float_switch` | 浮球开关 | Float Switch | E7 | 传感器 |
| block | `tcindustry:flotation_cell` | 浮选槽 | Flotation Cell | E7 | 选矿 |
| block | `tcindustry:fluid_loading_arm` | 流体装卸臂 | Fluid Loading Arm | E7 | 与 Networks 的流体交界 |
| block | `tcindustry:fluorescent_lamp` | 荧光灯 | Fluorescent Lamp | E8 | 照明负荷 |
| block | `tcindustry:flywheel` | 飞轮 | Flywheel | E6 | 平滑转速 |
| block | `tcindustry:flywheel_storage` | 飞轮储能 | Flywheel Storage | E8 | 储能 |
| block | `tcindustry:forge_hearth` | 锻炉 | Forge Hearth | E2 | 加热工件 |
| block | `tcindustry:forging_anvil` | 锻造砧 | Forging Anvil | E2 | 锻打 |
| block | `tcindustry:fuel_cell` | 燃料电池 | Fuel Cell | E9 | 氢发电；Orbit 电源 |
| block | `tcindustry:fuel_fabricator` | 燃料制造厂 | Fuel Fabricator | E8 | 芯块与组件 |
| block | `tcindustry:fuel_oil` | 燃料油（流体方块） | Fuel Oil | E7 | 可装桶流体 |
| block | `tcindustry:fulling_mill` | 漂洗磨坊 | Fulling Mill | E4 | 呢绒漂洗 |
| block | `tcindustry:gas_centrifuge` | 气体离心机 | Gas Centrifuge | E8 | 浓缩（≤ 20%） |
| block | `tcindustry:gas_engine` | 煤气机 | Gas Engine | E7 | 内燃 |
| block | `tcindustry:gas_producer` | 煤气发生炉 | Gas Producer | E6 | 发生炉煤气 |
| block | `tcindustry:gas_turbine` | 燃气轮机 | Gas Turbine | E8 | 发电、驱动 |
| block | `tcindustry:gasoline` | 汽油（流体方块） | Gasoline | E7 | 可装桶流体 |
| block | `tcindustry:gasoline_engine` | 汽油机 | Gasoline Engine | E7 | 内燃，供 Networks |
| block | `tcindustry:gearbox` | 齿轮箱 | Gearbox | E6 | 变速 |
| block | `tcindustry:geothermal_well` | 地热井 | Geothermal Well | E7 | 地热发电 |
| block | `tcindustry:glass_furnace` | 玻璃窑 | Glass Furnace | E3 | 玻璃 |
| block | `tcindustry:governor` | 调速器 | Governor | E6 | 离心调速 |
| block | `tcindustry:graphite_block` | 石墨块 | Graphite Block | E7 | 慢化剂、电极原料 |
| block | `tcindustry:graphite_water_reactor` | 石墨水冷堆 | Graphite Water Reactor | E8 | 发电堆 |
| block | `tcindustry:haber_reactor` | 合成氨塔 | Haber Reactor | E7 | 合成氨 |
| block | `tcindustry:hall_heroult_cell` | 霍尔-埃鲁电解槽 | Hall-Héroult Cell | E7 | 电解铝 |
| block | `tcindustry:hand_bellows` | 手风箱 | Hand Bellows | E2 | 鼓风 |
| block | `tcindustry:hand_crank` | 手摇曲柄 | Hand Crank | E3 | 人力动力 |
| block | `tcindustry:haystack_boiler` | 干草堆锅炉 | Haystack Boiler | E6 | 低压蒸汽 |
| block | `tcindustry:heat_recovery_boiler` | 余热锅炉 | Heat Recovery Boiler | E9 | 燃气-蒸汽联合循环 |
| block | `tcindustry:helical_gear` | 斜齿轮 | Helical Gear | E7 | 高速传动 |
| block | `tcindustry:hot_blast_stove` | 热风炉 | Hot Blast Stove | E6 | 热风 |
| block | `tcindustry:hv_line` | 高压线 | HV Line | E7 | 输电 |
| block | `tcindustry:hvdc_line` | 高压直流线 | HVDC Line | E9 | 远距离输电 |
| block | `tcindustry:hydro_turbine` | 水轮机 | Hydro Turbine | E7 | 水力发电 |
| block | `tcindustry:hydrogen_recombiner` | 氢复合器 | Hydrogen Recombiner | E8 | 反应堆安全 |
| block | `tcindustry:incandescent_lamp` | 白炽灯 | Incandescent Lamp | E7 | 照明负荷 |
| block | `tcindustry:incinerator` | 焚烧炉 | Incinerator | E9 | 有机废物处理 |
| block | `tcindustry:inverter` | 逆变器 | Inverter | E9 | 直流并网；FE 边界 |
| block | `tcindustry:iron_pipe` | 铁管 | Iron Pipe | E6 | 流体管道 |
| block | `tcindustry:iron_shaft` | 铁轴 | Iron Shaft | E5 | 传动 |
| block | `tcindustry:kerosene` | 煤油（流体方块） | Kerosene | E7 | 可装桶流体 |
| block | `tcindustry:labour_post` | 工位 | Labour Post | E1 | 可见工位，三种外观状态 |
| block | `tcindustry:ladle` | 钢包 | Ladle | E6 | 熔融金属转运 |
| block | `tcindustry:lancashire_boiler` | 兰开夏锅炉 | Lancashire Boiler | E6 | 中压蒸汽 |
| block | `tcindustry:lantern_gear` | 灯笼齿轮 | Lantern Gear | E3 | 传动 |
| block | `tcindustry:large_storage_jar` | 大储罐 | Large Storage Jar | E1 | 储粮 |
| block | `tcindustry:lathe` | 车床 | Lathe | E6 | 机加工 |
| block | `tcindustry:lead_acid_battery` | 铅酸蓄电池组 | Lead-Acid Battery | E7 | 储能 |
| block | `tcindustry:lead_block` | 铅块 | Block of Lead | E2 | 储存、屏蔽 |
| block | `tcindustry:lead_cask` | 铅罐 | Lead Cask | E8 | 放射性物品容器 |
| block | `tcindustry:lead_chamber` | 铅室 | Lead Chamber | E5 | 硫酸 |
| block | `tcindustry:led_lamp` | LED 灯 | LED Lamp | E9 | 照明负荷 |
| block | `tcindustry:lime_kiln` | 石灰窑 | Lime Kiln | E3 | 生石灰 |
| block | `tcindustry:lithium_battery_bank` | 锂电池组 | Lithium Battery Bank | E9 | 储能 |
| block | `tcindustry:lithography_stepper` | 光刻机 | Lithography Stepper | E9 | 集成电路 |
| block | `tcindustry:lubricating_oil` | 润滑油（流体方块） | Lubricating Oil | E7 | 可装桶流体 |
| block | `tcindustry:magnesia_bricks` | 镁砖 | Magnesia Bricks | E7 | 炉衬 1,680 °C |
| block | `tcindustry:mechanical_stoker` | 机械加煤机 | Mechanical Stoker | E6 | 自动加煤 |
| block | `tcindustry:milling_machine` | 铣床 | Milling Machine | E6 | 机加工 |
| block | `tcindustry:millstone` | 石磨 | Millstone | E3 | 磨粉、破碎 |
| block | `tcindustry:mixing_vat` | 搅拌槽 | Mixing Vat | E3 | 砂浆 |
| block | `tcindustry:molten_aluminium` | 熔融铝（流体方块） | Molten Aluminium | E7 | 同上 |
| block | `tcindustry:molten_brass` | 熔融黄铜（流体方块） | Molten Brass | E3 | 同上 |
| block | `tcindustry:molten_bronze` | 熔融青铜（流体方块） | Molten Bronze | E2 | 同上 |
| block | `tcindustry:molten_copper` | 熔融铜（流体方块） | Molten Copper | E1 | 溢出时凝固为对应金属块或熔渣 |
| block | `tcindustry:molten_gold` | 熔融金（流体方块） | Molten Gold | E1 | 同上 |
| block | `tcindustry:molten_iron` | 熔融铁（流体方块） | Molten Iron | E3 | 同上 |
| block | `tcindustry:molten_lead` | 熔融铅（流体方块） | Molten Lead | E2 | 同上 |
| block | `tcindustry:molten_pig_iron` | 熔融生铁（流体方块） | Molten Pig Iron | E4 | 同上 |
| block | `tcindustry:molten_silver` | 熔融银（流体方块） | Molten Silver | E2 | 同上 |
| block | `tcindustry:molten_steel` | 熔融钢（流体方块） | Molten Steel | E3 | 同上 |
| block | `tcindustry:molten_tin` | 熔融锡（流体方块） | Molten Tin | E2 | 同上 |
| block | `tcindustry:molten_zinc` | 熔融锌（流体方块） | Molten Zinc | E4 | 同上 |
| block | `tcindustry:mud_brick` | 泥砖 | Mud Brick | E1 | 建材 |
| block | `tcindustry:mv_line` | 中压线 | MV Line | E7 | 配电 |
| block | `tcindustry:naphtha` | 石脑油（流体方块） | Naphtha | E7 | 可装桶流体 |
| block | `tcindustry:newcomen_engine` | 纽科门蒸汽机 | Newcomen Engine | E6 | 大气式蒸汽机 |
| block | `tcindustry:nitre_bed` | 硝石床 | Nitre Bed | E5 | 硝石熟化 |
| block | `tcindustry:open_hearth_furnace` | 平炉 | Open Hearth Furnace | E6 | 炼钢 |
| block | `tcindustry:overhead_crane` | 行车 | Overhead Crane | E6 | 吊运 |
| block | `tcindustry:overshot_waterwheel` | 上射水车 | Overshot Waterwheel | E4 | 水力 |
| block | `tcindustry:panning_station` | 淘洗台 | Panning Station | E1 | 独立模式淘洗（§2.3） |
| block | `tcindustry:pid_controller` | PID 控制器 | PID Controller | E8 | 闭环控制 |
| block | `tcindustry:pit_kiln` | 坑窑 | Pit Kiln | E1 | 坑烧陶器 |
| block | `tcindustry:planer` | 刨床 | Planer | E6 | 机加工 |
| block | `tcindustry:plc` | 可编程控制器 | PLC | E9 | 梯形图程序 |
| block | `tcindustry:pole_transformer` | 杆上变压器 | Pole Transformer | E7 | 降压 |
| block | `tcindustry:post_windmill` | 柱式风车 | Post Windmill | E4 | 风力 |
| block | `tcindustry:potters_wheel` | 陶轮 | Potter's Wheel | E2 | 拉坯 |
| block | `tcindustry:pottery_jar` | 陶罐 | Pottery Jar | E1 | 小型储物 |
| block | `tcindustry:powder_mill` | 火药磨 | Powder Mill | E5 | 火药 |
| block | `tcindustry:power_loom` | 动力织机 | Power Loom | E6 | 机械织布 |
| block | `tcindustry:power_meter` | 电表 | Power Meter | E7 | 计量 |
| block | `tcindustry:precast_panel` | 预制板 | Precast Panel | E7 | 建材 |
| block | `tcindustry:pressure_switch` | 压力开关 | Pressure Switch | E7 | 传感器 |
| block | `tcindustry:puddling_furnace` | 搅炼炉 | Puddling Furnace | E6 | 熟铁 |
| block | `tcindustry:pulley` | 皮带轮 | Pulley | E4 | 带传动 |
| block | `tcindustry:pump` | 泵 | Pump | E5 | 流体输送 |
| block | `tcindustry:pumpjack` | 抽油机 | Pumpjack | E7 | 采油 |
| block | `tcindustry:pwr_reactor` | 压水堆 | PWR Reactor | E8 | 发电堆 |
| block | `tcindustry:radioactive_debris` | 放射性碎片 | Radioactive Debris | E8 | 事故产物 |
| block | `tcindustry:refrigeration_unit` | 冷藏机 | Refrigeration Unit | E7 | Cultivation 冷藏 |
| block | `tcindustry:reinforced_concrete` | 钢筋混凝土 | Reinforced Concrete | E6 | 建材 |
| block | `tcindustry:relay_panel` | 继电器盘 | Relay Panel | E7 | 逻辑控制 |
| block | `tcindustry:remote_manipulator` | 遥控机械手 | Remote Manipulator | E8 | 操作放射性物品 |
| block | `tcindustry:reprocessing_plant` | 后处理厂 | Reprocessing Plant | E10 | MOX、Pu-238 热源 |
| block | `tcindustry:research_pile` | 研究堆 | Research Pile | E8 | 首个临界装置 |
| block | `tcindustry:retort` | 干馏釜 | Retort | E6 | 木焦油、木醋液 |
| block | `tcindustry:robot_arm` | 机械臂 | Robot Arm | E9 | 自动上下料 |
| block | `tcindustry:rolling_mill` | 轧机 | Rolling Mill | E5 | 轧板 |
| block | `tcindustry:roman_concrete` | 罗马混凝土 | Roman Concrete | E3 | 建材 |
| block | `tcindustry:rotary_kiln` | 回转窑 | Rotary Kiln | E6 | 水泥熟料 |
| block | `tcindustry:rtg` | 放射性同位素发电机 | Radioisotope Thermoelectric Generator | E10 | Orbit 电源 |
| block | `tcindustry:saddle_quern` | 鞍形磨盘 | Saddle Quern | E1 | 手工磨粉 |
| block | `tcindustry:sawmill` | 锯木厂 | Sawmill | E4 | 锯木 |
| block | `tcindustry:scr_unit` | 选择性催化还原装置 | SCR Unit | E9 | 脱硝 |
| block | `tcindustry:signal_converter` | 信号转换器 | Signal Converter | E7 | 红石与通道互转 |
| block | `tcindustry:silica_bricks` | 硅砖 | Silica Bricks | E6 | 炉衬 1,550 °C |
| block | `tcindustry:silver_block` | 银块 | Block of Silver | E2 | 储存 |
| block | `tcindustry:slip_coupling` | 滑差联轴器 | Slip Coupling | E6 | 过载保护 |
| block | `tcindustry:sluice_box` | 溜槽淘洗器 | Sluice Box | E3 | 水流连续淘洗 |
| block | `tcindustry:sluice_gate` | 闸门 | Sluice Gate | E4 | 控制水渠 |
| block | `tcindustry:solar_panel` | 太阳能板 | Solar Panel | E9 | 光伏 |
| block | `tcindustry:solidified_slag` | 凝固熔渣 | Solidified Slag | E2 | 溢出熔渣凝固 |
| block | `tcindustry:sorter` | 分拣器 | Sorter | E7 | 过滤分流 |
| block | `tcindustry:spent_fuel_pool` | 乏燃料水池 | Spent Fuel Pool | E8 | 乏燃料冷却 |
| block | `tcindustry:spinning_mule` | 走锭纺纱机 | Spinning Mule | E6 | 机械纺纱 |
| block | `tcindustry:stainless_pipe` | 不锈钢管 | Stainless Pipe | E8 | 耐酸管道 |
| block | `tcindustry:stainless_steel_block` | 不锈钢块 | Block of Stainless Steel | E7 | 储存 |
| block | `tcindustry:stamp_mill` | 捣矿机 | Stamp Mill | E4 | 破碎矿石 |
| block | `tcindustry:stationary_engine` | 固定式发动机 | Stationary Engine | E7 | 驱动轴网 |
| block | `tcindustry:steam_generator` | 蒸汽发生器 | Steam Generator | E8 | 压水堆二回路 |
| block | `tcindustry:steel_block` | 钢块 | Block of Steel | E3 | 储存；Networks 结构材料 |
| block | `tcindustry:steel_gear` | 钢齿轮 | Steel Gear | E6 | 传动 |
| block | `tcindustry:steel_pipe` | 钢管 | Steel Pipe | E6 | 高压管道 |
| block | `tcindustry:steel_shaft` | 钢轴 | Steel Shaft | E6 | 传动 |
| block | `tcindustry:step_up_station` | 升压站 | Step-Up Station | E7 | 升压 |
| block | `tcindustry:substation` | 变电站 | Substation | E7 | 多方块 |
| block | `tcindustry:tailings_dam` | 尾矿坝 | Tailings Dam | E7 | 尾矿储存 |
| block | `tcindustry:tank` | 储罐 | Tank | E6 | 流体储存 |
| block | `tcindustry:tanning_pit` | 鞣制坑 | Tanning Pit | E1 | 鞣革 |
| block | `tcindustry:thermostat` | 温控器 | Thermostat | E7 | 传感器 |
| block | `tcindustry:tin_block` | 锡块 | Block of Tin | E2 | 储存 |
| block | `tcindustry:tokamak_controller` | 托卡马克控制器 | Tokamak Controller | E10 | 实验聚变装置 |
| block | `tcindustry:tower_windmill` | 塔式风车 | Tower Windmill | E5 | 风力 |
| block | `tcindustry:transmission_tower` | 输电塔 | Transmission Tower | E7 | 架线 |
| block | `tcindustry:treadwheel` | 踏车 | Treadwheel | E3 | 人力动力 |
| block | `tcindustry:trip_hammer` | 水力锻锤 | Trip Hammer | E4 | 锻打、冲压 |
| block | `tcindustry:turbine_generator` | 汽轮发电机 | Turbine Generator | E7 | 发电 |
| block | `tcindustry:undershot_waterwheel` | 下射水车 | Undershot Waterwheel | E4 | 水力 |
| block | `tcindustry:updraft_kiln` | 升焰窑 | Updraft Kiln | E1 | 多方块控制器 |
| block | `tcindustry:uranium_mill` | 铀水冶厂 | Uranium Mill | E8 | 黄饼 |
| block | `tcindustry:utility_pole` | 电线杆 | Utility Pole | E7 | 架线 |
| block | `tcindustry:warp_weighted_loom` | 经重织机 | Warp-Weighted Loom | E1 | 手工织布 |
| block | `tcindustry:washing_trough` | 洗泥槽 | Washing Trough | E1 | 淘洗黏土（无门槛） |
| block | `tcindustry:wastewater_plant` | 污水处理站 | Wastewater Plant | E7 | 废水治理 |
| block | `tcindustry:water_bellows` | 水力风箱 | Water Bellows | E4 | 水力鼓风 |
| block | `tcindustry:water_tube_boiler` | 水管锅炉 | Water-Tube Boiler | E7 | 高压与过热蒸汽 |
| block | `tcindustry:watt_engine` | 瓦特蒸汽机 | Watt Engine | E6 | 分离冷凝器蒸汽机 |
| block | `tcindustry:wellhead` | 井口装置 | Wellhead | E7 | 采气 |
| block | `tcindustry:wind_turbine` | 风力机 | Wind Turbine | E9 | 风力发电 |
| block | `tcindustry:wire_drawing_bench` | 拉丝台 | Wire Drawing Bench | E5 | 拉丝 |
| block | `tcindustry:wood_tar` | 木焦油（流体方块） | Wood Tar | E6 | 可装桶流体 |
| block | `tcindustry:wooden_chute` | 木溜槽 | Wooden Chute | E1 | 重力输送 |
| block | `tcindustry:wooden_shaft` | 木轴 | Wooden Shaft | E3 | 传动 |
| block | `tcindustry:works_office` | 工业区账房 | Works Office | E1 | 定义工业区（§5.11） |
| block | `tcindustry:zinc_block` | 锌块 | Block of Zinc | E4 | 储存 |
| item | `tcindustry:acid_carboy` | 酸瓶 | Acid Carboy | E5 | 酸容器 |
| item | `tcindustry:air_tank` | 气罐 | Air Tank | E6 | 风镐气源 |
| item | `tcindustry:al_li_alloy_ingot` | 铝锂合金锭 | Al-Li Alloy Ingot | E8 | Orbit 贮箱 |
| item | `tcindustry:al_li_alloy_plate` | 铝锂合金板 | Al-Li Alloy Plate | E8 | Orbit 贮箱 |
| item | `tcindustry:alloy_steel_axe` | 合金钢斧 | Alloy Steel Axe | E7 | T5 |
| item | `tcindustry:alloy_steel_hoe` | 合金钢锄 | Alloy Steel Hoe | E7 | T5 |
| item | `tcindustry:alloy_steel_ingot` | 合金钢锭 | Alloy Steel Ingot | E7 | T5 工具 |
| item | `tcindustry:alloy_steel_pickaxe` | 合金钢镐 | Alloy Steel Pickaxe | E7 | T5 |
| item | `tcindustry:alloy_steel_plate` | 合金钢板 | Alloy Steel Plate | E7 | 压力容器 |
| item | `tcindustry:alloy_steel_rod` | 合金钢棒 | Alloy Steel Rod | E7 | 高速轴 |
| item | `tcindustry:alloy_steel_shovel` | 合金钢锹 | Alloy Steel Shovel | E7 | T5 |
| item | `tcindustry:alumina` | 氧化铝 | Alumina | E7 | 电解铝原料 |
| item | `tcindustry:aluminium_ingot` | 铝锭 | Aluminium Ingot | E7 | c:ingots/aluminum |
| item | `tcindustry:aluminium_plate` | 铝板 | Aluminium Plate | E7 | c:plates/aluminum |
| item | `tcindustry:aluminium_wire` | 铝线 | Aluminium Wire | E8 | 高压线材料 |
| item | `tcindustry:ammonium_nitrate` | 硝酸铵 | Ammonium Nitrate | E7 | 化肥、炸药原料 |
| item | `tcindustry:ammonium_sulfate` | 硫酸铵 | Ammonium Sulfate | E6 | 化肥 |
| item | `tcindustry:angle_grinder` | 角磨机 | Angle Grinder | E9 | 动力工具，锂电池 |
| item | `tcindustry:arsenical_bronze_ingot` | 砷青铜锭 | Arsenical Bronze Ingot | E2 | 早期合金 |
| item | `tcindustry:biodiesel_bucket` | 生物柴油桶 | Biodiesel Bucket | E7 | 流体桶 |
| item | `tcindustry:bitumen` | 沥青 | Bitumen | E7 | 防水、筑路 |
| item | `tcindustry:blowpipe` | 吹管 | Blowpipe | E3 | 吹制玻璃 |
| item | `tcindustry:brass_gear` | 黄铜齿轮 | Brass Gear | E5 | 仪器齿轮、钟表 |
| item | `tcindustry:brass_ingot` | 黄铜锭 | Brass Ingot | E3 | c:ingots/brass |
| item | `tcindustry:brass_nugget` | 黄铜粒 | Brass Nugget | E3 | c:nuggets/brass |
| item | `tcindustry:brass_plate` | 黄铜板 | Brass Plate | E4 | 仪器 |
| item | `tcindustry:brass_rod` | 黄铜棒 | Brass Rod | E4 | 仪器 |
| item | `tcindustry:broadcloth` | 呢绒 | Broadcloth | E4 | 漂洗毛布 |
| item | `tcindustry:bronze_axe` | 青铜斧 | Bronze Axe | E2 | T2 |
| item | `tcindustry:bronze_chisel` | 青铜凿 | Bronze Chisel | E2 | 同上 |
| item | `tcindustry:bronze_hammer` | 青铜锻锤 | Bronze Hammer | E2 | #tcindustry:hammers |
| item | `tcindustry:bronze_hoe` | 青铜锄 | Bronze Hoe | E2 | T2 |
| item | `tcindustry:bronze_ingot` | 青铜锭 | Bronze Ingot | E2 | c:ingots/bronze |
| item | `tcindustry:bronze_nugget` | 青铜粒 | Bronze Nugget | E2 | c:nuggets/bronze |
| item | `tcindustry:bronze_pickaxe` | 青铜镐 | Bronze Pickaxe | E2 | T2 |
| item | `tcindustry:bronze_plate` | 青铜板 | Bronze Plate | E4 | c:plates/bronze |
| item | `tcindustry:bronze_saw` | 青铜锯 | Bronze Saw | E2 | 木工 |
| item | `tcindustry:bronze_shears` | 青铜剪 | Bronze Shears | E2 | 剪羊毛 |
| item | `tcindustry:bronze_shovel` | 青铜锹 | Bronze Shovel | E2 | T2 |
| item | `tcindustry:bronze_sickle` | 青铜镰 | Bronze Sickle | E2 | 同上 |
| item | `tcindustry:bronze_tongs` | 青铜火钳 | Bronze Tongs | E2 | #tcindustry:tongs，夹持热工件 |
| item | `tcindustry:canned_food` | 罐头 | Canned Food | E6 | 长期保存食物 |
| item | `tcindustry:carbon_fiber` | 碳纤维 | Carbon Fiber | E10 | 复合材料 |
| item | `tcindustry:carbon_fiber_panel` | 碳纤维板 | Carbon Fiber Panel | E10 | Orbit 结构 |
| item | `tcindustry:caustic_soda` | 烧碱 | Caustic Soda | E7 | 拜耳法、化工 |
| item | `tcindustry:chainsaw` | 链锯 | Chainsaw | E7 | 动力工具，耗汽油 |
| item | `tcindustry:chromium_concentrate` | 铬精矿 | Chromium Concentrate | E7 | 独立模式副产 |
| item | `tcindustry:chromium_ingot` | 铬锭 | Chromium Ingot | E7 | 不锈钢原料 |
| item | `tcindustry:circuit_board` | 电路板 | Circuit Board | E7 | 电子组件 |
| item | `tcindustry:clay_crucible` | 陶坩埚 | Clay Crucible | E2 | 盛熔融金属 |
| item | `tcindustry:clay_mold` | 陶铸模 | Clay Mold | E2 | 浇铸 |
| item | `tcindustry:clinker` | 水泥熟料 | Clinker | E6 | 回转窑产物 |
| item | `tcindustry:coal_tar_bucket` | 煤焦油桶 | Coal Tar Bucket | E6 | 流体桶 |
| item | `tcindustry:cobalt_concentrate` | 钴精矿 | Cobalt Concentrate | E7 | 独立模式副产 |
| item | `tcindustry:cobalt_ingot` | 钴锭 | Cobalt Ingot | E9 | 锂电池正极 |
| item | `tcindustry:coke` | 焦炭 | Coke | E6 | 燃料、还原剂 |
| item | `tcindustry:copper_chisel` | 铜凿 | Copper Chisel | E1 | 石工、木工 |
| item | `tcindustry:copper_plate` | 铜板 | Copper Plate | E4 | 水力锻锤冲压 |
| item | `tcindustry:copper_sickle` | 铜镰 | Copper Sickle | E1 | 收割；c:tools/sickle |
| item | `tcindustry:cotton_cloth` | 棉布 | Cotton Cloth | E6 | 布 |
| item | `tcindustry:crude_oil_bucket` | 原油桶 | Crude Oil Bucket | E7 | 流体桶 |
| item | `tcindustry:cryolite` | 冰晶石 | Cryolite | E7 | 电解质 |
| item | `tcindustry:depleted_uranium` | 贫铀 | Depleted Uranium | E8 | 浓缩副产 |
| item | `tcindustry:diesel_bucket` | 柴油桶 | Diesel Bucket | E7 | 流体桶 |
| item | `tcindustry:drop_spindle` | 纺锤 | Drop Spindle | E1 | 手工纺线工具 |
| item | `tcindustry:engineering_tablet` | 工程平板 | Engineering Tablet | E9 | 远程诊断；工程工具 |
| item | `tcindustry:ethanol_bucket` | 乙醇桶 | Ethanol Bucket | E6 | 流体桶 |
| item | `tcindustry:fiberglass` | 玻璃纤维 | Fiberglass | E9 | 复合材料 |
| item | `tcindustry:fire_clay` | 耐火黏土 | Fire Clay | E4 | 耐火砖原料 |
| item | `tcindustry:fuel_assembly` | 燃料组件 | Fuel Assembly | E8 | 压水堆燃料 |
| item | `tcindustry:fuel_oil_bucket` | 燃料油桶 | Fuel Oil Bucket | E7 | 流体桶 |
| item | `tcindustry:fuel_rod` | 燃料棒 | Fuel Rod | E8 | 组件原料 |
| item | `tcindustry:fuse` | 保险丝 | Fuse | E7 | 低压保护 |
| item | `tcindustry:gas_cylinder` | 气瓶 | Gas Cylinder | E7 | 气体容器 |
| item | `tcindustry:gasoline_bucket` | 汽油桶 | Gasoline Bucket | E7 | 流体桶 |
| item | `tcindustry:gold_pan` | 淘金盘 | Gold Pan | E3 | 铁制淘盘，淘洗收得率更高（§5.12） |
| item | `tcindustry:graphite_electrode` | 石墨电极 | Graphite Electrode | E7 | 电弧炉消耗 |
| item | `tcindustry:gypsum` | 石膏 | Gypsum | E7 | 水泥缓凝剂 |
| item | `tcindustry:heat_shield_tile` | 隔热瓦 | Heat Shield Tile | E9 | Orbit 再入防护 |
| item | `tcindustry:heavy_mineral_sand` | 重矿砂 | Heavy Mineral Sand | E8 | 钛、锆、稀土原料 |
| item | `tcindustry:integrated_circuit` | 集成电路 | Integrated Circuit | E9 | 控制与计算 |
| item | `tcindustry:iron_bloom` | 铁坯 | Iron Bloom | E3 | 块炼产物，需锻打 |
| item | `tcindustry:iron_chisel` | 铁凿 | Iron Chisel | E3 | 同上 |
| item | `tcindustry:iron_file` | 铁锉 | Iron File | E3 | 精修、维护 |
| item | `tcindustry:iron_hammer` | 铁锻锤 | Iron Hammer | E3 | 同上 |
| item | `tcindustry:iron_plate` | 铁板 | Iron Plate | E4 | c:plates/iron |
| item | `tcindustry:iron_plough_share` | 铁犁铧 | Iron Plough Share | E3 | c:plough_shares，供 Cultivation |
| item | `tcindustry:iron_rod` | 铁棒 | Iron Rod | E4 | c:rods/iron |
| item | `tcindustry:iron_saw` | 铁锯 | Iron Saw | E3 | 木工 |
| item | `tcindustry:iron_sickle` | 铁镰 | Iron Sickle | E3 | 同上 |
| item | `tcindustry:iron_tongs` | 铁火钳 | Iron Tongs | E3 | 同上 |
| item | `tcindustry:kerosene_bucket` | 煤油桶 | Kerosene Bucket | E7 | 流体桶 |
| item | `tcindustry:lead_acid_cell` | 铅酸电池单体 | Lead-Acid Cell | E7 | 蓄电池组件 |
| item | `tcindustry:lead_concentrate` | 铅精矿 | Lead Concentrate | E2 | 独立模式副产 |
| item | `tcindustry:lead_ingot` | 铅锭 | Lead Ingot | E2 | c:ingots/lead |
| item | `tcindustry:lead_nugget` | 铅粒 | Lead Nugget | E2 | c:nuggets/lead |
| item | `tcindustry:lead_plate` | 铅板 | Lead Plate | E4 | 铅室、屏蔽 |
| item | `tcindustry:leather_belt` | 皮带 | Leather Belt | E4 | 带传动 |
| item | `tcindustry:lime_mortar` | 石灰砂浆 | Lime Mortar | E3 | 砌筑 |
| item | `tcindustry:linen_cloth` | 亚麻布 | Linen Cloth | E1 | 布 |
| item | `tcindustry:lithium_cell` | 锂电池单体 | Lithium Cell | E9 | 动力工具、储能 |
| item | `tcindustry:lithium_ingot` | 锂锭 | Lithium Ingot | E9 | 锂电池、铝锂合金 |
| item | `tcindustry:lubricating_oil_bucket` | 润滑油桶 | Lubricating Oil Bucket | E7 | 流体桶 |
| item | `tcindustry:magnesium_ingot` | 镁锭 | Magnesium Ingot | E8 | 克罗尔法还原剂 |
| item | `tcindustry:maintenance_kit_iron` | 铁制维护包 | Iron Maintenance Kit | E5 | wear −0.7（§5.8.5） |
| item | `tcindustry:maintenance_kit_precision` | 精密维护包 | Precision Maintenance Kit | E9 | wear −0.95，并清零水垢 |
| item | `tcindustry:maintenance_kit_steel` | 钢制维护包 | Steel Maintenance Kit | E7 | wear −0.85 |
| item | `tcindustry:molybdenum_concentrate` | 钼精矿 | Molybdenum Concentrate | E7 | 独立模式副产 |
| item | `tcindustry:molybdenum_ingot` | 钼锭 | Molybdenum Ingot | E8 | 合金钢 |
| item | `tcindustry:mox_fuel_assembly` | MOX 燃料组件 | MOX Fuel Assembly | E10 | 快堆燃料 |
| item | `tcindustry:naphtha_bucket` | 石脑油桶 | Naphtha Bucket | E7 | 流体桶 |
| item | `tcindustry:natural_uranium_rod` | 天然铀棒 | Natural Uranium Rod | E8 | 研究堆、石墨堆燃料 |
| item | `tcindustry:nickel_concentrate` | 镍精矿 | Nickel Concentrate | E7 | 独立模式副产 |
| item | `tcindustry:nickel_ingot` | 镍锭 | Nickel Ingot | E7 | 不锈钢原料 |
| item | `tcindustry:nylon_thread` | 尼龙线 | Nylon Thread | E8 | 合成纤维 |
| item | `tcindustry:optical_glass` | 光学玻璃 | Optical Glass | E5 | 透镜 |
| item | `tcindustry:peat` | 泥炭 | Peat | E1 | 燃料 |
| item | `tcindustry:pig_iron` | 生铁 | Pig Iron | E4 | 高炉产物 |
| item | `tcindustry:piston_assembly` | 活塞组件 | Piston Assembly | E6 | 蒸汽机与内燃机部件 |
| item | `tcindustry:pneumatic_drill` | 风镐 | Pneumatic Drill | E6 | 动力工具，需气罐 |
| item | `tcindustry:polyethylene` | 聚乙烯 | Polyethylene | E8 | 绝缘、容器 |
| item | `tcindustry:polysilicon` | 多晶硅 | Polysilicon | E8 | 单晶硅原料 |
| item | `tcindustry:porcelain` | 瓷器 | Porcelain | E5 | 贸易品、绝缘子 |
| item | `tcindustry:portland_cement` | 硅酸盐水泥 | Portland Cement | E6 | 混凝土 |
| item | `tcindustry:potash` | 钾碱 | Potash | E1 | 玻璃、化肥 |
| item | `tcindustry:power_drill` | 电钻 | Power Drill | E9 | 动力工具，锂电池 |
| item | `tcindustry:pozzolana` | 火山灰 | Pozzolana | E3 | 罗马混凝土 |
| item | `tcindustry:pu238_heat_source` | 钚-238 热源 | Pu-238 Heat Source | E10 | RTG 热源 |
| item | `tcindustry:punched_card` | 穿孔卡 | Punched Card | E6 | 提花织机花样 |
| item | `tcindustry:quicklime` | 生石灰 | Quicklime | E3 | 砂浆、熔剂 |
| item | `tcindustry:rare_earth_concentrate` | 稀土精矿 | Rare Earth Concentrate | E8 | 独立模式副产 |
| item | `tcindustry:rawhide` | 生皮 | Rawhide | E1 | 鞣制原料（见 §18 C-6） |
| item | `tcindustry:red_mud` | 赤泥 | Red Mud | E7 | 拜耳法废料 |
| item | `tcindustry:rubber` | 橡胶 | Rubber | E7 | 密封、绝缘 |
| item | `tcindustry:rubber_belt` | 橡胶带 | Rubber Belt | E7 | 带传动、传送带 |
| item | `tcindustry:sail_cloth` | 帆布 | Sail Cloth | E4 | 风车帆、船帆 |
| item | `tcindustry:salt` | 盐 | Salt | E1 | c:dusts/salt（见 §18 C-4） |
| item | `tcindustry:saltpeter` | 硝石 | Saltpeter | E5 | 火药 |
| item | `tcindustry:silicon_boule` | 单晶硅棒 | Silicon Boule | E8 | 切片原料 |
| item | `tcindustry:silicon_wafer` | 硅片 | Silicon Wafer | E8 | 光刻原料 |
| item | `tcindustry:silk_cloth` | 丝绸 | Silk Cloth | E4 | 贸易品 |
| item | `tcindustry:silver_ingot` | 银锭 | Silver Ingot | E2 | c:ingots/silver |
| item | `tcindustry:silver_nugget` | 银粒 | Silver Nugget | E2 | c:nuggets/silver |
| item | `tcindustry:slag` | 熔渣 | Slag | E2 | 冶炼副产；水泥掺料 |
| item | `tcindustry:soda_ash` | 纯碱 | Soda Ash | E6 | 玻璃 |
| item | `tcindustry:solar_cell` | 太阳能电池片 | Solar Cell | E9 | 太阳能板部件 |
| item | `tcindustry:solder` | 焊锡 | Solder | E5 | 电路、罐头 |
| item | `tcindustry:spent_fuel_assembly` | 乏燃料组件 | Spent Fuel Assembly | E8 | 需冷却贮存 |
| item | `tcindustry:stainless_steel_ingot` | 不锈钢锭 | Stainless Steel Ingot | E7 | 耐酸设备 |
| item | `tcindustry:stainless_steel_plate` | 不锈钢板 | Stainless Steel Plate | E7 | 耐酸设备 |
| item | `tcindustry:steel_axe` | 钢斧 | Steel Axe | E3 | T4 |
| item | `tcindustry:steel_chisel` | 钢凿 | Steel Chisel | E3 | 同上 |
| item | `tcindustry:steel_file` | 钢锉 | Steel File | E3 | 精修、维护 |
| item | `tcindustry:steel_hammer` | 钢锻锤 | Steel Hammer | E3 | 同上 |
| item | `tcindustry:steel_hoe` | 钢锄 | Steel Hoe | E3 | T4 |
| item | `tcindustry:steel_ingot` | 钢锭 | Steel Ingot | E3 | c:ingots/steel |
| item | `tcindustry:steel_nugget` | 钢粒 | Steel Nugget | E3 | c:nuggets/steel |
| item | `tcindustry:steel_pickaxe` | 钢镐 | Steel Pickaxe | E3 | T4 |
| item | `tcindustry:steel_plate` | 钢板 | Steel Plate | E6 | c:plates/steel |
| item | `tcindustry:steel_plough_share` | 钢犁铧 | Steel Plough Share | E3 | 同上 |
| item | `tcindustry:steel_rod` | 钢棒 | Steel Rod | E6 | c:rods/steel |
| item | `tcindustry:steel_saw` | 钢锯 | Steel Saw | E3 | 木工、金工 |
| item | `tcindustry:steel_shovel` | 钢锹 | Steel Shovel | E3 | T4 |
| item | `tcindustry:steel_sickle` | 钢镰 | Steel Sickle | E3 | 同上 |
| item | `tcindustry:steel_tongs` | 钢火钳 | Steel Tongs | E3 | 同上 |
| item | `tcindustry:sulfur` | 硫磺 | Sulfur | E5 | 火药、硫酸 |
| item | `tcindustry:superphosphate` | 过磷酸钙 | Superphosphate | E6 | 化肥 |
| item | `tcindustry:synthetic_rubber` | 合成橡胶 | Synthetic Rubber | E8 | 密封、轮胎 |
| item | `tcindustry:tallow` | 牛油 | Tallow | E1 | 润滑、蜡烛 |
| item | `tcindustry:thread` | 线 | Thread | E1 | 织布原料 |
| item | `tcindustry:tin_can` | 空罐 | Tin Can | E6 | 罐头容器 |
| item | `tcindustry:tin_concentrate` | 锡精矿 | Tin Concentrate | E2 | 独立模式副产 |
| item | `tcindustry:tin_ingot` | 锡锭 | Tin Ingot | E2 | c:ingots/tin |
| item | `tcindustry:tin_nugget` | 锡粒 | Tin Nugget | E2 | c:nuggets/tin |
| item | `tcindustry:tin_plate` | 锡板 | Tin Plate | E4 | c:plates/tin |
| item | `tcindustry:tinplate` | 马口铁 | Tinplate | E6 | 罐头 |
| item | `tcindustry:titanium_alloy_ingot` | 钛合金锭 | Titanium Alloy Ingot | E8 | 航空航天 |
| item | `tcindustry:titanium_alloy_plate` | 钛合金板 | Titanium Alloy Plate | E8 | Orbit 结构 |
| item | `tcindustry:titanium_concentrate` | 钛精矿 | Titanium Concentrate | E8 | 独立模式副产 |
| item | `tcindustry:titanium_ingot` | 钛锭 | Titanium Ingot | E8 | c:ingots/titanium |
| item | `tcindustry:titanium_plate` | 钛板 | Titanium Plate | E8 | 航空航天 |
| item | `tcindustry:transistor` | 晶体管 | Transistor | E8 | 固态电子 |
| item | `tcindustry:tungsten_concentrate` | 钨精矿 | Tungsten Concentrate | E7 | 独立模式副产 |
| item | `tcindustry:tungsten_ingot` | 钨锭 | Tungsten Ingot | E7 | 灯丝、电极 |
| item | `tcindustry:tungsten_wire` | 钨丝 | Tungsten Wire | E7 | 白炽灯、电子管 |
| item | `tcindustry:turbine_core` | 涡轮核心 | Turbine Core | E8 | 燃气轮机、喷气发动机部件 |
| item | `tcindustry:unfired_brick` | 砖坯 | Unfired Brick | E4 | 待烧 |
| item | `tcindustry:unfired_clay_mold` | 铸模坯 | Unfired Clay Mold | E2 | 待烧 |
| item | `tcindustry:unfired_crucible` | 坩埚坯 | Unfired Crucible | E2 | 待烧 |
| item | `tcindustry:unfired_fire_brick` | 耐火砖坯 | Unfired Fire Brick | E4 | 待烧 |
| item | `tcindustry:unfired_porcelain` | 瓷坯 | Unfired Porcelain | E5 | 待烧 |
| item | `tcindustry:unfired_pot` | 陶坯 | Unfired Pot | E1 | 待烧 |
| item | `tcindustry:uo2_pellet` | 二氧化铀芯块 | UO₂ Pellet | E8 | 燃料原料 |
| item | `tcindustry:uranium_concentrate` | 铀精矿 | Uranium Concentrate | E8 | 独立模式副产 |
| item | `tcindustry:uranium_tailings` | 铀尾矿 | Uranium Tailings | E8 | 放射性废料 |
| item | `tcindustry:vacuum_tube` | 电子管 | Vacuum Tube | E7 | 早期电子 |
| item | `tcindustry:vitrified_waste` | 玻璃固化废物 | Vitrified Waste | E8 | 长期贮存 |
| item | `tcindustry:washed_clay` | 洗净黏土 | Washed Clay | E1 | 陶坯、瓷器、砖原料 |
| item | `tcindustry:wax_model` | 蜡模 | Wax Model | E2 | 失蜡铸造 |
| item | `tcindustry:wind_turbine_blade` | 风机叶片 | Wind Turbine Blade | E9 | 风力机部件 |
| item | `tcindustry:wood_tar_bucket` | 木焦油桶 | Wood Tar Bucket | E6 | 流体桶 |
| item | `tcindustry:wooden_gear` | 木齿轮 | Wooden Gear | E3 | 维修件 |
| item | `tcindustry:wooden_pan` | 木淘盘 | Wooden Pan | E1 | 淘洗 |
| item | `tcindustry:wool_cloth` | 毛布 | Wool Cloth | E1 | 布 |
| item | `tcindustry:workpiece` | 锻件 | Workpiece | E2 | 锻造中间品，带温度与锻打状态 |
| item | `tcindustry:wrench` | 扳手 | Wrench | E3 | 工程工具；显示 HUD 控件 |
| item | `tcindustry:yellowcake` | 黄饼 | Yellowcake | E8 | 铀浓缩物 |
| item | `tcindustry:zinc_concentrate` | 锌精矿 | Zinc Concentrate | E3 | 独立模式副产 |
| item | `tcindustry:zinc_ingot` | 锌锭 | Zinc Ingot | E4 | c:ingots/zinc |
| item | `tcindustry:zircaloy_tube` | 锆合金管 | Zircaloy Tube | E8 | 包壳 |
| item | `tcindustry:zircon` | 锆英砂 | Zircon | E8 | 锆原料 |
| item | `tcindustry:zirconium_ingot` | 锆锭 | Zirconium Ingot | E8 | 锆合金包壳 |
| fluid | `tcindustry:ammonia` | 氨 | Ammonia | E7 | 合成氨；气体 |
| fluid | `tcindustry:ammonia_liquor` | 氨水 | Ammonia Liquor | E6 | 焦炉副产 |
| fluid | `tcindustry:argon` | 氩气 | Argon | E7 | 保护气；Orbit 电推进 |
| fluid | `tcindustry:biodiesel` | 生物柴油 | Biodiesel | E7 | 柴油替代；可装桶 |
| fluid | `tcindustry:chlorine` | 氯气 | Chlorine | E7 | 氯碱；气体 |
| fluid | `tcindustry:coal_gas` | 煤气 | Coal Gas | E6 | 焦炉、发生炉；气体 |
| fluid | `tcindustry:coal_tar` | 煤焦油 | Coal Tar | E6 | 焦炉副产；可装桶 |
| fluid | `tcindustry:crude_oil` | 原油 | Crude Oil | E7 | 储层抽取；可装桶 |
| fluid | `tcindustry:deuterium` | 氘 | Deuterium | E10 | 聚变燃料 |
| fluid | `tcindustry:diesel` | 柴油 | Diesel | E7 | c:fuels/diesel；可装桶 |
| fluid | `tcindustry:ethanol` | 乙醇 | Ethanol | E6 | 发酵蒸馏；可装桶 |
| fluid | `tcindustry:ethylene` | 乙烯 | Ethylene | E8 | 裂解；气体 |
| fluid | `tcindustry:flowing_biodiesel` | 生物柴油（流动） | Flowing Biodiesel | E7 | 流动态 |
| fluid | `tcindustry:flowing_coal_tar` | 煤焦油（流动） | Flowing Coal Tar | E6 | 流动态 |
| fluid | `tcindustry:flowing_crude_oil` | 原油（流动） | Flowing Crude Oil | E7 | 流动态 |
| fluid | `tcindustry:flowing_diesel` | 柴油（流动） | Flowing Diesel | E7 | 流动态 |
| fluid | `tcindustry:flowing_ethanol` | 乙醇（流动） | Flowing Ethanol | E6 | 流动态 |
| fluid | `tcindustry:flowing_fuel_oil` | 燃料油（流动） | Flowing Fuel Oil | E7 | 流动态 |
| fluid | `tcindustry:flowing_gasoline` | 汽油（流动） | Flowing Gasoline | E7 | 流动态 |
| fluid | `tcindustry:flowing_kerosene` | 煤油（流动） | Flowing Kerosene | E7 | 流动态 |
| fluid | `tcindustry:flowing_lubricating_oil` | 润滑油（流动） | Flowing Lubricating Oil | E7 | 流动态 |
| fluid | `tcindustry:flowing_molten_aluminium` | 熔融铝（流动） | Flowing Molten Aluminium | E7 | 流动态 |
| fluid | `tcindustry:flowing_molten_brass` | 熔融黄铜（流动） | Flowing Molten Brass | E3 | 流动态 |
| fluid | `tcindustry:flowing_molten_bronze` | 熔融青铜（流动） | Flowing Molten Bronze | E2 | 流动态 |
| fluid | `tcindustry:flowing_molten_copper` | 熔融铜（流动） | Flowing Molten Copper | E1 | 流动态 |
| fluid | `tcindustry:flowing_molten_gold` | 熔融金（流动） | Flowing Molten Gold | E1 | 流动态 |
| fluid | `tcindustry:flowing_molten_iron` | 熔融铁（流动） | Flowing Molten Iron | E3 | 流动态 |
| fluid | `tcindustry:flowing_molten_lead` | 熔融铅（流动） | Flowing Molten Lead | E2 | 流动态 |
| fluid | `tcindustry:flowing_molten_pig_iron` | 熔融生铁（流动） | Flowing Molten Pig Iron | E4 | 流动态 |
| fluid | `tcindustry:flowing_molten_silver` | 熔融银（流动） | Flowing Molten Silver | E2 | 流动态 |
| fluid | `tcindustry:flowing_molten_steel` | 熔融钢（流动） | Flowing Molten Steel | E3 | 流动态 |
| fluid | `tcindustry:flowing_molten_tin` | 熔融锡（流动） | Flowing Molten Tin | E2 | 流动态 |
| fluid | `tcindustry:flowing_molten_zinc` | 熔融锌（流动） | Flowing Molten Zinc | E4 | 流动态 |
| fluid | `tcindustry:flowing_naphtha` | 石脑油（流动） | Flowing Naphtha | E7 | 流动态 |
| fluid | `tcindustry:flowing_wood_tar` | 木焦油（流动） | Flowing Wood Tar | E6 | 流动态 |
| fluid | `tcindustry:fuel_oil` | 燃料油 | Fuel Oil | E7 | 蒸馏产物；可装桶 |
| fluid | `tcindustry:gasoline` | 汽油 | Gasoline | E7 | c:fuels/gasoline；可装桶 |
| fluid | `tcindustry:hydrochloric_acid` | 盐酸 | Hydrochloric Acid | E6 | 酸 |
| fluid | `tcindustry:hydrofluoric_acid` | 氢氟酸 | Hydrofluoric Acid | E7 | 酸 |
| fluid | `tcindustry:hydrogen` | 氢气 | Hydrogen | E7 | 气体 |
| fluid | `tcindustry:hypergolic_propellant` | 自燃推进剂 | Hypergolic Propellant | E8 | Orbit 姿控 |
| fluid | `tcindustry:kerosene` | 煤油 | Kerosene | E7 | c:fuels/kerosene；可装桶 |
| fluid | `tcindustry:liquefied_natural_gas` | 液化天然气 | Liquefied Natural Gas | E8 | Orbit 甲烷推进剂 |
| fluid | `tcindustry:liquid_hydrogen` | 液氢 | Liquid Hydrogen | E8 | Orbit 推进剂 |
| fluid | `tcindustry:liquid_oxygen` | 液氧 | Liquid Oxygen | E7 | Orbit 推进剂 |
| fluid | `tcindustry:lubricating_oil` | 润滑油 | Lubricating Oil | E7 | 维护；可装桶 |
| fluid | `tcindustry:mercury` | 汞 | Mercury | E4 | 辰砂焙烧 |
| fluid | `tcindustry:molten_aluminium` | 熔融铝 | Molten Aluminium | E7 | 同上 |
| fluid | `tcindustry:molten_brass` | 熔融黄铜 | Molten Brass | E3 | 同上 |
| fluid | `tcindustry:molten_bronze` | 熔融青铜 | Molten Bronze | E2 | 同上 |
| fluid | `tcindustry:molten_copper` | 熔融铜 | Molten Copper | E1 | 144 mB = 1 锭 |
| fluid | `tcindustry:molten_gold` | 熔融金 | Molten Gold | E1 | 同上 |
| fluid | `tcindustry:molten_iron` | 熔融铁 | Molten Iron | E3 | 同上 |
| fluid | `tcindustry:molten_lead` | 熔融铅 | Molten Lead | E2 | 同上 |
| fluid | `tcindustry:molten_pig_iron` | 熔融生铁 | Molten Pig Iron | E4 | 同上 |
| fluid | `tcindustry:molten_silver` | 熔融银 | Molten Silver | E2 | 同上 |
| fluid | `tcindustry:molten_steel` | 熔融钢 | Molten Steel | E3 | 同上 |
| fluid | `tcindustry:molten_tin` | 熔融锡 | Molten Tin | E2 | 同上 |
| fluid | `tcindustry:molten_zinc` | 熔融锌 | Molten Zinc | E4 | 同上 |
| fluid | `tcindustry:naphtha` | 石脑油 | Naphtha | E7 | 裂化原料；可装桶 |
| fluid | `tcindustry:natural_gas` | 天然气 | Natural Gas | E7 | 储层采气；气体 |
| fluid | `tcindustry:nitric_acid` | 硝酸 | Nitric Acid | E5 | 酸 |
| fluid | `tcindustry:nitrogen` | 氮气 | Nitrogen | E7 | 气体 |
| fluid | `tcindustry:oxygen` | 氧气 | Oxygen | E7 | 气体 |
| fluid | `tcindustry:phosphoric_acid` | 磷酸 | Phosphoric Acid | E7 | 酸 |
| fluid | `tcindustry:pyroligneous_acid` | 木醋液 | Pyroligneous Acid | E6 | 干馏副产 |
| fluid | `tcindustry:steam` | 蒸汽 | Steam | E6 | 等级存于 tcindustry:steam_grade；气体 |
| fluid | `tcindustry:sulfuric_acid` | 硫酸 | Sulfuric Acid | E5 | 酸 |
| fluid | `tcindustry:tritium` | 氚 | Tritium | E10 | 聚变燃料 |
| fluid | `tcindustry:uranium_hexafluoride` | 六氟化铀 | Uranium Hexafluoride | E8 | 浓缩度存于 tcindustry:enrichment |
| fluid | `tcindustry:wood_tar` | 木焦油 | Wood Tar | E6 | 干馏；可装桶 |
| block_entity | `tcindustry:automation_device` | 自动化设备 | Automation Device | E7 | 继电器盘、传感器、PID、PLC、控制室 |
| block_entity | `tcindustry:boiler` | 锅炉 | Boiler | E6 | 全部锅炉与蒸汽发生器 |
| block_entity | `tcindustry:casting_table` | 铸造台 | Casting Table | E2 | 浇铸 |
| block_entity | `tcindustry:chemical_plant` | 化工装置 | Chemical Plant | E5 | 铅室、反应釜、蒸馏塔、合成氨塔、空分、浮选、裂化 |
| block_entity | `tcindustry:combustion_engine` | 内燃机 | Combustion Engine | E7 | 煤气机、汽油机、柴油机、燃气轮机 |
| block_entity | `tcindustry:conveyor` | 输送 | Conveyor | E1 | 溜槽、提升机、传送带、行车、分拣器、机械臂 |
| block_entity | `tcindustry:electric_consumer` | 电力负荷 | Electric Consumer | E7 | 灯与通用电负荷 |
| block_entity | `tcindustry:electric_motor` | 电动机 | Electric Motor | E7 | 三种电动机 |
| block_entity | `tcindustry:emission_control` | 排放治理 | Emission Control | E4 | 烟囱、除尘、脱硫、脱硝、污水、尾矿、焚烧 |
| block_entity | `tcindustry:energy_storage` | 储能 | Energy Storage | E7 | 三种储能 |
| block_entity | `tcindustry:extraction_well` | 采掘井 | Extraction Well | E7 | 钻机、抽油机、井口、地热井 |
| block_entity | `tcindustry:fluid_pipe` | 管道 | Fluid Pipe | E3 | 四种管 |
| block_entity | `tcindustry:forge` | 锻造 | Forge | E2 | 锻炉、锻造砧 |
| block_entity | `tcindustry:generator` | 发电机 | Generator | E7 | 全部发电设备 |
| block_entity | `tcindustry:grid_node` | 电网节点 | Grid Node | E7 | 线、杆、变压器、变电站、断路器、电表 |
| block_entity | `tcindustry:handcraft_station` | 手工工作站 | Handcraft Station | E1 | 磨盘、织机、鞣制坑、淘洗台、陶轮 |
| block_entity | `tcindustry:kiln` | 窑 | Kiln | E1 | 坑窑、升焰窑、砖窑、龙窑、石灰窑、回转窑、炭窑、玻璃窑 |
| block_entity | `tcindustry:kinetic_part` | 传动件 | Kinetic Part | E3 | 轴、齿轮、皮带轮、离合器、飞轮 |
| block_entity | `tcindustry:nuclear_facility` | 核设施 | Nuclear Facility | E8 | 燃料循环与乏燃料设施 |
| block_entity | `tcindustry:power_source` | 动力源 | Power Source | E3 | 人力、畜力、水车、风车 |
| block_entity | `tcindustry:process_machine` | 加工机器 | Process Machine | E3 | 磨、锤、轧机、机床、搅拌、罐装 |
| block_entity | `tcindustry:pump` | 泵 | Pump | E5 | 泵 |
| block_entity | `tcindustry:reactor` | 反应堆 | Reactor | E8 | 裂变堆与托卡马克 |
| block_entity | `tcindustry:semiconductor_fab` | 半导体厂 | Semiconductor Fab | E8 | 单晶炉、光刻机 |
| block_entity | `tcindustry:smelter` | 熔炼炉 | Smelter | E2 | 坩埚炉、块炼炉、高炉、焦炉、炼钢炉、电弧炉、电解槽 |
| block_entity | `tcindustry:steam_engine` | 蒸汽机 | Steam Engine | E6 | 三种蒸汽机 |
| block_entity | `tcindustry:tank` | 储罐 | Tank | E6 | 储罐、陶罐、装卸臂 |
| block_entity | `tcindustry:works_office` | 工业区账房 | Works Office | E1 | 工业区数据 |
| menu | `tcindustry:boiler` | 锅炉界面 | Boiler | E6 | 压力、水位、安全阀 |
| menu | `tcindustry:control_room` | 控制室界面 | Control Room | E9 | 集中监控 |
| menu | `tcindustry:forging` | 锻造界面 | Forging | E2 | 节拍条（§9.4） |
| menu | `tcindustry:furnace` | 炉窑界面 | Furnace | E1 | 温度曲线、炉衬 |
| menu | `tcindustry:generator` | 发电机界面 | Generator | E7 | 出力、频率 |
| menu | `tcindustry:grid_node` | 电网节点界面 | Grid Node | E7 | 电压、负荷、切负荷顺序 |
| menu | `tcindustry:machine` | 机器界面 | Machine | E1 | 31 §5.8 机器模板（§10.1） |
| menu | `tcindustry:plc` | PLC 编程界面 | PLC | E9 | 梯形图编辑 |
| menu | `tcindustry:reactor` | 反应堆界面 | Reactor | E8 | 控制棒、功率、安全系统 |
| menu | `tcindustry:sorter` | 分拣器界面 | Sorter | E7 | 过滤槽 |
| menu | `tcindustry:tank` | 储罐界面 | Tank | E6 | 流体、管口 |
| menu | `tcindustry:works_office` | 账房界面 | Works Office | E1 | 工业区汇总 |
| keybind | `tcindustry:network_overlay` | 网络标注开关 | Toggle Network Overlay | — | 默认未绑定 |
| payload | `tcindustry:accident_fx` | 事故特效 | Accident FX | — | S→C，事件 |
| payload | `tcindustry:forge_strike` | 锻打 | Forge Strike | — | C→S，20/s |
| payload | `tcindustry:grid_dispatch` | 电网调度 | Grid Dispatch | — | C→S，5/s |
| payload | `tcindustry:machine_anim` | 机器动画 | Machine Animation | — | S→C，4 Hz |
| payload | `tcindustry:machine_config` | 机器设置 | Machine Config | — | C→S，10/s |
| payload | `tcindustry:machine_sync` | 机器状态同步 | Machine Sync | — | S→C，4 Hz/界面 |
| payload | `tcindustry:multiblock_preview` | 多方块预览 | Multiblock Preview | — | S→C，按请求 |
| payload | `tcindustry:network_annotations` | 网络标注 | Network Annotations | — | S→C，开启时 1 Hz |
| payload | `tcindustry:plc_upload` | PLC 上传 | PLC Upload | — | C→S，1/s |
| payload | `tcindustry:production_action` | 生产页签操作 | Production Action | — | C→S，5/s |
| payload | `tcindustry:production_snapshot` | 生产快照 | Production Snapshot | — | S→C，页签打开时 |
| payload | `tcindustry:reactor_command` | 反应堆指令 | Reactor Command | — | C→S，5/s |
| payload | `tcindustry:sorter_filter` | 分拣过滤 | Sorter Filter | — | C→S，10/s |
| hud_widget | `tcindustry:network_status` | 网络状态 | Network Status | — | 扩展等级，右中；手持 #tcindustry:engineering_tools 时出现 |
| civ_tab | `tcindustry:production` | 生产 | Production | — | 经济分组，排序 450 |
| region_layer | `tcindustry:emission` | 排放 | Emissions | — | 地区排放指数图层 |
| sound | `tcindustry:block.boiler.hiss` | 锅炉嘶声 | Boiler Hiss | E6 | 运行 |
| sound | `tcindustry:block.boiler.safety_valve` | 安全阀泄压 | Safety Valve | E6 | 超压 |
| sound | `tcindustry:block.converter.blow` | 转炉吹炼 | Converter Blow | E6 | 贝塞麦与氧气转炉 |
| sound | `tcindustry:block.forge.quench` | 淬火 | Quench | E3 | 工件入水 |
| sound | `tcindustry:block.forge.strike` | 锻打 | Forge Strike | E2 | 锤击 |
| sound | `tcindustry:block.furnace.roar` | 炉火轰鸣 | Furnace Roar | E2 | 熔炼炉运行 |
| sound | `tcindustry:block.grid.transformer_hum` | 变压器嗡鸣 | Transformer Hum | E7 | 循环 |
| sound | `tcindustry:block.kiln.crackle` | 窑火噼啪 | Kiln Crackle | E1 | 窑运行 |
| sound | `tcindustry:block.pipe.leak` | 管道泄漏 | Pipe Leak | E3 | 管道故障 |
| sound | `tcindustry:event.accident.explosion` | 事故爆炸 | Industrial Explosion | E6 | 锅炉、化工、粉尘事故 |
| sound | `tcindustry:event.grid.breaker_trip` | 断路器跳闸 | Breaker Trip | E7 | 切负荷 |
| sound | `tcindustry:event.multiblock.formed` | 结构成形 | Multiblock Formed | E1 | 多方块首次成形 |
| sound | `tcindustry:event.process.complete` | 工序完成 | Process Complete | E1 | 批次完成 |
| sound | `tcindustry:event.reactor.alarm` | 反应堆警报 | Reactor Alarm | E8 | 安全系统动作 |
| sound | `tcindustry:machine.bearing_squeal` | 轴承尖啸 | Bearing Squeal | E3 | 磨损 NEEDS_REPAIR |
| sound | `tcindustry:machine.belt_slip` | 皮带打滑 | Belt Slip | E4 | 过载 |
| sound | `tcindustry:machine.centrifuge.whine` | 离心机尖鸣 | Centrifuge Whine | E8 | 循环 |
| sound | `tcindustry:machine.conveyor.run` | 传送带运转 | Conveyor Running | E6 | 循环 |
| sound | `tcindustry:machine.diesel_engine.run` | 柴油机运转 | Diesel Engine Running | E7 | 循环 |
| sound | `tcindustry:machine.fault.generic` | 机器故障 | Machine Fault | E1 | FAULT 状态 |
| sound | `tcindustry:machine.gas_turbine.run` | 燃气轮机运转 | Gas Turbine Running | E8 | 循环 |
| sound | `tcindustry:machine.gear_whine` | 齿轮啸叫 | Gear Whine | E3 | 高转速传动 |
| sound | `tcindustry:machine.jam` | 卡料 | Machine Jam | E3 | 输出堵塞 |
| sound | `tcindustry:machine.lathe.cut` | 车削 | Lathe Cutting | E6 | 机床运行 |
| sound | `tcindustry:machine.loom.clack` | 织机咔嗒 | Loom Clack | E1 | 随节拍 |
| sound | `tcindustry:machine.pump.run` | 泵运转 | Pump Running | E5 | 循环 |
| sound | `tcindustry:machine.steam_engine.chuff` | 蒸汽机排汽 | Steam Engine Chuff | E6 | 随转速 |
| sound | `tcindustry:machine.steam_engine.run` | 蒸汽机运转 | Steam Engine Running | E6 | 循环 |
| sound | `tcindustry:machine.trip_hammer.strike` | 水力锻锤击打 | Trip Hammer Strike | E4 | 随节拍 |
| sound | `tcindustry:machine.turbine.run` | 汽轮机运转 | Turbine Running | E7 | 循环 |
| sound | `tcindustry:machine.waterwheel.turn` | 水车转动 | Waterwheel Turning | E4 | 循环 |
| sound | `tcindustry:machine.windmill.creak` | 风车吱嘎 | Windmill Creak | E4 | 循环 |

### 5.8 通途 `tcnetworks`（210 项，来源 [14-networks.md](14-networks.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcnetworks:airfield_core` | 机场核心 | Airfield Core | E8 | 机场多方块核心 |
| block | `tcnetworks:asphalt_road` | 沥青路 | Asphalt Road | E7 | R5 路面 |
| block | `tcnetworks:asrs_rack` | 立体仓库 | AS/RS Rack | E9 | 自动化立体仓储 |
| block | `tcnetworks:bollard` | 系船柱 | Bollard | E3 | 船舶系泊点 |
| block | `tcnetworks:buffer_stop` | 车挡 | Buffer Stop | E6 | 尽头线终点 |
| block | `tcnetworks:bus_stop` | 公交站 | Bus Stop | E7 | 公路客运站点 |
| block | `tcnetworks:cab_signal_beacon` | 应答器 | Cab Signal Beacon | E9 | 机车信号，取代地面信号 |
| block | `tcnetworks:canal_lock_gate` | 船闸闸门 | Canal Lock Gate | E4 | 船闸闸门 |
| block | `tcnetworks:cart_shed` | 车棚 | Cart Shed | E2 | 停放并维护畜力车 |
| block | `tcnetworks:catenary_mast` | 接触网支柱 | Catenary Mast | E7 | 电气化铁路供电 |
| block | `tcnetworks:coal_stage` | 煤台 | Coal Stage | E6 | 蒸汽机车补煤 |
| block | `tcnetworks:color_light_signal` | 色灯信号 | Color Light Signal | E7 | 多显示信号；灯位与符号表示状态 |
| block | `tcnetworks:concrete_road` | 混凝土路 | Concrete Road | E8 | R6 路面，高速公路 |
| block | `tcnetworks:container_terminal` | 集装箱码头 | Container Terminal | E9 | TEU 港口核心 |
| block | `tcnetworks:control_tower` | 塔台 | Control Tower | E8 | 起降排序 |
| block | `tcnetworks:crate` | 货箱 | Crate | E1 | 1 货位 货包容器，可装车 |
| block | `tcnetworks:crew_office` | 雇工站 | Crew Office | E2 | 无 Settlements 时雇佣乘务员 |
| block | `tcnetworks:ctc_panel` | CTC 控制台 | CTC Panel | E8 | 调度集中控制 |
| block | `tcnetworks:dispatch_board` | 调度板 | Dispatch Board | E2 | 发布物流订单 |
| block | `tcnetworks:dispatch_terminal` | 调度终端 | Dispatch Terminal | E9 | 自动调度与全网实时数据 |
| block | `tcnetworks:distribution_center` | 配送中心 | Distribution Center | E7 | 区域分拨仓储 |
| block | `tcnetworks:dry_dock` | 干船坞 | Dry Dock | E6 | 建造与维修金属船 |
| block | `tcnetworks:engine_shed` | 机车库 | Engine Shed | E6 | 装配与维修机车车辆 |
| block | `tcnetworks:fiber_optic_cable` | 光纤 | Fiber Optic Cable | E9 | 计算机网络线路 |
| block | `tcnetworks:freight_depot` | 货栈 | Freight Depot | E2 | 最早的站点核心 |
| block | `tcnetworks:freight_station` | 货运站 | Freight Station | E6 | 铁路货运站点核心 |
| block | `tcnetworks:fuel_pump` | 加油站 | Fuel Pump | E7 | 液体燃料补给 |
| block | `tcnetworks:gantry_crane` | 龙门吊 | Gantry Crane | E9 | 集装箱装卸 |
| block | `tcnetworks:gravel_road` | 碎石路 | Gravel Road | E3 | R2 路面 |
| block | `tcnetworks:guard_rail` | 护栏 | Guard Rail | E8 | 降低高速事故严重度 |
| block | `tcnetworks:hangar` | 机库 | Hangar | E8 | 装配与维修飞机 |
| block | `tcnetworks:harbor_master` | 港务所 | Harbor Master | E3 | 港口核心，管理泊位与航线 |
| block | `tcnetworks:level_crossing` | 道口 | Level Crossing | E6 | 道路与铁路平交 |
| block | `tcnetworks:loading_bay` | 装卸台 | Loading Bay | E2 | 站点装卸位 |
| block | `tcnetworks:lock_controller` | 船闸控制器 | Lock Controller | E4 | 控制船闸注排水 |
| block | `tcnetworks:macadam_road` | 碎石沥青路 | Macadam Road | E6 | R4 路面 |
| block | `tcnetworks:maglev_guideway` | 磁浮导轨 | Maglev Guideway | E10 | 磁浮线路，64 b/s（舱内模式 140） |
| block | `tcnetworks:milestone` | 里程碑 | Milestone | E3 | 显示到最近站点的距离（kb） |
| block | `tcnetworks:network_router` | 路由器 | Network Router | E9 | 网络间路由 |
| block | `tcnetworks:network_switch` | 交换机柜 | Network Switch | E9 | 计算机网络节点 |
| block | `tcnetworks:packed_earth_road` | 夯土路 | Packed Earth Road | E2 | R1 路面；雨天泥泞降速 |
| block | `tcnetworks:pallet_rack` | 托盘货架 | Pallet Rack | E7 | 托盘化仓储单元 |
| block | `tcnetworks:pipeline` | 管道 | Pipeline | E8 | 长距离流体输送线段 |
| block | `tcnetworks:pipeline_pump_station` | 管道泵站 | Pipeline Pump Station | E8 | 管道加压 |
| block | `tcnetworks:pipeline_terminal` | 管道终端 | Pipeline Terminal | E8 | 管道进出口 |
| block | `tcnetworks:platform_edge` | 站台边缘 | Platform Edge | E6 | 车站站台识别 |
| block | `tcnetworks:quay` | 码头 | Quay | E3 | 港口泊位 |
| block | `tcnetworks:radio_mast` | 无线电台 | Radio Mast | E8 | 无线覆盖，船舶与飞机通信 |
| block | `tcnetworks:rail_concrete_sleeper` | 混凝土轨枕钢轨 | Concrete Sleeper Rail | E8 | 最高 45 b/s |
| block | `tcnetworks:rail_iron` | 铁轨 | Iron Rail | E6 | 早期铁路，最高 16 b/s |
| block | `tcnetworks:rail_slab_track` | 无砟轨道 | Slab Track | E9 | 高速铁路，最高 80 b/s |
| block | `tcnetworks:rail_steel` | 钢轨 | Steel Rail | E7 | 干线铁路，最高 32 b/s |
| block | `tcnetworks:railway_station` | 火车站 | Railway Station | E6 | 客运站点核心 |
| block | `tcnetworks:road_curb` | 路缘石 | Road Curb | E3 | 道路边界，计入道路宽度识别 |
| block | `tcnetworks:road_depot` | 养路站 | Road Depot | E3 | 自动修复范围内路面磨损 |
| block | `tcnetworks:road_marking` | 道路标线 | Road Marking | E7 | 车道识别；形状区分实线/虚线 |
| block | `tcnetworks:road_sign` | 路牌 | Road Sign | E3 | 可写目的地，路线规划用作途经点 |
| block | `tcnetworks:runway_light` | 跑道灯 | Runway Light | E8 | 跑道识别与夜间起降 |
| block | `tcnetworks:semaphore_signal` | 臂板信号 | Semaphore Signal | E6 | 闭塞分区边界；臂板角度表示状态 |
| block | `tcnetworks:signal_box` | 信号楼 | Signal Box | E6 | 联锁与进路控制 |
| block | `tcnetworks:slipway` | 船台 | Slipway | E3 | 分阶段造船 |
| block | `tcnetworks:steam_crane` | 蒸汽起重机 | Steam Crane | E6 | 港口与货场装卸 |
| block | `tcnetworks:stone_paved_road` | 石板路 | Stone Paved Road | E3 | R3 路面；伟大道路里程碑的主要路面 |
| block | `tcnetworks:storage_rack` | 货架 | Storage Rack | E4 | 仓库容量单元 |
| block | `tcnetworks:suspension_cable` | 悬索桥主缆 | Suspension Cable | E8 | 悬索桥，单跨 ≤ 64 格 |
| block | `tcnetworks:switch_motor` | 转辙机 | Switch Motor | E7 | 远程控制道岔 |
| block | `tcnetworks:switch_stand` | 道岔扳道器 | Switch Stand | E6 | 手动道岔 |
| block | `tcnetworks:telegraph_key` | 电报键 | Telegraph Key | E7 | 手动发报 |
| block | `tcnetworks:telegraph_office` | 电报局 | Telegraph Office | E7 | 信息网络节点 |
| block | `tcnetworks:telegraph_pole` | 电报线杆 | Telegraph Pole | E7 | 架设电报线路 |
| block | `tcnetworks:telephone` | 电话机 | Telephone | E7 | 站点间即时通话，降低调度延迟 |
| block | `tcnetworks:telephone_exchange` | 电话交换机 | Telephone Exchange | E7 | 电话网络节点 |
| block | `tcnetworks:timber_trestle` | 木栈桥构件 | Timber Trestle | E2 | 木桥桥墩与桥面支撑 |
| block | `tcnetworks:traction_substation` | 牵引变电所 | Traction Substation | E7 | 电气化铁路供电，无 Industry 时可燃煤发电 |
| block | `tcnetworks:traffic_light` | 红绿灯 | Traffic Light | E7 | 路口信号；灯位上下区分状态 |
| block | `tcnetworks:trail_cairn` | 路标石堆 | Trail Cairn | E0 | 标记小径与路线点，供路线规划识别 |
| block | `tcnetworks:treadwheel_crane` | 踏轮起重机 | Treadwheel Crane | E4 | 港口装卸 |
| block | `tcnetworks:trodden_trail` | 踏出小径 | Trodden Trail | E0 | 草地被反复踩踏后自动生成，速度系数 1.1 |
| block | `tcnetworks:truck_depot` | 卡车站 | Truck Depot | E7 | 汽车装配、维修与公路货运站点 |
| block | `tcnetworks:truss_bridge_segment` | 桁架桥段 | Truss Bridge Segment | E6 | 钢桁架，单跨 ≤ 16 格 |
| block | `tcnetworks:tunnel_lining` | 隧道内衬 | Tunnel Lining | E6 | 防止隧道坍塌，计入隧道识别 |
| block | `tcnetworks:tunnel_portal` | 隧道洞门 | Tunnel Portal | E6 | 隧道两端，命名隧道 |
| block | `tcnetworks:warehouse_core` | 仓库核心 | Warehouse Core | E4 | 仓库多方块核心 |
| block | `tcnetworks:water_tower` | 水塔 | Water Tower | E6 | 蒸汽机车补水 |
| block | `tcnetworks:waystation` | 驿站 | Waystation | E3 | 道路站点，换马与补给 |
| block | `tcnetworks:wooden_wagonway` | 木轨 | Wooden Wagonway | E5 | 马拉轨道，最高 6 b/s |
| item | `tcnetworks:aero_engine` | 航空发动机 | Aero Engine | E8 | 螺旋桨飞机动力 |
| item | `tcnetworks:bogie` | 转向架 | Bogie | E6 | 铁路车辆零件 |
| item | `tcnetworks:cargo_barrel` | 货桶 | Cargo Barrel | E2 | 1 货位 液体或散料货包 |
| item | `tcnetworks:cargo_sack` | 货袋 | Cargo Sack | E1 | 1 货位 货包，可背负或驮运 |
| item | `tcnetworks:combustion_powertrain` | 内燃动力总成 | Combustion Powertrain | E7 | 汽车与内燃机车动力 |
| item | `tcnetworks:crucible_steel` | 坩埚钢 | Crucible Steel | E6 | 无 Industry 时的钢回退材料 |
| item | `tcnetworks:electric_traction_motor` | 牵引电机 | Electric Traction Motor | E7 | 电力机车与电车动力 |
| item | `tcnetworks:fuel_canister` | 燃料罐 | Fuel Canister | E7 | 1,000 mB 液体燃料；无 Industry 时的燃料回退 |
| item | `tcnetworks:harness` | 挽具 | Harness | E2 | 役畜套车 |
| item | `tcnetworks:hull_section` | 船体段 | Hull Section | E2 | 造船零件 |
| item | `tcnetworks:iron_tyred_wheel` | 包铁车轮 | Iron-Tyred Wheel | E6 | 车辆零件 |
| item | `tcnetworks:mast` | 桅杆 | Mast | E2 | 造船零件，帆型决定数量 |
| item | `tcnetworks:pack_saddle` | 驮鞍 | Pack Saddle | E1 | 使驮兽可驮 1-2 货位 |
| item | `tcnetworks:pallet` | 托盘 | Pallet | E7 | 4 货位 托盘化货包 |
| item | `tcnetworks:pneumatic_tyre` | 充气轮胎 | Pneumatic Tyre | E7 | 汽车零件 |
| item | `tcnetworks:railway_wheelset` | 轮对 | Railway Wheelset | E6 | 铁路车辆零件 |
| item | `tcnetworks:rammer` | 夯具 | Rammer | E2 | 把泥土夯实为夯土路 |
| item | `tcnetworks:repair_kit` | 维修工具包 | Repair Kit | E6 | 恢复载具车况 |
| item | `tcnetworks:road_planner` | 路线规划板 | Road Planner | E2 | 规划道路与路线，显示地形代价 |
| item | `tcnetworks:sail` | 帆 | Sail | E2 | 造船零件 |
| item | `tcnetworks:shipping_container` | 集装箱 | Shipping Container | E9 | 1 TEU 货包 |
| item | `tcnetworks:spoked_wheel` | 辐条轮 | Spoked Wheel | E4 | 马车零件 |
| item | `tcnetworks:steam_powertrain` | 蒸汽动力总成 | Steam Powertrain | E6 | 机车与蒸汽船动力 |
| item | `tcnetworks:survey_level` | 水准仪 | Survey Level | E3 | 测坡度与地形代价，规划桥隧 |
| item | `tcnetworks:telegram` | 电报纸条 | Telegram | E7 | 电报消息实体化，可存档 |
| item | `tcnetworks:ticket` | 车票 | Ticket | E6 | 客运凭证；有 Exchange 时记录票价 |
| item | `tcnetworks:track_layer` | 铺轨器 | Track Layer | E5 | 沿规划曲线铺设轨道 |
| item | `tcnetworks:turbofan` | 涡扇发动机 | Turbofan | E9 | 喷气飞机动力 |
| item | `tcnetworks:waybill` | 运单 | Waybill | E2 | 记录批次发货人、收货人与货物；信息网络前的订单凭证 |
| item | `tcnetworks:wooden_wheel` | 木轮 | Wooden Wheel | E2 | 车辆零件 |
| entity | `tcnetworks:automated_guided_vehicle` | 自动导引车 | Automated Guided Vehicle | E9 | 港口与仓库内自动搬运 |
| entity | `tcnetworks:automobile` | 汽车 | Automobile | E7 | 客 4，14 b/s |
| entity | `tcnetworks:bulk_carrier` | 散货船 | Bulk Carrier | E8 | 散料 2,000 t |
| entity | `tcnetworks:bus` | 公共汽车 | Bus | E7 | 客 30 |
| entity | `tcnetworks:canal_barge` | 运河驳船 | Canal Barge | E4 | 纤道拖曳，16 货位 |
| entity | `tcnetworks:caravel` | 卡拉维尔船 | Caravel | E5 | 纵帆，可逆风，24 货位 |
| entity | `tcnetworks:cargo_jet` | 货运喷气机 | Cargo Jet | E9 | 16 货位 / 60 t |
| entity | `tcnetworks:coach` | 驿车 | Coach | E4 | 客 6，4 马 |
| entity | `tcnetworks:cog` | 柯克船 | Cog | E4 | 横帆，24 货位 |
| entity | `tcnetworks:container_ship` | 集装箱船 | Container Ship | E9 | 48 TEU，甲板可见 |
| entity | `tcnetworks:container_wagon` | 集装箱平车 | Container Wagon | E9 | 2 TEU |
| entity | `tcnetworks:delivery_drone` | 货运无人机 | Delivery Drone | E9 | 1 货箱，末端配送 |
| entity | `tcnetworks:diesel_locomotive` | 内燃机车 | Diesel Locomotive | E8 | 无需电气化，牵引 2,000 t |
| entity | `tcnetworks:dugout_canoe` | 独木舟 | Dugout Canoe | E1 | 划桨，3.5 b/s |
| entity | `tcnetworks:electric_locomotive` | 电力机车 | Electric Locomotive | E7 | 24 b/s，牵引 2,400 t |
| entity | `tcnetworks:electric_tram` | 有轨电车 | Electric Tram | E7 | 城市客运，接触网 |
| entity | `tcnetworks:freight_wagon` | 棚车 | Freight Wagon | E6 | 12 货位 / 20 t |
| entity | `tcnetworks:galleon` | 盖伦船 | Galleon | E5 | 远洋，48 货位 |
| entity | `tcnetworks:galley` | 桨帆船 | Galley | E3 | 桨 + 横帆，12 货位 |
| entity | `tcnetworks:handcart` | 手推车 | Handcart | E2 | 人力，1 货位 |
| entity | `tcnetworks:helicopter` | 直升机 | Helicopter | E9 | 垂直起降，7×7 停机坪 |
| entity | `tcnetworks:high_speed_train` | 高速动车组 | High-Speed Train | E9 | 8 节固定编组，客 400 |
| entity | `tcnetworks:hopper_wagon` | 漏斗车 | Hopper Wagon | E6 | 散料 30 t |
| entity | `tcnetworks:horse_wagon` | 马车 | Horse Wagon | E4 | 2-4 马，4 货位 |
| entity | `tcnetworks:jet_airliner` | 喷气客机 | Jet Airliner | E9 | 客 200，舱内模式 |
| entity | `tcnetworks:maglev_train` | 磁浮列车 | Maglev Train | E10 | 6 节，舱内 140 b/s |
| entity | `tcnetworks:mine_wagon` | 木轨矿车 | Mine Wagon | E5 | 木轨，人力或 1 马 |
| entity | `tcnetworks:oil_tanker` | 油轮 | Oil Tanker | E8 | 2,000,000 mB |
| entity | `tcnetworks:ox_cart` | 牛车 | Ox Cart | E2 | 畜力，3 货位 |
| entity | `tcnetworks:passenger_coach` | 客车 | Passenger Coach | E6 | 客 40 |
| entity | `tcnetworks:plank_boat` | 河船 | Plank Boat | E2 | 划桨，4 货位 |
| entity | `tcnetworks:propeller_aircraft` | 螺旋桨运输机 | Propeller Aircraft | E8 | 4 货位，客 6 |
| entity | `tcnetworks:propeller_airliner` | 螺旋桨客机 | Propeller Airliner | E8 | 客 40 |
| entity | `tcnetworks:raft` | 木筏 | Raft | E0 | 撑篙/划桨，2.0 b/s，1 货位 |
| entity | `tcnetworks:sailing_boat` | 近岸帆船 | Sailing Boat | E2 | 纵帆，6 货位 |
| entity | `tcnetworks:semi_truck` | 半挂车 | Semi Truck | E8 | 16 货位，E9 起 1 TEU |
| entity | `tcnetworks:sledge` | 拖橇 | Sledge | E1 | 人/犬/畜拉，雪地 4.5 b/s |
| entity | `tcnetworks:steam_locomotive` | 蒸汽机车 | Steam Locomotive | E6 | 16 b/s，牵引 900 t |
| entity | `tcnetworks:steamship` | 蒸汽船 | Steamship | E6 | 不依赖风，64 货位 |
| entity | `tcnetworks:tank_wagon` | 罐车 | Tank Wagon | E6 | 40,000 mB |
| entity | `tcnetworks:truck` | 卡车 | Truck | E7 | 8 货位 |
| structure | `tcnetworks:wildlife_overpass` | 动物通道 | Wildlife Overpass | E8 | 结构模板；恢复 Ecology 种群连通度 |
| block_entity | `tcnetworks:airfield` | 机场 | Airfield | E8 | 跑道与起降排序 |
| block_entity | `tcnetworks:crane` | 起重机 | Crane | E4 | 装卸动画与队列 |
| block_entity | `tcnetworks:crate` | 货箱 | Crate | E1 | 货包库存 |
| block_entity | `tcnetworks:dispatch_board` | 调度板 | Dispatch Board | E2 | 订单与批次 |
| block_entity | `tcnetworks:lock_controller` | 船闸控制器 | Lock Controller | E4 | 船闸水位状态 |
| block_entity | `tcnetworks:pipeline_node` | 管道节点 | Pipeline Node | E8 | 管道网络节点 |
| block_entity | `tcnetworks:signal` | 信号 | Signal | E6 | 闭塞分区边界与显示状态 |
| block_entity | `tcnetworks:station` | 站点 | Station | E2 | 站点核心通用方块实体 |
| block_entity | `tcnetworks:telegraph_office` | 电报局 | Telegraph Office | E7 | 消息队列 |
| block_entity | `tcnetworks:telephone_exchange` | 电话交换机 | Telephone Exchange | E7 | 电话网络节点 |
| block_entity | `tcnetworks:track` | 轨道 | Track | E5 | 轨道锚点，关联 TrackGraph 边 |
| block_entity | `tcnetworks:warehouse` | 仓库 | Warehouse | E4 | 仓库库存 |
| menu | `tcnetworks:crate` | 货箱菜单 | Crate Menu | E1 | 货箱物品栏 |
| menu | `tcnetworks:lock_controller` | 船闸菜单 | Lock Controller Menu | E4 | 船闸注排水 |
| menu | `tcnetworks:station` | 站点菜单 | Station Menu | E2 | 就地装卸规则与站名 |
| menu | `tcnetworks:telegraph` | 电报菜单 | Telegraph Menu | E7 | 收发电报 |
| menu | `tcnetworks:vehicle` | 载具菜单 | Vehicle Menu | E0 | 载具物品栏、燃料、车况、编组 |
| menu | `tcnetworks:warehouse` | 仓库菜单 | Warehouse Menu | E4 | 就地库存与出入库规则 |
| keybind | `tcnetworks:horn` | 鸣笛 | Horn | — | key.tcnetworks.horn，默认未绑定（默认用跳跃键鸣笛） |
| keybind | `tcnetworks:reverser` | 换向 | Reverser | — | key.tcnetworks.reverser，默认未绑定 |
| payload | `tcnetworks:dispatch_action` | 调度操作 | Dispatch Action | — | C→S 订单与批次操作 |
| payload | `tcnetworks:proxy_positions` | 代理位置 | Proxy Positions | — | S→C 地图上 L1/L2 载具位置 |
| payload | `tcnetworks:route_plan_edit` | 路线编辑 | Route Plan Edit | — | C→S 编辑规划 |
| payload | `tcnetworks:route_plan_preview` | 路线预览 | Route Plan Preview | — | S→C 规划路线与代价 |
| payload | `tcnetworks:signal_sync` | 信号同步 | Signal Sync | — | S→C 闭塞分区状态 |
| payload | `tcnetworks:telegraph_message` | 电报消息 | Telegraph Message | — | 双向收发电报 |
| payload | `tcnetworks:timetable_edit` | 时刻表编辑 | Timetable Edit | — | C→S 时刻表编辑 |
| payload | `tcnetworks:track_graph_delta` | 轨道图增量 | Track Graph Delta | — | S→C 轨道图增量同步 |
| payload | `tcnetworks:vehicle_control` | 载具控制 | Vehicle Control | — | C→S 驾驶输入 |
| payload | `tcnetworks:vehicle_state` | 载具状态 | Vehicle State | — | S→C 车况、速度、燃料 |
| hud_widget | `tcnetworks:route_survey` | 路线勘测 | Route Survey | — | 手持规划工具时显示，标准级，右侧中部；语言键 hud.tcnetworks.route_survey |
| hud_widget | `tcnetworks:vehicle_status` | 载具状态 | Vehicle Status | — | 乘坐时显示，标准级，底部居中；语言键 hud.tcnetworks.vehicle_status |
| civ_tab | `tcnetworks:logistics` | 物流 | Logistics | — | 文明面板 ECONOMY 组，顺序 500；语言键 civtab.tcnetworks.logistics |
| region_layer | `tcnetworks:transport` | 交通层 | Transport Layer | — | 区域可达性、道路密度、运量统计 |
| sound | `tcnetworks:ambient.port.gulls` | 港口海鸥 | Port Gulls | E3 | 港口环境音 |
| sound | `tcnetworks:ambient.station.bustle` | 车站嘈杂 | Station Bustle | E6 | 车站环境音 |
| sound | `tcnetworks:block.crane.operate` | 起重机运转 | Crane Operate | E4 | 装卸 |
| sound | `tcnetworks:block.lock.water` | 船闸水流 | Lock Water | E4 | 注排水 |
| sound | `tcnetworks:block.signal.change` | 信号转换 | Signal Change | E6 | 臂板/色灯变化 |
| sound | `tcnetworks:block.telegraph.key` | 电报键 | Telegraph Key | E7 | 发报嘀嗒 |
| sound | `tcnetworks:event.milestone.first_train` | 首列火车 | First Train | E6 | 第一列火车到站仪式音 |
| sound | `tcnetworks:ui.dispatch.alert` | 调度提示 | Dispatch Alert | — | 订单异常提示音 |
| sound | `tcnetworks:vehicle.aircraft.jet` | 喷气发动机 | Jet Engine | E9 | 喷气飞机 |
| sound | `tcnetworks:vehicle.aircraft.propeller` | 螺旋桨 | Propeller | E8 | 螺旋桨飞机 |
| sound | `tcnetworks:vehicle.car.engine` | 汽车引擎 | Car Engine | E7 | 随转速变化 |
| sound | `tcnetworks:vehicle.car.horn` | 汽车喇叭 | Car Horn | E7 | 鸣笛 |
| sound | `tcnetworks:vehicle.cart.roll` | 车轮滚动 | Cart Roll | E2 | 畜力车与手推车 |
| sound | `tcnetworks:vehicle.locomotive.chuff` | 蒸汽喘息 | Locomotive Chuff | E6 | 随速度变化 |
| sound | `tcnetworks:vehicle.locomotive.whistle` | 机车汽笛 | Locomotive Whistle | E6 | 鸣笛 |
| sound | `tcnetworks:vehicle.ship.creak` | 船体吱嘎 | Ship Creak | E2 | 木船 |
| sound | `tcnetworks:vehicle.ship.horn` | 船笛 | Ship Horn | E6 | 汽船鸣笛 |
| sound | `tcnetworks:vehicle.tram.bell` | 电车铃 | Tram Bell | E7 | 电车鸣铃 |

### 5.9 聚落 `tcsettlements`（84 项，来源 [15-settlements.md](15-settlements.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcsettlements:aqueduct_channel` | 引水渠 | Aqueduct Channel | E3 | 水源输送，质量 0.90 |
| block | `tcsettlements:bath_basin` | 浴池 | Bath Basin | E3 | 浴场房间核心 |
| block | `tcsettlements:census_desk` | 普查台 | Census Desk | E2 | 镇的必需建筑 |
| block | `tcsettlements:cistern` | 蓄水池 | Cistern | E3 | 水源，质量 0.80 |
| block | `tcsettlements:classroom_blackboard` | 黑板 | Classroom Blackboard | E6 | 公立学校教学站 |
| block | `tcsettlements:clinic_cabinet` | 诊所柜 | Clinic Cabinet | E8 | 医疗容量 6 |
| block | `tcsettlements:construction_stake` | 施工桩 | Construction Stake | E0 | 乡土模板与蓝图施工 |
| block | `tcsettlements:drafting_table` | 绘图桌 | Drafting Table | E2 | 保存与复制蓝图 |
| block | `tcsettlements:drain_channel` | 排水沟 | Drain Channel | E2 | 卫生排水覆盖 |
| block | `tcsettlements:electric_street_lamp` | 电路灯 | Electric Street Lamp | E7 | 需电网供电 |
| block | `tcsettlements:granary` | 粮仓 | Granary | E1 | 聚落粮食储备，可 1-4 个合并 |
| block | `tcsettlements:habitat_bunk` | 栖息舱铺位 | Habitat Bunk | E10 | 太空住房床位 |
| block | `tcsettlements:healer_mat` | 医者席 | Healer Mat | E1 | 医者工作地，照护 1 人 |
| block | `tcsettlements:health_monitor_terminal` | 健康监测终端 | Health Monitor Terminal | E9 | 疫情预警与自动隔离 |
| block | `tcsettlements:herb_drying_rack` | 草药晾架 | Herb Drying Rack | E1 | 制作草药敷剂 |
| block | `tcsettlements:hide_cover` | 兽皮篷 | Hide Cover | E0 | 窝棚屋顶/门帘 |
| block | `tcsettlements:house_number_plate` | 门牌 | House Number Plate | E3 | 住所命名与编号 |
| block | `tcsettlements:infirmary_bed` | 病床 | Infirmary Bed | E4 | 医疗容量 2 |
| block | `tcsettlements:job_board` | 岗位板 | Job Board | E0 | 16 格内工作地汇总 |
| block | `tcsettlements:latrine` | 厕所 | Latrine | E1 | 卫生，10 人 |
| block | `tcsettlements:oil_street_lamp` | 油路灯 | Oil Street Lamp | E5 | 夜间照明 |
| block | `tcsettlements:planning_table` | 规划桌 | Planning Table | E3 | 分区编辑 |
| block | `tcsettlements:public_fountain` | 公共喷泉 | Public Fountain | E3 | 水与文化 |
| block | `tcsettlements:quarantine_flag` | 隔离旗 | Quarantine Flag | E4 | 隔离一户 |
| block | `tcsettlements:rat_trap` | 捕鼠笼 | Rat Trap | E4 | 16 格捕鼠覆盖 |
| block | `tcsettlements:school_desk` | 课桌 | School Desk | E3 | 教室容量 |
| block | `tcsettlements:settlement_core` | 聚落核心 | Settlement Core | E0 | 每聚落唯一，方块状态 stage 7 级外观 |
| block | `tcsettlements:sewage_outlet` | 排污口 | Sewage Outlet | E6 | 管网出口，向 Ecology 上报污染 |
| block | `tcsettlements:sewer_pipe` | 下水管 | Sewer Pipe | E6 | 下水道管网 |
| block | `tcsettlements:shrine` | 神龛 | Shrine | E1 | 文化 10，节庆聚集点 |
| block | `tcsettlements:sleeping_mat` | 睡垫 | Sleeping Mat | E0 | NPC 床位，不设重生点 |
| block | `tcsettlements:standpipe` | 公共水栓 | Standpipe | E6 | 管网供水点 |
| block | `tcsettlements:surgery_table` | 手术台 | Surgery Table | E5 | 医院必需，外科医生工作地 |
| block | `tcsettlements:teachers_lectern` | 讲台 | Teacher's Lectern | E3 | 教师工作地 |
| block | `tcsettlements:thatch` | 茅草 | Thatch | E0 | 窝棚屋顶材料 |
| block | `tcsettlements:thatch_slab` | 茅草台阶 | Thatch Slab | E0 | 屋檐 |
| block | `tcsettlements:timber_frame` | 木构架 | Timber Frame | E4 | 木构架房屋墙体，材料等级 2 |
| block | `tcsettlements:town_bell` | 镇钟 | Town Bell | E2 | 召集与避难 |
| block | `tcsettlements:vaccination_station` | 接种站 | Vaccination Station | E6 | 每日接种 20 人 |
| block | `tcsettlements:water_treatment_controller` | 水处理控制器 | Water Treatment Controller | E7 | 多方块水处理厂 |
| block | `tcsettlements:wattle` | 编条墙 | Wattle | E1 | 早期墙体，计入 defensive_wall 标签 |
| block | `tcsettlements:wattle_and_daub` | 抹泥编条墙 | Wattle and Daub | E1 | 早期房屋墙体，材料等级 1 |
| block | `tcsettlements:well` | 水井 | Well | E1 | 水覆盖 32 格 |
| item | `tcsettlements:antibiotic_dose` | 抗生素 | Antibiotic Dose | E8 | 细菌性疾病 CFR ×0.2 |
| item | `tcsettlements:building_plan` | 建筑蓝图 | Building Plan | E2 | 存储 ≤ 32³ 蓝图 |
| item | `tcsettlements:herbal_poultice` | 草药敷剂 | Herbal Poultice | E1 | 对 NPC 治疗 |
| item | `tcsettlements:surgical_kit` | 手术包 | Surgical Kit | E5 | 病危居民存活率 +40% |
| item | `tcsettlements:vaccine_vial` | 疫苗瓶 | Vaccine Vial | E6 | 接种站耗材 |
| item | `tcsettlements:zoning_brush` | 分区刷 | Zoning Brush | E3 | 世界内涂分区 |
| block_entity | `tcsettlements:census_desk` | 普查台 | Census Desk | E2 | 统计快照 |
| block_entity | `tcsettlements:drafting_table` | 绘图桌 | Drafting Table | E2 | 框选角点与蓝图缓存 |
| block_entity | `tcsettlements:granary` | 粮仓 | Granary | E1 | 储备 FU 与多方块合并 |
| block_entity | `tcsettlements:health_monitor_terminal` | 健康监测终端 | Health Monitor Terminal | E9 | 预警阈值设置 |
| block_entity | `tcsettlements:job_board` | 岗位板 | Job Board | E0 | 工作地缓存与优先级 |
| block_entity | `tcsettlements:planning_table` | 规划桌 | Planning Table | E3 | 分区地图缓存 |
| block_entity | `tcsettlements:settlement_core` | 聚落核心 | Settlement Core | E0 | 聚落 id、名称、等级缓存 |
| block_entity | `tcsettlements:town_bell` | 镇钟 | Town Bell | E2 | 召集/避难状态 |
| block_entity | `tcsettlements:water_treatment_controller` | 水处理控制器 | Water Treatment Controller | E7 | 多方块状态与处理量 |
| block_entity | `tcsettlements:well` | 水井 | Well | E1 | 地下水检查结果 |
| menu | `tcsettlements:drafting_table` | 绘图桌菜单 | Drafting Table Menu | E2 | 保存/复制蓝图 |
| menu | `tcsettlements:granary` | 粮仓菜单 | Granary Menu | E1 | 储物与储备天数 |
| menu | `tcsettlements:job_board` | 岗位板菜单 | Job Board Menu | E0 | 岗位槽与人物芯片拖放 |
| menu | `tcsettlements:planning_table` | 规划桌菜单 | Planning Table Menu | E3 | 分区编辑 |
| menu | `tcsettlements:settlement_core` | 聚落核心菜单 | Settlement Core Menu | E0 | 名称、等级、人口与面板入口 |
| payload | `tcsettlements:building_plan_place` | 蓝图放置 | Building Plan Place | — | C→S |
| payload | `tcsettlements:directive_toggle` | 指令切换 | Directive Toggle | — | C→S，经 DirectiveGate |
| payload | `tcsettlements:health_alert` | 卫生警报 | Health Alert | — | S→C 疫情摘要 |
| payload | `tcsettlements:job_assign` | 岗位分配 | Job Assign | — | C→S |
| payload | `tcsettlements:resident_info` | 居民信息 | Resident Info | — | S→C 人物界面分区 |
| payload | `tcsettlements:resident_info_request` | 居民信息请求 | Resident Info Request | — | C→S |
| payload | `tcsettlements:room_scan_result` | 房间识别结果 | Room Scan Result | — | S→C |
| payload | `tcsettlements:settlement_sync` | 聚落同步 | Settlement Sync | — | S→C HUD 数据 |
| payload | `tcsettlements:zone_paint` | 分区涂刷 | Zone Paint | — | C→S |
| hud_widget | `tcsettlements:settlement_status` | 聚落状态 | Settlement Status | E0 | 标准级，右上，仅在己方聚落内 |
| civ_tab | `tcsettlements:settlements` | 聚落 | Settlements | E0 | 文明组，排序 200 |
| region_layer | `tcsettlements:demography` | 人口分布 | Demography | — | 64×64 格人口、职业、满意度、患病率 |
| sound | `tcsettlements:ambient.crowd.large` | 大型人群 | Large Crowd | — | 都市以上 |
| sound | `tcsettlements:ambient.crowd.medium` | 中型人群 | Medium Crowd | — | 镇与城 |
| sound | `tcsettlements:ambient.crowd.small` | 小型人群 | Small Crowd | — | 32 格内 ≥ 12 名 NPC |
| sound | `tcsettlements:ambient.village.day` | 村落白天 | Village Daytime | — | 村落白天环境床 |
| sound | `tcsettlements:block.town_bell.ring` | 镇钟鸣响 | Town Bell Ring | — | 召集/避难 |
| sound | `tcsettlements:event.birth` | 新生 | Birth | — | 出生通知 |
| sound | `tcsettlements:event.epidemic` | 疫情爆发 | Epidemic Outbreak | — | 危急通知 |
| sound | `tcsettlements:event.settlement_upgrade` | 聚落升级 | Settlement Upgrade | — | 仪式通知 |

### 5.10 市集 `tcexchange`（75 项，来源 [16-exchange.md](16-exchange.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcexchange:bank_counter` | 银行柜台 | Bank Counter | E5 | 账户、贷款、纸币；banker 岗位 |
| block | `tcexchange:bank_vault` | 金库 | Bank Vault | E5 | 准备金与抵押品，防爆 |
| block | `tcexchange:barter_mat` | 交换席 | Barter Mat | E1 | 集市锚点、常设交换 |
| block | `tcexchange:clearing_house` | 清算所 | Clearing House | E10 | 跨天体结算 |
| block | `tcexchange:counting_house` | 账房 | Counting House | E4 | 合同交付、托管仓 |
| block | `tcexchange:exchange_board` | 交易所行情板 | Exchange Board | E6 | 证券集合竞价 |
| block | `tcexchange:fair_banner` | 集市旗 | Fair Banner | E4 | 宣布集市 |
| block | `tcexchange:forex_board` | 外汇牌价板 | Forex Board | E8 | 货币兑换 |
| block | `tcexchange:guild_hall` | 行会会所 | Guild Hall | E4 | 行会；guild_master 岗位 |
| block | `tcexchange:market_stall` | 市场摊位 | Market Stall | E2 | 市场点；merchant 岗位 |
| block | `tcexchange:merchant_scale` | 商秤 | Merchant Scale | E2 | 称量碎银；价差 −1 个百分点 |
| block | `tcexchange:mint` | 铸币台 | Mint | E3 | 铸币与回炉 |
| block | `tcexchange:order_board` | 订单板 | Order Board | E3 | 挂单撮合、取货格 |
| block | `tcexchange:payment_terminal` | 支付终端 | Payment Terminal | E9 | 自助商店、刷卡 |
| block | `tcexchange:stock_ticker` | 报价机 | Stock Ticker | E6 | 价格 → 红石信号 |
| block | `tcexchange:trading_post` | 贸易站 | Trading Post | E5 | 无聚落市场锚点、里程碑 |
| block | `tcexchange:wire_counter` | 电汇柜台 | Wire Counter | E7 | 跨市场即时转账 |
| item | `tcexchange:banknote` | 纸币 | Banknote | E5 | 面值 1/5/10/50/100 |
| item | `tcexchange:bill_of_exchange` | 汇票 | Bill of Exchange | E4 | 异地兑付 |
| item | `tcexchange:bond` | 债券 | Bond | E6 | 国库债券 |
| item | `tcexchange:bronze_coin` | 铜币 | Bronze Coin | E3 | 面值 1 单位 |
| item | `tcexchange:cheque_book` | 支票簿 | Cheque Book | E7 | 签发支票 |
| item | `tcexchange:coin_die` | 印模 | Coin Die | E3 | 铸币台消耗品 |
| item | `tcexchange:coin_purse` | 钱袋 | Coin Purse | E3 | 收纳货币 |
| item | `tcexchange:contract_scroll` | 合同卷轴 | Contract Scroll | E3 | 合同的物品形态 |
| item | `tcexchange:cowrie_shell` | 贝 | Cowrie Shell | E2 | 商品货币 |
| item | `tcexchange:credit_chit` | 赊账凭据 | Credit Chit | E3 | 市场欠条 |
| item | `tcexchange:gold_coin` | 金币 | Gold Coin | E3 | 面值 100 单位 |
| item | `tcexchange:guild_charter` | 行会特许状 | Guild Charter | E4 | 建立行会 |
| item | `tcexchange:hacksilver` | 碎银 | Hacksilver | E2 | 称量银 |
| item | `tcexchange:insurance_policy` | 保单 | Insurance Policy | E5 | 货物保险 |
| item | `tcexchange:ledger` | 账簿 | Ledger | E5 | 摊位与商号流水 |
| item | `tcexchange:payment_card` | 支付卡 | Payment Card | E8 | 绑定账户 |
| item | `tcexchange:salt_cake` | 盐饼 | Salt Cake | E2 | 商品货币 |
| item | `tcexchange:share_certificate` | 股票 | Share Certificate | E5 | 公司股份 |
| item | `tcexchange:silver_coin` | 银币 | Silver Coin | E3 | 面值 10 单位 |
| block_entity | `tcexchange:bank_counter` | 银行柜台 | Bank Counter | E5 | 银行 id |
| block_entity | `tcexchange:bank_vault` | 金库 | Bank Vault | E5 | 54 格 |
| block_entity | `tcexchange:clearing_house` | 清算所 | Clearing House | E10 | 结算批次 |
| block_entity | `tcexchange:counting_house` | 账房 | Counting House | E4 | 托管仓 27 格 |
| block_entity | `tcexchange:exchange_board` | 交易所行情板 | Exchange Board | E6 | 显示缓存 |
| block_entity | `tcexchange:forex_board` | 外汇牌价板 | Forex Board | E8 | 牌价缓存 |
| block_entity | `tcexchange:guild_hall` | 行会会所 | Guild Hall | E4 | 行会 id |
| block_entity | `tcexchange:market_stall` | 市场摊位 | Market Stall | E2 | 展示格与账本 |
| block_entity | `tcexchange:mint` | 铸币台 | Mint | E3 | 铸币进度 |
| block_entity | `tcexchange:order_board` | 订单板 | Order Board | E3 | 取货格 |
| block_entity | `tcexchange:payment_terminal` | 支付终端 | Payment Terminal | E9 | 自助价格表 |
| block_entity | `tcexchange:trading_post` | 贸易站 | Trading Post | E5 | 仓储 54 格 |
| block_entity | `tcexchange:wire_counter` | 电汇柜台 | Wire Counter | E7 | 电报节点 |
| menu | `tcexchange:bank` | 银行 | Bank | E5 | — |
| menu | `tcexchange:barter` | 物物交换 | Barter | E1 | 满意度条 |
| menu | `tcexchange:caravan_planner` | 商队规划 | Caravan Planner | E2 | 10.5 |
| menu | `tcexchange:contract_editor` | 合同编辑器 | Contract Editor | E3 | 10.4 |
| menu | `tcexchange:counting_house` | 账房 | Counting House | E4 | 验收槽 |
| menu | `tcexchange:exchange_board` | 交易所 | Exchange | E6 | 证券下单 |
| menu | `tcexchange:market` | 市场交易 | Market Trade | E1 | 10.3 |
| menu | `tcexchange:mint` | 铸币 | Mint | E3 | — |
| menu | `tcexchange:order_board` | 订单板 | Order Board | E3 | 10.4 |
| keybind | `tcexchange:open_market` | 打开市集 | Open Market | — | 未绑定 |
| payload | `tcexchange:bank_action` | 银行操作 | Bank Action | — | C→S |
| payload | `tcexchange:caravan_plan` | 商队计划 | Caravan Plan | — | C→S，带版本号 |
| payload | `tcexchange:contract_edit` | 合同修改 | Contract Edit | — | C→S，带版本号 |
| payload | `tcexchange:contract_sign` | 合同签字 | Contract Sign | — | C→S |
| payload | `tcexchange:mint_request` | 铸币请求 | Mint Request | — | C→S |
| payload | `tcexchange:order_cancel` | 撤单 | Order Cancel | — | C→S |
| payload | `tcexchange:order_place` | 挂单 | Order Place | — | C→S |
| payload | `tcexchange:ticker_sync` | HUD 行情 | Ticker Sync | — | S→C |
| payload | `tcexchange:trade_offer` | 交易提交 | Trade Offer | — | C→S |
| payload | `tcexchange:trade_result` | 交易结果 | Trade Result | — | S→C |
| hud_widget | `tcexchange:market_ticker` | 行情条 | Market Ticker | E2 | MIDDLE_RIGHT，EXPANDED |
| civ_tab | `tcexchange:market` | 市集 | Market | E0 | 经济分组，排序 550 |
| region_layer | `tcexchange:market` | 市场 | Market | — | 64×64 格；价格指数、过路财富、贫困 |
| sound | `tcexchange:block.market_stall.bell` | 摊铃 | Stall Bell | — | 摊位开市 |
| sound | `tcexchange:block.mint.strike` | 铸币声 | Mint Strike | — | 铸币 |
| sound | `tcexchange:event.coin_clink` | 钱币声 | Coin Clink | — | 成交 |

### 5.11 求知 `tcdiscovery`（112 项，来源 [17-discovery.md](17-discovery.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcdiscovery:academy_lectern` | 学府讲台 | Academy Lectern | E4 | 学府核心 |
| block | `tcdiscovery:air_pump` | 抽气机 | Air Pump | E5 | 真空实验 |
| block | `tcdiscovery:armillary_sphere` | 浑仪 | Armillary Sphere | E3 | 2 级天文仪器 |
| block | `tcdiscovery:calendar_stone` | 历石 | Calendar Stone | E1 | 记录季节与天象 |
| block | `tcdiscovery:camera` | 照相机 | Camera | E6 | 摄影记录 |
| block | `tcdiscovery:cloud_chamber` | 云室 | Cloud Chamber | E8 | 粒子探测 |
| block | `tcdiscovery:compound_microscope` | 复式显微镜 | Compound Microscope | E5 | 显微实验站 |
| block | `tcdiscovery:dig_site_stake` | 发掘桩 | Dig Site Stake | E2 | 发掘网格 |
| block | `tcdiscovery:display_case` | 展示柜 | Display Case | E5 | 展示 1 件 |
| block | `tcdiscovery:elder_seat` | 长老之座 | Elder Seat | E1 | E1 设施核心 |
| block | `tcdiscovery:electron_microscope` | 电子显微镜 | Electron Microscope | E8 | 5 级显微 |
| block | `tcdiscovery:engineering_college_desk` | 工程学院讲桌 | Engineering College Desk | E6 | E6 设施核心 |
| block | `tcdiscovery:experiment_bench` | 实验台 | Experiment Bench | E3 | 实验站 |
| block | `tcdiscovery:fume_hood` | 通风橱 | Fume Hood | E6 | 事故减半 |
| block | `tcdiscovery:gnomon` | 圭表 | Gnomon | E1 | 1 级天文仪器 |
| block | `tcdiscovery:lab_bench` | 实验桌 | Lab Bench | E6 | 化学实验站 |
| block | `tcdiscovery:mainframe_rack` | 主机机柜 | Mainframe Rack | E9 | 提供算力 |
| block | `tcdiscovery:mainframe_terminal` | 主机终端 | Mainframe Terminal | E9 | E9 设施核心 |
| block | `tcdiscovery:mounted_specimen` | 剥制标本 | Mounted Specimen | E5 | 动物标本 |
| block | `tcdiscovery:national_lab_control` | 国家实验室控制台 | National Lab Control | E8 | E8 设施核心 |
| block | `tcdiscovery:oscilloscope` | 示波器 | Oscilloscope | E7 | 电学仪器 |
| block | `tcdiscovery:paper_vat` | 纸槽 | Paper Vat | E4 | 抄纸 |
| block | `tcdiscovery:printing_press` | 印刷机 | Printing Press | E5 | 印刷 |
| block | `tcdiscovery:pulp_stamper` | 纸浆捣碓 | Pulp Stamper | E4 | 制浆 |
| block | `tcdiscovery:radio_telescope_dish` | 射电望远镜 | Radio Telescope Dish | E8 | 5 × 5 多方块 |
| block | `tcdiscovery:refracting_telescope` | 折射望远镜 | Refracting Telescope | E5 | 天文台核心 |
| block | `tcdiscovery:research_director_desk` | 所长办公桌 | Research Director Desk | E7 | E7 设施核心 |
| block | `tcdiscovery:scholars_table` | 学者桌 | Scholar's Table | E3 | E3 设施核心 |
| block | `tcdiscovery:scribe_desk` | 书吏桌 | Scribe Desk | E2 | E2 设施核心 |
| block | `tcdiscovery:scriptorium_desk` | 缮写桌 | Scriptorium Desk | E4 | E4 设施核心 |
| block | `tcdiscovery:scroll_shelf` | 卷轴架 | Scroll Shelf | E3 | 藏书家具 |
| block | `tcdiscovery:sieve` | 筛子 | Sieve | E0 | 筛发掘土方 |
| block | `tcdiscovery:space_research_console` | 空间研究控制台 | Space Research Console | E10 | E10 设施核心 |
| block | `tcdiscovery:specimen_cabinet` | 标本柜 | Specimen Cabinet | E5 | 27 格 |
| block | `tcdiscovery:spectroscope` | 分光镜 | Spectroscope | E7 | 光谱仪器 |
| block | `tcdiscovery:tablet_rack` | 泥板架 | Tablet Rack | E2 | 藏书家具 |
| block | `tcdiscovery:type_case` | 活字盒 | Type Case | E5 | 排版 |
| block | `tcdiscovery:water_clock` | 漏刻 | Water Clock | E3 | 计时仪器 |
| item | `tcdiscovery:alembic` | 蒸馏器 | Alembic | E4 | 玻璃器皿 |
| item | `tcdiscovery:analytical_balance` | 分析天平 | Analytical Balance | E6 | 4 级称量 |
| item | `tcdiscovery:balance_scale` | 天平 | Balance Scale | E2 | 1 级称量 |
| item | `tcdiscovery:bamboo_slips` | 竹简 | Bamboo Slips | E3 | 知识载体 |
| item | `tcdiscovery:barometer` | 气压计 | Barometer | E5 | 3 级测压 |
| item | `tcdiscovery:clay_tablet` | 泥板 | Clay Tablet | E2 | 知识载体 |
| item | `tcdiscovery:data_tape` | 数据磁带 | Data Tape | E9 | 知识载体 |
| item | `tcdiscovery:fossil` | 化石 | Fossil | E0 | 带化石组件 |
| item | `tcdiscovery:hand_lens` | 放大镜 | Hand Lens | E4 | 2 级透镜 |
| item | `tcdiscovery:herbarium_sheet` | 植物标本 | Herbarium Sheet | E3 | 植物标本 |
| item | `tcdiscovery:ink` | 墨水 | Ink | E3 | 书写消耗 |
| item | `tcdiscovery:journal_issue` | 期刊 | Journal Issue | E6 | 知识载体 |
| item | `tcdiscovery:lens` | 透镜 | Lens | E4 | 光学部件 |
| item | `tcdiscovery:manuscript` | 手抄本 | Manuscript | E4 | 知识载体 |
| item | `tcdiscovery:microfilm_reel` | 缩微胶卷 | Microfilm Reel | E8 | 知识载体 |
| item | `tcdiscovery:mineral_sample` | 矿物样本 | Mineral Sample | E3 | 岩矿标本 |
| item | `tcdiscovery:movable_type` | 活字 | Movable Type | E5 | 排版部件 |
| item | `tcdiscovery:paper_mould` | 抄纸帘 | Paper Mould | E4 | 抄纸工具 |
| item | `tcdiscovery:parchment` | 羊皮纸 | Parchment | E3 | 书写材料 |
| item | `tcdiscovery:photographic_plate` | 照相底片 | Photographic Plate | E6 | 仪器 +1 级 |
| item | `tcdiscovery:printed_book` | 印本 | Printed Book | E5 | 知识载体 |
| item | `tcdiscovery:quill` | 羽毛笔 | Quill | E4 | 手抄工具 |
| item | `tcdiscovery:scroll` | 卷轴 | Scroll | E3 | 知识载体 |
| item | `tcdiscovery:space_telescope` | 太空望远镜 | Space Telescope | E10 | Orbit 载荷 |
| item | `tcdiscovery:specimen_jar` | 标本罐 | Specimen Jar | E3 | 动物标本 |
| item | `tcdiscovery:streak_plate` | 条痕板 | Streak Plate | E3 | 矿物条痕 |
| item | `tcdiscovery:stylus` | 刻写笔 | Stylus | E2 | 书写泥板 |
| item | `tcdiscovery:tally_stick` | 刻符棒 | Tally Stick | E1 | 计数载体 |
| item | `tcdiscovery:technical_report` | 技术报告 | Technical Report | E7 | 知识载体 |
| item | `tcdiscovery:thermometer` | 温度计 | Thermometer | E5 | 3 级测温 |
| item | `tcdiscovery:trowel` | 小铲 | Trowel | E0 | 精细发掘 |
| item | `tcdiscovery:wet_clay_tablet` | 湿泥板 | Wet Clay Tablet | E2 | 待烧制 |
| block_entity | `tcdiscovery:copying_desk` | 书桌 | Copying Desk | E2 | 写作与抄写，书桌共用 |
| block_entity | `tcdiscovery:dig_site` | 发掘点 | Dig Site | E2 | 网格与地层 |
| block_entity | `tcdiscovery:display_case` | 展示柜 | Display Case | E5 | 渲染展品 |
| block_entity | `tcdiscovery:experiment_bench` | 实验台 | Experiment Bench | E3 | 实验运行 |
| block_entity | `tcdiscovery:knowledge_shelf` | 藏书架 | Knowledge Shelf | E2 | 泥板架、卷轴架共用 |
| block_entity | `tcdiscovery:lab_bench` | 实验桌 | Lab Bench | E6 | 实验运行 |
| block_entity | `tcdiscovery:mainframe` | 主机 | Mainframe | E9 | 算力 |
| block_entity | `tcdiscovery:microscope` | 显微镜 | Microscope | E5 | 显微观测 |
| block_entity | `tcdiscovery:paper_vat` | 纸槽 | Paper Vat | E4 | 纸浆存量 |
| block_entity | `tcdiscovery:printing_press` | 印刷机 | Printing Press | E5 | 排版与印刷 |
| block_entity | `tcdiscovery:pulp_stamper` | 纸浆捣碓 | Pulp Stamper | E4 | 动力输入 |
| block_entity | `tcdiscovery:research_facility` | 研究设施 | Research Facility | E1 | 所有设施核心共用 |
| block_entity | `tcdiscovery:sieve` | 筛子 | Sieve | E0 | 筛选进度 |
| block_entity | `tcdiscovery:specimen_cabinet` | 标本柜 | Specimen Cabinet | E5 | 存放标本 |
| block_entity | `tcdiscovery:telescope` | 望远镜 | Telescope | E5 | 天文观测 |
| menu | `tcdiscovery:copying` | 写作与抄写 | Copying | E2 | 书桌菜单 |
| menu | `tcdiscovery:dig_site` | 发掘 | Dig Site | E2 | 网格与地层 |
| menu | `tcdiscovery:experiment` | 实验 | Experiment | E3 | 实验菜单 |
| menu | `tcdiscovery:paper_vat` | 纸槽 | Paper Vat | E4 | 抄纸 |
| menu | `tcdiscovery:printing_press` | 印刷机 | Printing Press | E5 | 印刷菜单 |
| menu | `tcdiscovery:research_facility` | 研究设施 | Research Facility | E1 | 设施配置 |
| keybind | `tcdiscovery:journal` | 日志 | Journal | — | Y，key.tcdiscovery.journal，打开 J 面板日志页 |
| keybind | `tcdiscovery:observe` | 观察模式 | Observe | — | V，key.tcdiscovery.observe，切换 |
| payload | `tcdiscovery:book_compose` | 编写书籍 | Book Compose | — | C2S |
| payload | `tcdiscovery:dig_record` | 发掘记录 | Dig Record | — | C2S |
| payload | `tcdiscovery:experiment_action` | 实验操作 | Experiment Action | — | C2S |
| payload | `tcdiscovery:experiment_update` | 实验更新 | Experiment Update | — | S2C |
| payload | `tcdiscovery:facility_config` | 设施配置 | Facility Config | — | C2S |
| payload | `tcdiscovery:hypothesis_action` | 假说操作 | Hypothesis Action | — | C2S |
| payload | `tcdiscovery:known_properties_sync` | 已知性质同步 | Known Properties Sync | — | S2C |
| payload | `tcdiscovery:observation_result` | 观察结果 | Observation Result | — | S2C |
| payload | `tcdiscovery:observe_request` | 观察请求 | Observe Request | — | C2S |
| hud_widget | `tcdiscovery:experiment_status` | 实验状态 | Experiment Status | — | STANDARD，hud.tcdiscovery.experiment_status |
| hud_widget | `tcdiscovery:field_notes` | 野外笔记 | Field Notes | — | EXPANDED，hud.tcdiscovery.field_notes |
| civ_tab | `tcdiscovery:journal` | 日志 | Journal | — | 知识组，顺序 850，civtab.tcdiscovery.journal |
| region_layer | `tcdiscovery:field_record` | 观察记录 | Field Record | — | 每区域观察计数 |
| sound | `tcdiscovery:block.printing_press.stamp` | 印刷机压印 | Printing Press Stamp | — | — |
| sound | `tcdiscovery:event.discovery_made` | 新发现 | Discovery Made | — | — |
| sound | `tcdiscovery:event.experiment_fail` | 实验失败 | Experiment Fail | — | — |
| sound | `tcdiscovery:event.observe_complete` | 观察完成 | Observe Complete | — | — |
| sound | `tcdiscovery:item.quill.scratch` | 笔尖沙沙 | Quill Scratch | — | — |
| sound | `tcdiscovery:ui.page_turn` | 翻页 | Page Turn | — | — |

### 5.12 遗响 `tcechoes`（84 项，来源 [18-echoes.md](18-echoes.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcechoes:ancient_slag` | 古矿渣 | Ancient Slag | E0 | 可回收铁 |
| block | `tcechoes:ancient_tablet` | 古代碑刻 | Ancient Tablet | E0 | 铭文 |
| block | `tcechoes:artefact_deposit` | 考古沉积 | Artefact Deposit | E0 | 可刷 |
| block | `tcechoes:conservation_bench` | 修复台 | Conservation Bench | E7 | 修复品相 |
| block | `tcechoes:cultural_layer` | 文化层 | Cultural Layer | E0 | 地层年代 |
| block | `tcechoes:epigraphy_table` | 碑铭台 | Epigraphy Table | E2 | 鉴定 |
| block | `tcechoes:frozen_remains` | 冻土遗骸 | Frozen Remains | E0 | 组织样本 |
| block | `tcechoes:midden_soil` | 灰坑土 | Midden Soil | E0 |  |
| block | `tcechoes:relic_shrine` | 祖物龛 | Relic Shrine | E1 | 展出 1 件文物 |
| block | `tcechoes:rock_art` | 岩画 | Rock Art | E0 | 可临摹 |
| block | `tcechoes:stele` | 纪念碑 | Memorial Stele | E3 | 纪念铭文 |
| item | `tcechoes:ancient_blade` | 古剑 | Ancient Blade | E0 | 文物 |
| item | `tcechoes:ancient_cargo_bundle` | 古代货包 | Ancient Cargo Bundle | E0 |  |
| item | `tcechoes:ancient_chart` | 古海图 | Ancient Chart | E0 | Atlas 缺席时 |
| item | `tcechoes:ancient_seed_jar` | 古种子罐 | Ancient Seed Jar | E0 |  |
| item | `tcechoes:ancient_tool` | 古工具 | Ancient Tool | E0 | 文物 |
| item | `tcechoes:astrolabe_fragment` | 星盘残件 | Astrolabe Fragment | E0 | 文物 |
| item | `tcechoes:bone_harpoon` | 骨鱼叉 | Bone Harpoon | E0 | 文物 |
| item | `tcechoes:bronze_ritual_vessel` | 青铜礼器 | Bronze Ritual Vessel | E0 | 文物 |
| item | `tcechoes:captains_log` | 航海日志 | Captain's Log | E0 |  |
| item | `tcechoes:carved_figurine` | 石雕小像 | Carved Figurine | E0 | 文物 |
| item | `tcechoes:chronicle_book` | 编年史书 | Chronicle Book | E3 |  |
| item | `tcechoes:coin_hoard` | 钱币窖藏 | Coin Hoard | E0 | 文物 |
| item | `tcechoes:flint_scraper` | 燧石刮削器 | Flint Scraper | E0 | 文物 |
| item | `tcechoes:funerary_mask` | 葬礼面具 | Funerary Mask | E0 | 文物 |
| item | `tcechoes:glass_beads` | 玻璃珠串 | Glass Beads | E0 | 文物 |
| item | `tcechoes:inscribed_fragment` | 铭文残片 | Inscribed Fragment | E0 | 文物 |
| item | `tcechoes:painted_pottery` | 彩陶 | Painted Pottery | E0 | 文物 |
| item | `tcechoes:royal_regalia` | 王权器物 | Royal Regalia | E0 | 文物 |
| item | `tcechoes:rubbing` | 拓片 | Rubbing | E0 |  |
| item | `tcechoes:scroll_case` | 书卷筒 | Scroll Case | E0 | 文物 |
| item | `tcechoes:seal_stone` | 印章石 | Seal Stone | E0 | 文物 |
| item | `tcechoes:tomb_key` | 墓室钥匙 | Tomb Key | E0 | 开启宝库 |
| item | `tcechoes:votive_offering` | 供奉品 | Votive Offering | E0 | 文物 |
| structure | `tcechoes:ancient_harbor` | 古港 | Ancient Harbor | E0 |  |
| structure | `tcechoes:ancient_mine` | 古矿坑 | Ancient Mine | E0 | 替代原版矿井 |
| structure | `tcechoes:ancient_quarry` | 古采石场 | Ancient Quarry | E0 |  |
| structure | `tcechoes:ancient_road` | 古道 | Ancient Road | E0 | 可修复为道路 |
| structure | `tcechoes:ancient_wreck` | 古沉船 | Ancient Wreck | E0 |  |
| structure | `tcechoes:aqueduct_ruin` | 水道遗址 | Aqueduct Ruin | E0 |  |
| structure | `tcechoes:barrow` | 古冢 | Barrow | E0 |  |
| structure | `tcechoes:battlefield` | 古战场 | Battlefield | E0 | 地表遗物散布 |
| structure | `tcechoes:camp_remains` | 营地遗存 | Camp Remains | E0 | 旧石器营地 |
| structure | `tcechoes:city_ruin` | 城址 | City Ruin | E0 | 政体都城 |
| structure | `tcechoes:cult_sanctum` | 秘教圣所 | Cult Sanctum | E0 | Beyond 碎塔焦点 |
| structure | `tcechoes:fortress_ruin` | 要塞遗址 | Fortress Ruin | E0 |  |
| structure | `tcechoes:frozen_camp` | 冰原营地 | Frozen Camp | E0 | 替代冰屋 |
| structure | `tcechoes:furnace_site` | 冶炼遗址 | Furnace Site | E0 | 可修复为 Industry 多方块 |
| structure | `tcechoes:library_ruin` | 藏书阁遗址 | Library Ruin | E0 | 含制图台 |
| structure | `tcechoes:megalith` | 巨石阵 | Megalith | E0 | 天象与祭祀 |
| structure | `tcechoes:necropolis` | 墓城 | Necropolis | E0 | 王陵宝库 |
| structure | `tcechoes:observatory_ruin` | 观象台遗址 | Observatory Ruin | E0 |  |
| structure | `tcechoes:rock_shelter` | 岩厦 | Rock Shelter | E0 | 岩画与灰坑 |
| structure | `tcechoes:sunken_city` | 沉没之城 | Sunken City | E0 | 灾难遗址 |
| structure | `tcechoes:tell` | 土丘遗址 | Tell | E0 | 多层叠压聚落 |
| structure | `tcechoes:temple_ruin` | 神庙遗址 | Temple Ruin | E0 |  |
| structure | `tcechoes:terrace_temple` | 台阶神庙 | Terrace Temple | E0 | 替代丛林神庙 |
| structure | `tcechoes:village_ruin` | 村落遗址 | Village Ruin | E0 |  |
| structure | `tcechoes:waystation_ruin` | 驿站遗址 | Waystation Ruin | E0 | 古道节点 |
| block_entity | `tcechoes:ancient_tablet` | 古代碑刻 | Ancient Tablet | E0 | 铭文 ID |
| block_entity | `tcechoes:conservation_bench` | 修复台 | Conservation Bench | E7 | 修复进度 |
| block_entity | `tcechoes:epigraphy_table` | 碑铭台 | Epigraphy Table | E2 | 槽位与进度 |
| block_entity | `tcechoes:relic_shrine` | 祖物龛 | Relic Shrine | E1 | 展品 |
| block_entity | `tcechoes:stele` | 纪念碑 | Memorial Stele | E3 | 文字与日期 |
| menu | `tcechoes:conservation_bench` | 修复台 | Conservation Bench | E7 |  |
| menu | `tcechoes:epigraphy_table` | 碑铭台 | Epigraphy Table | E2 | §10.4 |
| menu | `tcechoes:stele_inscription` | 刻碑 | Stele Inscription | E3 |  |
| keybind | `tcechoes:journal` | 打开传说 | Open Legends | — | key.tcechoes.journal，默认 Y，仅 Discovery 缺席时注册 |
| payload | `tcechoes:appraisal_result` | 鉴定结果 | Appraisal Result | E0 | S2C |
| payload | `tcechoes:appraise_request` | 鉴定请求 | Appraise Request | E0 | C2S |
| payload | `tcechoes:chronicle_compile` | 编纂 | Chronicle Compile | E3 | C2S |
| payload | `tcechoes:heritage_edit` | 遗产编辑 | Heritage Edit | E0 | C2S，版本号 |
| payload | `tcechoes:history_preview` | 模拟预览 | History Preview | E0 | S2C |
| payload | `tcechoes:history_progress` | 模拟进度 | History Progress | E0 | S2C |
| payload | `tcechoes:legend_sync` | 传说同步 | Legend Sync | E0 | S2C |
| payload | `tcechoes:script_sync` | 文字同步 | Script Sync | E0 | S2C |
| payload | `tcechoes:site_dossier` | 遗址档案 | Site Dossier | E0 | 双向 |
| payload | `tcechoes:stele_text` | 碑文 | Stele Text | E3 | C2S |
| hud_widget | `tcechoes:clue_compass` | 线索罗盘 | Clue Compass | E0 |  |
| civ_tab | `tcechoes:legends` | 传说 | Legends | E0 | 知识分组，排序 870 |
| region_layer | `tcechoes:history` | 历史分量 | History Layer | E0 | §11.3 |
| sound | `tcechoes:block.relic_shrine.place` | 器物落座 | Shrine Place | E1 |  |
| sound | `tcechoes:event.legend_revealed` | 传说揭晓 | Legend Revealed | E0 |  |
| sound | `tcechoes:item.rubbing.take` | 擦拓 | Rubbing Take | E0 |  |

### 5.13 契约 `tccommons`（58 项，来源 [19-commons.md](19-commons.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tccommons:ballot_box` | 投票箱 | Ballot Box | E1 | 投票页入口 |
| block | `tccommons:civic_terminal` | 市民终端 | Civic Terminal | E9 | 远程投票 |
| block | `tccommons:claim_post` | 界标 | Claim Post | E0 | 地块生效标记 |
| block | `tccommons:council_seat` | 议席 | Council Seat | E1 | 表决者就座 |
| block | `tccommons:council_table` | 议事桌 | Council Table | E2 | 编纂法典、组装石碑 |
| block | `tccommons:courthouse_bench` | 审判席 | Courthouse Bench | E4 | 案件队列 |
| block | `tccommons:covenant_chest` | 约定箱 | Covenant Chest | E1 | 约定托管 |
| block | `tccommons:donation_box` | 捐献箱 | Donation Box | E1 | 工程材料众筹 |
| block | `tccommons:law_stele` | 律法石碑 | Law Stele | E2 | stele_of_law 判定 |
| block | `tccommons:notice_board` | 公告板 | Notice Board | E2 | 公示法令与提案 |
| block | `tccommons:polling_booth` | 投票亭 | Polling Booth | E7 | 普选投票点 |
| block | `tccommons:treasury_chest` | 国库箱 | Treasury Chest | E3 | 实物国库 |
| block | `tccommons:works_marker` | 工程标记 | Works Marker | E1 | 立项与蓝图 |
| block | `tccommons:works_scaffold` | 工程脚手架 | Works Scaffold | E1 | 自动生成，不可合成 |
| block | `tccommons:zone_sign` | 区域标牌 | Zone Sign | E0 | 法律区域标记 |
| item | `tccommons:colony_charter` | 殖民地特许状 | Colony Charter | E10 | 殖民地自治 |
| item | `tccommons:contract_scroll` | 契约卷轴 | Contract Scroll | E0 | 约定凭证 |
| item | `tccommons:land_deed` | 地契 | Land Deed | E2 | 转让与租借 |
| item | `tccommons:law_code` | 法典 | Law Code | E2 | 生效法令快照 |
| item | `tccommons:survey_cord` | 勘界绳 | Survey Cord | E0 | 勘界模式 |
| item | `tccommons:town_charter` | 城镇特许状 | Town Charter | E4 | 地方治理 |
| block_entity | `tccommons:ballot_box` | 投票箱 | Ballot Box | E1 | 议事地点缓存 |
| block_entity | `tccommons:civic_terminal` | 市民终端 | Civic Terminal | E9 | 能源与绑定文明 |
| block_entity | `tccommons:council_table` | 议事桌 | Council Table | E2 | 编纂槽 |
| block_entity | `tccommons:courthouse_bench` | 审判席 | Courthouse Bench | E4 | 就座法官 |
| block_entity | `tccommons:covenant_chest` | 约定箱 | Covenant Chest | E1 | 各方投入与取货格 |
| block_entity | `tccommons:donation_box` | 捐献箱 | Donation Box | E1 | 材料缓冲 |
| block_entity | `tccommons:law_stele` | 律法石碑 | Law Stele | E2 | 阶段与法典快照 |
| block_entity | `tccommons:notice_board` | 公告板 | Notice Board | E2 | 显示内容 |
| block_entity | `tccommons:polling_booth` | 投票亭 | Polling Booth | E7 | 选区 |
| block_entity | `tccommons:treasury_chest` | 国库箱 | Treasury Chest | E3 | 物品栏 |
| block_entity | `tccommons:works_marker` | 工程标记 | Works Marker | E1 | 工程 ID |
| menu | `tccommons:council_table` | 议事桌菜单 | Council Table Menu | E2 | 法典与石碑 |
| menu | `tccommons:covenant_chest` | 约定箱菜单 | Covenant Chest Menu | E1 | 当事方视图 |
| menu | `tccommons:donation_box` | 捐献菜单 | Donation Menu | E1 | 投入材料 |
| menu | `tccommons:treasury_chest` | 国库菜单 | Treasury Menu | E3 | 合并国库视图 |
| payload | `tccommons:ballot_cast` | 投票 | Ballot Cast | E0 | C→S |
| payload | `tccommons:case_rule` | 案件裁决 | Case Rule | E2 | C→S |
| payload | `tccommons:claim_edit` | 勘界提交 | Claim Edit | E0 | C→S |
| payload | `tccommons:claim_sync` | 地块同步 | Claim Sync | E0 | S→C 增量 |
| payload | `tccommons:covenant_edit` | 约定编辑 | Covenant Edit | E0 | C→S |
| payload | `tccommons:covenant_sign` | 约定签署 | Covenant Sign | E0 | C→S |
| payload | `tccommons:office_assign` | 职位任免 | Office Assign | E0 | C→S |
| payload | `tccommons:parcel_settings` | 地块设置 | Parcel Settings | E0 | C→S，带 version |
| payload | `tccommons:proposal_submit` | 提交提案 | Proposal Submit | E0 | C→S |
| payload | `tccommons:role_assign` | 角色调整 | Role Assign | E0 | C→S |
| payload | `tccommons:statute_decree` | 颁布法令 | Statute Decree | E0 | C→S |
| payload | `tccommons:stele_dedicate` | 石碑刻立 | Stele Dedicate | E2 | C→S |
| payload | `tccommons:vote_update` | 投票更新 | Vote Update | E0 | S→C |
| payload | `tccommons:works_edit` | 工程编辑 | Works Edit | E1 | C→S |
| hud_widget | `tccommons:claim_status` | 领地状态 | Claim Status | E0 | 情境显示 |
| civ_tab | `tccommons:governance` | 治理 | Governance | E0 | 文明组，排序 250 |
| region_layer | `tccommons:claim` | 领地层 | Claim Layer | E0 | Atlas 与核心地图着色 |
| sound | `tccommons:block.stele.engrave` | 石碑刻字 | Stele Engrave | E2 | 每阶段 |
| sound | `tccommons:event.vote.failed` | 提案失败 | Vote Failed | E0 | 未通过 |
| sound | `tccommons:event.vote.open` | 投票开始 | Vote Open | E0 | 公示 |
| sound | `tccommons:event.vote.passed` | 提案通过 | Vote Passed | E0 | 生效 |
| sound | `tccommons:event.works_complete` | 工程落成 | Works Complete | E1 | 落成仪式 |

### 5.14 远征 `tcfrontiers`（188 项，来源 [20-frontiers.md](20-frontiers.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcfrontiers:air_raid_siren` | 防空警报 | Air Raid Siren | E8 | 空袭预警 |
| block | `tcfrontiers:alarm_bell` | 警钟 | Alarm Bell | E3 | 集结民兵、避难 |
| block | `tcfrontiers:anti_aircraft_gun` | 防空炮 | Anti-Aircraft Gun | E8 | 对空射击 |
| block | `tcfrontiers:archery_target` | 箭靶 | Archery Target | E2 | 远程训练 |
| block | `tcfrontiers:armory_rack` | 军械架 | Armory Rack | E3 | 民兵装备库 |
| block | `tcfrontiers:arrow_slit` | 箭孔 | Arrow Slit | E3 | 单向射孔 |
| block | `tcfrontiers:barbed_wire` | 铁丝网 | Barbed Wire | E6 | 减速与割伤 |
| block | `tcfrontiers:blast_door` | 防爆门 | Blast Door | E8 | 钢档门 |
| block | `tcfrontiers:bounty_board` | 告示板 | Bounty Board | E2 | 悬赏、雇佣佣兵 |
| block | `tcfrontiers:caltrops` | 铁蒺藜 | Caltrops | E3 | 减速与刺伤 |
| block | `tcfrontiers:drawbridge_hinge` | 吊桥铰链 | Drawbridge Hinge | E4 | 收放吊桥 |
| block | `tcfrontiers:earthwork_parapet` | 土木胸墙 | Earthwork Parapet | E6 | 抗爆掩体 |
| block | `tcfrontiers:emergency_beacon` | 应急信标 | Emergency Beacon | E10 | 远征求救定位 |
| block | `tcfrontiers:expedition_table` | 远征桌 | Expedition Table | E2 | 装载补给，打开远征规划 |
| block | `tcfrontiers:field_tent` | 野营帐篷 | Field Tent | E2 | 宿营点，可设重生点 |
| block | `tcfrontiers:gabion` | 石笼 | Gabion | E5 | 炮兵掩体 |
| block | `tcfrontiers:gate_winch` | 绞盘 | Gate Winch | E3 | 控制闸门 |
| block | `tcfrontiers:guard_post` | 哨位 | Guard Post | E1 | 站岗点，瞭望 64 |
| block | `tcfrontiers:gun_platform` | 炮位 | Gun Platform | E5 | 固定火炮 |
| block | `tcfrontiers:hazard_forecast_console` | 太空风险预报台 | Hazard Forecast Console | E10 | 远征风险窗口 |
| block | `tcfrontiers:machine_gun_nest` | 机枪巢 | Machine Gun Nest | E7 | 架设机枪 |
| block | `tcfrontiers:murder_hole` | 杀人孔 | Murder Hole | E4 | 向下攻击 |
| block | `tcfrontiers:outpost_core` | 前哨核心 | Outpost Core | E2 | 建立前哨 §5.4 |
| block | `tcfrontiers:pavise` | 大盾 | Pavise | E4 | 可放置掩体 |
| block | `tcfrontiers:pillbox_embrasure` | 碉堡射孔 | Pillbox Embrasure | E7 | 混凝土射孔 |
| block | `tcfrontiers:pitch_cauldron` | 热油锅 | Pitch Cauldron | E4 | 倾倒热沥青 |
| block | `tcfrontiers:portcullis` | 闸门 | Portcullis | E3 | 多格升降闸门 |
| block | `tcfrontiers:powder_magazine` | 火药库 | Powder Magazine | E5 | 火药存储，可殉爆 |
| block | `tcfrontiers:rapid_response_console` | 快速反应调度台 | Rapid Response Console | E9 | 汇总警报并派遣 |
| block | `tcfrontiers:sandbag` | 沙袋 | Sandbag | E6 | 半格掩体 |
| block | `tcfrontiers:searchlight` | 探照灯 | Searchlight | E7 | 夜间瞭望 160 |
| block | `tcfrontiers:signal_beacon` | 烽燧 | Signal Beacon | E3 | 链式警报 |
| block | `tcfrontiers:stake_barrier` | 尖桩障碍 | Stake Barrier | E1 | 接触伤害，阻骑兵 |
| block | `tcfrontiers:supply_cache` | 补给箱 | Supply Cache | E2 | 补给计数容器 |
| block | `tcfrontiers:training_dummy` | 训练假人 | Training Dummy | E3 | 近战训练 |
| block | `tcfrontiers:war_banner` | 军旗 | War Banner | E3 | 士气下限 +15 |
| block | `tcfrontiers:watch_fire` | 警戒火堆 | Watch Fire | E1 | 守夜瞭望 48，吓阻野兽 |
| block | `tcfrontiers:watchtower_platform` | 瞭望台平台 | Watchtower Platform | E3 | 瞭望 96-160 |
| item | `tcfrontiers:aramid_helmet` | 芳纶头盔 | Aramid Helmet | E9 | §6.3 |
| item | `tcfrontiers:arbalest` | 重弩 | Arbalest | E4 | §6.2 |
| item | `tcfrontiers:armet` | 全盔 | Armet | E4 | §6.3 |
| item | `tcfrontiers:arquebus` | 火绳枪 | Arquebus | E5 | §6.2 |
| item | `tcfrontiers:artillery_shell` | 炮弹 | Artillery Shell | E6 | 器械弹药 |
| item | `tcfrontiers:assault_rifle` | 突击步枪 | Assault Rifle | E8 | §6.2 |
| item | `tcfrontiers:bandit_insignia` | 匪首徽记 | Bandit Insignia | E2 | 悬赏凭证 |
| item | `tcfrontiers:bayonet` | 刺刀 | Bayonet | E6 | §6.1 |
| item | `tcfrontiers:bodkin_arrow` | 锥头箭 | Bodkin Arrow | E4 | 破甲弹药 |
| item | `tcfrontiers:bolt_action_rifle` | 栓动步枪 | Bolt-Action Rifle | E7 | §6.2 |
| item | `tcfrontiers:breech_loading_rifle` | 后装步枪 | Breech-Loading Rifle | E6 | §6.2 |
| item | `tcfrontiers:bronze_arrow` | 青铜箭 | Bronze Arrow | E2 | 弹药 |
| item | `tcfrontiers:bronze_battle_axe` | 青铜战斧 | Bronze Battle Axe | E2 | §6.1 |
| item | `tcfrontiers:bronze_dagger` | 青铜匕首 | Bronze Dagger | E2 | §6.1 |
| item | `tcfrontiers:bronze_greaves` | 青铜胫甲 | Bronze Greaves | E2 | §6.3 |
| item | `tcfrontiers:bronze_helmet` | 青铜盔 | Bronze Helmet | E2 | §6.3 |
| item | `tcfrontiers:bronze_shield` | 青铜盾 | Bronze Shield | E2 | §6.3 |
| item | `tcfrontiers:bronze_spear` | 青铜矛 | Bronze Spear | E2 | §6.1 |
| item | `tcfrontiers:buff_coat` | 牛皮外套 | Buff Coat | E5 | §6.3 |
| item | `tcfrontiers:cannonball` | 实心炮弹 | Cannonball | E5 | 器械弹药 |
| item | `tcfrontiers:carbine` | 卡宾枪 | Carbine | E9 | §6.2 |
| item | `tcfrontiers:cavalry_sabre` | 骑兵马刀 | Cavalry Sabre | E6 | §6.1 |
| item | `tcfrontiers:combat_boots` | 作战靴 | Combat Boots | E8 | §6.3 |
| item | `tcfrontiers:composite_bow` | 复合弓 | Composite Bow | E2 | §6.2 |
| item | `tcfrontiers:crossbow` | 弩 | Crossbow | E3 | §6.2 |
| item | `tcfrontiers:crossbow_bolt` | 弩矢 | Crossbow Bolt | E3 | 弹药 |
| item | `tcfrontiers:field_medical_kit` | 战地医疗包 | Field Medical Kit | E2 | 3 MU |
| item | `tcfrontiers:firing_table` | 射表 | Firing Table | E6 | 仰角射程换算 |
| item | `tcfrontiers:flak_jacket` | 防破片背心 | Flak Jacket | E8 | §6.3 |
| item | `tcfrontiers:flintlock_pistol` | 燧发手枪 | Flintlock Pistol | E5 | §6.2 |
| item | `tcfrontiers:fodder_sack` | 草料袋 | Fodder Sack | E2 | 4 GU |
| item | `tcfrontiers:halberd` | 长戟 | Halberd | E4 | §6.1 |
| item | `tcfrontiers:hand_grenade` | 手榴弹 | Hand Grenade | E7 | §6.2 |
| item | `tcfrontiers:heater_shield` | 鸢形盾 | Heater Shield | E4 | §6.3 |
| item | `tcfrontiers:hobnailed_boots` | 钉靴 | Hobnailed Boots | E3 | §6.3 |
| item | `tcfrontiers:intermediate_cartridge` | 中间威力弹 | Intermediate Cartridge | E8 | 弹药 |
| item | `tcfrontiers:iron_arrow` | 铁箭 | Iron Arrow | E3 | 弹药 |
| item | `tcfrontiers:iron_battle_axe` | 铁战斧 | Iron Battle Axe | E3 | §6.1 |
| item | `tcfrontiers:iron_helmet` | 铁盔 | Iron Helmet | E3 | §6.3 |
| item | `tcfrontiers:iron_spear` | 铁矛 | Iron Spear | E3 | §6.1 |
| item | `tcfrontiers:iron_sword` | 铁剑 | Iron Sword | E3 | §6.1 |
| item | `tcfrontiers:javelin` | 标枪 | Javelin | E2 | §6.2 |
| item | `tcfrontiers:khopesh` | 镰形剑 | Khopesh | E2 | §6.1 |
| item | `tcfrontiers:lamellar_cuirass` | 札甲 | Lamellar Cuirass | E3 | §6.3 |
| item | `tcfrontiers:lamellar_tassets` | 札甲裙 | Lamellar Tassets | E3 | §6.3 |
| item | `tcfrontiers:lead_ball` | 铅弹 | Lead Ball | E5 | 弹药 |
| item | `tcfrontiers:light_machine_gun` | 轻机枪 | Light Machine Gun | E7 | §6.2 |
| item | `tcfrontiers:linen_cuirass` | 亚麻胸甲 | Linen Cuirass | E2 | §6.3 |
| item | `tcfrontiers:longbow` | 长弓 | Longbow | E4 | §6.2 |
| item | `tcfrontiers:longsword` | 长剑 | Longsword | E4 | §6.1 |
| item | `tcfrontiers:mace` | 钉头锤 | Mace | E4 | §6.1 |
| item | `tcfrontiers:mail_chausses` | 锁子护腿 | Mail Chausses | E4 | §6.3 |
| item | `tcfrontiers:mail_hauberk` | 锁子长衣 | Mail Hauberk | E4 | §6.3 |
| item | `tcfrontiers:matchlock_musket` | 火绳滑膛枪 | Matchlock Musket | E5 | §6.2 |
| item | `tcfrontiers:metallic_cartridge` | 金属弹壳弹 | Metallic Cartridge | E6 | 弹药 |
| item | `tcfrontiers:morion` | 高冠盔 | Morion | E5 | §6.3 |
| item | `tcfrontiers:mortar_shell` | 迫击炮弹 | Mortar Shell | E7 | 器械弹药 |
| item | `tcfrontiers:night_vision_goggles` | 夜视仪 | Night Vision Goggles | E9 | 头盔配件 |
| item | `tcfrontiers:paper_cartridge` | 纸壳弹 | Paper Cartridge | E5 | 弹药 |
| item | `tcfrontiers:pike` | 长枪 | Pike | E4 | §6.1 |
| item | `tcfrontiers:pikeman_cuirass` | 防弹胸甲 | Pikeman Cuirass | E5 | §6.3 |
| item | `tcfrontiers:plate_carrier` | 插板防弹衣 | Plate Carrier | E9 | §6.3 |
| item | `tcfrontiers:plate_cuirass` | 板甲胸甲 | Plate Cuirass | E4 | §6.3 |
| item | `tcfrontiers:plate_cuisses` | 板甲腿甲 | Plate Cuisses | E4 | §6.3 |
| item | `tcfrontiers:powder_flask` | 火药壶 | Powder Flask | E5 | 散装火药 |
| item | `tcfrontiers:rapier` | 刺剑 | Rapier | E5 | §6.1 |
| item | `tcfrontiers:ration_pack` | 口粮包 | Ration Pack | E2 | 4 RU |
| item | `tcfrontiers:repair_kit` | 修理包 | Repair Kit | E3 | 4 PU |
| item | `tcfrontiers:revolver` | 左轮手枪 | Revolver | E6 | §6.2 |
| item | `tcfrontiers:rifled_musket` | 线膛枪 | Rifled Musket | E6 | §6.2 |
| item | `tcfrontiers:round_shield` | 圆盾 | Round Shield | E3 | §6.3 |
| item | `tcfrontiers:sabatons` | 铁靴 | Sabatons | E4 | §6.3 |
| item | `tcfrontiers:siege_engine_frame` | 攻城器械框架 | Siege Engine Frame | E3 | 组装器械 |
| item | `tcfrontiers:siege_ladder` | 云梯 | Siege Ladder | E3 | 攀墙 |
| item | `tcfrontiers:signal_horn` | 号角 | Signal Horn | E1 | 便携警报 |
| item | `tcfrontiers:sling` | 投石索 | Sling | E2 | §6.2 |
| item | `tcfrontiers:sling_bullet` | 投石弹 | Sling Bullet | E2 | 弹药 |
| item | `tcfrontiers:steel_helmet` | 钢盔 | Steel Helmet | E7 | §6.3 |
| item | `tcfrontiers:stone_shot` | 石弹 | Stone Shot | E3 | 器械弹药 |
| item | `tcfrontiers:submachine_gun` | 冲锋枪 | Submachine Gun | E8 | §6.2 |
| item | `tcfrontiers:warhammer` | 战锤 | Warhammer | E4 | §6.1 |
| item | `tcfrontiers:water_cask` | 水桶囊 | Water Cask | E2 | 8 WU |
| item | `tcfrontiers:wicker_shield` | 柳条盾 | Wicker Shield | E2 | §6.3 |
| entity | `tcfrontiers:ballista` | 弩炮 | Ballista | E3 | §5.9.2 |
| entity | `tcfrontiers:ballistic_projectile` | 枪弹 | Ballistic Projectile | E5 | 火器弹道 |
| entity | `tcfrontiers:battering_ram` | 攻城槌 | Battering Ram | E3 | §5.9.2 |
| entity | `tcfrontiers:bombard` | 射石炮 | Bombard | E5 | §5.9.2 |
| entity | `tcfrontiers:field_cannon` | 野战炮 | Field Cannon | E5 | §5.9.2 |
| entity | `tcfrontiers:field_gun` | 后膛野战炮 | Field Gun | E6 | §5.9.2 |
| entity | `tcfrontiers:mangonel` | 投石机 | Mangonel | E3 | §5.9.2 |
| entity | `tcfrontiers:mortar` | 迫击炮 | Mortar | E7 | §5.9.2 |
| entity | `tcfrontiers:ranged_projectile` | 箭矢弹丸 | Ranged Projectile | E2 | 箭、弩矢、投石弹共用 |
| entity | `tcfrontiers:siege_projectile` | 器械弹丸 | Siege Projectile | E3 | 石弹与炮弹 |
| entity | `tcfrontiers:siege_tower` | 攻城塔 | Siege Tower | E3 | §5.9.2 |
| entity | `tcfrontiers:thrown_grenade` | 投掷手榴弹 | Thrown Grenade | E7 | 定时引信 |
| entity | `tcfrontiers:trebuchet` | 配重投石机 | Trebuchet | E4 | §5.9.2 |
| structure | `tcfrontiers:bandit_fort` | 匪寨 | Bandit Fort | E2 | 20 人以上团体 |
| structure | `tcfrontiers:bandit_lair` | 匪窝 | Bandit Lair | E2 | 运行时占据生成 |
| structure | `tcfrontiers:deserter_camp` | 逃兵营 | Deserter Camp | E5 | 壕沟营地 |
| structure | `tcfrontiers:pirate_cove` | 海盗湾 | Pirate Cove | E5 | 海岸洞窟 |
| structure | `tcfrontiers:raider_camp` | 劫掠者营地 | Raider Camp | E2 | 可移动帐篷群 |
| block_entity | `tcfrontiers:alarm_bell` | 警钟 | Alarm Bell | E3 | 冷却与集结点 |
| block_entity | `tcfrontiers:anti_aircraft_gun` | 防空炮 | Anti-Aircraft Gun | E8 | 目标与弹药 |
| block_entity | `tcfrontiers:armory_rack` | 军械架 | Armory Rack | E3 | 装备与领用记录 |
| block_entity | `tcfrontiers:bounty_board` | 告示板 | Bounty Board | E2 | 悬赏缓存 |
| block_entity | `tcfrontiers:drawbridge_hinge` | 吊桥铰链 | Drawbridge Hinge | E4 | 吊桥结构 |
| block_entity | `tcfrontiers:emergency_beacon` | 应急信标 | Emergency Beacon | E10 | 广播状态 |
| block_entity | `tcfrontiers:expedition_table` | 远征桌 | Expedition Table | E2 | 装载缓存 |
| block_entity | `tcfrontiers:gate_winch` | 绞盘 | Gate Winch | E3 | 闸门结构 |
| block_entity | `tcfrontiers:hazard_forecast_console` | 太空风险预报台 | Hazard Forecast Console | E10 | 预报缓存 |
| block_entity | `tcfrontiers:machine_gun_nest` | 机枪巢 | Machine Gun Nest | E7 | 架设武器 |
| block_entity | `tcfrontiers:outpost_core` | 前哨核心 | Outpost Core | E2 | 前哨状态 |
| block_entity | `tcfrontiers:powder_magazine` | 火药库 | Powder Magazine | E5 | 库存 |
| block_entity | `tcfrontiers:rapid_response_console` | 快速反应调度台 | Rapid Response Console | E9 | 警报队列 |
| block_entity | `tcfrontiers:searchlight` | 探照灯 | Searchlight | E7 | 朝向 |
| block_entity | `tcfrontiers:signal_beacon` | 烽燧 | Signal Beacon | E3 | 链路状态 |
| block_entity | `tcfrontiers:supply_cache` | 补给箱 | Supply Cache | E2 | 库存 |
| block_entity | `tcfrontiers:war_banner` | 军旗 | War Banner | E3 | 旗帜图案 |
| menu | `tcfrontiers:armory_rack` | 军械架 | Armory Rack | E3 | 容器 |
| menu | `tcfrontiers:bounty_board` | 告示板 | Bounty Board | E2 | 悬赏与雇佣 |
| menu | `tcfrontiers:expedition_loadout` | 远征装载 | Expedition Loadout | E2 | 装载补给 |
| menu | `tcfrontiers:outpost` | 前哨 | Outpost | E2 | 前哨核心界面 |
| menu | `tcfrontiers:siege_engine` | 攻城器械 | Siege Engine | E3 | 装填与瞄准 |
| menu | `tcfrontiers:supply_cache` | 补给箱 | Supply Cache | E2 | 容器 |
| keybind | `tcfrontiers:reload` | 装填 | Reload | — | key.tcfrontiers.reload，默认不绑定 |
| payload | `tcfrontiers:band_intel_sync` | 盗匪情报同步 | Band Intel Sync | — | S→C |
| payload | `tcfrontiers:bounty_list_sync` | 悬赏列表同步 | Bounty List Sync | — | S→C |
| payload | `tcfrontiers:danger_sync` | 危险度同步 | Danger Sync | — | S→C |
| payload | `tcfrontiers:expedition_plan_update` | 远征草稿更新 | Expedition Plan Update | — | C→S |
| payload | `tcfrontiers:expedition_state_sync` | 远征状态同步 | Expedition State Sync | — | S→C |
| payload | `tcfrontiers:militia_order` | 民兵命令 | Militia Order | — | C→S |
| payload | `tcfrontiers:outpost_sync` | 前哨同步 | Outpost Sync | — | S→C |
| payload | `tcfrontiers:siege_engine_control` | 器械操控 | Siege Engine Control | — | C→S |
| payload | `tcfrontiers:threat_alert` | 威胁警报 | Threat Alert | — | S→C |
| payload | `tcfrontiers:weapon_action` | 武器动作 | Weapon Action | — | C→S |
| hud_widget | `tcfrontiers:ammo_counter` | 弹药计数 | Ammo Counter | E2 | hud.tcfrontiers.ammo_counter |
| hud_widget | `tcfrontiers:danger_level` | 危险等级 | Danger Level | — | hud.tcfrontiers.danger_level |
| hud_widget | `tcfrontiers:expedition_supply` | 远征补给 | Expedition Supply | E2 | hud.tcfrontiers.expedition_supply |
| civ_tab | `tcfrontiers:defense` | 防务 | Defense | E1 | civtab.tcfrontiers.defense，文明组 280 |
| civ_tab | `tcfrontiers:expeditions` | 远征 | Expeditions | E2 | civtab.tcfrontiers.expeditions，世界组 700 |
| region_layer | `tcfrontiers:danger` | 危险度 | Danger | — | 7 分量，每地区 8 字节 |
| sound | `tcfrontiers:block.air_raid_siren.wail` | 防空警报声 | Air Raid Siren | E8 | — |
| sound | `tcfrontiers:block.alarm_bell.ring` | 警钟声 | Alarm Bell Ring | E3 | — |
| sound | `tcfrontiers:entity.bowstring.release` | 弓弦声 | Bowstring Release | E2 | — |
| sound | `tcfrontiers:entity.cannon.fire` | 炮声 | Cannon Fire | E5 | — |
| sound | `tcfrontiers:entity.machine_gun.fire` | 机枪射击 | Machine Gun Fire | E7 | — |
| sound | `tcfrontiers:entity.musket.fire` | 火枪射击 | Musket Fire | E5 | — |
| sound | `tcfrontiers:entity.trebuchet.release` | 投石机释放 | Trebuchet Release | E4 | — |
| sound | `tcfrontiers:event.war_drum` | 战鼓 | War Drum | E2 | 盗匪进攻信号 |
| sound | `tcfrontiers:item.signal_horn.blow` | 号角声 | Signal Horn Blow | E1 | — |

### 5.15 异界 `tcbeyond`（105 项，来源 [21-beyond.md](21-beyond.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcbeyond:alembic` | 蒸馏器 | Alembic | E4 | 灵药 |
| block | `tcbeyond:anomalous_crystal` | 异质晶簇 | Anomalous Crystal | — | 覆盖方块；3 阶段 |
| block | `tcbeyond:anomaly_sensor` | 异象传感器 | Anomaly Sensor | E8 | 红石输出、涌潮预报 |
| block | `tcbeyond:attunement_altar` | 调谐台 | Attunement Altar | E3 | 研磨 / 精炼 / 调谐 |
| block | `tcbeyond:chalk_ward` | 镇界粉线 | Chalk Ward | E3 | 压制 15 |
| block | `tcbeyond:crystal_synthesizer` | 晶体合成器 | Crystal Synthesizer | E9 | 合成晶体 |
| block | `tcbeyond:ember_crucible` | 余烬坩埚 | Ember Crucible | E6 | 200 HU/s |
| block | `tcbeyond:ember_heart` | 余烬心座 | Ember Heart | — | 产出余烬之心 |
| block | `tcbeyond:field_stabilizer` | 场稳定器 | Field Stabilizer | E8 | 压制 60，r128 |
| block | `tcbeyond:gate_spire_core` | 门塔核心 | Gate Spire Core | — | 调谐 |
| block | `tcbeyond:glimmer_lichen` | 微光地衣 | Glimmer Lichen | — | 覆盖方块 |
| block | `tcbeyond:pact_stone` | 契约石 | Pact Stone | — | 4 块 |
| block | `tcbeyond:resonance_gate` | 共鸣门 | Resonance Gate | E7 | 跨维度门 |
| block | `tcbeyond:resonance_relay` | 共鸣中继 | Resonance Relay | E7 | 跨维度通信 |
| block | `tcbeyond:resonant_cluster` | 共鸣晶簇 | Resonant Cluster | — | silent_deep |
| block | `tcbeyond:rift_gate` | 门芯 | Rift Gate | — | 裂隙之门 |
| block | `tcbeyond:rift_portal` | 裂隙帷幕 | Rift Portal | — | 传送方块 |
| block | `tcbeyond:ritual_circle` | 仪式法阵 | Ritual Circle | E4 | 封印 |
| block | `tcbeyond:veil_bloom` | 帷花 | Veil Bloom | — | 覆盖方块 |
| block | `tcbeyond:verdant_charm` | 翠华护符 | Verdant Charm | E4 | 作物 +25% |
| block | `tcbeyond:ward_stone` | 镇界石 | Ward Stone | E4 | 压制 25，r48 |
| block | `tcbeyond:waystone` | 渡界石 | Waystone | E5 | 传送 |
| block | `tcbeyond:whisper_reed` | 低语芦苇 | Whisper Reed | — | 覆盖方块 |
| item | `tcbeyond:anomalous_dust` | 异质粉尘 | Anomalous Dust | E3 | 研磨 |
| item | `tcbeyond:anomalous_shard` | 异质碎片 | Anomalous Shard | — | 基础材料 |
| item | `tcbeyond:anomaly_lens` | 异象透镜 | Anomaly Lens | E3 | 读数、观察 |
| item | `tcbeyond:ashen_sigil` | 灰烬信物 | Ashen Sigil | E5 | 门塔 |
| item | `tcbeyond:attuned_crystal` | 已调谐晶体 | Attuned Crystal | E4 | — |
| item | `tcbeyond:elixir_of_embers` | 余烬灵药 | Elixir of Embers | E4 | — |
| item | `tcbeyond:elixir_of_hush` | 静默灵药 | Elixir of Hush | E4 | — |
| item | `tcbeyond:elixir_of_sight` | 明视灵药 | Elixir of Sight | E4 | — |
| item | `tcbeyond:elixir_of_warding` | 镇邪灵药 | Elixir of Warding | E4 | — |
| item | `tcbeyond:ember_core` | 余烬之心 | Ember Core | — | 余烬心座产出 |
| item | `tcbeyond:pale_sigil` | 苍白信物 | Pale Sigil | E5 | 门塔 |
| item | `tcbeyond:refined_crystal` | 精炼晶体 | Refined Crystal | E4 | — |
| item | `tcbeyond:resonant_crystal` | 共鸣晶体 | Resonant Crystal | — | silent_deep |
| item | `tcbeyond:rift_key` | 界钥 | Rift Key | E5 | 开门 |
| item | `tcbeyond:sigil_fragment` | 信物残片 | Sigil Fragment | — | 门塔守卫掉落 |
| item | `tcbeyond:silent_sigil` | 静默信物 | Silent Sigil | E5 | 门塔 |
| item | `tcbeyond:stabilized_essence` | 稳定精华 | Stabilized Essence | E8 | 离心机 |
| item | `tcbeyond:synthetic_crystal` | 合成晶体 | Synthetic Crystal | E9 | 代替已调谐晶体 |
| item | `tcbeyond:twisted_produce` | 异化作物 | Twisted Produce | — | 炼药试剂 |
| item | `tcbeyond:veil_essence` | 帷质 | Veil Essence | E4 | 蒸馏 |
| item | `tcbeyond:verdant_tincture` | 翠华酊 | Verdant Tincture | E4 | — |
| item | `tcbeyond:warding_chalk` | 镇界粉笔 | Warding Chalk | E3 | 放置粉线 |
| entity | `tcbeyond:crystal_mite` | 晶螨 | Crystal Mite | — | 8 HP |
| entity | `tcbeyond:rift_wisp` | 裂隙磷火 | Rift Wisp | — | 6 HP |
| entity | `tcbeyond:spire_sentinel` | 门塔守卫 | Spire Sentinel | — | 60 HP |
| entity | `tcbeyond:veil_stalker` | 帷影潜伏者 | Veil Stalker | — | 28 HP |
| dimension | `tcbeyond:ashen_reach` | 灰烬之境 | Ashen Reach | E5 | 下界内容；默认只在深地模式启用 |
| dimension | `tcbeyond:pale_hollow` | 苍白林境 | Pale Hollow | E5 | 灾厄村民、府邸、前哨 |
| dimension | `tcbeyond:silent_deep` | 静默深渊 | Silent Deep | E5 | 监守者、远古城市、沉渊 |
| biome | `tcbeyond:drowned_vault` | 沉渊 | Drowned Vault | — | silent_deep；守卫者 |
| biome | `tcbeyond:hollow_moor` | 空寂荒原 | Hollow Moor | — | pale_hollow |
| biome | `tcbeyond:mansion_wilds` | 府邸荒林 | Mansion Wilds | — | pale_hollow；林地府邸 |
| biome | `tcbeyond:pale_thicket` | 苍白密林 | Pale Thicket | — | pale_hollow |
| biome | `tcbeyond:raider_steppe` | 劫掠草原 | Raider Steppe | — | pale_hollow；前哨站 |
| biome | `tcbeyond:resonant_caverns` | 共鸣洞 | Resonant Caverns | — | silent_deep；共鸣晶簇 |
| biome | `tcbeyond:silent_dark` | 静默暗域 | Silent Dark | — | silent_deep；远古城市 |
| structure | `tcbeyond:aurora_ring` | 磁石环 | Aurora Ring | — | 极光疤焦点中心 |
| structure | `tcbeyond:drowned_reliquary` | 沉渊圣所 | Drowned Reliquary | — | 代替海底神殿 |
| structure | `tcbeyond:ember_altar` | 余烬祭坛 | Ember Altar | — | 余烬空洞 / 原版下界 |
| structure | `tcbeyond:gate_spire` | 门塔 | Gate Spire | — | 外岛 3 座 |
| structure | `tcbeyond:pact_circle` | 契约法阵 | Pact Circle | — | 末地主岛 |
| structure | `tcbeyond:return_spire` | 返回塔 | Return Spire | — | 异界维度出口 |
| structure | `tcbeyond:rift_gate_ruin` | 裂隙之门遗迹 | Rift Gate Ruin | — | 界阈入口；末影之眼目标 |
| structure | `tcbeyond:shattered_spire` | 碎塔 | Shattered Spire | — | 焦点中心（独立回退） |
| structure | `tcbeyond:starfall_crater` | 星坠坑 | Starfall Crater | — | 焦点中心 |
| structure | `tcbeyond:threshold_landing` | 界阈落点 | Threshold Landing | — | 末地主岛 |
| structure | `tcbeyond:vent_throat` | 晶喉 | Vent Throat | — | 深脉焦点中心 |
| block_entity | `tcbeyond:alembic` | 蒸馏器 | Alembic | E4 | — |
| block_entity | `tcbeyond:anomaly_sensor` | 异象传感器 | Anomaly Sensor | E8 | — |
| block_entity | `tcbeyond:attunement_altar` | 调谐台 | Attunement Altar | E3 | — |
| block_entity | `tcbeyond:crystal_synthesizer` | 晶体合成器 | Crystal Synthesizer | E9 | — |
| block_entity | `tcbeyond:ember_crucible` | 余烬坩埚 | Ember Crucible | E6 | — |
| block_entity | `tcbeyond:ember_heart` | 余烬心座 | Ember Heart | — | — |
| block_entity | `tcbeyond:field_stabilizer` | 场稳定器 | Field Stabilizer | E8 | — |
| block_entity | `tcbeyond:gate_spire_core` | 门塔核心 | Gate Spire Core | — | — |
| block_entity | `tcbeyond:pact_stone` | 契约石 | Pact Stone | — | — |
| block_entity | `tcbeyond:resonance_gate` | 共鸣门 | Resonance Gate | E7 | — |
| block_entity | `tcbeyond:resonance_relay` | 共鸣中继 | Resonance Relay | E7 | — |
| block_entity | `tcbeyond:rift_gate` | 门芯 | Rift Gate | — | — |
| block_entity | `tcbeyond:ritual_circle` | 仪式法阵 | Ritual Circle | E4 | — |
| block_entity | `tcbeyond:ward_stone` | 镇界石 | Ward Stone | E4 | — |
| block_entity | `tcbeyond:waystone` | 渡界石 | Waystone | E5 | — |
| menu | `tcbeyond:alembic` | 蒸馏器 | Alembic | E4 | — |
| menu | `tcbeyond:attunement_altar` | 调谐台 | Attunement Altar | E3 | — |
| menu | `tcbeyond:crystal_synthesizer` | 晶体合成器 | Crystal Synthesizer | E9 | — |
| menu | `tcbeyond:gate_spire` | 门塔 | Gate Spire | — | — |
| menu | `tcbeyond:waystone` | 渡界石 | Waystone | E5 | — |
| payload | `tcbeyond:beyond_mode_sync` | 模式同步 | Mode Sync | — | S→C |
| payload | `tcbeyond:gate_action` | 门操作 | Gate Action | — | C→S |
| payload | `tcbeyond:meter_sync` | 异常计同步 | Meter Sync | — | S→C |
| payload | `tcbeyond:relay_action` | 中继操作 | Relay Action | — | C→S |
| payload | `tcbeyond:rite_preview` | 法式预览 | Rite Preview | — | S→C |
| payload | `tcbeyond:rite_preview_request` | 法式预览请求 | Rite Preview Request | — | C→S |
| payload | `tcbeyond:waystone_action` | 渡界石操作 | Waystone Action | — | C→S |
| payload | `tcbeyond:zone_event` | 区域事件 | Zone Event | — | S→C |
| hud_widget | `tcbeyond:anomaly_meter` | 异常计 | Anomaly Meter | — | 标准，右中 |
| civ_tab | `tcbeyond:anomalies` | 异界 | Anomalies | — | 世界组，排序 750 |
| region_layer | `tcbeyond:anomaly` | 异常强度层 | Anomaly Layer | — | 每地区 ≤ 16 KiB |
| sound | `tcbeyond:ambient.anomaly_whisper` | 异常耳语 | Anomaly Whisper | — | 音景 |
| sound | `tcbeyond:block.crystal.resonance` | 晶体共鸣 | Crystal Resonance | — | 敲取 |
| sound | `tcbeyond:block.gate.hum` | 门之嗡鸣 | Gate Hum | — | 激活的门 |
| sound | `tcbeyond:event.surge_horn` | 涌潮号角 | Surge Horn | — | 涌潮 |

### 5.16 舆图 `tcatlas`（122 项，来源 [22-atlas.md](22-atlas.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcatlas:boundary_stone` | 界碑 | Boundary Stone | E2 | 定位点；Commons 边界标记 |
| block | `tcatlas:cairn` | 石堆 | Cairn | E0 | 记忆锚点；E2 起为定位点 |
| block | `tcatlas:chart_table` | 海图桌 | Chart Table | E5 | 地图桌 + 航线规划 |
| block | `tcatlas:developing_tank` | 显影罐 | Developing Tank | E8 | 冲洗航空照片 |
| block | `tcatlas:dumpy_level` | 水准仪 | Dumpy Level | E6 | 高差测量 |
| block | `tcatlas:field_wire` | 野战电线 | Field Wire | E7 | 无 Networks 时连接报告站 |
| block | `tcatlas:gis_terminal` | GIS 终端 | GIS Terminal | E9 | 全图层分析 |
| block | `tcatlas:globe` | 地球仪 | Globe | E5 | 打开 L0 全球视图 |
| block | `tcatlas:ground_station` | 地面站 | Ground Station | E10 | 卫星下行与抽象发射 |
| block | `tcatlas:map_stele` | 地图碑 | Map Stele | E2 | 周围 64 格写入 SL3 |
| block | `tcatlas:map_table` | 地图桌 | Map Table | E1 | 入库、复制、回忆绘图、任务区 |
| block | `tcatlas:plane_table` | 平板仪 | Plane Table | E6 | 测站周边 96 格 |
| block | `tcatlas:radar_antenna` | 雷达天线 | Radar Antenna | E8 | 雷达多方块顶部 |
| block | `tcatlas:radar_console` | 雷达操作台 | Radar Console | E8 | PPI 显示 |
| block | `tcatlas:radar_station` | 雷达站 | Radar Station | E8 | 雷达机柜，20 kW |
| block | `tcatlas:radio_beacon` | 无线电信标 | Radio Beacon | E7 | 测向定位，0.5 kW |
| block | `tcatlas:ranging_pole` | 标杆 | Ranging Pole | E3 | 测桩 128 格外可见 |
| block | `tcatlas:reporting_station` | 报告站 | Reporting Station | E7 | 实时数据，0.2 kW |
| block | `tcatlas:signpost` | 路标 | Signpost | E2 | 最多 4 块指向牌；定位点 |
| block | `tcatlas:situation_board` | 态势板 | Situation Board | E7 | 墙面实时地图 |
| block | `tcatlas:stereo_plotter` | 立体测图仪 | Stereo Plotter | E8 | 照片对 → SL5+H |
| block | `tcatlas:survey_stake` | 测桩 | Survey Stake | E3 | 导线端点 |
| block | `tcatlas:theodolite` | 经纬仪 | Theodolite | E6 | 三角测量 |
| block | `tcatlas:trig_pillar` | 三角点 | Trig Pillar | E6 | 三角网顶点与觅标 |
| block | `tcatlas:wall_map` | 挂墙地图 | Wall Map | E1 | 1×1 至 8×8 拼接显示 |
| block | `tcatlas:waypoint_flag` | 路点旗 | Waypoint Flag | E1 | 放置即创建文明路点 |
| item | `tcatlas:aerial_camera` | 航空相机 | Aerial Camera | E8 | 航拍 |
| item | `tcatlas:aerial_film` | 航空胶卷 | Aerial Film | E8 | 8 张/卷 |
| item | `tcatlas:aerial_photograph` | 航空照片 | Aerial Photograph | E8 | 冲洗后可测图 |
| item | `tcatlas:ancient_map` | 古地图 | Ancient Map | — | Echoes 战利品，SL1-SL3 |
| item | `tcatlas:astrolabe` | 星盘 | Astrolabe | E4 | 纬度 ±0.3° |
| item | `tcatlas:bound_atlas` | 地图集 | Bound Atlas | E5 | 容纳 16 张图纸 |
| item | `tcatlas:charcoal_stylus` | 炭笔 | Charcoal Stylus | E1 | 绘制草图 |
| item | `tcatlas:clay_tablet_map` | 陶板地图 | Clay Tablet Map | E2 | 只读，1,024 区块 |
| item | `tcatlas:gps_receiver` | GPS 接收机 | GPS Receiver | E10 | ±1 格定位 |
| item | `tcatlas:groma` | 十字杆 | Groma | E3 | 直角与直线 |
| item | `tcatlas:hand_drawn_map` | 手绘地图 | Hand-drawn Map | E1 | 手持记录，256 区块 |
| item | `tcatlas:imaging_payload` | 成像载荷 | Imaging Payload | E10 | 卫星影像 |
| item | `tcatlas:levelling_staff` | 水准尺 | Levelling Staff | E6 | 配合水准仪 |
| item | `tcatlas:lodestone_needle` | 磁石针 | Lodestone Needle | E4 | 罗盘部件 |
| item | `tcatlas:log_line` | 计程绳 | Log Line | E5 | 船速 |
| item | `tcatlas:magnetic_compass` | 磁罗盘 | Magnetic Compass | E4 | 方位与罗盘条 |
| item | `tcatlas:map_hide` | 空白兽皮图 | Blank Map Hide | E1 | E1 图纸 |
| item | `tcatlas:marine_chronometer` | 航海天文钟 | Marine Chronometer | E5 | 经度、推算漂移减半 |
| item | `tcatlas:measuring_rope` | 测绳 | Measuring Rope | E3 | 导线测距 ≤ 32 |
| item | `tcatlas:nautical_chart` | 航海图 | Nautical Chart | E5 | 经纬网、测深 |
| item | `tcatlas:navigation_payload` | 导航载荷 | Navigation Payload | E10 | GPS 星座 |
| item | `tcatlas:portolan_chart` | 波特兰海图 | Portolan Chart | E4 | 4,096 区块，岸线 |
| item | `tcatlas:radio_direction_finder` | 无线电测向仪 | Radio Direction Finder | E7 | σ 2° |
| item | `tcatlas:scroll_map` | 卷轴地图 | Scroll Map | E3 | 背包内记录，1,024 区块 |
| item | `tcatlas:sextant` | 六分仪 | Sextant | E5 | 纬度 ±0.03° |
| item | `tcatlas:survey_chain` | 测链 | Survey Chain | E6 | 基线 ≤ 64 |
| item | `tcatlas:surveyors_wheel` | 测距轮 | Surveyor's Wheel | E3 | 沿路测距 |
| item | `tcatlas:topographic_map` | 地形图 | Topographic Map | E6 | 地图库印制，只读 |
| item | `tcatlas:total_station` | 全站仪 | Total Station | E9 | 角度 + 距离 |
| block_entity | `tcatlas:chart_table` | 海图桌 | Chart Table | E5 | 航线规划状态 |
| block_entity | `tcatlas:developing_tank` | 显影罐 | Developing Tank | E8 | 冲洗进度 |
| block_entity | `tcatlas:gis_terminal` | GIS 终端 | GIS Terminal | E9 | 分析任务 |
| block_entity | `tcatlas:ground_station` | 地面站 | Ground Station | E10 | 下行窗口、抽象发射 |
| block_entity | `tcatlas:map_stele` | 地图碑 | Map Stele | E2 | 刻写文字 |
| block_entity | `tcatlas:map_table` | 地图桌 | Map Table | E1 | 海图桌复用其逻辑 |
| block_entity | `tcatlas:plane_table` | 平板仪 | Plane Table | E6 | 测站进度 |
| block_entity | `tcatlas:radar_station` | 雷达站 | Radar Station | E8 | 扫描状态与接触 |
| block_entity | `tcatlas:radio_beacon` | 无线电信标 | Radio Beacon | E7 | 频率与呼号 |
| block_entity | `tcatlas:reporting_station` | 报告站 | Reporting Station | E7 | 连接状态 |
| block_entity | `tcatlas:signpost` | 路标 | Signpost | E2 | 4 块指向牌 |
| block_entity | `tcatlas:situation_board` | 态势板 | Situation Board | E7 | 显示区域 |
| block_entity | `tcatlas:stereo_plotter` | 立体测图仪 | Stereo Plotter | E8 | 照片对队列 |
| block_entity | `tcatlas:theodolite` | 经纬仪 | Theodolite | E6 | 观测记录 |
| block_entity | `tcatlas:trig_pillar` | 三角点 | Trig Pillar | E6 | 网内编号与 ε |
| block_entity | `tcatlas:wall_map` | 挂墙地图 | Wall Map | E1 | 显示区域与拼接 |
| menu | `tcatlas:bound_atlas` | 地图集 | Bound Atlas | E5 | 16 格图纸容器 |
| menu | `tcatlas:developing_tank` | 显影罐 | Developing Tank | E8 | 冲洗 |
| menu | `tcatlas:gis_terminal` | GIS 终端 | GIS Terminal | E9 | 分析 |
| menu | `tcatlas:ground_station` | 地面站 | Ground Station | E10 | 下行与发射 |
| menu | `tcatlas:map_table` | 地图桌 | Map Table | E1 | 地图桌与海图桌共用 |
| menu | `tcatlas:plane_table` | 平板仪 | Plane Table | E6 | 测站 |
| menu | `tcatlas:radar_console` | 雷达操作台 | Radar Console | E8 | PPI |
| menu | `tcatlas:signpost` | 路标 | Signpost | E2 | 编辑指向牌 |
| menu | `tcatlas:stereo_plotter` | 立体测图仪 | Stereo Plotter | E8 | 测图 |
| menu | `tcatlas:theodolite` | 经纬仪 | Theodolite | E6 | 取景与观测 |
| keybind | `tcatlas:add_waypoint` | 添加路点 | Add Waypoint | — | key.tcatlas.add_waypoint，默认 B |
| keybind | `tcatlas:add_waypoint_quick` | 快速添加路点 | Quick Add Waypoint | — | key.tcatlas.add_waypoint_quick，默认 Shift+B，跳过命名对话框 |
| keybind | `tcatlas:hud_level` | 切换 HUD 级别 | Cycle HUD Level | — | key.tcatlas.hud_level，默认 H；安装 Atlas 时核心不注册同名键 |
| keybind | `tcatlas:layout_editor` | HUD 布局编辑器 | HUD Layout Editor | — | key.tcatlas.layout_editor，默认 Ctrl+H |
| keybind | `tcatlas:minimap_mode` | 小地图模式 | Minimap Mode | — | key.tcatlas.minimap_mode，默认 N，关 → 北向上 → 旋转 → 关 |
| keybind | `tcatlas:minimap_zoom_in` | 小地图放大 | Minimap Zoom In | — | key.tcatlas.minimap_zoom_in，默认未绑定 |
| keybind | `tcatlas:minimap_zoom_out` | 小地图缩小 | Minimap Zoom Out | — | key.tcatlas.minimap_zoom_out，默认未绑定 |
| keybind | `tcatlas:ping` | 临时标记 | Ping | — | key.tcatlas.ping，默认未绑定 |
| keybind | `tcatlas:world_map` | 世界地图 | World Map | — | key.tcatlas.world_map，默认 M，单人不暂停 |
| payload | `tcatlas:annotation_edit` | 注释编辑 | Annotation Edit | — | C→S |
| payload | `tcatlas:annotation_sync` | 注释同步 | Annotation Sync | — | S→C |
| payload | `tcatlas:capability_sync` | 能力同步 | Capability Sync | — | S→C |
| payload | `tcatlas:knowledge_mask_sync` | 知识掩码同步 | Knowledge Mask Sync | — | S→C |
| payload | `tcatlas:layer_data_request` | 图层数据请求 | Layer Data Request | — | C→S |
| payload | `tcatlas:layer_data_sync` | 图层数据同步 | Layer Data Sync | — | S→C |
| payload | `tcatlas:live_track_sync` | 实时目标同步 | Live Track Sync | — | S→C，1 Hz |
| payload | `tcatlas:map_sheet_sync` | 图纸同步 | Map Sheet Sync | — | S→C |
| payload | `tcatlas:map_tile_request` | 瓦片请求 | Map Tile Request | — | C→S |
| payload | `tcatlas:map_tile_sync` | 瓦片同步 | Map Tile Sync | — | S→C，分片 32 KB |
| payload | `tcatlas:memory_sync` | 记忆同步 | Memory Sync | — | S→C |
| payload | `tcatlas:nav_state_sync` | 导航状态同步 | Navigation State Sync | — | S→C，4 Hz |
| payload | `tcatlas:ping` | 临时标记 | Ping | — | 双向 |
| payload | `tcatlas:survey_action` | 测量操作 | Survey Action | — | C→S |
| payload | `tcatlas:survey_result` | 测量结果 | Survey Result | — | S→C |
| payload | `tcatlas:waypoint_edit` | 路点编辑 | Waypoint Edit | — | C→S |
| payload | `tcatlas:waypoint_sync` | 路点同步 | Waypoint Sync | — | S→C |
| hud_widget | `tcatlas:compass_bar` | 罗盘条 | Compass Bar | E4 | STANDARD，上中，240×14 |
| hud_widget | `tcatlas:coordinates` | 坐标 | Coordinates | E10 | EXPANDED，需 GPS 或 F3，96×10 |
| hud_widget | `tcatlas:minimap` | 小地图 | Minimap | E3 | STANDARD，左上，96×96 |
| hud_widget | `tcatlas:waypoint_nav` | 路点导航 | Waypoint Navigation | E1 | MINIMAL，导航时显示，160×12 |
| civ_tab | `tcatlas:cartography` | 测绘 | Cartography | E1 | WORLD 组，序号 610，civtab.tcatlas.cartography |
| region_layer | `tcatlas:survey` | 测绘知识层 | Survey Knowledge | — | 每区块 SL/H/ε/日期，ON_DEMAND |
| sound | `tcatlas:block.radar.sweep` | 雷达扫描 | Radar Sweep | E8 |  |
| sound | `tcatlas:block.theodolite.adjust` | 经纬仪调节 | Theodolite Adjust | E6 |  |
| sound | `tcatlas:item.camera.shutter` | 相机快门 | Camera Shutter | E8 |  |
| sound | `tcatlas:item.compass.needle` | 罗盘摆针 | Compass Needle | E4 |  |
| sound | `tcatlas:item.gps.fix` | GPS 定位 | GPS Fix | E10 |  |
| sound | `tcatlas:ui.layout.snap` | 布局吸附 | Layout Snap | — | 编辑器吸附提示 |
| sound | `tcatlas:ui.map.close` | 关闭地图 | Map Close | — |  |
| sound | `tcatlas:ui.map.open` | 打开地图 | Map Open | — | 纸张展开，随皮肤变化 |
| sound | `tcatlas:ui.waypoint.add` | 添加路点 | Waypoint Add | — |  |

### 5.17 列国 `tcnations`（51 项，来源 [23-nations.md](23-nations.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcnations:ai_settlement_core` | 异邦聚落核心 | Foreign Settlement Core | E0 | AI 聚落锚点，不可合成 |
| block | `tcnations:border_stone` | 界碑 | Border Stone | E2 | 条约边界标记 |
| block | `tcnations:embassy_desk` | 使馆公案 | Embassy Desk | E4 | 设立使馆 |
| block | `tcnations:seat_of_power` | 权力宝座 | Seat of Power | E2 | 首都外交中心 |
| block | `tcnations:treaty_lectern` | 条约台 | Treaty Lectern | E2 | 起草与签署条约 |
| block | `tcnations:tribute_chest` | 贡箱 | Tribute Chest | E2 | 贡金与赔款交接 |
| block | `tcnations:war_table` | 军议桌 | War Table | E3 | 军队编组与命令 |
| item | `tcnations:envoy_seal` | 使节印信 | Envoy Seal | E2 | 授予 EnvoyComponent |
| item | `tcnations:gift_bundle` | 礼物包 | Gift Bundle | E1 | 外交礼物容器 |
| item | `tcnations:regiment_banner` | 团旗 | Regiment Banner | E3 | 团编组与集结点 |
| item | `tcnations:treaty_scroll` | 条约卷轴 | Treaty Scroll | E2 | 草案/已签条约 |
| item | `tcnations:war_declaration` | 宣战书 | Declaration of War | E2 | 由军议桌生成 |
| item | `tcnations:white_flag` | 白旗 | White Flag | E2 | 实体战斗停火谈判 |
| structure | `tcnations:ai_camp` | 异邦营地 | Foreign Camp | E0 | 10-30 人 |
| structure | `tcnations:ai_castle` | 异邦城堡 | Foreign Castle | E4 | 封建据点 |
| structure | `tcnations:ai_city` | 异邦城市 | Foreign City | E5 | 800-3,000 人 |
| structure | `tcnations:ai_colony` | 异邦殖民地 | Foreign Colony | E5 | 海外据点 |
| structure | `tcnations:ai_frontier_fort` | 异邦边堡 | Foreign Frontier Fort | E3 | 边境驻军 |
| structure | `tcnations:ai_metropolis` | 异邦都会 | Foreign Metropolis | E7 | 中心 3×3 区块实体化 |
| structure | `tcnations:ai_town` | 异邦城镇 | Foreign Town | E3 | 200-800 人 |
| structure | `tcnations:ai_village` | 异邦村落 | Foreign Village | E1 | 30-200 人 |
| structure | `tcnations:embassy_compound` | 使馆院落 | Embassy Compound | E4 | 9×9 使馆地块 |
| block_entity | `tcnations:ai_settlement_core` | 异邦聚落核心 | Foreign Settlement Core | E0 | 聚落 ID 与文明 ID |
| block_entity | `tcnations:embassy_desk` | 使馆公案 | Embassy Desk | E4 | 绑定驻在国与大使 |
| block_entity | `tcnations:treaty_lectern` | 条约台 | Treaty Lectern | E2 | 保存草案队列 |
| block_entity | `tcnations:tribute_chest` | 贡箱 | Tribute Chest | E2 | 应付/应收清单 |
| block_entity | `tcnations:war_table` | 军议桌 | War Table | E3 | 绑定战争与军队 |
| menu | `tcnations:embassy` | 使馆菜单 | Embassy Menu | E4 | 驻在国情报 |
| menu | `tcnations:treaty_lectern` | 条约台菜单 | Treaty Lectern Menu | E2 | 草案列表与签署 |
| menu | `tcnations:tribute_chest` | 贡箱菜单 | Tribute Chest Menu | E2 | 支付与收取 |
| menu | `tcnations:war_table` | 军议桌菜单 | War Table Menu | E3 | 战区地图与命令 |
| keybind | `tcnations:diplomacy` | 打开外交 | Open Diplomacy | — | key.tcnations.diplomacy，默认未绑定，打开 J 面板外交页签 |
| payload | `tcnations:army_order` | 军队命令 | Army Order | — | C→S |
| payload | `tcnations:army_snapshot` | 军队快照 | Army Snapshot | — | S→C |
| payload | `tcnations:border_overlay` | 国界叠层 | Border Overlay | — | S→C |
| payload | `tcnations:diplomacy_snapshot` | 外交快照 | Diplomacy Snapshot | — | S→C |
| payload | `tcnations:proposal_response` | 提案答复 | Proposal Response | — | S→C |
| payload | `tcnations:proposal_submit` | 提交提案 | Proposal Submit | — | C→S |
| payload | `tcnations:relation_update` | 关系更新 | Relation Update | — | S→C |
| payload | `tcnations:treaty_sync` | 条约同步 | Treaty Sync | — | S→C |
| payload | `tcnations:vote_cast` | 投票 | Vote Cast | — | C→S |
| payload | `tcnations:war_state_sync` | 战争状态同步 | War State Sync | — | S→C |
| hud_widget | `tcnations:territory_entry` | 进入国境 | Territory Entry | E0 | 极简；跨越国界后 3 秒；语言键 hud.tcnations.territory_entry |
| hud_widget | `tcnations:war_status` | 战争状态 | War Status | E2 | 标准；处于战争时显示；语言键 hud.tcnations.war_status |
| civ_tab | `tcnations:diplomacy` | 外交 | Diplomacy | E0 | CIV 组 order 300；语言键 civtab.tcnations.diplomacy |
| region_layer | `tcnations:influence` | 影响力 | Influence | — | 每地区前 3 个文明的影响力 |
| sound | `tcnations:event.envoy_horn` | 使节抵达 | Envoy Horn | E2 | — |
| sound | `tcnations:event.first_contact` | 首次接触 | First Contact | E0 | 号角 |
| sound | `tcnations:event.peace_signed` | 和约 | Peace Signed | E2 | 钟声 |
| sound | `tcnations:event.treaty_seal` | 盖印 | Treaty Seal | E2 | — |
| sound | `tcnations:event.war_declared` | 宣战 | War Declared | E2 | 按时代变体 |

### 5.18 寰宇 `tcorbit`（188 项，来源 [24-orbit.md](24-orbit.md)）

| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |
| --- | --- | --- | --- | --- | --- |
| block | `tcorbit:airlock_controller` | 气闸控制器 | Airlock Controller | E10 | — |
| block | `tcorbit:airlock_module` | 气闸舱 | Airlock Module | E10 | — |
| block | `tcorbit:anorthosite` | 斜长岩 | Anorthosite | E10 | — |
| block | `tcorbit:assembly_controller` | 总装厂房控制器 | Assembly Controller | E9 | — |
| block | `tcorbit:assembly_crane` | 总装吊车 | Assembly Crane | E9 | — |
| block | `tcorbit:asteroid_metal` | 小行星金属 | Asteroid Metal | E10 | — |
| block | `tcorbit:asteroid_rock` | 小行星岩 | Asteroid Rock | E10 | — |
| block | `tcorbit:avionics_unit` | 航电单元 | Avionics Unit | E8 | — |
| block | `tcorbit:beacon_payload` | 信标载荷 | Beacon Payload | E9 | — |
| block | `tcorbit:capsule_hatch` | 返回舱舱门 | Capsule Hatch | E10 | — |
| block | `tcorbit:capsule_hull` | 返回舱壳体 | Capsule Hull | E10 | — |
| block | `tcorbit:carbonaceous_rock` | 碳质岩 | Carbonaceous Rock | E10 | — |
| block | `tcorbit:co2_collector` | 二氧化碳收集器 | CO2 Collector | E10 | — |
| block | `tcorbit:co2_frost` | 二氧化碳霜 | CO2 Frost | E10 | — |
| block | `tcorbit:co2_scrubber` | 二氧化碳洗涤器 | CO2 Scrubber | E10 | — |
| block | `tcorbit:colony_beacon` | 殖民地信标 | Colony Beacon | E10 | — |
| block | `tcorbit:comm_payload` | 通信载荷 | Comm Payload | E9 | — |
| block | `tcorbit:core_module` | 核心舱 | Core Module | E10 | — |
| block | `tcorbit:crawlerway` | 履带运输道 | Crawlerway | E9 | — |
| block | `tcorbit:crew_berth` | 乘员卧铺 | Crew Berth | E10 | Settlements 缺失时可合成 |
| block | `tcorbit:crew_seat` | 乘员座椅 | Crew Seat | E10 | — |
| block | `tcorbit:cryogenic_tank` | 低温贮箱 | Cryogenic Tank | E9 | — |
| block | `tcorbit:debris_collector_payload` | 碎片收集载荷 | Debris Collector Payload | E10 | — |
| block | `tcorbit:deep_space_antenna` | 深空天线 | Deep Space Antenna | E10 | — |
| block | `tcorbit:docking_port` | 对接口 | Docking Port | E10 | — |
| block | `tcorbit:dry_ice` | 干冰 | Dry Ice | E10 | — |
| block | `tcorbit:earth_observation_payload` | 对地观测载荷 | Earth Observation Payload | E9 | — |
| block | `tcorbit:flame_trench` | 导流槽 | Flame Trench | E9 | — |
| block | `tcorbit:greenhouse_module` | 温室舱 | Greenhouse Module | E10 | — |
| block | `tcorbit:grid_fin` | 栅格舵 | Grid Fin | E10 | — |
| block | `tcorbit:habitat_module_frame` | 居住舱框架 | Habitat Module Frame | E10 | — |
| block | `tcorbit:heat_shield` | 防热罩 | Heat Shield | E9 | — |
| block | `tcorbit:helium3_extractor` | 氦-3 提取器 | Helium-3 Extractor | E10 | — |
| block | `tcorbit:hematite_regolith` | 赤铁矿火星土 | Hematite Regolith | E10 | — |
| block | `tcorbit:hull_plating` | 舱体壁板 | Hull Plating | E10 | — |
| block | `tcorbit:hydrolox_engine` | 氢氧主发动机 | Hydrolox Engine | E9 | — |
| block | `tcorbit:hydrolox_upper_engine` | 氢氧上面级发动机 | Hydrolox Upper Engine | E9 | — |
| block | `tcorbit:hypergolic_engine` | 自燃推进剂发动机 | Hypergolic Engine | E9 | — |
| block | `tcorbit:ice_drill` | 冰钻 | Ice Drill | E10 | — |
| block | `tcorbit:ilmenite_regolith` | 钛铁矿月壤 | Ilmenite Regolith | E10 | — |
| block | `tcorbit:ion_thruster` | 离子推力器 | Ion Thruster | E10 | — |
| block | `tcorbit:kerolox_engine` | 煤油液氧主发动机 | Kerolox Engine | E9 | — |
| block | `tcorbit:kerolox_vacuum_engine` | 真空煤油发动机 | Kerolox Vacuum Engine | E9 | — |
| block | `tcorbit:kreep_basalt` | 克里普玄武岩 | KREEP Basalt | E10 | — |
| block | `tcorbit:lab_module` | 实验舱 | Lab Module | E10 | — |
| block | `tcorbit:lander_base` | 着陆器底座 | Lander Base | E10 | — |
| block | `tcorbit:landing_barge` | 回收船平台 | Landing Barge | E10 | — |
| block | `tcorbit:landing_leg` | 着陆腿 | Landing Leg | E10 | — |
| block | `tcorbit:launch_escape_tower` | 逃逸塔 | Launch Escape Tower | E10 | — |
| block | `tcorbit:launch_pad_controller` | 发射台控制器 | Launch Pad Controller | E9 | — |
| block | `tcorbit:launch_pad_deck` | 发射台面 | Launch Pad Deck | E9 | — |
| block | `tcorbit:life_support_unit` | 生命维持单元 | Life Support Unit | E10 | — |
| block | `tcorbit:lunar_basalt` | 月球玄武岩 | Lunar Basalt | E10 | — |
| block | `tcorbit:lunar_ice` | 月球冰 | Lunar Ice | E10 | — |
| block | `tcorbit:mare_regolith` | 月海月壤 | Mare Regolith | E10 | — |
| block | `tcorbit:mars_basalt` | 火星玄武岩 | Martian Basalt | E10 | — |
| block | `tcorbit:mars_clay` | 火星黏土 | Martian Clay | E10 | — |
| block | `tcorbit:mars_ice` | 火星冰 | Martian Ice | E10 | — |
| block | `tcorbit:mars_regolith` | 火星土 | Martian Regolith | E10 | — |
| block | `tcorbit:methalox_engine` | 甲烷液氧主发动机 | Methalox Engine | E10 | — |
| block | `tcorbit:methalox_vacuum_engine` | 真空甲烷发动机 | Methalox Vacuum Engine | E10 | — |
| block | `tcorbit:mission_control_console` | 任务控制台 | Mission Control Console | E9 | — |
| block | `tcorbit:mobile_launch_platform` | 移动发射平台 | Mobile Launch Platform | E9 | — |
| block | `tcorbit:molten_regolith_electrolyzer` | 熔融月壤电解槽 | Molten Regolith Electrolyzer | E10 | — |
| block | `tcorbit:node_module` | 节点舱 | Node Module | E10 | — |
| block | `tcorbit:nose_cone` | 整流头锥 | Nose Cone | E8 | — |
| block | `tcorbit:nuclear_thermal_engine` | 核热发动机 | Nuclear Thermal Engine | E10 | — |
| block | `tcorbit:orbital_dock` | 轨道船坞 | Orbital Dock | E10 | — |
| block | `tcorbit:outpost_core` | 前哨核心 | Outpost Core | E10 | — |
| block | `tcorbit:oxygen_generator` | 制氧机 | Oxygen Generator | E10 | — |
| block | `tcorbit:oxygen_tank` | 氧气罐 | Oxygen Tank | E10 | — |
| block | `tcorbit:parachute_pack` | 降落伞包 | Parachute Pack | E9 | — |
| block | `tcorbit:payload_fairing` | 整流罩 | Payload Fairing | E9 | — |
| block | `tcorbit:perchlorate_washer` | 高氯酸盐清洗机 | Perchlorate Washer | E10 | — |
| block | `tcorbit:pressure_hatch` | 耐压舱门 | Pressure Hatch | E10 | — |
| block | `tcorbit:pressure_window` | 耐压舷窗 | Pressure Window | E10 | — |
| block | `tcorbit:propellant_depot` | 推进剂补给站 | Propellant Depot | E10 | — |
| block | `tcorbit:propellant_liquefier` | 推进剂液化器 | Propellant Liquefier | E10 | — |
| block | `tcorbit:propellant_loading_arm` | 加注臂 | Propellant Loading Arm | E9 | — |
| block | `tcorbit:propellant_tank` | 推进剂贮箱 | Propellant Tank | E8 | — |
| block | `tcorbit:radial_decoupler` | 径向分离器 | Radial Decoupler | E9 | — |
| block | `tcorbit:radiator_panel` | 散热板 | Radiator Panel | E10 | — |
| block | `tcorbit:rcs_thruster` | 姿控推力器 | RCS Thruster | E9 | — |
| block | `tcorbit:regolith` | 月壤 | Regolith | E10 | — |
| block | `tcorbit:regolith_excavator` | 月壤挖掘机 | Regolith Excavator | E10 | — |
| block | `tcorbit:regolith_reducer` | 月壤氢还原炉 | Regolith Reducer | E10 | — |
| block | `tcorbit:regolith_sinterer` | 月壤烧结机 | Regolith Sinterer | E10 | — |
| block | `tcorbit:relay_payload` | 中继载荷 | Relay Payload | E10 | — |
| block | `tcorbit:rocket_engine` | 早期液体发动机 | Rocket Engine | E8 | — |
| block | `tcorbit:sabatier_reactor` | 萨巴蒂尔反应器 | Sabatier Reactor | E10 | — |
| block | `tcorbit:satellite_bus` | 卫星平台 | Satellite Bus | E9 | — |
| block | `tcorbit:science_payload` | 科学载荷 | Science Payload | E9 | — |
| block | `tcorbit:scorched_pad_deck` | 烧蚀发射台面 | Scorched Pad Deck | E9 | 需修复 |
| block | `tcorbit:sealant_coating` | 密封涂层 | Sealant Coating | E10 | — |
| block | `tcorbit:sintered_regolith_brick` | 烧结月壤砖 | Sintered Regolith Brick | E10 | — |
| block | `tcorbit:solar_array` | 太阳翼 | Solar Array | E9 | — |
| block | `tcorbit:solar_monitor_payload` | 太阳监测载荷 | Solar Monitor Payload | E9 | — |
| block | `tcorbit:solid_motor_nozzle` | 固体发动机喷管 | Solid Motor Nozzle | E8 | — |
| block | `tcorbit:solid_motor_segment` | 固体发动机段 | Solid Motor Segment | E8 | — |
| block | `tcorbit:solid_oxide_electrolyzer` | 固体氧化物电解器 | Solid Oxide Electrolyzer | E10 | — |
| block | `tcorbit:sound_suppression_tower` | 喷水降噪塔 | Sound Suppression Tower | E9 | — |
| block | `tcorbit:sounding_rail` | 探空架 | Sounding Rail | E8 | — |
| block | `tcorbit:stage_separator` | 级间分离器 | Stage Separator | E8 | — |
| block | `tcorbit:station_arm` | 空间站机械臂 | Station Arm | E10 | — |
| block | `tcorbit:storm_shelter` | 辐射掩体 | Storm Shelter | E10 | — |
| block | `tcorbit:suit_port` | 套装接口 | Suit Port | E10 | — |
| block | `tcorbit:tracking_antenna` | 跟踪天线 | Tracking Antenna | E9 | — |
| block | `tcorbit:truss_segment` | 桁架段 | Truss Segment | E10 | — |
| block | `tcorbit:umbilical_tower` | 脐带塔 | Umbilical Tower | E9 | — |
| block | `tcorbit:washed_mars_soil` | 洗净火星土 | Washed Martian Soil | E10 | — |
| block | `tcorbit:water_recycler` | 水回收器 | Water Recycler | E10 | — |
| block | `tcorbit:water_tank` | 水箱 | Water Tank | E10 | — |
| block | `tcorbit:whipple_shield` | 惠普尔防护板 | Whipple Shield | E10 | — |
| item | `tcorbit:asteroid_sample` | 小行星样本 | Asteroid Sample | E10 | — |
| item | `tcorbit:engine_refurbish_kit` | 发动机翻修套件 | Engine Refurbish Kit | E10 | — |
| item | `tcorbit:eva_suit` | 舱外航天服 | EVA Suit | E10 | — |
| item | `tcorbit:helium3_canister` | 氦-3 罐 | Helium-3 Canister | E10 | — |
| item | `tcorbit:lunar_metal_slag` | 月壤金属渣 | Lunar Metal Slag | E10 | — |
| item | `tcorbit:lunar_sample` | 月球样本 | Lunar Sample | E10 | — |
| item | `tcorbit:mars_sample` | 火星样本 | Martian Sample | E10 | — |
| item | `tcorbit:nickel_iron_chunk` | 镍铁块 | Nickel-Iron Chunk | E10 | — |
| item | `tcorbit:pad_repair_kit` | 发射台修复包 | Pad Repair Kit | E9 | — |
| item | `tcorbit:pgm_concentrate` | 铂族金属精矿 | PGM Concentrate | E10 | — |
| item | `tcorbit:repair_kit` | 维修包 | Repair Kit | E9 | — |
| item | `tcorbit:rocket_blueprint` | 火箭蓝图 | Rocket Blueprint | E9 | — |
| item | `tcorbit:space_ration` | 航天口粮 | Space Ration | E10 | — |
| entity | `tcorbit:crawler_transporter` | 履带运输车 | Crawler Transporter | E9 | — |
| entity | `tcorbit:lunar_rover` | 月面车 | Lunar Rover | E10 | — |
| entity | `tcorbit:pressurized_rover` | 加压车 | Pressurized Rover | E10 | — |
| entity | `tcorbit:rocket` | 火箭 | Rocket | E8 | 方块快照 |
| entity | `tcorbit:spacecraft` | 航天器 | Spacecraft | E10 | 返回舱/着陆器 |
| entity | `tcorbit:spent_stage` | 废弃级 | Spent Stage | E8 | — |
| fluid | `tcorbit:carbon_dioxide` | 二氧化碳 | Carbon Dioxide | E10 | Industry 无同类时注册 |
| fluid | `tcorbit:helium_3` | 氦-3 | Helium-3 | E10 | — |
| dimension | `tcorbit:low_orbit` | 近地轨道 | Low Orbit | E9 | 不环绕 |
| dimension | `tcorbit:mars` | 火星 | Mars | E10 | X 向环绕 |
| dimension | `tcorbit:moon` | 月球 | Moon | E10 | X 向环绕 |
| biome | `tcorbit:lunar_crater` | 环形山 | Lunar Crater | E10 | — |
| biome | `tcorbit:lunar_highlands` | 月陆高地 | Lunar Highlands | E10 | — |
| biome | `tcorbit:lunar_mare` | 月海 | Lunar Mare | E10 | — |
| biome | `tcorbit:lunar_polar` | 月球极区 | Lunar Polar | E10 | 永久阴影冰 |
| biome | `tcorbit:mars_canyon` | 火星峡谷 | Martian Canyon | E10 | — |
| biome | `tcorbit:mars_highlands` | 火星高地 | Martian Highlands | E10 | — |
| biome | `tcorbit:mars_lowlands` | 火星低地 | Martian Lowlands | E10 | — |
| biome | `tcorbit:mars_polar` | 火星极冠 | Martian Polar Cap | E10 | — |
| biome | `tcorbit:mars_volcano` | 火星火山 | Martian Volcano | E10 | — |
| biome | `tcorbit:orbit_void` | 轨道虚空 | Orbit Void | E9 | — |
| block_entity | `tcorbit:airlock_controller` | 气闸控制器 | Airlock Controller | E10 | — |
| block_entity | `tcorbit:assembly_controller` | 总装厂房控制器 | Assembly Controller | E9 | — |
| block_entity | `tcorbit:colony_beacon` | 殖民地信标 | Colony Beacon | E10 | — |
| block_entity | `tcorbit:isru_machine` | 原位资源机器 | ISRU Machine | E10 | ISRU 方块共用 |
| block_entity | `tcorbit:launch_pad_controller` | 发射台控制器 | Launch Pad Controller | E9 | — |
| block_entity | `tcorbit:life_support_unit` | 生命维持单元 | Life Support Unit | E10 | — |
| block_entity | `tcorbit:mission_control_console` | 任务控制台 | Mission Control Console | E9 | — |
| block_entity | `tcorbit:outpost_core` | 前哨核心 | Outpost Core | E10 | — |
| menu | `tcorbit:airlock` | 气闸 | Airlock | E10 | — |
| menu | `tcorbit:colony_beacon` | 殖民地信标 | Colony Beacon | E10 | — |
| menu | `tcorbit:isru_machine` | 原位资源机器 | ISRU Machine | E10 | — |
| menu | `tcorbit:launch_pad` | 发射台 | Launch Pad | E9 | — |
| menu | `tcorbit:life_support` | 生命维持 | Life Support | E10 | — |
| menu | `tcorbit:mission_control` | 任务控制 | Mission Control | E9 | — |
| menu | `tcorbit:propellant_depot` | 推进剂补给站 | Propellant Depot | E10 | — |
| menu | `tcorbit:rocket_assembly` | 火箭装配 | Rocket Assembly | E9 | — |
| menu | `tcorbit:suit_port` | 套装接口 | Suit Port | E10 | — |
| keybind | `tcorbit:cruise_confirm` | 确认巡航 | Confirm Cruise | E9 | key.tcorbit.cruise_confirm，默认未绑定（轮盘） |
| keybind | `tcorbit:toggle_rcs_mode` | 切换 RCS 模式 | Toggle RCS Mode | E10 | key.tcorbit.toggle_rcs_mode，默认未绑定（驾驶时疾跑键） |
| keybind | `tcorbit:vehicle_headlights` | 车灯 | Vehicle Headlights | E10 | key.tcorbit.vehicle_headlights，默认未绑定（默认自动） |
| payload | `tcorbit:assembly_action` | 装配操作 | Assembly Action | — | C→S |
| payload | `tcorbit:assembly_state` | 装配状态 | Assembly State | — | S→C |
| payload | `tcorbit:flight_telemetry` | 飞行遥测 | Flight Telemetry | — | S→C |
| payload | `tcorbit:launch_command` | 发射指令 | Launch Command | — | C→S |
| payload | `tcorbit:life_support_hud` | 生命维持 HUD | Life Support HUD | — | S→C |
| payload | `tcorbit:mission_plan` | 任务规划 | Mission Plan | — | C→S |
| payload | `tcorbit:mission_sync` | 任务同步 | Mission Sync | — | S→C |
| payload | `tcorbit:orbit_view` | 轨道视图 | Orbit View | — | S→C |
| payload | `tcorbit:rcs_input` | RCS 输入 | RCS Input | — | C→S |
| payload | `tcorbit:sky_snapshot` | 天空快照 | Sky Snapshot | — | S→C |
| payload | `tcorbit:wrap_preload` | 环绕预加载 | Wrap Preload | — | S→C |
| hud_widget | `tcorbit:flight_telemetry` | 飞行遥测 | Flight Telemetry | E9 | 标准，下中，与 vehicle_status 互斥；31 §3.3.2 |
| hud_widget | `tcorbit:life_support` | 生命维持 | Life Support | E10 | 标准，左中 |
| civ_tab | `tcorbit:space_program` | 太空计划 | Space Program | E8 | 世界分组，排序 800 |
| region_layer | `tcorbit:remote_sensing` | 遥感 | Remote Sensing | E9 | — |
| sound | `tcorbit:item.eva_suit.warning` | 套装警告 | Suit Warning | E10 | — |
| sound | `tcorbit:vehicle.docking.lock` | 对接锁定 | Docking Lock | E10 | — |
| sound | `tcorbit:vehicle.reentry.plasma` | 再入等离子 | Reentry Plasma | E9 | — |
| sound | `tcorbit:vehicle.rocket.countdown` | 倒计时 | Countdown Beep | E9 | — |
| sound | `tcorbit:vehicle.rocket.ignition` | 点火 | Launch Ignition | E8 | — |
| sound | `tcorbit:vehicle.rocket.stage_separation` | 级间分离 | Stage Separation | E8 | — |

## 6. 注册别名

v0.3 起，注册 ID 改名必须在此登记（[40 §9.2](40-roadmap.md)）。别名由各模组章节的附录 A 说明列标注 `alias-of:<新 ID>` 后自动收录。

| 旧 ID | 新 ID | 版本 | 来源 |
| --- | --- | --- | --- |
| — | — | — | 暂无 |

## 7. 校验问题明细

| 级别 | 代码 | 位置 | 说明 |
| --- | --- | --- | --- |
| — | — | — | 无 |

