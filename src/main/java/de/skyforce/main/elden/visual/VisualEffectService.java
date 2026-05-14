package de.skyforce.main.elden.visual;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class VisualEffectService {

    private static final Particle.DustOptions GRACE_GOLD = new Particle.DustOptions(Color.fromRGB(255, 202, 54), 1.45F);
    private static final Particle.DustOptions GRACE_AMBER = new Particle.DustOptions(Color.fromRGB(255, 132, 32), 1.05F);
    private static final Particle.DustOptions DODGE_ASH = new Particle.DustOptions(Color.fromRGB(170, 178, 190), 0.85F);
    private static final Particle.DustOptions LEVEL_RUNE = new Particle.DustOptions(Color.fromRGB(113, 255, 143), 1.2F);
    private static final Particle.DustOptions FLASK_CRIMSON = new Particle.DustOptions(Color.fromRGB(224, 42, 58), 1.25F);
    private static final Particle.DustOptions FLASK_CERULEAN = new Particle.DustOptions(Color.fromRGB(64, 152, 255), 1.25F);

    private final JavaPlugin plugin;

    public VisualEffectService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playGraceDiscovery(Location graceLocation, Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.enabled", true)
                || !plugin.getConfig().getBoolean("visual-effects.grace.discovery", true)) {
            return;
        }

        Location center = graceLocation.clone().add(0.5D, 0.35D, 0.5D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 1.1F, 0.8F);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 0.75F, 1.45F);

        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (tick > 34 || !player.isOnline()) {
                    cancel();
                    return;
                }

                double rise = tick * 0.045D;
                double radius = 0.55D + tick * 0.018D;
                for (int i = 0; i < 10; i++) {
                    double angle = tick * 0.42D + i * Math.PI * 0.2D;
                    Location point = center.clone().add(Math.cos(angle) * radius, rise + i * 0.035D, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, i % 2 == 0 ? GRACE_GOLD : GRACE_AMBER);
                }

                if (tick % 4 == 0) {
                    world.spawnParticle(Particle.END_ROD, center.clone().add(0.0D, rise + 0.45D, 0.0D), 5, 0.22D, 0.12D, 0.22D, 0.02D);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0.0D, 0.5D, 0.0D), 4, 0.45D, 0.35D, 0.45D, 0.01D);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void playGraceActivation(Location graceLocation, Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.enabled", true)
                || !plugin.getConfig().getBoolean("visual-effects.grace.activation", true)) {
            return;
        }

        Location center = graceLocation.clone().add(0.5D, 0.25D, 0.5D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.MASTER, 0.8F, 1.4F);
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16.0D;
            double radius = 1.15D;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.05D, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, GRACE_GOLD);
        }
        world.spawnParticle(Particle.WAX_ON, center.clone().add(0.0D, 0.65D, 0.0D), 18, 0.55D, 0.35D, 0.55D, 0.02D);
    }

    public void playDodgeTrail(Player player, Vector direction) {
        if (!plugin.getConfig().getBoolean("visual-effects.enabled", true)
                || !plugin.getConfig().getBoolean("visual-effects.dodge-trail", true)) {
            return;
        }

        Vector flatDirection = direction.clone();
        flatDirection.setY(0.0D);
        if (flatDirection.lengthSquared() < 0.0001D) {
            flatDirection = new Vector(0, 0, 1);
        }
        flatDirection.normalize();

        Location start = player.getLocation().clone().add(0.0D, 0.25D, 0.0D);
        World world = start.getWorld();
        if (world == null) {
            return;
        }

        Vector backward = flatDirection.clone().multiply(-0.18D);
        Vector side = new Vector(-flatDirection.getZ(), 0.0D, flatDirection.getX()).multiply(0.42D);
        world.playSound(start, Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.PLAYERS, 0.65F, 1.7F);

        for (int i = 0; i < 8; i++) {
            Location point = start.clone().add(backward.clone().multiply(i));
            world.spawnParticle(Particle.CLOUD, point, 2, 0.13D, 0.06D, 0.13D, 0.015D);
            world.spawnParticle(Particle.DUST, point.clone().add(side.clone().multiply(i % 2 == 0 ? 1.0D : -1.0D)), 1,
                    0.0D, 0.0D, 0.0D, 0.0D, DODGE_ASH);
        }
    }

    public void playLevelUp(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.enabled", true)
                || !plugin.getConfig().getBoolean("visual-effects.level-up", true)) {
            return;
        }

        Location center = player.getLocation().clone().add(0.0D, 0.15D, 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0.0D, 1.0D, 0.0D), 28, 0.45D, 0.65D, 0.45D, 0.02D);
        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8F, 1.65F);

        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12.0D;
            Location point = center.clone().add(Math.cos(angle) * 0.85D, 0.08D, Math.sin(angle) * 0.85D);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, LEVEL_RUNE);
        }
    }

    public void playCrimsonFlask(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.enabled", true)
                || !plugin.getConfig().getBoolean("visual-effects.flasks.crimson", true)) {
            return;
        }

        Location center = player.getLocation().clone().add(0.0D, 0.35D, 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.65F, 1.45F);
        world.spawnParticle(Particle.HEART, center.clone().add(0.0D, 1.15D, 0.0D), 3, 0.28D, 0.2D, 0.28D, 0.01D);
        world.spawnParticle(Particle.WAX_ON, center.clone().add(0.0D, 0.8D, 0.0D), 18, 0.45D, 0.55D, 0.45D, 0.03D);

        for (int i = 0; i < 30; i++) {
            double angle = i * Math.PI / 15.0D;
            double radius = 0.55D + (i % 3) * 0.16D;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.15D + (i % 5) * 0.12D, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, FLASK_CRIMSON);
        }
    }

    public void playCeruleanFlask(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.enabled", true)
                || !plugin.getConfig().getBoolean("visual-effects.flasks.cerulean", true)) {
            return;
        }

        Location center = player.getLocation().clone().add(0.0D, 0.25D, 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.75F, 1.65F);
        world.spawnParticle(Particle.ENCHANT, center.clone().add(0.0D, 1.0D, 0.0D), 34, 0.45D, 0.55D, 0.45D, 0.02D);

        for (int i = 0; i < 36; i++) {
            double angle = i * Math.PI / 9.0D;
            double radius = 0.45D + i * 0.012D;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.18D + i * 0.028D, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, FLASK_CERULEAN);
            if (i % 6 == 0) {
                world.spawnParticle(Particle.END_ROD, point, 1, 0.06D, 0.06D, 0.06D, 0.01D);
            }
        }
    }
}
