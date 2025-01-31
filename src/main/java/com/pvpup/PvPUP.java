package com.pvpup;

import com.pvpup.managers.DatabaseManager;
import com.pvpup.managers.LevelManager;
import com.pvpup.managers.ScoreboardManager;
import com.pvpup.listeners.CombatListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;

public class PvPUP extends JavaPlugin {
    private static PvPUP instance;
    private DatabaseManager databaseManager;
    private LevelManager levelManager;
    private ScoreboardManager scoreboardManager;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Load messages
        messagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        
        // Initialize managers
        databaseManager = new DatabaseManager(this);
        levelManager = new LevelManager(this);
        scoreboardManager = new ScoreboardManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        
        getLogger().info("PvPUP has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("PvPUP has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("pvpup")) return false;

        if (!sender.hasPermission("pvpup.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /pvpup <setlevel|level> <jugador> [nivel]");
            return true;
        }

        Player target = getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "level":
                int level = levelManager.getPlayerLevel(target);
                sender.sendMessage(ChatColor.GREEN + "Nivel de " + target.getName() + ": " + level);
                break;

            case "setlevel":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /pvpup setlevel <jugador> <nivel>");
                    return true;
                }
                try {
                    int newLevel = Integer.parseInt(args[2]);
                    if (newLevel < 1 || newLevel > getConfig().getInt("max-level", 20)) {
                        sender.sendMessage(ChatColor.RED + "Nivel inválido. Debe estar entre 1 y " + 
                            getConfig().getInt("max-level", 20));
                        return true;
                    }
                    levelManager.setLevel(target, newLevel);
                    sender.sendMessage(ChatColor.GREEN + "Nivel de " + target.getName() + 
                        " establecido a " + newLevel);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Nivel inválido.");
                }
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Comando desconocido. Usa setlevel o level.");
                break;
        }
        return true;
    }

    public static PvPUP getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}