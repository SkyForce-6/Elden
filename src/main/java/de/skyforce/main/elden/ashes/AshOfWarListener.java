package de.skyforce.main.elden.ashes;

import de.skyforce.main.elden.ashes.gui.AshApplyMenu;
import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarBindingService;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class AshOfWarListener implements Listener {

    private final JavaPlugin plugin;
    private final AshOfWarRegistry ashRegistry;
    private final AshOfWarItemFactory ashItemFactory;
    private final AshOfWarBindingService bindingService;
    private final FocusManager focusManager;
    private final AshApplyMenu ashApplyMenu;
    private final Map<UUID, Map<String, Long>> cooldownUntilByPlayer = new HashMap<>();

    public AshOfWarListener(JavaPlugin plugin, AshOfWarRegistry ashRegistry, AshOfWarItemFactory ashItemFactory,
                            AshOfWarBindingService bindingService, FocusManager focusManager,
                            WeaponRegistry weaponRegistry, WeaponItemFactory weaponItemFactory,
                            AshApplyMenu ashApplyMenu) {
        this.plugin = plugin;
        this.ashRegistry = ashRegistry;
        this.ashItemFactory = ashItemFactory;
        this.bindingService = bindingService;
        this.focusManager = focusManager;
        this.ashApplyMenu = ashApplyMenu;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        if (ashItemFactory.isAshOfWar(item)) {
            event.setCancelled(true);
            ashApplyMenu.open(event.getPlayer(), event.getHand(), item);
            return;
        }

        handleBoundAshSkill(event, item);
    }

    private void handleBoundAshSkill(PlayerInteractEvent event, ItemStack weapon) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        String ashId = bindingService.getBoundAshId(weapon);
        if (ashId == null) {
            return;
        }

        AshOfWarDefinition ash = ashRegistry.getById(ashId).orElse(null);
        if (ash == null) {
            player.sendActionBar(Component.text("Unknown Ash of War.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        long cooldownUntil = cooldownUntilByPlayer
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .getOrDefault(ashId, 0L);
        if (now < cooldownUntil) {
            long remaining = cooldownUntil - now;
            player.sendActionBar(Component.text("Ash cooldown: " + remaining + "t", NamedTextColor.GRAY));
            event.setCancelled(true);
            return;
        }

        if (!focusManager.spend(player, ash.fpCost())) {
            player.sendActionBar(Component.text("Not enough FP (" + formatNumber(ash.fpCost()) + ")", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        applyAshOfWarEffect(player, ashId);
        cooldownUntilByPlayer.get(player.getUniqueId()).put(ashId, now + ash.cooldownTicks());
        player.sendActionBar(Component.text(
                "Ash: " + ash.displayName() + " | FP -" + formatNumber(ash.fpCost()),
                NamedTextColor.GOLD
        ));
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void applyAshOfWarEffect(Player player, String ashId) {
        switch (ashId.toLowerCase()) {
            case "assassins_gambit" -> applyAssassinsGambit(player);
            case "barbaric_roar" -> applyBarbaricRoar(player);
            case "barrage" -> applyBarrage(player);
            case "barricade_shield" -> applyBarricadeShield(player);
            case "beasts_roar" -> applyBeastsRoar(player);
            case "black_flame_tornado" -> applyBlackFlameTornado(player);
            case "blood_blade" -> applyBloodBlade(player);
            case "blood_tax" -> applyBloodTax(player);
            case "bloodhounds_step" -> applyBloodhoundsStep(player);
            case "bloody_slash" -> applyBloodySlash(player);
            case "braggarts_roar" -> applyBraggartsRoar(player);
            case "carian_grandeur" -> applyCarianGrandeur(player);
            case "carian_greatsword" -> applyCarianGreatsword(player);
            case "carian_retaliation" -> applyCarianRetaliation(player);
            case "charge_forth" -> applyChargeForth(player);
            case "chilling_mist" -> applyChillingMist(player);
            case "cragblade" -> applyCragblade(player);
            case "determination" -> applyDetermination(player);
            case "double_slash" -> applyDoubleSlash(player);
            case "earthshaker" -> applyEarthshaker(player);
            case "enchanted_shot" -> applyEnchantedShot(player);
            case "endure" -> applyEndure(player);
            case "eruption" -> applyEruption(player);
            case "flame_of_the_redmanes" -> applyFlameOfTheRedmanes(player);
            case "flaming_strike" -> applyFlamingStrike(player);
            case "giant_hunt" -> applyGiantHunt(player);
            case "gravity_well" -> applyGravityWell(player);
            case "lions_claw" -> applyLionsClaw(player);
            case "parry" -> applyParry(player);
            default -> player.sendMessage(Component.text("Unknown Ash of War!", NamedTextColor.RED));
        }
    }

    private void applyAssassinsGambit(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0, false, false));
        player.setHealth(Math.max(0.5, player.getHealth() - 4));
        player.sendMessage(Component.text("Assassin's Gambit activated!", NamedTextColor.DARK_PURPLE));
    }

    private void applyBarbaricRoar(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0, false, false));
        player.sendMessage(Component.text("Barbaric Roar activated!", NamedTextColor.DARK_RED));
    }

    private void applyBarrage(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 1, false, false));
        player.sendMessage(Component.text("Barrage activated!", NamedTextColor.BLUE));
    }

    private void applyBarricadeShield(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 2, false, false));
        player.sendMessage(Component.text("Barricade Shield activated!", NamedTextColor.YELLOW));
    }

    private void applyBeastsRoar(Player player) {
        player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10).forEach(entity -> {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                Vector direction = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                entity.setVelocity(direction.multiply(2));
            }
        });
        player.sendMessage(Component.text("Beast's Roar activated!", NamedTextColor.DARK_GREEN));
    }

    private void applyBlackFlameTornado(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false));
        player.getWorld().getNearbyEntities(player.getLocation(), 8, 8, 8).forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity && !entity.equals(player)) {
                livingEntity.setFireTicks(80);
            }
        });
        player.sendMessage(Component.text("Black Flame Tornado activated!", NamedTextColor.DARK_RED));
    }

    private void applyBloodBlade(Player player) {
        player.setHealth(Math.max(1.0, player.getHealth() - 2.0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
        player.sendMessage(Component.text("Blood Blade activated!", NamedTextColor.DARK_RED));
    }

    private void applyBloodTax(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
        player.sendMessage(Component.text("Blood Tax activated!", NamedTextColor.RED));
    }

    private void applyBloodhoundsStep(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, false, false));
        player.setVelocity(player.getEyeLocation().getDirection().multiply(1.5));
        player.sendMessage(Component.text("Bloodhound's Step activated!", NamedTextColor.LIGHT_PURPLE));
    }

    private void applyBloodySlash(Player player) {
        player.setHealth(Math.max(1.0, player.getHealth() - 3.0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 1, false, false));
        player.sendMessage(Component.text("Bloody Slash activated!", NamedTextColor.DARK_RED));
    }

    private void applyBraggartsRoar(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 220, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 220, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0, false, false));
        player.sendMessage(Component.text("Braggart's Roar activated!", NamedTextColor.GOLD));
    }

    private void applyCarianGrandeur(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 1, false, false));
        player.sendMessage(Component.text("Carian Grandeur activated!", NamedTextColor.AQUA));
    }

    private void applyCarianGreatsword(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
        player.sendMessage(Component.text("Carian Greatsword activated!", NamedTextColor.AQUA));
    }

    private void applyCarianRetaliation(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1, false, false));
        player.sendMessage(Component.text("Carian Retaliation activated!", NamedTextColor.BLUE));
    }

    private void applyChargeForth(Player player) {
        player.setVelocity(player.getEyeLocation().getDirection().multiply(2.2));
        player.sendMessage(Component.text("Charge Forth activated!", NamedTextColor.YELLOW));
    }

    private void applyChillingMist(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0, false, false));
        player.sendMessage(Component.text("Chilling Mist activated!", NamedTextColor.AQUA));
    }

    private void applyCragblade(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 0, false, false));
        player.sendMessage(Component.text("Cragblade activated!", NamedTextColor.GRAY));
    }

    private void applyDetermination(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 2, false, false));
        player.sendMessage(Component.text("Determination activated!", NamedTextColor.WHITE));
    }

    private void applyDoubleSlash(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
        player.sendMessage(Component.text("Double Slash activated!", NamedTextColor.YELLOW));
    }

    private void applyEarthshaker(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, false, false));
        player.sendMessage(Component.text("Earthshaker activated!", NamedTextColor.GOLD));
    }

    private void applyEnchantedShot(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 80, 0, false, false));
        player.sendMessage(Component.text("Enchanted Shot activated!", NamedTextColor.AQUA));
    }

    private void applyEndure(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 1, false, false));
        player.sendMessage(Component.text("Endure activated!", NamedTextColor.GRAY));
    }

    private void applyEruption(Player player) {
        player.getWorld().getNearbyEntities(player.getLocation(), 6, 4, 6).forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity && !entity.equals(player)) {
                livingEntity.setFireTicks(100);
            }
        });
        player.sendMessage(Component.text("Eruption activated!", NamedTextColor.RED));
    }

    private void applyFlameOfTheRedmanes(Player player) {
        player.getWorld().getNearbyEntities(player.getLocation(), 7, 4, 7).forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity && !entity.equals(player)) {
                livingEntity.setFireTicks(80);
            }
        });
        player.sendMessage(Component.text("Flame of the Redmanes activated!", NamedTextColor.GOLD));
    }

    private void applyFlamingStrike(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 120, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 0, false, false));
        player.sendMessage(Component.text("Flaming Strike activated!", NamedTextColor.RED));
    }

    private void applyGiantHunt(Player player) {
        player.setVelocity(player.getEyeLocation().getDirection().multiply(1.8).setY(0.9));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 1, false, false));
        player.sendMessage(Component.text("Giant Hunt activated!", NamedTextColor.YELLOW));
    }

    private void applyGravityWell(Player player) {
        player.getWorld().getNearbyEntities(player.getLocation(), 12, 12, 12).forEach(entity -> {
            if (entity instanceof LivingEntity livingEntity && !entity.equals(player)) {
                Vector direction = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
                livingEntity.setVelocity(direction.multiply(1.5));
            }
        });
        player.sendMessage(Component.text("Gravity Well activated!", NamedTextColor.AQUA));
    }

    private void applyLionsClaw(Player player) {
        player.setVelocity(player.getEyeLocation().getDirection().multiply(2).setY(1.2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 2, false, false));
        player.sendMessage(Component.text("Lion's Claw activated!", NamedTextColor.GOLD));
    }

    private void applyParry(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 3, false, false));
        player.sendMessage(Component.text("Parry activated!", NamedTextColor.YELLOW));
    }
}

