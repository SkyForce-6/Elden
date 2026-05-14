package de.skyforce.main.elden.equipment;

import de.skyforce.main.elden.armor.model.ArmorDefinition;
import de.skyforce.main.elden.armor.registry.ArmorRegistry;
import de.skyforce.main.elden.armor.service.ArmorItemFactory;
import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.Locale;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public final class EquipmentWeightService {

    private static final double BASE_MOVEMENT_SPEED = 0.1D;
    private static final double MAX_MOVEMENT_SPEED = 0.15D;

    private final LevelManager levelManager;
    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final ArmorRegistry armorRegistry;
    private final ArmorItemFactory armorItemFactory;
    private Function<Player, Double> equipLoadMultiplierProvider = player -> 1.0D;

    public EquipmentWeightService(LevelManager levelManager, WeaponRegistry weaponRegistry,
                                  WeaponItemFactory weaponItemFactory, ArmorRegistry armorRegistry,
                                  ArmorItemFactory armorItemFactory) {
        this.levelManager = levelManager;
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.armorRegistry = armorRegistry;
        this.armorItemFactory = armorItemFactory;
    }

    public EquipmentWeightSnapshot snapshot(Player player) {
        double maxLoad = Math.max(1.0D, levelManager.getDerivedEquipLoad(player)
                * Math.max(0.0D, equipLoadMultiplierProvider.apply(player)));
        double currentLoad = calculateCurrentLoad(player);
        double loadRatio = currentLoad / maxLoad;
        return new EquipmentWeightSnapshot(currentLoad, maxLoad, loadRatio, resolveTier(loadRatio));
    }

    public void setEquipLoadMultiplierProvider(Function<Player, Double> equipLoadMultiplierProvider) {
        this.equipLoadMultiplierProvider = equipLoadMultiplierProvider == null ? player -> 1.0D : equipLoadMultiplierProvider;
    }

    public void applyMovementSpeed(Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        int endurance = levelManager.getOrCreate(player).attribute(AttributeType.ENDURANCE);
        double enduranceBonus = Math.max(0, endurance - 10) * 0.0015D;
        double baseSpeed = Math.min(MAX_MOVEMENT_SPEED, BASE_MOVEMENT_SPEED + enduranceBonus);
        movementSpeed.setBaseValue(baseSpeed * movementSpeedMultiplier(snapshot(player).tier()));
    }

    public double dodgeHorizontalMultiplier(WeightTier tier) {
        return switch (tier) {
            case LIGHT -> 1.12D;
            case MEDIUM -> 1.0D;
            case HEAVY -> 0.82D;
            case OVERLOADED -> 0.0D;
        };
    }

    public double dodgeVerticalMultiplier(WeightTier tier) {
        return switch (tier) {
            case LIGHT -> 1.08D;
            case MEDIUM -> 1.0D;
            case HEAVY -> 0.9D;
            case OVERLOADED -> 0.0D;
        };
    }

    public double dodgeCostMultiplier(WeightTier tier) {
        return switch (tier) {
            case LIGHT -> 0.9D;
            case MEDIUM -> 1.0D;
            case HEAVY -> 1.18D;
            case OVERLOADED -> 1.0D;
        };
    }

    public int dodgeIframeBonusTicks(WeightTier tier) {
        return switch (tier) {
            case LIGHT -> 2;
            case MEDIUM -> 0;
            case HEAVY -> -2;
            case OVERLOADED -> -8;
        };
    }

    public int dodgeCooldownBonusTicks(WeightTier tier) {
        return switch (tier) {
            case LIGHT -> -2;
            case MEDIUM -> 0;
            case HEAVY -> 4;
            case OVERLOADED -> 20;
        };
    }

    private double calculateCurrentLoad(Player player) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) {
            return 0.0D;
        }

        double total = 0.0D;
        total += itemWeight(equipment.getItemInMainHand());
        total += itemWeight(equipment.getItemInOffHand());
        total += armorWeight(equipment.getHelmet());
        total += armorWeight(equipment.getChestplate());
        total += armorWeight(equipment.getLeggings());
        total += armorWeight(equipment.getBoots());
        return total;
    }

    private double itemWeight(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0.0D;
        }

        String weaponId = weaponItemFactory.getWeaponId(item);
        if (weaponId != null) {
            WeaponDefinition weapon = weaponRegistry.getById(weaponId).orElse(null);
            if (weapon != null) {
                return weapon.weight();
            }
        }

        return vanillaWeaponWeight(item.getType());
    }

    private double armorWeight(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0.0D;
        }

        String armorId = armorItemFactory.getArmorId(item);
        if (armorId != null) {
            ArmorDefinition armor = armorRegistry.getById(armorId).orElse(null);
            if (armor != null) {
                return armor.weight();
            }
        }

        return vanillaArmorWeight(item.getType());
    }

    private WeightTier resolveTier(double ratio) {
        if (ratio < 0.30D) {
            return WeightTier.LIGHT;
        }
        if (ratio < 0.70D) {
            return WeightTier.MEDIUM;
        }
        if (ratio < 1.00D) {
            return WeightTier.HEAVY;
        }
        return WeightTier.OVERLOADED;
    }

    private double movementSpeedMultiplier(WeightTier tier) {
        return switch (tier) {
            case LIGHT -> 1.05D;
            case MEDIUM -> 1.0D;
            case HEAVY -> 0.86D;
            case OVERLOADED -> 0.72D;
        };
    }

    private double vanillaArmorWeight(Material material) {
        String name = material.name().toUpperCase(Locale.ROOT);
        if (name.equals("ELYTRA")) {
            return 6.0D;
        }
        if (name.equals("TURTLE_HELMET")) {
            return 4.0D;
        }

        double materialBase;
        if (name.startsWith("LEATHER_")) {
            materialBase = 1.0D;
        } else if (name.startsWith("CHAINMAIL_")) {
            materialBase = 1.8D;
        } else if (name.startsWith("GOLDEN_")) {
            materialBase = 2.0D;
        } else if (name.startsWith("IRON_")) {
            materialBase = 2.4D;
        } else if (name.startsWith("DIAMOND_")) {
            materialBase = 2.9D;
        } else if (name.startsWith("NETHERITE_")) {
            materialBase = 3.5D;
        } else {
            return 0.0D;
        }

        return materialBase * armorSlotMultiplier(name);
    }

    private double armorSlotMultiplier(String name) {
        if (name.endsWith("_HELMET")) {
            return 1.0D;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return 1.8D;
        }
        if (name.endsWith("_LEGGINGS")) {
            return 1.5D;
        }
        if (name.endsWith("_BOOTS")) {
            return 0.9D;
        }
        return 1.0D;
    }

    private double vanillaWeaponWeight(Material material) {
        String name = material.name().toUpperCase(Locale.ROOT);
        if (name.endsWith("_SWORD")) {
            return 3.5D;
        }
        if (name.endsWith("_AXE")) {
            return 6.5D;
        }
        if (name.endsWith("_PICKAXE")) {
            return 5.5D;
        }
        if (name.endsWith("_SHOVEL")) {
            return 3.0D;
        }
        if (name.endsWith("_HOE")) {
            return 2.5D;
        }
        if (name.endsWith("_MACE")) {
            return 7.5D;
        }
        if (name.endsWith("_BOW") || name.endsWith("_CROSSBOW")) {
            return 4.0D;
        }
        if (name.equals("TRIDENT")) {
            return 8.0D;
        }
        if (name.contains("SHIELD")) {
            return 5.5D;
        }
        if (name.equals("TOTEM_OF_UNDYING")) {
            return 1.5D;
        }
        return 0.0D;
    }
}
