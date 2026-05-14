package de.skyforce.main.elden.boss.remembrance;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class RemembranceListener implements Listener {

    private final RemembranceStationService remembranceStationService;
    private final RemembranceMenu remembranceMenu;

    public RemembranceListener(RemembranceStationService remembranceStationService, RemembranceMenu remembranceMenu) {
        this.remembranceStationService = remembranceStationService;
        this.remembranceMenu = remembranceMenu;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!remembranceStationService.isStationItem(event.getItemInHand())) {
            return;
        }
        remembranceStationService.registerPlacedStation(event.getBlockPlaced());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!remembranceStationService.isStation(block)) {
            return;
        }

        remembranceStationService.unregisterStation(block);
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            block.getWorld().dropItemNaturally(block.getLocation(), remembranceStationService.createStationItem());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!remembranceStationService.isStation(block)) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.0f);
        remembranceMenu.open(player);
    }
}
