package de.skyforce.main.elden.grace;

import de.skyforce.main.elden.persistence.PlayerDataRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GraceManager {

    private final JavaPlugin plugin;
    private final PlayerDataRepository playerDataRepository;
    private final Map<String, GracePoint> gracePoints = new HashMap<>();
    private final Map<UUID, String> activeGraceByPlayer = new HashMap<>();
    private final Map<UUID, Set<String>> discoveredGracesByPlayer = new HashMap<>();

    public GraceManager(JavaPlugin plugin, PlayerDataRepository playerDataRepository) {
        this.plugin = plugin;
        this.playerDataRepository = playerDataRepository;
        plugin.getDataFolder().mkdirs();

        loadGracePoints();
        playerDataRepository.migrateFromYamlIfNeeded();
        loadPlayerData();
    }

    public boolean setGrace(String name, Location location) {
        String key = normalize(name);
        if (key.isBlank()) {
            return false;
        }

        String displayName = toDisplayName(key);
        gracePoints.put(key, new GracePoint(key, displayName, location));
        return true;
    }

    public boolean removeGrace(String name) {
        String key = normalize(name);
        GracePoint removed = gracePoints.remove(key);
        if (removed == null) {
            return false;
        }

        activeGraceByPlayer.entrySet().removeIf(entry -> entry.getValue().equals(key));
        discoveredGracesByPlayer.values().forEach(set -> set.remove(key));
        return true;
    }

    public Optional<GracePoint> getGracePoint(String name) {
        return Optional.ofNullable(gracePoints.get(normalize(name)));
    }

    public Optional<Location> getGrace(String name) {
        return getGracePoint(name).map(GracePoint::getLocation);
    }

    public Optional<String> getGraceDisplayName(String name) {
        return getGracePoint(name).map(GracePoint::getDisplayName);
    }

    public Set<String> getGraceNames() {
        return Collections.unmodifiableSet(gracePoints.keySet());
    }

    public void discoverGrace(Player player, String name) {
        String key = normalize(name);
        if (!gracePoints.containsKey(key)) {
            return;
        }

        discoveredGracesByPlayer
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>())
                .add(key);
    }

    public boolean hasDiscoveredGrace(Player player, String name) {
        String key = normalize(name);
        return discoveredGracesByPlayer
                .getOrDefault(player.getUniqueId(), Collections.emptySet())
                .contains(key);
    }

    public Set<String> getDiscoveredGraces(Player player) {
        return Collections.unmodifiableSet(
                discoveredGracesByPlayer.getOrDefault(player.getUniqueId(), Collections.emptySet())
        );
    }

    public Collection<GracePoint> getDiscoveredGracePoints(Player player) {
        return getDiscoveredGraces(player).stream()
                .map(gracePoints::get)
                .filter(point -> point != null)
                .sorted(Comparator.comparing(GracePoint::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void activateGrace(Player player, String name) {
        String key = normalize(name);
        if (!gracePoints.containsKey(key)) {
            return;
        }

        discoverGrace(player, key);
        activeGraceByPlayer.put(player.getUniqueId(), key);
    }

    public Optional<Location> getActiveGraceLocation(Player player) {
        String active = activeGraceByPlayer.get(player.getUniqueId());
        if (active == null) {
            return Optional.empty();
        }

        GracePoint point = gracePoints.get(active);
        return Optional.ofNullable(point == null ? null : point.getLocation());
    }

    public Optional<String> getActiveGraceName(Player player) {
        return Optional.ofNullable(activeGraceByPlayer.get(player.getUniqueId()));
    }

    public Optional<String> getActiveGraceDisplayName(Player player) {
        return getActiveGraceName(player).flatMap(this::getGraceDisplayName);
    }

    public Optional<String> getNearestGrace(Location origin, double maxDistance) {
        double maxDistanceSq = maxDistance * maxDistance;

        return gracePoints.entrySet().stream()
                .filter(entry -> entry.getValue().getLocation().getWorld() != null)
                .filter(entry -> entry.getValue().getLocation().getWorld().equals(origin.getWorld()))
                .filter(entry -> entry.getValue().getLocation().distanceSquared(origin) <= maxDistanceSq)
                .min(Comparator.comparingDouble(entry -> entry.getValue().getLocation().distanceSquared(origin)))
                .map(Map.Entry::getKey);
    }

    public Collection<String> formatGraceList() {
        return gracePoints.values().stream()
                .sorted(Comparator.comparing(GracePoint::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(point -> {
                    Location loc = point.getLocation();
                    String worldName = loc.getWorld() == null ? "unknown" : loc.getWorld().getName();
                    return point.getDisplayName() + " [" + point.getKey() + "] (" + worldName + " "
                            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
                })
                .collect(Collectors.toList());
    }

    public double getActivationRadius() {
        return Math.max(1.0D, plugin.getConfig().getDouble("grace.activation-radius", 3.0D));
    }

    public void saveAll() {
        saveGracePoints();
        savePlayerData();
    }

    private void loadGracePoints() {
        gracePoints.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("graces");
        if (section == null) {
            return;
        }

        for (String rawKey : section.getKeys(false)) {
            String key = normalize(rawKey);
            String basePath = "graces." + rawKey;

            String worldName = plugin.getConfig().getString(basePath + ".world");
            if (worldName == null) {
                continue;
            }

            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Grace '" + rawKey + "' ignored: world not found: " + worldName);
                continue;
            }

            double x = plugin.getConfig().getDouble(basePath + ".x");
            double y = plugin.getConfig().getDouble(basePath + ".y");
            double z = plugin.getConfig().getDouble(basePath + ".z");
            float yaw = (float) plugin.getConfig().getDouble(basePath + ".yaw");
            float pitch = (float) plugin.getConfig().getDouble(basePath + ".pitch");

            String displayName = plugin.getConfig().getString(basePath + ".display-name", toDisplayName(key));
            Location location = new Location(world, x, y, z, yaw, pitch);

            gracePoints.put(key, new GracePoint(key, displayName, location));
        }
    }

    private void saveGracePoints() {
        plugin.getConfig().set("graces", null);

        for (GracePoint point : gracePoints.values()) {
            Location loc = point.getLocation();
            if (loc.getWorld() == null) {
                continue;
            }

            String basePath = "graces." + point.getKey();
            plugin.getConfig().set(basePath + ".display-name", point.getDisplayName());
            plugin.getConfig().set(basePath + ".world", loc.getWorld().getName());
            plugin.getConfig().set(basePath + ".x", loc.getX());
            plugin.getConfig().set(basePath + ".y", loc.getY());
            plugin.getConfig().set(basePath + ".z", loc.getZ());
            plugin.getConfig().set(basePath + ".yaw", loc.getYaw());
            plugin.getConfig().set(basePath + ".pitch", loc.getPitch());
        }

        plugin.saveConfig();
    }

    private void loadPlayerData() {
        activeGraceByPlayer.clear();
        discoveredGracesByPlayer.clear();

        Map<UUID, String> loadedActive = playerDataRepository.loadActiveGraces();
        Map<UUID, Set<String>> loadedDiscovered = playerDataRepository.loadDiscoveredGraces();

        for (Map.Entry<UUID, String> entry : loadedActive.entrySet()) {
            String key = normalize(entry.getValue());
            if (gracePoints.containsKey(key)) {
                activeGraceByPlayer.put(entry.getKey(), key);
            }
        }

        for (Map.Entry<UUID, Set<String>> entry : loadedDiscovered.entrySet()) {
            Set<String> filtered = entry.getValue().stream()
                    .map(this::normalize)
                    .filter(gracePoints::containsKey)
                    .collect(Collectors.toCollection(HashSet::new));

            if (!filtered.isEmpty()) {
                discoveredGracesByPlayer.put(entry.getKey(), filtered);
            }
        }
    }

    private void savePlayerData() {
        playerDataRepository.saveGraceData(activeGraceByPlayer, discoveredGracesByPlayer);
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
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
}

