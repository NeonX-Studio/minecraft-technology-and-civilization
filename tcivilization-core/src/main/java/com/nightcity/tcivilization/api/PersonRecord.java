package com.nightcity.tcivilization.api;

import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMaps;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;

/**
 * Server-side authoritative record for a shared person across EMBODIED/PROXY/ABSTRACT/DEAD states (03 §5.7.2, §11).
 */
public record PersonRecord(
        UUID id,
        UUID civ,
        Optional<UUID> settlement,
        PersonName name,
        int birthDay,
        Identifier culture,
        long appearance,
        Map<Identifier, Object> components,
        TaskSummary task,
        GlobalPos lastPos,
        PersonState state,
        EnumSet<ImportanceFlag> importance,
        Int2ByteMap relations
) {
    public static final int MAX_RELATIONS = 32;

    public PersonRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(civ, "civ");
        settlement = settlement == null ? Optional.empty() : settlement;
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(culture, "culture");
        components = components == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(components));
        task = task == null ? TaskSummary.idle() : task;
        Objects.requireNonNull(lastPos, "lastPos");
        Objects.requireNonNull(state, "state");
        importance = importance == null ? EnumSet.noneOf(ImportanceFlag.class) : EnumSet.copyOf(importance);
        relations = relations == null ? Int2ByteMaps.EMPTY_MAP : Int2ByteMaps.unmodifiable(new Int2ByteOpenHashMap(relations));
    }

    public PersonRecord withState(PersonState nextState) {
        return new PersonRecord(id, civ, settlement, name, birthDay, culture, appearance, components, task, lastPos, nextState, importance, relations);
    }

    public PersonRecord withLastPos(GlobalPos nextPos) {
        return new PersonRecord(id, civ, settlement, name, birthDay, culture, appearance, components, task, nextPos, state, importance, relations);
    }

    public PersonRecord withTask(TaskSummary nextTask) {
        return new PersonRecord(id, civ, settlement, name, birthDay, culture, appearance, components, nextTask, lastPos, state, importance, relations);
    }

    public PersonRecord withComponents(Map<Identifier, Object> nextComponents) {
        return new PersonRecord(id, civ, settlement, name, birthDay, culture, appearance, nextComponents, task, lastPos, state, importance, relations);
    }

    public boolean isProtectedFromAbstractDeath() {
        return !importance.isEmpty();
    }
}
