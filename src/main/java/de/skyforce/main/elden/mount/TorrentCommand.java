package de.skyforce.main.elden.mount;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TorrentCommand implements CommandExecutor {

    private final TorrentManager torrentManager;

    public TorrentCommand(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("elden.mount.torrent")) {
            player.sendMessage(Component.text("You do not have permission to use Torrent.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0) {
            player.sendMessage(Component.text("Usage: /torrent", NamedTextColor.YELLOW));
            return true;
        }

        torrentManager.giveWhistle(player, true);
        return true;
    }
}
