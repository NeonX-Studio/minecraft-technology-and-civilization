package com.nightcity.tcivilization.api.event;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.Identifier;

public final class CivMemberChangedEvent extends TcEvent {
    public enum Change {
        JOINED,
        LEFT,
        KICKED,
        ROLE_CHANGED
    }

    private final UUID civId;
    private final UUID playerId;
    private final Change change;
    private final Identifier role;

    public CivMemberChangedEvent(UUID civId, UUID playerId, Change change, Identifier role) {
        this.civId = Objects.requireNonNull(civId, "civId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.change = Objects.requireNonNull(change, "change");
        this.role = Objects.requireNonNull(role, "role");
    }

    public UUID civId() {
        return civId;
    }

    public UUID playerId() {
        return playerId;
    }

    public Change change() {
        return change;
    }

    public Identifier role() {
        return role;
    }
}
