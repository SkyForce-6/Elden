package de.skyforce.main.elden.boss;

import de.skyforce.main.elden.boss.model.BossDefinition;
import de.skyforce.main.elden.boss.registry.BossRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class BossTabCompleter implements TabCompleter {

    private final BossRegistry bossRegistry;
    private final BossManager bossManager;
    private final BossPortalManager bossPortalManager;

    public BossTabCompleter(BossRegistry bossRegistry, BossManager bossManager, BossPortalManager bossPortalManager) {
        this.bossRegistry = bossRegistry;
        this.bossManager = bossManager;
        this.bossPortalManager = bossPortalManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of(
                    "spawn",
                    "list",
                    "station",
                    "remembrance",
                    "despawn",
                    "setspawn",
                    "removespawn",
                    "setportalentrance",
                    "setportalexit",
                    "setportalexitlock",
                    "setportalboss",
                    "portalinfo",
                    "removeportal"
            ), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "spawn" -> filter(bossIds(), args[1]);
                case "station" -> filter(List.of("give"), args[1]);
                case "remembrance" -> filter(List.of("info", "exchange"), args[1]);
                case "despawn" -> filter(activeBossTargets(), args[1]);
                case "removespawn" -> filter(configuredSpawnNames(), args[1]);
                case "setportalentrance", "setportalexit", "setportalexitlock", "setportalboss", "portalinfo", "removeportal" -> filter(portalNames(), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "station" -> args[1].equalsIgnoreCase("give")
                        ? filter(onlinePlayerNames(), args[2])
                        : List.of();
                case "remembrance" -> args[1].equalsIgnoreCase("exchange")
                        ? filter(List.of("reward", "runes"), args[2])
                        : List.of();
                case "setspawn", "setportalboss" -> filter(bossIds(), args[2]);
                case "setportalexitlock" -> filter(List.of("true", "false"), args[2]);
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> bossIds() {
        List<String> ids = new ArrayList<>();
        for (BossDefinition definition : bossRegistry.getAll()) {
            ids.add(definition.id());
        }
        return ids;
    }

    private List<String> configuredSpawnNames() {
        return new ArrayList<>(bossManager.getConfiguredSpawns().keySet());
    }

    private List<String> portalNames() {
        return new ArrayList<>(bossPortalManager.getConfiguredPortals().keySet());
    }

    private List<String> activeBossTargets() {
        List<String> targets = new ArrayList<>();
        targets.add("all");
        for (Map.Entry<java.util.UUID, String> entry : bossManager.getActiveBossNames().entrySet()) {
            targets.add(entry.getKey().toString());
        }
        return targets;
    }

    private List<String> onlinePlayerNames() {
        return org.bukkit.Bukkit.getOnlinePlayers().stream()
                .map(org.bukkit.entity.Player::getName)
                .sorted()
                .toList();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
