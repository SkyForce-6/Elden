package de.skyforce.main.elden.ashes.skill;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface AshSkill {
    void execute(Player player, ItemStack weapon);
}
