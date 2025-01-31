package com.pvpup.managers;

import com.pvpup.PvPUP;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class SafeZoneManager {
    private final PvPUP plugin;
    private Location pos1;
    private Location pos2;

    public SafeZoneManager(PvPUP plugin) {
        this.plugin = plugin;
        loadSafeZone();
    }

    public void setPos1(Location location) {
        this.pos1 = location.clone();
        saveSafeZone();
    }

    public void setPos2(Location location) {
        this.pos2 = location.clone();
        saveSafeZone();
    }

    public boolean isInSafeZone(Location location) {
        if (pos1 == null || pos2 == null) return false;
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

    private void saveSafeZone() {
        FileConfiguration config = plugin.getConfig();
        if (pos1 != null) {
            config.set("safezone.pos1.world", pos1.getWorld().getName());
            config.set("safezone.pos1.x", pos1.getX());
            config.set("safezone.pos1.y", pos1.getY());
            config.set("safezone.pos1.z", pos1.getZ());
        }
        if (pos2 != null) {
            config.set("safezone.pos2.world", pos2.getWorld().getName());
            config.set("safezone.pos2.x", pos2.getX());
            config.set("safezone.pos2.y", pos2.getY());
            config.set("safezone.pos2.z", pos2.getZ());
        }
        plugin.saveConfig();
    }

    private void loadSafeZone() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("safezone.pos1") && config.contains("safezone.pos2")) {
            String world1 = config.getString("safezone.pos1.world");
            String world2 = config.getString("safezone.pos2.world");
            if (world1 != null && world2 != null && 
                plugin.getServer().getWorld(world1) != null && 
                plugin.getServer().getWorld(world2) != null) {
                
                pos1 = new Location(
                    plugin.getServer().getWorld(world1),
                    config.getDouble("safezone.pos1.x"),
                    config.getDouble("safezone.pos1.y"),
                    config.getDouble("safezone.pos1.z")
                );
                pos2 = new Location(
                    plugin.getServer().getWorld(world2),
                    config.getDouble("safezone.pos2.x"),
                    config.getDouble("safezone.pos2.y"),
                    config.getDouble("safezone.pos2.z")
                );
            }
        }
    }
}
