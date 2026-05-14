package de.skyforce.main.elden.boss;

import de.skyforce.main.elden.boss.model.BossDefinition;
import de.skyforce.main.elden.boss.registry.BossRegistry;
import de.skyforce.main.elden.boss.remembrance.RemembranceStationService;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class BossCommand implements CommandExecutor {

    private final BossRegistry bossRegistry;
    private final BossManager bossManager;
    private final BossPortalManager bossPortalManager;
    private final RemembranceStationService remembranceStationService;

    public BossCommand(BossRegistry bossRegistry,
                       BossManager bossManager,
                       BossPortalManager bossPortalManager,
                       RemembranceStationService remembranceStationService) {
        this.bossRegistry = bossRegistry;
        this.bossManager = bossManager;
        this.bossPortalManager = bossPortalManager;
        this.remembranceStationService = remembranceStationService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "list" -> handleList(sender);
            case "station" -> handleStation(sender, args);
            case "remembrance" -> handleRemembrance(sender, args);
            case "despawn" -> handleDespawn(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "removespawn" -> handleRemoveSpawn(sender, args);
            case "setportalentrance" -> handleSetPortalEntrance(sender, args);
            case "setportalexit" -> handleSetPortalExit(sender, args);
            case "setportalexitlock" -> handleSetPortalExitLock(sender, args);
            case "setportalboss" -> handleSetPortalBoss(sender, args);
            case "portalinfo" -> handlePortalInfo(sender, args);
            case "removeportal" -> handleRemovePortal(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can spawn bosses at their location.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss spawn <id>", NamedTextColor.YELLOW));
            return;
        }

        BossDefinition definition = bossRegistry.getById(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("Unknown boss: " + args[1], NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /boss list for available bosses.", NamedTextColor.GRAY));
            return;
        }

        boolean spawned = bossManager.spawnBoss(definition, player.getLocation());
        if (!spawned) {
            sender.sendMessage(Component.text("Boss could not be spawned here.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Spawned boss: " + definition.displayName(), NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Available Bosses ===", NamedTextColor.GOLD));
        for (BossDefinition definition : bossRegistry.getAll()) {
            sender.sendMessage(Component.text(
                    definition.id() + " | " + definition.displayName() + " | " + definition.runeReward() + " runes",
                    NamedTextColor.AQUA
            ));
        }

        Map<UUID, String> activeBosses = bossManager.getActiveBossNames();
        sender.sendMessage(Component.text("=== Active Bosses ===", NamedTextColor.GOLD));
        if (activeBosses.isEmpty()) {
            sender.sendMessage(Component.text("No active bosses.", NamedTextColor.GRAY));
        } else {
            for (Map.Entry<UUID, String> entry : activeBosses.entrySet()) {
                sender.sendMessage(Component.text(entry.getKey() + " | " + entry.getValue(), NamedTextColor.RED));
            }
        }

        Map<String, String> configuredSpawns = bossManager.getConfiguredSpawns();
        sender.sendMessage(Component.text("=== Configured Spawns ===", NamedTextColor.GOLD));
        if (configuredSpawns.isEmpty()) {
            sender.sendMessage(Component.text("No configured boss spawns.", NamedTextColor.GRAY));
            return;
        }
        for (Map.Entry<String, String> entry : configuredSpawns.entrySet()) {
            sender.sendMessage(Component.text(entry.getKey() + " | " + entry.getValue(), NamedTextColor.YELLOW));
        }

        Map<String, String> configuredPortals = bossPortalManager.getConfiguredPortals();
        sender.sendMessage(Component.text("=== Boss Portals ===", NamedTextColor.GOLD));
        if (configuredPortals.isEmpty()) {
            sender.sendMessage(Component.text("No configured boss portals.", NamedTextColor.GRAY));
            return;
        }
        for (Map.Entry<String, String> entry : configuredPortals.entrySet()) {
            sender.sendMessage(Component.text(entry.getKey() + " | " + entry.getValue(), NamedTextColor.GOLD));
        }
    }

    private void handleRemembrance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can exchange remembrances.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("info")) {
            BossManager.RemembrancePreview preview = bossManager.inspectHeldRemembrance(player);
            if (preview == null) {
                sender.sendMessage(Component.text("Hold a remembrance in your main hand.", NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text("=== " + preview.remembranceName() + " ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Boss: " + preview.bossName(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Reward exchange: " + preview.rewardName(), NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Rune exchange: " + preview.runeValue() + " runes", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Use /boss remembrance exchange <reward|runes>", NamedTextColor.GRAY));
            return;
        }

        if (!args[1].equalsIgnoreCase("exchange")) {
            sender.sendMessage(Component.text("Usage: /boss remembrance <info|exchange>", NamedTextColor.YELLOW));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /boss remembrance exchange <reward|runes>", NamedTextColor.YELLOW));
            return;
        }

        BossManager.ExchangeResult result = bossManager.exchangeHeldRemembrance(player, args[2]);
        sender.sendMessage(Component.text(
                result.message(),
                result.success() ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }

    private void handleStation(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /boss station give [player]", NamedTextColor.YELLOW));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Usage from console: /boss station give <player>", NamedTextColor.YELLOW));
            return;
        }

        target.getInventory().addItem(remembranceStationService.createStationItem());
        sender.sendMessage(Component.text("Gave a Remembrance Station to " + target.getName() + ".", NamedTextColor.GREEN));
        if (target != sender) {
            target.sendMessage(Component.text("You received a Remembrance Station.", NamedTextColor.LIGHT_PURPLE));
        }
    }

    private void handleDespawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss despawn <all|uuid>", NamedTextColor.YELLOW));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int removed = bossManager.despawnAll();
            sender.sendMessage(Component.text("Despawned " + removed + " boss(es).", NamedTextColor.GREEN));
            return;
        }

        try {
            UUID entityId = UUID.fromString(args[1]);
            Entity entity = Bukkit.getEntity(entityId);
            if (entity == null && !bossManager.getActiveBossNames().containsKey(entityId)) {
                sender.sendMessage(Component.text("No active boss found for UUID: " + args[1], NamedTextColor.RED));
                return;
            }
            boolean removed = bossManager.despawnBoss(entityId, true);
            if (!removed) {
                sender.sendMessage(Component.text("No active boss found for UUID: " + args[1], NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text("Boss despawned.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Invalid UUID: " + args[1], NamedTextColor.RED));
        }
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can save boss spawns from their position.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /boss setspawn <name> <boss-id>", NamedTextColor.YELLOW));
            return;
        }

        BossDefinition definition = bossRegistry.getById(args[2]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("Unknown boss: " + args[2], NamedTextColor.RED));
            return;
        }

        boolean saved = bossManager.saveConfiguredSpawn(args[1], definition, player.getLocation());
        if (!saved) {
            sender.sendMessage(Component.text("Spawn point could not be saved.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Saved boss spawn '" + args[1] + "' for " + definition.displayName() + ".", NamedTextColor.GREEN));
    }

    private void handleRemoveSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss removespawn <name>", NamedTextColor.YELLOW));
            return;
        }

        boolean removed = bossManager.removeConfiguredSpawn(args[1]);
        if (!removed) {
            sender.sendMessage(Component.text("No configured spawn found with that name.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Removed configured boss spawn '" + args[1] + "'.", NamedTextColor.GREEN));
    }

    private void handleSetPortalEntrance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can create boss portal entrances from their position.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss setportalentrance <name>", NamedTextColor.YELLOW));
            return;
        }

        boolean saved = bossPortalManager.savePortalEntrance(args[1], player.getLocation());
        if (!saved) {
            sender.sendMessage(Component.text("Boss portal entrance could not be saved.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "Saved entrance portal '" + args[1] + "'.",
                NamedTextColor.GREEN
        ));
    }

    private void handleSetPortalExit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can create boss portal exits from their position.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss setportalexit <name>", NamedTextColor.YELLOW));
            return;
        }

        boolean saved = bossPortalManager.savePortalExit(args[1], player.getLocation());
        if (!saved) {
            sender.sendMessage(Component.text("Boss portal exit could not be saved.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "Saved destination portal '" + args[1] + "'.",
                NamedTextColor.GREEN
        ));
    }

    private void handleSetPortalExitLock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /boss setportalexitlock <name> <true|false>", NamedTextColor.YELLOW));
            return;
        }

        String value = args[2].toLowerCase(java.util.Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) {
            sender.sendMessage(Component.text("Use true or false.", NamedTextColor.RED));
            return;
        }

        boolean saved = bossPortalManager.setPortalExitLock(args[1], Boolean.parseBoolean(value));
        if (!saved) {
            sender.sendMessage(Component.text("Exit lock could not be updated for that portal.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "Exit lock for portal '" + args[1] + "' set to " + value + ".",
                NamedTextColor.GREEN
        ));
    }

    private void handleSetPortalBoss(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /boss setportalboss <name> <boss-id>", NamedTextColor.YELLOW));
            return;
        }

        BossDefinition definition = bossRegistry.getById(args[2]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("Unknown boss: " + args[2], NamedTextColor.RED));
            return;
        }

        boolean saved = bossPortalManager.assignBoss(args[1], definition);
        if (!saved) {
            sender.sendMessage(Component.text("Boss could not be assigned to the portal.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "Assigned boss '" + definition.displayName() + "' to portal '" + args[1] + "'.",
                NamedTextColor.GREEN
        ));
    }

    private void handleRemovePortal(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss removeportal <name>", NamedTextColor.YELLOW));
            return;
        }

        boolean removed = bossPortalManager.removePortal(args[1]);
        if (!removed) {
            sender.sendMessage(Component.text("No configured boss portal found with that name.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Removed boss portal '" + args[1] + "'.", NamedTextColor.GREEN));
    }

    private void handlePortalInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.boss.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /boss portalinfo <name>", NamedTextColor.YELLOW));
            return;
        }

        BossPortalManager.PortalInfo info = bossPortalManager.getPortalInfo(args[1]).orElse(null);
        if (info == null) {
            sender.sendMessage(Component.text("No boss portal found with that name.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Portal Info: " + info.name() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Boss: " + (info.bossId() == null ? "unset" : info.bossId()), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Ready: " + (info.ready() ? "yes" : "no"), info.ready() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text(
                "Exit Requires Boss Kill: " + (info.requireBossDeathToExit() ? "yes" : "no"),
                info.requireBossDeathToExit() ? NamedTextColor.RED : NamedTextColor.GREEN
        ));
        sender.sendMessage(Component.text("Entrance: " + formatLocation(info.entranceLocation()), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Exit Portal: " + formatLocation(info.exitLocation()), NamedTextColor.AQUA));
    }

    private String formatLocation(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return "unset";
        }
        return location.getWorld().getName()
                + " "
                + String.format("%.1f %.1f %.1f", location.getX(), location.getY(), location.getZ())
                + " yaw=" + String.format("%.1f", location.getYaw());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Boss Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/boss spawn <id> - Spawn a boss at your position", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss list - Show available and active bosses", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss station give [player] - Give a placeable remembrance station", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss remembrance info - Show the exchange options of the remembrance in your hand", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss remembrance exchange <reward|runes> - Consume the held remembrance", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss despawn <all|uuid> - Remove active bosses", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss setspawn <name> <boss-id> - Save your position as a persistent spawn", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss removespawn <name> - Remove a persistent boss spawn", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss setportalentrance <name> - Set the entry portal", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss setportalexit <name> - Set the destination portal inside the boss arena", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss setportalexitlock <name> <true|false> - Require boss death before leaving", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss setportalboss <name> <boss-id> - Assign the boss to the portal", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss portalinfo <name> - Show boss portal details", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/boss removeportal <name> - Remove a boss portal", NamedTextColor.YELLOW));
    }
}
