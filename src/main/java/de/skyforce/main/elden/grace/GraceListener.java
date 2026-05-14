package de.skyforce.main.elden.grace;

import de.skyforce.main.elden.flask.FlaskService;
import de.skyforce.main.elden.enemy.EnemySpawnerManager;
import de.skyforce.main.elden.visual.VisualEffectService;
import java.util.Optional;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class GraceListener implements Listener {

    private final GraceManager graceManager;
    private final GraceGuiService graceGuiService;
    private final FlaskService flaskService;
    private final EnemySpawnerManager enemySpawnerManager;
    private final VisualEffectService visualEffectService;

    public GraceListener(GraceManager graceManager,
                         GraceGuiService graceGuiService,
                         FlaskService flaskService,
                         EnemySpawnerManager enemySpawnerManager,
                         VisualEffectService visualEffectService) {
        this.graceManager = graceManager;
        this.graceGuiService = graceGuiService;
        this.flaskService = flaskService;
        this.enemySpawnerManager = enemySpawnerManager;
        this.visualEffectService = visualEffectService;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Location> activeGrace = graceManager.getActiveGraceLocation(player);

        activeGrace.ifPresent(location -> {
            event.setRespawnLocation(location);
            player.sendMessage(Component.text("You are reborn at your Site of Grace.", NamedTextColor.GOLD));
        });
    }

    @EventHandler
    public void onCampfireInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        Material type = clicked.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("elden.grace.gui")) {
            player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            return;
        }

        Optional<String> nearestGrace = graceManager.getNearestGrace(
                clicked.getLocation(),
                graceManager.getActivationRadius()
        );

        if (nearestGrace.isEmpty()) {
            return;
        }

        String graceKey = nearestGrace.get();
        String displayName = graceManager.getGraceDisplayName(graceKey).orElse(graceKey);
        boolean newlyDiscovered = !graceManager.hasDiscoveredGrace(player, graceKey);

        if (newlyDiscovered) {
            graceManager.discoverGrace(player, graceKey);
            visualEffectService.playGraceDiscovery(clicked.getLocation(), player);

            player.showTitle(Title.title(
                    Component.text("Site of Grace discovered", NamedTextColor.GOLD),
                    Component.text(displayName, NamedTextColor.YELLOW)
            ));
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.2f);
        } else {
            visualEffectService.playGraceActivation(clicked.getLocation(), player);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
        }

        graceManager.activateGrace(player, graceKey);
        graceManager.saveAll();
        flaskService.refillFlasks(player);
        if (enemySpawnerManager != null) {
            enemySpawnerManager.resetGraceSpawners();
        }

        event.setCancelled(true);
        graceGuiService.openMainMenu(player, graceKey);
    }
}
