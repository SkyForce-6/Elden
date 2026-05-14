package de.skyforce.main.elden.enemy.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public record PatrolPoint(
        String worldName,
        double x,
        double y,
        double z
) {

    public static PatrolPoint fromLocation(Location location) {
        return new PatrolPoint(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }

    public Location toLocation(JavaPlugin plugin) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }
}
