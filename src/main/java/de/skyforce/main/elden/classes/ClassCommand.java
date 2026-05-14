package de.skyforce.main.elden.classes;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ClassCommand implements CommandExecutor {

    private final ClassManager classManager;
    private final ClassGuiService classGuiService;

    public ClassCommand(ClassManager classManager, ClassGuiService classGuiService) {
        this.classManager = classManager;
        this.classGuiService = classGuiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("elden.class.use")) {
            player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            classGuiService.openClassMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("current")) {
            showCurrentClass(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "open" -> classGuiService.openClassMenu(player);
            case "list" -> showList(player);
            case "info" -> showInfo(player, args);
            case "choose" -> choose(player, args);
            case "reset" -> reset(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void showCurrentClass(Player player) {
        Optional<EldenClass> current = classManager.getPlayerClass(player);
        if (current.isEmpty()) {
            player.sendMessage(Component.text("No class selected yet. Use /eldenclass list and /eldenclass choose <name>", NamedTextColor.YELLOW));
            return;
        }

        EldenClass eldenClass = current.get();
        player.sendMessage(Component.text("Current class: " + eldenClass.displayName() + " (LVL " + eldenClass.level() + ")", NamedTextColor.GOLD));
    }

    private void showList(Player player) {
        String classes = java.util.Arrays.stream(EldenClass.values())
                .sorted(Comparator.comparing(EldenClass::displayName))
                .map(eldenClass -> eldenClass.displayName() + " (" + eldenClass.key() + ", lvl " + eldenClass.level() + ")")
                .collect(Collectors.joining(", "));

        player.sendMessage(Component.text("Available classes:", NamedTextColor.GOLD));
        player.sendMessage(Component.text(classes, NamedTextColor.YELLOW));
    }

    private void showInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /eldenclass info <name>", NamedTextColor.YELLOW));
            return;
        }

        EldenClass.byKey(args[1]).ifPresentOrElse(
                eldenClass -> {
                    player.sendMessage(Component.text(eldenClass.displayName() + " (LVL " + eldenClass.level() + ")", NamedTextColor.GOLD));
                    player.sendMessage(Component.text(
                            "VIG " + eldenClass.vig()
                                    + " | MND " + eldenClass.mnd()
                                    + " | END " + eldenClass.end()
                                    + " | STR " + eldenClass.str()
                                    + " | DEX " + eldenClass.dex()
                                    + " | INT " + eldenClass.intl()
                                    + " | FTH " + eldenClass.fth()
                                    + " | ARC " + eldenClass.arc(),
                            NamedTextColor.YELLOW));
                },
                () -> player.sendMessage(Component.text("Unknown class.", NamedTextColor.RED))
        );
    }

    private void choose(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /eldenclass choose <name>", NamedTextColor.YELLOW));
            return;
        }

        Optional<EldenClass> parsed = EldenClass.byKey(args[1]);
        if (parsed.isEmpty()) {
            player.sendMessage(Component.text("Unknown class.", NamedTextColor.RED));
            return;
        }

        EldenClass selected = parsed.get();
        if (!classManager.chooseClass(player, selected)) {
            player.sendMessage(Component.text("Class is already locked. Ask an admin or enable classes.allow-change in config.", NamedTextColor.RED));
            return;
        }

        classManager.saveAll();
        player.sendMessage(Component.text("Class selected: " + selected.displayName(), NamedTextColor.GREEN));
    }

    private void reset(Player player, String[] args) {
        if (!player.hasPermission("elden.class.admin")) {
            player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /eldenclass reset <player>", NamedTextColor.YELLOW));
            return;
        }

        Player target = player.getServer().getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player not online: " + args[1], NamedTextColor.RED));
            return;
        }

        boolean changed = classManager.resetClass(target);
        classManager.saveAll();

        if (changed) {
            player.sendMessage(Component.text("Class reset for " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your class was reset by an admin.", NamedTextColor.YELLOW));
            classGuiService.openClassMenu(target);
            return;
        }

        player.sendMessage(Component.text(target.getName() + " has no selected class.", NamedTextColor.GRAY));
        classGuiService.openClassMenu(target);
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("/eldenclass", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/eldenclass open", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/eldenclass current", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/eldenclass list", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/eldenclass info <name>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/eldenclass choose <name>", NamedTextColor.YELLOW));
        if (player.hasPermission("elden.class.admin")) {
            player.sendMessage(Component.text("/eldenclass reset <player>", NamedTextColor.YELLOW));
        }
    }
}

