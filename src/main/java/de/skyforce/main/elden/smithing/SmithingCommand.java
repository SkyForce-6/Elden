package de.skyforce.main.elden.smithing;

import de.skyforce.main.elden.smithing.model.SmithingTrack;
import de.skyforce.main.elden.smithing.service.SmithingAnvilService;
import de.skyforce.main.elden.smithing.service.SmithingService;
import de.skyforce.main.elden.smithing.service.SmithingStoneService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SmithingCommand implements CommandExecutor {

    private final SmithingService smithingService;
    private final SmithingAnvilService smithingAnvilService;
    private final SmithingStoneService smithingStoneService;

    public SmithingCommand(SmithingService smithingService,
                           SmithingAnvilService smithingAnvilService,
                           SmithingStoneService smithingStoneService) {
        this.smithingService = smithingService;
        this.smithingAnvilService = smithingAnvilService;
        this.smithingStoneService = smithingStoneService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("elden.smithing.admin")) {
            sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "info" -> handleInfo(sender, args);
            case "anvil" -> handleAnvil(sender, args);
            case "stone" -> handleStone(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /smithing set <level> [player]", NamedTextColor.YELLOW));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("Smithing level must be a number.", NamedTextColor.RED));
            return;
        }

        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        String result = smithingService.applyUpgradeToMainHand(target, level);
        if (result != null) {
            sender.sendMessage(Component.text(result, NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Set " + target.getName() + "'s held weapon to +" + level + ".", NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 1);
        if (target == null) {
            return;
        }

        SmithingService.SmithingWeaponInfo info = smithingService.inspectMainHand(target);
        if (info == null) {
            sender.sendMessage(Component.text(target.getName() + " is not holding a custom weapon.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text(
                target.getName() + " holds " + info.displayName() + " at +" + info.smithingLevel()
                        + " (" + info.track().displayName() + ").",
                NamedTextColor.YELLOW));
        if (info.nextRequirement() != null) {
            sender.sendMessage(Component.text(
                    "Next: " + info.nextRequirement().runeCost() + " runes and "
                            + info.nextRequirement().stoneAmount() + "x "
                            + smithingStoneService.displayName(info.nextRequirement().track(), info.nextRequirement().stoneTier()),
                    NamedTextColor.GRAY));
        }
    }

    private void handleAnvil(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /smithing anvil give [player]", NamedTextColor.YELLOW));
            return;
        }

        Player target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        target.getInventory().addItem(smithingAnvilService.createAnvilItem());
        sender.sendMessage(Component.text("Gave a Smithing Anvil to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleStone(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /smithing stone give <standard|somber> <tier> [amount] [player]", NamedTextColor.YELLOW));
            return;
        }

        SmithingTrack track;
        if ("standard".equalsIgnoreCase(args[2])) {
            track = SmithingTrack.STANDARD;
        } else if ("somber".equalsIgnoreCase(args[2])) {
            track = SmithingTrack.SOMBER;
        } else {
            sender.sendMessage(Component.text("Track must be standard or somber.", NamedTextColor.RED));
            return;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("Tier must be a number.", NamedTextColor.RED));
            return;
        }

        if (tier < 1 || tier > (track == SmithingTrack.STANDARD ? 9 : 10)) {
            sender.sendMessage(Component.text("Tier is out of range for " + track.displayName() + " stones.", NamedTextColor.RED));
            return;
        }

        int amount = 1;
        int playerArgIndex = 4;
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
                playerArgIndex = 5;
            } catch (NumberFormatException ignored) {
                amount = 1;
                playerArgIndex = 4;
            }
        }

        Player target = resolveTarget(sender, args, playerArgIndex);
        if (target == null) {
            return;
        }

        target.getInventory().addItem(smithingStoneService.createStone(track, tier, amount));
        sender.sendMessage(Component.text(
                "Gave " + amount + "x " + smithingStoneService.displayName(track, tier) + " to " + target.getName() + ".",
                NamedTextColor.GREEN));
    }

    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player target = Bukkit.getPlayerExact(args[index]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[index], NamedTextColor.RED));
            }
            return target;
        }

        if (sender instanceof Player player) {
            return player;
        }

        sender.sendMessage(Component.text("Specify a target player.", NamedTextColor.RED));
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/smithing info [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/smithing set <level> [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/smithing anvil give [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/smithing stone give <standard|somber> <tier> [amount] [player]", NamedTextColor.YELLOW));
    }
}
