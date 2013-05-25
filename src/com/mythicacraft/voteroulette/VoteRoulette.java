package com.mythicacraft.voteroulette;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mythicacraft.voteroulette.cmdexecutors.Commands;
import com.mythicacraft.voteroulette.listeners.VoteListener;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;

public class VoteRoulette extends JavaPlugin {

	public static Economy economy = null;
	public static Permission permission = null;
	private static boolean vaultEnabled = false;
	private static boolean hasPermPlugin = false;
	private static boolean hasEconPlugin = false;
	private static final Logger log = Logger.getLogger("VoteRoulette");
	FileConfiguration newConfig;
	static RewardManager rm = new RewardManager();

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
		loadRewards();
		loadMilestones();
		pm.registerEvents(new VoteListener(), this);
		getCommand("debugvote").setExecutor(new Commands());
		getCommand("vr").setExecutor(new Commands());
		getCommand("voteroulette").setExecutor(new Commands());
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
				log.warning("[VoteRoulette] No plugin to handle currency, cash rewards will not be given!");
				return true;
			}
			if(!setupPermissions()) {
				log.warning("[VoteRoulette] No plugin to handle permission groups, permission group reward settings will be ignored!");
				return true;
			}
		} else {
			log.warning("[VoteRoulette] Vault plugin not found. Currency and permission group reward settings will be ignored!");
			return false;
		}
		return true;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			hasEconPlugin = true;
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			hasPermPlugin = true;
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

	public static boolean vaultIsEnabled() {
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

	void loadRewards() {
		rm.clearRewards();
		ConfigurationSection cs = getConfig().getConfigurationSection("Rewards");
		if(cs != null) {
			for(String rewardName : cs.getKeys(false)) {
				ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
				if (rewardOptions != null) {
					rm.addReward(new Reward(rewardName, rewardOptions));
					System.out.println("[VR] Added Reward: " + rewardName);
					if(rewardName.equals(getConfig().getString("defaultReward"))) {
						rm.setDefaultReward(new Reward(rewardName, rewardOptions));
						System.out.println("[VR] Saved as default.");
					}
					continue;
				}
				log.warning("[VoteRoulette] The reward \"" + rewardName + "\" is empty! Skipping...");
			}
			if(rm.hasDefaultReward() == false && getConfig().getBoolean("giveRandomReward") == false) {
				log.warning("[VoteRoulette] The deafult reward coult not be matched to a reward and you have giveRandomReward set to false, players will NOT receive awards for votes.");
			}
			return;
		}
		log.severe("[VoteRoulette] Your reward section is empty, no rewards will be given!");
	}

	void loadMilestones() {
		rm.clearMilestones();
		ConfigurationSection cs = getConfig().getConfigurationSection("Milestones");
		if(cs != null) {
			for (String milestoneName : cs.getKeys(false)) {
				ConfigurationSection milestoneOptions = cs.getConfigurationSection(milestoneName);
				if (milestoneOptions != null) {
					if(milestoneOptions.contains("votes")) {
						rm.addMilestone(new Milestone(milestoneName, milestoneOptions));
						System.out.println("[VR] Added Milestone: " + milestoneName);
						continue;
					}
					log.warning("[VoteRoulette] Milestone \"" + milestoneName + "\" doesn't have a vote number set! Ignoring Milestone...");
					continue;
				}
				log.warning("[VoteRoulette] The reward \"" + milestoneName + "\" is empty! Skipping...");
			}
			return;
		}
		log.warning("[VoteRoulette] Your milestone section is empty, no milestones will be given!");
	}

	public static boolean hasPermPlugin() {
		return hasPermPlugin;
	}

	public static boolean hasEconPlugin() {
		return hasEconPlugin;
	}

	public static RewardManager getRewardManager() {
		return rm;
	}
}
