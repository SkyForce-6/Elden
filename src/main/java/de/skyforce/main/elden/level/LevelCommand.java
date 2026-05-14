package de.skyforce.main.elden.level;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LevelCommand implements CommandExecutor {

    private final LevelManager levelManager;
    private final LevelGuiService levelGuiService;

    public LevelCommand(LevelManager levelManager, LevelGuiService levelGuiService) {
        this.levelManager = levelManager;
        this.levelGuiService = levelGuiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("elden.level.use")) {
            player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            levelGuiService.openLevelMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("up")) {
            levelUp(player, args);
            return true;
        }

        sendHelp(player);
        return true;
    }


    private void levelUp(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /level up <vig|mnd|end|str|dex|int|fth|arc>", NamedTextColor.YELLOW));
            return;
        }

        Optional<AttributeType> attributeType = AttributeType.fromInput(args[1]);
        if (attributeType.isEmpty()) {
            player.sendMessage(Component.text("Unknown attribute.", NamedTextColor.RED));
            return;
        }

        Optional<String> error = levelManager.levelUp(player, attributeType.get());
        if (error.isPresent()) {
            player.sendMessage(Component.text(error.get(), NamedTextColor.RED));
            return;
        }

        PlayerProgress progress = levelManager.getOrCreate(player);
        player.sendMessage(Component.text(
                "Level up successful: " + attributeType.get().displayName() + " is now " + progress.attribute(attributeType.get())
                        + " (Level " + progress.level() + ")",
                NamedTextColor.GREEN));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("/level", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/level info", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/level up <attribute>", NamedTextColor.YELLOW));
    }
}

