package de.skyforce.main.elden.weapon;

import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WeaponCommand implements CommandExecutor {

    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;

    public WeaponCommand(WeaponRegistry weaponRegistry, WeaponItemFactory weaponItemFactory) {
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Subcommands
    // -----------------------------------------------------------------------

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.weapon.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /weapon give <id> [player]", NamedTextColor.YELLOW));
            return;
        }

        String weaponId = args[1];
        WeaponDefinition weapon = weaponRegistry.getById(weaponId).orElse(null);
        if (weapon == null) {
            sender.sendMessage(Component.text("Unknown weapon: " + weaponId, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /weapon list for available weapons.", NamedTextColor.GRAY));
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
            sender.sendMessage(Component.text("Specify a target player.", NamedTextColor.RED));
            return;
        }

        target.getInventory().addItem(weaponItemFactory.createWeaponItem(weapon));
        target.sendMessage(Component.text("You received: " + weapon.displayName(), NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text(
                    "Gave " + weapon.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(Component.text("Available weapons:", NamedTextColor.YELLOW));
        for (WeaponDefinition weapon : weaponRegistry.all()) {
            sender.sendMessage(Component.text(
                    "  " + weapon.id() + " - " + weapon.displayName(),
                    NamedTextColor.GRAY));
        }
    }

    // -----------------------------------------------------------------------
    // Help
    // -----------------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/weapon give <id> [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/weapon list", NamedTextColor.YELLOW));
    }
}

