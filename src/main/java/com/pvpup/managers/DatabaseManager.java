package com.pvpup.managers;

import com.pvpup.PvPUP;
import com.pvpup.models.PlayerData;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private final PvPUP plugin;
    private Connection connection;
    private final String databaseType;

    public DatabaseManager(PvPUP plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfig().getString("database.type", "sqlite");
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            if ("mysql".equalsIgnoreCase(databaseType)) {
                initializeMySql();
            } else {
                initializeSqlite();
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "level INTEGER DEFAULT 1," +
                    "kills INTEGER DEFAULT 0" +
                    ")"
                );
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void initializeMySql() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "pvpup");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");

        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        connection = DriverManager.getConnection(url, username, password);
    }

    private void initializeSqlite() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        String filename = plugin.getConfig().getString("database.sqlite.file", "players.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/" + filename);
    }

    public PlayerData loadPlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new PlayerData(
                    uuid,
                    rs.getInt("level"),
                    rs.getInt("kills")
                );
            } else {
                createNewPlayer(uuid);
                return new PlayerData(uuid, 1, 0);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
            return new PlayerData(uuid, 1, 0);
        }
    }

    private void createNewPlayer(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO players (uuid, level, kills) VALUES (?, 1, 0)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void savePlayer(PlayerData playerData) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE players SET level = ?, kills = ? WHERE uuid = ?")) {
            ps.setInt(1, playerData.getLevel());
            ps.setInt(2, playerData.getKills());
            ps.setString(3, playerData.getUuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }
}