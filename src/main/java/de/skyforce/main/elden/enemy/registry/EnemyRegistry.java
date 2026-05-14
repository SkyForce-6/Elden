package de.skyforce.main.elden.enemy.registry;

import de.skyforce.main.elden.enemy.model.EnemyArchetype;
import de.skyforce.main.elden.enemy.model.EnemyDefinition;
import de.skyforce.main.elden.enemy.model.EnemyRewardDefinition;
import de.skyforce.main.elden.enemy.model.EnemyRewardType;
import de.skyforce.main.elden.smithing.model.SmithingTrack;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public final class EnemyRegistry {

    private final Map<String, EnemyDefinition> enemiesById = new LinkedHashMap<>();

    public EnemyRegistry() {
        registerDefaults();
    }

    public Optional<EnemyDefinition> getById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(enemiesById.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<EnemyDefinition> getAll() {
        return Collections.unmodifiableCollection(enemiesById.values());
    }

    public void register(EnemyDefinition definition) {
        enemiesById.put(definition.id().toLowerCase(Locale.ROOT), definition);
    }

    private void registerDefaults() {
        register(new EnemyDefinition(
                "wandering_noble",
                "Wandering Noble",
                EntityType.ZOMBIE,
                EnemyArchetype.MELEE,
                26.0D,
                4.0D,
                0.24D,
                24.0D,
                45,
                20L * 20L,
                26.0D,
                false,
                null,
                Material.WOODEN_SWORD,
                null,
                Material.LEATHER_CHESTPLATE,
                null,
                null,
                false
        ));
        register(new EnemyDefinition(
                "godrick_soldier",
                "Godrick Soldier",
                EntityType.ZOMBIE,
                EnemyArchetype.MELEE,
                36.0D,
                6.0D,
                0.27D,
                28.0D,
                90,
                20L * 30L,
                30.0D,
                false,
                null,
                Material.STONE_SWORD,
                Material.CHAINMAIL_HELMET,
                Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS,
                Material.CHAINMAIL_BOOTS,
                false
        ));
        register(new EnemyDefinition(
                "godrick_guard",
                "Godrick Guard",
                EntityType.ZOMBIE,
                EnemyArchetype.SHIELD,
                44.0D,
                6.0D,
                0.24D,
                28.0D,
                120,
                20L * 35L,
                30.0D,
                false,
                new EnemyRewardDefinition(EnemyRewardType.SMITHING_STONE, SmithingTrack.STANDARD, 1, 1, "Smithing Stone [1]"),
                Material.IRON_SWORD,
                Material.IRON_HELMET,
                Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS,
                Material.IRON_BOOTS,
                false
        ));
        register(new EnemyDefinition(
                "grave_rogue",
                "Grave Rogue",
                EntityType.HUSK,
                EnemyArchetype.FAST,
                30.0D,
                5.0D,
                0.31D,
                30.0D,
                110,
                20L * 24L,
                34.0D,
                false,
                null,
                Material.IRON_SWORD,
                null,
                Material.LEATHER_CHESTPLATE,
                null,
                Material.LEATHER_BOOTS,
                false
        ));
        register(new EnemyDefinition(
                "banished_archer",
                "Banished Archer",
                EntityType.SKELETON,
                EnemyArchetype.RANGED,
                32.0D,
                6.0D,
                0.27D,
                34.0D,
                125,
                20L * 32L,
                34.0D,
                false,
                new EnemyRewardDefinition(EnemyRewardType.SMITHING_STONE, SmithingTrack.SOMBER, 1, 1, "Somber Smithing Stone [1]"),
                Material.BOW,
                Material.CHAINMAIL_HELMET,
                Material.LEATHER_CHESTPLATE,
                null,
                null,
                false
        ));
        register(new EnemyDefinition(
                "banished_knight",
                "Banished Knight",
                EntityType.WITHER_SKELETON,
                EnemyArchetype.ELITE,
                68.0D,
                9.0D,
                0.29D,
                34.0D,
                260,
                20L * 45L,
                36.0D,
                true,
                new EnemyRewardDefinition(EnemyRewardType.SMITHING_STONE, SmithingTrack.STANDARD, 3, 1, "Smithing Stone [3]"),
                Material.IRON_SWORD,
                Material.IRON_HELMET,
                Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS,
                Material.IRON_BOOTS,
                false
        ));
    }
}
