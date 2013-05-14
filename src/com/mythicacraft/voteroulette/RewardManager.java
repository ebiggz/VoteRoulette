package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;

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
					System.out.println("[VL] Added Reward: " + rewardName);
					if(rewardName.equals(plugin.getConfig().getString("defaultReward"))) {
						setDefaultReward(new Reward(rewardName, rewardOptions));
						System.out.println("[VL] Saved as default.");
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
					if(!milestoneOptions.contains("votes")) {
						log.warning("[VoteRoulette] Milestone \"" + milestoneName + "\" doesn't have a vote number set! Ignoring Milestone...");
						continue;
					}
					milestones.add(new Milestone(milestoneName, milestoneOptions));
					System.out.println("[VL] Added Milestone: " + milestoneName);
				}
			}
		}
	}

	public void printRewards() {
		for(int i = 0; i < rewards.size(); i++) {
			System.out.println(rewards.get(i).getName());
		}
	}

	public Reward getDefaultReward() {
		return defaultReward;
	}

	public void setDefaultReward(Reward defaultReward) {
		this.defaultReward = defaultReward;
	}
}
