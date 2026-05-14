package de.skyforce.main.elden.smithing.service;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmithingAnvilService {

    private final JavaPlugin plugin;
    private final NamespacedKey anvilItemKey;
    private final File dataFile;
    private final Set<String> anvilLocations = new HashSet<>();

    public SmithingAnvilService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.anvilItemKey = new NamespacedKey(plugin, "smithing-anvil-item");
        this.dataFile = new File(plugin.getDataFolder(), "smithing-anvils.yml");
        load();
    }

    public ItemStack createAnvilItem() {
        Material material = resolveAnvilMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Smithing Anvil", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Place and right click to open smithing.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Break it to pick it back up.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(anvilItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSmithingAnvilItem(ItemStack item) {
        return item != null
                && item.getType() != Material.AIR
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(anvilItemKey, PersistentDataType.BYTE);
    }

    public boolean isSmithingAnvil(Block block) {
        return block != null && anvilLocations.contains(serialize(block.getLocation()));
    }

    public void registerPlacedAnvil(Block block) {
        anvilLocations.add(serialize(block.getLocation()));
        save();
    }

    public void unregisterAnvil(Block block) {
        if (block == null) {
            return;
        }
        if (anvilLocations.remove(serialize(block.getLocation()))) {
            save();
        }
    }

    private Material resolveAnvilMaterial() {
        String configured = plugin.getConfig().getString("smithing.anvil-material", "CHIPPED_ANVIL");
        Material material = Material.matchMaterial(configured == null ? "CHIPPED_ANVIL" : configured);
        if (material == null || (!material.name().endsWith("ANVIL") && material != Material.ANVIL)) {
            return Material.CHIPPED_ANVIL;
        }
        return material;
    }

    private void load() {
        anvilLocations.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        anvilLocations.addAll(yaml.getStringList("anvils"));
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("anvils", anvilLocations.stream().sorted().toList());
        try {
            yaml.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save smithing anvils: " + exception.getMessage());
        }
    }

    private String serialize(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public Location deserialize(String value) {
        String[] parts = value.split(":");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
