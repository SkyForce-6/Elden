package de.skyforce.main.elden.compass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CompassBarService {

    private static final String PERMISSION = "elden.compass.use";

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> barsByPlayer = new HashMap<>();
    private BukkitTask updateTask;

    public CompassBarService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("compass.enabled", true)) {
            return;
        }

        stop();
        long interval = Math.max(1L, plugin.getConfig().getLong("compass.update-interval-ticks", 2L));
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickUpdate, 1L, interval);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (BossBar bar : barsByPlayer.values()) {
            bar.removeAll();
        }
        barsByPlayer.clear();
    }

    public void removePlayer(Player player) {
        removeBar(player.getUniqueId());
    }

    private void tickUpdate() {
        boolean showIntercardinal = plugin.getConfig().getBoolean("compass.show-intercardinal", true);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.hasPermission(PERMISSION)) {
                removeBar(player.getUniqueId());
                continue;
            }

            BossBar bar = barsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> createBar());
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }

            CompassDirection center = CompassDirection.fromYaw(player.getLocation().getYaw(), showIntercardinal);
            bar.setTitle(buildEldenStyleCompass(center, showIntercardinal));
            bar.setProgress(1.0D);
            bar.setVisible(true);
        }

        Iterator<Map.Entry<UUID, BossBar>> iterator = barsByPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BossBar> entry = iterator.next();
            if (plugin.getServer().getPlayer(entry.getKey()) == null) {
                entry.getValue().removeAll();
                iterator.remove();
            }
        }
    }

    private String buildEldenStyleCompass(CompassDirection center, boolean showIntercardinal) {
        if (showIntercardinal) {
            CompassDirection left3 = center.previous(true).previous(true).previous(true);
            CompassDirection left2 = center.previous(true).previous(true);
            CompassDirection left1 = center.previous(true);
            CompassDirection right1 = center.next(true);
            CompassDirection right2 = center.next(true).next(true);
            CompassDirection right3 = center.next(true).next(true).next(true);

            return "§8·  §7"
                    + left3.shortLabel()
                    + "  §8·  §7"
                    + left2.shortLabel()
                    + "  §8·  §f"
                    + left1.shortLabel()
                    + "  §8·  §6✦ "
                    + center.shortLabel()
                    + " ✦  §8·  §f"
                    + right1.shortLabel()
                    + "  §8·  §7"
                    + right2.shortLabel()
                    + "  §8·  §7"
                    + right3.shortLabel()
                    + "  §8·";
        }

        CompassDirection left2 = center.previous(false).previous(false);
        CompassDirection left1 = center.previous(false);
        CompassDirection right1 = center.next(false);
        CompassDirection right2 = center.next(false).next(false);

        return "§8·  §7"
                + left2.shortLabel()
                + "  §8·  §f"
                + left1.shortLabel()
                + "  §8·  §6✦ "
                + center.shortLabel()
                + " ✦  §8·  §f"
                + right1.shortLabel()
                + "  §8·  §7"
                + right2.shortLabel()
                + "  §8·";
    }

    private BossBar createBar() {
        BarColor color = parseColor(plugin.getConfig().getString("compass.bossbar.color", "YELLOW"));
        BarStyle style = parseStyle(plugin.getConfig().getString("compass.bossbar.style", "SOLID"));
        return plugin.getServer().createBossBar("§8·  §7W  §8·  §fNW  §8·  §6✦ N ✦  §8·  §fNE  §8·  §7E  §8·", color, style);
    }

    private void removeBar(UUID playerId) {
        BossBar bar = barsByPlayer.remove(playerId);
        if (bar != null) {
            bar.removeAll();
        }
    }

    private BarColor parseColor(String value) {
        try {
            return BarColor.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BarColor.YELLOW;
        }
    }

    private BarStyle parseStyle(String value) {
        try {
            return BarStyle.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return BarStyle.SOLID;
        }
    }
}