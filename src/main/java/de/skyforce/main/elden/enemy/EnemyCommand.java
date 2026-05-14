package de.skyforce.main.elden.enemy;

import de.skyforce.main.elden.enemy.model.EnemyDefinition;
import de.skyforce.main.elden.enemy.model.EnemySpawnerDefinition;
import de.skyforce.main.elden.enemy.registry.EnemyRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EnemyCommand implements CommandExecutor {

    private final EnemyRegistry enemyRegistry;
    private final EnemyManager enemyManager;
    private final EnemySpawnerManager spawnerManager;

    public EnemyCommand(EnemyRegistry enemyRegistry, EnemyManager enemyManager, EnemySpawnerManager spawnerManager) {
        this.enemyRegistry = enemyRegistry;
        this.enemyManager = enemyManager;
        this.spawnerManager = spawnerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "spawner" -> handleSpawner(sender, args);
            case "group" -> handleGroup(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("elden.enemy.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Available Enemies ===", NamedTextColor.GOLD));
        for (EnemyDefinition definition : enemyRegistry.getAll()) {
            sender.sendMessage(Component.text(
                    definition.id() + " | " + definition.displayName() + " | " + definition.archetype().displayName()
                            + " | " + definition.runeReward() + " runes"
                            + (definition.elite() ? " | elite" : ""),
                    NamedTextColor.AQUA
            ));
        }

        sender.sendMessage(Component.text("=== Configured Spawners ===", NamedTextColor.GOLD));
        if (spawnerManager.getSpawners().isEmpty()) {
            sender.sendMessage(Component.text("No configured enemy spawners.", NamedTextColor.GRAY));
            return;
        }
        for (EnemySpawnerDefinition spawner : spawnerManager.getSpawners().values()) {
            sender.sendMessage(Component.text(
                    spawner.spawnerId() + " | " + spawner.enemyId()
                            + " | group=" + (spawner.groupId() == null ? "-" : spawner.groupId())
                            + " | patrol=" + spawner.patrolPoints().size()
                            + " | eliteChance=" + String.format(java.util.Locale.ROOT, "%.0f%%", spawner.eliteChance() * 100.0D)
                            + " | enabled=" + spawner.enabled(),
                    NamedTextColor.YELLOW
            ));
        }

        sender.sendMessage(Component.text("=== Enemy Groups ===", NamedTextColor.GOLD));
        if (spawnerManager.getGroupIds().isEmpty()) {
            sender.sendMessage(Component.text("No configured enemy groups.", NamedTextColor.GRAY));
            return;
        }
        for (String groupId : spawnerManager.getGroupIds()) {
            sender.sendMessage(Component.text(groupId, NamedTextColor.YELLOW));
        }
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.enemy.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can spawn enemies at their location.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /enemy spawn <id>", NamedTextColor.YELLOW));
            return;
        }

        EnemyDefinition definition = enemyRegistry.getById(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("Unknown enemy: " + args[1], NamedTextColor.RED));
            return;
        }

        if (!enemyManager.spawnEnemyAt(definition, player.getLocation())) {
            sender.sendMessage(Component.text("Enemy could not be spawned here.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Spawned enemy: " + definition.displayName(), NamedTextColor.GREEN));
    }

    private void handleSpawner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.enemy.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /enemy spawner <create|remove|info|enable|disable|reset|group|patrol>", NamedTextColor.YELLOW));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handleCreateSpawner(sender, args);
            case "remove" -> handleRemoveSpawner(sender, args);
            case "info" -> handleSpawnerInfo(sender, args);
            case "enable" -> handleEnableSpawner(sender, args, true);
            case "disable" -> handleEnableSpawner(sender, args, false);
            case "reset" -> handleResetSpawner(sender, args);
            case "group" -> handleSpawnerGroup(sender, args);
            case "patrol" -> handleSpawnerPatrol(sender, args);
            default -> sender.sendMessage(Component.text("Usage: /enemy spawner <create|remove|info|enable|disable|reset|group|patrol>", NamedTextColor.YELLOW));
        }
    }

    private void handleGroup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.enemy.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("reset")) {
            sender.sendMessage(Component.text("Usage: /enemy group reset <group-id>", NamedTextColor.YELLOW));
            return;
        }
        if (!spawnerManager.resetGroup(args[2])) {
            sender.sendMessage(Component.text("No enemy group found with that id.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Enemy group reset.", NamedTextColor.GREEN));
    }

    private void handleCreateSpawner(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can create enemy spawners from their position.", NamedTextColor.RED));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /enemy spawner create <enemy-id> <spawner-id>", NamedTextColor.YELLOW));
            return;
        }

        EnemyDefinition definition = enemyRegistry.getById(args[2]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("Unknown enemy: " + args[2], NamedTextColor.RED));
            return;
        }

        if (!spawnerManager.createSpawner(args[3], definition, player.getLocation())) {
            sender.sendMessage(Component.text("Enemy spawner could not be created.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "Created enemy spawner '" + args[3] + "' for " + definition.displayName() + ".",
                NamedTextColor.GREEN
        ));
    }

    private void handleRemoveSpawner(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /enemy spawner remove <spawner-id>", NamedTextColor.YELLOW));
            return;
        }
        if (!spawnerManager.removeSpawner(args[2])) {
            sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Enemy spawner removed.", NamedTextColor.GREEN));
    }

    private void handleSpawnerInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /enemy spawner info <spawner-id>", NamedTextColor.YELLOW));
            return;
        }
        EnemySpawnerDefinition spawner = spawnerManager.getSpawner(args[2]).orElse(null);
        if (spawner == null) {
            sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("=== Enemy Spawner ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Id: " + spawner.spawnerId(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Enemy: " + spawner.enemyId(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("World: " + spawner.worldName(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Active limit: " + spawner.maxActive(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Radius: " + spawner.radius(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Elite chance: " + String.format(java.util.Locale.ROOT, "%.0f%%", spawner.eliteChance() * 100.0D), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Respawn delay: " + spawner.respawnDelayTicks() + " ticks", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Group: " + (spawner.groupId() == null ? "-" : spawner.groupId()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Patrol points: " + spawner.patrolPoints().size(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Reset on Grace: " + spawner.resetOnGrace(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Enabled: " + spawner.enabled(), NamedTextColor.GRAY));
    }

    private void handleEnableSpawner(CommandSender sender, String[] args, boolean enabled) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /enemy spawner " + (enabled ? "enable" : "disable") + " <spawner-id>", NamedTextColor.YELLOW));
            return;
        }
        if (!spawnerManager.setEnabled(args[2], enabled)) {
            sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "Enemy spawner " + (enabled ? "enabled" : "disabled") + ".",
                NamedTextColor.GREEN
        ));
    }

    private void handleResetSpawner(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /enemy spawner reset <spawner-id>", NamedTextColor.YELLOW));
            return;
        }
        if (!spawnerManager.resetSpawner(args[2])) {
            sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Enemy spawner reset.", NamedTextColor.GREEN));
    }

    private void handleSpawnerGroup(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /enemy spawner group <spawner-id> <group-id|none>", NamedTextColor.YELLOW));
            return;
        }
        String groupId = args[3].equalsIgnoreCase("none") ? null : args[3];
        if (!spawnerManager.setGroup(args[2], groupId)) {
            sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                groupId == null ? "Enemy spawner removed from its group." : "Enemy spawner assigned to group '" + groupId + "'.",
                NamedTextColor.GREEN
        ));
    }

    private void handleSpawnerPatrol(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /enemy spawner patrol <add|clear> <spawner-id>", NamedTextColor.YELLOW));
            return;
        }
        switch (args[2].toLowerCase()) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can add patrol points from their position.", NamedTextColor.RED));
                    return;
                }
                if (!spawnerManager.addPatrolPoint(args[3], player.getLocation())) {
                    sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
                    return;
                }
                sender.sendMessage(Component.text("Patrol point added.", NamedTextColor.GREEN));
            }
            case "clear" -> {
                if (!spawnerManager.clearPatrol(args[3])) {
                    sender.sendMessage(Component.text("No enemy spawner found with that id.", NamedTextColor.RED));
                    return;
                }
                sender.sendMessage(Component.text("Patrol route cleared.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /enemy spawner patrol <add|clear> <spawner-id>", NamedTextColor.YELLOW));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/enemy list", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawn <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner create <enemy-id> <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner info <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner enable <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner disable <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner reset <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner group <spawner-id> <group-id|none>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner patrol add <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner patrol clear <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy spawner remove <spawner-id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/enemy group reset <group-id>", NamedTextColor.YELLOW));
    }
}
