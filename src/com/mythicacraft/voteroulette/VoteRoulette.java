package com.mythicacraft.voteroulette;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mythicacraft.voteroulette.cmdexecutors.Commands;
import com.mythicacraft.voteroulette.listeners.VoteHandler;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;

public class VoteRoulette extends JavaPlugin {

	private static final Logger log = Logger.getLogger("VoteRoulette");

	public static Economy economy = null;
	public static Permission permission = null;
	private static boolean vaultEnabled = false;
	private static boolean hasPermPlugin = false;
	private static boolean hasEconPlugin = false;

	private static RewardManager rm = new RewardManager();

	//config constants
	public boolean REWARDS_ON_THRESHOLD;
	public int VOTE_THRESHOLD;
	public boolean MESSAGE_PLAYER;
	public boolean BROADCAST_TO_SERVER;
	public boolean GIVE_RANDOM_REWARD;
	public boolean GIVE_RANDOM_MILESTONE;
	public boolean ONLY_MILESTONE_ON_COMPLETION;
	public boolean BLACKLIST_AS_WHITELIST;
	public Player[] BLACKLIST_PLAYERS;

	//localizations constants
	public String SERVER_VOTE_CONFIRMATION;
	public String PLAYER_VOTE_CONFIRMATION;
	public String REWARD_MESSAGE;
	public String NO_PERMS_SELF_MESSAGE;
	public String NO_PERMS_OTHERS_MESSAGE;



	public void onDisable() {
		log.info("[VoteRoulette] Disabled!");
	}

	public void onEnable() {

		//check for votifier
		if(!setupVotifier()) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		//check for and setup vault
		if(setupVault()) {
			vaultEnabled = true;
		}

		//register events and commands
		getServer().getPluginManager().registerEvents(new VoteHandler(this), this);
		getCommand("debugvote").setExecutor(new Commands(this));
		getCommand("vr").setExecutor(new Commands(this));
		getCommand("voteroulette").setExecutor(new Commands(this));

		//load configs
		reloadConfigs();

		log.info("[VoteRoulette] Enabled!");
	}

	private boolean setupVotifier() {
		System.out.println("[VoteRoulette] Checking for Votifier...");
		Plugin votifier =  getServer().getPluginManager().getPlugin("Votifier");
		if (!(votifier != null && votifier instanceof com.vexsoftware.votifier.Votifier)) {
			log.severe("[VoteRoulette] Votifier was not found! Voltifer is required for VoteRoulette to work!");
			return false;
		}
		System.out.println("[VoteRoulette] ...found Votifier!");
		return true;
	}

	private boolean setupVault() {
		System.out.println("[VoteRoulette] Checking for Vault...");
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
		System.out.println("[VoteRoulette] ...found Vault!");
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

	public void reloadConfigs() {
		System.out.println("[VoteRoulette] Loading configs...");
		loadConfig();
		loadConfigOptions();
		loadPlayerData();
		loadLocalizations();
		loadRewards();
		loadMilestones();
		System.out.println("[VoteRoulette] ...finished loading configs!");
	}

	private void loadConfig() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		File configFile = new File(pluginFolder, "config.yml");
		if(!configFile.exists()) {
			saveResource("config.yml", true);
			return;
		}
		try {
			reloadConfig();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while loading VoteRoulette/config.yml", e);
			pm.disablePlugin(this);
		}
	}

	void loadPlayerData() {
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

	private void loadLocalizations() {
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

	private void loadConfigOptions() {
		REWARDS_ON_THRESHOLD = getConfig().getBoolean("giveRewardsOnThreshold");

		VOTE_THRESHOLD = getConfig().getInt("voteThreshold");

		MESSAGE_PLAYER = getConfig().getBoolean("messagePlayer");

		BROADCAST_TO_SERVER = getConfig().getBoolean("broadcastToServer");

		GIVE_RANDOM_REWARD = getConfig().getBoolean("giveRandomReward");

		GIVE_RANDOM_MILESTONE = getConfig().getBoolean("giveRandomMilestone");

		ONLY_MILESTONE_ON_COMPLETION = getConfig().getBoolean("onlyRewardMilestoneUponCompletion");

		BLACKLIST_AS_WHITELIST = getConfig().getBoolean("useBlacklistAsWhitelist");

		BLACKLIST_PLAYERS = Utils.getBlacklistPlayers();
	}

	private void loadRewards() {
		rm.clearRewards();
		ConfigurationSection cs = getConfig().getConfigurationSection("Rewards");
		if(cs != null) {
			System.out.println("[VoteRoulette] Loading rewards...");
			for(String rewardName : cs.getKeys(false)) {
				ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
				if (rewardOptions != null) {
					Reward newReward = new Reward(rewardName, rewardOptions);
					rm.addReward(newReward);
					System.out.println("[VoteRoulette] Added Reward: " + rewardName);
					if(rewardName.equals(getConfig().getString("defaultReward"))) {
						rm.setDefaultReward(newReward);
						System.out.println("[VoteRoulette] + \"" + rewardName + "\" saved as default reward.");
					}
					continue;
				}
				log.warning("[VoteRoulette] The reward \"" + rewardName + "\" is empty! Skipping reward.");
			}
			if(rm.hasDefaultReward() == false && getConfig().getBoolean("giveRandomReward") == false) {
				log.warning("[VoteRoulette] The deafult reward name could not be matched to a reward and you have giveRandomReward set to false, players will NOT receive awards for votes.");
			}
			System.out.println("[VoteRoulette] ...Finished loading rewards!");
		} else {
			log.severe("[VoteRoulette] Your reward section is empty, no rewards will be given!");
		}
	}

	private void loadMilestones() {
		rm.clearMilestones();
		ConfigurationSection cs = getConfig().getConfigurationSection("Milestones");
		if(cs != null) {
			System.out.println("[VoteRoulette] Loading milestones...");
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
			System.out.println("[VoteRoulette] ... Finished loading milestones!");
		} else {
			log.warning("[VoteRoulette] Your milestone section is empty, no milestones will be given!");
		}
	}

	public static boolean vaultIsEnabled() {
		return vaultEnabled;
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
