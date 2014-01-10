package com.mythicacraft.voteroulette;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.mythicacraft.voteroulette.utils.ConfigAccessor;


public class PlayerManager {

	private VoteRoulette plugin;

	PlayerManager(VoteRoulette instance) {
		plugin = instance;
	}

	public int getPlayerLifetimeVotes(String playerName) {
		int lifetimeVotes;
		if(plugin.USE_DATABASE) {
			//place holder for db code
			lifetimeVotes = 0;
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			lifetimeVotes = playerCfg.getConfig().getInt(playerName + ".lifetimeVotes", 0);
		}
		return lifetimeVotes;
	}

	public int getPlayerCurrentVoteCycle(String playerName) {
		int voteCycle;
		if(plugin.USE_DATABASE) {
			//place holder for db code
			voteCycle = 0;
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			voteCycle = playerCfg.getConfig().getInt(playerName + ".currentCycle", 0);
		}
		return voteCycle;
	}

	public void setPlayerLifetimeVotes(String playerName, int lifetimeVotes) {
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			playerCfg.getConfig().set(playerName + ".lifetimeVotes", lifetimeVotes);
			playerCfg.saveConfig();
		}
	}

	public void setPlayerCurrentVoteCycle(String playerName, int currentCycle) {
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			playerCfg.getConfig().set(playerName + ".currentCycle", currentCycle);
			playerCfg.saveConfig();
		}
	}

	public void saveUnclaimedReward(String playerName, String rewardName) {
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			List<String> rewardsList = playerCfg.getConfig().getStringList(playerName + ".unclaimedRewards");
			rewardsList.add(rewardName);
			playerCfg.getConfig().set(playerName + ".unclaimedRewards", rewardsList);
			playerCfg.saveConfig();
		}
	}

	public void removeUnclaimedReward(String playerName, String rewardName) {
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			List<String> rewardsList = playerCfg.getConfig().getStringList(playerName + ".unclaimedRewards");
			rewardsList.remove(rewardName);
			playerCfg.getConfig().set(playerName + ".unclaimedRewards", rewardsList);
			playerCfg.saveConfig();
		}
	}

	public List<Reward> getUnclaimedRewards(String playerName) {
		List<String> rewardsList = new ArrayList<String>();

		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			rewardsList = playerCfg.getConfig().getStringList(playerName + ".unclaimedRewards");
		}

		List<Reward> rewards = new ArrayList<Reward>();
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Rewards");
		if(cs != null) {
			for(String rewardName : rewardsList) {
				ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
				if (rewardOptions != null) {
					rewards.add(new Reward(rewardName, rewardOptions));
				} else {
					removeUnclaimedReward(playerName, rewardName);
				}
			}
		}
		return rewards;
	}

	public void saveUnclaimedMilestone(String playerName, String milestoneName) {
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			List<String> milestonesList = playerCfg.getConfig().getStringList(playerName + ".unclaimedMilestones");
			milestonesList.add(milestoneName);
			playerCfg.getConfig().set(playerName + ".unclaimedMilestones", milestonesList);
			playerCfg.saveConfig();
		}
	}

	public void removeUnclaimedMilestone(String playerName, String milestoneName) {
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			List<String> milestonesList = playerCfg.getConfig().getStringList(playerName + ".unclaimedMilestones");
			milestonesList.remove(milestoneName);
			playerCfg.getConfig().set(playerName + ".unclaimedMilestones", milestonesList);
			playerCfg.saveConfig();
		}
	}

	public List<Milestone> getUnclaimedMilestones(String playerName) {
		List<String> milestonesList = new ArrayList<String>();

		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			milestonesList = playerCfg.getConfig().getStringList(playerName + ".unclaimedMilestones");
		}

		List<Milestone> milestones = new ArrayList<Milestone>();
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Milestones");
		if(cs != null) {
			for(String milestoneName : milestonesList) {
				ConfigurationSection milestoneOptions = cs.getConfigurationSection(milestoneName);
				if (milestoneOptions != null) {
					milestones.add(new Milestone(milestoneName, milestoneOptions));
				} else {
					removeUnclaimedMilestone(playerName, milestoneName);
				}
			}
		}
		return milestones;
	}
}
