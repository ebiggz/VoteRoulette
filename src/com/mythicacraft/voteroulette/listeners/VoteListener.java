package com.mythicacraft.voteroulette.listeners;


import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {

	static ConfigAccessor playerCfg = new ConfigAccessor("players.yml");
	static FileConfiguration playerData = playerCfg.getConfig();
	static RewardManager rm = VoteRoulette.getRewardManager();

	final static Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");

	final static boolean REWARDS_ON_THRESHOLD = plugin.getConfig().getBoolean("giveRewardsOnThreshold");

	final static int VOTE_THRESHOLD = plugin.getConfig().getInt("voteThreshold");

	final static boolean MESSAGE_PLAYER = plugin.getConfig().getBoolean("messagePlayer");

	final static boolean BROADCAST_TO_SERVER = plugin.getConfig().getBoolean("broadcastToServer");

	final static boolean GIVE_RANDOM_REWARD = plugin.getConfig().getBoolean("giveRandomReward");

	final static boolean GIVE_RANDOM_MILESTONE = plugin.getConfig().getBoolean("giveRandomMilestone");

	final static boolean ONLY_MILESTONE_ON_COMPLETION = plugin.getConfig().getBoolean("onlyRewardMilestoneUponCompletion");

	final static boolean BLACKLIST_AS_WHITELIST = plugin.getConfig().getBoolean("useBlacklistAsWhitelist");

	final static Player[] BLACKLIST_PLAYERS = Utils.getBlacklistPlayers();

	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		Player p = plugin.getServer().getPlayerExact(vote.getUsername());
		updatePlayerVoteTotals(vote.getUsername());
		processVote(p, vote);
	}

	void updatePlayerVoteTotals(String playername) {
		if(playerCfg.getConfig().contains(playername)) {
			int currentCycle = playerCfg.getConfig().getInt(playername + ".currentCycle");
			int lifetimeVotes = playerCfg.getConfig().getInt(playername + ".lifetimeVotes");
			lifetimeVotes = lifetimeVotes + 1;
			currentCycle = currentCycle + 1;
			playerCfg.getConfig().set(playername + ".currentCycle", currentCycle);
			playerCfg.getConfig().set(playername + ".lifetimeVotes", lifetimeVotes);
			playerCfg.saveConfig();
		} else {
			playerCfg.getConfig().addDefault(playername + ".currentCycle", 1);
			playerCfg.getConfig().addDefault(playername + ".lifetimeVotes", 1);
			playerCfg.saveConfig();
		}
	}

	public static void processVote(Player player, Vote vote) {
		String playername = player.getName();
		System.out.println("Checking if player is blacklisted");
		if((BLACKLIST_AS_WHITELIST == false && Utils.playerIsBlacklisted(player)) || (BLACKLIST_AS_WHITELIST && Utils.playerIsBlacklisted(player) == false)) return;
		System.out.println("player is NOT blacklisted");
		if(rm.playerReachedMilestone(player)) {
			if(GIVE_RANDOM_MILESTONE) {
				rm.sendRandMilestone(player);
			} else {
				rm.sendMilestone(player);
			}
			if(ONLY_MILESTONE_ON_COMPLETION) return;
		}
		if(REWARDS_ON_THRESHOLD) {
			if(playerData.contains(playername)) {
				int playerVoteCycle = playerData.getInt(playername + ".voteCycle");
				if(!(playerVoteCycle >= VOTE_THRESHOLD)) return;
				playerData.set(playername + ".currentCycle", 0);
			}
		}
		if(rm.playerHasRewards(player)) {
			if(GIVE_RANDOM_REWARD) {
				rm.sendRandReward(player);
			} else {
				rm.sendReward(player);
			}
		}
	}
}

