package de.skyforce.main.elden.smithing.service;

import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.smithing.model.SmithingRequirement;
import de.skyforce.main.elden.smithing.model.SmithingTrack;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmithingService {

    private static final int[] STANDARD_RUNE_COSTS = {
            200, 400, 700, 1000, 1400, 1800, 2300, 2900, 3600, 4500,
            5600, 6800, 8200, 9800, 11600, 13600, 15800, 18200, 20800, 23600,
            26600, 29800, 33200, 36800, 42000
    };
    private static final int[] SOMBER_RUNE_COSTS = {
            800, 1500, 2400, 3600, 5200, 7400, 10200, 13800, 18400, 24000
    };

    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final RuneManager runeManager;
    private final SmithingStoneService smithingStoneService;

    public SmithingService(JavaPlugin plugin,
                           WeaponRegistry weaponRegistry,
                           WeaponItemFactory weaponItemFactory,
                           RuneManager runeManager,
                           SmithingStoneService smithingStoneService) {
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.runeManager = runeManager;
        this.smithingStoneService = smithingStoneService;
    }

    public String applyUpgradeToMainHand(Player player, int smithingLevel) {
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponDefinition weapon = resolveWeapon(item);
        if (weapon == null) {
            return "You must hold a custom weapon in your main hand.";
        }

        SmithingTrack track = trackFor(weapon);
        if (smithingLevel < 0 || smithingLevel > track.maxLevel()) {
            return "Smithing level must be between 0 and " + track.maxLevel() + " for " + track.displayName() + " weapons.";
        }

        weaponItemFactory.updateWeaponItem(item, weapon, smithingLevel);
        return null;
    }

    public SmithingWeaponInfo inspectMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponDefinition weapon = resolveWeapon(item);
        if (weapon == null) {
            return null;
        }
        SmithingTrack track = trackFor(weapon);
        int level = clampLevel(track, weaponItemFactory.getSmithingLevel(item));
        SmithingRequirement next = nextRequirement(weapon, level);
        return new SmithingWeaponInfo(weapon.displayName(), track, level, next);
    }

    public SmithingUpgradeResult upgradeMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponDefinition weapon = resolveWeapon(item);
        if (weapon == null) {
            return SmithingUpgradeResult.error("Hold a custom weapon in your main hand.");
        }

        SmithingTrack track = trackFor(weapon);
        int currentLevel = clampLevel(track, weaponItemFactory.getSmithingLevel(item));
        SmithingRequirement next = nextRequirement(weapon, currentLevel);
        if (next == null) {
            return SmithingUpgradeResult.error("This weapon is already at its maximum smithing level.");
        }

        int runes = runeManager.getRunes(player);
        if (runes < next.runeCost()) {
            return SmithingUpgradeResult.error("You need " + next.runeCost() + " runes.");
        }

        int stones = smithingStoneService.countMatching(player, next.track(), next.stoneTier());
        if (stones < next.stoneAmount()) {
            return SmithingUpgradeResult.error("You need " + next.stoneAmount() + "x "
                    + smithingStoneService.displayName(next.track(), next.stoneTier()) + ".");
        }

        if (!smithingStoneService.removeMatching(player, next.track(), next.stoneTier(), next.stoneAmount())) {
            return SmithingUpgradeResult.error("The required stones could not be consumed.");
        }
        if (!runeManager.spendRunes(player.getUniqueId(), next.runeCost())) {
            return SmithingUpgradeResult.error("You do not have enough runes.");
        }

        weaponItemFactory.updateWeaponItem(item, weapon, next.targetLevel());
        return SmithingUpgradeResult.success(track, next.targetLevel(), next.runeCost(), next.stoneTier(), next.stoneAmount());
    }

    public SmithingRequirement nextRequirement(WeaponDefinition weapon, int currentLevel) {
        SmithingTrack track = trackFor(weapon);
        int clampedLevel = clampLevel(track, currentLevel);
        int nextLevel = clampedLevel + 1;
        if (nextLevel > track.maxLevel()) {
            return null;
        }

        if (track == SmithingTrack.STANDARD) {
            int stoneTier = nextLevel == 25 ? 9 : ((nextLevel - 1) / 3) + 1;
            int stoneAmount = nextLevel == 25 ? 1 : switch ((nextLevel - 1) % 3) {
                case 0 -> 2;
                case 1 -> 4;
                default -> 6;
            };
            return new SmithingRequirement(track, nextLevel, STANDARD_RUNE_COSTS[nextLevel - 1], stoneTier, stoneAmount);
        }

        int stoneTier = nextLevel;
        return new SmithingRequirement(track, nextLevel, SOMBER_RUNE_COSTS[nextLevel - 1], stoneTier, 1);
    }

    public double damageMultiplier(WeaponDefinition weapon, int smithingLevel) {
        SmithingTrack track = trackFor(weapon);
        int clampedLevel = clampLevel(track, smithingLevel);
        return 1.0 + (clampedLevel * track.damageBonusPerLevel());
    }

    public SmithingTrack trackFor(WeaponDefinition weapon) {
        return weapon.smithingTrack();
    }

    public Optional<WeaponDefinition> getEquippedWeapon(Player player) {
        return Optional.ofNullable(resolveWeapon(player.getInventory().getItemInMainHand()));
    }

    private int clampLevel(SmithingTrack track, int level) {
        return Math.max(0, Math.min(level, track.maxLevel()));
    }

    private WeaponDefinition resolveWeapon(ItemStack item) {
        String weaponId = weaponItemFactory.getWeaponId(item);
        return weaponRegistry.getById(weaponId).orElse(null);
    }

    public record SmithingWeaponInfo(
            String displayName,
            SmithingTrack track,
            int smithingLevel,
            SmithingRequirement nextRequirement
    ) {
    }

    public record SmithingUpgradeResult(
            boolean success,
            String message,
            SmithingTrack track,
            int newLevel,
            int runeCost,
            int stoneTier,
            int stoneAmount
    ) {
        public static SmithingUpgradeResult success(SmithingTrack track, int newLevel, int runeCost, int stoneTier, int stoneAmount) {
            return new SmithingUpgradeResult(true, null, track, newLevel, runeCost, stoneTier, stoneAmount);
        }

        public static SmithingUpgradeResult error(String message) {
            return new SmithingUpgradeResult(false, message, null, 0, 0, 0, 0);
        }
    }
}
