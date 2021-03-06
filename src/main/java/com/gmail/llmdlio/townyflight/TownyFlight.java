package com.gmail.llmdlio.townyflight;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.llmdlio.townyflight.config.TownyFlightConfig;
import com.gmail.llmdlio.townyflight.listeners.PlayerEnterTownListener;
import com.gmail.llmdlio.townyflight.listeners.PlayerJoinListener;
import com.gmail.llmdlio.townyflight.listeners.PlayerLeaveTownListener;
import com.gmail.llmdlio.townyflight.listeners.PlayerPVPListener;
import com.gmail.llmdlio.townyflight.listeners.TownUnclaimListener;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyFlight extends JavaPlugin {
	
	private final PlayerEnterTownListener playerEnterListener = new PlayerEnterTownListener(this);
	private final PlayerJoinListener playerJoinListener = new PlayerJoinListener(this);
	private final PlayerLeaveTownListener playerLeaveListener = new PlayerLeaveTownListener(this);	
	private final PlayerPVPListener playerPVPListener = new PlayerPVPListener(this);
	private final TownUnclaimListener townUnclaimListener = new TownUnclaimListener(this);

	public static String pluginPrefix;
	private static String flightOnMsg;
	private static String flightOffMsg;
	private static String noTownMsg;
	private static String notInTownMsg;
	private static String flightDeactivatedMsg;
	private static String flightDeactivatedPVPMsg;
	private static String noPermission;
	private static String notDuringWar;
	
	public static Boolean autoEnableFlight;
	public static Boolean autoEnableSilent;
	private static Boolean disableDuringWar;
	public static Boolean disableCombatPrevention;
	private static Boolean showPermissionInMessage;
	
	private TownyFlightConfig config = new TownyFlightConfig(this);
	
	public void onEnable() {
		
    	reloadConfig();  
		
    	if (!LoadSettings()) {
    		getLogger().severe("Config failed to load!");    		
    		this.getServer().getPluginManager().disablePlugin(this);
    		return;
    	}   	
	    	    
    	Plugin towny = getServer().getPluginManager().getPlugin("Towny");
    	if (getServer().getPluginManager().getPlugin("Towny").isEnabled()) {
    		String version = towny.getDescription().getVersion().substring(1, 4);
    		version = version.replace(".","");
    		Integer ver = Integer.parseInt(version);
    		if (ver < 92) {
    			getLogger().severe("Towny version inadequate: 0.92.0.0 or newer required.");    		
				this.getServer().getPluginManager().disablePlugin(this);
				return;
    		}
    	}   		

    	registerEvents();
	    getLogger().info(this.getDescription().getFullName() + " by LlmDl Enabled.");
	}
    
    public void onDisable() {
    	getLogger().info("TownyFlight Disabled.");
    }
    
    public void reloadConfig() { 
        if (!getDataFolder().exists())  
            getDataFolder().mkdirs(); 
        config.reload();       
    }
	
    private boolean LoadSettings() {		
		pluginPrefix = ChatColor.translateAlternateColorCodes('&', config.getConfig().getString("pluginPrefix"));

		// Language Strings
		flightOnMsg = config.getConfig().getConfigurationSection("language").getString("flightOnMsg");
		flightOffMsg = config.getConfig().getConfigurationSection("language").getString("flightOffMsg");
		noTownMsg = config.getConfig().getConfigurationSection("language").getString("noTownMsg");
		notInTownMsg = config.getConfig().getConfigurationSection("language").getString("notInTownMsg");
		flightDeactivatedMsg = config.getConfig().getConfigurationSection("language").getString("flightDeactivatedMsg");
		flightDeactivatedPVPMsg = config.getConfig().getConfigurationSection("language").getString("flightDeactivatedPVPMsg");
		noPermission = config.getConfig().getConfigurationSection("language").getString("noPermission");
		notDuringWar = config.getConfig().getConfigurationSection("language").getString("notDuringWar");

		// Options
		autoEnableFlight = config.getConfig().getConfigurationSection("options").getString("auto_Enable_Flight").equalsIgnoreCase("true");
		autoEnableSilent = config.getConfig().getConfigurationSection("options").getString("auto_Enable_Silent").equalsIgnoreCase("true");
		disableDuringWar = config.getConfig().getConfigurationSection("options").getString("disable_During_Wartime").equalsIgnoreCase("true");
		disableCombatPrevention = config.getConfig().getConfigurationSection("options").getString("disable_Combat_Prevention").equalsIgnoreCase("true");
	        showPermissionInMessage = config.getConfig().getConfigurationSection("options").getString("show_Permission_After_No_Permission_Message").equalsIgnoreCase("true");	
	    
		return true;		
	}
    
    private void registerEvents(){
    	final PluginManager pluginManager = getServer().getPluginManager();
    	HandlerList.unregisterAll(playerEnterListener);
    	if (autoEnableFlight)
    		pluginManager.registerEvents(playerEnterListener, this);
    	pluginManager.registerEvents(playerJoinListener, this);
    	pluginManager.registerEvents(playerLeaveListener, this);
    	pluginManager.registerEvents(playerPVPListener, this);
    	pluginManager.registerEvents(townUnclaimListener, this);
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("tfly")) {
			if (args.length == 0) {
				if (!(sender instanceof Player))
					return false;                
                if (!canFly((Player) sender, false))
                	return false;                
                toggleFlight((Player) sender, false, false, "");
                return true;
	        }
			
			if (args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("townyflight.command.tfly.reload")) {
					sender.sendMessage(pluginPrefix + ChatColor.RED + noPermission + ((showPermissionInMessage) ? "townyflight.command.tfly.reload" : "");
					return false;
				}
				config.reload();
		    	LoadSettings();
		    	registerEvents();
				sender.sendMessage(pluginPrefix + "Config.yml reloaded");
				return true;
			}
		}
		return false;
    }
    
    /** 
     * Returns true if a player can fly according to TownyFlight's rules.
     * 
     * @param player
     * @param silent - show messages to player.
     **/
    public static boolean canFly(Player player, boolean silent) {
    	if (player.hasPermission("townyflight.bypass"))
    		return true;    	
    	if (!player.hasPermission("townyflight.command.tfly")) {
    		if (!silent) player.sendMessage(pluginPrefix + ChatColor.RED + noPermission + "townyflight.command.tfly");
        	return false;
        }
    	try {
			Resident resident = null;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (NotRegisteredException e) {
				// Sometimes when a player joins for the first time, there can be a canFly test run before Towny has 
				// the chance to save the player properly.
				return false;				
			}
			if (disableDuringWar)
				if (TownyUniverse.isWarTime()) {
					if (!silent) player.sendMessage(pluginPrefix + notDuringWar);
					return false;
				}
			if (!resident.hasTown()) {
				if (!silent) player.sendMessage(pluginPrefix + noTownMsg);
				return false;
			}        
			if (TownyUniverse.isWilderness(player.getLocation().getBlock())) {
				if (!silent) player.sendMessage(pluginPrefix + notInTownMsg);
				return false;
			}        
			Town town = null;
			town = TownyUniverse.getTownBlock(player.getLocation()).getTown();
			if (!resident.getTown().equals(town)) {
				if (!silent) player.sendMessage(pluginPrefix + notInTownMsg);
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
    }

    /**
     * If flight is on, turn it off and vice versa
     * 
     * @param player
     * @param silent - show messages to player
     * @param forced - whether this is a forced deactivation or not
     * @param cause - cause of disabling flight
     */    
    public static void toggleFlight(Player player, boolean silent, boolean forced, String cause) {    	    	
    	if (player.getAllowFlight()) {
    		if (!silent) {
    			if (forced) {
    				String reason = flightDeactivatedMsg;
    				if (cause == "pvp") {
    					reason = flightDeactivatedPVPMsg;    						
    				}
    				player.sendMessage(pluginPrefix + reason + flightOffMsg);
    			} else {
    				player.sendMessage(pluginPrefix + flightOffMsg);
    			}
    		}
    		if (player.isFlying())
    			player.setFallDistance(-100000);
    		player.setAllowFlight(false);
    	} else {
    		if (!silent)
    			player.sendMessage(pluginPrefix + flightOnMsg);
    		player.setAllowFlight(true);
    	}    		
    }  
}
