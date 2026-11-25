#!/usr/bin/env python3
"""LabPBR 1.3 Ore Texture Composition Pipeline (docs/design/32-art-audio.md section 3.7).

Strictly parses legal <host_rock>_<mineral>_ore IDs from WildlandsRegistries.java and
composes LabPBR 1.3 triplets (<ore>.png, <ore>_n.png, <ore>_s.png) independently at
each target resolution tier (64x, 128x, 256x, 512x) without pre-compositing blur:
  - albedo: alpha-over blend of host rock albedo + mineral overlay albedo (AR-2 S <= 0.45)
  - _n: mineral overlay height raised +10-20% over host rock, OpenGL Y+ tangent-space
    normals recomputed from combined surface + protrusion bevel gradient, contact crevice AO,
    height clamped >= 1
  - _s: discrete mineral S, F0 (metal presets 230..255 preserved without linear blur),
    porosity/SSS B channel, and non-emissive A=255
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import export as art_export  # noqa: E402

MODID = "tcwildlands"

KNOWN_HOSTS: tuple[str, ...] = (
    "basalt_rock",
    "sandstone_rock",
    "gypsum_rock",
    "rock_salt",
    "conglomerate",
    "limestone",
    "pegmatite",
    "peridotite",
    "kimberlite",
    "quartzite",
    "dolomite",
    "mudstone",
    "eclogite",
    "rhyolite",
    "laterite",
    "andesite",
    "diorite",
    "granite",
    "gabbro",
    "gneiss",
    "marble",
    "schist",
    "gravel",
    "shale",
    "slate",
    "chalk",
    "peat",
    "tuff",
    "sand",
    "mud",
)

KNOWN_MINERALS: tuple[str, ...] = (
    "native_copper",
    "native_silver",
    "native_gold",
    "chalcopyrite",
    "pentlandite",
    "molybdenite",
    "phosphorite",
    "cassiterite",
    "sphalerite",
    "wolframite",
    "uraninite",
    "spodumene",
    "cobaltite",
    "malachite",
    "magnetite",
    "chromite",
    "ilmenite",
    "monazite",
    "cinnabar",
    "calamine",
    "bog_iron",
    "hematite",
    "graphite",
    "amethyst",
    "sapphire",
    "bauxite",
    "sylvite",
    "diamond",
    "emerald",
    "galena",
    "rutile",
    "sulfur",
    "niter",
    "flint",
    "ruby",
    "jade",
    "lapis",
)

BLOCK_PATHS_BLOCK_RE = re.compile(
    r"BLOCK_PATHS\s*=\s*List\.of\((.*?)\);",
    re.DOTALL,
)
QUOTED_ID_RE = re.compile(r'"([a-z0-9_]+)"')


def parse_legal_ores_from_registries(repo_root: Path) -> list[tuple[str, str, str]]:
    """Return ordered list of (ore_block_id, host_id, mineral_id) from WildlandsRegistries.java."""
    reg_java = (
        repo_root
        / "tcwildlands"
        / "src"
        / "main"
        / "java"
        / "com"
        / "nightcity"
        / "wildlands"
        / "registry"
        / "WildlandsRegistries.java"
    )
    content = reg_java.read_text(encoding="utf-8")
    m = BLOCK_PATHS_BLOCK_RE.search(content)
    if not m:
        raise RuntimeError(f"Could not locate BLOCK_PATHS in {reg_java}")

    block_ids = QUOTED_ID_RE.findall(m.group(1))
    ore_ids = [bid for bid in block_ids if bid.endswith("_ore")]

    resolved: list[tuple[str, str, str]] = []
    host_set = set(KNOWN_HOSTS)
    mineral_set = set(KNOWN_MINERALS)

    for ore_id in ore_ids:
        stem = ore_id[: -len("_ore")]
        matches: list[tuple[str, str]] = []
        for host in KNOWN_HOSTS:
            prefix = f"{host}_"
            if stem.startswith(prefix):
                mineral = stem[len(prefix) :]
                if mineral in mineral_set and host in host_set:
                    matches.append((host, mineral))
        if len(matches) != 1:
            raise ValueError(f"Ambiguous or unknown <host>_<mineral>_ore decomposition for '{ore_id}': {matches}")
        resolved.append((ore_id, matches[0][0], matches[0][1]))

    return resolved


def resolve_host_master_dir(repo_root: Path, host: str) -> Path:
    """Locate PBR master directory for either a tcwildlands block host or a vanilla host_bases entry."""
    block_dir = repo_root / "art-src" / "blocks" / MODID / host
    if (block_dir / "albedo.png").exists():
        return block_dir
    host_base_dir = repo_root / "art-src" / "host_bases" / host
    if (host_base_dir / "albedo.png").exists():
        return host_base_dir
    raise FileNotFoundError(f"Missing host rock PBR master for '{host}' (checked {block_dir} and {host_base_dir})")


def _downsample_overlay_rgba_soft(img: Image.Image, tier: int) -> tuple[np.ndarray, np.ndarray]:
    """Premultiplied Lanczos downsample returning unpremultiplied RGB [0..255] float32 and soft alpha [0..1]."""
    rgba = img.convert("RGBA")
    arr = np.array(rgba, dtype=np.float32)
    if rgba.size != (tier, tier):
        alpha_src = arr[:, :, 3:4] / 255.0
        pre = np.concatenate([arr[:, :, :3] * alpha_src, arr[:, :, 3:4]], axis=2)
        pre_img = Image.fromarray(np.clip(np.round(pre), 0, 255).astype(np.uint8), mode="RGBA")
        ds = np.array(pre_img.resize((tier, tier), resample=Image.Resampling.LANCZOS), dtype=np.float32)
    else:
        ds = arr

    a01 = np.clip(ds[:, :, 3] / 255.0, 0.0, 1.0)
    rgb = np.where(a01[:, :, None] > 1e-3, ds[:, :, :3] / np.maximum(a01[:, :, None], 1e-3), 0.0)
    rgb = np.clip(rgb, 0.0, 255.0)
    soft_alpha = np.clip((a01 - 0.14) / 0.56, 0.0, 1.0)
    return rgb, soft_alpha


def compose_ore_triplet_for_tier(
    repo_root: Path,
    host: str,
    mineral: str,
    tier: int,
    materials_db: dict[str, dict[str, Any]] | None = None,
    host_cache: dict[tuple[str, int], tuple[np.ndarray, np.ndarray, np.ndarray]] | None = None,
    overlay_cache: dict[tuple[str, int], dict[str, Any]] | None = None,
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Independently downsample host & mineral overlay to `tier` and synthesize LabPBR 1.3 triplet."""
    if materials_db is None:
        materials_db = art_export.load_materials_library(repo_root)

    h_key = (host, tier)
    if host_cache is not None and h_key in host_cache:
        host_alb, host_nrm, host_spc = host_cache[h_key]
    else:
        host_dir = resolve_host_master_dir(repo_root, host)
        host_meta = json.loads((host_dir / "meta.json").read_text(encoding="utf-8"))
        host_alb, host_nrm, host_spc = art_export.pack_labpbr_triplet(host_dir, host_meta, materials_db, tier)
        if host_cache is not None:
            host_cache[h_key] = (host_alb, host_nrm, host_spc)

    o_key = (mineral, tier)
    if overlay_cache is not None and o_key in overlay_cache:
        ov = overlay_cache[o_key]
    else:
        ov_dir = repo_root / "art-src" / "ore_overlays" / mineral
        if not ov_dir.exists():
            raise FileNotFoundError(f"Missing mineral overlay master directory: {ov_dir}")
        ov_meta = json.loads((ov_dir / "meta.json").read_text(encoding="utf-8"))
        mat_key = ov_meta.get("material", mineral)
        mat_info = materials_db[mat_key]

        with Image.open(ov_dir / "albedo.png") as im_a:
            ov_rgb, ov_alpha = _downsample_overlay_rgba_soft(im_a, tier)
        with Image.open(ov_dir / "normal.png") as im_n:
            ov_nx_u8, ov_ny_u8 = art_export.downsample_and_renormalize_normal(im_n, tier, tier)
        with Image.open(ov_dir / "height.png") as im_h:
            ov_h_01 = art_export.downsample_scalar_lanczos(im_h, tier, tier).astype(np.float32) / 255.0
        with Image.open(ov_dir / "ao.png") as im_ao:
            ov_ao_01 = art_export.downsample_scalar_lanczos(im_ao, tier, tier).astype(np.float32) / 255.0
        with Image.open(ov_dir / "roughness.png") as im_r:
            ov_r_01 = art_export.downsample_scalar_lanczos(im_r, tier, tier).astype(np.float32) / 255.0
            ov_smooth_u8 = art_export.roughness_to_smoothness_u8(ov_r_01)

        ov = {
            "rgb": ov_rgb,
            "alpha": ov_alpha,
            "nx_u8": ov_nx_u8,
            "ny_u8": ov_ny_u8,
            "h_01": ov_h_01,
            "ao_01": ov_ao_01,
            "smooth_u8": ov_smooth_u8,
            "F0": int(mat_info["F0"]),
            "b_channel": int(mat_info.get("b_channel", mat_info.get("P", mat_info.get("SSS", 0)))),
        }
        if overlay_cache is not None:
            overlay_cache[o_key] = ov

    alpha = ov["alpha"]
    a3 = alpha[:, :, None]

    # 1. Albedo composition: alpha-over blend + AR-2 mean HSV saturation guard (<= 0.42 <= 0.45) + opaque A=255
    comp_f = np.clip(host_alb[:, :, :3].astype(np.float32) * (1.0 - a3) + ov["rgb"] * a3, 0.0, 255.0)
    cmax = np.max(comp_f, axis=2)
    cmin = np.min(comp_f, axis=2)
    hsv_s = np.where(cmax > 1e-3, (cmax - cmin) / np.maximum(cmax, 1e-3), 0.0)
    mean_s = float(np.mean(hsv_s))
    if mean_s > 0.42:
        lum = 0.299 * comp_f[:, :, 0:1] + 0.587 * comp_f[:, :, 1:2] + 0.114 * comp_f[:, :, 2:3]
        comp_f = np.clip(lum + (comp_f - lum) * (0.42 / mean_s), 0.0, 255.0)
    comp_rgb = np.clip(np.round(comp_f), 0, 255).astype(np.uint8)
    albedo_out = np.concatenate([comp_rgb, np.full((tier, tier, 1), 255, dtype=np.uint8)], axis=2)

    # 2. Height protrusion (+10% to +20% above host rock inside mineral grains) & OpenGL Y+ normal recomputation
    host_h_01 = host_nrm[:, :, 3].astype(np.float32) / 255.0
    protrusion = alpha * (0.11 + 0.08 * ov["h_01"])
    comp_h_01 = np.clip(host_h_01 + protrusion, 1.0 / 255.0, 1.0)
    height_u8 = np.clip(np.round(comp_h_01 * 255.0), 1, 255).astype(np.uint8)

    dh_dx = (np.roll(protrusion, -1, axis=1) - np.roll(protrusion, 1, axis=1)) * 0.5
    dh_dy_up = (np.roll(protrusion, 1, axis=0) - np.roll(protrusion, -1, axis=0)) * 0.5

    hx = (host_nrm[:, :, 0].astype(np.float32) / 255.0) * 2.0 - 1.0
    hy = (host_nrm[:, :, 1].astype(np.float32) / 255.0) * 2.0 - 1.0
    hz = np.sqrt(np.maximum(1.0 - hx * hx - hy * hy, 1e-4))

    ox = (ov["nx_u8"].astype(np.float32) / 255.0) * 2.0 - 1.0
    oy = (ov["ny_u8"].astype(np.float32) / 255.0) * 2.0 - 1.0
    oz = np.sqrt(np.maximum(1.0 - ox * ox - oy * oy, 1e-4))

    bevel_strength = 2.6
    nx = hx * (1.0 - alpha) + ox * alpha - bevel_strength * dh_dx
    ny = hy * (1.0 - alpha) + oy * alpha - bevel_strength * dh_dy_up
    nz = np.maximum(hz * (1.0 - alpha) + oz * alpha, 0.15)
    n_len = np.sqrt(nx * nx + ny * ny + nz * nz)
    nx /= np.maximum(n_len, 1e-6)
    ny /= np.maximum(n_len, 1e-6)

    nx_u8 = np.clip(np.round((nx * 0.5 + 0.5) * 255.0), 0, 255).astype(np.uint8)
    ny_u8 = np.clip(np.round((ny * 0.5 + 0.5) * 255.0), 0, 255).astype(np.uint8)

    edge_ring = 4.0 * alpha * (1.0 - alpha)
    host_ao_01 = host_nrm[:, :, 2].astype(np.float32) / 255.0
    comp_ao_01 = (host_ao_01 * (1.0 - alpha) + ov["ao_01"] * alpha) * (1.0 - 0.18 * edge_ring)
    ao_u8 = np.clip(np.round(comp_ao_01 * 255.0), 1, 255).astype(np.uint8)

    normal_out = np.stack([nx_u8, ny_u8, ao_u8, height_u8], axis=2)

    # 3. Specular (_s): discrete mineral mask preserves exact metal preset codes (230..255), A=255 non-emissive
    is_mineral = alpha >= 0.45
    smooth_u8 = np.where(is_mineral, ov["smooth_u8"], host_spc[:, :, 0]).astype(np.uint8)
    f0_u8 = np.where(is_mineral, np.uint8(ov["F0"]), host_spc[:, :, 1]).astype(np.uint8)
    b_u8 = np.where(is_mineral, np.uint8(np.clip(ov["b_channel"], 0, 255)), host_spc[:, :, 2]).astype(np.uint8)
    emissive_u8 = np.full((tier, tier), 255, dtype=np.uint8)
    specular_out = np.stack([smooth_u8, f0_u8, b_u8, emissive_u8], axis=2)

    return albedo_out, normal_out, specular_out


def ensure_ore_models_and_meta(repo_root: Path, legal_ores: list[tuple[str, str, str]]) -> None:
    """Ensure block/item models, weighted rotational blockstates, and art-src meta.json exist for all legal ores."""
    assets_dir = art_export.mod_assets_dir(repo_root, MODID)
    bs_dir = assets_dir / "blockstates"
    bm_dir = assets_dir / "models" / "block"
    im_dir = assets_dir / "models" / "item"
    bs_dir.mkdir(parents=True, exist_ok=True)
    bm_dir.mkdir(parents=True, exist_ok=True)
    im_dir.mkdir(parents=True, exist_ok=True)

    for ore_id, host, mineral in legal_ores:
        meta_dir = repo_root / "art-src" / "blocks" / MODID / ore_id
        meta_dir.mkdir(parents=True, exist_ok=True)
        meta_obj = {
            "schema_version": 1,
            "kind": "block",
            "id": f"{MODID}:{ore_id}",
            "material": mineral,
            "host": host,
            "mineral": mineral,
            "composed_ore": True,
            "variants": 1,
            "emissive": False,
            "iris_category": "solid",
            "master_resolution": 512,
            "description": f"Composed LabPBR 1.3 ore ({host} host + {mineral} overlay; 32-art-audio.md#3.7)",
        }
        (meta_dir / "meta.json").write_text(json.dumps(meta_obj, indent=2) + "\n", encoding="utf-8")

        bm_obj = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"{MODID}:block/{ore_id}"},
        }
        (bm_dir / f"{ore_id}.json").write_text(json.dumps(bm_obj, indent=2) + "\n", encoding="utf-8")

        bs_obj = {
            "variants": {
                "": [
                    {"model": f"{MODID}:block/{ore_id}", "y": 0, "weight": 4},
                    {"model": f"{MODID}:block/{ore_id}", "y": 90, "weight": 2},
                    {"model": f"{MODID}:block/{ore_id}", "y": 180, "weight": 3},
                    {"model": f"{MODID}:block/{ore_id}", "y": 270, "weight": 2},
                ]
            }
        }
        (bs_dir / f"{ore_id}.json").write_text(json.dumps(bs_obj, indent=2) + "\n", encoding="utf-8")

        im_obj = {"parent": f"{MODID}:block/{ore_id}"}
        (im_dir / f"{ore_id}.json").write_text(json.dumps(im_obj, indent=2) + "\n", encoding="utf-8")


def compose_all_ores(repo_root: Path, tiers: list[int]) -> dict[str, Any]:
    """Compose all legal WildlandsRegistries ores across requested tiers."""
    legal_ores = parse_legal_ores_from_registries(repo_root)
    materials_db = art_export.load_materials_library(repo_root)
    ensure_ore_models_and_meta(repo_root, legal_ores)

    legal_ore_names = {ore_id for ore_id, _, _ in legal_ores}

    for tier in tiers:
        if tier not in art_export.TIERS:
            raise ValueError(f"Unsupported tier {tier}; expected one of {art_export.TIERS}")
        if tier > 64:
            art_export.write_pack_mcmeta(repo_root / "resourcepacks" / f"tc-pbr-{tier}x", tier)

        out_tex_dir = art_export.tier_assets_dir(repo_root, tier, MODID) / "textures" / "block"
        out_tex_dir.mkdir(parents=True, exist_ok=True)

        for existing_png in out_tex_dir.glob("*_ore*.png"):
            stem = existing_png.stem
            if stem.endswith("_n") or stem.endswith("_s"):
                stem = stem[:-2]
            if stem.endswith("_ore") and stem not in legal_ore_names:
                existing_png.unlink()

        host_cache: dict[tuple[str, int], tuple[np.ndarray, np.ndarray, np.ndarray]] = {}
        overlay_cache: dict[tuple[str, int], dict[str, Any]] = {}

        for ore_id, host, mineral in legal_ores:
            alb, nrm, spc = compose_ore_triplet_for_tier(
                repo_root,
                host,
                mineral,
                tier,
                materials_db=materials_db,
                host_cache=host_cache,
                overlay_cache=overlay_cache,
            )
            Image.fromarray(alb, mode="RGBA").save(out_tex_dir / f"{ore_id}.png")
            Image.fromarray(nrm, mode="RGBA").save(out_tex_dir / f"{ore_id}_n.png")
            Image.fromarray(spc, mode="RGBA").save(out_tex_dir / f"{ore_id}_s.png")

    return {"legal_ore_count": len(legal_ores), "tiers": list(tiers)}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Compose legal Wildlands LabPBR 1.3 ore textures per tier.")
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--tiers", type=int, nargs="+", default=[64, 128], choices=art_export.TIERS)
    args = parser.parse_args(argv)

    summary = compose_all_ores(args.repo_root.resolve(), args.tiers)
    print(f"[compose_ores] composed={summary['legal_ore_count']} legal ores across tiers={summary['tiers']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
