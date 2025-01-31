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
    private final Map<UUID, Double> activeBoosts;
    private final int maxLevel;

    public LevelManager(PvPUP plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.activeBoosts = new HashMap<>();
        this.maxLevel = plugin.getConfig().getInt("max-level", 20);
    }

    public void loadPlayer(Player player) {
        PlayerData data = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        playerData.put(player.getUniqueId(), data);
        updatePlayerLevel(player);

        if (player.hasPermission("pvpup.booster.mvp")) {
            activeBoosts.put(player.getUniqueId(), 
                plugin.getConfig().getDouble("boosters.mvp", 2.0));
        } else if (player.hasPermission("pvpup.booster.vip")) {
            activeBoosts.put(player.getUniqueId(), 
                plugin.getConfig().getDouble("boosters.vip", 1.5));
        } else {
            activeBoosts.put(player.getUniqueId(), 
                plugin.getConfig().getDouble("boosters.default", 1.0));
        }
    }

    public void unloadPlayer(Player player) {
        PlayerData data = playerData.remove(player.getUniqueId());
        if (data != null) {
            plugin.getDatabaseManager().savePlayer(data);
        }
        activeBoosts.remove(player.getUniqueId());
    }

    public int getPlayerLevel(Player player) {
        PlayerData data = playerData.get(player.getUniqueId());
        return data != null ? data.getLevel() : 1;
    }

    public void addLevel(Player player, int amount) {
        PlayerData data = playerData.get(player.getUniqueId());
        if (data != null) {
            double boost = activeBoosts.getOrDefault(player.getUniqueId(), 1.0);
            int boostedAmount = (int) Math.ceil(amount * boost);

            int newLevel = Math.min(data.getLevel() + boostedAmount, maxLevel);
            data.setLevel(newLevel);
            updatePlayerLevel(player);

            String message = plugin.getMessagesConfig().getString("level-up")
                    .replace("%level%", String.valueOf(newLevel));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public void setBooster(Player player, double multiplier, boolean temporary) {
        activeBoosts.put(player.getUniqueId(), multiplier);

        String message = plugin.getMessagesConfig().getString("booster-activated")
                .replace("%multiplier%", String.valueOf(multiplier));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        if (temporary) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                removeBooster(player);
            }, 20L * 60 * 30);
        }
    }

    public void removeBooster(Player player) {
        activeBoosts.put(player.getUniqueId(), 
            plugin.getConfig().getDouble("boosters.default", 1.0));

        String message = plugin.getMessagesConfig().getString("booster-expired");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
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

        player.getInventory().clear();

        String itemPath = "levels." + level + ".items";
        if (plugin.getConfig().contains(itemPath)) {
            for (String item : plugin.getConfig().getStringList(itemPath)) {
                ItemStack itemStack = parseItem(item);
                if (itemStack != null) {
                    player.getInventory().addItem(itemStack);
                }
            }
        }

        plugin.getScoreboardManager().updateScoreboard(player);
    }

    private ItemStack parseItem(String itemString) {
        try {
            String[] parts = itemString.split(":");
            Material material = Material.valueOf(parts[0].toUpperCase());
            ItemStack item = new ItemStack(material);

            if (parts.length > 1) {
                item.setAmount(Integer.parseInt(parts[1]));
            }

            if (material.name().contains("CHESTPLATE") || 
                material.name().contains("LEGGINGS") || 
                material.name().contains("BOOTS") || 
                material.name().contains("HELMET")) {

                if (material.name().contains("DIAMOND")) {
                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 2);
                    item.addEnchantment(Enchantment.DURABILITY, 2);
                } 
                else if (material.name().contains("IRON")) {
                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
                    item.addEnchantment(Enchantment.DURABILITY, 1);
                }
                else if (material.name().contains("LEATHER")) {
                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
                }
            }

            if (material.name().contains("SWORD")) {
                if (material.name().contains("DIAMOND")) {
                    item.addEnchantment(Enchantment.DAMAGE_ALL, 2);
                    item.addEnchantment(Enchantment.DURABILITY, 2);
                    item.addEnchantment(Enchantment.FIRE_ASPECT, 1);
                }
                else if (material.name().contains("IRON")) {
                    item.addEnchantment(Enchantment.DAMAGE_ALL, 1);
                    item.addEnchantment(Enchantment.DURABILITY, 1);
                }
                else {
                    item.addEnchantment(Enchantment.DAMAGE_ALL, 1);
                }
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