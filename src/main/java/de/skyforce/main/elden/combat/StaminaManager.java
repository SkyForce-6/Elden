package de.skyforce.main.elden.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class StaminaManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Double> staminaByPlayer = new HashMap<>();
    private final Map<UUID, Double> maxStaminaByPlayer = new HashMap<>();
    private final Map<UUID, Long> lastSpendTickByPlayer = new HashMap<>();
    private Function<Player, Double> regenMultiplierProvider = player -> 1.0D;

    private BukkitTask regenTask;

    public StaminaManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startRegenTask() {
        stopRegenTask();

        regenTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickRegen, 1L, 1L);
    }

    public void stopRegenTask() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
    }

    public double getStamina(Player player) {
        return staminaByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> getMaxStamina(player));
    }

    public double getMaxStamina() {
        return getConfiguredBaseMaxStamina();
    }

    public double getMaxStamina(Player player) {
        return maxStaminaByPlayer.getOrDefault(player.getUniqueId(), getConfiguredBaseMaxStamina());
    }

    public void setMaxStamina(Player player, double maxStamina) {
        double previousMax = getMaxStamina(player);
        double clamped = Math.max(1.0D, maxStamina);
        maxStaminaByPlayer.put(player.getUniqueId(), clamped);

        double current = getStamina(player);
        double ratio = previousMax <= 0.0D ? 1.0D : (current / previousMax);
        double adjusted = Math.max(0.0D, Math.min(clamped, clamped * ratio));
        staminaByPlayer.put(player.getUniqueId(), adjusted);
        updateHud(player);
    }

    public void setRegenMultiplierProvider(Function<Player, Double> regenMultiplierProvider) {
        this.regenMultiplierProvider = regenMultiplierProvider == null ? player -> 1.0D : regenMultiplierProvider;
    }

    public boolean hasEnough(Player player, double cost) {
        return getStamina(player) >= Math.max(0.0D, cost);
    }

    public boolean spend(Player player, double amount) {
        double cost = Math.max(0.0D, amount);
        double current = getStamina(player);
        if (current < cost) {
            return false;
        }

        staminaByPlayer.put(player.getUniqueId(), Math.max(0.0D, current - cost));
        lastSpendTickByPlayer.put(player.getUniqueId(), (long) plugin.getServer().getCurrentTick());
        return true;
    }

    public void reset(Player player) {
        staminaByPlayer.put(player.getUniqueId(), getMaxStamina(player));
        player.setExp(1.0F);
        applyVanillaHungerOverride(player);
    }

    public void remove(Player player) {
        staminaByPlayer.remove(player.getUniqueId());
        maxStaminaByPlayer.remove(player.getUniqueId());
        lastSpendTickByPlayer.remove(player.getUniqueId());
    }

    public void updateHud(Player player) {
        double max = getMaxStamina(player);
        double current = Math.min(max, Math.max(0.0D, getStamina(player)));

        if (plugin.getConfig().getBoolean("combat.hunger.show-xp-bar", true)) {
            player.setExp((float) (current / max));
        }

        if (isVanillaHungerDisabled() && isHungerUiAsStaminaEnabled()) {
            player.setFoodLevel(toStaminaFoodLevel(player));
            player.setSaturation(0.0F);
            player.setExhaustion(0.0F);
        }
    }

    public boolean handleAttackCost(Player player) {
        if (!plugin.getConfig().getBoolean("combat.stamina.use-for-attacks", true)) {
            return true;
        }

        double attackCost = Math.max(0.0D, plugin.getConfig().getDouble("combat.stamina.attack-cost", 7.0D));
        if (attackCost <= 0.0D) {
            return true;
        }

        if (spend(player, attackCost)) {
            updateHud(player);
            return true;
        }

        return !plugin.getConfig().getBoolean("combat.stamina.block-attack-when-empty", true);
    }

    public void restore(Player player, double amount) {
        double restore = Math.max(0.0D, amount);
        if (restore <= 0.0D) {
            return;
        }

        double max = getMaxStamina(player);
        double next = Math.min(max, getStamina(player) + restore);
        staminaByPlayer.put(player.getUniqueId(), next);
        updateHud(player);
    }

    public void applyVanillaHungerOverride(Player player) {
        if (!plugin.getConfig().getBoolean("combat.hunger.disable-vanilla", true)) {
            return;
        }

        if (isHungerUiAsStaminaEnabled()) {
            player.setFoodLevel(toStaminaFoodLevel(player));
            player.setSaturation(0.0F);
            player.setExhaustion(0.0F);
            return;
        }

        int foodLevel = Math.max(1, Math.min(20, plugin.getConfig().getInt("combat.hunger.food-level", 20)));
        float saturation = (float) Math.max(0.0D, plugin.getConfig().getDouble("combat.hunger.saturation", 20.0D));

        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(0.0F);
    }

    public boolean isVanillaHungerDisabled() {
        return plugin.getConfig().getBoolean("combat.hunger.disable-vanilla", true);
    }

    public void handleFoodConsume(Player player, ItemStack consumedItem) {
        if (!isVanillaHungerDisabled()) {
            return;
        }

        applyFoodStamina(player, consumedItem);

        // Food-Level wird von Minecraft nach dem Consume gesetzt, deshalb im naechsten Tick neutralisieren.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                applyVanillaHungerOverride(player);
            }
        });
    }

    private void applyFoodStamina(Player player, ItemStack consumedItem) {
        if (!plugin.getConfig().getBoolean("combat.hunger.food-stamina.enabled", true)) {
            return;
        }
        if (consumedItem == null || !consumedItem.getType().isEdible()) {
            return;
        }

        double baseAmount = Math.max(0.0D, plugin.getConfig().getDouble("combat.hunger.food-stamina.base-amount", 8.0D));
        if (baseAmount <= 0.0D) {
            return;
        }

        double multiplier = resolveFoodMultiplier(consumedItem.getType());
        double gain = baseAmount * multiplier;
        if (gain <= 0.0D) {
            return;
        }

        double next = Math.min(getMaxStamina(player), getStamina(player) + gain);
        staminaByPlayer.put(player.getUniqueId(), next);
        updateHud(player);
    }

    private double resolveFoodMultiplier(Material material) {
        double highQualityMultiplier = Math.max(1.0D,
                plugin.getConfig().getDouble("combat.hunger.food-stamina.high-quality-multiplier", 1.5D));

        Set<String> highQualityFoods = new HashSet<>(plugin.getConfig().getStringList("combat.hunger.food-stamina.high-quality-food"));
        if (highQualityFoods.contains(material.name())) {
            return highQualityMultiplier;
        }

        return 1.0D;
    }

    private boolean isHungerUiAsStaminaEnabled() {
        return plugin.getConfig().getBoolean("combat.hunger.show-as-stamina-bar", true);
    }

    private int toStaminaFoodLevel(Player player) {
        double max = getMaxStamina(player);
        if (max <= 0.0D) {
            return 20;
        }

        double current = Math.max(0.0D, Math.min(max, getStamina(player)));
        int mapped = (int) Math.round((current / max) * 20.0D);
        return Math.max(1, Math.min(20, mapped));
    }

    private void applySprintDrain(Player player) {
        if (!plugin.getConfig().getBoolean("combat.hunger.use-stamina-for-sprint", true)) {
            return;
        }
        if (!player.isSprinting() || player.isFlying() || player.isInsideVehicle()) {
            return;
        }

        double sprintCostPerSecond = Math.max(0.0D, plugin.getConfig().getDouble("combat.hunger.sprint-cost-per-second", 10.0D));
        if (sprintCostPerSecond <= 0.0D) {
            return;
        }

        double costPerTick = sprintCostPerSecond / 20.0D;
        if (!spend(player, costPerTick)) {
            player.setSprinting(false);
        }
    }

    private void tickRegen() {
        double regenPerSecond = Math.max(0.0D, plugin.getConfig().getDouble("combat.stamina.regen-per-second", 20.0D));
        double regenPerTick = regenPerSecond / 20.0D;
        int regenDelayTicks = Math.max(0, plugin.getConfig().getInt("combat.stamina.regen-delay-ticks", 30));
        long now = plugin.getServer().getCurrentTick();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            double max = getMaxStamina(player);

            applyVanillaHungerOverride(player);
            applySprintDrain(player);

            double current = getStamina(player);
            long lastSpend = lastSpendTickByPlayer.getOrDefault(playerId, Long.MIN_VALUE);

            if (now - lastSpend < regenDelayTicks) {
                updateHud(player);
                continue;
            }

            double multiplier = Math.max(0.0D, regenMultiplierProvider.apply(player));
            double next = Math.min(max, current + (regenPerTick * multiplier));
            staminaByPlayer.put(playerId, next);
            updateHud(player);
        }
    }

    private double getConfiguredBaseMaxStamina() {
        return Math.max(1.0D, plugin.getConfig().getDouble("combat.stamina.max", 100.0D));
    }
}
