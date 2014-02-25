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
	private String website;

	public VoteProcessor(String playerName, VoteRoulette plugin, boolean ignoreBlackList, String website) {
		this.playerName = playerName;
		this.plugin = plugin;
		this.ignoreBlackList = ignoreBlackList;
		this.website = website;
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
				if(plugin.RANDOMIZE_SAME_PRIORITY) {
					//randomize the highest priority rewards if there is more than one
					List<Milestone> sameP = getSamePriorityMilestones(reachedMils);
					if(sameP.size() > 1) {
						giveRandomMilestone(playerName, sameP);
					} else {
						rm.administerMilestoneContents(reachedMils.get(0), playerName);
					}
				} else {
					//give highest priority milestone
					rm.administerMilestoneContents(reachedMils.get(0), playerName);
				}
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
		//website filter
		if(website != null) {
			qualRewards = websiteFilteredRewards(qualRewards, website);
		}

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
				int random = 1 + (int)(Math.random() * ((reward.getChanceMax() - 1) + 1));
				if(random > reward.getChanceMin()) continue;
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

	public List<Milestone> getSamePriorityMilestones(List<Milestone> milestones) {
		List<Milestone> samePriority = new ArrayList<Milestone>();
		int firstPriority = milestones.get(0).getPriority();
		for(Milestone milestone: milestones) {
			if(milestone.getPriority() == firstPriority) {
				samePriority .add(milestone);
			}
		}
		return samePriority;
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
				int random = 1 + (int)(Math.random() * ((milestone.getChanceMax() - 1) + 1));
				if(random > milestone.getChanceMin()) continue;

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

	Reward[] websiteFilteredRewards(Reward[] rewards, String website) {
		List<Reward> websiteRewards = new ArrayList<Reward>();
		for(Reward reward : rewards) {
			if(reward.hasWebsites()) {
				if(reward.getWebsites().contains(website)) {
					websiteRewards.add(reward);
				}
			} else {
				websiteRewards.add(reward);
			}
		}
		Reward[] websiteRewardsArray = new Reward[websiteRewards.size()];
		websiteRewards.toArray(websiteRewardsArray);
		return websiteRewardsArray;
	}
}
