package com.mythicacraft.voteroulette.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mythicacraft.voteroulette.Milestone;
import com.mythicacraft.voteroulette.PlayerManager;
import com.mythicacraft.voteroulette.Reward;
import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.Utils;


public class VoteProcessor implements Runnable {

	private final String playerName;
	private VoteRoulette plugin;
	private boolean ignoreBlackList;
	private RewardManager rm;
	PlayerManager pm;

	public VoteProcessor(String playerName, VoteRoulette plugin, boolean ignoreBlackList) {
		this.playerName = playerName;
		this.plugin = plugin;
		this.ignoreBlackList = ignoreBlackList;
		rm = VoteRoulette.getRewardManager();
		pm = VoteRoulette.getPlayerManager();
	}
	public void run()
	{
		//First check if player is blacklisted & check if the blacklist is being used as a white list
		if(!ignoreBlackList) {
			if((plugin.BLACKLIST_AS_WHITELIST == false && Utils.playerIsBlacklisted(playerName)) || (plugin.BLACKLIST_AS_WHITELIST && Utils.playerIsBlacklisted(playerName) == false)) return;
		}
		//now check if a player has reached a milestone
		List<Milestone> reachedMils = rm.getReachedMilestones(playerName);
		if(!reachedMils.isEmpty()) {
			//if player has reached one, check if it should be a random
			if(plugin.GIVE_RANDOM_MILESTONE) {
				giveRandomMilestone(playerName, reachedMils);
			} else {
				//give highest priority milestone
				rm.administerMilestoneContents(reachedMils.get(0), playerName);
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
		Reward[] qualRewards = rm.getQualifiedRewards(playerName);
		if(qualRewards.length > 0) {
			//check if it should be random
			if(plugin.GIVE_RANDOM_REWARD) {
				giveRandomReward(playerName, qualRewards);
			} else {
				rm.giveDefaultReward(playerName);
			}
		}
	}

	private void giveRandomReward(String playerName, Reward[] qualRewards) {

		if(rm.rewardsContainChance(qualRewards)) {
			List<Reward> rewardsWithChance = new ArrayList<Reward>();
			List<Reward> rewardsNoChance = new ArrayList<Reward>();
			for(Reward reward : qualRewards) {
				if(reward.hasChance()) {
					rewardsWithChance.add(reward);
				} else {
					rewardsNoChance.add(reward);
				}
			}
			for(Reward reward: rewardsWithChance) {
				int random = 1 + (int)(Math.random() * ((100 - 1) + 1));
				if(random > reward.getChance()) continue;
				rm.administerRewardContents(reward, playerName);
				return;
			}

			if(rewardsNoChance.size() > 0) {
				Random rand = new Random();
				Reward reward = rewardsNoChance.get(rand.nextInt(rewardsNoChance.size()));
				rm.administerRewardContents(reward, playerName);
			}

		} else {

			Random rand = new Random();
			Reward reward = qualRewards[rand.nextInt(qualRewards.length)];

			rm.administerRewardContents(reward, playerName);
		}
	}

	public void giveRandomMilestone(String playerName, List<Milestone> reachedMils) {
		if(rm.milestonesContainChance(reachedMils)) {
			List<Milestone> milestonesWithChance = new ArrayList<Milestone>();
			List<Milestone> milestonesNoChance = new ArrayList<Milestone>();
			for(Milestone milestone : reachedMils) {
				if(milestone.hasChance()) {
					milestonesWithChance.add(milestone);
				} else {
					milestonesNoChance.add(milestone);
				}
			}
			for(Milestone milestone: milestonesWithChance) {
				int random = 1 + (int)(Math.random() * ((100 - 1) + 1));
				if(random > milestone.getChance()) continue;

				rm.administerMilestoneContents(milestone, playerName);

				return;
			}

			if(milestonesNoChance.size() > 0) {
				Random rand = new Random();
				Milestone milestone = milestonesNoChance.get(rand.nextInt(milestonesNoChance.size()));
				rm.administerMilestoneContents(milestone, playerName);
			}

		} else {

			Random rand = new Random();
			Milestone milestone = reachedMils.get(rand.nextInt(reachedMils.size()));
			rm.administerMilestoneContents(milestone, playerName);

		}
	}
}
