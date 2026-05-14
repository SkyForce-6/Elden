package de.skyforce.main.elden.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomModelDataRegistry {

    private static final String ROOT_PATH = "items.custom-model-data";

    private final JavaPlugin plugin;

    public CustomModelDataRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Integer weapon(String weaponId) {
        return resolve(ROOT_PATH + ".weapons", weaponId);
    }

    public Integer armor(String armorId) {
        return resolve(ROOT_PATH + ".armor", armorId);
    }

    public Integer spell(String spellId) {
        return resolve(ROOT_PATH + ".spells", spellId);
    }

    public Integer ash(String ashId) {
        return resolve(ROOT_PATH + ".ashes", ashId);
    }

    public Integer spiritAsh(String spiritAshId) {
        return resolve(ROOT_PATH + ".spirit-ashes", spiritAshId);
    }

    public Integer talisman(String talismanId) {
        return resolve(ROOT_PATH + ".talismans", talismanId);
    }

    public Integer flask(String flaskKey) {
        return resolve(ROOT_PATH + ".flasks", flaskKey);
    }

    public Integer torrentWhistle() {
        return resolve(ROOT_PATH + ".torrent.whistle");
    }

    public Integer torrentRaisin(String raisinKey) {
        return resolve(ROOT_PATH + ".torrent.raisins", raisinKey);
    }

    public Integer runeDrop() {
        return resolve(ROOT_PATH + ".runes.drop");
    }

    private Integer resolve(String sectionPath, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section == null) {
            return null;
        }
        return positive(section.getInt(key, 0));
    }

    private Integer resolve(String path) {
        return positive(plugin.getConfig().getInt(path, 0));
    }

    private Integer positive(int value) {
        return value > 0 ? value : null;
    }
}
