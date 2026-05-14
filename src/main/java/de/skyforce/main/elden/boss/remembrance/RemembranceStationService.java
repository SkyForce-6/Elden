package de.skyforce.main.elden.boss.remembrance;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class RemembranceStationService {

    private final JavaPlugin plugin;
    private final NamespacedKey stationItemKey;
    private final File dataFile;
    private final Set<String> stationLocations = new HashSet<>();

    public RemembranceStationService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stationItemKey = new NamespacedKey(plugin, "remembrance-station-item");
        this.dataFile = new File(plugin.getDataFolder(), "remembrance-stations.yml");
        load();
    }

    public ItemStack createStationItem() {
        ItemStack item = new ItemStack(stationMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Remembrance Station", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Place and right click to exchange remembrances.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Break it to pick it back up.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(stationItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isStationItem(ItemStack item) {
        return item != null
                && item.getType() != Material.AIR
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(stationItemKey, PersistentDataType.BYTE);
    }

    public boolean isStation(Block block) {
        return block != null && stationLocations.contains(serialize(block));
    }

    public void registerPlacedStation(Block block) {
        if (block == null) {
            return;
        }
        stationLocations.add(serialize(block));
        save();
    }

    public void unregisterStation(Block block) {
        if (block == null) {
            return;
        }
        if (stationLocations.remove(serialize(block))) {
            save();
        }
    }

    public Material stationMaterial() {
        String configured = plugin.getConfig().getString("bosses.remembrance-station.material", "LODESTONE");
        if (configured == null || configured.isBlank()) {
            return Material.LODESTONE;
        }

        try {
            return Material.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Material.LODESTONE;
        }
    }

    private void load() {
        stationLocations.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        stationLocations.addAll(yaml.getStringList("stations"));
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("stations", stationLocations.stream().sorted().toList());
        try {
            yaml.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save remembrance stations: " + exception.getMessage());
        }
    }

    private String serialize(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
