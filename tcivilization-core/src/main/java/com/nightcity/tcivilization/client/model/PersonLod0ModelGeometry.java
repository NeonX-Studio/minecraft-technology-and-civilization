package com.nightcity.tcivilization.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * LOD0 (<= 16 blocks) full modular humanoid geometry with separate facial expression, hair, E0 hide/hemp clothing layers, headwear mount, and tool sockets.
 *
 * <p>Generated from Blender source {@code art-src/entities/tcivilization/person/person_lod0.blend}.
 * Dedicated non-player UV layout ({@code 128x128}) per 32-art-audio.md section 4.3 rule 4.
 */
@OnlyIn(Dist.CLIENT)
public final class PersonLod0ModelGeometry {
    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 128;

    public static final String PART_WAIST = "waist";
    public static final String PART_BODY = "body";
    public static final String PART_OUTFIT_TOP = "outfit_top";
    public static final String PART_HEAD = "head";
    public static final String PART_FACE_EXPRESSION = "face_expression";
    public static final String PART_HAIR_LAYER = "hair_layer";
    public static final String PART_HEADWEAR_MOUNT = "headwear_mount";
    public static final String PART_RIGHT_ARM = "right_arm";
    public static final String PART_OUTFIT_RIGHT_SLEEVE = "outfit_right_sleeve";
    public static final String PART_RIGHT_HAND_TOOL_MOUNT = "right_hand_tool_mount";
    public static final String PART_LEFT_ARM = "left_arm";
    public static final String PART_OUTFIT_LEFT_SLEEVE = "outfit_left_sleeve";
    public static final String PART_LEFT_HAND_TOOL_MOUNT = "left_hand_tool_mount";
    public static final String PART_RIGHT_LEG = "right_leg";
    public static final String PART_OUTFIT_BOTTOM_RIGHT = "outfit_bottom_right";
    public static final String PART_LEFT_LEG = "left_leg";
    public static final String PART_OUTFIT_BOTTOM_LEFT = "outfit_bottom_left";

    private PersonLod0ModelGeometry() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part_waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition part_body = part_waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        part_body.addOrReplaceChild("outfit_top", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F))
                .texOffs(64, 18).addBox(-4.5F, 9.5F, -2.5F, 9.0F, 2.0F, 5.0F), PartPose.ZERO);
        PartDefinition part_head = part_waist.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        part_head.addOrReplaceChild("face_expression", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-3.0F, -5.0F, -4.15F, 2.0F, 1.0F, 1.0F)
                .texOffs(44, 0).addBox(1.0F, -5.0F, -4.15F, 2.0F, 1.0F, 1.0F)
                .texOffs(36, 4).addBox(-1.5F, -2.2F, -4.1F, 3.0F, 1.0F, 1.0F), PartPose.ZERO);
        part_head.addOrReplaceChild("hair_layer", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.35F))
                .texOffs(36, 64).addBox(-1.5F, -2.0F, 3.5F, 3.0F, 5.0F, 2.0F), PartPose.ZERO);
        part_head.addOrReplaceChild("headwear_mount", CubeListBuilder.create()
                .texOffs(64, 96).addBox(-4.5F, -1.0F, -4.5F, 9.0F, 2.0F, 9.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition part_right_arm = part_waist.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(28, 20).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, -10.0F, 0.0F));
        part_right_arm.addOrReplaceChild("outfit_right_sleeve", CubeListBuilder.create()
                .texOffs(96, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.22F)), PartPose.ZERO);
        part_right_arm.addOrReplaceChild("right_hand_tool_mount", CubeListBuilder.create()
                .texOffs(104, 96).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(-1.0F, 9.0F, 0.0F));
        PartDefinition part_left_arm = part_waist.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(46, 20).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, -10.0F, 0.0F));
        part_left_arm.addOrReplaceChild("outfit_left_sleeve", CubeListBuilder.create()
                .texOffs(96, 14).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.22F)), PartPose.ZERO);
        part_left_arm.addOrReplaceChild("left_hand_tool_mount", CubeListBuilder.create()
                .texOffs(114, 96).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(1.0F, 9.0F, 0.0F));
        PartDefinition part_right_leg = part_waist.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 0.0F, 0.0F));
        part_right_leg.addOrReplaceChild("outfit_bottom_right", CubeListBuilder.create()
                .texOffs(64, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
        PartDefinition part_left_leg = part_waist.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(18, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 0.0F, 0.0F));
        part_left_leg.addOrReplaceChild("outfit_bottom_left", CubeListBuilder.create()
                .texOffs(84, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
