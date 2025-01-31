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
        
        Score levelScore = objective.getScore(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level);
        levelScore.setScore(2);

        Score blank = objective.getScore(ChatColor.GRAY + "");
        blank.setScore(1);

        Score maxLevel = objective.getScore(ChatColor.YELLOW + "Max Level: " + 
            ChatColor.WHITE + plugin.getConfig().getInt("max-level", 20));
        maxLevel.setScore(0);

        player.setScoreboard(board);
    }
}
