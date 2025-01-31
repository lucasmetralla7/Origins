package com.pvpup.managers;

import com.pvpup.PvPUP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardManager {
    private final PvPUP plugin;

    public ScoreboardManager(PvPUP plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("pvpup", "dummy", 
            ChatColor.GOLD + "" + ChatColor.BOLD + "PvP UP");
        
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int level = plugin.getLevelManager().getPlayerLevel(player);
        int kills = plugin.getDatabaseManager().loadPlayer(player.getUniqueId()).getKills();

        Score levelScore = objective.getScore(ChatColor.YELLOW + "Nivel: " + ChatColor.WHITE + level);
        levelScore.setScore(4);

        Score killsScore = objective.getScore(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + kills);
        killsScore.setScore(3);

        Score blank = objective.getScore(ChatColor.GRAY + "");
        blank.setScore(2);

        Score maxLevel = objective.getScore(ChatColor.YELLOW + "Nivel Máximo: " + 
            ChatColor.WHITE + plugin.getConfig().getInt("max-level", 20));
        maxLevel.setScore(1);

        // Mostrar booster activo si existe
        if (player.hasPermission("pvpup.booster.mvp") || player.hasPermission("pvpup.booster.vip")) {
            String boosterType = player.hasPermission("pvpup.booster.mvp") ? "MVP" : "VIP";
            double multiplier = player.hasPermission("pvpup.booster.mvp") ? 
                plugin.getConfig().getDouble("boosters.mvp", 2.0) :
                plugin.getConfig().getDouble("boosters.vip", 1.5);

            Score boosterScore = objective.getScore(
                ChatColor.GREEN + "Booster " + boosterType + ": " + ChatColor.WHITE + "x" + multiplier);
            boosterScore.setScore(0);
        }

        player.setScoreboard(board);
    }
}