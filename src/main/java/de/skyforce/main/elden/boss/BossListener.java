package de.skyforce.main.elden.boss;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BossListener implements Listener {

    private final BossManager bossManager;

    public BossListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamagedByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!bossManager.isBoss(entity)) {
            return;
        }
        bossManager.recordParticipant(player, entity);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossHitsPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity boss) || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!bossManager.isBoss(boss)) {
            return;
        }
        bossManager.handleBossHitPlayer(boss, player);
        bossManager.handleBossMeleeEffect(boss, player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossProjectileHitsPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile) || !(event.getEntity() instanceof Player player)) {
            return;
        }
        bossManager.handleBossProjectileHit(projectile, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!bossManager.isBoss(entity)) {
            return;
        }
        event.getDrops().clear();
        bossManager.handleBossDeath(entity);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBossTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss) || !(event.getTarget() instanceof Player player)) {
            return;
        }
        if (!bossManager.isBoss(boss)) {
            return;
        }
        bossManager.handleBossHitPlayer(boss, player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Fights keep running; viewers and reward participants are adjusted lazily.
    }
}
