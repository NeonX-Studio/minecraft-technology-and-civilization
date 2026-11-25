package com.nightcity.tcivilization.api.event;

import com.nightcity.tcivilization.api.PersonRecord;
import java.util.Objects;
import net.neoforged.bus.api.ICancellableEvent;

public final class PersonEmbodyEvent {
    private PersonEmbodyEvent() {
    }

    public static final class Pre extends TcEvent implements ICancellableEvent {
        private final PersonRecord person;

        public Pre(PersonRecord person) {
            this.person = Objects.requireNonNull(person, "person");
        }

        public PersonRecord person() {
            return person;
        }
    }
}
