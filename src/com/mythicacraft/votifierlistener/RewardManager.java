package com.mythicacraft.votifierlistener;

import java.util.ArrayList;

import org.bukkit.configuration.ConfigurationSection;

public class RewardManager {

	private ArrayList<Reward> rewards = new ArrayList<Reward>();
	private VotifierListener plugin;
	private Reward defaultReward;

	public RewardManager(VotifierListener plugin) {
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

	public Reward getDefaultReward() {
		return defaultReward;
	}

	public void setDefaultReward(Reward defaultReward) {
		this.defaultReward = defaultReward;
	}
}
