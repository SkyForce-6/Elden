package de.skyforce.main.elden.smithing;

import de.skyforce.main.elden.smithing.model.SmithingTrack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public final class SmithingTabCompleter implements TabCompleter {

    private static final List<String> ROOT = List.of("info", "set", "anvil", "stone");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        if (!sender.hasPermission("elden.smithing.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterByPrefix(args[0], ROOT);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("anvil")) {
            return filterByPrefix(args[1], List.of("give"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("stone")) {
            return filterByPrefix(args[1], List.of("give"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filterByPrefix(args[1], List.of("0", "5", "10", "15", "20", "25"));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("stone") && args[1].equalsIgnoreCase("give")) {
            return filterByPrefix(args[2], List.of("standard", "somber"));
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("stone") && args[1].equalsIgnoreCase("give")) {
            SmithingTrack track = "somber".equalsIgnoreCase(args[2]) ? SmithingTrack.SOMBER : SmithingTrack.STANDARD;
            List<String> tiers = new ArrayList<>();
            for (int tier = 1; tier <= (track == SmithingTrack.STANDARD ? 9 : 10); tier++) {
                tiers.add(String.valueOf(tier));
            }
            return filterByPrefix(args[3], tiers);
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("stone") && args[1].equalsIgnoreCase("give")) {
            return filterByPrefix(args[4], List.of("1", "2", "4", "8", "16"));
        }

        if ((args.length == 2 && args[0].equalsIgnoreCase("info"))
                || (args.length == 3 && args[0].equalsIgnoreCase("set"))
                || (args.length == 3 && args[0].equalsIgnoreCase("anvil") && args[1].equalsIgnoreCase("give"))
                || (args.length == 5 && args[0].equalsIgnoreCase("stone") && args[1].equalsIgnoreCase("give")
                && !isInteger(args[4]))
                || (args.length == 6 && args[0].equalsIgnoreCase("stone") && args[1].equalsIgnoreCase("give"))) {
            return filterByPrefix(args[args.length - 1], Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .sorted()
                    .toList());
        }

        return Collections.emptyList();
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private List<String> filterByPrefix(String input, Collection<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(input, values, matches);
        return matches;
    }
}
