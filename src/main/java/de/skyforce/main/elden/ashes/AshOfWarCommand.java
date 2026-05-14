package de.skyforce.main.elden.ashes;

import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AshOfWarCommand implements CommandExecutor {

    private final AshOfWarRegistry ashRegistry;
    private final AshOfWarItemFactory ashItemFactory;

    public AshOfWarCommand(AshOfWarRegistry ashRegistry, AshOfWarItemFactory ashItemFactory) {
        this.ashRegistry = ashRegistry;
        this.ashItemFactory = ashItemFactory;
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
        if (!sender.hasPermission("elden.ash.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ash give <id> [player]", NamedTextColor.YELLOW));
            return;
        }

        String ashId = args[1];
        AshOfWarDefinition ash = ashRegistry.getById(ashId).orElse(null);
        if (ash == null) {
            sender.sendMessage(Component.text("Unknown Ash of War: " + ashId, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /ash list for available ashes.", NamedTextColor.GRAY));
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

        target.getInventory().addItem(ashItemFactory.createAshOfWarItem(ash));
        target.sendMessage(Component.text("You received: " + ash.displayName(), NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text(
                    "Gave " + ash.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("elden.ash.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Available Ashes of War ===", NamedTextColor.GOLD));
        Map<String, AshOfWarDefinition> ashes = ashRegistry.getAll();

        for (AshOfWarDefinition ash : ashes.values()) {
            sender.sendMessage(Component.text("  - " + ash.id() + " - " + ash.displayName(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("    Affinity: " + ash.affinity(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("    Type: " + ash.weaponType(), NamedTextColor.GRAY));
        }

        sender.sendMessage(Component.text("Use /ash give <id> [player] to give an ash of war.", NamedTextColor.GRAY));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Ash of War Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ash give <id> [player] - Give an Ash of War", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/ash list - Show all Ashes of War", NamedTextColor.YELLOW));
    }
}
