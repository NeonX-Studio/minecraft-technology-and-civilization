# 30 · 原版改动总表（Vanilla Overrides）

> 本章是**横切章节**，不使用 README §5.3 的 18 节模组模板。它登记整个模组家族对 Minecraft Java 26.3 原版内容的**所有**改动：世界生成、结构、生物生成、袭击、村民、配方、战利品、战斗、天气、火焰、界面、物品用途、附魔酿造与游戏规则。每一项都写明拥有者模组、实现方式、配置开关与拥有者缺失时的回退。
>
> 冲突裁决顺序：`design-brief.md` 契约 → 00 章 DR/DA 决议 → **本章** → 各模组章节 §16。模组章节 §16 只描述“本模组做了什么”；**哪个模组拥有某个原版系统、两个模组同时改动时谁优先、拥有者缺失时怎么办**，以本章为准。不一致时在 40 章问题清单中登记并按本章修正模组章节。

---

## 0. 文档信息

| 项 | 内容 |
| --- | --- |
| 章节编号 | 30 |
| 适用模组 | 全部 18 个；原版策略服务 `VanillaPolicyService` 与怪物回退门由 `tcivilization` 提供，配方门槛框架 `tc_recipe_gate` 由 `tctechnology` 提供 |
| 依赖章节 | 00（DR-08 主世界替换、DR-15 原版工具配方、DR-16 下界/末地、DR-17 村民、DR-18 威胁、DR-19 无个人生存、DA-08、DA-09）、02（数据驱动原则、存档布局）、03 §13（`vanilla.monster_policy`）、04 §16（配方门槛）、10、11、12、13、14、15、17、20、21、22、23 各章 §16 |
| 下游章节 | 34 联动矩阵（原版系统列）、36 注册表索引（游戏规则、数据包覆写条目）、40 路线图 EC-P1-7 |
| 版本 | 设计 v1.0（对应代码 v0.2 起逐步落地） |
| 状态 | 规范稿；标记“待 NN 确认”的行来自尚在编写中的章节，等该章 §16 完成后核对（§10.2） |

### 0.1 术语

| 术语 | 含义 |
| --- | --- |
| 星球模式 | `tcwildlands` 已安装、`replace_overworld = true` 且主世界生成器为 `tcwildlands:planet`（`TcPlanetChunkGenerator`）。只要选择了原版世界预设或关闭替换，就**不是**星球模式 |
| 深地模式 | `tcwildlands` 已安装且 `replace_nether = true`：`minecraft:the_nether` 使用深地维度类型与生成器 |
| 界阈模式 | `tcbeyond` 已安装且 `end.rework = true`：`minecraft:the_end` 重做为界阈 |
| 拥有者 | 对某个原版系统有最终决定权的模组；同一系统只有一个拥有者 |
| 回退 | 拥有者缺失时由其他模组（或不处理）提供的替代行为 |
| 首次加载写入 | 只在世界第一次以该模组加载时写入游戏规则，此后不再覆盖（§4.3） |
| 怪物稀缺 | 主世界原版敌对生物只在遗迹或影响区出现的状态（§3.4.1 的有效策略为 `BEYOND` 或 `RUINS_ONLY`） |

---

## 1. 原则

| 编号 | 原则 | 落地规则 |
| --- | --- | --- |
| VP-1 | 单一拥有者 | 每个原版系统在 §3 中只有一个拥有者；其他模组只能通过拥有者的 API 或数据标签“附加条件”，不得直接改同一系统 |
| VP-2 | 可关闭、可回滚 | 每项改动都有配置开关或数据包入口；卸载拥有者模组后，原版行为恢复（游戏规则除外，见 §4.3 与 `/tc vanilla restore`） |
| VP-3 | 最轻机制优先 | 按 §2 的优先级选择实现方式：数据包 → 标签 → 事件 → 首次加载游戏规则 → mixin。mixin 只允许出现在 §2.2 的白名单中 |
| VP-4 | 单独安装可玩 | 每个模组单独安装时，只改动“自己的玩法确实需要”的原版内容；联动才出现的改动（例如怪物稀缺）只在条件成立时生效 |
| VP-5 | 写实主世界 | 主世界的奇幻原版内容（下界传送门、灾厄村民、末影人、末地要塞）只在 Beyond 的影响区或异界维度出现（DR-06、DR-16、DR-18） |
| VP-6 | 不增加个人生存负担 | 不新增体温、口渴、疾病状态条；原版饥饿与自然回血保留（DR-19、DA-12） |
| VP-7 | 原版物品 ID 复用 | 原版已有的产物（小麦、铁锭、纸、红石）复用原版 ID；只改获得途径，不重复注册（02 §3） |
| VP-8 | 改动可见 | 被门控的原版配方在配方书与 JEI/EMI 中显示锁定原因；被移除的生成与结构在 `/tc vanilla status` 中可查 |

---

## 2. 实现机制目录

### 2.1 允许的机制与优先级

| 优先级 | 机制 | 典型用途 | 可被整合包覆盖 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | 数据包覆写（`data/minecraft/...`） | 结构集频率、维度定义、配方移除 | 是 | 全部带 `neoforge:conditions`，条件为“拥有者模组已加载 + 配置开关为真” |
| 2 | 数据标签 | 群系加入 `#minecraft:has_structure/*`、`c:` 标签扩展、生成豁免标签 | 是 | 标签是跨模组的首选接口 |
| 3 | NeoForge 生物群系修改器 / 结构修改器 | 移除或补全生成条目 | 是 | `tcecology:remove_vanilla_wildlife`、`tcbeyond:monster_spawns` |
| 4 | 全局战利品修改器 | 附加掉落 | 是 | 只附加，不删除原版掉落（§4.6） |
| 5 | NeoForge 事件 | 生成过滤、实体替换、配方书过滤 | 否（由配置开关控制） | `LevelEvent.PotentialSpawns`、`FinalizeSpawnEvent`、`EntityJoinLevelEvent`、`PlayerSpawnPhantomsEvent` |
| 6 | 首次加载写入游戏规则 | 袭击、巡逻、定位栏、流浪商人 | 是（管理员可再改） | §4.3 与 §6 |
| 7 | mixin | 原版没有事件钩子的核心逻辑 | 否 | 只限 §2.2 白名单；全部 `require = 1` 且带版本断言，失败时模组拒绝加载并给出明确报错 |

### 2.2 mixin 白名单

新增 mixin 必须先登记到本表，并在 40 章问题清单中说明为什么事件与数据包不够用。

| 编号 | 模组 | 目标 | 端 | 用途 | 出处 |
| --- | --- | --- | --- | --- | --- |
| MX-01 | tcwildlands | `Level#isRainingAt`、`Biome#getPrecipitationAt`、`Biome#shouldFreeze`、`Biome#shouldSnow` | 两端 | 局地天气格代替全局雨雪；星球主世界的积雪与结冰交给自有降水刻 | 10 §5.13.3、§16.2 |
| MX-02 | tcwildlands | `FireBlock` 蔓延逻辑 | 服务端 | 山火模型（Rothermel；读取 `minecraft:fire_spread_radius_around_player`） | 10 §5.14.6、§16.2 |
| MX-03 | tcwildlands | `BaseFireBlock#onPlace` 中的 `PortalShape.findEmptyPortalShape` | 服务端 | 深地模式下禁用下界传送门点火。**默认不应用**：`BlockEvent.PortalSpawnEvent` 存在时 VO-WG-10 由该事件实现，本 mixin 只作事件被移除时的后备 | 10 §5.16.2、§16.2 |
| MX-04 | tcwildlands | `Level#getTimeOfDay(float)`（RETURN 改写返回值，只在客户端星球主世界生效） | 客户端 | 日长随纬度与季节变化；天空颜色仍由 `DimensionSpecialEffects` 负责，天体角本身没有事件钩子，因此用 mixin（Iris 兼容见 §8）；客户端 `client.sun_remap = false` 时直通 | 10 §5.1.5、§16.2 |

其余模组（11-24）的 §16 均声明“不使用 mixin”或尚未声明；尚未声明的章节完成后核对（§10.2）。

### 2.3 统一服务 `VanillaPolicyService`

`tcivilization` 提供一个只读查询服务，所有模组通过它判断原版系统当前由谁接管，避免各自猜测：

```java
public interface VanillaPolicyService {
    boolean isPlanetMode(MinecraftServer server);          // 主世界生成器的 codec 键为 tcwildlands:planet
    boolean isDeepEarthMode(MinecraftServer server);       // the_nether 的维度类型为 tcwildlands:deep_earth
    boolean isThresholdMode(MinecraftServer server);       // tcbeyond 已注册 ThresholdPresence 且 end.rework = true
    MonsterPolicy effectiveMonsterPolicy(ServerLevel level); // VANILLA | RUINS_ONLY | BEYOND（§3.4.1）
    Optional<String> owner(VanillaSystem system);          // 当前接管该系统的模组 ID
    void registerOwner(VanillaSystem system, String modId, int priority); // 模组在 FMLCommonSetup 中登记
}
```

- 判断“是否为星球模式”只看生成器的注册键，不加载 `tcwildlands` 的类，因此核心对 Wildlands 没有编译期依赖。
- `VanillaSystem` 枚举是**封闭列表**，下表为全集；新增值必须先改本表。只有存在“多个模组可能接管”或“需要回退”的系统才进入枚举，单一拥有者且无回退的改动（如 VO-CB 战斗、VO-IT 物品用途）不登记。

| 枚举值 | 对应 §3 条目 | 拥有者链（链首 100，依次 −10） |
| --- | --- | --- |
| `WORLDGEN` | VO-WG、VO-DM-01 | tcwildlands → 原版 |
| `STRUCTURES` | VO-ST | tcwildlands → 原版（其他模组只能在其结果上移除，§4.2） |
| `MONSTER_SPAWNS` | VO-SP-01、06、14 | tcbeyond → tcivilization → 原版 |
| `PHANTOMS` | VO-SP-02 | tcbeyond → tcivilization → 原版 |
| `WILDLIFE_SPAWNS` | VO-SP-03~04 | tcecology → 原版 |
| `RAIDS` | VO-RD-01 | tcfrontiers → tcbeyond → tcivilization → 原版 |
| `PATROLS` | VO-RD-02 | tcfrontiers → tcbeyond → tcivilization → 原版 |
| `WANDERING_TRADER` | VO-TR-01 | tcexchange → 原版 |
| `VILLAGERS` | VO-VL | tcsettlements → 原版 |
| `RECIPE_GATES` | VO-RC | tctechnology → 原版 |
| `WEATHER` | VO-WX | tcwildlands → 原版 |
| `FIRE` | VO-FR | tcwildlands → 原版 |
| `LOCATOR_BAR` | VO-UI-01 | tcatlas → 原版 |
| `COORDINATES` | VO-UI-02 | tcatlas → 原版 |
| `NETHER` | VO-WG-09、VO-DM-02 | tcwildlands（深地）→ 原版；tcbeyond 只登记 `ashen_reach`，不登记本项 |
| `END` | VO-WG-11、VO-DM-03 | tcbeyond（界阈）→ 原版 |
- 同一系统登记两个拥有者时，高 `priority` 获胜，低者在日志中给出 `WARN` 并自动只作为回退（§4.1）。

---

## 3. 原版改动总表

### 3.0 读表说明

- **编号**：`VO-<组>-<序号>`，全书引用以此为准，编号一经发布不重排（新增只追加）。
- **拥有者**：最终决定权；括号内为回退提供者，按顺序生效。
- **机制**：对应 §2.1 的优先级名称。
- **开关**：`文件名#键`；文件省略时为拥有者的 `<modid>-server.toml`。
- **状态**：`定稿` = 拥有者章节 §16 已写明并与本章一致；`待 NN 确认` = 拥有者章节尚在编写，本行是本章的规定，章节完成后核对。

### 3.1 世界生成（VO-WG）

| 编号 | 原版内容 | 改动 | 拥有者 | 机制 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| VO-WG-01 | 主世界生成器 | 替换为程序生成的类地球星球 `TcPlanetChunkGenerator`（DR-08~DR-12）；原版预设在新建世界时仍可选，选中后 Wildlands 降级（10 §2.5） | tcwildlands | 数据包（世界预设）+ 生成器注册 | `tcwildlands-common.toml#replace_overworld` | 定稿（10 §16.1） |
| VO-WG-02 | 原版矿石 `minecraft:*_ore`、`deepslate_*_ore`、`ancient_debris`、`nether_*_ore` | 星球模式下不生成；矿物由 Wildlands 的成矿系统提供（10 §8），原版产物经 `raw_*` 合成或冶炼得到 | tcwildlands | 生成器不放置 | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-03 | 非星球模式下的原版矿石 | **照常生成**；Industry 使用原版矿石与回退储层图（13 §2.2） | — | 不改 | — | 定稿（13） |
| VO-WG-04 | `minecraft:stone`、`minecraft:deepslate` | 星球模式下主世界不生成；深部为结晶基底岩（10 §5.9） | tcwildlands | 生成器 | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-05 | 原版雕刻器（洞穴、峡谷、意面洞） | 星球模式下替换为与岩性挂钩的自有洞穴系统（10 §5.11）；原版 `dripstone` 只出现在喀斯特溶洞 | tcwildlands | 生成器 | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-06 | 原版植被特征 | 星球模式下由 Ecology 的 `vegetation_slot` 提供；Ecology 缺失时 `VanillaVegetationFallback` 注入原版树木与植被 | tcwildlands（tcecology） | 群系特征列表 | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-07 | 原版群系 | 星球模式下主世界只使用 `tcwildlands:*` 群系；其 `temperature`/`downfall` 字段取气候包络中值，`temperature` 下限 0.15，保留原版草色混合规则（10 §5.8、§6.3） | tcwildlands | 群系源 | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-08 | 世界边界 | X 方向设为 ±(81,920 + 256)，用于东西环绕缓冲带（DA-13、10 §5.1.3） | tcwildlands | 事件 `LevelEvent.Load`（主世界首次加载且边界仍为原版默认值时设置） | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-09 | `minecraft:the_nether` | 维度 ID 保留，维度类型替换为 `tcwildlands:deep_earth`，生成器替换为深地（DA-09、10 §5.16）；无下界生物、无下界要塞与堡垒遗迹 | tcwildlands | 数据包覆写 `data/minecraft/dimension/the_nether.json` | `tcwildlands-common.toml#replace_nether` | 定稿（10 §16.1） |
| VO-WG-10 | 下界传送门 | 深地模式下黑曜石框架点火不创建传送门（取消 `BlockEvent.PortalSpawnEvent`；命令放置的传送门方块由 `EntityTravelToDimensionEvent` 拦截），首次尝试时提示一次；入口改为 `deep_conduit` 底部的 `deep_threshold` | tcwildlands | 事件（MX-03 只作后备，默认不应用） | 随 VO-WG-09 | 定稿（10 §16.1） |
| VO-WG-11 | `minecraft:the_end` | 维度 ID 保留，重做为“界阈”，作为异界维度枢纽（DR-16、21 §2）；停用内置数据包 `tcbeyond:threshold_rework` 时保留原版末地 | tcbeyond | 数据包覆写 + 结构集 | 内置数据包 `tcbeyond:threshold_rework`（默认启用）；`end.rework` 只做一致性检查（§4.4、21 §13） | 定稿（21） |
| VO-WG-12 | 原版花岗岩、闪长岩、安山岩、凝灰岩 | 星球模式下挖掘掉落对应 `_cobble`（10 §9.1 第 1 条）；精准采集仍掉原岩 | tcwildlands | GLM `tcwildlands:vanilla_rock_cobble`（战利品条件 `tcwildlands:planet_active`） | 随 VO-WG-01 | 定稿（10 §16.1） |
| VO-WG-13 | 原版海平面与世界高度 | 主世界高度 -512 至 1,024（DR-12），维度类型 `min_y = -512`、`height = 1536`；方块海平面 Y 63 由生成器给出（10 §5.17） | tcwildlands | 维度类型 `tcwildlands:earthlike` | 随 VO-WG-01 | 定稿（10 §16.1） |

### 3.2 结构（VO-ST）

星球模式下原版结构是否出现，由 Wildlands 的群系**是否加入**对应的 `#minecraft:has_structure/*` 标签决定（机制：标签）；非星球模式下由各拥有者用数据包把结构集 `frequency` 覆写为 0（机制：数据包）。原则：保留“考古与沉船”类结构（支撑 Discovery 的考古玩法），移除与写实世界历史（DR-07）或奇幻主线冲突的结构。Echoes 的程序化遗迹是星球上历史遗迹的主体。

| 编号 | 原版结构集 | 星球模式 | 非星球模式 | 拥有者 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| VO-ST-01 | `minecraft:villages` | 不生成 | Settlements 安装时不生成（`frequency = 0`）；缺失时原版生成 | tcsettlements（tcwildlands） | 内置数据包 `tcsettlements:vanilla_overrides`（15 §16） | 定稿（15） |
| VO-ST-02 | `minecraft:pillager_outposts` | 不生成 | Beyond 安装时不生成（移入 `tcbeyond:pale_hollow`）；缺失时原版生成 | tcbeyond（tcwildlands） | 内置数据包 `tcbeyond:vanilla_relocation`（默认启用）；`structures.relocate_vanilla` 只做一致性检查（21 §13） | 定稿（21） |
| VO-ST-03 | `minecraft:woodland_mansions` | 不生成 | 同 VO-ST-02 | tcbeyond（tcwildlands） | 同上 | 定稿（21） |
| VO-ST-04 | `minecraft:strongholds` | 不生成；界阈只能经裂隙之门进入（21 §5.6） | Beyond 安装且 `tcbeyond:threshold_rework` 启用时不生成；否则原版生成 | tcbeyond（tcwildlands） | 同 VO-WG-11 | 定稿（21） |
| VO-ST-05 | `minecraft:ocean_monuments` | 不生成；海绵与海晶改由 `silent_deep` 的沉渊圣所提供 | Beyond 安装时不生成（21 BQ-4）；缺失时原版生成 | tcbeyond（tcwildlands） | 同 VO-ST-02 | 定稿（21） |
| VO-ST-06 | `minecraft:ancient_cities` | 不生成；远古城市只在 `tcbeyond:silent_deep` | Beyond 安装时不生成（循守卫者只在 `silent_deep`）；缺失时原版生成 | tcbeyond（tcwildlands） | 同 VO-ST-02 | 定稿（21） |
| VO-ST-07 | `minecraft:mineshafts` | 不生成；历史矿坑由 Echoes 的遗迹模拟产生；无 Echoes 时 Wildlands 内置数据包恢复（10 §16） | 原版生成 | tcwildlands（tcechoes） | — | 定稿（18、10） |
| VO-ST-08 | `minecraft:desert_pyramids` | 生成（沙漠群系加入标签；保留可疑沙考古） | 原版生成 | tcwildlands | 数据标签 | 定稿（10 §16.5） |
| VO-ST-09 | `minecraft:jungle_temples`、`minecraft:igloos`、`minecraft:swamp_huts` | 不生成 | 原版生成 | tcwildlands | — | 定稿（10 §16.5） |
| VO-ST-10 | `minecraft:shipwrecks`、`minecraft:buried_treasures` | 生成（海洋、海滩群系加入标签）；Echoes 安装时沉船箱子可能出现古代货包（14 §16） | 原版生成 | tcwildlands（tcechoes 附加战利品） | 数据标签 | 定稿（18、10 §16.5） |
| VO-ST-11 | `minecraft:ocean_ruins`、`minecraft:trail_ruins` | 生成（考古结构；可疑沙/砾石由 Discovery 附加“未知古物”标签，17 §16） | 原版生成 | tcwildlands（tcdiscovery） | 数据标签 | 定稿（10 §16.5） |
| VO-ST-12 | `minecraft:trial_chambers` | 生成于地下（Y -40 ~ -20，非海洋陆地群系）；作为“古代试炼场”遗迹，试炼刷怪笼是星球上少数合法的原版怪物来源（§3.4，VO-OQ-02） | 原版生成 | tcwildlands（tcechoes 提供背景） | 数据标签 | 定稿（18、10 §16.5） |
| VO-ST-13 | `minecraft:ruined_portals` | 深地模式下不生成（下界传送门无法点燃）；否则照常 | 同左 | tcwildlands | 随 VO-WG-09（内置数据包） | 定稿（10 §16.5） |
| VO-ST-14 | `minecraft:nether_complexes`（下界要塞、堡垒遗迹）、`minecraft:nether_fossils` | 深地模式下不生成（深地生成器不放置）；Beyond 安装时下界要塞与堡垒遗迹位于 `tcbeyond:ashen_reach` | 同左 | tcwildlands（tcbeyond） | 随 VO-WG-09 | 定稿（21、10 §16.5） |
| VO-ST-15 | `minecraft:end_cities` | 界阈外岛保留末地城（潜影贝来源） | 同左；`threshold_rework` 停用时为原版末地 | tcbeyond | — | 定稿（21） |
| VO-ST-16 | 地牢（`minecraft:monster_room` 特征）与原版化石特征 | 地牢保留（刷怪笼遗迹，合 DR-18），由 Wildlands 地下特征阶段放置；原版化石特征由 `tcwildlands:fossil_bed` 代替 | 原版 | tcwildlands | 群系特征列表 | 定稿（10 §16.1） |
| VO-ST-17 | 原版制图台、讲台等工作方块所在的结构 | 随所在结构的去留 | 同左 | — | — | 定稿（22） |
| VO-ST-18 | 未列出的原版或第三方主世界结构集 | 只有当 Wildlands 群系属于其群系标签时才会出现：Wildlands 群系只加入本表“生成”行的原版标签，以及 `c:is_*` 系列通用标签（§8）；**不加入** `#minecraft:is_*`（10 §16.5、OQ-WLD-10） | 原版或第三方自行决定 | tcwildlands | 数据标签 | 定稿（10 §16.5） |

### 3.3 生物生成（VO-SP）

| 编号 | 原版内容 | 改动 | 拥有者 | 机制 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| VO-SP-01 | `MobCategory.MONSTER` 自然生成 | 见 §3.4：Beyond 在场时按影响区强度分档（21 §5.3）；缺失时由核心回退门 `vanilla.monster_policy` 决定 | tcbeyond（tcivilization） | 生物群系修改器 + `LevelEvent.PotentialSpawns` | `tcivilization-server.toml#vanilla.monster_policy`；Beyond 见 21 §13 | 定稿（03、21） |
| VO-SP-02 | 幻翼（`PhantomSpawner`，基于失眠） | 怪物稀缺时禁用失眠生成：`PlayerSpawnPhantomsEvent` 返回拒绝；Beyond 在场时幻翼改为 `I ≥ 60` 夜晚按普通怪物规则生成（21 §5.3.1） | tcbeyond（tcivilization） | 事件 | 随 VO-SP-01 | 定稿（21） |
| VO-SP-03 | 被动、环境、水生生物的自然与初始生成 | 主世界移除，改由 Ecology 物种账本分布（11 §5） | tcecology | 生物群系修改器 `tcecology:remove_vanilla_wildlife` | `tcecology-server.toml#spawning.replace_vanilla_wildlife` | 定稿（11） |
| VO-SP-04 | 原版野生动物实体 AI | 附加 `WildlifeComponent`，替换逃跑/警觉参数；繁殖、拴绳、驯服、骑乘规则不变 | tcecology | `EntityJoinLevelEvent` + Goal 注入 | 随 VO-SP-03 | 定稿（11） |
| VO-SP-05 | 野生蜂巢 | 保留世界生成特征；蜂群计入授粉指数 | tcecology | 不改 | — | 定稿（11） |
| VO-SP-06 | 史莱姆区块 | Beyond 在场时替换为 `I ≥ 30` 的沼泽与洞穴；怪物稀缺且 Beyond 缺失时不自然生成 | tcbeyond（tcivilization） | 同 VO-SP-01 | 同 VO-SP-01 | 定稿（21） |
| VO-SP-07 | 循守卫者 | Beyond 在场时只在 `tcbeyond:silent_deep` 由幽匿尖啸体召唤；主世界尖啸体召唤由 Beyond 以事件拒绝（不改全局游戏规则 `minecraft:spawn_wardens`，否则会同时关闭异界维度） | tcbeyond | 事件 | — | 定稿（21） |
| VO-SP-08 | 猪灵、疣猪兽、僵尸猪灵 | 只在 `tcbeyond:ashen_reach`；深地不生成 | tcbeyond（tcwildlands） | 维度生成表 | — | 定稿（21） |
| VO-SP-09 | 灾厄村民（掠夺者、卫道士、唤魔者、恼鬼） | 只在 `tcbeyond:pale_hollow`（21 §5.3.3） | tcbeyond | 结构移位 + 维度生成表 | — | 定稿（21） |
| VO-SP-10 | 深地中的敌对生物 | 深地本身不生成；Beyond 在余烬空洞口袋中补充岩浆怪、烈焰人、恶魂、凋灵骷髅（21 §5.3.2） | tcwildlands（tcbeyond） | 深地群系生成表为空 | — | 定稿（21、10 §16.1） |
| VO-SP-11 | 守卫者、远古守卫者 | 海底神殿不生成（VO-ST-05），因此不出现 | tcbeyond | 结构移除 | — | 定稿（21） |
| VO-SP-12 | 铁傀儡 | 无原版村庄，不会由村民召唤；玩家可照常建造 | — | 不改 | — | 定稿（20） |
| VO-SP-13 | 僵尸村民 | 生成规则不改（只在怪物允许生成的地方出现）；治愈后转换为 `tcivilization:person`（VO-VL-02） | tcsettlements | 事件 | `tcsettlements-server.toml#vanilla.convert_villagers` | 定稿（15） |
| VO-SP-14 | 骷髅陷阱马 | Beyond 在场时只在 `I ≥ 30` 的雷暴中出现；怪物稀缺且 Beyond 缺失时不出现 | tcbeyond（tcivilization） | 同 VO-SP-01 | — | 定稿（21） |
| VO-SP-15 | 太空维度 | Orbit 的维度不生成任何生物；刷怪笼与试炼刷怪笼不运作 | tcorbit | 刷怪表为空；`FinalizeSpawnEvent` 拒绝 `NATURAL`、`CHUNK_GENERATION`、`PATROL`、`REINFORCEMENT`、`EVENT` 原因 | `tcorbit-server.toml#[vanilla] space_spawn_guard`（关闭时仍无自然生成，刷怪笼照常） | 定稿（24 §16） |

### 3.4 怪物生成：Beyond 与核心回退门

#### 3.4.1 有效策略的判定

`VanillaPolicyService.effectiveMonsterPolicy(level)` 按以下顺序返回第一条成立的结果：

| 顺序 | 条件 | 有效策略 | 执行者 |
| --- | --- | --- | --- |
| 1 | `tcbeyond` 已安装且 `beyond.mode ∈ {full, quiet}` | `BEYOND` | Beyond 按 21 §5.3 分档；核心不过滤 |
| 2 | `tcbeyond` 已安装且 `beyond.mode = mundane` | `RUINS_ONLY` | 核心过滤（21 §5.1.2 第 6 步委托给核心，不重复实现；21 已确认） |
| 3 | `vanilla.monster_policy = vanilla` | `VANILLA` | 不过滤 |
| 4 | `vanilla.monster_policy = ruins_only` | `RUINS_ONLY` | 核心过滤 |
| 5 | `vanilla.monster_policy = auto`（默认）且为星球模式 | `RUINS_ONLY` | 核心过滤 |
| 6 | 其他（`auto` 且非星球模式） | `VANILLA` | 不过滤 |

理由：只装核心或只装 Wildlands 以外的模组时，玩家拿到的是原版难度的世界（VP-4）；一旦主世界换成写实星球，夜晚遍地僵尸与 DR-18 冲突，因此 `auto` 自动收紧。服务器管理员可以用 `vanilla` 强制恢复原版怪物。

#### 3.4.2 `RUINS_ONLY` 的规则

适用维度：标签 `#tcivilization:monster_policy/restricted` 中的维度，默认只有 `minecraft:overworld`。深地本身没有怪物生成表；原版下界（非深地模式）与原版末地保持原版。

| 生成原因（`EntitySpawnReason`） | `RUINS_ONLY` 下 |
| --- | --- |
| `NATURAL`、`CHUNK_GENERATION` | **拒绝**；例外：位置处于标签 `#tcivilization:monster_ruins` 中某结构的结构件包围盒内时允许（结构专属生成表 `spawn_overrides` 照常生效） |
| `SPAWNER`、`TRIAL_SPAWNER` | 允许（地牢、试炼密室、Echoes 遗迹中的刷怪笼） |
| `STRUCTURE`（结构放置时生成的生物） | 允许 |
| `PATROL` | 拒绝（与 VO-RD-02 一致） |
| `SPAWN_ITEM_USE`、`COMMAND`、`BREEDING`、`CONVERSION`、`REINFORCEMENT`、`MOB_SUMMONED`、`TRIGGERED`、`EVENT` 等其他原因 | 允许 |

- 实现：`LevelEvent.PotentialSpawns` 中对 `MONSTER` 类别清空候选列表（结构内例外除外），并以 `FinalizeSpawnEvent` 兜底拦截 `PATROL`；失眠幻翼见 VO-SP-02。
- `#tcivilization:monster_ruins` 默认内容：`#tcechoes:hostile_ruins`（可选引用，`required: false`；内容见 18 §7.5）、`minecraft:trial_chambers`。
- 豁免：标签 `#tcivilization:monster_policy/exempt` 中的实体类型不受过滤（默认空；给第三方 Boss 或任务生物使用，§8“生物模组”行）。
- 已存在的怪物不删除；切换策略只影响此后的生成。
- `/tc vanilla status` 显示当前有效策略、执行者与判定依据（§4.7）。

#### 3.4.3 与 DR-18 / DA-08 的对应

| 组合 | 主世界夜晚 | 原版怪物出现的地方 |
| --- | --- | --- |
| 星球模式 + Beyond | 影响区之外 `I < 10`，没有原版怪物 | 影响区（按强度分档）、刷怪笼、试炼密室、Echoes 敌对遗迹、异界维度 |
| 星球模式，无 Beyond | 没有原版怪物 | 刷怪笼、试炼密室、Echoes 敌对遗迹 |
| 原版预设 + Beyond | 同第一行（Beyond 接管所有维度的自然生成） | 同第一行 |
| 原版预设，无 Beyond，`auto` | 原版 | 原版 |

### 3.5 袭击与巡逻（VO-RD）

| 编号 | 原版内容 | 改动 | 拥有者链 | 机制 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| VO-RD-01 | 袭击（Raid） | 关闭：首次加载写入 `minecraft:raids = false`（26.x 由 `disableRaids` 改名且**取值反转**） | tcfrontiers → tcbeyond → tcivilization（有效策略为 `RUINS_ONLY` 时）→ 原版 | 首次加载游戏规则 | `tcfrontiers-server.toml#vanilla.disable_raids_and_patrols`；Frontiers 缺失时 `tcbeyond-server.toml#vanilla.raids_fallback` | 定稿（20、21） |
| VO-RD-02 | 灾厄巡逻队（`PatrolSpawner`） | 关闭：首次加载写入 `minecraft:spawn_patrols = false`，并以 `FinalizeSpawnEvent` 拦截 `PATROL` 作为保险 | 同上 | 首次加载游戏规则 + 事件 | 同上；Beyond 回退键 `vanilla.patrols_fallback` | 定稿（20、21） |
| VO-RD-03 | 不祥之兆、袭击之兆、不祥之瓶 | 物品与效果保留；袭击关闭后不祥之兆不会转为袭击；试炼之兆照常用于不祥试炼 | — | 不改 | — | 定稿 |
| VO-RD-04 | 村庄英雄 | 无袭击，无法获得；相关原版进度不可完成（编年史只读，不受影响） | — | 不改 | — | 定稿 |

拥有者链的含义：链上第一个已安装的模组负责写入游戏规则与安装保险事件；后面的模组检测到 `VanillaPolicyService.owner(RAIDS)` 已被占用时什么也不做。Frontiers 的替代内容是盗匪经济（20 §5），Nations 的替代内容是 AI 军队与边境冲突（23 §16）。

### 3.6 流浪商人（VO-TR）

| 编号 | 原版内容 | 改动 | 拥有者 | 机制 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| VO-TR-01 | 流浪商人与行商羊驼 | Exchange 安装时以流动行商 `tcexchange:peddler` 代替（16 §5.19.2）：首次加载写入 `minecraft:spawn_wandering_traders = false`；缺失时原版照常生成（流浪商人不是村民，不参与 VO-VL-02 转换） | tcexchange | 首次加载游戏规则 | `tcexchange-server.toml#vanilla.replace_wandering_trader`（默认 `true`；不覆盖玩家后续修改） | 定稿（16 §16） |

### 3.7 村庄与村民（VO-VL）

| 编号 | 原版内容 | 改动 | 拥有者 | 机制 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| VO-VL-01 | 原版村庄 | 不生成（VO-ST-01） | tcsettlements | 内置数据包 | `tcsettlements:vanilla_overrides` | 定稿（15） |
| VO-VL-02 | `minecraft:villager` | 加入世界时（刷怪蛋、治愈僵尸村民、旧存档）替换为 `tcivilization:person`：在聚落内成为移民户，否则成为流浪者；职业按原版职业映射 | tcsettlements | `EntityJoinLevelEvent` | `vanilla.convert_villagers` | 定稿（15） |
| VO-VL-03 | 原版工作站点方块 | 方块不改；`job_board` 视为通用工作地 | tcsettlements | 不改 | — | 定稿（15） |
| VO-VL-04 | 原版床 | 不改；NPC 可占用玩家放置的床，玩家睡觉时 NPC 让出 | tcsettlements | 事件 | — | 定稿（15） |
| VO-VL-05 | 村民交易 | Settlements 把村民转换为 `tcivilization:person`（VO-VL-02）后原版交易随之消失；NPC 交易由 Exchange 的 `MerchantComponent` 与对话交易 `tcexchange:open_trade` 接替（16 §5.19.1）。Exchange 本身不改原版交易；Settlements 缺失时原版村民与交易照常。Settlements 在场时制图师探险家地图只能从战利品获得（22 §16.1） | tcexchange（tcsettlements） | — | 随 `vanilla.convert_villagers` | 定稿（16 §16） |
| VO-VL-06 | Settlements 缺失时 | 原版村庄与村民照常；核心不改村民（03 §16） | — | — | — | 定稿（03） |

### 3.8 配方（VO-RC）

所有配方改动都经 `tctechnology` 的 `tc_recipe_gate` 框架（04 §8）实现：配方本体不删除，而是包一层“需要科技 X”的条件；未解锁时配方书与 JEI/EMI 显示锁定图标和所需科技名（VP-8）。“移除”只用于确有替代配方的情况，用 `neoforge:conditions` 覆写为空配方。Technology 缺失时，所有门控失效，原版配方恢复（各模组只在自身配方被替代时才移除原版配方）。

| 编号 | 原版配方 | 改动 | 拥有者 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- |
| VO-RC-01 | 原版木、石、铜、铁、金、钻石工具；弓、弩、剑 | 全部原版配方移除（DR-15）；E0-E1 由 Technology 的打制工具与狩猎武器代替（04 §5.13），E2 起由 Industry/Frontiers 注册替代品。替代品的拥有者缺失时，由 `tc_recipe_gate/fallback/` 按时代恢复原版配方（04 §8 后备表：石质工具 ← `bronze/smelting`，铁工具 ← `iron/bloomery`，钻石工具 ← `industrial/machine_tools`，弓 ← `neolithic/flatbow`，弩 ← `medieval/guilds`，铁剑 ← `iron/bloomery`）。Technology 本身缺失时原版配方完整恢复 | tctechnology（tcindustry、tcfrontiers） | 不可关闭（DR-15）；后备门槛 `tctechnology-server.toml#[fallback] enable_stubs` | 定稿（04） |
| VO-RC-02 | 下界合金升级、锻造台 | 锻造台配方的门槛为 `industrial/machine_tools`：Industry 缺失时由 04 §8 后备表执行，在场时由 13 §8.14 执行。Industry 在场时钻石工具只作战利品（`false` 时在 `industrial/machine_tools` 增加锻造配方），下界合金镐、斧、锹、锄的锻造台升级移除；下界合金盔甲升级与盔甲纹饰保留（13 §15）。下界合金只在 Beyond 维度获得（VO-DM-02） | tctechnology（tcindustry） | 同上；`tcindustry-server.toml` 的 `tools.diamond_tools_loot_only`（默认 `true`）、`vanilla.netherite_tool_upgrade`（默认 `false`） | 定稿（04 §8、13 §8.14、13 §18 C-2） |
| VO-RC-03 | 原版盔甲 | 04 §16 未移除盔甲配方；04 §8 后备表把**铁甲**列入 `iron/bloomery` 门槛。其他材料（皮革、锁链、金、钻石）是否按时代门控尚未规定。盔甲减伤由 Frontiers 重算（VO-CB-01） | tctechnology | — | 待定（VO-OQ-03） |
| VO-RC-04 | 营火、灵魂营火 | 原版配方改为 `tctechnology:fire_pit` 配方；营火方块本身保留，可在原版结构中找到 | tctechnology | `tctechnology-server.toml#[content] replace_vanilla_campfire` | 定稿（04） |
| VO-RC-05 | 火把、打火石 | 火把门控于 `stone/fire_making`；打火石门控于 `iron/bloomery` | tctechnology | 数据包 `tc_recipe_gate` | 定稿（04） |
| VO-RC-06 | 各木船、运输船 | 需要 `bronze/plank_boats`（Technology 缺失时为 Networks 独立模式 T1）；E0 改用木筏 | tcnetworks | 数据包（`neoforge:conditions`，可被整合包覆盖） | 定稿（14） |
| VO-RC-07 | 矿车，普通、动力、探测、激活铁轨 | 需要 `renaissance/wagonways`（独立模式 T4）；原版矿车视为木轨矿车的原版版本 | tcnetworks | 数据包（同上） | 定稿（14） |
| VO-RC-08 | 望远镜 | 门控于 Discovery 的 `renaissance/telescope`；Discovery 缺失而 Technology 在场时回退为 Atlas 的 `renaissance/celestial_navigation`；两者都缺失时原版配方 | tcdiscovery（tcatlas） | `tcdiscovery-server.toml#vanilla.gate_spyglass` | 定稿（17、22） |
| VO-RC-09 | 空地图、指南针、追溯指南针、制图台 | 原版配方移除；由 Atlas 的测绘仪器与地图系统代替（22 §16.1）。物品仍存在，可由创造模式、战利品或命令获得；原版制图台只能在未被 Settlements 抑制的原版村庄与 Echoes 遗迹中找到 | tcatlas | `tcatlas-common.toml#remove_vanilla_map_recipes` | 定稿（22） |
| VO-RC-10 | 纸、书 | 甘蔗造纸配方门控于 `medieval/papermaking`；Discovery 提供破布纸、木浆纸链 | tcdiscovery | `tcdiscovery-server.toml#vanilla.gate_paper` | 定稿（17） |
| VO-RC-11 | 弓、弩 | 原版弓配方由 tctechnology 管理（VO-RC-01，自有 `simple_bow`/`flatbow` 代替）；原版弩配方从配方书移除，物品保留并映射为 Frontiers 的 `crossbow` 档（20 §16），避免两套弩 | tcfrontiers（tctechnology） | 不单独设开关 | 定稿（04、20） |
| VO-RC-12 | 原版矿石熔炉/高炉冶炼 | 星球模式下原版矿石不生成，配方保留但无用；Industry 安装时以本模组路线为主（块炼等，约 4.5 粒/原矿）；原版熔炉/高炉炼原矿降为低收率回退：1 原矿 → 2 粒、烹饪时间 ×2，铜、金需 `bronze/smelting`，铁需 `iron/bloomery` | tcindustry | `tcindustry-server.toml#vanilla.furnace_ore_smelting`（`LOW_YIELD` 默认 / `DISABLED` / `VANILLA`） | 定稿（13 §8.14） |
| VO-RC-13 | 红石粉 | 星球模式下红石来自辰砂（13 §8.6，E4 起）；原版红石矿不生成，原版红石相关配方不变 | tcindustry | — | 定稿（13） |
| VO-RC-14 | 原版食物与种子 | 原版食物配方保留（含 3 小麦 → 面包）；原版食物附加 FU、营养组与 `freshness`，饥饿值与饱和度不改（DR-19）；种子在种植时由 Cultivation 替换为带基因的作物（VO-IT-11）；Cultivation 新增的加工食品不覆盖原版 ID | tccultivation | `food.spoilage_enabled`、`vanilla.food_freshness_on_vanilla_items`（12 §13） | 定稿（12 §16） |
| VO-RC-15 | 附魔台、酿造台、铁砧、砂轮、锻造台 | 配方不改（材料获取途径改变见 VO-MG） | — | 不改 | 定稿（17） |
| VO-RC-16 | 配方书 | 被门控的配方在未解锁时于配方书中显示为锁定；被移除的配方从配方书中隐藏 | tctechnology | 事件（`RecipesReceivedEvent` 客户端过滤） | 定稿（04） |
| VO-RC-17 | 星球模式下缺少原料的原版配方 | 由 §7 稀缺替代数据包补齐（线、骨头、黏液球；火药由 Wildlands/Industry 提供） | tcivilization | `tcivilization-common.toml#vanilla.scarcity_substitutes` | 本章定稿 |

### 3.9 战利品（VO-LT）

原则：**只附加，不删除**（§4.6）。所有改动通过全局战利品修改器（GLM）实现，可被整合包数据包覆盖。

| 编号 | 原版内容 | 改动 | 拥有者 | 状态 |
| --- | --- | --- | --- | --- |
| VO-LT-01 | 沙砾 | 徒手破坏掉落燧石 25%、锤石 10%（GLM `tctechnology:gravel_knapping`；`[content] gravel_flint_chance`、`gravel_hammerstone_chance`） | tctechnology | 定稿（04） |
| VO-LT-02 | 原版动物 | 附加 `tcecology:fur_pelt`、`tcecology:antler`（鹿科）等物种账本掉落；原版生皮/肉照常 | tcecology | 定稿（11） |
| VO-LT-03 | 可疑沙/可疑砾石 | 战利品附加“未知古物”标签与发现地点（17 §16），鉴定后成为知识线索 | tcdiscovery | 定稿（17） |
| VO-LT-04 | 沉船、埋藏宝藏箱子 | Echoes 安装时附加古代货包、航海日志；Networks 安装时附加船只残骸部件（14 §16） | tcechoes、tcnetworks | 定稿（18） |
| VO-LT-05 | 骨头 | 怪物稀缺（有效策略 ≠ `VANILLA`）时，核心 GLM `tcivilization:scarcity_bones` 让 `#tcivilization:bone_bearing` 中的动物有 35% 掉落 1 根骨头（抢夺每级 +10%）；Ecology 在场时向该标签追加体型 M 及以上的野生哺乳类，兽穴战利品也含骨头（11 §6.3；§7.1） | tcivilization（tcecology） | 定稿（03 §16、11 §6.3） |
| VO-LT-06 | 原版结构箱子中的奇幻物品（末影珍珠、烈焰粉、恶魂之泪） | Beyond 战利品修改器 `tcbeyond:overworld_loot_swap` 在主世界 `I_block < 30` 的结构箱子中替换为同价值的写实物品；异界维度与影响区不替换（`loot.swap_overworld_fantasy`，21 §5.3.7、§16）。`purge_map` 是另一张表，只用于 `mundane` 与卸载（21 §5.1.2） | tcbeyond | 定稿（21） |
| VO-LT-07 | 原版矿石方块掉落 | 不改；星球模式下不生成 | — | 定稿 |
| VO-LT-08 | 制图师探险家地图、藏宝图 | 保持可用；没有村民交易时只能从结构箱子获得。Atlas 安装时手持会在世界地图中显示为该结构周围 512 格的 SL1 传闻圈（22 §16.1） | tcatlas | 定稿（22） |

### 3.10 战斗（VO-CB）

| 编号 | 原版内容 | 改动 | 拥有者 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- |
| VO-CB-01 | 盔甲减伤公式 | Frontiers 安装时，其伤害类型加入 `minecraft:bypasses_armor`，再按命中部位与穿甲自行结算护甲（20 §5.8.3）；不使用 mixin。`combat.hit_locations = false` 时所有命中按躯干结算 | tcfrontiers | `tcfrontiers-server.toml#combat.hit_locations` | 定稿（20） |
| VO-CB-02 | 原版护甲与武器 | 由数据包 `weapon_profile`/`armour_profile` 映射到 Frontiers 档位（20 §6.3），使战利品中的原版装备仍有意义 | tcfrontiers | 数据包 | 定稿（20） |
| VO-CB-03 | 攻击冷却、横扫、暴击、盾牌 | 不改 | — | — | 定稿 |
| VO-CB-04 | 弩 | 见 VO-RC-11 | tcfrontiers | — | 定稿（20） |
| VO-CB-05 | 自然回血、饥饿、饱和 | 不改（DR-19、DA-12）；不新增体温、口渴、疾病状态 | — | — | 定稿 |
| VO-CB-06 | 技能对战斗的加成 | 技能不提供任何伤害加成或伤害减免，也不给玩家本体添加属性修饰（05 §16、DR-19）；`combat` 树只降低武器磨损（`wear/weapon`）并提高远程精度（`precision/aim`，05 §6.6.4）。唯一的技能属性修饰 `tcskills:vehicle_speed` 只加在坐骑上，且不存档 | tcskills | — | 定稿（05 §16） |

### 3.11 天气与时间（VO-WX）

| 编号 | 原版内容 | 改动 | 拥有者 | 机制 | 状态 |
| --- | --- | --- | --- | --- | --- |
| VO-WX-01 | 全局下雨/雷暴 | 星球模式下改为局地天气格（MX-01）；原版全局天气状态只作为“天气格引擎关闭时”的回退 | tcwildlands | mixin MX-01 | 定稿（10 §16.1、§16.2） |
| VO-WX-02 | `/weather` 命令 | 星球模式下作用于执行者所在天气格及相邻 8 格，持续时间按命令参数；`/tcwildlands weather set <type> <ticks> global` 提供全局控制（10 §13.4） | tcwildlands | 事件 `CommandEvent`（拦截根节点 `weather`） | 定稿（10 §16.1） |
| VO-WX-03 | `minecraft:advance_weather`、`minecraft:advance_time` | 不改；为 false 时天气格与季节也停止推进 | tcwildlands | 读取游戏规则 | 定稿（10 §16.3） |
| VO-WX-04 | 昼夜长度 | 星球模式下日照时长随纬度与季节变化；一天总长仍为 24,000 刻（与床、村民日程、原版计时兼容） | tcwildlands | MX-04 | 定稿（10 §16.1） |
| VO-WX-05 | 天空变暗 | 由天气格本地雨强决定，不读全局雨强 | tcwildlands | 事件 `ClientTickEvent.Post`（`ClientLevel#setRainLevel`/`setThunderLevel`） | 定稿（10 §16.1） |
| VO-WX-06 | 积雪 | Wildlands 自有积雪逻辑，上限 `seasons.max_snow_layers`（默认 8 层），不读也不写 `minecraft:max_snow_accumulation_height`；星球主世界的 `Biome#shouldFreeze`/`shouldSnow` 返回 false（MX-01） | tcwildlands | 自有降水刻（10 §5.12.3） | 定稿（10 §16.1） |
| VO-WX-07 | 季节 | Wildlands 提供天文季节；Wildlands 缺失时 Cultivation 使用核心日历季节 + 回退温度（12 §2.7，`fallback.season_length_days = 12`） | tcwildlands（tccultivation） | — | 定稿（10 §16.1、12 §16） |
| VO-WX-08 | 树叶颜色 | 星球模式下树叶与草色随季节变化，冬季落叶树显示裸枝模型（客户端颜色解析器，不用 mixin） | tcwildlands | 事件 `RegisterColorHandlersEvent.Block`、`RegisterColorHandlersEvent.ColorResolvers` | 定稿（10 §16.1） |
| VO-WX-09 | 冰雹 | 新增天气类型：雷暴格中的 `HAIL` 使露天原版作物以 5% 概率退一个生长阶段；**对实体无伤害**（DR-19，10 OQ-WLD-6）；原版没有对应内容 | tcwildlands | 自有 | 定稿（10 §16.1） |
| VO-WX-10 | 洋流 | 星球模式下洋流推动原版船与漂浮实体（10 §5.5、§5.6.2；河流同样施加推力） | tcwildlands | 事件 `EntityTickEvent.Pre` | 定稿（10 §16.1） |
| VO-WX-11 | 睡觉跳过夜晚 | 不改；`minecraft:players_sleeping_percentage` 照常；跳夜时 Settlements、Industry 等按经过的刻数做补算 | — | 不改 | 定稿（02 §7） |

### 3.12 火焰（VO-FR）

| 编号 | 原版内容 | 改动 | 拥有者 | 状态 |
| --- | --- | --- | --- | --- |
| VO-FR-01 | 火焰蔓延 | 星球模式下由山火模型（湿度、风向、燃料）接管（MX-02）；`minecraft:fire_spread_radius_around_player = 0` 时完全不蔓延，大于 0 时只在该半径内蔓延（与原版语义一致） | tcwildlands | 定稿（10 §16.1、§16.3） |
| VO-FR-02 | 单次山火规模 | 全服燃烧中的火焰方块上限 `hazards.wildfire.max_burning_blocks`（默认 4,000），达到后不再点燃新方块；玩家点火、熔岩等其他来源使总数超限时，按距玩家由远到近熄灭最外圈 | tcwildlands | 定稿（10 §16.1） |
| VO-FR-03 | 非星球模式 | 原版火焰逻辑 | — | 定稿 |

### 3.13 界面（VO-UI）

UI 的完整规格见 31 章；本表只登记“改动了原版界面”的条目。

| 编号 | 原版界面 | 改动 | 拥有者 | 开关 | 状态 |
| --- | --- | --- | --- | --- | --- |
| VO-UI-01 | 定位栏（经验条位置） | Atlas 安装时首次加载写入 `minecraft:locator_bar = false`，文明成员的方向改由罗盘条 `tcatlas:compass_bar` 显示（E4 起；E4 前只有共享路点与临时标记）。为 false 时保留原版定位栏，罗盘条不显示玩家 | tcatlas | `tcatlas-server.toml#replace_locator_bar` | 定稿（22） |
| VO-UI-02 | F3 坐标 | 默认不改。开启后，对未掌握 `space/satellite_navigation` 的生存模式玩家逐个发送“简化调试信息”实体事件（22/23），不改全局游戏规则 `minecraft:reduced_debug_info`；OP 与创造模式除外 | tcatlas | `tcatlas-server.toml#hide_vanilla_coordinates`（默认 false） | 定稿（22） |
| VO-UI-03 | 键位 | 新增键位全部在 31 §6.1 登记；不覆盖原版默认键位；冲突时原版优先，本模组键位标红提示 | 各模组 | — | 定稿（31） |
| VO-UI-04 | 骑乘生物血条 | 乘坐 Networks 载具时，该位置由 `tcnetworks:vehicle_status` 替换（31 §3.3.2）；骑乘原版生物时不改 | tcnetworks | — | 定稿（14、31） |
| VO-UI-05 | 配方书 | 见 VO-RC-16 | tctechnology | — | 定稿（04） |
| VO-UI-06 | 暂停菜单 | 不改；模组入口为 `J` 文明面板（03 §5.10.1）与 NeoForge 模组列表中的配置页 | — | — | 定稿（03、31） |
| VO-UI-07 | 死亡界面 | 不改；Atlas 读取原版 `lastDeathLocation` 生成 `death` 路点（保留最近 5 个），不修改原版字段（22 §16.6） | tcatlas | — | 定稿（22） |
| VO-UI-08 | 原版进度界面（L） | 不改；编年史是 `J` 面板中的 `tcivilization:chronicle` 页签（03 §10.2），`Y` 属于 Discovery 日志（Discovery 缺失时为 Echoes 传说） | — | — | 定稿（03） |
| VO-UI-09 | HUD 原版元素 | 不隐藏原版血量、饥饿、经验、快捷栏；模组 HUD 部件避开这些区域（31 §5） | — | — | 定稿（31） |

### 3.14 原版物品用途（VO-IT）

| 编号 | 原版物品/方块 | 改动 | 拥有者 | 状态 |
| --- | --- | --- | --- | --- |
| VO-IT-01 | 骨粉 | 对 Cultivation 作物不再直接催熟；改为施肥（土壤磷 +8，12 §5）；对原版树苗、草、花照常（`vanilla.bone_meal_mode = "phosphorus"`；设为 `vanilla` 时推进一个生长阶段） | tccultivation | 定稿（12 §16） |
| VO-IT-02 | 刷子 | 原版考古照常；Discovery 安装时可刷 Echoes 的 `artefact_deposit`，刷沉积岩可得化石（17 §16） | tcdiscovery | 定稿（17） |
| VO-IT-03 | 讲台、书架、雕纹书架 | 讲台放在设施内计为 `reading` 家具，讲台上的载体可被学者阅读；书架计为藏书家具，雕纹书架中的载体计入书目 | tcdiscovery | 定稿（17） |
| VO-IT-04 | 钟 | Discovery 安装时可作为 2 级计时仪器（17 §16）；Wildlands 星球模式下钟显示当地太阳时（与 MX-04 读同一天体角；客户端 `client.sun_remap = false` 时为全局时间，10 OQ-WLD-5） | tcdiscovery（tcwildlands） | 定稿（17、10 §16.1） |
| VO-IT-05 | 望远镜 | 原版缩放照常；Discovery 安装时作为 2 级天文仪器（17）；Atlas 安装时远眺可把 64 格外的区块写入个人记忆（22 §16.3） | tcdiscovery、tcatlas | 定稿（17、22） |
| VO-IT-06 | 已填充的原版地图、原版制图台 | 原版制图台右键打开 Atlas `map_table` 界面；放入 `filled_map` 可按已探索像素导入地图库（SL3，ε 由原版地图比例决定），原地图不消耗（22 §16.1） | tcatlas | 定稿（22） |
| VO-IT-07 | 原版指南针、追溯指南针、磁石 | 保持原功能（指向出生点/死亡点），但不提供罗盘条；磁石保持原版行为，并用于磁化 `lodestone_needle`（22 §16.1） | tcatlas | 定稿（22） |
| VO-IT-08 | 床 | 见 VO-VL-04；床仍决定重生点 | tcsettlements | 定稿（15） |
| VO-IT-09 | 原版铁轨与矿车实体 | 保留，可继续使用（兼容既有存档与红石装置）；Networks 列车不在原版铁轨上运行 | tcnetworks | 定稿（14） |
| VO-IT-10 | 马、驴、骡、骆驼、羊驼 | 可佩戴 Networks 驮鞍/挽具成为役畜与驮兽；骆驼与羊驼的原版行为不变；原版鞍、拴绳照常 | tcnetworks | 定稿（14） |
| VO-IT-11 | 原版种子与作物（小麦、甜菜、胡萝卜、土豆、南瓜、西瓜、可可豆、甜浆果、甘蔗） | 种植时替换为 Cultivation 的基因作物方块（村民、发射器、他模组放置在本 tick 末替换；世界生成的村庄农田、野生甜浆果与甘蔗在区块首次加载时替换）；收获物仍为原版 ID；配置 `vanilla.replace_seeds_on_place`（12 §16） | tccultivation | 定稿（12） |
| VO-IT-12 | 泥土小径 | 被频繁踩踏的草方块自动变为土径（14 §5.2.3）；铲子手动制造照常 | tcnetworks | 定稿（14） |
| VO-IT-13 | 铜、铁工具 | 原版铜工具（26.x 新增）与铁工具作为 Technology 工具档位的原版对应物；金工具只作装饰与仪式用途 | tctechnology | 定稿（04） |
| VO-IT-14 | Beyond 惰性物品 | **不采用**（21 BQ-6）：末影珍珠、烈焰棒等原版物品在任何地方保持原版功能；Beyond 自身物品在影响区外也照常工作。数据组件 `tcbeyond:inert` 只在 `mundane` 模式下加到 Beyond 自身物品上（21 §5.1.2） | tcbeyond | 不采用（21） |
| VO-IT-15 | 原版 `raw_*` 物品 | Industry 缺失时可在熔炉直接冶炼（原版行为）；Industry 安装时默认低收率：1 原矿 → 2 粒、烹饪时间 ×2，并按金属受科技门槛限制（13 §8.14，`vanilla.furnace_ore_smelting`） | tcindustry | 定稿（13 §8.14） |

### 3.15 玩家成长（VO-PL）

| 编号 | 原版内容 | 改动 | 拥有者 | 状态 |
| --- | --- | --- | --- | --- |
| VO-PL-01 | 饥饿、饱和 | 不改（DR-19） | — | 定稿 |
| VO-PL-02 | 原版进度 | 不改、不隐藏；编年功绩可用触发器 `tcivilization:advancement` 只读监听原版进度（03 §5.5.2，例如 `story/enter_the_nether` → `tcivilization:first_nether`） | tcivilization | 定稿（03） |
| VO-PL-03 | 经验来源 | 原版来源保留；星球模式下原版矿石不生成，因此 Wildlands 矿物挖掘给予与原版同档的经验（数值见 10 §16.7；精准采集不给经验）；技能熟练度与原版经验完全分离（05） | tcwildlands、tcskills | 定稿（05 §16、10 §16.7） |
| VO-PL-04 | 原版行为对技能的贡献 | 技能系统只读监听原版事件（挖掘、合成、击杀、交易）累计熟练度，不改原版结果（05 §15 原版回退挂钩：`EventPriority.LOWEST`、`receiveCanceled = false`，从不取消或修改事件，15.2 第 5 条） | tcskills | 定稿（05 §15.2） |
| VO-PL-05 | 重生 | 不改；`minecraft:respawn_radius` 照常 | — | 定稿 |

### 3.16 附魔与酿造（VO-MG）

| 编号 | 原版内容 | 改动 | 拥有者 | 状态 |
| --- | --- | --- | --- | --- |
| VO-MG-01 | 附魔台、附魔书、铁砧 | 机制不改；材料来源改变：青金石 E2、钻石 E5、绿宝石由 Wildlands 宝石系统生成（10 §8；Industry 缺失时 10 §8.4 回退配方补齐）；书需要 Discovery 的纸（VO-RC-10） | — | 定稿（17、10 §16.1） |
| VO-MG-02 | 附魔条件 | 附魔机制不改、不移除任何附魔；Beyond 安装时影响区内附魔台等级 +1/+2/+3，宝藏附魔书只出现在影响区与异界战利品中（21 §5.3.8、§16） | tcbeyond | 定稿（21） |
| VO-MG-03 | 酿造台与药水 | 机制不改；酿造台需要烈焰棒，药水需要下界疣，两者在星球模式下只来自 Beyond 维度或深地余烬空洞。因此**星球模式下酿造是 Beyond 路线的内容**，与 DR-06“魔法独立路线”一致。Beyond 追加帷花基底（代替下界疣）与异质粉尘燃料（代替烈焰粉），但酿造台本身仍需烈焰棒（21 §5.3.8） | tcbeyond | 定稿（21）/ 无 Beyond 时待定（VO-OQ-05） |
| VO-MG-04 | 信标、潮涌核心、不死图腾、鞘翅 | 下界之星（凋灵骷髅头颅：余烬空洞或 `ashen_reach`）、不死图腾（唤魔者：`pale_hollow`）、鞘翅（界阈末地城）只在 Beyond 维度获得；潮涌核心的海洋之心与鹦鹉螺壳仍在主世界（VO-ST-10、溺尸、钓鱼）；物品机制不改 | tcbeyond | 定稿（21） |

### 3.17 维度（VO-DM）

| 编号 | 维度 | 改动 | 拥有者 | 状态 |
| --- | --- | --- | --- | --- |
| VO-DM-01 | `minecraft:overworld` | VO-WG-01 | tcwildlands | 定稿（10 §16.1） |
| VO-DM-02 | `minecraft:the_nether` | 深地（VO-WG-09）；原版下界内容迁入 `tcbeyond:ashen_reach` | tcwildlands（tcbeyond） | 定稿（21、10 §16.1） |
| VO-DM-03 | `minecraft:the_end` | 界阈（VO-WG-11） | tcbeyond | 定稿（21） |
| VO-DM-04 | 异界维度 | `tcbeyond:ashen_reach`（下界内容）、`tcbeyond:pale_hollow`（灾厄内容）、`tcbeyond:silent_deep`（远古城市）等，均为新增维度 | tcbeyond | 定稿（21） |
| VO-DM-05 | 太空维度 | Orbit 新增 `tcorbit:low_orbit`（虚空轨道维度，24 §5.9）、`tcorbit:moon`（24 §5.10）、`tcorbit:mars`（24 §5.13），通用规则见 24 §5.18；不改原版维度 | tcorbit | 定稿（24 §5、§16） |
| VO-DM-06 | 原版下界、末地的可达性 | 星球模式 + 深地模式 + 无 Beyond：下界内容不可达（深地代替）；无要塞、无末影人、无烈焰人，因此原版末地**没有自然入口**；末地仍存在，可用命令进入。见 VO-OQ-04 | — | 待定（VO-OQ-04） |

---

## 4. 冲突与优先级规则

### 4.1 拥有者登记

1. 每个模组在 `FMLCommonSetupEvent` 中对自己拥有的系统调用 `VanillaPolicyService.registerOwner(system, modId, priority)`；优先级取 §3 拥有者链的顺序（链首 100，依次 −10）。
2. 同一系统出现两个登记者时，高优先级者成为拥有者；低优先级者 `owner(system)` 返回值不是自己，必须跳过自己的实现（包括游戏规则写入与事件拦截）。
3. 同优先级冲突视为设计错误：开发构建中抛出异常，发布构建中按模组 ID 字典序取第一个并记录 `ERROR`。
4. 第三方模组可登记自己为某系统的拥有者（例如另一个世界生成模组登记 `WORLDGEN`），此时本家族的对应模组自动降级（VP-2）。

### 4.2 同一原版对象的多处改动

| 对象 | 规则 |
| --- | --- |
| 配方 | 每个原版配方只能有**一个主门槛**。次级门槛必须声明 `when_absent: [modid]`，只在主门槛拥有者缺失时生效（例：VO-RC-08 望远镜）。`RecipeGateService` 在加载时检测重复主门槛并报错 |
| 战利品 | 只附加（§4.6）；多个修改器按模组加载顺序依次附加，互不覆盖 |
| 生物生成 | 执行顺序：Ecology 生物群系修改器（移除被动生物）→ Beyond 生物群系修改器（怪物分档）→ 核心 `PotentialSpawns` 过滤（仅 `RUINS_ONLY`）→ 第三方。后者只能在前者的结果上继续过滤，不能加回 |
| 结构 | 星球模式下由 Wildlands 群系标签决定能否出现；其他模组只能在 Wildlands 允许的基础上再移除（例：Beyond 移走灾厄结构），不能加回 |
| 实体替换 | 只允许一次替换：`minecraft:villager` → `tcivilization:person`（VO-VL-02）。其他模组需要“改造”原版实体时，附加组件而不替换实体 |
| 界面 | 31 章的 HUD 区域登记是唯一依据；两个模组声明同一区域时由 31 章裁决 |

### 4.3 游戏规则写入协议

游戏规则是存档级状态，卸载模组后不会自动恢复，因此单独规定：

1. **只在首次加载写入。** 拥有者在世界第一次以该模组加载时（`ServerStartedEvent`，且 `vanilla_rules.dat` 中没有该规则的记录）写入 §6 规定的值。
2. **记录。** 写入记录保存于 `data/tcivilization/vanilla_rules.dat`：`{ rule, written_value, previous_value, writer_mod, game_time }`。
3. **尊重管理员。** 之后每次加载时，若当前值 ≠ `written_value`，视为管理员手动修改：不再写入，日志 `WARN` 一次，并向在线 OP 发送一次聊天提示；此后不再提示。
4. **配置开关改为 false。** 不自动恢复原版值（避免覆盖管理员意图）；提示管理员使用 `/tcdebug vanilla restore <rule>`。
5. **保险事件。** 对袭击、巡逻这类“规则被改回后会立即影响玩法”的系统，拥有者另以事件拦截作为保险（VO-RD-02）；保险只在配置开关为 true 时生效，与游戏规则当前值无关。
6. **卸载。** 卸载拥有者模组前应运行 `/tcdebug vanilla restore all`，把记录中的规则恢复为 `previous_value`。若直接卸载，核心在下次加载时发现 `writer_mod` 缺失，向 OP 提示一次可恢复的规则清单。

### 4.4 数据包与配置的加载时序

NeoForge 的服务器配置（`serverconfig/*-server.toml`）在 `ServerAboutToStartEvent` 才加载，晚于世界数据包、维度与结构注册表的加载。因此：

| 改动类型 | 允许读取的开关 | 实现方式 |
| --- | --- | --- |
| 维度、维度类型、世界预设、结构集、群系标签（世界生成期确定的内容） | 只读 `*-common.toml`，或作为**内置可选数据包** | 由 `AddPackFindersEvent` 注册内置数据包 `<modid>:vanilla_overrides`，默认启用；创建世界的“数据包”界面或 `/datapack disable` 可按世界关闭 |
| 配方移除、配方门槛 | 可读 `*-server.toml` | 经 `tc_recipe_gate` 在运行时判定（被“移除”的配方实为永不满足的门槛），不依赖加载期条件 |
| 战利品附加、生成过滤、实体替换、游戏规则 | 可读 `*-server.toml` | 运行时事件或 GLM 内部判断开关 |

- 加载期 `neoforge:conditions` 只允许使用 `neoforge:mod_loaded`、`neoforge:tag_empty`、逻辑组合 `neoforge:and`/`or`/`not`，以及读取 common 配置的 `tcivilization:common_config` 条件（03 §16）。
- 位于 `*-server.toml` 却控制世界生成期内容的键，按本节改为：内置数据包承担实际改动，server 键只用于“数据包状态与配置不一致时提示管理员”。15 章 `vanilla.suppress_villages` 已按此修改（15 §16）；21 章 `end.rework`、`structures.relocate_vanilla`、`dimensions.pale_hollow`、`dimensions.silent_deep` 已按此修改（21 §13）。

### 4.5 配方门槛的显示

- 未解锁：配方书显示灰色配方与锁图标，悬停显示“需要科技：<名称>（<时代>）”；JEI/EMI 通过 `tctechnology` 提供的插件显示同样的锁定信息。
- 已移除：配方书与 JEI/EMI 隐藏；物品详情页显示“获取途径：<替代内容>”（由拥有者提供一行说明键 `vanilla_override.<modid>.<recipe>`）。
- 创造模式玩家不受配方门槛限制（本章规定；`RecipeGateService` 对创造模式直接放行，04 章实现时补入 §5）。

### 4.6 战利品“只附加”原则

- 不删除原版掉落，也不降低原版掉落数量。唯一例外是 VO-WG-12（星球模式下原版岩石掉落 `_cobble`），它是 Wildlands 岩石体系的一部分，以 GLM `tcwildlands:vanilla_rock_cobble` 实现（10 §16.1、OQ-WLD-9），随 `replace_overworld` 一起启用。
- 附加掉落必须标注来源模组（GLM ID 以拥有者命名空间开头），便于整合包作者用数据包移除。

### 4.7 命令

| 命令 | 权限 | 作用 |
| --- | --- | --- |
| `/tc vanilla status` | 0 | 列出当前维度的有效怪物策略、袭击与巡逻状态、星球/深地/界阈模式、各 `VanillaSystem` 的拥有者；对 OP 额外显示判定依据与 `vanilla_rules.dat` 记录 |
| `/tc vanilla status <system>` | 0 | 单个系统的详情（拥有者链、开关值、回退来源） |
| `/tcdebug vanilla owners` | 3 | 拥有者登记表与冲突记录 |
| `/tcdebug vanilla restore <rule>\|all` | 3 | 把本家族写入过的游戏规则恢复为 `previous_value`，并删除对应记录 |
| `/tcdebug vanilla policy <dimension> <vanilla\|ruins_only\|clear>` | 3 | 运行时临时覆盖某维度的有效怪物策略（不写配置，重启失效），用于测试 |

---

## 5. 拥有者缺失时的回退矩阵

### 5.1 按系统

| 原版系统 | 拥有者 | 拥有者缺失、回退者在场 | 本家族都不处理时 |
| --- | --- | --- | --- |
| 主世界生成 | tcwildlands | — | 原版主世界（Industry 用回退储层图，13 §2.2） |
| 下界 | tcwildlands（深地） | Beyond 在场时 `ashen_reach` 提供下界内容，原版下界保留 | 原版下界 |
| 末地 | tcbeyond（界阈） | — | 原版末地 |
| 原版村庄与村民 | tcsettlements | 星球模式下 Wildlands 不提供村庄标签，因此仍没有村庄（VO-ST-01） | 原版 |
| 主世界怪物 | tcbeyond | 核心回退门（§3.4） | 原版（`auto` 且非星球模式） |
| 袭击、巡逻 | tcfrontiers | Beyond → 核心（`RUINS_ONLY` 时） | 原版 |
| 被动生物 | tcecology | — | 原版（星球模式下 Wildlands 群系按 10 §6.3.1 照搬原版模板群系的生成表；`MONSTER` 类别再经 §3.4 过滤） |
| 原版工具、武器配方 | tctechnology | `tc_recipe_gate/fallback/` | 原版 |
| 地图、罗盘配方 | tcatlas | — | 原版 |
| 纸、望远镜配方 | tcdiscovery | 望远镜：Atlas 次级门槛 | 原版 |
| 船、铁轨配方 | tcnetworks | — | 原版 |
| 流浪商人 | tcexchange | — | 原版 |
| 天气、火焰、昼夜 | tcwildlands | — | 原版 |
| 定位栏 | tcatlas | — | 原版 |
| 护甲结算 | tcfrontiers | — | 原版 |
| 骨粉、种子 | tccultivation | — | 原版 |

### 5.2 典型安装组合

| 组合 | 主世界 | 怪物 | 袭击 | 村庄 | 原版工具配方 |
| --- | --- | --- | --- | --- | --- |
| A：只装两个核心 | 原版 | 原版 | 原版 | 原版 | 移除，按时代后备恢复 |
| B：核心 + Wildlands | 星球 | `RUINS_ONLY`（核心回退门） | 关闭（核心，§3.5） | 无（无村庄标签） | 同 A |
| C：核心 + Settlements（原版世界） | 原版 | 原版 | 原版 | 不生成；原版村民转为 `person` | 同 A |
| D：完整整合包 | 星球 | Beyond 分档 | 关闭（Frontiers） | 无 | 由 Industry/Frontiers 替代品提供 |
| E：完整整合包，`beyond.mode = mundane` | 星球 | `RUINS_ONLY`（核心执行） | 关闭（Frontiers） | 无 | 同 D |
| F：完整整合包去掉 Beyond | 星球 | `RUINS_ONLY` | 关闭（Frontiers） | 无 | 同 D；异界材料不可得（§7.3） |

---

## 6. 游戏规则

26.x 起游戏规则使用带命名空间的蛇形命名，部分规则取值反转（例如 `disableRaids` → `raids`）。本表是整合包对游戏规则的唯一规定；表中“首次加载写入”按 §4.3 执行。

| 规则（26.x） | 旧名 | 原版默认 | 本整合包 | 写入者 | 理由 |
| --- | --- | --- | --- | --- | --- |
| `minecraft:raids` | `disableRaids`（取值反转） | true | **false**（首次加载写入） | VO-RD-01 拥有者链 | DR-17、DR-18 |
| `minecraft:spawn_patrols` | `doPatrolSpawning` | true | **false**（首次加载写入） | 同上 | 同上 |
| `minecraft:spawn_wandering_traders` | `doTraderSpawning` | true | Exchange 安装时 **false**（首次加载写入）；否则不改 | tcexchange | VO-TR-01（16 §16） |
| `minecraft:locator_bar` | （26.x 新增） | true | Atlas `replace_locator_bar = true` 时 **false**（首次加载写入） | tcatlas | VO-UI-01 |
| `minecraft:spawn_phantoms` | `doInsomnia` | true | 不写；怪物稀缺时由事件拒绝失眠生成（VO-SP-02） | — | 不让规则状态依赖安装组合 |
| `minecraft:spawn_wardens` | `doWardenSpawning` | true | 不写；Beyond 以事件限制主世界（VO-SP-07） | — | 全局关闭会同时影响 `silent_deep` |
| `minecraft:reduced_debug_info` | `reducedDebugInfo` | false | 永不写；需要时逐玩家发送标志（VO-UI-02） | — | 不影响 OP 与创造模式 |
| `minecraft:fire_spread_radius_around_player` | `doFireTick`（语义改为半径） | 128 | 不改；Wildlands 山火模型遵守该值（0 = 不蔓延，VO-FR-01） | — | 定稿（10 §16.3） |
| `minecraft:random_tick_speed` | `randomTickSpeed` | 3 | 不改；Wildlands 的植被与积雪按默认值调参，Cultivation 作物生长不使用随机刻 | — | 改动会破坏 10、12 的数值 |
| `minecraft:max_snow_accumulation_height` | `snowAccumulationHeight` | 1 | 不改；星球模式下 Wildlands 自有积雪逻辑（上限 `seasons.max_snow_layers`，默认 8 层）不读也不写此规则 | — | 定稿（10 §16.3） |
| `minecraft:advance_weather` | `doWeatherCycle` | true | 不改；false 时天气格停止推进 | — | VO-WX-03 |
| `minecraft:advance_time` | `doDaylightCycle` | true | 不改；false 时季节、日历与模拟补算也停止 | — | 02 §7、03 §5.3 |
| `minecraft:spawn_mobs` | `doMobSpawning` | true | 不改；false 时 Ecology 账本照常模拟，但不实体化新个体，已实体化的保留 | — | 11 §16（已核对） |
| `minecraft:keep_inventory` | `keepInventory` | false | 不改 | — | DR-19 |
| `minecraft:mob_griefing` | `mobGriefing` | true | 不改；为 false 时 NPC 的“破坏方块”类任务（伐木、采矿）**不受影响**，因为它们由模组任务系统执行而非生物 AI 破坏 | — | 03 §5.7 |
| `minecraft:natural_health_regeneration` | `naturalRegeneration` | true | 不改 | — | DR-19、DA-12 |
| `minecraft:respawn_radius` | `spawnRadius` | 10 | 不改 | — | — |
| `minecraft:mob_drops` | `doMobLoot` | true | 不改；false 时 VO-LT 附加掉落一并停止 | — | — |
| `minecraft:block_drops` | `doTileDrops` | true | 不改 | — | — |
| `minecraft:spread_vines` | `doVinesSpread` | true | 不改 | — | — |
| `minecraft:players_sleeping_percentage` | `playersSleepingPercentage` | 100 | 不改 | — | VO-WX-11 |

> 本家族**不新增**游戏规则；所有可调项都放在各模组的配置文件中（02 §9），避免与原版规则命名空间混淆。26.1 起游戏规则保存在世界目录 `data/minecraft/game_rules.dat`，`vanilla_rules.dat` 只记录本家族写入过的条目。

---

## 7. 稀缺材料与替代来源

星球模式（或怪物稀缺）下，许多原版配方的原料来源消失。本节保证：**写实路线（不装 Beyond）下，所有“非奇幻”原版功能方块都能制作**；奇幻材料按设计只来自 Beyond（DR-06）。

替代配方由核心内置数据包 `tcivilization:monster_scarcity_substitutes` 提供：

- 配方**始终加载**（在原版怪物世界中只是多一条途径），因为数据包加载时还无法得知有效怪物策略（§4.4）。
- 由 common 配置 `tcivilization-common.toml#vanilla.scarcity_substitutes`（默认 true）整体关闭。
- 依赖其他模组物品的配方带 `neoforge:mod_loaded` 条件；依赖通用标签的配方带 `neoforge:not` + `neoforge:tag_empty` 条件。

### 7.1 怪物来源材料

| 原版材料 | 原版主要来源 | 稀缺时的替代来源 | 提供者 | 状态 |
| --- | --- | --- | --- | --- |
| 线 `string` | 蜘蛛 | ① `#minecraft:wool` ×1 → 线 ×3（无序；比原版 4 线 → 1 羊毛有损耗，不形成循环收益）；② `tctechnology:plant_fiber` ×3 → 线 ×1；③ 亚麻、大麻：Industry 在场时由纺锤或走锭纺纱机纺成 `tcindustry:thread`（在 `#c:strings` 中，13 §8.3）；Industry 缺失时 Cultivation 提供工作台配方 亚麻或大麻 ×2 → `minecraft:string` ×1（12 §16） | tcivilization、tctechnology、tccultivation、tcindustry | ①② 本章定稿；③ 定稿（12 §16、13 §8.3） |
| 骨头 `bone` | 骷髅 | ① 原版钓鱼垃圾（不改）；② Ecology 在场时兽穴 `tcecology:animal_den` 的战利品含骨头 ×3-8，并向 `#tcivilization:bone_bearing` 追加体型 M 及以上的野生哺乳类（马鹿、驯鹿、原牛、野猪等；11 §6.3）；③ 有效策略 ≠ `VANILLA` 时（无论 Ecology 是否在场），GLM `tcivilization:scarcity_bones` 让 `#tcivilization:bone_bearing` 中的动物（原版：牛、猪、羊、马、驴、骡、山羊、羊驼、骆驼；03 §16）有 35% 掉落 1 根骨头，抢夺每级 +10% | tcivilization（tcecology） | ②③ 定稿（03 §16、11 §6.3） |
| 火药 `gunpowder` | 苦力怕、恶魂 | 星球模式：硝石 + 硫磺 + 木炭（10 `tcwildlands:fallback/gunpowder_from_niter`，只在 `tcindustry` 未加载时注册；Industry 在场时由 13 的火药碾 `tcindustry:powder_mill` 代替：`saltpeter` ×6 + 木炭 ×1 + `sulfur` ×1 → 火药 ×8，`renaissance/gunpowder`） | tcwildlands、tcindustry | 定稿（10 §8.4、13） |
| 黏液球 `slime_ball` | 史莱姆 | ① 拴绳替代配方：线 ×5（无黏液球）；② 粘性活塞替代配方：活塞 + 蜂蜜瓶（无序）；③ 黏液块只能用真黏液球（Beyond 或刷怪笼） | tcivilization | ①② 本章定稿（13 不提供胶，已核对） |
| 腐肉、蜘蛛眼 | 僵尸、蜘蛛 | 不提供替代；腐肉无关键用途，蜘蛛眼只用于酿造（VO-MG-03） | — | 定稿 |
| 墨囊 | 鱿鱼（水生，不是怪物） | 不受怪物策略影响；Ecology 按账本分布鱿鱼；另有木炭 ×2 → 黑色染料 | tcecology、tcivilization | 本章定稿 |
| 幻翼膜 | 幻翼 | 不提供替代（鞘翅修复只用经验修补）；Beyond 在场时幻翼在影响区生成 | tcbeyond | 定稿 |
| 末影珍珠、烈焰棒、恶魂之泪、潜影壳、海晶碎片、不死图腾、鞘翅 | 末影人、烈焰人、恶魂、潜影贝、守卫者、唤魔者、末地城 | **只来自 Beyond**（DR-06、DR-16）；因此写实路线没有末影箱、潜影盒、酿造、信标中的部分材料（VO-MG） | tcbeyond | 定稿（设计取舍） |

### 7.2 下界来源材料（深地模式）

| 原版材料 | 用途 | 星球/深地模式来源 | 状态 |
| --- | --- | --- | --- |
| 下界石英 `quartz` | 比较器、侦测器、阳光探测器、石英建材 | Wildlands 热液石英脉：`pegmatite`、`quartzite` 以 10% / 5% 概率掉落原版 `minecraft:quartz`（10 §9.1、§16.1 VO-OQ-08） | 定稿（10） |
| 荧石粉 `glowstone_dust` | 红石灯、荧石、药水 | 不提供原版替代；Industry 电灯（`tcindustry:incandescent_lamp`、`fluorescent_lamp`、`led_lamp`，E7-E9，13 §6.7）代替照明；Beyond 深地余烬空洞与 `ashen_reach` 提供原版荧石（21 §16） | 定稿（21、13 §6.7） |
| 玄武岩、黑石 | 建材 | Wildlands 不产出原版 `minecraft:basalt`、`minecraft:blackstone`；建材用途由 `basalt_rock` 系列代替，`basalt_rock_cobble` 与全部 `_cobble` 一样加入 `#minecraft:stone_crafting_materials`、`#minecraft:stone_tool_materials`（10 §8.1）。原版方块来源：Beyond 的 `tcbeyond:ashen_reach`（玄武岩三角洲，21 §5.7.2），或非深地模式的原版下界 | 定稿（10 OQ-WLD-13、21） |
| 下界疣、灵魂沙、岩浆块、下界岩 | 酿造、气泡柱、建材 | 只来自 Beyond `ashen_reach`（下界疣另有帷花基底替代，VO-MG-03） | 定稿（21） |
| 远古残骸、下界合金 | 顶级装备 | 只来自 Beyond `ashen_reach` | 定稿（21） |

### 7.3 末地来源材料

紫珀、末地石、紫颂果、鞘翅、潜影壳、龙息、龙蛋：只来自界阈（Beyond）。Beyond 缺失时原版末地仍可用命令进入（VO-DM-06）。

---

## 8. 第三方模组兼容

| 类别 | 规则 |
| --- | --- |
| Sodium / Iris / 光影 | 客户端视觉改动优先使用 `DimensionSpecialEffects`、`ColorResolver`、NeoForge 渲染事件，不 mixin 渲染管线；MX-04 必须在 Iris + Complementary Reimagined 下通过日长与天空验收（VO-AC-12） |
| JEI / EMI | 由 `tctechnology` 插件显示锁定配方与“已移除”说明（§4.5）；被移除的原版配方在 JEI/EMI 中隐藏 |
| 小地图模组（Xaero、JourneyMap 等） | 不主动禁用；Atlas 的 `hide_vanilla_coordinates` 开启时，第三方小地图坐标不受控制，在 22 章兼容说明中提示服务器管理员。原版定位栏关闭不影响第三方小地图 |
| 世界生成模组（Terralith、Tectonic 等） | 与 `replace_overworld = true` 不兼容（星球生成器不读取原版噪声设置）；检测到时在日志与世界创建界面提示。`replace_overworld = false` 时可共存，Wildlands 降级 |
| 结构模组 | 星球模式下第三方结构只有在其群系标签包含 Wildlands 群系时才会出现（VO-ST-18）；Wildlands 群系加入 `c:is_*` 通用群系标签以提高兼容性 |
| 生物模组 | 第三方 `MONSTER` 类别生物同样受 §3.4 过滤；需要豁免的放入 `#tcivilization:monster_policy/exempt`。第三方被动生物不受 Ecology 移除（Ecology 只移除原版条目，11 §16） |
| 农业模组（Farmer's Delight 等） | Cultivation 只替换原版种子（VO-IT-11），第三方作物不受影响 |
| 科技模组（Create 等） | 不受影响；原版配方门槛只覆盖本章列出的原版配方，第三方配方不加门槛 |
| 其他改原版游戏规则的模组 | 遵守 §4.3：管理员或其他模组改过的规则不被覆盖 |

---

## 9. 验收标准

| 编号 | 验收 |
| --- | --- |
| VO-AC-01 | 完整整合包新建星球世界，`/tc vanilla status` 显示：星球模式、深地模式、界阈模式均为是；怪物策略 `BEYOND`；袭击与巡逻由 tcfrontiers 关闭；`minecraft:raids` 与 `minecraft:spawn_patrols` 为 false |
| VO-AC-02 | 只装 `tcivilization` + `tcwildlands`：新世界主世界夜晚 20 分钟内，影响区外（无 Beyond 时为全部地表）没有自然生成的原版怪物；地牢刷怪笼与试炼刷怪笼照常工作；`/summon zombie` 与刷怪蛋可用 |
| VO-AC-03 | 只装两个核心、原版世界预设：怪物、袭击、村庄、流浪商人均与原版一致；`/tc vanilla status` 显示有效策略 `VANILLA`，判定依据为“auto 且非星球模式” |
| VO-AC-04 | 管理员在首次加载后执行 `/gamerule minecraft:raids true` 并重启：规则保持 true，日志出现一次 `WARN`，OP 收到一次提示；再次重启不再提示 |
| VO-AC-05 | `/tcdebug vanilla restore all` 后，`vanilla_rules.dat` 中的所有规则恢复为 `previous_value`，记录被清空 |
| VO-AC-06 | 星球模式 200 个随机出生点各生成 512×512 区域：不出现村庄、灾厄前哨、林地府邸、要塞、海底神殿、远古城市、废弃矿井、丛林神庙、冰屋、女巫小屋；沙漠神殿、沉船、海底废墟、古迹废墟至少各出现一次（按群系出现率统计） |
| VO-AC-07 | 原版世界预设 + Settlements：512×512 平原区域无原版村庄；用刷怪蛋生成村民，下一刻已变为 `tcivilization:person` |
| VO-AC-08 | 星球模式，用黑曜石框架与打火石点火：不出现下界传送门，玩家收到一次提示；`deep_threshold` 可进入深地 |
| VO-AC-09 | Technology 安装、Industry 与 Frontiers 未安装：未完成 `iron/bloomery` 时原版铁镐配方在配方书与 JEI 中显示锁定及科技名；完成后可合成 |
| VO-AC-10 | Discovery 未安装、Technology 与 Atlas 已安装：望远镜配方门槛为 `renaissance/celestial_navigation`；安装 Discovery 后门槛变为 `renaissance/telescope`，且只有一个门槛生效 |
| VO-AC-11 | 写实路线（完整整合包去掉 Beyond）中，比较器、侦测器、粘性活塞、拴绳、书与笔、TNT、望远镜、指南针替代品均可在生存模式中制作（JEI 中可找到完整获取链） |
| VO-AC-12 | Iris + Complementary Reimagined：星球模式高纬度夏季白昼明显长于低纬度；天空、太阳位置与光影一致，无闪烁；关闭光影后同样正确 |
| VO-AC-13 | 星球模式 `minecraft:fire_spread_radius_around_player = 0` 时，点燃森林后火焰不蔓延；恢复默认后山火按模型蔓延且单次不超过 4,000 个火焰方块 |
| VO-AC-14 | `beyond.mode = mundane`：有效策略为 `RUINS_ONLY`，执行者显示为 tcivilization；Beyond 不重复过滤（日志中只出现一处过滤器注册） |
| VO-AC-15 | 加入 `#tcivilization:monster_policy/exempt` 的第三方怪物在 `RUINS_ONLY` 下正常自然生成 |
| VO-AC-16 | 关闭 `tcivilization-common.toml#vanilla.scarcity_substitutes` 后，§7 的替代配方从配方书与 JEI 中消失，骨头 GLM 停止附加 |
| VO-AC-17 | 两个模组对同一系统以同一优先级登记时，开发构建启动失败并指出冲突双方 |

---

## 10. 开放问题与核对清单

### 10.1 开放问题

| 编号 | 问题 | 本章建议 | 需要谁决定 |
| --- | --- | --- | --- |
| VO-OQ-01 | `beyond.mode = mundane` 时怪物过滤由谁执行？ | **已解决**：由核心回退门执行（§3.4.1 第 2 行），Beyond 只登记“mundane 时让出 `MONSTER_SPAWNS`”，只有一套过滤器（21 §5.1.2、§16） | 21 章（已修改） |
| VO-OQ-02 | 星球上是否保留试炼密室？它是原版内容中较“游戏化”的结构 | 保留，背景由 Echoes 解释为“古代试炼场”；它是写实路线中少数可获得旋风棒、重锤的来源 | 用户 / 18 章 |
| VO-OQ-03 | 原版盔甲（皮革、锁链、金、钻石）是否按时代门控？ | 皮革 E0 不门控；锁链 `bronze/alloying`；金、铁 `iron/bloomery`；钻石 `industrial/machine_tools`（与钻石工具一致）。由 04 章写入后备表 | 04 章 / 用户 |
| VO-OQ-04 | 星球模式无 Beyond 时，原版末地没有自然入口，编年功绩 `first_end`、`dragon_slain` 不可达 | 接受（DR-16：奇幻内容只走 Beyond）；编年史在这种组合下隐藏这两项功绩（已写入 03 OQ-03-07） | 用户 |
| VO-OQ-05 | 星球模式无 Beyond 时没有酿造（无烈焰棒、下界疣） | 接受。DR-19：家族不为玩家提供药水替代——Cultivation 不提供玩家药效（12 §16），Settlements 的 `herbal_poultice` 等医疗物品只对 NPC 有效（15 §5.9.5、§7），Skills 的 `medicine` 只作用于 NPC（05 §16）；玩家生命恢复靠原版饱食自然回复 | 定稿（用户 DR-19；12 §16、15 §7 已核对） |
| VO-OQ-06 | 15 章 `vanilla.suppress_villages` 位于 server 配置，但实现为数据包覆写，加载时序不成立（§4.4） | **已解决**：改为内置数据包 `tcsettlements:vanilla_overrides`，server 键改为“不一致时提示”（15 §13、§16、AC-14） | 15 章（已修改） |
| VO-OQ-07 | 深地模式下进入深地会触发原版进度 `story/enter_the_nether` 与编年功绩 `tcivilization:first_nether`（“炽热之门”），名称与深地不符 | 编年功绩在深地模式下显示为“深地之门”（同一 ID，名称由 `VanillaPolicyService.isDeepEarthMode` 选择）；原版进度不改（已写入 03 OQ-03-06） | 03 章（已修改） |
| VO-OQ-08 | 下界石英、荧石在写实路线中的来源（§7.2） | 石英由 Wildlands 热液石英脉产出原版 ID；荧石不替代 | 10 章 |

### 10.2 待确认核对清单

以下章节完成 §16 后，按本表逐项核对；不一致时以本章为准修改该章，或在本章登记例外。

| 章节 | 核对项 |
| --- | --- |
| 05 Skills | VO-CB-06、VO-PL-03、VO-PL-04：技能只用属性修饰符与只读监听；`hooks.vanilla_fallback` 的事件清单。**已核对**：05 §15.2、§16 逐项回答；VO-CB-06 改为“无伤害加成、不给玩家本体加属性修饰”，VO-PL-04 定稿 |
| 10 Wildlands | VO-WG-01~13、VO-ST-07~16、VO-SP-10、VO-WX-01~10、VO-FR-01~02、VO-IT-04、MX-01~04 的实现方式；`fire_spread_radius_around_player` 与积雪规则；矿物经验数值；石英来源（VO-OQ-08）；火药回退配方 ID；`replace_overworld`/`replace_nether` 位于 common 配置。**已核对**：10 §16.1-16.7 逐行回答，本章各行已改为定稿（10 §18.1 C-2~C-9） |
| 11 Ecology | `spawn_mobs = false` 时的账本行为；尸骸骨头来源（§7.1）。**已核对**：11 §16 增加 `spawn_mobs` 行；Ecology 不做尸骸分解，骨头改由 11 §6.3 向 `#tcivilization:bone_bearing` 追加物种，核心 GLM 不再要求 Ecology 缺失（03 §16 同步） |
| 12 Cultivation | VO-IT-01、VO-IT-11、VO-RC-14、VO-WX-07；亚麻/大麻纺线；草药能否替代药水（VO-OQ-05）。**已核对**：12 §16 逐行回答（VO-IT-01、VO-RC-14、纺线、VO-OQ-05：不替代） |
| 13 Industry | VO-RC-02、VO-RC-12、VO-IT-15；锻造台与钻石工具替代品；粘性活塞胶料；火药工坊；荧石替代照明。**已核对**：13 §8.14、§13、§15；13 不提供胶，胶标签从 §7.1 删去；锻造台门槛补入 13 §8.14；C-2 已解决 |
| 15 Settlements | VO-OQ-06（已完成） |
| 03 Core Civilization | `vanilla.monster_policy`、`vanilla.scarcity_substitutes`、`/tc vanilla` 命令、§16 改动表、AC-03-19~21、OQ-03-06/07（已完成） |
| 16 Exchange | VO-TR-01 配置键名；VO-VL-05。**已核对**：16 §16 |
| 18 Echoes | 已完成（18 §16、OQ-ECH-7）。剩余：Echoes 缺席时 Wildlands 是否恢复矿井标签（转 10 章） |
| 19 Commons | 原版出生点保护（`server.properties` 的 `spawn-protection`）与 PvP 开关和 Commons 领地权限的关系；本章暂不登记改动 |
| 21 Beyond | 已完成（21 §13、§16；VO-IT-14 不采用，见 BQ-6；§7.2、§7.3 材料来源由 21 §16 确认）。剩余：`#minecraft:brewing_fuel` 是否为 26.x 实际存在的标签（21 BQ-10） |
| 24 Orbit | VO-SP-15、VO-DM-05。**已核对**：24 §5、§16 |
