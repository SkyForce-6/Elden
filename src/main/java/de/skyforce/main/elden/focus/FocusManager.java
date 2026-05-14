package de.skyforce.main.elden.focus;

import de.skyforce.main.elden.persistence.PlayerDataRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class FocusManager {

    private final PlayerDataRepository repository;
    private final Map<UUID, Double> focusByPlayer = new HashMap<>();
    private final Map<UUID, Double> maxFocusByPlayer = new HashMap<>();

    public FocusManager(PlayerDataRepository repository) {
        this.repository = repository;
        focusByPlayer.putAll(repository.loadPlayerFocus());
    }

    public double getFocus(Player player) {
        return focusByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> getMaxFocus(player));
    }

    public double getMaxFocus() {
        return 100.0D;
    }

    public double getMaxFocus(Player player) {
        return maxFocusByPlayer.getOrDefault(player.getUniqueId(), getMaxFocus());
    }

    public void setMaxFocus(Player player, double maxFocus) {
        double clamped = Math.max(1.0D, maxFocus);
        maxFocusByPlayer.put(player.getUniqueId(), clamped);

        if (focusByPlayer.containsKey(player.getUniqueId())) {
            double adjusted = Math.max(0.0D, Math.min(clamped, focusByPlayer.get(player.getUniqueId())));
            focusByPlayer.put(player.getUniqueId(), adjusted);
            savePlayer(player);
            return;
        }

        focusByPlayer.put(player.getUniqueId(), clamped);
        savePlayer(player);
    }

    public void setFocus(Player player, double focus) {
        double clamped = Math.max(0.0D, Math.min(getMaxFocus(player), focus));
        focusByPlayer.put(player.getUniqueId(), clamped);
        savePlayer(player);
    }

    public boolean hasEnough(Player player, double cost) {
        return getFocus(player) >= Math.max(0.0D, cost);
    }

    public boolean spend(Player player, double amount) {
        double cost = Math.max(0.0D, amount);
        double current = getFocus(player);
        if (current < cost) {
            return false;
        }

        focusByPlayer.put(player.getUniqueId(), Math.max(0.0D, current - cost));
        savePlayer(player);
        return true;
    }

    public void restore(Player player, double amount) {
        double restore = Math.max(0.0D, amount);
        if (restore <= 0.0D) {
            return;
        }

        double next = Math.min(getMaxFocus(player), getFocus(player) + restore);
        focusByPlayer.put(player.getUniqueId(), next);
        savePlayer(player);
    }

    public void reset(Player player) {
        focusByPlayer.put(player.getUniqueId(), getMaxFocus(player));
        savePlayer(player);
    }

    public void savePlayer(Player player) {
        repository.savePlayerFocus(player.getUniqueId(), getFocus(player));
    }

    public void saveAll() {
        repository.savePlayerFocus(focusByPlayer);
    }

    public void remove(Player player) {
        maxFocusByPlayer.remove(player.getUniqueId());
    }
}