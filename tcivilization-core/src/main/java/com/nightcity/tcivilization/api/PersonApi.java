package com.nightcity.tcivilization.api;

import com.nightcity.tcivilization.person.PersonEntity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Server-side shared NPC record and component management facade (03 §5.7, §12.1).
 */
public interface PersonApi {
    PersonRecord create(PersonSpec spec);

    Optional<PersonRecord> get(UUID id);

    <T> Optional<T> component(UUID person, PersonComponentType<T> type);

    <T> void setComponent(UUID person, PersonComponentType<T> type, @Nullable T value);

    void assignTask(UUID person, TaskSpec task);

    void kill(UUID person, Identifier cause);

    Optional<PersonEntity> entity(UUID person);

    void requestEmbody(UUID person, float importanceFactor, int ticks);
}
