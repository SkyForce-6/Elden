package de.skyforce.main.elden.enemy;

import de.skyforce.main.elden.enemy.model.EnemyDefinition;
import de.skyforce.main.elden.enemy.registry.EnemyRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class EnemyTabCompleter implements TabCompleter {

    private final EnemyRegistry enemyRegistry;
    private final EnemySpawnerManager spawnerManager;

    public EnemyTabCompleter(EnemyRegistry enemyRegistry, EnemySpawnerManager spawnerManager) {
        this.enemyRegistry = enemyRegistry;
        this.spawnerManager = spawnerManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("list", "spawn", "spawner", "group"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "spawn" -> filter(enemyIds(), args[1]);
                case "spawner" -> filter(List.of("create", "remove", "info", "enable", "disable", "reset", "group", "patrol"), args[1]);
                case "group" -> filter(List.of("reset"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("spawner")) {
                return switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "create" -> filter(enemyIds(), args[2]);
                    case "remove", "info", "enable", "disable", "reset", "group" -> filter(spawnerIds(), args[2]);
                    case "patrol" -> filter(List.of("add", "clear"), args[2]);
                    default -> List.of();
                };
            }
            if (args[0].equalsIgnoreCase("group") && args[1].equalsIgnoreCase("reset")) {
                return filter(groupIds(), args[2]);
            }
            return List.of();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("patrol")) {
            return filter(spawnerIds(), args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("spawner") && args[1].equalsIgnoreCase("group")) {
            List<String> values = new ArrayList<>(groupIds());
            values.add("none");
            return filter(values, args[3]);
        }
        return List.of();
    }

    private List<String> enemyIds() {
        List<String> ids = new ArrayList<>();
        for (EnemyDefinition definition : enemyRegistry.getAll()) {
            ids.add(definition.id());
        }
        return ids;
    }

    private List<String> spawnerIds() {
        return new ArrayList<>(spawnerManager.getSpawners().keySet());
    }

    private List<String> groupIds() {
        return new ArrayList<>(spawnerManager.getGroupIds());
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
