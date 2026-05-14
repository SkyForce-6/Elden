package de.skyforce.main.elden.talisman.service;

import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.level.AttributeType;
import de.skyforce.main.elden.level.EnduranceScaling;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.level.MindScaling;
import de.skyforce.main.elden.level.PlayerProgress;
import de.skyforce.main.elden.level.VigorScaling;
import de.skyforce.main.elden.persistence.PlayerDataRepository;
import de.skyforce.main.elden.talisman.model.TalismanDefinition;
import de.skyforce.main.elden.talisman.model.TalismanEffectType;
import de.skyforce.main.elden.talisman.registry.TalismanRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TalismanManager {

    public static final int SLOT_COUNT = 4;

    private final JavaPlugin plugin;
    private final PlayerDataRepository repository;
    private final TalismanRegistry talismanRegistry;
    private final LevelManager levelManager;
    private final StaminaManager staminaManager;
    private final FocusManager focusManager;
    private final Map<UUID, List<String>> equippedByPlayer = new HashMap<>();

    public TalismanManager(JavaPlugin plugin, PlayerDataRepository repository, TalismanRegistry talismanRegistry,
                           LevelManager levelManager, StaminaManager staminaManager, FocusManager focusManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.talismanRegistry = talismanRegistry;
        this.levelManager = levelManager;
        this.staminaManager = staminaManager;
        this.focusManager = focusManager;
        equippedByPlayer.putAll(repository.loadEquippedTalismans());
    }

    public List<String> equippedIds(Player player) {
        return new ArrayList<>(slots(player.getUniqueId()));
    }

    public Optional<TalismanDefinition> equipped(Player player, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return Optional.empty();
        }
        String id = slots(player.getUniqueId()).get(slot);
        return id == null || id.isBlank() ? Optional.empty() : talismanRegistry.getById(id);
    }

    public boolean equip(Player player, int slot, String talismanId) {
        if (player == null || slot < 0 || slot >= SLOT_COUNT || talismanId == null || talismanId.isBlank()) {
            return false;
        }
        TalismanDefinition talisman = talismanRegistry.getById(talismanId).orElse(null);
        if (talisman == null) {
            return false;
        }
        List<String> slots = slots(player.getUniqueId());
        String normalizedId = talisman.id().toLowerCase(Locale.ROOT);
        for (int i = 0; i < slots.size(); i++) {
            if (i != slot && normalizedId.equals(slots.get(i))) {
                return false;
            }
        }
        slots.set(slot, normalizedId);
        repository.saveEquippedTalisman(player.getUniqueId(), slot, normalizedId);
        applyPassiveStats(player);
        return true;
    }

    public boolean unequip(Player player, int slot) {
        if (player == null || slot < 0 || slot >= SLOT_COUNT) {
            return false;
        }
        List<String> slots = slots(player.getUniqueId());
        if (slots.get(slot) == null) {
            return false;
        }
        slots.set(slot, null);
        repository.saveEquippedTalisman(player.getUniqueId(), slot, null);
        applyPassiveStats(player);
        return true;
    }

    public void applyPassiveStats(Player player) {
        PlayerProgress progress = levelManager.getOrCreate(player);
        double health = VigorScaling.maxHealthForVigor(progress.attribute(AttributeType.VIGOR)) * healthMultiplier(player);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.max(1.0D, health));
            if (player.getHealth() > maxHealth.getBaseValue()) {
                player.setHealth(maxHealth.getBaseValue());
            }
        }

        double baseStamina = plugin.getConfig().getDouble("combat.stamina.max", 100.0D);
        double enduranceBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.endurance-max-stamina-bonus", 80.0D))
                * EnduranceScaling.staminaBonusRatioForEndurance(progress.attribute(AttributeType.ENDURANCE));
        staminaManager.setMaxStamina(player, (baseStamina + enduranceBonus) * staminaMultiplier(player));

        double baseFocus = Math.max(1.0D, plugin.getConfig().getDouble("focus.base", 100.0D));
        double mindBonus = Math.max(0.0D, plugin.getConfig().getDouble("levels.mind-max-focus-bonus",
                plugin.getConfig().getDouble("levels.mind-max-stamina-bonus", 60.0D)))
                * MindScaling.focusBonusRatioForMind(progress.attribute(AttributeType.MIND));
        focusManager.setMaxFocus(player, (baseFocus + mindBonus) * focusMultiplier(player));
    }

    public double healthMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.MAX_HEALTH) + erdtreeSharedBonus(player);
    }

    public double staminaMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.MAX_STAMINA) + erdtreeSharedBonus(player);
    }

    public double focusMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.MAX_FOCUS);
    }

    public double equipLoadMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.EQUIP_LOAD);
    }

    public double staminaRegenMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.STAMINA_REGEN);
    }

    public double physicalDefenseMultiplier(Player player) {
        return 1.0D - Math.min(0.85D, bonus(player, TalismanEffectType.PHYSICAL_DEFENSE));
    }

    public double jumpAttackDamageMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.JUMP_ATTACK_DAMAGE);
    }

    public double runeGainMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.RUNE_GAIN);
    }

    public double castSpeedMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.CAST_SPEED);
    }

    public double statusBuildupMultiplier(Player player) {
        return 1.0D + bonus(player, TalismanEffectType.STATUS_BUILDUP);
    }

    private double erdtreeSharedBonus(Player player) {
        return equippedDefinitions(player).stream()
                .filter(talisman -> talisman.id().equals("erdtrees_favor"))
                .mapToDouble(TalismanDefinition::value)
                .sum();
    }

    private double bonus(Player player, TalismanEffectType effectType) {
        return equippedDefinitions(player).stream()
                .filter(talisman -> talisman.effectType() == effectType)
                .mapToDouble(TalismanDefinition::value)
                .sum();
    }

    private List<TalismanDefinition> equippedDefinitions(Player player) {
        List<TalismanDefinition> result = new ArrayList<>();
        for (String id : slots(player.getUniqueId())) {
            if (id == null || id.isBlank()) {
                continue;
            }
            talismanRegistry.getById(id).ifPresent(result::add);
        }
        return result;
    }

    private List<String> slots(UUID playerId) {
        return equippedByPlayer.computeIfAbsent(playerId, ignored -> {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < SLOT_COUNT; i++) {
                list.add(null);
            }
            return list;
        });
    }
}
