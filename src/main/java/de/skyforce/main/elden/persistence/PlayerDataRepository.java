package de.skyforce.main.elden.persistence;

import de.skyforce.main.elden.level.PlayerProgressData;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerDataRepository {

    private final JavaPlugin plugin;
    private final File databaseFile;
    private final File migrationFlagFile;
    private Connection connection;

    public PlayerDataRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "player-data.db");
        this.migrationFlagFile = new File(plugin.getDataFolder(), "sqlite-migrated.flag");
    }

    public void initialize() {
        try {
            plugin.getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            createSchema();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize SQLite", exception);
        }
    }

    public void migrateFromYamlIfNeeded() {
        if (migrationFlagFile.exists()) {
            return;
        }

        try {
            if (!isDatabaseEmpty()) {
                writeMigrationFlag();
                return;
            }

            migrateRunesYaml();
            migrateGraceYaml();
            writeMigrationFlag();
        } catch (Exception exception) {
            plugin.getLogger().severe("SQLite migration failed: " + exception.getMessage());
        }
    }

    public Map<UUID, Integer> loadRunes() {
        Map<UUID, Integer> result = new HashMap<>();
        String sql = "SELECT player_uuid, runes FROM player_runes";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    int runes = Math.max(0, rs.getInt("runes"));
                    result.put(playerId, runes);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_runes");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load runes: " + exception.getMessage());
        }

        return result;
    }

    public void saveRunes(Map<UUID, Integer> runesByPlayer) {
        try {
            connection.setAutoCommit(false);
            upsertRunes(runesByPlayer);
            deleteMissingPlayers("player_runes", runesByPlayer.keySet());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            rollbackQuietly();
            plugin.getLogger().severe("Could not save runes: " + exception.getMessage());
        }
    }

    public Map<UUID, String> loadActiveGraces() {
        Map<UUID, String> result = new HashMap<>();
        String sql = "SELECT player_uuid, active_grace FROM player_grace_state WHERE active_grace IS NOT NULL";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    String activeGrace = normalize(rs.getString("active_grace"));
                    if (!activeGrace.isBlank()) {
                        result.put(playerId, activeGrace);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_grace_state");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load active graces: " + exception.getMessage());
        }

        return result;
    }

    public Map<UUID, Set<String>> loadDiscoveredGraces() {
        Map<UUID, Set<String>> result = new HashMap<>();
        String sql = "SELECT player_uuid, grace_key FROM player_discovered_graces";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    String grace = normalize(rs.getString("grace_key"));
                    if (!grace.isBlank()) {
                        result.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(grace);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_discovered_graces");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load discovered graces: " + exception.getMessage());
        }

        return result;
    }

    public void saveGraceData(Map<UUID, String> activeGraceByPlayer, Map<UUID, Set<String>> discoveredGracesByPlayer) {
        try {
            connection.setAutoCommit(false);
            upsertActiveGraces(activeGraceByPlayer);
            deleteMissingPlayers("player_grace_state", activeGraceByPlayer.keySet());
            replaceDiscoveredGraces(discoveredGracesByPlayer);
            deleteMissingPlayers("player_discovered_graces", discoveredGracesByPlayer.keySet());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            rollbackQuietly();
            plugin.getLogger().severe("Could not save grace player data: " + exception.getMessage());
        }
    }

    public Map<UUID, String> loadPlayerClasses() {
        Map<UUID, String> result = new HashMap<>();
        String sql = "SELECT player_uuid, class_key FROM player_classes";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    String classKey = normalize(rs.getString("class_key"));
                    if (!classKey.isBlank()) {
                        result.put(playerId, classKey);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_classes");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load classes: " + exception.getMessage());
        }

        return result;
    }

    public void savePlayerClasses(Map<UUID, String> classByPlayer) {
        try {
            connection.setAutoCommit(false);
            upsertPlayerClasses(classByPlayer);
            deleteMissingPlayers("player_classes", classByPlayer.keySet());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            rollbackQuietly();
            plugin.getLogger().severe("Could not save classes: " + exception.getMessage());
        }
    }

    public Map<UUID, PlayerProgressData> loadPlayerProgress() {
        Map<UUID, PlayerProgressData> result = new HashMap<>();
        String sql = "SELECT player_uuid, level, vigor, mind, endurance, strength, dexterity, intelligence, faith, arcane FROM player_progress";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    PlayerProgressData data = new PlayerProgressData(
                            Math.max(1, rs.getInt("level")),
                            Math.max(1, rs.getInt("vigor")),
                            Math.max(1, rs.getInt("mind")),
                            Math.max(1, rs.getInt("endurance")),
                            Math.max(1, rs.getInt("strength")),
                            Math.max(1, rs.getInt("dexterity")),
                            Math.max(1, rs.getInt("intelligence")),
                            Math.max(1, rs.getInt("faith")),
                            Math.max(1, rs.getInt("arcane"))
                    );
                    result.put(playerId, data);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_progress");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load level progress: " + exception.getMessage());
        }

        return result;
    }

    public void savePlayerProgress(Map<UUID, PlayerProgressData> progressByPlayer) {
        try {
            connection.setAutoCommit(false);
            upsertPlayerProgress(progressByPlayer);
            deleteMissingPlayers("player_progress", progressByPlayer.keySet());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            rollbackQuietly();
            plugin.getLogger().severe("Could not save level progress: " + exception.getMessage());
        }
    }

    public void savePlayerProgress(UUID playerId, PlayerProgressData data) {
        String sql = """
                INSERT INTO player_progress(player_uuid, level, vigor, mind, endurance, strength, dexterity, intelligence, faith, arcane)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    level = excluded.level,
                    vigor = excluded.vigor,
                    mind = excluded.mind,
                    endurance = excluded.endurance,
                    strength = excluded.strength,
                    dexterity = excluded.dexterity,
                    intelligence = excluded.intelligence,
                    faith = excluded.faith,
                    arcane = excluded.arcane
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, Math.max(1, data.level()));
            statement.setInt(3, Math.max(1, data.vigor()));
            statement.setInt(4, Math.max(1, data.mind()));
            statement.setInt(5, Math.max(1, data.endurance()));
            statement.setInt(6, Math.max(1, data.strength()));
            statement.setInt(7, Math.max(1, data.dexterity()));
            statement.setInt(8, Math.max(1, data.intelligence()));
            statement.setInt(9, Math.max(1, data.faith()));
            statement.setInt(10, Math.max(1, data.arcane()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not save player progress for " + playerId + ": " + exception.getMessage());
        }
    }

    public Map<UUID, Double> loadPlayerFocus() {
        Map<UUID, Double> result = new HashMap<>();
        String sql = "SELECT player_uuid, current_focus FROM player_focus";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    double currentFocus = Math.max(0.0D, rs.getDouble("current_focus"));
                    result.put(playerId, currentFocus);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_focus");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load player focus: " + exception.getMessage());
        }

        return result;
    }

    public void savePlayerFocus(Map<UUID, Double> focusByPlayer) {
        try {
            connection.setAutoCommit(false);
            upsertPlayerFocus(focusByPlayer);
            deleteMissingPlayers("player_focus", focusByPlayer.keySet());
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            rollbackQuietly();
            plugin.getLogger().severe("Could not save player focus: " + exception.getMessage());
        }
    }

    public void savePlayerFocus(UUID playerId, double currentFocus) {
        String sql = """
                INSERT INTO player_focus(player_uuid, current_focus)
                VALUES(?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    current_focus = excluded.current_focus
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setDouble(2, Math.max(0.0D, currentFocus));
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not save player focus for " + playerId + ": " + exception.getMessage());
        }
    }

    public Map<UUID, Set<String>> loadClaimedBossFirstKillRewards() {
        return loadBossRewardFlags("first_kill_claimed");
    }

    public Map<UUID, Set<String>> loadGrantedBossRemembrances() {
        return loadBossRewardFlags("remembrance_granted");
    }

    public void markBossFirstKillRewardClaimed(UUID playerId, String bossId) {
        upsertBossRewardFlag(playerId, bossId, "first_kill_claimed");
    }

    public void markBossRemembranceGranted(UUID playerId, String bossId) {
        upsertBossRewardFlag(playerId, bossId, "remembrance_granted");
    }

    public Map<UUID, List<String>> loadEquippedTalismans() {
        Map<UUID, List<String>> result = new HashMap<>();
        String sql = "SELECT player_uuid, slot, talisman_id FROM player_talismans ORDER BY slot ASC";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    int slot = rs.getInt("slot");
                    if (slot < 0 || slot >= 4) {
                        continue;
                    }
                    List<String> slots = result.computeIfAbsent(playerId, ignored -> emptyTalismanSlots());
                    String talismanId = normalize(rs.getString("talisman_id"));
                    slots.set(slot, talismanId.isBlank() ? null : talismanId);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_talismans");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load talismans: " + exception.getMessage());
        }

        return result;
    }

    public void saveEquippedTalisman(UUID playerId, int slot, String talismanId) {
        if (playerId == null || slot < 0 || slot >= 4) {
            return;
        }

        if (talismanId == null || talismanId.isBlank()) {
            String deleteSql = "DELETE FROM player_talismans WHERE player_uuid = ? AND slot = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
                statement.setString(1, playerId.toString());
                statement.setInt(2, slot);
                statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().severe("Could not remove talisman slot for " + playerId + ": " + exception.getMessage());
            }
            return;
        }

        String sql = """
                INSERT INTO player_talismans(player_uuid, slot, talisman_id)
                VALUES(?, ?, ?)
                ON CONFLICT(player_uuid, slot) DO UPDATE SET
                    talisman_id = excluded.talisman_id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setInt(2, slot);
            statement.setString(3, normalize(talismanId));
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not save talisman slot for " + playerId + ": " + exception.getMessage());
        }
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Error while closing the SQLite connection: " + exception.getMessage());
        }
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_runes (player_uuid TEXT PRIMARY KEY, runes INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_grace_state (player_uuid TEXT PRIMARY KEY, active_grace TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_discovered_graces (player_uuid TEXT NOT NULL, grace_key TEXT NOT NULL, PRIMARY KEY(player_uuid, grace_key))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_classes (player_uuid TEXT PRIMARY KEY, class_key TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_progress (player_uuid TEXT PRIMARY KEY, level INTEGER NOT NULL, vigor INTEGER NOT NULL, mind INTEGER NOT NULL, endurance INTEGER NOT NULL, strength INTEGER NOT NULL, dexterity INTEGER NOT NULL, intelligence INTEGER NOT NULL, faith INTEGER NOT NULL, arcane INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_focus (player_uuid TEXT PRIMARY KEY, current_focus REAL NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_boss_rewards (player_uuid TEXT NOT NULL, boss_id TEXT NOT NULL, first_kill_claimed INTEGER NOT NULL DEFAULT 0, remembrance_granted INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(player_uuid, boss_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_talismans (player_uuid TEXT NOT NULL, slot INTEGER NOT NULL, talisman_id TEXT NOT NULL, PRIMARY KEY(player_uuid, slot))");
        }
    }

    private boolean isDatabaseEmpty() throws SQLException {
        return countRows("player_runes") == 0
                && countRows("player_grace_state") == 0
                && countRows("player_discovered_graces") == 0
                && countRows("player_classes") == 0
                && countRows("player_progress") == 0
                && countRows("player_focus") == 0
                && countRows("player_boss_rewards") == 0
                && countRows("player_talismans") == 0;
    }

    private int countRows(String table) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM " + table;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt("count") : 0;
        }
    }

    private void migrateRunesYaml() {
        File runesFile = new File(plugin.getDataFolder(), "runes.yml");
        if (!runesFile.exists()) {
            return;
        }

        Map<UUID, Integer> runes = new HashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(runesFile);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section != null) {
            for (String uuidText : section.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidText);
                    int amount = Math.max(0, section.getInt(uuidText + ".runes", 0));
                    runes.put(playerId, amount);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Migration skipped (invalid UUID in runes.yml): " + uuidText);
                }
            }
        }

        if (!runes.isEmpty()) {
            saveRunes(runes);
        }
        backupMigratedFile(runesFile);
    }

    private void migrateGraceYaml() {
        File graceFile = new File(plugin.getDataFolder(), "grace-players.yml");
        if (!graceFile.exists()) {
            return;
        }

        Map<UUID, String> active = new HashMap<>();
        Map<UUID, Set<String>> discovered = new HashMap<>();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(graceFile);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section != null) {
            for (String uuidText : section.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidText);

                    String activeGrace = section.getString(uuidText + ".active-grace");
                    if (activeGrace != null && !activeGrace.isBlank()) {
                        active.put(playerId, normalize(activeGrace));
                    }

                    Set<String> discoveredSet = new HashSet<>();
                    for (String grace : section.getStringList(uuidText + ".discovered")) {
                        String normalized = normalize(grace);
                        if (!normalized.isBlank()) {
                            discoveredSet.add(normalized);
                        }
                    }

                    if (!discoveredSet.isEmpty()) {
                        discovered.put(playerId, discoveredSet);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Migration skipped (invalid UUID in grace-players.yml): " + uuidText);
                }
            }
        }

        saveGraceData(active, discovered);
        backupMigratedFile(graceFile);
    }

    private void backupMigratedFile(File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".migrated.bak");
        try {
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not create migration backup for " + file.getName());
        }
    }

    private void writeMigrationFlag() {
        try {
            Files.writeString(migrationFlagFile.toPath(), "ok");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not write sqlite-migrated.flag");
        }
    }

    private void upsertRunes(Map<UUID, Integer> runesByPlayer) throws SQLException {
        String sql = """
                INSERT INTO player_runes(player_uuid, runes)
                VALUES(?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    runes = excluded.runes
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, Integer> entry : runesByPlayer.entrySet()) {
                statement.setString(1, entry.getKey().toString());
                statement.setInt(2, Math.max(0, entry.getValue()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void upsertActiveGraces(Map<UUID, String> activeGraceByPlayer) throws SQLException {
        String sql = """
                INSERT INTO player_grace_state(player_uuid, active_grace)
                VALUES(?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    active_grace = excluded.active_grace
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, String> entry : activeGraceByPlayer.entrySet()) {
                String activeGrace = normalize(entry.getValue());
                if (activeGrace.isBlank()) {
                    continue;
                }
                statement.setString(1, entry.getKey().toString());
                statement.setString(2, activeGrace);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void replaceDiscoveredGraces(Map<UUID, Set<String>> discoveredGracesByPlayer) throws SQLException {
        String deleteSql = "DELETE FROM player_discovered_graces WHERE player_uuid = ?";
        String insertSql = "INSERT INTO player_discovered_graces(player_uuid, grace_key) VALUES(?, ?)";

        try (PreparedStatement delete = connection.prepareStatement(deleteSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (Map.Entry<UUID, Set<String>> entry : discoveredGracesByPlayer.entrySet()) {
                delete.setString(1, entry.getKey().toString());
                delete.executeUpdate();

                for (String grace : entry.getValue()) {
                    String normalizedGrace = normalize(grace);
                    if (normalizedGrace.isBlank()) {
                        continue;
                    }
                    insert.setString(1, entry.getKey().toString());
                    insert.setString(2, normalizedGrace);
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        }
    }

    private void upsertPlayerClasses(Map<UUID, String> classByPlayer) throws SQLException {
        String sql = """
                INSERT INTO player_classes(player_uuid, class_key)
                VALUES(?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    class_key = excluded.class_key
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, String> entry : classByPlayer.entrySet()) {
                String classKey = normalize(entry.getValue());
                if (classKey.isBlank()) {
                    continue;
                }
                statement.setString(1, entry.getKey().toString());
                statement.setString(2, classKey);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void upsertPlayerProgress(Map<UUID, PlayerProgressData> progressByPlayer) throws SQLException {
        String sql = """
                INSERT INTO player_progress(player_uuid, level, vigor, mind, endurance, strength, dexterity, intelligence, faith, arcane)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    level = excluded.level,
                    vigor = excluded.vigor,
                    mind = excluded.mind,
                    endurance = excluded.endurance,
                    strength = excluded.strength,
                    dexterity = excluded.dexterity,
                    intelligence = excluded.intelligence,
                    faith = excluded.faith,
                    arcane = excluded.arcane
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, PlayerProgressData> entry : progressByPlayer.entrySet()) {
                PlayerProgressData data = entry.getValue();
                statement.setString(1, entry.getKey().toString());
                statement.setInt(2, Math.max(1, data.level()));
                statement.setInt(3, Math.max(1, data.vigor()));
                statement.setInt(4, Math.max(1, data.mind()));
                statement.setInt(5, Math.max(1, data.endurance()));
                statement.setInt(6, Math.max(1, data.strength()));
                statement.setInt(7, Math.max(1, data.dexterity()));
                statement.setInt(8, Math.max(1, data.intelligence()));
                statement.setInt(9, Math.max(1, data.faith()));
                statement.setInt(10, Math.max(1, data.arcane()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void upsertPlayerFocus(Map<UUID, Double> focusByPlayer) throws SQLException {
        String sql = """
                INSERT INTO player_focus(player_uuid, current_focus)
                VALUES(?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    current_focus = excluded.current_focus
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<UUID, Double> entry : focusByPlayer.entrySet()) {
                statement.setString(1, entry.getKey().toString());
                statement.setDouble(2, Math.max(0.0D, entry.getValue()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Map<UUID, Set<String>> loadBossRewardFlags(String column) {
        Map<UUID, Set<String>> result = new HashMap<>();
        String sql = "SELECT player_uuid, boss_id FROM player_boss_rewards WHERE " + column + " = 1";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    String bossId = normalize(rs.getString("boss_id"));
                    if (!bossId.isBlank()) {
                        result.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(bossId);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid UUID in player_boss_rewards");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not load boss reward flags (" + column + "): " + exception.getMessage());
        }

        return result;
    }

    private void upsertBossRewardFlag(UUID playerId, String bossId, String column) {
        String normalizedBossId = normalize(bossId);
        if (playerId == null || normalizedBossId.isBlank()) {
            return;
        }

        String sql = """
                INSERT INTO player_boss_rewards(player_uuid, boss_id, %s)
                VALUES(?, ?, 1)
                ON CONFLICT(player_uuid, boss_id) DO UPDATE SET
                    %s = 1
                """.formatted(column, column);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, normalizedBossId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not update boss reward flag for " + playerId + " / " + normalizedBossId + ": " + exception.getMessage());
        }
    }

    private void deleteMissingPlayers(String table, Set<UUID> playerIds) throws SQLException {
        if (playerIds.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table)) {
                statement.executeUpdate();
            }
            return;
        }

        StringBuilder sql = new StringBuilder("DELETE FROM ").append(table).append(" WHERE player_uuid NOT IN (");
        for (int i = 0; i < playerIds.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append('?');
        }
        sql.append(')');

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (UUID playerId : playerIds) {
                statement.setString(index++, playerId.toString());
            }
            statement.executeUpdate();
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // no-op
        }
    }

    private List<String> emptyTalismanSlots() {
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            slots.add(null);
        }
        return slots;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}





