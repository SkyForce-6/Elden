package de.skyforce.main.elden.weapon;

import de.skyforce.main.elden.weapon.service.WeaponGameplayService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class WeaponSkillListener implements Listener {

    private final WeaponGameplayService weaponGameplayService;

    public WeaponSkillListener(WeaponGameplayService weaponGameplayService) {
        this.weaponGameplayService = weaponGameplayService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        if (weaponGameplayService.tryUseBuiltInSkill(event.getPlayer(), item)) {
            event.setCancelled(true);
        }
    }
}
