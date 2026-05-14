package de.skyforce.main.elden.runes;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

public final class RuneListener implements Listener {

    private final RuneManager runeManager;

    public RuneListener(RuneManager runeManager) {
        this.runeManager = runeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        int lostRunes = runeManager.takeAllRunes(player.getUniqueId());
        if (lostRunes <= 0) {
            return;
        }

        Item dropped = runeManager.spawnRuneDrop(player, lostRunes, player.getLocation());
        if (dropped == null) {
            runeManager.addRunes(player.getUniqueId(), lostRunes);
            return;
        }

        player.sendMessage(text("✖ You lost " + lostRunes + " runes.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRunePickup(PlayerAttemptPickupItemEvent event) {
        Item item = event.getItem();
        if (!runeManager.isRuneDrop(item)) {
            return;
        }

        Player player = event.getPlayer();
        UUID owner = runeManager.getRuneDropOwner(item);
        if (owner == null) {
            return;
        }

        if (!owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendActionBar(text("✖ These are not your runes.", NamedTextColor.DARK_RED));
            return;
        }

        int recovered = runeManager.consumeRuneDrop(item);
        if (recovered <= 0) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        int total = runeManager.addRunes(player.getUniqueId(), recovered);

        player.sendMessage(text("✦ Runes reclaimed: +" + recovered + " (" + total + ")", NamedTextColor.GOLD));
        player.sendActionBar(text("✦ Runes reclaimed", NamedTextColor.GOLD));
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}