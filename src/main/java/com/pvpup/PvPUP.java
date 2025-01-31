package com.pvpup;

import com.pvpup.managers.*;
import com.pvpup.listeners.CombatListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.Arrays;

public class PvPUP extends JavaPlugin {
    private static PvPUP instance;
    private DatabaseManager databaseManager;
    private LevelManager levelManager;
    private ScoreboardManager scoreboardManager;
    private SafeZoneManager safeZoneManager;
    private ArenaManager arenaManager;
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
        safeZoneManager = new SafeZoneManager(this);
        arenaManager = new ArenaManager(this);
        
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

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpup.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 1) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "arena":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /pvpup arena <create|delete|list|tp|setspawn> [nombre]");
                    return true;
                }
                handleArenaCommand(player, Arrays.copyOfRange(args, 1, args.length));
                return true;

            case "pos1":
                safeZoneManager.setPos1(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Posición 1 de la zona segura establecida.");
                return true;

            case "pos2":
                safeZoneManager.setPos2(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Posición 2 de la zona segura establecida.");
                return true;

            case "level":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /pvpup level <jugador>");
                    return true;
                }
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return true;
                }
                int level = levelManager.getPlayerLevel(target);
                player.sendMessage(ChatColor.GREEN + "Nivel de " + target.getName() + ": " + level);
                return true;

            case "setlevel":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /pvpup setlevel <jugador> <nivel>");
                    return true;
                }
                Player targetPlayer = getServer().getPlayer(args[1]);
                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return true;
                }
                try {
                    int newLevel = Integer.parseInt(args[2]);
                    if (newLevel < 1 || newLevel > getConfig().getInt("max-level", 20)) {
                        player.sendMessage(ChatColor.RED + "Nivel inválido. Debe estar entre 1 y " + 
                            getConfig().getInt("max-level", 20));
                        return true;
                    }
                    levelManager.setLevel(targetPlayer, newLevel);
                    player.sendMessage(ChatColor.GREEN + "Nivel de " + targetPlayer.getName() + 
                        " establecido a " + newLevel);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Nivel inválido.");
                }
                return true;

            default:
                showHelp(player);
                return true;
        }
    }

    private void handleArenaCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Uso: /pvpup arena <create|delete|list|tp|setspawn> [nombre]");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /pvpup arena create <nombre>");
                    return;
                }
                arenaManager.createArena(args[1], player.getLocation(), player.getLocation(), player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Arena '" + args[1] + "' creada. Usa pos1, pos2 y setspawn para configurarla.");
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /pvpup arena delete <nombre>");
                    return;
                }
                arenaManager.removeArena(args[1]);
                player.sendMessage(ChatColor.GREEN + "Arena '" + args[1] + "' eliminada.");
                break;

            case "list":
                player.sendMessage(ChatColor.YELLOW + "Arenas disponibles:");
                for (String arenaName : arenaManager.getArenaNames()) {
                    player.sendMessage(ChatColor.GRAY + "- " + arenaName);
                }
                break;

            case "tp":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /pvpup arena tp <nombre>");
                    return;
                }
                if (arenaManager.teleportToArena(player, args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Has sido teletransportado a la arena '" + args[1] + "'.");
                } else {
                    player.sendMessage(ChatColor.RED + "Arena no encontrada o spawn no configurado.");
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Comando de arena desconocido. Usa create, delete, list o tp.");
                break;
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Comandos de PvPUP:");
        player.sendMessage(ChatColor.GRAY + "/pvpup level <jugador> " + ChatColor.WHITE + "- Ver nivel de un jugador");
        player.sendMessage(ChatColor.GRAY + "/pvpup setlevel <jugador> <nivel> " + ChatColor.WHITE + "- Establecer nivel");
        player.sendMessage(ChatColor.GRAY + "/pvpup pos1 " + ChatColor.WHITE + "- Establecer posición 1 de zona segura");
        player.sendMessage(ChatColor.GRAY + "/pvpup pos2 " + ChatColor.WHITE + "- Establecer posición 2 de zona segura");
        player.sendMessage(ChatColor.GRAY + "/pvpup arena create <nombre> " + ChatColor.WHITE + "- Crear una arena");
        player.sendMessage(ChatColor.GRAY + "/pvpup arena delete <nombre> " + ChatColor.WHITE + "- Eliminar una arena");
        player.sendMessage(ChatColor.GRAY + "/pvpup arena list " + ChatColor.WHITE + "- Listar arenas");
        player.sendMessage(ChatColor.GRAY + "/pvpup arena tp <nombre> " + ChatColor.WHITE + "- Ir a una arena");
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

    public SafeZoneManager getSafeZoneManager() {
        return safeZoneManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}