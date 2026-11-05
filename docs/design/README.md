# Technology_and_Civalization 设计文档总索引

> 文档版本：DD-0.2（对应模组家族 v0.2 规划）
> 适用：Minecraft Java 26.3 · NeoForge 26.3.0.101 · Sodium 0.9.2 · Iris 1.11.7
> 语言约定：中文为主；专有名词、注册 ID、代码标识符附英文。
> 本目录是整个模组家族的**唯一权威设计来源**。代码、配置、数据包与本文档冲突时，以本文档为准并提交修订。

---

## 1. 阅读顺序

| 顺序 | 文件 | 读者 | 说明 |
| --- | --- | --- | --- |
| 1 | [00-vision-and-pillars.md](00-vision-and-pillars.md) | 全员 | 愿景、设计支柱、已确认决策记录 |
| 2 | [01-eras-and-progression.md](01-eras-and-progression.md) | 全员 | 11 个时代、里程碑、节奏曲线、前 10 小时脚本 |
| 3 | [02-architecture.md](02-architecture.md) | 程序 | 18 模组依赖、共享数据层、模拟分级、性能预算 |
| 4 | 03-05 核心章节 | 程序/策划 | 文明核心、科技核心、技能成长 |
| 5 | 10-24 模组章节 | 按负责模块 | 每个模组的完整规格 |
| 6 | 30-36 横向章节 | 全员 | 原版改动、UI、美术、API、联动矩阵、数值、ID 总表 |
| 7 | [40-roadmap.md](40-roadmap.md) | 全员 | 实现阶段与依赖顺序 |

## 2. 章节目录

编号按号段分配：00–05 基础与核心模组，10–24 主题模组，30–36 横向章节，40 路线图。06–09、25–29、37–39 为预留号段，目前没有章节，留给后续核心扩展、新模组与横向专题。34 与 36 由 `aggregate.py` 生成，不要手工编辑；源章节修改后重新运行该脚本。

### 2.1 基础

| 文件 | 标题 |
| --- | --- |
| [00-vision-and-pillars.md](00-vision-and-pillars.md) | 愿景、设计支柱与决策记录 |
| [01-eras-and-progression.md](01-eras-and-progression.md) | 时代与进度 |
| [02-architecture.md](02-architecture.md) | 技术架构 |

### 2.2 核心模组

| 文件 | 模组 | 注册命名空间 |
| --- | --- | --- |
| [03-core-civilization.md](03-core-civilization.md) | 文明核心 Civilization Core | `tcivilization` |
| [04-core-technology.md](04-core-technology.md) | 科技核心 Technology Core | `tctechnology` |
| [05-skills.md](05-skills.md) | 技能成长 Skills | `tcskills` |

### 2.3 主题模组

| 文件 | 模组 | 注册命名空间 |
| --- | --- | --- |
| [10-wildlands.md](10-wildlands.md) | 山河 Wildlands | `tcwildlands` |
| [11-ecology.md](11-ecology.md) | 生息 Living Ecology | `tcecology` |
| [12-cultivation.md](12-cultivation.md) | 田园 Cultivation | `tccultivation` |
| [13-industry.md](13-industry.md) | 工坊 Industry | `tcindustry` |
| [14-networks.md](14-networks.md) | 通途 Networks | `tcnetworks` |
| [15-settlements.md](15-settlements.md) | 聚落 Settlements | `tcsettlements` |
| [16-exchange.md](16-exchange.md) | 市集 Exchange | `tcexchange` |
| [17-discovery.md](17-discovery.md) | 求知 Discovery | `tcdiscovery` |
| [18-echoes.md](18-echoes.md) | 遗响 Echoes | `tcechoes` |
| [19-commons.md](19-commons.md) | 契约 Commons | `tccommons` |
| [20-frontiers.md](20-frontiers.md) | 远征 Frontiers | `tcfrontiers` |
| [21-beyond.md](21-beyond.md) | 异界 Beyond | `tcbeyond` |
| [22-atlas.md](22-atlas.md) | 舆图 Atlas | `tcatlas` |
| [23-nations.md](23-nations.md) | 列国 Nations | `tcnations` |
| [24-orbit.md](24-orbit.md) | 寰宇 Orbit | `tcorbit` |

### 2.4 横向章节

| 文件 | 标题 |
| --- | --- |
| [30-vanilla-overrides.md](30-vanilla-overrides.md) | 原版改动总表 |
| [31-ui-ux.md](31-ui-ux.md) | 全局 UI/UX 规范 |
| [32-art-audio.md](32-art-audio.md) | 美术与音频规范 |
| [33-data-api.md](33-data-api.md) | 数据、API、事件与网络（索引块由 `api_index.py` 生成） |
| [34-interaction-matrix.md](34-interaction-matrix.md) | 18×18 联动矩阵（由 `aggregate.py` 生成，勿手改） |
| [35-balance.md](35-balance.md) | 数值与公式汇总 |
| [36-registry-index.md](36-registry-index.md) | 注册 ID 总表（由 `aggregate.py` 生成，勿手改） |
| [40-roadmap.md](40-roadmap.md) | 实现路线图 |

---

## 3. 全局常量（所有章节必须引用这些值）

| 常量 | 代码名 | 值 | 说明 |
| --- | --- | --- | --- |
| 赤道周长 | `PLANET_CIRCUMFERENCE` | 163,840 格 | X ∈ [-81,920, 81,920)，东西无缝环绕 |
| 极到极距离 | `PLANET_MERIDIAN` | 81,920 格 | Z ∈ [-40,960, 40,960]，-Z 为北 |
| 水平比例 | `HORIZONTAL_SCALE` | 1 格 ≈ 244.6 m（约 1:250） | 40,075 km / 163,840 |
| 纬度换算 | — | `lat = -z / 40,960 × 90°` | 北纬为正 |
| 经度换算 | — | `lon = x / 81,920 × 180°` | 东经为正 |
| 世界高度 | `MIN_Y` / `MAX_Y` | -512 / 1024 | 总高 1,536，96 个区段 |
| 海平面 | `SEA_LEVEL` | Y = 63 | 与原版一致，便于兼容 |
| 最高山峰 | — | 约 Y = 860 | 海拔约 800 格 |
| 深海平原 | — | 约 Y = -240 至 -300 | 海沟最深约 Y = -460 |
| 不可破坏底层 | — | Y = -512 至 -505 | 基岩与岩浆混合层 |
| MC 日 | `TICKS_PER_DAY` | 24,000 tick（20 分钟） | 原版不变 |
| 季 | `DAYS_PER_SEASON` | 12 MC 日 | 实时 4 小时 |
| 年 | `DAYS_PER_YEAR` | 48 MC 日 | 实时 16 小时 |
| 季节顺序 | — | 春 → 夏 → 秋 → 冬 | 南半球相位偏移半年 |
| 时钟 | — | 全球统一时钟，无时区 | 纬度只影响日长与太阳高度，见 10 |
| 地区格 | `REGION_SIZE` | 512 × 512 格（32 × 32 区块） | 模拟、存档与统计的基本单元 |
| 地区格数量 | — | 320 × 160 = 51,200 | 全星球 |
| 气候格 | `CLIMATE_CELL` | 64 × 64 格 | 气候场的分辨率 |
| 星球粗模型 | `PLANET_GRID` | 每 64 格 1 个样本，2,560 × 1,280 | 板块、高度、水文、气候预计算 |
| 实体 NPC 上限 | `MAX_EMBODIED_NPC` | 500（可配置 100-1,000） | 单人世界同时以实体形式存在的人 |
| 全局人口目标 | — | 2 万至 8 万 | 抽象层模拟 |
| 模组 tick 预算 | `TC_TICK_BUDGET_MS` | 平均 ≤ 10 ms / tick | 全家族合计，见 02 |
| 开局部族 | `STARTING_BAND` | 玩家 + 4 名族人（可配置 0-8） | 见 01 |
| 目标通关时长 | — | 300-500 小时（标准约 407 小时） | 见 01 |

## 4. 时代 ID 速查

| 序号 | 时代 ID | 中文 | 英文 |
| --- | --- | --- | --- |
| E0 | `stone` | 石器时代 | Stone Age |
| E1 | `neolithic` | 新石器与农耕时代 | Neolithic Age |
| E2 | `bronze` | 青铜时代 | Bronze Age |
| E3 | `iron` | 铁器时代 | Iron Age |
| E4 | `medieval` | 中世纪 | Medieval Age |
| E5 | `renaissance` | 文艺复兴与大航海 | Age of Sail |
| E6 | `industrial` | 工业时代 | Industrial Age |
| E7 | `electric` | 电气时代 | Electric Age |
| E8 | `atomic` | 原子与航空时代 | Atomic Age |
| E9 | `information` | 信息时代 | Information Age |
| E10 | `space` | 太空时代 | Space Age |

## 5. 文档约定

### 5.1 单位与写法

- 距离：格（block）。大距离可写“千格 kb”，例如 12 kb = 12,000 格。
- 时间：tick、MC 日（day）、季（season）、年（year）；实时时间写“实时 x 分钟”。
- 数值：千位逗号，小数点后最多 3 位；百分比写 `%`。
- 公式：使用行内代码或代码块，变量为 `snake_case`，常量为 `UPPER_SNAKE_CASE`。
- 概率：写成 0-1 小数或百分比，必须说明判定周期（每 tick / 每 MC 日 / 每次交互）。
- 时代引用：写 `E3 iron`，不要只写“铁器时代”。
- 能量单位：机械动力 `SU`（Stress Unit，转速 × 扭矩），电能 `J` 与功率 `W`，热能 `HU`。定义见 13。
- 货币单位：各文明货币独立，跨文明统一换算使用内部记账单位 `VU`（Value Unit），见 16。

### 5.2 注册 ID 命名规范

| 类型 | 格式 | 示例 |
| --- | --- | --- |
| 方块/物品 | `<modid>:<snake_case>`，形态后置 | `tcindustry:bronze_ingot`、`tcwildlands:raw_chalcopyrite` |
| 岩石 | `<rock>`、`<rock>_cobble`、`<rock>_bricks` | `tcwildlands:basalt_cobble` |
| 矿石 | `<host_rock>_<mineral>_ore` | `tcwildlands:granite_cassiterite_ore` |
| 实体 | `<modid>:<snake_case>` | `tcivilization:person`、`tcecology:red_deer` |
| 流体 | `<modid>:<fluid>`，桶为 `<fluid>_bucket` | `tcindustry:crude_oil` |
| 维度 | `<modid>:<snake_case>` | `tcorbit:moon`、`tcorbit:mars` |
| 生物群系 | `<modid>:<climate>_<landform>` | `tcwildlands:temperate_broadleaf_forest` |
| 科技 | `tctechnology:<era>/<tech>` | `tctechnology:bronze/alloying` |
| 技能 | `tcskills:<skill>` | `tcskills:mining` |
| 天赋 | `tcskills:<skill>/<talent>` | `tcskills:mining/vein_sense` |
| 标签 | 通用材料优先使用 NeoForge `c:` 命名空间 | `c:ingots/bronze`、`c:ores/tin` |
| 配置文件 | `<modid>-common.toml`、`<modid>-client.toml`、`<modid>-server.toml` | `tcatlas-client.toml` |
| 配置键 | 节名与键名均为 `snake_case`，正文写作 `节.键`（02 §9） | `maintenance.auto_threshold` |
| 数据包目录 | `data/<modid>/<type>/` | `data/tctechnology/tc_tech/bronze/alloying.json` |
| 翻译键 | `<category>.<modid>.<path>` | `block.tcwildlands.basalt`、`gui.tcatlas.layer.geology` |
| 网络负载 | `<modid>:<snake_case>` | `tcatlas:map_tile_sync` |
| 按键 | `key.<modid>.<action>`，分类 `key.categories.tc` | `key.tcatlas.world_map` |
| 事件类 | `<Module><Subject><Verb>Event` | `SettlementFoundedEvent` |

规则：

1. 所有 ID 全局唯一，统一登记在 [36-registry-index.md](36-registry-index.md)。
2. 方块与其物品使用相同 ID。
3. 同一材料在多个模组出现时，只由**拥有者模组**注册，其他模组通过 `c:` 标签使用。材料拥有者见 02 的“材料归属表”。
4. 禁止注册与原版重名的路径来“覆盖”原版；原版改动统一通过数据包与事件处理，见 30。

### 5.3 模组章节统一模板

每个 03-05、10-24 章节按以下顺序组织；不适用的小节写“无”并说明原因。

1. 定位与设计支柱
2. 独立玩法（只安装本模组与必需核心时）
3. 联动玩法（逐个列出与其他模组的联动）
4. 核心玩法循环（含 mermaid 流程图）
5. 系统规则与公式
6. 数值表
7. 内容清单：方块 / 物品 / 实体 / 流体 / 维度 / 结构（含 ID、时代、用途）
8. 配方与生产链
9. 交互规范（手持、左/右键、潜行组合、按键、GUI 操作）
10. UI 线框描述（ASCII 线框）
11. 数据结构与存档
12. 公共 API、事件、网络负载
13. 配置项
14. 性能预算
15. 技能熟练度挂钩（调用 `SkillsApi.addProficiency(...)` 的行为与数值）
16. 原版改动（若有）
17. 验收标准（可验证条目）
18. 开放问题与风险

### 5.4 生成章节与校验工具

34、36 两章和 33 中 `<!-- GENERATED:… -->` 标记之间的内容由脚本从各模组章节生成。脚本位于仓库的 `tools/design/`，只依赖 Python 3 标准库，在仓库根目录 `I:\Projects\SuperMC` 运行。

| 命令 | 读取 | 作用 | 写入 |
| --- | --- | --- | --- |
| `python tools/design/aggregate.py` | 03-05、10-24 章附录 A-D | 校验跨模组契约（ID 重复、科技前置、时代配额、联动双向登记、技能挂钩），并重新生成 34 与 36 | 34、36 |
| `python tools/design/aggregate.py --check` | 同上 | 只校验，在控制台列出错误与警告 | 无 |
| `python tools/design/api_index.py` | 各模组章节第 11、12 节与全文 | 校验数据路径、API、事件、负载与命令约定，重写 33 的生成块（标记外的手写内容保留） | 33 |
| `python tools/design/api_index.py --check` | 同上 | 只校验 | 无 |

两个脚本都支持：`--strict`（有错误时退出码为 1，供 CI 使用）、`--json <文件>`（另外输出完整报告；报告的 `issues` 每项含 `level`、`code`、`message`、`where`）、`--docs <目录>`（默认 `docs/design`）。`api_index.py` 复用 `aggregate.py` 的解析器，两者须放在同一目录。

工作流：

1. 只改来源章节（附录 A-D、第 11-12 节）；不要手改 34、36 和 33 的生成块，否则下次生成会被覆盖。
2. 先运行两个 `--check`。错误必须在合并前清零；警告必须在所属阶段结束前清零（[33 §15](33-data-api.md)、[40](40-roadmap.md) EC-P0-8）。
3. 再运行两个不带 `--check` 的命令重新生成，把生成结果和来源修改一并提交。
4. 表格行内的代码片段不能含 `|`（例如写 `abs(v)` 而不是竖线绝对值），否则会被 Markdown 和解析器当作列分隔符。

## 6. 全局按键（权威表，31 章细化）

| 默认键 | 功能 | 所属 |
| --- | --- | --- |
| `M` | 全屏世界地图 | Atlas |
| `N` | 小地图模式切换（关闭 / 北向上 / 随朝向旋转）；形状与缩放在地图设置中调整 | Atlas |
| `B` | 在准星指向处（64 格内）或当前位置添加路径点；`Shift + B` 跳过命名 | Atlas |
| `H` | HUD 显示等级切换（极简 / 标准 / 扩展）；`F1` 仍为原版全隐藏 | Atlas（未安装 Atlas 时由 Civilization Core 注册） |
| `Ctrl + H` | HUD 布局编辑器 | Atlas |
| `J` | 文明管理面板 | Civilization Core |
| `U` | 科技树（文明面板的科技页） | Technology Core |
| `Z` | 指令轮盘（按住显示、松开执行；对准 NPC/方块时出现上下文指令） | Civilization Core（外壳）/ Settlements、Networks 等提供指令 |
| `V` | 观察模式（显示方块与生物的已知属性） | Discovery |
| `Y` | 日志：笔记、发现、任务、传说 | Discovery / Echoes |
| 未绑定 | 技能与天赋（经 `J` › 个人 › 技能，或 `Z` 轮盘“个人”项进入） | Skills |
| 未绑定 | 外交页（经 `J` › 文明 › 外交进入） | Nations |
| 未绑定 | 其他可选键：市集页签、网络标注、小地图缩放、临时标记、提前换弹；载具键（鸣笛、换向、RCS 模式、确认巡航、车灯）默认复用跳跃/疾跑键或轮盘。完整列表见 [31 §6.1](31-ui-ux.md) | Exchange、Industry、Atlas、Frontiers、Networks、Orbit |

所有按键归入分类 `key.categories.tc`（“文明与科技”），可在原版按键设置中修改。

**被占用、不得使用的键**（以本整合包实例 `options.txt` 为准）：原版 `G` 快捷操作、`O` 好友、`P` 社交、`L` 进度、`C`/`X` 快捷栏存取；Iris `R` 重载光影、`K` 开关光影、`I` 光影包选择。详见 [31 §6.3](31-ui-ux.md)。

## 7. 术语表

| 术语 | 英文/代码 | 定义 |
| --- | --- | --- |
| 文明 | Civilization | 共享科技、货币、政策与历史的最高组织。多人模式中全部玩家属于同一个玩家文明；其余为 AI 文明 |
| 聚落 | Settlement | 有边界、居民与设施的定居点；等级：营地 → 村落 → 镇 → 城 → 都市 → 特大城市 |
| 地区 | Region | 512 × 512 格的模拟单元，存放地理、生态、经济、领土等统计 |
| 流域 | Watershed | 由预计算水文模型划分的汇水区，决定河流与农业灌溉 |
| 时代 | Era | 文明整体技术阶段，E0-E10 |
| 科技 | Technology | 科技树节点，属于文明 |
| 知识 | Knowledge | 由观察、实验与实践获得的“未成形科技”，积累到阈值后可研究对应科技 |
| 研究点 | Research Points（RP） | 研究机构把知识转化为科技时消耗的资源 |
| 熟练度 | Proficiency | 玩家个人在某项技能上的经验，属于 `tcskills` |
| 实体化 | Embodied | NPC 以 Minecraft 实体形式存在并逐 tick 运行 |
| 抽象化 | Abstract | NPC 或聚落以数据形式按低频周期模拟 |
| 模拟分级 | Simulation LOD | L0 实体 / L1 代理 / L2 聚落聚合 / L3 文明聚合，见 02 |
| 繁荣度 | Prosperity | 聚落综合发展指标，见 15 |
| 稳定度 | Stability | 聚落与文明的秩序与满意度指标，见 19 |
| 信誉 | Reputation | 交易与外交中对方的信任程度，见 16、23 |
| 环境负荷 | Environmental Load | 地区的污染、碳排放与生态破坏累计值，见 10、11 |
| 测绘等级 | Survey Level | 某地区在地图上被认知的精度，见 22 |
| 世界历史 | World History | 种子生成时模拟出的数千年历史记录，见 18 |
| 影响区 | Anomaly Zone | 异界渗透区域，原版怪物只在此出现，见 21 |
| 里程碑 | Milestone | 推进时代所需的标志性成就，见 01 |
| 人 | Person | 所有人形 NPC 的共享实体 `tcivilization:person`，由各模组附加职业与阵营组件 |
