package de.skyforce.main.elden.grace;

import java.util.Arrays;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GraceCommand implements CommandExecutor {

    private final GraceManager graceManager;

    public GraceCommand(GraceManager graceManager) {
        this.graceManager = graceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set" -> handleSet(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player);
            case "activate" -> handleActivate(player, args);
            case "warp" -> handleWarp(player, args);
            case "info" -> handleInfo(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleSet(Player player, String[] args) {
        if (!player.hasPermission("elden.grace.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /grace set <name>");
            return;
        }

        String name = joinName(args, 1);
        Location location = player.getLocation().getBlock().getLocation().add(0.5, 0.0, 0.5);

        boolean success = graceManager.setGrace(name, location);
        if (!success) {
            player.sendMessage(ChatColor.RED + "Invalid grace name.");
            return;
        }

        graceManager.saveAll();
        player.sendMessage(ChatColor.GOLD + "Site of Grace set: " + ChatColor.YELLOW + name.toLowerCase());
    }

    private void handleRemove(Player player, String[] args) {
        if (!player.hasPermission("elden.grace.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /grace remove <name>");
            return;
        }

        String name = joinName(args, 1);
        boolean removed = graceManager.removeGrace(name);
        if (!removed) {
            player.sendMessage(ChatColor.RED + "Grace not found.");
            return;
        }

        graceManager.saveAll();
        player.sendMessage(ChatColor.GREEN + "Grace removed: " + name.toLowerCase());
    }

    private void handleList(Player player) {
        if (!player.hasPermission("elden.grace.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        if (graceManager.getGraceNames().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "There are no graces set yet.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "- Sites of Grace -");
        for (String line : graceManager.formatGraceList()) {
            player.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + line);
        }
    }

    private void handleActivate(Player player, String[] args) {
        if (!player.hasPermission("elden.grace.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /grace activate <name>");
            return;
        }

        String name = joinName(args, 1);
        if (graceManager.getGrace(name).isEmpty()) {
            player.sendMessage(ChatColor.RED + "Grace not found.");
            return;
        }

        graceManager.activateGrace(player, name);
        graceManager.saveAll();
        player.sendMessage(ChatColor.GOLD + "Grace attuned: " + ChatColor.YELLOW + name.toLowerCase());
    }

    private void handleWarp(Player player, String[] args) {
        if (!player.hasPermission("elden.grace.warp")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /grace warp <name>");
            return;
        }

        String name = joinName(args, 1);
        Optional<Location> graceLoc = graceManager.getGrace(name);
        if (graceLoc.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Grace not found.");
            return;
        }

        player.teleport(graceLoc.get());
        player.sendMessage(ChatColor.GREEN + "Warped to grace: " + name.toLowerCase());
    }

    private void handleInfo(Player player) {
        if (!player.hasPermission("elden.grace.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        Optional<String> active = graceManager.getActiveGraceName(player);
        if (active.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "You have not attuned any grace yet.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Active Grace: " + ChatColor.YELLOW + active.get());
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "Grace Commands:");
        player.sendMessage(ChatColor.YELLOW + "Right-click a registered Site of Grace" + ChatColor.GRAY + " - Open Grace GUI");
        player.sendMessage(ChatColor.YELLOW + "/grace list" + ChatColor.GRAY + " - Show all graces");
        player.sendMessage(ChatColor.YELLOW + "/grace activate <name>" + ChatColor.GRAY + " - Attune a grace");
        player.sendMessage(ChatColor.YELLOW + "/grace info" + ChatColor.GRAY + " - Show active grace");

        if (player.hasPermission("elden.grace.warp")) {
            player.sendMessage(ChatColor.YELLOW + "/grace warp <name>" + ChatColor.GRAY + " - Warp to a grace");
        }

        if (player.hasPermission("elden.grace.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/grace set <name>" + ChatColor.GRAY + " - Create a grace");
            player.sendMessage(ChatColor.YELLOW + "/grace remove <name>" + ChatColor.GRAY + " - Remove a grace");
        }
    }

    private String joinName(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
    }
}