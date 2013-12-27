package com.mythicacraft.voteroulette.listeners;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteHandler implements Listener {

	static ConfigAccessor playerCfg = new ConfigAccessor("players.yml");
	static FileConfiguration playerData = playerCfg.getConfig();
	static RewardManager rm = VoteRoulette.getRewardManager();

	private static VoteRoulette plugin;

	public VoteHandler(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		Player p = plugin.getServer().getPlayerExact(vote.getUsername());
		updatePlayerVoteTotals(vote.getUsername());
		processVote(p, vote);
	}

	//increments players vote stats
	public static void updatePlayerVoteTotals(String playername) {

		ConfigAccessor playerCfg = new ConfigAccessor("players.yml");
		int currentCycle;
		int lifetimeVotes;

		if(playerCfg.getConfig().contains(playername)) {
			currentCycle = playerCfg.getConfig().getInt(playername + ".currentCycle") + 1;
			lifetimeVotes = playerCfg.getConfig().getInt(playername + ".lifetimeVotes") + 1;
		} else {
			currentCycle = 1;
			lifetimeVotes = 1;
		}
		playerCfg.getConfig().set(playername + ".currentCycle", currentCycle);
		playerCfg.getConfig().set(playername + ".lifetimeVotes", lifetimeVotes);
		playerCfg.saveConfig();
	}

	//checks if the player is eligible to receive a reward
	public static void processVote(Player player, Vote vote) {

		ConfigAccessor playerCfg = new ConfigAccessor("players.yml");
		FileConfiguration playerData = playerCfg.getConfig();
		String playername = player.getName();

		//First check if player is blacklisted & check if the blacklist is being used as a white list
		if((plugin.BLACKLIST_AS_WHITELIST == false && Utils.playerIsBlacklisted(player)) || (plugin.BLACKLIST_AS_WHITELIST && Utils.playerIsBlacklisted(player) == false)) return;
		//now check if a player has reached a milestone
		if(rm.playerReachedMilestone(player)) {
			//if player has reached one, check if it should be a random
			if(plugin.GIVE_RANDOM_MILESTONE) {
				rm.giveRandomMilestone(player);
			} else {
				rm.giveDefaultMilestone(player);
			}
			//if player is to only receive milestone, end
			if(plugin.ONLY_MILESTONE_ON_COMPLETION) return;
		}
		//check if player should only receive a vote after meeting a threshold
		if(plugin.REWARDS_ON_THRESHOLD) {
			if(playerData.contains(playername)) {
				int playerVoteCycleCount = playerData.getInt(playername + ".voteCycle");
				//check the players current vote cycle, if it hasn't met the threshold, end
				if(playerVoteCycleCount < plugin.VOTE_THRESHOLD) return;
				playerData.set(playername + ".currentCycle", 0);
				playerCfg.saveConfig();
			}
		}
		//check if there is rewards the player is qualified to receive
		if(rm.playerHasRewards(player)) {
			//check if it should be random
			if(plugin.GIVE_RANDOM_REWARD) {
				rm.giveRandomReward(player);
			} else {
				rm.giveDefaultReward(player);
			}
		}
	}
}

