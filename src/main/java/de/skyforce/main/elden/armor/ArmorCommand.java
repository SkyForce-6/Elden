package de.skyforce.main.elden.armor;

import de.skyforce.main.elden.armor.model.ArmorDefinition;
import de.skyforce.main.elden.armor.registry.ArmorRegistry;
import de.skyforce.main.elden.armor.service.ArmorItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ArmorCommand implements CommandExecutor {

    private final ArmorRegistry armorRegistry;
    private final ArmorItemFactory armorItemFactory;

    public ArmorCommand(ArmorRegistry armorRegistry, ArmorItemFactory armorItemFactory) {
        this.armorRegistry = armorRegistry;
        this.armorItemFactory = armorItemFactory;
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

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elden.armor.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /armor give <id> [player]", NamedTextColor.YELLOW));
            return;
        }

        String armorId = args[1];
        ArmorDefinition armor = armorRegistry.getById(armorId).orElse(null);
        if (armor == null) {
            sender.sendMessage(Component.text("Unknown armor: " + armorId, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /armor list for available armor pieces.", NamedTextColor.GRAY));
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

        target.getInventory().addItem(armorItemFactory.createArmorItem(armor));
        target.sendMessage(Component.text("You received: " + armor.displayName(), NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text(
                    "Gave " + armor.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(Component.text("Available armor pieces:", NamedTextColor.YELLOW));
        for (ArmorDefinition armor : armorRegistry.all()) {
            sender.sendMessage(Component.text(
                    "  " + armor.id() + " - " + armor.displayName(),
                    NamedTextColor.GRAY));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/armor give <id> [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/armor list", NamedTextColor.YELLOW));
    }
}
