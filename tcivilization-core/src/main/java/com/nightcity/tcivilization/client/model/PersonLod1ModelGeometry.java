package com.nightcity.tcivilization.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * LOD1 (16-48 blocks) simplified humanoid rig with clothing layers merged onto torso and limbs, no separate facial expression mesh.
 *
 * <p>Generated from Blender source {@code art-src/entities/tcivilization/person/person_lod1.blend}.
 * Dedicated non-player UV layout ({@code 64x64}) per 32-art-audio.md section 4.3 rule 4.
 */
@OnlyIn(Dist.CLIENT)
public final class PersonLod1ModelGeometry {
    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 64;

    public static final String PART_BODY = "body";
    public static final String PART_HEAD = "head";
    public static final String PART_RIGHT_ARM = "right_arm";
    public static final String PART_LEFT_ARM = "left_arm";
    public static final String PART_RIGHT_LEG = "right_leg";
    public static final String PART_LEFT_LEG = "left_leg";

    private PersonLod1ModelGeometry() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part_body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);
        part_body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        part_body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(26, 18).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        part_body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(44, 18).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        part_body.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        part_body.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(18, 36).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
