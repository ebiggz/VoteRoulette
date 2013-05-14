package com.mythicacraft.voteroulette;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mythicacraft.voteroulette.listeners.VoteListener;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;

public class VoteRoulette extends JavaPlugin {

	public Economy economy = null;
	public static Permission permission = null;
	private boolean vaultEnabled = false;
	private static final Logger log = Logger.getLogger("VoteRoulette");
	FileConfiguration newConfig;
	RewardManager rm = new RewardManager(this);

	public void onDisable() {
		log.info("[VoteRoulette] Disabled!");
	}

	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		if(!setupVotifier()) {
			pm.disablePlugin(this);
			return;
		}
		if(setupVault()) {
			vaultEnabled = true;
		}
		loadConfig();
		loadPlayerData();
		loadLocalizations();
		rm.loadRewards();
		rm.loadMilestones();
		pm.registerEvents(new VoteListener(this), this);
		rm.printRewards();
		log.info("[VoteRoulette] Enabled!");
	}

	private boolean setupVotifier() {
		Plugin votifier =  getServer().getPluginManager().getPlugin("Votifier");
		if (!(votifier != null && votifier instanceof com.vexsoftware.votifier.Votifier)) {
			log.severe("[VoteRoulette] Votifier was not found!");
			return false;
		}
		return true;
	}

	private boolean setupVault() {
		Plugin vault =  getServer().getPluginManager().getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			if(!setupEconomy()) {
				log.warning("[VoteRoulette] No plugin to handle cash, cash rewards will NOT be given!");
				return false;
			}
			if(!setupPermissions()) {
				log.warning("[VoteRoulette] No plugin to handle permission groups, perm group settings will be igored!");
				return false;
			}
		} else {
			log.warning("[VoteRoulette] Vault plugin not found, cash rewards will NOT be given!");
			return false;
		}
		return true;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

	public boolean isVaultEnabled() {
		return vaultEnabled;
	}

	public void loadConfig() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		File configFile = new File(pluginFolder, "config.yml");
		if(!configFile.exists()) {
			this.saveResource("config.yml", true);
			return;
		}
		try {
			reloadConfig();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while loading VoteRoulette/config.yml", e);
			pm.disablePlugin(this);
		}
	}

	public void loadPlayerData() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		String playerFolder = pluginFolder + File.separator + "data";
		(new File(playerFolder)).mkdirs();
		File playerDataFile = new File(playerFolder, "players.yml");
		ConfigAccessor playerData = new ConfigAccessor("players.yml");

		if (!playerDataFile.exists()) {
			try {
				playerData.saveDefaultConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/players.yml", e);
				pm.disablePlugin(this);
			}
			return;
		} else {
			try {
				playerData.getConfig().options().header("You do NOT need to touch this file!");
				playerData.getConfig().options().copyHeader();
				playerData.reloadConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/players.yml", e);
				pm.disablePlugin(this);
			}
		}
	}

	public void loadLocalizations() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		String playerFolder = pluginFolder + File.separator + "data";
		(new File(playerFolder)).mkdirs();
		File localizationsFile = new File(playerFolder, "localizations.yml");
		ConfigAccessor localizations = new ConfigAccessor("localizations.yml");

		if(!localizationsFile.exists()) {
			try {
				localizations.saveDefaultConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/localizations.yml", e);
				pm.disablePlugin(this);
			}
		} else {
			try {
				localizations.reloadConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/localizations.yml", e);
				pm.disablePlugin(this);
			}
		}
	}
}
