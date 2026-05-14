package de.skyforce.main.elden.boss.registry;

import de.skyforce.main.elden.boss.model.BossArchetype;
import de.skyforce.main.elden.boss.model.BossDefinition;
import de.skyforce.main.elden.boss.model.BossRewardDefinition;
import de.skyforce.main.elden.boss.model.BossRewardType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;

public final class BossRegistry {

    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();

    public BossRegistry() {
        register(new BossDefinition(
                "tree_sentinel",
                "Tree Sentinel",
                BossArchetype.TREE_SENTINEL,
                new BossRewardDefinition(
                        BossRewardType.WEAPON,
                        "halberd",
                        "Halberd",
                        "remembrance_tree_sentinel",
                        "Remembrance of the Tree Sentinel",
                        BossRewardType.SPELL,
                        "barrier_of_gold",
                        "Barrier of Gold",
                        5000
                ),
                EntityType.VINDICATOR,
                320.0D,
                14.0D,
                0.33D,
                1800,
                42.0D,
                48.0D,
                20L * 25L,
                0.66D,
                0.33D,
                1.30D,
                1.15D,
                1.55D,
                1.28D,
                20L * 8L,
                Particle.WAX_OFF
        ));
        register(new BossDefinition(
                "night_cavalry",
                "Night's Cavalry",
                BossArchetype.NIGHT_CAVALRY,
                new BossRewardDefinition(
                        BossRewardType.ASH_OF_WAR,
                        "bloodhounds_step",
                        "Bloodhound's Step",
                        "remembrance_nights_cavalry",
                        "Remembrance of Night's Cavalry",
                        BossRewardType.SPELL,
                        "assassins_approach",
                        "Assassin's Approach",
                        4200
                ),
                EntityType.WITHER_SKELETON,
                280.0D,
                12.0D,
                0.36D,
                1500,
                40.0D,
                46.0D,
                20L * 22L,
                0.70D,
                0.30D,
                1.25D,
                1.18D,
                1.50D,
                1.26D,
                20L * 7L,
                Particle.SCULK_SOUL
        ));
    }

    public Optional<BossDefinition> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bosses.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<BossDefinition> getAll() {
        return bosses.values();
    }

    private void register(BossDefinition definition) {
        bosses.put(definition.id().toLowerCase(Locale.ROOT), definition);
    }
}
