package com.mythicacraft.voteroulette;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import com.mythicacraft.voteroulette.cmdexecutors.Commands;
import com.mythicacraft.voteroulette.listeners.LoginListener;
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

	private static RewardManager rm;
	private static PlayerManager pm;
	private BukkitRunnable periodicReminder;
	public static List<Player> notifiedPlayers = new ArrayList<Player>();

	//config constants
	public boolean REWARDS_ON_THRESHOLD;
	public boolean USE_DATABASE = false;
	public int VOTE_THRESHOLD;
	public boolean MESSAGE_PLAYER;
	public boolean BROADCAST_TO_SERVER;
	public boolean ONLY_BROADCAST_ONLINE;
	public boolean LOG_TO_CONSOLE;
	public boolean ONLY_PRIMARY_GROUP;
	public boolean GIVE_RANDOM_REWARD;
	public boolean GIVE_RANDOM_MILESTONE;
	public boolean ONLY_MILESTONE_ON_COMPLETION;
	public boolean CONSIDER_REWARDS_FOR_CURRENT_WORLD = true;
	public boolean CONSIDER_MILESTONES_FOR_CURRENT_WORLD = false;
	public boolean BLACKLIST_AS_WHITELIST;
	public List<String> BLACKLIST_PLAYERS;
	public List<String> BLACKLIST_WORLDS;
	public boolean USE_PERIODIC_REMINDER;
	public boolean USE_TWENTYFOUR_REMINDER;
	public long REMINDER_INTERVAL;
	public double CONFIG_VERSION;

	//messages constants
	public String SERVER_BROADCAST_MESSAGE;
	public String PLAYER_VOTE_MESSAGE;
	public String PERIODIC_REMINDER;
	public String TWENTYFOUR_REMINDER;
	public List<String> VOTE_WEBSITES;

	public void onDisable() {
		log.info("[VoteRoulette] Disabled!");
	}

	public void onEnable() {

		pm = new PlayerManager(this);
		rm = new RewardManager(this);

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
		getServer().getPluginManager().registerEvents(new LoginListener(this), this);

		getCommand("vr").setExecutor(new Commands(this));
		getCommand("vtr").setExecutor(new Commands(this));
		getCommand("voteroulette").setExecutor(new Commands(this));
		getCommand("vote").setExecutor(new Commands(this));

		//load configs
		reloadConfigs();

		if(CONFIG_VERSION != 1.3) {
			log.warning("[VoteRoulette] It appears that your config is out of date. There's new options! It's recommended that you take your old config out to let the new one save.");
		}

		log.info("[VoteRoulette] Enabled!");
	}

	private boolean setupVotifier() {
		Plugin votifier =  getServer().getPluginManager().getPlugin("Votifier");
		if (!(votifier != null && votifier instanceof com.vexsoftware.votifier.Votifier)) {
			log.severe("[VoteRoulette] Votifier was not found! Voltifer is required for VoteRoulette to work!");
			return false;
		}
		System.out.println("[VoteRoulette] Hooked into Votifier!");
		return true;
	}

	private boolean setupVault() {
		Plugin vault =  getServer().getPluginManager().getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			System.out.println("[VoteRoulette] Hooked into Vault!");
			if(!setupEconomy()) {
				log.warning("[VoteRoulette] No plugin to handle currency, cash rewards will not be given!");
			}
			if(!setupPermissions()) {
				log.warning("[VoteRoulette] No plugin to handle permission groups, permission group reward settings will be ignored!");
			}
			return true;
		} else {
			log.warning("[VoteRoulette] Vault plugin not found. Currency and permission group reward settings will be ignored!");
			return false;
		}
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
		loadMessagesFile();
		loadMessagesData();
		loadPlayerData();
		loadRewards();
		loadMilestones();
		System.out.println("[VoteRoulette] ...finished loading configs!");
		scheduleTasks();
	}

	void scheduleTasks() {

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.cancelTasks(this);

		if(USE_PERIODIC_REMINDER) {
			periodicReminder = new PeriodicReminder(PERIODIC_REMINDER.replace("%server%", Bukkit.getServerName()));
			scheduler.scheduleSyncRepeatingTask(this, periodicReminder,REMINDER_INTERVAL, REMINDER_INTERVAL);
		}

		if(USE_TWENTYFOUR_REMINDER) {
			scheduler.scheduleSyncRepeatingTask(this, new TwentyFourHourCheck(TWENTYFOUR_REMINDER), 6000, 6000);
		}
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

	void loadMessagesFile() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		File messagesFile = new File(pluginFolder, "messages.yml");
		ConfigAccessor messageData = new ConfigAccessor("messages.yml");

		if(!messagesFile.exists()) {
			saveResource("messages.yml", true);
			return;
		}
		try {
			messageData.reloadConfig();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while loading VoteRoulette/messages.yml", e);
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
		ConfigAccessor playerData = new ConfigAccessor("data" + File.separator + "players.yml");

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

	void loadMessagesData() {
		ConfigAccessor messageData = new ConfigAccessor("messages.yml");

		SERVER_BROADCAST_MESSAGE = ChatColor.translateAlternateColorCodes('&', messageData.getConfig().getString("server-broadcast-message"));

		PLAYER_VOTE_MESSAGE = ChatColor.translateAlternateColorCodes('&', messageData.getConfig().getString("player-reward-message"));

		PERIODIC_REMINDER = ChatColor.translateAlternateColorCodes('&', messageData.getConfig().getString("periodic-reminder"));

		TWENTYFOUR_REMINDER = ChatColor.translateAlternateColorCodes('&', messageData.getConfig().getString("twentyfour-hour-reminder", "&b24 hours have passed since your last vote!"));

		VOTE_WEBSITES = messageData.getConfig().getStringList("vote-websites");
	}

	private void loadConfigOptions() {
		REWARDS_ON_THRESHOLD = getConfig().getBoolean("giveRewardsOnThreshold");

		VOTE_THRESHOLD = getConfig().getInt("voteThreshold");

		MESSAGE_PLAYER = getConfig().getBoolean("messagePlayer");

		BROADCAST_TO_SERVER = getConfig().getBoolean("broadcastToServer");

		ONLY_BROADCAST_ONLINE = getConfig().getBoolean("onlyBroadcastOnlinePlayerVotes", false);

		LOG_TO_CONSOLE = getConfig().getBoolean("logToConsole");

		GIVE_RANDOM_REWARD = getConfig().getBoolean("giveRandomReward");

		GIVE_RANDOM_MILESTONE = getConfig().getBoolean("giveRandomMilestone");

		ONLY_MILESTONE_ON_COMPLETION = getConfig().getBoolean("onlyRewardMilestoneUponCompletion");

		BLACKLIST_AS_WHITELIST = getConfig().getBoolean("useBlacklistAsWhitelist");

		USE_TWENTYFOUR_REMINDER = getConfig().getBoolean("useTwentyFourHourReminder", true);

		USE_PERIODIC_REMINDER = getConfig().getBoolean("usePeriodicReminder");

		REMINDER_INTERVAL = getConfig().getLong("periodicReminderInterval")*1200;

		BLACKLIST_PLAYERS = Utils.getBlacklistPlayers();

		BLACKLIST_WORLDS = getConfig().getStringList("blacklistedWorlds");

		CONSIDER_REWARDS_FOR_CURRENT_WORLD = getConfig().getBoolean("considerRewardsForPlayersCurrentWorld");

		CONSIDER_MILESTONES_FOR_CURRENT_WORLD = getConfig().getBoolean("considerMilestonesForPlayersCurrentWorld");

		ONLY_PRIMARY_GROUP = getConfig().getBoolean("onlyConsiderPlayersPrimaryGroup", false);

		CONFIG_VERSION = getConfig().getDouble("configVersion", 1.0);
	}

	private void loadRewards() {
		rm.clearRewards();
		ConfigurationSection cs = getConfig().getConfigurationSection("Rewards");
		if(cs != null) {
			for(String rewardName : cs.getKeys(false)) {
				ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
				if (rewardOptions != null) {
					Reward newReward = new Reward(rewardName, rewardOptions);
					rm.addReward(newReward);
					System.out.println("[VoteRoulette] Added Reward: " + rewardName);
					if(rewardName.equals(getConfig().getString("defaultReward"))) {
						rm.setDefaultReward(newReward);
						System.out.println("[VoteRoulette] \"" + rewardName + "\" saved as default reward.");
					}
					continue;
				}
				log.warning("[VoteRoulette] The reward \"" + rewardName + "\" is empty! Skipping reward.");
			}
			if(!rm.hasDefaultReward() && !getConfig().getBoolean("giveRandomReward")) {
				log.warning("[VoteRoulette] The deafult reward name could not be matched to a reward and you have giveRandomReward set to false, players will NOT receive awards for votes.");
			}
		} else {
			log.warning("[VoteRoulette] Your reward section is empty, no rewards will be given!");
		}
	}

	private void loadMilestones() {
		rm.clearMilestones();
		ConfigurationSection cs = getConfig().getConfigurationSection("Milestones");
		if(cs != null) {
			for (String milestoneName : cs.getKeys(false)) {
				ConfigurationSection milestoneOptions = cs.getConfigurationSection(milestoneName);
				if (milestoneOptions != null) {
					if(milestoneOptions.contains("votes")) {
						rm.addMilestone(new Milestone(milestoneName, milestoneOptions));
						System.out.println("[VoteRoulette] Added Milestone: " + milestoneName);
						continue;
					}
					log.warning("[VoteRoulette] Milestone \"" + milestoneName + "\" doesn't have a vote number set! Ignoring Milestone...");
					continue;
				}
				log.warning("[VoteRoulette] The reward \"" + milestoneName + "\" is empty! Skipping...");
			}
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

	public static PlayerManager getPlayerManager() {
		return pm;
	}

	private class PeriodicReminder extends BukkitRunnable {

		private String message;

		PeriodicReminder(String message) {
			this.message = message;
		}

		@Override
		public void run() {
			Bukkit.broadcastMessage(message);
		}
	}

	private class TwentyFourHourCheck extends BukkitRunnable {

		private String message;

		TwentyFourHourCheck(String message) {
			this.message = message;
		}

		@Override
		public void run() {
			Player[] onlinePlayers = Bukkit.getOnlinePlayers();
			for(Player player : onlinePlayers) {
				if(VoteRoulette.getPlayerManager().playerHasntVotedInADay(player.getName())) {
					if(!VoteRoulette.notifiedPlayers.contains(player)) {
						player.sendMessage(ChatColor.AQUA + "[VoteRoulette] " + ChatColor.RESET + message.replace("%player%", player.getName()));
						VoteRoulette.notifiedPlayers.add(player);
					}
				}
			}
		}
	}
}
