package de.skyforce.main.elden.flask;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlaskListener implements Listener {

    private final JavaPlugin plugin;
    private final FlaskService flaskService;

    public FlaskListener(JavaPlugin plugin, FlaskService flaskService) {
        this.plugin = plugin;
        this.flaskService = flaskService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> flaskService.grantStarterFlasksIfMissing(player), 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        flaskService.grantStarterFlasksIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flaskService.clearPlayerCooldowns(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
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

        if (!flaskService.isFlaskItem(item)) {
            return;
        }

        if (flaskService.shouldUseDrinkAnimation()) {
            if (!flaskService.prepareFlaskUse(event.getPlayer(), item)) {
                event.setCancelled(true);
            }
            return;
        }

        boolean consumed = flaskService.tryUseFlask(event.getPlayer(), item, hand);
        if (consumed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!flaskService.shouldUseDrinkAnimation() || !flaskService.isFlaskItem(item)) {
            return;
        }

        EquipmentSlot hand = flaskService.resolveHeldFlaskHand(event.getPlayer(), item);
        event.setCancelled(true);
        flaskService.finishAnimatedFlaskUse(event.getPlayer(), item, hand);
    }
}
