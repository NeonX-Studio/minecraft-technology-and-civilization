#!/usr/bin/env python3
r"""Headless Blender 3D modeling & 2048x/1024x Cycles PBR master baking for P0 foundation assets.

Builds:
  1. 3D cuboid models (<= 64 elements) exported to assets/<modid>/models/block|item/ & blockstates/
  2. High-detail 3D relief meshes + calibrated procedural shaders baked in Blender Cycles to:
     - art-src/blocks/<modid>/<name>/{albedo,normal,height,ao,roughness[,f0,emissive]}.png + meta.json (2048x2048)
     - art-src/items/<modid>/<name>/{albedo,normal,height,ao,roughness}.png + meta.json (1024x1024)
     - art-src/skins/<modid>/<skin>/{frame,button,tab,scrollbar,toast}.png + meta.json (512x512 nine-slice masters)
  3. Reproducible .blend files under art-src/blend/

Invoke:
  & 'F:\Programs\Blender\blender.exe' --background --factory-startup --python tools\art-export\blender\build_p0_assets.py -- --repo-root I:\Projects\SuperMC
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path
from typing import Any

import addon_utils
import bmesh
import bpy
import numpy as np


def _parse_args() -> argparse.Namespace:
    argv = sys.argv
    if "--" in argv:
        argv = argv[argv.index("--") + 1 :]
    else:
        argv = []
    parser = argparse.ArgumentParser(description="Model P0 3D blocks/items/skins and bake PBR masters in Blender.")
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--block-res", type=int, default=2048)
    parser.add_argument("--item-res", type=int, default=1024)
    parser.add_argument("--skin-res", type=int, default=512)
    return parser.parse_args(argv)


def srgb_to_linear(c: float) -> float:
    c = max(0.0, min(1.0, c))
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def srgb_rgb255_to_linear_rgba(rgb: list[int] | tuple[int, int, int], scale: float = 1.0) -> tuple[float, float, float, float]:
    return (
        srgb_to_linear((rgb[0] / 255.0) * scale),
        srgb_to_linear((rgb[1] / 255.0) * scale),
        srgb_to_linear((rgb[2] / 255.0) * scale),
        1.0,
    )


def smoothness_to_roughness(s_val: int) -> float:
    return max(0.02, min(0.99, (1.0 - float(s_val) / 255.0) ** 2))


def mod_resource_dir(repo_root: Path, modid: str) -> Path:
    folder = "tcivilization-core" if modid == "tcivilization" else modid
    return repo_root / folder / "src" / "main" / "resources" / "assets" / modid


def reset_cycles_scene(ao_samples: int = 4) -> bpy.types.Scene:
    bpy.ops.wm.read_factory_settings(use_empty=True)
    addon_utils.enable("cycles")
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = ao_samples
    scene.cycles.use_denoising = False
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"
    if scene.world is None:
        scene.world = bpy.data.worlds.new("BakeWorld")
    scene.world.light_settings.distance = 0.14
    return scene


def save_float_rgba_png(rgba_bottom_up: np.ndarray, out_path: Path, is_srgb: bool) -> None:
    """Save bottom-to-top float [0,1] RGBA buffer through Blender's native PNG writer without AgX distortion."""
    h, w, _ = rgba_bottom_up.shape
    img = bpy.data.images.new("out_buf", width=w, height=h, alpha=True, float_buffer=False)
    img.colorspace_settings.name = "sRGB" if is_srgb else "Non-Color"
    img.pixels[:] = np.clip(rgba_bottom_up, 0.0, 1.0).ravel().tolist()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.filepath_raw = str(out_path)
    img.file_format = "PNG"
    img.save()
    bpy.data.images.remove(img)


# ---------------------------------------------------------------------------
# 1. Minecraft 3D Block Model Construction in Blender & JSON Export
# ---------------------------------------------------------------------------

# UV Quadrant helper: divides [0, 16] x [0, 16] into 4 clean material zones:
# Q0: [0, 0, 8, 8]   (top-left: primary stone / frame)
# Q1: [8, 0, 16, 8]  (top-right: secondary wood / trim / metal)
# Q2: [0, 8, 8, 16]  (bottom-left: detail / recessed / surface)
# Q3: [8, 8, 16, 16] (bottom-right: accent / embers / screen / glyph)
QUADRANTS = {
    0: (0.0, 0.0, 8.0, 8.0),
    1: (8.0, 0.0, 16.0, 8.0),
    2: (0.0, 8.0, 8.0, 16.0),
    3: (8.0, 8.0, 16.0, 16.0),
}


def build_blender_3d_cuboid_scene_and_mc_json(
    modid: str,
    block_name: str,
    elements_spec: list[dict[str, Any]],
    blend_out: Path,
    res_root: Path,
    light_emission: int = 0,
) -> dict[str, Any]:
    """Instantiate every 3D cuboid element inside Blender, save .blend, and export <=64-element MC model JSON."""
    assert 1 <= len(elements_spec) <= 64, f"{modid}:{block_name} element count {len(elements_spec)} exceeds 64"
    reset_cycles_scene(ao_samples=1)

    preview_mat = bpy.data.materials.new(name=f"{modid}_{block_name}_preview")
    json_elements: list[dict[str, Any]] = []

    for idx, elem in enumerate(elements_spec):
        f_mc = [float(v) for v in elem["from"]]
        t_mc = [float(v) for v in elem["to"]]
        quad_default = int(elem.get("quadrant", 0))
        face_quads: dict[str, int] = elem.get("face_quadrants", {})

        # Create real 3D cuboid in Blender (16 MC units = 1.0m, centered X/Y [-0.5,0.5], Z [0,1])
        bm = bmesh.new()
        x0, y0_mc, z0_mc = f_mc[0] / 16.0 - 0.5, f_mc[1] / 16.0, f_mc[2] / 16.0 - 0.5
        x1, y1_mc, z1_mc = t_mc[0] / 16.0 - 0.5, t_mc[1] / 16.0, t_mc[2] / 16.0 - 0.5

        verts = [
            bm.verts.new((x0, z0_mc, y0_mc)),
            bm.verts.new((x1, z0_mc, y0_mc)),
            bm.verts.new((x1, z1_mc, y0_mc)),
            bm.verts.new((x0, z1_mc, y0_mc)),
            bm.verts.new((x0, z0_mc, y1_mc)),
            bm.verts.new((x1, z0_mc, y1_mc)),
            bm.verts.new((x1, z1_mc, y1_mc)),
            bm.verts.new((x0, z1_mc, y1_mc)),
        ]
        bm.faces.new((verts[0], verts[1], verts[5], verts[4]))  # north
        bm.faces.new((verts[2], verts[3], verts[7], verts[6]))  # south
        bm.faces.new((verts[3], verts[0], verts[4], verts[7]))  # west
        bm.faces.new((verts[1], verts[2], verts[6], verts[5]))  # east
        bm.faces.new((verts[4], verts[5], verts[6], verts[7]))  # up
        bm.faces.new((verts[3], verts[2], verts[1], verts[0]))  # down

        mesh = bpy.data.meshes.new(f"{block_name}_elem_{idx:02d}")
        bm.to_mesh(mesh)
        bm.free()
        obj = bpy.data.objects.new(elem.get("name", f"elem_{idx:02d}"), mesh)
        obj.data.materials.append(preview_mat)
        bpy.context.collection.objects.link(obj)

        rot_spec = elem.get("rotation")
        if rot_spec:
            ox, oy, oz = [v / 16.0 for v in rot_spec["origin"]]
            obj.location = (0.0, 0.0, 0.0)
            axis = rot_spec["axis"]
            rad = math.radians(float(rot_spec["angle"]))
            if axis == "y":
                obj.rotation_euler.z = rad
            elif axis == "x":
                obj.rotation_euler.x = rad
            elif axis == "z":
                obj.rotation_euler.y = rad

        # Map each face proportional sub-rect inside its assigned UV quadrant [0..8] or [8..16]
        dx = max(1.0, t_mc[0] - f_mc[0])
        dy = max(1.0, t_mc[1] - f_mc[1])
        dz = max(1.0, t_mc[2] - f_mc[2])

        def sub_uv(q_idx: int, u_start: float, v_start: float, u_span: float, v_span: float) -> list[float]:
            qu0, qv0, _, _ = QUADRANTS[q_idx]
            ua = qu0 + (u_start / 16.0) * 7.5 + 0.25
            va = qv0 + (v_start / 16.0) * 7.5 + 0.25
            ub = min(qu0 + 7.75, ua + max(0.5, (u_span / 16.0) * 7.5))
            vb = min(qv0 + 7.75, va + max(0.5, (v_span / 16.0) * 7.5))
            return [round(ua, 3), round(va, 3), round(ub, 3), round(vb, 3)]

        faces_json = {
            "north": {"uv": sub_uv(face_quads.get("north", quad_default), f_mc[0], 16.0 - t_mc[1], dx, dy), "texture": "#all"},
            "south": {"uv": sub_uv(face_quads.get("south", quad_default), f_mc[0], 16.0 - t_mc[1], dx, dy), "texture": "#all"},
            "west":  {"uv": sub_uv(face_quads.get("west", quad_default),  f_mc[2], 16.0 - t_mc[1], dz, dy), "texture": "#all"},
            "east":  {"uv": sub_uv(face_quads.get("east", quad_default),  f_mc[2], 16.0 - t_mc[1], dz, dy), "texture": "#all"},
            "up":    {"uv": sub_uv(face_quads.get("up", quad_default),    f_mc[0], f_mc[2], dx, dz),        "texture": "#all"},
            "down":  {"uv": sub_uv(face_quads.get("down", quad_default),  f_mc[0], f_mc[2], dx, dz),        "texture": "#all"},
        }
        el_entry: dict[str, Any] = {
            "from": [round(v, 3) for v in f_mc],
            "to": [round(v, 3) for v in t_mc],
            "faces": faces_json,
        }
        if rot_spec:
            el_entry["rotation"] = {
                "origin": [round(float(v), 3) for v in rot_spec["origin"]],
                "axis": rot_spec["axis"],
                "angle": float(rot_spec["angle"]),
            }
        if int(elem.get("light_emission", light_emission)) > 0:
            el_entry["light_emission"] = int(elem.get("light_emission", light_emission))
        json_elements.append(el_entry)

    blend_out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(blend_out))

    block_model_json = {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"{modid}:block/{block_name}",
            "all": f"{modid}:block/{block_name}",
        },
        "elements": json_elements,
    }
    block_model_path = res_root / "models" / "block" / f"{block_name}.json"
    block_model_path.parent.mkdir(parents=True, exist_ok=True)
    block_model_path.write_text(json.dumps(block_model_json, indent=2) + "\n", encoding="utf-8")

    blockstate_json = {
        "variants": {
            "": {"model": f"{modid}:block/{block_name}"}
        }
    }
    blockstate_path = res_root / "blockstates" / f"{block_name}.json"
    blockstate_path.parent.mkdir(parents=True, exist_ok=True)
    blockstate_path.write_text(json.dumps(blockstate_json, indent=2) + "\n", encoding="utf-8")

    # BlockItem model: for tcivilization:placeholder, item/placeholder.json is the 2D item fallback
    if not (modid == "tcivilization" and block_name == "placeholder"):
        item_model_json = {"parent": f"{modid}:block/{block_name}"}
        item_model_path = res_root / "models" / "item" / f"{block_name}.json"
        item_model_path.parent.mkdir(parents=True, exist_ok=True)
        item_model_path.write_text(json.dumps(item_model_json, indent=2) + "\n", encoding="utf-8")

    return {"element_count": len(json_elements), "block_model": str(block_model_path)}


# ---------------------------------------------------------------------------
# 2. Blender Cycles 2048x / 1024x PBR Master Baking
# ---------------------------------------------------------------------------

def _create_quadrant_relief_plane(
    quad_idx: int,
    mat: bpy.types.Material,
    relief_kind: str,
) -> list[bpy.types.Object]:
    """Create UV-mapped quadrant [0..1] sub-plane + 3D beveled relief details for genuine Cycles AO & height."""
    # Quadrant UV bounds in Blender UV space (U: 0..1 left-to-right, V: 0..1 bottom-to-top)
    # Recall MC v=0 is top => V_blender = 1 - v_mc/16
    qu0, qv0, qu1, qv1 = QUADRANTS[quad_idx]
    u_min, u_max = qu0 / 16.0, qu1 / 16.0
    v_max, v_min = 1.0 - qv0 / 16.0, 1.0 - qv1 / 16.0

    # World placement mirrors UV [0..2m] x [0..2m] so neighboring quadrants don't cross-shadow
    wx0, wx1 = u_min * 2.0, u_max * 2.0
    wy0, wy1 = v_min * 2.0, v_max * 2.0

    bm = bmesh.new()
    uv_layer = bm.loops.layers.uv.new("UVMap")
    v0 = bm.verts.new((wx0, wy0, 0.0))
    v1 = bm.verts.new((wx1, wy0, 0.0))
    v2 = bm.verts.new((wx1, wy1, 0.0))
    v3 = bm.verts.new((wx0, wy1, 0.0))
    face = bm.faces.new((v0, v1, v2, v3))
    for loop, uv in zip(face.loops, [(u_min, v_min), (u_max, v_min), (u_max, v_max), (u_min, v_max)]):
        loop[uv_layer].uv = uv

    mesh = bpy.data.meshes.new(f"quad_{quad_idx}_base")
    bm.to_mesh(mesh)
    bm.free()
    base_obj = bpy.data.objects.new(f"Quad_{quad_idx}_Base", mesh)
    base_obj.data.materials.append(mat)
    bpy.context.collection.objects.link(base_obj)

    # Add 3D occluder ribs/grooves just above the plane to cast real geometric Cycles AO into crevices
    occluder_objs: list[bpy.types.Object] = []
    bm_occ = bmesh.new()
    cx, cy = 0.5 * (wx0 + wx1), 0.5 * (wy0 + wy1)
    span = wx1 - wx0

    import mathutils

    def add_bar(bx0: float, by0: float, bx1: float, by1: float, bz0: float, bz1: float) -> None:
        bmesh.ops.create_cube(bm_occ, size=1.0, matrix=mathutils.Matrix((
            ((bx1 - bx0), 0.0, 0.0, 0.5 * (bx0 + bx1)),
            (0.0, (by1 - by0), 0.0, 0.5 * (by0 + by1)),
            (0.0, 0.0, (bz1 - bz0), 0.5 * (bz0 + bz1)),
            (0.0, 0.0, 0.0, 1.0),
        )))

    # Border frame ribs cast realistic edge crevice AO
    t = span * 0.045
    add_bar(wx0, wy0, wx1, wy0 + t, 0.0, 0.018)
    add_bar(wx0, wy1 - t, wx1, wy1, 0.0, 0.018)
    add_bar(wx0, wy0, wx0 + t, wy1, 0.0, 0.018)
    add_bar(wx1 - t, wy0, wx1, wy1, 0.0, 0.018)

    if relief_kind == "chiseled_glyph":
        # Stele archaic concentric frame + cross bars
        m = span * 0.18
        add_bar(wx0 + m, wy0 + m, wx1 - m, wy0 + m + t, 0.0, 0.022)
        add_bar(wx0 + m, wy1 - m - t, wx1 - m, wy1 - m, 0.0, 0.022)
        add_bar(cx - t * 0.6, wy0 + m, cx + t * 0.6, wy1 - m, 0.0, 0.020)
    elif relief_kind == "planks":
        for i in (1, 2, 3):
            py = wy0 + span * (i / 4.0)
            add_bar(wx0, py - t * 0.35, wx1, py + t * 0.35, 0.0, 0.014)
    elif relief_kind == "cobble_ring":
        for i in range(4):
            ang = i * math.pi * 0.5 + 0.4
            rx = cx + math.cos(ang) * span * 0.24
            ry = cy + math.sin(ang) * span * 0.24
            add_bar(rx - span * 0.09, ry - span * 0.09, rx + span * 0.09, ry + span * 0.09, 0.0, 0.025)
    elif relief_kind == "panel_rivets":
        inset = span * 0.14
        for sx in (wx0 + inset, wx1 - inset):
            for sy in (wy0 + inset, wy1 - inset):
                add_bar(sx - t * 0.8, sy - t * 0.8, sx + t * 0.8, sy + t * 0.8, 0.0, 0.020)

    occ_mesh = bpy.data.meshes.new(f"quad_{quad_idx}_occ")
    bm_occ.to_mesh(occ_mesh)
    bm_occ.free()
    occ_obj = bpy.data.objects.new(f"Quad_{quad_idx}_Occ", occ_mesh)
    occ_obj.data.materials.append(mat)
    bpy.context.collection.objects.link(occ_obj)
    occluder_objs.append(occ_obj)

    return [base_obj] + occluder_objs


def build_pbr_shader(
    name: str,
    rgb_srgb: list[int],
    smoothness_s: int,
    noise_scale: float = 24.0,
    bump_strength: float = 0.35,
    wave_bands: float = 0.0,
    voronoi_mix: float = 0.25,
    color_spread: float = 0.14,
) -> tuple[bpy.types.Material, bpy.types.NodeSocket]:
    """Build a Cycles Principled BSDF procedural material calibrated to art-src/materials.json."""
    mat = bpy.data.materials.new(name=name)
    nt = mat.node_tree
    nodes = nt.nodes
    links = nt.links
    nodes.clear()

    out = nodes.new("ShaderNodeOutputMaterial")
    out.location = (900, 0)
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.location = (580, 0)
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])

    coord = nodes.new("ShaderNodeTexCoord")
    coord.location = (-900, 0)

    noise = nodes.new("ShaderNodeTexNoise")
    noise.location = (-620, 180)
    noise.inputs["Scale"].default_value = noise_scale
    noise.inputs["Detail"].default_value = 8.0
    noise.inputs["Roughness"].default_value = 0.62
    links.new(coord.outputs["UV"], noise.inputs["Vector"])

    voro = nodes.new("ShaderNodeTexVoronoi")
    voro.location = (-620, -80)
    voro.inputs["Scale"].default_value = noise_scale * 0.55
    links.new(coord.outputs["UV"], voro.inputs["Vector"])

    # Blend Noise + Voronoi (+ optional Wave for wood/layered rock) into height scalar [0..1]
    mix_h = nodes.new("ShaderNodeMix")
    mix_h.data_type = "FLOAT"
    mix_h.location = (-340, 60)
    mix_h.inputs["Factor"].default_value = voronoi_mix
    links.new(noise.outputs["Fac"], mix_h.inputs[2])
    links.new(voro.outputs["Distance"], mix_h.inputs[3])
    height_socket = mix_h.outputs[0]

    if wave_bands > 0.0:
        wave = nodes.new("ShaderNodeTexWave")
        wave.location = (-620, -320)
        wave.wave_type = "BANDS"
        wave.inputs["Scale"].default_value = wave_bands
        wave.inputs["Distortion"].default_value = 3.5
        wave.inputs["Detail"].default_value = 5.0
        links.new(coord.outputs["UV"], wave.inputs["Vector"])

        mix_w = nodes.new("ShaderNodeMix")
        mix_w.data_type = "FLOAT"
        mix_w.location = (-160, 40)
        mix_w.inputs["Factor"].default_value = 0.42
        links.new(height_socket, mix_w.inputs[2])
        links.new(wave.outputs["Fac"], mix_w.inputs[3])
        height_socket = mix_w.outputs[0]

    # Color ramp around calibrated material sRGB albedo (AR-2 restrained saturation)
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.location = (80, 220)
    ramp.color_ramp.elements[0].position = 0.18
    ramp.color_ramp.elements[0].color = srgb_rgb255_to_linear_rgba(rgb_srgb, 1.0 - color_spread)
    ramp.color_ramp.elements[1].position = 0.84
    ramp.color_ramp.elements[1].color = srgb_rgb255_to_linear_rgba(rgb_srgb, 1.0 + color_spread)
    links.new(height_socket, ramp.inputs["Fac"])
    links.new(ramp.outputs["Color"], bsdf.inputs["Base Color"])

    # Calibrated roughness around inverse of smoothness S
    base_r = smoothness_to_roughness(smoothness_s)
    map_r = nodes.new("ShaderNodeMapRange")
    map_r.location = (80, -20)
    map_r.inputs["From Min"].default_value = 0.0
    map_r.inputs["From Max"].default_value = 1.0
    map_r.inputs["To Min"].default_value = max(0.02, base_r - 0.06)
    map_r.inputs["To Max"].default_value = min(0.99, base_r + 0.06)
    links.new(height_socket, map_r.inputs["Value"])
    links.new(map_r.outputs["Result"], bsdf.inputs["Roughness"])

    # Tangent-space bump -> Principled Normal
    bump = nodes.new("ShaderNodeBump")
    bump.location = (280, -200)
    bump.inputs["Strength"].default_value = bump_strength
    bump.inputs["Distance"].default_value = 0.02
    links.new(height_socket, bump.inputs["Height"])
    links.new(bump.outputs["Normal"], bsdf.inputs["Normal"])

    return mat, height_socket


def bake_block_pbr_maps(
    asset_key: str,
    quad_configs: list[dict[str, Any]],
    res: int,
    out_dir: Path,
) -> None:
    """Bake 2048x2048 albedo, normal (OpenGL Y+), height, ao, roughness (+ optional f0/emissive) in Cycles."""
    scene = reset_cycles_scene(ao_samples=4)
    bake_planes: list[bpy.types.Object] = []
    mat_height_sockets: list[tuple[bpy.types.Material, bpy.types.NodeSocket]] = []

    for q_idx, cfg in enumerate(quad_configs):
        mat, h_sock = build_pbr_shader(
            f"{asset_key}_q{q_idx}",
            cfg["albedo"],
            cfg["S"],
            noise_scale=cfg.get("noise_scale", 26.0),
            bump_strength=cfg.get("bump_strength", 0.35),
            wave_bands=cfg.get("wave_bands", 0.0),
            voronoi_mix=cfg.get("voronoi_mix", 0.28),
            color_spread=cfg.get("color_spread", 0.14),
        )
        mat_height_sockets.append((mat, h_sock))
        objs = _create_quadrant_relief_plane(q_idx, mat, cfg.get("relief", "stone"))
        bake_planes.append(objs[0])  # base UV plane receives the bake

    def run_cycles_bake(bake_type: str, is_srgb: bool, samples: int = 1, normal_opengl: bool = False) -> np.ndarray:
        scene.cycles.samples = samples
        img = bpy.data.images.new(f"{asset_key}_{bake_type}", width=res, height=res, alpha=True)
        img.colorspace_settings.name = "sRGB" if is_srgb else "Non-Color"

        nodes_added = []
        for mat, _ in mat_height_sockets:
            nt = mat.node_tree
            tn = nt.nodes.new("ShaderNodeTexImage")
            tn.image = img
            for n in nt.nodes:
                n.select = False
            tn.select = True
            nt.nodes.active = tn
            nodes_added.append((nt, tn))

        bpy.ops.object.select_all(action="DESELECT")
        for p in bake_planes:
            p.select_set(True)
        bpy.context.view_layer.objects.active = bake_planes[0]

        scene.render.bake.margin = 4
        scene.render.bake.use_clear = True
        if bake_type == "DIFFUSE":
            scene.render.bake.use_pass_direct = False
            scene.render.bake.use_pass_indirect = False
            scene.render.bake.use_pass_color = True
        if normal_opengl:
            scene.render.bake.normal_space = "TANGENT"
            scene.render.bake.normal_r = "POS_X"
            scene.render.bake.normal_g = "POS_Y"
            scene.render.bake.normal_b = "POS_Z"

        bpy.ops.object.bake(type=bake_type)

        for nt, tn in nodes_added:
            nt.nodes.remove(tn)
        arr = np.array(img.pixels[:], dtype=np.float32).reshape((res, res, 4))
        bpy.data.images.remove(img)
        return arr

    # 1. Albedo, Normal (OpenGL Y+), Roughness (1 sample deterministic), AO (4 samples geometric + crevices)
    albedo_arr = run_cycles_bake("DIFFUSE", is_srgb=True, samples=1)
    albedo_arr[:, :, 3] = 1.0
    normal_arr = run_cycles_bake("NORMAL", is_srgb=False, samples=1, normal_opengl=True)
    normal_arr[:, :, 3] = 1.0
    rough_arr = run_cycles_bake("ROUGHNESS", is_srgb=False, samples=1)
    rough_arr[:, :, 3] = 1.0
    ao_arr = run_cycles_bake("AO", is_srgb=False, samples=4)
    ao_arr[:, :, 3] = 1.0

    # 2. Height map via EMIT pass wired to height_socket clamped to [0.05..1.0]
    orig_links = []
    emit_nodes = []
    for mat, h_sock in mat_height_sockets:
        nt = mat.node_tree
        out_node = next(n for n in nt.nodes if n.type == "OUTPUT_MATERIAL")
        bsdf_node = next(n for n in nt.nodes if n.type == "BSDF_PRINCIPLED")
        for l in list(out_node.inputs["Surface"].links):
            nt.links.remove(l)
        orig_links.append((nt, bsdf_node, out_node))
        mr = nt.nodes.new("ShaderNodeMapRange")
        mr.inputs["From Min"].default_value = 0.0
        mr.inputs["From Max"].default_value = 1.0
        mr.inputs["To Min"].default_value = 0.12
        mr.inputs["To Max"].default_value = 1.0
        em = nt.nodes.new("ShaderNodeEmission")
        nt.links.new(h_sock, mr.inputs["Value"])
        nt.links.new(mr.outputs["Result"], em.inputs["Color"])
        nt.links.new(em.outputs["Emission"], out_node.inputs["Surface"])
        emit_nodes.append((nt, mr, em))

    height_arr = run_cycles_bake("EMIT", is_srgb=False, samples=1)
    # Modulate height slightly with AO crevices so mortar/grooves sit lower
    height_rgb = np.clip(height_arr[:, :, 0:3] * (0.72 + 0.28 * ao_arr[:, :, 0:3]), 0.04, 1.0)
    height_arr = np.concatenate([height_rgb, np.ones((res, res, 1), dtype=np.float32)], axis=2)

    save_float_rgba_png(albedo_arr, out_dir / "albedo.png", is_srgb=True)
    save_float_rgba_png(normal_arr, out_dir / "normal.png", is_srgb=False)
    save_float_rgba_png(rough_arr, out_dir / "roughness.png", is_srgb=False)
    save_float_rgba_png(ao_arr, out_dir / "ao.png", is_srgb=False)
    save_float_rgba_png(height_arr, out_dir / "height.png", is_srgb=False)

    # Optional per-quadrant F0 / Emissive maps for multi-material or glowing-ember blocks
    has_custom_f0 = any("F0" in q for q in quad_configs)
    has_emissive = any(q.get("emissive_a", 255) < 255 for q in quad_configs)
    half = res // 2
    # Note: bottom-up row layout in Blender: V in [0..0.5] is rows [0..half] (MC bottom quadrants Q2, Q3),
    # V in [0.5..1.0] is rows [half..res] (MC top quadrants Q0, Q1).
    q_slices = {
        0: (slice(half, res), slice(0, half)),
        1: (slice(half, res), slice(half, res)),
        2: (slice(0, half), slice(0, half)),
        3: (slice(0, half), slice(half, res)),
    }
    if has_custom_f0:
        f0_buf = np.zeros((res, res, 4), dtype=np.float32)
        f0_buf[:, :, 3] = 1.0
        for qi, qcfg in enumerate(quad_configs):
            val = float(qcfg.get("F0", 10)) / 255.0
            rs, cs = q_slices[qi]
            f0_buf[rs, cs, 0:3] = val
        save_float_rgba_png(f0_buf, out_dir / "f0.png", is_srgb=False)

    if has_emissive:
        em_buf = np.ones((res, res, 4), dtype=np.float32)  # 1.0 => 255 non-emissive
        for qi, qcfg in enumerate(quad_configs):
            em_a = int(qcfg.get("emissive_a", 255))
            if em_a < 255:
                rs, cs = q_slices[qi]
                # Glowing ember / indicator core modulated by height
                glow_mask = height_arr[rs, cs, 0] > 0.45
                sub = em_buf[rs, cs, 0]
                sub[glow_mask] = float(em_a) / 255.0
                em_buf[rs, cs, 0] = sub
                em_buf[rs, cs, 1] = sub
                em_buf[rs, cs, 2] = sub
        save_float_rgba_png(em_buf, out_dir / "emissive.png", is_srgb=False)


def bake_item_pbr_master(
    modid: str,
    item_name: str,
    kind: str,
    rgb_main: list[int],
    rgb_accent: list[int],
    smoothness_s: int,
    res: int,
    out_dir: Path,
    blend_out: Path,
    res_root: Path,
) -> None:
    """Model a real 3D relief item token in Blender, bake 1024x PBR maps with alpha cutout silhouette, and write item JSON."""
    scene = reset_cycles_scene(ao_samples=4)

    mat_main, h_main = build_pbr_shader(f"{item_name}_main", rgb_main, smoothness_s, noise_scale=20.0, bump_strength=0.4)
    mat_acc, h_acc = build_pbr_shader(f"{item_name}_acc", rgb_accent, max(25, smoothness_s - 15), noise_scale=38.0, bump_strength=0.3)

    # Build 3D relief token inside [-0.38..0.38] XY on a [0..1] UV bake plane
    bm = bmesh.new()
    uv_layer = bm.loops.layers.uv.new("UVMap")
    if kind == "stele_tablet":
        # Octagonal carved stone tablet silhouette
        pts = [
            (-0.26, -0.38), (0.26, -0.38), (0.36, -0.26), (0.36, 0.26),
            (0.22, 0.38), (-0.22, 0.38), (-0.36, 0.26), (-0.36, -0.26),
        ]
    else:
        # Broken archaic seal tablet bound with cordage (legacy_item)
        pts = [
            (-0.32, -0.34), (0.24, -0.36), (0.36, -0.10), (0.18, 0.08),
            (0.30, 0.32), (-0.14, 0.36), (-0.36, 0.18), (-0.28, -0.08),
        ]

    bottom_vs = [bm.verts.new((x, y, 0.0)) for x, y in pts]
    top_vs = [bm.verts.new((x * 0.92, y * 0.92, 0.08)) for x, y in pts]
    f_top = bm.faces.new(top_vs)
    for i in range(len(pts)):
        ni = (i + 1) % len(pts)
        bm.faces.new((bottom_vs[i], bottom_vs[ni], top_vs[ni], top_vs[i]))

    for f in bm.faces:
        for loop in f.loops:
            co = loop.vert.co
            loop[uv_layer].uv = (co.x + 0.5, co.y + 0.5)

    mesh = bpy.data.meshes.new(f"{item_name}_mesh")
    bm.to_mesh(mesh)
    bm.free()
    obj_main = bpy.data.objects.new(f"{item_name}_tablet", mesh)
    obj_main.data.materials.append(mat_main)
    bpy.context.collection.objects.link(obj_main)

    # Raised inner emblem / cordage band
    bm2 = bmesh.new()
    uv2 = bm2.loops.layers.uv.new("UVMap")
    if kind == "stele_tablet":
        ring_boxes = [
            (-0.18, -0.24, 0.18, -0.17),
            (-0.18, 0.17, 0.18, 0.24),
            (-0.04, -0.18, 0.04, 0.18),
        ]
    else:
        ring_boxes = [
            (-0.33, -0.06, 0.26, 0.05),
            (-0.08, -0.28, 0.05, 0.28),
        ]
    for bx0, by0, bx1, by1 in ring_boxes:
        v_a = bm2.verts.new((bx0, by0, 0.081))
        v_b = bm2.verts.new((bx1, by0, 0.081))
        v_c = bm2.verts.new((bx1, by1, 0.081))
        v_d = bm2.verts.new((bx0, by1, 0.081))
        v_a2 = bm2.verts.new((bx0, by0, 0.115))
        v_b2 = bm2.verts.new((bx1, by0, 0.115))
        v_c2 = bm2.verts.new((bx1, by1, 0.115))
        v_d2 = bm2.verts.new((bx0, by1, 0.115))
        bm2.faces.new((v_a2, v_b2, v_c2, v_d2))
        bm2.faces.new((v_a, v_b, v_b2, v_a2))
        bm2.faces.new((v_b, v_c, v_c2, v_b2))
        bm2.faces.new((v_c, v_d, v_d2, v_c2))
        bm2.faces.new((v_d, v_a, v_a2, v_d2))
    for f in bm2.faces:
        for loop in f.loops:
            co = loop.vert.co
            loop[uv2].uv = (co.x + 0.5, co.y + 0.5)
    mesh2 = bpy.data.meshes.new(f"{item_name}_emblem")
    bm2.to_mesh(mesh2)
    bm2.free()
    obj_acc = bpy.data.objects.new(f"{item_name}_emblem", mesh2)
    obj_acc.data.materials.append(mat_acc)
    bpy.context.collection.objects.link(obj_acc)

    blend_out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(blend_out))

    targets = [obj_main, obj_acc]
    mats = [(mat_main, h_main), (mat_acc, h_acc)]

    def bake_pass(btype: str, is_srgb: bool, samples: int = 1, normal_gl: bool = False) -> np.ndarray:
        scene.cycles.samples = samples
        img = bpy.data.images.new(f"{item_name}_{btype}", width=res, height=res, alpha=True)
        img.colorspace_settings.name = "sRGB" if is_srgb else "Non-Color"
        img.generated_color = (0.0, 0.0, 0.0, 0.0)
        added = []
        for m, _ in mats:
            nt = m.node_tree
            tn = nt.nodes.new("ShaderNodeTexImage")
            tn.image = img
            for n in nt.nodes:
                n.select = False
            tn.select = True
            nt.nodes.active = tn
            added.append((nt, tn))
        bpy.ops.object.select_all(action="DESELECT")
        for t in targets:
            t.select_set(True)
        bpy.context.view_layer.objects.active = targets[0]
        scene.render.bake.margin = 0  # Keep crisp silhouette alpha!
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
        arr = np.array(img.pixels[:], dtype=np.float32).reshape((res, res, 4))
        bpy.data.images.remove(img)
        return arr

    alb = bake_pass("DIFFUSE", is_srgb=True, samples=1)
    mask = alb[:, :, 3:4] > 0.05
    alb_alpha = np.where(mask, 1.0, 0.0).astype(np.float32)
    alb = np.concatenate([alb[:, :, 0:3] * alb_alpha, alb_alpha], axis=2)

    nrm = bake_pass("NORMAL", is_srgb=False, samples=1, normal_gl=True)
    default_nrm = np.broadcast_to(np.array([0.5, 0.5, 1.0], dtype=np.float32), (res, res, 3))
    nrm_rgb = np.where(mask, nrm[:, :, 0:3], default_nrm)
    nrm = np.concatenate([nrm_rgb, np.ones((res, res, 1), dtype=np.float32)], axis=2)

    rgh = bake_pass("ROUGHNESS", is_srgb=False, samples=1)
    base_r = smoothness_to_roughness(smoothness_s)
    rgh_rgb = np.where(mask, rgh[:, :, 0:3], base_r)
    rgh = np.concatenate([rgh_rgb, np.ones((res, res, 1), dtype=np.float32)], axis=2)

    ao = bake_pass("AO", is_srgb=False, samples=4)
    ao_rgb = np.where(mask, ao[:, :, 0:3], 1.0)
    ao = np.concatenate([ao_rgb, np.ones((res, res, 1), dtype=np.float32)], axis=2)

    hgt_rgb = np.where(mask, np.clip(0.35 + 0.65 * ao_rgb, 0.08, 1.0), 0.04)
    hgt = np.concatenate([hgt_rgb, np.ones((res, res, 1), dtype=np.float32)], axis=2)

    save_float_rgba_png(alb, out_dir / "albedo.png", is_srgb=True)
    save_float_rgba_png(nrm, out_dir / "normal.png", is_srgb=False)
    save_float_rgba_png(rgh, out_dir / "roughness.png", is_srgb=False)
    save_float_rgba_png(ao, out_dir / "ao.png", is_srgb=False)
    save_float_rgba_png(hgt, out_dir / "height.png", is_srgb=False)

    item_json = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"{modid}:item/{item_name}"
        }
    }
    item_model_path = res_root / "models" / "item" / f"{item_name}.json"
    item_model_path.parent.mkdir(parents=True, exist_ok=True)
    item_model_path.write_text(json.dumps(item_json, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# 3. UI Nine-Slice Skin Master Baking in Blender (neutral & E0 stone)
# ---------------------------------------------------------------------------

def bake_ui_skin_masters(
    modid: str,
    skin_name: str,
    style: str,
    res: int,
    out_dir: Path,
    blend_out: Path,
) -> None:
    """Bake 512x512 nine-slice UI skin masters (frame, button, tab, scrollbar, toast) in Blender Cycles."""
    scene = reset_cycles_scene(ao_samples=1)
    # Create a [0..1] UV plane in Blender and bake procedural hide/neutral grain, then stamp exact 6/64 nine-slice frame
    if style == "neutral":
        # 31-ui-ux.md section 10.2 Rule 5: panel #D4D4D0, background #E6E6E3, border #4A4A4A, text #1F1F1F
        mat, _ = build_pbr_shader("skin_neutral", [212, 212, 208], 120, noise_scale=16.0, bump_strength=0.02, color_spread=0.005)
    else:
        # 32-art-audio.md section 5.1: E0 stone tanned hide (#DFCFAF) + charcoal hand-drawn border (#231C16)
        mat, _ = build_pbr_shader("skin_stone_hide", [223, 207, 175], 45, noise_scale=32.0, bump_strength=0.25, voronoi_mix=0.35, color_spread=0.07)

    bm = bmesh.new()
    uv_l = bm.loops.layers.uv.new("UVMap")
    vs = [bm.verts.new((0, 0, 0)), bm.verts.new((1, 0, 0)), bm.verts.new((1, 1, 0)), bm.verts.new((0, 1, 0))]
    f = bm.faces.new(vs)
    for loop, uv in zip(f.loops, [(0, 0), (1, 0), (1, 1), (0, 1)]):
        loop[uv_l].uv = uv
    mesh = bpy.data.meshes.new(f"skin_{skin_name}_plane")
    bm.to_mesh(mesh)
    bm.free()
    plane_obj = bpy.data.objects.new(f"Skin_{skin_name}", mesh)
    plane_obj.data.materials.append(mat)
    bpy.context.collection.objects.link(plane_obj)

    blend_out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(blend_out))

    img = bpy.data.images.new("skin_bake", width=res, height=res, alpha=True)
    img.colorspace_settings.name = "sRGB"
    nt = mat.node_tree
    tn = nt.nodes.new("ShaderNodeTexImage")
    tn.image = img
    for n in nt.nodes:
        n.select = False
    tn.select = True
    nt.nodes.active = tn
    bpy.ops.object.select_all(action="DESELECT")
    plane_obj.select_set(True)
    bpy.context.view_layer.objects.active = plane_obj
    scene.render.bake.use_pass_direct = False
    scene.render.bake.use_pass_indirect = False
    scene.render.bake.use_pass_color = True
    bpy.ops.object.bake(type="DIFFUSE")

    base_linear = np.array(img.pixels[:], dtype=np.float32).reshape((res, res, 4))
    bpy.data.images.remove(img)

    # Scale factor from 64 logical GUI pixels (nine_slice margin = 6 => 6/64 of res)
    px = max(1, res // 64)
    margin = 6 * px

    def make_widget_variant(variant: str) -> np.ndarray:
        buf = base_linear.copy()
        buf[:, :, 3] = 1.0
        if style == "neutral":
            # Exact flat #D4D4D0 interior / #E6E6E3 button/toast highlight + 1 GUI px #4A4A4A solid border
            fill_rgb = [230, 230, 227] if variant in ("button", "toast") else [212, 212, 208]
            if variant == "scrollbar":
                fill_rgb = [186, 186, 182]
            c_fill = np.array(srgb_rgb255_to_linear_rgba(fill_rgb)[:3], dtype=np.float32)
            c_border = np.array(srgb_rgb255_to_linear_rgba([74, 74, 74])[:3], dtype=np.float32)
            c_bevel = np.array(srgb_rgb255_to_linear_rgba([242, 242, 240])[:3], dtype=np.float32)
            buf[:, :, 0:3] = c_fill
            # Inner subtle 1px highlight inside 6px nine-slice zone
            buf[px : 2 * px, px : res - px, 0:3] = c_bevel
            buf[res - 2 * px : res - px, px : res - px, 0:3] = c_bevel
            # Crisp 1 GUI-px outer #4A4A4A border
            buf[0:px, :, 0:3] = c_border
            buf[res - px : res, :, 0:3] = c_border
            buf[:, 0:px, 0:3] = c_border
            buf[:, res - px : res, 0:3] = c_border
        else:
            # E0 Stone: keep Cycles-baked tanned hide interior, add hand-drawn charcoal double border in 6-px margin
            yy, xx = np.mgrid[0:res, 0:res]
            wobble = np.sin(xx * 0.09) * np.cos(yy * 0.09)
            charcoal = np.array(srgb_rgb255_to_linear_rgba([36, 28, 22])[:3], dtype=np.float32)
            ochre_stitch = np.array(srgb_rgb255_to_linear_rgba([112, 74, 44])[:3], dtype=np.float32)
            if variant == "button":
                buf[:, :, 0:3] = np.clip(buf[:, :, 0:3] * 0.94, 0.0, 1.0)
            elif variant == "scrollbar":
                buf[:, :, 0:3] = np.clip(buf[:, :, 0:3] * 0.78, 0.0, 1.0)

            outer = (xx < 2 * px) | (xx >= res - 2 * px) | (yy < 2 * px) | (yy >= res - 2 * px)
            inner_line = (
                ((xx >= 4 * px) & (xx < 5 * px) & (yy >= 3 * px) & (yy < res - 3 * px))
                | ((xx >= res - 5 * px) & (xx < res - 4 * px) & (yy >= 3 * px) & (yy < res - 3 * px))
                | ((yy >= 4 * px) & (yy < 5 * px) & (xx >= 3 * px) & (xx < res - 3 * px))
                | ((yy >= res - 5 * px) & (yy < res - 4 * px) & (xx >= 3 * px) & (xx < res - 3 * px))
            ) & (wobble > -0.65)
            corner = (
                ((xx < margin) & (yy < margin))
                | ((xx >= res - margin) & (yy < margin))
                | ((xx < margin) & (yy >= res - margin))
                | ((xx >= res - margin) & (yy >= res - margin))
            ) & (((xx // px + yy // px) % 3) == 0)
            buf[inner_line, 0:3] = ochre_stitch
            buf[outer, 0:3] = charcoal
            buf[corner & ~outer, 0:3] = ochre_stitch
        return buf

    for wname in ("frame", "button", "tab", "scrollbar", "toast"):
        save_float_rgba_png(make_widget_variant(wname), out_dir / f"{wname}.png", is_srgb=True)


# ---------------------------------------------------------------------------
# 4. Asset Definitions & Execution
# ---------------------------------------------------------------------------

def main() -> int:
    args = _parse_args()
    repo_root = args.repo_root.resolve()
    materials_db = json.loads((repo_root / "art-src" / "materials.json").read_text(encoding="utf-8"))["materials"]
    blend_root = repo_root / "art-src" / "blend"

    block_defs: list[dict[str, Any]] = [
        {
            "modid": "tcivilization",
            "name": "placeholder",
            "material": "slate",
            "iris_category": "solid",
            "emissive": False,
            "description": "RM-P8 chiseled stone stele placeholder block (zero missing-texture checkerboard)",
            "elements": [
                {"name": "plinth_lower", "from": [1, 0, 1], "to": [15, 2, 15], "quadrant": 0},
                {"name": "plinth_upper", "from": [2, 2, 2], "to": [14, 4, 14], "quadrant": 1},
                {"name": "stele_core", "from": [3, 4, 3], "to": [13, 13, 13], "quadrant": 2, "face_quadrants": {"north": 3, "south": 3, "east": 3, "west": 3}},
                {"name": "pillar_nw", "from": [2.5, 4, 2.5], "to": [4, 13, 4], "quadrant": 0},
                {"name": "pillar_ne", "from": [12, 4, 2.5], "to": [13.5, 13, 4], "quadrant": 0},
                {"name": "pillar_sw", "from": [2.5, 4, 12], "to": [4, 13, 13.5], "quadrant": 0},
                {"name": "pillar_se", "from": [12, 4, 12], "to": [13.5, 13, 13.5], "quadrant": 0},
                {"name": "cap_collar", "from": [2, 13, 2], "to": [14, 14.5, 14], "quadrant": 1},
                {"name": "cap_crown", "from": [3.5, 14.5, 3.5], "to": [12.5, 16, 12.5], "quadrant": 0},
            ],
            "quads": [
                {"albedo": materials_db["slate"]["albedo"], "S": materials_db["slate"]["S"], "relief": "stone", "wave_bands": 4.0},
                {"albedo": materials_db["granite"]["albedo"], "S": materials_db["granite"]["S"], "relief": "stone"},
                {"albedo": [82, 86, 92], "S": 75, "relief": "stone"},
                {"albedo": [96, 100, 106], "S": 65, "relief": "chiseled_glyph", "bump_strength": 0.50},
            ],
        },
        {
            "modid": "tctechnology",
            "name": "fire_pit",
            "material": "basalt_rock",
            "iris_category": "emissive_partial",
            "emissive": True,
            "description": "E0 tribal hearth / fire pit with river-basalt ring, split oak fuel logs, and glowing ember bed",
            "elements": [
                {"name": "ash_bed", "from": [2, 0, 2], "to": [14, 1.5, 14], "quadrant": 2},
                {"name": "ember_core", "from": [4.5, 1.5, 4.5], "to": [11.5, 3.0, 11.5], "quadrant": 3, "light_emission": 12},
                {"name": "stone_n", "from": [5, 0, 1], "to": [11, 3.5, 3.5], "quadrant": 0},
                {"name": "stone_s", "from": [5, 0, 12.5], "to": [11, 3.5, 15], "quadrant": 0},
                {"name": "stone_w", "from": [1, 0, 5], "to": [3.5, 3.5, 11], "quadrant": 0},
                {"name": "stone_e", "from": [12.5, 0, 5], "to": [15, 3.5, 11], "quadrant": 0},
                {"name": "stone_nw", "from": [2, 0, 2], "to": [5, 3, 5], "quadrant": 0},
                {"name": "stone_ne", "from": [11, 0, 2], "to": [14, 3, 5], "quadrant": 0},
                {"name": "stone_sw", "from": [2, 0, 11], "to": [5, 3, 14], "quadrant": 0},
                {"name": "stone_se", "from": [11, 0, 11], "to": [14, 3, 14], "quadrant": 0},
                {"name": "log_ns_1", "from": [4, 2, 3], "to": [6.5, 4.5, 13], "quadrant": 1, "rotation": {"origin": [8, 3, 8], "axis": "y", "angle": 22.5}},
                {"name": "log_ns_2", "from": [9.5, 2, 3], "to": [12, 4.5, 13], "quadrant": 1, "rotation": {"origin": [8, 3, 8], "axis": "y", "angle": -22.5}},
                {"name": "log_ew_1", "from": [3, 4, 5], "to": [13, 6.2, 7.2], "quadrant": 1, "rotation": {"origin": [8, 5, 8], "axis": "y", "angle": 45.0}},
                {"name": "log_ew_2", "from": [3, 4, 8.8], "to": [13, 6.2, 11], "quadrant": 1, "rotation": {"origin": [8, 5, 8], "axis": "y", "angle": -45.0}},
            ],
            "quads": [
                {"albedo": materials_db["basalt_rock"]["albedo"], "S": materials_db["basalt_rock"]["S"], "F0": 10, "relief": "cobble_ring"},
                {"albedo": [92, 68, 44], "S": 38, "F0": 10, "relief": "planks", "wave_bands": 7.0},
                {"albedo": [98, 95, 90], "S": 18, "F0": 10, "relief": "stone", "noise_scale": 48.0},
                {"albedo": [196, 88, 34], "S": 45, "F0": 10, "emissive_a": 90, "relief": "cobble_ring", "color_spread": 0.25},
            ],
        },
        {
            "modid": "tctechnology",
            "name": "grinding_slab",
            "material": "sandstone_rock",
            "iris_category": "solid",
            "emissive": False,
            "description": "E1 Neolithic concave sandstone grinding slab with wet abrasive trough and granite hand muller",
            "elements": [
                {"name": "base_slab", "from": [1, 0, 1], "to": [15, 5, 15], "quadrant": 0},
                {"name": "rim_north", "from": [1, 5, 1], "to": [15, 7, 3.5], "quadrant": 0},
                {"name": "rim_south", "from": [1, 5, 12.5], "to": [15, 7, 15], "quadrant": 0},
                {"name": "rim_west", "from": [1, 5, 3.5], "to": [3, 7, 12.5], "quadrant": 0},
                {"name": "rim_east", "from": [13, 5, 3.5], "to": [15, 6.2, 12.5], "quadrant": 0},
                {"name": "trough_bed", "from": [3, 5, 3.5], "to": [13, 5.6, 12.5], "quadrant": 2},
                {"name": "muller_stone", "from": [5.5, 5.6, 5.5], "to": [10.5, 7.6, 9.5], "quadrant": 1, "rotation": {"origin": [8, 6.5, 7.5], "axis": "y", "angle": 22.5}},
                {"name": "axe_blank_prop", "from": [4, 5.6, 9.8], "to": [7.5, 6.6, 12.0], "quadrant": 3},
            ],
            "quads": [
                {"albedo": materials_db["sandstone_rock"]["albedo"], "S": materials_db["sandstone_rock"]["S"], "relief": "stone", "wave_bands": 5.0},
                {"albedo": materials_db["granite"]["albedo"], "S": 85, "relief": "stone"},
                {"albedo": [156, 134, 96], "S": 135, "relief": "stone", "bump_strength": 0.18},
                {"albedo": materials_db["gabbro"]["albedo"], "S": 110, "relief": "stone"},
            ],
        },
        {
            "modid": "tctechnology",
            "name": "research_table",
            "material": "oak",
            "iris_category": "solid",
            "emissive": False,
            "description": "E1 fallback oak research table with tanned hide scroll, tally stick, and specimen bowl",
            "elements": [
                {"name": "leg_nw", "from": [1, 0, 1], "to": [3.5, 13, 3.5], "quadrant": 0},
                {"name": "leg_ne", "from": [12.5, 0, 1], "to": [15, 13, 3.5], "quadrant": 0},
                {"name": "leg_sw", "from": [1, 0, 12.5], "to": [3.5, 13, 15], "quadrant": 0},
                {"name": "leg_se", "from": [12.5, 0, 12.5], "to": [15, 13, 15], "quadrant": 0},
                {"name": "stretcher_n", "from": [3.5, 3, 1.8], "to": [12.5, 4.5, 3.0], "quadrant": 0},
                {"name": "stretcher_s", "from": [3.5, 3, 13.0], "to": [12.5, 4.5, 14.2], "quadrant": 0},
                {"name": "shelf_lower", "from": [3.0, 3.5, 3.0], "to": [13.0, 4.5, 13.0], "quadrant": 1},
                {"name": "tabletop", "from": [0, 13, 0], "to": [16, 15.5, 16], "quadrant": 1},
                {"name": "hide_scroll", "from": [2.5, 15.5, 3.0], "to": [11.5, 15.9, 13.0], "quadrant": 2},
                {"name": "tally_stick", "from": [3.5, 15.9, 4.0], "to": [9.5, 16.5, 5.2], "quadrant": 0, "rotation": {"origin": [6.5, 16.0, 4.6], "axis": "y", "angle": -22.5}},
                {"name": "specimen_bowl", "from": [12.0, 15.5, 3.5], "to": [14.5, 17.0, 6.0], "quadrant": 3},
                {"name": "flint_nodule", "from": [12.2, 15.5, 9.5], "to": [14.2, 16.8, 11.5], "quadrant": 3},
            ],
            "quads": [
                {"albedo": [136, 104, 62], "S": 45, "relief": "planks", "wave_bands": 8.0},
                {"albedo": materials_db["oak"]["albedo"], "S": materials_db["oak"]["S"], "relief": "planks", "wave_bands": 6.0},
                {"albedo": [212, 192, 154], "S": 40, "relief": "stone", "bump_strength": 0.22},
                {"albedo": materials_db["terracotta"]["albedo"], "S": materials_db["terracotta"]["S"], "relief": "stone"},
            ],
        },
        {
            "modid": "tctechnology",
            "name": "milestone_marker",
            "material": "granite",
            "iris_category": "solid",
            "emissive": False,
            "description": "Fallback milestone boundary obelisk with granite plinth, bronze collar, and directional stele",
            "elements": [
                {"name": "base_step_1", "from": [1, 0, 1], "to": [15, 2, 15], "quadrant": 0},
                {"name": "base_step_2", "from": [2.5, 2, 2.5], "to": [13.5, 4, 13.5], "quadrant": 0},
                {"name": "shaft_lower", "from": [4, 4, 4], "to": [12, 10, 12], "quadrant": 2},
                {"name": "bronze_collar", "from": [3.6, 10, 3.6], "to": [12.4, 11.5, 12.4], "quadrant": 1},
                {"name": "shaft_upper", "from": [4.5, 11.5, 4.5], "to": [11.5, 14.5, 11.5], "quadrant": 3},
                {"name": "capstone_1", "from": [5.2, 14.5, 5.2], "to": [10.8, 15.4, 10.8], "quadrant": 0},
                {"name": "capstone_peak", "from": [6.5, 15.4, 6.5], "to": [9.5, 16.0, 9.5], "quadrant": 1},
            ],
            "quads": [
                {"albedo": materials_db["granite"]["albedo"], "S": materials_db["granite"]["S"], "F0": 10, "relief": "stone"},
                {"albedo": materials_db["bronze"]["albedo"], "S": materials_db["bronze"]["S"], "F0": 255, "relief": "panel_rivets"},
                {"albedo": materials_db["limestone"]["albedo"], "S": materials_db["limestone"]["S"], "F0": 10, "relief": "stone"},
                {"albedo": materials_db["granite"]["albedo"], "S": 75, "F0": 10, "relief": "chiseled_glyph"},
            ],
        },
        {
            "modid": "tctechnology",
            "name": "milestone_cache",
            "material": "limestone",
            "iris_category": "solid",
            "emissive": False,
            "description": "Fallback milestone supply vault with dressed limestone walls, oak lid, and bronze brackets",
            "elements": [
                {"name": "vault_plinth", "from": [0.5, 0, 0.5], "to": [15.5, 2, 15.5], "quadrant": 0},
                {"name": "vault_body", "from": [1, 2, 1], "to": [15, 11.5, 15], "quadrant": 0},
                {"name": "lid_rim", "from": [0.5, 11.5, 0.5], "to": [15.5, 13.5, 15.5], "quadrant": 1},
                {"name": "lid_crown", "from": [1.5, 13.5, 1.5], "to": [14.5, 15, 14.5], "quadrant": 1},
                {"name": "strap_w", "from": [3, 11.4, 0.3], "to": [5, 15.2, 15.7], "quadrant": 2},
                {"name": "strap_e", "from": [11, 11.4, 0.3], "to": [13, 15.2, 15.7], "quadrant": 2},
                {"name": "clasp_front", "from": [6.5, 9.5, 0.2], "to": [9.5, 13.0, 1.2], "quadrant": 3},
                {"name": "clasp_back", "from": [6.5, 9.5, 14.8], "to": [9.5, 13.0, 15.8], "quadrant": 3},
                {"name": "handle_w", "from": [0.3, 6, 5.5], "to": [1.1, 8.5, 10.5], "quadrant": 2},
                {"name": "handle_e", "from": [14.9, 6, 5.5], "to": [15.7, 8.5, 10.5], "quadrant": 2},
            ],
            "quads": [
                {"albedo": materials_db["limestone"]["albedo"], "S": materials_db["limestone"]["S"], "F0": 10, "relief": "stone"},
                {"albedo": materials_db["oak"]["albedo"], "S": materials_db["oak"]["S"], "F0": 10, "relief": "planks", "wave_bands": 7.0},
                {"albedo": materials_db["wrought_iron"]["albedo"], "S": materials_db["wrought_iron"]["S"], "F0": 230, "relief": "panel_rivets"},
                {"albedo": materials_db["bronze"]["albedo"], "S": materials_db["bronze"]["S"], "F0": 255, "relief": "chiseled_glyph"},
            ],
        },
        {
            "modid": "tctechnology",
            "name": "comm_station",
            "material": "aluminum",
            "iris_category": "metal",
            "emissive": True,
            "description": "E9 fallback global communication station with aluminum enclosure, operator console, and dish antenna",
            "elements": [
                {"name": "base_pad", "from": [1, 0, 1], "to": [15, 1.5, 15], "quadrant": 1},
                {"name": "cabinet_main", "from": [2, 1.5, 3], "to": [14, 10, 14], "quadrant": 0},
                {"name": "console_desk", "from": [2.5, 6.5, 1], "to": [13.5, 9.5, 4], "quadrant": 1},
                {"name": "status_screen", "from": [4, 7.6, 1.2], "to": [10, 9.8, 2.8], "quadrant": 3, "light_emission": 7},
                {"name": "keypad_unit", "from": [10.5, 7.6, 1.2], "to": [12.8, 9.2, 2.8], "quadrant": 2},
                {"name": "vent_louver", "from": [3, 3, 13.8], "to": [13, 8, 14.4], "quadrant": 1},
                {"name": "antenna_mast", "from": [7, 10, 8], "to": [9, 14.5, 10], "quadrant": 1},
                {"name": "dish_hub", "from": [6, 12.5, 6.5], "to": [10, 14.5, 9.5], "quadrant": 2},
                {"name": "dish_wing_w", "from": [3, 12.0, 5.5], "to": [6.2, 15.5, 7.2], "quadrant": 0, "rotation": {"origin": [6, 13.5, 6.5], "axis": "y", "angle": 22.5}},
                {"name": "dish_wing_e", "from": [9.8, 12.0, 5.5], "to": [13, 15.5, 7.2], "quadrant": 0, "rotation": {"origin": [10, 13.5, 6.5], "axis": "y", "angle": -22.5}},
                {"name": "dish_top", "from": [5.5, 14.5, 5.5], "to": [10.5, 16.0, 7.2], "quadrant": 0},
                {"name": "feed_horn", "from": [7.5, 13.2, 3.2], "to": [8.5, 14.2, 6.5], "quadrant": 2},
            ],
            "quads": [
                {"albedo": materials_db["aluminum"]["albedo"], "S": materials_db["aluminum"]["S"], "F0": 232, "relief": "panel_rivets", "bump_strength": 0.16, "color_spread": 0.06},
                {"albedo": materials_db["steel"]["albedo"], "S": materials_db["steel"]["S"], "F0": 230, "relief": "panel_rivets", "bump_strength": 0.20, "color_spread": 0.07},
                {"albedo": materials_db["bakelite"]["albedo"], "S": materials_db["bakelite"]["S"], "F0": 10, "relief": "stone", "bump_strength": 0.12},
                {"albedo": [56, 148, 118], "S": 210, "F0": 12, "emissive_a": 140, "relief": "chiseled_glyph", "bump_strength": 0.08, "color_spread": 0.12},
            ],
        },
    ]

    # Build & bake all 7 3D blocks
    for bdef in block_defs:
        modid = bdef["modid"]
        bname = bdef["name"]
        res_root = mod_resource_dir(repo_root, modid)
        blend_file = blend_root / "blocks" / f"{modid}_{bname}.blend"
        art_dir = repo_root / "art-src" / "blocks" / modid / bname

        model_info = build_blender_3d_cuboid_scene_and_mc_json(
            modid,
            bname,
            bdef["elements"],
            blend_file,
            res_root,
        )
        bake_block_pbr_maps(f"{modid}_{bname}", bdef["quads"], args.block_res, art_dir)

        meta = {
            "schema_version": 1,
            "kind": "block",
            "id": f"{modid}:{bname}",
            "material": bdef["material"],
            "variants": 1,
            "emissive": bdef["emissive"],
            "iris_category": bdef["iris_category"],
            "master_resolution": args.block_res,
            "element_count": model_info["element_count"],
            "source_blend": str(blend_file.relative_to(repo_root)),
            "description": bdef["description"],
        }
        (art_dir / "meta.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Built & baked block {modid}:{bname} ({model_info['element_count']} elements, {args.block_res}x)")

    # Build & bake 2 items (tcivilization:placeholder & tcivilization:legacy_item)
    item_defs = [
        {
            "modid": "tcivilization",
            "name": "placeholder",
            "kind": "stele_tablet",
            "material": "slate",
            "rgb_main": materials_db["slate"]["albedo"],
            "rgb_accent": materials_db["granite"]["albedo"],
            "S": materials_db["slate"]["S"],
            "description": "RM-P8 chiseled stone tablet icon for placeholder items",
        },
        {
            "modid": "tcivilization",
            "name": "legacy_item",
            "kind": "legacy_seal",
            "material": "terracotta",
            "rgb_main": materials_db["terracotta"]["albedo"],
            "rgb_accent": [176, 152, 108],
            "S": materials_db["terracotta"]["S"],
            "description": "Retired content seal fragment bound with cordage (tcivilization:legacy_item)",
        },
    ]
    for idef in item_defs:
        modid = idef["modid"]
        iname = idef["name"]
        res_root = mod_resource_dir(repo_root, modid)
        blend_file = blend_root / "items" / f"{modid}_{iname}.blend"
        art_dir = repo_root / "art-src" / "items" / modid / iname

        bake_item_pbr_master(
            modid,
            iname,
            idef["kind"],
            idef["rgb_main"],
            idef["rgb_accent"],
            idef["S"],
            args.item_res,
            art_dir,
            blend_file,
            res_root,
        )
        meta = {
            "schema_version": 1,
            "kind": "item",
            "id": f"{modid}:{iname}",
            "material": idef["material"],
            "variants": 1,
            "emissive": False,
            "master_resolution": args.item_res,
            "source_blend": str(blend_file.relative_to(repo_root)),
            "description": idef["description"],
        }
        (art_dir / "meta.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Built & baked item {modid}:{iname} ({args.item_res}x)")

    # Build & bake UI nine-slice skins (tcivilization:skin/neutral & tcatlas:skin/stone)
    skin_defs = [
        {
            "modid": "tcivilization",
            "skin": "neutral",
            "style": "neutral",
            "palette": {
                "background": "#E6E6E3",
                "panel": "#D4D4D0",
                "text": "#1F1F1F",
                "text_muted": "#555555",
                "accent": "#3B5B7A",
                "positive": "#2E6B30",
                "warning": "#8A5A00",
                "critical": "#A12622",
                "icon": "#2A2A2A",
            },
        },
        {
            "modid": "tcatlas",
            "skin": "stone",
            "style": "stone",
            "palette": {
                "background": "#E6D8BC",
                "panel": "#DFCFAF",
                "text": "#1E1914",
                "text_muted": "#594A38",
                "accent": "#7A4B24",
                "positive": "#2B682C",
                "warning": "#855300",
                "critical": "#962420",
                "icon": "#2C2218",
            },
        },
    ]
    for sdef in skin_defs:
        modid = sdef["modid"]
        sname = sdef["skin"]
        art_dir = repo_root / "art-src" / "skins" / modid / sname
        blend_file = blend_root / "skins" / f"{modid}_{sname}.blend"
        bake_ui_skin_masters(modid, sname, sdef["style"], args.skin_res, art_dir, blend_file)
        meta = {
            "schema_version": 1,
            "kind": "skin",
            "id": f"{modid}:skin/{sname}",
            "nine_slice": {"left": 6, "right": 6, "top": 6, "bottom": 6},
            "logical_texture_size": [64, 64],
            "master_resolution": args.skin_res,
            "widgets": ["frame", "button", "tab", "scrollbar", "toast"],
            "palette": sdef["palette"],
            "source_blend": str(blend_file.relative_to(repo_root)),
        }
        (art_dir / "meta.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Built & baked UI skin {modid}:skin/{sname} ({args.skin_res}x)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
