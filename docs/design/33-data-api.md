# 33 · 数据与 API 总规范（Data & API）

> 本章是**横切章节**，不使用 README §5.3 的 18 节模组模板。它规定 18 个模组之间**如何交换数据**：包与命名、API 模式、线程、事件总线、网络载荷、存档、附件与数据组件、数据包、标签、地区层、命令、拒绝原因与深链接。各模组章节第 11、12 节与附录 A 只描述“自己暴露了什么”；本章给出共同规则，并用脚本把各章内容汇总成索引。
>
> 冲突裁决顺序：`design-brief.md` 契约 → 00 章 DR/DA 决议 → 02 架构 → **本章** → 03/04/05 核心章节 → 各模组章节。界面相关的载荷、限流与按键以 31 章为准，本章只引用。
>
> 本章中 `<!-- GENERATED:… -->` 标记之间的内容由 `tools/design/api_index.py` 生成，**不要手改**；修改应落在来源章节，然后重新生成。

---

## 0. 文档信息

| 项 | 内容 |
| --- | --- |
| 章节编号 | 33 |
| 适用模组 | 全部 18 个 |
| 依赖章节 | 00（DR-01 软依赖、DR-08 世界生成替换、DR-33 技能独立）、02（§1.4 软联动、§2 共享数据层、§5 线程、§7 存档、§9 配置）、03 §12（核心 API、事件、载荷、核心托管契约 §12.5）、31 §4.8（深链接）、§14（网络与同步）、§16（拒绝原因、并发）、40 §2（版本与兼容承诺） |
| 下游章节 | 03-24 各章第 11、12 节与附录 A；34（联动矩阵）、36（注册表索引） |
| 生成工具 | `tools/design/api_index.py`（依赖同目录 `aggregate.py`） |
| 版本 | 设计 v1.0；对应代码 v0.2（P0）起落地，`TcApi.VERSION = 1` |
| 状态 | 规范稿；第 17 节列出未决问题 |

### 0.1 术语

| 术语 | 含义 |
| --- | --- |
| 提供者 | 实现并安装某个 API 的模组 |
| 读者 | 调用某个 API、订阅某个事件或引用某个 ID 的模组 |
| 软依赖 | `neoforge.mods.toml` 中 `type = "optional"` 的依赖；读者在提供者缺失时仍能完整运行（DR-01） |
| 核心托管契约 | 接口放在核心包、由某个主题模组安装实现的 API（03 §12.5）；读者只依赖核心 |
| 桥接类 | `com.nightcity.<模组>.compat.<对方>` 包中的类，只在对方已加载时才被类加载（02 §1.4） |
| 载荷 | NeoForge `CustomPacketPayload`；ID 形如 `<modid>:<name>` |
| 地区 | 512×512 格的网格单元 `RegionCell`（02 §2.1） |
| 地区层 | 挂在地区上的、按模组划分的一块可序列化数据（02 §2.2、03 §5.2.3） |
| 附件 | NeoForge 数据附件（`AttachmentType`），挂在实体、方块实体、区块或维度上 |
| 数据组件 | 原版 `DataComponentType`，挂在物品堆叠上 |
| 稳定面 | 受 `TcApi.VERSION` 约束、只增不删的公开类型与方法（§3.4） |

### 0.2 生成与校验

| 命令（在仓库根目录执行） | 作用 |
| --- | --- |
| `python tools/design/api_index.py` | 解析 03-24 各章，重写本章所有生成块 |
| `python tools/design/api_index.py --check` | 只校验，不写文件；在控制台列出错误与警告 |
| `python tools/design/api_index.py --check --strict` | 有错误时退出码为 1，供 CI 使用 |
| `python tools/design/api_index.py --json <文件>` | 另外输出完整索引与问题清单的 JSON |

Windows 下先设置 `PYTHONIOENCODING=utf-8`。脚本读取各章节第 11 节（配置）、第 12 节（API、事件、载荷）、附录 A（注册条目）以及全文中的存档路径、标签、数据包目录与命令；规则见每个生成块之前的说明。

### 0.3 总览

<!-- GENERATED:summary BEGIN -->
> 自动生成于 2026-11-05 13:13，来源：18 个模组章节的第 11、12 节与全文；重新生成：`python tools/design/api_index.py`。

| 模组 | 章节 | 事件 | 负载（S→C / C→S / 双向） | API 类型 | 入口 | 附件/组件 | 地区层 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 文明核心 `tcivilization` | [03](03-core-civilization.md) | 34 | 20（12 / 8 / 0） | 52 | 1 | 0 | 1 |
| 科技核心 `tctechnology` | [04](04-core-technology.md) | 10 | 7（4 / 3 / 0） | 14 | 2 | 0 | 0 |
| 技能 `tcskills` | [05](05-skills.md) | 10 | 14（6 / 8 / 0） | 34 | 0 | 2 | 0 |
| 山河 `tcwildlands` | [10](10-wildlands.md) | 11 | 15（13 / 2 / 0） | 35 | 0 | 0 | 3 |
| 生息 `tcecology` | [11](11-ecology.md) | 10 | 8（3 / 5 / 0） | 7 | 1 | 8 | 1 |
| 田园 `tccultivation` | [12](12-cultivation.md) | 8 | 9（3 / 6 / 0） | 13 | 1 | 3 | 1 |
| 工坊 `tcindustry` | [13](13-industry.md) | 9 | 13（6 / 7 / 0） | 16 | 0 | 1 | 1 |
| 通途 `tcnetworks` | [14](14-networks.md) | 9 | 10（5 / 4 / 1） | 11 | 1 | 1 | 1 |
| 聚落 `tcsettlements` | [15](15-settlements.md) | 9 | 9（4 / 5 / 0） | 14 | 0 | 0 | 1 |
| 市集 `tcexchange` | [16](16-exchange.md) | 10 | 10（2 / 8 / 0） | 16 | 0 | 0 | 1 |
| 求知 `tcdiscovery` | [17](17-discovery.md) | 8 | 9（3 / 6 / 0） | 16 | 1 | 0 | 1 |
| 遗响 `tcechoes` | [18](18-echoes.md) | 7 | 10（5 / 4 / 1） | 22 | 1 | 0 | 1 |
| 契约 `tccommons` | [19](19-commons.md) | 14 | 14（2 / 12 / 0） | 8 | 0 | 0 | 1 |
| 远征 `tcfrontiers` | [20](20-frontiers.md) | 10 | 10（6 / 4 / 0） | 13 | 1 | 6 | 1 |
| 异界 `tcbeyond` | [21](21-beyond.md) | 9 | 8（4 / 4 / 0） | 7 | 1 | 4 | 1 |
| 舆图 `tcatlas` | [22](22-atlas.md) | 8 | 17（11 / 5 / 1） | 15 | 1 | 0 | 1 |
| 列国 `tcnations` | [23](23-nations.md) | 10 | 10（7 / 3 / 0） | 11 | 1 | 0 | 1 |
| 寰宇 `tcorbit` | [24](24-orbit.md) | 10 | 11（7 / 4 / 0） | 19 | 1 | 0 | 1 |
| **合计** | — | **196** | **204** | **323** | **13** | **25** | **18** |

问题：错误 0，警告 0，提示 79（明细见 §15）；未解析的事件引用 0 个（见 §7.4）。
<!-- GENERATED:summary END -->

---

## 1. 原则

1. **软依赖优先（DR-01）。** 除 02 §1.3 列出的必需依赖外，模组之间只能软依赖。读者在提供者缺失时必须能启动、能存档、能完成自己的独立玩法；缺失只会让联动内容消失，不能让它报错。
2. **核心负责连接。** 被两个以上**不依赖提供者**的模组读取的数据，接口放在核心（§3.1）。核心不实现玩法，只定义契约并提供降级默认值。
3. **用 ID 引用，不用常量。** 模组之间禁止引用对方的 `Block`、`Item`、`DeferredHolder` 等 Java 常量；改用 ID 字符串、`c:` 标签和注册表查询（02 §1.4.3）。
4. **服务端权威。** 客户端只显示服务端发来的数据；所有修改都是请求，在服务端校验后执行（31 §14.1）。
5. **一个事实只有一个主人。** 每个事件类、每个载荷、每个地区层、每个存档文件、每个数据包目录都只有一个拥有者模组；其他模组只能读或请求。共享事件放在核心（§5.3）。
6. **能登记就能检查。** 所有跨模组的 ID 必须登记在拥有者章节的附录 A 或第 12 节表格中，`api_index.py` 据此生成索引并报告冲突；未登记的联动视为缺陷（02 §1.4.4）。
7. **数值进数据包，开关进 TOML。** 平衡数值优先放在数据包；只有性能参数与功能开关放在 TOML（02 §9）。

---

## 2. 包与命名约定

### 2.1 根包

| 模组 ID | Gradle 子项目 | 根包 | 主类（v0.1 已存在） |
| --- | --- | --- | --- |
| `tcivilization` | `tcivilization-core` | `com.nightcity.tcivilization` | `TCivilization` |
| `tctechnology` | `tctechnology` | `com.nightcity.technology` | `TTechnology` |
| `tcskills` | `tcskills` | `com.nightcity.skills` | `TSkills` |
| `tcwildlands` | `tcwildlands` | `com.nightcity.wildlands` | `TWildlands` |
| `tcecology` | `tcecology` | `com.nightcity.ecology` | `TEcology` |
| `tccultivation` | `tccultivation` | `com.nightcity.cultivation` | `TCultivation` |
| `tcindustry` | `tcindustry` | `com.nightcity.industry` | `TIndustry` |
| `tcnetworks` | `tcnetworks` | `com.nightcity.networks` | `TNetworks` |
| `tcsettlements` | `tcsettlements` | `com.nightcity.settlements` | `TSettlements` |
| `tcexchange` | `tcexchange` | `com.nightcity.exchange` | `TExchange` |
| `tcdiscovery` | `tcdiscovery` | `com.nightcity.discovery` | `TDiscovery` |
| `tcechoes` | `tcechoes` | `com.nightcity.echoes` | `TEchoes` |
| `tccommons` | `tccommons` | `com.nightcity.commons` | `TCommons` |
| `tcfrontiers` | `tcfrontiers` | `com.nightcity.frontiers` | `TFrontiers` |
| `tcbeyond` | `tcbeyond` | `com.nightcity.beyond` | `TBeyond` |
| `tcatlas` | `tcatlas` | `com.nightcity.atlas` | `TAtlas` |
| `tcnations` | `tcnations` | `com.nightcity.nations` | `TNations` |
| `tcorbit` | `tcorbit` | `com.nightcity.orbit` | `TOrbit` |

规则：根包是 `com.nightcity.` 加上**去掉 `tc` 前缀**的模组 ID；两个核心例外：`tcivilization` 的根包保留全名（`com.nightcity.tcivilization`，避免与 `com.nightcity.civilization` 混淆），`tctechnology` 用 `com.nightcity.technology`。章节中出现的 `com.nightcity.tc<name>`（例如 `com.nightcity.tcbeyond`）或 `com.nightcity.civilization` 都是笔误，以本表为准。

### 2.2 子包

| 子包 | 可见性 | 内容 | 读者可否引用 |
| --- | --- | --- | --- |
| `<根>`（根包本身） | 内部 | 主类 `T<Name>`、`<Name>Config`、初始化 | 否 |
| `<根>.api` | **稳定面** | 入口 `<Name>Api`、接口、记录、枚举、常量 ID | 是（仅桥接类内） |
| `<根>.api.event` | **稳定面** | 本模组拥有的事件类 | 是（仅桥接类内） |
| `<根>.api.impl` | 内部 | 核心托管契约的实现（03 §12.5 规则 5） | 否 |
| `<根>.client.api` | **稳定面**（仅客户端） | 客户端 API：HUD 控件、地图图层、页签渲染器、客户端事件 | 是（仅客户端桥接类内） |
| `<根>.client` | 内部 | 屏幕、渲染器、按键处理 | 否 |
| `<根>.network` | 内部 | 载荷记录与处理器 | 否（通过载荷 ID 协作） |
| `<根>.data` | 内部（JSON 结构是公开契约） | 数据包条目的 `Codec` 记录、重载监听器 | 否（通过数据包目录与 ID 协作） |
| `<根>.registry` | 内部 | `DeferredRegister` 与持有者 | **禁止**（原则 3） |
| `<根>.world` | 内部 | 世界生成、结构、特征 | 否 |
| `<根>.sim` | 内部 | L2/L3 模拟与后台任务 | 否 |
| `<根>.compat.<对方>` | 内部 | 桥接类：调用对方 `api` 包 | — |

核心另有三个托管区域，它们的包属于核心但实现来自主题模组（03 §12.5）：

| 包 | 内容 | 提供者 |
| --- | --- | --- |
| `com.nightcity.tcivilization.api.world` | `TcWorldApis`、`WorldApiSet` 与九个世界接口 | tcwildlands |
| `com.nightcity.tcivilization.api.world.event` | 世界事件类（03 §12.5 规则 6） | tcwildlands；排放事件由排放方发布 |
| `com.nightcity.tcivilization.api.history` | `TcHistory`、`WorldHistoryView` 与历史记录类型 | tcechoes |

### 2.3 类型命名

| 对象 | 命名 | 例子 |
| --- | --- | --- |
| API 入口 | `<Name>Api`，静态方法 `get()` | `ExchangeApi.get()` |
| 核心托管入口 | `Tc<Area>` 或 `<Area>Apis`，静态安装与读取方法 | `TcWorldApis`、`TcHistory` |
| 通知事件 | `<主语><过去分词>Event` | `SettlementFoundedEvent` |
| 成对事件 | 外层类 + 嵌套 `Pre`（可取消）/ `Post` | `TradeExecutedEvent.Pre` |
| 注册扩展点事件 | `Register<对象复数>Event`（模组总线） | `RegisterRegionLayersEvent` |
| 载荷记录 | `<Name>Payload`；ID 为蛇形小写 | `PanelRequestPayload` ↔ `tcivilization:panel_request` |
| 地区层常量 | `<NAME>_LAYER` | `TWildlands.GEO_LAYER` |
| 附件持有者类 | `<Name>Attachments` | `SettlementsAttachments.RESIDENT` |
| 数据组件持有者类 | `<Name>Components` | `CultivationComponents.FRESHNESS` |
| 值对象 | `record`；封闭集合用 `enum`；开放集合用 `Identifier` + 注册表或数据包 | `record CivRef(UUID id)` |

### 2.4 ID 与本地化键

1. 所有 ID 为 `<modid>:<path>`，`path` 只含 `[a-z0-9_./-]`。注册对象（方块、物品、实体、方块实体、菜单）的 `path` 使用扁平蛇形，不含 `/`；数据驱动条目可用 `/` 分组，具体形式以拥有者章节为准。
2. 改名必须登记注册别名（40 §9.2）；v1.0 后不再改名。
3. 本地化键的格式由 31 §12.2 规定；本章新增两类：拒绝原因 `gui.<modid>.reason.<id>`（§14.1）、命令反馈 `commands.<modid>.<root>.<sub>.<message>`（§13）。

### 2.5 v0.1 代码与本规范的差异

| 现状 | 规范 | 处理 |
| --- | --- | --- |
| 每个模组只有主类和 `<Name>Events`，没有 `api` 子包 | §2.2 | P0 按章节第 12 节建立 `api` 与 `api.event` |
| `tcskills` 的 `SkillsApi`、`PlayerSkillsProfile` 位于根包 `com.nightcity.skills` | 应在 `com.nightcity.skills.api` | P0 移动；技能是独立模组，移动不影响其他模组 |
| 核心 `CivilizationSavedData`、`TechnologySavedData` 无 `formatVersion` | §8.1 | P0 加字段，v0.1 存档按版本 0 读取 |
| 所有 `neoforge.mods.toml` 把核心写为 `optional` | 02 §1.3 | P0 修正 |

---

## 3. API 模式

模组之间交换数据有五种模式。选择顺序：能用数据（模式 c）就不用代码；能用事件（模式 d）就不用直接调用；需要直接调用时，按读者数量选择核心托管（a）或模组入口（b）；需要别人往自己这里加东西时，用扩展点（e）。

| 模式 | 适用 | 读者依赖 | 例子 |
| --- | --- | --- | --- |
| (a) 核心托管契约 | 两个以上不依赖提供者的读者；或核心自己要读；或世界生成阶段要读 | 只依赖核心 | `TcWorldApis.climate()`、`TcHistory.view()`、`KnowledgeView` |
| (b) 模组入口 `XApi` | 一两个读者，且读者愿意写桥接类 | 软依赖提供者 | `ExchangeApi.get().quote(...)` |
| (c) 数据驱动 | 内容引用：物品、方块、物种、科技、配方 | 无代码依赖 | 数据包中写 `"input": "#c:ingots/bronze"` |
| (d) 事件 | 通知：某事发生了，谁关心谁订阅 | 订阅核心事件只依赖核心；订阅模组事件需桥接类 | `SettlementFoundedEvent` |
| (e) 扩展点 | 让其他模组向自己注册东西 | 被注册方：核心或提供者 | `RegisterRegionLayersEvent`、`CivPermissions.registerResolver` |

### 3.1 模式 (a)：核心托管契约

完整规则与契约清单在 03 §12.5，这里只概括：

1. 接口、记录与事件类放在 `com.nightcity.tcivilization.api.<area>`；
2. 提供者在 `FMLCommonSetupEvent` 内用 `event.enqueueWork(...)` 调用安装方法（公共初始化事件在各模组之间**并行**触发，所以安装必须排入主线程队列）；
3. 核心在提供者缺失时安装降级默认实现，读取方法**永不返回 null、永不抛异常**；
4. 读者需要知道“是否由真正的提供者实现”时，调用 `isProvidedBy(modid)`、`isLoaded()` 或 `isAvailable()`，禁止 `instanceof` 实现类；
5. 新增托管区域必须先登记到 03 §12.5，`api_index.py` 会把“非核心章节定义在核心包里、但 03 未登记的类型”报为 `api-core-hosted-unregistered`。

### 3.2 模式 (b)：模组入口 `XApi`

```java
// 提供者：com.nightcity.exchange.api.ExchangeApi
public interface ExchangeApi {
    static ExchangeApi get() { return ExchangeApiHolder.get(); }   // 公共初始化完成前调用：抛 IllegalStateException
    Quote quote(ServerLevel level, long regionKey, Identifier item, int amount);
    void registerDemandSource(Identifier id, DemandSource source);
}

// 读者：com.nightcity.settlements.compat.exchange.ExchangeBridge
public final class ExchangeBridge {
    private ExchangeBridge() {}
    public static void init() {                                     // 只在下方守卫内调用
        ExchangeApi.get().registerDemandSource(SettlementDemand.ID, new SettlementDemand());
    }
}

// 读者主类，公共初始化
event.enqueueWork(() -> {
    if (ModList.get().isLoaded("tcexchange")) ExchangeBridge.init();
});
```

规则：

1. **只有桥接类**可以 `import` 另一个主题模组的 `api` 包。主类、方块、实体、屏幕中出现对方类型即为缺陷：对方缺失时会抛 `NoClassDefFoundError`。
2. 守卫只用 `ModList.get().isLoaded(modid)`。桥接类的静态方法只能在守卫内调用；桥接类型不得出现在守卫外的字段、方法签名或泛型参数中（JVM 校验时可能提前加载）。
3. `get()` 的标准形式返回非空实例；在提供者公共初始化完成前调用时抛 `IllegalStateException`，开发环境下这是读者的初始化顺序错误。
4. 需要服务器状态的 API 不用 `Optional` 表达“未就绪”，而是把 `MinecraftServer` 或 `ServerLevel` 作为参数（如 03 的 `CivilizationApi.get(MinecraftServer)`）。章节中 `Optional<XApi> get()` 的写法统一改为上述形式（§15 中的 `entry-optional` 提示）。
5. 读者不得缓存提供者返回的可变对象跨 tick 使用；需要长期引用时保存 ID 或 UUID。
6. 每个 `XApi` 方法在 Javadoc 中标注线程（§4.2）与副作用；修改类方法必须走与载荷相同的权限检查（`CivPermissions.check`），除非调用方是服务端模拟本身。

### 3.3 模式 (c)、(d)、(e)

- **(c) 数据驱动。** 数据包条目中引用其他模组的内容时写 ID 或标签。引用的 ID 不存在（对方未安装）时，该字段按“缺失”处理：配方被跳过、战利品条目被移除、物种的食物列表忽略该项，并在重载摘要中计数，日志级别 `INFO`，不是错误。需要“对方存在才加载”的整条数据，用 NeoForge 加载条件 `neoforge:mod_loaded`。
- **(d) 事件。** 规则见 §5 与 §7。
- **(e) 扩展点。** 拥有者在模组总线上触发 `Register*Event`，或提供 `register*` 静态方法（只在初始化期有效，规则同 03 §12.5 规则 1）。注册内容在初始化结束后冻结；运行期新增内容只能通过数据包。

### 3.4 版本与稳定性

| 项 | 规则 |
| --- | --- |
| 版本常量 | 核心暴露 `TcApi.VERSION`（`int`，P0 = 1）。主题模组不单独设 API 版本，整个家族共用 `mod_version`（40 §2.1） |
| 依赖声明 | 主题模组对核心：`versionRange = "[<当前 minor>,)"`；主题模组之间的可选依赖：`versionRange = "[<当前 minor>,<下一 minor>)"`，避免混装不同阶段的模组 |
| 稳定面 | `api`、`api.event`、`client.api` 包中的公开类型；核心托管区域（§2.2） |
| 标注 | `@ApiStatus.Experimental`：可能在下一版本改动，读者自担风险；`@ApiStatus.Internal`：公开只因 Java 可见性需要，读者禁止使用；`@Deprecated(since = "0.N", forRemoval = true)`：保留至少 1 个版本（40 §2.2） |
| 演进 | v0.3-v0.9 只增不删。接口新增方法必须带 `default` 实现；枚举可以新增常量，读者的 `switch` 必须有 `default` 分支；`sealed` 接口在一个 `VERSION` 内冻结 |
| 记录 | API 记录只由提供者构造，读者只用访问器；需要读者构造的参数对象用构建器（`Quote.Request.builder()`），因为给记录增加分量会破坏规范构造器 |
| 破坏性修改 | 只在 `TcApi.VERSION` 提升时允许；提升时在 40 章更新日志列出全部变化 |

### 3.5 入口索引

由各章第 12 节中形如 `XApi.get(...)` 的入口生成。“获取方式”为章节原文。

<!-- GENERATED:entries BEGIN -->
| 入口类型 | 模组 | 包 | 获取方式 | 位置 |
| --- | --- | --- | --- | --- |
| `CivilizationApi` | 文明核心 | `com.nightcity.tcivilization.api` | `get(MinecraftServer server)` | 03-core-civilization.md:1109 |
| `DifficultyApi` | 科技核心 | `com.nightcity.technology.api` | `get()` | 04-core-technology.md:878 |
| `TechApi` | 科技核心 | `com.nightcity.technology.api` | `get()` | 04-core-technology.md:856 |
| `EcologyApi` | 生息 | `com.nightcity.ecology.api` | `get()` | 11-ecology.md:1027 |
| `CultivationApi` | 田园 | `com.nightcity.cultivation.api` | `get()` | 12-cultivation.md:1374 |
| `NetworksApi` | 通途 | `com.nightcity.networks.api` | `get()` | 14-networks.md:1183 |
| `DiscoveryApi` | 求知 | `com.nightcity.discovery.api` | `get()` | 17-discovery.md:1010 |
| `EchoesApi` | 遗响 | `com.nightcity.echoes.api` | `get()` | 18-echoes.md:1283 |
| `FrontiersApi` | 远征 | `com.nightcity.frontiers.api` | `get()` | 20-frontiers.md:1023 |
| `BeyondApi` | 异界 | `com.nightcity.beyond.api` | `get()` | 21-beyond.md:1235 |
| `AtlasApi` | 舆图 | `com.nightcity.atlas.api` | `get()` | 22-atlas.md:1372 |
| `NationsApi` | 列国 | `com.nightcity.nations.api` | `get()` | 23-nations.md:951 |
| `OrbitApi` | 寰宇 | `com.nightcity.orbit.api` | `get()` | 24-orbit.md:1324 |
<!-- GENERATED:entries END -->

### 3.6 公开类型索引

由各章第 12 节 Java 代码块中声明的 `interface`、`record`、`enum`、`class` 生成，按包分组。“（未声明）”表示代码块没有写 `package` 行，应补上（§15 中的 `type-no-package` 提示）。

<!-- GENERATED:types BEGIN -->
| 模组 | 包 | 接口 | 记录 | 枚举 | 类 |
| --- | --- | --- | --- | --- | --- |
| 文明核心 | `com.nightcity.tcivilization.api` | `ActionCheck`、`AlertProvider`、`CivilizationApi`、`KnowledgeView`、`MemoryApi`、`PermissionResolver`、`PersonApi`、`RegionApi`、`SettlementRegistry`、`TrackerApi`、`VanillaPolicyService`、`VersionedShared` | `ActionScope`、`CeremonySpec`、`CivMember`、`Civilization`、`MemoryEntry`、`MilestoneRecord`、`Notification`、`PersonRecord`、`SettlementRecord`、`TrackerPin` | `CeremonyScale`、`MonsterPolicy`、`Severity`、`VanillaSystem` | `Ceremonies`、`CivPermissions`、`ProficiencyBridge`、`TcApi`、`TcNotifications` |
| 文明核心 | `com.nightcity.tcivilization.api.history` | — | — | — | `TcHistory` |
| 文明核心 | `com.nightcity.tcivilization.api.value` | `ValuationProvider` | — | — | `VuFallback`、`VuValuation` |
| 文明核心 | `com.nightcity.tcivilization.api.world` | — | `WorldApiSet` | — | `EmissionLedger`、`TcWorldApis` |
| 文明核心 | `com.nightcity.tcivilization.client.api` | `CivPanelTabRegistry`、`CommandWheelRegistry`、`DeepLinkRegistry`、`HudWidgetRegistry`、`PersonSectionRegistry`、`TargetInfoProvider`、`WheelProvider` | `CivPanelTabSpec`、`HudWidgetSpec`、`WheelOption` | `CivTabGroup`、`HudLevel` | `CeremonyOverlay`、`CivPanel` |
| 科技核心 | `com.nightcity.technology.api` | `ContactProvider`、`CostFactorProvider`、`DifficultyApi`、`MilestoneChecker`、`ResearchSite`、`TechApi` | `EraDefinition`、`EraProgress`、`KnowledgeSource`、`TechDefinition`、`TechUnlocks` | `KnowledgeSourceType`、`TechEra`、`TechState` | — |
| 技能 | `com.nightcity.skills.api` | — | `GainResult` | `GainFlag` | `SkillsApi` |
| 技能 | `（未声明）` | — | `Apprenticeship`、`DeltaEntry`、`Fallback`、`GrowthLogEntry`、`Modifier`、`OpGrant`、`PendingGrant`、`PendingTokens`、`ProfileView`、`RepeatEntry`、`RosterSummary`、`SkillEntry`、`SkillProfile`、`SkillView`、`Specialty`、`TalentCommitPayload`、`TalentDef` | `LogType`、`Slot` | `ApprenticeshipEvent`、`ClientSkillsUpdatedEvent`、`GrowthLevelUpEvent`、`PlacedBlocks`、`ProficiencyGainEvent`、`ReflectionTokenEvent`、`SkillLevelUpEvent`、`SkillsSavedData`、`SpecialtyChangedEvent`、`TalentEffectsChangedEvent`、`TalentLearnedEvent`、`TalentRespecEvent` |
| 山河 | `com.nightcity.tcivilization.api.world` | `ClimateApi`、`GeoApi`、`HazardApi`、`HydroApi`、`LocalSeasonProvider`、`PlanetApi`、`ProspectingApi`、`SeasonalColorApi`、`SuitabilityApi`、`WeatherApi` | `Anomaly`、`ClimateNormals`、`ClimateSample`、`HistoricalTrace`、`LocalSeason`、`ProspectReading`、`SiteScore`、`WeatherState`、`WorldApiSet` | `ForecastSkill`、`ProspectMethod`、`TransportMode` | `PlanetProgressStages`、`TcWorldApis` |
| 山河 | `（未声明）` | — | `CivGeoKnowledge`、`ClimateLayerData`、`DepositState`、`ExplorerData`、`GeoLayerData`、`HazardRecord`、`HydroLayerData`、`LocalClimatePayload`、`RiverReachState`、`WildlandsWorldState` | `HazardType` | — |
| 生息 | `com.nightcity.ecology.api` | `EcologyApi` | — | `DisturbanceKind`、`HarvestSource`、`PopulationStatus` | — |
| 生息 | `（未声明）` | — | `EcosystemLayer`、`HerdRecord`、`PopulationRecord` | — | — |
| 田园 | `com.nightcity.cultivation.api` | `CultivationApi`、`WaterRightsProvider` | — | `WaterRule` | — |
| 田园 | `（未声明）` | — | `ChunkSoil`、`Freshness`、`LivestockData`、`SeedLot`、`SpecialtyProvenance`、`TraitVector`、`VesselData` | `WoolGrade` | `CropBlockEntity`、`SoilCell` |
| 工坊 | `com.nightcity.industry.api` | `ExternalHeatSource`、`IndustryFallbackReservoirs`、`MachineWearModifier` | — | — | `IndustryAbstractSimulator`、`IndustryApi`、`IndustryEnergyApi` |
| 工坊 | `com.nightcity.industry.data` | — | `AccidentRecord`、`IndustrialZone`、`LabourSlot`、`MaintenanceTicket`、`PowerGridRecord`、`ProcessRecipeRef`、`ReactorState`、`ShedGroup`、`ZoneStats` | — | — |
| 工坊 | `（未声明）` | — | `EmissionLayer` | — | — |
| 通途 | `com.nightcity.networks.api` | `NetworksApi` | `CargoLine`、`FreightEstimate`、`LogisticsOrder`、`RouteSegment`、`StationLedger`、`TrackEdge`、`TractionProfile`、`VehicleProxy`、`VehicleStats` | `SimTier` | — |
| 聚落 | `com.nightcity.settlements.api` | `CultureProvider`、`DirectiveGate`、`FoodStorageProvider`、`SettlementsApi`、`WorkplaceProvider` | `SettlementDirective`、`SettlementView`、`WorkplaceSpec` | — | — |
| 聚落 | `com.nightcity.settlements.data` | — | `CohortPool`、`EpidemicState`、`Household`、`ResidentData`、`RoomInstance`、`SettlementData` | — | — |
| 市集 | `com.nightcity.exchange.api` | `ExchangeApi` | `Quote`、`TreasuryAuth` | — | — |
| 市集 | `（未声明）` | — | `Account`、`CaravanPlan`、`CaravanStop`、`ContractTerms`、`CurrencyDef`、`Loan`、`MarketLedger`、`PriceEntry`、`ReputationRecord`、`TradeOrder` | `ContractState`、`MarketTier`、`ReputationScope` | — |
| 求知 | `com.nightcity.discovery.api` | `DigSiteProvider`、`DiscoveryApi`、`TrueValueProvider` | — | — | `JournalSectionRegistry` |
| 求知 | `com.nightcity.discovery.data` | — | `CarrierEntry`、`CivKnowledgeState`、`DataPoint`、`ExperimentDef`、`ExperimentRun`、`FacilityState`、`FieldRecordCell`、`Hypothesis`、`JournalEntry`、`KnowledgeFragment`、`ObservationTarget`、`WrittenKnowledge` | — | — |
| 遗响 | `com.nightcity.echoes.api` | `EchoesApi`、`WorldHistoryView` | — | — | — |
| 遗响 | `（未声明）` | — | `AncientMine`、`Culture`、`EchoClue`、`EventEdge`、`Extinction`、`HeritageEntry`、`HistoryCell`、`HistoryEvent`、`LegendInstance`、`LegendSeed`、`LiveHistoryEntry`、`Person`、`Polity`、`Provenance`、`Route`、`ScriptMastery`、`Site` | `PolityFate`、`ScriptType`、`SiteOrigin` | — |
| 契约 | `com.nightcity.commons.api` | `CommonsApi` | — | — | — |
| 契约 | `（未声明）` | — | `CivGovernance`、`Covenant`、`Parcel`、`Proposal`、`PublicWork`、`StatuteState`、`ViolationRecord` | — | — |
| 远征 | `com.nightcity.frontiers.api` | `CombatApi`、`DangerContributor`、`FrontiersApi`、`SupplyApi`、`WeaponMountApi` | `BattleResult`、`ForceSpec` | — | — |
| 远征 | `com.nightcity.frontiers.data` | — | `ArmourProfile`、`BanditBand`、`DangerComponents`、`Expedition`、`SupplyStock`、`WeaponProfile` | — | — |
| 异界 | `com.nightcity.beyond.api` | `BeyondApi` | — | — | — |
| 异界 | `（未声明）` | — | `AnomalyCell`、`Focus`、`FocusDynamics`、`WardRecord` | `BeyondMode`、`FocusType` | — |
| 舆图 | `com.nightcity.atlas.api` | `AncientMapContentProvider`、`AtlasApi`、`MapFeatureProvider`、`MapLayerProvider` | `AncientMapContent`、`AncientMapHeader`、`Annotation`、`AtlasCapabilities`、`ChunkKnowledge`、`KnowledgeHolder`、`MapFeature`、`MapSheet`、`SurveyStation`、`Waypoint` | `SurveyLevel` | — |
| 列国 | `com.nightcity.nations.api` | `BattleResolver`、`NationsApi` | `ArmyRecord`、`NationState`、`OpinionModifier`、`Personality`、`Regiment`、`RelationRecord`、`SiegeRecord`、`TreatyRecord`、`WarRecord` | — | — |
| 寰宇 | `com.nightcity.orbit.api` | `OrbitApi`、`PayloadHandler` | `PayloadSpec` | — | — |
| 寰宇 | `（未声明）` | — | `Colony`、`Consumables`、`DebrisState`、`Leg`、`Mission`、`MissionTarget`、`OrbitElements`、`OrbitalAsset`、`PressurizedVolume`、`RocketDesign`、`SpaceProgram`、`StageSpec` | `AssetStatus`、`Body`、`LifeSupportState`、`PropellantCombo` | — |
<!-- GENERATED:types END -->

---

## 4. 线程

### 4.1 线程表

02 §5 定义线程模型，本节规定每个线程**可以调用什么**。

| 线程 | 可调用 | 禁止 |
| --- | --- | --- |
| 服务器主线程 | 全部服务端 API；发布事件；修改世界、存档数据、地区层 | 阻塞超过 1 ms 的 I/O；等待后台线程结果 |
| 世界生成线程 | `TcWorldApis` 中标注 `@AnyThread` 的只读方法（星球、地质、气候、水文都来自不可变的 `planet.bin`）；`TcHistory.view()`（就绪后不可变） | 读写地区层、`SavedData`、`CivilizationApi`；发布事件 |
| `TCSimulationExecutor`（L2/L3 模拟） | 主线程准备的只读快照 | 访问任何 `Level`、实体、方块实体；发布事件 |
| `TCPathExecutor` | 地区图快照 | 同上 |
| 存档 I/O 线程 | 主线程拷贝出的不可变快照 | 读取活动对象 |
| 网络线程 | 载荷解码 | 处理逻辑（处理器保持 NeoForge 默认在主线程执行） |
| 客户端主线程（渲染线程） | `client.api`；HUD 数据缓存；客户端事件 | 访问服务端对象（集成服务器下也禁止） |
| 客户端后台（Atlas 瓦片） | 瓦片缓存的不可变快照 | 访问 `ClientLevel` 活动区块 |

### 4.2 标注与检查

1. 核心提供文档标注 `@MainThread` 与 `@AnyThread`（包 `com.nightcity.tcivilization.api.annotation`）。没有标注的 API 方法视为 `@MainThread`。
2. `TcThreads.assertMain()` 在 `general.api_strict_mode = true`（开发环境默认）时检查当前线程，不符时抛 `IllegalStateException`；生产环境只在首次违规时记录一次 `ERROR`。
3. 后台任务只产出**变更指令**列表，由主线程在 `ServerTickEvent.Post` 中按预算应用（02 §5）；应用指令时才发布相应事件，所以事件永远在主线程触发。
4. 每 tick 应用后台结果的预算：全家族合计 ≤ 2 ms；超出部分顺延到下一 tick，并计入 `/tcdebug` 的积压计数。

---

## 5. 事件总线

### 5.1 总线

| 总线 | 何时使用 | 例子 |
| --- | --- | --- |
| 模组总线（构造函数参数 `IEventBus modBus`） | 生命周期与注册：`RegisterEvent`、载荷注册、`FMLCommonSetupEvent`、`FMLClientSetupEvent`、核心触发的 `Register*Event` | `RegisterRegionLayersEvent` |
| 游戏总线 `NeoForge.EVENT_BUS`（服务端） | 运行期的家族事件与原版/NeoForge 游戏事件 | `EraAdvancedEvent` |
| 游戏总线（客户端） | 只在物理客户端触发的家族事件；类放在 `client.api` 子包 | `HudDataUpdatedEvent` |

注意：NeoForge 的模组总线事件可能在不同模组之间并行触发；核心自己的 `Register*Event` 由核心在主线程上**按模组加载顺序依次**触发，订阅者可以假设单线程。

### 5.2 事件类约定

1. 继承 `net.neoforged.bus.api.Event`；可取消的事件实现 `ICancellableEvent`。可取消性必须写在拥有者第 12 节事件表的“可取消”列（缺失时生成 `event-cancel-unknown` 提示）。
2. 字段 `final`，通过访问器读取；只有“结果型”事件（例如价格修正）提供明确的 setter，且在 Javadoc 中写清合并规则（取最大、相乘或最后写入）。
3. 事件携带 ID、UUID、`BlockPos`、`ResourceKey<Level>` 与不可变记录；需要世界对象时携带 `ServerLevel`，订阅者不得在事件返回后保留它。
4. 成对事件：`Pre` 在状态改变前触发、可取消；`Post` 在改变后触发、不可取消。只发布通知的事件用过去分词命名，不带 `Pre/Post`。
5. **频率上限**：任何事件类在标准场景（02 §8.1）下全服平均 ≤ 100 次/秒。逐 tick、逐实体的变化必须聚合后发布（例如 `EmissionRecordedEvent` 按地区每 1,000 tick 一批）。
6. 只在服务器主线程发布（§4.2.3）；客户端事件只在客户端主线程发布。
7. 重入：订阅者可以发布其他事件，但同一线程上的家族事件嵌套深度 ≤ 4；开发环境超出时抛异常，生产环境丢弃并记录 `ERROR`。

### 5.3 拥有权

1. 每个事件类只有一个拥有者：类放在拥有者的 `api.event` 包（核心为 `api` 下的对应区域），由拥有者章节第 12 节登记。
2. 需要**多个模组发布**、或需要**不依赖发布者也能订阅**的事件，类放在核心（`SeasonChangedEvent`、`SettlementFoundedEvent`、`SettlementTierChangedEvent`、`WorldHistoryReadyEvent`、`EmissionRecordedEvent` 等），在 03 §12.3 登记；其他模组的章节在事件表中写明“发布核心事件”。生成工具把这种情况报为 `event-shared` 提示，属于正常。
3. 订阅另一个主题模组的事件，必须在桥接类里注册监听器（类型引用规则同 §3.2.2）。

### 5.4 订阅约定

| 优先级 | 用途 |
| --- | --- |
| `HIGHEST` | 只限核心：权限与时代门槛（取消不合规的操作） |
| `HIGH` | 否决与规则：领地、法律、危险区（Commons、Frontiers） |
| `NORMAL` | 玩法响应 |
| `LOW` | 统计、熟练度、编年史记录 |
| `LOWEST` | 只读观察：界面刷新、调试日志 |

订阅者不得抛出异常；NeoForge 事件总线会把异常传给发布方。订阅者内部捕获异常并记录 `ERROR`，带上事件类名与模组 ID。

---

## 6. 网络载荷

### 6.1 ID 与定义

1. 载荷 ID 写全：`<modid>:<name>`，`name` 为蛇形小写。章节表格省略命名空间时，生成工具按章节模组补全并给出 `payload-no-namespace` 提示。
2. 每个载荷是一个 `record`，实现 `CustomPacketPayload`，带静态 `TYPE` 与 `STREAM_CODEC`：

```java
public record PanelRequestPayload(int requestId, Identifier tab, int page, String filter)
        implements CustomPacketPayload {
    public static final Type<PanelRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("tcivilization", "panel_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PanelRequestPayload::requestId,
                    Identifier.STREAM_CODEC, PanelRequestPayload::tab,
                    ByteBufCodecs.VAR_INT, PanelRequestPayload::page,
                    ByteBufCodecs.stringUtf8(64), PanelRequestPayload::filter,
                    PanelRequestPayload::new);
    @Override public Type<PanelRequestPayload> type() { return TYPE; }
}
```

3. 注册在模组总线的载荷注册事件中进行；注册器的版本字符串等于 `mod_version` 的 `major.minor`（31 §14.1.5）。改动任一载荷的字段时必须提升 `mod_version` 的 minor 或在同一 minor 内保证兼容。
4. 方向：`playToClient`（S→C）、`playToServer`（C→S）、`playBidirectional`（双向，仅在两端结构完全相同时使用）。章节表格的“方向”列只写 `S→C`、`C→S` 或 `双向`。
5. 处理器保持 NeoForge 默认的主线程执行，不切换到网络线程。

### 6.2 编解码

1. 所有字符串用有长度上限的编解码器（`ByteBufCodecs.stringUtf8(n)`）；所有集合用带上限的集合编解码器。超出上限时解码失败，载荷被丢弃（§6.3.6）。
2. 载荷中不放玩家 UUID 作为身份；身份只取自处理器上下文中的玩家（31 §14.5.1）。
3. 位置用 `BlockPos`；地区用 `long regionKey`；维度用 `ResourceKey<Level>`；内容用 `Identifier`；物品用 `ItemStack.STREAM_CODEC`。
4. **大小**：S→C 单包 ≤ 64 KiB（31 §14.1.4；原版硬上限远高于此，64 KiB 是本家族的预算）；C→S 单包 ≤ 30 KiB（原版服务端自定义载荷上限约 32 KiB，留余量）。超过时由模组自行分片：载荷带 `uploadId`、`part`、`total`，服务端在 10 s 内重组，单次上传 ≤ 256 KiB，每片计入限流。
5. 面板快照的结构由各模组的页签提供者定义并用自己的 `StreamCodec` 编码，外层由核心 `panel_snapshot` 包装（03 §12.4）。

### 6.3 限流与校验

1. **默认限流**：章节未写数值时，C→S 载荷按每玩家每载荷类型 ≤ 4 次/秒（令牌桶，容量 8）。生成工具把这些载荷报为 `payload-default-rate` 提示，并在 §6.6 的“频率上限”列显示“默认”。
2. 模组在注册载荷时用 `TcNet.limit(TYPE, perSecond, burst)` 声明自己的上限；核心的限流器统一计数。
3. **距离**：涉及方块或实体的请求，目标必须在玩家 8 格内且已加载（31 §14.5.2）；远程操作（地图、面板）不检查距离，但检查权限。
4. **权限与身份**：修改类请求一律经 `CivPermissions.check`（31 §16.1）并检查文明成员身份。
5. **并发**：修改共享对象的请求携带客户端所见的 `version`；不一致时返回 `action_result{ok=false, reason=conflict}` 与最新快照（31 §16.3）。
6. **畸形与超频**：解码失败的载荷丢弃并记录 `DEBUG`，不回显异常；超频的载荷丢弃并计数，同一玩家 10 s 内丢弃超过 200 个时记录一次 `WARN`，不踢出（31 §14.5.5）。
7. **应答**：每个修改类 C→S 请求带客户端分配的递增 `requestId`，服务端必须以核心 `tcivilization:action_result`（带回 `requestId`）或模组自己的结果载荷应答；模组结果载荷中的拒绝原因也必须使用 §14.1 的原因 ID。

### 6.4 订阅、快照与增量

1. **文明面板页签**的数据统一走核心 `panel_request` / `panel_snapshot`；模组不为页签另建请求载荷。
2. **HUD 控件**的数据统一走核心 `hud_subscribe` / `hud_data`（31 §14.3）；通道 ID 等于控件 ID。
3. 模组自有载荷只用于：专用界面（机器、地图、科技树以外的独立屏幕）、世界内的即时交互、流式数据（地图瓦片、粒子与动画提示）。
4. 服务端为每玩家每通道保存上次发送内容的 64 位哈希；内容不变时不发包（31 §14.1.3）。
5. 列表每页 50 条；需要更多时客户端翻页请求，不一次推送全部。

### 6.5 带宽预算（每玩家）

| 来源 | 稳态平均 | 峰值 |
| --- | --- | --- |
| 核心：HUD、面板、通知、追踪、轮盘、人物、地区层同步 | ≤ 4 KiB/s | ≤ 32 KiB/s |
| Atlas：瓦片与要素 | 追平后 ≈ 0；同步中 ≤ 16 KiB/s | ≤ 64 KiB/s（令牌桶） |
| 其他模组自有载荷合计 | ≤ 2 KiB/s | ≤ 16 KiB/s |

以 31 §14.4 为准；本表只是引用，便于模组作者核对自己的载荷。

### 6.6 载荷索引

由各章第 12 节载荷表生成。“频率上限”列中的“默认”表示适用 §6.3.1；“—”表示 S→C 载荷未写频率（受 §6.5 带宽约束）。

<!-- GENERATED:payloads BEGIN -->
| 负载 ID | 方向 | 频率上限 | 大小 | 内容 | 位置 |
| --- | --- | --- | --- | --- | --- |
| `tcivilization:action_result` | S→C | 随请求 | — | 回应任一修改类 C→S 请求：requestId（客户端为每个修改请求分配的递增 int，原样带回）、ok、reason（ActionCheck 原因 ID 或 | 03-core-civilization.md:1347 |
| `tcivilization:action_undo` | C→S | ≤ 2 次/s | — | 撤销令牌（31 §16.4） | 03-core-civilization.md:1348 |
| `tcivilization:ceremony` | S→C | 事件；同一玩家同时只排队 1 个大型仪式 | — | CeremonySpec | 03-core-civilization.md:1333 |
| `tcivilization:civ_state_sync` | S→C | 变化时 + 登录 | — | 文明名、旗帜、时代、日历、成员摘要 | 03-core-civilization.md:1330 |
| `tcivilization:dialogue_choice` | C→S | ≤ 4 次/s | — | 选项索引 | 03-core-civilization.md:1346 |
| `tcivilization:dialogue_open` | S→C | 事件 | — | 对话节点 | 03-core-civilization.md:1345 |
| `tcivilization:hud_data` | S→C | 每通道 ≤ 1 次/10 tick | — | 通道数据 | 03-core-civilization.md:1335 |
| `tcivilization:hud_subscribe` | C→S | ≤ 2 次/s | — | 订阅/退订 HUD 数据通道（可见控件集合变化，去抖 10 tick） | 03-core-civilization.md:1334 |
| `tcivilization:notify` | S→C | 限流见 5.10.5 | — | Notification | 03-core-civilization.md:1332 |
| `tcivilization:panel_presence` | S→C | 变化时，≤ 1 次/s | — | 同一共享对象的查看者列表（≤ 4 + “+N”）与 30 s 软锁持有者（31 §16.2）；只在同一文明内 | 03-core-civilization.md:1349 |
| `tcivilization:panel_request` | C→S | ≤ 4 次/s | — | 页签 ID、分页参数 | 03-core-civilization.md:1336 |
| `tcivilization:panel_snapshot` | S→C | 应答 + 打开期间每 40 tick 增量；单包 ≤ 64 KiB | — | 页签数据（各模组 StreamCodec） | 03-core-civilization.md:1337 |
| `tcivilization:person_inspect_request` | C→S | ≤ 2 次/s | — | 人物 UUID | 03-core-civilization.md:1343 |
| `tcivilization:person_inspect_snapshot` | S→C | 应答 + 打开期间每 20 tick | — | 人物记录 + 各分节数据 | 03-core-civilization.md:1344 |
| `tcivilization:region_layer_sync` | S→C | 每层每地区 ≤ 1 次/40 tick | — | 3×3 地区内 NEAR 层数据 | 03-core-civilization.md:1331 |
| `tcivilization:tracker_pin` | C→S | ≤ 2 次/s | — | 固定/取消 | 03-core-civilization.md:1341 |
| `tcivilization:tracker_sync` | S→C | 每 20 tick（有变化时） | — | 3 个固定目标的标签与进度 | 03-core-civilization.md:1342 |
| `tcivilization:wheel_execute` | C→S | ≤ 4 次/s；同一选项 10 tick 防抖 | — | 选项 ID + 上下文 | 03-core-civilization.md:1340 |
| `tcivilization:wheel_options` | S→C | 应答 | — | List<WheelOption> | 03-core-civilization.md:1339 |
| `tcivilization:wheel_query` | C→S | ≤ 4 次/s | — | 准星目标 | 03-core-civilization.md:1338 |
| `tctechnology:declare_era` | C→S | 用户操作；服务端复核 5.7 条件 | — | 宣布新时代请求 | 04-core-technology.md:927 |
| `tctechnology:era_advanced` | S→C | 每次时代跃迁 1 次；客户端播放仪式，Atlas 切皮肤 | — | civ、from、to、回顾数据、新动词 | 04-core-technology.md:929 |
| `tctechnology:knapping_strike` | C→S | 用户操作；每 tick ≤ 4 次 | — | 网格格子索引 | 04-core-technology.md:928 |
| `tctechnology:research_command` | C→S | 用户操作；服务端校验权限与距离 | — | 设为研究、队列、投入研究池、主持开关 | 04-core-technology.md:926 |
| `tctechnology:tech_defs_sync` | S→C | 登录与重载时；≤ 64 KB，超出分片 | — | 全部科技与时代定义（压缩） | 04-core-technology.md:923 |
| `tctechnology:tech_delta` | S→C | 每 20 tick 合并，仅变化时；≤ 1 KB | — | 状态/知识/进度增量 | 04-core-technology.md:925 |
| `tctechnology:tech_state_sync` | S→C | 登录时；≤ 16 KB | — | 玩家文明完整状态 | 04-core-technology.md:924 |
| `tcskills:apprentice_request` | C→S | 默认 | — | — | 05-skills.md:1768 |
| `tcskills:goal_pin` | C→S | 默认 | — | — | 05-skills.md:1771 |
| `tcskills:history_page` | S→C | — | — | — | 05-skills.md:1776 |
| `tcskills:history_request` | C→S | 默认 | — | — | 05-skills.md:1770 |
| `tcskills:proficiency_delta` | S→C | — | — | — | 05-skills.md:1773 |
| `tcskills:profile_request` | C→S | 默认 | — | — | 05-skills.md:1769 |
| `tcskills:profile_snapshot` | S→C | — | — | — | 05-skills.md:1772 |
| `tcskills:respec_request` | C→S | 默认 | — | — | 05-skills.md:1765 |
| `tcskills:roster_request` | C→S | 默认 | — | — | 05-skills.md:1767 |
| `tcskills:roster_snapshot` | S→C | — | — | — | 05-skills.md:1774 |
| `tcskills:sense_highlight` | S→C | — | — | — | 05-skills.md:1777 |
| `tcskills:specialty_set` | C→S | 默认 | — | — | 05-skills.md:1766 |
| `tcskills:talent_commit` | C→S | 默认 | — | — | 05-skills.md:1764 |
| `tcskills:talent_result` | S→C | — | — | — | 05-skills.md:1775 |
| `tcwildlands:calendar_sync` | S→C | 登录 + 每 MC 日 | 24 B | 年、日、全局季节、日长参数 | 10-wildlands.md:2646 |
| `tcwildlands:flow_field` | S→C | 玩家距河道 ≤ 64 格时，区块进入视距时发送 | 40 B/区块 | 一个区块的 L1 流场（4 × 4 个采样，i8 向量） | 10-wildlands.md:2660 |
| `tcwildlands:hazard_state` | S→C | 变化时 | ≤ 2 KB | 玩家 4,096 格内的预警与进行中灾害摘要 | 10-wildlands.md:2649 |
| `tcwildlands:local_climate` | S→C | 每 20 tick（仅 EXPANDED 玩家） | ≤ 64 B | HUD 控件数据（如上） | 10-wildlands.md:2648 |
| `tcwildlands:planet_preview` | S→C | 每阶段一次 | ≤ 40 KB | 256 × 128 调色板索引图 | 10-wildlands.md:2656 |
| `tcwildlands:planet_progress` | S→C | 每 250 ms（合并本模组与追加阶段的 report） | ≤ 96 B | 阶段下标（PlanetProgressStages.stages() 顺序，含追加阶段）、轮次、百分比、可选明细文本 | 10-wildlands.md:2655 |
| `tcwildlands:prospect_result` | S→C | 每次仪器读数 | ≤ 4 KB | ProspectReading 摘要 | 10-wildlands.md:2652 |
| `tcwildlands:quake_shake` | S→C | 地震时 | 12 B | 震动强度、持续 tick、方向 | 10-wildlands.md:2650 |
| `tcwildlands:region_summary` | S→C | 响应请求 | ≤ 16 KB | 页签快照（地区、气候、水文、勘探、灾害） | 10-wildlands.md:2658 |
| `tcwildlands:region_summary_request` | C→S | 打开页签/翻页时；限速 4 次/秒 | ≤ 128 B | 页签子页、地区 key、查询参数 | 10-wildlands.md:2657 |
| `tcwildlands:rock_tap_result` | S→C | 每次敲击 | ≤ 256 B | 岩石 ID、识别等级、矿化提示 | 10-wildlands.md:2651 |
| `tcwildlands:start_site_choice` | C→S | 一次（服务端校验权限） | 16 B | mode、候选索引或自定义坐标 | 10-wildlands.md:2654 |
| `tcwildlands:start_site_options` | S→C | 一次 | ≤ 32 KB | 5 个候选（坐标、评分、气候、64×64 地形缩略图） | 10-wildlands.md:2653 |
| `tcwildlands:weather_cells` | S→C | 每 200 tick 或变化时 | ≤ 600 B | 玩家周围 5 × 5 天气格的 WeatherState 压缩表 | 10-wildlands.md:2647 |
| `tcwildlands:wrap_preload` | S→C | 距接缝 ≤ 256 格时一次 | 16 B | 即将跨越环绕接缝的提示，客户端预取接缝另一侧区块 | 10-wildlands.md:2659 |
| `tcecology:burn_confirm` | C→S | 每次交互 | — | 位置、确认令牌 | 11-ecology.md:1085 |
| `tcecology:codex_sighting` | C→S | ≤ 1 次/40 tick；服务端校验距离与视线 | — | 实体 ID 列表（客户端视线检测结果） | 11-ecology.md:1083 |
| `tcecology:codex_update` | S→C | 事件驱动 | — | 物种、新等级、进度 | 11-ecology.md:1084 |
| `tcecology:collar_update` | S→C | 每 100 tick | — | 项圈个体位置（仅打开面板/固定追踪时） | 11-ecology.md:1087 |
| `tcecology:pen_action` | C→S | 权限校验 | — | 繁育栏操作（配对、放归、复原） | 11-ecology.md:1088 |
| `tcecology:reserve_edit` | C→S | 权限校验 + 版本号 | — | 保护区 ID、多边形顶点、规则 | 11-ecology.md:1086 |
| `tcecology:track_request` | C→S | ≤ 1 次/10 tick/玩家 | — | 中心区块、半径 | 11-ecology.md:1081 |
| `tcecology:track_sync` | S→C | 应答 | — | 区块 → TrackEntry 列表（仅玩家可见字段，按认知等级裁剪） | 11-ecology.md:1082 |
| `tccultivation:allocate_water` | C→S | 默认 | ≤ 256 B | — | 12-cultivation.md:1437 |
| `tccultivation:animal_inspect` | S→C | — | ≤ 512 B | — | 12-cultivation.md:1433 |
| `tccultivation:field_status` | S→C | — | ≤ 96 B，状态变化时或 20 tick | — | 12-cultivation.md:1431 |
| `tccultivation:herd_action` | C→S | 默认 | ≤ 600 B | — | 12-cultivation.md:1438 |
| `tccultivation:hitch` | C→S | 默认 | 12 B | — | 12-cultivation.md:1439 |
| `tccultivation:name_variety` | C→S | 默认 | ≤ 100 B | — | 12-cultivation.md:1435 |
| `tccultivation:seed_select` | C→S | 默认 | 8 B | — | 12-cultivation.md:1434 |
| `tccultivation:set_rotation` | C→S | 默认 | ≤ 120 B | — | 12-cultivation.md:1436 |
| `tccultivation:soil_probe` | S→C | — | 16 B | — | 12-cultivation.md:1432 |
| `tcindustry:accident_fx` | S→C | 事件 | — | 事故特效（爆炸、蒸汽、熔融溢出）位置与强度 | 13-industry.md:2592 |
| `tcindustry:forge_strike` | C→S | 20/s | — | {pos, clientTick, assisted} | 13-industry.md:2596 |
| `tcindustry:grid_dispatch` | C→S | 5/s | — | 投切机组、断路器、切负荷顺序 + version | 13-industry.md:2600 |
| `tcindustry:machine_anim` | S→C | 4 Hz | — | 64 格内机器动画参数 | 13-industry.md:2593 |
| `tcindustry:machine_config` | C→S | 10/s | — | 配方选择、齿轮比、优先级、管口 | 13-industry.md:2595 |
| `tcindustry:machine_sync` | S→C | 4 Hz/界面 | — | 打开界面的机器状态增量 | 13-industry.md:2589 |
| `tcindustry:multiblock_preview` | S→C | 按请求 | — | 结构模板与缺失位置 | 13-industry.md:2591 |
| `tcindustry:network_annotations` | S→C | 开启时 1 Hz | — | 网络标注（≤ 256 项） | 13-industry.md:2590 |
| `tcindustry:plc_upload` | C→S | 1/s | — | 梯形图（≤ 4 KB） | 13-industry.md:2597 |
| `tcindustry:production_action` | C→S | 5/s | — | 页签操作：改区名、维护队列排序、请求快照 + version | 13-industry.md:2601 |
| `tcindustry:production_snapshot` | S→C | 页签打开时 1/40 tick | — | 生产页签快照（≤ 64 KiB，分页） | 13-industry.md:2594 |
| `tcindustry:reactor_command` | C→S | 5/s | — | 控制棒、停堆、换料 + version | 13-industry.md:2599 |
| `tcindustry:sorter_filter` | C→S | 10/s | — | 过滤槽设置 | 13-industry.md:2598 |
| `tcnetworks:dispatch_action` | C→S | 5/s | — | 订单新建/暂停/取消、批次确认（附带数据版本号） | 14-networks.md:1234 |
| `tcnetworks:proxy_positions` | S→C | 1/s | — | 打开物流页签时，L1/L2 载具的估算位置（分页 ≤ 256 条） | 14-networks.md:1238 |
| `tcnetworks:route_plan_edit` | C→S | 5/s | — | 规划点增删、宽度、路面类型 | 14-networks.md:1233 |
| `tcnetworks:route_plan_preview` | S→C | 按需 | — | 规划预览几何与材料统计 | 14-networks.md:1232 |
| `tcnetworks:signal_sync` | S→C | 4/s（区域） | — | 128 格内信号显示变化 | 14-networks.md:1236 |
| `tcnetworks:telegraph_message` | 双向 | 1/s | — | 电报文本（≤ 256 字）与收件确认 | 14-networks.md:1237 |
| `tcnetworks:timetable_edit` | C→S | 2/s | — | 时刻表整体替换（≤ 64 条指令） | 14-networks.md:1235 |
| `tcnetworks:track_graph_delta` | S→C | 按需 | — | 玩家 256 格内的轨道图增量（节点/边/分区状态） | 14-networks.md:1231 |
| `tcnetworks:vehicle_control` | C→S | 20/s | — | 油门级位、转向、喇叭、制动、反向；服务端校验驾驶员身份 | 14-networks.md:1229 |
| `tcnetworks:vehicle_state` | S→C | 10/s（附近） | — | 速度、手柄、信号前瞻、燃料、车况（供 HUD）；只发给乘客 | 14-networks.md:1230 |
| `tcsettlements:building_plan_place` | C→S | 校验领地权限（Commons） | — | 蓝图物品槽、位置、旋转、镜像 | 15-settlements.md:1228 |
| `tcsettlements:directive_toggle` | C→S | 经 DirectiveGate 校验 | — | 聚落 id、指令 id、开/关 | 15-settlements.md:1227 |
| `tcsettlements:health_alert` | S→C | 疫情期间每 MC 日 1 次 | — | 各聚落疫情摘要（病种、I、R0 估计） | 15-settlements.md:1229 |
| `tcsettlements:job_assign` | C→S | 服务器校验距离与权限 | — | 人物 UUID、岗位位置、pin 标志、优先级 | 15-settlements.md:1224 |
| `tcsettlements:resident_info` | S→C | 响应 | — | “职业与住所”分区数据 | 15-settlements.md:1223 |
| `tcsettlements:resident_info_request` | C→S | 打开人物界面时；限速 4 次/秒 | — | 人物 UUID | 15-settlements.md:1222 |
| `tcsettlements:room_scan_result` | S→C | 准星指向门/床时，≤ 2 次/秒 | — | 房间类型、面积、Q、缺陷码列表 | 15-settlements.md:1225 |
| `tcsettlements:settlement_sync` | S→C | 进入边界时 + 变化时，≤ 1 次/100 tick | — | HUD 数据：聚落 id、名、等级、人口、满意度、警报数 | 15-settlements.md:1221 |
| `tcsettlements:zone_paint` | C→S | 拖动时批量，≤ 10 次/秒 | — | 格坐标列表（≤ 64）、分区类型 | 15-settlements.md:1226 |
| `tcexchange:bank_action` | C→S | 校验 tcexchange:bank | — | 存、取、借、还、发行纸币 | 16-exchange.md:1505 |
| `tcexchange:caravan_plan` | C→S | ≤ 1 次/秒 | — | 商队计划（≤ 8 经停点）+ 版本号 | 16-exchange.md:1503 |
| `tcexchange:contract_edit` | C→S | 版本不符返回冲突与差异 | — | 合同 id、版本号、改动的条款 | 16-exchange.md:1501 |
| `tcexchange:contract_sign` | C→S | 双方签字后锁定 | — | 合同 id、版本号 | 16-exchange.md:1502 |
| `tcexchange:mint_request` | C→S | 校验 tcexchange:mint | — | 铸币台位置、锭数、印模槽 | 16-exchange.md:1504 |
| `tcexchange:order_cancel` | C→S | — | — | 订单 id + 版本号 | 16-exchange.md:1500 |
| `tcexchange:order_place` | C→S | ≤ 2 次/秒；每玩家挂单上限（5.9） | — | 订单字段 + 版本号 | 16-exchange.md:1499 |
| `tcexchange:ticker_sync` | S→C | 范围内每 20 tick，≤ 256 B | — | HUD 3 行行情 | 16-exchange.md:1496 |
| `tcexchange:trade_offer` | C→S | 校验距离 ≤ 8、权限；≤ 4 次/秒 | — | 交易界面的买卖清单、出价 | 16-exchange.md:1497 |
| `tcexchange:trade_result` | S→C | 响应 | — | 成交明细或拒绝原因（action_result） | 16-exchange.md:1498 |
| `tcdiscovery:book_compose` | C→S | 每秒 ≤ 2 | — | 选择条目、书名 | 17-discovery.md:1067 |
| `tcdiscovery:dig_record` | C→S | 每秒 ≤ 4 | — | 发掘桩网格与层记录请求 | 17-discovery.md:1068 |
| `tcdiscovery:experiment_action` | C→S | 每秒 ≤ 4 | — | 开始/取消实验、选择实验 | 17-discovery.md:1065 |
| `tcdiscovery:experiment_update` | S→C | 每 20 tick 一次 | — | 运行进度、新数据点 | 17-discovery.md:1066 |
| `tcdiscovery:facility_config` | C→S | 每秒 ≤ 4 | — | 专精、学者分配、研究目标 | 17-discovery.md:1069 |
| `tcdiscovery:hypothesis_action` | C→S | 每秒 ≤ 10 | — | 放入/移出碎片、检验、改名 | 17-discovery.md:1064 |
| `tcdiscovery:known_properties_sync` | S→C | 登录全量 + 增量 | — | 本文明已知性质集合增量（用于 TargetInfo） | 17-discovery.md:1063 |
| `tcdiscovery:observation_result` | S→C | 事件驱动 | — | 目标、价值、碎片、首次标记、本区域计数 | 17-discovery.md:1062 |
| `tcdiscovery:observe_request` | C→S | 每秒 ≤ 4 | — | 目标类型（方块位置/实体 ID）、观察 tick 数 | 17-discovery.md:1061 |
| `tcechoes:appraisal_result` | S→C | 事件驱动 | — | 鉴定级别、显示文本、掌握度变化 | 18-echoes.md:1346 |
| `tcechoes:appraise_request` | C→S | 每秒 ≤ 2 | — | 碑铭台槽位、比对 / 解读 | 18-echoes.md:1345 |
| `tcechoes:chronicle_compile` | C→S | 每秒 ≤ 1 | — | 编纂编年史书的条目选择 | 18-echoes.md:1349 |
| `tcechoes:heritage_edit` | C→S | 每秒 ≤ 2 | — | 遗产登记修改，带版本号（VersionedShared） | 18-echoes.md:1348 |
| `tcechoes:history_preview` | S→C | 每阶段一次 | — | 大陆轮廓位图（256×256）与 ≤ 12 条兴衰摘要 | 18-echoes.md:1342 |
| `tcechoes:history_progress` | S→C | 每 10 tick 一次 | — | 模拟阶段、百分比、当前年份 | 18-echoes.md:1341 |
| `tcechoes:legend_sync` | S→C | 登录全量 + 增量 | — | 本文明传说增量（状态、传闻圈、线索链） | 18-echoes.md:1343 |
| `tcechoes:script_sync` | S→C | 变化时 | — | 文字掌握度增量 | 18-echoes.md:1347 |
| `tcechoes:site_dossier` | 双向 | 每秒 ≤ 4 | — | 请求 / 返回一个遗址档案 | 18-echoes.md:1344 |
| `tcechoes:stele_text` | C→S | 每秒 ≤ 1 | — | 石碑铭文（≤ 4 行 × 32 字） | 18-echoes.md:1350 |
| `tccommons:ballot_cast` | C→S | 默认 | — | 提案草稿 / 提案 ID + 选择 | 19-commons.md:1267 |
| `tccommons:case_rule` | C→S | 默认 | — | 案件裁决 / 石碑位置 | 19-commons.md:1270 |
| `tccommons:claim_edit` | C→S | 默认 | — | 选区区块、类型、确认 | 19-commons.md:1264 |
| `tccommons:claim_sync` | S→C | — | — | 视距内区块的地块增量（进入维度时全量） | 19-commons.md:1271 |
| `tccommons:covenant_edit` | C→S | 默认 | — | 对象 + version | 19-commons.md:1269 |
| `tccommons:covenant_sign` | C→S | 默认 | — | 对象 + version | 19-commons.md:1269 |
| `tccommons:office_assign` | C→S | 默认 | — | 成员、角色或职位、version | 19-commons.md:1266 |
| `tccommons:parcel_settings` | C→S | 默认 | — | parcel、标志、角色覆盖、version | 19-commons.md:1265 |
| `tccommons:proposal_submit` | C→S | 默认 | — | 提案草稿 / 提案 ID + 选择 | 19-commons.md:1267 |
| `tccommons:role_assign` | C→S | 默认 | — | 成员、角色或职位、version | 19-commons.md:1266 |
| `tccommons:statute_decree` | C→S | 默认 | — | 法令、选项（直接颁布） | 19-commons.md:1268 |
| `tccommons:stele_dedicate` | C→S | 默认 | — | 案件裁决 / 石碑位置 | 19-commons.md:1270 |
| `tccommons:vote_update` | S→C | — | — | 提案票数与表决者表态（仅打开投票页的客户端） | 19-commons.md:1272 |
| `tccommons:works_edit` | C→S | 默认 | — | 对象 + version | 19-commons.md:1269 |
| `tcfrontiers:band_intel_sync` | S→C | 情报变化时 | — | 玩家已知的盗匪团情报（位置精度随侦察等级） | 20-frontiers.md:1096 |
| `tcfrontiers:bounty_list_sync` | S→C | 打开告示板时 | — | 悬赏列表 | 20-frontiers.md:1092 |
| `tcfrontiers:danger_sync` | S→C | 进入新地区时 + 每 100 tick 增量 | — | 玩家周围 5×5 地区的分量（每地区 8 字节） | 20-frontiers.md:1087 |
| `tcfrontiers:expedition_plan_update` | C→S | 每 5 tick 1 次 | — | 草稿编辑（目标、路线、编组、补给）+ 版本号 | 20-frontiers.md:1089 |
| `tcfrontiers:expedition_state_sync` | S→C | 20 tick | — | HUD 数据：剩余天数、补给、士气 | 20-frontiers.md:1090 |
| `tcfrontiers:militia_order` | C→S | 每 10 tick 1 次 | — | 民兵命令（经核心指令轮盘转发时不使用） | 20-frontiers.md:1093 |
| `tcfrontiers:outpost_sync` | S→C | 打开时 + 40 tick | — | 前哨面板快照 | 20-frontiers.md:1091 |
| `tcfrontiers:siege_engine_control` | C→S | 每 2 tick 1 次 | — | 仰角、方位、开火 | 20-frontiers.md:1094 |
| `tcfrontiers:threat_alert` | S→C | 每目标每 5 秒 1 次 | — | 敌人方向、类型、距离（瞭望发现） | 20-frontiers.md:1088 |
| `tcfrontiers:weapon_action` | C→S | 每 2 tick 1 次 | — | 装填开始/中断、射击模式切换 | 20-frontiers.md:1095 |
| `tcbeyond:beyond_mode_sync` | S→C | 登录、切换时 | — | 模式、end.rework、渗入系数 | 21-beyond.md:1289 |
| `tcbeyond:gate_action` | C→S | 操作时 | — | 门塔调谐、门芯确认 | 21-beyond.md:1296 |
| `tcbeyond:meter_sync` | S→C | ≤ 1 次/20 tick，仅变化时 | — | I_block、主导焦点 ID 与名、趋势、档位、状态位 | 21-beyond.md:1290 |
| `tcbeyond:relay_action` | C→S | 操作时 | — | 配对、断开 | 21-beyond.md:1295 |
| `tcbeyond:rite_preview` | S→C | 应答 | — | §5.8.5 的字段 | 21-beyond.md:1293 |
| `tcbeyond:rite_preview_request` | C→S | ≤ 1 次/10 tick | — | 法式 ID、位置 | 21-beyond.md:1292 |
| `tcbeyond:waystone_action` | C→S | 操作时（服务端校验距离 ≤ 6 格） | — | 绑定 / 传送目的地 | 21-beyond.md:1294 |
| `tcbeyond:zone_event` | S→C | 事件时 | — | 涌潮开始/结束、蔓延（驱动音景与天空色） | 21-beyond.md:1291 |
| `tcatlas:annotation_edit` | C→S | 玩家编辑时 | ≤ 4 KB | 注释编辑（新增/修改/删除） | 22-atlas.md:1472 |
| `tcatlas:annotation_sync` | S→C | 变更 | ≤ 4 KB | 注释 | 22-atlas.md:1471 |
| `tcatlas:capability_sync` | S→C | 登录、科技变化 | 200 B | AtlasCapabilities | 22-atlas.md:1463 |
| `tcatlas:knowledge_mask_sync` | S→C | 登录、区域变化 | 10 KB / 1.5 KB | L9 概览掩码或区域 SL 掩码 | 22-atlas.md:1464 |
| `tcatlas:layer_data_request` | C→S | 打开图层时 | ≤ 1 KB | 图层 ID + 区域 | 22-atlas.md:1473 |
| `tcatlas:layer_data_sync` | S→C | 响应请求 | ≤ 16 KB | 采样结果 | 22-atlas.md:1474 |
| `tcatlas:live_track_sync` | S→C | 1 Hz | ≤ 2 KB | 实时对象（雷达接触、文明成员、载具、天气符号） | 22-atlas.md:1475 |
| `tcatlas:map_sheet_sync` | S→C | 切换手持时 | ≤ 32 KB | 手持图纸的区块数据 | 22-atlas.md:1478 |
| `tcatlas:map_tile_request` | C→S | 平移地图时 | ≤ 1 KB | 区域坐标列表 + 本地 tileVersion | 22-atlas.md:1465 |
| `tcatlas:map_tile_sync` | S→C | 按请求，受令牌桶限制 | ≤ 200 KB，分片 32 KB | 区域摘要（Zstd） | 22-atlas.md:1466 |
| `tcatlas:memory_sync` | S→C | 每 5 秒 | ≤ 2 KB | 个人记忆增量（seenTime） | 22-atlas.md:1467 |
| `tcatlas:nav_state_sync` | S→C | 4 Hz（静止时 1 Hz） | 40 B | 估计位置偏差、ε_p、偏角校正位、导航目标 | 22-atlas.md:1468 |
| `tcatlas:ping` | 双向 | 玩家操作，每人 1 次/2 秒 | 40 B | 临时标记位置与发起人 | 22-atlas.md:1479 |
| `tcatlas:survey_action` | C→S | 玩家操作 | ≤ 200 B | 仪器操作（类型、目标、小游戏偏差） | 22-atlas.md:1476 |
| `tcatlas:survey_result` | S→C | 每次测量 | ≤ 500 B | 读数、闭合差、获得 SL 的区块数 | 22-atlas.md:1477 |
| `tcatlas:waypoint_edit` | C→S | 玩家操作 | ≤ 300 B | 新增/修改/删除 | 22-atlas.md:1470 |
| `tcatlas:waypoint_sync` | S→C | 登录、变更 | ≤ 64 KB | 路点全量或增量 | 22-atlas.md:1469 |
| `tcnations:army_order` | C→S | 默认 | — | 军议桌命令（军队、命令、目标地区） | 23-nations.md:1003 |
| `tcnations:army_snapshot` | S→C | — | — | 战区地图上的军队（已侦察部分） | 23-nations.md:1004 |
| `tcnations:border_overlay` | S→C | — | — | 玩家附近 5×5 地区的归属（territory_entry 控件与地图片段） | 23-nations.md:1005 |
| `tcnations:diplomacy_snapshot` | S→C | — | — | 打开外交页签时的已知文明列表、关系、条约（只含该玩家文明已知的信息） | 23-nations.md:997 |
| `tcnations:proposal_response` | S→C | — | — | AI 答复（接受/还价/拒绝 + 理由） | 23-nations.md:1000 |
| `tcnations:proposal_submit` | C→S | 默认 | — | 草案（类型、条款、送达方式）；服务端校验权限与科技 | 23-nations.md:999 |
| `tcnations:relation_update` | S→C | — | — | 单个关系值增量 | 23-nations.md:998 |
| `tcnations:treaty_sync` | S→C | — | — | 条约状态变化 | 23-nations.md:1001 |
| `tcnations:vote_cast` | C→S | 默认 | — | 无 Commons 时的宣战/媾和投票 | 23-nations.md:1006 |
| `tcnations:war_state_sync` | S→C | — | — | 战争分数、疲劳、围城进度（war_status 控件用，每 100 tick 最多 1 次） | 23-nations.md:1002 |
| `tcorbit:assembly_action` | C→S | 4/s | — | 保存蓝图、吊车装配、装配确认、拆解 | 24-orbit.md:1372 |
| `tcorbit:assembly_state` | S→C | 4/s | — | 各级质量、Δv、TWR、问题列表 | 24-orbit.md:1371 |
| `tcorbit:flight_telemetry` | S→C | 10/s | — | 高度、速度、Δv、对接误差 | 24-orbit.md:1378 |
| `tcorbit:launch_command` | C→S | 2/s | — | 加注、倒计时、scrub、发射、巡航确认 | 24-orbit.md:1375 |
| `tcorbit:life_support_hud` | S→C | 1/s（变化时） | — | 容积/航天器/套装状态 | 24-orbit.md:1377 |
| `tcorbit:mission_plan` | C→S | 2/s | — | 任务草案 | 24-orbit.md:1373 |
| `tcorbit:mission_sync` | S→C | 2/s | — | 任务列表与状态（增量） | 24-orbit.md:1374 |
| `tcorbit:orbit_view` | S→C | 1/s | — | 轨道资产（分页，≤ 256 项） | 24-orbit.md:1376 |
| `tcorbit:rcs_input` | C→S | 20/s | — | RCS 输入 | 24-orbit.md:1379 |
| `tcorbit:sky_snapshot` | S→C | 进入维度时 | — | 地球纹理快照版本号 | 24-orbit.md:1381 |
| `tcorbit:wrap_preload` | S→C | 事件 | — | 环绕边界预加载 | 24-orbit.md:1380 |
<!-- GENERATED:payloads END -->

---

## 7. 事件

### 7.1 登记要求

每个事件在拥有者章节第 12 节的事件表中占一行，列：事件类名、总线（模组/游戏/游戏（客户端））、端、可取消、发布时机、字段、典型订阅者。生成工具从这些表收集事件；引用了却没有任何章节定义的事件名列入 §7.4。

### 7.2 核心托管事件

| 事件 | 包 | 发布者 | 说明 |
| --- | --- | --- | --- |
| `SeasonChangedEvent` | `api.world.event` | tcwildlands；缺失时核心按日历发布 | 03 §12.5 |
| `EmissionRecordedEvent` | `api.world.event` | 排放方（Industry、Networks、Cultivation） | 按地区每 1,000 tick 一批 |
| `PlanetGeneratedEvent` 等世界事件 | `api.world.event` | tcwildlands | 03 §12.5 规则 6 |
| `WorldHistoryReadyEvent` | `api.history` | tcechoes | Nations 用它作为初始文明播种的触发 |
| `SettlementFoundedEvent` / `SettlementTierChangedEvent` | `api` | tcivilization；Settlements 亦发布 | 03 §12.3 |

### 7.3 事件索引

<!-- GENERATED:events BEGIN -->
| 事件 | 拥有者 | 总线 | 端 | 可取消 | 时机 | 典型订阅者 | 位置 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `ChronicleFeatEvent` | 文明核心 | 游戏 | 服务端 | 否 | 功绩达成 | — | 03-core-civilization.md:1311 |
| `CivDayEvent` | 文明核心 | 游戏 | 服务端 | 否 | 主世界跨入新的一日（5.3）；带 elapsedDays | — | 03-core-civilization.md:1309 |
| `CivMemberChangedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 成员加入/离开/角色变化 | — | 03-core-civilization.md:1302 |
| `CivPanelOpenedEvent` | 文明核心 | 游戏（客户端） | 客户端 | 否 | 文明面板打开或切换页签后；只用于 UI | — | 03-core-civilization.md:1319 |
| `CivilizationCreatedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 新文明创建 | — | 03-core-civilization.md:1301 |
| `EmissionRecordedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 排放方调用 EmissionLedger.record（12.5）后，核心按地区每 1,000 tick 合批发布一次。类在核心托管包 api.world.event（12.5）；排放方为 Industry、Networks（车辆）、Cultivation（畜牧），订阅者为 Wildlands（气候积分）、Ecology（毒性）、Settlements（健康） | — | 03-core-civilization.md:1321 |
| `HudDataUpdatedEvent` | 文明核心 | 游戏（客户端） | 客户端 | 否 | 收到并解码 hud_data 后；只用于 UI 刷新 | — | 03-core-civilization.md:1318 |
| `MemoryRecordEvent.Pre` | 文明核心 | 游戏 | 服务端 | Pre 可 | 写入记忆前/后 | — | 03-core-civilization.md:1310 |
| `MemoryRecordedEvent` | 文明核心 | 游戏 | 服务端 | Pre 可 | 写入记忆前/后 | — | 03-core-civilization.md:1310 |
| `PersonAbstractedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 抽象化后 | — | 03-core-civilization.md:1314 |
| `PersonComponentChangedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 组件增删 | — | 03-core-civilization.md:1315 |
| `PersonCreatedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 人物创建/死亡 | — | 03-core-civilization.md:1312 |
| `PersonDiedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 人物创建/死亡 | — | 03-core-civilization.md:1312 |
| `PersonEmbodiedEvent` | 文明核心 | 游戏 | 服务端 | Pre 可 | 实体化前/后 | — | 03-core-civilization.md:1313 |
| `PersonEmbodyEvent.Pre` | 文明核心 | 游戏 | 服务端 | Pre 可 | 实体化前/后 | — | 03-core-civilization.md:1313 |
| `RegionFirstVisitEvent` | 文明核心 | 游戏 | 服务端 | 否 | 文明首次到访地区 | — | 03-core-civilization.md:1306 |
| `RegionLoadPressureEvent` | 文明核心 | 游戏 | 服务端 | 否 | 压力跨越阈值（5.2.4） | — | 03-core-civilization.md:1307 |
| `RegionTierChangedEvent` | 文明核心 | 游戏 | 服务端 | 否 | 地区在 L1（含已加载区块）与 L2（无已加载区块）之间切换；滞回 200 tick，防止玩家在边界来回时抖动（5.8.1）。人物级的 L0 切换用 PersonEmbodiedEvent/PersonAbstractedEvent | — | 03-core-civilization.md:1317 |
| `RegisterCivTabsEvent` | 文明核心 | 模组（客户端） | 客户端 | 否 | 客户端初始化 | — | 03-core-civilization.md:1300 |
| `RegisterDeepLinksEvent` | 文明核心 | 模组（客户端） | 客户端 | 否 | 客户端初始化 | — | 03-core-civilization.md:1300 |
| `RegisterFeatTriggersEvent` | 文明核心 | 模组 | 服务端 | 否 | 公共初始化 | — | 03-core-civilization.md:1299 |
| `RegisterHudWidgetsEvent` | 文明核心 | 模组（客户端） | 客户端 | 否 | 客户端初始化 | — | 03-core-civilization.md:1300 |
| `RegisterMemoryTypesEvent` | 文明核心 | 模组 | 服务端 | 否 | 公共初始化 | — | 03-core-civilization.md:1298 |
| `RegisterPersonComponentsEvent` | 文明核心 | 模组 | 服务端 | 否 | 公共初始化 | — | 03-core-civilization.md:1297 |
| `RegisterPersonSectionsEvent` | 文明核心 | 模组（客户端） | 客户端 | 否 | 客户端初始化 | — | 03-core-civilization.md:1300 |
| `RegisterRegionLayersEvent` | 文明核心 | 模组 | 服务端 | 否 | 公共初始化 | — | 03-core-civilization.md:1296 |
| `RegisterTargetInfoEvent` | 文明核心 | 模组（客户端） | 客户端 | 否 | 客户端初始化 | — | 03-core-civilization.md:1300 |
| `SeasonChangedEvent` | 文明核心（山河 亦发布） | 游戏 | 服务端 | 否 | 仅 Wildlands 缺失时由核心发布 | — | 03-core-civilization.md:1308 |
| `SettlementFoundedEvent` | 文明核心（聚落 亦发布） | 游戏 | 服务端 | 否 | SettlementRegistry.found | — | 03-core-civilization.md:1303 |
| `SettlementRemovedEvent` | 文明核心 | 游戏 | 服务端 | 否 | remove | — | 03-core-civilization.md:1305 |
| `SettlementTierChangedEvent` | 文明核心（聚落 亦发布） | 游戏 | 服务端 | 否 | setTier | — | 03-core-civilization.md:1304 |
| `SimulationStepEvent` | 文明核心 | 游戏 | 服务端 | 否 | L1-L3 结果应用时 | — | 03-core-civilization.md:1316 |
| `TcNotificationReceivedEvent` | 文明核心 | 游戏（客户端） | 客户端 | 是 | 通知进入客户端队列前（31 §7） | — | 03-core-civilization.md:1320 |
| `WorldHistoryReadyEvent` | 文明核心（遗响 亦发布） | 游戏 | 服务端 | 否 | 世界历史生成或从 tc/history.bin 载入完成后、玩家可加入前；Echoes 缺失时不发布。类在核心托管包 api.history（12.5），由 tcechoes 发布；Nations 据此播种幸存政体（23）、Ecology 读取灭绝记录 | — | 03-core-civilization.md:1322 |
| `EraAdvanceEvent.Pre` | 科技核心 | 游戏 | 服务端 | 是 | civ、from、to、declaredBy；Commons 可取消（投票未通过） | — | 04-core-technology.md:916 |
| `EraAdvancedEvent` | 科技核心 | 游戏 | 服务端 | 否 | civ、from、to、tick、durationDays（02 §2.3） | — | 04-core-technology.md:917 |
| `KnowledgeGainedEvent` | 科技核心 | 游戏 | 服务端 | 否 | 每 100 tick 合并发布：civ、Map<tech, delta> | — | 04-core-technology.md:912 |
| `KnowledgeThresholdReachedEvent` | 科技核心 | 游戏 | 服务端 | 否 | civ、tech、insightRp | — | 04-core-technology.md:913 |
| `RegisterKnowledgeTriggersEvent` | 科技核心 | 模组 | 服务端 | 否 | 启动时注册触发器类型 | — | 04-core-technology.md:908 |
| `RegisterMilestoneCheckersEvent` | 科技核心 | 模组 | 服务端 | 否 | 注册里程碑判定器 | — | 04-core-technology.md:910 |
| `RegisterResearchSitesEvent` | 科技核心 | 模组 | 服务端 | 否 | 注册研究地点类型（方块实体类型 → 工厂） | — | 04-core-technology.md:909 |
| `ResearchStartedEvent` | 科技核心 | 游戏 | 服务端 | 否 | civ、tech、site | — | 04-core-technology.md:914 |
| `TechDefinitionsReloadedEvent` | 科技核心 | 游戏 | 服务端 | 否 | 建图完成；含错误汇总 | — | 04-core-technology.md:911 |
| `TechnologyUnlockedEvent` | 科技核心 | 游戏 | 服务端 | 否 | civ、tech、reason（research/grant/command）、tick（02 §2.3） | — | 04-core-technology.md:915 |
| `ApprenticeshipEvent` | 技能 | 游戏* | 服务端 | 否 | — | Settlements（师父 NPC 的对白）、核心记忆 | 05-skills.md:1754 |
| `ClientSkillsUpdatedEvent` | 技能 | 游戏* | 服务端 | 否 | — | 客户端渲染（circuit_view、forage_sense） | 05-skills.md:1756 |
| `GrowthLevelUpEvent` | 技能 | 游戏* | 服务端 | 否 | — | 本模组通知 | 05-skills.md:1751 |
| `ProficiencyGainEvent.Post` | 技能 | 游戏* | 服务端 | 否 | — | Atlas（统计）、核心编年 | 05-skills.md:1749 |
| `ProficiencyGainEvent.Pre` | 技能 | 游戏* | 服务端 | 是 | — | 服务器整合包脚本、Beyond | 05-skills.md:1748 |
| `ReflectionTokenEvent` | 技能 | 游戏* | 服务端 | 否 | — | 本模组通知 | 05-skills.md:1755 |
| `SkillLevelUpEvent` | 技能 | 游戏* | 服务端 | 否 | — | 本模组通知；Settlements（居民对玩家称呼） | 05-skills.md:1750 |
| `TalentEffectsChangedEvent` | 技能 | 游戏* | 服务端 | 否 | — | 自行缓存乘数的模组（Networks crewFactor、Atlas 误差） | 05-skills.md:1753 |
| `TalentLearnedEvent` | 技能 | 游戏* | 服务端 | 否 | — | 本模组；消费 op 的主题模组 | 05-skills.md:1752 |
| `TalentRespecEvent` | 技能 | 游戏* | 服务端 | 否 | — | 本模组；消费 op 的主题模组 | 05-skills.md:1752 |
| `ClimateShiftedEvent` | 山河 | 游戏* | 服务端 | 否 | 全球 ΔT 每变化 0.25 °C（5.17 第 4 条） | — | 10-wildlands.md:2622 |
| `DepositDiscoveredEvent` | 山河 | 游戏* | 服务端 | 否 | 文明首次发现矿床 | — | 10-wildlands.md:2620 |
| `DisasterEndedEvent` | 山河 | 游戏* | 服务端 | 否 | 灾害结束；含抽象影响统计 | — | 10-wildlands.md:2618 |
| `DisasterStartedEvent` | 山河 | 游戏* | 服务端 | 否 | 灾害开始 | — | 10-wildlands.md:2617 |
| `DisasterWarnedEvent` | 山河 | 游戏* | 服务端 | 否 | 灾害进入预警期（前兆） | — | 10-wildlands.md:2616 |
| `HazardDamageEvent` | 山河 | 游戏* | 服务端 | 是 | 灾害即将破坏某个方块或伤害实体前（Commons 抗震加固、Settlements 防灾建筑用于修正） | — | 10-wildlands.md:2624 |
| `PlanetGeneratedEvent` | 山河 | 游戏* | 服务端 | 否 | 预计算完成或从磁盘载入后、玩家可加入前 | — | 10-wildlands.md:2613 |
| `ReservoirDepletedEvent` | 山河 | 游戏* | 服务端 | 否 | 抽取越过阈值 | — | 10-wildlands.md:2621 |
| `RiverFloodStageChangedEvent` | 山河 | 游戏* | 服务端 | 否 | 河段水位等级变化 | — | 10-wildlands.md:2619 |
| `RockIdentifiedEvent` | 山河 | 游戏* | 服务端 | 否 | 敲击或观察识别岩石 | — | 10-wildlands.md:2623 |
| `WeatherChangedEvent` | 山河 | 游戏* | 服务端 | 否 | 天气格更新（200 tick）且类别变化时 | — | 10-wildlands.md:2615 |
| `CodexLevelChangedEvent` | 生息 | 游戏 | 服务端 | 否 | 认知升级 | Technology（知识值）、Discovery、Skills | 11-ecology.md:1069 |
| `MigrationEvent` | 生息 | 游戏 | 服务端 | 否 | 迁徙开始/经过要冲 | Atlas、Frontiers | 11-ecology.md:1065 |
| `PopulationStatusChangedEvent` | 生息 | 游戏 | 服务端 | 否 | 状态等级变化 | Exchange（价格）、Settlements（猎人产量）、Discovery | 11-ecology.md:1061 |
| `SpeciesExtinctEvent` | 生息 | 游戏 | 服务端 | 否 | 全球灭绝 | Echoes（传说）、核心编年史、Nations | 11-ecology.md:1063 |
| `SpeciesExtirpatedEvent` | 生息 | 游戏 | 服务端 | 否 | 局部灭绝 | Echoes、Commons、Atlas | 11-ecology.md:1062 |
| `SpeciesRestoredEvent` | 生息 | 游戏 | 服务端 | 否 | 全球灭绝物种被复原 | 核心编年史（典礼通知） | 11-ecology.md:1064 |
| `SuccessionStageChangedEvent` | 生息 | 游戏 | 服务端 | 否 | 区块阶段变化（只对 L0 区块发布，批量） | Wildlands、Atlas | 11-ecology.md:1068 |
| `WildAncestorPennedEvent` | 生息 | 游戏 | 服务端 | 否 | 驯化入口满足条件（5.15） | Cultivation | 11-ecology.md:1067 |
| `WildlifeEmbodyEvent` | 生息 | 游戏 | 服务端 | 是 | 见正文 | — | 11-ecology.md:1071 |
| `WildlifeThreatEvent` | 生息 | 游戏 | 服务端 | 否 | 大型捕食者进入聚落 48 格内或袭击家畜 | Settlements、Cultivation | 11-ecology.md:1066 |
| `CropHarvestedEvent` | 田园 | 游戏 | 服务端 | 否 | level、pos、crop、seedLot、drops（可修改）、harvester（可空：NPC/机械） | — | 12-cultivation.md:1402 |
| `FamineEvent` | 田园 | 游戏 | 服务端 | 否 | civ、region、severity（SHORTAGE/FAMINE/GREAT_FAMINE）、deficitFU | — | 12-cultivation.md:1406 |
| `LandReclaimedEvent` | 田园 | 游戏 | 服务端 | 否 | civ、area、kind（DRAIN/TERRACE/DESALINATE/POLDER） | — | 12-cultivation.md:1408 |
| `LivestockBornEvent` | 田园 | 游戏 | 服务端 | 否 | mother、child、data | — | 12-cultivation.md:1409 |
| `PastureDegradedEvent` | 田园 | 游戏 | 服务端 | 否 | region、paddock、level | — | 12-cultivation.md:1407 |
| `SeedLotCreatedEvent` | 田园 | 游戏 | 服务端 | 否 | lot、来源（野生/选种/杂交/编辑）、player | — | 12-cultivation.md:1403 |
| `SpecialtyRecognizedEvent` | 田园 | 游戏 | 服务端 | 否 | region、product、Q | — | 12-cultivation.md:1405 |
| `VarietyNamedEvent` | 田园 | 游戏 | 服务端 | 是 | civ、lot、name、player | — | 12-cultivation.md:1404 |
| `IndustryAccidentEvent` | 工坊 | 游戏 | 服务端 | 否 | 事故发生（§5.13.4） | Echoes、Commons、Settlements、Exchange | 13-industry.md:2564 |
| `IndustryGridBlackoutEvent` | 工坊 | 游戏 | 服务端 | 否 | 整网失电/恢复 | Settlements、Networks | 13-industry.md:2566 |
| `IndustryGridOverloadEvent` | 工坊 | 游戏 | 服务端 | 否 | 切负荷或保护跳闸 | Settlements（停电）、Networks（电力机车） | 13-industry.md:2565 |
| `IndustryMachineFailureEvent` | 工坊 | 游戏 | 服务端 | 否 | 机器进入 FAULT | Settlements（维修岗位）、通知 | 13-industry.md:2563 |
| `IndustryMachineFormedEvent` | 工坊 | 游戏 | 服务端 | 否 | 多方块成形/解体 | 核心编年史（首座高炉等里程碑）、Settlements（新增岗位） | 13-industry.md:2560 |
| `IndustryProcessCompletedEvent` | 工坊 | 游戏 | 服务端 | 否 | 每批次完成（L0 逐批；L1/L2 合并为一次，带 batches） | Technology 实践知识（§6.12）、Skills（§15）、Exchange 产量 | 13-industry.md:2561 |
| `IndustryProductionDemandEvent` | 工坊 | 游戏 | 服务端 | 否 | 每 L2 日结，工业区报告缺口物料 | Exchange（订单）、Networks（货运） | 13-industry.md:2568 |
| `IndustryQualityRolledEvent` | 工坊 | 游戏 | 服务端 | 否（可修改质量 ±1，Skills 天赋） | 产出带质量的物品时 | Skills、Echoes | 13-industry.md:2562 |
| `IndustryReactorStateChangedEvent` | 工坊 | 游戏 | 服务端 | 否 | 反应堆状态迁移 | Commons（核安全法）、Echoes | 13-industry.md:2567 |
| `CargoInspectEvent` | 通途 | 游戏 | 服务端 | 是 | 跨境或进港时检查货物 | Nations（禁运）、Commons（违禁品） | 14-networks.md:1222 |
| `CrewAssignEvent` | 通途 | 游戏 | 服务端 | 是 | 乘员分配到载具 | Settlements（岗位校验） | 14-networks.md:1223 |
| `InfrastructureDamagedEvent` | 通途 | 游戏 | 服务端 | 否 | 路段/桥/轨道/线路损毁 | Settlements（维修任务）、Commons | 14-networks.md:1219 |
| `RouteSegmentBuiltEvent` | 通途 | 游戏 | 服务端 | 否 | 一段道路/轨道完工 | Ecology（栖息地切割）、Technology（实践） | 14-networks.md:1220 |
| `ShipmentDeliveredEvent` | 通途 | 游戏 | 服务端 | 否 | 批次交付（含 L2 对账后） | Exchange（合同交付）、Technology（实践）、Skills | 14-networks.md:1215 |
| `ShipmentDispatchedEvent` | 通途 | 游戏 | 服务端 | 否 | 批次离开起点 | Frontiers（袭击概率计算） | 14-networks.md:1216 |
| `TollPointEvent` | 通途 | 游戏 | 服务端 | 是 | 载具通过收费点 | Commons、Nations（边境） | 14-networks.md:1221 |
| `VehicleAccidentEvent` | 通途 | 游戏 | 服务端 | 否 | 脱轨、碰撞、搁浅、沉没、坠毁 | Ecology、Commons（事故调查）、Echoes（沉船遗迹） | 14-networks.md:1218 |
| `VehicleTierChangeEvent` | 通途 | 游戏 | 服务端 | 否 | L0/L1/L2/L3 转换 | Atlas（标记刷新） | 14-networks.md:1217 |
| `SettlementBuildingRecognizedEvent` | 聚落 | 游戏 | 服务端 | 否 | 新房间/建筑首次识别或类型变化 | Skills（construction）、Echoes | 15-settlements.md:1209 |
| `SettlementDirectiveToggledEvent` | 聚落 | 游戏 | 服务端 | 否 | 指令切换 | Commons、Echoes | 15-settlements.md:1211 |
| `SettlementEpidemicEndedEvent` | 聚落 | 游戏 | 服务端 | 否 | 疫情宣告/结束 | Commons（隔离法）、Exchange（贸易减少）、Echoes | 15-settlements.md:1210 |
| `SettlementEpidemicStartedEvent` | 聚落 | 游戏 | 服务端 | 否 | 疫情宣告/结束 | Commons（隔离法）、Exchange（贸易减少）、Echoes | 15-settlements.md:1210 |
| `SettlementImmigrantArrivedEvent` | 聚落 | 游戏 | 服务端 | 是 | 移民户到达前 | Commons（移民法）、Nations（AI 文明人口来源） | 15-settlements.md:1206 |
| `SettlementJobAssignedEvent` | 聚落 | 游戏 | 服务端 | 否 | 分配或固定岗位 | Skills（玩家分配挂钩）、Industry | 15-settlements.md:1207 |
| `SettlementNeedsUpdatedEvent` | 聚落 | 游戏 | 服务端 | 否 | 每 MC 日结算后，携带需求快照 | Commons（稳定度）、Exchange（需求） | 15-settlements.md:1208 |
| `SettlementResidentBornEvent` | 聚落 | 游戏 | 服务端 | 否 | 出生（含 L2 累加生成） | Echoes | 15-settlements.md:1204 |
| `SettlementResidentDiedEvent` | 聚落 | 游戏 | 服务端 | 否 | 死亡，含 cause | Echoes、Commons（稳定度） | 15-settlements.md:1205 |
| `CaravanArrivedEvent` | 市集 | 游戏 | 服务端 | 否 | 商队发车/抵达经停点 | Frontiers（目标池）、Atlas（商路要素） | 16-exchange.md:1485 |
| `CaravanDepartedEvent` | 市集 | 游戏 | 服务端 | 否 | 商队发车/抵达经停点 | Frontiers（目标池）、Atlas（商路要素） | 16-exchange.md:1485 |
| `CaravanRegionEnteredEvent` | 市集 | 游戏 | 服务端 | 否 | 商队（实体或抽象）进入新的 64×64 区域格 | Frontiers（伏击判定） | 16-exchange.md:1486 |
| `ContractDefaultedEvent` | 市集 | 游戏 | 服务端 | 否 | 合同违约 | Nations（关系）、Commons（司法） | 16-exchange.md:1481 |
| `ContractFulfilledEvent` | 市集 | 游戏 | 服务端 | 否 | 合同验收通过 | Skills、Technology（实践触发器）、Nations（信任） | 16-exchange.md:1480 |
| `CurrencyIssuedEvent` | 市集 | 游戏 | 服务端 | 否 | 首次铸币、纸币首发、成色贬损 | Echoes（编年史）、Commons | 16-exchange.md:1483 |
| `ExchangeMarketSettledEvent` | 市集 | 游戏 | 服务端 | 否 | 市场日结算后，携带价格指数与短缺清单 | Settlements（需求满足）、Atlas、Echoes | 16-exchange.md:1479 |
| `ExchangeReputationChangedEvent` | 市集 | 游戏 | 服务端 | 否 | 信誉跨档位 | Nations、Settlements（商人迁入意愿） | 16-exchange.md:1482 |
| `ExchangeSovereignDefaultEvent` | 市集 | 游戏 | 服务端 | 否 | 文明主权违约 | Nations（关系与议程）、Echoes | 16-exchange.md:1484 |
| `ExchangeTradeEvent` | 市集 | 游戏 | 服务端 | 是 | 每笔成交前（含 NPC 与订单撮合） | Commons（市场规则、税）、Skills | 16-exchange.md:1478 |
| `ArtefactExcavatedEvent` | 求知 | 游戏 | 服务端 | 否 | 发掘出土一件物品 | — | 17-discovery.md:1052 |
| `DiscoveryMadeEvent` | 求知 | 游戏 | 服务端 | 否 | 文明首次观察某目标 / 首次确认某定律 | — | 17-discovery.md:1047 |
| `ExperimentConcludedEvent` | 求知 | 游戏 | 服务端 | 否 | 实验确认或更新准确度 | — | 17-discovery.md:1048 |
| `FacilityTierChangedEvent` | 求知 | 游戏 | 服务端 | 否 | 设施级别变化 | — | 17-discovery.md:1053 |
| `HypothesisConfirmedEvent` | 求知 | 游戏 | 服务端 | 否 | 假说证实或证伪 | — | 17-discovery.md:1049 |
| `KnowledgeCarrierCreatedEvent` | 求知 | 游戏 | 服务端 | 是 | 写作、抄写、印刷 | — | 17-discovery.md:1050 |
| `KnowledgeDiffusedEvent` | 求知 | 游戏 | 服务端 | 否 | 任一跨文明传播 | — | 17-discovery.md:1051 |
| `ObservationCompletedEvent` | 求知 | 游戏 | 服务端 | 否（可修改 value） | 一次观察完成 | — | 17-discovery.md:1046 |
| `ArtefactAppraisedEvent` | 遗响 | 游戏 | 服务端 | 否 | 鉴定级别提升 | — | 18-echoes.md:1328 |
| `HeritageDesignatedEvent` | 遗响 | 游戏 | 服务端 | 是 | 遗产登记或等级变化 | — | 18-echoes.md:1331 |
| `LegendStateChangedEvent` | 遗响 | 游戏 | 服务端 | 否 | 传说状态推进（§5.6.1） | — | 18-echoes.md:1326 |
| `LegendToldEvent` | 遗响 | 游戏 | 服务端 | 否 | 任一来源讲出传说 | — | 18-echoes.md:1325 |
| `RuinDiscoveredEvent` | 遗响 | 游戏 | 服务端 | 否 | 文明首次进入遗址范围 | — | 18-echoes.md:1327 |
| `ScriptMasteryChangedEvent` | 遗响 | 游戏 | 服务端 | 否 | 文字掌握度跨 30/60/90 | — | 18-echoes.md:1329 |
| `SiteRuinifiedEvent` | 遗响 | 游戏 | 服务端 | 是 | 聚落或残骸转为遗址 | — | 18-echoes.md:1330 |
| `CovenantBreachedEvent` | 契约 | 游戏 | 服务端 | 否 | covenant、party、term | — | 19-commons.md:1256 |
| `GovernanceReformedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、from、to | — | 19-commons.md:1253 |
| `LawSteleDedicatedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、pos、milestoneWritten | — | 19-commons.md:1258 |
| `ParcelClaimedEvent` | 契约 | 游戏 | 服务端 | 是 | civ、parcel、chunks、claimer | — | 19-commons.md:1249 |
| `ParcelOwnershipChangedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、parcel、旧/新所有者、原因 | — | 19-commons.md:1250 |
| `ParcelReleasedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、parcel、旧/新所有者、原因 | — | 19-commons.md:1250 |
| `PublicWorkCompletedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、work、template、contributors | — | 19-commons.md:1255 |
| `PublicWorkStateChangedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、work、template、contributors | — | 19-commons.md:1255 |
| `StabilityBandChangedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、from、to | — | 19-commons.md:1257 |
| `StatuteEnactedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、statute、option、settlement（可空） | — | 19-commons.md:1251 |
| `StatuteRepealedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、statute、option、settlement（可空） | — | 19-commons.md:1251 |
| `ViolationRecordedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、record | — | 19-commons.md:1254 |
| `VoteClosedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、proposal、结果 | — | 19-commons.md:1252 |
| `VoteOpenedEvent` | 契约 | 游戏 | 服务端 | 否 | civ、proposal、结果 | — | 19-commons.md:1252 |
| `FrontiersBanditBandFormedEvent` | 远征 | 游戏 | 服务端 | 是 | band, region, cause | Echoes（写历史）、Commons | 20-frontiers.md:1071 |
| `FrontiersBountyClaimedEvent` | 远征 | 游戏 | 服务端 | 否 | bounty, claimer, rewardVu | Exchange | 20-frontiers.md:1078 |
| `FrontiersDangerChangedEvent` | 远征 | 游戏 | 服务端 | 否 | region, oldTier, newTier | Settlements（选址）、Atlas（图层刷新） | 20-frontiers.md:1070 |
| `FrontiersExpeditionCompletedEvent` | 远征 | 游戏 | 服务端 | 否 | expedition, outcome, objectivesMet | Atlas（测绘等级）、Discovery（知识）、Nations（接触） | 20-frontiers.md:1075 |
| `FrontiersExpeditionStartedEvent` | 远征 | 游戏 | 服务端 | 是 | expedition | Atlas、Discovery | 20-frontiers.md:1074 |
| `FrontiersOutpostEstablishedEvent` | 远征 | 游戏 | 服务端 | 是 | outpost, tier | Settlements、Commons（领地） | 20-frontiers.md:1076 |
| `FrontiersRaidResolvedEvent` | 远征 | 游戏 | 服务端 | 否 | band, target, result | Exchange（损失记账）、Echoes | 20-frontiers.md:1073 |
| `FrontiersRaidStartedEvent` | 远征 | 游戏 | 服务端 | 是 | band, target, simTier | Settlements（避难）、Nations | 20-frontiers.md:1072 |
| `FrontiersSiegeBlockBreachedEvent` | 远征 | 游戏 | 服务端 | 是 | pos, blockState, source | Commons（领地标志二次校验） | 20-frontiers.md:1077 |
| `FrontiersSpecialEnemySpawnedEvent` | 远征 | 游戏 | 服务端 | 是 | template, entity, region | Echoes、Ecology（凶兽） | 20-frontiers.md:1079 |
| `AnomalousDisturbanceEvent` | 异界 | 游戏 | 服务端 | 否（可修改 amount） | 扰动记入账本前 | — | 21-beyond.md:1263 |
| `AnomalyDayEvent` | 异界 | 游戏 | 服务端 | 否 | AnomalyDayTicker 每日结算后 | — | 21-beyond.md:1255 |
| `BeyondModeChangedEvent` | 异界 | 游戏 | 服务端 | 否 | /tcbeyond mode | — | 21-beyond.md:1262 |
| `FocusChangedEvent` | 异界 | 游戏 | 服务端 | 否 | 强度跨分档、上限变化、改名 | — | 21-beyond.md:1256 |
| `FocusSpreadEvent` | 异界 | 游戏 | 服务端 | 是 | 生成子焦点前 | — | 21-beyond.md:1257 |
| `RiteCompletedEvent` | 异界 | 游戏 | 服务端 | 否 | 小封印、大封印、契约、调谐 | — | 21-beyond.md:1259 |
| `SpireAttunedEvent` | 异界 | 游戏 | 服务端 | 否 | 门塔调谐 | — | 21-beyond.md:1261 |
| `SurgeEvent.Pre` | 异界 | 游戏 | 服务端 | Pre 可取消 | 涌潮开始前 / 结束后 | — | 21-beyond.md:1258 |
| `ThresholdResolvedEvent` | 异界 | 游戏 | 服务端 | 否 | 契约成立或末影龙被击败 | — | 21-beyond.md:1260 |
| `KnowledgeChangedEvent` | 舆图 | 游戏 | 服务端 | 否 | 合并后批量触发（每 tick 每持有者最多 1 次，含区块数与最高 SL） | — | 22-atlas.md:1451 |
| `MapScreenEvent.Open` | 舆图 | 客户端 | 客户端 | Open 可取消 | 世界地图开关 | — | 22-atlas.md:1455 |
| `PositionFixEvent` | 舆图 | 游戏 | 服务端 | 否 | 玩家获得定位（来源、ε） | — | 22-atlas.md:1454 |
| `RegisterAncientMapProvidersEvent` | 舆图 | 模组 | 服务端 | 否 | 公共初始化（注册 AncientMapContentProvider） | — | 22-atlas.md:1450 |
| `RegisterMapFeaturesEvent` | 舆图 | 模组 | 服务端 | 否 | 公共初始化 | — | 22-atlas.md:1449 |
| `RegisterMapLayersEvent` | 舆图 | 模组 | 服务端 | 否 | 公共初始化 | — | 22-atlas.md:1448 |
| `SurveyCompletedEvent` | 舆图 | 游戏 | 服务端 | 否 | 导线闭合、三角网平差、水准线、航拍带、天体定位 | — | 22-atlas.md:1453 |
| `WaypointEvent.Added` | 舆图 | 游戏 | 服务端 | Added 可取消 | 路点增删、导航到达 | — | 22-atlas.md:1452 |
| `NationsBorderChangedEvent` | 列国 | 游戏 | 服务端 | 否 | dim, regionKey, oldOwner, newOwner | Atlas、Commons | 23-nations.md:987 |
| `NationsCivCollapsedEvent` | 列国 | 游戏 | 服务端 | 否 | civId, conqueror?, settlements | Echoes（遗迹）、Atlas | 23-nations.md:981 |
| `NationsCivFoundedEvent` | 列国 | 游戏 | 服务端 | 否 | civId, capital, cause（SEED/SPLIT/COLONY/REBIRTH） | Echoes、Atlas | 23-nations.md:980 |
| `NationsDecisionEvent` | 列国 | 游戏 | 服务端 | 否（只读，调试与 UI） | civId, action, topFactors | — | 23-nations.md:989 |
| `NationsFirstContactEvent` | 列国 | 游戏 | 服务端 | 否 | civA, civB, pos | Technology（扩散）、Discovery、Skills | 23-nations.md:982 |
| `NationsPeaceSignedEvent` | 列国 | 游戏 | 服务端 | 否 | war, terms | Settlements（解除占领）、Echoes | 23-nations.md:986 |
| `NationsSiegeStageEvent` | 列国 | 游戏 | 服务端 | 否 | siege, phase | Frontiers、Settlements | 23-nations.md:988 |
| `NationsTreatyBrokenEvent` | 列国 | 游戏 | 服务端 | 否 | treaty, breaker | 核心文明记忆 | 23-nations.md:984 |
| `NationsTreatySignedEvent` | 列国 | 游戏 | 服务端 | 否 | treaty | Commons（边界）、Exchange（关税） | 23-nations.md:983 |
| `NationsWarDeclaredEvent` | 列国 | 游戏 | 服务端 | 是 | war | Frontiers（危险度）、Exchange（禁运） | 23-nations.md:985 |
| `ColonyPsiEvent` | 寰宇 | 游戏* | 服务端 | 否 | 每日 ψ 结算 | Settlements、Echoes | 24-orbit.md:1365 |
| `DockingEvent` | 寰宇 | 游戏* | 服务端 | 否 | 对接锁定 | 技能 | 24-orbit.md:1361 |
| `LandingEvent` | 寰宇 | 游戏* | 服务端 | 否 | 着陆（含助推器回收） | 技能、编年史 | 24-orbit.md:1362 |
| `LaunchEvent.Pre` | 寰宇 | 游戏* | 服务端 | Pre 是 | T−0 / 点火后 | Commons 权限、Nations、Echoes | 24-orbit.md:1357 |
| `LifeSupportStateEvent` | 寰宇 | 游戏* | 服务端 | 否 | 容积状态变化 | Settlements、HUD、通知 | 24-orbit.md:1363 |
| `OrbitBodyReachedEvent` | 寰宇 | 游戏* | 服务端 | 否 | 首个探测器/轨道器抵达月球、火星 | Atlas（生成地图库）、Frontiers | 24-orbit.md:1360 |
| `OrbitHazardEvent` | 寰宇 | 游戏* | 服务端 | 否 | SPE 预警/开始、尘暴、碰撞 | Frontiers、通知 | 24-orbit.md:1364 |
| `OrbitInsertionEvent` | 寰宇 | 游戏* | 服务端 | 否 | 入轨 | Atlas、技能、里程碑 | 24-orbit.md:1358 |
| `PayloadDeployedEvent` | 寰宇 | 游戏* | 服务端 | 否 | 载荷激活 | Atlas、Discovery | 24-orbit.md:1359 |
| `RocketAssembledEvent` | 寰宇 | 游戏* | 服务端 | 是 | 装配确认 | 技能、Commons | 24-orbit.md:1356 |
<!-- GENERATED:events END -->

### 7.4 未解析的事件引用

章节正文或订阅表中出现、但没有任何章节第 12 节定义、也不是 NeoForge/原版事件的名字。每一项都需要处理：在拥有者章节补上定义、改用已有事件，或从正文删除。

<!-- GENERATED:events-unresolved BEGIN -->
| 事件 | 引用章节 |
| --- | --- |
| — | 无 |
<!-- GENERATED:events-unresolved END -->

---

## 8. 存档

### 8.1 全局状态：`SavedData`

1. 每个模组至多一个全局 `SavedData`，文件为 `data/<modid>.dat`（02 §7）。确实需要拆分时用 `data/<modid>_<part>.dat`，并在章节中登记。
2. 用 `Codec` 定义；顶层必须有 `int formatVersion`。读取时 `formatVersion` 缺失视为 0（v0.1 存档）。
3. **迁移**：每个文件注册迁移器链 `TcMigrators.register(fileId, fromVersion, UnaryOperator<Dynamic<?>>)`，逐级升到当前版本。遇到**高于**当前版本的文件时拒绝加载世界并提示“存档来自更新的版本”，不尝试降级。
4. 玩家数据：需要在玩家离线时被别人读取的（文明成员、技能档案用于排行或继承）放在 `SavedData` 中按 UUID 索引；只与玩家本人在线状态相关的放玩家附件（§9）。

### 8.2 `tc/` 目录下的二进制文件

| 文件 | 拥有者 | 访问 | 结构要点 |
| --- | --- | --- | --- |
| `tc/planet.bin` | tcwildlands（核心托管路径） | 世界创建时写一次，之后只读 | 星球粗模型；已有存档永不重算（40 §2.2） |
| `tc/history.bin` | tcechoes（核心托管路径） | 世界创建时写入，运行期只追加 | 世界历史；就绪后发布 `WorldHistoryReadyEvent` |
| `tc/regions/r.<rx>.<rz>.tcr` | 核心（共享容器） | 按需读写 | 每文件 8×8 个地区；各模组的地区层作为分段存放 |
| `tc/regions/<namespace>/<path>/r.<rx>.<rz>.tcr` | 核心 | 同上 | 非主世界维度（03 §5.2.2） |
| `tc/people/p.<bucket>.tcp` | 核心 | 按需读写 | 抽象人员记录；`bucket` 为 UUID 哈希低 8 位的两位十六进制（256 桶） |
| `tc/atlas/` | tcatlas（核心托管路径） | 按需读写 | 服务器端测绘数据 |

通用文件头（所有 `tc/` 二进制文件）：

```text
magic        4 B   "TCB1"
formatVersion u16
producer     短字符串（写入者模组 ID，例如 "tcwildlands"）
flags        u16   bit0 = DEFLATE 压缩
sectionCount varint
sections     { id 短字符串, length varint, bytes }*
```

地区文件 `.tcr` 的结构：

```text
文件头（同上，producer = "tcivilization"）
index        64 × { offset u32, length u32 }      // 8×8 个地区，未写入为 0
region blob  layerCount varint
             { layerId 字符串, layerFormat u16, length varint, DEFLATE(bytes) }*
```

规则：

1. **未知层原样保留。** 读取到未注册的 `layerId`（对应模组已被移除）时，作为不透明字节保存在内存中并原样写回；模组重新安装后数据仍在。`/tcdebug regions prune <layerId>` 可手动清除（权限 3）。
2. 每层每地区序列化后（压缩前）≤ 16 KiB；超出时写入失败并告警（03 §5.2.3）。
3. 地区文件按需加载，LRU 缓存 256 个地区（02 §7）。只有脏层写回。
4. **原子写入**：先写 `<文件>.tmp`，刷盘后重命名覆盖；`tc/` 下的文件保留上一代 `<文件>.bak`。
5. 写入在存档 I/O 线程进行，内容是主线程在存档时拷贝的不可变快照（§4.1）。

### 8.3 客户端缓存

| 路径 | 拥有者 | 内容 |
| --- | --- | --- |
| `.minecraft/tcatlas/<server-or-world-id>/` | tcatlas | 地图瓦片与本地标记（22 章） |
| `.minecraft/config/tcivilization-client.toml` 等 | 各模组 | 客户端配置（31 §13） |

客户端缓存可以随时删除，删除后从服务器重新同步；缓存中不得保存服务端权威数据的唯一副本。

### 8.4 存档路径索引

由全文中出现的 `data/…`、`tc/…`、`<world>/…` 路径生成。“核心托管路径”表示路径属于核心目录约定，但文件由所列模组写入。

<!-- GENERATED:saves BEGIN -->
| 路径（相对存档根） | 拥有者 | 提及章节 |
| --- | --- | --- |
| `banks.dat` | 市集 | 16 |
| `caravans.dat` | 市集 | 16 |
| `codex/<civ_uuid>.dat` | 生息 | 11 |
| `contracts.dat` | 市集 | 16 |
| `currencies.dat` | 市集 | 16 |
| `data/<modid>.dat` | — | 02 |
| `data/tcbeyond/state.dat` | 异界 | 21 |
| `data/tccommons.dat` | 契约 | 19 |
| `data/tcdiscovery.dat` | 求知 | 17 |
| `data/tcfrontiers.dat` | 远征 | 20 |
| `data/tcindustry_accidents.dat` | 工坊 | 13 |
| `data/tcindustry_networks.dat` | 工坊 | 13 |
| `data/tcindustry_reservoirs.dat` | 工坊 | 13 |
| `data/tcindustry_zones.dat` | 工坊 | 13 |
| `data/tcivilization.dat` | 文明核心 | 02、03 |
| `data/tcnetworks.dat` | 通途 | 14 |
| `data/tcskills.dat` | 技能 | 02、05 |
| `data/tctechnology.dat` | 科技核心 | 02、04 |
| `data/tcwildlands.dat` | 山河 | 10 |
| `gene_bank.dat` | 生息 | 11 |
| `markets/<marketId>.dat` | 市集 | 16 |
| `orders.dat` | 市集 | 16 |
| `planet.bin` | 山河 | 10 |
| `playerdata/<uuid>.dat` | 技能 | 05 |
| `region/<dim>/tcfrontiers_danger.bin` | 远征 | 20 |
| `reputation.dat` | 市集 | 16 |
| `reserves.dat` | 生息 | 11 |
| `routes.dat` | 生息 | 11 |
| `securities.dat` | 市集 | 16 |
| `species_status.dat` | 生息 | 11 |
| `tc/` | 核心（共享） | 02、10 |
| `tc/atlas/` | 舆图（核心托管路径） | 02、22 |
| `tc/history.bin` | 遗响（核心托管路径） | 02 |
| `tc/nations/civs.dat` | 列国 | 23 |
| `tc/nations/relations.dat` | 列国 | 23 |
| `tc/nations/treaties.dat` | 列国 | 23 |
| `tc/nations/wars.dat` | 列国 | 23 |
| `tc/people/p.<bucket>.tcp` | 核心（共享） | 02 |
| `tc/planet.bin` | 山河（核心托管路径） | 02、10 |
| `tc/regions/r.<rx>.<rz>.tcr` | 核心（共享） | 02、10、11、14 |
| `tc/wildlands/` | 山河 | 10 |
| `tc/wildlands/export/` | 山河 | 10 |
| `tc/wildlands/start_site.json` | 山河 | 10 |
| `tc/wildlands/tiles/t.<rx>.<rz>.bin` | 山河 | 10 |
| `tcindustry_networks.dat` | 工坊 | 13 |
| `tcwildlands.dat` | 山河 | 10 |
| `tiles/*.bin` | 山河 | 10 |
<!-- GENERATED:saves END -->

---

## 9. 附件与数据组件

### 9.1 选择

| 状态挂在 | 机制 | 注册 | 例子 |
| --- | --- | --- | --- |
| 实体（含玩家、`PersonEntity`） | NeoForge 数据附件 `AttachmentType` | `NeoForgeRegistries.ATTACHMENT_TYPES` | 居民需求、动物个体基因 |
| 方块实体 | 方块实体自身字段；跨模组附加数据用附件 | 同上 | 机器磨损由 Industry 自己存；Commons 给任意容器加“公有”标记用附件 |
| 区块 / 维度 | 附件 | 同上 | 区块土壤肥力缓存 |
| 物品堆叠 | 原版数据组件 `DataComponentType` | `Registries.DATA_COMPONENT_TYPE` | 食物新鲜度、种子品系、文物鉴定状态 |
| 人物抽象记录 | 人物组件（03，`RegisterPersonComponentsEvent`） | 核心 | `ResidentComponent`、`MerchantComponent` |
| 地区 | 地区层（§12） | `RegisterRegionLayersEvent` | 生态健康、价格表 |

### 9.2 附件规则

1. 需要持久化的附件声明序列化 `Codec`；玩家附件按需声明 `copyOnDeath`（技能、发现记录跟随玩家；临时状态不跟随）。
2. 附件只存本模组的数据；读取其他模组的附件同样只能在桥接类中进行。
3. 需要同步到客户端的附件，若当前 NeoForge 版本的附件同步机制满足需要就用它，否则用本模组载荷，且只同步给能看见该对象的玩家。
4. 单个附件序列化后 ≤ 4 KiB；更大的数据放 `SavedData` 或地区层，附件中只存 ID。

### 9.3 数据组件规则

1. 组件值必须是**不可变**记录，实现 `equals`/`hashCode`（堆叠合并依赖它）。
2. 同时声明持久化 `Codec` 与网络 `StreamCodec`；纯服务端数据不放在组件里（组件会同步给客户端）。
3. 单个组件序列化后 ≤ 1 KiB；不在组件里存列表超过 16 项的数据。
4. 会让两个本应可堆叠的物品无法合并的组件（例如逐 tick 递减的新鲜度），改为存“创建时间”并在读取时计算当前值。
5. 禁止使用旧的物品 NBT 写法；跨模组读取组件只用拥有者 `api` 包中的组件键。

### 9.4 附件与组件索引

由各章附录 A 中类型为 `attachment`、`data_component`、`person_component` 的行生成。

<!-- GENERATED:attachments BEGIN -->
| ID | 种类 | 模组 | 宿主 / 挂载 | 位置 |
| --- | --- | --- | --- | --- |
| `tcskills:placed` | 附件 | 技能 | — | 05-skills.md:1461 |
| `tcskills:profile` | 附件 | 技能 | — | 05-skills.md:1459 |
| `tcecology:collar` | 组件 | 生息 | 戴项圈个体 | 11-ecology.md:1008 |
| `tcecology:player_placed` | 附件 | 生息 | 4 位分辨率的“玩家放置方块”位图（4×4×4 格为 1 位） | 11-ecology.md:991 |
| `tcecology:sample_data` | 组件 | 生息 | tissue_sample | 11-ecology.md:1010 |
| `tcecology:species` | 组件 | 生息 | fur_pelt、tissue_sample、网中动物物品 | 11-ecology.md:1009 |
| `tcecology:stand` | 附件 | 生息 | 原木计数缓存、上次增量日 | 11-ecology.md:990 |
| `tcecology:succession` | 附件 | 生息 | ChunkSuccession（5.8） | 11-ecology.md:988 |
| `tcecology:tracks` | 附件 | 生息 | TrackRing：16 × 14 B | 11-ecology.md:989 |
| `tcecology:wildlife` | 组件 | 生息 | 野生动物实体（含复用的原版实体） | 11-ecology.md:1007 |
| `tccultivation:livestock` | 附件 | 田园 | — | 12-cultivation.md:1335 |
| `tccultivation:soil` | 附件 | 田园 | — | 12-cultivation.md:1333 |
| `tccultivation:vessel` | 附件 | 田园 | — | 12-cultivation.md:1336 |
| `tcindustry:player_records` | 附件 | 工坊 | — | 13-industry.md:2425 |
| `tcnetworks:chunk_wear` | 附件 | 通途 | — | 14-networks.md:1106 |
| `tcfrontiers:insignia_record` | 组件 | 远征 | — | 20-frontiers.md:951 |
| `tcfrontiers:loaded_ammo` | 组件 | 远征 | — | 20-frontiers.md:951 |
| `tcfrontiers:notoriety` | 附件 | 远征 | — | 20-frontiers.md:950 |
| `tcfrontiers:nvg` | 组件 | 远征 | — | 20-frontiers.md:951 |
| `tcfrontiers:ration_contents` | 组件 | 远征 | — | 20-frontiers.md:951 |
| `tcfrontiers:siege_damage` | 附件 | 远征 | — | 20-frontiers.md:949 |
| `tcbeyond:inert` | 组件 | 异界 | — | 21-beyond.md:1220 |
| `tcbeyond:origin` | 附件 | 异界 | — | 21-beyond.md:1219 |
| `tcbeyond:overlay_marker` | 附件 | 异界 | — | 21-beyond.md:1217 |
| `tcbeyond:player` | 附件 | 异界 | — | 21-beyond.md:1218 |
<!-- GENERATED:attachments END -->

---

## 10. 数据包

### 10.1 目录与格式

1. 目录：`data/<modid>/<目录>/…/<条目>.json`。条目 ID = `<modid>:<相对路径去扩展名>`。
2. 新目录名用单数（与原版 1.21 起的 `recipe`、`loot_table` 一致）；已登记的目录名以 §10.4 为准，不为了统一而改名。
3. 每个数据包的 `pack.mcmeta` 带自定义字段 `"tc_data_format": <int>`（40 §2.1）；读取到更高的格式版本时，整包跳过并在重载摘要中报错。
4. 新增字段必须有默认值；字段改名或语义改变时提升 `tc_data_format` 并提供转换脚本（40 §2.2）。

### 10.2 加载方式

| 情况 | 方式 |
| --- | --- |
| 条目会被其他数据（标签、其他条目）按 ID 引用，或需要同步到客户端供界面显示 | 数据包注册表（NeoForge 的数据包注册表事件，带网络编解码） |
| 只在服务端使用的查表数据 | 服务端重载监听器；需要按服务器实例取用的数据使用 NeoForge 的保留监听器（`ListenerKey<T>`，从 `MinecraftServer` 取得），禁止静态单例持有服务器数据 |
| 客户端资源（模型、贴图、着色器、声音） | 资源包，不属于本节 |

1. 解析失败的条目跳过，记录 `WARN`（带文件路径与字段路径），不让整个重载失败。
2. 跨条目、跨目录的引用延迟到所有监听器完成后解析；引用不存在时按 §3.3(c) 处理。
3. 每次重载结束时，各模组在日志写一行摘要：`[tcindustry] 412 entries, 3 skipped, 2 missing refs`。
4. 需要客户端知道的服务端数据，在数据包同步时机用本模组载荷发送，不依赖客户端自己读数据包。

### 10.3 覆盖与扩展

1. 同一路径的条目，高优先级数据包覆盖低优先级（原版行为）。
2. 列表型字段支持 `"replace": false` 追加语义的，由拥有者章节说明；未说明的一律整体覆盖。
3. 其他模组为拥有者的目录追加条目时，条目放在**拥有者的命名空间目录**下、使用自己的命名空间 ID，并加 `neoforge:mod_loaded` 条件。

### 10.4 数据包目录索引

由全文中出现的 `data/<命名空间>/<目录>/` 路径生成。

<!-- GENERATED:datapack BEGIN -->
| 命名空间 | 目录 | 提及章节 |
| --- | --- | --- |
| `tcatlas` | `atlas_balance/` | 22 |
| `tcbeyond` | `loot_swap/` | 21 |
| `tcbeyond` | `purge_map/` | 21 |
| `tcbeyond` | `recipe/` | 21 |
| `tccommons` | `tc_policy/` | 19 |
| `tcdiscovery` | `collection/` | 17 |
| `tcdiscovery` | `domain_map/` | 17 |
| `tcdiscovery` | `experiment/` | 17 |
| `tcdiscovery` | `hypothesis/` | 17 |
| `tcdiscovery` | `observation_target/` | 17 |
| `tcexchange` | `market_profile/` | 16 |
| `tcexchange` | `specialty/` | 16 |
| `tcfrontiers` | `damage_type/` | 20 |
| `tcindustry` | `recipe/` | 13 |
| `tcindustry` | `tc_multiblock/` | 13 |
| `tcindustry` | `tc_recipe_gate/` | 13 |
| `tcivilization` | `tc_value/` | 03、16 |
| `tcnetworks` | `sail_polars/` | 14 |
| `tcnetworks` | `track_types/` | 14 |
| `tcnetworks` | `traction_profiles/` | 14 |
| `tcnetworks` | `vehicle_stats/` | 14 |
| `tcorbit` | `tags/` | 13、24 |
| `tcorbit` | `tc_disease/` | 24 |
| `tcsettlements` | `tc_building_type/` | 15 |
| `tcsettlements` | `tc_disease/` | 15 |
| `tcsettlements` | `tc_profession/` | 15 |
| `tcsettlements` | `tc_room_type/` | 15 |
| `tcsettlements` | `tc_schedule/` | 15 |
| `tcskills` | `loot_modifiers/` | 05 |
| `tctechnology` | `tc_tech/` | 04、13 |
| `tctechnology` | `tc_tech_fallback/` | 04 |
| `tcwildlands` | `neoforge/` | 10 |
| `tcwildlands` | `tags/` | 10 |
| `tcwildlands` | `tcwildlands/` | 10 |
| `tcwildlands` | `worldgen/` | 10 |
| `minecraft` | `dimension/` | 10、30 |
| `minecraft` | `tags/worldgen/` | 10 |
| `minecraft` | `worldgen/world_preset/` | 10 |
<!-- GENERATED:datapack END -->

---

## 11. 标签

1. 跨模组共享的材料、作物、食物、工具类别使用 `c:` 标签（NeoForge 通用标签约定），例如 `c:ingots/bronze`、`c:ores/cassiterite`、`c:crops/emmer`。
2. 只在本家族内有意义的分类使用 `<modid>:` 标签，例如 `tcindustry:heat_sources`。
3. 读者用标签而不是物品 ID 匹配材料；拥有者负责把自己的物品加入对应 `c:` 标签（02 §3）。
4. 标签目录用原版 1.21 起的单数名：`data/<ns>/tags/item/`、`tags/block/`、`tags/entity_type/`。
5. 标签只表示“属于某类”，不表示数值；数值放数据包条目。

### 11.1 标签索引

由全文中以 `#` 开头的标签引用生成，按命名空间分组。

<!-- GENERATED:tags BEGIN -->
| 命名空间 | 数量 | 标签（引用章节） |
| --- | --- | --- |
| `c` | 69 | `#c:barks`（13）；`#c:bitumen`（14）；`#c:brine`（13）；`#c:carbon_dioxide`（24）；`#c:circuits`（14）；`#c:circuits/integrated`（13、22）；`#c:cloth`（14）；`#c:coal`（14）；`#c:coke`（14）；`#c:concretes`（14）；`#c:crops`（05、12）；`#c:crops/cotton`（12、13）；`#c:crops/flax`（12、13）；`#c:crops/hemp`（12、13）；`#c:crops/sugarcane`（13）；`#c:dusts/vanadium`（13）；`#c:dusts/wood_ash`（12、13）；`#c:foods`（05、12、13）；`#c:foods/raw_meat`（13）；`#c:gears`（14）；`#c:gems/quartz`（21）；`#c:glass_blocks`（12、21）；`#c:glass_panes`（21）；`#c:ingots`（05）；`#c:ingots/brass`（13、22）；`#c:ingots/copper`（21）；`#c:ingots/gold`（21）；`#c:ingots/iron`（14、21）；`#c:ingots/silver`（22）；`#c:ingots/steel`（10、13、14）；`#c:is_`（10）；`#c:is_beach`（16）；`#c:is_mountain`（16）；`#c:is_ocean`（16）；`#c:is_swamp`（21）；`#c:latex`（13）；`#c:leathers`（16）；`#c:nuggets`（05）；`#c:nuggets/copper`（14）；`#c:nuggets/iron`（14）；`#c:obsidians`（21）；`#c:ores`（05、10）；`#c:ores/iron`（24）；`#c:ores/thorium`（24）；`#c:pipes`（14）；`#c:pistons`（14）；`#c:plates/aluminum`（14）；`#c:plates/copper`（13）；`#c:plates/iron`（14）；`#c:plates/steel`（14、24）；`#c:plates/titanium`（14）；`#c:raw_materials/native_copper`（13）；`#c:rods/iron`（14）；`#c:rods/steel`（10、14）；`#c:rods/wooden`（13）；`#c:rubbers`（14）；`#c:scrap/steel`（13）；`#c:seeds`（12）；`#c:silk_cocoons`（12、13）；`#c:stones`（13、21）；`#c:stones/limestone`（13）；`#c:storage/pottery_jars`（12）；`#c:storage_blocks`（05）；`#c:storage_blocks/steel`（14）；`#c:strings`（04、13、14、30）；`#c:tools`（16）；`#c:tools/knife`（13）；`#c:vegetable_oils`（13）；`#c:weapons`（16） |
| `tcbeyond` | 1 | `#tcbeyond:no_monsters`（21） |
| `tccultivation` | 4 | `#tccultivation:cereals`（12）；`#tccultivation:hedgerows`（12）；`#tccultivation:oilseeds`（12）；`#tccultivation:vanilla_seeds`（12） |
| `tcechoes` | 8 | `#tcechoes:adoptable`（18）；`#tcechoes:artefacts`（18）；`#tcechoes:cult_site`（18）；`#tcechoes:hostile_ruins`（03、18、30）；`#tcechoes:lost_knowledge`（18）；`#tcechoes:regalia`（18）；`#tcechoes:scholarly`（18）；`#tcechoes:stele_stones`（18） |
| `tcecology` | 2 | `#tcecology:built_surface`（11）；`#tcecology:coppiceable`（11） |
| `tcexchange` | 2 | `#tcexchange:category`（16）；`#tcexchange:category/metal`（16） |
| `tcindustry` | 16 | `#tcindustry:acid_alkali`（13）；`#tcindustry:bricks_raw`（13）；`#tcindustry:dusty`（13）；`#tcindustry:engineering_tools`（13）；`#tcindustry:hammers`（13）；`#tcindustry:hot_items`（13）；`#tcindustry:linings_1250c`（13）；`#tcindustry:machined_part`（13）；`#tcindustry:polymerization`（13）；`#tcindustry:pottery`（13）；`#tcindustry:relay_controlled`（13）；`#tcindustry:saddle_quern`（13）；`#tcindustry:steam_powered`（13）；`#tcindustry:superconducting`（13）；`#tcindustry:tannin`（13）；`#tcindustry:tongs`（13） |
| `tcivilization` | 4 | `#tcivilization:bone_bearing`（03、11、30）；`#tcivilization:monster_policy/exempt`（03、30）；`#tcivilization:monster_policy/restricted`（03、30）；`#tcivilization:monster_ruins`（03、10、18、30） |
| `tcorbit` | 24 | `#tcorbit:airtight`（24）；`#tcorbit:electronics/integrated_circuit`（13、24）；`#tcorbit:fluids`（24）；`#tcorbit:fusion_fuel`（24）；`#tcorbit:materials/aluminium`（13、24）；`#tcorbit:materials/aluminium_lithium`（24）；`#tcorbit:materials/carbon_fiber`（13、24）；`#tcorbit:materials/heat_ceramic`（13、24）；`#tcorbit:materials/titanium`（13、24）；`#tcorbit:nuclear_fuel`（13、24）；`#tcorbit:power/battery`（13、24）；`#tcorbit:power/fission_reactor`（13、24）；`#tcorbit:power/fuel_cell`（13、24）；`#tcorbit:power/rtg`（13、24）；`#tcorbit:power/solar_cell`（24）；`#tcorbit:power/solar_panel`（13、24）；`#tcorbit:propellants`（24）；`#tcorbit:propellants/argon`（13、24）；`#tcorbit:propellants/hypergolic`（13、24）；`#tcorbit:propellants/kerosene`（13、24）；`#tcorbit:propellants/liquid_hydrogen`（13、24）；`#tcorbit:propellants/liquid_oxygen`（13、24）；`#tcorbit:propellants/methane`（13、24）；`#tcorbit:space_food`（24） |
| `tcskills` | 6 | `#tcskills:building_blocks`（05）；`#tcskills:forage_sense`（05）；`#tcskills:livestock`（05）；`#tcskills:natural_logs`（05）；`#tcskills:no_proficiency`（05）；`#tcskills:redstone_components`（05、13） |
| `tctechnology` | 6 | `#tctechnology:cordage`（04、13）；`#tctechnology:fire_starters`（04）；`#tctechnology:forage`（04）；`#tctechnology:knappable`（04、10）；`#tctechnology:knappable_coarse`（04）；`#tctechnology:soft_rock`（04、10） |
| `minecraft` | 32 | `#minecraft:allows_surface_slime_spawns`（10）；`#minecraft:brewing_fuel`（21、30）；`#minecraft:crops`（10）；`#minecraft:decorated_pot_sherds`（18）；`#minecraft:eye_of_ender_located`（21）；`#minecraft:fences`（12）；`#minecraft:has_structure`（10、30）；`#minecraft:has_structure/ancient_city`（21）；`#minecraft:has_structure/mineshaft`（18）；`#minecraft:has_structure/ocean_monument`（21）；`#minecraft:has_structure/pillager_outpost`（21）；`#minecraft:has_structure/stronghold`（21）；`#minecraft:has_structure/woodland_mansion`（21）；`#minecraft:is_`（10、30）；`#minecraft:is_nether`（21）；`#minecraft:logs`（05、10、13、14、16）；`#minecraft:mineable/axe`（05）；`#minecraft:mineable/pickaxe`（05）；`#minecraft:more_frequent_drowned_spawns`（10）；`#minecraft:normal`（10）；`#minecraft:pickaxes`（10）；`#minecraft:planks`（13、16）；`#minecraft:produces_corals_from_bonemeal`（10）；`#minecraft:snow_golem_melts`（10）；`#minecraft:spawns_snow_foxes`（10）；`#minecraft:spawns_white_rabbits`（10）；`#minecraft:stone_crafting_materials`（10、30）；`#minecraft:stone_tool_materials`（10、30）；`#minecraft:walls`（12）；`#minecraft:water_on_map_outlines`（10）；`#minecraft:wooden_slabs`（14）；`#minecraft:wool`（12、13、16、30） |
<!-- GENERATED:tags END -->

---

## 12. 地区层

1. 注册与属性以 03 §5.2.3 为准：在模组总线的 `RegisterRegionLayersEvent` 中注册；ID 为 `<modid>:<layer>`；磁盘 `Codec` 与可选同步 `StreamCodec`；同步策略 `LayerSync` 取 `NONE`、`NEAR`（玩家所在 3×3 地区，经 `tcivilization:region_layer_sync`）、`ON_DEMAND`、`GLOBAL_SUMMARY`。
2. 每个模组一般只注册一层；确实需要多层时（如 Wildlands 的 `geo`、`climate`、`hydro`）在章节中说明理由。
3. 每层每地区 ≤ 16 KiB（§8.2）。逐方块数据不放地区层；地区层只存统计、索引与摘要。
4. 读取不存在的地区返回默认值，不创建文件；写入后才标脏。
5. 读其他模组的层：核心托管契约暴露的数据通过契约接口读（例如气候走 `TcWorldApis.climate()`，不直接读 `tcwildlands:climate`）；其余通过拥有者 `XApi`（§3.2）。禁止直接解码别人的层。
6. 每层必须登记在拥有者附录 A（类型 `region_layer`）。02 §2.2 的表只是概览，本节生成表是权威清单。

<!-- GENERATED:layers BEGIN -->
| 地区层 | 拥有者 | 说明 | 02 §2.2 | 登记位置 |
| --- | --- | --- | --- | --- |
| `tcivilization:civ` | 文明核心 | 发现位集、聚落索引、人物计数、压力 | 是 | 03-core-civilization.md:1675 |
| `tcwildlands:climate` | 山河 | 11.4 | 是 | 10-wildlands.md:3574 |
| `tcwildlands:geo` | 山河 | 11.4 | 是 | 10-wildlands.md:3573 |
| `tcwildlands:hydro` | 山河 | 11.4 | 是 | 10-wildlands.md:3575 |
| `tcecology:ecosystem` | 生息 | 种群、兽群、土地覆盖、干扰、毒性、EHI | 是 | 11-ecology.md:1223 |
| `tccultivation:agri` | 田园 | 11.3 | 是 | 12-cultivation.md:1642 |
| `tcindustry:emission` | 工坊 | 地区排放指数图层 | 是 | 13-industry.md:3496 |
| `tcnetworks:transport` | 通途 | 区域可达性、道路密度、运量统计 | 是 | 14-networks.md:1570 |
| `tcsettlements:demography` | 聚落 | 64×64 格人口、职业、满意度、患病率 | 是 | 15-settlements.md:1437 |
| `tcexchange:market` | 市集 | 64×64 格；价格指数、过路财富、贫困 | 是 | 16-exchange.md:1697 |
| `tcdiscovery:field_record` | 求知 | 每区域观察计数 | 是 | 17-discovery.md:1270 |
| `tcechoes:history` | 遗响 | §11.3 | 是 | 18-echoes.md:1572 |
| `tccommons:claim` | 契约 | Atlas 与核心地图着色 | 是 | 19-commons.md:1433 |
| `tcfrontiers:danger` | 远征 | 7 分量，每地区 8 字节 | 是 | 20-frontiers.md:1398 |
| `tcbeyond:anomaly` | 异界 | 每地区 ≤ 16 KiB | 是 | 21-beyond.md:1616 |
| `tcatlas:survey` | 舆图 | 每区块 SL/H/ε/日期，ON_DEMAND | 是 | 22-atlas.md:1772 |
| `tcnations:influence` | 列国 | 每地区前 3 个文明的影响力 | 是 | 23-nations.md:1174 |
| `tcorbit:remote_sensing` | 寰宇 | — | 是 | 24-orbit.md:1728 |
<!-- GENERATED:layers END -->

---

## 13. 命令

| 命令根 | 拥有者 | 权限 | 用途 |
| --- | --- | --- | --- |
| `/tc` | 核心 | 0（查询）/ 2（管理） | 文明与聚落的查询与管理（03 §5.13） |
| `/tcdebug` | 核心 | 3 | 调试：模拟积压、地区层、性能计数、存档检查 |
| `/tcclient` | 核心（客户端命令） | — | 客户端设置与缓存 |
| `/tcopen` | 核心（客户端命令） | — | 打开深链接（31 §4.8.3） |
| `/<modid>` | 各模组 | 按子命令 | 模组自己的查询、管理与调试 |

规则：

1. 服务端命令在命令注册事件中用 Brigadier 注册；客户端命令在客户端命令注册事件中注册。
2. 模组自己的命令根只能是完整模组 ID（例如 `/tcindustry`），不注册短别名，避免与其他模组冲突。需要挂在 `/tc` 下的子命令由核心提供 `TcCommands.registerSub(...)` 扩展点（只在初始化期）。
3. 权限等级：0 = 只读查询自己的数据；2 = 管理（修改他人或世界状态）；3 = 调试与可能破坏存档的操作；4 不使用。
4. 输出一律用 `Component.translatable`（键见 §2.4.3）；不在服务端拼接本地化文字。
5. 修改类命令同样写入编年史（31 §16.4.2）。

### 13.1 命令索引

由全文中出现的 `/tc…` 命令生成。

<!-- GENERATED:commands BEGIN -->
| 命令根 | 拥有者 | 子命令（提及章节） |
| --- | --- | --- |
| `/tc` | 核心 | `（根）`（03、40）；`chronicle`（03）；`civ`（03、31）；`person`（03）；`region`（03）；`status`（03）；`vanilla`（03、30） |
| `/tcclient` | 核心 | `debug`（03、31）；`shadercompat`（32、40） |
| `/tcdebug` | 核心 | `（根）`（03、31、35、40）；`person`（03）；`profile`（02、03、40）；`selftest`（03、04、40）；`simulate`（02、03、35、40）；`ui`（03、31）；`vanilla`（03、30）；`weather`（10） |
| `/tcopen` | 核心 | `（根）`（31） |
| `/tcatlas` | 舆图 | `reveal`（22）；`status`（22）；`survey`（22）；`tier`（22）；`waypoint`（22） |
| `/tcbeyond` | 异界 | `focus`（21）；`mode`（21）；`purge`（21）；`surge`（21） |
| `/tccultivation` | 田园 | `status`（12） |
| `/tcdiscovery` | 求知 | `status`（17） |
| `/tcechoes` | 遗响 | `status`（18） |
| `/tcecology` | 生息 | `status`（11） |
| `/tcexchange` | 市集 | `audit`（16） |
| `/tcindustry` | 工坊 | `status`（13） |
| `/tcivilization` | 文明核心 | `perf`（13）；`status`（03） |
| `/tcnations` | 列国 | `status`（23） |
| `/tcorbit` | 寰宇 | `status`（24） |
| `/tcskills` | 技能 | `（根）`（05）；`debug`（05、16）；`grant`（05）；`open`（05）；`reset`（05）；`respec`（05）；`set`（05）；`status`（05）；`talent`（05）；`tokens`（05）；`train`（05） |
| `/tcwildlands` | 山河 | `climate`（10）；`disaster`（10）；`here`（10）；`locate`（10）；`planet`（10）；`season`（10）；`status`（10）；`weather`（10、30） |
<!-- GENERATED:commands END -->

---

## 14. 拒绝原因与深链接

### 14.1 拒绝原因

`ActionCheck.Deny(Identifier reason, Component detail, Optional<String> fixLink)`（31 §16.1）中的 `reason` 取自下表；本地化键 `gui.<命名空间>.reason.<id>`。

| 原因 ID | 含义 | 典型提供者 |
| --- | --- | --- |
| `tcivilization:permission` | 角色或领地权限不足 | 核心默认规则、Commons |
| `tcivilization:era` | 时代不足 | Technology |
| `tcivilization:tech` | 缺少科技 | Technology |
| `tcivilization:knowledge` | 尚未认识该物 | Discovery |
| `tcivilization:resource` | 缺少材料 | 各模组 |
| `tcivilization:distance` | 距离过远 | 核心（§6.3.3） |
| `tcivilization:busy` | 目标忙碌 | 各模组 |
| `tcivilization:owner` | 属于其他文明 | 核心、Nations |
| `tcivilization:vote` | 需要投票 | Commons |
| `tcivilization:cooldown` | 冷却中 | 各模组 |
| `tcivilization:server` | 服务器关闭了此功能 | 核心（31 §16.5.2） |
| `tcivilization:conflict` | 版本冲突（只出现在 `action_result`） | 核心（§6.3.5） |

模组可以新增 `<modid>:<id>` 形式的原因，但必须先确认上表没有可用的原因；新增原因在章节附录 A 登记（类型 `deny_reason`），并提供本地化键与“如何解决”的深链接规则。

### 14.2 深链接

1. 格式 `tc:<modid>/<path>[?<query>]`（31 §4.8）。`modid` 是负责解析的模组，不一定是数据的拥有者。
2. 各模组在客户端初始化时用 `DeepLinkRegistry.register(modid, handler)` 注册解析器。
3. 生成链接前调用 `DeepLink.isResolvable(link)`；不可解析时显示为纯文本。
4. 查询参数只用 ASCII，值做 URL 编码；坐标用整数；对象用 ID 或 UUID。
5. 深链接不携带权限：打开后的界面照常按 §6.3 请求数据并校验。

---

## 15. 问题清单

由 `api_index.py` 生成。级别：**错误**必须在合并前修复；**警告**需要在所属阶段结束前处理；**提示**用于审阅，可以保留。

| 代码 | 级别 | 含义 |
| --- | --- | --- |
| `target-missing` | 错误 | 生成目标章节（33）不存在 |
| `event-dup` | 错误 | 同一事件类被两个模组定义，且不是核心共享事件 |
| `event-02-owner` | 错误 | 02 §2.3 登记的发布者与事件定义所在模组不一致 |
| `payload-dup` | 错误 | 同一负载 ID 在多个模组定义 |
| `payload-namespace` | 错误 | 负载 ID 的命名空间不是本模组 ID（核心托管负载除外） |
| `event-table-missing` | 警告 | 模组章节第 12 节没有事件表 |
| `event-dup-local` | 警告 | 同一事件在本章事件表中重复列出 |
| `event-02-undefined` | 警告 | 02 §2.3 列出的事件不在任何模组第 12 节事件表中 |
| `payload-table-missing` | 警告 | 模组章节第 12 节没有负载表 |
| `payload-path` | 警告 | 负载路径不是 `snake_case` |
| `payload-direction` | 警告 | 方向列无法识别为 S→C / C→S / 双向 |
| `payload-unregistered` | 警告 | 第 12 节有负载，附录 A 没有对应 `payload` 行 |
| `payload-undocumented` | 警告 | 附录 A 登记了负载，但第 12 节负载表中没有 |
| `api-dup-name` | 警告 | 同一类型简单名出现在多个模组 |
| `api-package` | 警告 | 类型所在包不符合 §2.1 根包约定 |
| `api-core-hosted-unregistered` | 警告 | 非核心章节在核心包中定义类型，但 03 未登记（§2.2） |
| `save-owner` | 警告 | `tc/` 存档文件的拥有者章节第 11 节未提及该文件 |
| `attachment-namespace` | 警告 | 附件/组件 ID 的命名空间不属于本模组 |
| `layer-unregistered` | 警告 | 02 §2.2 列出的地区层未在拥有者附录 A 登记 |
| `datapack-owner` | 警告 | 数据包目录的命名空间拥有者章节未提及该目录 |
| `tag-owner` | 警告 | 标签命名空间对应的模组章节未提及该标签 |
| `command-root` | 警告 | 命令根不对应任何模组 ID 或核心保留根 |
| `command-owner` | 警告 | 命令根的拥有者章节没有定义它 |
| `marker-missing` / `marker-unknown` | 警告 | 本章生成标记缺失或名称不在生成器列表中 |
| `event-shared` | 提示 | 核心事件由其他模组共同发布（§7.2，正常） |
| `event-cancel-unknown` | 提示 | 事件表未写可取消性 |
| `payload-default-rate` | 提示 | C→S 负载未写频率，按 §6.3 默认 |
| `payload-no-namespace` | 提示 | 负载表省略命名空间，已按本模组补全 |
| `api-core-hosted` | 提示 | 核心托管类型，已在 03 登记（正常） |
| `entry-optional` | 提示 | `XApi.get()` 返回 `Optional`，应按 §3.2 改为非空 `get()` |
| `type-no-package` | 提示 | `*Api` 类型的代码块没有 `package` 行 |
| `layer-02-missing` | 提示 | 附录 A 登记的地区层未列入 02 §2.2 概览 |

完整代码以脚本输出为准；下表为本次生成的明细。

<!-- GENERATED:issues BEGIN -->
| 级别 | 代码 | 位置 | 说明 |
| --- | --- | --- | --- |
| 提示 | `api-core-hosted` | 10-wildlands.md:2407 | `WorldApiSet` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2411 | `TcWorldApis` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2427 | `PlanetApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2445 | `ClimateApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2453 | `ClimateSample` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2456 | `ClimateNormals` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2459 | `WeatherApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2466 | `WeatherState` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2469 | `ForecastSkill` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2471 | `GeoApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2486 | `HistoricalTrace` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2497 | `HydroApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2507 | `SuitabilityApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2513 | `SiteScore` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2514 | `TransportMode` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2516 | `HazardApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2527 | `ProspectingApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2531 | `ProspectMethod` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2533 | `ProspectReading` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2536 | `Anomaly` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2539 | `LocalSeasonProvider` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2542 | `LocalSeason` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2545 | `SeasonalColorApi` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted` | 10-wildlands.md:2555 | `PlanetProgressStages` 由 tcwildlands 章节定义，位于核心包 `com.nightcity.tcivilization.api.world`，已在 03 登记 |
| 提示 | `api-core-hosted-shared` | 03-core-civilization.md:1374, 10-wildlands.md:2407 | 类型 `WorldApiSet` 在 tcivilization, tcwildlands 两处声明，同属核心包 `com.nightcity.tcivilization.api.world`，视为同一类型 |
| 提示 | `api-core-hosted-shared` | 03-core-civilization.md:1378, 10-wildlands.md:2411 | 类型 `TcWorldApis` 在 tcivilization, tcwildlands 两处声明，同属核心包 `com.nightcity.tcivilization.api.world`，视为同一类型 |
| 提示 | `event-shared` | 03-core-civilization.md:1303, 15-settlements.md:1202 | `SettlementFoundedEvent` 类由核心定义，tcsettlements 也发布（核心托管，可无硬依赖地共同发布） |
| 提示 | `event-shared` | 03-core-civilization.md:1304, 15-settlements.md:1203 | `SettlementTierChangedEvent` 类由核心定义，tcsettlements 也发布（核心托管，可无硬依赖地共同发布） |
| 提示 | `event-shared` | 03-core-civilization.md:1308, 10-wildlands.md:2614 | `SeasonChangedEvent` 类由核心定义，tcwildlands 也发布（核心托管，可无硬依赖地共同发布） |
| 提示 | `event-shared` | 03-core-civilization.md:1322, 18-echoes.md:1324 | `WorldHistoryReadyEvent` 类由核心定义，tcechoes 也发布（核心托管，可无硬依赖地共同发布） |
| 提示 | `payload-default-rate` | 04-core-technology.md:926 | C→S 负载 `tctechnology:research_command` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1764 | C→S 负载 `tcskills:talent_commit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1765 | C→S 负载 `tcskills:respec_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1766 | C→S 负载 `tcskills:specialty_set` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1767 | C→S 负载 `tcskills:roster_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1768 | C→S 负载 `tcskills:apprentice_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1769 | C→S 负载 `tcskills:profile_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1770 | C→S 负载 `tcskills:history_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 05-skills.md:1771 | C→S 负载 `tcskills:goal_pin` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 10-wildlands.md:2654 | C→S 负载 `tcwildlands:start_site_choice` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 11-ecology.md:1085 | C→S 负载 `tcecology:burn_confirm` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 11-ecology.md:1086 | C→S 负载 `tcecology:reserve_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 11-ecology.md:1088 | C→S 负载 `tcecology:pen_action` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 12-cultivation.md:1434 | C→S 负载 `tccultivation:seed_select` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 12-cultivation.md:1435 | C→S 负载 `tccultivation:name_variety` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 12-cultivation.md:1436 | C→S 负载 `tccultivation:set_rotation` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 12-cultivation.md:1437 | C→S 负载 `tccultivation:allocate_water` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 12-cultivation.md:1438 | C→S 负载 `tccultivation:herd_action` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 12-cultivation.md:1439 | C→S 负载 `tccultivation:hitch` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 15-settlements.md:1224 | C→S 负载 `tcsettlements:job_assign` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 15-settlements.md:1227 | C→S 负载 `tcsettlements:directive_toggle` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 15-settlements.md:1228 | C→S 负载 `tcsettlements:building_plan_place` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 16-exchange.md:1500 | C→S 负载 `tcexchange:order_cancel` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 16-exchange.md:1501 | C→S 负载 `tcexchange:contract_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 16-exchange.md:1502 | C→S 负载 `tcexchange:contract_sign` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 16-exchange.md:1504 | C→S 负载 `tcexchange:mint_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 16-exchange.md:1505 | C→S 负载 `tcexchange:bank_action` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1264 | C→S 负载 `tccommons:claim_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1265 | C→S 负载 `tccommons:parcel_settings` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1266 | C→S 负载 `tccommons:role_assign` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1266 | C→S 负载 `tccommons:office_assign` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1267 | C→S 负载 `tccommons:proposal_submit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1267 | C→S 负载 `tccommons:ballot_cast` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1268 | C→S 负载 `tccommons:statute_decree` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1269 | C→S 负载 `tccommons:works_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1269 | C→S 负载 `tccommons:covenant_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1269 | C→S 负载 `tccommons:covenant_sign` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1270 | C→S 负载 `tccommons:case_rule` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 19-commons.md:1270 | C→S 负载 `tccommons:stele_dedicate` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 21-beyond.md:1295 | C→S 负载 `tcbeyond:relay_action` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 21-beyond.md:1296 | C→S 负载 `tcbeyond:gate_action` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 22-atlas.md:1465 | C→S 负载 `tcatlas:map_tile_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 22-atlas.md:1470 | C→S 负载 `tcatlas:waypoint_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 22-atlas.md:1472 | C→S 负载 `tcatlas:annotation_edit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 22-atlas.md:1473 | C→S 负载 `tcatlas:layer_data_request` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 22-atlas.md:1476 | C→S 负载 `tcatlas:survey_action` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 23-nations.md:1003 | C→S 负载 `tcnations:army_order` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 23-nations.md:1006 | C→S 负载 `tcnations:vote_cast` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
| 提示 | `payload-default-rate` | 23-nations.md:999 | C→S 负载 `tcnations:proposal_submit` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家 |
<!-- GENERATED:issues END -->

---

## 16. 验收标准

| 编号 | 标准 | 验证方法 |
| --- | --- | --- |
| AC-33-01 | 18 个模组任意组合安装（必需依赖满足的前提下）都能启动、创建世界、存档、重新加载 | CI 组合矩阵：每个主题模组单独 + 核心；全家族；随机 20 种子集 |
| AC-33-02 | 移除任一主题模组后打开旧存档：不崩溃；该模组的地区层原样保留；重新安装后数据恢复 | 测试档：安装全部 → 运行 1 MC 日 → 移除 → 加载 → 再安装 → 比对层字节 |
| AC-33-03 | 所有主题模组的主类、方块、物品、实体、屏幕类中不出现其他主题模组的类型引用 | 构建期字节码扫描（`compat` 包除外） |
| AC-33-04 | `api_index.py --check --strict` 无错误；警告数在阶段结束前清零 | CI |
| AC-33-05 | 每个 C→S 载荷在 10 倍限流的压测下不导致服务器 MSPT 上升超过 1 ms，并产生预期的 `WARN` | 压测机器人 |
| AC-33-06 | 解码失败、超长字符串、超长集合的载荷全部被丢弃，服务端无异常栈 | 模糊测试：每个载荷 10,000 个随机字节包 |
| AC-33-07 | v0.N 存档在 v0.N+1 中可加载，迁移器链覆盖所有 `formatVersion` | CI 样本存档（40 §2.2） |
| AC-33-08 | 后台线程访问 `Level` 或发布事件时，开发环境立即报错 | 单元测试 + `api_strict_mode` |
| AC-33-09 | 10 名玩家各自打开文明面板并移动 10 分钟，TC 出口流量平均 ≤ 60 KiB/s（不含 Atlas 首次同步） | 31 §14.4 验收场景 |
| AC-33-10 | 数据包中引用未安装模组的 ID 时，重载不失败，摘要中计数正确 | 测试数据包 |

---

## 17. 开放问题与风险

| 编号 | 问题 | 当前倾向 | 影响 |
| --- | --- | --- | --- |
| OQ-33-1 | NeoForge 26.x 的附件同步机制是否足以覆盖人物与动物的客户端显示 | 先用模组载荷实现，P1 评估后迁移 | §9.2 |
| OQ-33-2 | 地区文件是否需要按维度分目录以外的分片（超大服务器） | 不需要；LRU 256 足够标准场景 | §8.2 |
| OQ-33-3 | 是否为第三方模组提供公开 API 文档与 Maven 发布 | v1.0 后再定；当前稳定面只承诺家族内部 | §3.4 |
| OQ-33-4 | `TcNet.limit` 的令牌桶参数是否应进入服务器配置 | 进入 `tcivilization-server.toml [network]`，默认值即本章数值 | §6.3 |
| OQ-33-5 | 生成工具对章节格式的依赖较强（表头文字、节号） | 保持；在 README §5.3 模板中写明必需表头 | §0.2 |
