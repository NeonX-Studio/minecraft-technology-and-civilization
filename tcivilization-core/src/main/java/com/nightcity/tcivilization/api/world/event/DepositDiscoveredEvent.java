package com.nightcity.tcivilization.api.world.event;

import com.nightcity.tcivilization.api.event.TcEvent;
import com.nightcity.tcivilization.api.world.Deposit;
import com.nightcity.tcivilization.api.world.ProspectMethod;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a civilization first discovers a mineral deposit (10 §12.2).
 */
public final class DepositDiscoveredEvent extends TcEvent {
    private final Deposit deposit;
    private final UUID civ;
    private final Optional<ServerPlayer> player;
    private final ProspectMethod method;

    public DepositDiscoveredEvent(Deposit deposit, UUID civ, Optional<ServerPlayer> player, ProspectMethod method) {
        this.deposit = Objects.requireNonNull(deposit, "deposit");
        this.civ = Objects.requireNonNull(civ, "civ");
        this.player = player == null ? Optional.empty() : player;
        this.method = Objects.requireNonNull(method, "method");
    }

    public Deposit deposit() {
        return deposit;
    }

    public UUID civ() {
        return civ;
    }

    public Optional<ServerPlayer> player() {
        return player;
    }

    public ProspectMethod method() {
        return method;
    }
}
