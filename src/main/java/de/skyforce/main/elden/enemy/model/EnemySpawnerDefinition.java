package de.skyforce.main.elden.enemy.model;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public record EnemySpawnerDefinition(
        String spawnerId,
        String enemyId,
        String groupId,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double radius,
        double eliteChance,
        List<PatrolPoint> patrolPoints,
        int maxActive,
        long respawnDelayTicks,
        boolean resetOnGrace,
        boolean enabled
) {

    public Location toLocation(JavaPlugin plugin) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}
