package de.skyforce.main.elden.ashes.skill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class StormBladeSkill implements AshSkill {

    @Override
    public void execute(Player player, ItemStack weapon) {
        player.sendMessage(Component.text("Storm Blade unleashed!", NamedTextColor.AQUA));
        // Later: projectile, particles, damage, etc.
    }
}

