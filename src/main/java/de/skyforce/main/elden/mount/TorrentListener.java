package de.skyforce.main.elden.mount;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class TorrentListener implements Listener {

    private final TorrentManager torrentManager;

    public TorrentListener(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        torrentManager.grantStarterWhistleIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        torrentManager.grantStarterWhistleIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        torrentManager.handleDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        torrentManager.handleDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        torrentManager.handleTorrentDeath(event.getEntity());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        torrentManager.handleDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            Material type = clicked.getType();
            if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
                return;
            }
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        if (torrentManager.tryUseWhistle(event.getPlayer(), item, hand)
                || torrentManager.tryUseRaisin(event.getPlayer(), item, hand)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof AbstractHorse horse) || !torrentManager.isTorrent(horse)) {
            return;
        }

        if (!torrentManager.isOwner(event.getPlayer(), horse)) {
            event.setCancelled(true);
            return;
        }

        if (!horse.getPassengers().contains(event.getPlayer())) {
            horse.addPassenger(event.getPlayer());
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHorseJump(HorseJumpEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse) {
            torrentManager.markJumpStarted(horse, event.getPower());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        torrentManager.tryAirJump(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        torrentManager.tryTriggerSpiritSpring(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
            return;
        }
        if (!(event.getVehicle() instanceof AbstractHorse horse) || !torrentManager.isTorrent(horse)) {
            return;
        }
        if (!torrentManager.isOwner(player, horse)) {
            return;
        }

        torrentManager.dismiss(player, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        torrentManager.handleFallDamage(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTorrentDamage(EntityDamageByEntityEvent event) {
        if (!torrentManager.isProtectedFromDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractHorse horse) || !torrentManager.isTorrent(horse)) {
            return;
        }

        event.setCancelled(true);
    }
}
