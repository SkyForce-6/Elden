package de.skyforce.main.elden.level;

import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.classes.ClassManager;
import de.skyforce.main.elden.classes.EldenClass;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.persistence.PlayerDataRepository;
import de.skyforce.main.elden.runes.RuneManager;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LevelManager {

    private final JavaPlugin plugin;
    private final PlayerDataRepository repository;
    private final RuneManager runeManager;
    private final ClassManager classManager;
    private final StaminaManager staminaManager;
    private final FocusManager focusManager;

    private final Map<UUID, PlayerProgress> progressByPlayer = new HashMap<>();
    private Consumer<Player> derivedStatsPostProcessor = player -> {
    };

    public LevelManager(JavaPlugin plugin, PlayerDataRepository repository, RuneManager runeManager,
                        ClassManager classManager, StaminaManager staminaManager, FocusManager focusManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.runeManager = runeManager;
        this.classManager = classManager;
        this.staminaManager = staminaManager;
        this.focusManager = focusManager;
        loadAll();
    }

    public PlayerProgress getOrCreate(Player player) {
        return progressByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> {
            EldenClass selectedClass = classManager.getPlayerClass(player).orElse(EldenClass.WRETCH);
            PlayerProgress progress = PlayerProgress.fromClass(selectedClass);
            applyDerivedStats(player, progress, false);
            return progress;
        });
    }

    public int getNextCost(Player player) {
        return LevelFormula.nextLevelRuneCost(getOrCreate(player).level());
    }

    public double getDerivedMaxStamina(Player player) {
        return computeTargetMaxStamina(getOrCreate(player));
    }

    public double getDerivedMaxFocus(Player player) {
        return computeTargetMaxFocus(getOrCreate(player));
    }

    public double getDerivedEquipLoad(Player player) {
        PlayerProgress progress = getOrCreate(player);
        return EnduranceScaling.equipLoadForEndurance(progress.attribute(AttributeType.ENDURANCE));
    }

    public double getStrengthAttackMultiplier(Player player, boolean twoHanded, boolean criticalHit) {
        PlayerProgress progress = getOrCreate(player);
        int baseStrength = progress.attribute(AttributeType.STRENGTH);
        int effectiveStrength = StrengthScaling.effectiveStrength(baseStrength, twoHanded, criticalHit);
        double ratio = StrengthScaling.scalingRatio(effectiveStrength);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.strength-max-attack-bonus", 0.35D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getStrengthDefenseMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int strength = progress.attribute(AttributeType.STRENGTH);
        double ratio = StrengthScaling.scalingRatio(strength);
        double maxReduction = Math.max(0.0D, plugin.getConfig().getDouble("levels.strength-max-defense-reduction", 0.18D));
        double reduction = Math.min(0.95D, maxReduction * ratio);
        return 1.0D - reduction;
    }

    public double getDexterityAttackMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int dexterity = progress.attribute(AttributeType.DEXTERITY);
        double ratio = DexterityScaling.attackScalingRatio(dexterity);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.dexterity-max-attack-bonus", 0.30D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getDexterityFallDamageMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int dexterity = progress.attribute(AttributeType.DEXTERITY);
        double ratio = DexterityScaling.attackScalingRatio(dexterity);
        double maxReduction = Math.max(0.0D, plugin.getConfig().getDouble("levels.dexterity-max-fall-reduction", 0.25D));
        double reduction = Math.min(0.90D, maxReduction * ratio);
        return 1.0D - reduction;
    }

    public double getDexterityCastSpeedMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int dexterity = progress.attribute(AttributeType.DEXTERITY);
        double ratio = DexterityScaling.castSpeedRatio(dexterity);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.dexterity-max-cast-speed-bonus", 0.20D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getIntelligenceAttackMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int intelligence = progress.attribute(AttributeType.INTELLIGENCE);
        double ratio = IntelligenceScaling.attackScalingRatio(intelligence);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.intelligence-max-attack-bonus", 0.32D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getIntelligenceMagicDefenseMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int intelligence = progress.attribute(AttributeType.INTELLIGENCE);
        double ratio = IntelligenceScaling.attackScalingRatio(intelligence);
        double maxReduction = Math.max(0.0D, plugin.getConfig().getDouble("levels.intelligence-max-magic-reduction", 0.25D));
        double reduction = Math.min(0.95D, maxReduction * ratio);
        return 1.0D - reduction;
    }

    public double getIntelligenceCastSpeedMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int intelligence = progress.attribute(AttributeType.INTELLIGENCE);
        double ratio = IntelligenceScaling.castSpeedRatio(intelligence);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.intelligence-max-cast-speed-bonus", 0.25D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getFaithAttackMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int faith = progress.attribute(AttributeType.FAITH);
        double ratio = FaithScaling.attackScalingRatio(faith);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.faith-max-attack-bonus", 0.32D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getFaithCastSpeedMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int faith = progress.attribute(AttributeType.FAITH);
        double ratio = FaithScaling.castSpeedRatio(faith);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.faith-max-cast-speed-bonus", 0.25D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getArcaneAttackMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int arcane = progress.attribute(AttributeType.ARCANE);
        double ratio = ArcaneScaling.attackScalingRatio(arcane);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.arcane-max-attack-bonus", 0.30D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getArcaneHolyDefenseMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int arcane = progress.attribute(AttributeType.ARCANE);
        double ratio = ArcaneScaling.attackScalingRatio(arcane);
        double maxReduction = Math.max(0.0D, plugin.getConfig().getDouble("levels.arcane-max-holy-reduction", 0.22D));
        double reduction = Math.min(0.95D, maxReduction * ratio);
        return 1.0D - reduction;
    }

    public double getArcaneDeathResistanceMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int arcane = progress.attribute(AttributeType.ARCANE);
        double ratio = ArcaneScaling.statusScalingRatio(arcane);
        double maxReduction = Math.max(0.0D, plugin.getConfig().getDouble("levels.arcane-max-death-reduction", 0.20D));
        double reduction = Math.min(0.95D, maxReduction * ratio);
        return 1.0D - reduction;
    }

    public double getArcaneStatusBuildupMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int arcane = progress.attribute(AttributeType.ARCANE);
        double ratio = ArcaneScaling.statusScalingRatio(arcane);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.arcane-max-status-buildup-bonus", 0.35D));
        return 1.0D + (maxBonus * ratio);
    }

    public double getArcaneCastSpeedMultiplier(Player player) {
        PlayerProgress progress = getOrCreate(player);
        int arcane = progress.attribute(AttributeType.ARCANE);
        double ratio = ArcaneScaling.castSpeedRatio(arcane);
        double maxBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.arcane-max-cast-speed-bonus", 0.18D));
        return 1.0D + (maxBonus * ratio);
    }

    public int getArcaneItemDiscovery(Player player) {
        PlayerProgress progress = getOrCreate(player);
        return ArcaneScaling.itemDiscovery(progress.attribute(AttributeType.ARCANE));
    }

    public Optional<String> levelUp(Player player, AttributeType attributeType) {
        PlayerProgress progress = getOrCreate(player);

        int levelCap = Math.max(1, plugin.getConfig().getInt("levels.max-level", 713));
        int attributeCap = Math.max(1, plugin.getConfig().getInt("levels.max-attribute", 99));

        if (progress.level() >= levelCap) {
            return Optional.of("You reached the level cap.");
        }

        if (progress.attribute(attributeType) >= attributeCap) {
            return Optional.of(attributeType.displayName() + " reached the cap.");
        }

        int cost = LevelFormula.nextLevelRuneCost(progress.level());
        if (!runeManager.spendRunes(player.getUniqueId(), cost)) {
            return Optional.of("Not enough runes. Need: " + cost);
        }

        boolean changed = progress.increase(attributeType, attributeCap, levelCap);
        if (!changed) {
            runeManager.addRunes(player.getUniqueId(), cost);
            return Optional.of("Cannot level this attribute right now.");
        }

        applyDerivedStats(player, progress);
        savePlayer(player);
        return Optional.empty();
    }

    public void applyDerivedStats(Player player) {
        applyDerivedStats(player, getOrCreate(player));
    }

    public void savePlayer(Player player) {
        PlayerProgress progress = progressByPlayer.get(player.getUniqueId());
        if (progress == null) {
            return;
        }
        repository.savePlayerProgress(player.getUniqueId(), toData(progress));
    }

    public void setDerivedStatsPostProcessor(Consumer<Player> derivedStatsPostProcessor) {
        this.derivedStatsPostProcessor = derivedStatsPostProcessor == null ? player -> {
        } : derivedStatsPostProcessor;
    }

    public void saveAll() {
        Map<UUID, PlayerProgressData> payload = new HashMap<>();
        for (Map.Entry<UUID, PlayerProgress> entry : progressByPlayer.entrySet()) {
            PlayerProgress progress = entry.getValue();
            payload.put(entry.getKey(), toData(progress));
        }
        repository.savePlayerProgress(payload);
    }

    private void loadAll() {
        progressByPlayer.clear();
        Map<UUID, PlayerProgressData> loaded = repository.loadPlayerProgress();
        for (Map.Entry<UUID, PlayerProgressData> entry : loaded.entrySet()) {
            progressByPlayer.put(entry.getKey(), fromData(entry.getValue()));
        }
    }

    private PlayerProgress fromData(PlayerProgressData data) {
        EnumMap<AttributeType, Integer> attributes = new EnumMap<>(AttributeType.class);
        attributes.put(AttributeType.VIGOR, data.vigor());
        attributes.put(AttributeType.MIND, data.mind());
        attributes.put(AttributeType.ENDURANCE, data.endurance());
        attributes.put(AttributeType.STRENGTH, data.strength());
        attributes.put(AttributeType.DEXTERITY, data.dexterity());
        attributes.put(AttributeType.INTELLIGENCE, data.intelligence());
        attributes.put(AttributeType.FAITH, data.faith());
        attributes.put(AttributeType.ARCANE, data.arcane());
        return PlayerProgress.of(data.level(), attributes);
    }

    private PlayerProgressData toData(PlayerProgress progress) {
        return new PlayerProgressData(
                progress.level(),
                progress.attribute(AttributeType.VIGOR),
                progress.attribute(AttributeType.MIND),
                progress.attribute(AttributeType.ENDURANCE),
                progress.attribute(AttributeType.STRENGTH),
                progress.attribute(AttributeType.DEXTERITY),
                progress.attribute(AttributeType.INTELLIGENCE),
                progress.attribute(AttributeType.FAITH),
                progress.attribute(AttributeType.ARCANE)
        );
    }

    private void applyDerivedStats(Player player, PlayerProgress progress) {
        applyDerivedStats(player, progress, true);
    }

    private void applyDerivedStats(Player player, PlayerProgress progress, boolean runPostProcessor) {
        staminaManager.setMaxStamina(player, computeTargetMaxStamina(progress));
        focusManager.setMaxFocus(player, computeTargetMaxFocus(progress));

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double nextMaxHealth = VigorScaling.maxHealthForVigor(progress.attribute(AttributeType.VIGOR));
            maxHealth.setBaseValue(nextMaxHealth);
            if (player.getHealth() > nextMaxHealth) {
                player.setHealth(nextMaxHealth);
            }
        }

        applyMovementSpeedFromEndurance(player, progress.attribute(AttributeType.ENDURANCE));
        if (runPostProcessor) {
            derivedStatsPostProcessor.accept(player);
        }
    }

    private void applyMovementSpeedFromEndurance(Player player, int endurance) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        double bonus = Math.max(0, endurance - 10) * 0.0015D;
        double nextSpeed = Math.min(0.15D, 0.1D + bonus);
        movementSpeed.setBaseValue(nextSpeed);
    }

    private double computeTargetMaxStamina(PlayerProgress progress) {
        int endurance = progress.attribute(AttributeType.ENDURANCE);
        double baseStamina = staminaManager.getMaxStamina();
        double maxEnduranceBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.endurance-max-stamina-bonus", 80.0D));
        double enduranceBonus = maxEnduranceBonus * EnduranceScaling.staminaBonusRatioForEndurance(endurance);
        return baseStamina + enduranceBonus;
    }

    private double computeTargetMaxFocus(PlayerProgress progress) {
        int mind = progress.attribute(AttributeType.MIND);
        double baseFocus = Math.max(1.0D, plugin.getConfig().getDouble("focus.base", 100.0D));
        double maxMindBonus = Math.max(0.0D,
                plugin.getConfig().getDouble("levels.mind-max-focus-bonus",
                        plugin.getConfig().getDouble("levels.mind-max-stamina-bonus", 60.0D)));
        double mindBonus = maxMindBonus * MindScaling.focusBonusRatioForMind(mind);
        return baseFocus + mindBonus;
    }
}
