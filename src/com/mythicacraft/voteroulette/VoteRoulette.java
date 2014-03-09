package com.mythicacraft.voteroulette;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.mythicacraft.voteroulette.cmdexecutors.Commands;
import com.mythicacraft.voteroulette.listeners.LoginListener;
import com.mythicacraft.voteroulette.listeners.VoteListener;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;

public class VoteRoulette extends JavaPlugin {

	private static final Logger log = Logger.getLogger("VoteRoulette");

	public static Economy economy = null;
	public static Permission permission = null;
	private static boolean vaultEnabled = false;
	private static boolean hasPermPlugin = false;
	private static boolean hasEconPlugin = false;
	private boolean hasUpdate = false;

	private static RewardManager rm;
	private static PlayerManager pm;
	private BukkitRunnable periodicReminder;
	private BukkitRunnable twentyFourHourChecker;
	private BukkitRunnable updateChecker;
	public static List<Player> notifiedPlayers = new ArrayList<Player>();
	public static List<DelayedCommand> delayedCommands = new ArrayList<DelayedCommand>();

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
	public boolean RANDOMIZE_SAME_PRIORITY;
	public long REMINDER_INTERVAL;
	public boolean CHECK_UPDATES;
	public double CONFIG_VERSION;

	//messages constants
	public String SERVER_BROADCAST_MESSAGE;
	public String PLAYER_VOTE_MESSAGE;
	public String PERIODIC_REMINDER;
	public String TWENTYFOUR_REMINDER;
	public List<String> VOTE_WEBSITES;
	public double MESSAGES_VERSION;

	//localizations constants
	public String UNCLAIMED_AWARDS_NOTIFICATION;
	public String NO_UNCLAIMED_AWARDS_NOTIFICATION;
	public String BLACKLISTED_WORLD_NOTIFICATION;
	public String WRONG_AWARD_WORLD_NOTIFICATION;
	public String INVENTORY_FULL_NOTIFICATION;
	public String NO_PERM_NOTIFICATION;
	public String BASE_CMD_NOTIFICATION;
	public String REROLL_NOTIFICATION;
	public String REROLL_FAILED_NOTIFICATION;
	public double LOCALIZATIONS_VERSION;


	public void onDisable() {

		if(periodicReminder != null) {
			periodicReminder.cancel();
		}
		if(twentyFourHourChecker != null) {
			twentyFourHourChecker.cancel();
		}
		if(updateChecker != null) {
			updateChecker.cancel();
		}

		for(int i = 0; i < delayedCommands.size(); i++) {
			DelayedCommand dCmd = delayedCommands.get(i);
			if(dCmd.shouldRunOnShutdown()) {
				dCmd.run();
				dCmd.cancel();
			}
		}

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
		getServer().getPluginManager().registerEvents(new VoteListener(this), this);
		getServer().getPluginManager().registerEvents(new LoginListener(this), this);

		getCommand("voteroulette").setExecutor(new Commands(this));
		getCommand("votelinks").setExecutor(new Commands(this));

		//load configs
		reloadConfigs();

		//check file versions
		if(CONFIG_VERSION != 1.5) {
			log.warning("[VoteRoulette] It appears that your config is out of date. There may be new options! It's recommended that you take your old config out to let the new one save.");
		}

		if(MESSAGES_VERSION != 1.0) {
			log.warning("[VoteRoulette] It appears that your messages.yml file is out of date. There may be new options! It's recommended that you take your old messages file out to let the new one save.");
		}

		if(LOCALIZATIONS_VERSION != 1.0) {
			log.warning("[VoteRoulette] It appears that your localizations.yml file is out of date. There may be new options! It's recommended that you take your old localizations file out to let the new one save.");
		}

		log.info("[VoteRoulette] Enabled!");
	}

	private boolean setupVotifier() {
		Plugin votifier =  getServer().getPluginManager().getPlugin("Votifier");
		if (!(votifier != null && votifier instanceof com.vexsoftware.votifier.Votifier)) {
			log.severe("[VoteRoulette] Votifier was not found! Votifier is required for VoteRoulette to work!");
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
		this.reloadConfig();
		loadConfigOptions();
		loadMessagesFile();
		loadMessagesData();
		loadLocalizationsFile();
		loadLocalizationsData();
		loadPlayerData();
		loadRewards();
		loadMilestones();
		loadKnownSitesFile();
		System.out.println("[VoteRoulette] ...finished loading configs!");
		scheduleTasks();
	}

	void scheduleTasks() {

		if(periodicReminder != null) {
			periodicReminder.cancel();
		}
		if(twentyFourHourChecker != null) {
			twentyFourHourChecker.cancel();
		}
		if(updateChecker != null) {
			updateChecker.cancel();
		}

		if(USE_PERIODIC_REMINDER) {
			periodicReminder = new PeriodicReminder(PERIODIC_REMINDER);
			periodicReminder.runTaskTimer(this, REMINDER_INTERVAL, REMINDER_INTERVAL);
		}

		if(USE_TWENTYFOUR_REMINDER) {
			twentyFourHourChecker = new TwentyFourHourCheck(TWENTYFOUR_REMINDER);
			twentyFourHourChecker.runTaskTimer(this, 12000, 12000);
		}

		if (CHECK_UPDATES) {
			if (getDescription().getVersion().contains("SNAPSHOT")) {
				getLogger().info("This is not a release version. Automatic update checking will be disabled.");
			} else {
				updateChecker = new UpdateChecker(this);
				updateChecker.runTaskTimerAsynchronously(this, 40, 432000);
			}
		}
	}

	private void loadConfig() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		File configFile = new File(pluginFolder, "config.yml");
		if(!configFile.exists()) {
			saveResource("config.yml", true);
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

	void loadLocalizationsFile() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath() + File.separator + "data";
		(new File(pluginFolder)).mkdirs();
		File localizationsFile = new File(pluginFolder, "localizations.yml");
		ConfigAccessor localizationsData = new ConfigAccessor("data" + File.separator + "localizations.yml");

		if(!localizationsFile.exists()) {
			saveResource("data" + File.separator + "localizations.yml", true);
			return;
		}
		try {
			localizationsData.reloadConfig();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/localizations.yml", e);
			pm.disablePlugin(this);
		}
	}

	void loadLocalizationsData() {
		ConfigAccessor localeData = new ConfigAccessor("data" + File.separator + "localizations.yml");

		UNCLAIMED_AWARDS_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("unclaimed-awards"));

		NO_UNCLAIMED_AWARDS_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("no-unclaimed-awards"));

		BLACKLISTED_WORLD_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("blacklisted-world"));

		WRONG_AWARD_WORLD_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("award-wrong-world"));

		INVENTORY_FULL_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("award-inventory-full"));

		NO_PERM_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("no-permission"));

		BASE_CMD_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("base-command-text"));

		REROLL_FAILED_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("reroll-failed"));

		REROLL_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("rerolling"));

		LOCALIZATIONS_VERSION = localeData.getConfig().getDouble("config-version", 1.0);


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

	void loadKnownSitesFile() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		String playerFolder = pluginFolder + File.separator + "data";
		(new File(playerFolder)).mkdirs();
		File playerDataFile = new File(playerFolder, "known websites.yml");
		ConfigAccessor playerData = new ConfigAccessor("data" + File.separator + "known websites.yml");

		if (!playerDataFile.exists()) {
			try {
				playerData.saveDefaultConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/known websites.yml", e);
				pm.disablePlugin(this);
			}
			return;
		} else {
			try {
				playerData.getConfig().options().header("This file collects all the known voting websites that have been used on your server. Every time a vote comes through and if the website hasnt been saved before, the website is added here.");
				playerData.getConfig().options().copyHeader();
				playerData.reloadConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/known websites.yml", e);
				pm.disablePlugin(this);
			}
		}
	}

	void loadMessagesData() {
		ConfigAccessor messageData = new ConfigAccessor("messages.yml");

		SERVER_BROADCAST_MESSAGE = Utils.transcribeColorCodes(messageData.getConfig().getString("server-broadcast-message"));

		PLAYER_VOTE_MESSAGE = Utils.transcribeColorCodes(messageData.getConfig().getString("player-reward-message"));

		PERIODIC_REMINDER = Utils.transcribeColorCodes(messageData.getConfig().getString("periodic-reminder").replace("%server%", Bukkit.getServerName()));

		TWENTYFOUR_REMINDER = Utils.transcribeColorCodes(messageData.getConfig().getString("twentyfour-hour-reminder", "&b24 hours have passed since your last vote!"));

		List<String> voteSites = messageData.getConfig().getStringList("vote-websites");
		for(int i = 0; i < voteSites.size(); i++) {
			String website = voteSites.get(i);
			website = Utils.transcribeColorCodes(website);
			voteSites.set(i, website);
		}
		VOTE_WEBSITES = voteSites;

		MESSAGES_VERSION = messageData.getConfig().getDouble("config-version", 1.0);
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

		RANDOMIZE_SAME_PRIORITY = getConfig().getBoolean("randomizeTiedHighestPriorityMilestones", false);

		ONLY_MILESTONE_ON_COMPLETION = getConfig().getBoolean("onlyRewardMilestoneUponCompletion");

		BLACKLIST_AS_WHITELIST = getConfig().getBoolean("useBlacklistAsWhitelist");

		USE_TWENTYFOUR_REMINDER = getConfig().getBoolean("useTwentyFourHourReminder", true);

		USE_PERIODIC_REMINDER = getConfig().getBoolean("usePeriodicReminder");

		REMINDER_INTERVAL = getConfig().getLong("periodicReminderInterval")*1200;

		BLACKLIST_PLAYERS = Utils.getBlacklistPlayers();

		BLACKLIST_WORLDS = getConfig().getStringList("blacklistedWorlds");

		CHECK_UPDATES = getConfig().getBoolean("checkForUpdates", true);

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
				log.warning("[VoteRoulette] The default reward name could not be matched to a reward and you have giveRandomReward set to false, players will NOT receive awards for votes.");
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

	public boolean hasUpdate() {
		return hasUpdate;
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

	private class UpdateChecker extends BukkitRunnable {

		private String CURRENT_VERSION;

		private final VoteRoulette plugin;

		private UpdateChecker(VoteRoulette plugin) {
			this.plugin = plugin;
			CURRENT_VERSION = plugin.getDescription().getVersion();
		}

		@Override
		public void run() {
			final File pluginsFolder = this.plugin.getDataFolder().getParentFile();
			final File updaterFolder = new File(pluginsFolder, "Updater");
			final File updaterConfigFile = new File(updaterFolder, "config.yml");
			String apiKey = null;
			String latest = null;

			if (updaterFolder.exists()) {
				if (updaterConfigFile.exists()) {
					final YamlConfiguration config = YamlConfiguration.loadConfiguration(updaterConfigFile);
					apiKey = config.getString("api-key");
				}
			}

			URL url;
			try {
				url = new URL("https://api.curseforge.com/servermods/files?projectIds=71726");
			} catch (final Exception e) {
				return;
			}

			URLConnection conn;
			try {
				conn = url.openConnection();

				conn.setConnectTimeout(5000);
				if (apiKey != null) {
					conn.addRequestProperty("X-API-Key", apiKey);
				}
				conn.addRequestProperty("User-Agent", "VoteRoulette UpdateChecker");
				conn.setDoOutput(true);

				final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				final String response = reader.readLine();

				final JSONArray array = (JSONArray) JSONValue.parse(response);
				if (array.size() == 0) {
					return;
				}

				latest = (String) ((JSONObject) array.get(array.size() - 1)).get("name");
			} catch (final Exception e) {
			}
			if (latest != null) {
				latest = latest.replace("VoteRoulette v", "");
				if (!CURRENT_VERSION.equals(latest)) {
					plugin.getLogger().info("There's a different version available: " + latest + " (Current version is: " + CURRENT_VERSION + ")");
					plugin.getLogger().info("Visit http://dev.bukkit.org/bukkit-plugins/voteroulette/");
					plugin.getLogger().info("You can disable automatic update checking in the config.");
					plugin.hasUpdate = true;
				}
			} else {
				this.plugin.getLogger().info("Couldn't check for plugin updates. Will try again later.");
			}
		}
	}
}
