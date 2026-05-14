package de.skyforce.main.elden.ashes.listener;

import de.skyforce.main.elden.ashes.service.AshSkillService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.net.http.WebSocket;

public final class AshListener implements WebSocket.Listener {

    private final AshSkillService ashSkillService;

    public AshListener(AshSkillService ashSkillService) {
        this.ashSkillService = ashSkillService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack weapon = event.getItem();
        if (weapon == null) {
            return;
        }

        if (ashSkillService.executeBoundSkill(event.getPlayer(), weapon)) {
            event.setCancelled(true);
        }
    }
}
