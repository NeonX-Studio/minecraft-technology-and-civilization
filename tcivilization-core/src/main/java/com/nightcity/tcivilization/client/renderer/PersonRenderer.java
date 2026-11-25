package com.nightcity.tcivilization.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightcity.tcivilization.client.model.PersonModelLocations;
import com.nightcity.tcivilization.person.PersonEntity;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Multi-LOD entity renderer for {@code tcivilization:person} backed by Blender-exported
 * LOD0/LOD1/LOD2 non-player geometry and baked E0 textures (32-art-audio.md section 4.2-4.4).
 */
public final class PersonRenderer extends MobRenderer<PersonEntity, PersonRenderer.PersonRenderState, PersonRenderer.PersonEntityModel> {
    private static final double DEFAULT_ENTITY_LOD_SCALE = 1.0D;

    private final PersonEntityModel lod0Model;
    private final PersonEntityModel lod1Model;
    private final PersonEntityModel lod2Model;

    public PersonRenderer(EntityRendererProvider.Context context) {
        this(
                context,
                new PersonEntityModel(context.bakeLayer(PersonModelLocations.LOD0), 0),
                new PersonEntityModel(context.bakeLayer(PersonModelLocations.LOD1), 1),
                new PersonEntityModel(context.bakeLayer(PersonModelLocations.LOD2), 2)
        );
    }

    private PersonRenderer(
            EntityRendererProvider.Context context,
            PersonEntityModel lod0Model,
            PersonEntityModel lod1Model,
            PersonEntityModel lod2Model
    ) {
        super(context, lod0Model, 0.5F);
        this.lod0Model = lod0Model;
        this.lod1Model = lod1Model;
        this.lod2Model = lod2Model;
    }

    @Override
    public PersonRenderState createRenderState() {
        return new PersonRenderState();
    }

    @Override
    public void extractRenderState(PersonEntity entity, PersonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        double distanceBlocks = Math.sqrt(Math.max(0.0D, state.distanceToCameraSq));
        state.lodTier = PersonModelLocations.selectLodTier(distanceBlocks, DEFAULT_ENTITY_LOD_SCALE);
    }

    @Override
    public void submit(
            PersonRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (state.lodTier < 0) {
            return;
        }
        this.model = switch (state.lodTier) {
            case 0 -> lod0Model;
            case 1 -> lod1Model;
            default -> lod2Model;
        };
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(PersonRenderState state) {
        return switch (state.lodTier) {
            case 0 -> PersonModelLocations.TEXTURE_LOD0_COMPOSITE_E0;
            case 1 -> PersonModelLocations.TEXTURE_LOD1_E0;
            default -> PersonModelLocations.TEXTURE_LOD2_E0;
        };
    }

    public static final class PersonRenderState extends LivingEntityRenderState {
        public int lodTier = 0;
    }

    public static final class PersonEntityModel extends EntityModel<PersonRenderState> {
        private final int lodTier;
        @Nullable
        private final ModelPart head;
        @Nullable
        private final ModelPart rightArm;
        @Nullable
        private final ModelPart leftArm;
        @Nullable
        private final ModelPart rightLeg;
        @Nullable
        private final ModelPart leftLeg;

        public PersonEntityModel(ModelPart root, int lodTier) {
            super(root);
            this.lodTier = lodTier;
            Function<String, ModelPart> lookup = root.createPartLookup();
            this.head = lookup.apply("head");
            this.rightArm = lookup.apply("right_arm");
            this.leftArm = lookup.apply("left_arm");
            this.rightLeg = lookup.apply("right_leg");
            this.leftLeg = lookup.apply("left_leg");
        }

        @Override
        public void setupAnim(PersonRenderState state) {
            super.setupAnim(state);
            if (lodTier >= 2) {
                return;
            }
            float walkPos = lodTier == 1 ? Math.round(state.walkAnimationPos * 0.5F) * 2.0F : state.walkAnimationPos;
            float walkSpeed = state.walkAnimationSpeed;
            if (head != null) {
                head.yRot = state.yRot * ((float) Math.PI / 180.0F);
                head.xRot = state.xRot * ((float) Math.PI / 180.0F);
            }
            float swing = (float) Math.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
            float oppositeSwing = (float) Math.cos(walkPos * 0.6662F + Math.PI) * 1.4F * walkSpeed;
            if (rightArm != null) {
                rightArm.xRot = oppositeSwing * 0.5F;
            }
            if (leftArm != null) {
                leftArm.xRot = swing * 0.5F;
            }
            if (rightLeg != null) {
                rightLeg.xRot = swing;
            }
            if (leftLeg != null) {
                leftLeg.xRot = oppositeSwing;
            }
        }
    }
}
