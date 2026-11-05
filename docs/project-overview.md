# Technology and Civilization

## Minecraft 模组项目介绍

**Technology and Civilization** 是一个面向 Minecraft Java Edition 的 NeoForge 模组家族项目。项目把文明发展、科技解锁、个人技能、生态环境、农业、工业、交通、聚落、贸易、探索、外交与太空殖民拆分为多个可以独立安装和演进的模组。

项目的长期目标是让玩家从石器时代的部族起步，经历农业、冶金、城市、工业化、信息时代，最终进入轨道、月球与火星阶段。每个阶段都提供完整的玩法切片，同时保持模组之间清晰的 API、数据和存档边界。

## 项目定位

项目关注的是一个持续演化的文明世界，而不是单一的内容扩展包：

- **文明是长期状态**：世界历史、聚落、人口、地区和文明事件会持续记录并影响后续玩法。
- **科技是进度骨架**：时代、科技、知识和研究点共同决定玩家可使用的配方、设施与系统。
- **个人成长与文明成长分离**：玩家技能属于个人，文明科技属于共同世界，两者通过稳定 API 联动。
- **自然与社会相互反馈**：地理、气候、生态、资源、污染、人口、贸易和外交共同构成世界循环。
- **模块可以独立运行**：核心模组提供稳定合同，主题模组在安装核心后各自提供完整的垂直切片。

## 技术基线

| 项目 | 当前基线 |
| --- | --- |
| 游戏 | Minecraft Java 26.3 |
| 模组平台 | NeoForge 26.3.0.101 |
| 构建系统 | Gradle 多模块工程 |
| Java 工具链 | Java 25 |
| 可选图形环境 | Sodium 0.9.2、Iris 1.11.7 |
| 主要语言 | Java、Groovy、Python、Markdown |
| 设计文档语言 | 中文为主，代码标识符使用英文 |

根工程名为 `technology-and-civilization`，远程仓库为 [`minecraft-technology-and-civilization`](https://github.com/NeonX-Studio/minecraft-technology-and-civilization)。

## 模块架构

项目当前包含 18 个模组，分为核心层和主题层。根目录的 `settings.gradle` 负责统一声明这些子工程。

### 核心层

| 模组 | 职责 |
| --- | --- |
| `tcivilization-core` | 文明、聚落登记、共享人物实体、地区网格、事件总线、世界历史、存档状态和模拟调度 |
| `tctechnology` | 时代、科技、知识、研究点、科技前置、配方门槛和科技 API |
| `tcskills` | 玩家熟练度、技能经验、成长等级、技能点、天赋和熟练度倍率 API |

`tcivilization-core` 是大多数主题模组的共享基础。`tctechnology` 建立在文明核心之上，`tcskills` 保持独立，以便其他模组按需接入。

### 主题层

| 模组 | 主要内容 |
| --- | --- |
| `tcwildlands` | 星球地理、岩性、矿床、地形、水文、气候和自然资源 |
| `tcecology` | 物种、栖息地、种群、生态健康和生态变化 |
| `tccultivation` | 作物、土壤、灌溉、畜牧、食品和保存 |
| `tcindustry` | 制造、能源、维护、自动化、金属和工业设施 |
| `tcnetworks` | 道路、铁路、航运、车站、仓储和物流路由 |
| `tcsettlements` | 家庭、职业、住房、聚落等级和城镇成长 |
| `tcexchange` | 区域供需、价格、订单、货币和声誉 |
| `tcdiscovery` | 观察、实验、考古、研究和知识发现 |
| `tcechoes` | 遗迹、地方历史、传说和探索事件 |
| `tccommons` | 合作建造、土地权限、法律和公共工程 |
| `tcfrontiers` | 前哨、远征、补给、危险区域和边疆活动 |
| `tcbeyond` | 可选的魔法生态、异界、其他维度和异常资源 |
| `tcatlas` | 小地图、世界地图、测绘、图层、路径点和 HUD 编辑器 |
| `tcnations` | AI 文明、领土、外交、影响力和边境冲突 |
| `tcorbit` | 火箭、卫星、轨道站、月球和火星殖民 |

每个模块目前都带有独立的状态命令。这些命令是当前版本的垂直切片入口，后续会在不改变模块边界的前提下逐步扩展为完整系统。

## 核心运行模型

### 共享数据层

文明核心以地区网格作为跨模组数据的基础。每个模组可以为地区注册自己的附加层，例如：

- Wildlands 保存地质、气候和水文数据；
- Ecology 保存物种种群和生态健康；
- Settlements 保存人口、住房和职业结构；
- Exchange 保存区域市场和供需；
- Atlas 保存测绘等级和地图瓦片版本；
- Nations 保存文明影响力；
- Orbit 保存遥感覆盖和重访时间。

这种设计让模组共享世界状态，同时避免把所有系统耦合到一个大型实现中。

### 事件与 API

跨模块联动通过核心 API、事件和受保护的兼容桥接完成。典型事件包括时代推进、科技解锁、聚落建立、人物实体化、季节变化和历史记录。主题模组之间不直接依赖对方的实现细节，而是使用稳定接口、注册 ID、标签和事件完成协作。

### 模拟分级

世界中的实体和文明采用分级模拟：

1. **L0 实体层**：玩家附近的 NPC、动物、车辆和机器，进行完整交互和逐 tick 行为。
2. **L1 代理层**：已加载但距离玩家较远的对象，按较低频率进行批量结算。
3. **L2 聚落层**：未加载区域中的聚落，按 MC 小时结算产出、人口、消耗和建造进度。
4. **L3 文明层**：远方 AI 文明，按 MC 日处理扩张、科技、外交和宏观经济。

这种 LOD 设计用于支持大范围世界和较高人口规模，同时控制单 tick 的计算预算。

## 设计文档

`docs/design/` 是项目的权威设计规格，代码、数据包和配置与设计冲突时，应先修订设计文档。

推荐阅读顺序：

1. [`00-vision-and-pillars.md`](design/00-vision-and-pillars.md)：愿景、设计支柱和决策记录；
2. [`01-eras-and-progression.md`](design/01-eras-and-progression.md)：时代、里程碑和前期体验；
3. [`02-architecture.md`](design/02-architecture.md)：依赖、数据层、线程模型、性能预算和存档；
4. [`03`–`05`](design/03-core-civilization.md)：文明核心、科技核心和技能系统；
5. [`10`–`24`](design/10-wildlands.md)：各主题模组规格；
6. [`30`–`36`](design/30-vanilla-overrides.md)：原版改动、UI、美术、API、联动矩阵、平衡和注册表；
7. [`40-roadmap.md`](design/40-roadmap.md)：阶段路线图和退出标准。

其中 `33-data-api.md`、`34-interaction-matrix.md` 和 `36-registry-index.md` 包含由工具生成的内容，不应直接手工编辑生成区域。

## 设计与校验工具

项目提供两个只依赖 Python 标准库的设计校验工具：

```powershell
python tools\design\aggregate.py --check
python tools\design\api_index.py --check
```

`aggregate.py` 检查跨模组注册 ID、科技前置、时代配额、联动登记和技能挂钩，并可重新生成联动矩阵与注册表。`api_index.py` 检查数据路径、API、事件、网络负载、命令和数据包约定，并更新 API 索引。

修改设计章节后，建议按以下顺序工作：

1. 修改来源章节；
2. 运行两个 `--check` 命令；
3. 修复错误和阶段内警告；
4. 运行不带 `--check` 的命令重新生成索引；
5. 将来源章节和生成结果放在同一个提交中。

## 开发与构建

项目使用 Gradle 多模块结构，每个模组目录都包含自己的 `build.gradle`、`gradle.properties` 和 `src/main`。开发环境需要 Java 17 或更高版本，项目源码中的 Java toolchain 当前配置为 Java 25。

常用操作包括：

```powershell
# 查看所有 Gradle 任务
gradle tasks --all

# 构建全部模组
gradle clean build

# 启动客户端运行配置
gradle :tcivilization-core:runClient
```

当前仓库没有提交本地 Gradle 分发包，因此应使用本机 Gradle、IDE 集成 Gradle 或项目维护者提供的 Gradle 环境。构建输出、缓存、发行包和本地工具目录由 `.gitignore` 排除。

## 当前状态与路线

当前版本属于 **v0.1 Skeleton**：

- 18 个模组的模块边界已经建立；
- 核心 API、事件、SavedData 和部分状态命令已有代码骨架；
- 中英文资源文件和 NeoForge 模组元数据已经建立；
- 设计规格、注册 ID、数据 API、联动矩阵和路线图已经完成初版；
- 完整世界生成、NPC 行为、经济循环、外交和太空内容仍按路线图逐阶段实现。

路线图的关键顺序是：

```text
核心 API → 星球生成 → 聚落与人物 → 工业与交通 → 全球文明 → 轨道与火星
```

下一阶段 P0（v0.2）的重点是冻结核心 API、完善科技加载器和技能框架、建立 HUD 外壳、补齐测试设施、修正模块依赖，并让文档校验和基础构建进入持续验证流程。

## 设计原则

- **地基先行**：核心 API 在早期阶段冻结，后续以兼容扩展为主。
- **垂直切片**：每个阶段交付一段完整可玩的时代体验。
- **数据驱动**：科技、配方、物种、作物、建筑、事件和传说优先使用数据文件描述。
- **稳定 ID**：注册 ID 一经进入阶段版本，后续通过迁移和别名保持存档兼容。
- **独立可玩**：核心加任一主题模组都应具备可验证的独立运行路径。
- **性能预算前置**：从基础阶段开始测量 tick、世界生成、模拟和地图渲染开销。
- **文档即规格**：实现与设计冲突时先修订设计，再修改代码。

## 项目入口

- [项目 README](../README.md)
- [设计文档索引](design/README.md)
- [技术架构](design/02-architecture.md)
- [实现路线图](design/40-roadmap.md)
- [设计校验工具](../tools/design/)
- [GitHub 仓库](https://github.com/NeonX-Studio/minecraft-technology-and-civilization)
