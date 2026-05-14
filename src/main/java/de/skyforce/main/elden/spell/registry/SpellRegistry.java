package de.skyforce.main.elden.spell.registry;

import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.spell.model.SpellDefinition;
import de.skyforce.main.elden.spell.model.SpellSchool;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class SpellRegistry {

    private final Map<String, SpellDefinition> spells = new LinkedHashMap<>();

    public SpellRegistry() {
        register(new SpellDefinition("glintstone_pebble", "Glintstone Pebble", SpellSchool.SORCERY, Material.LAPIS_LAZULI,
                "Fires a small shard of glintstone at the targeted enemy.", 9.0D, 16L, 12L,
                AttributeType.INTELLIGENCE, AttributeType.INTELLIGENCE, 12, null, 0, "Academy of Raya Lucaria"));
        register(new SpellDefinition("glintstone_arc", "Glintstone Arc", SpellSchool.SORCERY, Material.PRISMARINE_SHARD,
                "Sweeps a wide arc of glintstone through enemies in front of the caster.", 10.0D, 18L, 14L,
                AttributeType.INTELLIGENCE, AttributeType.INTELLIGENCE, 13, null, 0, "Academy of Raya Lucaria"));
        register(new SpellDefinition("carian_slicer", "Carian Slicer", SpellSchool.SORCERY, Material.DIAMOND_SWORD,
                "Conjures a swift magical blade for a short-range slash.", 11.0D, 12L, 8L,
                AttributeType.INTELLIGENCE, AttributeType.INTELLIGENCE, 16, null, 0, "Carian"));
        register(new SpellDefinition("magic_glintblade", "Magic Glintblade", SpellSchool.SORCERY, Material.AMETHYST_SHARD,
                "Forms a delayed glintblade that launches at the targeted enemy after a short pause.", 12.0D, 20L, 16L,
                AttributeType.INTELLIGENCE, AttributeType.INTELLIGENCE, 14, null, 0, "Carian"));
        register(new SpellDefinition("urgent_heal", "Urgent Heal", SpellSchool.INCANTATION, Material.GHAST_TEAR,
                "Channels restorative light to recover your health.", 14.0D, 24L, 18L,
                AttributeType.FAITH, AttributeType.FAITH, 14, null, 0, "Two Fingers"));
        register(new SpellDefinition("heal", "Heal", SpellSchool.INCANTATION, Material.GOLDEN_APPLE,
                "Channels a broader restorative incantation to recover more health.", 20.0D, 30L, 22L,
                AttributeType.FAITH, AttributeType.FAITH, 12, null, 0, "Two Fingers"));
        register(new SpellDefinition("catch_flame", "Catch Flame", SpellSchool.INCANTATION, Material.BLAZE_POWDER,
                "Ignites a burst of flame in front of the caster at very close range.", 10.0D, 10L, 6L,
                AttributeType.FAITH, AttributeType.FAITH, 8, null, 0, "Fire Monks"));
        register(new SpellDefinition("lightning_spear", "Lightning Spear", SpellSchool.INCANTATION, Material.LIGHTNING_ROD,
                "Hurls a focused bolt of lightning at a distant foe.", 18.0D, 26L, 20L,
                AttributeType.FAITH, AttributeType.FAITH, 18, null, 0, "Dragon Cult"));

        register(new SpellDefinition("agheels_flame", "Agheel's Flame", SpellSchool.INCANTATION, Material.FIRE_CHARGE,
                "Spews Agheel's burning breath from above onto enemies in front of the caster.", 36.0D, 48L, 28L,
                AttributeType.FAITH, AttributeType.FAITH, 23, AttributeType.ARCANE, 15, "Dragon Communion"));
        register(new SpellDefinition("ancient_dragons_lightning_spear", "Ancient Dragons' Lightning Spear", SpellSchool.INCANTATION, Material.LIGHTNING_ROD,
                "Calls down a red lightning spear from above onto a distant target.", 30.0D, 40L, 24L,
                AttributeType.FAITH, AttributeType.FAITH, 32, null, 0, "Dragon Cult"));
        register(new SpellDefinition("ancient_dragons_lightning_strike", "Ancient Dragons' Lightning Strike", SpellSchool.INCANTATION, Material.YELLOW_DYE,
                "Summons red lightning that bursts outward from the impact point.", 36.0D, 50L, 28L,
                AttributeType.FAITH, AttributeType.FAITH, 26, null, 0, "Dragon Cult"));
        register(new SpellDefinition("aspect_of_the_crucible_breath", "Aspect of the Crucible: Breath", SpellSchool.INCANTATION, Material.BLAZE_POWDER,
                "Creates a crucible throat and spews fire while advancing.", 28.0D, 42L, 24L,
                AttributeType.FAITH, AttributeType.FAITH, 27, null, 0, "Erdtree"));
        register(new SpellDefinition("aspect_of_the_crucible_horns", "Aspect of the Crucible: Horns", SpellSchool.INCANTATION, Material.GOAT_HORN,
                "Forms great horns and rushes forward to gore enemies.", 20.0D, 26L, 14L,
                AttributeType.FAITH, AttributeType.FAITH, 27, null, 0, "Erdtree"));
        register(new SpellDefinition("aspects_of_the_crucible_tail", "Aspects of the Crucible: Tail", SpellSchool.INCANTATION, Material.LEATHER,
                "Manifests a tail to sweep a wide area around the caster.", 22.0D, 28L, 16L,
                AttributeType.FAITH, AttributeType.FAITH, 27, null, 0, "Erdtree"));
        register(new SpellDefinition("assassins_approach", "Assassin's Approach", SpellSchool.INCANTATION, Material.FEATHER,
                "Silences movement and softens falls for careful approach.", 15.0D, 30L, 12L,
                AttributeType.FAITH, AttributeType.FAITH, 10, null, 0, "Two Fingers"));
        register(new SpellDefinition("barrier_of_gold", "Barrier of Gold", SpellSchool.INCANTATION, Material.GOLD_INGOT,
                "Greatly increases magic resistance for the caster and nearby allies.", 30.0D, 44L, 20L,
                AttributeType.FAITH, AttributeType.FAITH, 24, null, 0, "Erdtree"));
        register(new SpellDefinition("beast_claw", "Beast Claw", SpellSchool.INCANTATION, Material.IRON_SHOVEL,
                "Rips claws through the earth in a line toward enemies ahead.", 10.0D, 18L, 10L,
                AttributeType.FAITH, AttributeType.FAITH, 8, null, 0, "Bestial"));
        register(new SpellDefinition("bestial_constitution", "Bestial Constitution", SpellSchool.INCANTATION, Material.COOKED_BEEF,
                "Steadies the body against blood loss and frostbite afflictions.", 10.0D, 24L, 10L,
                AttributeType.FAITH, AttributeType.FAITH, 9, null, 0, "Bestial"));
        register(new SpellDefinition("bestial_sling", "Bestial Sling", SpellSchool.INCANTATION, Material.COBBLESTONE,
                "Swiftly hurls a burst of jagged stone shards.", 7.0D, 12L, 6L,
                AttributeType.FAITH, AttributeType.FAITH, 10, null, 0, "Bestial"));
    }

    public Optional<SpellDefinition> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(spells.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<SpellDefinition> getAll() {
        return spells.values();
    }

    private void register(SpellDefinition spell) {
        spells.put(spell.id().toLowerCase(Locale.ROOT), spell);
    }
}
