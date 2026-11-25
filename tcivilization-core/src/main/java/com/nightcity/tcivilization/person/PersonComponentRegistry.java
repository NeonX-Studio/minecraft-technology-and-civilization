package com.nightcity.tcivilization.person;

import com.nightcity.tcivilization.api.PersonComponentType;
import com.nightcity.tcivilization.api.event.RegisterPersonComponentsEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;

/**
 * Registry for PersonComponentType definitions, pre-seeded with schedule and needs.
 */
public final class PersonComponentRegistry {
    private static final PersonComponentRegistry GLOBAL = new PersonComponentRegistry();

    private final Map<Identifier, PersonComponentType<?>> types = new LinkedHashMap<>();
    private volatile boolean frozen = false;

    public PersonComponentRegistry() {
        seedBuiltins();
    }

    private void seedBuiltins() {
        types.put(PersonSchedule.ID, PersonSchedule.TYPE);
        types.put(PersonNeeds.ID, PersonNeeds.TYPE);
    }

    public static PersonComponentRegistry global() {
        return GLOBAL;
    }

    public synchronized <T> void register(PersonComponentType<T> type) {
        if (frozen) {
            throw new IllegalStateException("PersonComponentRegistry is already frozen");
        }
        if (types.containsKey(type.id()) && types.get(type.id()) != type) {
            throw new IllegalArgumentException("Duplicate PersonComponentType: " + type.id());
        }
        types.put(type.id(), type);
    }

    public synchronized void dispatchRegistration(IEventBus modBus) {
        if (frozen) {
            return;
        }
        com.nightcity.tcivilization.TCivilization.postModBusEvent(modBus, new RegisterPersonComponentsEvent(types));
        frozen = true;
    }

    public synchronized Optional<PersonComponentType<?>> get(Identifier id) {
        return Optional.ofNullable(types.get(id));
    }

    public synchronized Map<Identifier, PersonComponentType<?>> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(types));
    }

    public synchronized void resetForTests() {
        frozen = false;
        types.clear();
        seedBuiltins();
    }
}
