package de.skyforce.main.elden.spirit;

import de.skyforce.main.elden.spirit.model.SpiritAshDefinition;
import de.skyforce.main.elden.spirit.model.SpiritAshSummonType;
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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class SpiritAshManager {

    private final JavaPlugin plugin;
    private final NamespacedKey spiritKey;
    private final NamespacedKey spiritOwnerKey;
    private final NamespacedKey spiritIdKey;
    private final Map<UUID, ActiveSummon> activeSummons = new HashMap<>();
    private final Map<UUID, Long> abilityCooldownByEntity = new HashMap<>();
    private final Map<UUID, Long> reviveCooldownByEntity = new HashMap<>();
    private final BukkitTask maintenanceTask;

    public SpiritAshManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spiritKey = new NamespacedKey(plugin, "spirit-summon");
        this.spiritOwnerKey = new NamespacedKey(plugin, "spirit-summon-owner");
        this.spiritIdKey = new NamespacedKey(plugin, "spirit-summon-id");
        this.maintenanceTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSummons, 20L, 20L);
    }

    public boolean hasActiveSummon(Player player) {
        ActiveSummon summon = activeSummons.get(player.getUniqueId());
        return summon != null && !summon.entityIds.isEmpty();
    }

    public boolean summon(Player player, SpiritAshDefinition spiritAsh) {
        dismiss(player, false);
        List<UUID> entityIds = switch (spiritAsh.summonType()) {
            case WOLF_PACK -> spawnWolfPack(player, spiritAsh);
            case SKELETAL_MILITIA -> spawnSkeletalMilitia(player, spiritAsh);
            case GREATSHIELD_PHALANX -> spawnGreatshieldPhalanx(player, spiritAsh);
            case BLACK_KNIFE_ASSASSIN -> spawnBlackKnifeAssassin(player, spiritAsh);
        };
        if (entityIds.isEmpty()) {
            return false;
        }

        long expiresAt = plugin.getServer().getCurrentTick() + spiritAsh.summonDurationTicks();
        activeSummons.put(player.getUniqueId(), new ActiveSummon(spiritAsh.id(), entityIds, expiresAt));
        Location center = player.getLocation().add(0.0D, 1.0D, 0.0D);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 36, 1.0D, 0.8D, 1.0D, 0.03D);
        player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, center, 20, 0.9D, 0.7D, 0.9D, 0.01D);
        player.getWorld().spawnParticle(Particle.DUST, center, 30, 0.9D, 0.4D, 0.9D, new DustOptions(Color.fromRGB(120, 235, 255), 1.4F));
        player.getWorld().playSound(player.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.0F, 1.15F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.65F, 1.55F);
        player.sendMessage(Component.text(spiritAsh.displayName() + " answer your call.", NamedTextColor.AQUA));
        return true;
    }

    public void dismiss(Player player, boolean notify) {
        ActiveSummon summon = activeSummons.remove(player.getUniqueId());
        if (summon == null) {
            return;
        }

        for (UUID entityId : summon.entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation().add(0.0D, 0.8D, 0.0D), 12, 0.25D, 0.4D, 0.25D, 0.01D);
                entity.remove();
            }
        }

        if (notify) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 0.9F, 0.8F);
            player.sendMessage(Component.text("Your spirit returns to the ashes.", NamedTextColor.GRAY));
        }
    }

    public void shutdown() {
        maintenanceTask.cancel();
        abilityCooldownByEntity.clear();
        reviveCooldownByEntity.clear();
        for (UUID ownerId : List.copyOf(activeSummons.keySet())) {
            Player player = Bukkit.getPlayer(ownerId);
            if (player != null) {
                dismiss(player, false);
                continue;
            }
            ActiveSummon summon = activeSummons.remove(ownerId);
            if (summon == null) {
                continue;
            }
            for (UUID entityId : summon.entityIds) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
        }
    }

    public boolean isSpiritSummon(Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getPersistentDataContainer().has(spiritKey, PersistentDataType.BYTE);
    }

    public UUID getOwnerId(Entity entity) {
        if (entity == null) {
            return null;
        }
        String owner = entity.getPersistentDataContainer().get(spiritOwnerKey, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean isFriendly(UUID playerId, Entity entity) {
        if (playerId == null || entity == null) {
            return false;
        }
        if (entity instanceof Player player) {
            return playerId.equals(player.getUniqueId());
        }
        UUID ownerId = getOwnerId(entity);
        return playerId.equals(ownerId);
    }

    public boolean isFriendlyPair(Entity first, Entity second) {
        if (first == null || second == null) {
            return false;
        }
        UUID firstOwner = first instanceof Player player ? player.getUniqueId() : getOwnerId(first);
        UUID secondOwner = second instanceof Player player ? player.getUniqueId() : getOwnerId(second);
        return firstOwner != null && firstOwner.equals(secondOwner);
    }

    public void directSummons(Player player, LivingEntity target) {
        ActiveSummon summon = activeSummons.get(player.getUniqueId());
        if (summon == null || target == null || !target.isValid() || target.isDead()) {
            return;
        }
        if (isFriendly(player.getUniqueId(), target)) {
            return;
        }
        for (UUID entityId : summon.entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity instanceof Creature creature && entity.isValid()) {
                creature.setTarget(target);
            }
        }
    }

    public void handleSpiritAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity spirit) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!isSpiritSummon(spirit) || target instanceof Player) {
            return;
        }

        String spiritId = spirit.getPersistentDataContainer().get(spiritIdKey, PersistentDataType.STRING);
        if (spiritId == null) {
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        switch (spiritId) {
            case "lone_wolf_ashes" -> {
                if (tryUseAbility(spirit, now, 30L)) {
                    Vector lunge = target.getLocation().toVector().subtract(spirit.getLocation().toVector()).normalize().multiply(0.65D).setY(0.26D);
                    target.setVelocity(target.getVelocity().add(lunge));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 50, 0, false, true));
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0D, 1.0D, 0.0D), 2, 0.25D, 0.25D, 0.25D, 0.0D);
                    target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.2D, 0.25D, 0.2D, 0.01D);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WOLF_GROWL, SoundCategory.PLAYERS, 0.8F, 1.35F);
                    event.setDamage(event.getDamage() + 2.0D);
                }
            }
            case "skeletal_militiaman_ashes" -> {
                if (tryUseAbility(spirit, now, 40L)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.3D, 0.35D, 0.3D, 0.02D);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SKELETON_STEP, SoundCategory.PLAYERS, 0.9F, 0.7F);
                    event.setDamage(event.getDamage() + 2.5D);
                }
            }
            case "greatshield_soldier_ashes" -> {
                if (tryUseAbility(spirit, now, 55L)) {
                    for (Entity nearby : target.getWorld().getNearbyEntities(target.getLocation(), 2.8D, 2.0D, 2.8D)) {
                        if (!(nearby instanceof LivingEntity nearbyLiving) || nearbyLiving.equals(spirit) || isFriendlyPair(spirit, nearbyLiving)) {
                            continue;
                        }
                        Vector push = nearbyLiving.getLocation().toVector().subtract(spirit.getLocation().toVector());
                        if (push.lengthSquared() > 0.01D) {
                            nearbyLiving.setVelocity(nearbyLiving.getVelocity().add(push.normalize().multiply(0.7D).setY(0.22D)));
                        }
                        nearbyLiving.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
                    }
                    target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0.0D, 0.6D, 0.0D), 18, 0.55D, 0.25D, 0.55D, 0.0D, org.bukkit.Material.STONE.createBlockData());
                    target.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, target.getLocation().add(0.0D, 0.8D, 0.0D), 8, 0.4D, 0.2D, 0.4D, 0.01D);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.8F);
                    event.setDamage(event.getDamage() + 3.0D);
                }
            }
            case "black_knife_tiche" -> {
                if (tryUseAbility(spirit, now, 45L)) {
                    Vector dash = target.getLocation().toVector().subtract(spirit.getLocation().toVector()).normalize().multiply(0.85D).setY(0.18D);
                    spirit.setVelocity(spirit.getVelocity().add(dash));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 50, 0, false, true));
                    target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.25D, 0.35D, 0.25D, 0.02D);
                    target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0.0D, 1.0D, 0.0D), 14, 0.2D, 0.25D, 0.2D, 0.01D);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SKELETON_STEP, SoundCategory.PLAYERS, 0.7F, 1.45F);
                    event.setDamage(event.getDamage() + 5.0D);
                }
            }
            default -> {
            }
        }
    }

    private List<UUID> spawnWolfPack(Player player, SpiritAshDefinition spiritAsh) {
        List<UUID> entityIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Location spawn = resolveSpawnLocation(player, i, 0.9D);
            Wolf wolf = (Wolf) player.getWorld().spawnEntity(spawn, EntityType.WOLF);
            markSpiritEntity(wolf, player, spiritAsh.id());
            wolf.setOwner(player);
            wolf.setAdult();
            wolf.setRemoveWhenFarAway(false);
            wolf.customName(Component.text("Spirit Wolf", NamedTextColor.AQUA));
            wolf.setCustomNameVisible(false);
            wolf.setGlowing(true);
            setAttribute(wolf, Attribute.MAX_HEALTH, 22.0D);
            wolf.setHealth(22.0D);
            setAttribute(wolf, Attribute.MOVEMENT_SPEED, 0.38D);
            setAttribute(wolf, Attribute.ATTACK_DAMAGE, 6.0D);
            setAttribute(wolf, Attribute.FOLLOW_RANGE, 28.0D);
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, false, false));
            wolf.getWorld().spawnParticle(Particle.SOUL, wolf.getLocation().add(0.0D, 0.8D, 0.0D), 16, 0.35D, 0.45D, 0.35D, 0.02D);
            entityIds.add(wolf.getUniqueId());
        }
        return entityIds;
    }

    private List<UUID> spawnSkeletalMilitia(Player player, SpiritAshDefinition spiritAsh) {
        List<UUID> entityIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Location spawn = resolveSpawnLocation(player, i, 1.1D);
            Skeleton skeleton = (Skeleton) player.getWorld().spawnEntity(spawn, EntityType.SKELETON);
            configureSkeletonSpirit(skeleton, player, spiritAsh.id(), "Skeletal Militiaman", new ItemStack(org.bukkit.Material.STONE_SWORD), null,
                    28.0D, 0.31D, 7.0D);
            entityIds.add(skeleton.getUniqueId());
        }
        return entityIds;
    }

    private List<UUID> spawnGreatshieldPhalanx(Player player, SpiritAshDefinition spiritAsh) {
        List<UUID> entityIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Location spawn = resolveSpawnLocation(player, i, 1.3D);
            Skeleton skeleton = (Skeleton) player.getWorld().spawnEntity(spawn, EntityType.SKELETON);
            ItemStack helmet = new ItemStack(org.bukkit.Material.IRON_HELMET);
            Damageable meta = (Damageable) Objects.requireNonNull(helmet.getItemMeta());
            meta.setDamage(helmet.getType().getMaxDurability() / 2);
            helmet.setItemMeta((org.bukkit.inventory.meta.ItemMeta) meta);
            configureSkeletonSpirit(skeleton, player, spiritAsh.id(), "Greatshield Soldier", new ItemStack(org.bukkit.Material.STONE_SWORD),
                    new ItemStack(org.bukkit.Material.SHIELD), 40.0D, 0.25D, 6.0D);
            skeleton.getEquipment().setHelmet(helmet);
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
            entityIds.add(skeleton.getUniqueId());
        }
        return entityIds;
    }

    private List<UUID> spawnBlackKnifeAssassin(Player player, SpiritAshDefinition spiritAsh) {
        List<UUID> entityIds = new ArrayList<>();
        Location spawn = resolveSpawnLocation(player, 0, 1.0D);
        WitherSkeleton assassin = (WitherSkeleton) player.getWorld().spawnEntity(spawn, EntityType.WITHER_SKELETON);
        markSpiritEntity(assassin, player, spiritAsh.id());
        assassin.setRemoveWhenFarAway(false);
        assassin.customName(Component.text("Black Knife Tiche", NamedTextColor.DARK_AQUA));
        assassin.setCustomNameVisible(false);
        assassin.setGlowing(true);
        setAttribute(assassin, Attribute.MAX_HEALTH, 52.0D);
        assassin.setHealth(52.0D);
        setAttribute(assassin, Attribute.MOVEMENT_SPEED, 0.36D);
        setAttribute(assassin, Attribute.ATTACK_DAMAGE, 9.0D);
        setAttribute(assassin, Attribute.FOLLOW_RANGE, 34.0D);
        assassin.getEquipment().setItemInMainHand(new ItemStack(org.bukkit.Material.NETHERITE_SWORD));
        assassin.getEquipment().setHelmetDropChance(0.0F);
        assassin.getEquipment().setItemInMainHandDropChance(0.0F);
        assassin.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        assassin.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, false, false));
        assassin.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        assassin.getWorld().spawnParticle(Particle.SCULK_SOUL, assassin.getLocation().add(0.0D, 0.9D, 0.0D), 18, 0.3D, 0.45D, 0.3D, 0.02D);
        assassin.getWorld().playSound(assassin.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, SoundCategory.PLAYERS, 0.45F, 1.55F);
        entityIds.add(assassin.getUniqueId());
        return entityIds;
    }

    private void configureSkeletonSpirit(Skeleton skeleton, Player player, String spiritId, String name, ItemStack mainHand,
                                         ItemStack offHand, double maxHealth, double moveSpeed, double attackDamage) {
        markSpiritEntity(skeleton, player, spiritId);
        skeleton.setRemoveWhenFarAway(false);
        skeleton.customName(Component.text(name, NamedTextColor.AQUA));
        skeleton.setCustomNameVisible(false);
        skeleton.setGlowing(true);
        setAttribute(skeleton, Attribute.MAX_HEALTH, maxHealth);
        skeleton.setHealth(maxHealth);
        setAttribute(skeleton, Attribute.MOVEMENT_SPEED, moveSpeed);
        setAttribute(skeleton, Attribute.ATTACK_DAMAGE, attackDamage);
        setAttribute(skeleton, Attribute.FOLLOW_RANGE, 28.0D);
        skeleton.getEquipment().setItemInMainHand(mainHand);
        skeleton.getEquipment().setItemInOffHand(offHand);
        skeleton.getEquipment().setHelmet(new ItemStack(org.bukkit.Material.CHAINMAIL_HELMET));
        skeleton.getEquipment().setHelmetDropChance(0.0F);
        skeleton.getEquipment().setItemInMainHandDropChance(0.0F);
        skeleton.getEquipment().setItemInOffHandDropChance(0.0F);
        skeleton.setCanPickupItems(false);
        skeleton.setShouldBurnInDay(false);
        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        skeleton.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
        skeleton.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, skeleton.getLocation().add(0.0D, 0.9D, 0.0D), 10, 0.3D, 0.5D, 0.3D, 0.01D);
    }

    private void markSpiritEntity(LivingEntity entity, Player owner, String spiritId) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(spiritKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(spiritOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        pdc.set(spiritIdKey, PersistentDataType.STRING, spiritId);
    }

    private void tickSummons() {
        long now = plugin.getServer().getCurrentTick();
        Iterator<Map.Entry<UUID, ActiveSummon>> iterator = activeSummons.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveSummon> entry = iterator.next();
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || !owner.isOnline() || owner.isDead()) {
                removeSummon(entry.getValue());
                iterator.remove();
                continue;
            }

            ActiveSummon summon = entry.getValue();
            if (now >= summon.expiresAt) {
                owner.sendMessage(Component.text("Your Spirit Ash fades.", NamedTextColor.GRAY));
                removeSummon(summon);
                iterator.remove();
                continue;
            }

            refreshSummon(owner, summon);
            if (summon.entityIds.isEmpty()) {
                owner.sendMessage(Component.text("Your summoned spirit has fallen.", NamedTextColor.GRAY));
                iterator.remove();
            }
        }
    }

    private void refreshSummon(Player owner, ActiveSummon summon) {
        Iterator<UUID> iterator = summon.entityIds.iterator();
        while (iterator.hasNext()) {
            UUID entityId = iterator.next();
            Entity entity = Bukkit.getEntity(entityId);
            if (!(entity instanceof LivingEntity living) || !entity.isValid() || living.isDead()) {
                iterator.remove();
                continue;
            }

            if (!entity.getWorld().equals(owner.getWorld())) {
                entity.teleport(owner.getLocation());
            } else if (entity.getLocation().distanceSquared(owner.getLocation()) > 30.0D * 30.0D) {
                entity.teleport(resolveSpawnLocation(owner, 0, 1.2D));
            }

            if (entity instanceof Creature creature) {
                LivingEntity currentTarget = creature.getTarget();
                if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead() || isFriendly(owner.getUniqueId(), currentTarget)) {
                    LivingEntity nextTarget = findNearestHostile(owner, entity.getLocation(), 20.0D);
                    creature.setTarget(nextTarget);
                }
                applySpiritCombatStyle(entity, creature.getTarget());
            }

            handleLowHealthSpirit(entity, owner);
            emitAmbientEffects(entity, summon.spiritId, summon.expiresAt - plugin.getServer().getCurrentTick());
        }
        sendSummonStatus(owner, summon);
    }

    private void removeSummon(ActiveSummon summon) {
        for (UUID entityId : summon.entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    private LivingEntity findNearestHostile(Player owner, Location source, double radius) {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity nearby : source.getWorld().getNearbyEntities(source, radius, radius, radius)) {
            if (!(nearby instanceof Monster monster) || monster.isDead() || !monster.isValid()) {
                continue;
            }
            if (isSpiritSummon(monster) || isFriendly(owner.getUniqueId(), monster)) {
                continue;
            }
            double distance = nearby.getLocation().distanceSquared(source);
            if (distance < nearestDistance) {
                nearest = monster;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private Location resolveSpawnLocation(Player player, int index, double radius) {
        double angle = (Math.PI * 2.0D * index) / 4.0D;
        Location base = player.getLocation();
        Location spawn = base.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
        spawn.setY(player.getWorld().getHighestBlockYAt(spawn) + 1.0D);
        return spawn;
    }

    private void applySpiritCombatStyle(Entity entity, LivingEntity target) {
        if (!(entity instanceof LivingEntity living) || target == null) {
            return;
        }

        double distanceSquared = entity.getLocation().distanceSquared(target.getLocation());
        if (entity instanceof Wolf wolf) {
            if (distanceSquared > 6.25D && distanceSquared < 64.0D) {
                Vector leap = target.getLocation().toVector().subtract(wolf.getLocation().toVector()).normalize().multiply(0.55D).setY(0.32D);
                wolf.setVelocity(wolf.getVelocity().add(leap));
            }
            if (distanceSquared < 4.0D) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0, false, true));
            }
            return;
        }

        if (entity instanceof Skeleton skeleton) {
            String spiritId = skeleton.getPersistentDataContainer().get(spiritIdKey, PersistentDataType.STRING);
            if ("skeletal_militiaman_ashes".equals(spiritId) && distanceSquared < 5.0D) {
                skeleton.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 1, false, false));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 0, false, true));
                return;
            }
            if ("greatshield_soldier_ashes".equals(spiritId)) {
                if (distanceSquared < 9.0D) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35, 1, false, true));
                }
                if (living.getHealth() <= living.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.45D) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 2, false, false));
                }
                if (distanceSquared > 16.0D && distanceSquared < 90.0D) {
                    Vector rush = target.getLocation().toVector().subtract(skeleton.getLocation().toVector()).normalize().multiply(0.28D);
                    skeleton.setVelocity(skeleton.getVelocity().add(rush));
                }
            }
        }

        if (entity instanceof WitherSkeleton assassin) {
            String spiritId = assassin.getPersistentDataContainer().get(spiritIdKey, PersistentDataType.STRING);
            if ("black_knife_tiche".equals(spiritId)) {
                if (distanceSquared > 9.0D && distanceSquared < 100.0D) {
                    Vector dash = target.getLocation().toVector().subtract(assassin.getLocation().toVector()).normalize().multiply(0.42D).setY(0.12D);
                    assassin.setVelocity(assassin.getVelocity().add(dash));
                }
                if (distanceSquared < 6.5D) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 30, 0, false, true));
                }
            }
        }
    }

    private void handleLowHealthSpirit(Entity entity, Player owner) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (living.getAttribute(Attribute.MAX_HEALTH) == null) {
            return;
        }

        String spiritId = living.getPersistentDataContainer().get(spiritIdKey, PersistentDataType.STRING);
        if (!"skeletal_militiaman_ashes".equals(spiritId)) {
            return;
        }

        double maxHealth = living.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (living.getHealth() > maxHealth * 0.3D) {
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        if (!tryUseReviveWindow(living, now, 20L * 20L)) {
            return;
        }

        living.setHealth(Math.min(maxHealth, maxHealth * 0.65D));
        living.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 2, false, false));
        living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1, false, false));
        living.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, living.getLocation().add(0.0D, 1.0D, 0.0D), 24, 0.35D, 0.5D, 0.35D, 0.02D);
        living.getWorld().spawnParticle(Particle.SOUL, living.getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.25D, 0.4D, 0.25D, 0.02D);
        living.getWorld().playSound(living.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.85F, 1.25F);
        owner.sendActionBar(Component.text("Skeletal Militiaman rallies from the ashes.", NamedTextColor.YELLOW));
    }

    private void emitAmbientEffects(Entity entity, String spiritId, long remainingTicks) {
        Location location = entity.getLocation().add(0.0D, 0.9D, 0.0D);
        switch (spiritId) {
            case "lone_wolf_ashes" -> {
                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location, 4, 0.2D, 0.35D, 0.2D, 0.01D);
                entity.getWorld().spawnParticle(Particle.DUST, location, 3, 0.18D, 0.25D, 0.18D,
                        new DustOptions(Color.fromRGB(110, 220, 255), 1.0F));
            }
            case "skeletal_militiaman_ashes" -> {
                entity.getWorld().spawnParticle(Particle.SOUL, location, 4, 0.18D, 0.3D, 0.18D, 0.01D);
                entity.getWorld().spawnParticle(Particle.ENCHANT, location, 3, 0.2D, 0.3D, 0.2D, 0.01D);
            }
            case "greatshield_soldier_ashes" -> {
                entity.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, location, 3, 0.2D, 0.35D, 0.2D, 0.0D);
                entity.getWorld().spawnParticle(Particle.DUST, location, 2, 0.16D, 0.25D, 0.16D,
                        new DustOptions(Color.fromRGB(200, 235, 255), 1.15F));
            }
            case "black_knife_tiche" -> {
                entity.getWorld().spawnParticle(Particle.SCULK_SOUL, location, 4, 0.18D, 0.3D, 0.18D, 0.02D);
                entity.getWorld().spawnParticle(Particle.DUST, location, 2, 0.12D, 0.22D, 0.12D,
                        new DustOptions(Color.fromRGB(35, 35, 35), 1.0F));
            }
            default -> entity.getWorld().spawnParticle(Particle.SOUL, location, 2, 0.16D, 0.25D, 0.16D, 0.01D);
        }

        if (remainingTicks <= 100L) {
            entity.getWorld().spawnParticle(Particle.OMINOUS_SPAWNING, location, 2, 0.2D, 0.3D, 0.2D, 0.01D);
        }
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        if (entity.getAttribute(attribute) != null) {
            entity.getAttribute(attribute).setBaseValue(value);
        }
    }

    private void sendSummonStatus(Player owner, ActiveSummon summon) {
        long remainingTicks = Math.max(0L, summon.expiresAt - plugin.getServer().getCurrentTick());
        long seconds = (remainingTicks + 19L) / 20L;
        int activeCount = 0;
        for (UUID entityId : summon.entityIds) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity instanceof LivingEntity living && entity.isValid() && !living.isDead()) {
                activeCount++;
            }
        }
        owner.sendActionBar(Component.text(
                "Spirit Ash: " + summon.spiritId.replace('_', ' ') + " | " + activeCount + " active | " + seconds + "s",
                NamedTextColor.AQUA
        ));
    }

    private boolean tryUseAbility(Entity entity, long now, long cooldownTicks) {
        long readyAt = abilityCooldownByEntity.getOrDefault(entity.getUniqueId(), 0L);
        if (now < readyAt) {
            return false;
        }
        abilityCooldownByEntity.put(entity.getUniqueId(), now + cooldownTicks);
        return true;
    }

    private boolean tryUseReviveWindow(Entity entity, long now, long cooldownTicks) {
        long readyAt = reviveCooldownByEntity.getOrDefault(entity.getUniqueId(), 0L);
        if (now < readyAt) {
            return false;
        }
        reviveCooldownByEntity.put(entity.getUniqueId(), now + cooldownTicks);
        return true;
    }

    private static final class ActiveSummon {
        private final String spiritId;
        private final List<UUID> entityIds;
        private final long expiresAt;

        private ActiveSummon(String spiritId, List<UUID> entityIds, long expiresAt) {
            this.spiritId = spiritId;
            this.entityIds = entityIds;
            this.expiresAt = expiresAt;
        }
    }


}
