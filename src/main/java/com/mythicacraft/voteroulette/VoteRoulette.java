package com.mythicacraft.voteroulette;

import com.mythicacraft.voteroulette.awardcreator.ACListener;
import com.mythicacraft.voteroulette.awardcreator.AwardCreator;
import com.mythicacraft.voteroulette.awards.AwardManager;
import com.mythicacraft.voteroulette.awards.DelayedCommand;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.cmdexecutors.Commands;
import com.mythicacraft.voteroulette.listeners.AwardListener;
import com.mythicacraft.voteroulette.listeners.LoginListener;
import com.mythicacraft.voteroulette.listeners.VoteListener;
import com.mythicacraft.voteroulette.stats.StatManager;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Metrics;
import com.mythicacraft.voteroulette.utils.UUIDFetcher;
import com.mythicacraft.voteroulette.utils.Utils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VoteRoulette extends JavaPlugin {

	/**
	 * A Bukkit plugin for Minecraft servers that gives random
	 * rewards to players who vote for the server.
	 * 
	 * @author Erik Bigler (ebiggz)
	 */

	//plugin variables
	private static final Logger log = Logger.getLogger("VoteRoulette");
	private static Plugin VR = null;

	public static Economy economy = null;
	public static Permission permission = null;
	private static boolean vaultEnabled = false;
	private static boolean hasPermPlugin = false;
	private static boolean hasEconPlugin = false;
	private boolean hasUpdate = false;
	public boolean isOn1dot7 = false;
	public String DEFAULT_ALIAS = "vr";

	private static AwardManager rm;
	private static VoterManager pm;
	private static StatManager sm;
	private BukkitRunnable periodicReminder;
	private BukkitRunnable twentyFourHourChecker;
	private BukkitRunnable updateChecker;

	public static List<Player> notifiedPlayers = new ArrayList<Player>();
	public static HashMap<Player,Integer> lookingAtRewards = new HashMap<Player,Integer>();
	public static HashMap<Player,Integer> lookingAtMilestones = new HashMap<Player,Integer>();
	public static HashMap<Player,AwardCreator> inAwardCreator = new HashMap<Player,AwardCreator>();
	public static List<DelayedCommand> delayedCommands = new ArrayList<DelayedCommand>();
	public static List<String> cooldownPlayers = new ArrayList<String>();

	/**
	 * TODO: Create a class to hold all these constants
	 */

	//config.yml constants
	public boolean REWARDS_ON_THRESHOLD;
	public static boolean USE_DATABASE = false;
	public static boolean USE_UUIDS;
	private boolean FORCE_UUIDS = false;
	public boolean HAS_VOTE_LIMIT = false;
	public int VOTE_LIMIT;
	public int VOTE_THRESHOLD;
	public boolean MESSAGE_PLAYER;
	public boolean BROADCAST_TO_SERVER;
	public int BROADCAST_COOLDOWN;
	public boolean USE_BROADCAST_COOLDOWN;
	public boolean ONLY_BROADCAST_ONLINE;
	public static boolean DEBUG;
	public boolean LOG_TO_CONSOLE;
	public boolean ONLY_PRIMARY_GROUP;
	public boolean GIVE_RANDOM_REWARD;
	public boolean PRIORITIZE_VOTESTREAKS;
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
	public boolean GUI_FOR_AWARDS;
	public boolean SHOW_COMMANDS_IN_AWARD;
	public boolean 	SHOW_PLAYER_AND_GROUPS;
	public boolean USE_FANCY_LINKS;
	public boolean USE_SCOREBOARD;
	public boolean FIREWORK_ON_MILESTONE;
	public static boolean DISABLE_UNCLAIMED;
	public static boolean AUTO_CLAIM;
	public static boolean DISABLE_INVENTORY_PROT;
	public double CONFIG_VERSION;

	//messages.yml constants
	public String SERVER_BROADCAST_MESSAGE;
	public String SERVER_BROADCAST_MESSAGE_NO_AWARD;
	public String PLAYER_VOTE_MESSAGE;
	public String PLAYER_VOTE_MESSAGE_NO_AWARD;
	public String PERIODIC_REMINDER;
	public String TWENTYFOUR_REMINDER;
	public List<String> VOTE_WEBSITES;
	public double MESSAGES_VERSION;

	//localizations.yml constants
	public String UNCLAIMED_AWARDS_NOTIFICATION;
	public String NO_UNCLAIMED_AWARDS_NOTIFICATION;
	public String BLACKLISTED_WORLD_NOTIFICATION;
	public String WRONG_AWARD_WORLD_NOTIFICATION;
	public String INVENTORY_FULL_NOTIFICATION;
	public String NO_PERM_NOTIFICATION;
	public String BASE_CMD_NOTIFICATION;
	public String REACHED_LIMIT_NOTIFICATION;
	public String REROLL_NOTIFICATION;
	public String REROLL_FAILED_NOTIFICATION;
	public String LAST_VOTE_SELF_CMD;
	public String LAST_VOTE_OTHER_CMD;
	public String LAST_VOTE_NONE_NOTIFICATION;
	public String TOP_10_CMD;
	public String CANT_FIND_PLAYER_NOTIFICATION;
	public String INVALID_NUMBER_NOTIFICATION;
	public double LOCALIZATIONS_VERSION;

	//localization word definitions
	public String REWARD_DEF;
	public String REWARDS_PURAL_DEF;
	public String MILESTONE_DEF;
	public String MILESTONE_PURAL_DEF;
	public String CURRENCY_DEF;
	public String CURRENCY_PURAL_DEF;
	public String CURRENCY_SYMBOL;
	public String ITEM_DEF;
	public String ITEM_PLURAL_DEF;
	public String WORLDS_DEF;
	public String CHANCE_DEF;
	public String CLAIM_DEF;
	public String ALL_DEF;
	public String STATS_DEF;
	public String VOTE_DEF;
	public String VOTES_DEF;
	public String FANCY_LINK_POPUP;
	public String EVERY_DEF;
	public String VOTE_CYCLE_DEF;
	public String TOTAL_VOTES_DEF;
	public String TOTAL_DEF;
	public String TOP_DEF;
	public String VOTE_STREAK_DEF;
	public String STREAK_DEF;
	public String CURRENT_VOTE_STREAK_DEF;
	public String LONGEST_VOTE_STREAK_DEF;
	public String SETTOTAL_DEF;
	public String SETCYCLE_DEF;
	public String SETSTREAK_DEF;
	public String WIPESTATS_DEF;
	public String PLAYER_DEF;
	public String WEBSITES_DEF;
	public String XPLEVELS_DEF;
	public String RELOAD_DEF;
	public String REMIND_DEF;
	public String LASTVOTE_DEF;
	public String DAY_DEF;
	public String DAY_PLURAL_DEF;
	public String HOUR_DEF;
	public String HOUR_PLURAL_DEF;
	public String MINUTE_DEF;
	public String MINUTE_PLURAL_DEF;
	public String AND_DEF;
	public String FORCEVOTE_DEF;
	public String FORCEREWARD_DEF;
	public String FORCEMILESTONE_DEF;

	//Called when the plugin is booting up.
	public void onEnable() {

		VR = this;

		//instantiate the utils
		new Utils(this);

		//instantiate managers
		pm = new VoterManager(this);
		rm = new AwardManager(this);
		sm = StatManager.getInstance();

		//check for votifier
		if(!setupVotifier()) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		//check for and setup vault
		if(setupVault()) {
			vaultEnabled = true;
		}

		//load configs
		loadAllFilesAndData();

		//check for 1.7
		String vString = getVersion().replace("v", "");
		double v = 0;
		if (!vString.isEmpty()){
			String[] array = vString.split("_");
			v = Double.parseDouble(array[0] + "." + array[1]);
		}
		if(v > 1.6) {
			isOn1dot7 = true;
		}


		//register events and commands
		getServer().getPluginManager().registerEvents(new VoteListener(this), this);
		getServer().getPluginManager().registerEvents(new LoginListener(this), this);
		getServer().getPluginManager().registerEvents(new AwardListener(this), this);
		getServer().getPluginManager().registerEvents(new ACListener(this), this);

		getCommand("voteroulette").setExecutor(new Commands(this));
		getCommand("votelinks").setExecutor(new Commands(this));


		//convert old data, if present
		if(VoteRoulette.USE_UUIDS) {
			convertPlayersYmlToUUID();
			covertPlayersFolderToUUID();
		}


		//check file versions
		if(CONFIG_VERSION != 2.2) {
			log.warning("[VoteRoulette] It appears that your config is out of date. There may be new options! It's recommended that you take your old config out to let the new one save.");
		}

		if(MESSAGES_VERSION != 1.2) {
			log.warning("[VoteRoulette] It appears that your messages.yml file is out of date. There may be new options! It's recommended that you take your old messages file out to let the new one save.");
		}

		if(LOCALIZATIONS_VERSION != 1.5) {
			log.warning("[VoteRoulette] It appears that your localizations.yml file is out of date. There may be new options! It's recommended that you take your old localizations file out to let the new one save.");
		}

		//run a stat update
		sm.updateStats();

		//submit metrics
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit the metrics :-(
		}

		log.info("[VoteRoulette] Enabled!");
	}

	//Called when the plugin is getting disabled.
	public void onDisable() {

		//cancel scheduled events
		if(periodicReminder != null) {
			periodicReminder.cancel();
		}
		if(twentyFourHourChecker != null) {
			twentyFourHourChecker.cancel();
		}
		if(updateChecker != null) {
			updateChecker.cancel();
		}

		//run delayed commands
		for(int i = 0; i < delayedCommands.size(); i++) {
			DelayedCommand dCmd = delayedCommands.get(i);
			if(dCmd.shouldRunOnShutdown()) {
				dCmd.run();
				dCmd.cancel();
			}
		}

		//close any open reward/milestone inventory views
		Set<Player> rewardKeys = lookingAtRewards.keySet();
		for(Player key : rewardKeys) {
			key.closeInventory();
		}
		Set<Player> milestoneKeys = lookingAtMilestones.keySet();
		for(Player key : milestoneKeys) {
			key.closeInventory();
		}

		log.info("[VoteRoulette] Disabled!");
	}

	/*
	 * Plugin hooks
	 */

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

	/*
	 * File & data loading
	 */

	public void loadAllFilesAndData() {

		this.getLogger().info("Loading all files and data...");

		//load main config
		loadConfig();
		reloadConfig();
		loadConfigOptions();
		//load awards.yml
		loadAwardsFile();
		transferAwards();
		loadRewards();
		loadMilestones();
		//load messages.yml
		loadMessagesFile();
		loadMessagesData();
		//load localizations.yml
		loadLocalizationsFile();
		loadLocalizationsData();
		//load known sites.yml
		loadKnownSitesFile();
		//load stats.yml
		loadStatsFile();
		//load UUIDCache.yml
		loadUUIDCache();

		this.getLogger().info("...finished loading files and data!");

		if(getServer().getOnlineMode() == false && VoteRoulette.USE_UUIDS && this.FORCE_UUIDS == false){
			getLogger().warning("Your server is in offline mode but VoteRoulette is set to use UUIDs. Players with illegitimate copies of Minecraft will not get their stats tracked. The use of UUIDs has been automatically disabled. You can set \"useUUIDs\" to \"always\" to override this.");
			VoteRoulette.USE_UUIDS = false;
		}

		//schedule reminders and update checker
		scheduleTasks();

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

	private void loadConfigOptions() {
		REWARDS_ON_THRESHOLD = getConfig().getBoolean("giveRewardsOnThreshold");

		VOTE_THRESHOLD = getConfig().getInt("voteThreshold");

		MESSAGE_PLAYER = getConfig().getBoolean("messagePlayer");

		DISABLE_UNCLAIMED = getConfig().getBoolean("disableUnclaimedAwards", false);

		AUTO_CLAIM = getConfig().getBoolean("autoClaimAwards", false);

		String uuidOption = getConfig().getString("useUUIDs", "true").toLowerCase();

		if(uuidOption.equalsIgnoreCase("always") || uuidOption.equalsIgnoreCase("force")) {
			FORCE_UUIDS = true;
			USE_UUIDS = true;
		} else {
			USE_UUIDS = Boolean.parseBoolean(uuidOption);
		}

		DISABLE_INVENTORY_PROT = getConfig().getBoolean("disableInventoryProtection", false);

		String defaultAlias = getConfig().getString("defaultCommandAlias", "vr").trim();

		if(defaultAlias.equalsIgnoreCase("vr") || defaultAlias.equalsIgnoreCase("vtr") || defaultAlias.equalsIgnoreCase("voteroulette")) {
			DEFAULT_ALIAS = defaultAlias.toLowerCase().replace("/", "");
			Plugin voxelSniper =  getServer().getPluginManager().getPlugin("VoxelSniper");
			if (voxelSniper != null) {
				if(DEFAULT_ALIAS.equalsIgnoreCase("vr")) {
					getLogger().warning("VoxelSniper detected! Setting default alias to: \"/vtr\"");
					DEFAULT_ALIAS = "vtr";
				}
			} else {
				getLogger().info("Setting default command alias: " + DEFAULT_ALIAS);
			}
		} else {
			getLogger().info("Default alias in config isn't one of the options, keeping it at: vr ");
		}


		BROADCAST_TO_SERVER = getConfig().getBoolean("broadcastToServer");

		ONLY_BROADCAST_ONLINE = getConfig().getBoolean("onlyBroadcastOnlinePlayerVotes", false);

		LOG_TO_CONSOLE = getConfig().getBoolean("logToConsole");

		DEBUG = getConfig().getBoolean("debug", false);

		BROADCAST_COOLDOWN = getConfig().getInt("broadcastCooldown", 0);

		if(BROADCAST_COOLDOWN == 0) {
			USE_BROADCAST_COOLDOWN = false;
		} else {
			USE_BROADCAST_COOLDOWN = true;
		}

		VOTE_LIMIT = getConfig().getInt("voteLimit", 0);

		if(VOTE_LIMIT == 0) {
			HAS_VOTE_LIMIT = false;
		} else {
			HAS_VOTE_LIMIT = true;
		}

		GUI_FOR_AWARDS = getConfig().getBoolean("GUI.awards.guiForAwards", true);

		SHOW_COMMANDS_IN_AWARD = getConfig().getBoolean("GUI.awards.showCommands", false);

		SHOW_PLAYER_AND_GROUPS = getConfig().getBoolean("GUI.awards.showPlayersAndGroups", false);

		USE_FANCY_LINKS = getConfig().getBoolean("GUI.vote-command.useFancyLinks", false);

		GIVE_RANDOM_REWARD = getConfig().getBoolean("giveRandomReward");

		PRIORITIZE_VOTESTREAKS = getConfig().getBoolean("prioritizeVoteStreaks", true);

		USE_SCOREBOARD = getConfig().getBoolean("GUI.stats.useScoreboard", true);

		GIVE_RANDOM_MILESTONE = getConfig().getBoolean("giveRandomMilestone");

		RANDOMIZE_SAME_PRIORITY = getConfig().getBoolean("randomizeTiedHighestPriorityMilestones", false);

		ONLY_MILESTONE_ON_COMPLETION = getConfig().getBoolean("onlyGiveMilestoneUponCompletion", true);

		BLACKLIST_AS_WHITELIST = getConfig().getBoolean("useBlacklistAsWhitelist");

		USE_TWENTYFOUR_REMINDER = getConfig().getBoolean("useTwentyFourHourReminder", true);

		USE_PERIODIC_REMINDER = getConfig().getBoolean("usePeriodicReminder");

		REMINDER_INTERVAL = getConfig().getLong("periodicReminderInterval")*1200;

		BLACKLIST_PLAYERS = getConfig().getStringList("blacklistedPlayers");

		BLACKLIST_WORLDS = getConfig().getStringList("blacklistedWorlds");

		CHECK_UPDATES = getConfig().getBoolean("checkForUpdates", true);

		CONSIDER_REWARDS_FOR_CURRENT_WORLD = getConfig().getBoolean("considerRewardsForPlayersCurrentWorld");

		CONSIDER_MILESTONES_FOR_CURRENT_WORLD = getConfig().getBoolean("considerMilestonesForPlayersCurrentWorld");

		ONLY_PRIMARY_GROUP = getConfig().getBoolean("onlyConsiderPlayersPrimaryGroup", false);

		FIREWORK_ON_MILESTONE = getConfig().getBoolean("randomFireworkOnMilestone", true);

		CONFIG_VERSION = getConfig().getDouble("configVersion", 1.0);


	}

	private void loadAwardsFile() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		File awardsFile = new File(pluginFolder, "awards.yml");
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");

		if(!awardsFile.exists()) {
			saveResource("awards.yml", true);
			return;
		}
		try {
			awardsData.reloadConfig();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while loading VoteRoulette/awards.yml", e);
			pm.disablePlugin(this);
		}
	}

	private void transferAwards() {
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");
		if(this.getConfig().contains("Rewards")) {
			ConfigurationSection cs = this.getConfig().getConfigurationSection("Rewards");
			if(cs != null) {
				for(String rewardName : cs.getKeys(false)) {
					ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
					if (rewardOptions != null) {
						if(!awardsData.getConfig().contains("Rewards." + rewardName)) {
							awardsData.getConfig().set("Rewards." + rewardName, rewardOptions);
						}
					}
				}
				awardsData.saveConfig();
				getLogger().warning("Rewards have moved to the awards.yml, your current Rewards have been copied from config.yml to there! You can delete the old Rewards in your config.yml.");
			}
		}

		if(this.getConfig().contains("Milestones")) {
			ConfigurationSection cs1 = this.getConfig().getConfigurationSection("Milestones");
			if(cs1 != null) {
				for(String milestoneName : cs1.getKeys(false)) {
					ConfigurationSection milestoneOptions = cs1.getConfigurationSection(milestoneName);
					if (milestoneOptions != null) {
						if(!awardsData.getConfig().contains("Milestones." + milestoneName)) {
							awardsData.getConfig().set("Milestones." + milestoneName, milestoneOptions);
						}
					}
				}
				awardsData.saveConfig();
				getLogger().warning("Milestones have moved to the awards.yml, your current Milestones have been copied from config.yml to there! You can delete the old Milestones in your config.yml.");
			}
		}
	}

	private void loadRewards() {
		rm.clearRewards();
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");
		ConfigurationSection cs = awardsData.getConfig().getConfigurationSection("Rewards");
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
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");
		ConfigurationSection cs = awardsData.getConfig().getConfigurationSection("Milestones");
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
			log.warning("[VoteRoulette] Your Milestone section is empty, no milestones will be given!");
		}
	}

	private void loadMessagesFile() {
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

	private void loadMessagesData() {
		ConfigAccessor messageData = new ConfigAccessor("messages.yml");

		SERVER_BROADCAST_MESSAGE = Utils.transcribeColorCodes(messageData.getConfig().getString("server-broadcast-message"));

		SERVER_BROADCAST_MESSAGE_NO_AWARD = Utils.transcribeColorCodes(messageData.getConfig().getString("server-broadcast-message-no-award", "&b[&e%player%&b just voted for %server% on &e%site%&b!]"));

		PLAYER_VOTE_MESSAGE = Utils.transcribeColorCodes(messageData.getConfig().getString("player-reward-message"));

		PLAYER_VOTE_MESSAGE_NO_AWARD = Utils.transcribeColorCodes(messageData.getConfig().getString("player-no-reward-message", "&bThanks for voting for &e%server% &bon %site%, &e%player%&b!"));


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

	private void loadLocalizationsFile() {
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

	private void loadLocalizationsData() {
		ConfigAccessor localeData = new ConfigAccessor("data" + File.separator + "localizations.yml");

		UNCLAIMED_AWARDS_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("unclaimed-awards"));

		NO_UNCLAIMED_AWARDS_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("no-unclaimed-awards"));

		BLACKLISTED_WORLD_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("blacklisted-world"));

		REACHED_LIMIT_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("reached-vote-limit", "%red%[VoteRoulette] You have reached the vote limit for today!"));

		WRONG_AWARD_WORLD_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("award-wrong-world"));

		INVENTORY_FULL_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("award-inventory-full"));

		NO_PERM_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("no-permission"));

		BASE_CMD_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("base-command-text").replace("%alias%", this.DEFAULT_ALIAS));

		REROLL_FAILED_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("reroll-failed"));

		REROLL_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("rerolling"));

		LAST_VOTE_SELF_CMD = Utils.transcribeColorCodes(localeData.getConfig().getString("last-vote-self", "%darkaqua%[VoteRoulette] Time since your last vote: %time%"));

		LAST_VOTE_OTHER_CMD = Utils.transcribeColorCodes(localeData.getConfig().getString("last-vote-other", "%darkaqua%[VoteRoulette] Time since %player%s last vote: %time%"));

		CANT_FIND_PLAYER_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("cant-find-player", "%red%[VoteRoulette] Could not find player: %player%"));

		INVALID_NUMBER_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("invalid-number", "%red%[VoteRoulette] Invalid number!"));

		LAST_VOTE_NONE_NOTIFICATION = Utils.transcribeColorCodes(localeData.getConfig().getString("last-vote-none-saved", "%red%[VoteRoulette] A last vote time stamp has not been saved yet!"));

		TOP_10_CMD = Utils.transcribeColorCodes(localeData.getConfig().getString("top-10", "%aqua%[VoteRoulette] Showing top 10 players for %yellow%%stat%"));

		LOCALIZATIONS_VERSION = localeData.getConfig().getDouble("config-version", 1.0);

		// word definitions
		REWARD_DEF = localeData.getConfig().getString("general-word-definitions.reward", "Reward");
		REWARDS_PURAL_DEF = localeData.getConfig().getString("general-word-definitions.reward-plural", "Rewards");
		MILESTONE_DEF = localeData.getConfig().getString("general-word-definitions.milestone", "Milestone");
		MILESTONE_PURAL_DEF = localeData.getConfig().getString("general-word-definitions.milestone-plural", "Milestones");
		CURRENCY_DEF = localeData.getConfig().getString("general-word-definitions.currency", "Dollar");
		CURRENCY_PURAL_DEF = localeData.getConfig().getString("general-word-definitions.currency-plural", "Dollars");
		CURRENCY_SYMBOL = localeData.getConfig().getString("general-word-definitions.currency-symbol", "$");
		ITEM_DEF = localeData.getConfig().getString("general-word-definitions.item", "Item");
		ITEM_PLURAL_DEF = localeData.getConfig().getString("general-word-definitions.item-plural", "Items");
		WORLDS_DEF = localeData.getConfig().getString("general-word-definitions.worlds", "Worlds");
		CHANCE_DEF = localeData.getConfig().getString("general-word-definitions.chance", "Chance");
		CLAIM_DEF = localeData.getConfig().getString("general-word-definitions.claim", "Claim");
		ALL_DEF = localeData.getConfig().getString("general-word-definitions.all", "All");
		FANCY_LINK_POPUP = localeData.getConfig().getString("general-word-definitions.fancy-link-popup", "Click me to vote!");
		STATS_DEF = localeData.getConfig().getString("general-word-definitions.stats", "Stats");
		EVERY_DEF = localeData.getConfig().getString("general-word-definitions.every", "Every");
		VOTE_DEF = localeData.getConfig().getString("general-word-definitions.vote", "Vote");
		VOTES_DEF = localeData.getConfig().getString("general-word-definitions.vote-plural", "Votes");
		VOTE_CYCLE_DEF = localeData.getConfig().getString("general-word-definitions.vote-cycle", "Vote Cycle");
		TOTAL_VOTES_DEF = localeData.getConfig().getString("general-word-definitions.total-votes", "Lifetime Votes");
		TOTAL_DEF = localeData.getConfig().getString("general-word-definitions.total", "Total");
		TOP_DEF = localeData.getConfig().getString("general-word-definitions.top", "top");
		VOTE_STREAK_DEF  = localeData.getConfig().getString("general-word-definitions.vote-streak", "Votestreak");
		STREAK_DEF  = localeData.getConfig().getString("general-word-definitions.streak", "Streak");
		CURRENT_VOTE_STREAK_DEF = localeData.getConfig().getString("general-word-definitions.current-vote-streak", "Current Votestreak");
		LONGEST_VOTE_STREAK_DEF = localeData.getConfig().getString("general-word-definitions.longest-vote-streak", "Longest Votestreak");
		SETTOTAL_DEF = localeData.getConfig().getString("general-word-definitions.settotal", "settotal");
		SETCYCLE_DEF = localeData.getConfig().getString("general-word-definitions.setcycle", "setcycle");
		SETSTREAK_DEF = localeData.getConfig().getString("general-word-definitions.setstreak", "setstreak");
		WIPESTATS_DEF = localeData.getConfig().getString("general-word-definitions.wipestats", "wipestats");
		PLAYER_DEF = localeData.getConfig().getString("general-word-definitions.player", "Player");
		WEBSITES_DEF = localeData.getConfig().getString("general-word-definitions.websites", "Websites");
		XPLEVELS_DEF = localeData.getConfig().getString("general-word-definitions.xplevels", "XP levels");
		RELOAD_DEF = localeData.getConfig().getString("general-word-definitions.reload", "reload");
		REMIND_DEF = localeData.getConfig().getString("general-word-definitions.remind", "remind");
		LASTVOTE_DEF = localeData.getConfig().getString("general-word-definitions.last-vote", "lastvote");
		DAY_DEF = localeData.getConfig().getString("general-word-definitions.day", "day");
		DAY_PLURAL_DEF = localeData.getConfig().getString("general-word-definitions.day-plural", "days");
		HOUR_DEF = localeData.getConfig().getString("general-word-definitions.hour", "hour");
		HOUR_PLURAL_DEF = localeData.getConfig().getString("general-word-definitions.hour-plural", "hours");
		MINUTE_DEF = localeData.getConfig().getString("general-word-definitions.minute", "minute");
		MINUTE_PLURAL_DEF = localeData.getConfig().getString("general-word-definitions.minute-plural", "minutes");
		AND_DEF = localeData.getConfig().getString("general-word-definitions.and", "and");
		FORCEVOTE_DEF = localeData.getConfig().getString("general-word-definitions.forcevote", "forcevote");
		FORCEREWARD_DEF = localeData.getConfig().getString("general-word-definitions.forcereward", "forcereward");
		FORCEMILESTONE_DEF = localeData.getConfig().getString("general-word-definitions.forcemilestone", "forcemilestone");

	}

	private void loadKnownSitesFile() {
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

	private void loadUUIDCache() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		String playerFolder = pluginFolder + File.separator + "data";
		(new File(playerFolder)).mkdirs();
		File playerDataFile = new File(playerFolder, "UUIDCache.yml");
		ConfigAccessor playerData = new ConfigAccessor("data" + File.separator + "UUIDCache.yml");

		if (!playerDataFile.exists()) {
			try {
				playerData.saveDefaultConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/UUIDCache.yml", e);
				pm.disablePlugin(this);
			}
			return;
		} else {
			try {
				playerData.getConfig().options().header("This file caches playername/uuid combos so VoteRoulette doesn't have to contact Mojang servers as often.");
				playerData.getConfig().options().copyHeader();
				playerData.reloadConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/UUIDCache.yml", e);
				pm.disablePlugin(this);
			}
		}
	}

	private void loadStatsFile() {
		PluginManager pm = getServer().getPluginManager();
		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		String playerFolder = pluginFolder + File.separator + "data";
		(new File(playerFolder)).mkdirs();
		File playerDataFile = new File(playerFolder, "stats.yml");
		ConfigAccessor playerData = new ConfigAccessor("data" + File.separator + "stats.yml");

		if (!playerDataFile.exists()) {
			try {
				playerData.saveDefaultConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/stats.yml", e);
				pm.disablePlugin(this);
			}
			return;
		} else {
			try {
				playerData.getConfig().options().header("This file keeps track of the stats of VR. Theres no need to edit anything here.");
				playerData.getConfig().options().copyHeader();
				playerData.reloadConfig();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/stats.yml", e);
				pm.disablePlugin(this);
			}
		}
	}

	private void covertPlayersFolderToUUID() {

		String oldPlayerFilePath = getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + "players";
		String newPlayerFilePath = getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + "playerdata";
		(new File(newPlayerFilePath)).mkdirs();

		File oldPlayerFolder = new File(oldPlayerFilePath);
		if(!oldPlayerFolder.exists()) return;

		String[] oldPlayersNamesLs = oldPlayerFolder.list();
		String[] oldPlayersNames = null;
		ArrayList<String> oldPlayersNamesAl = new ArrayList<String>();

		for(String pn : oldPlayersNamesLs) {
			if(pn.endsWith(".yml")) {
				oldPlayersNamesAl.add(pn.replace(".yml", ""));
			}
		}

		oldPlayersNames = new String[oldPlayersNamesAl.size()];
		oldPlayersNamesAl.toArray(oldPlayersNames);

		if(oldPlayersNames.length > 0) {

			getLogger().info("Detected old player folder, attempting to convert files to UUID...");

			UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(oldPlayersNames));

			Map<String, UUID> response = null;
			try {
				response = fetcher.call();
			} catch (Exception e) {
				this.getLogger().severe("There was an issue getting UUIDs while attempting to convert data in the old players folder to the new format. The conversion will not happen. The error was: " + e.toString());
				return;
			}

			if(response == null) {
				System.out.println("Error getting UUIDs");
				return;
			}

			for(String oldPlayer : oldPlayersNames) {

				if(response.containsKey(oldPlayer)) {

					UUID uuid = response.get(oldPlayer);
					if(uuid == null) continue;
					File newPlayerFile = new File(newPlayerFilePath + File.separator + uuid.toString() + ".yml");

					if(!newPlayerFile.exists()) {

						ConfigAccessor oldPlayerCfg = new ConfigAccessor("data" + File.separator + "players" + File.separator + oldPlayer + ".yml");

						Voter voter = getVoterManager().getVoter(oldPlayer);


						if(oldPlayerCfg.getConfig().contains("currentCycle")) {
							voter.setCurrentVoteCycle(oldPlayerCfg.getConfig().getInt("currentCycle", 0));
						}
						if(oldPlayerCfg.getConfig().contains("lifetimeVotes")) {
							voter.setLifetimeVotes(oldPlayerCfg.getConfig().getInt("lifetimeVotes", 0));
						}
						if(oldPlayerCfg.getConfig().contains("lastVote")) {
							voter.setLastVoteTimeStamp(oldPlayerCfg.getConfig().getString("lastVote", ""));
						}
						if(oldPlayerCfg.getConfig().contains("currentVoteStreak")) {
							voter.setCurrentVoteStreak(oldPlayerCfg.getConfig().getInt("currentVoteStreak", 0));
						}
						if(oldPlayerCfg.getConfig().contains("longestVoteStreak")) {
							voter.setLongestVoteStreak(oldPlayerCfg.getConfig().getInt("longestVoteStreak", 0));
						}
						if(oldPlayerCfg.getConfig().contains("unclaimedRewards")) {
							List<String> unclaimedRewards = oldPlayerCfg.getConfig().getStringList("unclaimedRewards");
							for(String unclaimedReward : unclaimedRewards) {
								if(unclaimedReward != null) {
									voter.saveUnclaimedReward(unclaimedReward);
								}
							}
						}
						if(oldPlayerCfg.getConfig().contains("unclaimedMilestones")) {
							List<String> unclaimedMilestones = oldPlayerCfg.getConfig().getStringList("unclaimedMilestones");
							for(String unclaimedMilestone : unclaimedMilestones) {
								if(unclaimedMilestone != null) {
									voter.saveUnclaimedMilestone(unclaimedMilestone);
								}
							}
						}
						File oldPlayerFile = new File(oldPlayerFilePath + File.separator + oldPlayer + ".yml");
						oldPlayerFile.delete();
					}
				} else {
					getLogger().warning("Could not convert name \"" + oldPlayer + "\" to a UUID as the name is not an actual minecraft player. Was it a mistyped name or is there connectivity issues with Mojangs UUID server?");
				}
			}
		}
	}

	private void convertPlayersYmlToUUID() {

		String pluginFolder = this.getDataFolder().getAbsolutePath();
		(new File(pluginFolder)).mkdirs();
		String playerFolder = pluginFolder + File.separator + "data";
		(new File(playerFolder)).mkdirs();
		File playerDataFile = new File(playerFolder, "players.yml");
		ConfigAccessor playersCfg = new ConfigAccessor("data" + File.separator + "players.yml");

		if (playerDataFile.exists()) {
			try {
				getLogger().info("Old players.yml file detected. Attempting to transfer player data to new file format...");
				String oldPlayerFilePath = getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + "playerdata";
				Set<String> players = playersCfg.getConfig().getKeys(false);
				int count = 0;
				int failCount = 0;
				for(String playerName : players) {
					ConfigurationSection playerData = playersCfg.getConfig().getConfigurationSection(playerName);
					if(playerName.equals("currentCycle") || playerName.equals("lifetimeVotes") || playerName.equals("lastVote") || playerName.equals("unclaimedRewards") || playerName.equals("currentMilestones")) continue;
					(new File(oldPlayerFilePath)).mkdirs();
					if(playerData == null) continue;

					UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(playerName));
					Map<String, UUID> response = null;
					try {
						response = fetcher.call();
					} catch (Exception e) {
						this.getLogger().severe("There was an issue getting UUIDs while attempting to convert \"" + playerName + "\" in the old players.yml to the new format. The conversion will not happen. The error was: " + e.toString());
						failCount++;
						continue;
					}
					if(response != null) {
						UUID id = response.get(playerName);
						if(id == null) {
							this.getLogger().severe("Could not get a UUID for \"" + playerName + "\" while attempting to convert to new format. Perhaps this is a mistyped name?");
							failCount++;
							continue;
						}
						String newPlayerFilePath = getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + "playerdata";
						(new File(newPlayerFilePath)).mkdirs();
						File playerFile = new File(newPlayerFilePath + File.separator + id.toString() + ".yml");
						if(!playerFile.exists()) {
							count++;
							if(playersCfg.getConfig().contains(playerName)) {
								Voter voter = VoteRoulette.getVoterManager().getVoter(playerName);
								if(playerData.contains("currentCycle")) {
									voter.setCurrentVoteCycle(playerData.getInt("currentCycle", 0));
								}
								if(playerData.contains("lifetimeVotes")) {
									voter.setLifetimeVotes(playerData.getInt("lifetimeVotes", 0));
								}
								if(playerData.contains("lastVote")) {
									voter.setLastVoteTimeStamp(playerData.getString("lastVote", ""));
								}
								if(playerData.contains("unclaimedRewards")) {
									List<String> unclaimedRewards = playerData.getStringList("unclaimedRewards");
									for(String unclaimedReward : unclaimedRewards) {
										if(unclaimedReward != null) {
											voter.saveUnclaimedReward(unclaimedReward);
										}
									}
								}
								if(playerData.contains("unclaimedMilestones")) {
									List<String> unclaimedMilestones = playerData.getStringList("unclaimedMilestones");
									for(String unclaimedMilestone : unclaimedMilestones) {
										if(unclaimedMilestone != null) {
											voter.saveUnclaimedMilestone(unclaimedMilestone);
										}
									}
								}
								playersCfg.getConfig().set(playerName, null);
							}
						}
					}
				}
				if(count == 0 && failCount == 0) {
					getLogger().info("All players appear to have already been transfered. You can now delete the players.yml file.");
				} else if(count > 0) {
					getLogger().info("Successfully transfered " + count + " player(s) to new format.");
				}
				else if(failCount > 0) {
					getLogger().info("Failed to transfer " + failCount + " player(s) to new format.");
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while loading VoteRoulette/data/players.yml", e);
			}
		}
	}

	private void scheduleTasks() {
		//cancel any previously set tasks
		if(periodicReminder != null) {
			periodicReminder.cancel();
		}
		if(twentyFourHourChecker != null) {
			twentyFourHourChecker.cancel();
		}
		if(updateChecker != null) {
			updateChecker.cancel();
		}

		//schedule new ones
		if(USE_PERIODIC_REMINDER) {
			periodicReminder = new PeriodicReminder(PERIODIC_REMINDER);
			periodicReminder.runTaskTimer(this, REMINDER_INTERVAL, REMINDER_INTERVAL);
		}

		if(USE_TWENTYFOUR_REMINDER) {
			twentyFourHourChecker = new TwentyFourHourCheck(TWENTYFOUR_REMINDER);
			twentyFourHourChecker.runTaskTimer(this, 12000, 12000);
		}

		if (CHECK_UPDATES) {
			if (getDescription().getVersion().toLowerCase().contains("snapshot")) {
				getLogger().info("This is not a release version. Automatic update checking will be disabled.");
			} else {
				updateChecker = new UpdateChecker(this);
				updateChecker.runTaskTimerAsynchronously(this, 40, 432000);
			}
		}
	}

	/*
	 * Getters and such
	 */

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

	public static Plugin getPlugin() {
		return VR;
	}

	public static AwardManager getAwardManager() {
		return rm;
	}

	public static VoterManager getVoterManager() {
		return pm;
	}

	public static StatManager getStatsManager() {
		return sm;
	}

	/**
	 * Determines the version string used by Craftbukkit's safeguard (e.g. 1_7_R4).
	 * @return the version string used by Craftbukkit's safeguard
	 */
	private static String getVersion(){
		String[] array = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",");
		if (array.length == 4)
			return array[3] + ".";
		return "";
	}

	/*
	 * 
	 * Runnable Tasks
	 *
	 */

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
<<<<<<< Updated upstream:src/main/java/com/mythicacraft/voteroulette/VoteRoulette.java
			Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
			for(Player player : onlinePlayers) {
=======
			for(Player player : Bukkit.getOnlinePlayers()) {
>>>>>>> Stashed changes:src/com/mythicacraft/voteroulette/VoteRoulette.java
				Voter voter = VoteRoulette.getVoterManager().getVoter(player.getName());
				if(voter.hasntVotedInADay()) {
					if(!VoteRoulette.notifiedPlayers.contains(player)) {
						player.sendMessage(message.replace("%player%", player.getName()));
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

			String latest = null;

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
				conn.addRequestProperty("User-Agent", "VoteRoulette Update Checker");
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
