package de.skyforce.main.elden.weapon.service;

import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.smithing.service.SmithingService;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.model.WeaponRequirements;
import de.skyforce.main.elden.weapon.model.WeaponScaling;
import de.skyforce.main.elden.weapon.model.WeaponScalingGrade;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Computes final Minecraft attack damage for custom Elden weapons.
 *
 * <p>Elden Ring AR is converted to Minecraft damage using a configurable scale:
 * {@code minecraftDamage = (totalBaseDamage / AR_SCALE) * statMultiplier * requirementFactor}
 *
 * <p>Default AR_SCALE of 20.0 maps a 100 AR weapon to 5.0 Minecraft damage -
 * roughly equivalent to an Iron Sword.
 */
public final class WeaponDamageService {

    /** 40 % penalty when attribute requirements are not met (like Elden Ring). */
    private static final double REQUIREMENT_PENALTY = 0.40;

    /**
     * Divisor to convert Elden Ring AR to Minecraft damage.
     * 100 AR -> 5.0 MC damage, 102 AR (Short Sword) -> ~5.1 MC damage.
     */
    private static final double AR_SCALE = 20.0;

    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final SmithingService smithingService;

    public WeaponDamageService(WeaponRegistry weaponRegistry,
                               WeaponItemFactory weaponItemFactory,
                               SmithingService smithingService) {
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.smithingService = smithingService;
    }

    /** Returns the weapon definition the player currently holds, if it is a custom weapon. */
    public Optional<WeaponDefinition> getEquippedWeapon(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        String id = weaponItemFactory.getWeaponId(held);
        return weaponRegistry.getById(id);
    }

    /**
     * Computes final Minecraft damage for the equipped custom weapon.
     *
     * @return calculated damage value, or {@code -1.0} if the player holds no custom weapon.
     */
    public double computeFinalDamage(Player player, LevelManager levelManager) {
        return getEquippedWeapon(player)
                .map(weapon -> computeWeaponDamage(player, weapon, levelManager))
                .orElse(-1.0);
    }

    private double computeWeaponDamage(Player player, WeaponDefinition weapon, LevelManager levelManager) {
        double scaledBase = weapon.attackStats().totalBaseDamage() / AR_SCALE;
        double statMultiplier = computeStatMultiplier(player, weapon, levelManager);
        double requirementFactor = meetsRequirements(player, weapon, levelManager) ? 1.0 : REQUIREMENT_PENALTY;
        int smithingLevel = weaponItemFactory.getSmithingLevel(player.getInventory().getItemInMainHand());
        double smithingMultiplier = smithingService.damageMultiplier(weapon, smithingLevel);
        return scaledBase * statMultiplier * smithingMultiplier * requirementFactor;
    }

    /**
     * Blends the attribute bonuses from all scaling stats present on the weapon.
     * Each stat's bonus is weighted by its scaling grade factor and then
     * averaged over the total grade weight to avoid exponential stacking when
     * multiple stats scale the weapon.
     */
    private double computeStatMultiplier(Player player, WeaponDefinition weapon, LevelManager levelManager) {
        WeaponScaling scaling = weapon.scaling();

        double weightedBonus = 0.0;
        double totalWeight = 0.0;

        if (scaling.strength() != WeaponScalingGrade.NONE) {
            double bonus = levelManager.getStrengthAttackMultiplier(player, false, false) - 1.0;
            weightedBonus += scaling.strength().factor() * bonus;
            totalWeight += scaling.strength().factor();
        }

        if (scaling.dexterity() != WeaponScalingGrade.NONE) {
            double bonus = levelManager.getDexterityAttackMultiplier(player) - 1.0;
            weightedBonus += scaling.dexterity().factor() * bonus;
            totalWeight += scaling.dexterity().factor();
        }

        if (scaling.intelligence() != WeaponScalingGrade.NONE) {
            double bonus = levelManager.getIntelligenceAttackMultiplier(player) - 1.0;
            weightedBonus += scaling.intelligence().factor() * bonus;
            totalWeight += scaling.intelligence().factor();
        }

        if (scaling.faith() != WeaponScalingGrade.NONE) {
            double bonus = levelManager.getFaithAttackMultiplier(player) - 1.0;
            weightedBonus += scaling.faith().factor() * bonus;
            totalWeight += scaling.faith().factor();
        }

        if (scaling.arcane() != WeaponScalingGrade.NONE) {
            double bonus = levelManager.getArcaneAttackMultiplier(player) - 1.0;
            weightedBonus += scaling.arcane().factor() * bonus;
            totalWeight += scaling.arcane().factor();
        }

        double normalizedBonus = totalWeight > 0.0 ? weightedBonus / totalWeight : 0.0;
        return 1.0 + normalizedBonus;
    }

    /** Returns {@code false} when any attribute requirement of the weapon is not met. */
    private boolean meetsRequirements(Player player, WeaponDefinition weapon, LevelManager levelManager) {
        WeaponRequirements requirements = weapon.requirements();
        var progress = levelManager.getOrCreate(player);

        if (requirements.strength() > 0 && progress.attribute(AttributeType.STRENGTH) < requirements.strength()) {
            return false;
        }
        if (requirements.dexterity() > 0 && progress.attribute(AttributeType.DEXTERITY) < requirements.dexterity()) {
            return false;
        }
        if (requirements.intelligence() > 0 && progress.attribute(AttributeType.INTELLIGENCE) < requirements.intelligence()) {
            return false;
        }
        if (requirements.faith() > 0 && progress.attribute(AttributeType.FAITH) < requirements.faith()) {
            return false;
        }
        if (requirements.arcane() > 0 && progress.attribute(AttributeType.ARCANE) < requirements.arcane()) {
            return false;
        }

        return true;
    }
}
