package de.skyforce.main.elden.combat;

import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.weapon.service.WeaponDamageService;
import de.skyforce.main.elden.weapon.service.WeaponGameplayService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import java.util.function.Function;

public final class CombatListener implements Listener {

    private final StaminaManager staminaManager;
    private final DodgeManager dodgeManager;
    private final LevelManager levelManager;
    private final WeaponDamageService weaponDamageService;
    private final WeaponGameplayService weaponGameplayService;
    private final FocusManager focusManager;
    private Function<Player, Double> physicalDefenseMultiplierProvider = player -> 1.0D;

    public CombatListener(StaminaManager staminaManager, DodgeManager dodgeManager, LevelManager levelManager,
                          WeaponDamageService weaponDamageService, WeaponGameplayService weaponGameplayService,
                          FocusManager focusManager) {
        this.staminaManager = staminaManager;
        this.dodgeManager = dodgeManager;
        this.levelManager = levelManager;
        this.weaponDamageService = weaponDamageService;
        this.weaponGameplayService = weaponGameplayService;
        this.focusManager = focusManager;
    }

    public void setPhysicalDefenseMultiplierProvider(Function<Player, Double> physicalDefenseMultiplierProvider) {
        this.physicalDefenseMultiplierProvider = physicalDefenseMultiplierProvider == null ? player -> 1.0D : physicalDefenseMultiplierProvider;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        staminaManager.reset(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        staminaManager.applyVanillaHungerOverride(event.getPlayer());
        staminaManager.updateHud(event.getPlayer());
        focusManager.reset(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dodgeManager.clear(player);
        levelManager.savePlayer(player);
        focusManager.savePlayer(player);
        staminaManager.remove(player);
        focusManager.remove(player);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!staminaManager.isVanillaHungerDisabled()) {
            return;
        }

        event.setCancelled(true);
        staminaManager.applyVanillaHungerOverride(player);
    }

    @EventHandler
    public void onExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!staminaManager.isVanillaHungerDisabled()) {
            return;
        }

        event.setCancelled(true);
        event.setExhaustion(0.0F);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        staminaManager.handleFoodConsume(player, event.getItem());
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        double customDamage = weaponDamageService.computeFinalDamage(player, levelManager);
        if (customDamage >= 0.0) {
            boolean allowed = weaponGameplayService.handleCustomWeaponAttack(player, event.getEntity());
            if (!allowed) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(customDamage);
            if (event.getEntity() instanceof org.bukkit.entity.LivingEntity livingEntity) {
                weaponGameplayService.applyPassiveOnHit(player, livingEntity, event);
            }
            return;
        }

        boolean allowed = staminaManager.handleAttackCost(player);
        if (!allowed) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Not enough stamina", NamedTextColor.RED));
            return;
        }

        boolean twoHanded = isTwoHanded(player);
        boolean criticalHit = isLikelyCriticalHit(player);
        double multiplier = resolveVanillaAttackMultiplier(player, twoHanded, criticalHit);
        if (multiplier != 1.0D) {
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        dodgeManager.attemptDodge(player, staminaManager);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!dodgeManager.hasIframe(player, event.getCause())) {
            weaponGameplayService.applyGuard(player, event);
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                double fallMultiplier = levelManager.getDexterityFallDamageMultiplier(player);
                event.setDamage(event.getDamage() * fallMultiplier);
            }
            if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
                double magicDefenseMultiplier = levelManager.getIntelligenceMagicDefenseMultiplier(player);
                event.setDamage(event.getDamage() * magicDefenseMultiplier);
            }
            if (isHolyLikeCause(event.getCause())) {
                double holyDefenseMultiplier = levelManager.getArcaneHolyDefenseMultiplier(player);
                event.setDamage(event.getDamage() * holyDefenseMultiplier);
            }
            if (isDeathLikeCause(event.getCause())) {
                double deathResistanceMultiplier = levelManager.getArcaneDeathResistanceMultiplier(player);
                event.setDamage(event.getDamage() * deathResistanceMultiplier);
            }
            if (isStrengthDefensiveCause(event.getCause())) {
                double defenseMultiplier = levelManager.getStrengthDefenseMultiplier(player);
                event.setDamage(event.getDamage() * defenseMultiplier * Math.max(0.0D, physicalDefenseMultiplierProvider.apply(player)));
            }
            return;
        }

        event.setCancelled(true);
        player.sendActionBar(Component.text("I-Frame", NamedTextColor.AQUA));
    }

    private boolean isTwoHanded(Player player) {
        return player.getInventory().getItemInOffHand().getType() == Material.AIR;
    }

    private boolean isLikelyCriticalHit(Player player) {
        return player.getFallDistance() > 0.0F && !player.isOnGround() && !player.isSprinting();
    }

    private boolean isStrengthDefensiveCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                || cause == EntityDamageEvent.DamageCause.PROJECTILE;
    }

    private double resolveVanillaAttackMultiplier(Player player, boolean twoHanded, boolean criticalHit) {
        Material weapon = player.getInventory().getItemInMainHand().getType();
        if (!isVanillaMeleeWeapon(weapon)) {
            return 1.0D;
        }
        if (isDexterityWeapon(weapon)) {
            return levelManager.getDexterityAttackMultiplier(player);
        }
        if (isStrengthWeapon(weapon)) {
            return levelManager.getStrengthAttackMultiplier(player, twoHanded, criticalHit);
        }
        return 1.0D;
    }

    private boolean isVanillaMeleeWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_MACE")
                || name.endsWith("_TRIDENT");
    }

    private boolean isDexterityWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_TRIDENT");
    }

    private boolean isStrengthWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_AXE")
                || name.endsWith("_MACE");
    }

    private boolean isHolyLikeCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.SONIC_BOOM;
    }

    private boolean isDeathLikeCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.WITHER
                || cause == EntityDamageEvent.DamageCause.POISON;
    }
}

