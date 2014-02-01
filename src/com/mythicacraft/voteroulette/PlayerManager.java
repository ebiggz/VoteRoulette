package com.mythicacraft.voteroulette;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.bukkit.configuration.ConfigurationSection;

import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;


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

	public boolean playerHasntVotedInADay(String playerName) {
		String lastVoteTimeStamp;
		if(plugin.USE_DATABASE) {
			//place holder for db code
			lastVoteTimeStamp = "";
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			lastVoteTimeStamp = playerCfg.getConfig().getString(playerName + ".lastVote", "");
		}
		if(lastVoteTimeStamp.equals("")) {
			return false;
		}

		try {
			int hours = getHoursSince(lastVoteTimeStamp);
			if(hours >= 24) {
				return true;
			} else {
				return false;
			}
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void savePlayerLastVoteTimeStamp(String playerName) {
		String timeStamp = Utils.getTime();
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			playerCfg.getConfig().set(playerName + ".lastVote", timeStamp);
			playerCfg.saveConfig();
		}
	}

	public String getPlayerLastVoteTimeStamp(String playerName) {
		String timeStamp = "";
		if(plugin.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			timeStamp = playerCfg.getConfig().getString(playerName + ".lastVote", "");
		}
		return timeStamp;
	}
	public boolean playerHasLastVoteTimeStamp(String playerName) {
		if(plugin.USE_DATABASE) {
			return false;
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
			if(playerCfg.getConfig().contains(playerName + ".lastVote")) return true;
			return false;
		}
	}

	private static int getHoursSince(String time) throws ParseException {

		String currentTime = Utils.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH);
		Date date1 = sdf.parse(time);
		Date date2 = sdf.parse(currentTime);

		Long differnceInMills = date2.getTime() - date1.getTime();

		long timeInMinutes = differnceInMills/60000;
		int totalMinutes = (int) timeInMinutes;

		return totalMinutes/60;
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
