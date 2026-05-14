package de.skyforce.main.elden.enemy;

import de.skyforce.main.elden.enemy.model.EnemyDefinition;
import de.skyforce.main.elden.enemy.model.EnemyRewardDefinition;
import de.skyforce.main.elden.enemy.model.EnemyRewardType;
import de.skyforce.main.elden.enemy.model.EnemySpawnerDefinition;
import de.skyforce.main.elden.enemy.model.PatrolPoint;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.smithing.service.SmithingStoneService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EnemyManager {

    private final JavaPlugin plugin;
    private final RuneManager runeManager;
    private final SmithingStoneService smithingStoneService;
    private final NamespacedKey enemyKey;
    private final NamespacedKey enemyIdKey;
    private final NamespacedKey spawnerIdKey;
    private final Map<UUID, ActiveEnemy> activeEnemies = new HashMap<>();
    private final BukkitTask maintenanceTask;
    private EnemySpawnerManager spawnerManager;

    public EnemyManager(JavaPlugin plugin, RuneManager runeManager, SmithingStoneService smithingStoneService) {
        this.plugin = plugin;
        this.runeManager = runeManager;
        this.smithingStoneService = smithingStoneService;
        this.enemyKey = new NamespacedKey(plugin, "elden-enemy");
        this.enemyIdKey = new NamespacedKey(plugin, "elden-enemy-id");
        this.spawnerIdKey = new NamespacedKey(plugin, "elden-enemy-spawner-id");
        this.maintenanceTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickActiveEnemies, 20L, 20L);
    }

    public void setSpawnerManager(EnemySpawnerManager spawnerManager) {
        this.spawnerManager = spawnerManager;
    }

    public boolean spawnEnemy(EnemyDefinition definition, EnemySpawnerDefinition spawner) {
        if (definition == null || spawner == null) {
            return false;
        }
        Location location = spawner.toLocation(plugin);
        if (location == null || location.getWorld() == null) {
            return false;
        }

        boolean eliteVariant = !definition.elite() && spawner.eliteChance() > 0.0D && Math.random() < spawner.eliteChance();
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(randomizedSpawn(location, spawner.radius()), definition.entityType());
        prepareEntity(entity, definition, spawner.spawnerId(), eliteVariant);
        activeEnemies.put(entity.getUniqueId(), new ActiveEnemy(
                entity.getUniqueId(),
                definition,
                spawner.spawnerId(),
                location.clone(),
                eliteVariant,
                List.copyOf(spawner.patrolPoints()),
                0
        ));
        return true;
    }

    public boolean spawnEnemyAt(EnemyDefinition definition, Location location) {
        if (definition == null || location == null || location.getWorld() == null) {
            return false;
        }
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.entityType());
        prepareEntity(entity, definition, null, definition.elite());
        activeEnemies.put(entity.getUniqueId(), new ActiveEnemy(entity.getUniqueId(), definition, null, location.clone(), definition.elite(), List.of(), 0));
        return true;
    }

    public int getActiveCount(String spawnerId) {
        int count = 0;
        for (ActiveEnemy enemy : activeEnemies.values()) {
            if (Objects.equals(spawnerId, enemy.spawnerId())) {
                count++;
            }
        }
        return count;
    }

    public void handleEnemyDeath(LivingEntity entity, Player killer) {
        ActiveEnemy activeEnemy = activeEnemies.remove(entity.getUniqueId());
        if (activeEnemy == null) {
            return;
        }

        int rewardedRunes = activeEnemy.definition().runeReward() + (activeEnemy.eliteVariant() ? Math.max(10, activeEnemy.definition().runeReward() / 2) : 0);
        if (killer != null && rewardedRunes > 0) {
            runeManager.addRunes(killer, rewardedRunes, true);
            killer.sendActionBar(Component.text(
                    "+" + rewardedRunes + " runes",
                    NamedTextColor.GOLD
            ));
            giveGuaranteedReward(entity, killer, activeEnemy);
        }

        if (spawnerManager != null && activeEnemy.spawnerId() != null) {
            spawnerManager.handleEnemyRemoved(activeEnemy.spawnerId(), activeEnemy.definition().respawnDelayTicks());
        }
    }

    public boolean isManagedEnemy(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }
        PersistentDataContainer container = livingEntity.getPersistentDataContainer();
        return container.has(enemyKey, PersistentDataType.BYTE);
    }

    public void despawnSpawnerEnemies(String spawnerId, boolean scheduleRespawn, long respawnDelayTicks) {
        for (ActiveEnemy enemy : activeEnemies.values().stream().filter(active -> Objects.equals(spawnerId, active.spawnerId())).toList()) {
            Entity entity = Bukkit.getEntity(enemy.entityId());
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
            activeEnemies.remove(enemy.entityId());
        }
        if (scheduleRespawn && spawnerManager != null && spawnerId != null) {
            spawnerManager.handleEnemyRemoved(spawnerId, respawnDelayTicks);
        }
    }

    public void shutdown() {
        maintenanceTask.cancel();
        for (UUID entityId : activeEnemies.keySet().stream().toList()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeEnemies.clear();
    }

    public void alertNearbyEnemies(LivingEntity damagedEntity, Player target) {
        ActiveEnemy source = activeEnemies.get(damagedEntity.getUniqueId());
        if (source == null || target == null) {
            return;
        }
        for (ActiveEnemy activeEnemy : new ArrayList<>(activeEnemies.values())) {
            if (activeEnemy.entityId().equals(source.entityId())) {
                continue;
            }
            Entity entity = Bukkit.getEntity(activeEnemy.entityId());
            if (!(entity instanceof Mob mob) || !mob.isValid() || mob.isDead()) {
                continue;
            }
            if (!isSameCamp(source, activeEnemy)) {
                continue;
            }
            if (mob.getLocation().distanceSquared(damagedEntity.getLocation()) > 18.0D * 18.0D) {
                continue;
            }
            mob.setTarget(target);
        }
    }

    private void tickActiveEnemies() {
        Iterator<Map.Entry<UUID, ActiveEnemy>> iterator = activeEnemies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveEnemy> entry = iterator.next();
            ActiveEnemy activeEnemy = entry.getValue();
            Entity entity = Bukkit.getEntity(activeEnemy.entityId());
            if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid() || livingEntity.isDead()) {
                if (spawnerManager != null && activeEnemy.spawnerId() != null) {
                    spawnerManager.handleEnemyRemoved(activeEnemy.spawnerId(), activeEnemy.definition().respawnDelayTicks());
                }
                iterator.remove();
                continue;
            }

            if (livingEntity.getLocation().distanceSquared(activeEnemy.origin()) > activeEnemy.definition().maxLeashDistance() * activeEnemy.definition().maxLeashDistance()) {
                livingEntity.teleport(activeEnemy.origin());
                livingEntity.setHealth(resolveMaxHealth(activeEnemy.definition(), activeEnemy.eliteVariant()));
                if (livingEntity instanceof Mob mob) {
                    mob.setTarget(null);
                }
                iterator.remove();
                activeEnemies.put(activeEnemy.entityId(), new ActiveEnemy(
                        activeEnemy.entityId(),
                        activeEnemy.definition(),
                        activeEnemy.spawnerId(),
                        activeEnemy.origin(),
                        activeEnemy.eliteVariant(),
                        activeEnemy.patrolPoints(),
                        0
                ));
                continue;
            }

            if (livingEntity instanceof Mob mob && mob.getTarget() == null && !activeEnemy.patrolPoints().isEmpty()) {
                int nextIndex = runPatrol(mob, activeEnemy);
                if (nextIndex != activeEnemy.patrolIndex()) {
                    iterator.remove();
                    activeEnemies.put(activeEnemy.entityId(), new ActiveEnemy(
                            activeEnemy.entityId(),
                            activeEnemy.definition(),
                            activeEnemy.spawnerId(),
                            activeEnemy.origin(),
                            activeEnemy.eliteVariant(),
                            activeEnemy.patrolPoints(),
                            nextIndex
                    ));
                }
            }
        }
    }

    private int runPatrol(Mob mob, ActiveEnemy activeEnemy) {
        int currentIndex = Math.max(0, Math.min(activeEnemy.patrolIndex(), activeEnemy.patrolPoints().size() - 1));
        PatrolPoint patrolPoint = activeEnemy.patrolPoints().get(currentIndex);
        Location targetLocation = patrolPoint.toLocation(plugin);
        if (targetLocation == null || targetLocation.getWorld() != mob.getWorld()) {
            return currentIndex;
        }
        mob.getPathfinder().moveTo(targetLocation);
        if (mob.getLocation().distanceSquared(targetLocation) <= 2.25D) {
            return (currentIndex + 1) % activeEnemy.patrolPoints().size();
        }
        return currentIndex;
    }

    private void prepareEntity(LivingEntity entity, EnemyDefinition definition, String spawnerId, boolean eliteVariant) {
        entity.customName(Component.text(resolveDisplayName(definition, eliteVariant), eliteVariant ? NamedTextColor.GOLD : NamedTextColor.RED));
        entity.setCustomNameVisible(true);
        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);

        if (entity instanceof Mob mob) {
            mob.setCanPickupItems(false);
        }

        if (entity instanceof Zombie zombie) {
            zombie.setBaby(definition.baby());
        }

        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(resolveMaxHealth(definition, eliteVariant));
        }
        entity.setHealth(resolveMaxHealth(definition, eliteVariant));

        if (entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(definition.movementSpeed() * (eliteVariant ? 1.08D : 1.0D));
        }
        if (entity.getAttribute(Attribute.FOLLOW_RANGE) != null) {
            entity.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(definition.followRange() * (eliteVariant ? 1.12D : 1.0D));
        }
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(definition.baseDamage() * (eliteVariant ? 1.35D : 1.0D));
        }

        applyEquipment(entity.getEquipment(), definition);

        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(enemyKey, PersistentDataType.BYTE, (byte) 1);
        container.set(enemyIdKey, PersistentDataType.STRING, definition.id());
        if (spawnerId != null && !spawnerId.isBlank()) {
            container.set(spawnerIdKey, PersistentDataType.STRING, spawnerId);
        }
        if (eliteVariant) {
            entity.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, entity.getLocation().add(0.0D, 1.0D, 0.0D), 18, 0.4D, 0.6D, 0.4D, 0.01D);
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_TOTEM_USE, 0.4F, 1.7F);
        }
    }

    private void applyEquipment(EntityEquipment equipment, EnemyDefinition definition) {
        if (equipment == null) {
            return;
        }
        equipment.setItemInMainHand(item(definition.mainHand()));
        equipment.setHelmet(item(definition.helmet()));
        equipment.setChestplate(item(definition.chestplate()));
        equipment.setLeggings(item(definition.leggings()));
        equipment.setBoots(item(definition.boots()));
        equipment.setItemInMainHandDropChance(0.0F);
        equipment.setHelmetDropChance(0.0F);
        equipment.setChestplateDropChance(0.0F);
        equipment.setLeggingsDropChance(0.0F);
        equipment.setBootsDropChance(0.0F);
    }

    private ItemStack item(Material material) {
        return material == null ? null : new ItemStack(material);
    }

    private double resolveMaxHealth(EnemyDefinition definition, boolean eliteVariant) {
        double health = definition.maxHealth() * (eliteVariant ? 1.45D : 1.0D);
        return Math.max(1.0D, health);
    }

    private String resolveDisplayName(EnemyDefinition definition, boolean eliteVariant) {
        if (eliteVariant) {
            return "Elite " + definition.displayName();
        }
        return definition.displayName();
    }

    private void giveGuaranteedReward(LivingEntity entity, Player killer, ActiveEnemy activeEnemy) {
        EnemyRewardDefinition reward = resolveReward(activeEnemy);
        if (reward == null) {
            return;
        }

        ItemStack item = switch (reward.rewardType()) {
            case SMITHING_STONE -> smithingStoneService.createStone(
                    reward.smithingTrack(),
                    reward.smithingTier(),
                    reward.amount()
            );
        };
        entity.getWorld().dropItemNaturally(entity.getLocation(), item);
        killer.sendMessage(Component.text(
                "Reward: " + reward.displayName(),
                activeEnemy.eliteVariant() ? NamedTextColor.GOLD : NamedTextColor.GREEN
        ));
    }

    private EnemyRewardDefinition resolveReward(ActiveEnemy activeEnemy) {
        if (activeEnemy.definition().guaranteedReward() != null) {
            return activeEnemy.definition().guaranteedReward();
        }
        if (!activeEnemy.eliteVariant()) {
            return null;
        }
        return new EnemyRewardDefinition(
                EnemyRewardType.SMITHING_STONE,
                activeEnemy.definition().archetype().name().equals("RANGED")
                        ? de.skyforce.main.elden.smithing.model.SmithingTrack.SOMBER
                        : de.skyforce.main.elden.smithing.model.SmithingTrack.STANDARD,
                activeEnemy.definition().archetype().displayName().equals("Elite") ? 3 : 2,
                1,
                activeEnemy.definition().archetype().displayName().equals("Ranged")
                        ? "Somber Smithing Stone [1]"
                        : "Smithing Stone [2]"
        );
    }

    private boolean isSameCamp(ActiveEnemy first, ActiveEnemy second) {
        if (first.spawnerId() == null || second.spawnerId() == null) {
            return false;
        }
        if (Objects.equals(first.spawnerId(), second.spawnerId())) {
            return true;
        }
        if (spawnerManager == null) {
            return false;
        }
        String firstGroup = spawnerManager.getSpawner(first.spawnerId()).map(EnemySpawnerDefinition::groupId).orElse(null);
        String secondGroup = spawnerManager.getSpawner(second.spawnerId()).map(EnemySpawnerDefinition::groupId).orElse(null);
        return firstGroup != null && firstGroup.equals(secondGroup);
    }

    private Location randomizedSpawn(Location location, double radius) {
        if (radius <= 0.0D) {
            return location.clone();
        }
        double offsetX = (Math.random() - 0.5D) * 2.0D * radius;
        double offsetZ = (Math.random() - 0.5D) * 2.0D * radius;
        return location.clone().add(offsetX, 0.0D, offsetZ);
    }

    private record ActiveEnemy(
            UUID entityId,
            EnemyDefinition definition,
            String spawnerId,
            Location origin,
            boolean eliteVariant,
            List<PatrolPoint> patrolPoints,
            int patrolIndex
    ) {
    }
}
