package com.mythicacraft.voteroulette;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.ConfigConstants;
import com.mythicacraft.voteroulette.utils.Utils;


public class VoteHandler implements ConfigConstants {

	static ConfigAccessor playerCfgAccessor = new ConfigAccessor("players.yml");
	static FileConfiguration playerData = playerCfgAccessor.getConfig();
	static RewardManager rm = VoteRoulette.getRewardManager();

	public static void processVote(Player player) {
		String playername = player.getName();
		System.out.println("Checking if player is blacklisted");
		if((BLACKLIST_AS_WHITELIST == false && Utils.playerIsBlacklisted(player)) || (BLACKLIST_AS_WHITELIST && Utils.playerIsBlacklisted(player) == false)) return;
		System.out.println("if player is NOT blacklisted");
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

