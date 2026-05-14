package de.skyforce.main.elden.ashes.registry;

import de.skyforce.main.elden.ashes.model.AshOfWar;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AshRegistry {

    private final Map<String, AshOfWar> ashesById = new HashMap<>();

    public void register(AshOfWar ash) {
        ashesById.put(ash.id().toLowerCase(), ash);
    }

    public Optional<AshOfWar> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ashesById.get(id.toLowerCase()));
    }

    public Map<String, AshOfWar> all() {
        return Map.copyOf(ashesById);
    }
}
