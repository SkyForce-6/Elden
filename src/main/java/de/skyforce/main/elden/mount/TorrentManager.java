package de.skyforce.main.elden.mount;

import de.skyforce.main.elden.flask.FlaskService;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.spiritspring.SpiritSpring;
import de.skyforce.main.elden.spiritspring.SpiritSpringManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class TorrentManager {

    private final JavaPlugin plugin;
    private final CustomModelDataRegistry customModelDataRegistry;
    private final NamespacedKey torrentKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey whistleKey;
    private final NamespacedKey whistleOwnerKey;
    private final NamespacedKey raisinTypeKey;
    private final NamespacedKey raisinOwnerKey;
    private final Map<UUID, UUID> torrentByOwner = new HashMap<>();
    private final Map<UUID, Long> whistleCooldownByPlayer = new HashMap<>();
    private final Map<UUID, Boolean> airJumpReadyByPlayer = new HashMap<>();
    private final Map<UUID, Long> spiritSpringCooldownByPlayer = new HashMap<>();
    private final Map<UUID, Long> spiritSpringLaunchedAtByPlayer = new HashMap<>();
    private final Map<UUID, String> spiritSpringApproachHintedByPlayer = new HashMap<>();
    private final Map<UUID, Long> revivePromptUntilByPlayer = new HashMap<>();
    private final Set<UUID> reviveRequiredByPlayer = new HashSet<>();

    private FlaskService flaskService;
    private SpiritSpringManager spiritSpringManager;

    public TorrentManager(JavaPlugin plugin, CustomModelDataRegistry customModelDataRegistry) {
        this.plugin = plugin;
        this.customModelDataRegistry = customModelDataRegistry;
        this.torrentKey = new NamespacedKey(plugin, "torrent");
        this.ownerKey = new NamespacedKey(plugin, "torrent-owner");
        this.whistleKey = new NamespacedKey(plugin, "spectral-steed-whistle");
        this.whistleOwnerKey = new NamespacedKey(plugin, "spectral-steed-whistle-owner");
        this.raisinTypeKey = new NamespacedKey(plugin, "torrent-raisin-type");
        this.raisinOwnerKey = new NamespacedKey(plugin, "torrent-raisin-owner");
    }

    public void setFlaskService(FlaskService flaskService) {
        this.flaskService = flaskService;
    }

    public void setSpiritSpringManager(SpiritSpringManager spiritSpringManager) {
        this.spiritSpringManager = spiritSpringManager;
    }

    public void toggle(Player player) {
        if (hasActiveTorrent(player)) {
            dismiss(player, true);
            return;
        }
        summon(player);
    }

    public void summon(Player player) {
        dismiss(player, false);

        Location spawnLocation = resolveSpawnLocation(player);
        Horse horse = (Horse) player.getWorld().spawnEntity(spawnLocation, EntityType.HORSE);
        configureTorrent(player, horse);
        torrentByOwner.put(player.getUniqueId(), horse.getUniqueId());
        airJumpReadyByPlayer.put(player.getUniqueId(), false);
        spiritSpringCooldownByPlayer.remove(player.getUniqueId());
        spiritSpringLaunchedAtByPlayer.remove(player.getUniqueId());
        spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
        reviveRequiredByPlayer.remove(player.getUniqueId());
        revivePromptUntilByPlayer.remove(player.getUniqueId());

        horse.addPassenger(player);
        player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0F, 1.35F);
        player.sendMessage(Component.text("Torrent answers your call.", NamedTextColor.AQUA));
    }

    public void dismiss(Player player, boolean notify) {
        UUID torrentId = torrentByOwner.remove(player.getUniqueId());
        airJumpReadyByPlayer.remove(player.getUniqueId());
        spiritSpringCooldownByPlayer.remove(player.getUniqueId());
        spiritSpringLaunchedAtByPlayer.remove(player.getUniqueId());
        spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
        revivePromptUntilByPlayer.remove(player.getUniqueId());
        if (torrentId == null) {
            if (notify) {
                player.sendMessage(Component.text("Torrent is not currently summoned.", NamedTextColor.GRAY));
            }
            return;
        }

        Entity entity = Bukkit.getEntity(torrentId);
        if (entity instanceof AbstractHorse horse && entity.isValid()) {
            removeTorrent(horse, false);
        }

        if (notify) {
            player.playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.8F, 0.7F);
            player.sendMessage(Component.text("Torrent fades away.", NamedTextColor.GRAY));
        }
    }

    public void handleDisconnect(Player player) {
        dismiss(player, false);
        whistleCooldownByPlayer.remove(player.getUniqueId());
        airJumpReadyByPlayer.remove(player.getUniqueId());
        spiritSpringCooldownByPlayer.remove(player.getUniqueId());
        spiritSpringLaunchedAtByPlayer.remove(player.getUniqueId());
        spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
        revivePromptUntilByPlayer.remove(player.getUniqueId());
    }

    public void handleDeath(Player player) {
        dismiss(player, false);
        airJumpReadyByPlayer.remove(player.getUniqueId());
        spiritSpringCooldownByPlayer.remove(player.getUniqueId());
        spiritSpringLaunchedAtByPlayer.remove(player.getUniqueId());
        spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
        revivePromptUntilByPlayer.remove(player.getUniqueId());
    }

    public void grantStarterWhistleIfMissing(Player player) {
        if (!plugin.getConfig().getBoolean("mounts.torrent.whistle.auto-give", false)) {
            return;
        }
        if (hasOwnedWhistle(player)) {
            return;
        }
        giveWhistle(player, false);
    }

    public boolean tryUseWhistle(Player player, ItemStack item, EquipmentSlot hand) {
        if (!isWhistle(item)) {
            return false;
        }

        if (!player.hasPermission("elden.mount.torrent")) {
            player.sendMessage(Component.text("You do not have permission to summon Torrent.", NamedTextColor.RED));
            return true;
        }

        if (!isOwnedWhistle(player, item)) {
            player.sendActionBar(Component.text("This whistle does not answer to you.", NamedTextColor.RED));
            return true;
        }

        long now = plugin.getServer().getCurrentTick();
        long cooldownUntil = whistleCooldownByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            long remaining = cooldownUntil - now;
            player.sendActionBar(Component.text("Whistle cooldown: " + remaining + "t", NamedTextColor.GRAY));
            return true;
        }

        if (hasActiveTorrent(player)) {
            mountExistingTorrent(player);
        } else if (reviveRequiredByPlayer.contains(player.getUniqueId())) {
            if (!tryReviveTorrent(player, now)) {
                return true;
            }
        } else {
            summon(player);
        }

        whistleCooldownByPlayer.put(player.getUniqueId(), now + whistleCooldownTicks());
        swingHand(player, hand);
        return true;
    }

    public void markJumpStarted(AbstractHorse horse, float power) {
        if (!isTorrent(horse) || power < 0.2F) {
            return;
        }

        Player rider = getOwnerPassenger(horse);
        if (rider == null) {
            return;
        }

        airJumpReadyByPlayer.put(rider.getUniqueId(), true);

        if (isNearSpiritSpring(horse.getLocation())) {
            long now = plugin.getServer().getCurrentTick();
            long cooldownUntil = spiritSpringCooldownByPlayer.getOrDefault(rider.getUniqueId(), 0L);
            if (now < cooldownUntil) {
                return;
            }
            spiritSpringCooldownByPlayer.put(rider.getUniqueId(), now + spiritSpringLaunchCooldownTicks());
            launchFromSpiritSpring(horse, true);
            rider.playSound(rider.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.1F);
            rider.sendActionBar(Component.text(spiritSpringActionbar(horse.getLocation()), NamedTextColor.AQUA));
        }
    }

    public void tryTriggerSpiritSpring(Player player) {
        AbstractHorse horse = getMountedTorrent(player);
        if (horse == null || horse.isDead() || !horse.isValid()) {
            spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
            return;
        }

        // Approach hint: warn the player when they ride near a named spring
        checkSpiritSpringApproachHint(player, horse);

        if (!horse.isOnGround() || !isNearSpiritSpring(horse.getLocation())) {
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        long cooldownUntil = spiritSpringCooldownByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            return;
        }

        launchFromSpiritSpring(horse, false);
        spiritSpringCooldownByPlayer.put(player.getUniqueId(), now + spiritSpringLaunchCooldownTicks());
        spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.1F);
        player.sendActionBar(Component.text(spiritSpringActionbar(horse.getLocation()), NamedTextColor.AQUA));
    }

    public void tryAirJump(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse) || !isTorrent(horse)) {
            return;
        }
        if (!isOwner(player, horse) || horse.isOnGround()) {
            return;
        }
        if (!airJumpReadyByPlayer.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        Vector direction = horizontalDirection(horse.getLocation(), player);
        double forwardBoost = Math.max(0.0D, plugin.getConfig().getDouble("mounts.torrent.air-jump.forward-boost", 0.75D));
        double verticalBoost = Math.max(0.0D, plugin.getConfig().getDouble("mounts.torrent.air-jump.vertical-boost", 0.65D));
        Vector boost = direction.multiply(forwardBoost).setY(verticalBoost);
        horse.setVelocity(boost);
        airJumpReadyByPlayer.put(player.getUniqueId(), false);
        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_JUMP, 1.0F, 1.35F);
        player.sendActionBar(Component.text("Torrent leaps again.", NamedTextColor.AQUA));
    }

    public boolean tryUseRaisin(Player player, ItemStack item, EquipmentSlot hand) {
        RaisinType type = getRaisinType(item);
        if (type == null) {
            return false;
        }
        if (!isOwnedRaisin(player, item)) {
            player.sendActionBar(Component.text("This treat belongs to another steed.", NamedTextColor.RED));
            return true;
        }

        AbstractHorse horse = getMountedTorrent(player);
        if (horse == null) {
            player.sendActionBar(Component.text("Mount Torrent to feed him.", NamedTextColor.GRAY));
            return true;
        }

        if (healTorrent(horse, type.heal(this), true)) {
            consumeOne(player, hand, item);
            player.sendActionBar(Component.text(type.displayName + " restored Torrent.", NamedTextColor.GOLD));
        } else {
            player.sendActionBar(Component.text("Torrent is already at full health.", NamedTextColor.GRAY));
        }
        return true;
    }

    public void giveWhistle(Player player, boolean notify) {
        Inventory inventory = player.getInventory();
        ItemStack whistle = createWhistle(player);
        Map<Integer, ItemStack> leftover = inventory.addItem(whistle);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), whistle);
        }
        if (notify) {
            player.sendMessage(Component.text("You received the Spectral Steed Whistle.", NamedTextColor.GREEN));
        }
    }

    public void giveRaisins(Player player, RaisinType type, int amount, boolean notify) {
        int stackAmount = Math.max(1, amount);
        ItemStack raisins = createRaisin(player, type, stackAmount);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(raisins);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), raisins);
        }
        if (notify) {
            player.sendMessage(Component.text("You received " + stackAmount + " " + type.displayName + ".", NamedTextColor.GREEN));
        }
    }

    public boolean healMountedTorrent(Player player, double amount) {
        AbstractHorse horse = getMountedTorrent(player);
        return healTorrent(horse, amount, false);
    }

    public void handleTorrentDeath(Entity entity) {
        if (!(entity instanceof AbstractHorse horse) || !isTorrent(horse)) {
            return;
        }
        UUID ownerId = resolveOwnerId(horse);
        if (ownerId != null) {
            torrentByOwner.remove(ownerId);
            airJumpReadyByPlayer.remove(ownerId);
            spiritSpringCooldownByPlayer.remove(ownerId);
            spiritSpringLaunchedAtByPlayer.remove(ownerId);
            spiritSpringApproachHintedByPlayer.remove(ownerId);
            reviveRequiredByPlayer.add(ownerId);
            revivePromptUntilByPlayer.remove(ownerId);
        }
    }

    public void handleFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            AbstractHorse horse = getMountedTorrent(player);
            if (horse == null) {
                return;
            }
            handleMountedFall(player, horse, event);
            return;
        }

        if (event.getEntity() instanceof AbstractHorse horse && isTorrent(horse)) {
            Player rider = getOwnerPassenger(horse);
            handleMountedFall(rider, horse, event);
        }
    }

    public boolean hasActiveTorrent(Player player) {
        Entity entity = getTorrentEntity(player);
        return entity instanceof AbstractHorse horse && horse.isValid() && !horse.isDead();
    }

    public boolean isTorrent(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(torrentKey, PersistentDataType.BYTE);
    }

    public boolean isOwner(Player player, AbstractHorse horse) {
        String ownerId = horse.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return ownerId != null && ownerId.equals(player.getUniqueId().toString());
    }

    public boolean isProtectedFromDamage() {
        return plugin.getConfig().getBoolean("mounts.torrent.protect-from-damage", true);
    }

    public void shutdown() {
        for (UUID ownerId : torrentByOwner.keySet().toArray(UUID[]::new)) {
            Player player = Bukkit.getPlayer(ownerId);
            if (player != null) {
                dismiss(player, false);
                continue;
            }

            UUID torrentId = torrentByOwner.remove(ownerId);
            if (torrentId == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(torrentId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        whistleCooldownByPlayer.clear();
        airJumpReadyByPlayer.clear();
        spiritSpringCooldownByPlayer.clear();
        spiritSpringLaunchedAtByPlayer.clear();
        spiritSpringApproachHintedByPlayer.clear();
        revivePromptUntilByPlayer.clear();
        reviveRequiredByPlayer.clear();
    }

    private boolean tryReviveTorrent(Player player, long now) {
        long promptUntil = revivePromptUntilByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now > promptUntil) {
            revivePromptUntilByPlayer.put(player.getUniqueId(), now + reviveConfirmWindowTicks());
            player.sendActionBar(Component.text("Use the whistle again to spend 1 Crimson charge and revive Torrent.", NamedTextColor.GOLD));
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, 1.2F);
            return false;
        }

        if (flaskService == null || !flaskService.consumeCrimsonCharge(player)) {
            player.sendActionBar(Component.text("A Crimson Flask charge is required to revive Torrent.", NamedTextColor.RED));
            return false;
        }

        revivePromptUntilByPlayer.remove(player.getUniqueId());
        reviveRequiredByPlayer.remove(player.getUniqueId());
        summon(player);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0F, 1.25F);
        player.sendMessage(Component.text("A Crimson Flask charge revives Torrent.", NamedTextColor.GOLD));
        return true;
    }

    private void mountExistingTorrent(Player player) {
        Entity entity = getTorrentEntity(player);
        if (!(entity instanceof AbstractHorse horse) || !horse.isValid() || horse.isDead()) {
            summon(player);
            return;
        }

        if (horse.getPassengers().contains(player)) {
            return;
        }

        horse.teleport(resolveSpawnLocation(player));
        horse.addPassenger(player);
        player.sendActionBar(Component.text("You mount Torrent.", NamedTextColor.AQUA));
    }

    private void handleMountedFall(Player rider, AbstractHorse horse, EntityDamageEvent event) {
        double fallDistance = Math.max(event.getDamage(), rider != null ? rider.getFallDistance() : horse.getFallDistance());
        Location landing = horse.getLocation();
        if (isNearSpiritSpring(landing)) {
            event.setCancelled(true);
            resetFallDistance(rider, horse);
            if (rider != null) {
                rider.playSound(rider.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.3F);
            }
            return;
        }

        // Cancel fall damage for a window after a spirit spring launch
        if (rider != null) {
            long launchedAt = spiritSpringLaunchedAtByPlayer.getOrDefault(rider.getUniqueId(), -1L);
            long now = plugin.getServer().getCurrentTick();
            long immunityTicks = Math.max(0L, plugin.getConfig().getLong("mounts.torrent.spirit-spring.fall-immunity-ticks", 300L));
            if (launchedAt >= 0 && (now - launchedAt) <= immunityTicks) {
                event.setCancelled(true);
                resetFallDistance(rider, horse);
                return;
            }
        }

        double fatalDistance = Math.max(0.0D, plugin.getConfig().getDouble("mounts.torrent.fall.fatal-distance", 14.0D));
        if (fallDistance >= fatalDistance) {
            if (rider != null && rider.isValid() && rider.getHealth() > 0.0D) {
                rider.setHealth(0.0D);
            }
            removeTorrent(horse, true);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        resetFallDistance(rider, horse);
    }

    private void removeTorrent(AbstractHorse horse, boolean requiresRevive) {
        UUID ownerId = resolveOwnerId(horse);
        if (ownerId != null) {
            torrentByOwner.remove(ownerId);
            airJumpReadyByPlayer.remove(ownerId);
            spiritSpringCooldownByPlayer.remove(ownerId);
            spiritSpringLaunchedAtByPlayer.remove(ownerId);
            spiritSpringApproachHintedByPlayer.remove(ownerId);
            revivePromptUntilByPlayer.remove(ownerId);
            if (requiresRevive) {
                reviveRequiredByPlayer.add(ownerId);
            }
        }
        horse.remove();
    }

    private boolean healTorrent(AbstractHorse horse, double amount, boolean playFeedSound) {
        if (horse == null || horse.isDead() || !horse.isValid()) {
            return false;
        }

        double healAmount = Math.max(0.0D, amount);
        if (healAmount <= 0.0D) {
            return false;
        }

        double maxHealth = horse.getAttribute(Attribute.MAX_HEALTH) == null
                ? horse.getHealth()
                : horse.getAttribute(Attribute.MAX_HEALTH).getValue();
        double nextHealth = Math.min(maxHealth, horse.getHealth() + healAmount);
        if (nextHealth <= horse.getHealth()) {
            return false;
        }

        horse.setHealth(nextHealth);
        if (playFeedSound) {
            horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_HORSE_EAT, 1.0F, 1.1F);
        }
        return true;
    }

    private Entity getTorrentEntity(Player player) {
        UUID torrentId = torrentByOwner.get(player.getUniqueId());
        if (torrentId == null) {
            return null;
        }

        Entity entity = Bukkit.getEntity(torrentId);
        if (entity == null || !entity.isValid() || entity.isDead()) {
            torrentByOwner.remove(player.getUniqueId());
            airJumpReadyByPlayer.remove(player.getUniqueId());
            spiritSpringCooldownByPlayer.remove(player.getUniqueId());
            spiritSpringLaunchedAtByPlayer.remove(player.getUniqueId());
            return null;
        }
        return entity;
    }

    private AbstractHorse getMountedTorrent(Player player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractHorse horse && isTorrent(horse) && isOwner(player, horse)) {
            return horse;
        }
        return null;
    }

    private void configureTorrent(Player player, Horse horse) {
        FileConfiguration config = plugin.getConfig();
        PersistentDataContainer data = horse.getPersistentDataContainer();
        data.set(torrentKey, PersistentDataType.BYTE, (byte) 1);
        data.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());

        horse.customName(Component.text("Torrent", NamedTextColor.AQUA));
        horse.setCustomNameVisible(config.getBoolean("mounts.torrent.show-name", true));
        horse.setOwner(player);
        horse.setTamed(true);
        horse.setAdult();
        horse.setRemoveWhenFarAway(false);
        horse.setPersistent(false);
        horse.setDomestication(horse.getMaxDomestication());
        horse.setColor(Horse.Color.GRAY);
        horse.setStyle(Horse.Style.BLACK_DOTS);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));

        double maxHealth = Math.max(1.0D, config.getDouble("mounts.torrent.max-health", 30.0D));
        double moveSpeed = Math.max(0.05D, config.getDouble("mounts.torrent.movement-speed", 0.33D));
        double jumpStrength = Math.max(0.1D, config.getDouble("mounts.torrent.jump-strength", 0.9D));

        if (horse.getAttribute(Attribute.MAX_HEALTH) != null) {
            horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
        }
        horse.setHealth(maxHealth);

        if (horse.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(moveSpeed);
        }

        if (horse.getAttribute(Attribute.JUMP_STRENGTH) != null) {
            horse.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(jumpStrength);
        }
    }

    private ItemStack createWhistle(Player owner) {
        ItemStack item = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Spectral Steed Whistle", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Calls Torrent to your side", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click to summon or dismiss", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Sneak mid-air while mounted for a second leap", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Use twice after death to revive Torrent", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        Integer customModelData = customModelDataRegistry.torrentWhistle();
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(whistleKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(whistleOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRaisin(Player owner, RaisinType type, int amount) {
        ItemStack item = new ItemStack(type.material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.displayName, type.color).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Feed to Torrent while mounted", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Restores " + type.heal(this) + " Torrent HP", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        Integer customModelData = customModelDataRegistry.torrentRaisin(type.key);
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(raisinTypeKey, PersistentDataType.STRING, type.key);
        pdc.set(raisinOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private boolean isWhistle(ItemStack item) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(whistleKey, PersistentDataType.BYTE);
    }

    private boolean isOwnedWhistle(Player player, ItemStack item) {
        String owner = item.getItemMeta().getPersistentDataContainer().get(whistleOwnerKey, PersistentDataType.STRING);
        return owner != null && owner.equals(player.getUniqueId().toString());
    }

    private boolean hasOwnedWhistle(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWhistle(item) && isOwnedWhistle(player, item)) {
                return true;
            }
        }
        return false;
    }

    private RaisinType getRaisinType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String key = item.getItemMeta().getPersistentDataContainer().get(raisinTypeKey, PersistentDataType.STRING);
        if (key == null || key.isBlank()) {
            return null;
        }
        for (RaisinType type : RaisinType.values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    private boolean isOwnedRaisin(Player player, ItemStack item) {
        String owner = item.getItemMeta().getPersistentDataContainer().get(raisinOwnerKey, PersistentDataType.STRING);
        return owner != null && owner.equals(player.getUniqueId().toString());
    }

    private long whistleCooldownTicks() {
        return Math.max(0L, plugin.getConfig().getLong("mounts.torrent.whistle.cooldown-ticks", 20L));
    }

    private long reviveConfirmWindowTicks() {
        return Math.max(1L, plugin.getConfig().getLong("mounts.torrent.revive.confirm-window-ticks", 60L));
    }

    private Player getOwnerPassenger(AbstractHorse horse) {
        for (Entity passenger : horse.getPassengers()) {
            if (passenger instanceof Player player && isOwner(player, horse)) {
                return player;
            }
        }
        return null;
    }

    private UUID resolveOwnerId(AbstractHorse horse) {
        String owner = horse.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isNearSpiritSpring(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (resolveNearbySpiritSpring(location).isPresent()) {
            return true;
        }

        int radius = Math.max(0, plugin.getConfig().getInt("mounts.torrent.spirit-spring.radius", 1));
        Set<Material> materials = spiritSpringMaterials();
        if (materials.isEmpty()) {
            return false;
        }

        Location base = location.getBlock().getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 1; y++) {
                    Material material = base.clone().add(x, y, z).getBlock().getType();
                    if (materials.contains(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Set<Material> spiritSpringMaterials() {
        List<String> configured = plugin.getConfig().getStringList("mounts.torrent.spirit-spring.materials");
        if (configured.isEmpty()) {
            return EnumSet.of(Material.BUBBLE_COLUMN);
        }

        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String value : configured) {
            try {
                materials.add(Material.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid materials so one typo does not disable the feature.
            }
        }
        return materials;
    }

    private void launchFromSpiritSpring(AbstractHorse horse, boolean keepExistingMomentum) {
        Vector direction = horizontalDirection(horse.getLocation(), getOwnerPassenger(horse));
        double forwardBoost = Math.max(0.0D, plugin.getConfig().getDouble("mounts.torrent.spirit-spring.forward-boost", 0.35D));
        double verticalBoost = Math.max(0.0D, plugin.getConfig().getDouble("mounts.torrent.spirit-spring.vertical-boost", 1.45D));
        Vector launch = direction.multiply(forwardBoost).setY(verticalBoost);
        if (keepExistingMomentum) {
            Vector velocity = horse.getVelocity();
            launch.add(new Vector(velocity.getX(), 0.0D, velocity.getZ()).multiply(0.35D));
        }
        horse.setVelocity(launch);
        horse.setFallDistance(0.0F);
        Player rider = getOwnerPassenger(horse);
        if (rider != null) {
            rider.setFallDistance(0.0F);
            spiritSpringLaunchedAtByPlayer.put(rider.getUniqueId(), (long) plugin.getServer().getCurrentTick());
        }
        playSpiritSpringLaunchEffects(horse, launch);
    }

    private void checkSpiritSpringApproachHint(Player player, AbstractHorse horse) {
        if (spiritSpringManager == null) {
            return;
        }
        double approachRadius = Math.max(
                spiritSpringManager.triggerRadius() + 0.5D,
                plugin.getConfig().getDouble("mounts.torrent.spirit-spring.approach-hint-radius", 6.0D)
        );
        Optional<SpiritSpring> nearby = spiritSpringManager.findNearest(horse.getLocation(), approachRadius);
        if (nearby.isEmpty()) {
            spiritSpringApproachHintedByPlayer.remove(player.getUniqueId());
            return;
        }

        String key = nearby.get().key();
        String lastHinted = spiritSpringApproachHintedByPlayer.get(player.getUniqueId());
        if (key.equals(lastHinted)) {
            // Already shown hint for this spring - don't spam
            return;
        }

        spiritSpringApproachHintedByPlayer.put(player.getUniqueId(), key);
        player.sendActionBar(Component.text("A spirit spring lies ahead: " + nearby.get().displayName(), NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.45F, 1.6F);
    }

    private Optional<SpiritSpring> resolveNearbySpiritSpring(Location location) {
        if (spiritSpringManager == null) {
            return Optional.empty();
        }
        return spiritSpringManager.findActiveSpiritSpring(location);
    }

    private String spiritSpringActionbar(Location location) {
        return resolveNearbySpiritSpring(location)
                .map(spring -> "Torrent rides the spirit spring: " + spring.displayName())
                .orElse("Torrent rides the spirit spring.");
    }

    private long spiritSpringLaunchCooldownTicks() {
        return Math.max(1L, plugin.getConfig().getLong("mounts.torrent.spirit-spring.launch-cooldown-ticks", 12L));
    }

    private void playSpiritSpringLaunchEffects(AbstractHorse horse, Vector launch) {
        Location center = horse.getLocation().clone().add(0.0D, 0.2D, 0.0D);
        horse.getWorld().spawnParticle(Particle.CLOUD, center, 28, 0.4D, 0.18D, 0.4D, 0.03D);
        horse.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0.0D, 0.8D, 0.0D), 16, 0.25D, 0.7D, 0.25D, 0.03D);
        horse.getWorld().spawnParticle(Particle.SCULK_SOUL, center.clone().add(0.0D, 0.6D, 0.0D), 20, 0.35D, 0.55D, 0.35D, 0.04D);
        horse.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, SoundCategory.MASTER, 1.1F, 0.9F);
        horse.getWorld().playSound(center, Sound.ENTITY_HORSE_JUMP, SoundCategory.MASTER, 0.85F, 0.75F);
        resolveNearbySpiritSpring(horse.getLocation()).ifPresent(spring -> spiritSpringManager.playLaunchBurst(spring, launch));
    }

    private Vector horizontalDirection(Location horseLocation, Player fallbackPlayer) {
        Vector direction = horseLocation.getDirection().setY(0);
        if (direction.lengthSquared() < 0.001D && fallbackPlayer != null) {
            direction = fallbackPlayer.getLocation().getDirection().setY(0);
        }
        if (direction.lengthSquared() < 0.001D) {
            direction = new Vector(0, 0, 1);
        }
        return direction.normalize();
    }

    private void resetFallDistance(Player rider, AbstractHorse horse) {
        horse.setFallDistance(0.0F);
        if (rider != null) {
            rider.setFallDistance(0.0F);
        }
    }

    private void consumeOne(Player player, EquipmentSlot hand, ItemStack item) {
        int nextAmount = item.getAmount() - 1;
        if (nextAmount <= 0) {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }

        item.setAmount(nextAmount);
        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(item);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        }
    }

    private void swingHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.HAND) {
            player.swingMainHand();
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.swingOffHand();
        }
    }

    private Location resolveSpawnLocation(Player player) {
        Location base = player.getLocation().clone();
        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.001D) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize().multiply(1.5D);
        return base.add(direction);
    }

    public enum RaisinType {
        ROWA("rowa", "Rowa Raisin", Material.WHEAT, NamedTextColor.YELLOW, "mounts.torrent.healing.rowa"),
        SWEET("sweet", "Sweet Raisin", Material.SWEET_BERRIES, NamedTextColor.LIGHT_PURPLE, "mounts.torrent.healing.sweet"),
        FROZEN("frozen", "Frozen Raisin", Material.SNOWBALL, NamedTextColor.AQUA, "mounts.torrent.healing.frozen");

        private final String key;
        private final String displayName;
        private final Material material;
        private final NamedTextColor color;
        private final String healPath;

        RaisinType(String key, String displayName, Material material, NamedTextColor color, String healPath) {
            this.key = key;
            this.displayName = displayName;
            this.material = material;
            this.color = color;
            this.healPath = healPath;
        }

        public double heal(TorrentManager manager) {
            return Math.max(0.0D, manager.plugin.getConfig().getDouble(healPath, 6.0D));
        }

        public static RaisinType fromInput(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            for (RaisinType type : values()) {
                if (type.key.equalsIgnoreCase(input)) {
                    return type;
                }
            }
            return null;
        }
    }
}
