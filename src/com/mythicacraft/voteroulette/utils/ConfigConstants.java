package com.mythicacraft.voteroulette.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public interface ConfigConstants {

	final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");

	static final boolean REWARDS_ON_THRESHOLD = plugin.getConfig().getBoolean("giveRewardsOnThreshold");

	static final int VOTE_THRESHOLD = plugin.getConfig().getInt("voteThreshold");

	static final boolean MESSAGE_PLAYER = plugin.getConfig().getBoolean("messagePlayer");

	static final boolean BROADCAST_TO_SERVER = plugin.getConfig().getBoolean("broadcastToServer");

	static final boolean GIVE_RANDOM_REWARD = plugin.getConfig().getBoolean("giveRandomReward");

	static final boolean GIVE_RANDOM_MILESTONE = plugin.getConfig().getBoolean("giveRandomMilestone");

	static final boolean ONLY_MILESTONE_ON_COMPLETION = plugin.getConfig().getBoolean("onlyRewardMilestoneUponCompletion");

	static final boolean BLACKLIST_AS_WHITELIST = plugin.getConfig().getBoolean("useBlacklistAsWhitelist");

	static final Player[] BLACKLIST_PLAYERS = Utils.getBlacklistPlayers();

}
