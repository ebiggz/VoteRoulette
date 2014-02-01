package com.mythicacraft.voteroulette.listeners;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.voteroulette.PlayerManager;
import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteHandler implements Listener {

	static ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
	static FileConfiguration playerData = playerCfg.getConfig();
	static RewardManager rm = VoteRoulette.getRewardManager();
	static PlayerManager pm = VoteRoulette.getPlayerManager();

	private static VoteRoulette plugin;

	public VoteHandler(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {

		Vote vote = event.getVote();
		updatePlayerVoteTotals(vote.getUsername());
		processVote(vote.getUsername());
		pm.savePlayerLastVoteTimeStamp(vote.getUsername());

		String voteMessage = plugin.SERVER_BROADCAST_MESSAGE;
		voteMessage = voteMessage.replace("%player%", vote.getUsername()).replace("%server%", Bukkit.getServerName()).replace("%site%", vote.getServiceName());

		if(plugin.LOG_TO_CONSOLE) {
			System.out.println(voteMessage);
		}

		if(plugin.BROADCAST_TO_SERVER) {
			if(plugin.ONLY_BROADCAST_ONLINE && !Utils.playerIsOnline(vote.getUsername())) return;
			Player[] onlinePlayers = Bukkit.getOnlinePlayers();
			for(Player player : onlinePlayers) {
				if(player.getName().equals(vote.getUsername())) continue;
				player.sendMessage(voteMessage);
			}
		}
	}

	//increments players vote stats
	public static void updatePlayerVoteTotals(String playername) {
		pm.setPlayerLifetimeVotes(playername, pm.getPlayerLifetimeVotes(playername) + 1);
		pm.setPlayerCurrentVoteCycle(playername, pm.getPlayerCurrentVoteCycle(playername) + 1);
	}

	//checks if the player is eligible to receive a reward
	public static void processVote(String playerName) {

		//First check if player is blacklisted & check if the blacklist is being used as a white list
		if((plugin.BLACKLIST_AS_WHITELIST == false && Utils.playerIsBlacklisted(playerName)) || (plugin.BLACKLIST_AS_WHITELIST && Utils.playerIsBlacklisted(playerName) == false)) return;
		//now check if a player has reached a milestone
		if(rm.playerReachedMilestone(playerName)) {
			//if player has reached one, check if it should be a random
			if(plugin.GIVE_RANDOM_MILESTONE) {
				rm.giveRandomMilestone(playerName);
			} else {
				rm.giveHighestPriorityMilestone(playerName);
			}
			//if player is to only receive milestone, end
			if(plugin.ONLY_MILESTONE_ON_COMPLETION) return;
		}
		//check if player should only receive a vote after meeting a threshold
		if(plugin.REWARDS_ON_THRESHOLD) {
			//check the players current vote cycle, if it hasn't met the threshold, end
			if(pm.getPlayerCurrentVoteCycle(playerName) < plugin.VOTE_THRESHOLD) return;
			pm.setPlayerCurrentVoteCycle(playerName, 0);

		}
		//check if there is rewards the player is qualified to receive
		if(rm.playerHasRewards(playerName)) {
			//check if it should be random
			if(plugin.GIVE_RANDOM_REWARD) {
				rm.giveRandomReward(playerName);
			} else {
				rm.giveDefaultReward(playerName);
			}
		}
	}

	//checks if the player is eligible to receive a reward
	public static void processVoteIgnoreBlackList(String playerName) {

		//now check if a player has reached a milestone
		if(rm.playerReachedMilestone(playerName)) {
			//if player has reached one, check if it should be a random
			if(plugin.GIVE_RANDOM_MILESTONE) {
				rm.giveRandomMilestone(playerName);
			} else {
				rm.giveHighestPriorityMilestone(playerName);
			}
			//if player is to only receive milestone, end
			if(plugin.ONLY_MILESTONE_ON_COMPLETION) return;
		}
		//check if player should only receive a vote after meeting a threshold
		if(plugin.REWARDS_ON_THRESHOLD) {
			//check the players current vote cycle, if it hasn't met the threshold, end
			if(pm.getPlayerCurrentVoteCycle(playerName) < plugin.VOTE_THRESHOLD) return;
			pm.setPlayerCurrentVoteCycle(playerName, 0);

		}
		//check if there is rewards the player is qualified to receive
		if(rm.playerHasRewards(playerName)) {
			//check if it should be random
			if(plugin.GIVE_RANDOM_REWARD) {
				rm.giveRandomReward(playerName);
			} else {
				rm.giveDefaultReward(playerName);
			}
		}
	}
}

