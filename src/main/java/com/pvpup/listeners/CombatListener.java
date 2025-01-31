package com.pvpup.listeners;

import com.pvpup.PvPUP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.block.Block;
import org.bukkit.Material;

public class CombatListener implements Listener {
    private final PvPUP plugin;

    public CombatListener(PvPUP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLevelManager().loadPlayer(player);
        plugin.getScoreboardManager().updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLevelManager().unloadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Handle death by player
        if (killer != null) {
            plugin.getLevelManager().addLevel(killer, 1);
            plugin.getLevelManager().incrementKills(killer);
            
            String killMessage = plugin.getMessagesConfig().getString("kill-message")
                    .replace("%killer%", killer.getName())
                    .replace("%victim%", victim.getName());
            plugin.getServer().broadcastMessage(killMessage);
        }

        // Handle victim level loss
        plugin.getLevelManager().removeLevels(victim, 3);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block block = event.getTo().getBlock();
        
        // Check if player touches water
        if (block.getType() == Material.WATER) {
            player.setHealth(0); // Kill player instantly
            
            String waterDeathMessage = plugin.getMessagesConfig().getString("water-death-message")
                    .replace("%player%", player.getName());
            plugin.getServer().broadcastMessage(waterDeathMessage);
            
            plugin.getLevelManager().removeLevels(player, 3);
        }
    }
}
