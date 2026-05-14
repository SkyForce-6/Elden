package de.skyforce.main.elden.talisman.registry;

import de.skyforce.main.elden.talisman.model.TalismanDefinition;
import de.skyforce.main.elden.talisman.model.TalismanEffectType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class TalismanRegistry {

    private final Map<String, TalismanDefinition> talismansById = new LinkedHashMap<>();

    public TalismanRegistry() {
        registerDefaults();
    }

    public Optional<TalismanDefinition> getById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(talismansById.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<TalismanDefinition> all() {
        return Collections.unmodifiableCollection(talismansById.values());
    }

    private void register(TalismanDefinition talisman) {
        talismansById.put(talisman.id().toLowerCase(Locale.ROOT), talisman);
    }

    private void registerDefaults() {
        register(new TalismanDefinition(
                "crimson_amber_medallion",
                "Crimson Amber Medallion",
                Material.REDSTONE,
                "Raises maximum HP.",
                TalismanEffectType.MAX_HEALTH,
                0.06D
        ));
        register(new TalismanDefinition(
                "viridian_amber_medallion",
                "Viridian Amber Medallion",
                Material.EMERALD,
                "Raises maximum stamina.",
                TalismanEffectType.MAX_STAMINA,
                0.08D
        ));
        register(new TalismanDefinition(
                "cerulean_amber_medallion",
                "Cerulean Amber Medallion",
                Material.LAPIS_LAZULI,
                "Raises maximum FP.",
                TalismanEffectType.MAX_FOCUS,
                0.08D
        ));
        register(new TalismanDefinition(
                "erdtrees_favor",
                "Erdtree's Favor",
                Material.GOLD_NUGGET,
                "Raises HP, stamina, and equip load slightly.",
                TalismanEffectType.EQUIP_LOAD,
                0.05D
        ));
        register(new TalismanDefinition(
                "green_turtle_talisman",
                "Green Turtle Talisman",
                Material.TURTLE_HELMET,
                "Raises stamina recovery speed.",
                TalismanEffectType.STAMINA_REGEN,
                0.17D
        ));
        register(new TalismanDefinition(
                "dragoncrest_shield_talisman",
                "Dragoncrest Shield Talisman",
                Material.SHIELD,
                "Reduces physical damage taken.",
                TalismanEffectType.PHYSICAL_DEFENSE,
                0.08D
        ));
        register(new TalismanDefinition(
                "claw_talisman",
                "Claw Talisman",
                Material.FEATHER,
                "Enhances jump attacks.",
                TalismanEffectType.JUMP_ATTACK_DAMAGE,
                0.15D
        ));
        register(new TalismanDefinition(
                "gold_scarab",
                "Gold Scarab",
                Material.GOLD_INGOT,
                "Increases runes obtained from defeated enemies and bosses.",
                TalismanEffectType.RUNE_GAIN,
                0.20D
        ));
        register(new TalismanDefinition(
                "radagon_icon",
                "Radagon Icon",
                Material.CLOCK,
                "Shortens spell and skill casting time.",
                TalismanEffectType.CAST_SPEED,
                0.12D
        ));
        register(new TalismanDefinition(
                "silver_tear_mask",
                "Silver Tear Mask",
                Material.IRON_NUGGET,
                "Improves status buildup.",
                TalismanEffectType.STATUS_BUILDUP,
                0.10D
        ));
    }
}
