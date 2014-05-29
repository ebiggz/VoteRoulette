package com.mythicacraft.voteroulette;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.UUIDFetcher;
import com.mythicacraft.voteroulette.utils.Utils;


public class Voter {

	private Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");
	private UUID uuid;
	private boolean isReal;

	public Voter(String playerName) {
		UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(playerName));
		Map<String, UUID> response = null;
		try {
			response = fetcher.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(response != null) {
			UUID id = response.get(playerName);
			if(id != null) {
				this.uuid = id;
				isReal = true;
				createFile(plugin.getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + "playerdata", id.toString() + ".yml");
				this.setPlayerName(playerName);
				return;
			}
		}
		isReal = false;
	}


	public UUID getUUID() {
		return uuid;
	}

	public boolean isReal() {
		return isReal;
	}

	public enum Stat {
		CURRENT_VOTE_STREAK, LONGEST_VOTE_STREAK, CURRENT_VOTE_CYCLE, LIFETIME_VOTES, UNCLAIMED_MILSTONES, UNCLAIMED_REWARDS, LAST_VOTE, ALL
	}

	public void wipeStat(Stat stat) {
		switch(stat) {
		case ALL:
			setCurrentVoteCycle(0);
			setCurrentVoteStreak(0);
			setLongestVoteStreak(0);
			setLastVoteTimeStamp(null);
			setLifetimeVotes(0);
			removeUnclaimedMilestones();
			removeUnclaimedRewards();
			break;
		case CURRENT_VOTE_CYCLE:
			setCurrentVoteCycle(0);
			break;
		case CURRENT_VOTE_STREAK:
			setCurrentVoteStreak(0);
			break;
		case LAST_VOTE:
			setLastVoteTimeStamp(null);
			break;
		case LIFETIME_VOTES:
			setLifetimeVotes(0);
			break;
		case LONGEST_VOTE_STREAK:
			setLongestVoteStreak(0);
			break;
		case UNCLAIMED_MILSTONES:
			removeUnclaimedMilestones();
			break;
		case UNCLAIMED_REWARDS:
			removeUnclaimedRewards();
			break;
		default:
			break;
		}
	}

	public int getLifetimeVotes() {
		int lifetimeVotes;
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
			lifetimeVotes = 0;
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			lifetimeVotes = playerCfg.getConfig().getInt("lifetimeVotes", 0);
		}
		return lifetimeVotes;
	}

	public void setCurrentVoteStreak(int voteStreak) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("currentVoteStreak", voteStreak);
			playerCfg.saveConfig();
		}
	}

	public int getCurrentVoteStreak() {
		int voteStreak;
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
			voteStreak = 0;
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			voteStreak = playerCfg.getConfig().getInt("currentVoteStreak", 0);
		}
		return voteStreak;
	}

	public void setLongestVoteStreak(int voteStreak) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("longestVoteStreak", voteStreak);
			playerCfg.saveConfig();
		}
	}

	public int getLongestVoteStreak() {
		int voteStreak;
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
			voteStreak = 0;
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			voteStreak = playerCfg.getConfig().getInt("longestVoteStreak", 0);
		}
		return voteStreak;
	}

	public boolean hasntVotedInADay() {
		String lastVoteTimeStamp = getLastVoteTimeStamp();
		if(lastVoteTimeStamp.equals("")) {
			return false;
		}
		int hours = getHoursSince(lastVoteTimeStamp);
		if(hours >= 24) {
			return true;
		} else {
			return false;
		}
	}

	public void saveLastVoteTimeStamp() {
		String timeStamp = Utils.getTime();
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("lastVote", timeStamp);
			playerCfg.saveConfig();
		}
	}

	public void setLastVoteTimeStamp(String timeStamp) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("lastVote", timeStamp);
			playerCfg.saveConfig();
		}
	}

	public void setPlayerName(String name) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("name", name);
			playerCfg.saveConfig();
		}
	}

	public String getPlayerName() {
		String name = "";
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			name = playerCfg.getConfig().getString("name", "");
		}
		return name;
	}

	public String getLastVoteTimeStamp() {
		String timeStamp = "";
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			timeStamp = playerCfg.getConfig().getString("lastVote", "");
		}
		return timeStamp;
	}

	public boolean hasLastVoteTimeStamp() {
		if(VoteRoulette.USE_DATABASE) {
			return false;
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			if(playerCfg.getConfig().contains("lastVote")) return true;
			return false;
		}
	}

	private static int getHoursSince(String time) {

		String currentTime = Utils.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH);
		Date date1 = null;
		Date date2 = null;
		try {
			date1 = sdf.parse(time);
			date2 = sdf.parse(currentTime);
		} catch (ParseException e) {
			e.printStackTrace();
			return 0;
		}

		Long differnceInMills = date2.getTime() - date1.getTime();

		long timeInMinutes = differnceInMills/60000;
		int totalMinutes = (int) timeInMinutes;

		return totalMinutes/60;
	}

	public int getCurrentVoteCycle() {
		int voteCycle;
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
			voteCycle = 0;
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			voteCycle = playerCfg.getConfig().getInt("currentCycle", 0);
		}
		return voteCycle;
	}

	public void setLifetimeVotes(int lifetimeVotes) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("lifetimeVotes", lifetimeVotes);
			playerCfg.saveConfig();
		}
	}

	public void setCurrentVoteCycle(int currentCycle) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("currentCycle", currentCycle);
			playerCfg.saveConfig();
		}
	}

	public void incrementVoteTotals() {

		int newLifetime = getLifetimeVotes() + 1;

		Utils.debugMessage("New total votes: " + newLifetime);

		setLifetimeVotes(newLifetime);
		setCurrentVoteCycle(getCurrentVoteCycle() + 1);

		int hoursSince = getHoursSince(getLastVoteTimeStamp());

		Utils.debugMessage("Hours since last vote: " + hoursSince);

		if(hoursSince >= 23 && hoursSince < 48) {
			int newVoteStreak = getCurrentVoteStreak() + 1;
			Utils.debugMessage("New vote streak: " + newVoteStreak);
			setCurrentVoteStreak(newVoteStreak);
			if(newVoteStreak > getLongestVoteStreak()){
				Utils.debugMessage("Is now the longest");
				setLongestVoteStreak(newVoteStreak);
			}
		}
		else if(hoursSince >= 48) {
			Utils.debugMessage("Broke vote streak");
			setCurrentVoteStreak(1);
		}
	}

	public void saveUnclaimedReward(String rewardName) {
		if(!VoteRoulette.DISABLE_UNCLAIMED) {
			if(VoteRoulette.USE_DATABASE) {
				//place holder for db code
			} else {
				ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
				List<String> rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
				rewardsList.add(rewardName);
				playerCfg.getConfig().set("unclaimedRewards", rewardsList);
				playerCfg.saveConfig();
			}
		}
	}

	public void removeUnclaimedReward(String rewardName) {
		if(!VoteRoulette.DISABLE_UNCLAIMED) {
			if(VoteRoulette.USE_DATABASE) {
				//place holder for db code
			} else {
				ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
				List<String> rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
				rewardsList.remove(rewardName);
				playerCfg.getConfig().set("unclaimedRewards", rewardsList);
				playerCfg.saveConfig();
			}
		}
	}

	public void removeUnclaimedRewards() {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("unclaimedRewards", null);
			playerCfg.saveConfig();
		}
	}

	public List<Reward> getUnclaimedRewards() {
		List<String> rewardsList = new ArrayList<String>();

		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
		}

		List<Reward> rewards = new ArrayList<Reward>();
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Rewards");
		if(cs != null) {
			for(String rewardName : rewardsList) {
				ConfigurationSection rewardOptions = cs.getConfigurationSection(rewardName);
				if (rewardOptions != null) {
					rewards.add(new Reward(rewardName, rewardOptions));
				} else {
					removeUnclaimedReward(rewardName);
				}
			}
		}
		return rewards;
	}
	public int getUnclaimedRewardCount() {
		List<String> rewardsList = new ArrayList<String>();

		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
		}
		return rewardsList.size();
	}

	public void saveUnclaimedMilestone(String milestoneName) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			List<String> milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
			milestonesList.add(milestoneName);
			playerCfg.getConfig().set("unclaimedMilestones", milestonesList);
			playerCfg.saveConfig();
		}
	}

	public void removeUnclaimedMilestone(String milestoneName) {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			List<String> milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
			milestonesList.remove(milestoneName);
			playerCfg.getConfig().set("unclaimedMilestones", milestonesList);
			playerCfg.saveConfig();
		}
	}

	public void removeUnclaimedMilestones() {
		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			playerCfg.getConfig().set("unclaimedMilestones", null);
			playerCfg.saveConfig();
		}
	}

	public int getUnclaimedMilestoneCount() {
		List<String> milestonesList = new ArrayList<String>();

		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
		}
		return milestonesList.size();
	}

	public List<Milestone> getUnclaimedMilestones() {
		List<String> milestonesList = new ArrayList<String>();

		if(VoteRoulette.USE_DATABASE) {
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "playerdata" + File.separator + uuid.toString() + ".yml");
			milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
		}

		List<Milestone> milestones = new ArrayList<Milestone>();
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Milestones");
		if(cs != null) {
			for(String milestoneName : milestonesList) {
				ConfigurationSection milestoneOptions = cs.getConfigurationSection(milestoneName);
				if (milestoneOptions != null) {
					milestones.add(new Milestone(milestoneName, milestoneOptions));
				} else {
					removeUnclaimedMilestone(milestoneName);
				}
			}
		}
		return milestones;
	}

	private void createFile(String path, String fileName) {
		(new File(path)).mkdirs();
		File file = new File(path + File.separator + fileName);

		if (!file.exists()) {
			try {
				file.createNewFile();
				System.out.println("[VoteRoulette] Created new player file: \"VoteRoulette/data/playerdata/" + fileName + "\".");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
