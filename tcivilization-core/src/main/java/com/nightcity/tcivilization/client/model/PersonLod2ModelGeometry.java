package com.nightcity.tcivilization.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * LOD2 (48-128 blocks) single-mesh low-poly static pose proxy with baked base colors (no skeletal animation).
 *
 * <p>Generated from Blender source {@code art-src/entities/tcivilization/person/person_lod2.blend}.
 * Dedicated non-player UV layout ({@code 64x32}) per 32-art-audio.md section 4.3 rule 4.
 */
@OnlyIn(Dist.CLIENT)
public final class PersonLod2ModelGeometry {
    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 32;

    public static final String PART_PROXY = "proxy";

    private PersonLod2ModelGeometry() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("proxy", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -8.0F, -3.5F, 7.0F, 8.0F, 7.0F)
                .texOffs(30, 0).addBox(-5.5F, 0.0F, -2.0F, 11.0F, 12.0F, 4.0F)
                .texOffs(0, 16).addBox(-3.5F, 12.0F, -2.0F, 7.0F, 12.0F, 4.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
