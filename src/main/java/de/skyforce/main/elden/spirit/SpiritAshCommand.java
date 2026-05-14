package de.skyforce.main.elden.spirit;

import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import de.skyforce.main.elden.spirit.service.SpiritAshItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpiritAshCommand implements CommandExecutor {

    private final SpiritAshRegistry spiritAshRegistry;
    private final SpiritAshItemFactory spiritAshItemFactory;

    public SpiritAshCommand(SpiritAshRegistry spiritAshRegistry, SpiritAshItemFactory spiritAshItemFactory) {
        this.spiritAshRegistry = spiritAshRegistry;
        this.spiritAshItemFactory = spiritAshItemFactory;
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
        if (!sender.hasPermission("elden.spirit.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /spiritash give <id> [player]", NamedTextColor.YELLOW));
            return;
        }

        SpiritAshDefinition spiritAsh = spiritAshRegistry.getById(args[1]).orElse(null);
        if (spiritAsh == null) {
            sender.sendMessage(Component.text("Unknown Spirit Ash: " + args[1], NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /spiritash list for available Spirit Ashes.", NamedTextColor.GRAY));
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

        target.getInventory().addItem(spiritAshItemFactory.createSpiritAshItem(spiritAsh));
        target.sendMessage(Component.text("You received: " + spiritAsh.displayName(), NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text("Gave " + spiritAsh.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("elden.spirit.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Available Spirit Ashes ===", NamedTextColor.GOLD));
        for (SpiritAshDefinition spiritAsh : spiritAshRegistry.getAll()) {
            sender.sendMessage(Component.text("  - " + spiritAsh.id() + " | " + spiritAsh.displayName(), NamedTextColor.AQUA));
            sender.sendMessage(Component.text(
                    "    FP " + formatNumber(spiritAsh.fpCost()) + " | " + spiritAsh.cooldownTicks() + "t cooldown | " + spiritAsh.location(),
                    NamedTextColor.GRAY
            ));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Spirit Ash Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/spiritash give <id> [player] - Give a Spirit Ash", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/spiritash list - Show all Spirit Ashes", NamedTextColor.YELLOW));
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
