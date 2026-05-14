package de.skyforce.main.elden.enemy;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class EnemyListener implements Listener {

    private final EnemyManager enemyManager;

    public EnemyListener(EnemyManager enemyManager) {
        this.enemyManager = enemyManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }
        if (!enemyManager.isManagedEnemy(livingEntity)) {
            return;
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        enemyManager.alertNearbyEnemies(livingEntity, player);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!enemyManager.isManagedEnemy(event.getEntity())) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        event.setDroppedExp(0);
        enemyManager.handleEnemyDeath(event.getEntity(), killer);
    }
}
