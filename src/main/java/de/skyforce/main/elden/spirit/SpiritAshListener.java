package de.skyforce.main.elden.spirit;

import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import de.skyforce.main.elden.spirit.service.SpiritAshItemFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class SpiritAshListener implements Listener {

    private final SpiritAshRegistry spiritAshRegistry;
    private final SpiritAshItemFactory spiritAshItemFactory;
    private final SpiritAshManager spiritAshManager;
    private final FocusManager focusManager;
    private final Map<UUID, Long> cooldownUntilByPlayer = new HashMap<>();

    public SpiritAshListener(SpiritAshRegistry spiritAshRegistry, SpiritAshItemFactory spiritAshItemFactory,
                             SpiritAshManager spiritAshManager, FocusManager focusManager) {
        this.spiritAshRegistry = spiritAshRegistry;
        this.spiritAshItemFactory = spiritAshItemFactory;
        this.spiritAshManager = spiritAshManager;
        this.focusManager = focusManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!spiritAshItemFactory.isSpiritAshItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("elden.spirit.use")) {
            player.sendActionBar(Component.text("You do not have permission to summon spirits.", NamedTextColor.RED));
            return;
        }

        SpiritAshDefinition spiritAsh = spiritAshRegistry.getById(spiritAshItemFactory.getSpiritAshId(item)).orElse(null);
        if (spiritAsh == null) {
            player.sendActionBar(Component.text("Unknown Spirit Ash.", NamedTextColor.RED));
            return;
        }

        if (spiritAshManager.hasActiveSummon(player)) {
            spiritAshManager.dismiss(player, true);
            return;
        }

        long now = player.getServer().getCurrentTick();
        long cooldownUntil = cooldownUntilByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            player.sendActionBar(Component.text("Spirit cooldown: " + (cooldownUntil - now) + "t", NamedTextColor.GRAY));
            return;
        }

        if (!focusManager.hasEnough(player, spiritAsh.fpCost())) {
            player.sendActionBar(Component.text("Not enough FP (" + formatNumber(spiritAsh.fpCost()) + ")", NamedTextColor.RED));
            return;
        }

        if (!focusManager.spend(player, spiritAsh.fpCost())) {
            player.sendActionBar(Component.text("Not enough FP.", NamedTextColor.RED));
            return;
        }

        if (!spiritAshManager.summon(player, spiritAsh)) {
            focusManager.restore(player, spiritAsh.fpCost());
            player.sendActionBar(Component.text("Your Spirit Ash could not be summoned here.", NamedTextColor.RED));
            return;
        }

        cooldownUntilByPlayer.put(player.getUniqueId(), now + spiritAsh.cooldownTicks());
        player.swingHand(EquipmentSlot.HAND);
        player.sendActionBar(Component.text(spiritAsh.displayName() + " | FP -" + formatNumber(spiritAsh.fpCost()), NamedTextColor.AQUA));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!spiritAshManager.isSpiritSummon(event.getEntity())) {
            return;
        }

        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }

        UUID ownerId = spiritAshManager.getOwnerId(event.getEntity());
        if (ownerId == null) {
            return;
        }

        if (spiritAshManager.isFriendly(ownerId, target)) {
            event.setCancelled(true);
            event.setTarget(null);
            return;
        }

        if (target instanceof Player) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity attacker = resolveDamager(event.getDamager());
        Entity victim = event.getEntity();

        if (attacker != null && spiritAshManager.isFriendlyPair(attacker, victim)) {
            event.setCancelled(true);
            return;
        }

        if (spiritAshManager.isSpiritSummon(attacker) && victim instanceof Player) {
            event.setCancelled(true);
            return;
        }

        if (spiritAshManager.isSpiritSummon(attacker) && victim instanceof LivingEntity) {
            spiritAshManager.handleSpiritAttack(event);
        }

        if (victim instanceof Player player && attacker instanceof LivingEntity livingAttacker) {
            spiritAshManager.directSummons(player, livingAttacker);
        }

        if (attacker instanceof Player player && victim instanceof LivingEntity livingVictim) {
            spiritAshManager.directSummons(player, livingVictim);
        }

        if (spiritAshManager.isSpiritSummon(victim) && attacker instanceof LivingEntity livingAttacker) {
            UUID ownerId = spiritAshManager.getOwnerId(victim);
            if (ownerId != null) {
                Player owner = playerById(ownerId);
                if (owner != null && !spiritAshManager.isFriendly(ownerId, attacker)) {
                    spiritAshManager.directSummons(owner, livingAttacker);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        spiritAshManager.dismiss(event.getPlayer(), false);
        cooldownUntilByPlayer.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        spiritAshManager.dismiss(event.getPlayer(), false);
    }

    private Entity resolveDamager(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return damager;
    }

    private Player playerById(UUID playerId) {
        return org.bukkit.Bukkit.getPlayer(playerId);
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
