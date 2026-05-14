package de.skyforce.main.elden.boss;

import de.skyforce.main.elden.boss.model.BossDefinition;
import de.skyforce.main.elden.boss.registry.BossRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class BossPortalManager {

    private static final double DEFAULT_WIDTH = 2.6D;
    private static final double DEFAULT_HEIGHT = 3.4D;
    private static final double DEFAULT_THICKNESS = 0.9D;
    private static final long ACTIVATION_DELAY_TICKS = 12L;
    private static final double EXIT_PLAYER_OFFSET = 1.65D;
    private static final double BOSS_SPAWN_OFFSET = 5.0D;
    private static final String EXIT_LOCK_CONFIG_PATH = "bosses.portals-defaults.require-boss-death-to-exit";

    private final JavaPlugin plugin;
    private final BossRegistry bossRegistry;
    private final BossManager bossManager;
    private final Map<String, BossPortal> portals = new HashMap<>();
    private final Map<UUID, Long> playerTriggerCooldowns = new HashMap<>();
    private final BukkitTask renderTask;

    public BossPortalManager(JavaPlugin plugin, BossRegistry bossRegistry, BossManager bossManager) {
        this.plugin = plugin;
        this.bossRegistry = bossRegistry;
        this.bossManager = bossManager;
        loadConfiguredPortals();
        this.renderTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickPortals, 10L, 10L);
    }

    public boolean savePortalEntrance(String portalName, Location portalLocation) {
        if (portalName == null || portalName.isBlank() || portalLocation == null || portalLocation.getWorld() == null) {
            return false;
        }

        String normalizedName = normalizeName(portalName);
        BossPortal portal = getOrCreatePortal(normalizedName);
        Location center = portalLocation.clone();
        center.setX(Math.floor(center.getX()) + 0.5D);
        center.setZ(Math.floor(center.getZ()) + 0.5D);

        portal.portalLocation = center;
        portal.width = DEFAULT_WIDTH;
        portal.height = DEFAULT_HEIGHT;

        String basePath = basePath(normalizedName);
        plugin.getConfig().set("bosses.enabled", true);
        plugin.getConfig().set(basePath + ".entrance.world", center.getWorld().getName());
        plugin.getConfig().set(basePath + ".entrance.x", center.getX());
        plugin.getConfig().set(basePath + ".entrance.y", center.getY());
        plugin.getConfig().set(basePath + ".entrance.z", center.getZ());
        plugin.getConfig().set(basePath + ".entrance.yaw", center.getYaw());
        plugin.getConfig().set(basePath + ".entrance.pitch", center.getPitch());
        plugin.getConfig().set(basePath + ".width", DEFAULT_WIDTH);
        plugin.getConfig().set(basePath + ".height", DEFAULT_HEIGHT);
        plugin.saveConfig();
        return true;
    }

    public boolean savePortalExit(String portalName, Location exitLocation) {
        if (portalName == null || portalName.isBlank() || exitLocation == null || exitLocation.getWorld() == null) {
            return false;
        }

        String normalizedName = normalizeName(portalName);
        BossPortal portal = getOrCreatePortal(normalizedName);
        Location center = exitLocation.clone();
        center.setX(Math.floor(center.getX()) + 0.5D);
        center.setZ(Math.floor(center.getZ()) + 0.5D);
        portal.exitLocation = center;

        String basePath = basePath(normalizedName);
        plugin.getConfig().set("bosses.enabled", true);
        plugin.getConfig().set(basePath + ".exit.world", center.getWorld().getName());
        plugin.getConfig().set(basePath + ".exit.x", center.getX());
        plugin.getConfig().set(basePath + ".exit.y", center.getY());
        plugin.getConfig().set(basePath + ".exit.z", center.getZ());
        plugin.getConfig().set(basePath + ".exit.yaw", center.getYaw());
        plugin.getConfig().set(basePath + ".exit.pitch", center.getPitch());
        plugin.getConfig().set(basePath + ".spawn", null);
        plugin.saveConfig();
        return true;
    }

    public boolean assignBoss(String portalName, BossDefinition definition) {
        if (portalName == null || portalName.isBlank() || definition == null) {
            return false;
        }

        String normalizedName = normalizeName(portalName);
        BossPortal portal = getOrCreatePortal(normalizedName);
        portal.bossId = definition.id();

        plugin.getConfig().set("bosses.enabled", true);
        plugin.getConfig().set(basePath(normalizedName) + ".boss-id", definition.id());
        plugin.saveConfig();
        return true;
    }

    public boolean setPortalExitLock(String portalName, boolean requireBossDeathToExit) {
        if (portalName == null || portalName.isBlank()) {
            return false;
        }

        String normalizedName = normalizeName(portalName);
        BossPortal portal = getOrCreatePortal(normalizedName);
        portal.requireBossDeathToExit = requireBossDeathToExit;

        plugin.getConfig().set("bosses.enabled", true);
        plugin.getConfig().set(basePath(normalizedName) + ".require-boss-death-to-exit", requireBossDeathToExit);
        plugin.saveConfig();
        return true;
    }

    public boolean removePortal(String portalName) {
        if (portalName == null || portalName.isBlank()) {
            return false;
        }

        String normalizedName = normalizeName(portalName);
        String basePath = basePath(normalizedName);
        if (!plugin.getConfig().contains(basePath)) {
            return false;
        }
        plugin.getConfig().set(basePath, null);
        plugin.saveConfig();
        portals.remove(normalizedName);
        return true;
    }

    public Map<String, String> getConfiguredPortals() {
        Map<String, String> configured = new HashMap<>();
        for (Map.Entry<String, BossPortal> entry : portals.entrySet()) {
            configured.put(entry.getKey(), entry.getValue().describeState());
        }
        return configured;
    }

    public Optional<PortalInfo> getPortalInfo(String portalName) {
        if (portalName == null || portalName.isBlank()) {
            return Optional.empty();
        }

        BossPortal portal = portals.get(normalizeName(portalName));
        if (portal == null) {
            return Optional.empty();
        }
        return Optional.of(new PortalInfo(
                normalizeName(portalName),
                portal.bossId,
                portal.portalLocation == null ? null : portal.portalLocation.clone(),
                portal.exitLocation == null ? null : portal.exitLocation.clone(),
                portal.isReady(),
                portal.requireBossDeathToExit
        ));
    }

    public void handlePlayerMove(Player player, Location from, Location to) {
        if (player == null || from == null || to == null || to.getWorld() == null) {
            return;
        }

        long now = plugin.getServer().getCurrentTick();
        playerTriggerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (BossPortal portal : portals.values()) {
            if (!portal.isReady()) {
                continue;
            }

            long cooldownUntil = playerTriggerCooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (cooldownUntil > now) {
                return;
            }

            if (portal.enteredEntrance(from, to)) {
                triggerPortal(player, portal, now);
                return;
            }
            if (portal.enteredExit(from, to)) {
                returnFromExitPortal(player, portal, now);
                return;
            }
        }
    }

    public void handlePlayerDeath(Player player) {
        if (player == null || player.getWorld() == null) {
            return;
        }

        for (Map.Entry<String, BossPortal> entry : portals.entrySet()) {
            BossPortal portal = entry.getValue();
            updateActiveBossReference(portal);
            if (portal.activeBossId == null) {
                continue;
            }

            Entity activeBoss = Bukkit.getEntity(portal.activeBossId);
            if (activeBoss == null || !activeBoss.isValid() || activeBoss.getWorld() == null) {
                portal.activeBossId = null;
                continue;
            }

            BossDefinition definition = bossRegistry.getById(portal.bossId).orElse(null);
            if (definition == null || !activeBoss.getWorld().equals(player.getWorld())) {
                continue;
            }

            double arenaRadius = definition.arenaRadius();
            double distanceSquared = player.getLocation().distanceSquared(activeBoss.getLocation());
            if (distanceSquared > arenaRadius * arenaRadius) {
                continue;
            }

            if (bossManager.despawnBoss(portal.activeBossId, false)) {
                portal.activeBossId = null;
                portal.activationPendingUntilTick = 0L;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.MASTER, 0.8F, 0.8F);
            }
            return;
        }
    }

    public void shutdown() {
        renderTask.cancel();
        portals.clear();
        playerTriggerCooldowns.clear();
    }

    private void loadConfiguredPortals() {
        portals.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bosses.portals");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String normalizedName = normalizeName(key);
            String basePath = basePath(normalizedName);
            BossPortal portal = new BossPortal();
            portal.bossId = plugin.getConfig().getString(basePath + ".boss-id");
            portal.width = plugin.getConfig().getDouble(basePath + ".width", DEFAULT_WIDTH);
            portal.height = plugin.getConfig().getDouble(basePath + ".height", DEFAULT_HEIGHT);
            portal.portalLocation = readLocation(basePath + ".entrance");
            portal.exitLocation = readLocation(basePath + ".exit");
            portal.requireBossDeathToExit = plugin.getConfig().getBoolean(
                    basePath + ".require-boss-death-to-exit",
                    defaultRequireBossDeathToExit()
            );
            if (portal.exitLocation == null) {
                // Backward compatibility for older configs that stored the target under "spawn".
                portal.exitLocation = readLocation(basePath + ".spawn");
            }

            if (portal.bossId != null && bossRegistry.getById(portal.bossId).isEmpty()) {
                plugin.getLogger().warning("Boss portal '" + key + "' loaded without a valid boss assignment.");
                portal.bossId = null;
            }

            if (portal.portalLocation == null && portal.exitLocation == null && portal.bossId == null) {
                continue;
            }
            portals.put(normalizedName, portal);
        }
    }

    private Location readLocation(String pathPrefix) {
        String worldName = plugin.getConfig().getString(pathPrefix + ".world");
        if (worldName == null) {
            return null;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                plugin.getConfig().getDouble(pathPrefix + ".x"),
                plugin.getConfig().getDouble(pathPrefix + ".y"),
                plugin.getConfig().getDouble(pathPrefix + ".z"),
                (float) plugin.getConfig().getDouble(pathPrefix + ".yaw"),
                (float) plugin.getConfig().getDouble(pathPrefix + ".pitch")
        );
    }

    private void tickPortals() {
        for (BossPortal portal : portals.values()) {
            updateActiveBossReference(portal);
            if (portal.portalLocation != null || portal.exitLocation != null) {
                renderPortal(portal);
            }
        }
    }

    private void updateActiveBossReference(BossPortal portal) {
        if (portal.activationPendingUntilTick > 0L && portal.activationPendingUntilTick <= plugin.getServer().getCurrentTick()) {
            portal.activationPendingUntilTick = 0L;
        }
        if (portal.activeBossId == null) {
            return;
        }
        Entity activeEntity = Bukkit.getEntity(portal.activeBossId);
        if (activeEntity == null || !activeEntity.isValid() || activeEntity.isDead()) {
            portal.activeBossId = null;
        }
    }

    private void triggerPortal(Player player, BossPortal portal, long now) {
        updateActiveBossReference(portal);
        if (portal.activeBossId != null || portal.activationPendingUntilTick > now) {
            player.sendMessage(Component.text("This boss portal is already active.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 0.8F, 0.6F);
            playerTriggerCooldowns.put(player.getUniqueId(), now + 20L);
            return;
        }

        BossDefinition definition = bossRegistry.getById(portal.bossId).orElse(null);
        if (definition == null || portal.exitLocation == null) {
            player.sendMessage(Component.text("This boss portal is not fully configured yet.", NamedTextColor.RED));
            playerTriggerCooldowns.put(player.getUniqueId(), now + 20L);
            return;
        }

        World world = portal.portalLocation.getWorld();
        if (world != null) {
            playActivationEffects(world, portal, player);
        }
        teleportPlayerToExit(player, portal);

        portal.activationPendingUntilTick = now + ACTIVATION_DELAY_TICKS;
        playerTriggerCooldowns.put(player.getUniqueId(), now + 40L);
        player.sendMessage(Component.text("Boss portal activated: " + definition.displayName(), NamedTextColor.GOLD));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            portal.activationPendingUntilTick = 0L;
            updateActiveBossReference(portal);
            if (portal.activeBossId != null || portal.exitLocation == null) {
                return;
            }

            BossDefinition assignedDefinition = bossRegistry.getById(portal.bossId).orElse(null);
            if (assignedDefinition == null) {
                return;
            }

            UUID spawnedBossId = bossManager.spawnBossTracked(assignedDefinition, bossSpawnLocation(portal));
            if (spawnedBossId == null) {
                player.sendMessage(Component.text("The boss could not be spawned from this portal.", NamedTextColor.RED));
                return;
            }
            portal.activeBossId = spawnedBossId;
        }, ACTIVATION_DELAY_TICKS);
    }

    private void playActivationEffects(World world, BossPortal portal, Player player) {
        Location center = portal.portalLocation.clone().add(0.0D, portal.height / 2.0D, 0.0D);
        Vector pull = center.toVector().subtract(player.getLocation().toVector());
        if (pull.lengthSquared() > 0.01D) {
            pull.normalize().multiply(0.65D).setY(Math.max(0.18D, pull.getY() + 0.18D));
            player.setVelocity(pull);
        }

        world.playSound(portal.portalLocation, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 0.9F, 1.15F);
        world.playSound(portal.portalLocation, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 0.7F, 0.8F);
        world.spawnParticle(Particle.END_ROD, portal.portalLocation.clone().add(0.0D, 1.2D, 0.0D), 40, 0.7D, 1.1D, 0.7D, 0.03D);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, portal.portalLocation.clone().add(0.0D, 1.2D, 0.0D), 18, 0.45D, 0.9D, 0.45D, 0.02D);
        world.spawnParticle(Particle.PORTAL, center, 60, 0.8D, 1.2D, 0.8D, 0.35D);
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 45, 0.55D, 1.0D, 0.55D, 0.1D);
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2.0D / 3.0D) * i;
            Location swirlPoint = center.clone().add(Math.cos(angle) * 0.9D, -0.6D, Math.sin(angle) * 0.9D);
            Vector towardsCenter = center.toVector().subtract(swirlPoint.toVector()).normalize();
            world.spawnParticle(Particle.WITCH, swirlPoint, 8, towardsCenter.getX() * 0.12D, 0.25D, towardsCenter.getZ() * 0.12D, 0.01D);
        }
    }

    private void teleportPlayerToExit(Player player, BossPortal portal) {
        if (portal.exitLocation == null) {
            return;
        }
        Location exitTarget = portal.exitLocation.clone().add(exitForward(portal).multiply(EXIT_PLAYER_OFFSET));
        exitTarget.setPitch(0.0F);
        player.teleport(exitTarget);

        playArrivalEffects(exitTarget, portal.exitLocation, portal.height);
    }

    private void returnFromExitPortal(Player player, BossPortal portal, long now) {
        if (portal.portalLocation == null) {
            return;
        }
        updateActiveBossReference(portal);
        if (portal.requireBossDeathToExit && (portal.activeBossId != null || portal.activationPendingUntilTick > now)) {
            playerTriggerCooldowns.put(player.getUniqueId(), now + 20L);
            player.sendMessage(Component.text("You must defeat the boss before you can leave through this portal.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 0.8F, 0.6F);
            return;
        }
        if (portal.activeBossId != null && bossManager.despawnBoss(portal.activeBossId, false)) {
            portal.activeBossId = null;
            portal.activationPendingUntilTick = 0L;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.MASTER, 0.8F, 0.85F);
        }
        Location entranceTarget = portal.portalLocation.clone().add(entranceForward(portal).multiply(EXIT_PLAYER_OFFSET));
        entranceTarget.setPitch(0.0F);
        player.teleport(entranceTarget);
        playerTriggerCooldowns.put(player.getUniqueId(), now + 40L);
        playArrivalEffects(entranceTarget, portal.portalLocation, portal.height);
        player.sendMessage(Component.text("You left the boss portal.", NamedTextColor.GRAY));
    }

    private void playArrivalEffects(Location target, Location portalCenter, double portalHeight) {
        World world = target.getWorld();
        if (world == null || portalCenter == null) {
            return;
        }
        Location center = portalCenter.clone().add(0.0D, portalHeight / 2.0D, 0.0D);
        world.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 0.8F, 1.2F);
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 45, 0.55D, 1.0D, 0.55D, 0.08D);
        world.spawnParticle(Particle.PORTAL, center, 60, 0.75D, 1.2D, 0.75D, 0.2D);
    }

    private void renderPortal(BossPortal portal) {
        if (portal.portalLocation != null) {
            renderPortalFrame(portal.portalLocation, portal.width, portal.height);
        }
        if (portal.exitLocation != null) {
            renderPortalFrame(portal.exitLocation, portal.width, portal.height);
        }
    }

    private void renderPortalFrame(Location portalLocation, double width, double height) {
        World world = portalLocation.getWorld();
        if (world == null) {
            return;
        }

        Vector forward = horizontalDirection(portalLocation.getYaw());
        Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX());
        Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 202, 54), 1.6F);
        Particle.DustOptions orangeDust = new Particle.DustOptions(Color.fromRGB(255, 132, 32), 1.0F);

        for (int yStep = 0; yStep <= 6; yStep++) {
            double yOffset = (height / 6.0D) * yStep;
            for (int xStep = -2; xStep <= 2; xStep++) {
                double xOffset = (width / 4.0D) * xStep;
                Location point = portalLocation.clone()
                        .add(right.clone().multiply(xOffset))
                        .add(0.0D, yOffset, 0.0D);
                world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, goldDust);
                if (xStep != -2 && xStep != 2 && yStep != 0 && yStep != 6) {
                    world.spawnParticle(Particle.DUST, point.clone().add(forward.clone().multiply(0.08D)), 1, 0.0D, 0.0D, 0.0D, 0.0D, orangeDust);
                }
            }
        }

        world.spawnParticle(Particle.END_ROD, portalLocation.clone().add(0.0D, height / 2.0D, 0.0D), 2, 0.4D, 0.8D, 0.4D, 0.01D);
    }

    private Vector horizontalDirection(float yaw) {
        double radians = Math.toRadians(yaw);
        Vector direction = new Vector(-Math.sin(radians), 0.0D, Math.cos(radians));
        if (direction.lengthSquared() < 1.0E-6D) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }

    private BossPortal getOrCreatePortal(String normalizedName) {
        return portals.computeIfAbsent(normalizedName, ignored -> new BossPortal());
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase().replace(' ', '_');
    }

    private String basePath(String normalizedName) {
        return "bosses.portals." + normalizedName;
    }

    private boolean defaultRequireBossDeathToExit() {
        return plugin.getConfig().getBoolean(EXIT_LOCK_CONFIG_PATH, false);
    }

    private Vector exitForward(BossPortal portal) {
        if (portal.exitLocation == null) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return horizontalDirection(portal.exitLocation.getYaw());
    }

    private Vector entranceForward(BossPortal portal) {
        if (portal.portalLocation == null) {
            return new Vector(0.0D, 0.0D, 1.0D);
        }
        return horizontalDirection(portal.portalLocation.getYaw());
    }

    private Location bossSpawnLocation(BossPortal portal) {
        Location spawnBase = portal.exitLocation.clone().add(exitForward(portal).multiply(BOSS_SPAWN_OFFSET));
        spawnBase.setPitch(0.0F);
        return spawnBase;
    }

    private static final class BossPortal {
        private String bossId;
        private Location portalLocation;
        private Location exitLocation;
        private double width = DEFAULT_WIDTH;
        private double height = DEFAULT_HEIGHT;
        private boolean requireBossDeathToExit;
        private UUID activeBossId;
        private long activationPendingUntilTick;

        private boolean isReady() {
            return bossId != null && portalLocation != null && exitLocation != null;
        }

        private String describeState() {
            return "boss=" + (bossId == null ? "unset" : bossId)
                    + ", entrance=" + (portalLocation == null ? "unset" : "set")
                    + ", exit=" + (exitLocation == null ? "unset" : "set")
                    + ", exit-locked=" + (requireBossDeathToExit ? "yes" : "no");
        }

        private boolean enteredEntrance(Location from, Location to) {
            return enteredPortal(portalLocation, from, to);
        }

        private boolean enteredExit(Location from, Location to) {
            return enteredPortal(exitLocation, from, to);
        }

        private boolean enteredPortal(Location portalCenter, Location from, Location to) {
            return !contains(portalCenter, from) && contains(portalCenter, to);
        }

        private boolean contains(Location portalCenter, Location location) {
            if (portalCenter == null || location == null || location.getWorld() == null || !location.getWorld().equals(portalCenter.getWorld())) {
                return false;
            }

            Vector relative = location.toVector().subtract(portalCenter.toVector());
            Vector forward = new Vector(-Math.sin(Math.toRadians(portalCenter.getYaw())), 0.0D, Math.cos(Math.toRadians(portalCenter.getYaw()))).normalize();
            Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX());
            double depth = relative.dot(forward);
            double side = relative.dot(right);
            return Math.abs(depth) <= DEFAULT_THICKNESS
                    && Math.abs(side) <= width / 2.0D
                    && relative.getY() >= -0.35D
                    && relative.getY() <= height;
        }
    }

    public record PortalInfo(String name, String bossId, Location entranceLocation, Location exitLocation, boolean ready,
                             boolean requireBossDeathToExit) {
    }
}
