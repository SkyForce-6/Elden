package de.skyforce.main.elden.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PlayerCommand implements CommandExecutor {

    private final PlayerGuiService playerGuiService;

    public PlayerCommand(PlayerGuiService playerGuiService) {
        this.playerGuiService = playerGuiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("elden.player.gui")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        playerGuiService.openProfileMenu(player);
        return true;
    }
}
