package com.nightcity.tcivilization.person;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Shared humanoid NPC shell entity tcivilization:person (03 §5.7.1).
 * Hitbox 0.6 x 1.8; authoritative state lives in PersonRecord.
 */
public class PersonEntity extends PathfinderMob {
    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 1.8F;
    public static final double WALK_SPEED = 0.5D;
    public static final double SPRINT_SPEED = 0.65D;

    @Nullable
    private UUID personId;
    private UUID civId = com.nightcity.tcivilization.api.Civilization.PLAYER_CIV_ID;

    public PersonEntity(EntityType<? extends PersonEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public Optional<UUID> personId() {
        return Optional.ofNullable(personId);
    }

    public void setPersonId(@Nullable UUID personId) {
        this.personId = personId;
    }

    public UUID civId() {
        return civId;
    }

    public void setCivId(UUID civId) {
        if (civId != null) {
            this.civId = civId;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            String idStr = personId != null ? personId.toString() : this.getUUID().toString();
            serverPlayer.sendSystemMessage(Component.translatable("gui.tcivilization.person.inspect_stub", this.getDisplayName(), idStr));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("person_id", UUIDUtil.CODEC, this.personId);
        output.storeNullable("civ_id", UUIDUtil.CODEC, this.civId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.personId = input.read("person_id", UUIDUtil.CODEC).orElse(null);
        this.civId = input.read("civ_id", UUIDUtil.CODEC)
                .orElse(com.nightcity.tcivilization.api.Civilization.PLAYER_CIV_ID);
    }
}
