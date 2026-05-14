package de.skyforce.main.elden.equipment;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class EquipmentWeightListener implements Listener {

    private final JavaPlugin plugin;
    private final EquipmentWeightService equipmentWeightService;

    public EquipmentWeightListener(JavaPlugin plugin, EquipmentWeightService equipmentWeightService) {
        this.plugin = plugin;
        this.equipmentWeightService = equipmentWeightService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMainHandChanged(PlayerChangedMainHandEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    private void scheduleRefresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> equipmentWeightService.applyMovementSpeed(player));
    }
}
