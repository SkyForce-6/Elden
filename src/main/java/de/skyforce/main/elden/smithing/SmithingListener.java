package de.skyforce.main.elden.smithing;

import de.skyforce.main.elden.smithing.gui.SmithingMenu;
import de.skyforce.main.elden.smithing.service.SmithingAnvilService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class SmithingListener implements Listener {

    private final SmithingAnvilService smithingAnvilService;
    private final SmithingMenu smithingMenu;

    public SmithingListener(SmithingAnvilService smithingAnvilService, SmithingMenu smithingMenu) {
        this.smithingAnvilService = smithingAnvilService;
        this.smithingMenu = smithingMenu;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!smithingAnvilService.isSmithingAnvilItem(event.getItemInHand())) {
            return;
        }
        smithingAnvilService.registerPlacedAnvil(event.getBlockPlaced());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!smithingAnvilService.isSmithingAnvil(block)) {
            return;
        }

        smithingAnvilService.unregisterAnvil(block);
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            block.getWorld().dropItemNaturally(block.getLocation(), smithingAnvilService.createAnvilItem());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!smithingAnvilService.isSmithingAnvil(block)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("elden.smithing.use")) {
            player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.0f);
        smithingMenu.open(player);
    }
}
