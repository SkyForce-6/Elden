package de.skyforce.main.elden.runes;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RuneCommand implements CommandExecutor {

    private final RuneManager runeManager;

    public RuneCommand(RuneManager runeManager) {
        this.runeManager = runeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("elden.runes.use")) {
            player.sendMessage(text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            int runes = runeManager.getRunes(player);
            player.sendMessage(text("✦ Runes held: " + runes, NamedTextColor.GOLD));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!sub.equals("add") && !sub.equals("set") && !sub.equals("remove")) {
            sendHelp(player);
            return true;
        }

        if (!player.hasPermission("elden.runes.admin")) {
            player.sendMessage(text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sendHelp(player);
            return true;
        }

        Player target = player.getServer().getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(text("Player not found or not online: " + args[1], NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(text("Please enter a valid number.", NamedTextColor.RED));
            return true;
        }

        if (amount < 0) {
            player.sendMessage(text("Amount must be 0 or higher.", NamedTextColor.RED));
            return true;
        }

        int value;

        switch (sub) {
            case "add" -> {
                value = runeManager.addRunes(target.getUniqueId(), amount);
                player.sendMessage(text("✦ Added runes to " + target.getName() + ": " + value, NamedTextColor.GREEN));
                target.sendMessage(text("✦ Your runes increased to: " + value, NamedTextColor.YELLOW));
            }

            case "set" -> {
                runeManager.setRunes(target.getUniqueId(), amount);
                value = runeManager.getRunes(target);
                player.sendMessage(text("✦ Set runes for " + target.getName() + ": " + value, NamedTextColor.GREEN));
                target.sendMessage(text("✦ Your runes are now: " + value, NamedTextColor.YELLOW));
            }

            case "remove" -> {
                value = runeManager.removeRunes(target.getUniqueId(), amount);
                player.sendMessage(text("✦ Removed runes from " + target.getName() + ": " + value, NamedTextColor.GREEN));
                target.sendMessage(text("✦ Your runes were reduced to: " + value, NamedTextColor.YELLOW));
            }

            default -> {
                sendHelp(player);
                return true;
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(text("Runes Command", NamedTextColor.GOLD));
        player.sendMessage(text("/runes", NamedTextColor.YELLOW));
        player.sendMessage(text("/runes add <player> <amount>", NamedTextColor.YELLOW));
        player.sendMessage(text("/runes set <player> <amount>", NamedTextColor.YELLOW));
        player.sendMessage(text("/runes remove <player> <amount>", NamedTextColor.YELLOW));
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}