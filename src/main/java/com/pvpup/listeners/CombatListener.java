package com.pvpup.listeners;

import com.pvpup.PvPUP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {
    private final PvPUP plugin;
    private final Map<UUID, Long> deathCooldowns;

    public CombatListener(PvPUP plugin) {
        this.plugin = plugin;
        this.deathCooldowns = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Reiniciar nivel y cargar datos
        plugin.getLevelManager().setLevel(player, 1);
        plugin.getLevelManager().loadPlayer(player);
        plugin.getScoreboardManager().updateScoreboard(player);

        // Teleportar al spawn
        Location spawn = plugin.getSpawnLocation();
        if (spawn != null) {
            player.teleport(spawn);
        }

        // Mensaje de bienvenida
        String joinMessage = plugin.getMessagesConfig().getString("join-message", "&7¡Bienvenido a &6PvP UP&7!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', joinMessage));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Matar al jugador cuando se desconecta
        player.setHealth(0);

        // Al desconectarse, el nivel se reinicia pero guardamos las kills
        plugin.getLevelManager().setLevel(player, 1);
        plugin.getLevelManager().unloadPlayer(player);
        deathCooldowns.remove(player.getUniqueId());

        // La próxima vez que entre aparecerá en el spawn (gestionado por onPlayerJoin)
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Ocultar mensajes de muerte y pantalla
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();

        // Reset victim's killstreak
        plugin.getLevelManager().resetKillstreak(victim);

        // Handle death by player
        if (killer != null) {
            plugin.getLevelManager().addLevel(killer, 1);
            plugin.getLevelManager().incrementKills(killer);

            String killMessage = plugin.getMessagesConfig().getString("kill-message")
                    .replace("%killer%", killer.getName())
                    .replace("%victim%", victim.getName());
            plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', killMessage));
        }

        // Reiniciar nivel de la víctima
        plugin.getLevelManager().setLevel(victim, 1);

        // Set death cooldown
        deathCooldowns.put(victim.getUniqueId(), System.currentTimeMillis());

        // Respawn inmediato sin pantalla de muerte
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            victim.spigot().respawn();
            Location spawn = plugin.getSpawnLocation();
            if (spawn != null) {
                victim.teleport(spawn);
            }
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Check cooldowns
        long cooldownTime = plugin.getConfig().getInt("combat.death-cooldown", 5) * 1000L;
        long lastDeath = deathCooldowns.getOrDefault(attacker.getUniqueId(), 0L);
        long timeLeft = (lastDeath + cooldownTime - System.currentTimeMillis()) / 1000;

        if (timeLeft > 0) {
            event.setCancelled(true);
            String cooldownMessage = plugin.getConfig().getString("combat.cooldown-message", "&cDebes esperar %time% segundos.")
                    .replace("%time%", String.valueOf(timeLeft));
            attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMessage));
            return;
        }

        // Check if either player is in a safe zone
        if (plugin.getSafeZoneManager().isInSafeZone(victim.getLocation()) ||
                plugin.getSafeZoneManager().isInSafeZone(attacker.getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "No puedes atacar en una zona segura.");
            return;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block block = event.getTo().getBlock();

        // Check if player touches water
        if (block.getType() == Material.WATER) {
            // Play death effects before killing the player
            playWaterDeathEffects(player.getLocation());

            player.setHealth(0); // Kill player instantly

            String waterDeathMessage = plugin.getMessagesConfig().getString("water-death-message")
                    .replace("%player%", player.getName());
            plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', waterDeathMessage));

            // Reiniciar nivel
            plugin.getLevelManager().setLevel(player, 1);

            // Set death cooldown
            deathCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

            // Teleportar al spawn inmediatamente
            Location spawn = plugin.getSpawnLocation();
            if (spawn != null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.teleport(spawn);
                });
            }
        }
    }

    private void playWaterDeathEffects(Location location) {
        FileConfiguration config = plugin.getConfig();

        // Particle effects
        if (config.getBoolean("death-effects.water-death.particles.enabled", true)) {
            try {
                String particleType = config.getString("death-effects.water-death.particles.type", "WATER_SPLASH");
                int count = config.getInt("death-effects.water-death.particles.count", 50);
                double radius = config.getDouble("death-effects.water-death.particles.radius", 1.0);

                location.getWorld().spawnParticle(
                        Particle.valueOf(particleType),
                        location,
                        count,
                        radius, radius, radius,
                        0.1
                );
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type in config: " + e.getMessage());
            }
        }

        // Sound effects
        if (config.getBoolean("death-effects.water-death.sound.enabled", true)) {
            try {
                String soundType = config.getString("death-effects.water-death.sound.type", "ENTITY_PLAYER_SPLASH");
                float volume = (float) config.getDouble("death-effects.water-death.sound.volume", 1.0);
                float pitch = (float) config.getDouble("death-effects.water-death.sound.pitch", 1.0);

                location.getWorld().playSound(
                        location,
                        Sound.valueOf(soundType),
                        volume,
                        pitch
                );
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound type in config: " + e.getMessage());
            }
        }
    }
}