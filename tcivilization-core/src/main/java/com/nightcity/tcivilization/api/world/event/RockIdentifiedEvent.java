package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.ProspectMethod;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Fired when a player identifies a rock formation via tapping or observation (10 §12.2).
 */
public final class RockIdentifiedEvent extends TcEvent {
    @Nullable
    private final ServerPlayer player;
    private final Identifier rock;
    private final boolean firstForCiv;
    private final ProspectMethod method;

    public RockIdentifiedEvent(@Nullable ServerPlayer player, Identifier rock, boolean firstForCiv, ProspectMethod method) {
        this.player = player;
        this.rock = Objects.requireNonNull(rock, "rock");
        this.firstForCiv = firstForCiv;
        this.method = Objects.requireNonNull(method, "method");
    }

    @Nullable
    public ServerPlayer player() {
        return player;
    }

    public Identifier rock() {
        return rock;
    }

    public boolean firstForCiv() {
        return firstForCiv;
    }

    public ProspectMethod method() {
        return method;
    }
}
