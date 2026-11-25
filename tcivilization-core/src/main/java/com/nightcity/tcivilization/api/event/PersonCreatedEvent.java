package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.PersonRecord;
import java.util.Objects;

public final class PersonCreatedEvent extends TcEvent {
    private final PersonRecord person;

    public PersonCreatedEvent(PersonRecord person) {
        this.person = Objects.requireNonNull(person, "person");
    }

    public PersonRecord person() {
        return person;
    }
}
