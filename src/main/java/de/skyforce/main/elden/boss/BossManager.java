package de.skyforce.main.elden.boss;

import de.skyforce.main.elden.ashes.model.AshOfWarDefinition;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import de.skyforce.main.elden.boss.model.BossArchetype;
import de.skyforce.main.elden.boss.model.BossDefinition;
import de.skyforce.main.elden.boss.model.BossRewardDefinition;
import de.skyforce.main.elden.boss.model.BossRewardType;
import de.skyforce.main.elden.boss.registry.BossRegistry;
import de.skyforce.main.elden.persistence.PlayerDataRepository;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.spell.model.SpellDefinition;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import de.skyforce.main.elden.spirit.service.SpiritAshItemFactory;
import de.skyforce.main.elden.talisman.registry.TalismanRegistry;
import de.skyforce.main.elden.talisman.service.TalismanItemFactory;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class BossManager {

    private final JavaPlugin plugin;
    private final RuneManager runeManager;
    private final BossRegistry bossRegistry;
    private final PlayerDataRepository playerDataRepository;
    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final AshOfWarRegistry ashOfWarRegistry;
    private final AshOfWarItemFactory ashOfWarItemFactory;
    private final SpellRegistry spellRegistry;
    private final SpellItemFactory spellItemFactory;
    private final SpiritAshRegistry spiritAshRegistry;
    private final SpiritAshItemFactory spiritAshItemFactory;
    private final TalismanRegistry talismanRegistry;
    private final TalismanItemFactory talismanItemFactory;
    private final NamespacedKey bossKey;
    private final NamespacedKey bossIdKey;
    private final NamespacedKey remembranceIdKey;
    private final Map<UUID, ActiveBoss> activeBosses = new HashMap<>();
    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private final Map<UUID, ActiveHazard> activeHazards = new HashMap<>();
    private final Map<UUID, Set<String>> claimedFirstKillRewardsByPlayer;
    private final Map<UUID, Set<String>> grantedRemembrancesByPlayer;
    private final BukkitTask tickTask;

    public BossManager(JavaPlugin plugin,
                       RuneManager runeManager,
                       BossRegistry bossRegistry,
                       PlayerDataRepository playerDataRepository,
                       WeaponRegistry weaponRegistry,
                       WeaponItemFactory weaponItemFactory,
                       AshOfWarRegistry ashOfWarRegistry,
                       AshOfWarItemFactory ashOfWarItemFactory,
                       SpellRegistry spellRegistry,
                       SpellItemFactory spellItemFactory,
                       SpiritAshRegistry spiritAshRegistry,
                       SpiritAshItemFactory spiritAshItemFactory,
                       TalismanRegistry talismanRegistry,
                       TalismanItemFactory talismanItemFactory) {
        this.plugin = plugin;
        this.runeManager = runeManager;
        this.bossRegistry = bossRegistry;
        this.playerDataRepository = playerDataRepository;
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.ashOfWarRegistry = ashOfWarRegistry;
        this.ashOfWarItemFactory = ashOfWarItemFactory;
        this.spellRegistry = spellRegistry;
        this.spellItemFactory = spellItemFactory;
        this.spiritAshRegistry = spiritAshRegistry;
        this.spiritAshItemFactory = spiritAshItemFactory;
        this.talismanRegistry = talismanRegistry;
        this.talismanItemFactory = talismanItemFactory;
        this.bossKey = new NamespacedKey(plugin, "elden-boss");
        this.bossIdKey = new NamespacedKey(plugin, "elden-boss-id");
        this.remembranceIdKey = new NamespacedKey(plugin, "elden-remembrance-id");
        this.claimedFirstKillRewardsByPlayer = new HashMap<>(playerDataRepository.loadClaimedBossFirstKillRewards());
        this.grantedRemembrancesByPlayer = new HashMap<>(playerDataRepository.loadGrantedBossRemembrances());
        this.tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickBosses, 1L, 10L);
    }

    public boolean spawnBoss(BossDefinition definition, Location location) {
        return spawnBossTracked(definition, location) != null;
    }

    public UUID spawnBossTracked(BossDefinition definition, Location location) {
        if (definition == null || location == null || location.getWorld() == null) {
            return null;
        }
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.entityType());
        prepareBossEntity(entity, definition);
        BossBar bossBar = Bukkit.createBossBar(definition.displayName(), BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
        activeBosses.put(entity.getUniqueId(), new ActiveBoss(
                definition,
                entity.getUniqueId(),
                location.clone(),
                bossBar,
                new HashSet<>(),
                plugin.getServer().getCurrentTick(),
                1
        ));
        entity.getWorld().strikeLightningEffect(entity.getLocation());
        entity.getWorld().spawnParticle(definition.ambientParticle(), entity.getLocation().add(0.0D, 1.0D, 0.0D), 80, 0.8D, 1.1D, 0.8D, 0.02D);
        entity.customName(displayName(definition.displayName()));
        entity.setCustomNameVisible(true);
        return entity.getUniqueId();
    }

    public int loadConfiguredBosses(BossRegistry bossRegistry) {
        if (!plugin.getConfig().getBoolean("bosses.enabled", true)) {
            return 0;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bosses.spawns");
        if (section == null) {
            return 0;
        }

        int spawned = 0;
        for (String key : section.getKeys(false)) {
            String basePath = "bosses.spawns." + key;
            String bossId = plugin.getConfig().getString(basePath + ".boss-id");
            String worldName = plugin.getConfig().getString(basePath + ".world");
            if (bossId == null || worldName == null) {
                plugin.getLogger().warning("Boss spawn '" + key + "' ignored: missing boss-id or world.");
                continue;
            }

            BossDefinition definition = bossRegistry.getById(bossId).orElse(null);
            if (definition == null) {
                plugin.getLogger().warning("Boss spawn '" + key + "' ignored: unknown boss-id '" + bossId + "'.");
                continue;
            }

            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Boss spawn '" + key + "' ignored: world not found '" + worldName + "'.");
                continue;
            }

            double x = plugin.getConfig().getDouble(basePath + ".x");
            double y = plugin.getConfig().getDouble(basePath + ".y");
            double z = plugin.getConfig().getDouble(basePath + ".z");
            float yaw = (float) plugin.getConfig().getDouble(basePath + ".yaw");
            float pitch = (float) plugin.getConfig().getDouble(basePath + ".pitch");

            if (spawnBoss(definition, new Location(world, x, y, z, yaw, pitch))) {
                spawned++;
            }
        }
        return spawned;
    }

    public boolean saveConfiguredSpawn(String spawnName, BossDefinition definition, Location location) {
        if (spawnName == null || spawnName.isBlank() || definition == null || location == null || location.getWorld() == null) {
            return false;
        }

        String basePath = "bosses.spawns." + normalizeSpawnName(spawnName);
        plugin.getConfig().set("bosses.enabled", true);
        plugin.getConfig().set(basePath + ".boss-id", definition.id());
        plugin.getConfig().set(basePath + ".world", location.getWorld().getName());
        plugin.getConfig().set(basePath + ".x", location.getX());
        plugin.getConfig().set(basePath + ".y", location.getY());
        plugin.getConfig().set(basePath + ".z", location.getZ());
        plugin.getConfig().set(basePath + ".yaw", location.getYaw());
        plugin.getConfig().set(basePath + ".pitch", location.getPitch());
        plugin.saveConfig();
        return true;
    }

    public boolean removeConfiguredSpawn(String spawnName) {
        if (spawnName == null || spawnName.isBlank()) {
            return false;
        }

        String basePath = "bosses.spawns." + normalizeSpawnName(spawnName);
        if (!plugin.getConfig().contains(basePath)) {
            return false;
        }
        plugin.getConfig().set(basePath, null);
        plugin.saveConfig();
        return true;
    }

    public Map<String, String> getConfiguredSpawns() {
        Map<String, String> spawns = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bosses.spawns");
        if (section == null) {
            return spawns;
        }
        for (String key : section.getKeys(false)) {
            spawns.put(key, plugin.getConfig().getString("bosses.spawns." + key + ".boss-id", "unknown"));
        }
        return spawns;
    }

    public boolean despawnBoss(UUID entityId, boolean broadcast) {
        ActiveBoss activeBoss = activeBosses.remove(entityId);
        abilityCooldowns.remove(entityId);
        if (activeBoss == null) {
            return false;
        }
        clearBossExtras(activeBoss);
        activeBoss.bossBar.removeAll();
        Entity entity = Bukkit.getEntity(activeBoss.entityId);
        if (entity != null && entity.isValid()) {
            if (broadcast) {
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 0.6F, 1.8F);
            }
            entity.remove();
        }
        return true;
    }

    public int despawnAll() {
        int count = activeBosses.size();
        for (UUID entityId : Set.copyOf(activeBosses.keySet())) {
            despawnBoss(entityId, false);
        }
        return count;
    }

    public boolean isBoss(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }
        return livingEntity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE);
    }

    public void recordParticipant(Player player, Entity entity) {
        if (player == null || entity == null) {
            return;
        }
        ActiveBoss activeBoss = activeBosses.get(entity.getUniqueId());
        if (activeBoss == null) {
            return;
        }
        activeBoss.participants.add(player.getUniqueId());
        activeBoss.lastCombatTick = plugin.getServer().getCurrentTick();
    }

    public void handleBossHitPlayer(LivingEntity boss, Player player) {
        ActiveBoss activeBoss = activeBosses.get(boss.getUniqueId());
        if (activeBoss == null) {
            return;
        }
        activeBoss.participants.add(player.getUniqueId());
        activeBoss.lastCombatTick = plugin.getServer().getCurrentTick();
    }

    public void handleBossProjectileHit(Projectile projectile, Player player) {
        if (!(projectile.getShooter() instanceof LivingEntity boss) || !isBoss(boss)) {
            return;
        }
        ActiveBoss activeBoss = activeBosses.get(boss.getUniqueId());
        if (activeBoss == null) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60 + activeBoss.phase * 20, Math.max(0, activeBoss.phase - 1), false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30 + activeBoss.phase * 10, 0, false, true));
        activeBoss.participants.add(player.getUniqueId());
        activeBoss.lastCombatTick = plugin.getServer().getCurrentTick();
    }

    public void handleBossMeleeEffect(LivingEntity boss, Player player) {
        ActiveBoss activeBoss = activeBosses.get(boss.getUniqueId());
        if (activeBoss == null) {
            return;
        }
        switch (activeBoss.definition.archetype()) {
            case TREE_SENTINEL -> {
                if (activeBoss.phase >= 2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30 + activeBoss.phase * 10, 0, false, true));
                }
            }
            case NIGHT_CAVALRY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40 + activeBoss.phase * 10, Math.max(0, activeBoss.phase - 2), false, true));
                if (activeBoss.phase >= 3) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 50, 0, false, true));
                }
            }
        }
    }

    public void handleBossDeath(LivingEntity boss) {
        ActiveBoss activeBoss = activeBosses.remove(boss.getUniqueId());
        abilityCooldowns.remove(boss.getUniqueId());
        if (activeBoss == null) {
            return;
        }
        clearBossExtras(activeBoss);
        activeBoss.bossBar.removeAll();
        rewardParticipants(activeBoss, boss.getLocation());
        boss.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, boss.getLocation().add(0.0D, 1.2D, 0.0D), 1);
        boss.getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.9F, 0.95F);
    }

    public void shutdown() {
        tickTask.cancel();
        abilityCooldowns.clear();
        activeHazards.clear();
        despawnAll();
    }

    public Map<UUID, String> getActiveBossNames() {
        Map<UUID, String> result = new HashMap<>();
        for (ActiveBoss boss : activeBosses.values()) {
            result.put(boss.entityId, boss.definition.displayName());
        }
        return result;
    }

    public RemembrancePreview inspectHeldRemembrance(Player player) {
        if (player == null) {
            return null;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String remembranceId = meta.getPersistentDataContainer().get(remembranceIdKey, PersistentDataType.STRING);
        String bossId = meta.getPersistentDataContainer().get(bossIdKey, PersistentDataType.STRING);
        if (remembranceId == null || remembranceId.isBlank() || bossId == null || bossId.isBlank()) {
            return null;
        }

        BossDefinition definition = bossRegistry.getById(bossId).orElse(null);
        if (definition == null || definition.rewards() == null) {
            return null;
        }

        BossRewardDefinition rewards = definition.rewards();
        return new RemembrancePreview(
                definition.id(),
                definition.displayName(),
                rewards.remembranceName(),
                rewards.remembranceExchangeRewardName(),
                rewards.remembranceRuneValue()
        );
    }

    public ExchangeResult exchangeHeldRemembrance(Player player, String option) {
        RemembrancePreview preview = inspectHeldRemembrance(player);
        if (preview == null) {
            return ExchangeResult.error("Hold a boss remembrance in your main hand.");
        }

        String normalizedOption = option == null ? "" : option.toLowerCase(Locale.ROOT).trim();
        BossDefinition definition = bossRegistry.getById(preview.bossId()).orElse(null);
        if (definition == null || definition.rewards() == null) {
            return ExchangeResult.error("That remembrance is not linked to a valid boss reward.");
        }

        BossRewardDefinition rewards = definition.rewards();
        switch (normalizedOption) {
            case "reward" -> {
                if (rewards.remembranceExchangeRewardType() == null
                        || rewards.remembranceExchangeRewardId() == null
                        || rewards.remembranceExchangeRewardId().isBlank()) {
                    return ExchangeResult.error("This remembrance has no exchange reward configured.");
                }
                ItemStack reward = createRewardItem(rewards.remembranceExchangeRewardType(), rewards.remembranceExchangeRewardId());
                if (reward == null) {
                    return ExchangeResult.error("The configured remembrance reward could not be created.");
                }
                consumeMainHandItem(player);
                giveItem(player, reward);
                return ExchangeResult.success("Exchanged remembrance for " + rewards.remembranceExchangeRewardName() + ".");
            }
            case "runes" -> {
                if (rewards.remembranceRuneValue() <= 0) {
                    return ExchangeResult.error("This remembrance cannot be exchanged for runes.");
                }
                consumeMainHandItem(player);
                runeManager.addRunes(player, rewards.remembranceRuneValue(), true);
                return ExchangeResult.success("Exchanged remembrance for " + rewards.remembranceRuneValue() + " runes.");
            }
            default -> {
                return ExchangeResult.error("Use /boss remembrance exchange <reward|runes>.");
            }
        }
    }

    private void tickBosses() {
        long now = plugin.getServer().getCurrentTick();
        Iterator<Map.Entry<UUID, ActiveBoss>> iterator = activeBosses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveBoss> entry = iterator.next();
            ActiveBoss activeBoss = entry.getValue();
            Entity entity = Bukkit.getEntity(activeBoss.entityId);
            if (!(entity instanceof LivingEntity boss) || !entity.isValid() || boss.isDead()) {
                activeBoss.bossBar.removeAll();
                abilityCooldowns.remove(activeBoss.entityId);
                iterator.remove();
                continue;
            }

            updateBossBar(activeBoss, boss);
            updateAudience(activeBoss, boss);
            tickHazards(activeBoss, boss, now);
            maybeAdvancePhase(activeBoss, boss);
            keepBossLeashed(activeBoss, boss);
            retargetNearbyPlayer(activeBoss, boss);
            maybeUseBossAbility(activeBoss, boss, now);
            maybeResetFight(activeBoss, boss, now);
        }
    }

    private void prepareBossEntity(LivingEntity entity, BossDefinition definition) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(bossKey, PersistentDataType.BYTE, (byte) 1);
        container.set(bossIdKey, PersistentDataType.STRING, definition.id());
        Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(definition.maxHealth());
        entity.setHealth(definition.maxHealth());
        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(definition.baseDamage());
        }
        if (entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(definition.movementSpeed());
        }
        if (entity.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.FOLLOW_RANGE)).setBaseValue(Math.max(32.0D, definition.arenaRadius()));
        }
        if (definition.archetype() == BossArchetype.TREE_SENTINEL) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        }
    }

    private void updateBossBar(ActiveBoss activeBoss, LivingEntity boss) {
        double maxHealth = Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue();
        double progress = maxHealth <= 0.0D ? 0.0D : Math.max(0.0D, Math.min(1.0D, boss.getHealth() / maxHealth));
        activeBoss.bossBar.setProgress(progress);
        activeBoss.bossBar.setTitle(activeBoss.definition.displayName() + " | Phase " + activeBoss.phase + " | " + (int) Math.ceil(boss.getHealth()) + " HP");
    }

    private void updateAudience(ActiveBoss activeBoss, LivingEntity boss) {
        for (Player player : boss.getWorld().getPlayers()) {
            double distanceSquared = player.getLocation().distanceSquared(boss.getLocation());
            boolean canSeeBoss = distanceSquared <= activeBoss.definition.arenaRadius() * activeBoss.definition.arenaRadius();
            if (canSeeBoss) {
                activeBoss.bossBar.addPlayer(player);
            } else {
                activeBoss.bossBar.removePlayer(player);
            }
        }
    }

    private void maybeAdvancePhase(ActiveBoss activeBoss, LivingEntity boss) {
        double maxHealth = Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue();
        if (maxHealth <= 0.0D) {
            return;
        }
        double ratio = boss.getHealth() / maxHealth;

        if (activeBoss.phase < 2 && ratio <= activeBoss.definition.phaseTwoThreshold()) {
            activeBoss.phase = 2;
            applyPhaseBuffs(activeBoss, boss, activeBoss.definition.phaseTwoDamageMultiplier(), activeBoss.definition.phaseTwoSpeedMultiplier());
            announcePhase(activeBoss, boss, "enters phase two.");
            if (activeBoss.definition.archetype() == BossArchetype.NIGHT_CAVALRY) {
                summonNightAdds(activeBoss, boss, 2);
            }
        }
        if (activeBoss.phase < 3 && ratio <= activeBoss.definition.phaseThreeThreshold()) {
            activeBoss.phase = 3;
            applyPhaseBuffs(activeBoss, boss, activeBoss.definition.phaseThreeDamageMultiplier(), activeBoss.definition.phaseThreeSpeedMultiplier());
            announcePhase(activeBoss, boss, "erupts into phase three.");
            if (activeBoss.definition.archetype() == BossArchetype.NIGHT_CAVALRY) {
                summonNightAdds(activeBoss, boss, 3);
            }
        }
    }

    private void keepBossLeashed(ActiveBoss activeBoss, LivingEntity boss) {
        if (!boss.getWorld().equals(activeBoss.spawnLocation.getWorld())) {
            boss.teleport(activeBoss.spawnLocation);
            return;
        }
        if (boss.getLocation().distanceSquared(activeBoss.spawnLocation) <= activeBoss.definition.leashRadius() * activeBoss.definition.leashRadius()) {
            return;
        }
        boss.teleport(activeBoss.spawnLocation);
        boss.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation().add(0.0D, 1.0D, 0.0D), 32, 0.4D, 0.5D, 0.4D, 0.01D);
    }

    private void retargetNearbyPlayer(ActiveBoss activeBoss, LivingEntity boss) {
        if (!(boss instanceof Mob mob)) {
            return;
        }
        if (mob.getTarget() != null && mob.getTarget().isValid() && !mob.getTarget().isDead()) {
            return;
        }
        Player nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : boss.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode().isInvulnerable()) {
                continue;
            }
            double distanceSquared = player.getLocation().distanceSquared(boss.getLocation());
            if (distanceSquared > activeBoss.definition.arenaRadius() * activeBoss.definition.arenaRadius()) {
                continue;
            }
            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                nearest = player;
            }
        }
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private void maybeUseBossAbility(ActiveBoss activeBoss, LivingEntity boss, long now) {
        if (!(boss instanceof Mob mob)) {
            return;
        }
        if (!(mob.getTarget() instanceof Player target) || target.isDead()) {
            return;
        }
        long nextAllowed = abilityCooldowns.getOrDefault(activeBoss.entityId, 0L);
        if (now < nextAllowed) {
            return;
        }

        boolean used = switch (activeBoss.definition.archetype()) {
            case TREE_SENTINEL -> useTreeSentinelAbility(activeBoss, boss, target);
            case NIGHT_CAVALRY -> useNightCavalryAbility(activeBoss, boss, target);
        };
        if (used) {
            long cooldown = activeBoss.definition.abilityCooldownTicks();
            if (activeBoss.phase >= 3) {
                cooldown = Math.max(30L, cooldown - 40L);
            } else if (activeBoss.phase == 2) {
                cooldown = Math.max(40L, cooldown - 20L);
            }
            abilityCooldowns.put(activeBoss.entityId, now + cooldown);
            activeBoss.lastCombatTick = now;
        }
    }

    private boolean useTreeSentinelAbility(ActiveBoss activeBoss, LivingEntity boss, Player target) {
        double distanceSquared = boss.getLocation().distanceSquared(target.getLocation());
        if (activeBoss.phase >= 2 && shouldCreateHazard(activeBoss)) {
            createConsecratedGround(activeBoss, boss, target.getLocation());
            return true;
        }
        if (activeBoss.phase >= 3 && distanceSquared <= 100.0D) {
            return castHolySmite(activeBoss, boss, target);
        }
        if (distanceSquared <= 25.0D) {
            return unleashShockwave(activeBoss, boss);
        }
        return false;
    }

    private boolean useNightCavalryAbility(ActiveBoss activeBoss, LivingEntity boss, Player target) {
        double distanceSquared = boss.getLocation().distanceSquared(target.getLocation());
        if (activeBoss.phase >= 2 && distanceSquared >= 25.0D) {
            return castShadowVolley(boss, target);
        }
        if (distanceSquared <= 49.0D) {
            return performDashSlash(activeBoss, boss, target);
        }
        return false;
    }

    private boolean unleashShockwave(ActiveBoss activeBoss, LivingEntity boss) {
        Location center = boss.getLocation().add(0.0D, 0.4D, 0.0D);
        boss.getWorld().spawnParticle(Particle.EXPLOSION, center, 8, 1.2D, 0.2D, 1.2D, 0.02D);
        boss.getWorld().spawnParticle(Particle.DUST_PLUME, center, 40, 1.5D, 0.1D, 1.5D, 0.04D);
        boss.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0F, 0.85F);
        for (Entity nearby : boss.getWorld().getNearbyEntities(center, 4.5D, 2.5D, 4.5D)) {
            if (!(nearby instanceof Player player)) {
                continue;
            }
            Vector knockback = player.getLocation().toVector().subtract(boss.getLocation().toVector());
            if (knockback.lengthSquared() > 0.01D) {
                player.setVelocity(player.getVelocity().add(knockback.normalize().multiply(1.15D).setY(0.35D)));
            }
            player.damage(5.0D + (activeBoss.phase - 1) * 2.0D, boss);
        }
        return true;
    }

    private boolean castHolySmite(ActiveBoss activeBoss, LivingEntity boss, Player target) {
        Location strike = target.getLocation().clone();
        strike.getWorld().spawnParticle(Particle.GLOW, strike.clone().add(0.0D, 0.2D, 0.0D), 35, 0.8D, 0.1D, 0.8D, 0.02D);
        strike.getWorld().playSound(strike, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.0F, 1.35F);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (strike.getWorld() == null) {
                return;
            }
            strike.getWorld().strikeLightningEffect(strike);
            strike.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strike.clone().add(0.0D, 1.0D, 0.0D), 50, 0.6D, 1.0D, 0.6D, 0.03D);
            for (Entity nearby : strike.getWorld().getNearbyEntities(strike, 2.8D, 3.0D, 2.8D)) {
                if (nearby instanceof Player player) {
                    player.damage(7.0D + activeBoss.phase, boss);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, true));
                }
            }
        }, 18L);
        return true;
    }

    private boolean performDashSlash(ActiveBoss activeBoss, LivingEntity boss, Player target) {
        Vector dash = target.getLocation().toVector().subtract(boss.getLocation().toVector());
        if (dash.lengthSquared() < 0.01D) {
            return false;
        }
        boss.setVelocity(boss.getVelocity().add(dash.normalize().multiply(1.1D + (activeBoss.phase - 1) * 0.15D).setY(0.18D)));
        boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, boss.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.4D, 0.3D, 0.4D, 0.0D);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0F, 0.75F);
        return true;
    }

    private boolean castShadowVolley(LivingEntity boss, Player target) {
        Location eye = boss.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
        for (int i = -1; i <= 1; i++) {
            SmallFireball projectile = boss.getWorld().spawn(eye.clone().add(0.0D, 0.1D, 0.0D), SmallFireball.class);
            Vector spread = direction.clone().add(new Vector(i * 0.12D, 0.03D, i * 0.12D)).normalize().multiply(0.85D);
            projectile.setVelocity(spread);
            projectile.setShooter(boss);
            projectile.setIsIncendiary(false);
            projectile.setYield(0.0F);
        }
        boss.getWorld().spawnParticle(Particle.SCULK_SOUL, eye, 24, 0.25D, 0.25D, 0.25D, 0.02D);
        boss.getWorld().playSound(eye, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.9F, 0.7F);
        return true;
    }

    private void createConsecratedGround(ActiveBoss activeBoss, LivingEntity boss, Location center) {
        UUID hazardId = UUID.randomUUID();
        activeHazards.put(hazardId, new ActiveHazard(
                hazardId,
                activeBoss.entityId,
                center.clone(),
                2.6D + Math.max(0, activeBoss.phase - 2) * 0.8D,
                plugin.getServer().getCurrentTick() + 20L * (3L + activeBoss.phase),
                plugin.getServer().getCurrentTick()
        ));
        activeBoss.lastHazardTick = plugin.getServer().getCurrentTick();
        center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0.0D, 0.15D, 0.0D), 45, 1.1D, 0.05D, 1.1D, 0.02D);
        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 0.8F, 1.4F);
        boss.getWorld().spawnParticle(Particle.GLOW, boss.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.4D, 0.4D, 0.4D, 0.01D);
    }

    private void summonNightAdds(ActiveBoss activeBoss, LivingEntity boss, int wave) {
        int amount = wave == 2 ? 2 : 3;
        for (int i = 0; i < amount; i++) {
            double angle = (Math.PI * 2D / amount) * i;
            Location spawn = boss.getLocation().clone().add(Math.cos(angle) * 3.5D, 0.0D, Math.sin(angle) * 3.5D);
            Skeleton add = (Skeleton) boss.getWorld().spawnEntity(spawn, EntityType.SKELETON);
            add.customName(Component.text("Night Rider", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            add.setCustomNameVisible(true);
            add.setPersistent(true);
            add.setRemoveWhenFarAway(false);
            Objects.requireNonNull(add.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(18.0D + wave * 4.0D);
            add.setHealth(18.0D + wave * 4.0D);
            if (add.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                Objects.requireNonNull(add.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(4.0D + wave * 1.5D);
            }
            if (add.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                Objects.requireNonNull(add.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.30D + wave * 0.02D);
            }
            Player nearest = findNearestPlayer(add.getLocation(), activeBoss.definition.arenaRadius());
            if (nearest != null) {
                add.setTarget(nearest);
            }
            activeBoss.addIds.add(add.getUniqueId());
            boss.getWorld().spawnParticle(Particle.SMOKE, spawn.clone().add(0.0D, 1.0D, 0.0D), 25, 0.4D, 0.5D, 0.4D, 0.03D);
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.55F, 1.55F);
    }

    private void tickHazards(ActiveBoss activeBoss, LivingEntity boss, long now) {
        Iterator<Map.Entry<UUID, ActiveHazard>> iterator = activeHazards.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveHazard> entry = iterator.next();
            ActiveHazard hazard = entry.getValue();
            if (!hazard.ownerBossId.equals(activeBoss.entityId)) {
                continue;
            }
            if (!boss.getWorld().equals(hazard.center.getWorld()) || now >= hazard.expiresAtTick) {
                iterator.remove();
                continue;
            }
            if (now - hazard.lastPulseTick >= 20L) {
                hazard.lastPulseTick = now;
                pulseHazard(activeBoss, boss, hazard);
            }
            hazard.center.getWorld().spawnParticle(Particle.GLOW, hazard.center.clone().add(0.0D, 0.15D, 0.0D), 12,
                    hazard.radius * 0.45D, 0.02D, hazard.radius * 0.45D, 0.01D);
        }
    }

    private void pulseHazard(ActiveBoss activeBoss, LivingEntity boss, ActiveHazard hazard) {
        hazard.center.getWorld().spawnParticle(Particle.FLAME, hazard.center.clone().add(0.0D, 0.2D, 0.0D), 18,
                hazard.radius * 0.35D, 0.05D, hazard.radius * 0.35D, 0.01D);
        hazard.center.getWorld().playSound(hazard.center, Sound.BLOCK_BEACON_AMBIENT, SoundCategory.HOSTILE, 0.45F, 1.7F);
        for (Entity nearby : hazard.center.getWorld().getNearbyEntities(hazard.center, hazard.radius, 2.5D, hazard.radius)) {
            if (!(nearby instanceof Player player)) {
                continue;
            }
            if (player.getLocation().distanceSquared(hazard.center) > hazard.radius * hazard.radius) {
                continue;
            }
            player.damage(3.0D + activeBoss.phase, boss);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30, 0, false, true));
        }
    }

    private boolean shouldCreateHazard(ActiveBoss activeBoss) {
        return plugin.getServer().getCurrentTick() - activeBoss.lastHazardTick >= 20L * 10L;
    }

    private void maybeResetFight(ActiveBoss activeBoss, LivingEntity boss, long now) {
        if (now - activeBoss.lastCombatTick < activeBoss.definition.resetAfterIdleTicks()) {
            return;
        }
        boss.teleport(activeBoss.spawnLocation);
        double maxHealth = Objects.requireNonNull(boss.getAttribute(Attribute.MAX_HEALTH)).getValue();
        boss.setHealth(maxHealth);
        resetBossStats(activeBoss, boss);
        activeBoss.phase = 1;
        activeBoss.participants.clear();
        activeBoss.lastHazardTick = now;
        activeBoss.lastCombatTick = now;
        abilityCooldowns.remove(activeBoss.entityId);
        clearBossExtras(activeBoss);
        if (boss instanceof Creature creature) {
            creature.setTarget(null);
        }
        boss.getWorld().spawnParticle(Particle.END_ROD, boss.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.4D, 0.5D, 0.4D, 0.01D);
    }

    private void rewardParticipants(ActiveBoss activeBoss, Location deathLocation) {
        Set<UUID> rewarded = new HashSet<>(activeBoss.participants);
        if (rewarded.isEmpty()) {
            Player nearest = findNearestPlayer(deathLocation, activeBoss.definition.arenaRadius());
            if (nearest != null) {
                rewarded.add(nearest.getUniqueId());
            }
        }
        for (UUID playerId : rewarded) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            runeManager.addRunes(player, activeBoss.definition.runeReward(), true);
            player.sendMessage(Component.text(
                    "Boss defeated: " + activeBoss.definition.displayName() + " | +" + activeBoss.definition.runeReward() + " runes",
                    NamedTextColor.GOLD
            ).decoration(TextDecoration.ITALIC, false));
            grantBossRewards(player, activeBoss.definition);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 1.1F);
        }
    }

    private void grantBossRewards(Player player, BossDefinition definition) {
        BossRewardDefinition rewards = definition.rewards();
        if (rewards == null) {
            return;
        }

        String bossId = definition.id().toLowerCase();
        UUID playerId = player.getUniqueId();

        if (!hasClaimedFirstKillReward(playerId, bossId) && grantFirstKillReward(player, definition, rewards)) {
            claimedFirstKillRewardsByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(bossId);
            playerDataRepository.markBossFirstKillRewardClaimed(playerId, bossId);
        }

        if (!hasGrantedRemembrance(playerId, bossId) && grantRemembrance(player, definition, rewards)) {
            grantedRemembrancesByPlayer.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(bossId);
            playerDataRepository.markBossRemembranceGranted(playerId, bossId);
        }
    }

    private boolean hasClaimedFirstKillReward(UUID playerId, String bossId) {
        return claimedFirstKillRewardsByPlayer.getOrDefault(playerId, Set.of()).contains(bossId);
    }

    private boolean hasGrantedRemembrance(UUID playerId, String bossId) {
        return grantedRemembrancesByPlayer.getOrDefault(playerId, Set.of()).contains(bossId);
    }

    private boolean grantFirstKillReward(Player player, BossDefinition definition, BossRewardDefinition rewards) {
        if (rewards.firstKillRewardType() == null || rewards.firstKillRewardId() == null || rewards.firstKillRewardId().isBlank()) {
            return false;
        }

        ItemStack item = createRewardItem(rewards.firstKillRewardType(), rewards.firstKillRewardId());
        if (item == null) {
            plugin.getLogger().warning("Boss reward could not be resolved for boss '" + definition.id() + "': "
                    + rewards.firstKillRewardType() + " / " + rewards.firstKillRewardId());
            return false;
        }

        giveItem(player, item);
        String rewardName = rewards.firstKillRewardName() == null || rewards.firstKillRewardName().isBlank()
                ? rewards.firstKillRewardId()
                : rewards.firstKillRewardName();
        player.sendMessage(Component.text(
                "First victory reward: " + rewardName,
                NamedTextColor.AQUA
        ).decoration(TextDecoration.ITALIC, false));
        return true;
    }

    private boolean grantRemembrance(Player player, BossDefinition definition, BossRewardDefinition rewards) {
        if (rewards.remembranceId() == null || rewards.remembranceId().isBlank()) {
            return false;
        }

        ItemStack remembrance = createRemembranceItem(definition, rewards);
        giveItem(player, remembrance);
        String remembranceName = rewards.remembranceName() == null || rewards.remembranceName().isBlank()
                ? rewards.remembranceId()
                : rewards.remembranceName();
        player.sendMessage(Component.text(
                "Remembrance obtained: " + remembranceName,
                NamedTextColor.LIGHT_PURPLE
        ).decoration(TextDecoration.ITALIC, false));
        return true;
    }

    private ItemStack createRewardItem(BossRewardType rewardType, String rewardId) {
        return switch (rewardType) {
            case WEAPON -> weaponRegistry.getById(rewardId)
                    .map(weaponItemFactory::createWeaponItem)
                    .orElse(null);
            case SPELL -> spellRegistry.getById(rewardId)
                    .map(spellItemFactory::createSpellItem)
                    .orElse(null);
            case ASH_OF_WAR -> ashOfWarRegistry.getById(rewardId)
                    .map(ashOfWarItemFactory::createAshOfWarItem)
                    .orElse(null);
            case SPIRIT_ASH -> spiritAshRegistry.getById(rewardId)
                    .map(spiritAshItemFactory::createSpiritAshItem)
                    .orElse(null);
            case TALISMAN -> talismanRegistry.getById(rewardId)
                    .map(talismanItemFactory::createTalismanItem)
                    .orElse(null);
        };
    }

    private ItemStack createRemembranceItem(BossDefinition definition, BossRewardDefinition rewards) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(
                rewards.remembranceName(),
                NamedTextColor.LIGHT_PURPLE
        ).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Boss Remembrance", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("Source: " + definition.displayName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Can later be exchanged for unique power.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(remembranceIdKey, PersistentDataType.STRING, rewards.remembranceId());
        meta.getPersistentDataContainer().set(bossIdKey, PersistentDataType.STRING, definition.id());
        item.setItemMeta(meta);
        return item;
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void consumeMainHandItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) {
            return;
        }
        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        item.setAmount(amount - 1);
        player.getInventory().setItemInMainHand(item);
    }

    private Player findNearestPlayer(Location location, double radius) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        Player nearest = null;
        double bestDistance = radius * radius;
        for (Player player : location.getWorld().getPlayers()) {
            double distanceSquared = player.getLocation().distanceSquared(location);
            if (distanceSquared <= bestDistance) {
                bestDistance = distanceSquared;
                nearest = player;
            }
        }
        return nearest;
    }

    private void applyPhaseBuffs(ActiveBoss activeBoss, LivingEntity boss, double damageMultiplier, double speedMultiplier) {
        if (boss.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(boss.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(activeBoss.definition.baseDamage() * damageMultiplier);
        }
        if (boss.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(boss.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(activeBoss.definition.movementSpeed() * speedMultiplier);
        }
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 20, Math.max(0, activeBoss.phase - 2), false, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 20, 0, false, true));
    }

    private void announcePhase(ActiveBoss activeBoss, LivingEntity boss, String text) {
        boss.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, boss.getLocation().add(0.0D, 1.0D, 0.0D), 48, 0.6D, 0.8D, 0.6D, 0.01D);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 0.8F, 1.2F);
        for (Player player : boss.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(boss.getLocation()) <= activeBoss.definition.arenaRadius() * activeBoss.definition.arenaRadius()) {
                player.sendMessage(Component.text(activeBoss.definition.displayName() + " " + text, NamedTextColor.RED));
            }
        }
    }

    private void resetBossStats(ActiveBoss activeBoss, LivingEntity boss) {
        if (boss.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(boss.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(activeBoss.definition.baseDamage());
        }
        if (boss.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(boss.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(activeBoss.definition.movementSpeed());
        }
    }

    private Component displayName(String name) {
        return Component.text(name, NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false);
    }

    private String normalizeSpawnName(String spawnName) {
        return spawnName.trim().toLowerCase().replace(' ', '_');
    }

    private void clearBossExtras(ActiveBoss activeBoss) {
        for (UUID addId : List.copyOf(activeBoss.addIds)) {
            Entity entity = Bukkit.getEntity(addId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeBoss.addIds.clear();
        activeHazards.entrySet().removeIf(entry -> entry.getValue().ownerBossId.equals(activeBoss.entityId));
    }

    private static final class ActiveBoss {
        private final BossDefinition definition;
        private final UUID entityId;
        private final Location spawnLocation;
        private final BossBar bossBar;
        private final Set<UUID> participants;
        private final Set<UUID> addIds;
        private long lastCombatTick;
        private long lastHazardTick;
        private int phase;

        private ActiveBoss(BossDefinition definition, UUID entityId, Location spawnLocation, BossBar bossBar,
                           Set<UUID> participants, long lastCombatTick, int phase) {
            this.definition = definition;
            this.entityId = entityId;
            this.spawnLocation = spawnLocation;
            this.bossBar = bossBar;
            this.participants = participants;
            this.addIds = new HashSet<>();
            this.lastCombatTick = lastCombatTick;
            this.lastHazardTick = lastCombatTick;
            this.phase = phase;
        }
    }

    private static final class ActiveHazard {
        private final UUID id;
        private final UUID ownerBossId;
        private final Location center;
        private final double radius;
        private final long expiresAtTick;
        private long lastPulseTick;

        private ActiveHazard(UUID id, UUID ownerBossId, Location center, double radius, long expiresAtTick, long lastPulseTick) {
            this.id = id;
            this.ownerBossId = ownerBossId;
            this.center = center;
            this.radius = radius;
            this.expiresAtTick = expiresAtTick;
            this.lastPulseTick = lastPulseTick;
        }
    }

    public record RemembrancePreview(
            String bossId,
            String bossName,
            String remembranceName,
            String rewardName,
            int runeValue
    ) {
    }

    public record ExchangeResult(boolean success, String message) {
        public static ExchangeResult success(String message) {
            return new ExchangeResult(true, message);
        }

        public static ExchangeResult error(String message) {
            return new ExchangeResult(false, message);
        }
    }
}
