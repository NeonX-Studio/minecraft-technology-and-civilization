#!/usr/bin/env python3
r"""Headless Blender 3D Geological Modeling & Seamless Cycles PBR Master Baker for P1 Wildlands.

Produces:
  1. 3D cuboid non-full-cube geological & mine support blocks (<= 64 elements each) in Blender,
     exporting .blend scenes + vanilla JSON models & weighted rotational blockstates.
  2. Seamless 4D Clifford-torus Cycles procedural PBR masters (albedo, normal, height, ao,
     roughness[, f0, porosity, emissive] + meta.json) for:
     - All 22 custom rocks x 4 forms (natural rocks have 2 variants + y=0/90/180/270 blockstates)
     - 10 vanilla rock missing forms (cobble, bricks, polished)
     - 7 vanilla host_bases (granite, diorite, andesite, tuff, gravel, sand, mud)
     - 16 soils & sediments (2 variants + y=0/90/180/270 blockstates)
     - 3 coal seams + 3 special full-cube geological blocks (lava_crust, massive_sulfide, salt_crust)
     - All 37 mineral ore overlays in art-src/ore_overlays/<mineral>/
     - All 46 standalone 3D-relief item icons in art-src/items/tcwildlands/<item>/

Invoke:
  & 'F:\Programs\Blender\blender.exe' -b --factory-startup -P tools\art-export\blender\build_p1_wildlands_assets.py -- --repo-root I:\Projects\SuperMC
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path
from typing import Any

import addon_utils
import bmesh
import bpy
import mathutils
import numpy as np

THIS_DIR = Path(__file__).resolve().parent
if str(THIS_DIR) not in sys.path:
    sys.path.insert(0, str(THIS_DIR))

import build_p0_assets as p0  # noqa: E402
import p1_wildlands_specs as specs  # noqa: E402

MODID = "tcwildlands"
TWO_PI = 2.0 * math.pi


def _parse_args() -> argparse.Namespace:
    argv = sys.argv
    argv = argv[argv.index("--") + 1 :] if "--" in argv else []
    parser = argparse.ArgumentParser(description="Build P1 Wildlands 3D models & PBR masters in Blender.")
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--tile-res", type=int, default=512)
    parser.add_argument("--skip-items", action="store_true", help="Skip standalone item baking when already up to date")
    parser.add_argument("--skip-overlays", action="store_true", help="Skip mineral overlay baking when already up to date")
    return parser.parse_args(argv)


def _seed_vec4(seed_key: str) -> tuple[float, float, float, float]:
    h = hashlib.sha256(seed_key.encode("utf-8")).digest()
    vals = [int.from_bytes(h[i : i + 4], "little") / 4294967295.0 for i in range(0, 16, 4)]
    return (vals[0] * 17.3, vals[1] * 19.7, vals[2] * 23.1, vals[3] * 29.9)


def calibrate_albedo_linear(
    srgb_rgba: np.ndarray,
    target_brightness: float | None = None,
    min_brightness: float | None = None,
    max_brightness: float | None = None,
    max_mean_hsv_s: float = 0.42,
) -> np.ndarray:
    """Calibrate sRGB brightness & enforce AR-2 mean HSV saturation <= 0.45 on Blender byte-buffer sRGB RGBA."""
    out = srgb_rgba.copy()
    srgb = np.clip(out[:, :, :3], 0.0, 1.0)

    # 1. Enforce AR-2 saturation restraint (mean HSV S <= max_mean_hsv_s <= 0.45)
    cmax = np.max(srgb, axis=2)
    cmin = np.min(srgb, axis=2)
    hsv_s = np.where(cmax > 1e-5, (cmax - cmin) / np.maximum(cmax, 1e-5), 0.0)
    mean_s = float(np.mean(hsv_s))
    if mean_s > max_mean_hsv_s and mean_s > 1e-5:
        factor = max_mean_hsv_s / mean_s
        lum = 0.299 * srgb[:, :, 0:1] + 0.587 * srgb[:, :, 1:2] + 0.114 * srgb[:, :, 2:3]
        srgb = np.clip(lum + (srgb - lum) * factor, 0.0, 1.0)

    # 2. Calibrate mean sRGB brightness into geological target / [0.25, 0.55] natural rock window
    cur_mean = float(np.mean(srgb))
    desired = target_brightness
    if desired is None:
        if min_brightness is not None and cur_mean < min_brightness:
            desired = min_brightness
        elif max_brightness is not None and cur_mean > max_brightness:
            desired = max_brightness

    if desired is not None and cur_mean > 1e-5:
        desired_clamped = max(0.05, min(0.92, desired))
        gamma = math.log(desired_clamped) / math.log(max(0.02, min(0.98, cur_mean)))
        gamma = max(0.45, min(2.4, gamma))
        srgb = np.clip(np.power(np.maximum(srgb, 1e-5), gamma), 0.02, 0.96)
        post_mean = float(np.mean(srgb))
        if post_mean > 1e-5:
            srgb = np.clip(srgb * (desired_clamped / post_mean), 0.02, 0.96)

    # Re-verify saturation after gamma trim
    cmax2 = np.max(srgb, axis=2)
    cmin2 = np.min(srgb, axis=2)
    hsv_s2 = np.where(cmax2 > 1e-5, (cmax2 - cmin2) / np.maximum(cmax2, 1e-5), 0.0)
    mean_s2 = float(np.mean(hsv_s2))
    if mean_s2 > max_mean_hsv_s and mean_s2 > 1e-5:
        factor2 = max_mean_hsv_s / mean_s2
        lum2 = 0.299 * srgb[:, :, 0:1] + 0.587 * srgb[:, :, 1:2] + 0.114 * srgb[:, :, 2:3]
        srgb = np.clip(lum2 + (srgb - lum2) * factor2, 0.0, 1.0)

    out[:, :, :3] = srgb
    out[:, :, 3] = 1.0
    return out


# ---------------------------------------------------------------------------
# 1. Non-Full-Cube 3D Geological & Mine Block Modeling
# ---------------------------------------------------------------------------

def _write_cuboid_variant_model_json(
    modid: str,
    model_name: str,
    texture_ref: str,
    elements_spec: list[dict[str, Any]],
    res_root: Path,
) -> int:
    assert 1 <= len(elements_spec) <= 64, f"{modid}:{model_name} exceeds 64 elements"
    json_elements: list[dict[str, Any]] = []

    def sub_uv(q_idx: int, u_start: float, v_start: float, u_span: float, v_span: float) -> list[float]:
        qu0, qv0, _, _ = p0.QUADRANTS[q_idx]
        ua = qu0 + (u_start / 16.0) * 7.5 + 0.25
        va = qv0 + (v_start / 16.0) * 7.5 + 0.25
        ub = min(qu0 + 7.75, ua + max(0.5, (u_span / 16.0) * 7.5))
        vb = min(qv0 + 7.75, va + max(0.5, (v_span / 16.0) * 7.5))
        return [round(ua, 3), round(va, 3), round(ub, 3), round(vb, 3)]

    for elem in elements_spec:
        f_mc = [float(v) for v in elem["from"]]
        t_mc = [float(v) for v in elem["to"]]
        quad_default = int(elem.get("quadrant", 0))
        face_quads: dict[str, int] = elem.get("face_quadrants", {})
        dx = max(1.0, t_mc[0] - f_mc[0])
        dy = max(1.0, t_mc[1] - f_mc[1])
        dz = max(1.0, t_mc[2] - f_mc[2])
        faces_json = {
            "north": {"uv": sub_uv(face_quads.get("north", quad_default), f_mc[0], 16.0 - t_mc[1], dx, dy), "texture": "#all"},
            "south": {"uv": sub_uv(face_quads.get("south", quad_default), f_mc[0], 16.0 - t_mc[1], dx, dy), "texture": "#all"},
            "west": {"uv": sub_uv(face_quads.get("west", quad_default), f_mc[2], 16.0 - t_mc[1], dz, dy), "texture": "#all"},
            "east": {"uv": sub_uv(face_quads.get("east", quad_default), f_mc[2], 16.0 - t_mc[1], dz, dy), "texture": "#all"},
            "up": {"uv": sub_uv(face_quads.get("up", quad_default), f_mc[0], f_mc[2], dx, dz), "texture": "#all"},
            "down": {"uv": sub_uv(face_quads.get("down", quad_default), f_mc[0], f_mc[2], dx, dz), "texture": "#all"},
        }
        el_entry: dict[str, Any] = {
            "from": [round(v, 3) for v in f_mc],
            "to": [round(v, 3) for v in t_mc],
            "faces": faces_json,
        }
        rot_spec = elem.get("rotation")
        if rot_spec:
            el_entry["rotation"] = {
                "origin": [round(float(v), 3) for v in rot_spec["origin"]],
                "axis": rot_spec["axis"],
                "angle": float(rot_spec["angle"]),
            }
        if int(elem.get("light_emission", 0)) > 0:
            el_entry["light_emission"] = int(elem["light_emission"])
        json_elements.append(el_entry)

    model_obj = {
        "parent": "minecraft:block/block",
        "textures": {"particle": texture_ref, "all": texture_ref},
        "elements": json_elements,
    }
    mpath = res_root / "models" / "block" / f"{model_name}.json"
    mpath.parent.mkdir(parents=True, exist_ok=True)
    mpath.write_text(json.dumps(model_obj, indent=2) + "\n", encoding="utf-8")
    return len(json_elements)


def build_all_3d_geological_blocks(
    repo_root: Path,
    materials_db: dict[str, dict[str, Any]],
    tile_res: int,
) -> int:
    res_root = p0.mod_resource_dir(repo_root, MODID)
    blend_root = repo_root / "art-src" / "blend" / "blocks"
    blocks_art_root = repo_root / "art-src" / "blocks" / MODID

    block_defs = specs.get_3d_cuboid_block_specs(materials_db)
    for bdef in block_defs:
        bname = bdef["name"]
        blend_path = blend_root / f"{MODID}_{bname}.blend"
        info = p0.build_blender_3d_cuboid_scene_and_mc_json(
            MODID,
            bname,
            bdef["elements"],
            blend_path,
            res_root,
        )

        model_refs = [f"{MODID}:block/{bname}"]
        for extra in bdef.get("extra_3d_variants", []):
            v_model_name = f"{bname}{extra['suffix']}"
            _write_cuboid_variant_model_json(
                MODID,
                v_model_name,
                extra["texture_ref"],
                extra["elements"],
                res_root,
            )
            model_refs.append(f"{MODID}:block/{v_model_name}")

        # Write weighted y-rotated blockstate when applicable (e.g. 4 models x 4 rotations = 16 for loose_stones)
        if bdef.get("rotate_y", False) or len(model_refs) > 1:
            entries: list[dict[str, Any]] = []
            weights = (4, 2, 3, 2)
            for mref in model_refs:
                for y_deg, w in zip((0, 90, 180, 270), weights):
                    entries.append({"model": mref, "y": y_deg, "weight": w})
            bs_path = res_root / "blockstates" / f"{bname}.json"
            bs_path.write_text(json.dumps({"variants": {"": entries}}, indent=2) + "\n", encoding="utf-8")

        out_dir = blocks_art_root / bname
        p0.bake_block_pbr_maps(f"{MODID}_{bname}", bdef["quads"], tile_res, out_dir)
        # Enforce AR-2 saturation constraint on baked 3D block atlas
        alb_img = bpy.data.images.load(str(out_dir / "albedo.png"))
        alb_img.colorspace_settings.name = "sRGB"
        alb_arr = np.array(alb_img.pixels[:], dtype=np.float32).reshape((tile_res, tile_res, 4))
        bpy.data.images.remove(alb_img)
        cal_alb = calibrate_albedo_linear(alb_arr, max_mean_hsv_s=0.40)
        p0.save_float_rgba_png(cal_alb, out_dir / "albedo.png", is_srgb=True)

        if int(bdef.get("variants", 1)) > 1:
            # Bake second stone lithology atlas for loose_stones_v1
            v1_quads = [
                {"albedo": [78, 80, 84], "S": 40, "F0": 10, "relief": "stone"},
                {"albedo": [176, 168, 152], "S": 38, "F0": 10, "relief": "stone"},
                {"albedo": [122, 108, 92], "S": 32, "F0": 10, "relief": "stone"},
                {"albedo": [148, 142, 134], "S": 58, "F0": 10, "relief": "stone"},
            ]
            v1_dir = out_dir / "variant_1"
            p0.bake_block_pbr_maps(f"{MODID}_{bname}_v1", v1_quads, tile_res, v1_dir)

        meta_obj = {
            "schema_version": 1,
            "kind": "block",
            "id": f"{MODID}:{bname}",
            "material": bdef["material"],
            "variants": int(bdef.get("variants", 1)),
            "emissive": bool(bdef["emissive"]),
            "iris_category": bdef["iris_category"],
            "master_resolution": tile_res,
            "element_count": info["element_count"],
            "source_blend": str(blend_path.relative_to(repo_root)),
            "description": bdef["description"],
        }
        (out_dir / "meta.json").write_text(json.dumps(meta_obj, indent=2) + "\n", encoding="utf-8")

    return len(block_defs)


# ---------------------------------------------------------------------------
# 2. Seamless Clifford 4D Torus Cycles Node Builder & 4x4 Batch Baker
# ---------------------------------------------------------------------------

def _add_clifford_torus_4d_coords(
    nodes: bpy.types.Nodes,
    links: bpy.types.NodeLinks,
    u0: float,
    v0: float,
    grid_n: int,
    seed_key: str,
) -> tuple[bpy.types.NodeSocket, bpy.types.NodeSocket, bpy.types.NodeSocket, bpy.types.NodeSocket]:
    """Map sub-tile UV [u0..u0+1/N, v0..v0+1/N] -> local (u, v) in [0..1] -> 4D periodic torus (Vec3, W)."""
    sx, sy, sz, sw = _seed_vec4(seed_key)
    coord = nodes.new("ShaderNodeTexCoord")
    sep = nodes.new("ShaderNodeSeparateXYZ")
    links.new(coord.outputs["UV"], sep.inputs[0])

    def norm_axis(sock: bpy.types.NodeSocket, origin: float) -> bpy.types.NodeSocket:
        sub = nodes.new("ShaderNodeMath")
        sub.operation = "SUBTRACT"
        sub.inputs[1].default_value = origin
        links.new(sock, sub.inputs[0])
        mul = nodes.new("ShaderNodeMath")
        mul.operation = "MULTIPLY"
        mul.inputs[1].default_value = float(grid_n)
        links.new(sub.outputs[0], mul.inputs[0])
        return mul.outputs[0]

    lu = norm_axis(sep.outputs[0], u0)
    lv = norm_axis(sep.outputs[1], v0)

    def torus_pair(loc_sock: bpy.types.NodeSocket, off_a: float, off_b: float) -> tuple[bpy.types.NodeSocket, bpy.types.NodeSocket]:
        ang = nodes.new("ShaderNodeMath")
        ang.operation = "MULTIPLY"
        ang.inputs[1].default_value = TWO_PI
        links.new(loc_sock, ang.inputs[0])

        c_n = nodes.new("ShaderNodeMath")
        c_n.operation = "COSINE"
        links.new(ang.outputs[0], c_n.inputs[0])
        c_s = nodes.new("ShaderNodeMath")
        c_s.operation = "DIVIDE"
        c_s.inputs[1].default_value = TWO_PI
        links.new(c_n.outputs[0], c_s.inputs[0])
        c_o = nodes.new("ShaderNodeMath")
        c_o.operation = "ADD"
        c_o.inputs[1].default_value = off_a
        links.new(c_s.outputs[0], c_o.inputs[0])

        s_n = nodes.new("ShaderNodeMath")
        s_n.operation = "SINE"
        links.new(ang.outputs[0], s_n.inputs[0])
        s_s = nodes.new("ShaderNodeMath")
        s_s.operation = "DIVIDE"
        s_s.inputs[1].default_value = TWO_PI
        links.new(s_n.outputs[0], s_s.inputs[0])
        s_o = nodes.new("ShaderNodeMath")
        s_o.operation = "ADD"
        s_o.inputs[1].default_value = off_b
        links.new(s_s.outputs[0], s_o.inputs[0])
        return c_o.outputs[0], s_o.outputs[0]

    tx, ty = torus_pair(lu, sx, sy)
    tz, tw = torus_pair(lv, sz, sw)
    comb = nodes.new("ShaderNodeCombineXYZ")
    links.new(tx, comb.inputs[0])
    links.new(ty, comb.inputs[1])
    links.new(tz, comb.inputs[2])
    return lu, lv, comb.outputs[0], tw


def build_seamless_tile_shader(
    job: dict[str, Any],
    u0: float,
    v0: float,
    grid_n: int,
) -> tuple[bpy.types.Material, bpy.types.NodeSocket]:
    """Construct a seamless 4D-torus Principled BSDF material for rock, cobble, bricks, polished, soil, coal, or overlay."""
    mat = bpy.data.materials.new(name=job["key"])
    nt = mat.node_tree
    nodes = nt.nodes
    links = nt.links
    nodes.clear()

    out = nodes.new("ShaderNodeOutputMaterial")
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])

    lu, lv, vec3, w_sock = _add_clifford_torus_4d_coords(nodes, links, u0, v0, grid_n, job["seed_key"])

    n_scale = float(job.get("noise_scale", 18.0))
    fabric = job.get("fabric", "fine_massive")
    form = job.get("form", "raw")

    # Macro & micro 4D seamless noise
    n_macro = nodes.new("ShaderNodeTexNoise")
    n_macro.noise_dimensions = "4D"
    n_macro.inputs["Scale"].default_value = n_scale
    n_macro.inputs["Detail"].default_value = 6.0
    n_macro.inputs["Roughness"].default_value = 0.60
    n_macro.inputs["Distortion"].default_value = 0.35 if fabric in ("flow_banded", "gneissic_banded", "veined_metamorphic") else 0.1
    links.new(vec3, n_macro.inputs["Vector"])
    links.new(w_sock, n_macro.inputs["W"])

    n_micro = nodes.new("ShaderNodeTexNoise")
    n_micro.noise_dimensions = "4D"
    n_micro.inputs["Scale"].default_value = n_scale * 2.8
    n_micro.inputs["Detail"].default_value = 9.0
    n_micro.inputs["Roughness"].default_value = 0.72
    links.new(vec3, n_micro.inputs["Vector"])
    links.new(w_sock, n_micro.inputs["W"])

    voro = nodes.new("ShaderNodeTexVoronoi")
    voro.voronoi_dimensions = "4D"
    if form == "cobble":
        voro.feature = "DISTANCE_TO_EDGE"
        voro.inputs["Scale"].default_value = 4.6  # 3-6 cobble stones per face (32-art-audio.md#3.4)
    elif fabric in ("clastic_pebbly", "breccia_pipe", "nodular_soil", "vesicular", "salt_pan", "lava_cracks"):
        voro.feature = "DISTANCE_TO_EDGE" if fabric in ("breccia_pipe", "salt_pan", "lava_cracks") else "F1"
        voro.inputs["Scale"].default_value = n_scale * 0.55
    else:
        voro.feature = "F1"
        voro.inputs["Scale"].default_value = n_scale * 0.70
    links.new(vec3, voro.inputs["Vector"])
    links.new(w_sock, voro.inputs["W"])

    # Combine noise & voronoi into base height [0..1]
    mix_base = nodes.new("ShaderNodeMix")
    mix_base.data_type = "FLOAT"
    v_weight = 0.62 if form == "cobble" else (0.45 if fabric in ("coarse_crystalline", "giant_crystalline", "clastic_pebbly", "breccia_pipe", "salt_pan", "lava_cracks") else 0.25)
    mix_base.inputs[0].default_value = v_weight
    links.new(n_macro.outputs["Fac"], mix_base.inputs[2])
    links.new(voro.outputs["Distance"], mix_base.inputs[3])

    mix_detail = nodes.new("ShaderNodeMix")
    mix_detail.data_type = "FLOAT"
    mix_detail.inputs[0].default_value = 0.28
    links.new(mix_base.outputs[0], mix_detail.inputs[2])
    links.new(n_micro.outputs["Fac"], mix_detail.inputs[3])
    h_sock = mix_detail.outputs[0]

    # Optional periodic horizontal bedding / foliation: sin(2*pi * bands * lv + warp * n_macro)
    bands = int(job.get("bands", 0))
    if bands > 0 and form in ("raw", "seam"):
        v_ang = nodes.new("ShaderNodeMath")
        v_ang.operation = "MULTIPLY"
        v_ang.inputs[1].default_value = TWO_PI * float(bands)
        links.new(lv, v_ang.inputs[0])

        warp = nodes.new("ShaderNodeMath")
        warp.operation = "MULTIPLY"
        warp.inputs[1].default_value = 2.4
        links.new(n_macro.outputs["Fac"], warp.inputs[0])

        phase = nodes.new("ShaderNodeMath")
        phase.operation = "ADD"
        links.new(v_ang.outputs[0], phase.inputs[0])
        links.new(warp.outputs[0], phase.inputs[1])

        bed_sin = nodes.new("ShaderNodeMath")
        bed_sin.operation = "SINE"
        links.new(phase.outputs[0], bed_sin.inputs[0])

        bed_01 = nodes.new("ShaderNodeMapRange")
        bed_01.inputs["From Min"].default_value = -1.0
        bed_01.inputs["From Max"].default_value = 1.0
        bed_01.inputs["To Min"].default_value = 0.0
        bed_01.inputs["To Max"].default_value = 1.0
        links.new(bed_sin.outputs[0], bed_01.inputs["Value"])

        mix_bed = nodes.new("ShaderNodeMix")
        mix_bed.data_type = "FLOAT"
        mix_bed.inputs[0].default_value = 0.58 if form == "seam" else 0.40
        links.new(h_sock, mix_bed.inputs[2])
        links.new(bed_01.outputs["Result"], mix_bed.inputs[3])
        h_sock = mix_bed.outputs[0]

    rgb_lo = job["rgb_lo"]
    rgb_hi = job["rgb_hi"]
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].position = 0.16
    ramp.color_ramp.elements[0].color = p0.srgb_rgb255_to_linear_rgba(rgb_lo, 1.0)
    ramp.color_ramp.elements[1].position = 0.84
    ramp.color_ramp.elements[1].color = p0.srgb_rgb255_to_linear_rgba(rgb_hi, 1.0)
    links.new(h_sock, ramp.inputs["Fac"])
    links.new(ramp.outputs["Color"], bsdf.inputs["Base Color"])

    smooth_s = int(job["S"])
    base_r = p0.smoothness_to_roughness(smooth_s)
    r_span = 0.03 if form == "polished" else 0.07
    map_r = nodes.new("ShaderNodeMapRange")
    map_r.inputs["From Min"].default_value = 0.0
    map_r.inputs["From Max"].default_value = 1.0
    map_r.inputs["To Min"].default_value = max(0.02, base_r - r_span)
    map_r.inputs["To Max"].default_value = min(0.99, base_r + r_span)
    links.new(h_sock, map_r.inputs["Value"])
    links.new(map_r.outputs["Result"], bsdf.inputs["Roughness"])

    bump_str = 0.10 if form == "polished" else (0.52 if form == "cobble" else float(job.get("bump_strength", 0.34)))
    bump = nodes.new("ShaderNodeBump")
    bump.inputs["Strength"].default_value = bump_str
    bump.inputs["Distance"].default_value = 0.02
    links.new(h_sock, bump.inputs["Height"])
    links.new(bump.outputs["Normal"], bsdf.inputs["Normal"])

    return mat, h_sock


def _apply_brick_mortar_post(
    alb_lin: np.ndarray,
    nrm_arr: np.ndarray,
    rgh_arr: np.ndarray,
    ao_arr: np.ndarray,
    hgt_arr: np.ndarray,
    base_porosity: int,
) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Apply periodic 4-course running-bond mortar joints (2 px@64x, height -25%, porosity +15; 32#3.4)."""
    res = alb_lin.shape[0]
    # Bottom-up v, left-to-right u in [0..1)
    yy, xx = np.mgrid[0:res, 0:res]
    u = (xx.astype(np.float32) + 0.5) / float(res)
    v = (yy.astype(np.float32) + 0.5) / float(res)

    courses = 4.0
    bricks_per_row = 2.0
    row_idx = np.floor(v * courses)
    v_local = (v * courses) - row_idx  # [0..1) inside each course
    u_shifted = np.mod(u + 0.5 * np.mod(row_idx, 2.0), 1.0)
    u_local = np.mod(u_shifted * bricks_per_row, 1.0)

    # Mortar total width = 2/64 of block => half-width = 1/64 in block space:
    # In v_local (scaled by 4): 4 * (1/64) = 0.0625; in u_local (scaled by 2): 2 * (1/64) = 0.03125
    dist_h = np.minimum(v_local, 1.0 - v_local) / courses  # distance in block fraction
    dist_v = np.minimum(u_local, 1.0 - u_local) / bricks_per_row
    dist_joint = np.minimum(dist_h, dist_v)

    half_w = 1.0 / 64.0
    bevel_w = 0.75 / 64.0
    mortar_mask = np.clip((half_w + bevel_w - dist_joint) / max(bevel_w, 1e-5), 0.0, 1.0)
    m3 = mortar_mask[:, :, None]

    mortar_srgb = np.array([138.0 / 255.0, 134.0 / 255.0, 126.0 / 255.0], dtype=np.float32)
    out_alb = alb_lin.copy()
    out_alb[:, :, :3] = alb_lin[:, :, :3] * (1.0 - 0.72 * m3) + mortar_srgb * (0.72 * m3)

    # Height in mortar joint is 25% lower than brick face (32-art-audio.md#3.4 Rule 3)
    h_scalar = np.clip(hgt_arr[:, :, 0] * (1.0 - 0.25 * mortar_mask), 0.04, 1.0)
    out_hgt = hgt_arr.copy()
    out_hgt[:, :, 0:3] = h_scalar[:, :, None]

    # Recompute bevel normal perturbation into mortar groove (bottom-up: row+1 is +Y up!)
    dh_dx = (np.roll(h_scalar, -1, axis=1) - np.roll(h_scalar, 1, axis=1)) * 0.5
    dh_dy_up = (np.roll(h_scalar, -1, axis=0) - np.roll(h_scalar, 1, axis=0)) * 0.5
    nx = (nrm_arr[:, :, 0] * 2.0 - 1.0) - 3.2 * dh_dx
    ny = (nrm_arr[:, :, 1] * 2.0 - 1.0) - 3.2 * dh_dy_up
    nz = np.maximum(nrm_arr[:, :, 2] * 2.0 - 1.0, 0.2)
    nlen = np.sqrt(nx * nx + ny * ny + nz * nz)
    out_nrm = nrm_arr.copy()
    out_nrm[:, :, 0] = (nx / nlen) * 0.5 + 0.5
    out_nrm[:, :, 1] = (ny / nlen) * 0.5 + 0.5
    out_nrm[:, :, 2] = (nz / nlen) * 0.5 + 0.5

    out_ao = ao_arr.copy()
    out_ao[:, :, 0:3] = np.clip(ao_arr[:, :, 0:3] * (1.0 - 0.22 * m3), 0.05, 1.0)

    mortar_r = p0.smoothness_to_roughness(22)
    out_rgh = rgh_arr.copy()
    out_rgh[:, :, 0:3] = rgh_arr[:, :, 0:3] * (1.0 - m3) + mortar_r * m3

    # Porosity map: mortar porosity = brick porosity + 15 (clamped to LabPBR porosity ceiling 64)
    p_brick = float(min(64, max(0, base_porosity))) / 255.0
    p_mortar = float(min(64, max(0, base_porosity + 15))) / 255.0
    por_scalar = np.where(mortar_mask >= 0.5, p_mortar, p_brick).astype(np.float32)
    por_rgba = np.stack([por_scalar, por_scalar, por_scalar, np.ones_like(por_scalar)], axis=2)
    return out_alb, out_nrm, out_rgh, out_ao, out_hgt, por_rgba


def run_4x4_seamless_cycles_batches(jobs: list[dict[str, Any]], tile_res: int) -> None:
    """Bake up to 16 seamless 4D-torus materials simultaneously per Cycles 4x4 atlas pass."""
    grid_n = 4
    atlas_res = tile_res * grid_n

    for start in range(0, len(jobs), 16):
        batch = jobs[start : start + 16]
        scene = p0.reset_cycles_scene(ao_samples=1)
        planes: list[bpy.types.Object] = []
        mat_h_list: list[tuple[bpy.types.Material, bpy.types.NodeSocket, int, int]] = []

        for idx, job in enumerate(batch):
            row, col = idx // grid_n, idx % grid_n
            u0, v0 = col / float(grid_n), row / float(grid_n)
            u1, v1 = (col + 1) / float(grid_n), (row + 1) / float(grid_n)
            mat, h_sock = build_seamless_tile_shader(job, u0, v0, grid_n)

            bm = bmesh.new()
            uv_l = bm.loops.layers.uv.new("UVMap")
            wx0, wy0 = col * 1.5, row * 1.5
            vs = [
                bm.verts.new((wx0, wy0, 0.0)),
                bm.verts.new((wx0 + 1.0, wy0, 0.0)),
                bm.verts.new((wx0 + 1.0, wy0 + 1.0, 0.0)),
                bm.verts.new((wx0, wy0 + 1.0, 0.0)),
            ]
            face = bm.faces.new(vs)
            for loop, uv in zip(face.loops, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]):
                loop[uv_l].uv = uv
            mesh = bpy.data.meshes.new(f"tile_mesh_{idx}")
            bm.to_mesh(mesh)
            bm.free()
            obj = bpy.data.objects.new(f"Tile_{idx}", mesh)
            obj.data.materials.append(mat)
            bpy.context.collection.objects.link(obj)
            planes.append(obj)
            mat_h_list.append((mat, h_sock, row, col))

        def bake_atlas(btype: str, is_srgb: bool, normal_gl: bool = False) -> np.ndarray:
            img = bpy.data.images.new(f"atlas_{btype}", width=atlas_res, height=atlas_res, alpha=True)
            img.colorspace_settings.name = "sRGB" if is_srgb else "Non-Color"
            added = []
            for mat, _, _, _ in mat_h_list:
                nt = mat.node_tree
                tn = nt.nodes.new("ShaderNodeTexImage")
                tn.image = img
                for n in nt.nodes:
                    n.select = False
                tn.select = True
                nt.nodes.active = tn
                added.append((nt, tn))
            bpy.ops.object.select_all(action="DESELECT")
            for p in planes:
                p.select_set(True)
            bpy.context.view_layer.objects.active = planes[0]
            scene.render.bake.margin = 0
            scene.render.bake.use_clear = True
            if btype == "DIFFUSE":
                scene.render.bake.use_pass_direct = False
                scene.render.bake.use_pass_indirect = False
                scene.render.bake.use_pass_color = True
            if normal_gl:
                scene.render.bake.normal_space = "TANGENT"
                scene.render.bake.normal_r = "POS_X"
                scene.render.bake.normal_g = "POS_Y"
                scene.render.bake.normal_b = "POS_Z"
            bpy.ops.object.bake(type=btype)
            for nt, tn in added:
                nt.nodes.remove(tn)
            arr = np.array(img.pixels[:], dtype=np.float32).reshape((atlas_res, atlas_res, 4))
            bpy.data.images.remove(img)
            return arr

        alb_atlas = bake_atlas("DIFFUSE", is_srgb=True)
        nrm_atlas = bake_atlas("NORMAL", is_srgb=False, normal_gl=True)
        rgh_atlas = bake_atlas("ROUGHNESS", is_srgb=False)

        # Switch surface output to Emission(height) for crisp 1-sample height bake
        for mat, h_sock, _, _ in mat_h_list:
            nt = mat.node_tree
            out_node = next(n for n in nt.nodes if n.type == "OUTPUT_MATERIAL")
            for lk in list(out_node.inputs["Surface"].links):
                nt.links.remove(lk)
            mr = nt.nodes.new("ShaderNodeMapRange")
            mr.inputs["From Min"].default_value = 0.0
            mr.inputs["From Max"].default_value = 1.0
            mr.inputs["To Min"].default_value = 0.14
            mr.inputs["To Max"].default_value = 0.94
            em = nt.nodes.new("ShaderNodeEmission")
            nt.links.new(h_sock, mr.inputs["Value"])
            nt.links.new(mr.outputs["Result"], em.inputs["Color"])
            nt.links.new(em.outputs["Emission"], out_node.inputs["Surface"])

        hgt_atlas = bake_atlas("EMIT", is_srgb=False)

        # Extract each tile, derive periodic micro-cavity AO from periodic height Laplacian, & save
        for idx, job in enumerate(batch):
            _, _, row, col = mat_h_list[idx]
            r0, r1 = row * tile_res, (row + 1) * tile_res
            c0, c1 = col * tile_res, (col + 1) * tile_res

            alb_t = alb_atlas[r0:r1, c0:c1].copy()
            nrm_t = nrm_atlas[r0:r1, c0:c1].copy()
            nrm_t[:, :, 3] = 1.0
            rgh_t = rgh_atlas[r0:r1, c0:c1].copy()
            rgh_t[:, :, 3] = 1.0
            hgt_t = np.clip(hgt_atlas[r0:r1, c0:c1].copy(), 0.05, 1.0)
            hgt_t[:, :, 3] = 1.0

            # Periodic cavity AO from 4-neighbor height mean
            h0 = hgt_t[:, :, 0]
            nbr = 0.25 * (np.roll(h0, 1, 0) + np.roll(h0, -1, 0) + np.roll(h0, 1, 1) + np.roll(h0, -1, 1))
            cavity = np.clip(0.92 + 1.8 * (h0 - nbr), 0.45, 1.0)
            ao_t = np.stack([cavity, cavity, cavity, np.ones_like(cavity)], axis=2)

            out_dir: Path = job["out_dir"]
            kind = job.get("job_kind", "block")

            if kind == "overlay":
                _save_mineral_overlay_tile(job, alb_t, nrm_t, rgh_t, ao_t, hgt_t, out_dir)
                continue

            alb_t = calibrate_albedo_linear(
                alb_t,
                target_brightness=job.get("target_brightness"),
                max_mean_hsv_s=0.41,
            )

            if job.get("form") == "bricks":
                alb_t, nrm_t, rgh_t, ao_t, hgt_t, por_t = _apply_brick_mortar_post(
                    alb_t, nrm_t, rgh_t, ao_t, hgt_t, int(job.get("base_P", 25))
                )
                alb_t = calibrate_albedo_linear(alb_t, target_brightness=job.get("target_brightness"), max_mean_hsv_s=0.41)
                p0.save_float_rgba_png(por_t, out_dir / "porosity.png", is_srgb=False)

            if job.get("special_post") == "lava_crust":
                # Incandescent glowing magma fissures in deep cracks (iris_category=lava_like)
                crack = hgt_t[:, :, 0] < 0.28
                glow_srgb = np.array([216.0 / 255.0, 94.0 / 255.0, 34.0 / 255.0], dtype=np.float32)
                alb_t[crack, 0:3] = glow_srgb
                alb_t = calibrate_albedo_linear(alb_t, max_mean_hsv_s=0.38)
                em_s = np.where(crack, 95.0 / 255.0, 1.0).astype(np.float32)
                em_rgba = np.stack([em_s, em_s, em_s, np.ones_like(em_s)], axis=2)
                p0.save_float_rgba_png(em_rgba, out_dir / "emissive.png", is_srgb=False)
            elif job.get("special_post") == "massive_sulfide":
                # Discrete sulfide mineral F0 zones (chalcopyrite=255, galena=235, sphalerite=18)
                h0_val = hgt_t[:, :, 0]
                f0_s = np.where(h0_val > 0.62, 1.0, np.where(h0_val > 0.38, 235.0 / 255.0, 18.0 / 255.0)).astype(np.float32)
                f0_rgba = np.stack([f0_s, f0_s, f0_s, np.ones_like(f0_s)], axis=2)
                p0.save_float_rgba_png(f0_rgba, out_dir / "f0.png", is_srgb=False)

            p0.save_float_rgba_png(alb_t, out_dir / "albedo.png", is_srgb=True)
            p0.save_float_rgba_png(nrm_t, out_dir / "normal.png", is_srgb=False)
            p0.save_float_rgba_png(rgh_t, out_dir / "roughness.png", is_srgb=False)
            p0.save_float_rgba_png(ao_t, out_dir / "ao.png", is_srgb=False)
            p0.save_float_rgba_png(hgt_t, out_dir / "height.png", is_srgb=False)


def _save_mineral_overlay_tile(
    job: dict[str, Any],
    alb_t: np.ndarray,
    nrm_t: np.ndarray,
    rgh_t: np.ndarray,
    ao_t: np.ndarray,
    hgt_t: np.ndarray,
    out_dir: Path,
) -> None:
    """Extract crisp alpha-masked mineral grains/veins/nodules for art-src/ore_overlays/<mineral>/."""
    res = alb_t.shape[0]
    h0 = hgt_t[:, :, 0]
    morph = job["morphology"]

    # Normalize h0 percentiles so every mineral achieves consistent ~22-26% readable grain coverage
    yy, xx = np.mgrid[0:res, 0:res]
    u = (xx.astype(np.float32) + 0.5) / float(res)
    v = (yy.astype(np.float32) + 0.5) / float(res)

    if morph == "dendritic":
        # Branching veins modulated by secondary harmonic
        vein = 1.0 - np.abs(2.0 * h0 - 1.0)
        field = 0.65 * vein + 0.35 * (0.5 + 0.5 * np.sin(TWO_PI * (2.0 * u + v)))
        thresh = float(np.quantile(field, 0.77))
    elif morph == "foliated":
        streak = 0.60 * h0 + 0.40 * (0.5 + 0.5 * np.sin(TWO_PI * 3.0 * v + 3.5 * h0))
        field = streak
        thresh = float(np.quantile(field, 0.76))
    else:
        field = h0
        thresh = float(np.quantile(field, 0.76))

    alpha = np.clip((field - thresh) / max(1e-4, (float(np.max(field)) - thresh) * 0.32), 0.0, 1.0)
    mask = alpha > 0.02

    out_alb = np.zeros((res, res, 4), dtype=np.float32)
    out_alb[:, :, :3] = np.where(mask[:, :, None], alb_t[:, :, :3], 0.0)
    out_alb[:, :, 3] = np.where(mask, alpha, 0.0)

    flat_nrm = np.broadcast_to(np.array([0.5, 0.5, 1.0], dtype=np.float32), (res, res, 3))
    out_nrm = np.concatenate([np.where(mask[:, :, None], nrm_t[:, :, :3], flat_nrm), np.ones((res, res, 1), dtype=np.float32)], axis=2)

    grain_h = np.where(mask, np.clip((field - thresh) / max(1e-4, float(np.max(field)) - thresh), 0.05, 1.0), 0.0)
    out_hgt = np.stack([grain_h, grain_h, grain_h, np.ones_like(grain_h)], axis=2)

    out_ao = np.concatenate([np.where(mask[:, :, None], ao_t[:, :, :3], 1.0), np.ones((res, res, 1), dtype=np.float32)], axis=2)
    out_rgh = rgh_t.copy()

    p0.save_float_rgba_png(out_alb, out_dir / "albedo.png", is_srgb=True)
    p0.save_float_rgba_png(out_nrm, out_dir / "normal.png", is_srgb=False)
    p0.save_float_rgba_png(out_rgh, out_dir / "roughness.png", is_srgb=False)
    p0.save_float_rgba_png(out_ao, out_dir / "ao.png", is_srgb=False)
    p0.save_float_rgba_png(out_hgt, out_dir / "height.png", is_srgb=False)


# ---------------------------------------------------------------------------
# 3. 3D Relief Item Modeling & 4x4 Batch Baking (46 Standalone Items)
# ---------------------------------------------------------------------------

def _build_item_3d_submesh(
    bm: bmesh.types.BMesh,
    uv_l: Any,
    silhouette: str,
    cx: float,
    cy: float,
    u0: float,
    v0: float,
    grid_n: int,
    is_accent: bool,
) -> None:
    """Populate 3D beveled geological specimen or bucket silhouette centered at (cx, cy) mapped to sub-tile UV."""

    def add_prism(pts_local: list[tuple[float, float]], z0: float, z1: float, shrink: float = 0.86) -> None:
        b_vs = [bm.verts.new((cx + x, cy + y, z0)) for x, y in pts_local]
        t_vs = [bm.verts.new((cx + x * shrink, cy + y * shrink, z1)) for x, y in pts_local]
        new_faces = [bm.faces.new(t_vs)]
        for i in range(len(pts_local)):
            ni = (i + 1) % len(pts_local)
            new_faces.append(bm.faces.new((b_vs[i], b_vs[ni], t_vs[ni], t_vs[i])))
        for f in new_faces:
            for loop in f.loops:
                lx = loop.vert.co.x - cx + 0.5
                ly = loop.vert.co.y - cy + 0.5
                loop[uv_l].uv = (u0 + np.clip(lx, 0.01, 0.99) / grid_n, v0 + np.clip(ly, 0.01, 0.99) / grid_n)

    if silhouette == "fluid_bucket":
        if not is_accent:
            # Tapered iron/wood bucket body + rim (main = fluid pool inside top ellipse, wait: accent=bucket body or vice versa!)
            # Put fluid pool on main material? No: bucket rim/walls = accent steel/iron, wide top liquid meniscus = main fluid!
            pts_liq = [(0.24 * math.cos(a), 0.14 * math.sin(a) + 0.12) for a in [i * TWO_PI / 12 for i in range(12)]]
            add_prism(pts_liq, 0.075, 0.105, shrink=0.95)
        else:
            body = [(-0.20, -0.34), (0.20, -0.34), (0.29, 0.14), (-0.29, 0.14)]
            add_prism(body, 0.0, 0.07, shrink=0.92)
            rim = [(0.30 * math.cos(a), 0.17 * math.sin(a) + 0.12) for a in [i * TWO_PI / 12 for i in range(12)]]
            add_prism(rim, 0.055, 0.074, shrink=0.96)
            bail = [(-0.28, 0.14), (-0.22, 0.34), (0.22, 0.34), (0.28, 0.14), (0.22, 0.14), (0.17, 0.28), (-0.17, 0.28), (-0.22, 0.14)]
            add_prism(bail, 0.03, 0.06, shrink=0.96)
        return

    if silhouette == "crystal_cluster":
        if not is_accent:
            add_prism([(-0.10, -0.28), (0.08, -0.28), (0.12, 0.22), (0.0, 0.36), (-0.12, 0.20)], 0.02, 0.11, 0.80)
            add_prism([(-0.30, -0.18), (-0.14, -0.24), (-0.04, 0.12), (-0.16, 0.26), (-0.32, 0.10)], 0.01, 0.09, 0.82)
            add_prism([(0.10, -0.22), (0.28, -0.14), (0.32, 0.12), (0.18, 0.24), (0.06, 0.08)], 0.01, 0.09, 0.82)
        else:
            add_prism([(-0.32, -0.34), (0.30, -0.34), (0.34, -0.16), (-0.34, -0.14)], 0.0, 0.065, 0.88)
    elif silhouette == "cubic_cleavage":
        if not is_accent:
            add_prism([(-0.26, -0.24), (0.16, -0.26), (0.24, 0.16), (-0.18, 0.22)], 0.0, 0.10, 0.85)
            add_prism([(0.02, -0.02), (0.32, -0.04), (0.34, 0.28), (0.04, 0.30)], 0.03, 0.12, 0.86)
        else:
            add_prism([(-0.32, 0.02), (-0.06, 0.0), (-0.04, 0.30), (-0.30, 0.32)], 0.02, 0.09, 0.86)
    elif silhouette == "dendritic_nugget":
        if not is_accent:
            add_prism([(-0.12, -0.34), (0.10, -0.32), (0.16, 0.04), (0.32, 0.22), (0.16, 0.34), (-0.02, 0.18), (-0.24, 0.32), (-0.34, 0.16), (-0.14, -0.04)], 0.01, 0.10, 0.84)
        else:
            add_prism([(-0.28, -0.24), (0.24, -0.22), (0.18, -0.04), (-0.22, -0.02)], 0.0, 0.055, 0.90)
    elif silhouette == "botryoidal_cluster":
        for ox, oy, r, target_acc in [(-0.12, -0.10, 0.21, False), (0.14, -0.08, 0.19, False), (0.0, 0.14, 0.20, False), (-0.18, 0.14, 0.14, True), (0.18, 0.14, 0.13, True)]:
            if target_acc == is_accent:
                ring = [(ox + r * math.cos(a), oy + r * math.sin(a)) for a in [i * TWO_PI / 10 for i in range(10)]]
                add_prism(ring, 0.0, 0.105 if not is_accent else 0.085, 0.78)
    elif silhouette == "layered_slab":
        if not is_accent:
            add_prism([(-0.34, -0.28), (0.28, -0.32), (0.36, 0.10), (-0.26, 0.24)], 0.0, 0.065, 0.92)
            add_prism([(-0.26, -0.14), (0.24, -0.18), (0.30, 0.22), (-0.20, 0.32)], 0.05, 0.105, 0.90)
        else:
            add_prism([(-0.18, -0.04), (0.18, -0.06), (0.22, 0.16), (-0.14, 0.22)], 0.09, 0.125, 0.88)
    else:  # angular_ore
        if not is_accent:
            add_prism([(-0.30, -0.30), (0.22, -0.34), (0.36, -0.06), (0.24, 0.30), (-0.18, 0.34), (-0.36, 0.08)], 0.0, 0.095, 0.83)
        else:
            add_prism([(-0.14, -0.16), (0.18, -0.12), (0.22, 0.14), (-0.08, 0.20)], 0.075, 0.125, 0.85)


def bake_all_standalone_items(
    repo_root: Path,
    materials_db: dict[str, dict[str, Any]],
    tile_res: int,
) -> int:
    res_root = p0.mod_resource_dir(repo_root, MODID)
    items_art_root = repo_root / "art-src" / "items" / MODID
    blend_root = repo_root / "art-src" / "blend" / "items"
    blend_root.mkdir(parents=True, exist_ok=True)

    grid_n = 4
    atlas_res = tile_res * grid_n

    for start in range(0, len(specs.STANDALONE_ITEMS), 16):
        batch = specs.STANDALONE_ITEMS[start : start + 16]
        scene = p0.reset_cycles_scene(ao_samples=2)
        objs: list[bpy.types.Object] = []
        mats: list[bpy.types.Material] = []

        for idx, (iname, mat_key, sil_kind, acc_rgb) in enumerate(batch):
            row, col = idx // grid_n, idx % grid_n
            u0, v0 = col / float(grid_n), row / float(grid_n)
            cx, cy = col * 1.5 + 0.5, row * 1.5 + 0.5
            minfo = materials_db[mat_key]
            main_rgb = minfo["albedo"]
            s_val = int(minfo["S"])

            # Bucket accent is wrought_iron / steel bucket shell
            if sil_kind == "fluid_bucket":
                acc_rgb_eff = [138, 140, 145]
                acc_s = 165
            else:
                acc_rgb_eff = acc_rgb
                acc_s = max(25, s_val - 20)

            m_main, _ = p0.build_pbr_shader(f"{iname}_m", main_rgb, s_val, noise_scale=22.0, bump_strength=0.36, color_spread=0.12)
            m_acc, _ = p0.build_pbr_shader(f"{iname}_a", acc_rgb_eff, acc_s, noise_scale=30.0, bump_strength=0.28, color_spread=0.10)
            mats.extend([m_main, m_acc])

            for is_acc, mat_ref, tag in ((False, m_main, "main"), (True, m_acc, "acc")):
                bm = bmesh.new()
                uv_l = bm.loops.layers.uv.new("UVMap")
                _build_item_3d_submesh(bm, uv_l, sil_kind, cx, cy, u0, v0, grid_n, is_accent=is_acc)
                mesh = bpy.data.meshes.new(f"{iname}_{tag}")
                bm.to_mesh(mesh)
                bm.free()
                ob = bpy.data.objects.new(f"{iname}_{tag}", mesh)
                ob.data.materials.append(mat_ref)
                bpy.context.collection.objects.link(ob)
                objs.append(ob)

        batch_blend = blend_root / f"{MODID}_items_batch_{start // 16:02d}.blend"
        bpy.ops.wm.save_as_mainfile(filepath=str(batch_blend))

        def bake_item_atlas(btype: str, is_srgb: bool, samples: int = 1, normal_gl: bool = False) -> np.ndarray:
            scene.cycles.samples = samples
            img = bpy.data.images.new(f"item_atlas_{btype}", width=atlas_res, height=atlas_res, alpha=True)
            img.colorspace_settings.name = "sRGB" if is_srgb else "Non-Color"
            img.generated_color = (0.0, 0.0, 0.0, 0.0)
            added = []
            for m in mats:
                nt = m.node_tree
                tn = nt.nodes.new("ShaderNodeTexImage")
                tn.image = img
                for n in nt.nodes:
                    n.select = False
                tn.select = True
                nt.nodes.active = tn
                added.append((nt, tn))
            bpy.ops.object.select_all(action="DESELECT")
            for o in objs:
                o.select_set(True)
            bpy.context.view_layer.objects.active = objs[0]
            scene.render.bake.margin = 0
            scene.render.bake.use_clear = True
            if btype == "DIFFUSE":
                scene.render.bake.use_pass_direct = False
                scene.render.bake.use_pass_indirect = False
                scene.render.bake.use_pass_color = True
            if normal_gl:
                scene.render.bake.normal_space = "TANGENT"
                scene.render.bake.normal_r = "POS_X"
                scene.render.bake.normal_g = "POS_Y"
                scene.render.bake.normal_b = "POS_Z"
            bpy.ops.object.bake(type=btype)
            for nt, tn in added:
                nt.nodes.remove(tn)
            arr = np.array(img.pixels[:], dtype=np.float32).reshape((atlas_res, atlas_res, 4))
            bpy.data.images.remove(img)
            return arr

        alb_a = bake_item_atlas("DIFFUSE", is_srgb=True, samples=1)
        nrm_a = bake_item_atlas("NORMAL", is_srgb=False, samples=1, normal_gl=True)
        rgh_a = bake_item_atlas("ROUGHNESS", is_srgb=False, samples=1)
        ao_a = bake_item_atlas("AO", is_srgb=False, samples=2)

        default_nrm = np.broadcast_to(np.array([0.5, 0.5, 1.0], dtype=np.float32), (tile_res, tile_res, 3))

        for idx, (iname, mat_key, sil_kind, _) in enumerate(batch):
            row, col = idx // grid_n, idx % grid_n
            r0, r1 = row * tile_res, (row + 1) * tile_res
            c0, c1 = col * tile_res, (col + 1) * tile_res

            alb = alb_a[r0:r1, c0:c1].copy()
            mask = alb[:, :, 3:4] > 0.05
            alpha_bin = np.where(mask, 1.0, 0.0).astype(np.float32)
            alb = np.concatenate([alb[:, :, :3] * alpha_bin, alpha_bin], axis=2)

            nrm_sub = nrm_a[r0:r1, c0:c1, :3]
            nrm = np.concatenate([np.where(mask, nrm_sub, default_nrm), np.ones((tile_res, tile_res, 1), dtype=np.float32)], axis=2)

            base_r = p0.smoothness_to_roughness(int(materials_db[mat_key]["S"]))
            rgh_sub = np.where(mask, rgh_a[r0:r1, c0:c1, :3], base_r)
            rgh = np.concatenate([rgh_sub, np.ones((tile_res, tile_res, 1), dtype=np.float32)], axis=2)

            ao_sub = np.where(mask, ao_a[r0:r1, c0:c1, :3], 1.0)
            ao = np.concatenate([ao_sub, np.ones((tile_res, tile_res, 1), dtype=np.float32)], axis=2)

            hgt_sub = np.where(mask, np.clip(0.32 + 0.68 * ao_sub, 0.08, 1.0), 0.04)
            hgt = np.concatenate([hgt_sub, np.ones((tile_res, tile_res, 1), dtype=np.float32)], axis=2)

            out_dir = items_art_root / iname
            p0.save_float_rgba_png(alb, out_dir / "albedo.png", is_srgb=True)
            p0.save_float_rgba_png(nrm, out_dir / "normal.png", is_srgb=False)
            p0.save_float_rgba_png(rgh, out_dir / "roughness.png", is_srgb=False)
            p0.save_float_rgba_png(ao, out_dir / "ao.png", is_srgb=False)
            p0.save_float_rgba_png(hgt, out_dir / "height.png", is_srgb=False)

            if sil_kind == "fluid_bucket":
                # Bucket body has steel F0=230 where roughness is rougher than fluid surface
                is_metal_shell = (mask[:, :, 0]) & (rgh_sub[:, :, 0] > 0.06)
                f0_s = np.where(is_metal_shell, 230.0 / 255.0, 10.0 / 255.0).astype(np.float32)
                f0_rgba = np.stack([f0_s, f0_s, f0_s, np.ones_like(f0_s)], axis=2)
                p0.save_float_rgba_png(f0_rgba, out_dir / "f0.png", is_srgb=False)

            meta_obj = {
                "schema_version": 1,
                "kind": "item",
                "id": f"{MODID}:{iname}",
                "material": mat_key,
                "variants": 1,
                "emissive": False,
                "master_resolution": tile_res,
                "source_blend": str(batch_blend.relative_to(repo_root)),
                "description": f"P1 Wildlands 3D relief item icon ({sil_kind}) for {MODID}:{iname}",
            }
            (out_dir / "meta.json").write_text(json.dumps(meta_obj, indent=2) + "\n", encoding="utf-8")

            item_json = {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"{MODID}:item/{iname}"},
            }
            im_path = res_root / "models" / "item" / f"{iname}.json"
            im_path.parent.mkdir(parents=True, exist_ok=True)
            im_path.write_text(json.dumps(item_json, indent=2) + "\n", encoding="utf-8")

    return len(specs.STANDALONE_ITEMS)


# ---------------------------------------------------------------------------
# 4. Assemble All Seamless Jobs & Write Models / Blockstates / Meta
# ---------------------------------------------------------------------------

def _rgb_pair(rgb: list[int], spread: float = 0.16) -> tuple[list[int], list[int]]:
    lo = [max(4, min(250, int(round(c * (1.0 - spread))))) for c in rgb]
    hi = [max(8, min(254, int(round(c * (1.0 + spread))))) for c in rgb]
    return lo, hi


def _write_cube_models_and_blockstates(
    res_root: Path,
    block_name: str,
    variants: int,
    rotate_y: bool,
) -> None:
    bm_dir = res_root / "models" / "block"
    im_dir = res_root / "models" / "item"
    bs_dir = res_root / "blockstates"
    bm_dir.mkdir(parents=True, exist_ok=True)
    im_dir.mkdir(parents=True, exist_ok=True)
    bs_dir.mkdir(parents=True, exist_ok=True)

    model_names = [block_name] + [f"{block_name}_v{i}" for i in range(1, variants)]
    for mname in model_names:
        bm_json = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"{MODID}:block/{mname}"},
        }
        (bm_dir / f"{mname}.json").write_text(json.dumps(bm_json, indent=2) + "\n", encoding="utf-8")

    im_json = {"parent": f"{MODID}:block/{block_name}"}
    (im_dir / f"{block_name}.json").write_text(json.dumps(im_json, indent=2) + "\n", encoding="utf-8")

    if variants > 1 or rotate_y:
        entries: list[dict[str, Any]] = []
        weights = (4, 2, 3, 2) if rotate_y else (1,)
        y_vals = (0, 90, 180, 270) if rotate_y else (0,)
        for mname in model_names:
            for y_deg, w in zip(y_vals, weights):
                entry: dict[str, Any] = {"model": f"{MODID}:block/{mname}", "weight": w}
                if rotate_y:
                    entry["y"] = y_deg
                entries.append(entry)
        bs_json: dict[str, Any] = {"variants": {"": entries}}
    else:
        bs_json = {"variants": {"": {"model": f"{MODID}:block/{block_name}"}}}

    (bs_dir / f"{block_name}.json").write_text(json.dumps(bs_json, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    args = _parse_args()
    repo_root = args.repo_root.resolve()
    tile_res = int(args.tile_res)
    materials_db = json.loads((repo_root / "art-src" / "materials.json").read_text(encoding="utf-8"))["materials"]
    res_root = p0.mod_resource_dir(repo_root, MODID)
    blocks_art_root = repo_root / "art-src" / "blocks" / MODID

    print("[1/4] Building 12 3D cuboid geological & mine blocks in Blender...")
    c3d = build_all_3d_geological_blocks(repo_root, materials_db, tile_res)

    jobs: list[dict[str, Any]] = []

    def add_block_job(
        bname: str,
        mat_key: str,
        fabric: str,
        form: str,
        target_b: float | None,
        n_scale: float,
        bands: int,
        variants: int,
        rotate_y: bool,
        iris_cat: str = "solid",
        emissive: bool = False,
        special_post: str | None = None,
        rgb_override: tuple[list[int], list[int]] | None = None,
    ) -> None:
        minfo = materials_db[mat_key]
        base_s = int(minfo["S"])
        if form == "polished":
            s_eff = int(minfo.get("polished_S", min(230, base_s + 95)))
        elif form == "cobble":
            s_eff = max(12, base_s - 8)
        else:
            s_eff = base_s

        rgb_lo, rgb_hi = rgb_override if rgb_override else _rgb_pair(minfo["albedo"], 0.10 if form == "polished" else 0.17)
        out_dir = blocks_art_root / bname
        base_p = int(minfo.get("P", 20))

        jobs.append({
            "key": f"blk_{bname}",
            "seed_key": f"{MODID}:{bname}:v0",
            "job_kind": "block",
            "fabric": fabric,
            "form": form,
            "S": s_eff,
            "base_P": base_p,
            "rgb_lo": rgb_lo,
            "rgb_hi": rgb_hi,
            "target_brightness": target_b,
            "noise_scale": n_scale,
            "bands": bands,
            "special_post": special_post,
            "out_dir": out_dir,
        })
        for v_idx in range(1, variants):
            jobs.append({
                "key": f"blk_{bname}_v{v_idx}",
                "seed_key": f"{MODID}:{bname}:v{v_idx}",
                "job_kind": "block",
                "fabric": fabric,
                "form": form,
                "S": s_eff,
                "base_P": base_p,
                "rgb_lo": rgb_lo,
                "rgb_hi": rgb_hi,
                "target_brightness": target_b,
                "noise_scale": n_scale * 1.07,
                "bands": bands,
                "special_post": special_post,
                "out_dir": out_dir / f"variant_{v_idx}",
            })

        out_dir.mkdir(parents=True, exist_ok=True)
        meta_obj = {
            "schema_version": 1,
            "kind": "block",
            "id": f"{MODID}:{bname}",
            "material": mat_key,
            "variants": variants,
            "emissive": emissive,
            "iris_category": iris_cat,
            "master_resolution": tile_res,
            "description": f"P1 Wildlands seamless 4D-torus PBR block ({form}, {fabric})",
        }
        (out_dir / "meta.json").write_text(json.dumps(meta_obj, indent=2) + "\n", encoding="utf-8")
        _write_cube_models_and_blockstates(res_root, bname, variants=variants, rotate_y=rotate_y)

    # 22 custom rocks x 4 forms
    for rock_id, fabric, tb, n_scale, bands in specs.CUSTOM_ROCKS:
        add_block_job(rock_id, rock_id, fabric, "raw", tb, n_scale, bands, variants=2, rotate_y=True)
        add_block_job(f"{rock_id}_cobble", rock_id, fabric, "cobble", max(0.26, tb - 0.015), n_scale, 0, variants=1, rotate_y=True)
        add_block_job(f"{rock_id}_bricks", rock_id, fabric, "bricks", tb, n_scale, 0, variants=1, rotate_y=False)
        add_block_job(f"polished_{rock_id}", rock_id, fabric, "polished", min(0.54, tb + 0.01), n_scale * 0.85, bands, variants=1, rotate_y=False)

    # 10 vanilla rock missing forms
    for bname, mat_key, form, tb in specs.VANILLA_ROCK_FORMS:
        add_block_job(bname, mat_key, "coarse_crystalline", form, tb, 16.0, 0, variants=1, rotate_y=(form == "cobble"))

    # 16 soils & sediments (2 variants + y-rotation weights)
    for soil_id, fabric, tb, n_scale, bands in specs.SOILS:
        add_block_job(soil_id, soil_id, fabric, "raw", tb, n_scale, bands, variants=2, rotate_y=True)

    # 3 coal seams (shale-hosted horizontal coal beds)
    for seam_id, mat_key, tb in specs.COAL_SEAMS:
        coal_rgb = materials_db[mat_key]["albedo"]
        shale_rgb = materials_db["shale"]["albedo"]
        add_block_job(
            seam_id,
            mat_key,
            "fissile_bedded",
            "seam",
            tb,
            20.0,
            5,
            variants=1,
            rotate_y=True,
            rgb_override=(coal_rgb, shale_rgb),
        )

    # 3 special full-cube geological blocks
    add_block_job("lava_crust", "lava_crust", "lava_cracks", "raw", 0.275, 14.0, 0, variants=2, rotate_y=True, iris_cat="lava_like", emissive=True, special_post="lava_crust")
    add_block_job("massive_sulfide", "massive_sulfide", "coarse_crystalline", "raw", 0.395, 16.0, 0, variants=2, rotate_y=True, iris_cat="solid", emissive=False, special_post="massive_sulfide", rgb_override=([92, 82, 64], [176, 154, 88]))
    add_block_job("salt_crust", "salt_crust", "salt_pan", "raw", 0.530, 12.0, 0, variants=2, rotate_y=True, iris_cat="solid", emissive=False)

    # 7 vanilla host_bases for compose_ores.py
    host_bases_root = repo_root / "art-src" / "host_bases"
    for host_id, fabric, tb, n_scale, bands in specs.VANILLA_HOST_BASES:
        minfo = materials_db[host_id]
        rgb_lo, rgb_hi = _rgb_pair(minfo["albedo"], 0.16)
        h_dir = host_bases_root / host_id
        h_dir.mkdir(parents=True, exist_ok=True)
        (h_dir / "meta.json").write_text(
            json.dumps({
                "schema_version": 1,
                "kind": "host_base",
                "id": f"minecraft:{host_id}",
                "material": host_id,
                "emissive": False,
                "iris_category": "solid",
                "master_resolution": tile_res,
            }, indent=2) + "\n",
            encoding="utf-8",
        )
        jobs.append({
            "key": f"host_{host_id}",
            "seed_key": f"host_base:{host_id}",
            "job_kind": "block",
            "fabric": fabric,
            "form": "raw",
            "S": int(minfo["S"]),
            "base_P": int(minfo.get("P", 25)),
            "rgb_lo": rgb_lo,
            "rgb_hi": rgb_hi,
            "target_brightness": tb,
            "noise_scale": n_scale,
            "bands": bands,
            "out_dir": h_dir,
        })

    # 37 mineral overlays
    if not args.skip_overlays:
        overlays_root = repo_root / "art-src" / "ore_overlays"
        for mineral_id, morph, cov_t in specs.MINERAL_OVERLAYS:
            minfo = materials_db[mineral_id]
            rgb_lo, rgb_hi = _rgb_pair(minfo["albedo"], 0.18)
            if mineral_id == "flint":
                rgb_hi = [184, 178, 166]  # pale siliceous cortex rim on dark flint nodules
            elif mineral_id == "lapis":
                rgb_hi = [92, 118, 182]
            ov_dir = overlays_root / mineral_id
            ov_dir.mkdir(parents=True, exist_ok=True)
            (ov_dir / "meta.json").write_text(
                json.dumps({
                    "schema_version": 1,
                    "kind": "ore_overlay",
                    "mineral": mineral_id,
                    "material": mineral_id,
                    "morphology": morph,
                    "master_resolution": tile_res,
                }, indent=2) + "\n",
                encoding="utf-8",
            )
            jobs.append({
                "key": f"ov_{mineral_id}",
                "seed_key": f"ore_overlay:{mineral_id}",
                "job_kind": "overlay",
                "morphology": morph,
                "coverage_threshold": cov_t,
                "fabric": "coarse_crystalline",
                "form": "raw",
                "S": int(minfo["S"]),
                "rgb_lo": rgb_lo,
                "rgb_hi": rgb_hi,
                "noise_scale": 13.0 if morph in ("pisolitic", "nodule", "botryoidal") else 16.0,
                "bands": 0,
                "bump_strength": 0.45,
                "out_dir": ov_dir,
            })

    print(f"[2/4] Baking {len(jobs)} seamless 4D-torus block/host/overlay tiles in Cycles 4x4 batches...")
    run_4x4_seamless_cycles_batches(jobs, tile_res)

    # Save a reference master .blend for P1 geological shaders
    geol_blend = repo_root / "art-src" / "blend" / "blocks" / "tcwildlands_geology_masters.blend"
    geol_blend.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(geol_blend))

    if not args.skip_items:
        print(f"[3/4] Modeling & baking {len(specs.STANDALONE_ITEMS)} 3D relief standalone items in Blender...")
        icnt = bake_all_standalone_items(repo_root, materials_db, tile_res)
    else:
        icnt = 0

    print(f"[4/4] Done: 3d_blocks={c3d} seamless_tiles={len(jobs)} items={icnt}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
