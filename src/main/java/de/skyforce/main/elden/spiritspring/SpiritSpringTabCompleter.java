package de.skyforce.main.elden.spiritspring;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SpiritSpringTabCompleter implements TabCompleter {

    private static final List<String> PLAYER_SUBS = List.of("list", "info", "nearest");
    private static final List<String> ADMIN_EXTRA_SUBS = List.of("set", "remove", "rename", "tp", "reload");

    private final SpiritSpringManager spiritSpringManager;

    public SpiritSpringTabCompleter(SpiritSpringManager spiritSpringManager) {
        this.spiritSpringManager = spiritSpringManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(PLAYER_SUBS);
            if (player.hasPermission("elden.spiritspring.admin")) {
                subs.addAll(ADMIN_EXTRA_SUBS);
            }
            return filterPrefix(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        // arg[1]: spring name for these subcommands
        if (args.length == 2 && (sub.equals("remove") || sub.equals("tp") || sub.equals("rename"))) {
            if (!player.hasPermission("elden.spiritspring.admin")) {
                return List.of();
            }
            return filterPrefix(new ArrayList<>(spiritSpringManager.getSpiritSpringNames()), args[1]);
        }

        // arg[2]: new name for rename (free text, no suggestions needed)
        return List.of();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}

