package com.pvpup.managers;

import com.pvpup.PvPUP;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ArenaManager {
    private final PvPUP plugin;
    private final Map<String, Arena> arenas;

    public ArenaManager(PvPUP plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        loadArenas();
    }

    public void createArena(String name, Location pos1, Location pos2, Location spawn) {
        Arena arena = new Arena(name, pos1, pos2, spawn);
        arenas.put(name.toLowerCase(), arena);
        saveArena(arena);
    }

    public void removeArena(String name) {
        arenas.remove(name.toLowerCase());
        plugin.getConfig().set("arenas." + name.toLowerCase(), null);
        plugin.saveConfig();
    }

    public boolean teleportToArena(Player player, String arenaName) {
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena != null && arena.getSpawnLocation() != null) {
            player.teleport(arena.getSpawnLocation());
            return true;
        }
        return false;
    }

    public Set<String> getArenaNames() {
        return arenas.keySet();
    }

    public boolean isInArena(Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.isInside(location)) {
                return true;
            }
        }
        return false;
    }

    private void loadArenas() {
        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaName : arenasSection.getKeys(false)) {
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
                if (arenaSection != null) {
                    Location pos1 = loadLocation(arenaSection.getConfigurationSection("pos1"));
                    Location pos2 = loadLocation(arenaSection.getConfigurationSection("pos2"));
                    Location spawn = loadLocation(arenaSection.getConfigurationSection("spawn"));
                    
                    if (pos1 != null && pos2 != null && spawn != null) {
                        arenas.put(arenaName.toLowerCase(), new Arena(arenaName, pos1, pos2, spawn));
                    }
                }
            }
        }
    }

    private void saveArena(Arena arena) {
        String path = "arenas." + arena.getName().toLowerCase();
        saveLocation(path + ".pos1", arena.getPos1());
        saveLocation(path + ".pos2", arena.getPos2());
        saveLocation(path + ".spawn", arena.getSpawnLocation());
        plugin.saveConfig();
    }

    private Location loadLocation(ConfigurationSection section) {
        if (section == null) return null;
        
        String worldName = section.getString("world");
        if (worldName == null || plugin.getServer().getWorld(worldName) == null) {
            return null;
        }

        return new Location(
            plugin.getServer().getWorld(worldName),
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw", 0),
            (float) section.getDouble("pitch", 0)
        );
    }

    private void saveLocation(String path, Location location) {
        plugin.getConfig().set(path + ".world", location.getWorld().getName());
        plugin.getConfig().set(path + ".x", location.getX());
        plugin.getConfig().set(path + ".y", location.getY());
        plugin.getConfig().set(path + ".z", location.getZ());
        plugin.getConfig().set(path + ".yaw", location.getYaw());
        plugin.getConfig().set(path + ".pitch", location.getPitch());
    }

    public static class Arena {
        private final String name;
        private final Location pos1;
        private final Location pos2;
        private final Location spawnLocation;

        public Arena(String name, Location pos1, Location pos2, Location spawnLocation) {
            this.name = name;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.spawnLocation = spawnLocation;
        }

        public String getName() {
            return name;
        }

        public Location getPos1() {
            return pos1;
        }

        public Location getPos2() {
            return pos2;
        }

        public Location getSpawnLocation() {
            return spawnLocation;
        }

        public boolean isInside(Location location) {
            if (!location.getWorld().equals(pos1.getWorld())) return false;

            double minX = Math.min(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            return location.getX() >= minX && location.getX() <= maxX &&
                   location.getY() >= minY && location.getY() <= maxY &&
                   location.getZ() >= minZ && location.getZ() <= maxZ;
        }
    }
}
