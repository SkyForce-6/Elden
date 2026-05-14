package de.skyforce.main.elden.spiritspring;

import java.util.Arrays;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpiritSpringCommand implements CommandExecutor {

    private final SpiritSpringManager spiritSpringManager;

    public SpiritSpringCommand(SpiritSpringManager spiritSpringManager) {
        this.spiritSpringManager = spiritSpringManager;
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

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(player, args);
            case "remove" -> handleRemove(player, args);
            case "rename" -> handleRename(player, args);
            case "tp" -> handleTp(player, args);
            case "reload" -> handleReload(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player);
            case "nearest" -> handleNearest(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleSet(Player player, String[] args) {
        if (!player.hasPermission("elden.spiritspring.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /spiritspring set <name>");
            return;
        }

        String name = joinName(args, 1);
        if (!spiritSpringManager.setSpiritSpring(name, player.getLocation())) {
            player.sendMessage(ChatColor.RED + "Invalid spirit spring name.");
            return;
        }

        spiritSpringManager.saveAll();
        player.sendMessage(ChatColor.AQUA + "Spirit spring set: " + ChatColor.WHITE + name.toLowerCase());
    }

    private void handleRemove(Player player, String[] args) {
        if (!player.hasPermission("elden.spiritspring.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /spiritspring remove <name>");
            return;
        }

        String name = joinName(args, 1);
        if (!spiritSpringManager.removeSpiritSpring(name)) {
            player.sendMessage(ChatColor.RED + "Spirit spring not found.");
            return;
        }

        spiritSpringManager.saveAll();
        player.sendMessage(ChatColor.GREEN + "Spirit spring removed: " + name.toLowerCase());
    }

    private void handleList(Player player) {
        if (!player.hasPermission("elden.spiritspring.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        if (spiritSpringManager.getSpiritSpringNames().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "There are no spirit springs set yet.");
            return;
        }

        player.sendMessage(ChatColor.AQUA + "- Spirit Springs -");
        for (String line : spiritSpringManager.formatSpiritSpringList()) {
            player.sendMessage(ChatColor.WHITE + "- " + ChatColor.GRAY + line);
        }
    }

    private void handleInfo(Player player) {
        if (!player.hasPermission("elden.spiritspring.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        Optional<SpiritSpring> spring = spiritSpringManager.findActiveSpiritSpring(player.getLocation());
        if (spring.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No spirit spring is nearby.");
            return;
        }

        Location loc = spring.get().location();
        player.sendMessage(ChatColor.AQUA + "Nearby Spirit Spring: " + ChatColor.WHITE + spring.get().displayName());
        player.sendMessage(ChatColor.GRAY + "Location: "
                + (loc.getWorld() == null ? "unknown" : loc.getWorld().getName()) + " "
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }

    private void handleNearest(Player player) {
        if (!player.hasPermission("elden.spiritspring.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        Optional<SpiritSpring> spring = spiritSpringManager.findNearestAny(player.getLocation());
        if (spring.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No spirit springs exist in this world.");
            return;
        }

        Location loc = spring.get().location();
        double dist = player.getLocation().distance(loc);
        player.sendMessage(ChatColor.AQUA + "Nearest Spirit Spring: " + ChatColor.WHITE + spring.get().displayName());
        player.sendMessage(ChatColor.GRAY + "Location: "
                + (loc.getWorld() == null ? "unknown" : loc.getWorld().getName()) + " "
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + ChatColor.DARK_GRAY + " (" + String.format("%.1f", dist) + " blocks away)");
    }

    private void handleRename(Player player, String[] args) {
        if (!player.hasPermission("elden.spiritspring.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /spiritspring rename <oldname> <newname>");
            return;
        }

        String oldName = args[1];
        String newName = joinName(args, 2);

        if (!spiritSpringManager.renameSpiritSpring(oldName, newName)) {
            player.sendMessage(ChatColor.RED + "Could not rename: spring not found or new name is already taken.");
            return;
        }

        spiritSpringManager.saveAll();
        player.sendMessage(ChatColor.GREEN + "Renamed spirit spring "
                + ChatColor.WHITE + oldName.toLowerCase()
                + ChatColor.GREEN + " → "
                + ChatColor.WHITE + newName.toLowerCase());
    }

    private void handleTp(Player player, String[] args) {
        if (!player.hasPermission("elden.spiritspring.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /spiritspring tp <name>");
            return;
        }

        String name = joinName(args, 1);
        Optional<SpiritSpring> spring = spiritSpringManager.getSpiritSpring(name);
        if (spring.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Spirit spring not found: " + name.toLowerCase());
            return;
        }

        Location dest = spring.get().location().clone().add(0.0D, 0.5D, 0.0D);
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());
        player.teleport(dest);
        player.sendMessage(ChatColor.AQUA + "Teleported to spring: " + ChatColor.WHITE + spring.get().displayName());
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("elden.spiritspring.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        spiritSpringManager.reloadSprings();
        player.sendMessage(ChatColor.GREEN + "Spirit springs reloaded from config. ("
                + spiritSpringManager.getSpiritSpringNames().size() + " loaded)");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "Spirit Spring Commands:");
        player.sendMessage(ChatColor.YELLOW + "/spiritspring list" + ChatColor.GRAY + " - Show all spirit springs");
        player.sendMessage(ChatColor.YELLOW + "/spiritspring info" + ChatColor.GRAY + " - Show the nearest spirit spring");
        player.sendMessage(ChatColor.YELLOW + "/spiritspring nearest" + ChatColor.GRAY + " - Find the closest spring in the world");
        if (player.hasPermission("elden.spiritspring.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/spiritspring set <name>" + ChatColor.GRAY + " - Create a spirit spring");
            player.sendMessage(ChatColor.YELLOW + "/spiritspring remove <name>" + ChatColor.GRAY + " - Remove a spirit spring");
            player.sendMessage(ChatColor.YELLOW + "/spiritspring rename <old> <new>" + ChatColor.GRAY + " - Rename a spirit spring");
            player.sendMessage(ChatColor.YELLOW + "/spiritspring tp <name>" + ChatColor.GRAY + " - Teleport to a spirit spring");
            player.sendMessage(ChatColor.YELLOW + "/spiritspring reload" + ChatColor.GRAY + " - Reload springs from config");
        }
    }

    private String joinName(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
    }
}
