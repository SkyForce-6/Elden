package de.skyforce.main.elden.weapon.service;

import de.skyforce.main.elden.ashes.service.AshOfWarBindingService;
import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.level.PlayerProgress;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.weapon.model.WeaponDefinition;
import de.skyforce.main.elden.weapon.model.WeaponGuardStats;
import de.skyforce.main.elden.weapon.model.WeaponType;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class WeaponGameplayService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final double STATUS_DECAY_PER_SECOND = 6.0D;

    private final JavaPlugin plugin;
    private final WeaponRegistry weaponRegistry;
    private final WeaponItemFactory weaponItemFactory;
    private final WeaponDamageService weaponDamageService;
    private final AshOfWarBindingService ashOfWarBindingService;
    private final LevelManager levelManager;
    private final FocusManager focusManager;
    private final StaminaManager staminaManager;
    private final RuneManager runeManager;
    private final Map<UUID, Long> requirementFeedbackUntil = new HashMap<>();
    private final Map<UUID, Map<String, Long>> skillCooldownUntil = new HashMap<>();
    private final Map<UUID, Map<String, Double>> statusBuildupByTarget = new HashMap<>();
    private final Map<UUID, Long> statusLastUpdateTick = new HashMap<>();
    private final Map<UUID, Long> attackCooldownUntil = new HashMap<>();
    private final Map<UUID, Double> poiseDamageByTarget = new HashMap<>();
    private final Map<UUID, Long> poiseLastUpdateTick = new HashMap<>();
    private Function<Player, Double> jumpAttackMultiplierProvider = player -> 1.0D;
    private Function<Player, Double> statusBuildupMultiplierProvider = player -> 1.0D;
    private Function<Player, Double> castSpeedMultiplierProvider = player -> 1.0D;

    public WeaponGameplayService(JavaPlugin plugin, WeaponRegistry weaponRegistry, WeaponItemFactory weaponItemFactory,
                                 WeaponDamageService weaponDamageService, AshOfWarBindingService ashOfWarBindingService,
                                 LevelManager levelManager, FocusManager focusManager, StaminaManager staminaManager,
                                 RuneManager runeManager) {
        this.plugin = plugin;
        this.weaponRegistry = weaponRegistry;
        this.weaponItemFactory = weaponItemFactory;
        this.weaponDamageService = weaponDamageService;
        this.ashOfWarBindingService = ashOfWarBindingService;
        this.levelManager = levelManager;
        this.focusManager = focusManager;
        this.staminaManager = staminaManager;
        this.runeManager = runeManager;
    }

    public void setJumpAttackMultiplierProvider(Function<Player, Double> jumpAttackMultiplierProvider) {
        this.jumpAttackMultiplierProvider = jumpAttackMultiplierProvider == null ? player -> 1.0D : jumpAttackMultiplierProvider;
    }

    public void setStatusBuildupMultiplierProvider(Function<Player, Double> statusBuildupMultiplierProvider) {
        this.statusBuildupMultiplierProvider = statusBuildupMultiplierProvider == null ? player -> 1.0D : statusBuildupMultiplierProvider;
    }

    public void setCastSpeedMultiplierProvider(Function<Player, Double> castSpeedMultiplierProvider) {
        this.castSpeedMultiplierProvider = castSpeedMultiplierProvider == null ? player -> 1.0D : castSpeedMultiplierProvider;
    }

    public Optional<WeaponDefinition> getWeapon(ItemStack item) {
        return weaponRegistry.getById(weaponItemFactory.getWeaponId(item));
    }

    public boolean meetsRequirements(Player player, WeaponDefinition weapon) {
        return requirementFailure(player, weapon) == null;
    }

    public String requirementFailure(Player player, WeaponDefinition weapon) {
        PlayerProgress progress = levelManager.getOrCreate(player);
        if (progress.attribute(AttributeType.STRENGTH) < weapon.requirements().strength()) {
            return "Strength " + weapon.requirements().strength() + " required.";
        }
        if (progress.attribute(AttributeType.DEXTERITY) < weapon.requirements().dexterity()) {
            return "Dexterity " + weapon.requirements().dexterity() + " required.";
        }
        if (progress.attribute(AttributeType.INTELLIGENCE) < weapon.requirements().intelligence()) {
            return "Intelligence " + weapon.requirements().intelligence() + " required.";
        }
        if (progress.attribute(AttributeType.FAITH) < weapon.requirements().faith()) {
            return "Faith " + weapon.requirements().faith() + " required.";
        }
        if (progress.attribute(AttributeType.ARCANE) < weapon.requirements().arcane()) {
            return "Arcane " + weapon.requirements().arcane() + " required.";
        }
        return null;
    }

    public void maybeWarnRequirements(Player player, WeaponDefinition weapon) {
        String failure = requirementFailure(player, weapon);
        if (failure == null) {
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        long nextAllowed = requirementFeedbackUntil.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowed) {
            return;
        }

        player.sendActionBar(Component.text(failure, NamedTextColor.RED));
        requirementFeedbackUntil.put(player.getUniqueId(), now + 20L);
    }

    public WeaponDefinition getEquippedWeapon(Player player) {
        return getWeapon(player.getInventory().getItemInMainHand()).orElse(null);
    }

    public boolean handleCustomWeaponAttack(Player player, Entity entity) {
        WeaponDefinition weapon = getEquippedWeapon(player);
        if (weapon == null) {
            return true;
        }

        maybeWarnRequirements(player, weapon);
        if (!meetsRequirements(player, weapon)) {
            return false;
        }

        long now = plugin.getServer().getCurrentTick();
        long cooldownUntil = attackCooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            player.sendActionBar(Component.text("Weapon recovering", NamedTextColor.GRAY));
            return false;
        }

        if (entity instanceof LivingEntity livingEntity && !isWithinWeaponRange(player, livingEntity, weapon)) {
            player.sendActionBar(Component.text("Out of range", NamedTextColor.GRAY));
            return false;
        }

        double staminaCost = resolveAttackStaminaCost(weapon);
        if (!staminaManager.spend(player, staminaCost)) {
            player.sendActionBar(Component.text("Not enough stamina", NamedTextColor.RED));
            return false;
        }

        staminaManager.updateHud(player);
        attackCooldownUntil.put(player.getUniqueId(), now + resolveAttackCooldownTicks(weapon.weaponType()));
        return true;
    }

    public void applyPassiveOnHit(Player attacker, LivingEntity target, EntityDamageByEntityEvent event) {
        WeaponDefinition weapon = getWeapon(attacker.getInventory().getItemInMainHand()).orElse(null);
        if (weapon == null) {
            return;
        }

        maybeWarnRequirements(attacker, weapon);

        if (!meetsRequirements(attacker, weapon)) {
            return;
        }

        if (isJumpAttack(attacker)) {
            event.setDamage(event.getDamage() * resolveJumpAttackDamageMultiplier(weapon.weaponType())
                    * Math.max(0.0D, jumpAttackMultiplierProvider.apply(attacker)));
            target.setVelocity(target.getVelocity().add(attacker.getLocation().getDirection().normalize().multiply(0.25D)).setY(0.35D));
            attacker.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0D, 1.0D, 0.0D), 2, 0.2D, 0.2D, 0.2D, 0.0D);
        }

        String passive = weapon.passiveEffect().toLowerCase(Locale.ROOT);
        if (passive.contains("blood loss")) {
            applyStatusBuildup(attacker, target, "blood_loss", parseStatusValue(weapon.passiveEffect(), 38.0D), 100.0D);
        }

        if (passive.contains("scarlet rot")) {
            applyStatusBuildup(attacker, target, "scarlet_rot", parseStatusValue(weapon.passiveEffect(), 45.0D), 110.0D);
        }

        if (passive.contains("frost")) {
            applyStatusBuildup(attacker, target, "frost", parseStatusValue(weapon.passiveEffect(), 30.0D), 90.0D);
        }

        if (passive.contains("poison")) {
            applyStatusBuildup(attacker, target, "poison", parseStatusValue(weapon.passiveEffect(), 35.0D), 90.0D);
        }

        if (passive.contains("generates runes on hit")) {
            runeManager.addRunes(attacker.getUniqueId(), 1);
            if (plugin.getServer().getCurrentTick() % 10L == 0L) {
                attacker.sendActionBar(Component.text("+1 Rune", NamedTextColor.GOLD));
            }
        }

        if (passive.contains("bestial")) {
            staminaManager.restore(attacker, 1.5D);
        }

        applyUniqueHitEffects(attacker, target, weapon);
        applyWeaponTypeHitEffects(attacker, target, weapon, event);
        applyPoiseDamage(attacker, target, weapon, event);
    }

    public void applyGuard(Player player, EntityDamageEvent event) {
        if (!player.isBlocking()) {
            return;
        }

        WeaponDefinition shield = resolveBlockingShield(player);
        if (shield == null) {
            return;
        }

        maybeWarnRequirements(player, shield);
        if (!meetsRequirements(player, shield)) {
            return;
        }

        int guardValue = resolveGuardValue(shield.guardStats(), event.getCause());
        if (guardValue <= 0) {
            return;
        }

        double originalDamage = event.getDamage();
        double reducedDamage = originalDamage * Math.max(0.0D, 1.0D - (guardValue / 100.0D));
        double absorbed = Math.max(0.0D, originalDamage - reducedDamage);
        double staminaCost = Math.max(2.0D, absorbed * Math.max(0.2D, (100.0D - shield.guardStats().boost()) / 100.0D));

        if (!staminaManager.spend(player, staminaCost)) {
            player.sendActionBar(Component.text("Guard broken", NamedTextColor.RED));
            return;
        }

        staminaManager.updateHud(player);
        event.setDamage(reducedDamage);

        if (shield.passiveEffect().toLowerCase(Locale.ROOT).contains("magic guard")
                && event.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
            event.setDamage(event.getDamage() * 0.75D);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.9F);
    }

    public boolean tryUseBuiltInSkill(Player player, ItemStack item) {
        WeaponDefinition weapon = getWeapon(item).orElse(null);
        if (weapon == null) {
            return false;
        }

        if (ashOfWarBindingService.getBoundAshId(item) != null) {
            return false;
        }

        maybeWarnRequirements(player, weapon);
        if (!meetsRequirements(player, weapon)) {
            return true;
        }

        String normalizedSkill = normalizeSkillId(weapon.skillName());
        if (normalizedSkill.equals("no_skill")) {
            return false;
        }

        long now = plugin.getServer().getCurrentTick();
        long cooldownUntil = skillCooldownUntil
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .getOrDefault(normalizedSkill, 0L);
        if (now < cooldownUntil) {
            player.sendActionBar(Component.text("Weapon skill cooldown: " + (cooldownUntil - now) + "t", NamedTextColor.GRAY));
            return true;
        }

        double fpCost = parseFpCost(weapon.skillFpCost());
        if (!focusManager.hasEnough(player, fpCost)) {
            player.sendActionBar(Component.text("Not enough FP (" + formatNumber(fpCost) + ")", NamedTextColor.RED));
            return true;
        }

        if (!focusManager.spend(player, fpCost)) {
            return true;
        }

        boolean used = executeSkill(player, weapon, normalizedSkill);
        if (!used) {
            focusManager.restore(player, fpCost);
            return false;
        }

        long cooldown = Math.max(1L, Math.round(resolveSkillCooldown(normalizedSkill)
                / Math.max(0.1D, castSpeedMultiplierProvider.apply(player))));
        skillCooldownUntil.get(player.getUniqueId()).put(normalizedSkill, now + cooldown);
        player.sendActionBar(Component.text(weapon.skillName() + " | FP -" + formatNumber(fpCost), NamedTextColor.GOLD));
        return true;
    }

    private boolean executeSkill(Player player, WeaponDefinition weapon, String skillId) {
        return switch (skillId) {
            case "kick" -> {
                LivingEntity target = rayTarget(player, 3.0D);
                if (target != null) {
                    damageTarget(player, target, resolveSkillDamage(player, 3.0D));
                    target.setVelocity(target.getVelocity().add(player.getLocation().getDirection().normalize().multiply(0.8D)).setY(0.35D));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
                yield true;
            }
            case "quickstep" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 2, false, false));
                player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.3D));
                yield true;
            }
            case "parry", "buckler_parry" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 16, 3, false, false));
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.3F);
                yield true;
            }
            case "shield_bash" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 1, false, false));
                player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.1D).setY(0.2D));
                damageNearby(player, 2.5D, resolveSkillDamage(player, 4.0D));
                yield true;
            }
            case "spinning_slash" -> {
                damageNearby(player, 3.0D, resolveSkillDamage(player, 4.5D));
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.6D, 0.2D, 0.6D, 0.0D);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.1F);
                yield true;
            }
            case "wild_strikes" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 80, 1, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 0, false, false));
                yield true;
            }
            case "impaling_thrust" -> {
                player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.5D).setY(0.15D));
                LivingEntity target = rayTarget(player, 4.5D);
                if (target != null) {
                    damageTarget(player, target, resolveSkillDamage(player, 5.5D));
                }
                yield true;
            }
            case "unsheathe" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 16, 3, false, false));
                LivingEntity target = rayTarget(player, 4.0D);
                if (target != null) {
                    damageTarget(player, target, resolveSkillDamage(player, 6.5D));
                    target.setVelocity(target.getVelocity().add(player.getLocation().getDirection().normalize().multiply(0.5D)));
                }
                yield true;
            }
            case "charge_forth" -> {
                player.setVelocity(player.getLocation().getDirection().normalize().multiply(2.0D).setY(0.15D));
                damageNearby(player, 2.5D, resolveSkillDamage(player, 4.0D));
                yield true;
            }
            case "barrage" -> {
                launchArrowBurst(player, 3, 0.18D, 2.0D);
                yield true;
            }
            case "mighty_shot" -> {
                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(3.2D));
                arrow.setCritical(true);
                arrow.setDamage(resolveSkillDamage(player, 5.5D));
                yield true;
            }
            case "glintstone_dart" -> {
                LivingEntity target = rayTarget(player, 20.0D);
                if (target == null) {
                    player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
                    yield true;
                }
                damageTarget(player, target, 4.0D * levelManager.getIntelligenceAttackMultiplier(player));
                traceColoredParticles(player.getEyeLocation(), target.getEyeLocation(), Color.fromRGB(90, 180, 255));
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30, 0, false, false));
                yield true;
            }
            case "reduvia_blood_blade" -> {
                LivingEntity target = rayTarget(player, 14.0D);
                if (target == null) {
                    player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
                    yield true;
                }
                damageTarget(player, target, resolveSkillDamage(player, 4.5D));
                applyStatusBuildup(player, target, "blood_loss", 65.0D, 100.0D);
                traceColoredParticles(player.getEyeLocation(), target.getEyeLocation(), Color.fromRGB(160, 15, 35));
                yield true;
            }
            case "blade_of_gold" -> {
                LivingEntity target = rayTarget(player, 18.0D);
                if (target == null) {
                    player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
                    yield true;
                }
                damageTarget(player, target, 4.5D * levelManager.getFaithAttackMultiplier(player));
                target.setGlowing(true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (target.isValid()) {
                        target.setGlowing(false);
                    }
                }, 60L);
                traceColoredParticles(player.getEyeLocation(), target.getEyeLocation(), Color.fromRGB(245, 220, 90));
                yield true;
            }
            case "blade_of_death" -> {
                LivingEntity target = rayTarget(player, 18.0D);
                if (target == null) {
                    player.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
                    yield true;
                }
                damageTarget(player, target, 5.0D * Math.max(levelManager.getFaithAttackMultiplier(player),
                        levelManager.getDexterityAttackMultiplier(player)));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1, false, true));
                lowerTargetMaxHealth(target, 80L, 2.0D);
                traceColoredParticles(player.getEyeLocation(), target.getEyeLocation(), Color.fromRGB(40, 40, 40));
                yield true;
            }
            default -> false;
        };
    }

    private void applyUniqueHitEffects(Player attacker, LivingEntity target, WeaponDefinition weapon) {
        switch (weapon.id()) {
            case "glintstone_kris" -> {
                if (plugin.getServer().getCurrentTick() % 3L == 0L) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false));
                }
            }
            case "crystal_knife" -> {
                if (plugin.getServer().getCurrentTick() % 2L == 0L) {
                    target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0.0D, 1.0D, 0.0D), 10, 0.2D, 0.3D, 0.2D, 0.0D);
                    target.damage(0.5D, attacker);
                }
            }
            case "ivory_sickle" -> applyStatusBuildup(attacker, target, "frost", 24.0D, 90.0D);
            case "celebrants_sickle" -> runeManager.addRunes(attacker.getUniqueId(), 1);
            case "cinquedea" -> staminaManager.restore(attacker, 2.0D);
            default -> {
            }
        }
    }

    private void applyWeaponTypeHitEffects(Player attacker, LivingEntity target, WeaponDefinition weapon, EntityDamageByEntityEvent event) {
        switch (weapon.weaponType()) {
            case DAGGER -> target.setVelocity(target.getVelocity().add(attacker.getLocation().getDirection().normalize().multiply(0.12D)));
            case GREATSWORD, GREATAXE -> target.setVelocity(target.getVelocity().add(attacker.getLocation().getDirection().normalize().multiply(0.42D)).setY(0.24D));
            case SPEAR, HALBERD, THRUSTING_SWORD -> {
                if (isForwardAttack(attacker)) {
                    event.setDamage(event.getDamage() * 1.08D);
                }
            }
            case TWINBLADE -> {
                if (plugin.getServer().getCurrentTick() % 2L == 0L) {
                    target.damage(Math.max(0.5D, event.getDamage() * 0.18D), attacker);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0D, 1.0D, 0.0D), 1);
                }
            }
            case AXE -> staminaManager.restore(attacker, 0.4D);
            default -> {
            }
        }
    }

    private void applyPoiseDamage(Player attacker, LivingEntity target, WeaponDefinition weapon, EntityDamageByEntityEvent event) {
        long now = plugin.getServer().getCurrentTick();
        decayPoise(target.getUniqueId(), now);

        double basePoiseDamage = plugin.getConfig().getDouble(
                "combat.poise.weapon-type-damage." + weapon.weaponType().name(),
                defaultPoiseDamage(weapon.weaponType())
        );
        if (isJumpAttack(attacker)) {
            basePoiseDamage *= plugin.getConfig().getDouble("combat.poise.jump-attack-multiplier", 1.45D);
        }
        if (event.isCritical()) {
            basePoiseDamage *= 1.1D;
        }

        double next = poiseDamageByTarget.getOrDefault(target.getUniqueId(), 0.0D) + basePoiseDamage;
        double threshold = resolvePoiseThreshold(target);
        if (next >= threshold) {
            poiseDamageByTarget.put(target.getUniqueId(), Math.max(0.0D, next - threshold));
            triggerStagger(attacker, target, weapon);
        } else {
            poiseDamageByTarget.put(target.getUniqueId(), next);
            poiseLastUpdateTick.put(target.getUniqueId(), now);
            if (now % 8L == 0L) {
                attacker.sendActionBar(Component.text("Poise " + (int) Math.min(99.0D, Math.round((next / threshold) * 100.0D)) + "%", NamedTextColor.YELLOW));
            }
        }
    }

    private void applyStatusBuildup(Player attacker, LivingEntity target, String statusId, double amount, double threshold) {
        long now = plugin.getServer().getCurrentTick();
        decayStatuses(target.getUniqueId(), now);

        Map<String, Double> buildup = statusBuildupByTarget.computeIfAbsent(target.getUniqueId(), ignored -> new HashMap<>());
        double next = buildup.getOrDefault(statusId, 0.0D)
                + (amount * Math.max(0.0D, statusBuildupMultiplierProvider.apply(attacker)));
        if (next >= threshold) {
            buildup.put(statusId, Math.max(0.0D, next - threshold));
            triggerStatusProc(attacker, target, statusId);
        } else {
            buildup.put(statusId, next);
            showBuildupFeedback(attacker, statusId, next, threshold);
        }

        statusLastUpdateTick.put(target.getUniqueId(), now);
    }

    private void decayStatuses(UUID targetId, long now) {
        Long previousTick = statusLastUpdateTick.get(targetId);
        if (previousTick == null) {
            return;
        }

        long deltaTicks = now - previousTick;
        if (deltaTicks <= 0L) {
            return;
        }

        Map<String, Double> buildup = statusBuildupByTarget.get(targetId);
        if (buildup == null || buildup.isEmpty()) {
            return;
        }

        double decay = (deltaTicks / 20.0D) * STATUS_DECAY_PER_SECOND;
        List<String> depleted = new ArrayList<>();
        for (Map.Entry<String, Double> entry : buildup.entrySet()) {
            double next = Math.max(0.0D, entry.getValue() - decay);
            if (next <= 0.0D) {
                depleted.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        for (String key : depleted) {
            buildup.remove(key);
        }
    }

    private void triggerStatusProc(Player attacker, LivingEntity target, String statusId) {
        switch (statusId) {
            case "blood_loss" -> {
                double burstDamage = Math.max(2.5D, target.getMaxHealth() * 0.12D);
                target.damage(burstDamage, attacker);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, false, true));
                target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.4D, 0.35D, 0.0D);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.1F, 0.7F);
                attacker.sendActionBar(Component.text("Blood Loss proc", NamedTextColor.DARK_RED));
            }
            case "scarlet_rot" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 180, 1, false, true));
                target.getWorld().spawnParticle(Particle.OMINOUS_SPAWNING, target.getLocation().add(0.0D, 1.0D, 0.0D), 16, 0.2D, 0.4D, 0.2D, 0.0D);
                attacker.sendActionBar(Component.text("Scarlet Rot proc", NamedTextColor.RED));
            }
            case "poison" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 180, 0, false, true));
                attacker.sendActionBar(Component.text("Poison proc", NamedTextColor.GREEN));
            }
            case "frost" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, false, true));
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0.0D, 1.0D, 0.0D), 20, 0.3D, 0.4D, 0.3D, 0.01D);
                attacker.sendActionBar(Component.text("Frostbite proc", NamedTextColor.AQUA));
            }
            default -> {
            }
        }
    }

    private void showBuildupFeedback(Player attacker, String statusId, double amount, double threshold) {
        int percent = (int) Math.min(99.0D, Math.round((amount / threshold) * 100.0D));
        NamedTextColor color = switch (statusId) {
            case "blood_loss" -> NamedTextColor.DARK_RED;
            case "scarlet_rot" -> NamedTextColor.RED;
            case "poison" -> NamedTextColor.GREEN;
            case "frost" -> NamedTextColor.AQUA;
            default -> NamedTextColor.GRAY;
        };
        String label = switch (statusId) {
            case "blood_loss" -> "Bleed";
            case "scarlet_rot" -> "Rot";
            case "poison" -> "Poison";
            case "frost" -> "Frost";
            default -> "Status";
        };
        if (plugin.getServer().getCurrentTick() % 6L == 0L) {
            attacker.sendActionBar(Component.text(label + " buildup " + percent + "%", color));
        }
    }

    private void decayPoise(UUID targetId, long now) {
        Long previousTick = poiseLastUpdateTick.get(targetId);
        if (previousTick == null) {
            return;
        }
        long deltaTicks = now - previousTick;
        if (deltaTicks <= 0L) {
            return;
        }

        double decayPerSecond = plugin.getConfig().getDouble("combat.poise.decay-per-second", 10.0D);
        double current = poiseDamageByTarget.getOrDefault(targetId, 0.0D);
        if (current <= 0.0D) {
            return;
        }
        double next = Math.max(0.0D, current - ((deltaTicks / 20.0D) * decayPerSecond));
        poiseDamageByTarget.put(targetId, next);
        poiseLastUpdateTick.put(targetId, now);
    }

    private void triggerStagger(Player attacker, LivingEntity target, WeaponDefinition weapon) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                plugin.getConfig().getInt("combat.poise.stagger-duration-ticks", 30),
                4,
                false,
                true));
        target.setVelocity(attacker.getLocation().getDirection().normalize().multiply(0.25D).setY(0.35D));
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.4D, 0.35D, 0.05D);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.8F, 1.2F);
        attacker.sendActionBar(Component.text(weapon.displayName() + " staggered target", NamedTextColor.GOLD));
        poiseLastUpdateTick.put(target.getUniqueId(), (long) plugin.getServer().getCurrentTick());
    }

    private double parseStatusValue(String passive, double fallback) {
        Matcher matcher = NUMBER_PATTERN.matcher(passive);
        if (!matcher.find()) {
            return fallback;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private boolean isJumpAttack(Player attacker) {
        return attacker.getFallDistance() > 0.0F && !attacker.isOnGround() && !attacker.isInsideVehicle();
    }

    private double resolveJumpAttackDamageMultiplier(WeaponType type) {
        return plugin.getConfig().getDouble(
                "combat.jump-attacks.damage-multiplier." + type.name(),
                defaultJumpAttackDamageMultiplier(type)
        );
    }

    private double defaultJumpAttackDamageMultiplier(WeaponType type) {
        return switch (type) {
            case DAGGER -> 1.1D;
            case STRAIGHT_SWORD, THRUSTING_SWORD, KATANA, AXE -> 1.2D;
            case SPEAR, HALBERD, TWINBLADE, STAFF, SHIELD -> 1.18D;
            case GREATSWORD, GREATAXE -> 1.32D;
            case BOW, CROSSBOW -> 1.05D;
        };
    }

    private double resolvePoiseThreshold(LivingEntity target) {
        double base = plugin.getConfig().getDouble("combat.poise.base-threshold", 100.0D);
        return Math.max(20.0D, base + (target.getMaxHealth() * plugin.getConfig().getDouble("combat.poise.max-health-factor", 0.35D)));
    }

    private double defaultPoiseDamage(WeaponType type) {
        return switch (type) {
            case DAGGER -> 10.0D;
            case STRAIGHT_SWORD, THRUSTING_SWORD, KATANA -> 14.0D;
            case AXE, SPEAR, BOW, CROSSBOW -> 16.0D;
            case HALBERD, TWINBLADE, STAFF, SHIELD -> 18.0D;
            case GREATSWORD -> 24.0D;
            case GREATAXE -> 28.0D;
        };
    }

    private double resolveAttackStaminaCost(WeaponDefinition weapon) {
        double base = Math.max(0.0D, plugin.getConfig().getDouble("combat.stamina.attack-cost", 7.0D));
        double multiplier = plugin.getConfig().getDouble(
                "combat.weapon-types.attack-cost-multiplier." + weapon.weaponType().name(),
                defaultAttackCostMultiplier(weapon.weaponType())
        );
        return Math.max(1.0D, base * multiplier);
    }

    private long resolveAttackCooldownTicks(WeaponType type) {
        return Math.max(1L, plugin.getConfig().getLong(
                "combat.weapon-types.cooldown-ticks." + type.name(),
                defaultAttackCooldown(type)
        ));
    }

    private boolean isWithinWeaponRange(Player player, LivingEntity target, WeaponDefinition weapon) {
        double range = plugin.getConfig().getDouble(
                "combat.weapon-types.range." + weapon.weaponType().name(),
                defaultWeaponRange(weapon.weaponType())
        );
        double maxDistanceSquared = range * range;
        return player.getEyeLocation().distanceSquared(target.getEyeLocation()) <= maxDistanceSquared;
    }

    private boolean isForwardAttack(Player attacker) {
        return !attacker.isSneaking() && !attacker.isSprinting();
    }

    private double defaultAttackCostMultiplier(WeaponType type) {
        return switch (type) {
            case DAGGER -> 0.65D;
            case STRAIGHT_SWORD, THRUSTING_SWORD, KATANA -> 0.9D;
            case AXE, SPEAR -> 1.0D;
            case HALBERD, TWINBLADE, BOW, CROSSBOW -> 1.05D;
            case GREATSWORD -> 1.3D;
            case GREATAXE, SHIELD -> 1.4D;
            case STAFF -> 0.8D;
        };
    }

    private long defaultAttackCooldown(WeaponType type) {
        return switch (type) {
            case DAGGER -> 6L;
            case STRAIGHT_SWORD, THRUSTING_SWORD, KATANA -> 9L;
            case AXE, SPEAR, BOW, CROSSBOW -> 11L;
            case HALBERD, TWINBLADE, STAFF, SHIELD -> 12L;
            case GREATSWORD -> 15L;
            case GREATAXE -> 17L;
        };
    }

    private double defaultWeaponRange(WeaponType type) {
        return switch (type) {
            case DAGGER -> 2.5D;
            case STRAIGHT_SWORD, AXE, KATANA -> 3.0D;
            case THRUSTING_SWORD, TWINBLADE, STAFF, SHIELD -> 3.2D;
            case GREATSWORD -> 3.6D;
            case SPEAR, HALBERD -> 4.2D;
            case GREATAXE -> 3.4D;
            case BOW, CROSSBOW -> 12.0D;
        };
    }

    private WeaponDefinition resolveBlockingShield(Player player) {
        WeaponDefinition offhand = getWeapon(player.getInventory().getItemInOffHand()).orElse(null);
        if (offhand != null && offhand.weaponType().name().equals("SHIELD")) {
            return offhand;
        }

        WeaponDefinition mainHand = getWeapon(player.getInventory().getItemInMainHand()).orElse(null);
        if (mainHand != null && mainHand.weaponType().name().equals("SHIELD")) {
            return mainHand;
        }
        return null;
    }

    private int resolveGuardValue(WeaponGuardStats guardStats, EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA -> guardStats.fire();
            case LIGHTNING -> guardStats.lightning();
            case MAGIC, SONIC_BOOM -> guardStats.magic();
            case WITHER -> guardStats.holy();
            default -> guardStats.physical();
        };
    }

    private double parseFpCost(String value) {
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        if (!matcher.find()) {
            return 0.0D;
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String normalizeSkillId(String skillName) {
        return skillName.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replace("-", "_")
                .replace(" ", "_");
    }

    private long resolveSkillCooldown(String skillId) {
        return switch (skillId) {
            case "quickstep", "parry", "buckler_parry" -> 20L;
            case "barrage", "mighty_shot", "glintstone_dart", "blade_of_gold", "blade_of_death", "reduvia_blood_blade" -> 40L;
            default -> 30L;
        };
    }

    private double resolveSkillDamage(Player player, double fallback) {
        double weaponDamage = weaponDamageService.computeFinalDamage(player, levelManager);
        if (weaponDamage < 0.0D) {
            return fallback;
        }
        return Math.max(fallback, weaponDamage * 1.1D);
    }

    private LivingEntity rayTarget(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                0.35D,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );
        if (result == null || !(result.getHitEntity() instanceof LivingEntity livingEntity)) {
            return null;
        }
        return livingEntity;
    }

    private void damageTarget(Player attacker, LivingEntity target, double damage) {
        target.damage(damage, attacker);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8F, 1.0F);
    }

    private void damageNearby(Player player, double radius, double damage) {
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius,
                candidate -> candidate instanceof LivingEntity && !candidate.equals(player))) {
            damageTarget(player, (LivingEntity) entity, damage);
            Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.5D);
            entity.setVelocity(entity.getVelocity().add(knockback));
        }
    }

    private void launchArrowBurst(Player player, int count, double spread, double speed) {
        for (int i = 0; i < count; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Arrow arrow = player.launchProjectile(Arrow.class);
                Vector direction = player.getEyeLocation().getDirection().normalize();
                direction.add(new Vector((Math.random() - 0.5D) * spread, (Math.random() - 0.5D) * spread, (Math.random() - 0.5D) * spread));
                arrow.setVelocity(direction.normalize().multiply(speed));
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            }, i * 2L);
        }
    }

    private void traceParticles(Player player, LivingEntity target, Particle particle) {
        Vector from = player.getEyeLocation().toVector();
        Vector to = target.getEyeLocation().toVector();
        Vector delta = to.clone().subtract(from);
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            Vector point = from.clone().add(delta.clone().multiply(i / (double) steps));
            player.getWorld().spawnParticle(particle, point.getX(), point.getY(), point.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void traceColoredParticles(Location from, Location to, Color color) {
        Vector delta = to.toVector().subtract(from.toVector());
        int steps = 16;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.15F);
        for (int i = 0; i <= steps; i++) {
            Vector point = from.toVector().clone().add(delta.clone().multiply(i / (double) steps));
            from.getWorld().spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
        }
    }

    private void lowerTargetMaxHealth(LivingEntity target, long restoreDelayTicks, double amount) {
        if (target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null) {
            return;
        }

        var attribute = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double base = attribute.getBaseValue();
        double reduced = Math.max(1.0D, base - amount);
        attribute.setBaseValue(reduced);
        if (target.getHealth() > reduced) {
            target.setHealth(reduced);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!target.isValid() || target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null) {
                return;
            }
            double current = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
            target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(Math.max(current, base));
        }, restoreDelayTicks);
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
