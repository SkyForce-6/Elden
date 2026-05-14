package de.skyforce.main.elden.enemy;

import de.skyforce.main.elden.enemy.model.EnemyDefinition;
import de.skyforce.main.elden.enemy.model.EnemySpawnerDefinition;
import de.skyforce.main.elden.enemy.model.PatrolPoint;
import de.skyforce.main.elden.enemy.registry.EnemyRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EnemySpawnerManager {

    private final JavaPlugin plugin;
    private final EnemyRegistry enemyRegistry;
    private final EnemyManager enemyManager;
    private final Map<String, EnemySpawnerDefinition> spawners = new LinkedHashMap<>();
    private final Map<String, Long> nextSpawnTicks = new LinkedHashMap<>();
    private final BukkitTask spawnTask;

    public EnemySpawnerManager(JavaPlugin plugin, EnemyRegistry enemyRegistry, EnemyManager enemyManager) {
        this.plugin = plugin;
        this.enemyRegistry = enemyRegistry;
        this.enemyManager = enemyManager;
        loadConfiguredSpawners();
        this.spawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSpawners, 40L, 20L);
    }

    public Map<String, EnemySpawnerDefinition> getSpawners() {
        return Map.copyOf(spawners);
    }

    public Optional<EnemySpawnerDefinition> getSpawner(String spawnerId) {
        if (spawnerId == null || spawnerId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(spawners.get(normalizeId(spawnerId)));
    }

    public boolean createSpawner(String spawnerId, EnemyDefinition definition, Location location) {
        if (definition == null || spawnerId == null || spawnerId.isBlank() || location == null || location.getWorld() == null) {
            return false;
        }

        EnemySpawnerDefinition spawner = new EnemySpawnerDefinition(
                normalizeId(spawnerId),
                definition.id(),
                null,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                4.0D,
                0.12D,
                List.of(),
                1,
                definition.respawnDelayTicks(),
                true,
                true
        );
        spawners.put(spawner.spawnerId(), spawner);
        nextSpawnTicks.put(spawner.spawnerId(), plugin.getServer().getCurrentTick() + 1L);
        saveSpawner(spawner);
        return true;
    }

    public boolean removeSpawner(String spawnerId) {
        EnemySpawnerDefinition removed = spawners.remove(normalizeId(spawnerId));
        if (removed == null) {
            return false;
        }
        nextSpawnTicks.remove(removed.spawnerId());
        enemyManager.despawnSpawnerEnemies(removed.spawnerId(), false, 0L);
        plugin.getConfig().set(path(removed.spawnerId()), null);
        plugin.saveConfig();
        return true;
    }

    public boolean setEnabled(String spawnerId, boolean enabled) {
        EnemySpawnerDefinition existing = spawners.get(normalizeId(spawnerId));
        if (existing == null) {
            return false;
        }
        EnemySpawnerDefinition updated = new EnemySpawnerDefinition(
                existing.spawnerId(),
                existing.enemyId(),
                existing.groupId(),
                existing.worldName(),
                existing.x(),
                existing.y(),
                existing.z(),
                existing.yaw(),
                existing.pitch(),
                existing.radius(),
                existing.eliteChance(),
                existing.patrolPoints(),
                existing.maxActive(),
                existing.respawnDelayTicks(),
                existing.resetOnGrace(),
                enabled
        );
        spawners.put(updated.spawnerId(), updated);
        saveSpawner(updated);
        if (!enabled) {
            enemyManager.despawnSpawnerEnemies(updated.spawnerId(), false, 0L);
        } else {
            nextSpawnTicks.put(updated.spawnerId(), plugin.getServer().getCurrentTick() + 1L);
        }
        return true;
    }

    public boolean resetSpawner(String spawnerId) {
        EnemySpawnerDefinition spawner = spawners.get(normalizeId(spawnerId));
        if (spawner == null) {
            return false;
        }
        enemyManager.despawnSpawnerEnemies(spawner.spawnerId(), false, 0L);
        nextSpawnTicks.put(spawner.spawnerId(), plugin.getServer().getCurrentTick() + 20L);
        return true;
    }

    public boolean setGroup(String spawnerId, String groupId) {
        EnemySpawnerDefinition existing = spawners.get(normalizeId(spawnerId));
        if (existing == null) {
            return false;
        }
        String normalizedGroup = groupId == null || groupId.isBlank() ? null : normalizeId(groupId);
        EnemySpawnerDefinition updated = new EnemySpawnerDefinition(
                existing.spawnerId(),
                existing.enemyId(),
                normalizedGroup,
                existing.worldName(),
                existing.x(),
                existing.y(),
                existing.z(),
                existing.yaw(),
                existing.pitch(),
                existing.radius(),
                existing.eliteChance(),
                existing.patrolPoints(),
                existing.maxActive(),
                existing.respawnDelayTicks(),
                existing.resetOnGrace(),
                existing.enabled()
        );
        spawners.put(updated.spawnerId(), updated);
        saveSpawner(updated);
        return true;
    }

    public boolean resetGroup(String groupId) {
        String normalizedGroup = normalizeId(groupId);
        boolean any = false;
        long now = plugin.getServer().getCurrentTick();
        for (EnemySpawnerDefinition spawner : spawners.values()) {
            if (!normalizedGroup.equals(spawner.groupId())) {
                continue;
            }
            enemyManager.despawnSpawnerEnemies(spawner.spawnerId(), false, 0L);
            nextSpawnTicks.put(spawner.spawnerId(), now + 20L);
            any = true;
        }
        return any;
    }

    public Set<String> getGroupIds() {
        Set<String> groups = new LinkedHashSet<>();
        for (EnemySpawnerDefinition spawner : spawners.values()) {
            if (spawner.groupId() != null && !spawner.groupId().isBlank()) {
                groups.add(spawner.groupId());
            }
        }
        return groups;
    }

    public boolean addPatrolPoint(String spawnerId, Location location) {
        EnemySpawnerDefinition existing = spawners.get(normalizeId(spawnerId));
        if (existing == null || location == null || location.getWorld() == null) {
            return false;
        }
        List<PatrolPoint> patrolPoints = new ArrayList<>(existing.patrolPoints());
        patrolPoints.add(PatrolPoint.fromLocation(location));
        EnemySpawnerDefinition updated = copyWithPatrol(existing, patrolPoints);
        spawners.put(updated.spawnerId(), updated);
        saveSpawner(updated);
        return true;
    }

    public boolean clearPatrol(String spawnerId) {
        EnemySpawnerDefinition existing = spawners.get(normalizeId(spawnerId));
        if (existing == null) {
            return false;
        }
        EnemySpawnerDefinition updated = copyWithPatrol(existing, List.of());
        spawners.put(updated.spawnerId(), updated);
        saveSpawner(updated);
        return true;
    }

    public void resetGraceSpawners() {
        long now = plugin.getServer().getCurrentTick();
        for (EnemySpawnerDefinition spawner : spawners.values()) {
            if (!spawner.resetOnGrace()) {
                continue;
            }
            enemyManager.despawnSpawnerEnemies(spawner.spawnerId(), false, 0L);
            nextSpawnTicks.put(spawner.spawnerId(), now + 20L);
        }
    }

    public void handleEnemyRemoved(String spawnerId, long respawnDelayTicks) {
        if (spawnerId == null || spawnerId.isBlank() || !spawners.containsKey(spawnerId)) {
            return;
        }
        nextSpawnTicks.put(spawnerId, plugin.getServer().getCurrentTick() + Math.max(20L, respawnDelayTicks));
    }

    public void shutdown() {
        spawnTask.cancel();
    }

    private void tickSpawners() {
        long now = plugin.getServer().getCurrentTick();
        for (EnemySpawnerDefinition spawner : spawners.values()) {
            if (!spawner.enabled()) {
                continue;
            }

            EnemyDefinition definition = enemyRegistry.getById(spawner.enemyId()).orElse(null);
            if (definition == null) {
                continue;
            }

            long nextTick = nextSpawnTicks.getOrDefault(spawner.spawnerId(), 0L);
            if (now < nextTick) {
                continue;
            }

            if (enemyManager.getActiveCount(spawner.spawnerId()) >= Math.max(1, spawner.maxActive())) {
                continue;
            }

            boolean spawned = enemyManager.spawnEnemy(definition, spawner);
            if (spawned) {
                nextSpawnTicks.put(spawner.spawnerId(), now + 20L);
            }
        }
    }

    private void loadConfiguredSpawners() {
        spawners.clear();
        nextSpawnTicks.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("enemies.spawners");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String basePath = path(key);
            String enemyId = plugin.getConfig().getString(basePath + ".enemy-id");
            String world = plugin.getConfig().getString(basePath + ".world");
            if (enemyId == null || world == null) {
                continue;
            }

            EnemyDefinition definition = enemyRegistry.getById(enemyId).orElse(null);
            if (definition == null) {
                plugin.getLogger().warning("Enemy spawner '" + key + "' ignored: unknown enemy-id '" + enemyId + "'.");
                continue;
            }

            EnemySpawnerDefinition spawner = new EnemySpawnerDefinition(
                    normalizeId(key),
                    definition.id(),
                    normalizeNullable(plugin.getConfig().getString(basePath + ".group-id")),
                    world,
                    plugin.getConfig().getDouble(basePath + ".x"),
                    plugin.getConfig().getDouble(basePath + ".y"),
                    plugin.getConfig().getDouble(basePath + ".z"),
                    (float) plugin.getConfig().getDouble(basePath + ".yaw"),
                    (float) plugin.getConfig().getDouble(basePath + ".pitch"),
                    Math.max(0.0D, plugin.getConfig().getDouble(basePath + ".radius", 4.0D)),
                    Math.max(0.0D, Math.min(1.0D, plugin.getConfig().getDouble(basePath + ".elite-chance", 0.12D))),
                    loadPatrolPoints(basePath),
                    Math.max(1, plugin.getConfig().getInt(basePath + ".max-active", 1)),
                    Math.max(20L, plugin.getConfig().getLong(basePath + ".respawn-delay-ticks", definition.respawnDelayTicks())),
                    plugin.getConfig().getBoolean(basePath + ".reset-on-grace", true),
                    plugin.getConfig().getBoolean(basePath + ".enabled", true)
            );
            spawners.put(spawner.spawnerId(), spawner);
            nextSpawnTicks.put(spawner.spawnerId(), plugin.getServer().getCurrentTick() + 40L);
        }
    }

    private void saveSpawner(EnemySpawnerDefinition spawner) {
        String basePath = path(spawner.spawnerId());
        plugin.getConfig().set(basePath + ".enemy-id", spawner.enemyId());
        plugin.getConfig().set(basePath + ".group-id", spawner.groupId());
        plugin.getConfig().set(basePath + ".world", spawner.worldName());
        plugin.getConfig().set(basePath + ".x", spawner.x());
        plugin.getConfig().set(basePath + ".y", spawner.y());
        plugin.getConfig().set(basePath + ".z", spawner.z());
        plugin.getConfig().set(basePath + ".yaw", spawner.yaw());
        plugin.getConfig().set(basePath + ".pitch", spawner.pitch());
        plugin.getConfig().set(basePath + ".radius", spawner.radius());
        plugin.getConfig().set(basePath + ".elite-chance", spawner.eliteChance());
        plugin.getConfig().set(basePath + ".patrol", null);
        for (int index = 0; index < spawner.patrolPoints().size(); index++) {
            PatrolPoint point = spawner.patrolPoints().get(index);
            String patrolPath = basePath + ".patrol." + index;
            plugin.getConfig().set(patrolPath + ".world", point.worldName());
            plugin.getConfig().set(patrolPath + ".x", point.x());
            plugin.getConfig().set(patrolPath + ".y", point.y());
            plugin.getConfig().set(patrolPath + ".z", point.z());
        }
        plugin.getConfig().set(basePath + ".max-active", spawner.maxActive());
        plugin.getConfig().set(basePath + ".respawn-delay-ticks", spawner.respawnDelayTicks());
        plugin.getConfig().set(basePath + ".reset-on-grace", spawner.resetOnGrace());
        plugin.getConfig().set(basePath + ".enabled", spawner.enabled());
        plugin.saveConfig();
    }

    private String path(String spawnerId) {
        return "enemies.spawners." + normalizeId(spawnerId);
    }

    private String normalizeId(String value) {
        return value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeId(value);
    }

    private List<PatrolPoint> loadPatrolPoints(String basePath) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(basePath + ".patrol");
        if (section == null) {
            return List.of();
        }
        List<PatrolPoint> patrolPoints = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String patrolPath = basePath + ".patrol." + key;
            String worldName = plugin.getConfig().getString(patrolPath + ".world");
            if (worldName == null || worldName.isBlank()) {
                continue;
            }
            patrolPoints.add(new PatrolPoint(
                    worldName,
                    plugin.getConfig().getDouble(patrolPath + ".x"),
                    plugin.getConfig().getDouble(patrolPath + ".y"),
                    plugin.getConfig().getDouble(patrolPath + ".z")
            ));
        }
        return List.copyOf(patrolPoints);
    }

    private EnemySpawnerDefinition copyWithPatrol(EnemySpawnerDefinition existing, List<PatrolPoint> patrolPoints) {
        return new EnemySpawnerDefinition(
                existing.spawnerId(),
                existing.enemyId(),
                existing.groupId(),
                existing.worldName(),
                existing.x(),
                existing.y(),
                existing.z(),
                existing.yaw(),
                existing.pitch(),
                existing.radius(),
                existing.eliteChance(),
                List.copyOf(patrolPoints),
                existing.maxActive(),
                existing.respawnDelayTicks(),
                existing.resetOnGrace(),
                existing.enabled()
        );
    }
}
