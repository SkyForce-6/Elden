package de.skyforce.main.elden.weapon;

import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
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

public final class WeaponTabCompleter implements TabCompleter {

    private static final List<String> ADMIN_SUBCOMMANDS = List.of("give", "list");
    private static final List<String> USER_SUBCOMMANDS = List.of("list");

    private final WeaponRegistry weaponRegistry;

    public WeaponTabCompleter(WeaponRegistry weaponRegistry) {
        this.weaponRegistry = weaponRegistry;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        if (args.length == 1) {
            return filterByPrefix(args[0], sender.hasPermission("elden.weapon.admin") ? ADMIN_SUBCOMMANDS : USER_SUBCOMMANDS);
        }

        if (!sender.hasPermission("elden.weapon.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> weaponIds = weaponRegistry.all().stream()
                    .map(WeaponDefinition::id)
                    .sorted()
                    .toList();
            return filterByPrefix(args[1], weaponIds);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .sorted()
                    .toList();
            return filterByPrefix(args[2], playerNames);
        }

        return Collections.emptyList();
    }

    private List<String> filterByPrefix(String input, Collection<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(input, values, matches);
        return matches;
    }
}

