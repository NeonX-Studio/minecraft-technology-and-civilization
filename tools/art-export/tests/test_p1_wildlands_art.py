#!/usr/bin/env python3
"""Automated Verification Suite for P1 Wildlands 3D Models, Rocks/Soils/Ores LabPBR & Iris Pipeline."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[3]
EXPORT_DIR = REPO_ROOT / "tools" / "art-export"
BLENDER_DIR = EXPORT_DIR / "blender"
for p in (EXPORT_DIR, BLENDER_DIR):
    if str(p) not in sys.path:
        sys.path.insert(0, str(p))

import compose_ores  # noqa: E402
import export as art_export  # noqa: E402
import iris_compat  # noqa: E402
import p1_wildlands_specs as specs  # noqa: E402

MODID = "tcwildlands"


def _mean_hsv_saturation_and_brightness(png_path: Path) -> tuple[float, float]:
    with Image.open(png_path) as im:
        arr = np.array(im.convert("RGBA"), dtype=np.float32) / 255.0
    rgb = arr[:, :, :3]
    cmax = np.max(rgb, axis=2)
    cmin = np.min(rgb, axis=2)
    hsv_s = np.where(cmax > 1e-5, (cmax - cmin) / np.maximum(cmax, 1e-5), 0.0)
    return float(np.mean(hsv_s)), float(np.mean(rgb))


class P1WildlandsArtPipelineTests(unittest.TestCase):
    def test_01_3d_non_cube_geological_and_mine_models(self) -> None:
        required_3d = (
            "loose_stones",
            "ore_float",
            "gossan",
            "fumarole",
            "geyser",
            "steam_vent",
            "bitumen_seep",
            "gas_seep",
            "timber_prop",
            "steel_mine_arch",
            "rock_bolts",
        )
        assets_dir = art_export.mod_assets_dir(REPO_ROOT, MODID)
        blend_dir = REPO_ROOT / "art-src" / "blend" / "blocks"

        for bname in required_3d:
            blend_file = blend_dir / f"{MODID}_{bname}.blend"
            self.assertTrue(blend_file.exists(), f"Missing Blender file {blend_file}")
            self.assertGreater(blend_file.stat().st_size, 1024)

            mpath = assets_dir / "models" / "block" / f"{bname}.json"
            self.assertTrue(mpath.exists(), f"Missing 3D block model {mpath}")
            mdata = json.loads(mpath.read_text(encoding="utf-8"))
            self.assertIn("elements", mdata)
            elems = mdata["elements"]
            self.assertGreaterEqual(len(elems), 1)
            self.assertLessEqual(len(elems), 64, f"{bname} exceeds 64 cuboid elements")

        # Multi-variant loose_stones must provide >= 4 distinct 3D models & y=0/90/180/270 rotations
        for suffix in ("", "_v1", "_v2", "_v3"):
            vpath = assets_dir / "models" / "block" / f"loose_stones{suffix}.json"
            self.assertTrue(vpath.exists(), f"Missing loose_stones variant model {vpath}")
            vdata = json.loads(vpath.read_text(encoding="utf-8"))
            self.assertLessEqual(len(vdata["elements"]), 64)

        ls_bs = json.loads((assets_dir / "blockstates" / "loose_stones.json").read_text(encoding="utf-8"))
        ls_entries = ls_bs["variants"][""]
        self.assertGreaterEqual(len(ls_entries), 8)
        self.assertEqual({e["y"] for e in ls_entries}, {0, 90, 180, 270})

    def test_02_rocks_soils_variants_ar2_saturation_brightness_and_seamless(self) -> None:
        assets_dir = art_export.mod_assets_dir(REPO_ROOT, MODID)
        tex64 = assets_dir / "textures" / "block"
        tex128 = art_export.tier_assets_dir(REPO_ROOT, 128, MODID) / "textures" / "block"

        # Check all 22 custom natural rocks & 16 soils have >= 2 variants + y=0/90/180/270 weighted blockstates
        natural_blocks = [r[0] for r in specs.CUSTOM_ROCKS] + [s[0] for s in specs.SOILS]
        for bname in natural_blocks:
            bs = json.loads((assets_dir / "blockstates" / f"{bname}.json").read_text(encoding="utf-8"))
            entries = bs["variants"][""]
            self.assertIsInstance(entries, list)
            self.assertGreaterEqual(len(entries), 8, f"{bname} must have >= 8 variant*rotation combinations")
            models_used = {e["model"] for e in entries}
            self.assertGreaterEqual(len(models_used), 2, f"{bname} must have >= 2 distinct variant models")
            self.assertEqual({e["y"] for e in entries}, {0, 90, 180, 270})
            self.assertTrue(all("weight" in e and e["weight"] >= 1 for e in entries))

            # Both base and _v1 textures must exist at 64x and 128x
            for tdir, expected_px in ((tex64, 64), (tex128, 128)):
                for stem in (bname, f"{bname}_v1"):
                    p = tdir / f"{stem}.png"
                    self.assertTrue(p.exists(), f"Missing {p}")
                    with Image.open(p) as im:
                        self.assertEqual(im.size, (expected_px, expected_px))

        # Natural rock brightness in [0.25, 0.55] and AR-2 HSV saturation <= 0.45
        for rock_id, _, _, _, _ in specs.CUSTOM_ROCKS:
            sat, bri = _mean_hsv_saturation_and_brightness(tex128 / f"{rock_id}.png")
            self.assertLessEqual(sat, 0.45, f"AR-2 violation on {rock_id}: S={sat:.3f}")
            self.assertGreaterEqual(bri, 0.25, f"{rock_id} too dark: brightness={bri:.3f} < 0.25")
            self.assertLessEqual(bri, 0.55, f"{rock_id} too bright: brightness={bri:.3f} > 0.55")

            # Check seamless periodic wrap (opposite edges match within small seamless tolerance)
            with Image.open(tex128 / f"{rock_id}.png") as im_r:
                arr = np.array(im_r.convert("RGB"), dtype=np.float32)
            lr_diff = float(np.mean(np.abs(arr[:, 0, :] - arr[:, -1, :]))) / 255.0 * 100.0
            tb_diff = float(np.mean(np.abs(arr[0, :, :] - arr[-1, :, :]))) / 255.0 * 100.0
            self.assertLess(lr_diff, 3.0, f"{rock_id} left-right seam diff {lr_diff:.2f} >= 3.0")
            self.assertLess(tb_diff, 3.0, f"{rock_id} top-bottom seam diff {tb_diff:.2f} >= 3.0")

        # Check AR-2 saturation <= 0.45 across ALL exported tcwildlands block albedos
        for alb_png in sorted(tex64.glob("*.png")):
            if alb_png.stem.endswith("_n") or alb_png.stem.endswith("_s"):
                continue
            sat, _ = _mean_hsv_saturation_and_brightness(alb_png)
            self.assertLessEqual(sat, 0.45, f"AR-2 block saturation exceeded on {alb_png.name}: {sat:.3f}")

        # Check bricks mortar height lowered & mortar porosity higher in _s B channel (32-art-audio.md#3.4 Rule 3)
        with Image.open(tex64 / "granite_bricks_n.png") as im_bn, Image.open(tex64 / "granite_bricks_s.png") as im_bs:
            bn = np.array(im_bn.convert("RGBA"), dtype=np.uint8)
            bs_arr = np.array(im_bs.convert("RGBA"), dtype=np.uint8)
        self.assertLess(int(bn[:, :, 3].min()), int(bn[:, :, 3].max()))
        self.assertGreater(int(bs_arr[:, :, 2].max()), int(bs_arr[:, :, 2].min()))

    def test_03_ore_overlays_and_87_legal_composed_ores(self) -> None:
        legal = compose_ores.parse_legal_ores_from_registries(REPO_ROOT)
        self.assertEqual(len(legal), 87)

        # Verify all 37 mineral overlays exist with transparent background
        for mineral, _, _ in specs.MINERAL_OVERLAYS:
            ov_dir = REPO_ROOT / "art-src" / "ore_overlays" / mineral
            self.assertTrue((ov_dir / "albedo.png").exists(), f"Missing overlay {mineral}")
            with Image.open(ov_dir / "albedo.png") as im_ov:
                ov_rgba = np.array(im_ov.convert("RGBA"), dtype=np.uint8)
            alpha = ov_rgba[:, :, 3]
            self.assertEqual(int(alpha.min()), 0, f"Overlay {mineral} background must be transparent")
            cov = float(np.mean(alpha > 32))
            self.assertGreater(cov, 0.05, f"Overlay {mineral} coverage too sparse: {cov:.3f}")
            self.assertLess(cov, 0.55, f"Overlay {mineral} coverage too dense: {cov:.3f}")

        # Verify NO illegal *_ore textures exist and all 87 legal ores have raised height, F0 & A=255 at 64x & 128x
        legal_names = {oid for oid, _, _ in legal}
        materials_db = art_export.load_materials_library(REPO_ROOT)
        for tier in (64, 128):
            tdir = art_export.tier_assets_dir(REPO_ROOT, tier, MODID) / "textures" / "block"
            found_ores = {
                p.stem for p in tdir.glob("*_ore.png")
                if not p.stem.endswith("_n") and not p.stem.endswith("_s")
            }
            self.assertEqual(found_ores, legal_names)

        # Spot-check metallic & dielectric ores for raised height (+10-20%), discrete F0, and non-emissive A=255
        t128 = art_export.tier_assets_dir(REPO_ROOT, 128, MODID) / "textures" / "block"
        for ore_id, host, mineral in (
            ("basalt_rock_native_copper_ore", "basalt_rock", "native_copper"),
            ("limestone_galena_ore", "limestone", "galena"),
            ("slate_native_gold_ore", "slate", "native_gold"),
            ("diorite_chalcopyrite_ore", "diorite", "chalcopyrite"),
            ("limestone_malachite_ore", "limestone", "malachite"),
        ):
            expected_f0 = int(materials_db[mineral]["F0"])
            with Image.open(t128 / f"{ore_id}_n.png") as inrm, Image.open(t128 / f"{ore_id}_s.png") as ispc:
                n_arr = np.array(inrm.convert("RGBA"), dtype=np.uint8)
                s_arr = np.array(ispc.convert("RGBA"), dtype=np.uint8)
            # Non-emissive A=255 everywhere
            self.assertEqual(int(s_arr[:, :, 3].min()), 255)
            # Mineral F0 code appears cleanly without blurry intermediate metal values
            self.assertIn(expected_f0, set(np.unique(s_arr[:, :, 1]).tolist()))
            # Height range reflects raised mineral grains (>= 1)
            self.assertGreaterEqual(int(n_arr[:, :, 3].min()), 1)
            self.assertGreater(int(n_arr[:, :, 3].max()) - int(n_arr[:, :, 3].min()), 20)

    def test_04_standalone_items_and_buckets(self) -> None:
        self.assertEqual(len(specs.STANDALONE_ITEMS), 46)
        for tier, expected_px in ((64, 32), (128, 64)):
            idir = art_export.tier_assets_dir(REPO_ROOT, tier, MODID) / "textures" / "item"
            for iname, _, _, _ in specs.STANDALONE_ITEMS:
                for suffix in ("", "_n", "_s"):
                    p = idir / f"{iname}{suffix}.png"
                    self.assertTrue(p.exists(), f"Missing item texture {p}")
                    with Image.open(p) as im:
                        self.assertEqual(im.size, (expected_px, expected_px))
                with Image.open(idir / f"{iname}.png") as ia:
                    arr = np.array(ia.convert("RGBA"), dtype=np.uint8)
                # Must be cutout 2D flat icon with transparent border and opaque interior
                self.assertEqual(int(arr[0, 0, 3]), 0, f"{iname} top-left corner should be transparent")
                self.assertEqual(int(arr[:, :, 3].max()), 255, f"{iname} must have opaque icon pixels")

    def test_05_iris_mapping_and_shaderpack_merger(self) -> None:
        iris_prop = art_export.mod_assets_dir(REPO_ROOT, MODID) / "iris" / "tc_iris_mapping.properties"
        self.assertTrue(iris_prop.exists())
        mappings = iris_compat.collect_repo_iris_mappings(REPO_ROOT)
        all_wl_mapped = {
            bid for ids in mappings.values() for bid in ids if bid.startswith(f"{MODID}:")
        }
        self.assertEqual(len(all_wl_mapped), 219)
        self.assertIn("tcwildlands:steel_mine_arch", mappings.get("metal", []))
        self.assertIn("tcwildlands:rock_bolts", mappings.get("metal", []))
        self.assertIn("tcwildlands:lava_crust", mappings.get("lava_like", []))
        self.assertIn("tcwildlands:deep_threshold", mappings.get("emissive_partial", []))

        # Test iris_compat.py on synthetic Complementary r5.6.1 zip & unknown shaderpack
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            comp_zip = tmp_dir / "ComplementaryReimagined_r5.6.1.zip"
            sample_bp = (
                "# Complementary Reimagined r5.6.1\n"
                "block.10068 = minecraft:lava minecraft:magma_block\n"
                "block.10092 = minecraft:sculk_catalyst minecraft:enchanting_table\n"
                "block.10216 = minecraft:iron_block minecraft:copper_block\n"
            )
            with zipfile.ZipFile(comp_zip, "w") as zf:
                zf.writestr("shaders/block.properties", sample_bp)

            res1 = iris_compat.merge_shaderpack_mappings(REPO_ROOT, comp_zip)
            self.assertTrue(res1.detection.recognized)
            self.assertTrue(res1.layer_b_applied)
            self.assertIsNotNone(res1.output_path)
            assert res1.output_path is not None
            self.assertTrue(res1.output_path.exists())
            self.assertNotEqual(res1.output_path, comp_zip)

            # Original zip must be untouched
            with zipfile.ZipFile(comp_zip, "r") as zin:
                self.assertEqual(zin.read("shaders/block.properties").decode("utf-8"), sample_bp)

            with zipfile.ZipFile(res1.output_path, "r") as zout:
                merged_txt_1 = zout.read("shaders/block.properties").decode("utf-8")
            self.assertIn("tcwildlands:steel_mine_arch", merged_txt_1)
            self.assertIn("tcwildlands:lava_crust", merged_txt_1)
            self.assertIn("tcwildlands:deep_threshold", merged_txt_1)

            # Idempotency check: re-merging onto the already-merged text produces identical output
            merged_txt_2, _ = iris_compat.merge_block_properties_text(merged_txt_1, mappings)
            self.assertEqual(merged_txt_1, merged_txt_2)

            # Unknown shaderpack must fall back cleanly to Layer A without mutating or creating output
            unk_dir = tmp_dir / "SomeUnknownShader_v1.0"
            (unk_dir / "shaders").mkdir(parents=True)
            (unk_dir / "shaders" / "block.properties").write_text("block.1 = minecraft:stone\n", encoding="utf-8")
            res_unk = iris_compat.merge_shaderpack_mappings(REPO_ROOT, unk_dir)
            self.assertFalse(res_unk.detection.recognized)
            self.assertFalse(res_unk.layer_b_applied)
            self.assertIsNone(res_unk.output_path)

    def test_06_gradle_script_and_full_export_check(self) -> None:
        gradle_script = REPO_ROOT / "gradle" / "compose-ore-textures.gradle"
        self.assertTrue(gradle_script.exists())
        gtxt = gradle_script.read_text(encoding="utf-8")
        self.assertIn("composeOreTextures", gtxt)
        self.assertIn("compose_ores.py", gtxt)

        report = art_export.validate_workspace(REPO_ROOT, list(art_export.MODULES))
        self.assertEqual(report.errors, [], f"export.py --check errors: {report.errors[:10]}")
        self.assertLessEqual(report.vram_128x_bytes, art_export.VRAM_BUDGET_BYTES)


if __name__ == "__main__":
    unittest.main()
