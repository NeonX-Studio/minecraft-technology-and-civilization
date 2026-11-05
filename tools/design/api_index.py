#!/usr/bin/env python3
"""Generate the index blocks of docs/design/33-data-api.md.

Scans section 11 (data and saves) and section 12 (API, events, payloads) of
every module chapter, plus the whole chapter text for data-pack paths, tags
and commands, validates them against the conventions written in 33, and
rewrites every block delimited by

  <!-- GENERATED:<name> BEGIN -->
  ...
  <!-- GENERATED:<name> END -->

inside 33-data-api.md. Hand-written text outside the markers is preserved.

Usage:
  python tools/design/api_index.py [--docs DIR] [--json REPORT] [--strict] [--check]

--check  validate only, do not rewrite 33
--strict exit with status 1 when any error is found
"""

from __future__ import annotations

import argparse
import datetime as _dt
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import aggregate as ag  # noqa: E402

TARGET = "33-data-api.md"
CORE = "tcivilization"
MODULE_PREFIXES = set(ag.FILE_MODULE)
NAMESPACES = set(ag.MODULES)
PACKAGE_ROOT = "com.nightcity."

# Command roots owned by the core rather than by a single module chapter.
CORE_COMMAND_ROOTS = {"/tc": "03", "/tcdebug": "03", "/tcclient": "03", "/tcopen": "31"}

# NeoForge / vanilla events that chapters subscribe to; not defined by the family.
PLATFORM_EVENTS = {
    "AddReloadListenerEvent", "AddServerReloadListenersEvent", "AddClientReloadListenersEvent",
    "AdvancementEvent", "AnimalTameEvent", "AttackEntityEvent", "BabyEntitySpawnEvent",
    "BlockDropsEvent", "BlockEvent", "BonemealEvent", "BuildCreativeModeTabContentsEvent",
    "CanPlayerSleepEvent", "ChunkDataEvent", "ChunkEvent", "ChunkWatchEvent",
    "ClientChatEvent", "ClientPlayerNetworkEvent", "ClientTickEvent", "CommandEvent",
    "ComputeFovModifierEvent", "CropGrowEvent", "DatapackSyncEvent", "EntityAttributeCreationEvent",
    "EntityAttributeModificationEvent", "EntityEvent", "EntityJoinLevelEvent",
    "EntityLeaveLevelEvent", "EntityMobGriefingEvent", "EntityMountEvent", "EntityRenderersEvent",
    "EntityStruckByLightningEvent", "EntityTeleportEvent", "EntityTickEvent", "ExplosionEvent",
    "FinalizeSpawnEvent", "FMLClientSetupEvent", "FMLCommonSetupEvent", "FMLLoadCompleteEvent",
    "GatherDataEvent", "InputEvent", "ItemEntityPickupEvent", "ItemTooltipEvent",
    "LevelEvent", "LevelTickEvent", "LivingChangeTargetEvent", "LivingDamageEvent",
    "LivingDeathEvent", "LivingDropsEvent", "LivingEntityUseItemEvent", "LivingEquipmentChangeEvent",
    "LivingEvent", "LivingExperienceDropEvent", "LivingFallEvent", "LivingHealEvent",
    "ItemFishedEvent", "LivingConversionEvent", "PlayerBrewedPotionEvent",
    "LivingIncomingDamageEvent", "LivingShieldBlockEvent", "LootTableLoadEvent", "MobSpawnEvent",
    "MobEffectEvent", "ModConfigEvent", "ModelEvent", "MovementInputUpdateEvent", "NewRegistryEvent",
    "OnDatapackSyncEvent", "PlayerEvent", "PlayerInteractEvent", "PlayerTickEvent",
    "PlayerWakeUpEvent", "PlayerXpEvent", "ProjectileImpactEvent", "RegisterCapabilitiesEvent",
    "RegisterClientCommandsEvent", "RegisterClientReloadListenersEvent", "RegisterColorHandlersEvent",
    "RegisterCommandsEvent", "RegisterDimensionSpecialEffectsEvent", "RegisterEvent",
    "RegisterGuiLayersEvent", "RegisterKeyMappingsEvent", "RegisterParticleProvidersEvent",
    "RegisterPayloadHandlersEvent", "RegisterShadersEvent", "RegisterSpawnPlacementsEvent",
    "RenderGuiEvent", "RenderGuiLayerEvent", "RenderHighlightEvent", "RenderLevelStageEvent",
    "RenderPlayerEvent", "ScreenEvent", "ServerAboutToStartEvent", "ServerChatEvent",
    "ServerStartedEvent", "ServerStartingEvent", "ServerStoppedEvent", "ServerStoppingEvent",
    "ServerTickEvent", "SleepFinishedTimeEvent", "SleepingTimeCheckEvent", "TagsUpdatedEvent",
    "TradeWithVillagerEvent", "UseItemOnBlockEvent", "VillagerTradesEvent", "ViewportEvent",
    "WandererTradesEvent", "ItemCraftedEvent", "ItemSmeltedEvent", "PlayerLoggedInEvent",
    "PlayerLoggedOutEvent", "PlayerChangedDimensionEvent", "PlayerRespawnEvent",
    "BreakEvent", "EntityPlaceEvent", "FarmlandTrampleEvent", "NeighborNotifyEvent",
    "ServerLevelLoadEvent", "Load", "Unload", "Save",
    "AddPackFindersEvent", "EnchantmentLevelSetEvent", "EntityTravelToDimensionEvent",
    "PlayerEnchantItemEvent", "PlayerSpawnPhantomsEvent", "PotionBrewEvent", "RecipesReceivedEvent",
    "RegisterBrewingRecipesEvent", "PlayerContainerEvent", "SoundEvent", "BlockGrowFeatureEvent",
    "SaplingGrowTreeEvent", "PistonEvent", "FluidPlaceBlockEvent", "CreateFluidSourceEvent",
    "StructureTemplateEvent", "ModifyDefaultComponentsEvent", "RegisterDataMapTypesEvent",
    "DataMapsUpdatedEvent", "LivingBreatheEvent", "LivingDrownEvent", "EntityInvulnerabilityCheckEvent",
}

EVENT_RE = re.compile(r"(?<![A-Za-z0-9_.])([A-Z][A-Za-z0-9]*Event(?:\.[A-Z][A-Za-z0-9]*)?)\b")
EVENT_TOKEN_RE = re.compile(r"`([A-Z][A-Za-z0-9]*Event(?:\.[A-Z][A-Za-z0-9]*)?)(?:\([^`]*\))?`")
ID_TOKEN_RE = re.compile(r"\b([a-z][a-z0-9_]*):([a-z0-9_][a-z0-9_./-]*)")
SNAKE_RE = re.compile(r"^[a-z0-9_]+(?:/[a-z0-9_]+)*$")
DECL_RE = re.compile(
    r"^(?:public\s+|protected\s+)?(?:(?:sealed|non-sealed|abstract|final|static)\s+)*"
    r"(interface|class|record|enum|@interface)\s+([A-Z][A-Za-z0-9_]*)")
PACKAGE_RE = re.compile(r"^\s*package\s+([a-z][\w.]*)\s*;")
FQN_RE = re.compile(r"\b(com\.nightcity\.[a-z][a-z0-9_.]*[A-Za-z0-9_]*)")
ENTRY_RE = re.compile(r"static\s+(Optional<\s*)?([A-Z]\w*)\s*>?\s+get\s*\(([^)]*)\)")
SAVE_EXT = (".dat", ".dat_old", ".bin", ".tcr", ".tcp", ".nbt", ".mca")
CODE_TOKEN_RE = re.compile(r"`([^`\n]+)`")
DATAPACK_RE = re.compile(r"\bdata/([a-z0-9_.-]+)/([a-z0-9_]+)(?:/([a-z0-9_]+))?")
TAG_RE = re.compile(r"#([a-z][a-z0-9_.-]*):([a-z0-9_][a-z0-9_./-]*[a-z0-9_])")
COMMAND_RE = re.compile(r"`(/tc[a-z]*)(?:\s+([a-z][a-z_-]*))?")
TWO_LEVEL_DIRS = {"worldgen", "tags"}


# --------------------------------------------------------------------------
# Parsed structures
# --------------------------------------------------------------------------


@dataclass
class Table:
    file: str
    module: str
    section: int | None
    heading: str
    line: int
    header: list
    rows: list = field(default_factory=list)  # (line, cells)


@dataclass
class Code:
    file: str
    module: str
    section: int | None
    heading: str
    line: int
    lines: list


@dataclass
class Prose:
    file: str
    module: str
    section: int | None
    heading: str
    line: int
    text: str


@dataclass
class Chapter:
    file: str
    module: str | None
    text: str
    tables: list = field(default_factory=list)
    codes: list = field(default_factory=list)
    prose: list = field(default_factory=list)


@dataclass
class Index:
    chapters: dict = field(default_factory=dict)       # prefix -> Chapter
    issues: list = field(default_factory=list)
    events: dict = field(default_factory=dict)         # name -> record
    event_refs: dict = field(default_factory=lambda: defaultdict(set))
    payloads: dict = field(default_factory=dict)
    entries: list = field(default_factory=list)
    types: list = field(default_factory=list)
    saves: dict = field(default_factory=lambda: defaultdict(set))
    attachments: list = field(default_factory=list)
    layers: list = field(default_factory=list)
    datapack: dict = field(default_factory=lambda: defaultdict(set))
    tags: dict = field(default_factory=lambda: defaultdict(set))
    commands: dict = field(default_factory=lambda: defaultdict(lambda: defaultdict(set)))

    def add(self, level, code, message, where=""):
        self.issues.append(ag.Issue(level, code, message, where))


# --------------------------------------------------------------------------
# Parsing
# --------------------------------------------------------------------------


def parse_chapter(path: Path) -> Chapter:
    text = path.read_text(encoding="utf-8-sig")
    prefix = path.name[:2]
    chapter = Chapter(path.name, ag.FILE_MODULE.get(prefix), text)
    section = None
    heading = ""
    in_code = False
    code = None
    table = None
    for idx, raw in enumerate(text.splitlines(), start=1):
        stripped = raw.strip()
        if stripped.startswith("```") or stripped.startswith("~~~"):
            if in_code:
                chapter.codes.append(code)
                code = None
            else:
                code = Code(path.name, chapter.module, section, heading, idx, [])
            in_code = not in_code
            table = None
            continue
        if in_code:
            code.lines.append(raw)
            continue
        m = ag.HEADING_RE.match(raw)
        if m:
            heading = m.group(2).strip()
            if len(m.group(1)) == 2:
                sm = re.match(r"^(\d+)\.", heading)
                section = int(sm.group(1)) if sm else None
            table = None
            continue
        if stripped.startswith("|"):
            cells = split_raw(raw)
            if table is None:
                table = Table(path.name, chapter.module, section, heading, idx, [ag.clean(c) for c in cells])
                chapter.tables.append(table)
            elif all(ag.SEPARATOR_RE.match(ag.clean(c)) for c in cells if ag.clean(c)):
                continue
            else:
                table.rows.append((idx, cells))
            continue
        table = None
        if stripped:
            chapter.prose.append(Prose(path.name, chapter.module, section, heading, idx, stripped))
    return chapter


def split_raw(line: str) -> list:
    body = line.strip()
    if body.startswith("|"):
        body = body[1:]
    if body.endswith("|") and not body.endswith("\\|"):
        body = body[:-1]
    return [c.strip() for c in re.split(r"(?<!\\)\|", body)]


def col(header, *keys, exclude=()):
    for i, name in enumerate(header):
        if i in exclude:
            continue
        if any(k in name for k in keys):
            return i
    return None


def cell(cells, i, clean=True):
    if i is None or i >= len(cells):
        return ""
    return ag.clean(cells[i]) if clean else cells[i]


def where(file, line):
    return f"{file}:{line}"


def module_ns(module):
    return module


def expected_package(module):
    return PACKAGE_ROOT + (module if module == CORE else module[2:])


# --------------------------------------------------------------------------
# Events
# --------------------------------------------------------------------------


def bus_of(name, bus_text, heading):
    if bus_text:
        t = bus_text
        suffix = "（客户端）" if "客户端" in t else ""
        if "模组" in t or "mod" in t.lower():
            return "模组" + suffix
        if "游戏" in t or "game" in t.lower() or "NeoForge" in t:
            return "游戏" + suffix
        return t
    if name.startswith("Register"):
        return "模组*"
    if "模组总线" in heading:
        return "模组"
    if "游戏总线" in heading or "NeoForge" in heading:
        return "游戏"
    return "游戏*"


def cancel_of(text):
    t = text.strip()
    if not t or t in ("—", "-", "否", "不可", "No", "no"):
        return "否"
    if t.startswith("是") or t.lower().startswith("yes"):
        return "是"
    return t[:24]


def collect_events(index: Index):
    for prefix, ch in sorted(index.chapters.items()):
        if prefix not in MODULE_PREFIXES:
            continue
        found = False
        for tb in ch.tables:
            if tb.section != 12 or not tb.header or not tb.header[0].startswith("事件"):
                continue
            h = tb.header
            c_bus = col(h, "总线")
            c_side = next((i for i, n in enumerate(h) if n == "端" or n.startswith("端")), None)
            c_cancel = col(h, "可取消")
            c_time = col(h, "时机")
            c_fields = col(h, "字段", exclude=(c_time,) if c_time is not None else ())
            c_subs = col(h, "订阅", "监听")
            fields_only = (c_fields is not None and c_time is None and c_cancel is None
                           and c_bus is None and c_subs is None)
            for line, cells in tb.rows:
                names = EVENT_RE.findall(cell(cells, 0))
                for name in dict.fromkeys(names):
                    found = True
                    prev = index.events.get(name)
                    if fields_only and prev and prev["module"] == ch.module:
                        if not prev["fields"]:
                            prev["fields"] = cell(cells, c_fields)
                        continue
                    bus = bus_of(name, cell(cells, c_bus), tb.heading)
                    default_side = "客户端" if "客户端" in bus or "客户端" in tb.heading else "服务端"
                    record = {
                        "name": name, "module": ch.module, "file": ch.file, "line": line,
                        "bus": bus,
                        "side": cell(cells, c_side) or default_side,
                        "cancel": cancel_of(cell(cells, c_cancel)) if c_cancel is not None else "?",
                        "timing": cell(cells, c_time), "fields": cell(cells, c_fields),
                        "subs": cell(cells, c_subs),
                    }
                    if c_cancel is None and name.endswith(".Pre"):
                        record["cancel"] = "是"
                    register_event(index, record)
        for pr in ch.prose:
            if pr.section == 12 and "可取消事件" in pr.text:
                for name in EVENT_TOKEN_RE.findall(pr.text):
                    found = True
                    register_event(index, {
                        "name": name, "module": ch.module, "file": ch.file, "line": pr.line,
                        "bus": bus_of(name, "", pr.heading), "side": "服务端", "cancel": "是",
                        "timing": "见正文", "fields": "", "subs": "",
                    })
        for pr in ch.prose:
            if pr.section == 12 and "可取消" in pr.text and "可取消事件" not in pr.text:
                for name in EVENT_TOKEN_RE.findall(pr.text):
                    rec = index.events.get(name)
                    if rec and rec["module"] == ch.module and rec["cancel"] == "?":
                        rec["cancel"] = "是（正文）"
        if not found:
            index.add("warning", "event-table-missing", f"{ch.module} 第 12 节没有事件表", ch.file)
    for rec in index.events.values():
        if rec["cancel"] == "?":
            index.add("info", "event-cancel-unknown",
                      f"`{rec['name']}` 未声明是否可取消（默认按“否”处理）", where(rec["file"], rec["line"]))


def register_event(index: Index, record):
    name = record["name"]
    old = index.events.get(name)
    if old is None:
        index.events[name] = record
        return
    if old["module"] == record["module"]:
        if old["file"] != record["file"] or abs(old["line"] - record["line"]) > 0:
            index.add("warning", "event-dup-local",
                      f"`{name}` 在 {record['module']} 中重复列出",
                      f"{where(old['file'], old['line'])}, {where(record['file'], record['line'])}")
        return
    if CORE in (old["module"], record["module"]):
        core, other = (old, record) if old["module"] == CORE else (record, old)
        index.add("info", "event-shared",
                  f"`{name}` 类由核心定义，{other['module']} 也发布（核心托管，可无硬依赖地共同发布）",
                  f"{where(core['file'], core['line'])}, {where(other['file'], other['line'])}")
        core.setdefault("also", []).append(other["module"])
        index.events[name] = core
        return
    index.add("error", "event-dup",
              f"`{name}` 同时由 {old['module']} 与 {record['module']} 定义；事件类只能有一个拥有者",
              f"{where(old['file'], old['line'])}, {where(record['file'], record['line'])}")


def collect_event_refs(index: Index):
    for prefix, ch in index.chapters.items():
        for m in EVENT_TOKEN_RE.finditer(ch.text):
            index.event_refs[m.group(1)].add(prefix)
    unresolved = {}
    declared = {t["name"] for t in index.types}
    for name, files in index.event_refs.items():
        if name in index.events or name in declared:
            continue
        top = name.split(".")[0]
        if top in PLATFORM_EVENTS or name in PLATFORM_EVENTS or top in index.events:
            continue
        unresolved[name] = sorted(files)
    return unresolved


def check_events_against_02(index: Index):
    ch = index.chapters.get("02")
    if not ch:
        return
    for tb in ch.tables:
        if not tb.header or tb.header[0] != "事件" or col(tb.header, "发布者") is None:
            continue
        c_pub = col(tb.header, "发布者")
        for line, cells in tb.rows:
            pubs = ag.module_from_text(cell(cells, c_pub))
            for name in EVENT_RE.findall(cell(cells, 0)):
                rec = index.events.get(name)
                if rec is None:
                    index.add("warning", "event-02-undefined",
                              f"02 §2.3 列出的 `{name}` 未在任何模组第 12 节事件表中定义",
                              where(ch.file, line))
                elif pubs and pubs[0] != rec["module"] and pubs[0] not in rec.get("also", []):
                    index.add("error", "event-02-owner",
                              f"02 §2.3 称 `{name}` 由 {pubs[0]} 发布，但定义在 {rec['module']}",
                              where(ch.file, line))


# --------------------------------------------------------------------------
# Payloads
# --------------------------------------------------------------------------

PAYLOAD_FIRST = ("负载 ID", "负载", "载荷", "ID", "Payload ID", "Payload")


def norm_dir(text):
    up = text.replace(" ", "").upper().replace("→", "2").replace("->", "2")
    both = "双向" in text or "BOTH" in up
    up = re.sub(r"[^SC2/]", "", up)
    s2c, c2s = "S2C" in up, "C2S" in up
    if both or (s2c and c2s):
        return "双向"
    if s2c:
        return "S→C"
    if c2s:
        return "C→S"
    return "?"


def has_rate(text):
    return bool(re.search(r"\d|防抖|≤|<=|限流|上限", text))


def collect_payloads(index: Index, model: ag.Model):
    with_table = set()
    for prefix, ch in sorted(index.chapters.items()):
        if prefix not in MODULE_PREFIXES:
            continue
        for tb in ch.tables:
            h = tb.header
            if tb.section != 12 or not h or h[0] not in PAYLOAD_FIRST or col(h, "方向") is None:
                continue
            with_table.add(ch.module)
            c_dir = col(h, "方向")
            c_rate = col(h, "频率")
            c_size = col(h, "大小", exclude=(c_rate,) if c_rate is not None else ())
            c_body = col(h, "内容")
            bare = 0
            for line, cells in tb.rows:
                first = cell(cells, 0)
                ids = [f"{a}:{b}" for a, b in ID_TOKEN_RE.findall(first)]
                if not ids:
                    token = first.split()[0] if first.split() else ""
                    if not token:
                        continue
                    ids = [f"{ch.module}:{token}"]
                    bare += 1
                for pid in ids:
                    ns, path = pid.split(":", 1)
                    rec = {
                        "id": pid, "module": ch.module, "file": ch.file, "line": line,
                        "dir": norm_dir(cell(cells, c_dir)), "rate": cell(cells, c_rate),
                        "size": cell(cells, c_size), "body": cell(cells, c_body),
                    }
                    if pid in index.payloads:
                        old = index.payloads[pid]
                        index.add("error", "payload-dup", f"负载 `{pid}` 重复定义",
                                  f"{where(old['file'], old['line'])}, {where(ch.file, line)}")
                        continue
                    index.payloads[pid] = rec
                    if ns != ch.module:
                        index.add("error", "payload-namespace",
                                  f"负载 `{pid}` 的命名空间应为 `{ch.module}`", where(ch.file, line))
                    if not SNAKE_RE.match(path):
                        index.add("warning", "payload-path", f"负载 `{pid}` 的路径不是 snake_case",
                                  where(ch.file, line))
                    if rec["dir"] == "?":
                        index.add("warning", "payload-direction",
                                  f"负载 `{pid}` 方向无法识别：{cell(cells, c_dir)}", where(ch.file, line))
                    if rec["dir"] in ("C→S", "双向") and not has_rate(rec["rate"]):
                        index.add("info", "payload-default-rate",
                                  f"C→S 负载 `{pid}` 未写数值频率上限，按 33 §6.3 默认 ≤ 4 次/s/玩家",
                                  where(ch.file, line))
            if bare:
                index.add("info", "payload-no-namespace",
                          f"负载表中 {bare} 条 ID 省略了命名空间，按 `{ch.module}` 补全（33 §6.1 建议写全）",
                          where(ch.file, tb.line))
    for module in ag.MODULES:
        ch = next((c for c in index.chapters.values() if c.module == module), None)
        if ch and module not in with_table:
            index.add("warning", "payload-table-missing", f"{module} 第 12 节没有网络负载表", ch.file)
    registered = defaultdict(dict)
    for row in model.rows["A"]:
        if row.values.get("类型") == "payload":
            registered[row.module][row.values.get("ID", "")] = row
    for module in with_table:
        documented = {p for p, r in index.payloads.items() if r["module"] == module}
        for pid in sorted(documented - set(registered[module])):
            rec = index.payloads[pid]
            index.add("warning", "payload-unregistered",
                      f"负载 `{pid}` 未登记在附录 A（类型 payload）", where(rec["file"], rec["line"]))
        for pid in sorted(set(registered[module]) - documented):
            row = registered[module][pid]
            index.add("warning", "payload-undocumented",
                      f"附录 A 登记的负载 `{pid}` 不在第 12 节负载表中", ag.where(row))


# --------------------------------------------------------------------------
# API types and entry points
# --------------------------------------------------------------------------


def heading_package(heading):
    m = FQN_RE.search(heading)
    if not m:
        return None
    parts = m.group(1).split(".")
    while parts and parts[-1][:1].isupper():
        parts.pop()
    return ".".join(parts) if parts else None


def collect_types(index: Index):
    seen = defaultdict(list)
    for prefix, ch in sorted(index.chapters.items()):
        if prefix not in MODULE_PREFIXES:
            continue
        for code in ch.codes:
            if code.section not in (11, 12):
                continue
            package = heading_package(code.heading)
            depth = 0
            for offset, raw in enumerate(code.lines, start=1):
                pm = PACKAGE_RE.match(raw)
                if pm:
                    package = pm.group(1)
                    continue
                if depth == 0 and not raw.startswith((" ", "\t")):
                    dm = DECL_RE.match(raw)
                    if dm:
                        kind, name = dm.group(1), dm.group(2)
                        rec = {"name": name, "kind": kind, "module": ch.module, "package": package,
                               "file": ch.file, "line": code.line + offset}
                        index.types.append(rec)
                        seen[name].append(rec)
                        check_package(index, rec)
                em = ENTRY_RE.search(raw)
                if em and em.group(2).endswith("Api"):
                    if em.group(1):
                        index.add("info", "entry-optional",
                                  f"`{em.group(2)}.get()` 返回 `Optional`；按 33 §3.2 改为非空 `get()`",
                                  where(ch.file, code.line + offset))
                    index.entries.append({
                        "type": em.group(2), "module": ch.module, "package": package,
                        "form": "Optional<…> get(" + em.group(3).strip() + ")" if em.group(1)
                        else "get(" + em.group(3).strip() + ")",
                        "file": ch.file, "line": code.line + offset,
                    })
                depth += raw.count("{") - raw.count("}")
                depth = max(depth, 0)
    core_pkg = expected_package(CORE)
    for name, recs in seen.items():
        modules = sorted({r["module"] for r in recs})
        if len(modules) > 1:
            packages = {r["package"] for r in recs}
            pkg = next(iter(packages))
            if len(packages) == 1 and pkg and (pkg == core_pkg or pkg.startswith(core_pkg + ".")):
                index.add("info", "api-core-hosted-shared",
                          f"类型 `{name}` 在 {', '.join(modules)} 两处声明，同属核心包 `{pkg}`，视为同一类型",
                          ", ".join(where(r["file"], r["line"]) for r in recs[:4]))
                continue
            index.add("warning", "api-dup-name",
                      f"类型名 `{name}` 出现在多个模组：{', '.join(modules)}；跨模组引用时须带包名或改名",
                      ", ".join(where(r["file"], r["line"]) for r in recs[:4]))


def check_package(index: Index, rec):
    package = rec["package"]
    if not package:
        if rec["name"].endswith("Api"):
            index.add("info", "type-no-package",
                      f"`{rec['name']}` 的代码块未声明 `package`（33 §3.6）",
                      where(rec["file"], rec["line"]))
        return
    expected = expected_package(rec["module"])
    if package == expected or package.startswith(expected + "."):
        return
    core = expected_package(CORE)
    if rec["module"] != CORE and (package == core or package.startswith(core + ".")):
        core_ch = next((c for c in index.chapters.values() if c.module == CORE), None)
        if core_ch and re.search(r"\b" + re.escape(rec["name"]) + r"\b", core_ch.text):
            index.add("info", "api-core-hosted",
                      f"`{rec['name']}` 由 {rec['module']} 章节定义，位于核心包 `{package}`，已在 03 登记",
                      where(rec["file"], rec["line"]))
        else:
            index.add("warning", "api-core-hosted-unregistered",
                      f"`{rec['name']}` 由 {rec['module']} 章节定义在核心包 `{package}`，但 03 §12.5 未登记",
                      where(rec["file"], rec["line"]))
        return
    index.add("warning", "api-package",
              f"`{rec['name']}` 的包 `{package}` 不符合约定，应以 `{expected}` 开头",
              where(rec["file"], rec["line"]))


# --------------------------------------------------------------------------
# Saves, attachments, layers
# --------------------------------------------------------------------------


def is_save_path(token):
    t = token.strip()
    if " " in t or t.startswith("#") or t.startswith("."):
        return False
    if t.endswith(SAVE_EXT):
        return True
    return t.startswith("tc/")


def norm_save(token):
    t = token.strip()
    for prefix in ("<world>/", "<存档>/", "world/"):
        if t.startswith(prefix):
            t = t[len(prefix):]
    return t


TREE_RE = re.compile(r"^((?:[│ ]\s{2}|\s{3})*)[├└]─+\s*([^\s#]+)")


def tree_paths(lines):
    """Turn a box-drawing directory tree into full relative paths."""
    stack = []
    out = []
    for raw in lines:
        m = TREE_RE.match(raw)
        if not m:
            continue
        depth = len(m.group(1)) // 3
        name = m.group(2)
        stack = stack[:depth] + [name.rstrip("/")]
        path = "/".join(stack)
        if name.endswith("/"):
            path += "/"
        out.append(path)
    return out


def collect_saves(index: Index):
    for prefix, ch in sorted(index.chapters.items()):
        if prefix not in MODULE_PREFIXES and prefix != "02":
            continue
        chunks = []
        for tb in ch.tables:
            if tb.section == 11 or (prefix == "02" and tb.section == 7):
                chunks.extend((line, " | ".join(cells)) for line, cells in tb.rows)
        for pr in ch.prose:
            if pr.section == 11 or (prefix == "02" and pr.section == 7):
                chunks.append((pr.line, pr.text))
        for line, text in chunks:
            for token in CODE_TOKEN_RE.findall(text):
                if is_save_path(norm_save(token)):
                    index.saves[norm_save(token)].add(prefix)
        for code in ch.codes:
            if code.section == 11 or (prefix == "02" and code.section == 7):
                for path in tree_paths(code.lines):
                    if is_save_path(path) or (path.startswith("tc/") and path.endswith("/")):
                        index.saves[path].add(prefix)
    for path, files in index.saves.items():
        m = re.match(r"^data/(tc[a-z]+)(?:\.dat|/)", path)
        if m and m.group(1) in NAMESPACES:
            owner = owner_prefix(m.group(1))
            if owner and owner not in files and owner in index.chapters:
                index.add("warning", "save-owner",
                          f"存档 `{path}` 属于 {m.group(1)}，但其章节第 11 节未提及", ", ".join(sorted(files)))


def owner_prefix(module):
    for prefix, mod in ag.FILE_MODULE.items():
        if mod == module:
            return prefix
    return None


ATTACH_HEAD = re.compile(r"附件|附加数据|Attachment|组件|Component")


def collect_attachments(index: Index):
    for prefix, ch in sorted(index.chapters.items()):
        if prefix not in MODULE_PREFIXES:
            continue
        seen = set()
        for tb in ch.tables:
            if tb.section != 11:
                continue
            head_hit = bool(tb.header and ATTACH_HEAD.search(tb.header[0]))
            for line, cells in tb.rows:
                text = " | ".join(cells)
                if not head_hit and not re.search(r"附件|Attachment|附加数据|数据组件", text):
                    continue
                kind = "组件" if ("组件" in (tb.header[0] if head_hit else text)
                                and "附件" not in text) else "附件"
                for ns, path in ID_TOKEN_RE.findall(text):
                    if ns not in NAMESPACES:
                        continue
                    aid = f"{ns}:{path}"
                    if aid in seen or path.endswith((".dat", ".tcr")):
                        continue
                    seen.add(aid)
                    host = ag.clean(cells[1]) if head_hit and len(cells) > 1 else ""
                    index.attachments.append({"id": aid, "module": ch.module, "kind": kind,
                                              "host": host, "file": ch.file, "line": line})
                    if ns != ch.module:
                        index.add("warning", "attachment-namespace",
                                  f"`{aid}` 的命名空间应为 `{ch.module}`", where(ch.file, line))


def collect_layers(index: Index, model: ag.Model):
    registered = {}
    for row in model.rows["A"]:
        if row.values.get("类型") == "region_layer":
            registered[row.values.get("ID", "")] = row
            index.layers.append({"id": row.values.get("ID", ""), "module": row.module,
                                 "desc": row.values.get("说明", ""), "file": row.file, "line": row.line})
    ch = index.chapters.get("02")
    listed = {}
    if ch:
        for tb in ch.tables:
            if tb.header and tb.header[0] == "层 ID":
                for line, cells in tb.rows:
                    for ns, path in ID_TOKEN_RE.findall(cell(cells, 0)):
                        listed[f"{ns}:{path}"] = line
    for lid, row in registered.items():
        if lid not in listed:
            index.add("info", "layer-02-missing", f"地区层 `{lid}` 未列入 02 §2.2（以本章生成表为准）",
                      ag.where(row))
    for lid, line in listed.items():
        if lid not in registered:
            index.add("warning", "layer-unregistered",
                      f"02 §2.2 列出的地区层 `{lid}` 未在拥有者附录 A 中登记（类型 region_layer）",
                      where("02-architecture.md", line))
    for layer in index.layers:
        layer["in02"] = layer["id"] in listed


# --------------------------------------------------------------------------
# Data packs, tags, commands
# --------------------------------------------------------------------------


def collect_text_tokens(index: Index):
    for prefix, ch in sorted(index.chapters.items()):
        if prefix in ("33", "34", "36"):
            continue
        for m in DATAPACK_RE.finditer(ch.text):
            ns, d1, d2 = m.group(1), m.group(2), m.group(3)
            tail = ch.text[m.end():m.end() + 4]
            if tail.startswith(".dat") or d1.endswith("dat"):
                continue
            if ch.text[max(0, m.start() - 9):m.start()].endswith(("<world>/", "<存档>/", "world/")):
                continue
            if d2 is None and not ch.text[m.end():m.end() + 1] == "/":
                continue
            key = d1 if d1 not in TWO_LEVEL_DIRS or d2 is None else f"{d1}/{d2}"
            index.datapack[(ns, key)].add(prefix)
        for ns, path in TAG_RE.findall(ch.text):
            index.tags[f"#{ns}:{path}"].add(prefix)
        for m in COMMAND_RE.finditer(ch.text):
            root, sub = m.group(1), m.group(2) or ""
            index.commands[root][sub].add(prefix)
    for (ns, key), files in sorted(index.datapack.items()):
        if ns in NAMESPACES:
            owner = owner_prefix(ns)
            if owner and owner in index.chapters and owner not in files:
                index.add("warning", "datapack-owner",
                          f"数据包目录 `data/{ns}/{key}/` 的拥有者章节未提及", ", ".join(sorted(files)))
    for tag, files in sorted(index.tags.items()):
        ns = tag[1:].split(":", 1)[0]
        if ns in NAMESPACES:
            owner = owner_prefix(ns)
            if owner and owner in index.chapters and owner not in files:
                index.add("warning", "tag-owner",
                          f"标签 `{tag}` 的拥有者章节 {owner} 未定义或提及它", ", ".join(sorted(files)))
    for root, subs in index.commands.items():
        if root in CORE_COMMAND_ROOTS:
            continue
        module = root[1:]
        if module not in NAMESPACES:
            files = sorted({f for s in subs.values() for f in s})
            index.add("warning", "command-root",
                      f"命令根 `{root}` 不对应任何模组 ID（约定：核心 `/tc`，模组 `/<modid>`）", ", ".join(files))
            continue
        owner = owner_prefix(module)
        files = {f for s in subs.values() for f in s}
        if owner and owner in index.chapters and owner not in files:
            index.add("warning", "command-owner",
                      f"命令根 `{root}` 的拥有者章节 {owner} 未定义它", ", ".join(sorted(files)))


# --------------------------------------------------------------------------
# Rendering
# --------------------------------------------------------------------------


def esc(text):
    return ag.md_escape(text) if text else "—"


def code(text):
    return f"`{ag.md_escape(text)}`" if text else "—"


def short(module):
    return ag.ZH_NAME.get(module, module or "—")


def files_cell(files):
    return "、".join(sorted(files)) if files else "—"


def render_summary(index: Index, unresolved):
    modules = [c.module for p, c in sorted(index.chapters.items()) if p in MODULE_PREFIXES]
    counts = Counter(i.level for i in index.issues)
    stamp = _dt.datetime.now().strftime("%Y-%m-%d %H:%M")
    out = [
        f"> 自动生成于 {stamp}，来源：{len(modules)} 个模组章节的第 11、12 节与全文；"
        "重新生成：`python tools/design/api_index.py`。",
        "",
        "| 模组 | 章节 | 事件 | 负载（S→C / C→S / 双向） | API 类型 | 入口 | 附件/组件 | 地区层 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for prefix, ch in sorted(index.chapters.items()):
        if prefix not in MODULE_PREFIXES:
            continue
        m = ch.module
        ev = sum(1 for e in index.events.values() if e["module"] == m)
        pl = [p for p in index.payloads.values() if p["module"] == m]
        dirs = Counter(p["dir"] for p in pl)
        ty = sum(1 for t in index.types if t["module"] == m)
        en = sum(1 for e in index.entries if e["module"] == m)
        at = sum(1 for a in index.attachments if a["module"] == m)
        ly = sum(1 for l in index.layers if l["module"] == m)
        out.append(f"| {short(m)} `{m}` | [{prefix}]({ch.file}) | {ev} | "
                   f"{len(pl)}（{dirs['S→C']} / {dirs['C→S']} / {dirs['双向']}） | {ty} | {en} | {at} | {ly} |")
    out.append(f"| **合计** | — | **{len(index.events)}** | **{len(index.payloads)}** | **{len(index.types)}** | "
               f"**{len(index.entries)}** | **{len(index.attachments)}** | **{len(index.layers)}** |")
    out += ["", f"问题：错误 {counts['error']}，警告 {counts['warning']}，提示 {counts['info']}"
                f"（明细见 §15）；未解析的事件引用 {len(unresolved)} 个（见 §7.4）。"]
    return out


def render_entries(index: Index):
    out = ["| 入口类型 | 模组 | 包 | 获取方式 | 位置 |", "| --- | --- | --- | --- | --- |"]
    for e in sorted(index.entries, key=lambda e: (ag.MODULES.index(e["module"]), e["type"])):
        out.append(f"| `{e['type']}` | {short(e['module'])} | {code(e['package'])} | {code(e['form'])} | "
                   f"{e['file']}:{e['line']} |")
    if len(out) == 2:
        out.append("| — | — | — | — | — |")
    return out


def render_events(index: Index):
    out = ["| 事件 | 拥有者 | 总线 | 端 | 可取消 | 时机 | 典型订阅者 | 位置 |",
           "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    for e in sorted(index.events.values(), key=lambda e: (ag.MODULES.index(e["module"]), e["name"])):
        owner = short(e["module"])
        if e.get("also"):
            owner += "（" + "、".join(short(m) for m in e["also"]) + " 亦发布）"
        timing = e["timing"] or e["fields"]
        out.append(f"| `{e['name']}` | {owner} | {esc(e['bus'])} | {esc(e['side'])} | {esc(e['cancel'])} | "
                   f"{esc(timing)} | {esc(e['subs'])} | {e['file']}:{e['line']} |")
    return out


def render_unresolved(unresolved):
    out = ["| 事件 | 引用章节 |", "| --- | --- |"]
    for name, files in sorted(unresolved.items()):
        out.append(f"| `{name}` | {files_cell(files)} |")
    if len(out) == 2:
        out.append("| — | 无 |")
    return out


def render_payloads(index: Index):
    out = ["| 负载 ID | 方向 | 频率上限 | 大小 | 内容 | 位置 |", "| --- | --- | --- | --- | --- | --- |"]
    for p in sorted(index.payloads.values(), key=lambda p: (ag.MODULES.index(p["module"])
                                                             if p["module"] in ag.MODULES else 99, p["id"])):
        rate = p["rate"] or ("默认" if p["dir"] != "S→C" else "—")
        out.append(f"| `{p['id']}` | {p['dir']} | {esc(rate)} | {esc(p['size'])} | {esc(p['body'][:80])} | "
                   f"{p['file']}:{p['line']} |")
    return out


def render_types(index: Index):
    out = ["| 模组 | 包 | 接口 | 记录 | 枚举 | 类 |", "| --- | --- | --- | --- | --- | --- |"]
    groups = defaultdict(lambda: defaultdict(list))
    for t in index.types:
        groups[(t["module"], t["package"] or "（未声明）")][t["kind"]].append(t["name"])
    for (module, package), kinds in sorted(groups.items(), key=lambda kv: (ag.MODULES.index(kv[0][0]), kv[0][1])):
        def names(kind):
            values = sorted(set(kinds.get(kind, [])))
            return "、".join(f"`{n}`" for n in values) if values else "—"
        classes = sorted(set(kinds.get("class", []) + kinds.get("@interface", [])))
        out.append(f"| {short(module)} | `{package}` | {names('interface')} | {names('record')} | "
                   f"{names('enum')} | {'、'.join(f'`{n}`' for n in classes) if classes else '—'} |")
    return out


def render_saves(index: Index):
    out = ["| 路径（相对存档根） | 拥有者 | 提及章节 |", "| --- | --- | --- |"]
    shared = ("tc/regions", "tc/people", "tc/planet", "tc/history", "tc/atlas")

    def owner(path, files):
        m = re.search(r"(tc[a-z]+)", path)
        if m and m.group(1) in NAMESPACES:
            return short(m.group(1))
        if path.startswith(shared) or path == "tc/":
            return {"tc/planet.bin": "山河（核心托管路径）", "tc/history.bin": "遗响（核心托管路径）",
                    "tc/atlas/": "舆图（核心托管路径）"}.get(path, "核心（共享）")
        mods = {ag.FILE_MODULE[f] for f in files if f in ag.FILE_MODULE}
        return "、".join(short(m) for m in sorted(mods)) if mods else "—"
    for path, files in sorted(index.saves.items()):
        out.append(f"| `{ag.md_escape(path)}` | {owner(path, files)} | {files_cell(files)} |")
    return out


def render_attachments(index: Index):
    out = ["| ID | 种类 | 模组 | 宿主 / 挂载 | 位置 |", "| --- | --- | --- | --- | --- |"]
    for a in sorted(index.attachments, key=lambda a: (ag.MODULES.index(a["module"]), a["id"])):
        out.append(f"| `{a['id']}` | {a['kind']} | {short(a['module'])} | {esc(a['host'][:40])} | "
                   f"{a['file']}:{a['line']} |")
    if len(out) == 2:
        out.append("| — | — | — | — | 无 |")
    return out


def render_layers(index: Index):
    out = ["| 地区层 | 拥有者 | 说明 | 02 §2.2 | 登记位置 |", "| --- | --- | --- | --- | --- |"]
    for l in sorted(index.layers, key=lambda l: (ag.MODULES.index(l["module"]), l["id"])):
        out.append(f"| `{l['id']}` | {short(l['module'])} | {esc(l['desc'][:60])} | "
                   f"{'是' if l['in02'] else '否'} | {l['file']}:{l['line']} |")
    return out


def render_datapack(index: Index):
    out = ["| 命名空间 | 目录 | 提及章节 |", "| --- | --- | --- |"]
    for (ns, key), files in sorted(index.datapack.items(),
                                   key=lambda kv: (kv[0][0] not in NAMESPACES, kv[0][0], kv[0][1])):
        out.append(f"| `{ns}` | `{key}/` | {files_cell(files)} |")
    return out


def render_tags(index: Index):
    by_ns = defaultdict(list)
    for tag, files in index.tags.items():
        by_ns[tag[1:].split(":", 1)[0]].append((tag, files))
    out = ["| 命名空间 | 数量 | 标签（引用章节） |", "| --- | --- | --- |"]
    order = sorted(by_ns, key=lambda ns: (0 if ns == "c" else 1 if ns in NAMESPACES else 2, ns))
    for ns in order:
        items = sorted(by_ns[ns])
        body = "；".join(f"`{ag.md_escape(t)}`（{'、'.join(sorted(f))}）" for t, f in items)
        out.append(f"| `{ns}` | {len(items)} | {body} |")
    return out


def render_commands(index: Index):
    out = ["| 命令根 | 拥有者 | 子命令（提及章节） |", "| --- | --- | --- |"]
    for root in sorted(index.commands, key=lambda r: (r not in CORE_COMMAND_ROOTS, r)):
        subs = index.commands[root]
        owner = "核心" if root in CORE_COMMAND_ROOTS else short(root[1:])
        body = "；".join(f"`{s or '（根）'}`（{'、'.join(sorted(f))}）" for s, f in sorted(subs.items()))
        out.append(f"| `{root}` | {owner} | {body} |")
    return out


def render_issues(index: Index):
    return ag.issue_table(index.issues)


RENDERERS = {
    "summary": None, "entries": render_entries, "events": render_events, "events-unresolved": None,
    "payloads": render_payloads, "types": render_types, "saves": render_saves,
    "attachments": render_attachments, "layers": render_layers, "datapack": render_datapack,
    "tags": render_tags, "commands": render_commands, "issues": render_issues,
}

BLOCK_RE = re.compile(r"(<!-- GENERATED:([\w-]+) BEGIN -->\n)(.*?)(<!-- GENERATED:\2 END -->)", re.S)


def rewrite(path: Path, blocks: dict, index: Index):
    text = path.read_text(encoding="utf-8-sig")
    found = set()

    def repl(m):
        name = m.group(2)
        found.add(name)
        if name not in blocks:
            index.add("warning", "marker-unknown", f"未知的生成块 `{name}`", path.name)
            return m.group(0)
        return m.group(1) + "\n".join(blocks[name]) + "\n" + m.group(4)
    new = BLOCK_RE.sub(repl, text)
    for name in blocks:
        if name not in found:
            index.add("warning", "marker-missing", f"33 缺少生成块 `{name}` 的标记", path.name)
    return new


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------


def build(docs: Path):
    index = Index()
    for path in sorted(docs.glob("*.md")):
        prefix = path.name[:2]
        if prefix.isdigit() and prefix not in ("33", "34", "36"):
            index.chapters[prefix] = parse_chapter(path)
    model = ag.load(docs)
    collect_events(index)
    check_events_against_02(index)
    collect_payloads(index, model)
    collect_types(index)
    unresolved = collect_event_refs(index)
    collect_saves(index)
    collect_attachments(index)
    collect_layers(index, model)
    collect_text_tokens(index)
    return index, unresolved


def main(argv=None) -> int:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--docs", type=Path, default=root / "docs" / "design")
    parser.add_argument("--json", type=Path, help="write the index and issues as JSON")
    parser.add_argument("--strict", action="store_true", help="exit 1 when errors are found")
    parser.add_argument("--check", action="store_true", help="validate only; do not rewrite 33")
    args = parser.parse_args(argv)
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    index, unresolved = build(args.docs)
    blocks = {name: fn(index) for name, fn in RENDERERS.items() if fn}
    blocks["events-unresolved"] = render_unresolved(unresolved)
    blocks["summary"] = render_summary(index, unresolved)

    target = args.docs / TARGET
    if target.exists():
        new = rewrite(target, blocks, index)
        blocks["issues"] = render_issues(index)
        blocks["summary"] = render_summary(index, unresolved)
        new = rewrite(target, blocks, Index())
        if not args.check:
            target.write_text(new, encoding="utf-8", newline="\n")
    else:
        index.add("error", "target-missing", f"{TARGET} 不存在；请先创建带 GENERATED 标记的手写章节")

    counts = Counter(i.level for i in index.issues)
    print(f"events={len(index.events)} payloads={len(index.payloads)} types={len(index.types)} "
          f"entries={len(index.entries)} saves={len(index.saves)} attachments={len(index.attachments)} "
          f"layers={len(index.layers)} datapack_dirs={len(index.datapack)} tags={len(index.tags)} "
          f"command_roots={len(index.commands)} unresolved_events={len(unresolved)} "
          f"errors={counts['error']} warnings={counts['warning']} infos={counts['info']}")
    for issue in sorted(index.issues, key=lambda i: ({"error": 0, "warning": 1, "info": 2}[i.level], i.code)):
        if issue.level != "info":
            print(f"[{issue.level}] {issue.code} {issue.where} {issue.message}")

    if args.json:
        report = {
            "events": index.events, "payloads": index.payloads, "types": index.types,
            "entries": index.entries, "attachments": index.attachments, "layers": index.layers,
            "saves": {k: sorted(v) for k, v in index.saves.items()},
            "unresolved_events": unresolved,
            "issues": [i.__dict__ for i in index.issues],
        }
        args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return 1 if args.strict and counts["error"] else 0


if __name__ == "__main__":
    sys.exit(main())
