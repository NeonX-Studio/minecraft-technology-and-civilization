from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[3]
EXPORT_DIR = REPO_ROOT / "tools" / "art-export"
if str(EXPORT_DIR) not in sys.path:
    sys.path.insert(0, str(EXPORT_DIR))

import export as art_export  # noqa: E402


def test_materials_library_has_all_33_standard_materials() -> None:
    mat_file = REPO_ROOT / "art-src" / "materials.json"
    assert mat_file.exists()
    data = json.loads(mat_file.read_text(encoding="utf-8"))
    mats = data["materials"]
    expected = {
        "granite", "basalt_rock", "gabbro", "rhyolite", "pumice",
        "peridotite", "sandstone_rock", "shale", "limestone", "dolomite",
        "chalk", "conglomerate", "mudstone", "rock_salt", "gypsum_rock",
        "slate", "schist", "gneiss", "marble", "quartzite",
        "oak", "terracotta", "porcelain", "glass", "bronze",
        "wrought_iron", "steel", "brass", "aluminum", "titanium",
        "bakelite", "plastic", "MLI",
    }
    assert expected.issubset(set(mats.keys()))
    assert len(mats) >= 33
    for name, spec in mats.items():
        assert 0 <= int(spec["S"]) <= 255, name
        assert 0 <= int(spec["F0"]) <= 255, name
        assert 0 <= int(spec["b_channel"]) <= 255, name


def test_labpbr_smoothness_and_normal_renormalization_and_height_clamp() -> None:
    # Roughness -> Smoothness exact formula: S = round(255 * (1 - sqrt(roughness)))
    r = np.array([[0.0, 0.25, 1.0]], dtype=np.float32)
    s = art_export.roughness_to_smoothness_u8(r)
    assert s.tolist() == [[255, 128, 0]]

    # Perturbed normal map downsampled -> unit vectors in OpenGL tangent space
    nrm = np.full((16, 16, 3), [180, 90, 220], dtype=np.uint8)
    rx, gy = art_export.downsample_and_renormalize_normal(Image.fromarray(nrm, mode="RGB"), 4, 4)
    assert rx.shape == (4, 4) and gy.shape == (4, 4)
    nx = (rx.astype(np.float32) / 255.0) * 2.0 - 1.0
    ny = (gy.astype(np.float32) / 255.0) * 2.0 - 1.0
    assert np.all(nx * nx + ny * ny <= 1.0001)
    assert np.all(nx > 0.0) and np.all(ny < 0.0)


def test_person_lod_sources_and_java_geometry_exports() -> None:
    person_dir = REPO_ROOT / "art-src" / "entities" / "tcivilization" / "person"
    for lod in ("lod0", "lod1", "lod2"):
        assert (person_dir / f"person_{lod}.blend").exists()
        assert (person_dir / f"person_{lod}.glb").exists()
        assert (person_dir / f"person_{lod}.glb").stat().st_size > 200

    java_dir = (
        REPO_ROOT
        / "tcivilization-core"
        / "src"
        / "main"
        / "java"
        / "com"
        / "nightcity"
        / "tcivilization"
        / "client"
        / "model"
    )
    for cls in (
        "PersonModelLocations.java",
        "PersonLod0ModelGeometry.java",
        "PersonLod1ModelGeometry.java",
        "PersonLod2ModelGeometry.java",
    ):
        jf = java_dir / cls
        assert jf.exists(), f"Missing {cls}"
        content = jf.read_text(encoding="utf-8")
        # Zero coupling to other tcivilization gameplay classes
        for line in content.splitlines():
            if line.startswith("import com.nightcity."):
                raise AssertionError(f"Unexpected coupling import in {cls}: {line}")

    ent_tex_dir = (
        REPO_ROOT
        / "tcivilization-core"
        / "src"
        / "main"
        / "resources"
        / "assets"
        / "tcivilization"
        / "textures"
        / "entity"
        / "person"
    )
    for tname in (
        "person_lod0_base.png",
        "person_lod0_hair_e0.png",
        "person_lod0_outfit_e0_hide.png",
        "person_lod0_composite_e0.png",
        "person_lod1_e0.png",
        "person_lod2_e0.png",
    ):
        assert (ent_tex_dir / tname).exists()


def test_modeled_blocks_items_skins_and_full_pipeline_check() -> None:
    expected_blocks = [
        ("tcivilization", "placeholder"),
        ("tctechnology", "fire_pit"),
        ("tctechnology", "grinding_slab"),
        ("tctechnology", "research_table"),
        ("tctechnology", "milestone_marker"),
        ("tctechnology", "milestone_cache"),
        ("tctechnology", "comm_station"),
    ]
    for modid, bname in expected_blocks:
        master_dir = REPO_ROOT / "art-src" / "blocks" / modid / bname
        for map_name in ("albedo.png", "normal.png", "height.png", "ao.png", "roughness.png", "meta.json"):
            assert (master_dir / map_name).exists(), f"Missing {master_dir / map_name}"
        with Image.open(master_dir / "albedo.png") as im:
            assert im.size == (2048, 2048)

        model_path = art_export.mod_assets_dir(REPO_ROOT, modid) / "models" / "block" / f"{bname}.json"
        model_data = json.loads(model_path.read_text(encoding="utf-8"))
        assert 1 <= len(model_data["elements"]) <= 64

    # Check Iris mapping output
    for modid in ("tcivilization", "tctechnology"):
        iris_file = art_export.mod_assets_dir(REPO_ROOT, modid) / "iris" / "tc_iris_mapping.properties"
        assert iris_file.exists()
        text = iris_file.read_text(encoding="utf-8")
        assert "category." in text

    # Check registry coverage counts match 36-registry-index.md (1057 blocks, 674 items)
    reg = art_export.parse_registry_blocks_and_items(REPO_ROOT)
    assert sum(len(v["block"]) for v in reg.values()) == 1057
    assert sum(len(v["item"]) for v in reg.values()) == 674

    # Full workspace validation must pass with 0 errors and VRAM <= 1.5 GiB
    report = art_export.validate_workspace(REPO_ROOT, list(art_export.MODULES))
    assert report.errors == []
    assert 0 < report.vram_128x_bytes <= art_export.VRAM_BUDGET_BYTES
    assert report.skin_contrasts["tcivilization:skin/neutral"]["text_vs_panel"] >= 4.5
    assert report.skin_contrasts["tcatlas:skin/stone"]["text_vs_panel"] >= 4.5


def test_zero_height_clamp_and_multi_tier_export_in_temp_workspace() -> None:
    import tempfile

    materials_db = art_export.load_materials_library(REPO_ROOT)
    with tempfile.TemporaryDirectory() as tmp:
        tmp_dir = Path(tmp)
        src = tmp_dir / "zero_height_block"
        src.mkdir(parents=True)
        Image.fromarray(np.full((64, 64, 4), [140, 120, 110, 255], dtype=np.uint8), mode="RGBA").save(src / "albedo.png")
        Image.fromarray(np.full((64, 64, 3), [128, 128, 255], dtype=np.uint8), mode="RGB").save(src / "normal.png")
        Image.fromarray(np.zeros((64, 64), dtype=np.uint8), mode="L").save(src / "height.png")
        Image.fromarray(np.full((64, 64), 240, dtype=np.uint8), mode="L").save(src / "ao.png")
        Image.fromarray(np.full((64, 64), 190, dtype=np.uint8), mode="L").save(src / "roughness.png")
        meta = {"material": "granite", "emissive": False, "iris_category": "solid"}

        for target_tier in (64, 128, 256, 512):
            alb, nrm, spc = art_export.pack_labpbr_triplet(src, meta, materials_db, target_tier)
            assert alb.shape == (target_tier, target_tier, 4)
            assert nrm.shape == (target_tier, target_tier, 4)
            assert spc.shape == (target_tier, target_tier, 4)
            # Height alpha channel must be strictly clamped >= 1 even when source height == 0
            assert int(nrm[:, :, 3].min()) == 1
            # Granite F0=10, P=25, non-emissive A=255
            assert int(spc[0, 0, 1]) == 10
            assert int(spc[0, 0, 2]) == 25
            assert int(spc[0, 0, 3]) == 255


def test_placeholder_sync_preserves_modeled_entries() -> None:
    fire_pit_bs = (
        REPO_ROOT / "tctechnology" / "src" / "main" / "resources" / "assets" / "tctechnology" / "blockstates" / "fire_pit.json"
    )
    before = fire_pit_bs.read_text(encoding="utf-8")
    stats = art_export.sync_registry_placeholders(REPO_ROOT, ["tctechnology"])
    after = fire_pit_bs.read_text(encoding="utf-8")
    assert before == after
    assert "tctechnology:block/fire_pit" in after
    assert stats["preserved_modeled"] == 6


import unittest  # noqa: E402


class ArtPipelineUnitTests(unittest.TestCase):
    def test_all_p0_foundation_art_checks(self) -> None:
        test_materials_library_has_all_33_standard_materials()
        test_labpbr_smoothness_and_normal_renormalization_and_height_clamp()
        test_person_lod_sources_and_java_geometry_exports()
        test_modeled_blocks_items_skins_and_full_pipeline_check()
        test_zero_height_clamp_and_multi_tier_export_in_temp_workspace()
        test_placeholder_sync_preserves_modeled_entries()


if __name__ == "__main__":
    unittest.main()
