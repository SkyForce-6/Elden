package de.skyforce.main.elden.talisman;

import de.skyforce.main.elden.talisman.gui.TalismanMenu;
import de.skyforce.main.elden.talisman.model.TalismanDefinition;
import de.skyforce.main.elden.talisman.registry.TalismanRegistry;
import de.skyforce.main.elden.talisman.service.TalismanItemFactory;
import de.skyforce.main.elden.talisman.service.TalismanManager;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class TalismanCommand implements CommandExecutor {

    private final TalismanRegistry talismanRegistry;
    private final TalismanItemFactory talismanItemFactory;
    private final TalismanManager talismanManager;
    private final TalismanMenu talismanMenu;

    public TalismanCommand(TalismanRegistry talismanRegistry, TalismanItemFactory talismanItemFactory,
                           TalismanManager talismanManager, TalismanMenu talismanMenu) {
        this.talismanRegistry = talismanRegistry;
        this.talismanItemFactory = talismanItemFactory;
        this.talismanManager = talismanManager;
        this.talismanMenu = talismanMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("elden.talisman.use")) {
            player.sendMessage(text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            talismanMenu.open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> listTalismans(player);
            case "equip" -> equipHeld(player, args);
            case "unequip" -> unequip(player, args);
            case "give" -> give(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void listTalismans(Player player) {
        player.sendMessage(text("Talismans", NamedTextColor.GOLD));
        for (TalismanDefinition talisman : talismanRegistry.all()) {
            player.sendMessage(text("- " + talisman.id() + " | " + talisman.displayName(), NamedTextColor.YELLOW));
        }
    }

    private void equipHeld(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(text("Usage: /talisman equip <1-4>", NamedTextColor.RED));
            return;
        }
        int slot = parseSlot(args[1]);
        if (slot < 0) {
            player.sendMessage(text("Slot must be 1-4.", NamedTextColor.RED));
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        String talismanId = talismanItemFactory.getTalismanId(held);
        if (talismanId == null) {
            player.sendMessage(text("Hold a talisman in your main hand.", NamedTextColor.RED));
            return;
        }
        if (!talismanManager.equip(player, slot, talismanId)) {
            player.sendMessage(text("Could not equip that talisman. It may already be equipped.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(text("Equipped talisman in slot " + (slot + 1) + ".", NamedTextColor.GREEN));
    }

    private void unequip(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(text("Usage: /talisman unequip <1-4>", NamedTextColor.RED));
            return;
        }
        int slot = parseSlot(args[1]);
        if (slot < 0 || !talismanManager.unequip(player, slot)) {
            player.sendMessage(text("No talisman in that slot.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(text("Unequipped talisman slot " + (slot + 1) + ".", NamedTextColor.GREEN));
    }

    private void give(Player player, String[] args) {
        if (!player.hasPermission("elden.talisman.admin")) {
            player.sendMessage(text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(text("Usage: /talisman give <id> [player]", NamedTextColor.RED));
            return;
        }
        TalismanDefinition talisman = talismanRegistry.getById(args[1]).orElse(null);
        if (talisman == null) {
            player.sendMessage(text("Unknown talisman: " + args[1], NamedTextColor.RED));
            return;
        }
        Player target = args.length >= 3 ? player.getServer().getPlayerExact(args[2]) : player;
        if (target == null) {
            player.sendMessage(text("Player not found: " + args[2], NamedTextColor.RED));
            return;
        }
        target.getInventory().addItem(talismanItemFactory.createTalismanItem(talisman));
        player.sendMessage(text("Gave " + talisman.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private int parseSlot(String text) {
        try {
            int value = Integer.parseInt(text);
            return value >= 1 && value <= TalismanManager.SLOT_COUNT ? value - 1 : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(text("/talisman", NamedTextColor.YELLOW));
        player.sendMessage(text("/talisman list", NamedTextColor.YELLOW));
        player.sendMessage(text("/talisman equip <1-4>", NamedTextColor.YELLOW));
        player.sendMessage(text("/talisman unequip <1-4>", NamedTextColor.YELLOW));
        player.sendMessage(text("/talisman give <id> [player]", NamedTextColor.YELLOW));
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
