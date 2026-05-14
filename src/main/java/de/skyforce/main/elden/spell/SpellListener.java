package de.skyforce.main.elden.spell;

import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.level.PlayerProgress;
import de.skyforce.main.elden.spell.model.SpellDefinition;
import de.skyforce.main.elden.spell.model.SpellSchool;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class SpellListener implements Listener {

    private final JavaPlugin plugin;
    private final SpellRegistry spellRegistry;
    private final SpellItemFactory spellItemFactory;
    private final FocusManager focusManager;
    private final LevelManager levelManager;
    private final Map<UUID, Map<String, Long>> cooldownUntilByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> castingTasks = new HashMap<>();
    private Function<Player, Double> castSpeedMultiplierProvider = player -> 1.0D;

    public SpellListener(JavaPlugin plugin, SpellRegistry spellRegistry, SpellItemFactory spellItemFactory,
                         FocusManager focusManager, LevelManager levelManager) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.spellItemFactory = spellItemFactory;
        this.focusManager = focusManager;
        this.levelManager = levelManager;
    }

    public void setCastSpeedMultiplierProvider(Function<Player, Double> castSpeedMultiplierProvider) {
        this.castSpeedMultiplierProvider = castSpeedMultiplierProvider == null ? player -> 1.0D : castSpeedMultiplierProvider;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!spellItemFactory.isSpellItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        SpellDefinition spell = spellRegistry.getById(spellItemFactory.getSpellId(item)).orElse(null);
        if (spell == null) {
            player.sendActionBar(Component.text("Unknown spell.", NamedTextColor.RED));
            return;
        }

        if (castingTasks.containsKey(player.getUniqueId())) {
            player.sendActionBar(Component.text("You are already casting a spell.", NamedTextColor.GRAY));
            return;
        }

        PlayerProgress progress = levelManager.getOrCreate(player);
        String requirementFailure = validateRequirements(progress, spell);
        if (requirementFailure != null) {
            player.sendActionBar(Component.text(requirementFailure, NamedTextColor.RED));
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        long cooldownUntil = cooldownUntilByPlayer
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .getOrDefault(spell.id(), 0L);
        if (now < cooldownUntil) {
            player.sendActionBar(Component.text("Spell cooldown: " + (cooldownUntil - now) + "t", NamedTextColor.GRAY));
            return;
        }

        if (!focusManager.hasEnough(player, spell.fpCost())) {
            player.sendActionBar(Component.text("Not enough FP (" + formatNumber(spell.fpCost()) + ")", NamedTextColor.RED));
            return;
        }

        long castDelay = adjustedCastTime(player, spell);
        player.sendActionBar(Component.text("Casting " + spell.displayName() + "...", NamedTextColor.LIGHT_PURPLE));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.7F, 1.2F);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            castingTasks.remove(player.getUniqueId());
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            if (!focusManager.spend(player, spell.fpCost())) {
                player.sendActionBar(Component.text("Cast cancelled: not enough FP.", NamedTextColor.RED));
                return;
            }
            castSpell(player, spell);
            cooldownUntilByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                    .put(spell.id(), plugin.getServer().getCurrentTick() + adjustedCooldown(player, spell));
            player.sendActionBar(Component.text(
                    spell.displayName() + " | FP -" + formatNumber(spell.fpCost()),
                    NamedTextColor.AQUA
            ));
        }, castDelay);
        castingTasks.put(player.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayerState(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        clearPlayerState(event.getPlayer());
    }

    public void shutdown() {
        for (BukkitTask task : castingTasks.values()) {
            task.cancel();
        }
        castingTasks.clear();
        cooldownUntilByPlayer.clear();
    }

    private String validateRequirements(PlayerProgress progress, SpellDefinition spell) {
        if (progress.attribute(spell.primaryRequirementAttribute()) < spell.primaryRequirementLevel()) {
            return spell.primaryRequirementAttribute().displayName() + " " + spell.primaryRequirementLevel() + " required.";
        }
        if (spell.hasSecondaryRequirement()
                && progress.attribute(spell.secondaryRequirementAttribute()) < spell.secondaryRequirementLevel()) {
            return spell.secondaryRequirementAttribute().displayName() + " " + spell.secondaryRequirementLevel() + " required.";
        }
        return null;
    }

    private long adjustedCastTime(Player player, SpellDefinition spell) {
        double castSpeed = levelManager.getDexterityCastSpeedMultiplier(player);
        if (spell.school() == SpellSchool.SORCERY) {
            castSpeed *= levelManager.getIntelligenceCastSpeedMultiplier(player);
        } else {
            castSpeed *= levelManager.getFaithCastSpeedMultiplier(player);
            if (spell.secondaryRequirementAttribute() == AttributeType.ARCANE) {
                castSpeed *= levelManager.getArcaneCastSpeedMultiplier(player);
            }
        }
        castSpeed *= Math.max(0.1D, castSpeedMultiplierProvider.apply(player));
        return Math.max(2L, (long) Math.ceil(spell.castTimeTicks() / castSpeed));
    }

    private long adjustedCooldown(Player player, SpellDefinition spell) {
        return Math.max(1L, Math.round(spell.cooldownTicks() / Math.max(0.1D, castSpeedMultiplierProvider.apply(player))));
    }

    private void castSpell(Player player, SpellDefinition spell) {
        switch (spell.id()) {
            case "glintstone_pebble" -> castGlintstonePebble(player);
            case "glintstone_arc" -> castGlintstoneArc(player);
            case "carian_slicer" -> castCarianSlicer(player);
            case "magic_glintblade" -> castMagicGlintblade(player);
            case "urgent_heal" -> castUrgentHeal(player);
            case "heal" -> castHeal(player);
            case "catch_flame" -> castCatchFlame(player);
            case "lightning_spear", "ancient_dragons_lightning_spear" -> castLightningSpear(player, spell.id().startsWith("ancient") ? 8.5D : 7.0D);
            case "agheels_flame", "aspect_of_the_crucible_breath" -> castFlameBreath(player, spell.id().equals("agheels_flame") ? 7.5D : 6.0D);
            case "ancient_dragons_lightning_strike" -> castLightningBurst(player);
            case "aspect_of_the_crucible_horns" -> castCrucibleHorns(player);
            case "aspects_of_the_crucible_tail" -> castCrucibleTail(player);
            case "assassins_approach" -> castAssassinsApproach(player);
            case "barrier_of_gold" -> castBarrierOfGold(player);
            case "beast_claw" -> castBeastClaw(player);
            case "bestial_constitution" -> castBestialConstitution(player);
            case "bestial_sling" -> castBestialSling(player);
            default -> player.sendMessage(Component.text("Missing spell effect: " + spell.displayName(), NamedTextColor.RED));
        }
    }

    private void castGlintstonePebble(Player player) {
        LivingEntity target = rayTarget(player, 24.0D);
        if (target == null) {
            player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
            return;
        }
        double damage = 4.0D * levelManager.getIntelligenceAttackMultiplier(player);
        damageTarget(player, target, damage);
        traceParticles(player.getEyeLocation(), target.getEyeLocation(), Particle.ENCHANT, 18);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9F, 1.4F);
    }

    private void castCarianSlicer(Player player) {
        LivingEntity target = rayTarget(player, 4.0D);
        if (target == null) {
            player.sendActionBar(Component.text("No target for Carian Slicer.", NamedTextColor.GRAY));
            return;
        }
        double damage = 5.5D * levelManager.getIntelligenceAttackMultiplier(player);
        damageTarget(player, target, damage);
        target.setVelocity(target.getVelocity().add(player.getLocation().getDirection().normalize().multiply(0.4D)));
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.3F);
    }

    private void castGlintstoneArc(Player player) {
        boolean hit = false;
        for (LivingEntity target : nearbyTargets(player, 6.0D)) {
            if (isInFront(player, target, 0.45D)) {
                damageTarget(player, target, 4.5D * levelManager.getIntelligenceAttackMultiplier(player));
                hit = true;
            }
        }
        if (!hit) {
            player.sendActionBar(Component.text("No target in arc.", NamedTextColor.GRAY));
            return;
        }
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 35, 1.6D, 0.5D, 1.6D, 0.02D);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.8F, 1.3F);
    }

    private void castMagicGlintblade(Player player) {
        LivingEntity target = rayTarget(player, 24.0D);
        if (target == null) {
            player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
            return;
        }
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getEyeLocation(), 18, 0.15D, 0.15D, 0.15D, 0.01D);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.7F, 1.4F);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isDead() || target.isDead()) {
                return;
            }
            damageTarget(player, target, 5.0D * levelManager.getIntelligenceAttackMultiplier(player));
            traceParticles(player.getEyeLocation(), target.getEyeLocation(), Particle.WITCH, 20);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.8F, 1.6F);
        }, 14L);
    }

    private void castUrgentHeal(Player player) {
        if (player.getAttribute(Attribute.MAX_HEALTH) == null) {
            return;
        }
        double healAmount = 5.0D * levelManager.getFaithAttackMultiplier(player);
        double nextHealth = Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(), player.getHealth() + healAmount);
        player.setHealth(nextHealth);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 6, 0.4D, 0.6D, 0.4D, 0.0D);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8F, 1.6F);
    }

    private void castHeal(Player player) {
        if (player.getAttribute(Attribute.MAX_HEALTH) == null) {
            return;
        }
        double healAmount = 8.0D * levelManager.getFaithAttackMultiplier(player);
        double nextHealth = Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(), player.getHealth() + healAmount);
        player.setHealth(nextHealth);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.6D, 0.8D, 0.6D, 0.0D);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.9F, 1.4F);
    }

    private void castCatchFlame(Player player) {
        boolean hit = false;
        for (LivingEntity target : nearbyTargets(player, 4.0D)) {
            if (isInFront(player, target, 0.4D)) {
                damageTarget(player, target, 4.0D * levelManager.getFaithAttackMultiplier(player));
                target.setFireTicks(Math.max(target.getFireTicks(), 60));
                hit = true;
            }
        }
        if (!hit) {
            player.sendActionBar(Component.text("No target in flame range.", NamedTextColor.GRAY));
            return;
        }
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 30, 0.3D, 0.3D, 0.3D, 0.02D);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0F, 1.4F);
    }

    private void castLightningSpear(Player player, double baseDamage) {
        LivingEntity target = rayTarget(player, 28.0D);
        if (target == null) {
            player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
            return;
        }
        double damage = baseDamage * levelManager.getFaithAttackMultiplier(player);
        target.getWorld().strikeLightningEffect(target.getLocation());
        damageTarget(player, target, damage);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.7F, 1.5F);
    }

    private void castFlameBreath(Player player, double baseDamage) {
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 60, 0.4D, 0.4D, 0.4D, 0.02D);
        for (LivingEntity target : nearbyTargets(player, 7.0D)) {
            if (isInFront(player, target, 0.45D)) {
                damageTarget(player, target, baseDamage * levelManager.getFaithAttackMultiplier(player));
                target.setFireTicks(Math.max(target.getFireTicks(), 80));
            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0F, 0.8F);
    }

    private void castLightningBurst(Player player) {
        Location center = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(4.0D));
        center.getWorld().strikeLightningEffect(center);
        for (LivingEntity target : nearbyTargets(center, player, 5.0D)) {
            damageTarget(player, target, 7.5D * levelManager.getFaithAttackMultiplier(player));
        }
    }

    private void castCrucibleHorns(Player player) {
        player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.4D).setY(0.35D));
        for (LivingEntity target : nearbyTargets(player, 3.0D)) {
            if (isInFront(player, target, 0.3D)) {
                damageTarget(player, target, 6.5D * levelManager.getFaithAttackMultiplier(player));
            }
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GOAT_RAM_IMPACT, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private void castCrucibleTail(Player player) {
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 8, 1.5D, 0.3D, 1.5D, 0.0D);
        for (LivingEntity target : nearbyTargets(player, 4.5D)) {
            damageTarget(player, target, 5.5D * levelManager.getFaithAttackMultiplier(player));
            pushAway(player, target, 1.1D);
        }
    }

    private void castAssassinsApproach(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 45, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 45, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 45, 0, false, false));
    }

    private void castBarrierOfGold(Player player) {
        for (Player ally : player.getWorld().getNearbyPlayers(player.getLocation(), 6.0D)) {
            ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 20, 0, false, false));
        }
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 25, 1.0D, 1.0D, 1.0D, 0.05D);
    }

    private void castBeastClaw(Player player) {
        for (LivingEntity target : nearbyTargets(player, 8.0D)) {
            if (isInFront(player, target, 0.35D)) {
                damageTarget(player, target, 5.0D * levelManager.getFaithAttackMultiplier(player));
                pushAway(player, target, 0.8D);
            }
        }
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 0.1D, 0), 30, 1.2D, 0.2D, 1.2D, 0.1D);
    }

    private void castBestialConstitution(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0, false, false));
    }

    private void castBestialSling(Player player) {
        for (LivingEntity target : nearbyTargets(player, 9.0D)) {
            if (isInFront(player, target, 0.2D)) {
                damageTarget(player, target, 3.5D * levelManager.getFaithAttackMultiplier(player));
            }
        }
        player.getWorld().spawnParticle(Particle.BLOCK, player.getEyeLocation(), 25, 0.35D, 0.25D, 0.35D, 0.0D,
                org.bukkit.Material.STONE.createBlockData());
    }

    private LivingEntity rayTarget(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                0.35D,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        if (result == null) {
            return null;
        }
        Entity hit = result.getHitEntity();
        return hit instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private Iterable<LivingEntity> nearbyTargets(Player player, double range) {
        return nearbyTargets(player.getLocation(), player, range);
    }

    private java.util.List<LivingEntity> nearbyTargets(Location center, Player caster, double range) {
        java.util.List<LivingEntity> targets = new java.util.ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (entity instanceof LivingEntity living && !entity.equals(caster)) {
                targets.add(living);
            }
        }
        return targets;
    }

    private boolean isInFront(Player player, LivingEntity target, double threshold) {
        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
        if (toTarget.lengthSquared() < 0.0001D) {
            return true;
        }
        return player.getLocation().getDirection().normalize().dot(toTarget.normalize()) >= threshold;
    }

    private void damageTarget(Player player, LivingEntity target, double damage) {
        target.damage(damage, player);
        Vector offset = target.getLocation().toVector().subtract(player.getLocation().toVector());
        if (offset.lengthSquared() > 0.0001D) {
            Vector knockback = offset.normalize().multiply(0.25D);
            target.setVelocity(target.getVelocity().add(knockback));
        }
    }

    private void pushAway(Player player, LivingEntity target, double strength) {
        Vector offset = target.getLocation().toVector().subtract(player.getLocation().toVector());
        if (offset.lengthSquared() > 0.0001D) {
            target.setVelocity(target.getVelocity().add(offset.normalize().multiply(strength).setY(0.25D)));
        }
    }

    private void traceParticles(Location start, Location end, Particle particle, int points) {
        Vector delta = end.toVector().subtract(start.toVector());
        for (int i = 0; i <= points; i++) {
            double factor = (double) i / points;
            Location point = start.clone().add(delta.clone().multiply(factor));
            start.getWorld().spawnParticle(particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void clearPlayerState(Player player) {
        BukkitTask task = castingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        cooldownUntilByPlayer.remove(player.getUniqueId());
    }
}



