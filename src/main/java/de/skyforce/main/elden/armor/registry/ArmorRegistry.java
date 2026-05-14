package de.skyforce.main.elden.armor.registry;

import de.skyforce.main.elden.armor.model.ArmorDefinition;
import de.skyforce.main.elden.armor.model.ArmorSlot;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;

public final class ArmorRegistry {

    private final Map<String, ArmorDefinition> armorById = new HashMap<>();

    public ArmorRegistry() {
        registerDefaults();
    }

    public void register(ArmorDefinition armor) {
        Objects.requireNonNull(armor, "armor");
        String normalizedId = armor.id().toLowerCase(Locale.ROOT);
        if (armorById.containsKey(normalizedId)) {
            throw new IllegalArgumentException("Armor id already registered: " + armor.id());
        }
        armorById.put(normalizedId, armor);
    }

    public Optional<ArmorDefinition> getById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(armorById.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<ArmorDefinition> all() {
        return Collections.unmodifiableCollection(armorById.values());
    }

    private void registerDefaults() {
        register(new ArmorDefinition("vagabond_helm", "Vagabond Knight Helm", ArmorSlot.HEAD,
                Material.IRON_HELMET, 5, 4, 4, 3, 3, 7, 4.8D));
        register(new ArmorDefinition("vagabond_armor", "Vagabond Knight Armor", ArmorSlot.CHEST,
                Material.IRON_CHESTPLATE, 14, 10, 11, 9, 9, 18, 10.6D));
        register(new ArmorDefinition("vagabond_greaves", "Vagabond Knight Greaves", ArmorSlot.LEGS,
                Material.IRON_LEGGINGS, 9, 7, 7, 6, 6, 12, 6.8D));
        register(new ArmorDefinition("vagabond_boots", "Vagabond Knight Boots", ArmorSlot.FEET,
                Material.IRON_BOOTS, 5, 4, 4, 3, 3, 7, 4.4D));
        register(new ArmorDefinition("vagabond_knight_gauntlets", "Vagabond Knight Gauntlets", ArmorSlot.FEET,
                Material.IRON_BOOTS, 5, 4, 4, 3, 3, 7, 4.0D));

        register(new ArmorDefinition("carian_helm", "Carian Knight Helm", ArmorSlot.HEAD,
                Material.DIAMOND_HELMET, 4, 5, 4, 4, 5, 6, 4.1D));
        register(new ArmorDefinition("carian_armor", "Carian Knight Armor", ArmorSlot.CHEST,
                Material.DIAMOND_CHESTPLATE, 11, 13, 11, 12, 13, 14, 9.6D));
        register(new ArmorDefinition("carian_greaves", "Carian Knight Greaves", ArmorSlot.LEGS,
                Material.DIAMOND_LEGGINGS, 7, 8, 7, 8, 8, 9, 6.1D));
        register(new ArmorDefinition("carian_boots", "Carian Knight Boots", ArmorSlot.FEET,
                Material.DIAMOND_BOOTS, 4, 5, 4, 4, 5, 5, 3.9D));

        register(new ArmorDefinition("bandit_mask", "Bandit Mask", ArmorSlot.HEAD,
                Material.LEATHER_HELMET, 2, 2, 2, 2, 1, 2, 1.4D));
        register(new ArmorDefinition("bandit_garb", "Bandit Garb", ArmorSlot.CHEST,
                Material.LEATHER_CHESTPLATE, 5, 5, 5, 5, 4, 4, 3.6D));
        register(new ArmorDefinition("bandit_trousers", "Bandit Trousers", ArmorSlot.LEGS,
                Material.LEATHER_LEGGINGS, 3, 3, 3, 3, 2, 3, 2.2D));
        register(new ArmorDefinition("bandit_boots", "Bandit Boots", ArmorSlot.FEET,
                Material.LEATHER_BOOTS, 2, 2, 2, 2, 1, 2, 1.5D));
        register(new ArmorDefinition("bandit_manchettes", "Bandit Manchettes", ArmorSlot.FEET,
                Material.LEATHER_BOOTS, 2, 2, 2, 2, 1, 2, 1.3D));

        register(new ArmorDefinition("astrologer_hood", "Astrologer Hood", ArmorSlot.HEAD,
                Material.LEATHER_HELMET, 2, 4, 2, 2, 3, 2, 1.7D));
        register(new ArmorDefinition("astrologer_robe", "Astrologer Robe", ArmorSlot.CHEST,
                Material.LEATHER_CHESTPLATE, 5, 8, 5, 5, 6, 4, 3.2D));
        register(new ArmorDefinition("astrologer_trousers", "Astrologer Trousers", ArmorSlot.LEGS,
                Material.LEATHER_LEGGINGS, 3, 5, 3, 3, 4, 3, 2.1D));
        register(new ArmorDefinition("astrologer_gloves", "Astrologer Gloves", ArmorSlot.FEET,
                Material.LEATHER_BOOTS, 2, 3, 2, 2, 3, 2, 1.2D));

        register(new ArmorDefinition("confessor_hood", "Confessor Hood", ArmorSlot.HEAD,
                Material.CHAINMAIL_HELMET, 4, 4, 3, 3, 4, 5, 2.8D));
        register(new ArmorDefinition("confessor_armor", "Confessor Armor", ArmorSlot.CHEST,
                Material.CHAINMAIL_CHESTPLATE, 10, 9, 8, 8, 9, 11, 8.6D));
        register(new ArmorDefinition("confessor_boots", "Confessor Boots", ArmorSlot.FEET,
                Material.CHAINMAIL_BOOTS, 4, 4, 3, 3, 4, 5, 2.9D));
        register(new ArmorDefinition("confessor_gloves", "Confessor Gloves", ArmorSlot.FEET,
                Material.CHAINMAIL_BOOTS, 4, 4, 3, 3, 4, 5, 2.4D));

        register(new ArmorDefinition("champion_headband", "Champion Headband", ArmorSlot.HEAD,
                Material.LEATHER_HELMET, 2, 1, 2, 2, 1, 2, 0.8D));
        register(new ArmorDefinition("champion_pauldron", "Champion Pauldron", ArmorSlot.CHEST,
                Material.LEATHER_CHESTPLATE, 8, 5, 8, 7, 5, 8, 5.7D));
        register(new ArmorDefinition("champion_gaiters", "Champion Gaiters", ArmorSlot.LEGS,
                Material.LEATHER_LEGGINGS, 5, 3, 5, 4, 3, 5, 3.5D));
        register(new ArmorDefinition("champion_bracers", "Champion Bracers", ArmorSlot.FEET,
                Material.LEATHER_BOOTS, 2, 2, 2, 2, 2, 2, 1.1D));

        register(new ArmorDefinition("prisoner_iron_mask", "Prisoner Iron Mask", ArmorSlot.HEAD,
                Material.IRON_HELMET, 4, 5, 3, 3, 4, 6, 4.0D));
        register(new ArmorDefinition("prisoner_clothing", "Prisoner Clothing", ArmorSlot.CHEST,
                Material.CHAINMAIL_CHESTPLATE, 8, 9, 6, 6, 7, 7, 5.1D));
        register(new ArmorDefinition("prisoner_trousers", "Prisoner Trousers", ArmorSlot.LEGS,
                Material.CHAINMAIL_LEGGINGS, 5, 6, 4, 4, 5, 5, 3.2D));

        register(new ArmorDefinition("prophet_blindfold", "Prophet Blindfold", ArmorSlot.HEAD,
                Material.LEATHER_HELMET, 1, 2, 1, 1, 2, 1, 0.7D));
        register(new ArmorDefinition("prophet_robe", "Prophet Robe", ArmorSlot.CHEST,
                Material.LEATHER_CHESTPLATE, 5, 6, 5, 4, 6, 4, 3.4D));
        register(new ArmorDefinition("prophet_trousers", "Prophet Trousers", ArmorSlot.LEGS,
                Material.LEATHER_LEGGINGS, 3, 4, 3, 3, 4, 3, 2.0D));

        register(new ArmorDefinition("land_of_reeds_helm", "Land of Reeds Helm", ArmorSlot.HEAD,
                Material.IRON_HELMET, 4, 3, 4, 3, 3, 5, 3.6D));
        register(new ArmorDefinition("land_of_reeds_armor", "Land of Reeds Armor", ArmorSlot.CHEST,
                Material.IRON_CHESTPLATE, 10, 7, 9, 8, 7, 11, 8.0D));
        register(new ArmorDefinition("land_of_reeds_greaves", "Land of Reeds Greaves", ArmorSlot.LEGS,
                Material.IRON_LEGGINGS, 6, 4, 6, 5, 4, 7, 4.8D));
        register(new ArmorDefinition("land_of_reeds_gauntlets", "Land of Reeds Gauntlets", ArmorSlot.FEET,
                Material.IRON_BOOTS, 4, 3, 4, 3, 3, 4, 2.4D));

        register(new ArmorDefinition("blue_cloth_cowl", "Blue Cloth Cowl", ArmorSlot.HEAD,
                Material.LEATHER_HELMET, 2, 2, 2, 1, 1, 2, 1.2D));
        register(new ArmorDefinition("blue_cloth_vest", "Blue Cloth Vest", ArmorSlot.CHEST,
                Material.LEATHER_CHESTPLATE, 5, 4, 4, 3, 3, 4, 3.0D));
        register(new ArmorDefinition("warrior_greaves", "Warrior Greaves", ArmorSlot.LEGS,
                Material.LEATHER_LEGGINGS, 3, 3, 3, 2, 2, 3, 1.9D));
        register(new ArmorDefinition("warrior_gauntlets", "Warrior Gauntlets", ArmorSlot.FEET,
                Material.LEATHER_BOOTS, 2, 2, 2, 2, 2, 2, 1.0D));
    }
}
