#!/usr/bin/env python3
"""LabPBR 1.3 Multi-Tier Art Export, Registry Placeholder & Validation Pipeline.

Implements docs/design/32-art-audio.md sections 3.1-3.3, 3.8, 5.1, 6.2 & RM-P8:
  - Lanczos downsampling from 2K/1K/512 masters to --tier 64|128|256|512
  - Tangent-space normal renormalization (OpenGL Y+ up)
  - Smoothness conversion S = round(255 * (1 - sqrt(roughness)))
  - LabPBR 1.3 packing: <name>.png, <name>_n.png (A height >= 1), <name>_s.png (R=S, G=F0, B=P/SSS, A=emissive/255)
  - 64x -> <module>/src/main/resources/assets/<modid>/textures/
  - 128x/256x/512x -> resourcepacks/tc-pbr-<tier>x/ (with pack.mcmeta)
  - Iris shader mapping generation -> assets/<modid>/iris/tc_iris_mapping.properties
  - RM-P8 placeholder blockstates & models generation from docs/design/36-registry-index.md
  - Comprehensive --check validation (PoT, _n/_s parity, height>=1, model/texture refs, WCAG contrast, VRAM <= 1.5 GB)
"""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

TIERS = (64, 128, 256, 512)
PACK_FORMAT = 101  # Matches MC 26.3 pack.mcmeta
VRAM_BUDGET_BYTES = int(1.5 * (1024 ** 3))  # 1.5 GiB per 32-art-audio.md section 3.1

MODULES = (
    "tcivilization",
    "tctechnology",
    "tcskills",
    "tcwildlands",
    "tcecology",
    "tccultivation",
    "tcindustry",
    "tcnetworks",
    "tcsettlements",
    "tcexchange",
    "tcdiscovery",
    "tcechoes",
    "tccommons",
    "tcfrontiers",
    "tcbeyond",
    "tcatlas",
    "tcnations",
    "tcorbit",
)

VALID_IRIS_CATEGORIES = (
    "solid",
    "foliage_waving",
    "leaves_waving",
    "vine_waving",
    "water_like",
    "glass",
    "ice",
    "emissive_full",
    "emissive_partial",
    "metal",
    "snow",
    "lava_like",
)

REGISTRY_ROW_RE = re.compile(
    r"^\|\s*(block|item)\s*\|\s*`([a-z0-9_]+):([a-z0-9_/]+)`\s*\|"
)


def module_dir_for_modid(repo_root: Path, modid: str) -> Path:
    return repo_root / ("tcivilization-core" if modid == "tcivilization" else modid)


def mod_assets_dir(repo_root: Path, modid: str) -> Path:
    return module_dir_for_modid(repo_root, modid) / "src" / "main" / "resources" / "assets" / modid


def tier_assets_dir(repo_root: Path, tier: int, modid: str) -> Path:
    if tier == 64:
        return mod_assets_dir(repo_root, modid)
    return repo_root / "resourcepacks" / f"tc-pbr-{tier}x" / "assets" / modid


def is_power_of_two(n: int) -> bool:
    return n > 0 and (n & (n - 1)) == 0


# ---------------------------------------------------------------------------
# Core LabPBR 1.3 Math & Resampling
# ---------------------------------------------------------------------------

def load_materials_library(repo_root: Path) -> dict[str, dict[str, Any]]:
    mat_path = repo_root / "art-src" / "materials.json"
    raw = json.loads(mat_path.read_text(encoding="utf-8"))["materials"]
    lookup: dict[str, dict[str, Any]] = {}
    for key, val in raw.items():
        lookup[key] = val
        lookup[key.lower()] = val
    return lookup


def roughness_to_smoothness_u8(roughness_01: np.ndarray) -> np.ndarray:
    """Compute LabPBR smoothness R channel: S = round(255 * (1 - sqrt(roughness)))."""
    clamped = np.clip(roughness_01, 0.0, 1.0)
    s = np.round(255.0 * (1.0 - np.sqrt(clamped)))
    return np.clip(s, 0, 255).astype(np.uint8)


def downsample_rgba_lanczos(img: Image.Image, target_w: int, target_h: int) -> np.ndarray:
    """Premultiplied-alpha Lanczos downsampling to prevent dark fringes on cutout icons."""
    rgba = img.convert("RGBA")
    if rgba.size == (target_w, target_h):
        return np.array(rgba, dtype=np.uint8)

    arr = np.array(rgba, dtype=np.float32)
    alpha = arr[:, :, 3:4] / 255.0
    if np.all(arr[:, :, 3] == 255):
        resized = rgba.resize((target_w, target_h), resample=Image.Resampling.LANCZOS)
        return np.array(resized, dtype=np.uint8)

    premult = np.concatenate([arr[:, :, :3] * alpha, arr[:, :, 3:4]], axis=2)
    pre_img = Image.fromarray(np.clip(np.round(premult), 0, 255).astype(np.uint8), mode="RGBA")
    ds = np.array(pre_img.resize((target_w, target_h), resample=Image.Resampling.LANCZOS), dtype=np.float32)
    ds_a = ds[:, :, 3:4] / 255.0
    rgb = np.where(ds_a > 1e-3, ds[:, :, :3] / np.maximum(ds_a, 1e-3), 0.0)
    out_a = np.where(ds[:, :, 3:4] >= 96.0, 255.0, 0.0)
    rgb = np.where(out_a > 0.0, np.clip(np.round(rgb), 0, 255), 0.0)
    return np.concatenate([rgb, out_a], axis=2).astype(np.uint8)


def downsample_scalar_lanczos(img: Image.Image, target_w: int, target_h: int) -> np.ndarray:
    """Downsample single-channel non-color map (roughness/height/ao/f0) using Lanczos."""
    gray = img.convert("L")
    if gray.size != (target_w, target_h):
        gray = gray.resize((target_w, target_h), resample=Image.Resampling.LANCZOS)
    return np.array(gray, dtype=np.uint8)


def downsample_and_renormalize_normal(img: Image.Image, target_w: int, target_h: int) -> tuple[np.ndarray, np.ndarray]:
    """Lanczos downsample OpenGL tangent-space normal map (Y+ up) and re-normalize XYZ unit vectors."""
    rgb_img = img.convert("RGB")
    if rgb_img.size != (target_w, target_h):
        rgb_img = rgb_img.resize((target_w, target_h), resample=Image.Resampling.LANCZOS)
    arr = np.array(rgb_img, dtype=np.float32) / 255.0
    nx = arr[:, :, 0] * 2.0 - 1.0
    ny = arr[:, :, 1] * 2.0 - 1.0
    nz = np.maximum(arr[:, :, 2] * 2.0 - 1.0, 1e-3)
    length = np.sqrt(nx * nx + ny * ny + nz * nz)
    length = np.maximum(length, 1e-6)
    nx /= length
    ny /= length
    r_u8 = np.clip(np.round((nx * 0.5 + 0.5) * 255.0), 0, 255).astype(np.uint8)
    g_u8 = np.clip(np.round((ny * 0.5 + 0.5) * 255.0), 0, 255).astype(np.uint8)
    return r_u8, g_u8


def pack_labpbr_triplet(
    src_dir: Path,
    meta: dict[str, Any],
    materials_db: dict[str, dict[str, Any]],
    target_size: int,
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Produce (albedo_rgba, normal_rgba, specular_rgba) uint8 arrays for target_size x target_size."""
    mat_key = meta["material"]
    if mat_key not in materials_db:
        raise KeyError(f"Unknown material '{mat_key}' in {src_dir / 'meta.json'}")
    mat_info = materials_db[mat_key]

    with Image.open(src_dir / "albedo.png") as im_alb:
        albedo_rgba = downsample_rgba_lanczos(im_alb, target_size, target_size)
    with Image.open(src_dir / "normal.png") as im_nrm:
        nx_u8, ny_u8 = downsample_and_renormalize_normal(im_nrm, target_size, target_size)
    with Image.open(src_dir / "ao.png") as im_ao:
        ao_u8 = downsample_scalar_lanczos(im_ao, target_size, target_size)
    with Image.open(src_dir / "height.png") as im_h:
        h_raw = downsample_scalar_lanczos(im_h, target_size, target_size)
        height_u8 = np.clip(h_raw, 1, 255).astype(np.uint8)  # LabPBR 1.3: height clamped >= 1
    with Image.open(src_dir / "roughness.png") as im_r:
        r_u8 = downsample_scalar_lanczos(im_r, target_size, target_size)
        smooth_u8 = roughness_to_smoothness_u8(r_u8.astype(np.float32) / 255.0)

    # Optional per-pixel F0 / porosity / emissive maps, defaulting to materials.json
    f0_path = src_dir / "f0.png"
    if f0_path.exists():
        # Nearest resize for discrete metal preset codes (230..255) so presets never blur into non-metal values
        with Image.open(f0_path) as im_f0:
            f0_gray = im_f0.convert("L").resize((target_size, target_size), resample=Image.Resampling.NEAREST)
            f0_u8 = np.array(f0_gray, dtype=np.uint8)
    else:
        f0_u8 = np.full((target_size, target_size), int(mat_info["F0"]), dtype=np.uint8)

    porosity_path = src_dir / "porosity.png"
    if porosity_path.exists():
        with Image.open(porosity_path) as im_p:
            p_gray = im_p.convert("L").resize((target_size, target_size), resample=Image.Resampling.NEAREST)
            porosity_u8 = np.array(p_gray, dtype=np.uint8)
    else:
        b_val = int(mat_info.get("b_channel", mat_info.get("P", mat_info.get("SSS", 0))))
        porosity_u8 = np.full((target_size, target_size), np.clip(b_val, 0, 255), dtype=np.uint8)

    emissive_path = src_dir / "emissive.png"
    if meta.get("emissive", False) and emissive_path.exists():
        with Image.open(emissive_path) as im_em:
            emissive_u8 = downsample_scalar_lanczos(im_em, target_size, target_size)
    else:
        emissive_u8 = np.full((target_size, target_size), 255, dtype=np.uint8)

    normal_rgba = np.stack([nx_u8, ny_u8, ao_u8, height_u8], axis=2)
    specular_rgba = np.stack([smooth_u8, f0_u8, porosity_u8, emissive_u8], axis=2)
    return albedo_rgba, normal_rgba, specular_rgba


# ---------------------------------------------------------------------------
# Registry Parsing & RM-P8 Placeholder Generation
# ---------------------------------------------------------------------------

def parse_registry_blocks_and_items(repo_root: Path) -> dict[str, dict[str, list[str]]]:
    """Parse docs/design/36-registry-index.md section 5 for all block & item IDs per module."""
    index_md = repo_root / "docs" / "design" / "36-registry-index.md"
    registry: dict[str, dict[str, list[str]]] = {
        m: {"block": [], "item": []} for m in MODULES
    }
    for line in index_md.read_text(encoding="utf-8").splitlines():
        m = REGISTRY_ROW_RE.match(line.strip())
        if not m:
            continue
        entry_type, modid, path_name = m.group(1), m.group(2), m.group(3)
        if modid in registry:
            registry[modid][entry_type].append(path_name)
    return registry


def sync_registry_placeholders(repo_root: Path, target_mods: list[str]) -> dict[str, int]:
    """Generate placeholder blockstates & block/item models for unmodeled entries without overwriting modeled ones."""
    registry = parse_registry_blocks_and_items(repo_root)
    modeled_blocks = {
        (p.parent.name, p.stem)
        for p in (repo_root / "art-src" / "blocks").glob("*/*/meta.json")
        for p in [p.parent]
    }
    # Wait: fix comprehension binding cleanly
    modeled_blocks = {
        (meta_file.parent.parent.name, meta_file.parent.name)
        for meta_file in (repo_root / "art-src" / "blocks").glob("*/*/meta.json")
    }
    modeled_items = {
        (meta_file.parent.parent.name, meta_file.parent.name)
        for meta_file in (repo_root / "art-src" / "items").glob("*/*/meta.json")
    }

    stats = {"blockstates_written": 0, "block_items_written": 0, "items_written": 0, "preserved_modeled": 0}
    block_placeholder_bs = json.dumps(
        {"variants": {"": {"model": "tcivilization:block/placeholder"}}}, indent=2
    ) + "\n"
    block_item_placeholder = json.dumps(
        {"parent": "tcivilization:block/placeholder"}, indent=2
    ) + "\n"
    item_placeholder = json.dumps(
        {"parent": "tcivilization:item/placeholder"}, indent=2
    ) + "\n"

    for modid in target_mods:
        if modid not in registry:
            continue
        assets = mod_assets_dir(repo_root, modid)
        standalone_items = set(registry[modid]["item"])

        for bname in registry[modid]["block"]:
            bs_path = assets / "blockstates" / f"{bname}.json"
            if (modid, bname) in modeled_blocks:
                stats["preserved_modeled"] += 1
                continue
            if not bs_path.exists():
                bs_path.parent.mkdir(parents=True, exist_ok=True)
                bs_path.write_text(block_placeholder_bs, encoding="utf-8")
                stats["blockstates_written"] += 1

            # If block does not also declare a distinct item row, provide BlockItem model -> block/placeholder
            if bname not in standalone_items and (modid, bname) not in modeled_items:
                im_path = assets / "models" / "item" / f"{bname}.json"
                if not im_path.exists():
                    im_path.parent.mkdir(parents=True, exist_ok=True)
                    im_path.write_text(block_item_placeholder, encoding="utf-8")
                    stats["block_items_written"] += 1

        for iname in registry[modid]["item"]:
            if (modid, iname) in modeled_items:
                stats["preserved_modeled"] += 1
                continue
            im_path = assets / "models" / "item" / f"{iname}.json"
            if not im_path.exists():
                im_path.parent.mkdir(parents=True, exist_ok=True)
                im_path.write_text(item_placeholder, encoding="utf-8")
                stats["items_written"] += 1

    return stats


# ---------------------------------------------------------------------------
# Export & Iris Mapping Generation
# ---------------------------------------------------------------------------

def write_pack_mcmeta(pack_dir: Path, tier: int) -> None:
    pack_dir.mkdir(parents=True, exist_ok=True)
    mcmeta = {
        "pack": {
            "description": f"Technology and Civilization LabPBR 1.3 ({tier}x)",
            "pack_format": PACK_FORMAT,
        }
    }
    (pack_dir / "pack.mcmeta").write_text(json.dumps(mcmeta, indent=2) + "\n", encoding="utf-8")


def generate_iris_mappings(repo_root: Path, target_mods: list[str]) -> int:
    """Generate assets/<modid>/iris/tc_iris_mapping.properties from block meta.json files (32 section 6.2B)."""
    blocks_root = repo_root / "art-src" / "blocks"
    written = 0
    for modid in target_mods:
        mod_block_dir = blocks_root / modid
        by_cat: dict[str, list[str]] = {}
        if mod_block_dir.exists():
            for meta_path in sorted(mod_block_dir.glob("*/meta.json")):
                meta = json.loads(meta_path.read_text(encoding="utf-8"))
                cat = meta.get("iris_category", "solid")
                if cat not in VALID_IRIS_CATEGORIES:
                    raise ValueError(f"Invalid iris_category '{cat}' in {meta_path}")
                by_cat.setdefault(cat, []).append(meta["id"])

        if not by_cat:
            continue

        lines = [
            "# Generated by tools/art-export/export.py (docs/design/32-art-audio.md section 6.2 Layer B)",
            f"# Namespace: {modid}",
            "# Format: category.<iris_category>=<modid>:<block_a> <modid>:<block_b> ...",
        ]
        for cat in VALID_IRIS_CATEGORIES:
            if cat in by_cat:
                ids_str = " ".join(sorted(by_cat[cat]))
                lines.append(f"category.{cat}={ids_str}")

        iris_path = mod_assets_dir(repo_root, modid) / "iris" / "tc_iris_mapping.properties"
        iris_path.parent.mkdir(parents=True, exist_ok=True)
        iris_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        written += 1
    return written


def export_tier(repo_root: Path, tier: int, target_mods: list[str]) -> dict[str, Any]:
    """Export all block, item, skin, and entity textures for the given tier."""
    if tier not in TIERS:
        raise ValueError(f"Unsupported tier: {tier}; expected one of {TIERS}")

    materials_db = load_materials_library(repo_root)
    art_root = repo_root / "art-src"
    if tier > 64:
        write_pack_mcmeta(repo_root / "resourcepacks" / f"tc-pbr-{tier}x", tier)

    exported_blocks = 0
    exported_items = 0
    exported_skins = 0
    manifest_entries: list[dict[str, Any]] = []

    # 1. Blocks (face size = tier; supports multi-variant blocks & composed ores)
    ore_host_cache: dict[tuple[str, int], tuple[np.ndarray, np.ndarray, np.ndarray]] = {}
    ore_overlay_cache: dict[tuple[str, int], dict[str, Any]] = {}
    compose_ores_mod = None
    for modid in target_mods:
        bdir = art_root / "blocks" / modid
        if not bdir.exists():
            continue
        out_tex_dir = tier_assets_dir(repo_root, tier, modid) / "textures" / "block"
        out_tex_dir.mkdir(parents=True, exist_ok=True)
        for meta_path in sorted(bdir.glob("*/meta.json")):
            src_dir = meta_path.parent
            name = src_dir.name
            meta = json.loads(meta_path.read_text(encoding="utf-8"))
            if meta.get("composed_ore", False):
                if compose_ores_mod is None:
                    import compose_ores as compose_ores_mod  # noqa: PLC0415
                alb, nrm, spc = compose_ores_mod.compose_ore_triplet_for_tier(
                    repo_root,
                    meta["host"],
                    meta["mineral"],
                    tier,
                    materials_db=materials_db,
                    host_cache=ore_host_cache,
                    overlay_cache=ore_overlay_cache,
                )
            else:
                alb, nrm, spc = pack_labpbr_triplet(src_dir, meta, materials_db, tier)
            Image.fromarray(alb, mode="RGBA").save(out_tex_dir / f"{name}.png")
            Image.fromarray(nrm, mode="RGBA").save(out_tex_dir / f"{name}_n.png")
            Image.fromarray(spc, mode="RGBA").save(out_tex_dir / f"{name}_s.png")
            exported_blocks += 1
            manifest_entries.append({"id": f"{modid}:block/{name}", "size": tier})

            variant_count = int(meta.get("variants", 1))
            for v_idx in range(1, variant_count):
                v_dir = src_dir / f"variant_{v_idx}"
                if v_dir.exists() and (v_dir / "albedo.png").exists():
                    v_alb, v_nrm, v_spc = pack_labpbr_triplet(v_dir, meta, materials_db, tier)
                    v_name = f"{name}_v{v_idx}"
                    Image.fromarray(v_alb, mode="RGBA").save(out_tex_dir / f"{v_name}.png")
                    Image.fromarray(v_nrm, mode="RGBA").save(out_tex_dir / f"{v_name}_n.png")
                    Image.fromarray(v_spc, mode="RGBA").save(out_tex_dir / f"{v_name}_s.png")
                    exported_blocks += 1
                    manifest_entries.append({"id": f"{modid}:block/{v_name}", "size": tier})

    # 2. Items (icon size = tier // 2 per 32-art-audio.md section 3.1)
    item_size = tier // 2
    for modid in target_mods:
        idir = art_root / "items" / modid
        if not idir.exists():
            continue
        out_tex_dir = tier_assets_dir(repo_root, tier, modid) / "textures" / "item"
        out_tex_dir.mkdir(parents=True, exist_ok=True)
        for meta_path in sorted(idir.glob("*/meta.json")):
            src_dir = meta_path.parent
            name = src_dir.name
            meta = json.loads(meta_path.read_text(encoding="utf-8"))
            alb, nrm, spc = pack_labpbr_triplet(src_dir, meta, materials_db, item_size)
            Image.fromarray(alb, mode="RGBA").save(out_tex_dir / f"{name}.png")
            Image.fromarray(nrm, mode="RGBA").save(out_tex_dir / f"{name}_n.png")
            Image.fromarray(spc, mode="RGBA").save(out_tex_dir / f"{name}_s.png")
            exported_items += 1
            manifest_entries.append({"id": f"{modid}:item/{name}", "size": item_size})

    # 3. UI Nine-Slice Skins (size = tier: 64x64 at 64x, 128x128 at 128x)
    for modid in target_mods:
        sdir = art_root / "skins" / modid
        if not sdir.exists():
            continue
        for meta_path in sorted(sdir.glob("*/meta.json")):
            src_dir = meta_path.parent
            sname = src_dir.name
            meta = json.loads(meta_path.read_text(encoding="utf-8"))
            out_skin_dir = tier_assets_dir(repo_root, tier, modid) / "textures" / "gui" / "skin" / sname
            out_skin_dir.mkdir(parents=True, exist_ok=True)
            for wname in meta.get("widgets", ["frame", "button", "tab", "scrollbar", "toast"]):
                with Image.open(src_dir / f"{wname}.png") as im_w:
                    arr = downsample_rgba_lanczos(im_w, tier, tier)
                    Image.fromarray(arr, mode="RGBA").save(out_skin_dir / f"{wname}.png")
            exported_skins += 1

    # 4. Entity textures (when master exists in art-src/entities/<modid>/<ename>/textures/)
    for modid in target_mods:
        edir = art_root / "entities" / modid
        if not edir.exists():
            continue
        for geom_json in sorted(edir.glob("*/person_geometry.json")):
            ename = geom_json.parent.name
            src_tex = geom_json.parent / "textures"
            out_ent_dir = tier_assets_dir(repo_root, tier, modid) / "textures" / "entity" / ename
            out_ent_dir.mkdir(parents=True, exist_ok=True)
            mult = tier // 64
            size_targets = {
                "person_lod0_base.png": (128 * mult, 128 * mult),
                "person_lod0_hair_e0.png": (128 * mult, 128 * mult),
                "person_lod0_outfit_e0_hide.png": (128 * mult, 128 * mult),
                "person_lod0_composite_e0.png": (128 * mult, 128 * mult),
                "person_lod1_e0.png": (64 * mult, 64 * mult),
                "person_lod2_e0.png": (64 * mult, 32 * mult),
            }
            for fname, (tw, th) in size_targets.items():
                mfile = src_tex / fname
                if mfile.exists():
                    with Image.open(mfile) as im_e:
                        arr = downsample_rgba_lanczos(im_e, tw, th)
                        Image.fromarray(arr, mode="RGBA").save(out_ent_dir / fname)

    iris_count = generate_iris_mappings(repo_root, target_mods) if tier == 64 else 0
    placeholder_stats = sync_registry_placeholders(repo_root, target_mods) if tier == 64 else {}
    return {
        "tier": tier,
        "blocks": exported_blocks,
        "items": exported_items,
        "skins": exported_skins,
        "iris_files": iris_count,
        "placeholders": placeholder_stats,
        "manifest_count": len(manifest_entries),
    }


# ---------------------------------------------------------------------------
# Step 6 Comprehensive Validation (--check)
# ---------------------------------------------------------------------------

@dataclass
class ValidationReport:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    vram_128x_bytes: int = 0
    checked_pbr_sets: int = 0
    checked_models: int = 0
    checked_blockstates: int = 0
    skin_contrasts: dict[str, dict[str, float]] = field(default_factory=dict)

    @property
    def ok(self) -> bool:
        return len(self.errors) == 0


def _hex_to_rel_luminance(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2], 16) / 255.0, int(h[2:4], 16) / 255.0, int(h[4:6], 16) / 255.0

    def lin(c: float) -> float:
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

    return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)


def wcag_contrast_ratio(hex_a: str, hex_b: str) -> float:
    la, lb = _hex_to_rel_luminance(hex_a), _hex_to_rel_luminance(hex_b)
    lighter, darker = max(la, lb), min(la, lb)
    return (lighter + 0.05) / (darker + 0.05)


def estimate_pbr_vram_bytes(width: int, height: int, maps: int = 3) -> int:
    """32-art-audio.md section 3.1: w * h * 4 bytes * 3 maps * 1.33 mipmap."""
    return int(round(width * height * 4 * maps * 1.33))


def validate_workspace(repo_root: Path, target_mods: list[str]) -> ValidationReport:
    report = ValidationReport()

    # 0. Validate materials.json has all 33 materials
    mat_file = repo_root / "art-src" / "materials.json"
    if not mat_file.exists():
        report.errors.append("Missing art-src/materials.json")
    else:
        mats = json.loads(mat_file.read_text(encoding="utf-8")).get("materials", {})
        if len(mats) < 33:
            report.errors.append(f"art-src/materials.json has {len(mats)} materials, expected >= 33")

    # Collect all model JSONs & resolve texture references across all 18 mods
    referenced_textures: set[tuple[str, str]] = set()  # (modid, 'block/foo' or 'item/bar')
    all_models: dict[tuple[str, str], dict[str, Any]] = {}  # (modid, 'block/foo') -> json

    for modid in MODULES:
        assets = mod_assets_dir(repo_root, modid)
        models_dir = assets / "models"
        if not models_dir.exists():
            continue
        for mpath in models_dir.rglob("*.json"):
            rel = mpath.relative_to(models_dir).with_suffix("").as_posix()
            data = json.loads(mpath.read_text(encoding="utf-8"))
            all_models[(modid, rel)] = data
            if rel.startswith("block/") and "elements" in data:
                elem_cnt = len(data["elements"])
                if elem_cnt > 64:
                    report.errors.append(f"Model {modid}:{rel} has {elem_cnt} elements (> 64 limit)")

    # Validate parent references & collect resolved texture references
    vanilla_builtin_parents = {
        "block/block",
        "block/cube",
        "block/cube_all",
        "item/generated",
        "builtin/generated",
    }
    for (modid, rel), data in all_models.items():
        if modid not in target_mods:
            continue
        report.checked_models += 1
        parent = data.get("parent")
        if parent:
            p_ns, p_rel = parent.split(":", 1) if ":" in parent else ("minecraft", parent)
            if p_ns == "minecraft":
                if p_rel not in vanilla_builtin_parents:
                    report.errors.append(f"Model {modid}:{rel} references unknown vanilla parent {parent}")
            else:
                if (p_ns, p_rel) not in all_models:
                    report.errors.append(f"Model {modid}:{rel} references missing parent model {parent}")

        for tval in data.get("textures", {}).values():
            if isinstance(tval, str) and not tval.startswith("#"):
                t_ns, t_rel = tval.split(":", 1) if ":" in tval else (modid, tval)
                if t_ns != "minecraft":
                    referenced_textures.add((t_ns, t_rel))

    # Check blockstates -> models
    for modid in target_mods:
        bs_dir = mod_assets_dir(repo_root, modid) / "blockstates"
        if not bs_dir.exists():
            continue
        for bspath in bs_dir.glob("*.json"):
            report.checked_blockstates += 1
            bs_data = json.loads(bspath.read_text(encoding="utf-8"))
            variants = bs_data.get("variants", {})
            for v_spec in variants.values():
                entries = v_spec if isinstance(v_spec, list) else [v_spec]
                for entry in entries:
                    mref = entry.get("model", "")
                    m_ns, m_rel = mref.split(":", 1) if ":" in mref else (modid, mref)
                    if m_ns != "minecraft" and (m_ns, m_rel) not in all_models:
                        report.errors.append(f"Blockstate {bspath} references missing model {mref}")

    # Check RM-P8 registry coverage against 36-registry-index.md
    registry = parse_registry_blocks_and_items(repo_root)
    for modid in target_mods:
        if modid not in registry:
            continue
        assets = mod_assets_dir(repo_root, modid)
        for bname in registry[modid]["block"]:
            if not (assets / "blockstates" / f"{bname}.json").exists():
                report.errors.append(f"Missing RM-P8 blockstate for {modid}:{bname}")
        for iname in registry[modid]["item"]:
            if not (assets / "models" / "item" / f"{iname}.json").exists():
                report.errors.append(f"Missing RM-P8 item model for {modid}:{iname}")

    # Check missing textures (referenced -> exists on disk at 64x)
    for t_ns, t_rel in sorted(referenced_textures):
        if t_ns not in target_mods and t_ns != "tcivilization":
            continue
        tex_file = mod_assets_dir(repo_root, t_ns) / "textures" / f"{t_rel}.png"
        if not tex_file.exists():
            report.errors.append(f"Missing referenced texture: {t_ns}:textures/{t_rel}.png")

    # Check all 64x & 128x PBR triplets (PoT, _n/_s size match, _n alpha >= 1, no unreferenced block/item textures)
    for tier in (64, 128):
        if tier == 128:
            mcmeta = repo_root / "resourcepacks" / "tc-pbr-128x" / "pack.mcmeta"
            if not mcmeta.exists():
                report.errors.append("Missing resourcepacks/tc-pbr-128x/pack.mcmeta")

        for modid in target_mods:
            tex_root = tier_assets_dir(repo_root, tier, modid) / "textures"
            if not tex_root.exists():
                continue

            for sub in ("block", "item"):
                sdir = tex_root / sub
                if not sdir.exists():
                    continue
                base_pngs = [
                    p for p in sorted(sdir.glob("*.png"))
                    if not p.stem.endswith("_n") and not p.stem.endswith("_s")
                ]
                for alb_p in base_pngs:
                    rel_id = f"{sub}/{alb_p.stem}"
                    if tier == 64 and (modid, rel_id) not in referenced_textures:
                        report.errors.append(f"Unreferenced {tier}x texture: {modid}:{rel_id}.png")

                    n_p = sdir / f"{alb_p.stem}_n.png"
                    s_p = sdir / f"{alb_p.stem}_s.png"
                    if not n_p.exists():
                        report.errors.append(f"Missing LabPBR normal map: {n_p}")
                        continue
                    if not s_p.exists():
                        report.errors.append(f"Missing LabPBR specular map: {s_p}")
                        continue

                    with Image.open(alb_p) as ia, Image.open(n_p) as inrm, Image.open(s_p) as ispc:
                        wa, ha = ia.size
                        if not (is_power_of_two(wa) and is_power_of_two(ha)):
                            report.errors.append(f"Non-PoT texture size {wa}x{ha}: {alb_p}")
                        if inrm.size != (wa, ha):
                            report.errors.append(f"Size mismatch _n {inrm.size} vs albedo {(wa, ha)}: {n_p}")
                        if ispc.size != (wa, ha):
                            report.errors.append(f"Size mismatch _s {ispc.size} vs albedo {(wa, ha)}: {s_p}")

                        n_arr = np.array(inrm.convert("RGBA"), dtype=np.uint8)
                        if int(n_arr[:, :, 3].min()) < 1:
                            report.errors.append(f"Height alpha channel contains 0 (< 1 forbidden): {n_p}")

                    report.checked_pbr_sets += 1
                    if tier == 128:
                        report.vram_128x_bytes += estimate_pbr_vram_bytes(wa, ha, maps=3)

            # Also check GUI & entity PNGs for PoT dimensions and count 128x VRAM
            for sub in ("gui", "entity"):
                sdir = tex_root / sub
                if not sdir.exists():
                    continue
                for png_p in sorted(sdir.rglob("*.png")):
                    with Image.open(png_p) as im:
                        w, h = im.size
                        if not (is_power_of_two(w) and is_power_of_two(h)):
                            report.errors.append(f"Non-PoT {sub} texture {w}x{h}: {png_p}")
                        if tier == 128:
                            report.vram_128x_bytes += estimate_pbr_vram_bytes(w, h, maps=1)

    if report.vram_128x_bytes > VRAM_BUDGET_BYTES:
        report.errors.append(
            f"128x VRAM estimate {report.vram_128x_bytes} bytes exceeds 1.5 GiB budget ({VRAM_BUDGET_BYTES})"
        )

    # Check UI skin contrast >= 4.5:1 (32 section 5.1 & 31 section 10.2)
    for meta_path in sorted((repo_root / "art-src" / "skins").glob("*/*/meta.json")):
        smeta = json.loads(meta_path.read_text(encoding="utf-8"))
        sid = smeta["id"]
        pal = smeta["palette"]
        c_bg = wcag_contrast_ratio(pal["text"], pal["background"])
        c_panel = wcag_contrast_ratio(pal["text"], pal["panel"])
        c_warn = wcag_contrast_ratio(pal["warning"], pal["panel"])
        c_crit = wcag_contrast_ratio(pal["critical"], pal["panel"])
        report.skin_contrasts[sid] = {
            "text_vs_background": round(c_bg, 2),
            "text_vs_panel": round(c_panel, 2),
            "warning_vs_panel": round(c_warn, 2),
            "critical_vs_panel": round(c_crit, 2),
        }
        if c_bg < 4.5 or c_panel < 4.5:
            report.errors.append(f"Skin {sid} failed text contrast >= 4.5:1 (bg={c_bg:.2f}, panel={c_panel:.2f})")
        if c_warn < 3.0 or c_crit < 3.0:
            report.errors.append(f"Skin {sid} failed semantic contrast >= 3.0:1 (warn={c_warn:.2f}, crit={c_crit:.2f})")

    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="SuperMC LabPBR 1.3 Art Export & Validation Pipeline.")
    parser.add_argument("--tier", type=int, choices=TIERS, help="Resolution tier to export (64, 128, 256, 512)")
    parser.add_argument("--mod", type=str, default="all", help="Target modid or 'all'")
    parser.add_argument("--check", action="store_true", help="Run step 6 integrity, reference, contrast & VRAM checks")
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args(argv)

    repo_root = args.repo_root.resolve()
    if args.mod == "all":
        target_mods = list(MODULES)
    else:
        if args.mod not in MODULES:
            print(f"ERROR: Unknown mod '{args.mod}'. Expected 'all' or one of {MODULES}", file=sys.stderr)
            return 2
        target_mods = [args.mod]

    if args.tier is None and not args.check:
        parser.error("Specify --tier <64|128|256|512> and/or --check")

    if args.tier is not None:
        summary = export_tier(repo_root, args.tier, target_mods)
        print(
            f"[export] tier={summary['tier']}x blocks={summary['blocks']} "
            f"items={summary['items']} skins={summary['skins']} iris={summary['iris_files']}"
        )
        if summary["placeholders"]:
            ph = summary["placeholders"]
            print(
                f"[placeholders] blockstates_new={ph['blockstates_written']} "
                f"block_items_new={ph['block_items_written']} items_new={ph['items_written']} "
                f"modeled_kept={ph['preserved_modeled']}"
            )

    if args.check:
        rep = validate_workspace(repo_root, target_mods)
        vram_mb = rep.vram_128x_bytes / (1024 * 1024)
        print(
            f"[check] pbr_sets={rep.checked_pbr_sets} models={rep.checked_models} "
            f"blockstates={rep.checked_blockstates} vram_128x={vram_mb:.2f} MiB / 1536.00 MiB"
        )
        for sid, ratios in rep.skin_contrasts.items():
            print(f"[check] skin {sid}: {ratios}")
        if not rep.ok:
            for err in rep.errors:
                print(f"ERROR: {err}", file=sys.stderr)
            return 1
        print("[check] ALL CHECKS PASSED (0 errors).")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
