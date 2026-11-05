# 31 · 界面与交互（UI / UX）总规范

> 本章是**横切章节**，不使用 README §5.3 的 18 节模组模板。它规定整个模组家族共用的界面框架、HUD、按键、通知、提示框、无障碍、本地化与客户端配置；各模组章节只描述“自己往这个框架里放了什么”。
>
> 冲突裁决顺序：`design-brief.md` 契约 → 00 章 DR/DA 决议 → **本章** → 各模组章节。若模组章节与本章的 ID、按键、通知等级不一致，以本章为准，并在 40 章的问题清单中登记。

---

## 0. 文档信息

| 项 | 内容 |
| --- | --- |
| 章节编号 | 31 |
| 适用模组 | 全部 18 个；框架由 `tcivilization` 提供，地图与布局编辑由 `tcatlas` 提供 |
| 依赖章节 | 00（DR-20 可见实体过程、DR-25 技能影响中等、DR-26/27 地图系统与随科技解锁、DR-28 HUD 沉浸极简 + 可展开、DR-33 技能独立模组）、01 §3.2（地图时代表）、02 §8.2（客户端预算）、02 §9（配置分层）、22（Atlas）、32 §5（UI 美术） |
| 下游章节 | 03、04、05、10-24 各章的“界面”小节；36 章注册表索引中的 `key`、`hud_widget`、`civ_tab`、`payload`、`menu` 类型条目 |
| 版本 | 设计 v1.0（对应代码 v0.2 起逐步落地） |
| 状态 | 规范稿；第 19 节列出未决问题 |

### 0.1 术语

| 术语 | 含义 |
| --- | --- |
| HUD | 游戏世界视图上常驻的覆盖层，不捕获鼠标 |
| 控件（widget） | HUD 上的一个独立单元，例如小地图、日期季节 |
| 面板（panel） | 捕获鼠标的全屏或半屏界面（`Screen`） |
| 文明面板 | 按 J 打开的统一面板外壳，各模组以“页签”接入 |
| 页签（tab） | 文明面板中的一页，由某模组注册 |
| 指令轮盘 | 按住 Z 出现的径向菜单，对准目标后选择指令 |
| 叠层（overlay） | 不捕获鼠标、但会暂时覆盖 HUD 的演出层，例如时代跃迁演出 |
| 皮肤（skin） | 一组纯外观资源：边框、底纹、字色、UI 声音；不改变布局与信息 |
| 深链接（deep link） | `tc:` 协议地址，可从通知、提示框、日志直接跳转到某个面板位置 |
| 逻辑像素 | 以 GUI 缩放为 1 时的像素；本章所有尺寸都用逻辑像素，参考画布 960×540 |

---

## 1. 体验原则

| 编号 | 原则 | 落地规则 |
| --- | --- | --- |
| UP-1 | **世界优先，面板其次** | 能在世界中看到的状态（机器转动、烟囱冒烟、作物颜色、NPC 动作）不再用 HUD 数字重复；HUD 只放“看不见但需要时刻知道”的信息（DR-20、DR-28） |
| UP-2 | **按需显示** | 默认 HUD 等级为“标准”；控件只在相关时出现（例如没有研究时不显示研究进度） |
| UP-3 | **原版手感不被破坏** | 不移动原版热栏、血量、饥饿、经验条、Boss 栏、聊天框、副手栏、状态效果图标；不重绑原版默认按键 |
| UP-4 | **一个外壳，多个内容** | 所有模组共享一个文明面板（J）、一个指令轮盘（Z）、一个通知系统；不允许各模组自建同类入口 |
| UP-5 | **缺模组不残缺** | 任意子集安装时，界面不出现空页签、死按钮、无效链接；见第 17 节回退表 |
| UP-6 | **知识决定信息量** | 玩家未掌握的知识以“？”或模糊描述显示（第 8.3 节），而不是隐藏整个条目，保留“我还不知道”的探索感 |
| UP-7 | **时代改变外观，不改变位置** | 皮肤随时代变化，但按钮位置、快捷键、信息层级保持不变，避免玩家每个时代重新学习 |
| UP-8 | **不依赖颜色单独表达** | 所有状态同时使用形状、图标或文字（32 §5） |
| UP-9 | **打断要有理由** | 只有 `critical` 与 `ceremony` 级别可以打断玩家；战斗中进一步推迟（第 7.5 节） |
| UP-10 | **多人可解释** | 任何因权限无法执行的操作都显示为灰色并说明原因，而不是消失（第 16 节） |

---

## 2. 界面层级与输入捕获

### 2.1 层级栈

从下到上绘制；同一时刻只有一个层可以捕获鼠标。

| 层 | 名称 | 内容 | 捕获鼠标 | 捕获键盘 | 暂停单人游戏 |
| --- | --- | --- | --- | --- | --- |
| L0 | 世界 | 方块、实体、粒子、光影 | 否 | 否 | — |
| L1 | 世界内标注 | 路点信标、观察模式描边、施工蓝图幽灵方块、领地边界线 | 否 | 否 | — |
| L2 | 原版 HUD | 热栏、生命、饥饿、经验、Boss 栏、聊天 | 否 | 否 | — |
| L3 | TC HUD | 本章第 3 节的控件 | 否 | 否 | — |
| L4 | 叠层 | 时代跃迁演出、区域进入标题、观察模式信息卡 | 否 | 仅“跳过”键 | 否 |
| L5 | 轮盘 | 指令轮盘 Z | 是（仅方向） | 是 | 否 |
| L6 | 面板 | 文明面板、科技树、技能、日志、世界地图、机器 GUI、人物界面 | 是 | 是 | 见 2.2 |
| L7 | 模态对话 | 确认框、命名框、冲突解决框 | 是 | 是 | 继承 L6 |
| L8 | 通知吐司 | 第 7 节的弹出通知 | 否 | 否 | — |
| L9 | 原版系统 | 暂停菜单、设置、聊天输入、F3 | 是 | 是 | 原版规则 |

L8 绘制在 L6 之上，因此打开面板时仍能看到通知。游戏中（鼠标被捕获）通知不能点击；打开任一 TC 面板、鼠标释放后，仍在显示的 TC toast 可以点击跳转（7.2 第 4 条）。原版 toast 与非 TC 面板中的点击行为不变。

### 2.2 暂停规则

单人游戏（集成服务器且未对局域网开放）时：

| 界面 | 是否暂停 | 理由 |
| --- | --- | --- |
| 文明面板 J、科技树 U、技能、日志 Y、外交 | **是** | 阅读与规划型界面，暂停避免玩家在规划时被怪物攻击 |
| 世界地图 M | **否**（可配置为是） | 地图常用于导航中途查看；配置 `tcatlas-client.toml [map] pause_singleplayer = false` |
| 指令轮盘 Z | 否 | 必须实时，且按住才显示 |
| 观察模式 V | 否 | 观察是“在世界里看”，暂停会破坏动态观察（例如动物习性） |
| 机器 GUI、人物界面 | 否 | 与原版容器界面一致 |
| 时代跃迁演出（L4） | 否 | 但演出开始时若玩家处于危险中则推迟（第 9 节） |

多人游戏从不暂停。

### 2.3 打开面板时的行为

1. 打开 L6 面板时，L3 HUD 控件隐藏（与原版容器界面一致），L8 通知保留。
2. 从一个 L6 面板跳到另一个（例如在文明面板中点击“在地图上显示”）时，使用面板栈：`Esc` 返回上一个面板，而不是直接回到游戏。栈深上限 4，超出时丢弃最底层。
3. 面板按键可在面板内再次按下以关闭（例如在文明面板中按 J 关闭），前提是焦点不在文本框中。
4. 任何面板打开期间，玩家角色不响应移动键；但在单人非暂停面板（地图、机器）中，世界继续运行。

### 2.4 焦点与文本输入

1. 文本框获得焦点时，所有 `key.tc*` 按键失效，直到焦点离开（防止输入“j”时关闭面板）。
2. 文本框统一使用原版 `EditBox`，支持中文输入法（IME）候选框定位到光标。
3. 搜索框在面板打开时**不**自动获得焦点；按 `Ctrl+F` 或点击才聚焦。这样面板快捷键第一时间可用。

---

## 3. HUD

### 3.1 三个显示等级

`H` 键在三个等级之间循环（极简 → 标准 → 扩展 → 极简），切换后屏幕中上方显示 1.5 秒提示“HUD：标准”。原版 `F1` 仍然隐藏全部 HUD（含 TC 控件），不计入等级。

| 等级 | 枚举 | 默认 | 显示内容 | 目标玩家 |
| --- | --- | --- | --- | --- |
| 极简 | `MINIMAL` | | 只有 `minLevel = MINIMAL` 的控件：警报、导航中的路点指引 | 沉浸、截图、录像 |
| 标准 | `STANDARD` | ✔ | 极简 + 小地图、罗盘条、日期季节、目标信息、追踪、研究进度，以及各模组“情境控件”（只在相关时出现） | 大多数玩家 |
| 扩展 | `EXPANDED` | | 标准 + 坐标（已解锁时）、时代进度、各模组的统计型控件 | 规划与管理型玩家 |

等级保存在 `tcivilization-client.toml [hud] level`。每个控件可在布局编辑器中单独“强制显示/强制隐藏”，覆盖等级规则（第 3.6 节）。

### 3.2 控件注册接口

核心在客户端注册**一个** NeoForge GUI 层 `tcivilization:hud`（位于原版 `BOSS_OVERLAY` 之上、`CHAT` 之下），所有 TC 控件都在这个层内由核心统一布局与绘制。各模组不直接注册 GUI 层，而是在核心发出的 `RegisterHudWidgetsEvent`（模组总线，客户端）中登记控件：

```java
public record HudWidgetSpec(
        Identifier id,                     // 例如 tcatlas:minimap
        HudLevel minLevel,                 // MINIMAL | STANDARD | EXPANDED
        HudAnchor defaultAnchor,           // 9 个锚点之一，见 3.5
        int defaultX, int defaultY,        // 相对锚点的偏移（逻辑像素，向内为正）
        int prefWidth, int prefHeight,     // 首选尺寸
        int compactHeight,                 // 压缩模式高度；0 = 不支持压缩
        int priority,                      // 0-100；越大越靠近锚点、越晚被挤出
        boolean defaultVisible,            // 例如 tctechnology:era_progress 为 false
        Optional<Identifier> unlockTech,   // 解锁所需科技；空 = 始终可用
        int refreshTicks,                  // 数据刷新周期（绘制仍每帧）
        Set<Identifier> dataChannels,      // 订阅的服务端数据通道；空 = 纯客户端数据
        Predicate<HudContext> relevant,    // 返回 false 时不占位
        Supplier<HudWidget> factory) {}

public interface HudWidget {
    void tick(HudContext ctx);                                    // 客户端 tick：更新缓存，不绘制
    void render(GuiGraphics g, HudRect rect, float partialTick, HudContext ctx);
    default void onData(Identifier channel, CompoundTag data) {}  // 服务端推送（3.8）
    default void onSkinChanged(UiSkin skin) {}
}

// 用法（模组客户端入口）
modBus.addListener(RegisterHudWidgetsEvent.class, e -> e.register(MINIMAP_SPEC));
```

规则：

1. 控件 ID 命名 `<modid>:<widget>`，本地化键 `hud.<modid>.<widget>`（标题）与 `hud.<modid>.<widget>.desc`（布局编辑器中的说明）。
2. `relevant` 必须是 O(1) 判断（读缓存字段），不得在其中访问世界或发包。
3. `unlockTech` 未满足时控件不显示，布局编辑器中显示为锁定，并给出科技名（若玩家尚不知道该科技，则显示“？”，见 8.3）。
4. 所有控件同时登记到所属模组附录 A，类型 `hud_widget`；36 章自动汇总。
5. `HudContext` 提供：当前时代、已解锁科技集合（客户端镜像）、HUD 等级、GUI 缩放、皮肤、无障碍设置、玩家位置与朝向、是否战斗中（7.5）、是否在载具中、所在维度。

### 3.3 控件目录

#### 3.3.1 核心与 Atlas、Technology 控件（本章权威）

| 控件 ID | 所属 | 最低等级 | 默认锚点 | 首选尺寸 | 刷新 | 出现条件 | 时代门槛 | 内容 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `tcivilization:alerts` | 核心 | 极简 | 左上（小地图下方） | 96×≤60 | 20 tick | 至少 1 条持续警报 | 无 | 持续性状况（不是一次性通知）：如“粮仓 3 日内耗尽”“2 台机器损坏”“边境冲突”。最多 4 行，按严重度排序；超出时末行显示“另 N 条”。数据来自 `AlertProvider` |
| `tcivilization:date_season` | 核心 | 标准 | 左上 | 96×12 | 20 tick | 始终 | E0 只显示季节图标与“第 N 日”；掌握历法类科技后显示年份 | 季节图标 + “第 3 年 · 夏 · 第 7 日” + 昼夜小表盘（E4 前为日晷样式，之后为钟表） |
| `tcivilization:target_info` | 核心 | 标准 | 上中（罗盘条下方） | 200×≤48 | 4 tick | 准星指向方块/实体（距离 ≤ 交互距离） | 无 | 名称、来源模组、关键状态（作物生长阶段、机器状态、NPC 姓名与职业）。内容由各模组 `TargetInfoProvider` 提供 |
| `tcivilization:pinned_tracker` | 核心 | 标准 | 右中 | 160×≤90 | 20 tick | 至少 1 个已固定追踪项 | 无 | 最多 3 个追踪项：图标 + 标题 + 进度条 + 预计完成时间。来源：研究、公共工程、订单、远征、线索（`TrackerApi`） |
| `tctechnology:research_progress` | Technology | 标准 | 右中（追踪下方） | 160×20 | 20 tick | 存在进行中的研究 | 无 | 当前研究名、RP 进度条、预计剩余天数 |
| `tctechnology:era_progress` | Technology | 扩展 | 右中 | 160×20 | 100 tick | 默认隐藏 | 无 | 向下一时代的关键科技完成数，如“E3→E4 关键科技 2/4” |
| `tcatlas:minimap` | Atlas | 标准 | 左上 | 96×96（可调 64-160） | 地形异步；标记每帧 | 已解锁 | E3（半径 64，只显示已测绘区域）；E4 起半径 128 | 见 22 章 |
| `tcatlas:compass_bar` | Atlas | 标准 | 上中 | 240×14 | 每帧 | 已解锁 | E4 | 朝向刻度、方位字母、视野内路点与聚落标记 |
| `tcatlas:waypoint_nav` | Atlas | 极简 | 上中（罗盘条下方） | 160×12 | 10 tick | 正在导航到某路点 | E1 | 目标名、距离、方向箭头。精度随时代：E1-E3 只给模糊描述（“东北方，约半日路程”），E4-E9 精确到 10 格，E10 精确到 1 格 |
| `tcatlas:coordinates` | Atlas | 扩展 | 左上 | 96×10 | 5 tick | 已解锁 | E10（GPS） | 精确坐标与海拔。未解锁前玩家仍可用原版 F3 |

#### 3.3.2 各模组情境控件（建议 ID，以各章附录 A 为准）

以下控件的最终 ID、尺寸和内容由各模组章节定义；本表只规定它们在布局中的**位置约定与出现条件**，避免默认布局互相覆盖。

| 建议控件 ID | 所属章 | 最低等级 | 默认锚点 | 出现条件 |
| --- | --- | --- | --- | --- |
| `tcskills:proficiency_feed` | 05 | 标准 | 右中（堆叠末尾） | 最近 5 秒内获得熟练度；合并显示“+12 采矿”，3 秒淡出 |
| `tcwildlands:local_conditions` | 10 | 扩展 | 左上 | 始终（扩展等级）；显示气温、降水、海拔带、地区名 |
| `tcecology:ecosystem_health` | 11 | 扩展 | 左上 | 已掌握生态观察类知识 |
| `tcecology:tracking` | 11 | 标准 | 上中（`target_info` 下方） | 处于追踪姿态（地面上潜行静止 1 秒）；显示最新痕迹摘要 |
| `tccultivation:field_status` | 12 | 标准 | 右中 | 站在己方耕地或围场 8 格内，且手持锄、种子、肥料、镰刀或 `herding_crook` |
| `tcindustry:network_status` | 13 | 扩展 | 右中 | 手持扳手或工程类工具 |
| `tcnetworks:vehicle_status` | 14 | 标准 | 下中（热栏上方，动作栏之上） | 乘坐列车、船舶、车辆时；替换原版坐骑血条的位置；与 `tcorbit:flight_telemetry` 互斥 |
| `tcnetworks:route_survey` | 14 | 标准 | 右中 | 手持 `road_planner`、`track_layer` 或 `survey_level` |
| `tcsettlements:settlement_status` | 15 | 标准 | 右上（状态效果下方） | 位于己方聚落范围内 |
| `tcexchange:market_ticker` | 16 | 扩展 | 右中 | 位于市集或贸易站范围内 |
| `tcdiscovery:experiment_status` | 17 | 标准 | 右中 | 32 格内有自己主持的实验正在运行 |
| `tcdiscovery:field_notes` | 17 | 扩展 | 左下（聊天区可见时被推到其上方） | 所在区域仍有 ≥ 1 种未观察过的目标 |
| `tcechoes:clue_compass` | 18 | 标准 | 右中 | 正在追踪一条传说且位于其传闻圈 512 格内；进入遗址后隐藏 |
| `tccommons:claim_status` | 19 | 极简 | 左上（堆叠末尾） | 进入他人或公共地块后 3 秒；手持勘界绳时常驻。极简等级只显示地块名 + 身份图标 |
| `tcfrontiers:expedition_supply` | 20 | 标准 | 右中 | 处于远征状态 |
| `tcfrontiers:danger_level` | 20 | 标准 | 左上（`local_conditions` 下方） | 所在地区危险值 D ≥ 30；扩展等级下始终显示 |
| `tcfrontiers:ammo_counter` | 20 | 标准 | 下中侧挂（热栏右侧，`defaultX = 122`，3.5） | 手持需要装填的远程武器 |
| `tcbeyond:anomaly_meter` | 21 | 标准 | 右中 | 位于异界影响区 |
| `tcnations:war_status` | 23 | 标准 | 右中 | 玩家文明处于战争中 |
| `tcnations:territory_entry` | 23 | 极简 | 上中（堆叠末尾） | 跨越国界后 3 秒，淡入淡出 |
| `tcorbit:life_support` | 24 | 标准 | 左中 | 位于太空维度或航天器内 |
| `tcorbit:flight_telemetry` | 24 | 标准 | 下中（与 `vehicle_status` 互斥，本控件优先） | 驾驶或乘坐处于动力飞行、对接或着陆阶段的 24 章航天器 |

#### 3.3.3 同锚点默认优先级

下表给出 `HudWidgetSpec.priority` 的默认值：数值越大越靠近锚点，空间不足时越晚被挤出（3.5）。各章写出的数值与本表不同时以本表为准；新控件取同锚点中未占用的值并在此登记。

| 锚点 | 控件与 `priority`（从锚点向外） |
| --- | --- |
| 左上 `TOP_LEFT` | `tcatlas:minimap` 90 → `tcivilization:date_season` 85 → `tcatlas:coordinates` 80 → `tcivilization:alerts` 75 → `tcwildlands:local_conditions` 60 → `tcfrontiers:danger_level` 55 → `tcecology:ecosystem_health` 50 → `tccommons:claim_status` 35 |
| 上中 `TOP_CENTER` | `tcatlas:compass_bar` 90 → `tcatlas:waypoint_nav` 85 → `tcivilization:target_info` 70 → `tcecology:tracking` 60 → `tcnations:territory_entry` 40 |
| 右上 `TOP_RIGHT` | `tcsettlements:settlement_status` 50（位于状态效果与吐司禁区之下） |
| 左中 `MIDDLE_LEFT` | `tcorbit:life_support` 50 |
| 右中 `MIDDLE_RIGHT` | `tcivilization:pinned_tracker` 90 → `tcnations:war_status` 85 → `tcfrontiers:expedition_supply` 80 → `tcbeyond:anomaly_meter` 75 → `tctechnology:research_progress` 70 → `tctechnology:era_progress` 65 → `tcdiscovery:experiment_status` 60 → `tcechoes:clue_compass` 55 → `tcnetworks:route_survey` 45 → `tccultivation:field_status` 35 → `tcindustry:network_status` 30 → `tcexchange:market_ticker` 25 → `tcskills:proficiency_feed` 10 |
| 左下 `BOTTOM_LEFT` | `tcdiscovery:field_notes` 50 |
| 下中 `BOTTOM_CENTER` | 互斥组 `vehicle`：`tcorbit:flight_telemetry` 60 / `tcnetworks:vehicle_status` 50；侧挂：`tcfrontiers:ammo_counter` |

排序原则：危险与玩家主动固定的信息最靠近锚点；工具情境控件（手持某物才出现）居中；被动信息流在末尾。短暂控件（每次出现 ≤ 5 秒，例如进入地块、跨越国界）放在堆叠末尾，出现和消失时不会让常驻控件跳动。

### 3.4 默认布局（960×540 逻辑像素参考画布）

1920×1080 分辨率、GUI 缩放 2 时正好是 960×540。下图为“标准”等级、E4 之后的默认布局；方括号内为原版元素（不可移动，作为禁区）。

```text
(0,0)                                                                        (960,0)
┌──────────────────────────────────────────────────────────────────────────────────┐
│┌──────────┐         ┌──────── compass_bar 240×14 ────────┐   [状态效果图标 右上] │
││ minimap  │         └────────────────────────────────────┘   [原版吐司 / TC 通知]│
││  96×96   │            ↓ waypoint_nav 160×12                                     │
││          │          [Boss 栏：出现时 compass_bar 与其下控件整体下移]            │
││          │         ┌────── target_info 200×≤48 ──────┐   settlement_status    │
│└──────────┘         └─────────────────────────────────┘   （右上，效果图标下方）│
│ date_season 96×12                                                                │
│ coordinates 96×10（扩展/E10）                         ┌── pinned_tracker ──────┐ │
│ alerts 96×≤60                                         │ 160×≤90               │ │
│ local_conditions（扩展）                              └────────────────────────┘ │
│                                                        research_progress 160×20  │
│ life_support（左中，仅太空）                           expedition / anomaly 等   │
│                                                                                  │
│                                [动作栏 / 标题]                                   │
│ [原版聊天区 左下]                 vehicle_status（乘坐时）          [字幕 右下]  │
│                          [生命 / 饥饿 / 护甲 / 经验条]                           │
│                          [        热栏 182×22        ]                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

设计取舍：

1. **小地图放左上**，因为原版状态效果图标与吐司都在右上；左上原本只有 F3 调试信息（F3 打开时 TC 左上控件整体隐藏）。
2. 罗盘条放上中，与原版 Boss 栏共享区域，用动态下移解决（3.5）。
3. 右侧中部是“追踪类”信息的统一区域，从上到下按优先级堆叠。
4. 下中只留给载具状态，且仅在乘坐时出现，替代原版坐骑血条的视觉位置，不与热栏争空间。

### 3.5 锚点、堆叠与碰撞解决

**锚点**：9 个，`TOP_LEFT`、`TOP_CENTER`、`TOP_RIGHT`、`MIDDLE_LEFT`、`CENTER`、`MIDDLE_RIGHT`、`BOTTOM_LEFT`、`BOTTOM_CENTER`、`BOTTOM_RIGHT`。`CENTER` 锚点仅允许叠层使用，控件不得默认放在屏幕中心（准星区域）。

**堆叠方向**：上排锚点向下堆叠，下排锚点向上堆叠，中排向下堆叠；同锚点的控件按 `priority` 从大到小依次排列，间距 2 逻辑像素。

**禁区（每帧计算，开销 ≤ 0.01 ms）**：

| 禁区 | 计算方式 |
| --- | --- |
| 热栏 + 生命/饥饿/护甲/氧气/经验 | 底部居中，宽 182，高 = 22 + 原版状态行数 × 10 + 经验条 7 |
| Boss 栏 | 顶部居中，宽 182，高 = Boss 栏数 × 19；无 Boss 栏时为空 |
| 状态效果图标 | 右上；宽 = 有益/有害两行各自数量 × 25，高 = 行数 × 26 |
| 原版吐司 | 右上，宽 160，高 = 当前可见吐司数 × 32 |
| 聊天 | 左下；仅当聊天区有可见消息或聊天输入打开时计入，尺寸读取原版聊天设置 |
| 字幕 | 右下；仅当字幕开启且有内容 |
| 动作栏 / 标题 | 中下与屏幕中心；存在时计入 |
| F3 调试 | 打开时左上与右上整列视为禁区 |
| 副手槽 / 攻击指示器（热栏样式） | 热栏左右两侧各宽 29；仅在显示时计入（副手槽随主手方向换边） |

**解决算法**（布局变化或禁区变化时运行，结果缓存）：

```text
for anchor in anchors:
    cursor = anchor 的起点（加上已有禁区的推移）
    for w in widgets(anchor) sorted by priority desc:
        if not visible(w) or not relevant(w): continue
        rect = place(w, cursor, prefSize)
        if rect 与任何禁区相交:       rect = 沿堆叠方向推到禁区之外
        if rect 超出屏幕且 w 支持压缩: rect = place(w, cursor, compactSize)
        if rect 仍超出屏幕:           隐藏 w，并让 tcivilization:alerts 显示“HUD 空间不足：N 个控件已隐藏”
        cursor = rect 末端 + 2
```

玩家在布局编辑器中手动放置的控件使用“自由坐标”，不参与堆叠，但仍做禁区推移；与其他控件重叠时允许重叠（玩家自己的选择），编辑器会以黄色描边提示。

**侧挂**：中轴锚点（`TOP_CENTER`、`BOTTOM_CENTER`）上的 `defaultX` 以向右为正。`defaultX ≠ 0` 的控件不进入堆叠游标，按偏移直接放置，只做禁区推移。目前只有 `tcfrontiers:ammo_counter`（`defaultX = 122`：热栏半宽 91 + 副手槽 29 + 间距 2），因此它不会把 `vehicle_status` 顶高。

**互斥组**：同一互斥组的控件同一时刻只绘制 `relevant` 为真且 `priority` 最高的一个，其余不占位。目前只有 `vehicle` 组（3.3.3）。

### 3.6 布局数据与布局编辑器

1. **布局数据归核心所有**：`config/tcivilization/hud_layout.json`（客户端）。核心在没有 Atlas 时读取它（只能手动编辑或使用默认值）。
2. **布局编辑器由 Atlas 提供**（`Ctrl+H`，详见 22 章）：拖动控件、切换锚点、调整尺寸（若控件支持）、单独强制显示/隐藏、恢复默认、导入/导出布局字符串。编辑器以红色半透明显示禁区、黄色描边显示重叠。
3. 没有 Atlas 时，`Ctrl+H` 不注册；核心的设置界面（模组列表 → 配置）提供“重置 HUD 布局”按钮。

```json
{
  "schema": 1,
  "widgets": {
    "tcatlas:minimap":       { "anchor": "top_left", "x": 4, "y": 4, "w": 96, "h": 96, "mode": "auto" },
    "tctechnology:era_progress": { "mode": "force_hidden" },
    "tcivilization:pinned_tracker": { "anchor": "middle_right", "x": 4, "y": -40, "free": true }
  }
}
```

`mode` 取值 `auto`（按等级与相关性）、`force_shown`（只要已解锁就显示，即使不相关时也显示占位）、`force_hidden`。未列出的控件使用注册时的默认值。HUD 等级与缩放不在此文件中，而在 `tcivilization-client.toml [hud]`（13.2）。`schema` 升级时由核心迁移，旧版本文件备份为 `hud_layout.json.bak<n>`。

### 3.7 绘制与缓存

1. 每个控件在 `tick` 中准备好要绘制的文本与数值（`FormattedCharSequence`、进度比例、图标引用），`render` 只做提交，不做字符串拼接或本地化查找。
2. 纯文字控件在数据未变化时复用上一帧的排版结果。
3. 小地图使用一张动态纹理，以区块为单位增量更新，每帧最多更新 4 个区块瓦片（详见 22 章）。
4. 单个控件绘制预算 ≤ 0.05 ms/帧（小地图除外）；全部 HUD 加小地图 ≤ 0.6 ms/帧（02 §8.2）。
5. 调试：客户端指令 `/tcclient debug ui`（15.4）在屏幕上显示每个控件的平均与峰值耗时、数据包大小；超预算的控件名标红。
6. 动画（进度条平滑、提示淡入淡出）最长 250 ms；`reduced_motion = true` 时全部改为瞬时切换。

### 3.8 控件数据来源

| 类别 | 例子 | 数据方式 |
| --- | --- | --- |
| 纯客户端 | 罗盘条、坐标、日期季节、目标信息中的方块名 | 直接读取客户端世界与已同步的科技镜像，不发包 |
| 订阅型 | 警报、追踪、研究进度、聚落状态 | 打开 HUD 时发送 `tcivilization:hud_subscribe`（列出可见控件的数据通道），服务端按各通道刷新周期推送 `tcivilization:hud_data` 增量 |
| 请求型 | 目标信息中的 NPC 详情、机器内部状态 | 准星停留 ≥ 4 tick 后请求一次，结果缓存 40 tick |

切换 HUD 等级或控件可见性时重新发送订阅；不可见控件不接收数据。完整同步规则见第 14 节。

### 3.9 特殊状态下的 HUD

| 状态 | 行为 |
| --- | --- |
| F1 隐藏 HUD | 全部 TC 控件隐藏；通知仍入队，恢复后显示未过期的通知 |
| F3 调试 | 左上与右上的 TC 控件隐藏；中部与下部保留 |
| 旁观者模式 | 只显示罗盘条、坐标（若已解锁）与路点指引 |
| 创造模式 | 与生存相同；另外坐标控件不受时代门槛限制（方便建造） |
| 观察模式 V | 除警报外的控件淡出至 30% 不透明度，让出视野（5.4） |
| 时代跃迁演出 | 全部控件淡出，演出结束后恢复（第 9 节） |
| 截图（F2） | 不改变；玩家可先按 H 切换到极简 |
| 死亡界面 | 隐藏全部 TC 控件 |

---

## 4. 文明面板（J）外壳

### 4.1 定位

文明面板 `tcivilization:civ_panel` 是整个家族唯一的“管理入口”。核心只提供外壳（页签栏、页眉、通知中心、共享控件库）和两个页签（总览、编年史）；其余内容由各模组以页签接入。`U`、`Y` 以及技能、外交等快捷键本质上都是“打开文明面板并定位到某个页签”的快捷方式，以保证 UP-4“一个外壳”。

### 4.2 线框

面板尺寸：屏幕逻辑尺寸减去四周各 16 像素边距，最大 880×500，最小 480×300（低于最小值时启用紧凑布局：页签栏只显示图标）。

```text
┌─ 页眉 ───────────────────────────────────────────────────────────────────────────┐
│ [旗帜] 青川部族   [E4 中世纪]   第 3 年 夏 第 7 日   人口 46/312   ¥ 1,240 (≈860 VU) │
│                                       [研究：印刷术 62%]  [⚠ 3]  [地图 M]  [?]  [⚙] │
├────────┬─────────────────────────────────────────────────────────────────────────┤
│ 个人   │  面包屑：文明 › 聚落 › 青川镇                          [搜索 Ctrl+F]      │
│  技能  ├─────────────────────────────────────────────────────────────────────────┤
│ 文明   │                                                                         │
│ ▶总览  │                                                                         │
│  科技  │                        页签内容区                                       │
│  聚落  │                 （各模组页签在此绘制）                                  │
│  治理  │                                                                         │
│  外交  │                                                                         │
│ 经济   │                                                                         │
│  农业  │                                                                         │
│  生产  │                                                                         │
│  物流  │                                                                         │
│  市场  │                                                                         │
│ 世界   │                                                                         │
│  …     │                                                                         │
│ 知识   │                                                                         │
│  日志  │                                                                         │
│  编年史│                                                                         │
├────────┴─────────────────────────────────────────────────────────────────────────┤
│ [🔔 通知中心 5 未读]                              提示：Tab/Shift+Tab 切换页签     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

页签栏宽 72（紧凑 28），分组标题不可点击；滚动超出时出现滚动条。页眉高 36，底栏高 16。

### 4.3 页签注册

```java
public record CivPanelTabSpec(
        Identifier id,                       // 例如 tctechnology:tech_tree
        CivTabGroup group,                   // PERSONAL | CIVILIZATION | ECONOMY | WORLD | KNOWLEDGE
        int order,                           // 组内排序，小者在上
        Identifier icon,                     // 24×24 单色图标（32 §5）
        Optional<Identifier> unlockTech,     // 未解锁时页签显示为锁定，可查看解锁条件
        Predicate<CivTabContext> visible,    // 返回 false 时不显示（例如没有任何聚落）
        Optional<OverviewCard> overviewCard, // 可选：贡献给“总览”页的一张卡片
        Function<CivTabContext, CivTabPage> factory) {}

public interface CivTabPage {
    void init(CivTabContext ctx, Rect area);
    void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);
    boolean handleDeepLink(DeepLink link);      // 定位到页内某项；返回 false 时停在页首
    default void onSnapshot(CompoundTag data) {} // 服务端快照（4.6）
    default void onClose() {}
}
```

规则：

1. 页签只在所属模组安装时存在。**不得**为其他模组的功能预留空页签（UP-5）。
2. 页签 ID 同时登记到附录 A，类型 `civ_tab`；本地化键 `civtab.<modid>.<tab>`。
3. 同一模组最多注册 2 个页签；需要更多层级时在页签内部用子导航。
4. 打开文明面板时只构建当前页签；其他页签懒加载，切走 60 秒后释放。

### 4.4 默认页签表

“权威”行由本章确定；“建议”行的最终 ID 以各章附录 A 为准，但分组与排序遵循本表。

| 分组 | 排序 | 页签 ID | 所属 | 状态 | 快捷键 | 内容概要 |
| --- | --- | --- | --- | --- | --- | --- |
| 个人 | 50 | `tcskills:skills` | 05 | 权威 | 默认未绑定 | 22 项熟练度、技能点、天赋树、成长记录 |
| 文明 | 0 | `tcivilization:overview` | 03 | 权威 | J | 4.5 节 |
| 文明 | 100 | `tctechnology:tech_tree` | 04 | 权威 | U | 科技树（5.1 与 04 章） |
| 文明 | 200 | `tcsettlements:settlements` | 15 | 建议 | — | 聚落列表、岗位、政策、蓝图、居民 |
| 文明 | 250 | `tccommons:governance` | 19 | 建议 | — | 领地、权限、公共工程、城镇约定、投票 |
| 文明 | 280 | `tcfrontiers:defense` | 20 | 建议 | — | 防务概况、民兵、工事、军械（解锁 `neolithic/watch_and_ward`） |
| 文明 | 300 | `tcnations:diplomacy` | 23 | 权威 | 默认未绑定 | 已知文明、关系、条约、边境、战争状态 |
| 经济 | 400 | `tccultivation:agriculture` | 12 | 建议 | — | 农田、品种、畜群、粮食储备 |
| 经济 | 450 | `tcindustry:production` | 13 | 建议 | — | 工厂、机器网络、产能、维护队列 |
| 经济 | 500 | `tcnetworks:logistics` | 14 | 建议 | — | 线路、车站、车辆、仓储、配送 |
| 经济 | 550 | `tcexchange:market` | 16 | 建议 | — | 价格、订单、商队、信誉、货币汇率 |
| 世界 | 600 | `tcwildlands:geography` | 10 | 建议 | — | 已知地区、气候、矿产勘探记录 |
| 世界 | 610 | `tcatlas:cartography` | 22 | 权威（22 §10.10） | — | 路点、测绘覆盖、地图库、注释、地图交易 |
| 世界 | 650 | `tcecology:ecology` | 11 | 建议 | — | 物种图鉴、种群趋势、生态健康 |
| 世界 | 700 | `tcfrontiers:expeditions` | 20 | 建议 | — | 远征计划、据点、补给线 |
| 世界 | 750 | `tcbeyond:anomalies` | 21 | 建议 | — | 异界影响区、异质资源、奥术研究 |
| 世界 | 800 | `tcorbit:space_program` | 24 | 建议 | — | 发射计划、轨道资产、殖民地 |
| 知识 | 850 | `tcdiscovery:journal` | 17 | 权威 | Y | 观察记录、实验、线索、知识传播 |
| 知识 | 870 | `tcechoes:legends` | 18 | 建议 | Y（无 Discovery 时） | 遗迹、传说、历史线索 |
| 知识 | 900 | `tcivilization:chronicle` | 03 | 权威 | — | 编年史：自动记录的文明大事年表 |

### 4.5 总览页

总览页是一个卡片网格（2-3 列，按宽度自适应），每张卡片 200×100（可占 2 列宽）。核心固定卡片：

| 卡片 | 内容 | 点击 |
| --- | --- | --- |
| 文明概况 | 名称、旗帜、时代、建立年份、玩家成员数 | 编辑名称与旗帜（需权限） |
| 人口 | 实体 NPC 数 / 抽象人口总数、近一年趋势折线 | 聚落页签（若有） |
| 警报 | 与 HUD 警报同源，完整列表 | 各警报的深链接 |
| 时代进度 | 关键科技完成情况（无 Technology 时隐藏） | 科技树 |

其余卡片由各页签的 `overviewCard` 贡献，按页签排序排列；玩家可在总览页“编辑卡片”模式中隐藏或重排（保存在客户端 `config/tcivilization/overview.json`）。

### 4.6 数据获取

1. 打开面板时发送 `tcivilization:panel_request(tab, params)`，服务端返回 `tcivilization:panel_snapshot`（压缩 NBT，单个 ≤ 64 KB；更大的数据分页）。
2. 面板打开期间，当前页签每 40 tick 请求一次增量快照；不在当前页签的数据不刷新。
3. 列表数据（例如 312 名居民）由服务端分页，每页 50 条；客户端虚拟滚动。
4. 所有修改操作（改政策、改岗位）作为独立的 C2S 操作包发送，服务端校验权限与版本号（16.3），并返回结果。

### 4.7 共享控件库

核心提供以下客户端控件（包 `com.nightcity.tcivilization.client.ui`），各模组**必须**优先使用，以保证外观与无障碍行为一致：

| 控件 | 用途 | 要点 |
| --- | --- | --- |
| `TcTabBar` | 页签栏、页内子导航 | 键盘 Tab/Shift+Tab 与方向键 |
| `TcTable` | 可排序、可筛选的表格 | 虚拟滚动，≥ 10,000 行不掉帧；列宽可拖动；右键列标题选择列 |
| `TcChart` | 折线、柱状、面积、堆叠图 | 默认 Viridis/Okabe-Ito；悬停显示数值；可导出 CSV 到 `screenshots/tc/` |
| `TcMapSnippet` | 页内嵌入的小地图片段 | Atlas 存在时绘制地图；否则显示坐标文字与“在世界中标记”按钮 |
| `TcPersonChip` | 人物头像 + 姓名 + 职业 | 点击打开人物界面（5.9） |
| `TcItemChip` | 物品图标 + 数量 | 悬停显示完整物品提示框（第 8 节） |
| `TcProgressBar` | 进度条 | 同时显示百分比文字；颜色不单独表达状态 |
| `TcEraBadge` | 时代徽记 | “E4 中世纪”，形状 + 文字 |
| `TcSearchField` | 搜索框 | 支持拼音首字母匹配（中文环境）与英文模糊匹配 |
| `TcConfirmDialog` | 二次确认 | 危险操作（拆除、解散、宣战）使用红色边框 + 需输入名称确认 |
| `TcScrollPane` | 滚动区域 | 滚轮、拖动、键盘 PageUp/PageDown |
| `TcTooltip` | 分层提示框 | 第 8 节 |

### 4.8 深链接

格式：`tc:<modid>/<path>[?<query>]`。

| 例子 | 打开 |
| --- | --- |
| `tc:tctechnology/tech/<tech_id>` | 科技树，居中并选中该科技 |
| `tc:tcatlas/map?x=1200&z=-340&zoom=3` | 世界地图定位 |
| `tc:tcivilization/person/<uuid>` | 人物界面 |
| `tc:tcsettlements/settlement/<id>` | 聚落页签中的该聚落 |
| `tc:tcivilization/notifications?category=tcexchange:market` | 通知中心并筛选 |

规则：

1. 各模组通过 `DeepLinkRegistry.register(modid, handler)` 注册解析器。
2. 生成链接的一方必须先调用 `DeepLink.isResolvable(link)`；不可解析（目标模组未安装）时以纯文本显示，不显示为可点击（UP-5）。
3. 聊天中的深链接使用客户端指令 `/tcopen <link>` 作为点击事件，因此可以出现在聊天、书本与告示牌文本中。
4. 从深链接打开的面板压入面板栈，`Esc` 返回来源界面。

---

## 5. 专用界面

本节规定各专用界面的**外壳、交互与状态外观**；具体内容（科技清单、技能数值、地图图层等）以各模组章节为准。

### 5.1 科技树（U → `tctechnology:tech_tree`）

1. 按 U 打开文明面板并切到科技树页签，同时进入“宽屏模式”（隐藏左侧页签栏，按 `Tab` 恢复）。
2. 布局：横轴为时代列 E0-E10（每列宽度随该时代科技数量自适应），纵轴按 04 章的科技类别分泳道；节点 28×28 图标 + 下方名称；前置关系用折线连接，只在两端节点之一可见时绘制。
3. 视图操作：拖动或 WASD 平移；滚轮缩放（3 级：全局、时代、细节）；`Ctrl+F` 搜索（结果高亮并依次跳转）；筛选按钮：按模组、按类别、只看可研究。
4. 选中节点后右侧滑出详情栏（宽 220）：描述、前置、解锁内容（物品、方块、配方、HUD/地图功能）、研究成本（RP）、实践来源、知识传播状态、“设为当前研究”与“固定追踪”按钮。
5. 节点状态外观（形状 + 文字 + 颜色三者同时表达，满足 UP-8）：

| 状态 | 外观 | 文字 |
| --- | --- | --- |
| 未知 | 剪影 + “？”，名称显示为“？？？” | 悬停显示“尚未发现的知识”及已知的一条线索（若有） |
| 已知但前置未满足 | 灰色图标 + 锁形角标 | “需要：……” |
| 可研究 | 正常图标 + 细边框 | “可研究” |
| 研究中 | 正常图标 + 环形进度 | “研究中 62%” |
| 已完成 | 正常图标 + 实心底 + 勾形角标 | “已掌握” |
| 关键科技 | 任意状态外再加双边框 + 星形角标 | “关键科技：推动时代跃迁” |

6. 哪些科技显示为“未知”、哪些可见，由 04 章的可见性规则决定；本章只规定外观。
7. 性能：360 个节点、约 600 条连线，绘制 ≤ 2 ms/帧；视口外节点剔除，连线批量绘制。

### 5.2 技能（`tcskills:skills`）

1. 从文明面板“个人 › 技能”进入；快捷键 `key.tcskills.skills` 默认**未绑定**（K 被 Iris“切换光影”占用，见 6.3），玩家可在“控制”设置中自行绑定。例外：未装核心（S0）时默认为 `J`（6.1）。
2. 三栏布局：左栏为 22 项熟练度列表（分组见 05 章；每行显示图标、名称、等级、进度条）；中栏为所选熟练度的天赋树或全局天赋盘；右栏为详情：当前加成、下一级预览、获得熟练度的主要途径。
3. 技能点分配采用“暂存 → 确认”两步：点选天赋后进入暂存状态（虚线边框），点击“确认”后才提交服务端；关闭界面时若有未确认的暂存，弹出 `TcConfirmDialog`。
4. 升级与新天赋可用时发送 `success` 通知（类别 `tcskills:growth`，7.6），同一熟练度 10 秒内的多次升级合并为一条。
5. 因为技能影响为“中等”（DR-25），界面中所有加成以具体数值显示（“采伐速度 +8%”），不用模糊形容词。

### 5.3 指令轮盘（Z，`tcivilization:command_wheel`）

**外壳归核心**，指令由各模组的 `WheelProvider` 提供。

| 项 | 规则 |
| --- | --- |
| 触发 | 默认“按住 Z 显示，松开执行”（G 被原版“快捷操作”占用，R 被 Iris“重载光影”占用，见 6.3）；`tcivilization-client.toml [wheel] mode = hold \| toggle` |
| 目标 | 打开瞬间沿准星做射线检测，最远 64 格（需视线可达）；目标类型：人物实体、其他实体、方块、无目标。乘坐 TC 载具且射线未命中时，目标为所乘载具（例如 24 章“确认巡航”“车灯”） |
| 槽位 | 8 个方向槽（上、右上、右、右下、下、左下、左、左上）；超过 8 个选项时分页，滚轮翻页，最多 3 页 |
| 固定位置 | “上”槽固定为该目标的主要动作（例如对人物为“前往此处/跟随”）；“下”槽固定为“取消/关闭” |
| 选择 | 鼠标相对中心的方向（中心 12 像素为死区）；或数字键 1-8；或单击 |
| 不可用选项 | 灰色 + 原因文字（例如“需要聚落管理权限”“该居民正在睡觉”），不隐藏（UP-10） |
| 危险选项 | 例如“解雇”“拆除”，选择后弹出确认 |
| 延迟处理 | 打开时客户端先显示本地预测的选项，同时发送 `tcivilization:wheel_query`；服务端返回 `tcivilization:wheel_options` 后（通常 ≤ 100 ms）替换。执行发送 `tcivilization:wheel_execute`，服务端再次校验 |

```java
public interface WheelProvider {
    Identifier id();
    /** 服务端调用：返回此目标下的选项；不适用时返回空列表 */
    List<WheelOption> options(WheelContext ctx);
    /** 客户端可选：本地预测，减少等待感 */
    default List<WheelOption> predict(ClientWheelContext ctx) { return List.of(); }
    void execute(ServerPlayer player, WheelTarget target, Identifier optionId);
}

public record WheelOption(Identifier id, Identifier icon, Component label,
                          int priority, boolean enabled, Optional<Component> disabledReason,
                          boolean needsConfirm, WheelSlotHint slotHint) {}
```

只安装核心时的内置指令（对核心人物实体，DA-04）：跟随我、在此等候、前往该处、自由活动、查看（打开人物界面）。其他模组扩展的指令以各章为准，例如 Settlements 的“分配岗位”“标记建造区”，Networks 的“设为车站”“规划路线起点”，Atlas 的“在此添加路点”。

### 5.4 观察模式（V，Discovery）

1. 按 V 进入或退出；`Esc` 也退出。观察模式不暂停世界。
2. 进入后：HUD 除警报外淡出到 30% 不透明度；可观察对象（动植物、岩层露头、遗迹构件、机器）显示细线描边（L1），描边使用与原版方块选择框相同的线框渲染方式，以兼容 Iris 光影（32 §6）。
3. 准星对准可观察对象 ≥ 0.5 秒后，在对象旁出现信息卡（L4，不捕获鼠标）：已知信息、未知项“？”、观察进度环。持续对准使进度增加，满后获得观察记录（17 章）。
4. 可选视野放大 ×1.5（`[observe] zoom = true`）；`reduced_motion` 时放大为瞬时切换。
5. 没有 Discovery 时不注册 V；核心 `tcivilization:target_info` 仍提供基础名称与状态。

### 5.5 日志（Y → `tcdiscovery:journal`）

1. 按 Y 打开文明面板并切到“知识 › 日志”页签。日志采用“书页”式双栏排版（左目录、右正文），皮肤随时代从羊皮纸 → 笔记本 → 文件夹 → 平板。
2. 分区：观察、实验、线索、已知知识；Echoes 安装时追加“遗迹与传说”分区（通过 `JournalSectionRegistry`）。
3. 线索条目可“固定追踪”（进入 `pinned_tracker`），并带有深链接（地图位置、相关科技）。
4. 没有 Discovery、但有 Echoes 时：Echoes 自行注册 `key.tcechoes.journal`（Y），打开 `tcechoes:legends` 页签；两者都没有时 Y 不注册。

### 5.6 地图（M / N / B，Atlas）

世界地图 `M` 是独立全屏界面（L6，不是文明面板页签，因为它需要全部屏幕空间），详细设计见 22 章。本章只规定按键行为与“时代 → 界面能力”对照（来源 01 §3.2，DR-27）：

| 时代 | 世界地图（M） | 小地图（N 切换） | 罗盘条 | 路点（B） | 图层 | 坐标显示 |
| --- | --- | --- | --- | --- | --- | --- |
| E0 | 记忆地图：去过的地方模糊显示，随时间褪色 | 无 | 无 | 无 | 无 | 无（F3 可用） |
| E1 | 手绘地图（需持有地图物品），只显示亲自走过的区域 | 无 | 无 | 8 个 | 无 | 无 |
| E2 | 加入地标与路标 | 无 | 无 | 32 个 | 无 | 无 |
| E3 | 道路测量后区域变精确 | 半径 64，只显示已测绘地区 | 无 | 32 个 | 道路 | 无 |
| E4 | 同上 | 半径 128 | 有 | 64 个 | 道路、领地 | 无 |
| E5 | 航海图：经纬网格、海岸线 | 同上 | 有 | 128 个 | + 洋流 | 经纬度（取整到 1°） |
| E6 | 三角测量：等高线 | 同上 | 有 | 128 个 | + 等高线、地质、铁路 | 经纬度（0.1°） |
| E7 | 远程聚落实时数据 | 同上 | 有 | 256 个 | + 天气 | 同上 |
| E8 | 航拍：大范围高精度 | 同上 + 雷达移动目标 | 有 | 256 个 | + 雷达 | 同上 |
| E9 | GIS：图层叠加、分析、路线规划 | 同上 | 有 | 512 个 | 全部 + 分析 | 同上 |
| E10 | 卫星图像，月球与火星地图 | 同上 | 有 | 不限 | 全部 | 精确方块坐标（`tcatlas:coordinates`） |

E1 路点数 8、E2 路点数 32 来自 01 §3.2；E4 起的路点上限为本章建议值，最终以 22 章为准。

按键行为：

| 按键 | 行为 |
| --- | --- |
| M | 打开/关闭世界地图；E0 时打开“记忆地图” |
| N | 循环小地图模式：关闭 → 北向上 → 随朝向旋转 → 关闭；E3 之前按 N 显示提示“尚无小地图（需要道路测量）” |
| B | 在准星指向的方块处（64 格内）或玩家当前位置添加路点，弹出命名框（名称、图标、颜色 + 形状、共享范围）；`Shift+B` 跳过命名框直接添加 |

### 5.7 外交（`tcnations:diplomacy`）

从文明面板“文明 › 外交”进入；快捷键 `key.tcnations.diplomacy` 默认**未绑定**（O 在 26.x 为原版“好友”键）。外壳规则：

1. 左栏：已知文明列表（旗帜、名称、关系值文字档位 + 图标，例如“友好 ▲”），未接触的文明不出现。
2. 中栏：所选文明的概况卡、关系历史折线（`TcChart`）、条约列表、边境地图片段（`TcMapSnippet`）。
3. 右栏：可用外交行动（按钮），不可用时灰色并显示原因。宣战、撕毁条约使用 `TcConfirmDialog` 的输入名称确认。
4. 具体关系值、条约类型与 AI 行为见 23 章。

### 5.8 机器界面模板（DR-20）

所有 Industry（及其他模组的机器类方块）界面使用统一模板 `TcMachineScreen`，尺寸 256×200（原版容器 176 宽的扩展），分区如下：

```text
┌──────────────────────────────── 256 ────────────────────────────────┐
│ [图标] 蒸汽机 #3        状态：运行中 ●     磨损：轻度（等级 1）      │ ← 标题栏
├───────────────┬─────────────────────────────┬───────────────────────┤
│ 示意图 72×72  │  输入/输出槽位               │ 数值面板              │
│ （齿轮按实际  │  [燃料][水]  →  [输出]       │ 转速 128 RPM          │
│  转速旋转，   │                              │ 输出 2,400 SU         │
│  液位实时）   │  进度 ▓▓▓▓▓░░░ 62%           │ 负载 78%              │
│               │                              │ 温度 139 °C           │
├───────────────┴─────────────────────────────┴───────────────────────┤
│ 维护：下次保养约 2.5 日  [执行维护]（需要：润滑油 ×1）                │ ← 维护栏
├─────────────────────────────────────────────────────────────────────┤
│                   玩家物品栏（原版 9×4）                             │
└─────────────────────────────────────────────────────────────────────┘
```

规则：

1. **界面镜像世界，不代替世界**：界面中的每个状态在世界里都有对应表现（转动、冒烟、异响、停转），反之亦然（UP-1）。界面里不出现世界中无法观察到的“魔法数字”，除非玩家拥有相应测量仪器或科技（例如温度要求掌握温度计相关科技，否则显示“很烫”“温热”等文字档位）。
2. 状态用“文字 + 形状”表达：运行中 ●、待机 ○、缺料 ◐、故障 ✖、过载 ▲。
3. 多方块机器由控制器方块打开界面；结构不完整时，界面显示缺失清单，并在世界中以幽灵方块（L1）标出缺失位置。
4. 机器界面不暂停单人游戏。
5. 数值单位遵循契约：机械 SU、电 J/W、热 HU（DA-10），格式见 8.4。

### 5.9 人物界面（`tcivilization:person_screen`）

1. 打开方式：对核心人物实体 `Shift + 右键`；或指令轮盘“查看”；或任何 `TcPersonChip`。普通 `右键` 保留给对话与快捷交互。
2. 布局：左侧 3D 人物预览（可拖动旋转）+ 姓名、年龄、文化；右侧为可折叠的“分区卡片”。
3. 分区由 `PersonSectionRegistry` 注册：

| 分区 | ID | 来源 | `order` | 显示条件 | 内容 |
| --- | --- | --- | --- | --- | --- |
| 身份 | `tcivilization:identity` | 核心（03） | 0 | 始终 | 姓名、年龄、文化、家庭成员（`TcPersonChip`） |
| 需求与日程 | `tcivilization:needs_schedule` | 核心（03） | 10 | 人物有需求组件 | 心情、需求满足度、当前日程 |
| 关系 | `tcivilization:relations` | 核心（03） | 20 | 始终 | 与检视者及其他人物的关系 |
| 职业与住所 | `tcsettlements:residence_and_job` | Settlements（15 §10.4） | 100 | 有 Settlements 居民组件 | 岗位、工作地点、住所与住址、工作效率 |
| 交易 | `tcexchange:trade` | Exchange（16） | 200 | 有 `MerchantComponent` | 货物、报价、对玩家的个人与文明信誉及最近原因 |
| 职位 | `tccommons:office` | Commons（19） | 250 | 有 `OfficeComponent` | 所任职位 |
| 归属 | `tcnations:allegiance` | Nations（23） | 300 | AI 文明人物 | 所属文明、关系档位、军籍 |
| 技能与师承 | `tcskills:skills_and_mentors` | Skills（05 §10.8） | 400 | 被检视者是玩家，或是有 Settlements 职业组件的 NPC | 见 05 §10.8 |
| 远征 | — | Frontiers（20 章未注册；可选） | 500 | 人物是远征队员 | 远征角色、补给状态 |

4. 分区按 `order` 升序排列；核心占 0-99。新增分区取未占用的 50 的倍数并在本表登记。

### 5.10 世界内标注（L1）

| 标注 | 来源 | 何时显示 | 渲染要求 |
| --- | --- | --- | --- |
| 路点信标光柱 | Atlas | 导航中，或距离 ≤ 256 格 | 半透明光柱 + 名称牌，远处缩小为图标 |
| 领地边界 | Commons | 手持领地工具，或在治理页签点击“显示边界” | 地面贴合的虚线，颜色 + 线型区分归属 |
| 施工蓝图 | Settlements | 蓝图存在且距离 ≤ 64 格 | 半透明幽灵方块，已完成部分实体化 |
| 观察描边 | Discovery | 观察模式中 | 线框描边（5.4） |
| 路线预览 | Networks | 规划线路时 | 地面路径线 + 坡度警告标记 |
| 机器连接 | Industry | 手持扳手 | 输入输出方向箭头、传动轴连接线 |

统一规则：只用线框、半透明与名称牌三种渲染类型，均需在 Complementary Reimagined 下验证可见；最大绘制距离 128 格（信标 256）；任意时刻同类标注 ≤ 256 个，超出时只画最近的。

---

## 6. 按键与输入

### 6.1 全局按键权威表

所有按键在 `RegisterKeyMappingsEvent`（MOD 总线，仅客户端）中注册，分类统一为 `key.categories.tc`（显示名“文明与科技 / Civilization & Technology”）。ID 格式 `key.<modid>.<action>`。下表是**唯一权威**，README §6 是其摘要；各模组章节只能引用，不得另行分配默认键。

| 按键 ID | 默认 | 修饰键 | 冲突上下文 | 行为 | 注册者 | 条件 |
| --- | --- | --- | --- | --- | --- | --- |
| `key.tcatlas.world_map` | `M` | 无 | IN_GAME | 打开或关闭全屏世界地图（不暂停，见 2.2） | tcatlas | 总是 |
| `key.tcatlas.minimap_mode` | `N` | 无 | IN_GAME | 小地图：关闭 → 北向上 → 随朝向旋转 → 关闭 | tcatlas | 总是 |
| `key.tcatlas.add_waypoint` | `B` | 无 | IN_GAME | 在准星指向处（≤ 64 格）或脚下添加路点并弹出命名框 | tcatlas | 总是 |
| `key.tcatlas.add_waypoint_quick` | `B` | `SHIFT` | IN_GAME | 同上，跳过命名，使用自动名（“路点 12”） | tcatlas | 总是 |
| `key.tcatlas.hud_level` | `H` | 无 | IN_GAME | HUD 级别：极简 → 标准 → 扩展 → 极简 | tcatlas | 总是 |
| `key.tcivilization.hud_level` | `H` | 无 | IN_GAME | 同上 | tcivilization | **仅在未安装 Atlas 时注册** |
| `key.tcatlas.layout_editor` | `H` | `CONTROL` | IN_GAME | 打开 HUD 布局编辑器（3.6） | tcatlas | 总是 |
| `key.tcivilization.civ_panel` | `J` | 无 | IN_GAME | 打开文明面板，回到上次的页签 | tcivilization | 总是 |
| `key.tcivilization.command_wheel` | `Z` | 无 | IN_GAME | 按住显示指令轮盘，松开执行（5.3） | tcivilization | 总是 |
| `key.tctechnology.tech_tree` | `U` | 无 | IN_GAME | 打开文明面板的科技页签（宽模式） | tctechnology | 总是 |
| `key.tcskills.skills` | 未绑定；无核心（S0）时为 `J` | — | IN_GAME | 打开文明面板的技能页签；S0 下打开独立技能界面 | tcskills | 总是（S0 例外见 05 §10.6 第 4 条：核心缺席时 `J` 无人使用） |
| `key.tcnations.diplomacy` | 未绑定 | — | IN_GAME | 打开文明面板的外交页签 | tcnations | 总是 |
| `key.tcdiscovery.observe` | `V` | 无 | IN_GAME | 切换观察模式（5.4） | tcdiscovery | 总是 |
| `key.tcdiscovery.journal` | `Y` | 无 | IN_GAME | 打开日志（5.5） | tcdiscovery | 总是 |
| `key.tcechoes.journal` | `Y` | 无 | IN_GAME | 打开传说日志（Echoes 独立版本） | tcechoes | **仅在未安装 Discovery 时注册** |
| `key.tcexchange.open_market` | 未绑定 | — | IN_GAME | 打开文明面板的市集页签（入口：J › 市集） | tcexchange | 总是 |
| `key.tcindustry.network_overlay` | 未绑定 | — | IN_GAME | 不持扳手时开关网络连接标注（入口：轮盘、生产页签、机器界面“显示网络”，13 章） | tcindustry | 总是 |
| `key.tcatlas.minimap_zoom_in` / `key.tcatlas.minimap_zoom_out` | 未绑定 | — | IN_GAME | 小地图缩放，E9 起（入口：地图设置） | tcatlas | 总是 |
| `key.tcatlas.ping` | 未绑定 | — | IN_GAME | 在准星处放置 60 秒临时标记（入口：轮盘“标记此处”） | tcatlas | 总是 |
| `key.tcfrontiers.reload` | 未绑定 | — | IN_GAME | 弹匣未空时提前换弹（默认：无弹时按住使用键装填，20 章） | tcfrontiers | 总是 |
| `key.tcnetworks.horn` | 未绑定 | — | 载具 | 鸣笛；绑定后跳跃键不再鸣笛（默认：驾驶时跳跃键，14 章） | tcnetworks | 总是 |
| `key.tcnetworks.reverser` | 未绑定 | — | 载具 | 停稳时立即切换行车方向（入口：载具菜单 E） | tcnetworks | 总是 |
| `key.tcorbit.toggle_rcs_mode` | 未绑定 | — | 载具 | 航天器 RCS 平移/旋转切换（默认：驾驶时疾跑键，24 章） | tcorbit | 总是 |
| `key.tcorbit.cruise_confirm` | 未绑定 | — | 载具 | 确认巡航压缩（入口：轮盘对所乘航天器“确认巡航”） | tcorbit | 总是 |
| `key.tcorbit.vehicle_headlights` | 未绑定 | — | 载具 | 车灯 自动 → 开 → 关（默认自动；入口：轮盘对所乘车辆“车灯”） | tcorbit | 总是 |

规则：

1. **条件注册**：“仅在 X 未安装时注册”在 `RegisterKeyMappingsEvent` 中用 `ModList.get().isLoaded("x")` 判断。同一默认键同一时刻只能有一个 TC 按键生效。
2. **未绑定的入口必须可达**：每个默认未绑定的按键都必须有不依赖按键的入口（面板页签、轮盘、物品或载具交互、驾驶时复用的原版键；见上表“行为”列括注）。技能、外交还必须同时能从文明面板页签和轮盘的“个人/文明”分组到达，并在首次解锁时用一条信息通知提示：“可在 选项 › 控制 › 文明与科技 中为此功能设置快捷键”。
3. **不新增默认键**：新功能先挂在文明面板、轮盘或物品交互上。确需新键时须在本表登记，并通过 6.3 的冲突检查。
4. **长按与切换**：所有“按住”类操作（目前只有轮盘）都提供 `hold | toggle` 配置（11.4）。
5. **驾驶中复用原版键**：乘坐 TC 载具时，载具模组可以把原版移动类按键解释为载具动作（例如跳跃 = 鸣笛、疾跑 = 手刹或 RCS 模式，见 14 章驾驶键表与 24 章交互表）。这不是重绑原版键（UP-3）：键位仍由原版绑定决定，下车后恢复原义。对应的可选 TC 键一旦绑定，原版键的该驾驶含义即停用。
6. **分类唯一**：所有 TC 按键（包括载具键）都归入 `key.categories.tc`，各模组不得另建分类。

### 6.2 冲突上下文与修饰键

| 上下文 | NeoForge 类型 | 何时生效 | 用于 |
| --- | --- | --- | --- |
| IN_GAME | `KeyConflictContext.IN_GAME` | 世界中无界面打开 | 上表除载具键外的全局键 |
| 载具 | 各载具模组自定义 `IKeyConflictContext`（14 章 `RIDING_TC_VEHICLE`；24 章同法） | 世界中无界面打开，且玩家正乘坐本模组载具 | 上表“载具”行；`conflicts()` 与 IN_GAME 及其他载具上下文互判冲突（玩家同一时刻只乘一辆载具，但默认值仍须避开 6.3 已占用键） |
| GUI | `KeyConflictContext.GUI` | 有界面打开 | 不注册全局 GUI 键；界面内按键由界面自己处理（6.4） |
| UNIVERSAL | `KeyConflictContext.UNIVERSAL` | 任何时候 | 不使用。Esc、F1 等由原版处理 |

1. 修饰键使用 NeoForge `KeyModifier`（`SHIFT`、`CONTROL`、`ALT`）。`B` 与 `Shift + B`、`H` 与 `Ctrl + H` 依靠修饰键区分；不带修饰键的绑定在修饰键按下时**不触发**（NeoForge `KeyModifier.NONE` 的默认行为，在实现中需验证；若不满足，则在处理器中显式检查 `Screen.hasShiftDown()` 等）。
2. 疾跑键是左 `Ctrl`。`Ctrl + H` 在疾跑时按 H 也会触发布局编辑器。因此布局编辑器打开时**不暂停**，并且再按 `Ctrl + H` 或 Esc 立即关闭，误触成本很低。
3. 按键处理统一在 `ClientTickEvent.Post` 中用 `consumeClick()` 读取，不在渲染回调中读取输入。

### 6.3 被占用的键与冲突检测

**本整合包实例的已占用键**（来源：`F:\Games\MC\.minecraft\versions\Technology_and_Civalization\options.txt`，2026-11-04 读取）：

| 键 | 占用者 | 绑定 ID |
| --- | --- | --- |
| `W A S D`、空格、左 Shift、左 Ctrl | 原版移动 | `key.forward` 等 |
| `E`、`Q`、`F`、`T`、`/`、`Tab` | 原版物品栏、丢弃、副手、聊天、命令、玩家列表 | — |
| `G` | 原版**快捷操作**（1.21.6+） | `key.quickActions` |
| `O` | 原版**好友**（26.x） | `key.friends` |
| `P` | 原版社交互动 | `key.socialInteractions` |
| `L` | 原版进度 | `key.advancements` |
| `C`、`X` | 原版快捷栏保存、加载 | `key.saveToolbarActivator`、`key.loadToolbarActivator` |
| `1`-`9`、`F1`-`F3`、`F5`、`F11` | 原版快捷栏、界面、截图、调试、视角、全屏 | — |
| `R` | **Iris 重载光影** | `iris.keybind.reload` |
| `K` | **Iris 开关光影** | `iris.keybind.toggleShaders` |
| `I` | **Iris 光影包选择** | `iris.keybind.shaderPackSelection` |

`F3 + 字母` 组合是调试键，与单独按字母不冲突。剩余可用的单字母键是 `B H J M N U V Y Z`，已全部分配。今后需要新键时，只能使用修饰键组合或保持未绑定。

**启动冲突自检**（tcivilization 客户端，`FMLClientSetupEvent` 之后的第一帧）：

1. 遍历 `Minecraft.getInstance().options.keyMappings`，找出所有 `key.categories.tc` 中的绑定。
2. 对每个 TC 绑定，查找键码与修饰键相同、上下文重叠（`conflicts(other)`）的其他绑定。
3. 若有冲突：写一条 WARN 日志（列出双方 ID），并在进入世界后发送**一次** `warning` 通知：“按键 Z 同时绑定了 指令轮盘 与 某模组·某功能，可在 控制 设置中修改”。每个冲突对只提示一次，记录在 `config/tcivilization/seen_conflicts.json`。
4. 不自动改绑：玩家的选择优先。

**开发期自动测试** `KeyBindingContractTest`（GameTest 客户端测试或单元测试）：

- 断言上表每个默认键都不与原版、Iris 和 Sodium 的默认键冲突。
- 断言条件注册的键在两种安装组合下各只出现一个。
- 断言所有 TC 键 ID 都有 zh_cn 与 en_us 翻译。

**第三方地图模组**：Xaero's Minimap、JourneyMap、FTB Chunks 等与 Atlas 功能重叠，整合包不包含它们。玩家自行加入时：

- Atlas 检测到它们后，首次进入世界时提示一次，并将 `tcatlas-client.toml [compat] external_map` 设为 `detected`。
- 此时 Atlas 小地图默认关闭，世界地图与路点保留，按键不变（冲突由 6.3 第 3 步提示）。
- 不提供路点导入导出（开放问题 OQ-UX-04）。

### 6.4 界面内按键

所有 TC 界面（`TcScreen` 基类）统一处理以下按键。各模组界面可以增加按键，但不得改变这些含义。

| 按键 | 含义 | 备注 |
| --- | --- | --- |
| `Esc` | 关闭最上层面板；面板栈为空时关闭界面 | 面板栈规则见 2.3 |
| 打开该界面的同一个键（如 `J`） | 关闭界面 | 搜索框获得焦点时不响应 |
| `E` | 关闭界面 | 与原版容器一致；搜索框获得焦点时不响应 |
| `Tab` / `Shift + Tab` | 焦点在控件间前进 / 后退 | 焦点顺序 = 视觉阅读顺序（左→右，上→下） |
| `Ctrl + Tab` / `Ctrl + Shift + Tab` | 下一个 / 上一个页签 | 仅文明面板 |
| `Ctrl + F` | 聚焦当前页的搜索框 | 无搜索框时无效果 |
| 方向键 | 在列表、网格和科技树中移动选中项 | 科技树中沿依赖边移动（5.1） |
| `Enter` / 空格 | 激活选中项（按钮、列表行） | 与鼠标左键等价 |
| `Delete` | 删除选中项（路点、追踪项、草稿） | 永久删除需二次确认（16.4） |
| `Ctrl + 鼠标滚轮` | 缩放（科技树、地图、蓝图预览） | 普通滚轮用于滚动 |
| 鼠标中键拖拽 | 平移（科技树、地图） | 左键拖拽同效（无拖放操作时） |
| `Shift` 悬停 | 展开第二层提示（8.1） | — |
| `Ctrl` 悬停 | 展开第三层提示（8.1） | — |

输入法（IME）：搜索框和命名框获得焦点时，所有单字母快捷键失效，直到焦点离开（2.4）。

### 6.5 鼠标、手柄与触控

1. 鼠标为一等输入。所有操作在“仅鼠标”和“仅键盘”两种方式下都必须能完成（UX-AC-07）。
2. 轮盘的鼠标操作：从屏幕中心出发的方向决定选项，死区半径 12 GUI 像素（`[wheel] deadzone`）。键盘操作：数字键 `1`-`8` 直接选择第 n 个扇区（轮盘层 L5 捕获键盘，数字键不会切换快捷栏，2.1）；切换模式（`[wheel] mode = toggle`）下也可用方向键在扇区间移动，`Enter` 执行，`Esc` 取消。
3. 手柄：不在 1.0 范围。若玩家安装 Controlify，TC 界面依靠其“虚拟鼠标”工作；轮盘的右摇杆映射是开放问题 OQ-UX-05。
4. 触控：不支持。

---

## 7. 通知系统

### 7.1 严重级别

| 级别 | `Severity` | 图标形状 | 呈现 | 默认时长 | 声音 | 可打断 | 进入通知中心 | 典型用途 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 信息 | `INFO` | 圆点 | 右上角 toast | 5 s | 无（可选轻音 `tcivilization:ui.notify.info`） | 否 | 是 | 研究进度达到 50%、NPC 完成任务 |
| 成功 | `SUCCESS` | 对勾 | 右上角 toast，绿色强调条 | 6 s | `tcivilization:ui.notify.success` | 否 | 是 | 研究完成、建筑完工、订单交付 |
| 警告 | `WARNING` | 三角 | 右上角 toast，琥珀色强调条 | 8 s | `tcivilization:ui.notify.warning` | 否 | 是 | 粮食储备 < 3 日、机器维护到期、领地被侵入 |
| 危急 | `CRITICAL` | 八边形 | 顶部居中横幅 | 直到确认，最长 15 s | `tcivilization:ui.notify.critical` | **是**（只打断提示，不打断操作） | 是，置顶 | 聚落遭袭、饥荒、反应堆过热、宣战 |
| 仪式 | `CEREMONY` | 星 | L4 全屏覆盖（9） | 见 9（大型 7 s，小型 4 s） | `tcivilization:event.ceremony` + 时代音乐短句 | **是**（可延迟） | 是，带回看 | 时代晋升、奇观完工、首次接触 |

本表与 03 §5.10.5 一致；数值调整须同时修改两处。色彩只是辅助通道：每个级别的图标形状不同，在色盲模式下仍能区分（11.1）。

### 7.2 toast 呈现

1. 信息、成功、警告三级使用自定义 `TcToast implements Toast`，提交给原版 `ToastManager`。这样与原版进度、配方 toast 共用右上角队列，不会重叠。toast 位置不可配置；需要避开右上角的玩家可用 Atlas 布局编辑器移动其他控件。
2. 尺寸 160 × 32 GUI 像素（原版 toast 宽度）。左侧 16 × 16 图标，右侧最多两行文字：标题一行，正文一行，超长截断并加省略号。完整文字在通知中心查看。
3. 原版队列同时最多显示 5 个 toast。TC 自身同时最多占 3 个，多余的排队。排队超过 10 条时，最旧的信息级 toast 直接转入通知中心，不再显示。
4. **点击行为**：游戏中鼠标被捕获，toast 不能直接点击。通知带有深链接（4.8）时，toast 底部显示“J ›”提示；在 toast 显示期间或消失后 5 s 内按 `J`，文明面板会打开通知抽屉并高亮这一条，再点“前往 ›”跳转。打开任一 TC 界面（鼠标已释放）时，仍在显示的 toast 可以直接点击跳转。
5. 时长可用 `[notifications] duration_scale`（0.5-3.0）整体缩放。

### 7.3 危急横幅

1. 位置：顶部居中，在 Boss 血条之下，不覆盖准星和快捷栏。宽度为屏幕宽度的 60%，最大 320 GUI 像素；高度 28 GUI 像素。
2. 同一时刻只显示 1 条，其余排队，并在横幅右侧显示“+2”。
3. 关闭：按 `J` 打开通知抽屉即视为已读；或 15 s 后自动收起为 HUD 警报组件（`tcivilization:alerts`）中的常驻条目，直到问题解除（`AlertProvider` 报告解除）。
4. 横幅出现时播放一次声音，不循环。同一 `mergeKey` 的危急通知 60 s 内不重复响铃。

### 7.4 合并与限流

```java
public record Notification(
        Identifier category,       // 7.6 中注册的类别
        Severity severity,
        Component title,
        @Nullable Component body,
        @Nullable Identifier icon, // 纹理 ID；null 时使用类别图标
        @Nullable DeepLink link,   // 4.8；生产者须先调用 DeepLink.isResolvable
        String mergeKey,           // 合并键，空字符串表示不合并
        int ttlTicks               // 在通知中心的保留时长；0 = 默认 5 个游戏日
) {}
```

1. **合并**：同一玩家、同一 `mergeKey`、10 s 内的多条通知合并为一条，标题后加“×N”，正文使用最后一条。例：`tcsettlements:job_done/riverside` 把“阿青完成了伐木”“阿木完成了伐木”合并为“任务完成 ×2”。
2. **限流**（每个玩家）：

| 范围 | 上限 | 超出后 |
| --- | --- | --- |
| 单个模组的信息级 | 6 条 / 分钟 | 直接进入通知中心，不弹 toast |
| 全部信息级 + 成功级 | 12 条 / 分钟（服务器配置 `notify.info_per_minute`，03 §13.2） | 同上 |
| 警告级 | 6 条 / 分钟 | 合并为“N 条警告”一条 toast |
| 危急级 | 不限流，但同 `mergeKey` 60 s 内只响铃一次 | — |

3. 限流在**服务器端**执行（`TcNotifications` 内部），因此不会产生多余的网络流量。

### 7.5 战斗免打扰

1. 判定“战斗中”：玩家在过去 8 s 内受到或造成伤害，或附近 16 格内有以玩家为目标的敌对生物。客户端用本地数据判定，不发包。
2. 战斗中：信息级和成功级不弹 toast，只进入通知中心；警告级延迟到战斗结束后 2 s 再弹（若已过期则丢弃 toast，保留中心记录）；危急级照常显示；仪式延迟（9.3）。
3. 战斗结束时，如果期间积压了 ≥ 3 条，弹出一条汇总 toast：“战斗期间收到 5 条通知”。
4. 可在 `[notifications] combat_dnd = true` 中关闭此功能。

### 7.6 类别

类别通过 `TcNotifications.registerCategory(Identifier id, Component name, Identifier icon, Severity defaultMinimum)` 在 `FMLCommonSetupEvent` 中注册。玩家可以按类别静音（静音后仍进入通知中心，但不弹 toast；危急级不可静音）。

| 类别 ID | 注册者 | 内容 |
| --- | --- | --- |
| `tcivilization:system` | 核心 | 按键冲突、版本不匹配、配置错误 |
| `tcivilization:civilization` | 核心 | 成员加入或离开、文明更名 |
| `tctechnology:research` | 科技核心 | 研究完成、新科技可研究、时代晋升 |
| `tcskills:growth` | 技能 | 技能升级、天赋点获得 |
| `tcsettlements:settlement` | 聚落 | 人口、住房、职业、居民需求 |
| `tcindustry:production` | 工坊 | 机器故障、维护、产线停工 |
| `tcnetworks:logistics` | 通途 | 车辆抵达、线路阻塞、仓库满 |
| `tcexchange:market` | 市集 | 订单成交、价格异动、商队抵达 |
| `tccultivation:agriculture` | 田园 | 作物成熟、病害、储粮 |
| `tcecology:ecology` | 生息 | 物种迁徙、生态告警 |
| `tcwildlands:environment` | 山河 | 季节变化、灾害预警 |
| `tcatlas:cartography` | 舆图 | 新地区、测绘闭合、地图库扩充、首次 GPS 定位 |
| `tcdiscovery:discovery` | 求知 | 新发现、实验结果 |
| `tcechoes:legend` | 遗响 | 传说线索 |
| `tccommons:governance` | 契约 | 投票、公共项目 |
| `tcnations:diplomacy` | 列国 | 外交提议、边境事件、宣战 |
| `tcfrontiers:expedition` | 远征 | 远征补给、远征队状态 |
| `tcbeyond:anomaly` | 异界 | 异常事件 |
| `tcorbit:space` | 轨道 | 发射、轨道事件、生命维持 |

### 7.7 通知中心

1. 位置：文明面板右侧的抽屉，点击面板右上角的铃铛图标展开。未读数显示在铃铛上，最多显示“99+”。
2. 容量：客户端保留最近 200 条（会话内存）。服务器为每个玩家持久化最近 50 条警告级及以上的通知（`tc/notifications/<uuid>.dat`），登录时同步，因此离线期间发生的危急事件不会丢失。
3. 每条通知显示图标、标题、正文、相对时间（“3 分钟前”/“昨天”，游戏日与现实时间见 8.4）。有深链接的显示“前往 ›”。
4. 筛选：全部 / 未读 / 按类别 / 按级别。可“全部标为已读”。
5. 离线摘要：玩家登录时，如果离线期间有 ≥ 1 条警告级及以上的通知，进入世界 5 s 后弹出一条信息级 toast：“离线期间：2 条危急、5 条警告”，按 `J` 查看。

### 7.8 API

```java
public final class TcNotifications {
    /** 发给单个玩家；在服务器线程调用。 */
    public static void send(ServerPlayer player, Notification n);
    /** 发给文明的所有在线成员；离线成员在登录时收到（仅警告级及以上）。 */
    public static void send(CivId civ, Notification n);
    /** 发给某个区域内的所有玩家（灾害预警）。 */
    public static void sendToRegion(ServerLevel level, RegionPos region, Notification n);
    public static void registerCategory(Identifier id, Component name, Identifier icon, Severity defaultMinimum);
}
```

- 网络：`tcivilization:notify`（S2C），载荷为 `Notification` 的流编解码（`StreamCodec`）。`Component` 使用 `ComponentSerialization.STREAM_CODEC`，单条 ≤ 2 KB。
- 生产者必须用 `Component.translatable` 而不是拼接好的字符串（12.3）。
- 客户端事件 `TcNotificationReceivedEvent`（可取消），供 Atlas 等模组在地图上同步标注。

---

## 8. 提示框、信息分层与格式

### 8.1 三层提示

物品、方块、科技节点、HUD 控件、地图要素的悬停提示统一分三层。各模组通过 `TcTooltipProvider`（客户端，按物品/方块/对象类型注册）提供内容，外壳负责布局和按键提示。

| 层 | 触发 | 内容 | 行数上限 |
| --- | --- | --- | --- |
| 基础 | 悬停 | 名称、时代徽章（8.2）、一句话用途、关键状态（如“耐久 80%”“未成熟”） | 4 行 |
| 详细 | 悬停 + `Shift` | 完整属性表、配方去向（“可用于 3 种配方”）、所属科技、联动说明（“安装 Cultivation 后可育种”只在**已安装**时显示） | 12 行 |
| 技术 | 悬停 + `Ctrl` | 公式与原始数值（“产出 = 基础 40 × 土壤 0.85 × 季节 1.1”）、标签、来源模组；`F3 + H` 开启时额外显示注册 ID | 16 行 |

1. 基础层最后一行用灰色小字提示“按住 Shift 查看详情”，只在存在详细层时显示，且同一会话中每个物品类型最多提示 3 次（之后玩家显然已经知道）。
2. 提示框宽度上限 200 GUI 像素，自动换行；中文在字符边界换行，英文在单词边界换行。
3. 数值统一右对齐成“标签 …… 数值”两列（使用 `TcStatRow`），不在一行里堆叠多个数值。
4. 原版物品（如铁锭）同样会获得基础层的时代徽章和用途行，前提是它被某个 TC 模组的系统引用（30 章原版覆盖表）。

### 8.2 时代徽章

1. 物品、方块、科技、建筑在提示框右上角显示时代徽章：12×8 圆角矩形，内写 `E0`-`E10`，底色取 32 章该时代皮肤的主色。
2. 徽章表示“**最早在哪个时代可获得**”，不是“当前是否可用”。当前不可用时，名称后加灰色锁形图标和原因（“需要科技：青铜冶炼”）。
3. 未安装 Technology Core 时（`eraIndex = -1`，03 §5.1.1）不显示徽章。

### 8.3 知识门控显示

Discovery（17 章）定义“已知属性”：玩家没有观察或研究过的属性显示为“？”。

1. 查询接口 `KnowledgeView.isKnown(Player, Identifier subject, Identifier property)` 由核心定义；Discovery 安装时由它实现。**未安装 Discovery 时一律返回 true**，所有属性直接可见（独立可玩原则）。
2. 未知属性显示为 `？？？`，颜色为灰色斜体，悬停提示“通过观察模式（V）或实验了解此属性”。
3. 未知的不是“隐藏”：属性行仍然存在，让玩家知道“这里有值得研究的东西”。完全未发现的对象（如未接触的文明）则整个不出现。
4. 部分已知用区间表示：“硬度 约 3-5”。区间宽度由 Discovery 的知识等级决定。
5. 多人中知识按**文明**共享（同一文明成员看到同样的 `？`），由 17 章最终规定。

### 8.4 数字、单位与时间格式

所有格式化通过 `TcFormat` 工具类完成，禁止各模组自行 `String.format`。

| 类型 | 规则 | 示例（zh_cn） | 示例（en_us） |
| --- | --- | --- | --- |
| 整数 | 千位分隔；≥ 100,000 时可用紧凑写法（仅在 HUD 与列表中） | `12,480`；紧凑 `12.5 万` | `12,480`；`12.5K` |
| 百分比 | 0-1 的比例显示为整数百分比；< 1% 时保留一位小数 | `62%`、`0.4%` | 同左 |
| 距离 | 单位为格；≥ 1,000 格改写千格（kb）并保留 1 位小数；米只用于 Atlas 比例尺与地名尺度（1 格 ≈ 244.6 m，README `HORIZONTAL_SCALE`） | `340 格`、`12.6 千格` | `340 b`、`12.6 kb` |
| 速度 | 移动速度一律为格/秒（b/s，按实时秒计）；风速、流速、Δv 等物理量保留 m/s | `4.3 格/秒`、`64 格/秒` | `4.3 b/s`、`64 b/s` |
| 质量 | kg；≥ 1000 kg 用 t | `850 kg`、`2.4 t` | 同左 |
| 液体 | 1 mB = 1 L；≥ 1000 L 用 m³ | `750 L`、`12 m³` | 同左 |
| 温度 | 摄氏度，整数 | `139 °C`、`-12 °C` | 同左（en_us 可配置为 °F） |
| 机械动力 | `SU`（13 章权威） | `2,400 SU` | 同左 |
| 电能 | 功率 W/kW/MW/GW；能量 J/kJ/MJ/GJ（13 章） | `1.2 MW` | 同左 |
| 热量 | `HU`（13 章） | `300 HU` | 同左 |
| 价值 | 地方货币由 16 章定义名称与符号；跨文明记账单位 `VU`（DA-11） | `34 贝`、`120 VU` | `34 shells`、`120 VU` |
| 日期 | 03 §5.3 日历；E0 只显示季节与日 | `第 3 年 · 夏 · 第 7 日` | `Year 3 · Summer · Day 7` |
| 时刻 | 游戏时刻，24 小时制，1 游戏时 = 1000 tick | `14:30` | `14:30` |
| 持续时间（游戏） | 世界中发生的过程一律用游戏时间：日、时 | `2 日 3 时`、`约 5 时` | `2d 3h` |
| 持续时间（现实） | 仅用于现实时间计量（冷却、服务器事件、离线时长） | `实时 3 分 20 秒` | `3m 20s (real)` |
| 相对时间 | 通知中心使用游戏时间（“今天 14:00”“3 日前”），悬停显示现实时间 | `3 日前` | `3 days ago` |
| 坐标 | 按时代与 Atlas 精度（5.6）；E10 前不显示精确方块坐标 | `北纬 23.4°，东经 112.0°` | `23.4°N, 112.0°E` |

规则：

1. 估计值统一用“约”/“~”前缀，并按精度取整（“约半日路程”）。不要显示虚假的精度。
2. 负值使用真正的减号或连字符，并配合颜色**和**箭头（▼）表示下降，不能只依赖颜色。
3. 单位与数值之间有空格（`139 °C`），百分号除外（`62%`）。
4. `en_us` 下温度可通过 `[format] temperature_unit = celsius|fahrenheit` 切换；距离不提供英制（方块就是米）。

### 8.5 文本与排版

1. 字体：只使用原版字体（含 Unicode 字形）。不打包自定义字体，以免增大体积并破坏中文字形一致性。时代皮肤只改变颜色与边框，不改变字体（32 §5.1）。
2. 最小字号：原版 1 倍字号（9 GUI 像素行高），不缩小字体来塞信息。放不下时截断并提供悬停全文。
3. 截断：超出宽度时用“…”截断，悬停显示全文。名称类文本优先保留开头；文件路径类文本保留结尾。
4. 中文标点使用全角，中英文之间留空格（“安装 Atlas 后可用”）。
5. 不使用全大写英文强调；强调用颜色 + 粗体（`ChatFormatting.BOLD`）。

---

## 9. 仪式与演出

### 9.1 仪式类型

| 仪式 | 所有者 | 级别 | 总时长 | 触发对象 |
| --- | --- | --- | --- | --- |
| 时代晋升 | tctechnology | 大型 | 7 s | 文明全体在线成员 |
| 奇观完工 | tcsettlements | 大型 | 6 s | 文明全体在线成员，以及 128 格内的所有玩家 |
| 首次接触 | tcnations | 小型 | 4 s | 接触的玩家所在文明的成员 |
| 聚落建立 | tcsettlements | 小型 | 4 s | 建立者 |
| 传说揭晓 | tcechoes | 小型 | 4 s | 揭晓者 |
| 首次发射入轨 | tcorbit | 大型 | 7 s | 文明全体在线成员 |

核心提供通用的 `CeremonyOverlay`（L4 层）与 `CeremonySpec` 数据结构；各所有者模组只提供内容（标题、副标题、图标、解锁摘要、音乐短句 ID）。

```java
public record CeremonySpec(
        Identifier id,              // 例如 tctechnology:era/bronze
        CeremonyScale scale,        // MAJOR | MINOR
        Component title,            // “青铜时代”
        Component subtitle,         // “文明 · 河湾部落 · 第 4 年 春”
        Identifier emblem,          // 64×64 纹理
        List<UnlockEntry> unlocks,  // ≤ 8 条：图标 + 名称 + 深链接
        @Nullable Identifier music, // 音乐短句（32 §8）
        @Nullable Identifier skinAfter // 演出结束后切换到的皮肤；null 不切换
) {}
```

网络：服务器通过 `tcivilization:ceremony`（S2C）发送 `CeremonySpec`。时代晋升另外发送 `tctechnology:era_advanced`（S2C），供其他模组的客户端刷新与时代相关的缓存。

### 9.2 时代晋升时间线（大型）

| 时间 | 画面 | 声音 | 输入 |
| --- | --- | --- | --- |
| 0.0-0.5 s | 屏幕边缘渐暗（暗角 0 → 35%），HUD 淡出 | 环境音与音乐压低 12 dB | 正常 |
| 0.5-2.0 s | 中央出现时代徽记（64×64，缩放 0.8 → 1.0），下方标题与副标题逐字显示 | 时代音乐短句开始 | 正常 |
| 2.0-5.0 s | 徽记上移，下方出现解锁摘要：最多 8 个图标横排，每个间隔 0.15 s 依次出现 | 每个图标出现时轻音 | **3.0 s 起可跳过** |
| 5.0-6.3 s | 皮肤过渡：UI 色板从旧皮肤插值到新皮肤（只影响 HUD 与 toast 样式） | 音乐短句收尾 | 可跳过 |
| 6.3-7.0 s | 全部淡出，HUD 恢复 | 音量恢复 | — |
| 7.0 s | 发送一条成功级 toast：“已进入青铜时代 · 按 J 查看解锁” | — | — |

1. 演出是**非模态**覆盖层，不是 `Screen`：玩家始终可以移动、攻击。3.0 s 后玩家的任何移动、攻击或打开界面都会使演出在 0.3 s 内淡出（视为跳过）。
2. 演出不移动镜头，不锁定视角。
3. 解锁摘要中的每一项都可在之后的“文明面板 › 编年史”中回看（`tcivilization:chronicle`），编年史也提供“重播演出”按钮。

### 9.3 延迟条件

演出到达客户端时，满足任一条件就进入延迟队列，并每秒复查：

1. 战斗中（7.5）。
2. 有 L6/L7 界面打开（机器界面、文明面板、地图除外：文明面板与地图打开时，演出在面板关闭后立即播放）。
3. 坠落中（`fallDistance > 3`）、在水下且氧气 < 50%、乘坐速度 > 10 b/s 的载具、或正在滑翔（鞘翅）。
4. 处于加载界面或死亡界面。

延迟超过 5 分钟后，演出降级为一张静态卡片（3 s，无暗角、无皮肤动画），在下一次安全时刻显示。多个延迟中的演出合并为一个序列，每个之间间隔 1 s，大型优先。

### 9.4 小型仪式

小型仪式没有暗角和皮肤过渡：在屏幕上方三分之一处显示徽记 + 标题 + 副标题，持续 4 s，3 s 前不可跳过（但同样是非模态的），然后发送一条成功级 toast。

### 9.5 降低动态效果

`[accessibility] reduced_motion = true` 时：

- 所有演出改为静态卡片：直接显示最终画面，3 s 后淡出（淡出改为 0.2 s 的直接透明度切换）。
- 不使用缩放、位移、逐字显示和暗角动画。
- 皮肤直接切换，不插值。
- 声音照常（可在声音设置中单独关闭 `tcivilization:ui.ceremony` 类别）。

### 9.6 多人

1. 演出完全在客户端播放，不暂停服务器，也不影响其他玩家。
2. 同一事件只为每个玩家播放一次：客户端在 `config/tcivilization/seen_ceremonies.json`（按世界 + 服务器地址分区）中记录已播放的 `CeremonySpec.id + civId`。
3. 离线成员在下次登录时，如果事件发生在 3 个游戏日之内，就补播静态卡片；更早的事件只出现在编年史中。

---

## 10. 时代皮肤

### 10.1 解析顺序

1. 若 `tcatlas-client.toml [skin] skin_lock` 不是 `auto`，并且该皮肤已解锁（对应时代 ≤ 文明当前时代），就使用它。
2. 否则若 Atlas 已安装且 `eraIndex ≥ 0`，就使用 `tcatlas:skin/<era>`（例如 `tcatlas:skin/bronze`，ID 列表见 32 §5.1）。
3. 否则使用核心内置的 `tcivilization:skin/neutral`：浅灰白底、深灰字、1 GUI 像素深灰描边、无纹理装饰（取值见 10.2 规则 5），满足全部对比度要求。
4. 资源包可以用同名 JSON 覆盖任何皮肤。

### 10.2 皮肤 JSON

路径：皮肤 ID `<namespace>:skin/<name>` 对应 `assets/<namespace>/tc_skins/<name>.json`，例如 `tcatlas:skin/bronze` 对应 `assets/tcatlas/tc_skins/bronze.json`。由客户端资源重载监听器加载。皮肤 ID 是客户端资源 ID，不属于注册表类型，不列入附录 A。

```json
{
  "parent": "tcivilization:skin/neutral",
  "palette": {
    "background": "#E8D9B8",
    "panel": "#D9C59A",
    "text": "#2B1D0E",
    "text_muted": "#6B5A3E",
    "accent": "#8C5A2B",
    "positive": "#3D7A3A",
    "warning": "#B7791F",
    "critical": "#9B2C2C",
    "icon": "#3A2A16"
  },
  "frame": {
    "texture": "tcatlas:textures/gui/skin/bronze/frame.png",
    "nine_slice": { "left": 6, "right": 6, "top": 6, "bottom": 6 },
    "texture_size": [64, 64]
  },
  "widgets": {
    "button": "tcatlas:textures/gui/skin/bronze/button.png",
    "tab": "tcatlas:textures/gui/skin/bronze/tab.png",
    "scrollbar": "tcatlas:textures/gui/skin/bronze/scrollbar.png",
    "toast": "tcatlas:textures/gui/skin/bronze/toast.png"
  },
  "sounds": {
    "click": "tcatlas:ui.bronze.click",
    "page": "tcatlas:ui.bronze.page",
    "open": "tcatlas:ui.bronze.open"
  },
  "effects": { "scanlines": false, "paper_grain": 0.4 }
}
```

规则：

1. `parent` 继承：缺失的字段从父皮肤取，最终回落到 `tcivilization:skin/neutral`。
2. 加载时进行**对比度检查**：`text` 与 `background`、`text` 与 `panel` 的对比度必须 ≥ 4.5:1，`warning` 和 `critical` 与 `panel` 的对比度必须 ≥ 3:1。不合格时写 WARN 日志，并且在 `high_contrast` 模式下强制使用中性皮肤的对应颜色。
3. `effects` 中的动态效果（扫描线、颗粒）在 `reduced_motion` 或 `high_contrast` 下自动关闭。
4. 皮肤**不改变**任何尺寸、布局或字体（UP-7）。九宫格边框的内边距固定为 6 GUI 像素，皮肤纹理必须适配这个尺寸。
5. **中性皮肤** `tcivilization:skin/neutral`（核心内置，`assets/tcivilization/tc_skins/neutral.json`，无 `parent`）的取值：`background` #E6E6E3、`panel` #D4D4D0、`text` #1F1F1F、`text_muted` #555555、`accent` #3B5B7A、`positive` #2E6B30、`warning` #8A5A00、`critical` #A12622、`icon` #2A2A2A；边框为 1 GUI 像素 #4A4A4A 实线（九宫格纹理内边距仍为 6），无底纹、无效果、使用原版 UI 声音。对比度：`text` 对 `background` 约 13:1、对 `panel` 约 11:1；`warning` 对 `panel` 约 4.0:1，`critical` 对 `panel` 约 5.0:1。缺少 Atlas 或 Technology 时的回退界面（17）和不依赖核心的独立界面（例如 05 的 `VanillaUiKit`）都复制这组取值。

### 10.3 色盲调色板

`colorblind_mode` 不为 `off` 时，`positive`、`warning`、`critical` 以及地图分类色由核心替换为对应的安全调色板，替换优先于皮肤（11.1）。

---

## 11. 无障碍

### 11.1 颜色

1. `[accessibility] colorblind_mode = off | deuteranopia | protanopia | tritanopia | monochrome`（03 §13.3）。`monochrome` 面向全色盲与灰度显示：语义色退化为明度差 ≥ 40% 的灰阶，并强制启用图标与纹理通道。
2. 每种模式有一套固定的语义色替换（正面、警告、危急、中性，以及 8 种分类色）。分类色采用 Okabe-Ito 调色板为基础。
3. **任何信息都不只靠颜色传达**：状态同时使用图标形状；图表系列同时使用线型（实线、虚线、点线）或标记形状；地图分类同时使用纹理或图标。这是验收项 UX-AC-05。
4. `high_contrast = true`：背景不透明度提升到 95%，文字纯白或纯黑，边框 2 像素，关闭所有纹理底纹。

### 11.2 动态

1. `reduced_motion = true`：关闭演出动画（9.5）、HUD 数值滚动动画、轮盘展开动画、地图惯性平移、扫描线与闪烁效果。
2. 全局禁止频率高于 3 Hz 的闪烁；危急横幅的“脉动”最高 1 Hz，振幅不超过 20% 透明度，在 `reduced_motion` 下为静态。
3. 屏幕震动：TC 模组不使用屏幕震动。

### 11.3 文本与缩放

1. HUD 缩放 `[hud] scale = 0.5-2.0`，在原版 GUI 缩放基础上再相乘。面板不单独缩放，跟随原版 GUI 缩放。
2. 所有界面必须在 GUI 缩放 1-4、窗口 854×480 到 3840×2160 下可用：面板最小尺寸 320×240 GUI 像素，放不下时显示滚动条而不是裁切。
3. 原版“文字背景不透明度”设置对 TC 提示框和 toast 同样生效。

### 11.4 输入

1. 所有按住操作提供切换模式（轮盘 `[wheel] mode`）。
2. 所有操作都可只用键盘完成（6.4），也可只用鼠标完成。
3. 时间敏感的界面操作（如限时外交提议）至少给出 60 s 游戏时间，并在 HUD 警报中保留入口。
4. 双击和长按不作为唯一触发方式。

### 11.5 旁白与字幕

1. 所有自定义控件实现 `NarratableEntry`，支持原版旁白（Narrator）：焦点移动时朗读控件名称、状态和值，例如“科技 青铜冶炼，可研究，需要 120 知识点”。
2. 通知在到达时朗读标题（受原版旁白设置控制）。
3. 所有带有游戏意义的声音（机器故障、警报、动物叫声中的习性线索）都注册字幕（`subtitles.<modid>.<sound>`），见 32 §8。

---

## 12. 本地化

### 12.1 语言范围

| 语言 | 状态 | 说明 |
| --- | --- | --- |
| `zh_cn` | **主语言**，1.0 必须 100% | 设计文档与文本以中文为准 |
| `en_us` | 1.0 必须 100% | 原版回退语言，缺失时显示英文而不是键名 |
| `zh_tw`、`zh_hk` | 1.0 后，社区或自动转换后人工校对 | — |
| 其他 | 欢迎社区贡献 | — |

### 12.2 键命名

遵循原版风格 `<类型>.<modid>.<路径>`，路径用点分隔、小写下划线。

| 类型 | 格式 | 示例 |
| --- | --- | --- |
| 物品/方块/实体 | 原版自动：`item.<modid>.<id>` 等 | `item.tcindustry.bronze_gear` |
| 按键 | `key.<modid>.<action>` | `key.tcivilization.command_wheel` |
| 按键分类 | `key.categories.tc` | — |
| 界面文字 | `gui.<modid>.<screen>.<element>` | `gui.tcivilization.panel.tab.overview` |
| HUD 控件 | `hud.<modid>.<widget>`（标题）/ `.desc`（布局编辑器说明）/ `.<element>` | `hud.tcatlas.minimap`、`hud.tcatlas.minimap.desc` |
| 面板页签 | `civtab.<modid>.<tab>` / `.desc` | `civtab.tctechnology.research` |
| 轮盘选项 | `wheel.<modid>.<option>` | `wheel.tcivilization.open_panel` |
| 提示框 | `tooltip.<modid>.<subject>.<line>` | `tooltip.tccultivation.wheat_seed.yield` |
| 通知 | `notice.<modid>.<id>.title` / `.body` | `notice.tctechnology.research_done.title` |
| 科技 | `tech.<modid>.<era>.<id>` / `.desc` | `tech.tctechnology.bronze.smelting` |
| 技能 | `skill.tcskills.<id>` / `.desc` | `skill.tcskills.mining` |
| 天赋 | `talent.tcskills.<skill>.<id>` / `.desc` | `talent.tcskills.mining.vein_sense` |
| 时代 | `era.tctechnology.<id>` | `era.tctechnology.bronze` |
| 仪式 | `ceremony.<modid>.<id>.title` / `.subtitle` | `ceremony.tctechnology.era.bronze.title` |
| 字幕 | `subtitles.<modid>.<sound>` | `subtitles.tcindustry.machine.jam` |
| 格式模板 | `format.tcivilization.<type>` | `format.tcivilization.date` = `第 %1$s 年 · %2$s · 第 %3$s 日` |

### 12.3 规则

1. **禁止拼接**：句子必须是完整模板，参数用 `%1$s` 等位置参数，便于各语言调整语序。
2. **复数**：中文无复数。英文需要时使用两个键 `.one` 与 `.other`，由 `TcFormat.plural(key, n)` 选择。
3. 名称（NPC 名、地名、文明名）由各模组的名称生成器按语言生成：zh_cn 生成中文名，en_us 生成拉丁转写；同一实体在两种语言下保持同一种子（15 章、23 章）。
4. 长度：英文通常比中文长 1.5-2.5 倍。界面设计必须以**英文长度**为准留出空间，或使用截断 + 悬停全文（8.5）。
5. 设计文档中的英文暂名（如 Wildlands）就是 en_us 的显示名；中文名（山河）是 zh_cn 的显示名。

### 12.4 伪本地化与检查

1. 开发版提供伪语言 `tc_pseudo`（资源包形式，不随正式版发布）：每个字符串加方括号并扩展 40%（例：`[Ŕéšéàŕçĥ çöɱƥļéţé ~~~~]`），用于发现硬编码文本与截断问题。
2. 构建检查 `checkLang`（Gradle 任务）：
   - zh_cn 与 en_us 的键集合必须完全一致；
   - 代码中用到的每个 `Component.translatable("...")` 字面量键都必须存在（静态扫描）；
   - 参数个数在两种语言中必须一致；
   - 任何 `Component.literal` 的使用都需要 `// i18n-ok` 注释说明原因（玩家输入的名字、数字等）。

---

## 13. 客户端配置

### 13.1 原则

1. **客户端配置只改变呈现，不改变信息权限。** 时代门槛、测绘等级（SL，22 章）与知识门控（8.3）一律在服务端判定；客户端配置不能让玩家看到更多（UP-6）。
2. 配置文件遵循 02 §9：每个模组一个 `<modid>-client.toml`，位于 `config/`。界面相关的公共项全部集中在核心 `tcivilization-client.toml`，其他模组**读取**而不复制（例如 Atlas 读取 `[accessibility]`，22 §11）。
3. 所有项都可在游戏内修改并立即生效，无需重启；例外项在表中标注“重进世界”。
4. 每项都必须有 `en_us` 与 `zh_cn` 的说明文字（键 `config.<modid>.<section>.<key>` / `.tooltip`），由 `checkLang` 检查（12.4）。

### 13.2 `tcivilization-client.toml`（界面部分，权威）

03 §13.3 是同一文件的规格；两处键名、默认值、范围必须一致。渲染与音频项见 32 §10，本表不重复。

| 节 | 键 | 默认 | 范围 | 说明 | 本章 |
| --- | --- | --- | --- | --- | --- |
| `[hud]` | `level` | `standard` | `minimal` / `standard` / `expanded` | HUD 等级；H 循环（Atlas 读写同一键） | 3.1 |
| `[hud]` | `scale` | 1.0 | 0.5-2.0 | 在原版 GUI 缩放上再乘 | 11.3 |
| `[notifications]` | `duration_scale` | 1.0 | 0.5-3.0 | toast 时长倍率；危急横幅的 15 s 上限同样乘以此值 | 7.1 |
| `[notifications]` | `combat_dnd` | true | bool | 战斗免打扰 | 7.5 |
| `[notifications]` | `muted_categories` | `[]` | 类别 ID 列表 | 被静音的类别只进通知中心；`CRITICAL` 不可静音 | 7.6 |
| `[notifications]` | `narrate` | true | bool | 到达时用原版旁白朗读标题（仍受原版旁白开关控制） | 11.5 |
| `[wheel]` | `mode` | `hold` | `hold` / `toggle` | 指令轮盘触发方式 | 5.3 |
| `[wheel]` | `deadzone` | 12 | 4-32 | 轮盘中心死区半径（逻辑像素） | 6.5 |
| `[tooltip]` | `shift_hint` | true | bool | 提示框底部显示“按住 Shift 查看详情” | 8.1 |
| `[format]` | `temperature_unit` | `celsius` | `celsius` / `fahrenheit` | 只影响显示，且只在 `en_us` 下生效；内部始终是 °C 与 K | 8.4 |
| `[format]` | `compact_numbers` | true | bool | HUD 与列表中 ≥ 100,000 的整数用紧凑写法（`12.5 万` / `12.5K`）；面板详情始终显示完整数字 | 8.4 |
| `[panel]` | `remember_last_tab` | true | bool | J 打开上次所在页签；false 时总是总览 | 4.2 |
| `[panel]` | `pause_singleplayer` | true | bool | 单人游戏打开文明面板、科技树、技能、日志、外交时暂停（地图另由 Atlas 配置） | 2.2 |
| `[accessibility]` | `colorblind_mode` | `off` | `off` / `deuteranopia` / `protanopia` / `tritanopia` / `monochrome` | 语义色与分类色替换 | 11.1 |
| `[accessibility]` | `high_contrast` | false | bool | 不透明背景、2 像素边框 | 11.1 |
| `[accessibility]` | `reduced_motion` | false | bool | 关闭动画与仪式动效 | 11.2 |
| `[debug]` | `show_widget_bounds` | false | bool | 绘制每个 HUD 控件的边框与 ID（开发用） | 15.4 |

### 13.3 其他模组的界面相关客户端配置（索引）

本表只做索引，键的定义以各章为准。新增界面相关客户端键时，模组章节须在附录 A 中登记，并在此表补一行。

| 文件 | 键 | 定义处 |
| --- | --- | --- |
| `tctechnology-client.toml` | `[tree] default_zoom`、`wide_mode`、`show_fallback_techs`；`[ceremony] play_music` | 04 章 |
| `tcskills-client.toml` | `[screen] pause_singleplayer`、`remember_last_skill`、`show_reserve`；`[hud] feed_enabled`、`feed_lines`、`feed_hold_seconds`、`feed_fade_seconds`、`show_multiplier_tags`、`s0_offset_y`；`[toast] s0_enabled`、`s0_per_minute` | 05 §13.2 |
| `tcwildlands-client.toml` | `client.seasonal_colors`、`bare_branches`、`local_sky`、`weather_blend`、`rain_pillars`、`sun_remap`、`screen_shake`、`heat_haze` | 10 §13.3 |
| `tcecology-client.toml` | `tracking.decal_style`、`tracking.max_decals`、`audio.dawn_chorus_volume` | 11 章 |
| `tccultivation-client.toml` | `hud.field_status`、`tooltip.numeric`、`tooltip.freshness_icon`、`overlay.soil_particles`、`overlay.paddock_outline` | 12 §13.3 |
| `tcindustry-client.toml` | `client.machine_animation`、`annotation_radius`、`forge_beat_cue`、`smoke_particles` | 13 §13.2 |
| `tcsettlements-client.toml` | `client.hud_fade_seconds`、`show_room_tooltips`、`crowd_ambience` | 15 章 |
| `tcexchange-client.toml` | `client.ticker_rows`、`client.show_vu` | 16 章 |
| `tcdiscovery-client.toml` | `observe.mode`、`observe.zoom`、`observe.outline`、`journal.skin_by_stage` | 17 章 |
| `tcechoes-client.toml` | `hud.clue_compass`、`hud.clue_compass_range` | 18 章 |
| `tccommons-client.toml` | `hud.claim_status`、`render.claim_border_distance` | 19 章 |
| `tcfrontiers-client.toml` | `hud.show_danger_on_entry`、`combat.hit_marker`、`siege.show_trajectory_hint` | 20 章 |
| `tcbeyond-client.toml` | `ambience`、`sky_tint`、`meter_flash` | 21 章 |
| `tcatlas-client.toml` | `[map] pause_singleplayer`（默认 false）、`default_zoom`、`smooth_zoom`、`tile_uploads_per_frame`、`gpu_cache_tiles`、`client_cache_mb`、`show_grid` | 22 §13.1、31 §2.2 |
| `tcatlas-client.toml` | `[minimap] shape`、`size`（64-160，默认 96）、`zoom`、`max_markers`、`show_error_circle`；`[compass] tick_style`；`[waypoints] beams`、`beam_distance` | 22 §13.1 |
| `tcatlas-client.toml` | `[hud] context_profiles`、`profile_on_foot` / `profile_vehicle` / `profile_aircraft` / `profile_spacesuit` | 22 §13.1 |
| `tcatlas-client.toml` | `[skin] skin_lock`（`auto`）、`ui_effects`（true）；旧名 `[ui] ui_skin` / `[ui] ui_effects` 读取时迁移 | 22 §13.1、32 §10、31 §10.1 |
| `tcatlas-client.toml` | `[compat] external_map`（`none` / `detected`） | 22 §13.1、31 §6.3 |
| `tcnations-client.toml` | `ui.show_ai_reasons`、`ui.territory_entry_banner`、`ui.relation_number` | 23 §13.2 |
| `tcorbit-client.toml` | `sky.render_earth`、`sky.earth_texture_from_atlas`、`hud.telemetry_units`、`effects.launch_particles` | 24 章 |

`tcnetworks` 没有界面相关客户端配置。

各模组的 `hud.<控件>` 布尔键（12 `hud.field_status`、18 `hud.clue_compass`、19 `hud.claim_status`）以及 23 `ui.territory_entry_banner` 只决定该控件注册时的 `defaultVisible`；玩家在布局编辑器中设置的 `force_shown` / `force_hidden`（3.6）优先。

### 13.4 设置界面

1. 入口：文明面板右上角齿轮按钮 → “界面设置”；另可从原版“模组”列表进入 NeoForge 配置界面（两者写同一文件）。
2. 界面设置分 6 页：HUD、通知、输入、提示框与格式、无障碍、画面与声音。每页左侧是选项，右侧是**实时预览**（HUD 页显示缩小的默认布局，通知页可点“发送测试通知”触发 4 个级别各一条）。
3. 每页底部有“恢复本页默认”；全局“恢复全部默认”需二次确认（`TcConfirmDialog`，不要求输入名称）。
4. 按键设置不在此处重复，提供跳转按钮“按键绑定…”打开原版控制设置并预先筛选到 `key.categories.tc`。

---

## 14. 网络与数据同步

### 14.1 原则

1. **服务端权威**：客户端只显示服务端发来的数据；所有修改都是请求，由服务端校验后执行。
2. **订阅制**：服务端只推送玩家当前看得见的数据（显示中的 HUD 控件、打开中的页签、轮盘查询目标）。
3. **增量**：同一通道两次推送之间数据没有变化时不发包（服务端保存每玩家每通道上次发送内容的 64 位哈希）。
4. **分页**：面板快照单包 ≤ 64 KiB，列表每页 50 条（4.6）。
5. 载荷注册使用 NeoForge `PayloadRegistrar`，版本字符串与模组主版本一致；载荷 ID 与方向登记到各章附录 A，类型 `payload`。

### 14.2 核心界面载荷

完整列表与字段以 03 §12.4 为准。下表只列出界面框架相关的载荷及其限流。

| 载荷 ID | 方向 | 触发 | 限流 |
| --- | --- | --- | --- |
| `tcivilization:hud_subscribe` | C→S | 可见控件集合变化 | ≤ 2 次/秒 |
| `tcivilization:hud_data` | S→C | 订阅通道数据变化 | 每通道 ≤ 1 次/10 tick |
| `tcivilization:panel_request` | C→S | 打开页签、翻页、筛选 | ≤ 4 次/秒 |
| `tcivilization:panel_snapshot` | S→C | 响应请求；打开期间每 40 tick 增量 | 单包 ≤ 64 KiB |
| `tcivilization:wheel_query` | C→S | 轮盘打开时（目标变化去抖 10 tick） | ≤ 4 次/秒 |
| `tcivilization:wheel_options` | S→C | 响应查询 | — |
| `tcivilization:wheel_execute` | C→S | 选择选项 | ≤ 4 次/秒 |
| `tcivilization:notify` | S→C | 通知生成 | 服务端按 7.4 限流 |
| `tcivilization:ceremony` | S→C | 仪式触发 | 每玩家同时 1 个，余下排队（9.3） |
| `tcivilization:tracker_pin` / `tracker_sync` | C→S / S→C | 固定或取消追踪项；进度变化 | ≤ 2 次/秒 / 每 20 tick |
| `tcivilization:person_inspect_request` / `person_inspect_snapshot` | C→S / S→C | 打开人物界面 | ≤ 2 次/秒 |
| `tcivilization:action_result` | S→C | 回应任一修改类 C→S 请求：`requestId`、`ok`、`reason`、`detail`、`fixLink`、可选 `undoToken`（10 s）、冲突时附最新快照 | 随请求 |
| `tcivilization:action_undo` | C→S | 使用撤销令牌（16.4） | ≤ 2 次/秒 |
| `tcivilization:panel_presence` | S→C | 同一对象的查看者或软锁变化（16.2） | ≤ 1 次/秒 |

### 14.3 HUD 订阅流程

```text
客户端：控件可见集合变化（等级切换、进入聚落、手持扳手…）
   └─ 去抖 10 tick → hud_subscribe{channels:[tcsettlements:settlement_status, ...]}
服务端：校验每个通道（模组已安装、玩家有权查看）→ 记入订阅表
   └─ 每 10 tick 遍历订阅表：通道数据哈希变化 → hud_data{channel, payload}
客户端：控件隐藏 ≥ 5 s → 从订阅集合移除（避免频繁开关导致重订阅）
```

1. 通道 ID 与控件 ID 相同。一个控件只能有一个数据通道；需要多源数据时由服务端组合。
2. 订阅表随玩家下线清空；换维度时客户端重新发送完整订阅。
3. 客户端在收到首个 `hud_data` 之前，控件显示“…”占位，不显示 0（避免误导）。

### 14.4 带宽预算（每玩家）

| 来源 | 稳态平均 | 峰值 | 说明 |
| --- | --- | --- | --- |
| 核心：HUD、面板、通知、追踪、轮盘、人物 | ≤ 4 KiB/s | ≤ 32 KiB/s | 03 §14 |
| Atlas：地图瓦片与要素同步 | 追平后 ≈ 0；同步中 ≤ 16 KiB/s | ≤ 64 KiB/s（令牌桶） | 22 章；按离视口中心距离排队 |
| 其他模组的界面数据合计 | ≤ 2 KiB/s | ≤ 16 KiB/s | 面板快照计入核心预算；本行只计各模组自有载荷 |
| **合计** | **地图追平后 ≤ 6 KiB/s；同步中 ≤ 22 KiB/s** | **≤ 112 KiB/s 瞬时** | 首次打开全星球地图概览约 200 KiB，一次性 |

验收：10 名玩家在线、每人打开文明面板并移动 10 分钟，服务器出口的 TC 流量平均 ≤ 60 KiB/s（不含 Atlas 首次同步）。

### 14.5 C→S 校验

1. **身份**：只使用 `context.player()`，忽略载荷中任何玩家 UUID 字段。
2. **距离**：涉及世界中方块或实体的操作（轮盘执行、机器设置），目标必须在玩家 8 格内且已加载（03 §12.4）。
3. **权限**：经 `CivPermissions.check`（16.1；Commons 存在时委托给其领地与角色规则）判定；拒绝时返回 `tcivilization:action_result{ok=false, reason}`，客户端按 16.1 显示原因。
4. **版本**：修改共享对象（政策、岗位、蓝图、路线）的请求携带对象的 `version`；版本不一致时拒绝并返回最新快照（16.3）。
5. **限流**：超过 14.2 中的限流时，多余请求被丢弃并计数；同一玩家 10 s 内被丢弃超过 200 个时记录一次 `WARN` 日志（不踢出，以免误伤高延迟玩家）。
6. **畸形数据**：解码失败的载荷直接丢弃并记录 `DEBUG` 日志；不回显异常信息给客户端。

---

## 15. 性能预算（界面）

### 15.1 客户端（RTX 3070 级、1920×1080、GUI 缩放 2，00 章目标硬件）

| 项目 | 平均 | 峰值 | 来源 |
| --- | --- | --- | --- |
| 核心 HUD 控件合计 | ≤ 0.5 ms/帧 | ≤ 1.0 ms | 03 §14 |
| HUD + 小地图合计（全部模组控件） | ≤ 0.6 ms/帧 | ≤ 1.2 ms | 02 §8.2 |
| 世界地图打开时 | ≤ 4 ms/帧 | ≤ 8 ms | 02 §8.2 |
| 文明面板首帧 | ≤ 50 ms | — | 03 §14 |
| 文明面板稳态 | ≤ 2 ms/帧 | ≤ 4 ms | 本章 |
| 科技树（≥ 360 节点） | ≤ 3 ms/帧 | ≤ 6 ms | 本章；04 章 |
| 提示框（含知识门控） | ≤ 0.1 ms/帧 | ≤ 0.3 ms | 本章 |
| 指令轮盘 | ≤ 0.3 ms/帧 | — | 本章 |
| 仪式叠层 | ≤ 1 ms/帧 | ≤ 2 ms | 本章 |
| 世界内标注（L1） | ≤ 0.3 ms/帧（≤ 64 个标注） | — | 5.10 |
| 500 NPC 实体渲染 | ≤ 3 ms/帧 | — | 02 §8.2（不属于界面，列出供对照） |

### 15.2 服务端

| 项目 | 平均 | 峰值 | 说明 |
| --- | --- | --- | --- |
| 核心界面推送（HUD、面板、通知） | ≤ 0.1 ms/tick | ≤ 0.3 ms | 03 §14；计入核心 1.0 ms 总预算 |
| 全家族界面推送合计（10 名在线玩家） | ≤ 0.3 ms/tick | ≤ 1.0 ms | 各模组计入各自 02 §8.1 预算 |
| 单个面板快照生成 | ≤ 2 ms | ≤ 5 ms | 超过时拆分到后续 tick 继续生成 |

### 15.3 实现规则

1. HUD 绘制路径中**禁止每帧分配**：文本布局（`FormattedCharSequence`）在数据变化时生成并缓存；颜色、矩形使用预分配数组。
2. 控件在 `shouldRender == false` 时不调用 `render`，也不计算布局。
3. 图表与地图片段渲染到离屏纹理，数据变化时才重画。
4. 大列表一律虚拟滚动（`TcTable`），只为可见行创建行对象。
5. 字体字形使用原版字体管线；不引入自定义字体渲染器（中文字形由原版 Unifont 或资源包提供）。
6. 服务端快照构建在主线程只读取数据引用，序列化与压缩放到 `Util.backgroundExecutor()`，完成后在主线程发送。

### 15.4 测量与调试

1. 客户端指令 `/tcclient debug ui` 切换界面性能叠层：每个 HUD 控件的平均与最大耗时、本帧总耗时、订阅通道数、近 10 s 收到的字节数。
2. `[debug] show_widget_bounds = true` 绘制控件边框与 ID，并高亮碰撞解决后被挤开的控件（3.5）。
3. 服务端指令 `/tcdebug ui <player>`（权限等级 3，与 03 §5.13 的其他 `/tcdebug` 指令一致）输出该玩家的订阅表、各通道最近发送时间与字节数。
4. 自动化测试：GameTest 环境下脚本化打开每个页签、触发每级通知、开关每个 HUD 等级，记录帧时间并与 15.1 比较；超过预算 20% 时构建失败（UX-AC-10）。

---

## 16. 多人游戏

### 16.1 不可用操作：灰显并说明原因

UP-10 的落地规则。任何按钮、轮盘选项、菜单项都处于三种状态之一：

| 状态 | 何时 | 显示 |
| --- | --- | --- |
| `ENABLED` | 可执行 | 正常 |
| `DISABLED` | 提供该操作的模组已安装，但当前不能执行 | 灰色 + 锁形或原因图标；悬停或聚焦时显示原因与“如何解决” |
| `HIDDEN` | **只**用于提供该操作的模组未安装，或该操作在本世界被服务器配置关闭 | 不显示（UP-5） |

原因类别（本地化键 `gui.tcivilization.reason.<id>`，参数化）：

| 原因 ID | 例子 | “如何解决”深链接 |
| --- | --- | --- |
| `permission` | “需要角色：聚落管理者（你是：成员）” | Commons 角色页（若安装），否则无 |
| `era` | “需要时代 E5 文艺复兴” | 科技树中的关键科技 |
| `tech` | “需要科技：水力磨坊” | 科技树定位该科技 |
| `knowledge` | “尚未了解此物的性质” | 日志中的相关线索（Discovery） |
| `resource` | “缺少 12 × 铜锭” | 无（物品提示框可查配方） |
| `distance` | “距离过远（需要 ≤ 8 格）” | 无 |
| `busy` | “该居民正在睡觉”“机器正在运行” | 无 |
| `owner` | “属于 李四 的文明” | 外交页签中的该文明（Nations） |
| `vote` | “需要投票通过（进行中：2/3）” | 治理页签中的该投票 |
| `cooldown` | “冷却中：游戏时间 3 时后可用” | 无 |
| `server` | “服务器已关闭此功能” | 无 |

核心 API：

```java
public final class CivPermissions {
    /** 服务端权威判定；客户端预测使用同一接口的只读镜像 */
    public static ActionCheck check(ServerPlayer player, Identifier action, @Nullable ActionScope scope);
    /** Commons 等模组注册；多个解析器按优先级取第一个非 PASS 结果 */
    public static void registerResolver(PermissionResolver resolver);
}

public sealed interface ActionCheck permits ActionCheck.Allow, ActionCheck.Deny {
    record Allow() implements ActionCheck {}
    record Deny(Identifier reason, Component detail, Optional<String> fixLink) implements ActionCheck {}
}
```

没有 Commons 时的默认规则沿用 03 §5.1.2 与 §5.12：`founder` 与 `member` 同权，`guest` 只读；文明改名与解散只限 `founder` 或原版权限等级 2（03 §5.13）。

### 16.2 在场与编辑提示

1. 同一文明的多名玩家查看**同一对象**（同一聚落的政策页、同一条线路、同一科技）时，页面右上角显示其他查看者的头像，最多 4 个，其余为“+N”。
2. 玩家开始修改某对象（打开编辑表单、拖动蓝图）时，服务端为其登记**软锁** 30 s（每次操作续期）。其他查看者看到“李四 正在编辑…”，仍可编辑，但提交时走 16.3 的冲突流程。
3. 数据来源：服务端从 `panel_request` 推断“正在查看”；载荷 `tcivilization:panel_presence`（S→C，变化时发送，≤ 1 次/秒）。离开页签或断线 5 s 后移除。
4. 隐私：不同文明之间不显示在场信息。

### 16.3 乐观并发与冲突解决

1. 所有可被多人修改的共享对象（政策、岗位分配、蓝图、路线、订单、条约草案、HUD 以外的共享标记）带有 `long version`，每次成功修改后加 1。
2. 客户端提交修改时附带它所见的 `version`。服务端版本不同时拒绝，返回 `action_result{ok=false, reason=conflict}` 和最新快照。
3. 客户端弹出 L7 冲突对话框：左侧“你的修改”，右侧“当前值（由 李四 于 2 分钟前修改）”，按字段高亮差异；按钮“在最新值上重新应用我的修改”“放弃我的修改”“取消”。
4. 字段级合并：两次修改不涉及同一字段时，服务端自动合并并通知双方（信息级通知“已与 李四 的修改合并”），不弹对话框。
5. 列表类对象（岗位名单、路线站点）按元素 ID 合并；同一元素被两人改动才视为冲突。

### 16.4 危险操作的确认与撤销

| 操作 | 确认方式 | 撤销 |
| --- | --- | --- |
| 删除路点、取消固定追踪项、删除草稿 | 无确认 | toast“已删除 · 撤销（Ctrl+Z，10 s 内）”；客户端本地对象直接恢复 |
| 永久删除路点组、清空通知中心 | `TcConfirmDialog` 普通确认 | 无 |
| 解雇居民、拆除建筑、放弃领地、取消订单 | 普通确认，列出后果（“3 名居民将失去住所”） | 拆除：10 s 内可撤销，未开始的拆除直接取消 |
| 重置技能、重置 HUD 布局与全部设置 | 普通确认，显示代价 | 无 |
| 解散聚落、解散文明、宣战、废除条约 | **输入名称**确认 + 5 s 倒计时后按钮才可用 | 无；宣战另需 23 章规则（投票等） |

规则：

1. 确认框默认焦点在“取消”；`Enter` 不会直接确认危险操作。
2. 所有不可撤销的操作都写入编年史（`tcivilization:chronicle`），记录操作者与时间。
3. 撤销只在操作者自己的客户端提供；服务端按“撤销令牌”（随 `action_result` 下发、10 s 有效）执行，令牌过期后撤销按钮消失。

### 16.5 服务器与管理员

1. 权限等级 2 的管理员可用 `/tc civ inspect <name>` 以只读方式打开任意文明的文明面板（顶部显示“管理员查看 · 只读”横条；指令登记在 03 §5.13）。
2. 服务器配置 `tcivilization-server.toml [ui] disabled_features`（标识符列表）可关闭特定界面功能（例如 `tcatlas:waypoint_share`），被关闭的功能按 16.1 视为 `HIDDEN`。
3. 通知不镜像到聊天栏。服务器公告类事件（文明建立、宣战、时代晋升）可由服务器配置 `[notify] broadcast_to_chat`（默认 `false`）额外以一行系统消息发到聊天，消息内带深链接（4.8）。

---

## 17. 缺失模组时的界面回退

UP-5 的完整对照表。原则：**缺了就不出现**，而不是出现后不能用；但凡从其他模组可以得到部分信息，就降级显示而不是整体隐藏。

### 17.1 通用规则

1. 页签：未安装的模组不注册页签；某分组下没有页签时整个分组隐藏。
2. 总览卡片：只有已注册页签贡献卡片；核心固定卡片中依赖其他模组的项（“时代进度”依赖 Technology）在其缺失时隐藏。
3. 轮盘：选项由已安装模组的 `WheelProvider` 提供；不足 8 项时空槽不显示，不分页。
4. 深链接：目标模组缺失时 `DeepLink.isResolvable` 为 false，显示为纯文本（4.8）。
5. 提示框：来自缺失模组的行不出现；知识门控在没有 Discovery 时默认全部“已知”（8.3）。
6. HUD：控件随模组注册；布局文件中未知控件 ID 被保留但忽略（重新安装该模组后恢复原位置）。
7. 按键：只注册已安装模组的按键；两个模组可提供同一功能时只有一个注册（H、Y，6.1）。

### 17.2 逐模组回退

| 缺失的模组 | 界面变化 |
| --- | --- |
| `tctechnology` | 无 U 键与科技树；`eraIndex = -1`（03 §5.1.1）：时代徽章、时代皮肤、时代仪式、`era_progress` 与 `research_progress` 全部不出现；皮肤固定为 `tcivilization:skin/neutral`；人物服装固定为 E0 外观（03 §5.12）；`date_season` 显示完整日期（不再受历法科技门槛限制）；原因类别 `era`、`tech` 不出现，以其他模组自己的条件代替 |
| `tcskills` | 无技能页签与 `proficiency_feed`；“个人”分组隐藏；轮盘“个人”入口不出现 |
| `tcatlas` | 无 M/N/B、小地图、罗盘条、路点、坐标控件与布局编辑器；核心注册 H（6.1）；`TcMapSnippet` 显示坐标文字与“在世界中标记”按钮，该按钮改为在世界中放置一个 60 s 的 L1 光柱；时代皮肤不可用，使用 `tcivilization:skin/neutral` |
| `tcdiscovery` | 无 V 观察模式；若有 Echoes 则 Y 由 Echoes 注册并打开传说页签，否则无 Y；`KnowledgeView` 默认实现全部返回已知，提示框不出现“？”；原因类别 `knowledge` 不出现 |
| `tcechoes` | 无传说页签；编年史页无“世界历史”切换，只显示文明记忆（03 §5.12）；遗迹相关的地图标记与线索追踪项不出现 |
| `tcsettlements` | 无聚落页签、`settlement_status`；总览无人口卡；人物检视仍可用（核心人物，03 §5.7）；轮盘中的岗位与建造指令不出现 |
| `tccommons` | 无治理页签；权限按 16.1 默认规则；`vote` 原因只在 Nations 的无 Commons 投票（23 章）中出现 |
| `tcnations` | 无外交页签与 AI 文明；原因类别 `owner` 只用于其他玩家文明，且不提供深链接 |
| `tcexchange` | 无市场页签与 `market_ticker`；价值显示只用 `VU`（8.4） |
| `tcindustry` | 无生产页签与 `network_status`；机器界面模板（5.8）仍由核心提供，供其他模组的机器使用 |
| `tcnetworks` | 无物流页签与 `vehicle_status`；轮盘中“设为车站”等指令不出现 |
| `tcwildlands` | 无地理页签与 `local_conditions`；`date_season` 的季节名称仍来自核心日历（03 §5.3），无半球季节，不显示气候提示 |
| `tcecology`、`tccultivation`、`tcfrontiers`、`tcbeyond`、`tcorbit` | 各自的页签与情境控件不出现，无其他变化 |
| 只安装 `tcivilization` | 文明面板只有“总览”和“编年史”；轮盘只有核心人物指令；通知、提示框、无障碍、本地化完整可用；没有地图、科技、技能 |

### 17.3 验证

复用 [40 §10.3](40-roadmap.md) 的加载矩阵：仅核心（`tcivilization` + `tctechnology` + `tcskills`）；核心 + 单个主题模组（15 种）；全家族；全家族减一个主题模组（15 种）；仅 `tcskills`。另外补充“仅 `tcivilization`”与“核心减 `tctechnology`”“核心减 `tcskills`”三种界面专用组合。每种组合自动打开全部已注册页签与轮盘，断言：无异常日志、无空页签、无空分组、无 `isResolvable == false` 却显示为可点击的链接（UX-AC-03）。

---

## 18. 验收标准

每条都有可执行的检查方式。“自动”指 CI 中的 GameTest 或单元测试；“脚本”指按 40 §10.4 的试玩脚本由人执行；“清单”指发布前的人工核对清单。所有条目在 P0 结束时建好检查设施，相关功能落地的阶段内转为必须通过。

| 编号 | 标准 | 检查方式 | 对应 |
| --- | --- | --- | --- |
| UX-AC-01 | 1920×1080、GUI 缩放 2、标准等级、无情境控件时，TC HUD 控件总面积 ≤ 屏幕的 6%；任何等级下 TC 控件都不与原版 L2 元素（热栏、生命、饥饿、经验、Boss 栏、聊天、状态效果）重叠 | 自动：布局求解器对 4 种分辨率 × 4 种 GUI 缩放 × 3 个等级求解并检查矩形 | 3.4、3.5、UP-3 |
| UX-AC-02 | 全家族安装、全新配置下，TC 默认按键与原版、Iris、Sodium 的默认按键冲突数为 0（6.3 已占用键表） | 自动：`KeyBindingContractTest`（6.3） | 6.1、6.3 |
| UX-AC-03 | 任意 17.3 组合下没有空页签、空分组、死按钮、可点击但无法解析的深链接 | 自动：子集矩阵 | 17 |
| UX-AC-04 | 以 `guest` 角色和以“缺少前置科技”的成员身份遍历全部已注册操作：每个 `DISABLED` 状态都有非空原因文本，原因的本地化键在 `zh_cn` 与 `en_us` 中都存在 | 自动 | 16.1 |
| UX-AC-05 | 任何信息都不只靠颜色传达：5 种 `colorblind_mode` 下，所有语义状态都有对应的图标形状 ID，所有图表系列都有不同线型或标记 | 自动（检查注册数据）+ 清单（灰度截图人工辨认） | 11.1 |
| UX-AC-06 | `zh_cn` 与 `en_us` 键覆盖率 100%；伪本地化（加长 40%）下所有面板没有文本溢出或截断未加省略号的情况 | 自动：键差异检查 + 伪本地化截图的文本边界检查 | 12 |
| UX-AC-07 | 下列任务都能“仅鼠标”和“仅键盘”两种方式完成：打开文明面板并切换到任一页签；在科技树中搜索并选定研究；用轮盘给居民下达指令；设置路点并开始导航；在机器界面中切换配方；处理一条冲突对话框 | 脚本 | 6.4、6.5、11.4 |
| UX-AC-08 | 战斗免打扰窗口内没有非危急 toast；限流按 7.4 生效；同一 `mergeKey` 10 s 内只显示 1 条并计数；通知中心持久化最近 50 条警告及以上通知 | 自动 | 7 |
| UX-AC-09 | 大型仪式：3 s 后可跳过、不暂停服务器、满足 9.3 任一条件时推迟；`reduced_motion` 下没有任何位移、缩放或暗角动画 | 自动（状态机测试）+ 清单 | 9 |
| UX-AC-10 | 15.1 与 15.2 的客户端、服务端预算在参考场景下不超标 20% 以上 | 自动：15.4 第 4 条 | 15 |
| UX-AC-11 | 10 名玩家在线 10 分钟，TC 出口流量平均 ≤ 60 KiB/s（不含 Atlas 首次同步），单玩家稳态 ≤ 6 KiB/s | 自动：带宽统计 GameTest | 14.4 |
| UX-AC-12 | 向每个 C→S 载荷注入随机字节、越界数值、他人 UUID、过远坐标与过期版本：服务器不崩溃、不改变世界状态，返回或记录的结果符合 14.5 | 自动：模糊测试 | 14.5 |
| UX-AC-13 | 两个客户端同时修改同一政策的同一字段：后提交者看到冲突对话框，不丢失任何一方的修改；修改不同字段时自动合并 | 自动：双客户端测试 | 16.3 |
| UX-AC-14 | 16.4 表中“输入名称”类操作无法只用 `Enter` 或单击完成；可撤销操作在 10 s 内撤销后世界状态与操作前一致 | 自动 | 16.4 |
| UX-AC-15 | 窗口 854×480 至 3840×2160、GUI 缩放 1-4（含“自动”）下，所有面板的内容不被裁切（可滚动视为合格），`[hud] scale` 0.5-2.0 下 HUD 控件不重叠 | 自动：截图 + 边界检查 | 3.5、11.3 |
| UX-AC-16 | 文本框获得焦点时输入任意字母不会触发 TC 按键；中文输入法候选框出现在光标附近 | 自动（按键）+ 清单（IME） | 2.4 |
| UX-AC-17 | 从通知、提示框、日志打开的每个深链接都到达正确的页签与对象；`Esc` 沿面板栈逐层返回 | 自动 | 2.3、4.8 |
| UX-AC-18 | Discovery 安装时，玩家未知的属性在客户端收到的数据中不含真实值（只有“未知”或区间） | 自动：抓包检查 | 8.3 |
| UX-AC-19 | 新玩家只看游戏内界面（不看文档、不用命令），在试玩脚本 P2 中能自己找到文明面板、科技树、轮盘和地图，并说出 HUD 上每个控件的含义 | 脚本：40 §10.4 P2 | 1、3 |
| UX-AC-20 | 时代切换前后，文明面板、科技树与 HUD 中每个按钮的位置与快捷键都不变（只有皮肤变化） | 自动：E0 与 E10 皮肤下的控件矩形对比 | 10、UP-7 |

---

## 19. 开放问题与风险

### 19.1 开放问题

| 编号 | 问题 | 当前倾向 | 决定时间 |
| --- | --- | --- | --- |
| OQ-UX-01 | 是否为 Jade / WTHIT 提供兼容插件，把 `target_info` 的内容显示在它们的提示条中？ | 是：检测到它们时 `target_info` 自动隐藏，改为注册 Jade/WTHIT 插件提供同样的行（遵守 8.3 知识门控） | P1 |
| OQ-UX-02 | MC 26.x 的 GUI 绘制类名（本章写作 `GuiGraphics`）与 HUD 层注册接口需在代码中确认 | 以实际映射为准，本章接口签名中的类型名在 P0 冻结 API 时更新 | P0 |
| OQ-UX-03 | NPC 是否在人物界面中显示技能熟练度？ | 只显示“职业熟练”星级（与 Settlements 生产效率相关），不显示玩家技能树；最终由 05 章（技能）与 15 章（聚落）决定 | P2 |
| OQ-UX-04 | 是否支持路点导入导出（JourneyMap / Xaero 格式）？ | 1.0 不提供；如果玩家需求强烈，在 P6 后以 Atlas 子功能加入 | P6 |
| OQ-UX-05 | 手柄的轮盘映射（右摇杆选择、肩键确认） | 依赖 Controlify 的 API 稳定性；1.0 只保证 Controlify 虚拟鼠标可用 | P7 |
| OQ-UX-06 | 是否允许每个世界或服务器使用不同的 HUD 布局？ | 1.0 只有单一布局文件；如果反馈多，增加按服务器地址分区的布局（与 `seen_ceremonies.json` 相同的分区方式） | P5 |
| OQ-UX-07 | 原版旁白之外，是否需要更完整的读屏支持（逐控件朗读）？ | 1.0 只朗读通知标题、面板标题、焦点控件名称；更完整的支持取决于 NeoForge 无障碍接口 | P8 |
| OQ-UX-08 | 简体中文之外是否提供繁体中文（`zh_tw`、`zh_hk`）？ | 1.0 不提供官方翻译，但保证所有字符串可翻译；欢迎社区翻译 | P8 |

### 19.2 风险

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 18 个模组的界面由不同阶段完成，风格漂移 | 玩家感到“拼凑” | 共享控件库（4.7）是唯一允许的构件来源；每阶段结束时做一次皮肤截图评审（UX-AC-20） |
| HUD 控件数量随模组增加而膨胀 | 违背 DR-28 沉浸极简 | 新控件默认只能在 `EXPANDED` 或“情境”出现；进入 `STANDARD` 常驻需要在本章 3.3 登记并说明理由 |
| 与其他大型模组（地图、信息条、HUD 类）冲突 | 重叠、按键冲突 | 6.3 冲突检测与 `[compat] external_map`；OQ-UX-01 |
| Sodium / Iris 更新改变 HUD 或世界内标注的绘制方式 | L1 标注错位或闪烁 | L1 只使用原版渲染事件；每次 Sodium/Iris 大版本更新时回归 UX-AC-15、L1 截图 |
| 面板快照过大（大型文明的岗位、订单列表） | 带宽与卡顿 | 虚拟滚动 + 分页请求（4.6、15.3）；快照单包 ≤ 64 KiB |
| 知识门控与多人共享知识规则不一致 | 同一文明成员看到不同的“？” | 由 17 章唯一规定共享规则；客户端只渲染服务器下发的已知状态 |

---

## 附录 UD：界面设计决议

本章在写作中做出的、未在 00 章 DR/DA 中出现的决议。以后修改任何一条，都必须同时修改正文对应小节与 03、22 等引用章节。

| 编号 | 决议 | 理由 | 正文 |
| --- | --- | --- | --- |
| UD-01 | 指令轮盘使用 `Z`，默认按住显示 | `R`、`K`、`I` 被 Iris 占用，`G` 被原版快捷操作占用；`Z` 在左手区、与移动键相邻 | 5.3、6.1 |
| UD-02 | 技能面板与外交面板默认不绑定按键，从文明面板页签进入 | 可用单字母键只剩 9 个，已分配给使用频率更高的功能 | 6.1 |
| UD-03 | `H` 由 Atlas 注册；未装 Atlas 时由核心注册；布局编辑器 `Ctrl+H` 只在 Atlas 中存在 | 一个功能只有一个按键注册者；布局编辑依赖 Atlas 的锚点系统 | 3.1、3.6、6.1 |
| UD-04 | `Y` 由 Discovery 注册（日志）；未装 Discovery 但装了 Echoes 时，由 Echoes 注册并打开传说页签 | 两者内容相近，玩家只需记住一个键 | 5.5、6.1、17.2 |
| UD-05 | 通知 toast 使用原版 `ToastManager`，位置不可配置，TC 最多同时占 3 个 | 与原版 toast 共用队列才不会重叠；自定义位置会与其他 HUD 模组冲突 | 7.2 |
| UD-06 | 通知时长：信息 5 s、成功 6 s、警告 8 s、危急最多 15 s 后收起为警报；战斗免打扰窗口 8 s | 与 03 §5.10.5 统一；警告需要足够阅读时间 | 7.1、7.5 |
| UD-07 | 游戏中 toast 不可点击，改为 5 s 内按 `J`；在 TC 面板中可点击 | 游戏中鼠标被捕获，释放鼠标会打断操作 | 2.1、7.2 |
| UD-08 | 色盲模式保留 `monochrome`：灰度 + 相邻状态亮度差 ≥ 40% + 强制图标与纹理通道 | 覆盖全色盲与灰度显示器，也用作 UX-AC-05 的检查基准 | 10.3、11.1 |
| UD-09 | HUD 等级与缩放写在 `tcivilization-client.toml [hud]`；控件布局只有一个文件 `config/tcivilization/hud_layout.json` | 简单设置走标准配置界面；布局数据量大且由编辑器生成，不适合 TOML | 3.6、13.2 |
| UD-10 | 单人游戏中，文明面板、科技树、技能、日志、外交默认暂停；世界地图默认不暂停；两者都可配置 | 规划型界面需要安全阅读；地图常在移动中途查看 | 2.2 |
| UD-11 | 轮盘打开时可用数字键 1-8 直接选择 | 轮盘层（L5）捕获键盘，数字键不会切换快捷栏 | 5.3、6.5 |
| UD-12 | 界面带宽预算：核心 ≤ 4 KiB/s；Atlas 同步中 ≤ 16 KiB/s（突发 64 KiB/s）；其他模组合计 ≤ 2 KiB/s | 与 03 §14、22 章一致，并保证 10 人服务器 ≤ 60 KiB/s | 14.4 |
| UD-13 | 客户端配置只改变呈现方式，不改变信息权限 | 防止通过修改配置获得未知信息；知识与权限只由服务器决定 | 13.1、8.3 |
| UD-14 | 已安装模组提供但当前不可用的操作显示为灰色并说明原因；只有模组缺失或服务器关闭时才隐藏 | 多人中“为什么不能做”比“看不到”更容易理解（UP-10） | 16.1 |
| UD-15 | 共享对象用版本号乐观并发 + 字段级合并，不使用硬锁 | 硬锁在玩家掉线时需要超时处理且会阻塞他人；冲突在实际游戏中较少 | 16.2、16.3 |
| UD-16 | 危险程度最高的操作（解散、宣战、废除条约）需要输入名称 + 5 s 倒计时 | 不可撤销且影响其他玩家 | 16.4 |

## 附录 UR：职责划分

| 内容 | 规范在本章 | 实现模组 | 内容提供方 | 详细定义 |
| --- | --- | --- | --- | --- |
| 层级栈、暂停、面板栈 | 2 | `tcivilization` | — | — |
| HUD 控件注册、订阅、绘制缓存 | 3.2、3.7、3.8、14.3 | `tcivilization` | 各模组注册控件 | 各模组章节“界面”小节 |
| HUD 等级（`H`，无 Atlas 时） | 3.1 | `tcivilization` | — | 03 §5.12 |
| HUD 等级（`H`）、布局编辑器、锚点求解 | 3.4-3.6 | `tcatlas` | — | 22 章 |
| 小地图、世界地图、路点、罗盘条 | 5.6 | `tcatlas` | 各模组注册地图图层 | 22 章 |
| 文明面板外壳、总览、编年史、页签注册 | 4 | `tcivilization` | 各模组注册页签 | 03 §5.10.1 |
| 共享控件库 | 4.7 | `tcivilization` | — | 03 §12.2 |
| 深链接 | 4.8 | `tcivilization` | 各模组注册解析器 | 03 §12.2 |
| 科技树 | 5.1 | `tctechnology` | — | 04 章 |
| 技能页签 | 5.2 | `tcskills` | — | 05 章 |
| 指令轮盘外壳 | 5.3 | `tcivilization` | 各模组 `WheelProvider` | 03 §5.10.3 |
| 观察模式、日志 | 5.4、5.5 | `tcdiscovery` | 各模组提供可观察属性 | 17 章 |
| 外交界面 | 5.7 | `tcnations` | — | 23 章 |
| 机器界面模板 | 5.8 | `tcivilization` | `tcindustry` 等使用 | 13 章 |
| 人物界面 | 5.9 | `tcivilization` | 各模组添加分节 | 03 §5.10.4 |
| 按键注册与冲突检测 | 6 | 各模组按 6.1 注册；检测在 `tcivilization` | — | — |
| 通知、横幅、通知中心 | 7 | `tcivilization` | 各模组注册类别并发送 | 03 §5.10.5 |
| 提示框分层、时代徽章 | 8.1、8.2 | `tcivilization` | 各模组提供提示行 | — |
| 知识门控接口 | 8.3 | 接口在 `tcivilization`，实现在 `tcdiscovery` | — | 17 章 |
| 仪式与演出 | 9 | `tcivilization` | `tctechnology`（时代）及其他模组 | 03 §12.2 |
| 时代皮肤 | 10 | 解析在 `tcivilization`，皮肤资源在 `tcatlas` | — | 32 §5 |
| 无障碍、本地化 | 11、12 | 全部模组 | — | 32 §5 |
| 客户端配置（界面部分） | 13 | `tcivilization` | — | 03 §13.3 |
| 界面网络载荷 | 14 | `tcivilization` | 各模组自有载荷 | 03 §12.4、33 章 |
| 权限判定 `CivPermissions` | 16.1 | `tcivilization`；`tccommons` 注册解析器 | — | 03 §5.1.2、19 章 |
| 缺失模组回退 | 17 | 全部模组 | — | 03 §5.12、各模组“降级”小节 |

---
