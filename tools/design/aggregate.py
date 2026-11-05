#!/usr/bin/env python3
"""Aggregate the machine-readable appendices of the design chapters.

Reads appendix A (registry IDs), B (technologies), C (interactions) and
D (skill hooks) from every module chapter in docs/design, validates them
against the cross-module contract, and writes:

  docs/design/36-registry-index.md     registry and technology index
  docs/design/34-interaction-matrix.md interaction matrix

Usage:
  python tools/design/aggregate.py [--docs DIR] [--json REPORT] [--strict] [--check]

--check  validate only, do not write the generated chapters
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

# --------------------------------------------------------------------------
# Contract (keep in sync with docs/design/README.md, 01 and the brief)
# --------------------------------------------------------------------------

MODULES = [
    "tcivilization", "tctechnology", "tcskills",
    "tcwildlands", "tcecology", "tccultivation", "tcindustry", "tcnetworks",
    "tcsettlements", "tcexchange", "tcdiscovery", "tcechoes", "tccommons",
    "tcfrontiers", "tcbeyond", "tcatlas", "tcnations", "tcorbit",
]

FILE_MODULE = {
    "03": "tcivilization", "04": "tctechnology", "05": "tcskills",
    "10": "tcwildlands", "11": "tcecology", "12": "tccultivation",
    "13": "tcindustry", "14": "tcnetworks", "15": "tcsettlements",
    "16": "tcexchange", "17": "tcdiscovery", "18": "tcechoes",
    "19": "tccommons", "20": "tcfrontiers", "21": "tcbeyond",
    "22": "tcatlas", "23": "tcnations", "24": "tcorbit",
}

SHORT = {
    "tcivilization": "CIV", "tctechnology": "TEC", "tcskills": "SKL",
    "tcwildlands": "WLD", "tcecology": "ECO", "tccultivation": "CUL",
    "tcindustry": "IND", "tcnetworks": "NET", "tcsettlements": "SET",
    "tcexchange": "EXC", "tcdiscovery": "DIS", "tcechoes": "ECH",
    "tccommons": "COM", "tcfrontiers": "FRO", "tcbeyond": "BEY",
    "tcatlas": "ATL", "tcnations": "NAT", "tcorbit": "ORB",
}

ZH_NAME = {
    "tcivilization": "文明核心", "tctechnology": "科技核心", "tcskills": "技能",
    "tcwildlands": "山河", "tcecology": "生息", "tccultivation": "田园",
    "tcindustry": "工坊", "tcnetworks": "通途", "tcsettlements": "聚落",
    "tcexchange": "市集", "tcdiscovery": "求知", "tcechoes": "遗响",
    "tccommons": "契约", "tcfrontiers": "远征", "tcbeyond": "异界",
    "tcatlas": "舆图", "tcnations": "列国", "tcorbit": "寰宇",
}

EN_ALIAS = {
    "civilization": "tcivilization", "core": "tcivilization",
    "tcivilization-core": "tcivilization", "technology": "tctechnology",
    "skills": "tcskills", "wildlands": "tcwildlands", "ecology": "tcecology",
    "living ecology": "tcecology", "cultivation": "tccultivation",
    "industry": "tcindustry", "networks": "tcnetworks",
    "settlements": "tcsettlements", "exchange": "tcexchange",
    "discovery": "tcdiscovery", "echoes": "tcechoes", "commons": "tccommons",
    "frontiers": "tcfrontiers", "beyond": "tcbeyond", "atlas": "tcatlas",
    "nations": "tcnations", "orbit": "tcorbit",
}

ALLOWED_TYPES = [
    "block", "item", "entity", "fluid", "dimension", "biome", "structure",
    "block_entity", "menu", "keybind", "payload", "hud_widget", "civ_tab",
    "region_layer", "sound",
]

ERAS = ["stone", "neolithic", "bronze", "iron", "medieval", "renaissance",
        "industrial", "electric", "atomic", "information", "space"]
ERA_CODE = {name: f"E{i}" for i, name in enumerate(ERAS)}
ERA_TARGET = [18, 26, 30, 32, 36, 36, 40, 38, 36, 34, 34]

QUOTA = {
    "tctechnology": 25, "tcwildlands": 14, "tcecology": 14,
    "tccultivation": 33, "tcindustry": 70, "tcnetworks": 36,
    "tcsettlements": 28, "tcexchange": 18, "tcdiscovery": 26,
    "tcechoes": 6, "tccommons": 18, "tcfrontiers": 20, "tcbeyond": 8,
    "tcatlas": 12, "tcnations": 8, "tcorbit": 24,
}
QUOTA_TOLERANCE = 3

KEY_TECHS = {
    "tctechnology": ["stone/fire_making", "stone/knapping", "stone/foraging_lore"],
    "tccultivation": ["neolithic/cultivation", "neolithic/animal_husbandry"],
    "tcindustry": [
        "neolithic/pottery", "neolithic/weaving", "bronze/smelting",
        "bronze/alloying", "iron/bloomery", "medieval/water_power",
        "industrial/steam_engine", "industrial/steelmaking",
        "industrial/machine_tools", "electric/power_grid",
        "electric/internal_combustion", "electric/industrial_chemistry",
        "atomic/nuclear_fission", "atomic/transistor",
        "information/integrated_circuit", "information/automation",
    ],
    "tcnetworks": [
        "bronze/the_wheel", "iron/road_building", "industrial/railway",
        "electric/telegraphy", "atomic/powered_flight",
        "information/computer_networks",
    ],
    "tcsettlements": ["stone/shelter_building", "iron/masonry"],
    "tcexchange": ["iron/coinage", "medieval/guilds", "renaissance/banking"],
    "tcdiscovery": [
        "bronze/writing", "medieval/papermaking",
        "renaissance/printing_press", "renaissance/scientific_method",
    ],
    "tcatlas": [
        "medieval/magnetic_compass", "renaissance/celestial_navigation",
        "atomic/radar",
    ],
    "tcorbit": [
        "information/rocketry", "space/life_support", "space/orbital_assembly",
        "space/in_situ_resources", "space/interplanetary_transfer",
    ],
}
KEY_TECH_OWNER = {
    f"tctechnology:{tid}": owner
    for owner, ids in KEY_TECHS.items() for tid in ids
}

SKILLS = [
    "foraging", "hunting", "woodcutting", "mining", "farming", "husbandry",
    "cooking", "crafting", "smithing", "engineering", "construction",
    "logistics", "trade", "diplomacy", "leadership", "scholarship",
    "surveying", "exploration", "combat", "medicine", "aeronautics", "arcana",
]

DIRECTIONS = {"本模组→对方": "out", "对方→本模组": "in", "双向": "both"}

REQUIRED_DIMENSIONS = {
    "tcorbit:low_orbit": "tcorbit", "tcorbit:moon": "tcorbit",
    "tcorbit:mars": "tcorbit",
}

MINERALS = [
    "native_copper", "malachite", "chalcopyrite", "cassiterite", "galena",
    "sphalerite", "calamine", "bog_iron", "hematite", "magnetite",
    "native_gold", "native_silver", "cinnabar", "bauxite", "chromite",
    "pentlandite", "wolframite", "molybdenite", "uraninite", "ilmenite",
    "rutile", "spodumene", "monazite", "cobaltite", "sulfur", "niter",
    "sylvite", "phosphorite", "graphite", "kaolinite", "flint", "quartz_sand",
]
ROCKS = [
    "gabbro", "rhyolite", "basalt_rock", "pumice", "peridotite",
    "sandstone_rock", "shale", "limestone", "dolomite", "chalk",
    "conglomerate", "mudstone", "rock_salt", "gypsum_rock", "slate",
    "schist", "gneiss", "marble", "quartzite",
]

HEADERS = {
    "A": ["类型", "ID", "中文名", "英文名", "时代", "说明"],
    "B": ["科技 ID", "中文名", "时代", "RP", "知识阈值", "前置", "解锁"],
    "C": ["对方模组", "方向", "联动内容"],
    "D": ["行为", "技能", "数值", "冷却/上限"],
}

ID_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
TECH_RE = re.compile(r"(?:tctechnology:)?\b(" + "|".join(ERAS) + r")/([a-z0-9_]+)")
ERA_RE = re.compile(r"^E(\d{1,2})(?:\s*[-–~至]\s*E(\d{1,2}))?$")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.*?)\s*#*\s*$")
APPENDIX_RE = re.compile(r"^(?:[\d.]+\s*)?(?:附录|Appendix)\s*([ABCD])(?![A-Za-z0-9])")
APPENDIX_PARENT_RE = re.compile(r"^(?:[\d.]+\s*)?(?:附录|Appendices|Appendix)(?:\s*[（(].*[)）])?\s*$")
APPENDIX_CHILD_RE = re.compile(r"^([ABCD])(?:[\s.：:、]|$)")
MODULE_ID_RE = re.compile(r"tc[a-z]+(?:-core)?")
SEPARATOR_RE = re.compile(r"^:?-{2,}:?$")

# --------------------------------------------------------------------------
# Data model
# --------------------------------------------------------------------------


@dataclass
class Row:
    module: str
    file: str
    line: int
    values: dict


@dataclass
class Issue:
    level: str  # "error" | "warning" | "info"
    code: str
    message: str
    where: str = ""


@dataclass
class Model:
    files: dict = field(default_factory=dict)        # module -> file name
    rows: dict = field(default_factory=lambda: {k: [] for k in "ABCD"})
    issues: list = field(default_factory=list)

    def add(self, level, code, message, where=""):
        self.issues.append(Issue(level, code, message, where))


# --------------------------------------------------------------------------
# Parsing
# --------------------------------------------------------------------------


def clean(cell: str) -> str:
    cell = cell.strip().replace("\\|", "|")
    cell = re.sub(r"`([^`]*)`", r"\1", cell)
    cell = re.sub(r"\*\*([^*]*)\*\*", r"\1", cell)
    return re.sub(r"\s+", " ", cell).strip()


def split_row(line: str) -> list:
    body = line.strip()
    if body.startswith("|"):
        body = body[1:]
    if body.endswith("|") and not body.endswith("\\|"):
        body = body[:-1]
    return [clean(c) for c in re.split(r"(?<!\\)\|", body)]


def normalize_header(cells):
    return [re.sub(r"\s+", " ", c).strip() for c in cells]


def parse_file(path: Path, module: str, model: Model):
    text = path.read_text(encoding="utf-8-sig")
    lines = text.splitlines()
    current = None
    level = 0
    parent_level = None
    header = None
    in_code = False
    seen = Counter()
    for idx, raw in enumerate(lines, start=1):
        stripped = raw.strip()
        if stripped.startswith("```") or stripped.startswith("~~~"):
            in_code = not in_code
            header = None
            continue
        m = HEADING_RE.match(raw) if not in_code else None
        if m:
            lvl, title = len(m.group(1)), m.group(2).strip()
            if parent_level is not None and lvl <= parent_level:
                parent_level = None
            am = APPENDIX_RE.match(title)
            if am is None and parent_level is not None and lvl > parent_level:
                am = APPENDIX_CHILD_RE.match(title)
            if am:
                letter = am.group(1)
                if letter == current and lvl > level:
                    header = None
                    continue
                current, level, header = letter, lvl, None
                seen[current] += 1
            elif APPENDIX_PARENT_RE.match(title):
                parent_level, current, header = lvl, None, None
            elif current and lvl <= level:
                current, header = None, None
            continue
        if not current:
            continue
        if not stripped.startswith("|"):
            header = None
            continue
        cells = split_row(raw)
        if header is None:
            header = normalize_header(cells)
            expected = HEADERS[current]
            if header[: len(expected)] != expected:
                model.add("error", "header",
                          f"附录 {current} 表头应为 {expected}，实际为 {header}",
                          f"{path.name}:{idx}")
            continue
        if all(SEPARATOR_RE.match(c) for c in cells if c):
            continue
        if normalize_header(cells) == header:
            continue
        values = {}
        for i, name in enumerate(HEADERS[current]):
            values[name] = cells[i] if i < len(cells) else ""
        if len(cells) != len(HEADERS[current]):
            model.add("warning", "columns",
                      f"附录 {current} 行有 {len(cells)} 列，应为 {len(HEADERS[current])} 列",
                      f"{path.name}:{idx}")
        model.rows[current].append(Row(module, path.name, idx, values))
    for key in "ABCD":
        if seen[key] == 0:
            model.add("error", "appendix-missing", f"缺少附录 {key}", path.name)
        elif seen[key] > 1:
            model.add("warning", "appendix-dup", f"附录 {key} 出现 {seen[key]} 次", path.name)


def load(docs: Path) -> Model:
    model = Model()
    for path in sorted(docs.glob("*.md")):
        prefix = path.name[:2]
        module = FILE_MODULE.get(prefix)
        if not module:
            continue
        if module in model.files:
            model.add("error", "file-dup",
                      f"{module} 对应多个章节文件：{model.files[module]}、{path.name}")
            continue
        model.files[module] = path.name
        parse_file(path, module, model)
    for module in MODULES:
        if module not in model.files:
            model.add("warning", "chapter-missing", f"未找到 {module} 的章节文件")
    return model


# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------


def where(row: Row) -> str:
    return f"{row.file}:{row.line}"


def era_index(text: str):
    m = ERA_RE.match(text.strip())
    if not m:
        return None
    first = int(m.group(1))
    return first if 0 <= first <= 10 else None


def module_from_text(text: str) -> list:
    found = []
    lowered = text.lower()
    for token in MODULE_ID_RE.findall(lowered):
        token = token.replace("-core", "") if token == "tcivilization-core" else token
        if token in MODULES:
            found.append(token)
    if not found:
        for module, zh in ZH_NAME.items():
            if zh in text:
                found.append(module)
        for alias, module in EN_ALIAS.items():
            if re.search(rf"\b{re.escape(alias)}\b", lowered) and module not in found:
                found.append(module)
    result = []
    for module in found:
        if module not in result:
            result.append(module)
    return result


def parse_number(text: str):
    m = re.search(r"-?\d[\d,]*(?:\.\d+)?", text)
    if not m:
        return None
    try:
        return float(m.group(0).replace(",", ""))
    except ValueError:
        return None


def md_escape(text: str) -> str:
    return str(text).replace("|", "\\|").replace("\n", " ")


# --------------------------------------------------------------------------
# Validation
# --------------------------------------------------------------------------


def validate_registry(model: Model):
    seen = {}
    by_id_types = defaultdict(set)
    for row in model.rows["A"]:
        v = row.values
        rtype, rid, era = v["类型"], v["ID"], v["时代"]
        if rtype not in ALLOWED_TYPES:
            model.add("error", "a-type", f"未知类型 `{rtype}`（{rid}）", where(row))
        if not ID_RE.match(rid):
            model.add("error", "a-id", f"ID 格式错误 `{rid}`", where(row))
            continue
        ns = rid.split(":", 1)[0]
        if ns not in (row.module, "minecraft", "c"):
            model.add("warning", "a-namespace",
                      f"`{rid}` 的命名空间不是本模组 `{row.module}`", where(row))
        if era not in ("—", "-", "") and era_index(era) is None:
            model.add("warning", "a-era", f"时代值 `{era}` 无法识别（{rid}）", where(row))
        key = (rtype, rid)
        if key in seen:
            model.add("error", "a-dup",
                      f"重复注册 {rtype} `{rid}`（首次出现于 {seen[key]}）", where(row))
        else:
            seen[key] = where(row)
        by_id_types[rid].add(rtype)
    registered = {rid for (_t, rid) in seen}
    for rid, owner in REQUIRED_DIMENSIONS.items():
        if owner in model.files and rid not in registered:
            model.add("warning", "a-dimension", f"契约维度 `{rid}` 未在 {owner} 附录 A 登记")
    if "tcwildlands" in model.files:
        for mineral in MINERALS:
            candidates = {f"tcwildlands:raw_{mineral}", f"tcwildlands:{mineral}",
                          f"minecraft:{mineral}"}
            if not candidates & registered:
                model.add("warning", "a-mineral",
                          f"契约矿物 `{mineral}` 未在 Wildlands 附录 A 找到原矿物品")
        for rock in ROCKS:
            if f"tcwildlands:{rock}" not in registered:
                model.add("warning", "a-rock", f"契约岩石 `tcwildlands:{rock}` 未登记")
    return seen


def validate_techs(model: Model):
    techs = {}
    for row in model.rows["B"]:
        v = row.values
        m = TECH_RE.fullmatch(v["科技 ID"].strip())
        if not m:
            model.add("error", "b-id", f"科技 ID 格式错误 `{v['科技 ID']}`", where(row))
            continue
        tid = f"tctechnology:{m.group(1)}/{m.group(2)}"
        id_era = ERAS.index(m.group(1))
        col_era = era_index(v["时代"])
        if col_era is None:
            model.add("error", "b-era", f"`{tid}` 时代列 `{v['时代']}` 无法识别", where(row))
        elif col_era != id_era:
            model.add("error", "b-era-mismatch",
                      f"`{tid}` 时代列为 E{col_era}，ID 前缀为 E{id_era}", where(row))
        if tid in techs:
            other = techs[tid]
            model.add("error", "b-dup",
                      f"科技 `{tid}` 重复定义（{other['module']} @ {other['where']}）", where(row))
            continue
        prereqs = []
        cell = v["前置"]
        if cell not in ("—", "-", "无", ""):
            for pm in TECH_RE.finditer(cell):
                prereqs.append(f"tctechnology:{pm.group(1)}/{pm.group(2)}")
            if not prereqs:
                model.add("warning", "b-prereq-parse",
                          f"`{tid}` 前置 `{cell}` 中没有可识别的科技 ID", where(row))
        rp = parse_number(v["RP"])
        if rp is None:
            model.add("warning", "b-rp", f"`{tid}` RP `{v['RP']}` 不是数字", where(row))
        techs[tid] = {
            "id": tid, "module": row.module, "era": id_era, "name": v["中文名"],
            "rp": rp, "knowledge": parse_number(v["知识阈值"]),
            "prereqs": prereqs, "unlocks": v["解锁"], "where": where(row),
        }

    for tid, owner in KEY_TECH_OWNER.items():
        if owner not in model.files:
            continue
        tech = techs.get(tid)
        if tech is None:
            model.add("error", "b-key-missing", f"关键科技 `{tid}` 未由 {owner} 定义")
        elif tech["module"] != owner:
            model.add("error", "b-key-owner",
                      f"关键科技 `{tid}` 应由 {owner} 定义，实际为 {tech['module']}", tech["where"])

    for tech in techs.values():
        for pre in tech["prereqs"]:
            other = techs.get(pre)
            if other is None:
                owner = KEY_TECH_OWNER.get(pre)
                if owner and owner not in model.files:
                    model.add("info", "b-prereq-pending",
                              f"`{tech['id']}` 的前置 `{pre}` 属于尚未提供章节的 {owner}", tech["where"])
                else:
                    model.add("error", "b-prereq-missing",
                              f"`{tech['id']}` 的前置 `{pre}` 不存在", tech["where"])
                continue
            if other["module"] != tech["module"] and pre not in KEY_TECH_OWNER:
                model.add("error", "b-prereq-cross",
                          f"`{tech['id']}`（{tech['module']}）跨模组引用非关键科技 `{pre}`（{other['module']}）",
                          tech["where"])
            if other["era"] > tech["era"]:
                model.add("error", "b-prereq-era",
                          f"`{tech['id']}`（E{tech['era']}）的前置 `{pre}` 时代更晚（E{other['era']}）",
                          tech["where"])

    color = {}

    def visit(node, stack):
        color[node] = 1
        stack.append(node)
        for pre in techs[node]["prereqs"]:
            if pre not in techs:
                continue
            if color.get(pre) == 1:
                cycle = stack[stack.index(pre):] + [pre]
                model.add("error", "b-cycle", "科技前置成环：" + " → ".join(cycle), techs[node]["where"])
            elif color.get(pre) is None:
                visit(pre, stack)
        stack.pop()
        color[node] = 2

    sys.setrecursionlimit(10000)
    for tid in techs:
        if color.get(tid) is None:
            visit(tid, [])

    per_owner = Counter(t["module"] for t in techs.values())
    for owner, quota in QUOTA.items():
        if owner not in model.files:
            continue
        count = per_owner.get(owner, 0)
        if abs(count - quota) > QUOTA_TOLERANCE:
            model.add("warning", "b-quota",
                      f"{owner} 定义了 {count} 项科技，配额 {quota}±{QUOTA_TOLERANCE}")
    for module in ("tcivilization", "tcskills"):
        if per_owner.get(module):
            model.add("error", "b-owner", f"{module} 不应拥有科技（实际 {per_owner[module]} 项）")
    return techs


def validate_links(model: Model):
    links = defaultdict(list)  # (registrant, other) -> [entry]
    for row in model.rows["C"]:
        v = row.values
        targets = module_from_text(v["对方模组"])
        if not targets:
            model.add("error", "c-module", f"无法识别对方模组 `{v['对方模组']}`", where(row))
            continue
        direction = DIRECTIONS.get(v["方向"].replace(" ", ""))
        if direction is None:
            model.add("error", "c-direction", f"方向 `{v['方向']}` 不合法", where(row))
            direction = "both"
        for target in targets:
            if target == row.module:
                model.add("warning", "c-self", "联动对象是本模组", where(row))
                continue
            links[(row.module, target)].append({
                "direction": direction, "content": v["联动内容"], "where": where(row),
            })
    present = set(model.files)
    for (a, b) in list(links):
        if b in present and (b, a) not in links:
            model.add("warning", "c-reciprocal",
                      f"{a} 登记了与 {b} 的联动，但 {b} 没有登记与 {a} 的联动",
                      links[(a, b)][0]["where"])
    return links


def validate_skills(model: Model):
    hooks = []
    for row in model.rows["D"]:
        v = row.values
        skill = v["技能"].replace("tcskills:", "").strip().lower()
        skill = re.split(r"[\s（(]", skill)[0] if skill else skill
        if skill not in SKILLS:
            model.add("error", "d-skill", f"技能 `{v['技能']}` 不在 22 个技能 ID 中", where(row))
            continue
        value = parse_number(v["数值"])
        if value is None:
            model.add("warning", "d-value", f"数值 `{v['数值']}` 不是数字", where(row))
        elif not (0 < value <= 500):
            model.add("warning", "d-range", f"数值 {value:g} 超出 1-500 的尺度", where(row))
        if skill == "arcana" and row.module != "tcbeyond":
            model.add("error", "d-arcana", "`arcana` 只能由 tcbeyond 使用", where(row))
        hooks.append({"module": row.module, "skill": skill, "action": v["行为"],
                      "value": v["数值"], "limit": v["冷却/上限"], "where": where(row)})
    used = Counter(h["skill"] for h in hooks)
    for skill in SKILLS:
        if not used.get(skill) and model.files:
            model.add("warning", "d-unused", f"技能 `{skill}` 没有任何挂钩")
    return hooks


# --------------------------------------------------------------------------
# Rendering
# --------------------------------------------------------------------------


def header_block(title: str, purpose: str) -> list:
    stamp = _dt.datetime.now().strftime("%Y-%m-%d %H:%M")
    return [
        f"# {title}",
        "",
        "> **本文件由 `tools/design/aggregate.py` 自动生成，请勿手工编辑。**",
        f"> 生成时间：{stamp}",
        "> 修改来源：各模组章节末尾的附录 A-D；重新生成：`python tools/design/aggregate.py`",
        "",
        purpose,
        "",
    ]


def issue_table(issues, limit=None) -> list:
    out = ["| 级别 | 代码 | 位置 | 说明 |", "| --- | --- | --- | --- |"]
    order = {"error": 0, "warning": 1, "info": 2}
    rows = sorted(issues, key=lambda i: (order[i.level], i.code, i.where))
    if limit:
        rows = rows[:limit]
    label = {"error": "错误", "warning": "警告", "info": "提示"}
    for i in rows:
        out.append(f"| {label[i.level]} | `{i.code}` | {md_escape(i.where) or '—'} | {md_escape(i.message)} |")
    if len(out) == 2:
        out.append("| — | — | — | 无 |")
    return out


def render_registry(model: Model, registry, techs, hooks) -> str:
    out = header_block(
        "36 · 注册表索引 Registry Index",
        "本章汇总全部模组的注册 ID、科技与技能挂钩，并给出契约校验结果。"
        "它是实现阶段查重、查归属、查时代的唯一索引；"
        "实现状态（设计 / 占位 / 已实现）将在 P0 起由构建脚本从代码注册表回填（见 [40 §8](40-roadmap.md)）。",
    )
    errors = [i for i in model.issues if i.level == "error"]
    warnings = [i for i in model.issues if i.level == "warning"]
    infos = [i for i in model.issues if i.level == "info"]

    out += ["## 1. 校验摘要", "",
            "| 项目 | 数量 |", "| --- | --- |",
            f"| 已解析章节 | {len(model.files)} / {len(MODULES)} |",
            f"| 注册 ID（附录 A） | {len(model.rows['A'])} |",
            f"| 科技（附录 B） | {len(techs)} / 360 |",
            f"| 联动登记（附录 C） | {len(model.rows['C'])} |",
            f"| 技能挂钩（附录 D） | {len(hooks)} |",
            f"| 错误 | {len(errors)} |",
            f"| 警告 | {len(warnings)} |",
            f"| 提示 | {len(infos)} |", ""]

    out += ["## 2. 注册内容统计", "", "### 2.1 模组 × 类型", ""]
    by_mod_type = defaultdict(Counter)
    for row in model.rows["A"]:
        by_mod_type[row.module][row.values["类型"]] += 1
    types = [t for t in ALLOWED_TYPES if any(by_mod_type[m][t] for m in by_mod_type)]
    out.append("| 模组 | " + " | ".join(types) + " | 合计 |")
    out.append("| --- | " + " | ".join("---:" for _ in types) + " | ---: |")
    totals = Counter()
    for module in MODULES:
        if module not in model.files:
            continue
        counts = by_mod_type[module]
        totals.update(counts)
        out.append(f"| {ZH_NAME[module]} `{module}` | "
                   + " | ".join(str(counts[t] or "") for t in types)
                   + f" | **{sum(counts.values())}** |")
    out.append("| **合计** | " + " | ".join(f"**{totals[t]}**" for t in types)
               + f" | **{sum(totals.values())}** |")
    out.append("")

    out += ["### 2.2 按时代分布（注册内容首次出现的时代）", ""]
    by_era = defaultdict(Counter)
    for row in model.rows["A"]:
        idx = era_index(row.values["时代"])
        by_era[row.module]["—" if idx is None else f"E{idx}"] += 1
    cols = ["—"] + [f"E{i}" for i in range(11)]
    out.append("| 模组 | " + " | ".join(cols) + " |")
    out.append("| --- | " + " | ".join("---:" for _ in cols) + " |")
    for module in MODULES:
        if module in model.files:
            out.append(f"| {SHORT[module]} | " + " | ".join(str(by_era[module][c] or "") for c in cols) + " |")
    out.append("")

    out += ["## 3. 科技", "", "### 3.1 时代分布对照（目标来自 [01 第 2 节](01-eras-and-progression.md)）", ""]
    era_count = Counter(t["era"] for t in techs.values())
    out += ["| 时代 | 目标 | 实际 | 差值 | 平均 RP |", "| --- | ---: | ---: | ---: | ---: |"]
    for i, name in enumerate(ERAS):
        rps = [t["rp"] for t in techs.values() if t["era"] == i and t["rp"] is not None]
        avg = f"{sum(rps) / len(rps):,.0f}" if rps else "—"
        diff = era_count[i] - ERA_TARGET[i]
        out.append(f"| E{i} `{name}` | {ERA_TARGET[i]} | {era_count[i]} | {diff:+d} | {avg} |")
    out.append(f"| **合计** | **360** | **{len(techs)}** | **{len(techs) - 360:+d}** | |")
    out.append("")

    out += ["### 3.2 拥有者配额", "",
            "| 模组 | 配额 | 实际 | " + " | ".join(f"E{i}" for i in range(11)) + " |",
            "| --- | ---: | ---: | " + " | ".join("---:" for _ in range(11)) + " |"]
    owner_era = defaultdict(Counter)
    for t in techs.values():
        owner_era[t["module"]][t["era"]] += 1
    for module in MODULES:
        if module not in QUOTA:
            continue
        actual = sum(owner_era[module].values()) if module in model.files else "—"
        out.append(f"| {ZH_NAME[module]} `{module}` | {QUOTA[module]} | {actual} | "
                   + " | ".join(str(owner_era[module][i] or "") for i in range(11)) + " |")
    out.append("")

    out += ["### 3.3 时代关键科技", "", "| 科技 ID | 拥有者 | 状态 | 中文名 |", "| --- | --- | --- | --- |"]
    for tid, owner in KEY_TECH_OWNER.items():
        tech = techs.get(tid)
        state = "已定义" if tech else ("章节未提供" if owner not in model.files else "**缺失**")
        out.append(f"| `{tid}` | {owner} | {state} | {tech['name'] if tech else '—'} |")
    out.append("")

    out += ["### 3.4 全部科技（按时代）", ""]
    for i, name in enumerate(ERAS):
        group = sorted((t for t in techs.values() if t["era"] == i), key=lambda t: (t["module"], t["id"]))
        out += [f"#### E{i} `{name}`（{len(group)} 项）", ""]
        if not group:
            out += ["无。", ""]
            continue
        out += ["| 科技 ID | 中文名 | 拥有者 | RP | 知识阈值 | 前置 | 解锁 |",
                "| --- | --- | --- | ---: | ---: | --- | --- |"]
        for t in group:
            key = " 🔑" if t["id"] in KEY_TECH_OWNER else ""
            pre = "、".join(p.replace("tctechnology:", "") for p in t["prereqs"]) or "—"
            rp = f"{t['rp']:,.0f}" if t["rp"] is not None else "—"
            kn = f"{t['knowledge']:,.0f}" if t["knowledge"] is not None else "—"
            out.append(f"| `{t['id']}`{key} | {md_escape(t['name'])} | {SHORT[t['module']]} | {rp} | {kn} | "
                       f"{md_escape(pre)} | {md_escape(t['unlocks'])} |")
        out.append("")

    out += ["## 4. 技能挂钩汇总", "", "### 4.1 技能 × 模组（挂钩条数）", ""]
    matrix = defaultdict(Counter)
    for h in hooks:
        matrix[h["skill"]][h["module"]] += 1
    mods = [m for m in MODULES if m in model.files]
    out.append("| 技能 | " + " | ".join(SHORT[m] for m in mods) + " | 合计 |")
    out.append("| --- | " + " | ".join("---:" for _ in mods) + " | ---: |")
    for skill in SKILLS:
        row = matrix[skill]
        out.append(f"| `{skill}` | " + " | ".join(str(row[m] or "") for m in mods)
                   + f" | **{sum(row.values())}** |")
    out.append("")

    out += ["## 5. 注册 ID 总表", ""]
    for module in MODULES:
        rows = [r for r in model.rows["A"] if r.module == module]
        if module not in model.files:
            continue
        out += [f"### 5.{MODULES.index(module) + 1} {ZH_NAME[module]} `{module}`（{len(rows)} 项，来源 [{model.files[module]}]({model.files[module]})）", ""]
        if not rows:
            out += ["无。", ""]
            continue
        out += ["| 类型 | ID | 中文名 | 英文名 | 时代 | 说明 |", "| --- | --- | --- | --- | --- | --- |"]
        order = {t: i for i, t in enumerate(ALLOWED_TYPES)}
        for r in sorted(rows, key=lambda r: (order.get(r.values["类型"], 99), r.values["ID"])):
            v = r.values
            out.append(f"| {v['类型']} | `{v['ID']}` | {md_escape(v['中文名'])} | {md_escape(v['英文名'])} | "
                       f"{v['时代'] or '—'} | {md_escape(v['说明'])} |")
        out.append("")

    out += ["## 6. 注册别名", "",
            "v0.3 起，注册 ID 改名必须在此登记（[40 §9.2](40-roadmap.md)）。"
            "别名由各模组章节的附录 A 说明列标注 `alias-of:<新 ID>` 后自动收录。", "",
            "| 旧 ID | 新 ID | 版本 | 来源 |", "| --- | --- | --- | --- |"]
    aliases = []
    for r in model.rows["A"]:
        m = re.search(r"alias-of:\s*([a-z0-9_.-]+:[a-z0-9_./-]+)", r.values["说明"])
        if m:
            aliases.append(f"| `{r.values['ID']}` | `{m.group(1)}` | — | {where(r)} |")
    out += aliases or ["| — | — | — | 暂无 |"]
    out.append("")

    out += ["## 7. 校验问题明细", ""]
    out += issue_table(model.issues)
    out.append("")
    return "\n".join(out)


def render_links(model: Model, links) -> str:
    out = header_block(
        "34 · 联动矩阵 Interaction Matrix",
        "本章汇总各模组附录 C 的联动登记。按 [02 §1.4](02-architecture.md)，"
        "未在此登记的跨模组联动视为缺陷；每条联动都必须由双方章节登记。",
    )
    present = [m for m in MODULES if m in model.files]
    pairs = {}
    for (a, b), entries in links.items():
        key = tuple(sorted((a, b), key=MODULES.index))
        pairs.setdefault(key, {a: [], b: []})
        pairs[key].setdefault(a, [])
        pairs[key][a].extend(entries)

    out += ["## 1. 读法", "",
            "- 行 = 登记方，列 = 对方。单元格数字 = 登记方在附录 C 中关于对方的条目数。",
            "- `●` 双方都已登记；`◐` 只有本行模组登记，对方缺登记；空白 = 无联动。",
            "- 方向在第 4 节按绝对方向展示：`A → B` 表示 A 的状态或产物影响 B。",
            "", "## 2. 矩阵", ""]
    out.append("| 登记方 \\ 对方 | " + " | ".join(SHORT[m] for m in present) + " |")
    out.append("| --- | " + " | ".join(":---:" for _ in present) + " |")
    for a in present:
        cells = []
        for b in present:
            if a == b:
                cells.append("—")
                continue
            n = len(links.get((a, b), []))
            if not n:
                cells.append("")
            else:
                mark = "●" if links.get((b, a)) else "◐"
                cells.append(f"{mark}{n}")
        out.append(f"| **{SHORT[a]}** {ZH_NAME[a]} | " + " | ".join(cells) + " |")
    out.append("")
    out += ["缩写：" + "，".join(f"{SHORT[m]} = `{m}`" for m in present), ""]

    out += ["## 3. 对称性检查", ""]
    missing = [(a, b) for (a, b) in links if b in model.files and (b, a) not in links]
    if missing:
        out += ["| 已登记方 | 缺登记方 | 已登记条目数 |", "| --- | --- | ---: |"]
        for a, b in sorted(missing, key=lambda p: (MODULES.index(p[1]), MODULES.index(p[0]))):
            out.append(f"| {a} | **{b}** | {len(links[(a, b)])} |")
    else:
        out.append("全部联动均已双向登记。")
    out.append("")

    out += ["## 4. 联动明细（按模组对）", ""]
    arrow = {"out": "→", "in": "←", "both": "↔"}
    n = 0
    for (a, b) in sorted(pairs, key=lambda p: (MODULES.index(p[0]), MODULES.index(p[1]))):
        n += 1
        out += [f"### 4.{n} {ZH_NAME[a]} `{a}` ↔ {ZH_NAME[b]} `{b}`", "",
                "| 登记方 | 方向 | 联动内容 | 来源 |", "| --- | --- | --- | --- |"]
        for registrant, other in ((a, b), (b, a)):
            for e in links.get((registrant, other), []):
                d = f"{SHORT[registrant]} {arrow[e['direction']]} {SHORT[other]}"
                out.append(f"| {SHORT[registrant]} | {d} | {md_escape(e['content'])} | {e['where']} |")
        out.append("")

    out += ["## 5. 规则", "",
            "1. 主题模组之间只能通过核心接口或 `ModList.isLoaded()` 守卫的桥接类联动（[02 §1.4](02-architecture.md)）。",
            "2. 桥接类位于 `com.nightcity.<module>.compat.<other>`。",
            "3. 每条联动在两侧章节的附录 C 中登记；对方未安装时必须有降级行为。",
            "4. 每条联动在 [40 §10.3](40-roadmap.md) 的组合测试中至少有一个“核心 + 两个模组”的测试用例。",
            ""]
    return "\n".join(out)


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------


def main(argv=None) -> int:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--docs", type=Path, default=root / "docs" / "design")
    parser.add_argument("--json", type=Path, help="write the validation report as JSON")
    parser.add_argument("--strict", action="store_true", help="exit 1 when errors are found")
    parser.add_argument("--check", action="store_true", help="validate only; do not write 34/36")
    args = parser.parse_args(argv)

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    model = load(args.docs)
    registry = validate_registry(model)
    techs = validate_techs(model)
    links = validate_links(model)
    hooks = validate_skills(model)

    if not args.check:
        (args.docs / "36-registry-index.md").write_text(
            render_registry(model, registry, techs, hooks) + "\n", encoding="utf-8", newline="\n")
        (args.docs / "34-interaction-matrix.md").write_text(
            render_links(model, links) + "\n", encoding="utf-8", newline="\n")

    counts = Counter(i.level for i in model.issues)
    print(f"chapters={len(model.files)} ids={len(model.rows['A'])} techs={len(techs)} "
          f"links={len(model.rows['C'])} hooks={len(hooks)} "
          f"errors={counts['error']} warnings={counts['warning']} infos={counts['info']}")
    for issue in sorted(model.issues, key=lambda i: ({"error": 0, "warning": 1, "info": 2}[i.level], i.code)):
        if issue.level != "info":
            print(f"[{issue.level}] {issue.code} {issue.where} {issue.message}")

    if args.json:
        era_count = Counter(t["era"] for t in techs.values())
        report = {
            "chapters": model.files,
            "counts": {"ids": len(model.rows["A"]), "techs": len(techs),
                       "links": len(model.rows["C"]), "hooks": len(hooks)},
            "tech_per_era": {f"E{i}": era_count[i] for i in range(11)},
            "tech_per_owner": Counter(t["module"] for t in techs.values()),
            "issues": [i.__dict__ for i in model.issues],
        }
        args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    return 1 if args.strict and counts["error"] else 0


if __name__ == "__main__":
    sys.exit(main())
