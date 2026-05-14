package de.skyforce.main.elden.spirit.registry;

import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import de.skyforce.main.elden.spirit.model.SpiritAshSummonType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class SpiritAshRegistry {

    private final Map<String, SpiritAshDefinition> spirits = new LinkedHashMap<>();

    public SpiritAshRegistry() {
        register(new SpiritAshDefinition(
                "lone_wolf_ashes",
                "Lone Wolf Ashes",
                Material.BONE,
                "Summons three agile spirit wolves that harry nearby foes and quickly change targets.",
                32.0D,
                20L * 20L,
                20L * 75L,
                "Limgrave",
                SpiritAshSummonType.WOLF_PACK
        ));
        register(new SpiritAshDefinition(
                "skeletal_militiaman_ashes",
                "Skeletal Militiaman Ashes",
                Material.BONE_MEAL,
                "Summons two stubborn skeletal militiamen that rush down hostile creatures with chipped blades.",
                44.0D,
                20L * 24L,
                20L * 85L,
                "Summonwater Village Outskirts",
                SpiritAshSummonType.SKELETAL_MILITIA
        ));
        register(new SpiritAshDefinition(
                "greatshield_soldier_ashes",
                "Greatshield Soldier Ashes",
                Material.SHIELD,
                "Summons a wall of shield-bearing spirits that hold aggro and pin enemies in place.",
                52.0D,
                20L * 28L,
                20L * 95L,
                "Nokron, Eternal City",
                SpiritAshSummonType.GREATSHIELD_PHALANX
        ));
        register(new SpiritAshDefinition(
                "black_knife_tiche",
                "Black Knife Tiche",
                Material.NETHERITE_SWORD,
                "Summons the legendary black knife assassin, a relentless spirit that darts through foes with destined death.",
                78.0D,
                20L * 40L,
                20L * 70L,
                "Moonlight Altar",
                SpiritAshSummonType.BLACK_KNIFE_ASSASSIN
        ));
    }

    public Optional<SpiritAshDefinition> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(spirits.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<SpiritAshDefinition> getAll() {
        return spirits.values();
    }

    private void register(SpiritAshDefinition spirit) {
        spirits.put(spirit.id().toLowerCase(Locale.ROOT), spirit);
    }
}
