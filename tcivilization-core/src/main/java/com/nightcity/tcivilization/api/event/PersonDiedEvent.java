package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.PersonRecord;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class PersonDiedEvent extends TcEvent {
    private final PersonRecord person;
    private final Identifier cause;

    public PersonDiedEvent(PersonRecord person, Identifier cause) {
        this.person = Objects.requireNonNull(person, "person");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public PersonRecord person() {
        return person;
    }

    public Identifier cause() {
        return cause;
    }
}
