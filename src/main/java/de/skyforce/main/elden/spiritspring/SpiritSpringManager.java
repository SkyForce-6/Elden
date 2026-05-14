package de.skyforce.main.elden.spiritspring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class SpiritSpringManager {

    private static final Particle.DustOptions CORE_DUST = new Particle.DustOptions(Color.fromRGB(150, 244, 255), 1.5F);
    private static final Particle.DustOptions OUTER_DUST = new Particle.DustOptions(Color.fromRGB(205, 255, 255), 0.95F);
    private static final Particle.DustOptions MIST_DUST = new Particle.DustOptions(Color.fromRGB(118, 214, 242), 0.85F);

    private final JavaPlugin plugin;
    private final Map<String, SpiritSpring> spiritSprings = new HashMap<>();
    private BukkitTask ambientTask;
    private long ambientTick;

    public SpiritSpringManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadSpiritSprings();
        startAmbientTask();
    }

    public boolean setSpiritSpring(String name, Location location) {
        String key = normalize(name);
        if (key.isBlank() || location == null || location.getWorld() == null) {
            return false;
        }

        Location centered = location.getBlock().getLocation().add(0.5D, 0.0D, 0.5D);
        SpiritSpring spring = new SpiritSpring(key, toDisplayName(key), centered);
        spiritSprings.put(key, spring);
        playCreationBurst(spring);
        return true;
    }

    public boolean removeSpiritSpring(String name) {
        SpiritSpring removed = spiritSprings.remove(normalize(name));
        if (removed != null) {
            playRemovalBurst(removed);
            return true;
        }
        return false;
    }

    public boolean renameSpiritSpring(String oldName, String newName) {
        String oldKey = normalize(oldName);
        String newKey = normalize(newName);
        if (oldKey.isBlank() || newKey.isBlank()) {
            return false;
        }
        SpiritSpring existing = spiritSprings.get(oldKey);
        if (existing == null) {
            return false;
        }
        // Don't overwrite a different spring with the new name
        if (spiritSprings.containsKey(newKey) && !oldKey.equals(newKey)) {
            return false;
        }
        spiritSprings.remove(oldKey);
        SpiritSpring renamed = new SpiritSpring(newKey, toDisplayName(newKey), existing.location());
        spiritSprings.put(newKey, renamed);
        return true;
    }

    public void reloadSprings() {
        loadSpiritSprings();
    }

    public Optional<SpiritSpring> getSpiritSpring(String name) {
        return Optional.ofNullable(spiritSprings.get(normalize(name)));
    }

    public Optional<SpiritSpring> findNearest(Location origin, double maxDistance) {
        if (origin == null || origin.getWorld() == null) {
            return Optional.empty();
        }

        double maxDistanceSq = maxDistance * maxDistance;
        return spiritSprings.values().stream()
                .filter(spring -> spring.location().getWorld() != null)
                .filter(spring -> spring.location().getWorld().equals(origin.getWorld()))
                .filter(spring -> spring.location().distanceSquared(origin) <= maxDistanceSq)
                .min(Comparator.comparingDouble(spring -> spring.location().distanceSquared(origin)));
    }

    public Optional<SpiritSpring> findActiveSpiritSpring(Location origin) {
        return findNearest(origin, triggerRadius());
    }

    public Optional<SpiritSpring> findNearestAny(Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return Optional.empty();
        }
        return spiritSprings.values().stream()
                .filter(spring -> spring.location().getWorld() != null)
                .filter(spring -> spring.location().getWorld().equals(origin.getWorld()))
                .min(Comparator.comparingDouble(spring -> spring.location().distanceSquared(origin)));
    }

    public Set<String> getSpiritSpringNames() {
        return Collections.unmodifiableSet(spiritSprings.keySet());
    }

    public Collection<String> formatSpiritSpringList() {
        return spiritSprings.values().stream()
                .sorted(Comparator.comparing(SpiritSpring::displayName, String.CASE_INSENSITIVE_ORDER))
                .map(spring -> {
                    Location loc = spring.location();
                    String worldName = loc.getWorld() == null ? "unknown" : loc.getWorld().getName();
                    return spring.displayName() + " [" + spring.key() + "] (" + worldName + " "
                            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public double triggerRadius() {
        return Math.max(0.5D, plugin.getConfig().getDouble("mounts.torrent.spirit-spring.named-trigger-radius", 2.25D));
    }

    public void saveAll() {
        saveSpiritSprings();
    }

    public void shutdown() {
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
        saveSpiritSprings();
    }

    public void playLaunchBurst(SpiritSpring spring, Vector launchDirection) {
        if (spring == null) {
            return;
        }
        Location center = spring.location().clone().add(0.0D, burstBaseHeight(), 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        Vector forward = launchDirection == null ? new Vector(0.0D, 0.0D, 1.0D) : launchDirection.clone().setY(0.0D);
        if (forward.lengthSquared() < 0.0001D) {
            forward = new Vector(0.0D, 0.0D, 1.0D);
        }
        forward.normalize();
        Vector side = new Vector(-forward.getZ(), 0.0D, forward.getX());

        world.spawnParticle(Particle.CLOUD, center, 34, 0.4D, 0.45D, 0.4D, 0.03D);
        world.spawnParticle(Particle.END_ROD, center.clone().add(0.0D, 0.55D, 0.0D), 20, 0.25D, 0.8D, 0.25D, 0.03D);
        world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0.0D, 0.45D, 0.0D), 24, 0.35D, 0.7D, 0.35D, 0.04D);
        world.spawnParticle(Particle.WAX_OFF, center.clone().add(0.0D, 0.25D, 0.0D), 18, 0.45D, 0.2D, 0.45D, 0.02D);

        for (int i = 0; i < 18; i++) {
            double progress = i / 17.0D;
            Location point = center.clone()
                    .add(forward.clone().multiply(0.35D + progress * 1.65D))
                    .add(side.clone().multiply((i % 2 == 0 ? 1.0D : -1.0D) * (0.08D + progress * 0.18D)))
                    .add(0.0D, progress * 1.45D, 0.0D);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, i % 3 == 0 ? CORE_DUST : OUTER_DUST);
        }

        world.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.MASTER, 1.25F, 0.85F);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 0.65F, 1.75F);
    }

    private void loadSpiritSprings() {
        spiritSprings.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spirit-springs");
        if (section == null) {
            return;
        }

        for (String rawKey : section.getKeys(false)) {
            String key = normalize(rawKey);
            String basePath = "spirit-springs." + rawKey;
            String worldName = plugin.getConfig().getString(basePath + ".world");
            if (worldName == null || worldName.isBlank()) {
                continue;
            }

            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Spirit spring '" + rawKey + "' ignored: world not found: " + worldName);
                continue;
            }

            double x = plugin.getConfig().getDouble(basePath + ".x");
            double y = plugin.getConfig().getDouble(basePath + ".y");
            double z = plugin.getConfig().getDouble(basePath + ".z");
            String displayName = plugin.getConfig().getString(basePath + ".display-name", toDisplayName(key));
            spiritSprings.put(key, new SpiritSpring(key, displayName, new Location(world, x, y, z)));
        }
    }

    private void saveSpiritSprings() {
        plugin.getConfig().set("spirit-springs", null);

        for (SpiritSpring spring : spiritSprings.values()) {
            Location loc = spring.location();
            if (loc.getWorld() == null) {
                continue;
            }

            String basePath = "spirit-springs." + spring.key();
            plugin.getConfig().set(basePath + ".display-name", spring.displayName());
            plugin.getConfig().set(basePath + ".world", loc.getWorld().getName());
            plugin.getConfig().set(basePath + ".x", loc.getX());
            plugin.getConfig().set(basePath + ".y", loc.getY());
            plugin.getConfig().set(basePath + ".z", loc.getZ());
        }

        plugin.saveConfig();
    }

    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
    }

    private String toDisplayName(String key) {
        String[] parts = key.split("[_\\-\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.isEmpty() ? key : builder.toString();
    }

    private void startAmbientTask() {
        if (ambientTask != null) {
            ambientTask.cancel();
        }
        ambientTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::renderAmbientSprings, 20L, ambientPeriodTicks());
    }

    private void renderAmbientSprings() {
        if (spiritSprings.isEmpty()) {
            ambientTick++;
            return;
        }

        for (SpiritSpring spring : new ArrayList<>(spiritSprings.values())) {
            renderAmbientSpring(spring, ambientTick);
        }
        ambientTick++;
    }

    private void renderAmbientSpring(SpiritSpring spring, long tick) {
        Location center = spring.location().clone().add(0.0D, ambientBaseHeight(), 0.0D);
        World world = center.getWorld();
        if (world == null || world.getPlayers().isEmpty()) {
            return;
        }

        // Only render if at least one player is within the configured render radius.
        double renderRadiusSq = ambientRenderRadius();
        renderRadiusSq = renderRadiusSq * renderRadiusSq;
        boolean anyNearby = false;
        for (org.bukkit.entity.Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= renderRadiusSq) {
                anyNearby = true;
                break;
            }
        }
        if (!anyNearby) {
            return;
        }

        double time = tick * 0.18D;
        double innerRadius = ambientInnerRadius();
        double outerRadius = ambientOuterRadius();
        double height = ambientColumnHeight();

        for (int i = 0; i < 3; i++) {
            double angle = time + (Math.PI * 2.0D / 3.0D) * i;
            Location point = center.clone().add(
                    Math.cos(angle) * innerRadius,
                    0.2D + i * 0.3D + Math.sin(time * 0.8D + i) * 0.08D,
                    Math.sin(angle) * innerRadius
            );
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.02D, 0.0D, 0.0D, CORE_DUST);
            world.spawnParticle(Particle.END_ROD, point, 1, 0.02D, 0.05D, 0.02D, 0.0D);
        }

        for (int i = 0; i < 5; i++) {
            double progress = i / 4.0D;
            double angle = -time * 0.82D + progress * Math.PI * 2.0D;
            Location point = center.clone().add(
                    Math.cos(angle) * outerRadius,
                    0.12D + progress * height,
                    Math.sin(angle) * outerRadius
            );
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, OUTER_DUST);
        }

        world.spawnParticle(Particle.CLOUD, center.clone().add(0.0D, 0.22D, 0.0D), 4, 0.35D, 0.08D, 0.35D, 0.012D);
        world.spawnParticle(Particle.WAX_OFF, center.clone().add(0.0D, 0.35D, 0.0D), 3, 0.3D, 0.18D, 0.3D, 0.01D);
        world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0.0D, height * 0.55D, 0.0D), 3, 0.18D, 0.35D, 0.18D, 0.015D);
        world.spawnParticle(Particle.DUST, center.clone().add(0.0D, 0.6D, 0.0D), 5, 0.25D, 0.55D, 0.25D, 0.01D, MIST_DUST);

        long soundPeriod = ambientSoundPeriodTicks();
        if (soundPeriod > 0L && tick % soundPeriod == springSoundOffset(spring)) {
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.AMBIENT, 0.32F, 1.8F);
            world.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.AMBIENT, 0.18F, 0.7F);
        }
    }

    private void playCreationBurst(SpiritSpring spring) {
        Location center = spring.location().clone().add(0.0D, burstBaseHeight(), 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.CLOUD, center, 26, 0.45D, 0.3D, 0.45D, 0.025D);
        world.spawnParticle(Particle.END_ROD, center.clone().add(0.0D, 0.8D, 0.0D), 18, 0.22D, 0.65D, 0.22D, 0.03D);
        world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0.0D, 0.55D, 0.0D), 20, 0.35D, 0.55D, 0.35D, 0.03D);

        for (int i = 0; i < 18; i++) {
            double angle = i * Math.PI / 9.0D;
            Location point = center.clone().add(Math.cos(angle) * 0.95D, 0.08D, Math.sin(angle) * 0.95D);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, i % 2 == 0 ? CORE_DUST : OUTER_DUST);
        }

        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.MASTER, 0.8F, 1.35F);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.MASTER, 0.55F, 1.85F);
    }

    private void playRemovalBurst(SpiritSpring spring) {
        Location center = spring.location().clone().add(0.0D, burstBaseHeight(), 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.CLOUD, center, 18, 0.35D, 0.18D, 0.35D, 0.02D);
        world.spawnParticle(Particle.SMOKE, center.clone().add(0.0D, 0.4D, 0.0D), 14, 0.25D, 0.25D, 0.25D, 0.015D);
        world.spawnParticle(Particle.WAX_OFF, center.clone().add(0.0D, 0.35D, 0.0D), 10, 0.3D, 0.15D, 0.3D, 0.01D);
        world.playSound(center, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.MASTER, 0.55F, 0.8F);
    }

    private long ambientPeriodTicks() {
        return Math.max(1L, plugin.getConfig().getLong("mounts.torrent.spirit-spring.visuals.ambient-period-ticks", 2L));
    }

    private long ambientSoundPeriodTicks() {
        return Math.max(0L, plugin.getConfig().getLong("mounts.torrent.spirit-spring.visuals.ambient-sound-period-ticks", 80L));
    }

    private double ambientBaseHeight() {
        return plugin.getConfig().getDouble("mounts.torrent.spirit-spring.visuals.base-height", 0.15D);
    }

    private double burstBaseHeight() {
        return plugin.getConfig().getDouble("mounts.torrent.spirit-spring.visuals.burst-height", 0.2D);
    }

    private double ambientInnerRadius() {
        return Math.max(0.15D, plugin.getConfig().getDouble("mounts.torrent.spirit-spring.visuals.inner-radius", 0.55D));
    }

    private double ambientOuterRadius() {
        return Math.max(ambientInnerRadius(), plugin.getConfig().getDouble("mounts.torrent.spirit-spring.visuals.outer-radius", 1.0D));
    }

    private double ambientColumnHeight() {
        return Math.max(0.5D, plugin.getConfig().getDouble("mounts.torrent.spirit-spring.visuals.column-height", 2.15D));
    }

    private double ambientRenderRadius() {
        return Math.max(16.0D, plugin.getConfig().getDouble("mounts.torrent.spirit-spring.visuals.render-radius", 48.0D));
    }

    private long springSoundOffset(SpiritSpring spring) {
        long period = ambientSoundPeriodTicks();
        if (period <= 0L) {
            return 0L;
        }
        return Math.floorMod(spring.key().hashCode(), period);
    }
}
