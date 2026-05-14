package de.skyforce.main.elden.spell;

import de.skyforce.main.elden.spell.model.SpellDefinition;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpellCommand implements CommandExecutor {

    private final SpellRegistry spellRegistry;
    private final SpellItemFactory spellItemFactory;

    public SpellCommand(SpellRegistry spellRegistry, SpellItemFactory spellItemFactory) {
        this.spellRegistry = spellRegistry;
        this.spellItemFactory = spellItemFactory;
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
        if (!sender.hasPermission("elden.spell.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /spell give <id> [player]", NamedTextColor.YELLOW));
            return;
        }

        SpellDefinition spell = spellRegistry.getById(args[1]).orElse(null);
        if (spell == null) {
            sender.sendMessage(Component.text("Unknown spell: " + args[1], NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /spell list for available spells.", NamedTextColor.GRAY));
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

        target.getInventory().addItem(spellItemFactory.createSpellItem(spell));
        target.sendMessage(Component.text("You received: " + spell.displayName(), NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            sender.sendMessage(Component.text("Gave " + spell.displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("elden.spell.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Available Spells ===", NamedTextColor.GOLD));
        for (SpellDefinition spell : spellRegistry.getAll()) {
            sender.sendMessage(Component.text("  - " + spell.id() + " | " + spell.displayName(), NamedTextColor.AQUA));
            sender.sendMessage(Component.text(
                    "    " + spell.school().displayName() + " | " + spell.tradition() + " | " + requirementLine(spell),
                    NamedTextColor.GRAY
            ));
        }
    }

    private String requirementLine(SpellDefinition spell) {
        String line = spell.primaryRequirementAttribute().displayName() + " " + spell.primaryRequirementLevel();
        if (spell.hasSecondaryRequirement()) {
            line += ", " + spell.secondaryRequirementAttribute().displayName() + " " + spell.secondaryRequirementLevel();
        }
        return line;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Spell Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/spell give <id> [player] - Give a spell tome", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/spell list - Show all spells", NamedTextColor.YELLOW));
    }
}
