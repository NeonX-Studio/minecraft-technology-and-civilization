#!/usr/bin/env python3
r"""Headless Blender script: build tcivilization:person LOD0/LOD1/LOD2 rigs & meshes,
bake entity textures via Cycles, save .blend/.glb sources, and export MC Java geometry.

Invoke:
  & 'F:\Programs\Blender\blender.exe' --background --factory-startup --python tools\art-export\blender\build_person_lods.py -- --repo-root I:\Projects\SuperMC
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
from mathutils import Vector

PX_TO_M = 1.0 / 16.0  # 16 MC model units = 1 meter


def _parse_args() -> argparse.Namespace:
    argv = sys.argv
    if "--" in argv:
        argv = argv[argv.index("--") + 1 :]
    else:
        argv = []
    parser = argparse.ArgumentParser(description="Build tcivilization:person LOD0/1/2 in Blender.")
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--bake-scale", type=int, default=4, help="Master pixel multiplier per MC texel (default 4 -> 512px for 128 UV)")
    return parser.parse_args(argv)


def reset_scene() -> bpy.types.Scene:
    bpy.ops.wm.read_factory_settings(use_empty=True)
    addon_utils.enable("cycles")
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 16
    scene.cycles.use_denoising = False
    scene.unit_settings.system = "METRIC"
    scene.unit_settings.scale_length = 1.0
    return scene


def mc_pivot_to_blender(mc_offset: tuple[float, float, float], parent_bl: Vector | None = None) -> Vector:
    """Convert Minecraft PartPose offset (Y+ down, feet at Y=24) to Blender world meters (Z+ up, feet at Z=0)."""
    mx, my, mz = mc_offset
    if parent_bl is None:
        return Vector((mx * PX_TO_M, mz * PX_TO_M, (24.0 - my) * PX_TO_M))
    return parent_bl + Vector((mx * PX_TO_M, mz * PX_TO_M, -my * PX_TO_M))


def add_mc_box_to_bmesh(
    bm: bmesh.types.BMesh,
    uv_layer: Any,
    pivot_bl: Vector,
    origin_mc: tuple[float, float, float],
    size_mc: tuple[float, float, float],
    uv_origin: tuple[int, int],
    tex_size: tuple[int, int],
    deformation: float = 0.0,
) -> None:
    """Construct a cuboid in Blender matching Minecraft CubeDefinition geometry and exact box UV layout."""
    ox, oy, oz = origin_mc
    w, h, d = size_mc
    g = deformation

    # MC X: right(-) to left(+), Y: up(-) to down(+), Z: front(-) to back(+)
    # Blender X = MC X, Blender Y = MC Z, Blender Z = -MC Y (relative to pivot)
    x0 = pivot_bl.x + (ox - g) * PX_TO_M
    x1 = pivot_bl.x + (ox + w + g) * PX_TO_M
    y0 = pivot_bl.y + (oz - g) * PX_TO_M
    y1 = pivot_bl.y + (oz + d + g) * PX_TO_M
    z1 = pivot_bl.z - (oy - g) * PX_TO_M       # top in Blender
    z0 = pivot_bl.z - (oy + h + g) * PX_TO_M   # bottom in Blender

    v000 = bm.verts.new((x0, y0, z0))
    v100 = bm.verts.new((x1, y0, z0))
    v110 = bm.verts.new((x1, y1, z0))
    v010 = bm.verts.new((x0, y1, z0))
    v001 = bm.verts.new((x0, y0, z1))
    v101 = bm.verts.new((x1, y0, z1))
    v111 = bm.verts.new((x1, y1, z1))
    v011 = bm.verts.new((x0, y1, z1))

    u0, v0 = uv_origin
    tw, th = float(tex_size[0]), float(tex_size[1])
    wi, hi, di = int(round(w)), int(round(h)), int(round(d))

    def uv_pt(px_u: float, px_v: float) -> tuple[float, float]:
        # OpenGL/Blender UV V=1 is top of image, MC UV v=0 is top of image
        return (px_u / tw, 1.0 - (px_v / th))

    # Outward-winding quads (CCW from outside) + exact MC CubeDefinition UV rectangles:
    # Front (-Y in Blender, North in MC): [u+d, v+d] -> [u+d+w, v+d+h]
    f_north = bm.faces.new((v100, v000, v001, v101))
    # Back (+Y in Blender, South in MC): [u+2d+w, v+d] -> [u+2d+2w, v+d+h]
    f_south = bm.faces.new((v010, v110, v111, v011))
    # Right (-X in Blender, West in MC): [u, v+d] -> [u+d, v+d+h]
    f_west = bm.faces.new((v000, v010, v011, v001))
    # Left (+X in Blender, East in MC): [u+d+w, v+d] -> [u+2d+w, v+d+h]
    f_east = bm.faces.new((v110, v100, v101, v111))
    # Top (+Z in Blender, Up in MC): [u+d, v] -> [u+d+w, v+d]
    f_up = bm.faces.new((v001, v011, v111, v101))
    # Bottom (-Z in Blender, Down in MC): [u+d+w, v] -> [u+2d+w, v+d]
    f_down = bm.faces.new((v010, v000, v100, v110))

    face_uvs = [
        (f_north, (u0 + di, v0 + di, u0 + di + wi, v0 + di + hi)),
        (f_south, (u0 + 2 * di + wi, v0 + di, u0 + 2 * di + 2 * wi, v0 + di + hi)),
        (f_west, (u0, v0 + di, u0 + di, v0 + di + hi)),
        (f_east, (u0 + di + wi, v0 + di, u0 + 2 * di + wi, v0 + di + hi)),
        (f_up, (u0 + di, v0, u0 + di + wi, v0 + di)),
        (f_down, (u0 + di + wi, v0, u0 + di + 2 * wi, v0 + di)),
    ]
    for face, (ua, va, ub, vb) in face_uvs:
        coords = [uv_pt(ua, vb), uv_pt(ub, vb), uv_pt(ub, va), uv_pt(ua, va)]
        for loop, uv in zip(face.loops, coords):
            loop[uv_layer].uv = uv


def create_person_shader(mat_name: str, style: str) -> bpy.types.Material:
    """Create a procedural Cycles material for baking person skin, hair, or Stone-Age garments."""
    mat = bpy.data.materials.new(name=mat_name)
    nt = mat.node_tree
    nodes = nt.nodes
    links = nt.links
    nodes.clear()

    out = nodes.new("ShaderNodeOutputMaterial")
    out.location = (600, 0)
    bsdf = nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.location = (300, 0)
    links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])

    tex_coord = nodes.new("ShaderNodeTexCoord")
    tex_coord.location = (-600, 0)
    noise = nodes.new("ShaderNodeTexNoise")
    noise.location = (-350, 80)
    links.new(tex_coord.outputs["Object"], noise.inputs["Vector"])

    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.location = (-100, 80)
    links.new(noise.outputs["Fac"], ramp.inputs["Fac"])
    links.new(ramp.outputs["Color"], bsdf.inputs["Base Color"])

    if style == "skin":
        noise.inputs["Scale"].default_value = 28.0
        noise.inputs["Detail"].default_value = 5.0
        ramp.color_ramp.elements[0].position = 0.25
        ramp.color_ramp.elements[0].color = (0.42, 0.27, 0.19, 1.0)
        ramp.color_ramp.elements[1].position = 0.80
        ramp.color_ramp.elements[1].color = (0.56, 0.38, 0.27, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.68
    elif style == "face":
        noise.inputs["Scale"].default_value = 40.0
        ramp.color_ramp.elements[0].color = (0.10, 0.07, 0.06, 1.0)
        ramp.color_ramp.elements[1].color = (0.20, 0.14, 0.11, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.45
    elif style == "hair":
        noise.inputs["Scale"].default_value = 55.0
        noise.inputs["Detail"].default_value = 8.0
        noise.inputs["Roughness"].default_value = 0.7
        ramp.color_ramp.elements[0].position = 0.2
        ramp.color_ramp.elements[0].color = (0.05, 0.035, 0.025, 1.0)
        ramp.color_ramp.elements[1].position = 0.85
        ramp.color_ramp.elements[1].color = (0.13, 0.09, 0.06, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.58
    elif style == "hide_top":
        noise.inputs["Scale"].default_value = 22.0
        noise.inputs["Detail"].default_value = 7.0
        ramp.color_ramp.elements[0].position = 0.2
        ramp.color_ramp.elements[0].color = (0.24, 0.15, 0.09, 1.0)
        ramp.color_ramp.elements[1].position = 0.82
        ramp.color_ramp.elements[1].color = (0.40, 0.27, 0.16, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.82
    elif style == "hemp_bottom":
        noise.inputs["Scale"].default_value = 64.0
        noise.inputs["Detail"].default_value = 9.0
        ramp.color_ramp.elements[0].position = 0.25
        ramp.color_ramp.elements[0].color = (0.31, 0.26, 0.18, 1.0)
        ramp.color_ramp.elements[1].position = 0.80
        ramp.color_ramp.elements[1].color = (0.46, 0.39, 0.28, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.88
    else:  # socket / bone / wood accent
        noise.inputs["Scale"].default_value = 30.0
        ramp.color_ramp.elements[0].color = (0.52, 0.46, 0.36, 1.0)
        ramp.color_ramp.elements[1].color = (0.68, 0.61, 0.49, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.62

    return mat


def bake_objects_diffuse_atlas(
    objects: list[bpy.types.Object],
    img_name: str,
    width: int,
    height: int,
    out_png: Path,
) -> np.ndarray:
    """Bake Cycles DIFFUSE color + subtle AO for a set of UV-mapped objects onto a single RGBA image."""
    bake_objs = [o for o in objects if len(o.data.polygons) > 0]
    if not bake_objs:
        arr = np.zeros((height, width, 4), dtype=np.uint8)
        return arr

    img_col = bpy.data.images.new(f"{img_name}_col", width=width, height=height, alpha=True)
    img_col.colorspace_settings.name = "sRGB"
    img_col.generated_color = (0.0, 0.0, 0.0, 0.0)

    img_ao = bpy.data.images.new(f"{img_name}_ao", width=width, height=height, alpha=True)
    img_ao.colorspace_settings.name = "Non-Color"

    def attach_target(target_img: bpy.types.Image) -> list[tuple[bpy.types.NodeTree, bpy.types.Node]]:
        created = []
        seen_mats = set()
        for obj in bake_objs:
            for slot in obj.material_slots:
                mat = slot.material
                if mat is None or mat.name in seen_mats:
                    continue
                seen_mats.add(mat.name)
                nt = mat.node_tree
                tex_node = nt.nodes.new("ShaderNodeTexImage")
                tex_node.image = target_img
                for n in nt.nodes:
                    n.select = False
                tex_node.select = True
                nt.nodes.active = tex_node
                created.append((nt, tex_node))
        return created

    bpy.ops.object.select_all(action="DESELECT")
    for obj in bake_objs:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = bake_objs[0]

    scene = bpy.context.scene
    scene.render.bake.margin = 2
    scene.render.bake.use_clear = True

    # 1. Bake pure diffuse color (albedo without scene lighting)
    nodes_col = attach_target(img_col)
    scene.render.bake.use_pass_direct = False
    scene.render.bake.use_pass_indirect = False
    scene.render.bake.use_pass_color = True
    bpy.ops.object.bake(type="DIFFUSE")
    for nt, n in nodes_col:
        nt.nodes.remove(n)

    # 2. Bake gentle ambient occlusion for entity readability
    nodes_ao = attach_target(img_ao)
    bpy.ops.object.bake(type="AO")
    for nt, n in nodes_ao:
        nt.nodes.remove(n)

    col_px = np.array(img_col.pixels[:], dtype=np.float32).reshape((height, width, 4))
    ao_px = np.array(img_ao.pixels[:], dtype=np.float32).reshape((height, width, 4))

    # Blend subtle AO (0.82..1.0) into entity texture RGB while keeping crisp alpha
    ao_factor = np.clip(0.82 + 0.18 * ao_px[:, :, 0:1], 0.82, 1.0)
    rgb = np.clip(col_px[:, :, 0:3] * ao_factor, 0.0, 1.0)
    alpha = np.where(col_px[:, :, 3:4] > 0.02, 1.0, 0.0)
    rgba = np.concatenate([rgb, alpha], axis=2)

    # Save via Blender image API (Note: Blender image rows are bottom-to-top in .pixels)
    out_img = bpy.data.images.new(f"{img_name}_final", width=width, height=height, alpha=True)
    out_img.colorspace_settings.name = "sRGB"
    out_img.pixels[:] = rgba.ravel().tolist()
    out_png.parent.mkdir(parents=True, exist_ok=True)
    out_img.filepath_raw = str(out_png)
    out_img.file_format = "PNG"
    out_img.save()

    # Return top-to-bottom uint8 array for optional downsampling/compositing
    top_down = np.flipud((rgba * 255.0 + 0.5).astype(np.uint8))
    bpy.data.images.remove(img_col)
    bpy.data.images.remove(img_ao)
    bpy.data.images.remove(out_img)
    return top_down


def save_numpy_png_via_blender(arr_top_down: np.ndarray, out_png: Path) -> None:
    h, w, _ = arr_top_down.shape
    bottom_up = np.flipud(arr_top_down).astype(np.float32) / 255.0
    img = bpy.data.images.new("tmp_save", width=w, height=h, alpha=True)
    img.colorspace_settings.name = "sRGB"
    img.pixels[:] = bottom_up.ravel().tolist()
    out_png.parent.mkdir(parents=True, exist_ok=True)
    img.filepath_raw = str(out_png)
    img.file_format = "PNG"
    img.save()
    bpy.data.images.remove(img)


def downsample_rgba_nearest_or_box(arr: np.ndarray, target_w: int, target_h: int) -> np.ndarray:
    h, w, c = arr.shape
    if (w, h) == (target_w, target_h):
        return arr.copy()
    sy = h // target_h
    sx = w // target_w
    reshaped = arr.reshape(target_h, sy, target_w, sx, c).astype(np.float32)
    alpha = reshaped[:, :, :, :, 3:4] / 255.0
    weight_sum = np.sum(alpha, axis=(1, 3))
    premult = np.sum(reshaped[:, :, :, :, :3] * alpha, axis=(1, 3))
    rgb = np.divide(premult, np.maximum(weight_sum, 1e-5))
    a_out = np.where(weight_sum > (0.25 * sx * sy), 255.0, 0.0)
    out = np.concatenate([np.clip(np.round(rgb), 0, 255), a_out], axis=2).astype(np.uint8)
    return out


def build_scene_from_lod_spec(lod_key: str, lod_spec: dict[str, Any], materials: dict[str, bpy.types.Material]) -> tuple[bpy.types.Object | None, dict[str, list[bpy.types.Object]]]:
    tex_size = tuple(lod_spec["texture_size"])
    use_armature = bool(lod_spec.get("use_armature", True))

    arm_obj: bpy.types.Object | None = None
    if use_armature:
        arm_data = bpy.data.armatures.new(f"PersonArmature_{lod_key.upper()}")
        arm_obj = bpy.data.objects.new(f"PersonRig_{lod_key.upper()}", arm_data)
        bpy.context.collection.objects.link(arm_obj)
        bpy.context.view_layer.objects.active = arm_obj
        bpy.ops.object.mode_set(mode="EDIT")

        def add_bones(nodes: list[dict[str, Any]], parent_bone: Any, parent_piv: Vector | None) -> None:
            for node in nodes:
                piv = mc_pivot_to_blender(tuple(node.get("pose", {}).get("offset", [0, 0, 0])), parent_piv)
                eb = arm_data.edit_bones.new(node["name"])
                eb.head = piv
                tail_vec = Vector(node.get("bone_tail_delta", [0.0, 0.0, 0.15]))
                if tail_vec.length < 0.02:
                    tail_vec = Vector((0.0, 0.05, 0.0))
                eb.tail = piv + tail_vec
                if parent_bone is not None:
                    eb.parent = parent_bone
                add_bones(node.get("children", []), eb, piv)

        add_bones(lod_spec["parts"], None, None)
        bpy.ops.object.mode_set(mode="OBJECT")

    layer_groups: dict[str, list[bpy.types.Object]] = {}

    def create_meshes(nodes: list[dict[str, Any]], parent_piv: Vector | None) -> None:
        for node in nodes:
            piv = mc_pivot_to_blender(tuple(node.get("pose", {}).get("offset", [0, 0, 0])), parent_piv)
            boxes = node.get("boxes", [])
            if not boxes:
                create_meshes(node.get("children", []), piv)
                continue
            mesh_data = bpy.data.meshes.new(f"{lod_key}_{node['name']}_mesh")
            bm = bmesh.new()
            uv_layer = bm.loops.layers.uv.new("UVMap")
            for box in boxes:
                add_mc_box_to_bmesh(
                    bm,
                    uv_layer,
                    piv,
                    tuple(box["origin"]),
                    tuple(box["size"]),
                    tuple(box["uv"]),
                    tex_size,
                    float(box.get("deformation", 0.0)),
                )
            bm.to_mesh(mesh_data)
            bm.free()
            mesh_data.update()

            obj = bpy.data.objects.new(node["name"], mesh_data)
            bpy.context.collection.objects.link(obj)

            style = node.get("material_style", "skin")
            mat = materials[style]
            obj.data.materials.append(mat)

            layer_tag = node.get("texture_layer", "base")
            layer_groups.setdefault(layer_tag, []).append(obj)

            if arm_obj is not None:
                obj.parent = arm_obj
                vg = obj.vertex_groups.new(name=node["name"])
                if len(mesh_data.vertices) > 0:
                    vg.add(list(range(len(mesh_data.vertices))), 1.0, "REPLACE")
                mod = obj.modifiers.new(name="Armature", type="ARMATURE")
                mod.object = arm_obj

            create_meshes(node.get("children", []), piv)

    create_meshes(lod_spec["parts"], None)
    return arm_obj, layer_groups


def define_person_geometry_spec() -> dict[str, Any]:
    """Authoritative LOD0/1/2 specification with non-overlapping dedicated person UV layouts (not Steve 64x64)."""
    lod0 = {
        "description": "LOD0 (<= 16 blocks) full modular humanoid geometry with separate facial expression, hair, E0 hide/hemp clothing layers, headwear mount, and tool sockets.",
        "source_blend": "art-src/entities/tcivilization/person/person_lod0.blend",
        "texture_size": [128, 128],
        "use_armature": True,
        "parts": [
            {
                "name": "waist",
                "pose": {"offset": [0.0, 12.0, 0.0]},
                "bone_tail_delta": [0.0, 0.0, 0.15],
                "boxes": [],
                "material_style": "skin",
                "texture_layer": "base",
                "children": [
                    {
                        "name": "body",
                        "pose": {"offset": [0.0, -12.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, 0.35],
                        "material_style": "skin",
                        "texture_layer": "base",
                        "boxes": [
                            {"origin": [-4.0, 0.0, -2.0], "size": [8.0, 12.0, 4.0], "uv": [0, 20]}
                        ],
                        "children": [
                            {
                                "name": "outfit_top",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, 0.25],
                                "material_style": "hide_top",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-4.0, 0.0, -2.0], "size": [8.0, 12.0, 4.0], "uv": [64, 0], "deformation": 0.25},
                                    {"origin": [-4.5, 9.5, -2.5], "size": [9.0, 2.0, 5.0], "uv": [64, 18]}
                                ]
                            }
                        ]
                    },
                    {
                        "name": "head",
                        "pose": {"offset": [0.0, -12.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, 0.45],
                        "material_style": "skin",
                        "texture_layer": "base",
                        "boxes": [
                            {"origin": [-4.0, -8.0, -4.0], "size": [8.0, 8.0, 8.0], "uv": [0, 0]}
                        ],
                        "children": [
                            {
                                "name": "face_expression",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, -0.1, 0.0],
                                "material_style": "face",
                                "texture_layer": "base",
                                "boxes": [
                                    {"origin": [-3.0, -5.0, -4.15], "size": [2.0, 1.0, 1.0], "uv": [36, 0]},
                                    {"origin": [1.0, -5.0, -4.15], "size": [2.0, 1.0, 1.0], "uv": [44, 0]},
                                    {"origin": [-1.5, -2.2, -4.1], "size": [3.0, 1.0, 1.0], "uv": [36, 4]}
                                ]
                            },
                            {
                                "name": "hair_layer",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, 0.25],
                                "material_style": "hair",
                                "texture_layer": "hair",
                                "boxes": [
                                    {"origin": [-4.0, -8.0, -4.0], "size": [8.0, 8.0, 8.0], "uv": [0, 64], "deformation": 0.35},
                                    {"origin": [-1.5, -2.0, 3.5], "size": [3.0, 5.0, 2.0], "uv": [36, 64]}
                                ]
                            },
                            {
                                "name": "headwear_mount",
                                "pose": {"offset": [0.0, -8.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, 0.15],
                                "material_style": "socket",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-4.5, -1.0, -4.5], "size": [9.0, 2.0, 9.0], "uv": [64, 96]}
                                ]
                            }
                        ]
                    },
                    {
                        "name": "right_arm",
                        "pose": {"offset": [-5.0, -10.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.45],
                        "material_style": "skin",
                        "texture_layer": "base",
                        "boxes": [
                            {"origin": [-3.0, -2.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [28, 20]}
                        ],
                        "children": [
                            {
                                "name": "outfit_right_sleeve",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, -0.2],
                                "material_style": "hide_top",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-3.0, -2.0, -2.0], "size": [4.0, 7.0, 4.0], "uv": [96, 0], "deformation": 0.22}
                                ]
                            },
                            {
                                "name": "right_hand_tool_mount",
                                "pose": {"offset": [-1.0, 9.0, 0.0]},
                                "bone_tail_delta": [0.0, -0.15, 0.0],
                                "material_style": "socket",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-1.0, -1.0, -1.0], "size": [2.0, 2.0, 2.0], "uv": [104, 96]}
                                ]
                            }
                        ]
                    },
                    {
                        "name": "left_arm",
                        "pose": {"offset": [5.0, -10.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.45],
                        "material_style": "skin",
                        "texture_layer": "base",
                        "boxes": [
                            {"origin": [-1.0, -2.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [46, 20]}
                        ],
                        "children": [
                            {
                                "name": "outfit_left_sleeve",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, -0.2],
                                "material_style": "hide_top",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-1.0, -2.0, -2.0], "size": [4.0, 7.0, 4.0], "uv": [96, 14], "deformation": 0.22}
                                ]
                            },
                            {
                                "name": "left_hand_tool_mount",
                                "pose": {"offset": [1.0, 9.0, 0.0]},
                                "bone_tail_delta": [0.0, -0.15, 0.0],
                                "material_style": "socket",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-1.0, -1.0, -1.0], "size": [2.0, 2.0, 2.0], "uv": [114, 96]}
                                ]
                            }
                        ]
                    },
                    {
                        "name": "right_leg",
                        "pose": {"offset": [-1.9, 0.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.55],
                        "material_style": "skin",
                        "texture_layer": "base",
                        "boxes": [
                            {"origin": [-2.0, 0.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [0, 40]}
                        ],
                        "children": [
                            {
                                "name": "outfit_bottom_right",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, -0.3],
                                "material_style": "hemp_bottom",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-2.0, 0.0, -2.0], "size": [4.0, 9.0, 4.0], "uv": [64, 32], "deformation": 0.2}
                                ]
                            }
                        ]
                    },
                    {
                        "name": "left_leg",
                        "pose": {"offset": [1.9, 0.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.55],
                        "material_style": "skin",
                        "texture_layer": "base",
                        "boxes": [
                            {"origin": [-2.0, 0.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [18, 40]}
                        ],
                        "children": [
                            {
                                "name": "outfit_bottom_left",
                                "pose": {"offset": [0.0, 0.0, 0.0]},
                                "bone_tail_delta": [0.0, 0.0, -0.3],
                                "material_style": "hemp_bottom",
                                "texture_layer": "outfit",
                                "boxes": [
                                    {"origin": [-2.0, 0.0, -2.0], "size": [4.0, 9.0, 4.0], "uv": [84, 32], "deformation": 0.2}
                                ]
                            }
                        ]
                    }
                ]
            }
        ]
    }

    lod1 = {
        "description": "LOD1 (16-48 blocks) simplified humanoid rig with clothing layers merged onto torso and limbs, no separate facial expression mesh.",
        "source_blend": "art-src/entities/tcivilization/person/person_lod1.blend",
        "texture_size": [64, 64],
        "use_armature": True,
        "parts": [
            {
                "name": "body",
                "pose": {"offset": [0.0, 0.0, 0.0]},
                "bone_tail_delta": [0.0, 0.0, -0.5],
                "material_style": "hide_top",
                "texture_layer": "merged",
                "boxes": [
                    {"origin": [-4.0, 0.0, -2.0], "size": [8.0, 12.0, 4.0], "uv": [0, 18]}
                ],
                "children": [
                    {
                        "name": "head",
                        "pose": {"offset": [0.0, 0.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, 0.45],
                        "material_style": "skin",
                        "texture_layer": "merged",
                        "boxes": [
                            {"origin": [-4.0, -8.0, -4.0], "size": [8.0, 8.0, 8.0], "uv": [0, 0]}
                        ]
                    },
                    {
                        "name": "right_arm",
                        "pose": {"offset": [-5.0, 2.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.45],
                        "material_style": "hide_top",
                        "texture_layer": "merged",
                        "boxes": [
                            {"origin": [-3.0, -2.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [26, 18]}
                        ]
                    },
                    {
                        "name": "left_arm",
                        "pose": {"offset": [5.0, 2.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.45],
                        "material_style": "hide_top",
                        "texture_layer": "merged",
                        "boxes": [
                            {"origin": [-1.0, -2.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [44, 18]}
                        ]
                    },
                    {
                        "name": "right_leg",
                        "pose": {"offset": [-1.9, 12.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.55],
                        "material_style": "hemp_bottom",
                        "texture_layer": "merged",
                        "boxes": [
                            {"origin": [-2.0, 0.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [0, 36]}
                        ]
                    },
                    {
                        "name": "left_leg",
                        "pose": {"offset": [1.9, 12.0, 0.0]},
                        "bone_tail_delta": [0.0, 0.0, -0.55],
                        "material_style": "hemp_bottom",
                        "texture_layer": "merged",
                        "boxes": [
                            {"origin": [-2.0, 0.0, -2.0], "size": [4.0, 12.0, 4.0], "uv": [18, 36]}
                        ]
                    }
                ]
            }
        ]
    }

    lod2 = {
        "description": "LOD2 (48-128 blocks) single-mesh low-poly static pose proxy with baked base colors (no skeletal animation).",
        "source_blend": "art-src/entities/tcivilization/person/person_lod2.blend",
        "texture_size": [64, 32],
        "use_armature": False,
        "parts": [
            {
                "name": "proxy",
                "pose": {"offset": [0.0, 0.0, 0.0]},
                "material_style": "hide_top",
                "texture_layer": "proxy",
                "boxes": [
                    {"origin": [-3.5, -8.0, -3.5], "size": [7.0, 8.0, 7.0], "uv": [0, 0]},
                    {"origin": [-5.5, 0.0, -2.0], "size": [11.0, 12.0, 4.0], "uv": [30, 0]},
                    {"origin": [-3.5, 12.0, -2.0], "size": [7.0, 12.0, 4.0], "uv": [0, 16]}
                ]
            }
        ]
    }

    return {
        "mod_id": "tcivilization",
        "entity_id": "person",
        "spec_ref": "docs/design/32-art-audio.md#4.2-4.3",
        "textures": {
            "lod0_base": "textures/entity/person/person_lod0_base.png",
            "lod0_hair_e0": "textures/entity/person/person_lod0_hair_e0.png",
            "lod0_outfit_e0_hide": "textures/entity/person/person_lod0_outfit_e0_hide.png",
            "lod0_composite_e0": "textures/entity/person/person_lod0_composite_e0.png",
            "lod1_e0": "textures/entity/person/person_lod1_e0.png",
            "lod2_e0": "textures/entity/person/person_lod2_e0.png"
        },
        "lods": {
            "lod0": lod0,
            "lod1": lod1,
            "lod2": lod2
        }
    }


def export_glb(filepath: Path) -> None:
    filepath.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(filepath),
        export_format="GLB",
        use_selection=False,
        export_apply=False,
        export_yup=True,
    )


def main() -> int:
    args = _parse_args()
    repo_root = args.repo_root.resolve()
    person_src_dir = repo_root / "art-src" / "entities" / "tcivilization" / "person"
    person_tex_master_dir = person_src_dir / "textures"
    mod_tex_dir = repo_root / "tcivilization-core" / "src" / "main" / "resources" / "assets" / "tcivilization" / "textures" / "entity" / "person"
    rp128_tex_dir = repo_root / "resourcepacks" / "tc-pbr-128x" / "assets" / "tcivilization" / "textures" / "entity" / "person"
    java_out_dir = repo_root / "tcivilization-core" / "src" / "main" / "java" / "com" / "nightcity" / "tcivilization" / "client" / "model"

    spec = define_person_geometry_spec()
    manifest_stats: dict[str, Any] = {"lods": {}}

    # ---- LOD0 ----
    reset_scene()
    mats0 = {
        k: create_person_shader(f"lod0_{k}", k)
        for k in ("skin", "face", "hair", "hide_top", "hemp_bottom", "socket")
    }
    arm0, groups0 = build_scene_from_lod_spec("lod0", spec["lods"]["lod0"], mats0)
    scale = args.bake_scale

    arr_base = bake_objects_diffuse_atlas(
        groups0.get("base", []), "lod0_base", 128 * scale, 128 * scale, person_tex_master_dir / "person_lod0_base.png"
    )
    arr_hair = bake_objects_diffuse_atlas(
        groups0.get("hair", []), "lod0_hair", 128 * scale, 128 * scale, person_tex_master_dir / "person_lod0_hair_e0.png"
    )
    arr_outfit = bake_objects_diffuse_atlas(
        groups0.get("outfit", []), "lod0_outfit", 128 * scale, 128 * scale, person_tex_master_dir / "person_lod0_outfit_e0_hide.png"
    )

    # Composite E0 atlas (non-overlapping UV islands)
    arr_comp = np.maximum(np.maximum(arr_base, arr_hair), arr_outfit)
    save_numpy_png_via_blender(arr_comp, person_tex_master_dir / "person_lod0_composite_e0.png")

    # Export 64x baseline (128x128 entity sheet) and 128x hires (256x256 entity sheet)
    for fname, master_arr in [
        ("person_lod0_base.png", arr_base),
        ("person_lod0_hair_e0.png", arr_hair),
        ("person_lod0_outfit_e0_hide.png", arr_outfit),
        ("person_lod0_composite_e0.png", arr_comp),
    ]:
        save_numpy_png_via_blender(downsample_rgba_nearest_or_box(master_arr, 128, 128), mod_tex_dir / fname)
        save_numpy_png_via_blender(downsample_rgba_nearest_or_box(master_arr, 256, 256), rp128_tex_dir / fname)

    blend0 = person_src_dir / "person_lod0.blend"
    glb0 = person_src_dir / "person_lod0.glb"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend0))
    export_glb(glb0)
    manifest_stats["lods"]["lod0"] = {
        "bones": len(arm0.data.bones) if arm0 else 0,
        "mesh_objects": sum(len(v) for v in groups0.values()),
        "polygons": sum(len(o.data.polygons) for v in groups0.values() for o in v),
        "blend": str(blend0.relative_to(repo_root)),
        "glb": str(glb0.relative_to(repo_root)),
    }

    # ---- LOD1 ----
    reset_scene()
    mats1 = {
        k: create_person_shader(f"lod1_{k}", k)
        for k in ("skin", "hide_top", "hemp_bottom")
    }
    arm1, groups1 = build_scene_from_lod_spec("lod1", spec["lods"]["lod1"], mats1)
    arr_lod1 = bake_objects_diffuse_atlas(
        groups1.get("merged", []), "lod1_merged", 64 * scale, 64 * scale, person_tex_master_dir / "person_lod1_e0.png"
    )
    save_numpy_png_via_blender(downsample_rgba_nearest_or_box(arr_lod1, 64, 64), mod_tex_dir / "person_lod1_e0.png")
    save_numpy_png_via_blender(downsample_rgba_nearest_or_box(arr_lod1, 128, 128), rp128_tex_dir / "person_lod1_e0.png")

    blend1 = person_src_dir / "person_lod1.blend"
    glb1 = person_src_dir / "person_lod1.glb"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend1))
    export_glb(glb1)
    manifest_stats["lods"]["lod1"] = {
        "bones": len(arm1.data.bones) if arm1 else 0,
        "mesh_objects": sum(len(v) for v in groups1.values()),
        "polygons": sum(len(o.data.polygons) for v in groups1.values() for o in v),
        "blend": str(blend1.relative_to(repo_root)),
        "glb": str(glb1.relative_to(repo_root)),
    }

    # ---- LOD2 ----
    reset_scene()
    mats2 = {"hide_top": create_person_shader("lod2_proxy", "hide_top")}
    # Merge LOD2 into a single mesh object as required by 32 section 4.2 ('单网格低模 + 基础色')
    _, groups2 = build_scene_from_lod_spec("lod2", spec["lods"]["lod2"], mats2)
    arr_lod2 = bake_objects_diffuse_atlas(
        groups2.get("proxy", []), "lod2_proxy", 64 * scale, 32 * scale, person_tex_master_dir / "person_lod2_e0.png"
    )
    save_numpy_png_via_blender(downsample_rgba_nearest_or_box(arr_lod2, 64, 32), mod_tex_dir / "person_lod2_e0.png")
    save_numpy_png_via_blender(downsample_rgba_nearest_or_box(arr_lod2, 128, 64), rp128_tex_dir / "person_lod2_e0.png")

    blend2 = person_src_dir / "person_lod2.blend"
    glb2 = person_src_dir / "person_lod2.glb"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend2))
    export_glb(glb2)
    manifest_stats["lods"]["lod2"] = {
        "bones": 0,
        "mesh_objects": sum(len(v) for v in groups2.values()),
        "polygons": sum(len(o.data.polygons) for v in groups2.values() for o in v),
        "blend": str(blend2.relative_to(repo_root)),
        "glb": str(glb2.relative_to(repo_root)),
    }

    geom_json = person_src_dir / "person_geometry.json"
    geom_json.write_text(json.dumps(spec, indent=2, ensure_ascii=False), encoding="utf-8")
    manifest_json = person_src_dir / "manifest.json"
    manifest_json.write_text(json.dumps(manifest_stats, indent=2, ensure_ascii=False), encoding="utf-8")

    # Invoke standalone Java exporter
    sys.path.insert(0, str(repo_root / "tools" / "art-export" / "blender"))
    import export_mc_entity_java

    generated = export_mc_entity_java.generate_java_models(geom_json, java_out_dir)
    print(f"Person LOD build complete. Generated {len(generated)} Java files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
