package de.skyforce.main.elden.classes;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClassListener implements Listener {

    private final JavaPlugin plugin;
    private final ClassManager classManager;
    private final ClassGuiService classGuiService;

    public ClassListener(JavaPlugin plugin, ClassManager classManager, ClassGuiService classGuiService) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.classGuiService = classGuiService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        classManager.assignDefaultIfMissing(player);

        Optional<EldenClass> chosenClass = classManager.getPlayerClass(player);
        if (chosenClass.isPresent()) {
            player.sendActionBar(Component.text("Class: " + chosenClass.get().displayName(), NamedTextColor.GOLD));
            return;
        }

        player.sendMessage(Component.text("Choose your class in the GUI.", NamedTextColor.YELLOW));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && classManager.getPlayerClass(player).isEmpty()) {
                classGuiService.openClassMenu(player);
            }
        }, 20L);
    }
}

