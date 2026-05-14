package de.skyforce.main.elden.ashes;

import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AshOfWarTabCompleter implements TabCompleter {

    private final AshOfWarRegistry ashRegistry;

    public AshOfWarTabCompleter(AshOfWarRegistry ashRegistry) {
        this.ashRegistry = ashRegistry;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument: subcommand
            completions.add("give");
            completions.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Second argument: ash id
            completions.addAll(ashRegistry.getAll().keySet());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Third argument: player name
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}

