package de.skyforce.main.elden.spirit;

import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class SpiritAshTabCompleter implements TabCompleter {

    private final SpiritAshRegistry spiritAshRegistry;

    public SpiritAshTabCompleter(SpiritAshRegistry spiritAshRegistry) {
        this.spiritAshRegistry = spiritAshRegistry;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("give", "list"), args[0]);
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            List<String> ids = new ArrayList<>();
            for (SpiritAshDefinition spiritAsh : spiritAshRegistry.getAll()) {
                ids.add(spiritAsh.id());
            }
            return filter(ids, args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
            return filter(names, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
