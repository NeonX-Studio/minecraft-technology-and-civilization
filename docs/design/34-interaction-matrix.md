# 34 · 联动矩阵 Interaction Matrix

> **本文件由 `tools/design/aggregate.py` 自动生成，请勿手工编辑。**
> 生成时间：2026-11-05 13:14
> 修改来源：各模组章节末尾的附录 A-D；重新生成：`python tools/design/aggregate.py`

本章汇总各模组附录 C 的联动登记。按 [02 §1.4](02-architecture.md)，未在此登记的跨模组联动视为缺陷；每条联动都必须由双方章节登记。

## 1. 读法

- 行 = 登记方，列 = 对方。单元格数字 = 登记方在附录 C 中关于对方的条目数。
- `●` 双方都已登记；`◐` 只有本行模组登记，对方缺登记；空白 = 无联动。
- 方向在第 4 节按绝对方向展示：`A → B` 表示 A 的状态或产物影响 B。

## 2. 矩阵

| 登记方 \ 对方 | CIV | TEC | SKL | WLD | ECO | CUL | IND | NET | SET | EXC | DIS | ECH | COM | FRO | BEY | ATL | NAT | ORB |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **CIV** 文明核心 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **TEC** 科技核心 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **SKL** 技能 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **WLD** 山河 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **ECO** 生息 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **CUL** 田园 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **IND** 工坊 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **NET** 通途 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **SET** 聚落 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **EXC** 市集 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **DIS** 求知 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **ECH** 遗响 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 |
| **COM** 契约 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 | ●1 |
| **FRO** 远征 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 | ●1 |
| **BEY** 异界 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 | ●1 |
| **ATL** 舆图 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 | ●1 |
| **NAT** 列国 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — | ●1 |
| **ORB** 寰宇 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | ●1 | — |

缩写：CIV = `tcivilization`，TEC = `tctechnology`，SKL = `tcskills`，WLD = `tcwildlands`，ECO = `tcecology`，CUL = `tccultivation`，IND = `tcindustry`，NET = `tcnetworks`，SET = `tcsettlements`，EXC = `tcexchange`，DIS = `tcdiscovery`，ECH = `tcechoes`，COM = `tccommons`，FRO = `tcfrontiers`，BEY = `tcbeyond`，ATL = `tcatlas`，NAT = `tcnations`，ORB = `tcorbit`

## 3. 对称性检查

全部联动均已双向登记。

## 4. 联动明细（按模组对）

### 4.1 文明核心 `tcivilization` ↔ 科技核心 `tctechnology`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ TEC | 对方写入文明时代并发布时代事件；本模组显示时代徽章、记录时代跃迁记忆、提供概览卡位与 tctechnology:tech_tree 页签 | 03-core-civilization.md:1703 |
| TEC | TEC ↔ CIV | 本模组写入 Civilization.era、里程碑记录与 MemoryEntry；使用核心的 J 面板页签、HUD 注册、通知、深链接、目标信息、TrackerApi、AlertProvider 与共享控件 | 04-core-technology.md:1179 |

### 4.2 文明核心 `tcivilization` ↔ 技能 `tcskills`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ SKL | 本模组的 ProficiencyBridge 转发全家族熟练度调用；对方注册 sink、tcskills:skills 页签与人物检视“技能与师承”分节 | 03-core-civilization.md:1704 |
| SKL | SKL ↔ CIV | 对方的 ProficiencyBridge 转发全家族熟练度调用并发放核心自身挂钩；本模组注册 sink、tcskills:skills 页签、proficiency_feed HUD 控件、tcskills:growth 通知类别、人物检视“技能与师承”分节、轮盘“个人”项与技能目标追踪 | 05-skills.md:2233 |

### 4.3 文明核心 `tcivilization` ↔ 山河 `tcwildlands`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ WLD | 对方提供 geo/climate/hydro 层、正式季节事件与 earthlike 环绕；本模组提供地区网格、日历与缺失时的季节回退 | 03-core-civilization.md:1705 |
| WLD | WLD ↔ CIV | 核心提供区域层框架、日历、HUD/面板/警报/通知/追踪/指令轮盘入口与 TcWorldApis 安装点；本模组注册 geo/climate/hydro 三层，安装 PlanetApi、ClimateApi、WeatherApi、GeoApi、HydroApi、SuitabilityApi、HazardApi、ProspectingApi 与 PlanetProgressStages，提供 LocalSeasonProvider 并发布 SeasonChangedEvent；订阅 EmissionRecordedEvent 驱动气候（3.1）。缺失时：不可能（核心必需）；本模组缺失时核心用平坦默认实现（纬度 45°N） | 10-wildlands.md:3627 |

### 4.4 文明核心 `tcivilization` ↔ 生息 `tcecology`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ ECO | 对方提供 ecosystem 层与采伐压力分量；本模组发布地区负载压力事件并记录物种灭绝记忆 | 03-core-civilization.md:1706 |
| ECO | ECO ↔ CIV | 本模组注册 ecosystem 地区层与 tcecology:harvest 压力分量、生态页签与 HUD 控件；核心发布 RegionLoadPressureEvent 降低野生动物增长率，并把物种灭绝/恢复写入编年史 | 11-ecology.md:1454 |

### 4.5 文明核心 `tcivilization` ↔ 田园 `tccultivation`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ← CUL | 对方使用 agri 层与 person 农夫动作集，并写入首次收获等记忆 | 03-core-civilization.md:1707 |
| CUL | CUL ↔ CIV | 本模组提供 agri 区域层、农夫/牧人动作集、首次收获与第一粮仓编年史记忆；使用 CivPermissions 动作、页签、HUD、通知、轮盘与准星信息接口 | 12-cultivation.md:1889 |

### 4.6 文明核心 `tcivilization` ↔ 工坊 `tcindustry`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ IND | 对方提供 emission 层、污染压力分量、机器告警与目标信息；本模组提供 HUD 与告警框架 | 03-core-civilization.md:1708 |
| IND | IND ↔ CIV | 本模组注册并写入地区附加层 tcindustry:emission（§11.4），按 L2 周期汇总后作为核心污染压力的分量；CO₂ 经核心 EmissionLedger.record 记账（03 §12.5）；为机器注册 TargetInfoProvider、为持续问题注册 AlertProvider、一次性事件走 TcNotifications，并发布 §12.2 的事件供编年史记录里程碑。核心提供共享人物 tcivilization:person 与职业槽接口、HUD 与提醒框架、面板页签 tcindustry:production（经济组，排序 450）与 HUD 情境控件 tcindustry:network_status（扩展等级，右中）的宿主、L1/L2 调度器（本模组只实现 IndustryAbstractSimulator） | 13-industry.md:3627 |

### 4.7 文明核心 `tcivilization` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ NET | 对方提供道路/铁路寻路权重与 CrewComponent；本模组提供地区图远程寻路与 Z 轮盘车辆指令入口 | 03-core-civilization.md:1709 |
| NET | NET ↔ CIV | 本模组向人物附加 CrewComponent 并提供道路/铁路寻路权重；核心提供 TCPathExecutor 地区图远程寻路、Z 指令轮盘车辆指令入口、文明面板页签与 HUD 注册；人物乘坐载具的乘客关系在抽象化时保留 | 14-networks.md:1641 |

### 4.8 文明核心 `tcivilization` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ SET | 对方通过聚落登记 API 触发聚落事件并提供 ResidentComponent；本模组提供人物实体、需求与日程框架、实体化预算 | 03-core-civilization.md:1710 |
| SET | SET ← CIV | SettlementRecord、PersonRecord 与 tcivilization:person 实体；日程、需求、对话、寻路框架；区域层、文明面板、HUD、指令轮盘、警报与通知外壳 | 15-settlements.md:1486 |

### 4.9 文明核心 `tcivilization` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ← EXC | 对方提供 MerchantComponent、market 层、对话交易动作与人物检视“交易”分节 | 03-core-civilization.md:1711 |
| EXC | EXC → CIV | 提供 MerchantComponent、market 区域层、对话交易动作与人物“交易”分节、VuValuation；使用人物、编年史、通知、权限、面板、HUD | 16-exchange.md:1734 |

### 4.10 文明核心 `tcivilization` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ DIS | 对方观察模式接管 target_info 并注册 journal 页签；本模组提供 TargetInfoProvider 数据 | 03-core-civilization.md:1712 |
| DIS | DIS ↔ CIV | 核心提供 J 面板、人物 tcivilization:person（学者组件挂载处）、HUD/目标信息/警报/通知/深度链接注册表与 MemoryRecordedEvent；本模组注册日志标签页、HUD 控件与 TargetInfoProvider | 17-discovery.md:1313 |

### 4.11 文明核心 `tcivilization` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ ECH | 对方写入世界历史并订阅记忆事件生成传说；本模组存储 WorldHistory 并在编年史中提供切换 | 03-core-civilization.md:1713 |
| ECH | ECH ↔ CIV | 核心存储 WorldHistory 与 HISTORIC 文明、提供 J 面板/编年史切换/记忆事件；本模组生成历史、注册页签与记忆类型 | 18-echoes.md:1605 |

### 4.12 文明核心 `tcivilization` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ COM | 对方解释成员角色权限并提供 claim 层与公共工程指令；本模组委托命令权限检查 | 03-core-civilization.md:1714 |
| COM | COM ↔ CIV | 实现 CivPermissions 领地与角色解析器；读取成员、人物、区域层、编年史、追踪与通知外壳 | 19-commons.md:1467 |

### 4.13 文明核心 `tcivilization` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ← FRO | 对方提供 BanditComponent、danger 层、威胁告警与编队轮盘指令 | 03-core-civilization.md:1715 |
| FRO | FRO ↔ CIV | 使用 person 实体与组件实现盗匪、民兵、佣兵；注册地区附加层 tcfrontiers:danger；页签、HUD、警报、深链接接入核心 | 20-frontiers.md:1438 |

### 4.14 文明核心 `tcivilization` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ← BEY | 对方提供 anomaly 层与异质渗透目标信息，并写入影响区记忆 | 03-core-civilization.md:1716 |
| BEY | BEY ↔ CIV | 地区层 tcbeyond:anomaly；HUD、页签、通知、深链、轮盘、目标信息；VanillaPolicyService 拥有者（MONSTER_SPAWNS、PHANTOMS、END、RAIDS、PATROLS）；ThresholdPresence；编年史记忆 | 21-beyond.md:1637 |

### 4.15 文明核心 `tcivilization` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ ATL | 对方提供时代皮肤、布局编辑器与 H 键；本模组提供发现位集、中性皮肤与 HUD 组件注册表 | 03-core-civilization.md:1717 |
| ATL | ATL ↔ CIV | 核心提供 HudWidgetRegistry、CivPanelTabRegistry、区域层、DeepLinkRegistry、指令轮盘、TrackerApi、AlertProvider、TcNotifications、UiSkin 接口与 hud_layout.json；Atlas 提供 4 个 HUD 控件、测绘标签、布局编辑器、时代皮肤 tcatlas:skin/<era> 与 H 键（核心在 Atlas 缺失时自行注册） | 22-atlas.md:1825 |

### 4.16 文明核心 `tcivilization` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ↔ NAT | 对方创建 AI 文明并提供 SoldierComponent 与 influence 层；本模组提供文明记录、L3 调度与条约记忆 | 03-core-civilization.md:1718 |
| NAT | NAT ↔ CIV | 创建 AI Civilization；人物组件；tcnations:influence 区域层；外交页签与概览卡；通知、警报、指令轮盘条目；文明记忆记录条约与战争 | 23-nations.md:1208 |

### 4.17 文明核心 `tcivilization` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CIV | CIV ← ORB | 对方为月球与火星注册地区网格并提供 CrewComponent，写入航天记忆 | 03-core-civilization.md:1719 |
| ORB | ORB → CIV | 月球、火星 RegionGridSpec；页签、HUD 控件、通知类别 tcorbit:space；里程碑 first_satellite、mars_self_sufficiency | 24-orbit.md:1773 |

### 4.18 科技核心 `tctechnology` ↔ 技能 `tcskills`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC → SKL | 打制、生火、狩猎、研究、宣布时代产生熟练度（附录 D）；经 ProficiencyBridge.multiplier 读取 crafting、hunting、foraging、scholarship 的 05 倍率作效率加成（§15） | 04-core-technology.md:1180 |
| SKL | SKL ↔ TEC | 对方的打制、生火、狩猎、研究、宣布时代发放熟练度，并提供当前时代与 EraAdvancedEvent；本模组以时代作天赋门槛并在时代晋升时发反思令，crafting、scholarship 等级/乘数用于对方 ≤ +25% 的效率加成 | 05-skills.md:2234 |

### 4.19 科技核心 `tctechnology` ↔ 山河 `tcwildlands`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← WLD | 燧石结核与沉积岩充实 #tctechnology:knappable / #tctechnology:soft_rock；湿度与降雨影响钻木与火把束熄灭；大陆 ID 用于里程碑判定 | 04-core-technology.md:1181 |
| WLD | WLD ↔ TEC | 科技状态决定敲击、预报、仪器读数的信息精度（附录 B）；本模组注册并触发 tcwildlands:identify_rock、tcwildlands:record_hazard 两个知识触发器（3.2）；燧石结核与沉积岩充实 #tctechnology:knappable/#tctechnology:soft_rock，湿度与降雨影响钻木取火与火把熄灭，大陆 ID 供里程碑判定（3.2、04 附录 C）。缺失时：信息精度固定为 E5 等价 | 10-wildlands.md:3628 |

### 4.20 科技核心 `tctechnology` ↔ 生息 `tcecology`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ ECO | Ecology 物种为狩猎目标并产生 kill_entity 知识、鹿类提供鹿角；本模组的火塘威慑降低捕食者接近意愿；firebrand 可点燃 Ecology 的计划烧除 | 04-core-technology.md:1182 |
| ECO | ECO ↔ TEC | 物种认知提供知识值；15 项生态科技归属科技树 | 11-ecology.md:1452 |

### 4.21 科技核心 `tctechnology` ↔ 田园 `tccultivation`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ CUL | 本模组提供 E1 镰与锄（收割其作物种子 +1）；Cultivation 的 neolithic/cultivation、neolithic/animal_husbandry 为 E1 关键科技，缺失时由本模组替身与后备配方门槛接管 | 04-core-technology.md:1183 |
| CUL | CUL ↔ TEC | 本模组定义 33 项农业科技（含 E1 关键科技 neolithic/cultivation、neolithic/animal_husbandry）与 first_granary 判定器；使用其燧石镰、石锄、plant_fiber、cordage、配方门控与原版物品门控表 | 12-cultivation.md:1890 |

### 4.22 科技核心 `tctechnology` ↔ 工坊 `tcindustry`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ IND | Industry 自 E2 接管全部工具并引用 #tctechnology:cordage、flint_scraper；其生产线经 electric/corporate_rnd 向研究池贡献 RP；缺失时本模组恢复原版熔炉与工具配方 | 04-core-technology.md:1184 |
| IND | IND ↔ TEC | Technology 提供科技树、required_tech 门槛、配方门槛数据格式与实践知识框架；本模组拥有 71 项科技（附录 B）并注册触发器 tcindustry:process_completed、tcindustry:machine_formed、tcindustry:energy_produced、tcindustry:quality_reached（§6.12）。本模组自冷锻铜起接管全部金属工具（04 写作“自 E2”，见 C-9），引用 #tctechnology:cordage、#c:strings 与 tctechnology:flint_scraper（鞣制刮皮）；工业生产线经 electric/corporate_rnd 向研究池贡献 RP。本模组缺失时 Technology 恢复原版熔炉与原版工具配方 | 13-industry.md:3628 |

### 4.23 科技核心 `tctechnology` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← NET | 道路、干线铁路、洲际航线等时代里程碑由 Networks 判定（MilestoneChecker）；information/computer_networks 为协作研究前置 | 04-core-technology.md:1185 |
| NET | NET ↔ TEC | 本模组 37 项科技与 6 项关键科技；运量、建设线路、首次驾驶作为实践来源；四个交通里程碑条件 | 14-networks.md:1650 |

### 4.24 科技核心 `tctechnology` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ SET | 族人参与火塘议事、担任研究员、产生实践知识；部落火塘是研究地点；本模组读取人口门槛并以 craft_lineage 开放教师岗位 | 04-core-technology.md:1186 |
| SET | SET ↔ TEC | 科技解锁本模组方块、职业、房间与设施；本模组提供学者 RP、毕业生知识阈值加成、时代跃迁人口下限、火塘研究地点 | 15-settlements.md:1487 |

### 4.25 科技核心 `tctechnology` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← EXC | 贸易往来构成扩散接触（ContactProvider）；学术赞助按 VU 计价；renaissance/banking 为学术赞助前置 | 04-core-technology.md:1187 |
| EXC | EXC → TEC | 19 项科技（含关键科技 coinage/guilds/banking）；贸易往来作为扩散接触（ContactProvider）；学术赞助按 VU 计价；实践触发器 | 16-exchange.md:1735 |

### 4.26 科技核心 `tctechnology` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ DIS | Discovery 提供观察/实验/书籍知识来源与各时代研究设施（ResearchSite）；其存在时研究台停用；本模组提供科技框架与 TechApi | 04-core-technology.md:1188 |
| DIS | DIS → TEC | 观察、夜谈、实验、书籍通过 TechApi.addKnowledge 与触发器提供知识；设施实现 ResearchSite 产出 RP；学术接触实现 ContactProvider；拥有 4 项关键科技 | 17-discovery.md:1314 |

### 4.27 科技核心 `tctechnology` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ ECH | 遗迹线索提供 ECHO 成本折扣与古文物（器物考察）；本模组的时代跃迁写入编年史 | 04-core-technology.md:1189 |
| ECH | ECH ↔ TEC | 04 提供科技树与 addKnowledge；本模组注册 7 项科技、ECHO 触发器与 #tcechoes:lost_knowledge | 18-echoes.md:1606 |

### 4.28 科技核心 `tctechnology` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← COM | 宣布新时代的权限 tccommons:declare_era 与投票（取消 EraAdvanceEvent.Pre）；律法石碑里程碑判定 | 04-core-technology.md:1190 |
| COM | COM ↔ TEC | declare_era 权限与 EraAdvanceEvent.Pre 投票；law_stele 判定 stele_of_law 里程碑；18 项科技 | 19-commons.md:1468 |

### 4.29 科技核心 `tctechnology` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← FRO | E2 起武器由 Frontiers 提供；缺失时本模组按时代恢复原版弓、弩、剑配方 | 04-core-technology.md:1191 |
| FRO | FRO ↔ TEC | 本模组 20 项科技；E0-E1 狩猎矛与简易弓作为战斗基线；时代决定危险分量权重与遭遇表 | 20-frontiers.md:1439 |

### 4.30 科技核心 `tctechnology` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← BEY | Beyond 的 8 项异界科技使用本框架但不参与时代关键科技 | 04-core-technology.md:1192 |
| BEY | BEY ↔ TEC | 8 项 space 泳道支线科技；知识触发器（§5.11）；tc_recipe_gate | 21-beyond.md:1638 |

### 4.31 科技核心 `tctechnology` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC → ATL | 负载 tctechnology:era_advanced 通知 Atlas 切换时代皮肤；HUD 布局编辑器摆放 research_progress、era_progress | 04-core-technology.md:1193 |
| ATL | ATL ↔ TEC | Technology 门控 14 项 Atlas 科技；Atlas 提供实践知识来源（6.9）与 TechUnlockDescriber 描述 | 22-atlas.md:1826 |

### 4.32 科技核心 `tctechnology` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ↔ NAT | Nations 驱动 AI 文明研究选择、提供外交接触与“交换学者”动作；本模组提供 diffusionPressure、TechApi.grant 与 AI 文明科技存档 | 04-core-technology.md:1194 |
| NAT | NAT ↔ TEC | AI 科技状态存储与研究决策；接触/使馆/交流产生扩散压力；本模组 8 项科技 | 23-nations.md:1209 |

### 4.33 科技核心 `tctechnology` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| TEC | TEC ← ORB | 首颗人造卫星等 E9→E10 里程碑；space/orbital_assembly 为知识方舟前置；缺失时启用 comm_station 后备里程碑 | 04-core-technology.md:1195 |
| ORB | ORB → TEC | 附录 B 的 24 项科技（含 5 项关键科技） | 24-orbit.md:1774 |

### 4.34 技能 `tcskills` ↔ 山河 `tcwildlands`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ WLD | 对方的勘探、敲击识别、群系发现发放 mining/surveying/exploration，预期接管 mine_ore 回退挂钩；本模组提供 mining、surveying 乘数与 prospect、strata_read op | 05-skills.md:2235 |
| WLD | WLD ↔ SKL | 采矿、敲击、淘洗、发现经 ProficiencyBridge.add 发放 mining/surveying/exploration/construction；接管 mine_ore 回退挂钩；应用 yield/ore，读取 prospect、strata_read op 与等级（3.3、15 节）。缺失时：发放为空操作，等级视为 0、倍率 1 | 10-wildlands.md:3629 |

### 4.35 技能 `tcskills` ↔ 生息 `tcecology`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ ECO | 对方的狩猎、追踪、陷阱、采集、林业发放 hunting/foraging/woodcutting，预期接管 fell_log、forage、hunt_animal、fish；本模组的等级与 op 影响追踪半径、逃跑距离、陷阱效率、野生植物掉落 | 05-skills.md:2236 |
| ECO | ECO ↔ SKL | 狩猎、追踪、观察等行为产生熟练度；技能等级影响追踪半径、逃跑距离与陷阱效率 | 11-ecology.md:1453 |

### 4.36 技能 `tcskills` ↔ 田园 `tccultivation`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ CUL | 对方的耕作、育种、畜牧、食品加工发放 farming/husbandry/cooking，预期接管对应回退挂钩；本模组提供 yield/crop、speed/breeding、quality/food 与育种、储藏 op | 05-skills.md:2237 |
| CUL | CUL ↔ SKL | 本模组发放 farming/husbandry/cooking 等熟练度并接管对应原版回退挂钩；读取 yield/crop、speed/crop、speed/breeding、yield/animal_product、capacity/herd、quality/food 与 6 个 op | 12-cultivation.md:1891 |

### 4.37 技能 `tcskills` ↔ 工坊 `tcindustry`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ IND | 对方的手作工序、冶炼、锻造、机器装配与维修发放 crafting/smithing/engineering，预期接管 smelt_metal、smithing_table、redstone_place；本模组提供手工速度、品级、维修与诊断相关乘数和 op | 05-skills.md:2238 |
| IND | IND ↔ SKL | 本模组的手工工序、冶炼、锻造、机器装配与检修发放 crafting、smithing、engineering 等熟练度（§15、附录 D），并以同名来源 tcindustry:smelt_metal、tcindustry:smithing_table、tcindustry:redstone_place 接管 05 的回退挂钩；Skills 提供乘数 speed/handcraft、cost/handcraft、quality/tool、quality/metal/weapon、cost/fuel、wear/machine、speed/assembly 与 op cast_yield、alloy_insight、masterwork、diagnose_depth、network_insight、overhaul（masterwork 解释为 Q3 → Q4，C-8） | 13-industry.md:3629 |

### 4.38 技能 `tcskills` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ NET | 对方的驾驶、调度、筑路、勘测、飞行发放 logistics/engineering/construction/surveying/exploration/aeronautics/husbandry，预期接管 ride、elytra；本模组的 logistics/aeronautics 决定玩家驾驶 crewFactor | 05-skills.md:2239 |
| NET | NET → SKL | 驾驶、调度、筑路、勘测、航海、飞行提供 logistics、engineering、construction、surveying、exploration、aeronautics、husbandry 熟练度 | 14-networks.md:1651 |

### 4.39 技能 `tcskills` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ SET | 对方的建造、指挥、救治发放 construction/leadership/medicine/scholarship/surveying 并提供职业 NPC 用于拜师，预期接管 build、cure_villager；本模组的 leadership、medicine 提高居民效率与治疗效果 | 05-skills.md:2240 |
| SET | SET ↔ SKL | 本模组发放 construction、leadership、medicine、scholarship、surveying 熟练度；leadership 与 medicine 等级反向提高居民效率与治疗效果 | 15-settlements.md:1488 |

### 4.40 技能 `tcskills` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ EXC | 对方的交易、议价、商队发放 trade/diplomacy/logistics/leadership，预期接管 trade、first_trade；本模组提供 price/trade 倍率（≤ ±15%）与商队容量 | 05-skills.md:2241 |
| EXC | EXC ↔ SKL | trade、diplomacy、logistics、leadership 经验；读取 trade 价格倍率与商队容量 | 16-exchange.md:1736 |

### 4.41 技能 `tcskills` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ DIS | 对方的观察、实验、写作、发掘、标本发放熟练度，预期接管 enchant、sign_book；本模组提供 precision/experiment 与 observe_progress op | 05-skills.md:2242 |
| DIS | DIS → SKL | 观察、实验、写作、发掘、标本等行为发放熟练度（附录 D） | 17-discovery.md:1329 |

### 4.42 技能 `tcskills` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ ECH | 对方的遗迹解读与传说线索发放 exploration/scholarship；本模组提供 yield/loot、range/reveal 与 ruin_lore op | 05-skills.md:2243 |
| ECH | ECH → SKL | exploration、scholarship 经验（附录 D） | 18-echoes.md:1621 |

### 4.43 技能 `tcskills` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ COM | 对方的治理、投票、公共工程发放 leadership/diplomacy/construction；本模组提供 influence/morale、capacity/command 与 mediation、civic_aura op | 05-skills.md:2244 |
| COM | COM → SKL | leadership、diplomacy、construction 熟练度；读取倍率 | 19-commons.md:1469 |

### 4.44 技能 `tcskills` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ FRO | 对方的远征、防御战、侦察、补给发放 combat/exploration/leadership/logistics/surveying/medicine 等；本模组提供命中散布、装填、士气、补给损耗、侦察精度、NPC 救治相关乘数与 op | 05-skills.md:2245 |
| FRO | FRO → SKL | combat、exploration、leadership、logistics 等 12 项熟练度的获得与效果（附录 D） | 20-frontiers.md:1440 |

### 4.45 技能 `tcskills` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ BEY | 对方的异界行为是 arcana 的唯一来源，装载后 arcana 可见；本模组提供 arcana 天赋树与 precision/ritual、cost/anomaly 乘数 | 05-skills.md:2246 |
| BEY | BEY → SKL | arcana 全部来源；读取 precision/ritual 与 cost/anomaly；天赋 anomaly_insight、ritual_echo | 21-beyond.md:1639 |

### 4.46 技能 `tcskills` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ ATL | 对方的测绘与地图绘制发放 exploration/surveying，HUD 布局编辑器托管 proficiency_feed，预期接管 map_fill、first_biome；本模组修正步测误差、仪器误差、记忆半衰期、草图半径（6.7.4） | 05-skills.md:2247 |
| ATL | ATL ↔ SKL | Atlas 发放 exploration/surveying 等经验；Skills 修正步测误差、仪器误差、记忆半衰期、草图半径 | 22-atlas.md:1827 |

### 4.47 技能 `tcskills` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ NAT | 对方的谈判、条约、战争、朝贡发放 diplomacy/leadership/combat/trade/exploration/logistics；本模组提供 influence/relation、speed/negotiation 与 envoy_slots、treaty_slots op | 05-skills.md:2248 |
| NAT | NAT → SKL | 外交、领导、战斗、贸易、探索、后勤熟练度来源 | 23-nations.md:1223 |

### 4.48 技能 `tcskills` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SKL | SKL ↔ ORB | 对方的发射、轨道任务、航天器装配、地外开采发放 aeronautics/engineering/scholarship/mining；本模组提供 precision/flight/launch、cost/propellant 与 eva_efficiency op | 05-skills.md:2249 |
| ORB | ORB → SKL | 附录 D 的熟练度、乘数与 eva_efficiency 消费 | 24-orbit.md:1787 |

### 4.49 山河 `tcwildlands` ↔ 生息 `tcecology`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ ECO | 本模组提供群系 ID、气候包络、季节天气、山火洪水火山灰区域，群系只留 vegetation_slot；Ecology 回报植被覆盖与生物量（反照率、蒸散、燃料载量）及砍伐面积（3.4）。缺失时：VanillaVegetationFallback 注入原版植被（16.1 VO-WG-06） | 10-wildlands.md:3630 |
| ECO | ECO ↔ WLD | 读取气候/水文/岩性决定适宜度、洄游与酸缓冲；向对方提供森林与湿地覆盖用于径流和地方气候；山火洪水重置演替 | 11-ecology.md:1438 |

### 4.50 山河 `tcwildlands` ↔ 田园 `tccultivation`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ CUL | 本模组提供土壤母质 soilParent、ClimateApi.sample、SeasonChangedEvent、flood_silt 与 droughtIndex；Cultivation 回报取水量、排干与盐渍化（3.5、12 附录 C）。缺失时：原版作物按 16.4 的季节与温度系数生长 | 10-wildlands.md:3631 |
| CUL | CUL ↔ WLD | 读取母质、质地、气候、河段流量（HydroApi）初始化土壤与配水；回报取水量、排干与盐渍化供其水文与地貌使用 | 12-cultivation.md:1892 |

### 4.51 山河 `tcwildlands` ↔ 工坊 `tcindustry`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ IND | 本模组提供全部原矿、煤、岩石、quartz_sand、kaolinite、brine、thermal_water，油气/地热储层经 GeoApi.reservoirAt/extract/heatFlow；Industry 注册 tcindustry:crude_oil、tcindustry:natural_gas、tcindustry:salt，经核心 EmissionLedger 记账排放，排水泵、通风机对接矿井涌水与稳定性（3.6）。缺失时：启用 8.4 原版熔炉回退配方，油气只显示“油苗” | 10-wildlands.md:3632 |
| IND | IND ↔ WLD | Wildlands 提供原矿 tcwildlands:raw_<mineral>、岩石与煤种，地质层 tcwildlands:geo 中的原油、天然气、卤水、地热储层与深部钻探数据，hydro/climate 层的河流流量与风速（决定水车、风车、风力机出力）；本模组注册流体 tcindustry:crude_oil、tcindustry:natural_gas，开采时经 Wildlands API 写回储层余量，水坝通知 hydro 层下游流量变化；Wildlands 的原矿在本模组缺席时使用其自带的原版熔炉回退配方 | 13-industry.md:3630 |

### 4.52 山河 `tcwildlands` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → NET | SuitabilityApi.terrainCost 供路线造价；flow_field、海流与 WeatherApi.wind 作用于船只与帆船；DisasterStartedEvent 让 Networks 判定线路损坏（3.7）。缺失时：数据无人消费，本模组不受影响 | 10-wildlands.md:3633 |
| NET | NET ← WLD | 地形、植被、湿地、冻土图层生成地形代价网格；风场与洋流决定帆船航速；河流水深决定通航；洪水、滑坡、风暴、暴雪损毁或关闭线路 | 14-networks.md:1636 |

### 4.53 山河 `tcwildlands` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → SET | citySiteScore 选址分项；灾害物理影响（淹没深度、烈度、火势、积雪荷载）与 HazardApi.forecast 供疏散与预警岗位；日长与气温影响 NPC 日程和取暖（3.8）。缺失时：灾害只作用于方块与原版生物 | 10-wildlands.md:3634 |
| SET | SET ← WLD | climate 与 hydro 层用于选址评分；日照决定作息；季节影响疫病传染率；地下水深度决定水井；灾害毁房触发重识别 | 15-settlements.md:1489 |

### 4.54 山河 `tcwildlands` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → EXC | GeoApi.depositSummary 的剩余储量（扣除古代开采）决定地区矿产基准价与比较优势；灾害事件冲击受灾区价格（3.9）。缺失时：无影响 | 10-wildlands.md:3635 |
| EXC | EXC ← WLD | 矿床与资源分布决定比较优势；金属来源与采矿灾害冲击价格 | 16-exchange.md:1737 |

### 4.55 山河 `tcwildlands` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → DIS | 岩石、矿物、找矿标志、气象与地质构造登记为观察目标并提供 TrueValueProvider（星球周长、海拔等）；敲击与淘洗结果写入其日志；fossil_bed 为标本点（3.10）。缺失时：结果写入地理页签“勘探记录”，观察信息由核心 target_info 显示名称 | 10-wildlands.md:3636 |
| DIS | DIS ← WLD | 地质层、气候、海拔与星球周长作为观察目标与 TrueValueProvider；沉积岩化石 | 17-discovery.md:1317 |

### 4.56 山河 `tcwildlands` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ ECH | 本模组提供地质、气候、矿体与选址输入；Echoes 经 recordHistoricalTrace 提交古断崖、古火山灰层、古洪水台地与 ANCIENT_WORKINGS（按 minedFraction × tonnage 扣减储量），以 ancient_mine 代替原版矿井，并以 PlanetProgressStages（order 1000）接入进度画面（3.11、12.1）。缺失时：无历史痕迹、储量不扣减，内置数据包恢复原版矿井（16.1 VO-ST-07） | 10-wildlands.md:3637 |
| ECH | ECH ↔ WLD | 10 提供地质/气候/矿体输入；本模组提交 HistoricalTrace 并以 ancient_mine 代替原版矿井 | 18-echoes.md:1608 |

### 4.57 山河 `tcwildlands` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ COM | Commons 立项的堤坝、防火带、防波堤、抗震加固与矿井支护经 HazardApi.registerMitigation 降低灾害影响；本模组提供灾害记录供赈灾政策（3.12）。缺失时：玩家自建工程按物理规则同样生效 | 10-wildlands.md:3638 |
| COM | COM ↔ WLD | 堤坝、防火带、防波堤、抗震加固与矿井支护工程；赈灾政策 | 19-commons.md:1479 |

### 4.58 山河 `tcwildlands` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → FRO | HazardApi.hazardLevel、崎岖度、极端气候与水障构成危险度地形分量与远征补给系数；季节修正；灾害难民进入盗匪招募池（3.13）。缺失时：无影响 | 10-wildlands.md:3639 |
| FRO | FRO ← WLD | 崎岖度、极端气候、水障构成地形分量；季节修正；灾害难民进入盗匪招募池 | 20-frontiers.md:1441 |

### 4.59 山河 `tcwildlands` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ BEY | 本模组提供深地 5 个群系、坐标、heatFlow 与断层数据，星球模式决定 tcbeyond:ashen_reach 与核心怪物策略；Beyond 以群系修改器在深地余烬空洞补充敌对生物（3.14、16.1 VO-SP-10）。缺失时：深地没有敌对生物 | 10-wildlands.md:3640 |
| BEY | BEY ← WLD | 热流与纬度决定焦点候选；深地余烬空洞；星球模式决定 ashen_reach 与核心怪物策略 | 21-beyond.md:1640 |

### 4.60 山河 `tcwildlands` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → ATL | 全部仪器读数来自 ProspectingApi.query；专题图层取自三个区域层；PlanetApi.latLon、altitudeMeters、地形预览与磁偏角供坐标、经纬网和罗盘（3.15）。缺失时：只能用 2.3 的感官找矿，ProspectingApi 仍可被其他模组调用 | 10-wildlands.md:3641 |
| ATL | ATL ← WLD | 星球模型、TerrainPreviewApi 预览、地质/气候/水文/洋流/天气图层、地名、磁铁矿偏角 | 22-atlas.md:1828 |

### 4.61 山河 `tcwildlands` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD → NAT | SuitabilityApi 与 depositSummary 驱动 AI 扩张与资源争夺，边界倾向分水岭与大河；DisasterEndedEvent 统计削弱 AI 人口与粮食（3.16）。缺失时：无影响 | 10-wildlands.md:3642 |
| NAT | NAT ← WLD | 可居住度与天然边界决定播种与扩张；灾害削弱 AI 人口 | 23-nations.md:1217 |

### 4.62 山河 `tcwildlands` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| WLD | WLD ↔ ORB | 本模组提供 1,024 格分辨率的全球气候与地质栅格供遥感卫星，星球模型供发射纬度；本模组科技 space/planetary_geology 让 Orbit 样本获得矿物识别（3.17）。缺失时：无影响 | 10-wildlands.md:3643 |
| ORB | ORB ↔ WLD | 对地观测读取气候与地质快照；样本显示矿物；发射纬度使用星球模型 | 24-orbit.md:1782 |

### 4.63 生息 `tcecology` ↔ 田园 `tccultivation`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ CUL | 野生祖先与野生作物是驯化起点；提供授粉、害虫捕食与农田取食压力；读取开垦、排干与过度放牧 | 11-ecology.md:1439 |
| CUL | CUL ↔ ECO | 野生祖先与野生作物是驯化起点；读取授粉、天敌与取食压力；回报开垦、排干、过度放牧与野化种群（EcologyApi.introduce）；tree_tap 用其枫木 | 12-cultivation.md:1893 |

### 4.64 生息 `tcecology` ↔ 工坊 `tcindustry`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ IND | 读取排放计算毒性与生物放大；提供木材、矮林细杆、胶乳、鲸脂、毛皮原料；鱼道依托水坝 | 11-ecology.md:1440 |
| IND | IND ↔ ECO | 本模组的排放层（SO₂、NOx、颗粒物、重金属、放射性、热污染）供 Ecology 计算毒性、生物放大与水体鱼类影响；Ecology 提供木材、矮林细杆 tcecology:coppice_pole、胶乳 tcecology:latex（#c:latex，橡胶硫化原料）、鲸脂 tcecology:blubber（熬成 tallow，§8.3）与毛皮 tcecology:fur_pelt（视同生皮鞣制，§8.3）；鱼道 tcecology:fish_ladder 依托本模组的水坝（hydro_turbine 坝体与 sluice_gate）和水车堰；可经 MachineWearModifier 注入腐蚀磨损 | 13-industry.md:3631 |

### 4.65 生息 `tcecology` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ← NET | 道路铁路密度构成迁徙障碍与路杀；船运引起外来种扩散 | 11-ecology.md:1441 |
| NET | NET ↔ ECO | 道路与铁路切割栖息地、动物通道恢复连通；载具撞击计入非自然死亡；远洋航线引入入侵物种；交通燃料消耗计入污染 | 14-networks.md:1637 |

### 4.66 生息 `tcecology` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ SET | NPC 猎人樵夫渔夫通过收获 API 消耗种群；大型捕食者威胁事件；鼠类疾病宿主系数 | 11-ecology.md:1442 |
| SET | SET ↔ ECO | 物种丰度决定狩猎采集产出；鼠类密度驱动瘟疫输入；本模组上报人口压力与排污口污染点；捕鼠笼降低鼠类种群 | 15-settlements.md:1490 |

### 4.67 生息 `tcecology` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO → EXC | 毛皮、象牙、鲸脂、木材价格随种群丰度变化；保护政策下禁售 | 11-ecology.md:1446 |
| EXC | EXC ← ECO | 毛皮、象牙、鲸脂、木材价格随种群丰度变化；保护政策下禁售 | 16-exchange.md:1738 |

### 4.68 生息 `tcecology` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ DIS | 观察模式显示生态信息；样本进入实验设施；图鉴同步为观察日志 | 11-ecology.md:1444 |
| DIS | DIS ← ECO | 物种作为观察与标本对象；种群数量真值；鸟类图谱套组 | 17-discovery.md:1318 |

### 4.69 生息 `tcecology` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ ECH | 古文明灭绝记录与冻土遗骸样本；玩家导致的灭绝与复原写入传说 | 11-ecology.md:1447 |
| ECH | ECH ↔ ECO | 11 提供物种与灭绝事件；本模组提供历史灭绝与遗存物种分布 | 18-echoes.md:1609 |

### 4.70 生息 `tcecology` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ COM | 保护界碑注册为禁猎区/保护区法律区；狩猎季与配额政策；EHI 低时提供环境治理工程模板 | 11-ecology.md:1443 |
| COM | COM ↔ ECO | 禁猎区、保护林、自然保护区；猎季与捕捞配额读取种群 | 19-commons.md:1473 |

### 4.71 生息 `tcecology` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO → FRO | 大型捕食者密度贡献危险等级；远征狩猎补给按种群计算 | 11-ecology.md:1448 |
| FRO | FRO ↔ ECO | 掠食者生物量构成生态分量；凶兽以 notoriety 附件挂在 Ecology 动物上；猎杀掠食者的生态后果由 Ecology 结算 | 20-frontiers.md:1442 |

### 4.72 生息 `tcecology` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ← BEY | 异界影响区承载力 -50% 与逃离行为；怪物生成规则归对方 | 11-ecology.md:1450 |
| BEY | BEY ↔ ECO | 承载力 −50%；WildlifeEmbodyEvent 在 I ≥ 60 取消实体化 | 21-beyond.md:1641 |

### 4.73 生息 `tcecology` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO → ATL | 物种分布、迁徙路线与要冲、演替、EHI、保护区专题图层；项圈个体标记 | 11-ecology.md:1445 |
| ATL | ATL ← ECO | 生态健康、物种分布图层；生态考察点作为注释 | 22-atlas.md:1829 |

### 4.74 生息 `tcecology` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ NAT | AI 文明在 L3 消耗种群；跨境迁徙物种保护条约；生态信誉影响 AI 态度 | 11-ecology.md:1449 |
| NAT | NAT ↔ ECO | AI 人口消耗种群账本；林地守护者议程；迁徙物种保护条约条款 | 23-nations.md:1218 |

### 4.75 生息 `tcecology` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECO | ECO ↔ ORB | 卫星生态监测 ±3%；生物圈工程为封闭栖息地提供物种与循环效率 | 11-ecology.md:1451 |
| ORB | ORB ↔ ECO | 种群普查（±3%）与森林覆盖率；闭合生态效率用于温室舱 | 24-orbit.md:1783 |

### 4.76 田园 `tccultivation` ↔ 工坊 `tcindustry`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ IND | 使用其石磨、罐头机、冷藏机、犁铧、鞣制与纺线机器，并为 milling、canning、chemical_reaction 提供食物与农药配方；向其提供纤维、蚕茧、油料、wood_ash、wool_grade/hide_grade 与化肥施用效果 | 12-cultivation.md:1894 |
| IND | IND ↔ CUL | Cultivation 使用本模组的石磨 millstone 与鞍形磨盘、罐装机 canning_machine、冷藏机 refrigeration_unit、犁铧 iron_plough_share/steel_plough_share（c:plough_shares）、镰刀（c:tools/sickle）、鞣制坑 tanning_pit 与纺线机器，并在配方类型 tcindustry:milling、tcindustry:canning、tcindustry:chemical_reaction 中登记面粉、罐头与农药配方（OQ-2、OQ-4）；Cultivation 提供亚麻、棉花、羊毛、麻、蚕茧、皮张、油料、淀粉、甘蔗与 tccultivation:wood_ash（C-5），以及 wool_grade/hide_grade 组件（C-6）；本模组产出的化肥 c:fertilizers/nitrogen\|phosphorus\|potassium 的施用效果由 Cultivation 定义（OQ-1） | 13-industry.md:3632 |

### 4.77 田园 `tccultivation` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ NET | 登记役畜牵引参数与 tractionMultiplier；易腐货物在途按保鲜等级继续腐败；农场与粮仓作为订单来源；干草满足 tcnetworks:fodder | 12-cultivation.md:1895 |
| NET | NET ↔ CUL | 役畜品种性状决定牵引参数；易腐货物运输中继续腐败、冷藏载具降速腐败；农场与粮仓作为订单来源点 | 14-networks.md:1638 |

### 4.78 田园 `tccultivation` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ SET | 储藏方块经 FoodStorageProvider 计入粮食储备；农田与畜栏经 WorkplaceProvider 成为岗位；营养组决定食物多样性；suitability_mean 用于选址 | 12-cultivation.md:1896 |
| SET | SET ↔ CUL | 农田与畜栏注册为工作地；营养组决定食物多样性；储藏方块经 FoodStorageProvider 计入粮食储备；本模组提供农夫与牧人劳动力 | 15-settlements.md:1491 |

### 4.79 田园 `tccultivation` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ EXC | 特产 provenance 即其 origin_region（品质档 = tier − 1）与季节供给；田块页显示 ExchangeApi.price 本地市价 | 12-cultivation.md:1897 |
| EXC | EXC ↔ CUL | 特产 origin_region 与品质溢价、季节供给；本模组向其提供本地市价 | 16-exchange.md:1739 |

### 4.80 田园 `tccultivation` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ DIS | 提供 soil_fertility、heredity_ratio 实验真值与纸浆原料；农业岗位效率乘其采纳度；种质库实验写入 seed_vault | 12-cultivation.md:1898 |
| DIS | DIS ↔ CUL | 作物与施肥、遗传实验真值；造纸原料；实验结果提高农业 NPC 采纳度 | 17-discovery.md:1319 |

### 4.81 田园 `tccultivation` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ ECH | 其古种子罐战利品携带遗产品种 SeedLot；本模组向其提供古代品种与驯化起源记录 | 12-cultivation.md:1899 |
| ECH | ECH ↔ CUL | 12 提供古代品种；本模组提供古种子罐与驯化起源传说 | 18-echoes.md:1618 |

### 4.82 田园 `tccultivation` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ COM | 读取 tccommons:gmo_policy、WaterRightsProvider、休耕与禁牧政策、水费；向其提供取水计量（5.5.5）与 gm = true 品种的种植面积 | 12-cultivation.md:1900 |
| COM | COM ↔ CUL | gmo_policy、WaterRightsProvider、休耕与禁牧、水费 | 19-commons.md:1474 |

### 4.83 田园 `tccultivation` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ FRO | 提供口粮（c:foods）、驮兽、战马与草料；盗匪目标选择读取储粮账本 | 12-cultivation.md:1901 |
| FRO | FRO ↔ CUL | 口粮包封装 c:foods；驮兽、战马、草料来自 Cultivation；盗匪袭击优先瞄准粮仓与畜群 | 20-frontiers.md:1443 |

### 4.84 田园 `tccultivation` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ BEY | 提供 GrowthModifier 接口（翠华护符、异界生长减缓）；异化作物掉落 Beyond 的 tcbeyond:twisted_produce（5.18） | 12-cultivation.md:1902 |
| BEY | BEY ↔ CUL | 生长减缓、异化作物、翠华护符与翠华酊（BQ-8） | 21-beyond.md:1642 |

### 4.85 田园 `tccultivation` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL → ATL | 提供土壤、作物适宜性与农业图层 tccultivation:agriculture | 12-cultivation.md:1903 |
| ATL | ATL ← CUL | 土壤与作物适宜性图层 | 22-atlas.md:1830 |

### 4.86 田园 `tccultivation` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL → NAT | 提供 AI 文明粮食产量（L3）、饥荒事件与粮食援助所需的账本数据 | 12-cultivation.md:1904 |
| NAT | NAT ← CUL | AI 粮食产量；粮食援助作为礼物；饥荒提高扩张倾向 | 23-nations.md:1219 |

### 4.87 田园 `tccultivation` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| CUL | CUL ↔ ORB | 洗净火星土/月壤可改良为耕地；greenhouseYield 供温室舱计算作物与氧气产量；航天口粮营养 | 12-cultivation.md:1905 |
| ORB | ORB ↔ CUL | 洗净火星土可耕作；温室舱作物产量；航天口粮营养 | 24-orbit.md:1779 |

### 4.88 工坊 `tcindustry` ↔ 通途 `tcnetworks`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ NET | Networks 经 c: 标签读取本模组的燃料、钢轨钢材、发动机与电动机组件；电气化铁路的接触网与牵引变电所作为电网负载经 IndustryEnergyApi.registerConsumer 取电（1 FE/tick = 100 W，§12.6）；长输管道 tcnetworks:pipeline 输送工业流体，交界为本模组装液臂 tcindustry:fluid_loading_arm；工业区缺料时本模组发布 IndustryProductionDemandEvent，玩家开启自动补料后 Networks 生成物流订单；装卸台 tcnetworks:loading_bay 向本模组机器暴露 IItemHandler；turbine_core 由 Networks 组装为喷气发动机 | 13-industry.md:3633 |
| NET | NET ↔ IND | 燃料、钢材、零件经 c: 标签读取；电力牵引接入电网；长输管道输送工业流体；缺料事件自动生成物流订单；装卸台向机器暴露物品接口 | 14-networks.md:1639 |

### 4.89 工坊 `tcindustry` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ SET | 本模组实现 Settlements 的 WorkplaceProvider，机器工位（工业区为一组）由居民填充，工人数、教育与熟练度影响机器速度；排放层降低居住质量并提高呼吸道疾病率；IndustryEnergyApi.isPowered 为 home_electrification 提供供电判定；区域电网验收 EC-P5-4 由本模组上报；核事故经 reportExposure 上报受照人数。Settlements 提供操作员与维护职业（陶工、织工、冶炼工、铁匠、磨坊工、司炉、机械工、电工、化学家、核工程师、技术员）；本模组提议新增制革匠、玻璃匠两种职业（OQ-3） | 13-industry.md:3634 |
| SET | SET ↔ IND | 机器经 WorkplaceProvider 成为岗位；排放降低居住质量并提高呼吸道疾病；电网判定入户电气化；本模组提供操作员与维护职业，核事故经 reportExposure 上报 | 15-settlements.md:1492 |

### 4.90 工坊 `tcindustry` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ EXC | 本模组为机器与产品提供基准价值（VU）、产能预估（IndustryAbstractSimulator）与电价依据，工业区可挂销售订单进入市场仓库，IndustryProductionDemandEvent 生成采购订单；Exchange 给出工业品、原料与电力价格，结算工人工资，并提供价格趋势（生产页签显示“锡价上涨 18%，青铜成本 +12%”类提示） | 13-industry.md:3635 |
| EXC | EXC ↔ IND | 工业品与原料价格、电价；本模组结算工资并提供价格趋势 | 16-exchange.md:1740 |

### 4.91 工坊 `tcindustry` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ DIS | 本模组为 Discovery 提供动力、电力、石灰、金属、光学玻璃、精密黄铜、电子件与化学试剂，后期仪器作为电网负载接入；Discovery 的实验 effects 改善合金品质并解锁配方细节（如观察淬火颜色后炉窑界面显示温度刻度，§5.3.5）；观察模式读取机器经 TargetInfoProvider 扩展字段提供的转速、温度、磨损 | 13-industry.md:3636 |
| DIS | DIS ↔ IND | 提供动力、电力、石灰与金属；实验 effects 改善合金品质；后期仪器接入电网 | 17-discovery.md:1320 |

### 4.92 工坊 `tcindustry` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ ECH | 本模组发布 IndustryAccidentEvent、IndustryReactorStateChangedEvent 与 Q4 杰作（IndustryQualityRolledEvent）供活历史记录；Echoes 提供冶炼遗址 furnace_site 的修复（修复后为本模组多方块的“半成品”）、古矿渣 tcechoes:ancient_slag（c:slag，Echoes 数据包在本模组 tcindustry:smelting 中登记回收配方）与失传工艺（Q4 稀有配方） | 13-industry.md:3637 |
| ECH | ECH ↔ IND | 13 提供事故与杰作；本模组提供冶炼遗址修复与古矿渣 | 18-echoes.md:1619 |

### 4.93 工坊 `tcindustry` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ COM | Commons 提供分区（工业区、居住区）、排放标准与停产令、劳动与工业安全法规（含核安全法，监听 IndustryReactorStateChangedEvent）；大坝、发电站、区域电网可作为公共工程立项；本模组的事故（IndustryAccidentEvent）生成议案 | 13-industry.md:3638 |
| COM | COM ↔ IND | 分区、排放标准停产令、劳动与工业安全；事故生成提案 | 19-commons.md:1475 |

### 4.94 工坊 `tcindustry` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ FRO | Frontiers 的武器护甲使用本模组的钢、黄铜、火药、炸药、合金钢、钛与复合材料，并使用锻造砧 forging_anvil 等工作站（本模组不注册武器护甲）；军需订单进入工业区订单队列，装备修理在工作站排队；攻城与破坏增加机器磨损或直接 FAULT | 13-industry.md:3639 |
| FRO | FRO ↔ IND | 武器护甲使用 Industry 材料与工作站；军需订单与装备修理排队 | 20-frontiers.md:1444 |

### 4.95 工坊 `tcindustry` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ BEY | Beyond 经 MachineWearModifier 让异界影响区内机器磨损 ×1.5；余烬坩埚 tcbeyond:ember_crucible 经 IndustryApi.registerHeatSource 向正上方炉窑/锅炉供热 200 HU/s；Beyond 数据包在 E8 的电弧炉 electric_arc_furnace 与离心机配方中处理异质矿物，产物归 Beyond | 13-industry.md:3640 |
| BEY | BEY ↔ IND | MachineWearModifier；余烬坩埚供热；E8 电弧炉与离心机配方 | 21-beyond.md:1643 |

### 4.96 工坊 `tcindustry` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ ATL | 本模组为 Atlas 提供电网电力与零件（齿轮、铜线 copper_wire、电动机 electric_motor_small/_medium/_large）以及测绘仪器所需的精密黄铜与光学玻璃；Atlas 专题地图显示工业设施、电网线路、管道与排放热力图（读取 tcindustry:emission）；Atlas 缺失时生产页签用 TcMapSnippet 的坐标文字回退 | 13-industry.md:3641 |
| ATL | ATL ↔ IND | Industry 提供 W 电网与零件（齿轮、铜线、电动机）；Atlas 地图显示工业设施与排放图层 | 22-atlas.md:1831 |

### 4.97 工坊 `tcindustry` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND → NAT | Nations 以本模组 IndustryAbstractSimulator 的产能公式（§5.15）计算 AI 文明的工业等级，决定其装备、军需与贸易品；原油、铀、钨等战略资源的争夺可成为宣战理由；以工业设施为目标的战争由 Nations 决策、Frontiers 执行 | 13-industry.md:3642 |
| NAT | NAT ← IND | AI 工业等级影响装备；战略资源争夺成为宣战理由 | 23-nations.md:1220 |

### 4.98 工坊 `tcindustry` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| IND | IND ↔ ORB | Orbit 必需依赖本模组：本模组提供铝锂合金、钛合金、碳纤维、耐热陶瓷、集成电路、太阳能电池、RTG、燃料电池、推进剂流体（kerosene、liquid_hydrogen、liquid_oxygen、hypergolic_propellant；#tcorbit:propellants/methane 收录 natural_gas 与 liquefied_natural_gas）与电力；Orbit 提供月面金属渣 tcorbit:lunar_metal_slag、镍铁 tcorbit:nickel_iron_chunk、铂族精矿 tcorbit:pgm_concentrate 与氦-3 tcorbit:helium_3，由 Orbit 数据包在本模组的熔炼与电解配方类型中登记处理配方；燃烧类机器在无氧维度不运行 | 13-industry.md:3643 |
| ORB | ORB ↔ IND | Industry 提供材料、推进剂流体、电力与电子；本模组提供金属渣、镍铁、铂族精矿、氦-3（Q4） | 24-orbit.md:1775 |

### 4.99 通途 `tcnetworks` ↔ 聚落 `tcsettlements`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ SET | 交通岗位由聚落居民担任并获得 CrewComponent；可达性指数影响聚落选址与扩张；NPC 按路线规划施工；通勤客运提高满意度；客流数据用于疫病传播 | 14-networks.md:1640 |
| SET | SET ↔ NET | 道路提高通勤速度与通勤上限；疫病沿线路传播；车站与码头成为岗位；本模组提供客流与通勤需求 | 15-settlements.md:1493 |

### 4.100 通途 `tcnetworks` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ EXC | 商队实体化为真实车队船队；estimateFreight 运输成本约束区域价差；合同绑定物流订单；车票与运费以货币结算；共同判定海外贸易站里程碑 | 14-networks.md:1642 |
| EXC | EXC ↔ NET | 商队实体化为车队船队；estimateFreight 约束价差；合同绑定物流订单；车票运费以货币结算；共同判定海外贸易站里程碑 | 16-exchange.md:1741 |

### 4.101 通途 `tcnetworks` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ DIS | 信息网络系数加快知识传播；观察漂流木、滚木、海鸟获得交通科技线索 | 14-networks.md:1643 |
| DIS | DIS ← NET | 道路、邮路、电报与计算机网络加快科技采纳 | 17-discovery.md:1321 |

### 4.102 通途 `tcnetworks` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ← ECH | 古道、沉船、古港遗迹可修复并纳入路网；沉船内的古代货包 | 14-networks.md:1644 |
| ECH | ECH → NET | 古道、古港、驿站供修复；沉船货包 | 18-echoes.md:1611 |

### 4.103 通途 `tcnetworks` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ COM | 道路桥梁铁路作为公共工程立项；收费站与货运税；穿越领地的通行权与交通法规；TollPointEvent 与通行记录 | 14-networks.md:1645 |
| COM | COM ↔ NET | right_of_way、收费站、交通法规、禁飞区、道路桥梁铁路工程 | 19-commons.md:1476 |

### 4.104 通途 `tcnetworks` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ FRO | 强盗与海盗袭击商队、列车、船舶；护卫与护航；远征据点作为补给订单终点；WeaponMountApi 在车辆与船舶上挂载武器 | 14-networks.md:1646 |
| FRO | FRO ↔ NET | 路线伏击概率 ambushChance；道路降低偏远分量；WeaponMountApi 车辆与船挂载武器；补给线车队 | 20-frontiers.md:1445 |

### 4.105 通途 `tcnetworks` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ← BEY | 异界影响区内磁罗盘失灵、船舶与飞机航向偏转；航线规划绕开已知影响区 | 14-networks.md:1652 |
| BEY | BEY ↔ NET | 区内罗盘失灵与航线绕行；共鸣中继作为跨维度通信链路（BQ-8）；渡界石 | 21-beyond.md:1644 |

### 4.106 通途 `tcnetworks` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ ATL | 交通图层与线路预览交给 Atlas 绘制；海图降低航线风险；乘坐载具时路点导航显示下一站 | 14-networks.md:1647 |
| ATL | ATL ↔ NET | Networks 提供道路/铁路/航线要素、电报网、飞行器与船只、trail_cairn 作为定位点；Atlas 提供导线走廊 SL4、浅滩水深、路线规划与载具情境 HUD | 22-atlas.md:1832 |

### 4.107 通途 `tcnetworks` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET ↔ NAT | AI 文明在 L3 抽象修路开航线；过境条约与禁运；战争破坏桥梁与轨道 | 14-networks.md:1648 |
| NAT | NAT ↔ NET | 军队与使节沿道路、铁路移动；过境条约；战争破坏桥梁与轨道 | 23-nations.md:1215 |

### 4.108 通途 `tcnetworks` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NET | NET → ORB | 重型运输把火箭部件运抵发射场；磁浮线接入航天港 | 14-networks.md:1649 |
| ORB | ORB ↔ NET | Networks 运输部件、磁浮接驳；本模组提供 GEO 通信覆盖、月面车载具接口、relayLatencyTicks（Q2） | 24-orbit.md:1777 |

### 4.109 聚落 `tcsettlements` ↔ 市集 `tcexchange`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ↔ EXC | 工资计入繁荣度财富项，市集提供文化与食物多样性；本模组向其提供居民需求、繁荣度与商人劳动力 | 15-settlements.md:1494 |
| EXC | EXC ↔ SET | 居民需求与繁荣度、商人劳动力与市场分区；本模组提供工资（财富项）、市集文化与食物多样性 | 16-exchange.md:1742 |

### 4.110 聚落 `tcsettlements` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ↔ DIS | 书籍提高教学效率，医学知识解锁精确疫情数据，高等教育设施提供教育等级 3；本模组提供识字率与书吏、学者劳动力 | 15-settlements.md:1495 |
| DIS | DIS ↔ SET | 聚落提供学者受教育程度、识字率、疾病真值；本模组提供设施、图书馆、采纳度与博物馆文化建筑 | 17-discovery.md:1316 |

### 4.111 聚落 `tcsettlements` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET → ECH | 聚落成立、升级、大疫、首批毕业生写入文明记忆；遗弃聚落成为遗迹候选；CultureProvider 接收纪念物文化值 | 15-settlements.md:1496 |
| ECH | ECH ↔ SET | 15 提供聚落事件与博物馆；本模组提供 CultureProvider 文化值与遗产稳定度修正 | 18-echoes.md:1613 |

### 4.112 聚落 `tcsettlements` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ↔ COM | Commons 实现 DirectiveGate 并以法律与政策管理指令和移民；稳定度读取满意度并回馈修正；公共工程复用施工系统；领地边界与聚落边界同步；税基读取 demography 层 | 15-settlements.md:1497 |
| COM | COM ↔ SET | 实现 DirectiveGate、ClaimPermissionCheck；聚落辖区；法令覆盖聚落默认值；稳定度回馈满意度；税吏职业 | 19-commons.md:1470 |

### 4.113 聚落 `tcsettlements` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ↔ FRO | danger 层与守卫岗位决定安全需求；城墙识别为建筑类型；本模组提供 defensive_wall 方块标签；袭击伤亡经 reportCasualties 上报 | 15-settlements.md:1498 |
| FRO | FRO ↔ SET | 守卫岗位挂载 MilitiaComponent；警钟触发避难；失业与不满进入盗匪池；前哨 III 级可升级为聚落 | 20-frontiers.md:1446 |

### 4.114 聚落 `tcsettlements` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ← BEY | 异界渗透降低满意度并引入只作用于 NPC 的异质病 | 15-settlements.md:1499 |
| BEY | BEY ↔ SET | I ≥ 40 满意度 −10 与暴露上报；异术见闻（BQ-8）；镇界覆盖免除 | 21-beyond.md:1645 |

### 4.115 聚落 `tcsettlements` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET → ATL | demography 层人口图层；聚落图标、边界与人口标签显示在地图与罗盘条 | 15-settlements.md:1500 |
| ATL | ATL ↔ SET | 测量员/制图员职业、聚落要素与人口图层；Atlas 提供任务区与自动入库 | 22-atlas.md:1833 |

### 4.116 聚落 `tcsettlements` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ↔ NAT | AI 文明聚落使用本模组人口与需求公式（L3 聚合）；移民可来自 AI 文明；战争伤亡上报；人口结构用于兵源 | 15-settlements.md:1501 |
| NAT | NAT ↔ SET | AI 聚落按聚落规则模拟；兵源来自人口结构；移民与难民；占领状态 | 23-nations.md:1214 |

### 4.117 聚落 `tcsettlements` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| SET | SET ← ORB | space/life_support 为封闭栖息地前置；栖息舱成为太空住房；生命维持故障进入公共卫生事件 | 15-settlements.md:1502 |
| ORB | ORB → SET | 岗位（宇航员、任务控制员、殖民者）；isPressurized；reportExposure；火星殖民地是聚落 | 24-orbit.md:1778 |

### 4.118 市集 `tcexchange` ↔ 求知 `tcdiscovery`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC → DIS | 贸易路线带来接触传播；书籍与期刊订阅可交易 | 16-exchange.md:1743 |
| DIS | DIS ← EXC | 贸易路线带来接触传播；书籍与期刊订阅可交易 | 17-discovery.md:1322 |

### 4.119 市集 `tcexchange` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ↔ ECH | 本模组定价古物并标记 unprovenanced；对方提供 B_artifact、年代、品相、出处与编年史 | 16-exchange.md:1744 |
| ECH | ECH ↔ EXC | 16 定价古物并标记 unprovenanced；本模组提供 B_artifact、年代、品相、出处 | 18-echoes.md:1614 |

### 4.120 市集 `tcexchange` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ↔ COM | 税率、关税、铸币税立法、价格管制与市场规则；本模组提供税基与国库账户 | 16-exchange.md:1745 |
| COM | COM ↔ EXC | 实现 TaxPolicyProvider；国库账户支出权；市场、货币、银行、行会、专利法令 | 19-commons.md:1471 |

### 4.121 市集 `tcexchange` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ↔ FRO | 伏击、劫掠与走私；悬赏、赎金、贡赋以货币与 VU 结算；贫困与过路财富驱动招募；军火价格 | 16-exchange.md:1747 |
| FRO | FRO ↔ EXC | 悬赏、赎金、贡赋以文明货币与 VU 结算；贫困与过路财富驱动招募；军火价格 | 20-frontiers.md:1447 |

### 4.122 市集 `tcexchange` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ← BEY | 超常物品估值与禁售；Beyond 渗透地区的贸易中断 | 16-exchange.md:1746 |
| BEY | BEY → EXC | 异质物品带“异质”标签进入市场 | 21-beyond.md:1646 |

### 4.123 市集 `tcexchange` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ↔ ATL | 地图作为商品（AtlasApi.mapValue）；市场价格图层与商路要素；贸易站地标 | 16-exchange.md:1748 |
| ATL | ATL ↔ EXC | 地图作为商品（AtlasApi.mapValue）；市场价格图层与商路要素 | 22-atlas.md:1834 |

### 4.124 市集 `tcexchange` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ↔ NAT | AI 货币与汇率；贸易协定关税；贡金赔款以 VU 记账；禁运；信任作为主权信用输入；主权违约事件 | 16-exchange.md:1749 |
| NAT | NAT ↔ EXC | AI 货币与汇率；贸易协定关税；贡金赔款以 VU 记账；禁运；信任作为主权信用输入；tradeDependency 降低宣战效用（§5.7）并判定 merchant_princes 议程（§6.3） | 23-nations.md:1213 |

### 4.125 市集 `tcexchange` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| EXC | EXC ↔ ORB | 发射服务、卫星租用、氦-3 与铂族金属定价；多星球结算（E10） | 16-exchange.md:1750 |
| ORB | ORB → EXC | 发射服务合同、卫星租用、氦-3 与铂族金属定价（6.4）、天体市场 bodyOf | 24-orbit.md:1780 |

### 4.126 求知 `tcdiscovery` ↔ 遗响 `tcechoes`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ↔ ECH | Echoes 实现 DigSiteProvider 与日志“遗迹与传说”分区；本模组提供发掘玩法、ArtefactExcavatedEvent、夜谈讲述传说 | 17-discovery.md:1315 |
| ECH | ECH ↔ DIS | 17 提供发掘工具、网格、unknown_artifact 与日志；本模组实现 DigSiteProvider、日志分区与 LegendToldEvent | 18-echoes.md:1607 |

### 4.127 求知 `tcdiscovery` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ← COM | 设施与藏书的使用权限 | 17-discovery.md:1326 |
| COM | COM ↔ DIS | copyright、academic_freedom；图书馆与大学工程；use_facility | 19-commons.md:1482 |

### 4.128 求知 `tcdiscovery` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ← FRO | 远征带回的远方标本与新区域首次观察 | 17-discovery.md:1327 |
| FRO | FRO → DIS | 战斗、远征、侦察产生知识点；远征队学者记录远方观察与标本 | 20-frontiers.md:1448 |

### 4.129 求知 `tcdiscovery` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ← BEY | 异常区作为特殊观察目标；超自然内容不计入科学方法 | 17-discovery.md:1328 |
| BEY | BEY ← DIS | 观察模式产生 anomaly 知识碎片，换算为本模组科技知识 | 21-beyond.md:1647 |

### 4.130 求知 `tcdiscovery` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ↔ ATL | 测绘与天文观察共享地理数据；本模组不注册测量仪器 | 17-discovery.md:1324 |
| ATL | ATL ↔ DIS | 纸、墨、印刷机批量印图；Atlas 提供观察对象位置注释与地图碑铭文 | 22-atlas.md:1835 |

### 4.131 求知 `tcdiscovery` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ↔ NAT | 学者交流协定、学术会议、奖项声望；AI 文明的书籍与知识传播 | 17-discovery.md:1323 |
| NAT | NAT → DIS | 与 AI 学者交流、文化交流产生知识；外交事件写入日志 | 23-nations.md:1221 |

### 4.132 求知 `tcdiscovery` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| DIS | DIS ← ORB | 发射太空望远镜载荷，提供深空观测 | 17-discovery.md:1325 |
| ORB | ORB → DIS | 科学卫星、太空望远镜（3 次/日）、轨道实验室与样本观察记录 | 24-orbit.md:1785 |

### 4.133 遗响 `tcechoes` ↔ 契约 `tccommons`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECH | ECH ↔ COM | 19 提供 heritage 区域、heritage_protection 与发掘许可；本模组提供遗产登记与权限动作 | 18-echoes.md:1615 |
| COM | COM ↔ ECH | heritage 区域与 heritage_protection；发掘许可 | 19-commons.md:1483 |

### 4.134 遗响 `tcechoes` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECH | ECH ↔ FRO | 20 读取历史分量并占据遗迹；本模组记录战役与匪患传说 | 18-echoes.md:1616 |
| FRO | FRO ↔ ECH | 历史分量与遗民守军来自 Echoes；盗匪占据遗迹；战役与首领伏诛写入历史 | 20-frontiers.md:1449 |

### 4.135 遗响 `tcechoes` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECH | ECH ↔ BEY | 21 读取 #tcechoes:cult_site 并在异界放置遗迹；本模组提供模板池与传说 | 18-echoes.md:1617 |
| BEY | BEY ↔ ECH | tcechoes:cult_site 成为碎塔焦点；大事件写入历史 | 21-beyond.md:1648 |

### 4.136 遗响 `tcechoes` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECH | ECH → ATL | 遗址要素、传闻圈、tcechoes:history 图层、古地图内容 | 18-echoes.md:1612 |
| ATL | ATL ← ECH | ancient_map 战利品、遗迹与历史图层、传闻级知识 | 22-atlas.md:1836 |

### 4.137 遗响 `tcechoes` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECH | ECH ↔ NAT | 23 用存续政体播种 AI 文明与宿怨；本模组接收覆灭事件并遗迹化 | 18-echoes.md:1610 |
| NAT | NAT ↔ ECH | 从世界历史播种 AI 文明；AI 兴衰、战争与条约写回世界历史与遗迹 | 23-nations.md:1210 |

### 4.138 遗响 `tcechoes` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ECH | ECH ← ORB | 24 的终局编年史书读取世界历史 | 18-echoes.md:1620 |
| ORB | ORB → ECH | 文明纪元编年史；hasRemoteSensing 支持轨道考古 | 24-orbit.md:1786 |

### 4.139 契约 `tccommons` ↔ 远征 `tcfrontiers`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| COM | COM ↔ FRO | 赦免、征兵、悬赏、持械、俘虏法令；siege_damage、fire_spread 标志；动乱；前哨宣称 | 19-commons.md:1478 |
| FRO | FRO ← COM | 赦免令、悬赏法、俘虏处置、领地 siege_damage 标志约束攻城与袭击 | 20-frontiers.md:1450 |

### 4.140 契约 `tccommons` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| COM | COM ↔ BEY | 异常区块禁宣称；ward_zone；arcane_practice 政策 | 19-commons.md:1480 |
| BEY | BEY ↔ COM | I ≥ 10 不可宣称；政策 tcbeyond:arcane_practice（禁止 / 容许）；镇界区 | 21-beyond.md:1649 |

### 4.141 契约 `tccommons` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| COM | COM ↔ ATL | claim 区域层渲染领地；boundary_stone 兼作界标；路标权限别名 | 19-commons.md:1477 |
| ATL | ATL ↔ COM | 领地与界碑、共享路点编辑权限、小组共享范围 | 22-atlas.md:1837 |

### 4.142 契约 `tccommons` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| COM | COM ↔ NAT | 政体亲和与法令态度修正；war_vote 取消宣战；条约边界写入领地；区域归属判定 | 19-commons.md:1472 |
| NAT | NAT ↔ COM | 边界条约生成领地边界；外交权限节点与投票；外交法律；Commons 领地不可被影响力夺走 | 23-nations.md:1211 |

### 4.143 契约 `tccommons` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| COM | COM ↔ ORB | 发射与殖民地权限默认角色；nuclear_thermal_ban；月面领地 | 19-commons.md:1481 |
| ORB | ORB ↔ COM | 权限节点 tcorbit:launch 等（19 §5.2.4 登记默认角色）；Commons 的 nuclear_thermal_ban 与月面领地约束本模组 | 24-orbit.md:1781 |

### 4.144 远征 `tcfrontiers` ↔ 异界 `tcbeyond`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| FRO | FRO ← BEY | 异界影响区强度构成危险度 anomaly 分量；影响区内怪物由 Beyond 生成；本模组禁用原版袭击与巡逻，与 Beyond 原版生成规则协调 | 20-frontiers.md:1451 |
| BEY | BEY → FRO | DangerContributor（ANOMALY）；巡逻与袭击由 Frontiers 优先接管，本模组回退 | 21-beyond.md:1650 |

### 4.145 远征 `tcfrontiers` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| FRO | FRO ↔ ATL | 危险度图层与盗匪情报标记；远征亲临带来实测测绘等级；瞭望警报显示在小地图 | 20-frontiers.md:1452 |
| ATL | ATL ↔ FRO | 危险区要素与危险图层；远征队携带地形图并回传测绘 | 22-atlas.md:1839 |

### 4.146 远征 `tcfrontiers` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| FRO | FRO ↔ NAT | 军队使用本模组武器、工事与攻城器械；CombatApi 抽象战斗与攻城；战后逃兵生成盗匪团；边境冲突提升危险度 | 20-frontiers.md:1453 |
| NAT | NAT ↔ FRO | 军队使用 Frontiers 武器与攻城器械；CombatApi 抽象结算；战争提高危险度并产生逃兵盗匪 | 23-nations.md:1212 |

### 4.147 远征 `tcfrontiers` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| FRO | FRO ↔ ORB | 轨道碎片、太阳粒子事件、火星沙尘暴折算为远征风险；月面远征复用远征规划器与 SupplyApi | 20-frontiers.md:1454 |
| ORB | ORB ↔ FRO | 月面远征补给走 SupplyApi；OrbitHazardEvent 太阳风暴预警；防空不能攻击轨道资产 | 24-orbit.md:1788 |

### 4.148 异界 `tcbeyond` ↔ 舆图 `tcatlas`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| BEY | BEY → ATL | 地图图层；罗盘与 GPS 扰动；异界维度无卫星图 | 21-beyond.md:1651 |
| ATL | ATL ← BEY | 异常区罗盘干扰（σ 30°）、GPS 误差 ×10、虚假雷达回波 | 22-atlas.md:1840 |

### 4.149 异界 `tcbeyond` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| BEY | BEY ↔ NAT | 影响区不可宣称；共同对抗涌潮 +10 关系；文化特质决定异术态度 | 21-beyond.md:1652 |
| NAT | NAT ← BEY | 异界影响区不可宣称；共同对抗异界渗透带来好感 | 23-nations.md:1224 |

### 4.150 异界 `tcbeyond` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| BEY | BEY → ORB | 轨道遥感发现“地磁异常”；太空维度不生成原版怪物 | 21-beyond.md:1653 |
| ORB | ORB ← BEY | 遥感卫星可从轨道发现影响区（显示为“地磁异常”）；太空维度无影响区 | 24-orbit.md:1789 |

### 4.151 舆图 `tcatlas` ↔ 列国 `tcnations`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ATL | ATL ↔ NAT | 国界要素与领土图层、AI 文明作为知识持有者、地图交换条约（AtlasApi.transfer） | 22-atlas.md:1838 |
| NAT | NAT → ATL | 国界与影响力图层、AI 聚落标记、战线与已侦察军队；AI 地图库 | 23-nations.md:1216 |

### 4.152 舆图 `tcatlas` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| ATL | ATL ↔ ORB | Orbit 发射成像与导航载荷、地面站下行、月球与火星地图；Atlas 提供卫星影像与 GPS | 22-atlas.md:1841 |
| ORB | ORB ↔ ATL | 发射 Atlas 载荷，提供 visibleCount、hasService、OrbitBodyReachedEvent；Atlas 提供地面站与地图快照 | 24-orbit.md:1776 |

### 4.153 列国 `tcnations` ↔ 寰宇 `tcorbit`

| 登记方 | 方向 | 联动内容 | 来源 |
| --- | --- | --- | --- |
| NAT | NAT ↔ ORB | AI 抽象航天竞赛；太空合作条约；侦察卫星提供军队与边境情报 | 23-nations.md:1222 |
| ORB | ORB ↔ NAT | spaceRaceLevel；space_cooperation 条约；侦察卫星；AI 出售发射服务 | 24-orbit.md:1784 |

## 5. 规则

1. 主题模组之间只能通过核心接口或 `ModList.isLoaded()` 守卫的桥接类联动（[02 §1.4](02-architecture.md)）。
2. 桥接类位于 `com.nightcity.<module>.compat.<other>`。
3. 每条联动在两侧章节的附录 C 中登记；对方未安装时必须有降级行为。
4. 每条联动在 [40 §10.3](40-roadmap.md) 的组合测试中至少有一个“核心 + 两个模组”的测试用例。

