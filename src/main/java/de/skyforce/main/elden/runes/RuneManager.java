package de.skyforce.main.elden.runes;

import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.persistence.PlayerDataRepository;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RuneManager {

    private final JavaPlugin plugin;
    private final PlayerDataRepository playerDataRepository;
    private final CustomModelDataRegistry customModelDataRegistry;
    private final Map<UUID, Integer> runesByPlayer = new HashMap<>();

    private final NamespacedKey runeOwnerKey;
    private final NamespacedKey runeAmountKey;

    private BukkitTask actionBarTask;
    private de.skyforce.main.elden.level.LevelManager levelManager;
    private FocusManager focusManager;
    private Function<Player, Double> runeGainMultiplierProvider = player -> 1.0D;

    public RuneManager(JavaPlugin plugin, PlayerDataRepository playerDataRepository,
                       CustomModelDataRegistry customModelDataRegistry) {
        this.plugin = plugin;
        this.playerDataRepository = playerDataRepository;
        this.customModelDataRegistry = customModelDataRegistry;
        this.runeOwnerKey = new NamespacedKey(plugin, "rune-owner");
        this.runeAmountKey = new NamespacedKey(plugin, "rune-amount");

        playerDataRepository.migrateFromYamlIfNeeded();
        loadRunes();
    }

    public void setLevelManager(de.skyforce.main.elden.level.LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    public void setFocusManager(FocusManager focusManager) {
        this.focusManager = focusManager;
    }

    public void setRuneGainMultiplierProvider(Function<Player, Double> runeGainMultiplierProvider) {
        this.runeGainMultiplierProvider = runeGainMultiplierProvider == null ? player -> 1.0D : runeGainMultiplierProvider;
    }

    public int getRunes(Player player) {
        return runesByPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    public int getRunes(UUID playerId) {
        return runesByPlayer.getOrDefault(playerId, 0);
    }

    public void setRunes(UUID playerId, int amount) {
        runesByPlayer.put(playerId, Math.max(0, amount));
    }

    public int addRunes(UUID playerId, int amount) {
        if (amount <= 0) {
            return getRunes(playerId);
        }

        long sum = (long) getRunes(playerId) + amount;
        int next = (int) Math.min(Integer.MAX_VALUE, sum);
        runesByPlayer.put(playerId, next);
        return next;
    }

    public int addRunes(Player player, int amount, boolean applyGainMultiplier) {
        if (player == null) {
            return 0;
        }
        int adjusted = amount;
        if (applyGainMultiplier && amount > 0) {
            adjusted = Math.max(1, (int) Math.round(amount * Math.max(0.0D, runeGainMultiplierProvider.apply(player))));
        }
        return addRunes(player.getUniqueId(), adjusted);
    }

    public boolean spendRunes(UUID playerId, int amount) {
        int cost = Math.max(0, amount);
        int current = getRunes(playerId);
        if (current < cost) {
            return false;
        }

        runesByPlayer.put(playerId, current - cost);
        return true;
    }

    public int removeRunes(UUID playerId, int amount) {
        if (amount <= 0) {
            return getRunes(playerId);
        }

        int current = getRunes(playerId);
        int next = Math.max(0, current - amount);
        runesByPlayer.put(playerId, next);
        return next;
    }

    public int takeAllRunes(UUID playerId) {
        int amount = getRunes(playerId);
        runesByPlayer.put(playerId, 0);
        return amount;
    }

    public Item spawnRuneDrop(Player owner, int amount, Location location) {
        if (location.getWorld() == null) {
            return null;
        }

        Material material = resolveDropMaterial();
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(text("Lost Runes: " + amount, NamedTextColor.GOLD));
        Integer customModelData = customModelDataRegistry.runeDrop();
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(runeOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        container.set(runeAmountKey, PersistentDataType.INTEGER, amount);

        stack.setItemMeta(meta);

        Item item = location.getWorld().dropItem(location, stack);
        item.setUnlimitedLifetime(true);
        return item;
    }

    public boolean isRuneDrop(Item item) {
        ItemStack stack = item.getItemStack();
        if (stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
        return container.has(runeOwnerKey, PersistentDataType.STRING)
                && container.has(runeAmountKey, PersistentDataType.INTEGER);
    }

    public UUID getRuneDropOwner(Item item) {
        if (!item.getItemStack().hasItemMeta()) {
            return null;
        }

        PersistentDataContainer container = item.getItemStack().getItemMeta().getPersistentDataContainer();
        String uuidText = container.get(runeOwnerKey, PersistentDataType.STRING);
        if (uuidText == null) {
            return null;
        }

        try {
            return UUID.fromString(uuidText);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public int consumeRuneDrop(Item item) {
        if (!item.getItemStack().hasItemMeta()) {
            return 0;
        }

        PersistentDataContainer container = item.getItemStack().getItemMeta().getPersistentDataContainer();
        Integer amount = container.get(runeAmountKey, PersistentDataType.INTEGER);
        if (amount == null || amount <= 0) {
            return 0;
        }

        item.remove();
        return amount;
    }

    public void startActionBarTask() {
        boolean enabled = plugin.getConfig().getBoolean("runes.actionbar.enabled", true);
        if (!enabled) {
            return;
        }

        stopActionBarTask();

        long interval = Math.max(20L, plugin.getConfig().getLong("runes.actionbar.interval-ticks", 40L));
        actionBarTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                int runes = getRunes(online);

                if (focusManager != null) {
                    double currentFocus = focusManager.getFocus(online);
                    double maxFocus = focusManager.getMaxFocus(online);
                    online.sendActionBar(text(
                            "✦ Runes: " + runes + " | 🔵 FP: " + formatNumber(currentFocus) + "/" + formatNumber(maxFocus),
                            NamedTextColor.GOLD
                    ));
                } else {
                    online.sendActionBar(text("✦ Runes: " + runes, NamedTextColor.GOLD));
                }
            }
        }, interval, interval);
    }

    public void stopActionBarTask() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    public void saveAll() {
        saveRunes();
    }

    private Material resolveDropMaterial() {
        String configured = plugin.getConfig().getString("runes.drop.material", "RECOVERY_COMPASS");
        Material material = Material.matchMaterial(configured == null ? "RECOVERY_COMPASS" : configured);
        if (material == null || !material.isItem()) {
            return Material.RECOVERY_COMPASS;
        }
        return material;
    }

    private void loadRunes() {
        runesByPlayer.clear();
        runesByPlayer.putAll(playerDataRepository.loadRunes());
    }

    private void saveRunes() {
        playerDataRepository.saveRunes(runesByPlayer);
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
