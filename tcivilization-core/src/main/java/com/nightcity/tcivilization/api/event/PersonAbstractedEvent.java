package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.PersonRecord;
import com.nightcity.tcivilization.api.PersonState;
import java.util.Objects;

public final class PersonAbstractedEvent extends TcEvent {
    private final PersonRecord person;
    private final PersonState targetState;

    public PersonAbstractedEvent(PersonRecord person, PersonState targetState) {
        this.person = Objects.requireNonNull(person, "person");
        this.targetState = Objects.requireNonNull(targetState, "targetState");
    }

    public PersonRecord person() {
        return person;
    }

    public PersonState targetState() {
        return targetState;
    }
}
