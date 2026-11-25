package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.PersonRecord;
import com.nightcity.tcivilization.person.PersonEntity;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class PersonEmbodiedEvent extends TcEvent {
    private final PersonRecord person;
    @Nullable
    private final PersonEntity entity;

    public PersonEmbodiedEvent(PersonRecord person, @Nullable PersonEntity entity) {
        this.person = Objects.requireNonNull(person, "person");
        this.entity = entity;
    }

    public PersonRecord person() {
        return person;
    }

    @Nullable
    public PersonEntity entity() {
        return entity;
    }
}
