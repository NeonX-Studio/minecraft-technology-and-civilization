package com.nightcity.tcivilization.person;

import com.nightcity.tcivilization.api.ImportanceFlag;
import com.nightcity.tcivilization.api.PersonApi;
import com.nightcity.tcivilization.api.PersonComponentType;
import com.nightcity.tcivilization.api.PersonRecord;
import com.nightcity.tcivilization.api.PersonSpec;
import com.nightcity.tcivilization.api.PersonState;
import com.nightcity.tcivilization.api.TaskSpec;
import com.nightcity.tcivilization.api.TaskSummary;
import com.nightcity.tcivilization.api.event.PersonAbstractedEvent;
import com.nightcity.tcivilization.api.event.PersonComponentChangedEvent;
import com.nightcity.tcivilization.api.event.PersonCreatedEvent;
import com.nightcity.tcivilization.api.event.PersonDiedEvent;
import com.nightcity.tcivilization.api.event.PersonEmbodiedEvent;
import com.nightcity.tcivilization.api.event.PersonEmbodyEvent;
import com.nightcity.tcivilization.sim.TcEventBus;
import it.unimi.dsi.fastutil.ints.Int2ByteMaps;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Server-side PersonApi implementation managing PersonRecord states, components, and embodiment requests.
 */
public final class PersonManager implements PersonApi {
    public record EmbodyBoost(float factor, long expiresAtTick) {}

    private final Map<UUID, PersonRecord> records = new ConcurrentHashMap<>();
    private final Map<UUID, WeakReference<PersonEntity>> embodiedEntities = new ConcurrentHashMap<>();
    private final Map<UUID, EmbodyBoost> embodyBoosts = new ConcurrentHashMap<>();
    private final PersonComponentRegistry componentRegistry;
    private final TcEventBus eventBus;
    private final LongSupplier tickSupplier;

    public PersonManager(PersonComponentRegistry componentRegistry, TcEventBus eventBus, LongSupplier tickSupplier) {
        this.componentRegistry = Objects.requireNonNull(componentRegistry, "componentRegistry");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.tickSupplier = Objects.requireNonNull(tickSupplier, "tickSupplier");
    }

    @Override
    public synchronized PersonRecord create(PersonSpec spec) {
        Objects.requireNonNull(spec, "spec");
        UUID id = UUID.randomUUID();
        Map<Identifier, Object> defaultComponents = new LinkedHashMap<>();
        defaultComponents.put(PersonSchedule.ID, PersonSchedule.standard());
        defaultComponents.put(PersonNeeds.ID, PersonNeeds.full());

        PersonRecord record = new PersonRecord(
                id,
                spec.civ(),
                spec.settlement(),
                spec.name(),
                spec.birthDay(),
                spec.culture(),
                spec.appearance(),
                defaultComponents,
                TaskSummary.idle(),
                spec.initialPos(),
                PersonState.ABSTRACT,
                spec.importance(),
                Int2ByteMaps.EMPTY_MAP
        );
        records.put(id, record);
        eventBus.enqueue(new PersonCreatedEvent(record));
        return record;
    }

    @Override
    public Optional<PersonRecord> get(UUID id) {
        return Optional.ofNullable(records.get(id));
    }

    public Collection<PersonRecord> all() {
        return List.copyOf(records.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> component(UUID person, PersonComponentType<T> type) {
        PersonRecord rec = records.get(person);
        if (rec == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) rec.components().get(type.id()));
    }

    @Override
    public synchronized <T> void setComponent(UUID person, PersonComponentType<T> type, @Nullable T value) {
        Objects.requireNonNull(type, "type");
        PersonRecord rec = records.get(person);
        if (rec == null || rec.state() == PersonState.DEAD) {
            return;
        }
        Map<Identifier, Object> next = new LinkedHashMap<>(rec.components());
        boolean added;
        if (value == null) {
            if (!next.containsKey(type.id())) {
                return;
            }
            next.remove(type.id());
            added = false;
        } else {
            next.put(type.id(), value);
            added = true;
        }
        records.put(person, rec.withComponents(next));
        eventBus.enqueue(new PersonComponentChangedEvent(person, type.id(), added));
    }

    @Override
    public synchronized void assignTask(UUID person, TaskSpec task) {
        Objects.requireNonNull(task, "task");
        PersonRecord rec = records.get(person);
        if (rec == null || rec.state() == PersonState.DEAD) {
            return;
        }
        records.put(person, rec.withTask(new TaskSummary(task.type(), task.targetPos(), 0.0f)));
    }

    @Override
    public synchronized void kill(UUID person, Identifier cause) {
        Objects.requireNonNull(cause, "Explicit cause required to kill NPC");
        PersonRecord rec = records.get(person);
        if (rec == null || rec.state() == PersonState.DEAD) {
            return;
        }
        PersonEntity ent = entity(person).orElse(null);
        if (ent != null) {
            ent.discard();
        }
        embodiedEntities.remove(person);
        PersonRecord dead = rec.withState(PersonState.DEAD);
        records.put(person, dead);
        eventBus.enqueue(new PersonDiedEvent(dead, cause));
    }

    @Override
    public Optional<PersonEntity> entity(UUID person) {
        WeakReference<PersonEntity> ref = embodiedEntities.get(person);
        if (ref == null) {
            return Optional.empty();
        }
        PersonEntity ent = ref.get();
        if (ent == null || ent.isRemoved()) {
            embodiedEntities.remove(person);
            return Optional.empty();
        }
        return Optional.of(ent);
    }

    @Override
    public void requestEmbody(UUID person, float importanceFactor, int ticks) {
        float clamped = Math.clamp(importanceFactor, 0.0f, 1.0f);
        embodyBoosts.put(person, new EmbodyBoost(clamped, tickSupplier.getAsLong() + Math.max(1, ticks)));
    }

    public float effectiveImportanceFactor(PersonRecord record, long currentTick) {
        EmbodyBoost boost = embodyBoosts.get(record.id());
        if (boost != null) {
            if (currentTick <= boost.expiresAtTick()) {
                return boost.factor();
            }
            embodyBoosts.remove(record.id());
        }
        if (record.importance().contains(ImportanceFlag.NAMED)
                || record.importance().contains(ImportanceFlag.LEGEND)
                || record.importance().contains(ImportanceFlag.LEADER)) {
            return 0.7f;
        }
        if (!TaskSummary.IDLE_ID.equals(record.task().type())) {
            return 0.5f;
        }
        return 1.0f;
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean transitionToEmbodied(UUID personId, @Nullable PersonEntity spawnedEntity) {
        PersonRecord rec = records.get(personId);
        if (rec == null || rec.state() == PersonState.DEAD) {
            return false;
        }
        if (rec.state() == PersonState.EMBODIED) {
            return true;
        }
        PersonEmbodyEvent.Pre pre = eventBus.postImmediate(new PersonEmbodyEvent.Pre(rec));
        if (pre.isCanceled()) {
            return false;
        }
        PersonRecord embodied = rec.withState(PersonState.EMBODIED);
        records.put(personId, embodied);
        if (spawnedEntity != null) {
            spawnedEntity.setPersonId(personId);
            spawnedEntity.setCivId(embodied.civ());
            embodiedEntities.put(personId, new WeakReference<>(spawnedEntity));
            for (Identifier compId : embodied.components().keySet()) {
                componentRegistry.get(compId).ifPresent(type ->
                        ((PersonComponentType<Object>) type).onEmbody().accept(embodied, spawnedEntity));
            }
        }
        eventBus.enqueue(new PersonEmbodiedEvent(embodied, spawnedEntity));
        return true;
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean transitionFromEmbodied(UUID personId, PersonState targetState) {
        if (targetState != PersonState.PROXY && targetState != PersonState.ABSTRACT) {
            throw new IllegalArgumentException("Invalid non-embodied target state: " + targetState);
        }
        PersonRecord rec = records.get(personId);
        if (rec == null || rec.state() == PersonState.DEAD || rec.state() == targetState) {
            return false;
        }
        PersonEntity ent = entity(personId).orElse(null);
        if (ent != null) {
            for (Identifier compId : rec.components().keySet()) {
                componentRegistry.get(compId).ifPresent(type ->
                        ((PersonComponentType<Object>) type).onAbstract().accept(ent, rec));
            }
            ent.discard();
        }
        embodiedEntities.remove(personId);
        PersonRecord updated = rec.withState(targetState);
        records.put(personId, updated);
        eventBus.enqueue(new PersonAbstractedEvent(updated, targetState));
        return true;
    }

    public synchronized void setChunkLoadedState(UUID personId, boolean chunkLoaded) {
        PersonRecord rec = records.get(personId);
        if (rec == null || rec.state() == PersonState.DEAD) {
            return;
        }
        if (chunkLoaded && rec.state() == PersonState.ABSTRACT) {
            records.put(personId, rec.withState(PersonState.PROXY));
        } else if (!chunkLoaded && rec.state() != PersonState.ABSTRACT) {
            transitionFromEmbodied(personId, PersonState.ABSTRACT);
        }
    }

    public int activeCount() {
        int count = 0;
        for (PersonRecord r : records.values()) {
            if (r.state() != PersonState.DEAD) {
                count++;
            }
        }
        return count;
    }

    public int embodiedCount() {
        int count = 0;
        for (PersonRecord r : records.values()) {
            if (r.state() == PersonState.EMBODIED) {
                count++;
            }
        }
        return count;
    }
}
