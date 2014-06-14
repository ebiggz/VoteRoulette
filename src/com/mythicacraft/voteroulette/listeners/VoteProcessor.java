package com.mythicacraft.voteroulette.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.api.PlayerEarnedAwardEvent;
import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.AwardManager;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.utils.Utils;


public class VoteProcessor implements Runnable {

	private final String playerName;
	private VoteRoulette plugin;
	private boolean ignoreBlackList;
	private AwardManager rm;
	VoterManager vm;
	private String website;
	private Voter voter;

	public VoteProcessor(String playerName, VoteRoulette plugin, boolean ignoreBlackList, String website) {
		this.playerName = playerName;
		this.plugin = plugin;
		this.ignoreBlackList = ignoreBlackList;
		this.website = website;
		rm = VoteRoulette.getAwardManager();
		vm = VoteRoulette.getVoterManager();
		voter = vm.getVoter(playerName);
	}

	public void run()
	{
		if(!voter.isReal()) {
			plugin.getLogger().warning("A vote was recieved from the username \"" + playerName + "\" but VoteRoulette could not find a UUID for this name! Rewards will not be given and stats will not update. Maybe the name was typed incorrectly or there is a connectivity issue with Mojang's UUID server?");
			return;
		}

		Utils.debugMessage("Incrementing " + playerName + " vote totals");

		voter.incrementVoteTotals();
		voter.saveLastVoteTimeStamp();

		Utils.debugMessage("Beginning award processing for: " + playerName);

		VoteRoulette.getStatsManager().updateStats();

		String voteMessage = plugin.SERVER_BROADCAST_MESSAGE_NO_AWARD;
		voteMessage = voteMessage.replace("%player%", playerName).replace("%server%", Bukkit.getServerName()).replace("%site%", website);

		//First check if player is blacklisted & check if the blacklist is being used as a white list
		if(!ignoreBlackList) {
			if((plugin.BLACKLIST_AS_WHITELIST == false && Utils.playerIsBlacklisted(playerName)) || (plugin.BLACKLIST_AS_WHITELIST && Utils.playerIsBlacklisted(playerName) == false)) {
				if(!website.equals("forcevote")) {
					Utils.broadcastMessageToServer(voteMessage, playerName);
				}
				Utils.debugMessage(playerName + " is blacklisted. Stopped award processing.");
				return;
			}
		}
		//now check if a player has reached a milestone
		Milestone[] reachedMils = rm.getReachedMilestones(playerName);
		if(reachedMils.length != 0) {
			Utils.debugMessage(playerName + " reached milestone(s).");
			//if player has reached one, check if it should be a random
			if(plugin.GIVE_RANDOM_MILESTONE) {
				Utils.debugMessage("Giving random milestone to " + playerName);
				giveRandomMilestone(playerName, reachedMils);
			} else {
				if(plugin.RANDOMIZE_SAME_PRIORITY) {
					//randomize the highest priority rewards if there is more than one
					Milestone[] sameP = getSamePriorityMilestones(reachedMils);
					if(sameP.length > 1) {
						giveRandomMilestone(playerName, sameP);
					} else {
						rm.administerAwardContents(reachedMils[0], playerName);
						if(!website.equals("forcevote")) {
							Utils.broadcastMessageToServer(Utils.getServerMessageWithAward(reachedMils[0], playerName, website), playerName);
						}
					}
				} else {
					//give highest priority milestone
					rm.administerAwardContents(reachedMils[0], playerName);
					if(!website.equals("forcevote")) {
						Utils.broadcastMessageToServer(Utils.getServerMessageWithAward(reachedMils[0], playerName, website), playerName);
					}
				}
			}
			//if player is to only receive milestone, end
			if(plugin.ONLY_MILESTONE_ON_COMPLETION) {
				Utils.debugMessage("Only giving milestone on completion. Stopped award processing for " + playerName);
				return;
			}
		}
		//check if player should only receive a vote after meeting a threshold
		if(plugin.REWARDS_ON_THRESHOLD) {
			//check the players current vote cycle, if it hasn't met the threshold, end
			if(voter.getCurrentVoteCycle() < plugin.VOTE_THRESHOLD) {
				Utils.debugMessage(playerName + " has not met the vote threshold. Stopped award processing.");
				if(!website.equals("forcevote")) {
					Utils.broadcastMessageToServer(voteMessage, playerName);
				}
				return;
			}
			voter.setCurrentVoteCycle(0);
			Utils.debugMessage(playerName + " met the vote threshold.");

		}
		//check if there is rewards the player is qualified to receive
		Reward[] qualRewards = rm.getQualifiedRewards(playerName, false);
		//website filter
		if(website != null && !website.equals("forcevote")) {
			qualRewards = websiteFilteredRewards(qualRewards, website);
		}

		if(qualRewards.length > 0) {
			//check if it should be random
			if(plugin.GIVE_RANDOM_REWARD) {
				Utils.debugMessage("Giving random reward to " + playerName);
				giveRandomReward(playerName, qualRewards);
			} else {
				Utils.debugMessage("Giving default reward to " + playerName);
				playerEarnAward(playerName, rm.getDefaultReward());
			}
		} else {
			Utils.debugMessage(playerName + " qualified for no rewards. Stopped award processing.");
		}
	}

	private void giveRandomReward(String playerName, Reward[] qualRewards) {

		Utils.debugMessage("Started random reward processing for " + playerName);
		Utils.debugMessage(playerName + " qualifies for " + qualRewards.length + " reward(s)");

		if(rm.awardsContainChance(qualRewards)) {
			Utils.debugMessage("Some of those rewards contain chance settings");
			List<Reward> rewardsWithChance = new ArrayList<Reward>();
			List<Reward> rewardsNoChance = new ArrayList<Reward>();
			for(Reward reward : qualRewards) {
				if(reward.hasChance()) {
					rewardsWithChance.add(reward);
				} else {
					rewardsNoChance.add(reward);
				}
			}
			Utils.debugMessage(rewardsWithChance.size() + " have chance.");
			Utils.debugMessage(rewardsNoChance.size() + " dont have chance.");

			Utils.debugMessage("Running chance checks for rewards with chance...");
			Collections.shuffle(rewardsWithChance);
			Utils.debugMessage("Shuffling rewards with chance");
			Random rand = new Random();
			Reward rarestReward = null;
			for(Reward reward: rewardsWithChance) {
				if(reward.isRarer(rarestReward)) {
					Utils.debugMessage("Checking \"" + reward.getName() + "\" at " + reward.getChanceMin() + " in " + reward.getChanceMax());
					int random = rand.nextInt(reward.getChanceMax()) + 1;
					
					if(random <= reward.getChanceMin()) {
						Utils.debugMessage("Passed. (" + random + ") \"" + reward.getName() + "\" is now rarest");
						rarestReward = reward;
					} else {
						Utils.debugMessage("Failed. (" + random + ")");
					}
				} else {
					Utils.debugMessage("Skipping \"" + reward.getName() + "\" at " + reward.getChanceMin() + " in " + reward.getChanceMax() + " (not as rare)");
				}
			}

			if(rarestReward != null) {
				Utils.debugMessage("Administering \"" + rarestReward.getName() + "\" to " + playerName);
				playerEarnAward(playerName, rarestReward);
				return;
			} else {
				Utils.debugMessage("All reward chance checks failed for " + playerName);
			}

			if(rewardsNoChance.size() > 0) {
				Utils.debugMessage("Getting random reward from rewards with no chance...");
				Reward reward = rewardsNoChance.get(rand.nextInt(rewardsNoChance.size()));
				Utils.debugMessage("Administering \"" + reward.getName() + "\" to " + playerName);
				playerEarnAward(playerName, reward);
				return;
			}
		} else {
			Utils.debugMessage("None of the rewards contain chance settings, getting a random one...");
			Random rand = new Random();
			Reward reward = qualRewards[rand.nextInt(qualRewards.length)];
			Utils.debugMessage("Administering \"" + reward.getName() + "\" to " + playerName);
			playerEarnAward(playerName, reward);
			return;
		}
		Utils.debugMessage("No rewards were chosen for " +  playerName +". Stopping random reward processing.");
		String voteMessage = plugin.SERVER_BROADCAST_MESSAGE_NO_AWARD;
		voteMessage = voteMessage.replace("%player%", playerName).replace("%server%", Bukkit.getServerName()).replace("%site%", website);
		if(!website.equals("forcevote")) {
			Utils.broadcastMessageToServer(voteMessage, playerName);
		}
	}

	public Milestone[] getSamePriorityMilestones(Milestone[] milestones) {
		List<Milestone> samePriority = new ArrayList<Milestone>();
		int firstPriority = milestones[0].getPriority();
		for(Milestone milestone: milestones) {
			if(milestone.getPriority() == firstPriority) {
				samePriority.add(milestone);
			}
		}
		Milestone[] samePriorityA = new Milestone[samePriority.size()];
		samePriority.toArray(samePriorityA);
		return samePriorityA;
	}

	public void giveRandomMilestone(String playerName, Milestone[] reachedMils) {
		if(rm.awardsContainChance(reachedMils)) {
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
				playerEarnAward(playerName, milestone);
				return;
			}

			if(milestonesNoChance.size() > 0) {
				Random rand = new Random();
				Milestone milestone = milestonesNoChance.get(rand.nextInt(milestonesNoChance.size()));
				playerEarnAward(playerName, milestone);
				return;
			}

		} else {

			Random rand = new Random();
			Milestone milestone = reachedMils[rand.nextInt(reachedMils.length)];
			playerEarnAward(playerName, milestone);
			return;

		}
		if(!website.equals("forcevote")) {
			String voteMessage = plugin.SERVER_BROADCAST_MESSAGE_NO_AWARD;
			voteMessage = voteMessage.replace("%player%", playerName).replace("%server%", Bukkit.getServerName()).replace("%site%", website);
			Utils.broadcastMessageToServer(voteMessage, playerName);
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

	void playerEarnAward(String playerName, Award award) {
		PlayerEarnedAwardEvent event = new PlayerEarnedAwardEvent(playerName, award);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			// Now you do the event
			rm.administerAwardContents(event.getAward(), event.getPlayerName());
			if(!website.equals("forcevote")) {
				Utils.broadcastMessageToServer(Utils.getServerMessageWithAward(event.getAward(), event.getPlayerName(), website), event.getPlayerName());
			}
		} else {
			Utils.debugMessage("Event stopped by another pluggin. Cancelling award process.");
		}
	}
}
