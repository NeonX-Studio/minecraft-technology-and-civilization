package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.HazardType;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.ICancellableEvent;
import org.jspecify.annotations.Nullable;

/**
 * Cancellable event fired before a natural hazard damages a block or entity (10 §12.2).
 */
public final class HazardDamageEvent extends TcEvent implements ICancellableEvent {
    @Nullable
    private final ServerLevel level;
    private final BlockPos pos;
    private final HazardType type;
    private final float intensity;
    private float damageMultiplier;

    public HazardDamageEvent(
            @Nullable ServerLevel level,
            BlockPos pos,
            HazardType type,
            float intensity,
            float damageMultiplier
    ) {
        this.level = level;
        this.pos = Objects.requireNonNull(pos, "pos");
        this.type = Objects.requireNonNull(type, "type");
        this.intensity = intensity;
        this.damageMultiplier = damageMultiplier;
    }

    @Nullable
    public ServerLevel level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    public HazardType type() {
        return type;
    }

    public float intensity() {
        return intensity;
    }

    public float damageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = Math.max(0.0f, damageMultiplier);
    }
}
