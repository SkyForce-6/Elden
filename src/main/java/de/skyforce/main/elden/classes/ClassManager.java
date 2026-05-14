package de.skyforce.main.elden.classes;

import de.skyforce.main.elden.level.VigorScaling;
import de.skyforce.main.elden.persistence.PlayerDataRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClassManager {

    private static final double BASE_MAX_HEALTH = 20.0D;
    private static final double BASE_MOVEMENT_SPEED = 0.1D;

    private final JavaPlugin plugin;
    private final PlayerDataRepository playerDataRepository;
    private final Map<UUID, EldenClass> classByPlayer = new HashMap<>();
    private ClassLoadoutService loadoutService;

    public ClassManager(JavaPlugin plugin, PlayerDataRepository playerDataRepository) {
        this.plugin = plugin;
        this.playerDataRepository = playerDataRepository;
        loadAll();
    }

    public void setLoadoutService(ClassLoadoutService loadoutService) {
        this.loadoutService = loadoutService;
    }

    public Optional<EldenClass> getPlayerClass(Player player) {
        return Optional.ofNullable(classByPlayer.get(player.getUniqueId()));
    }

    public Optional<EldenClass> getPlayerClass(UUID playerId) {
        return Optional.ofNullable(classByPlayer.get(playerId));
    }

    public boolean canChooseClass(Player player) {
        if (plugin.getConfig().getBoolean("classes.allow-change", true)) {
            return true;
        }
        return !classByPlayer.containsKey(player.getUniqueId());
    }

    public boolean chooseClass(Player player, EldenClass chosenClass) {
        if (!canChooseClass(player)) {
            return false;
        }

        EldenClass previous = classByPlayer.get(player.getUniqueId());
        if (previous == chosenClass) {
            return false;
        }

        classByPlayer.put(player.getUniqueId(), chosenClass);
        applyBonuses(player, chosenClass);

        if (previous == null && loadoutService != null) {
            loadoutService.grantStarterLoadout(player, chosenClass);
        }
        return true;
    }

    public boolean resetClass(Player player) {
        EldenClass removed = classByPlayer.remove(player.getUniqueId());
        resetBonuses(player);
        return removed != null;
    }

    public void assignDefaultIfMissing(Player player) {
        if (classByPlayer.containsKey(player.getUniqueId())) {
            applyBonuses(player, classByPlayer.get(player.getUniqueId()));
            return;
        }

        if (!plugin.getConfig().getBoolean("classes.auto-assign-default", false)) {
            return;
        }

        EldenClass fallback = resolveDefaultClass();
        classByPlayer.put(player.getUniqueId(), fallback);
        applyBonuses(player, fallback);
        if (loadoutService != null) {
            loadoutService.grantStarterLoadout(player, fallback);
        }
        saveAll();
        player.sendMessage("[Elden] You were assigned class: " + fallback.displayName());
    }

    public Map<UUID, EldenClass> snapshot() {
        return Collections.unmodifiableMap(classByPlayer);
    }

    public void saveAll() {
        Map<UUID, String> payload = new HashMap<>();
        for (Map.Entry<UUID, EldenClass> entry : classByPlayer.entrySet()) {
            payload.put(entry.getKey(), entry.getValue().key());
        }
        playerDataRepository.savePlayerClasses(payload);
    }

    private void loadAll() {
        classByPlayer.clear();
        for (Map.Entry<UUID, String> entry : playerDataRepository.loadPlayerClasses().entrySet()) {
            EldenClass.byKey(entry.getValue()).ifPresent(eldenClass -> classByPlayer.put(entry.getKey(), eldenClass));
        }
    }

    private EldenClass resolveDefaultClass() {
        String configured = plugin.getConfig().getString("classes.default-class", "wretch");
        return EldenClass.byKey(configured).orElse(EldenClass.WRETCH);
    }

    private void applyBonuses(Player player, EldenClass eldenClass) {
        applyMaxHealthFromVigor(player, eldenClass.vig());
        applyMovementSpeedFromEndurance(player, eldenClass.end());
    }

    private void resetBonuses(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(BASE_MAX_HEALTH);
            if (player.getHealth() > BASE_MAX_HEALTH) {
                player.setHealth(BASE_MAX_HEALTH);
            }
        }

        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(BASE_MOVEMENT_SPEED);
        }
    }

    private void applyMaxHealthFromVigor(Player player, int vigor) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double nextMaxHealth = VigorScaling.maxHealthForVigor(vigor);
        maxHealth.setBaseValue(nextMaxHealth);

        if (player.getHealth() > nextMaxHealth) {
            player.setHealth(nextMaxHealth);
        }
    }

    private void applyMovementSpeedFromEndurance(Player player, int endurance) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        double bonus = Math.max(0, endurance - 10) * 0.0015D;
        double nextSpeed = Math.min(0.15D, BASE_MOVEMENT_SPEED + bonus);
        movementSpeed.setBaseValue(nextSpeed);
    }
}
