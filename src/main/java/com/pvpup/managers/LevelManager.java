package com.pvpup.managers;

import com.pvpup.PvPUP;
import com.pvpup.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

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
        this.maxLevel = plugin.getConfig().getInt("max-level", 80);
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
            int oldLevel = data.getLevel();
            int newLevel = Math.min(data.getLevel() + boostedAmount, maxLevel);

            if (newLevel > oldLevel) {
                data.setLevel(newLevel);
                updatePlayerLevel(player);
                playLevelUpEffects(player);

                String message = plugin.getMessagesConfig().getString("level-up")
                        .replace("%level%", String.valueOf(newLevel));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    private void playLevelUpEffects(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        // Efectos de sonido
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);

        // Tarea para animar partículas
        new BukkitRunnable() {
            double phi = 0;
            public void run() {
                phi += Math.PI/8;
                for (double theta = 0; theta <= 2*Math.PI; theta += Math.PI/8) {
                    double x = 1.5 * Math.cos(theta) * Math.cos(phi);
                    double y = 1.5 * Math.sin(phi) + 1;
                    double z = 1.5 * Math.sin(theta) * Math.cos(phi);

                    Location particleLoc = loc.clone().add(x, y, z);
                    world.spawnParticle(Particle.SPELL_WITCH, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }

                if (phi > 4*Math.PI) {
                    this.cancel();
                    // Efecto final
                    world.spawnParticle(Particle.EXPLOSION_LARGE, loc.clone().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
                    world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
                ItemStack itemStack = parseItem(item, level);
                if (itemStack != null) {
                    player.getInventory().addItem(itemStack);
                }
            }
        }

        plugin.getScoreboardManager().updateScoreboard(player);
    }

    private ItemStack parseItem(String itemString, int playerLevel) {
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

                if (material.name().contains("NETHERITE")) {
                    int protectionLevel = playerLevel >= 75 ? 5 : (playerLevel >= 65 ? 4 : 3);
                    int durabilityLevel = playerLevel >= 75 ? 4 : (playerLevel >= 65 ? 3 : 2);

                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, protectionLevel);
                    item.addEnchantment(Enchantment.DURABILITY, durabilityLevel);

                    if (playerLevel >= 70) {
                        item.addEnchantment(Enchantment.THORNS, 3);
                    }
                    if (playerLevel >= 80) {
                        if (material.name().contains("BOOTS")) {
                            item.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
                            item.addEnchantment(Enchantment.PROTECTION_FALL, 4);
                        }
                    }
                }
                else if (material.name().contains("DIAMOND")) {
                    int protectionLevel = playerLevel >= 45 ? 4 : (playerLevel >= 40 ? 3 : 2);
                    int durabilityLevel = playerLevel >= 45 ? 3 : 2;

                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, protectionLevel);
                    item.addEnchantment(Enchantment.DURABILITY, durabilityLevel);

                    if (playerLevel >= 45) {
                        item.addEnchantment(Enchantment.THORNS, 2);
                    }
                }
                else if (material.name().contains("IRON")) {
                    int protectionLevel = playerLevel >= 25 ? 3 : (playerLevel >= 20 ? 2 : 1);
                    int durabilityLevel = playerLevel >= 25 ? 2 : 1;

                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, protectionLevel);
                    item.addEnchantment(Enchantment.DURABILITY, durabilityLevel);
                }
                else if (material.name().contains("LEATHER") && playerLevel >= 5) {
                    item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
                }
            }

            if (material.name().contains("SWORD")) {
                if (material.name().contains("NETHERITE")) {
                    int sharpnessLevel = playerLevel >= 75 ? 6 : (playerLevel >= 65 ? 5 : 4);
                    int durabilityLevel = playerLevel >= 75 ? 4 : 3;

                    item.addEnchantment(Enchantment.DAMAGE_ALL, sharpnessLevel);
                    item.addEnchantment(Enchantment.DURABILITY, durabilityLevel);
                    item.addEnchantment(Enchantment.FIRE_ASPECT, 2);

                    if (playerLevel >= 70) {
                        item.addEnchantment(Enchantment.KNOCKBACK, 2);
                    }
                    if (playerLevel >= 80) {
                        item.addEnchantment(Enchantment.SWEEPING_EDGE, 3);
                        item.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
                    }
                }
                else if (material.name().contains("DIAMOND")) {
                    int sharpnessLevel = playerLevel >= 45 ? 5 : (playerLevel >= 40 ? 4 : 3);
                    int durabilityLevel = playerLevel >= 45 ? 3 : 2;

                    item.addEnchantment(Enchantment.DAMAGE_ALL, sharpnessLevel);
                    item.addEnchantment(Enchantment.DURABILITY, durabilityLevel);

                    if (playerLevel >= 40) {
                        item.addEnchantment(Enchantment.FIRE_ASPECT, 1);
                        item.addEnchantment(Enchantment.KNOCKBACK, 1);
                    }
                }
                else if (material.name().contains("IRON")) {
                    int sharpnessLevel = playerLevel >= 25 ? 3 : (playerLevel >= 20 ? 2 : 1);
                    int durabilityLevel = playerLevel >= 25 ? 2 : 1;

                    item.addEnchantment(Enchantment.DAMAGE_ALL, sharpnessLevel);
                    item.addEnchantment(Enchantment.DURABILITY, durabilityLevel);
                }
                else if (material.name().contains("STONE") && playerLevel >= 10) {
                    item.addEnchantment(Enchantment.DAMAGE_ALL, 1);
                    item.addEnchantment(Enchantment.DURABILITY, 1);
                }
                else if (material.name().contains("WOODEN") && playerLevel >= 5) {
                    item.addEnchantment(Enchantment.DAMAGE_ALL, 1);
                }
            }

            if (material == Material.BOW) {
                if (playerLevel >= 70) {
                    item.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
                    item.addEnchantment(Enchantment.DURABILITY, 3);
                    item.addEnchantment(Enchantment.ARROW_INFINITE, 1);
                    item.addEnchantment(Enchantment.ARROW_FIRE, 1);
                    item.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
                }
                else if (playerLevel >= 45) {
                    item.addEnchantment(Enchantment.ARROW_DAMAGE, 4);
                    item.addEnchantment(Enchantment.DURABILITY, 3);
                    item.addEnchantment(Enchantment.ARROW_INFINITE, 1);
                    item.addEnchantment(Enchantment.ARROW_FIRE, 1);
                }
                else if (playerLevel >= 35) {
                    item.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
                    item.addEnchantment(Enchantment.DURABILITY, 2);
                    item.addEnchantment(Enchantment.ARROW_INFINITE, 1);
                }
            }

            if (material == Material.CROSSBOW && playerLevel >= 75) {
                 item.addEnchantment(Enchantment.PIERCING, 4);
                 item.addEnchantment(Enchantment.QUICK_CHARGE, 3);
                 item.addEnchantment(Enchantment.DURABILITY, 3);
                 if (playerLevel >= 80) {
                     item.addEnchantment(Enchantment.MULTISHOT, 1);
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