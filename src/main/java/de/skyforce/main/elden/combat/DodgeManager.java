package de.skyforce.main.elden.combat;

import de.skyforce.main.elden.equipment.EquipmentWeightService;
import de.skyforce.main.elden.equipment.EquipmentWeightSnapshot;
import de.skyforce.main.elden.equipment.WeightTier;
import de.skyforce.main.elden.visual.VisualEffectService;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class DodgeManager {

    private final JavaPlugin plugin;
    private final EquipmentWeightService equipmentWeightService;
    private final VisualEffectService visualEffectService;
    private final Map<UUID, Long> lastDodgeTickByPlayer = new HashMap<>();
    private final Map<UUID, Long> iframeUntilTickByPlayer = new HashMap<>();

    public DodgeManager(JavaPlugin plugin, EquipmentWeightService equipmentWeightService, VisualEffectService visualEffectService) {
        this.plugin = plugin;
        this.equipmentWeightService = equipmentWeightService;
        this.visualEffectService = visualEffectService;
    }

    public boolean attemptDodge(Player player, StaminaManager staminaManager) {
        if (!plugin.getConfig().getBoolean("combat.enabled", true)) {
            return false;
        }
        if (!player.hasPermission("elden.combat.use")) {
            return false;
        }

        EquipmentWeightSnapshot weightSnapshot = equipmentWeightService.snapshot(player);
        WeightTier tier = weightSnapshot.tier();
        if (tier == WeightTier.OVERLOADED) {
            player.sendActionBar(Component.text("Overloaded - cannot dodge", NamedTextColor.RED));
            return false;
        }

        long now = plugin.getServer().getCurrentTick();
        int cooldownTicks = Math.max(0, plugin.getConfig().getInt("combat.dodge.cooldown-ticks", 14)
                + equipmentWeightService.dodgeCooldownBonusTicks(tier));
        long last = lastDodgeTickByPlayer.getOrDefault(player.getUniqueId(), Long.MIN_VALUE / 2);
        long remaining = cooldownTicks - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("Dodge Cooldown: " + remaining + "t", NamedTextColor.GRAY));
            return false;
        }

        double staminaCost = Math.max(0.0D, plugin.getConfig().getDouble("combat.dodge.cost", 35.0D)
                * equipmentWeightService.dodgeCostMultiplier(tier));
        if (!staminaManager.spend(player, staminaCost)) {
            player.sendActionBar(Component.text("Not enough stamina", NamedTextColor.RED));
            return false;
        }

        double horizontal = Math.max(0.0D, plugin.getConfig().getDouble("combat.dodge.horizontal-velocity", 1.1D)
                * equipmentWeightService.dodgeHorizontalMultiplier(tier));
        double vertical = plugin.getConfig().getDouble("combat.dodge.vertical-velocity", 0.12D)
                * equipmentWeightService.dodgeVerticalMultiplier(tier);

        Vector direction = player.getLocation().getDirection();
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.0001D) {
            direction = new Vector(0, 0, 1);
        }

        Vector velocity = direction.normalize().multiply(horizontal);
        velocity.setY(vertical);
        player.setVelocity(velocity);

        visualEffectService.playDodgeTrail(player, direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9F, 1.2F);

        lastDodgeTickByPlayer.put(player.getUniqueId(), now);

        if (plugin.getConfig().getBoolean("combat.iframes.enabled", true)) {
            int iframeDuration = Math.max(1, plugin.getConfig().getInt("combat.iframes.duration-ticks", 8)
                    + equipmentWeightService.dodgeIframeBonusTicks(tier));
            iframeUntilTickByPlayer.put(player.getUniqueId(), now + iframeDuration);
        }

        staminaManager.updateHud(player);
        return true;
    }

    public boolean hasIframe(Player player, EntityDamageEvent.DamageCause cause) {
        if (!plugin.getConfig().getBoolean("combat.iframes.enabled", true)) {
            return false;
        }

        long now = plugin.getServer().getCurrentTick();
        long until = iframeUntilTickByPlayer.getOrDefault(player.getUniqueId(), Long.MIN_VALUE);
        if (now > until) {
            return false;
        }

        Set<EntityDamageEvent.DamageCause> blockedCauses = resolveBlockedCauses();
        return blockedCauses.contains(cause);
    }

    public void clear(Player player) {
        lastDodgeTickByPlayer.remove(player.getUniqueId());
        iframeUntilTickByPlayer.remove(player.getUniqueId());
    }

    private Set<EntityDamageEvent.DamageCause> resolveBlockedCauses() {
        Set<String> configured = Set.copyOf(plugin.getConfig().getStringList("combat.iframes.blocked-causes"));
        if (configured.isEmpty()) {
            return EnumSet.of(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.PROJECTILE);
        }

        EnumSet<EntityDamageEvent.DamageCause> causes = EnumSet.noneOf(EntityDamageEvent.DamageCause.class);
        for (String name : configured) {
            try {
                causes.add(EntityDamageEvent.DamageCause.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown DamageCause in combat.iframes.blocked-causes: " + name);
            }
        }

        if (causes.isEmpty()) {
            causes.addAll(Arrays.asList(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.PROJECTILE));
        }

        return causes;
    }
}
