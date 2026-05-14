package de.skyforce.main.elden.flask;

import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.mount.TorrentManager;
import de.skyforce.main.elden.visual.VisualEffectService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlaskService {

    private final JavaPlugin plugin;
    private final StaminaManager staminaManager;
    private final FocusManager focusManager;
    private final CustomModelDataRegistry customModelDataRegistry;
    private final VisualEffectService visualEffectService;

    private final NamespacedKey flaskTypeKey;
    private final NamespacedKey flaskChargesKey;
    private final NamespacedKey flaskMaxChargesKey;
    private final NamespacedKey flaskOwnerKey;

    private final Map<UUID, Map<FlaskType, Long>> cooldownUntilByPlayer = new HashMap<>();
    private TorrentManager torrentManager;

    public FlaskService(JavaPlugin plugin, StaminaManager staminaManager, FocusManager focusManager,
                        CustomModelDataRegistry customModelDataRegistry, VisualEffectService visualEffectService) {
        this.plugin = plugin;
        this.staminaManager = staminaManager;
        this.focusManager = focusManager;
        this.customModelDataRegistry = customModelDataRegistry;
        this.visualEffectService = visualEffectService;
        this.flaskTypeKey = new NamespacedKey(plugin, "flask-type");
        this.flaskChargesKey = new NamespacedKey(plugin, "flask-charges");
        this.flaskMaxChargesKey = new NamespacedKey(plugin, "flask-max-charges");
        this.flaskOwnerKey = new NamespacedKey(plugin, "flask-owner");
    }

    public void setTorrentManager(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    public void grantStarterFlasksIfMissing(Player player) {
        if (!plugin.getConfig().getBoolean("flasks.auto-give", true)) {
            return;
        }

        Inventory inventory = player.getInventory();

        for (FlaskType type : FlaskType.values()) {
            if (hasFlask(player, type)) {
                continue;
            }

            ItemStack flask = createFlask(player, type, maxCharges(type), maxCharges(type));
            Map<Integer, ItemStack> leftover = inventory.addItem(flask);

            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), flask);
                player.sendMessage(text("Your flask has been placed at your feet.", NamedTextColor.YELLOW));
            }
        }
    }

    public void refillFlasks(Player player) {
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            FlaskType type = getType(item);
            if (type == null) {
                continue;
            }

            int max = Math.max(1, maxCharges(type));
            inventory.setItem(slot, createFlask(player, type, max, max));
        }

        grantStarterFlasksIfMissing(player);
    }

    public boolean tryUseFlask(Player player, ItemStack item, EquipmentSlot hand) {
        FlaskType type = getType(item);
        if (type == null) {
            return false;
        }
        if (!canUseNow(player, item, type)) {
            return true;
        }
        return consumeNow(player, item, hand, type);
    }

    public boolean shouldUseDrinkAnimation() {
        return plugin.getConfig().getBoolean("flasks.drink-animation.enabled", false);
    }

    public boolean isFlaskItem(ItemStack item) {
        return getType(item) != null;
    }

    public boolean prepareFlaskUse(Player player, ItemStack item) {
        FlaskType type = getType(item);
        return type != null && canUseNow(player, item, type);
    }

    public boolean finishAnimatedFlaskUse(Player player, ItemStack item, EquipmentSlot hand) {
        FlaskType type = getType(item);
        if (type == null || hand == null) {
            return type != null;
        }
        return consumeNow(player, item, hand, type);
    }

    private boolean canUseNow(Player player, ItemStack item, FlaskType type) {
        if (!player.hasPermission("elden.flask.use")) {
            player.sendMessage(text("You do not have permission.", NamedTextColor.RED));
            return false;
        }

        if (!isOwnedBy(player, item)) {
            player.sendActionBar(text("This flask does not answer to you.", NamedTextColor.RED));
            return false;
        }

        long now = plugin.getServer().getCurrentTick();
        Map<FlaskType, Long> cooldowns = cooldownUntilByPlayer.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new EnumMap<>(FlaskType.class)
        );

        long cooldownUntil = cooldowns.getOrDefault(type, 0L);
        if (now < cooldownUntil) {
            long remaining = cooldownUntil - now;
            player.sendActionBar(text("Flask cooldown: " + remaining + "t", NamedTextColor.GRAY));
            return false;
        }

        int charges = getCharges(item);
        if (charges <= 0) {
            player.sendActionBar(text("No flask charges remain.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean consumeNow(Player player, ItemStack item, EquipmentSlot hand, FlaskType type) {
        if (!canUseNow(player, item, type)) {
            return true;
        }
        int charges = getCharges(item);
        int maxCharges = getMaxCharges(item, type);
        long now = plugin.getServer().getCurrentTick();
        Map<FlaskType, Long> cooldowns = cooldownUntilByPlayer.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new EnumMap<>(FlaskType.class)
        );
        applyFlaskEffect(player, type);

        int nextCharges = charges - 1;
        ItemStack updated = createFlask(player, type, nextCharges, maxCharges);

        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(updated);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(updated);
        } else {
            return true;
        }

        long cooldownTicks = Math.max(0L, plugin.getConfig().getLong("flasks.cooldown-ticks", 20L));
        cooldowns.put(type, now + cooldownTicks);

        String msg = switch (type) {
            case CRIMSON -> "Flask of Crimson Tears used (" + nextCharges + "/" + maxCharges + ")";
            case CERULEAN -> "Flask of Cerulean Tears used (" + nextCharges + "/" + maxCharges + ")";
        };

        player.sendActionBar(text(msg, NamedTextColor.GOLD));
        return true;
    }

    public void clearPlayerCooldowns(Player player) {
        cooldownUntilByPlayer.remove(player.getUniqueId());
    }

    public boolean consumeCrimsonCharge(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (getType(item) != FlaskType.CRIMSON || !isOwnedBy(player, item)) {
                continue;
            }

            int charges = getCharges(item);
            if (charges <= 0) {
                continue;
            }

            int maxCharges = getMaxCharges(item, FlaskType.CRIMSON);
            inventory.setItem(slot, createFlask(player, FlaskType.CRIMSON, charges - 1, maxCharges));
            return true;
        }
        return false;
    }

    private void applyFlaskEffect(Player player, FlaskType type) {
        if (type == FlaskType.CRIMSON) {
            double healAmount = Math.max(0.0D, plugin.getConfig().getDouble("flasks.crimson.heal-hp", 8.0D));
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttribute == null ? 20.0D : maxHealthAttribute.getValue();

            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));
            if (torrentManager != null) {
                torrentManager.healMountedTorrent(player, healAmount);
            }
            visualEffectService.playCrimsonFlask(player);
            player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1.0f, 1.1f);
            return;
        }

        double restoreAmount = Math.max(0.0D,
                plugin.getConfig().getDouble("flasks.cerulean.restore-fp",
                        plugin.getConfig().getDouble("flasks.cerulean.restore-stamina", 25.0D)));
        focusManager.restore(player, restoreAmount);
        visualEffectService.playCeruleanFlask(player);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
    }

    private boolean hasFlask(Player player, FlaskType type) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (type == getType(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOwnedBy(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        String owner = item.getItemMeta().getPersistentDataContainer().get(flaskOwnerKey, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return false;
        }

        return owner.equals(player.getUniqueId().toString());
    }

    private ItemStack createFlask(Player owner, FlaskType type, int charges, int maxCharges) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta baseMeta = item.getItemMeta();

        if (!(baseMeta instanceof PotionMeta meta)) {
            return item;
        }

        if (type == FlaskType.CRIMSON) {
            meta.setColor(Color.fromRGB(196, 42, 42));
        } else {
            meta.setColor(Color.fromRGB(66, 121, 216));
        }

        meta.displayName(text(
                type.displayName(),
                type == FlaskType.CRIMSON ? NamedTextColor.RED : NamedTextColor.AQUA
        ));

        List<Component> lore = new ArrayList<>();
        if (type == FlaskType.CRIMSON) {
            lore.add(text("Restores lost HP", NamedTextColor.GRAY));
        } else {
            lore.add(text("Restores FP", NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(text("Charges: " + Math.max(0, charges) + "/" + Math.max(1, maxCharges), NamedTextColor.YELLOW));
        lore.add(text("Refills at a Site of Grace", NamedTextColor.DARK_GRAY));
        lore.add(text("Bound to its bearer", NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        Integer customModelData = customModelDataRegistry.flask(type.key());
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(flaskTypeKey, PersistentDataType.STRING, type.key());
        pdc.set(flaskChargesKey, PersistentDataType.INTEGER, Math.max(0, charges));
        pdc.set(flaskMaxChargesKey, PersistentDataType.INTEGER, Math.max(1, maxCharges));
        pdc.set(flaskOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());

        item.setItemMeta(meta);
        return item;
    }

    private FlaskType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        String key = item.getItemMeta().getPersistentDataContainer().get(flaskTypeKey, PersistentDataType.STRING);
        if (key == null || key.isBlank()) {
            return null;
        }

        for (FlaskType type : FlaskType.values()) {
            if (type.key().equalsIgnoreCase(key)) {
                return type;
            }
        }

        return null;
    }

    private int getCharges(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        Integer value = item.getItemMeta().getPersistentDataContainer().get(flaskChargesKey, PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, value);
    }

    private int getMaxCharges(ItemStack item, FlaskType type) {
        if (item != null && item.hasItemMeta()) {
            Integer value = item.getItemMeta().getPersistentDataContainer().get(flaskMaxChargesKey, PersistentDataType.INTEGER);
            if (value != null && value > 0) {
                return value;
            }
        }
        return maxCharges(type);
    }

    private int maxCharges(FlaskType type) {
        return switch (type) {
            case CRIMSON -> Math.max(1, plugin.getConfig().getInt("flasks.crimson.max-charges", 4));
            case CERULEAN -> Math.max(1, plugin.getConfig().getInt("flasks.cerulean.max-charges", 2));
        };
    }

    public boolean isSoulboundFlask(ItemStack item) {
        return getType(item) != null;
    }

    public EquipmentSlot resolveHeldFlaskHand(Player player, ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.equals(item)) {
            return EquipmentSlot.HAND;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.equals(item)) {
            return EquipmentSlot.OFF_HAND;
        }
        return null;
    }

    private Component text(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
