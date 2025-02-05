package com.pvpup.managers;

import com.pvpup.PvPUP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import java.util.List;

public class ScoreboardManager {
    private final PvPUP plugin;

    public ScoreboardManager(PvPUP plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("scoreboard.title", "&6&lPvP UP"));

        Objective objective = board.registerNewObjective("pvpup", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int score = lines.size();

        for (String line : lines) {
            // Si es una línea de booster, verificar si el jugador tiene booster
            if (line.contains("%booster%")) {
                if (player.hasPermission("pvpup.booster.mvp") || player.hasPermission("pvpup.booster.vip")) {
                    String boosterType = player.hasPermission("pvpup.booster.mvp") ? "MVP" : "VIP";
                    double multiplier = player.hasPermission("pvpup.booster.mvp") ? 
                        plugin.getConfig().getDouble("boosters.mvp", 2.0) :
                        plugin.getConfig().getDouble("boosters.vip", 1.5);

                    line = line.replace("%booster_type%", boosterType)
                             .replace("%booster%", String.valueOf(multiplier));
                } else {
                    continue; // Saltar esta línea si no tiene booster
                }
            }

            // Reemplazar variables
            line = line.replace("%level%", String.valueOf(plugin.getLevelManager().getPlayerLevel(player)))
                      .replace("%kills%", String.valueOf(plugin.getDatabaseManager().loadPlayer(player.getUniqueId()).getKills()))
                      .replace("%max_level%", String.valueOf(plugin.getConfig().getInt("max-level", 80)))
                      .replace("%killstreak%", String.valueOf(plugin.getLevelManager().getKillstreak(player)))
                      .replace("%player%", player.getName())
                      .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

            // Aplicar colores y establecer la línea
            String coloredLine = ChatColor.translateAlternateColorCodes('&', line);
            Score lineScore = objective.getScore(coloredLine);
            lineScore.setScore(score--);
        }

        player.setScoreboard(board);
    }
}