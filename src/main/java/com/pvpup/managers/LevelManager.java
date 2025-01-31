package com.pvpup.managers;

import com.pvpup.PvPUP;
import com.pvpup.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LevelManager {
    private final PvPUP plugin;
    private final Map<UUID, PlayerData> playerData;
    private final int maxLevel;

    public LevelManager(PvPUP plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.maxLevel = plugin.getConfig().getInt("max-level", 20);
    }

    public void loadPlayer(Player player) {
        PlayerData data = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        playerData.put(player.getUniqueId(), data);
        updatePlayerLevel(player);
    }

    public void unloadPlayer(Player player) {
        PlayerData data = playerData.remove(player.getUniqueId());
        if (data != null) {
            plugin.getDatabaseManager().savePlayer(data);
        }
    }

    public int getPlayerLevel(Player player) {
        PlayerData data = playerData.get(player.getUniqueId());
        return data != null ? data.getLevel() : 1;
    }

    public void addLevel(Player player, int amount) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data != null) {
            int newLevel = Math.min(data.getLevel() + amount, maxLevel);
            data.setLevel(newLevel);
            updatePlayerLevel(player);
            
            String message = plugin.getMessagesConfig().getString("level-up")
                    .replace("%level%", String.valueOf(newLevel));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public void removeLevels(Player player, int amount) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data != null) {
            int newLevel = Math.max(1, data.getLevel() - amount);
            data.setLevel(newLevel);
            updatePlayerLevel(player);
            
            String message = plugin.getMessagesConfig().getString("level-down")
                    .replace("%level%", String.valueOf(newLevel));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public void setLevel(Player player, int level) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data != null) {
            int newLevel = Math.min(Math.max(1, level), maxLevel);
            data.setLevel(newLevel);
            updatePlayerLevel(player);

            String message = plugin.getMessagesConfig().getString("level-set")
                    .replace("%level%", String.valueOf(newLevel));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private void updatePlayerLevel(Player player) {
        int level = getPlayerLevel(player);
        
        // Clear inventory
        player.getInventory().clear();
        
        // Give items for current level
        String itemPath = "levels." + level + ".items";
        if (plugin.getConfig().contains(itemPath)) {
            for (String item : plugin.getConfig().getStringList(itemPath)) {
                ItemStack itemStack = parseItem(item);
                if (itemStack != null) {
                    player.getInventory().addItem(itemStack);
                }
            }
        }
        
        // Update scoreboard
        plugin.getScoreboardManager().updateScoreboard(player);
    }

    private ItemStack parseItem(String itemString) {
        try {
            String[] parts = itemString.split(":");
            Material material = Material.valueOf(parts[0].toUpperCase());
            ItemStack item = new ItemStack(material);

            // If amount is specified
            if (parts.length > 1) {
                item.setAmount(Integer.parseInt(parts[1]));
            }

            // Si es una armadura, agregar automáticamente
            if (material.name().contains("CHESTPLATE") || 
                material.name().contains("LEGGINGS") || 
                material.name().contains("BOOTS") || 
                material.name().contains("HELMET")) {
                item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
            }

            // Si es una espada, agregar afilado
            if (material.name().contains("SWORD")) {
                item.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing item: " + itemString);
            return null;
        }
    }

    public void incrementKills(Player player) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data != null) {
            data.setKills(data.getKills() + 1);
            plugin.getScoreboardManager().updateScoreboard(player);
        }
    }
}