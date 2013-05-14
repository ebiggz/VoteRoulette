package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class RewardManager {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private ArrayList<Reward> rewards = new ArrayList<Reward>();
	private ArrayList<Milestone> milestones = new ArrayList<Milestone>();
	private VoteRoulette plugin;
	private Reward defaultReward;

	public RewardManager(VoteRoulette plugin) {
		this.plugin = plugin;
	}

	void loadRewards() {
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Rewards");
		if(cs != null) {
			for (String rewardName : cs.getKeys(false)) {
				ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
				if (rewardOptions != null) {
					rewards.add(new Reward(rewardName, rewardOptions));
					System.out.println("[VR] Added Reward: " + rewardName);
					if(rewardName.equals(plugin.getConfig().getString("defaultReward"))) {
						setDefaultReward(new Reward(rewardName, rewardOptions));
						System.out.println("[VR] Saved as default.");
					}
				}
			}
		}
	}

	void loadMilestones() {
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Milestones");
		if(cs != null) {
			for (String milestoneName : cs.getKeys(false)) {
				ConfigurationSection milestoneOptions = cs.getConfigurationSection(milestoneName);
				if (milestoneOptions != null) {
					if(milestoneOptions.contains("votes")) {
						milestones.add(new Milestone(milestoneName, milestoneOptions));
						System.out.println("[VR] Added Milestone: " + milestoneName);
						continue;
					}
					log.warning("[VoteRoulette] Milestone \"" + milestoneName + "\" doesn't have a vote number set! Ignoring Milestone...");
				}
			}
		}
	}

	public Reward[] getQualifiedRewards(Player player) {
		ArrayList<Reward> qualifiedRewards = new ArrayList<Reward>();
		Reward[] rewardsArray;
		if(VoteRoulette.hasPermPlugin()) {
			for(int i = 0; i < rewards.size(); i++) {
				if(rewards.get(i).hasPermissionGroups()) {
					String[] permGroups = rewards.get(i).getPermGroups();
					for(int j = 0; j < permGroups.length; j++) {
						if(plugin.permission.playerInGroup(player, permGroups[j])) {
							qualifiedRewards.add(rewards.get(i));
							break;

						}
					}
				}
			}
			rewardsArray = new Reward[qualifiedRewards.size()];
			qualifiedRewards.toArray(rewardsArray);
			return rewardsArray;
		}
		rewardsArray = new Reward[rewards.size()];
		rewards.toArray(rewardsArray);
		return rewardsArray;
	}

	public Reward getDefaultReward() {
		return defaultReward;
	}

	public void setDefaultReward(Reward defaultReward) {
		this.defaultReward = defaultReward;
	}
}
