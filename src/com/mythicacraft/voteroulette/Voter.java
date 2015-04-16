package com.mythicacraft.voteroulette;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.stats.VoterStatSheet;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.UUIDFetcher;
import com.mythicacraft.voteroulette.utils.Utils;


public class Voter {

	private Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");
	private UUID uuid;
	private String playerName;
	private boolean isReal = true;
	private String folderName;

	public Voter(String playerName) {
		Utils.debugMessage("Getting voter data for: " + playerName);
		if(VoteRoulette.USE_UUIDS) {
			Utils.debugMessage("VoteRoulette is using UUIDs");
			folderName = "playerdata";
			UUID id = findUUID(playerName);
			if(id != null) {
				Utils.debugMessage("Success!");
				this.uuid = id;
				this.playerName = playerName;
				Utils.saveKnownNameUUID(playerName, id);
			} else {
				Utils.debugMessage("Failed! Could not get a uuid for the player at all.");
				isReal = false;
				return;
			}
		} else {
			Utils.debugMessage("VoteRoulette is not using UUIDs, using playername.");
			folderName = "players";
			this.playerName = playerName;
		}
		createVoter();
	}

	public Voter(UUID id, String playerName) {
		if(id != null) {
			this.uuid = id;
			this.playerName = playerName;
			folderName = "playerdata";
			createVoter();
		} else {
			isReal = false;
		}
	}

	private void createVoter() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("INSERT IGNORE INTO vr_voters VALUES (\""+ this.getIdentifier() +"\",\"" + this.getPlayerName() +"\",0,0,0,0)");
			} catch (Exception e) {}
		} else {
			createFile(plugin.getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + folderName, this.getIdentifier() + ".yml");
		}
		this.setPlayerName(playerName);
	}

	private UUID findUUID(String playerName) {
		Utils.debugMessage("Attemping to get uuid from online player... ");
		//if the player is online, get the uuid from that
		Player player = Bukkit.getPlayerExact(playerName);
		if(player != null) {
			return player.getUniqueId();
		}
		Utils.debugMessage("Player is offline. Attempting to get uuid from local cache...");
		//if not, check to see if the uuid has been cached locally
		UUID cachedID = Utils.searchCacheForID(playerName);
		if(cachedID != null) {
			return cachedID;
		}

		Utils.debugMessage("Cache is empty. Attempting to get uuid from Mojang server...");
		//as a last resort, attempt to contact Mojang for the UUID.
		try {
			return UUIDFetcher.getUUIDOf(playerName);
		} catch (Exception e) {
			return null;
		}
	}

	public String getFilePath() {
		return "data" + File.separator + folderName + File.separator + getIdentifier() + ".yml";
	}
	public UUID getUUID() {
		return uuid;
	}

	public String getIdentifier() {
		if(VoteRoulette.USE_UUIDS) {
			return getUUID().toString();
		} else {
			return getPlayerName();
		}
	}

	public boolean isReal() {
		return isReal;
	}

	public HashMap<String, Object> getFlatFileData() {
		HashMap<String,Object> data = new HashMap<String, Object>();
		ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
		data.put("lifetime_votes", playerCfg.getConfig().getInt("lifetimeVotes", 0));
		data.put("current_vote_streak", playerCfg.getConfig().getInt("currentVoteStreak", 0));
		data.put("longest_vote_streak", playerCfg.getConfig().getInt("longestVoteStreak", 0));
		data.put("player_name", playerCfg.getConfig().getString("name", ""));
		data.put("current_cycle", playerCfg.getConfig().getInt("currentCycle", 0));
		data.put("unclaimed_rewards", playerCfg.getConfig().getStringList("unclaimedRewards"));
		data.put("unclaimed_milestones", playerCfg.getConfig().getStringList("unclaimedMilestones"));
		return data;
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
		int lifetimeVotes = 0;
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT lifetime_votes FROM vr_voters WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				lifetimeVotes = rs.getInt("lifetime_votes");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			lifetimeVotes = playerCfg.getConfig().getInt("lifetimeVotes", 0);
		}
		return lifetimeVotes;
	}

	public void setCurrentVoteStreak(int voteStreak) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET current_vote_streak = " + voteStreak + " WHERE player_id = \"" + this.getIdentifier() + "\"");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("currentVoteStreak", voteStreak);
			playerCfg.saveConfig();
		}
	}

	public int getCurrentVoteStreak() {
		int voteStreak = 0;
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT current_vote_streak FROM vr_voters WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				voteStreak = rs.getInt("current_vote_streak");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			voteStreak = playerCfg.getConfig().getInt("currentVoteStreak", 0);
		}
		return voteStreak;
	}

	public void setLongestVoteStreak(int voteStreak) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET longest_vote_streak = " + voteStreak + " WHERE player_id = \"" + this.getIdentifier() + "\"");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("longestVoteStreak", voteStreak);
			playerCfg.saveConfig();
		}
	}

	public int getLongestVoteStreak() {
		int voteStreak = 0;
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT longest_vote_streak FROM vr_voters WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				voteStreak = rs.getInt("longest_vote_streak");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			voteStreak = playerCfg.getConfig().getInt("longestVoteStreak", 0);
		}
		return voteStreak;
	}

	public boolean hasntVotedInADay() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT EXISTS(SELECT 1 FROM vr_votes WHERE player_id = \"" + this.getIdentifier() + "\" HAVING MAX(datetime) <= now() - INTERVAL 1 DAY) as 24h_check");
				rs.first();
				int check = rs.getInt("24h_check");
				if(check == 1) return true;
			} catch (Exception e) {}
			return false;
		} else {
			String lastVoteTimeStamp = getLastVoteTimeStamp();
			if(lastVoteTimeStamp == null || lastVoteTimeStamp.equals("")) return false;
			int hours = getHoursSinceLastVote();
			if(hours >= 24) return true;
			return false;
		}
	}

	public void saveLastVoteTimeStamp() {
		if(!VoteRoulette.USE_DATABASE) {
			String timeStamp = Utils.getTime();
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("lastVote", timeStamp);
			playerCfg.saveConfig();
		}
	}

	public void setLastVoteTimeStamp(String timeStamp) {
		if(VoteRoulette.USE_DATABASE) return;
		ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
		playerCfg.getConfig().set("lastVote", timeStamp);
		playerCfg.saveConfig();
	}



	public void setPlayerName(String name) {
		this.playerName = name;
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET player_name = " + name + " WHERE player_id = \"" + this.getIdentifier() + "\"");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("name", name);
			playerCfg.saveConfig();
		}
	}

	public String getPlayerName() {
		String name = "";
		if(playerName != null && !playerName.isEmpty()) {
			name = playerName;
		}
		else if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT player_name FROM vr_voters WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				name = rs.getString("player_name");
			} catch (Exception e) {}
		}
		else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			name = playerCfg.getConfig().getString("name", "");
		}
		return name;
	}

	public String getLastVoteTimeStamp() {
		String timeStamp = "";
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT MAX(datetime) AS last_vote FROM vr_votes WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				timeStamp = rs.getString("last_vote");
			} catch (Exception e) {e.printStackTrace();}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			timeStamp = playerCfg.getConfig().getString("lastVote", "");
		}
		return timeStamp;
	}

	public boolean hasLastVoteTimeStamp() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT MAX(datetime) AS last_vote FROM vr_votes WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				return rs != null && rs.getString("last_vote") != null;
			} catch (Exception e) {}
			return false;
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			if(playerCfg.getConfig().contains("lastVote")) return true;
			return false;
		}
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(playerName);
	}

	public int getHoursSinceLastVote() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT TIMESTAMPDIFF(HOUR, MAX(datetime), now()) AS hours_since FROM vr_votes WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				return rs.getInt("hours_since");
			} catch (Exception e) {}
			return 0;
		}
		else {

			String time = getLastVoteTimeStamp();
			Calendar cal = Calendar.getInstance();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

			String currentTime = sdf.format(cal.getTime());

			Date date1 = null;
			Date date2 = null;
			try {
				date1 = sdf.parse(time);
				date2 = sdf.parse(currentTime);
			} catch (Exception e) {
				return 0;
			}

			Long differnceInMills = date2.getTime() - date1.getTime();

			long timeInMinutes = differnceInMills/60000;
			int totalMinutes = (int) timeInMinutes;

			return totalMinutes/60;
		}
	}

	public int getCurrentVoteCycle() {
		int voteCycle = 0;
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT current_cycle FROM vr_voters WHERE player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				voteCycle = rs.getInt("current_cycle");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			voteCycle = playerCfg.getConfig().getInt("currentCycle", 0);
		}
		return voteCycle;
	}

	public void setLifetimeVotes(int lifetimeVotes) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET lifetime_votes = " + lifetimeVotes + " WHERE player_id = \"" + this.getIdentifier() + "\"");
			} catch (Exception e) {}
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("lifetimeVotes", lifetimeVotes);
			playerCfg.saveConfig();
		}
	}

	public void setCurrentVoteCycle(int currentCycle) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET current_cycle = " + currentCycle + " WHERE player_id = \"" + this.getIdentifier() + "\"");
			} catch (Exception e) {}
			//place holder for db code
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("currentCycle", currentCycle);
			playerCfg.saveConfig();
		}
	}

	public void incrementVoteTotals() {

		int newLifetime = getLifetimeVotes() + 1;

		Utils.debugMessage("New total votes: " + newLifetime);

		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET lifetime_votes = lifetime_votes + 1, current_cycle = current_cycle + 1 WHERE player_id = \"" + this.getIdentifier() + "\"");
			} catch (Exception e) {}
		} else {
			setLifetimeVotes(newLifetime);
			setCurrentVoteCycle(getCurrentVoteCycle() + 1);
		}

		int hoursSince = getHoursSinceLastVote();

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

	public boolean lastVoteWasToday() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT * FROM vr_votes WHERE YEAR(datetime) = YEAR(NOW()) AND MONTH(datetime) = MONTH(NOW()) AND DAY(datetime) = DAY(NOW()) AND player_id = \"" + this.getIdentifier() + "\" LIMIT 1");
				if(rs.next()) {
					return true;
				}
			} catch (Exception e) {}
			return false;
		}

		Calendar cal = Calendar.getInstance();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		String currentTime = sdf.format(cal.getTime());

		Date last = null;
		Date current = new Date();

		try {
			last = sdf.parse(getLastVoteTimeStamp());
			current = sdf.parse(currentTime);
			if(current.after(last)) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public void updateTimedStats() {
		if(!VoteRoulette.USE_DATABASE) {
			if(!lastVoteWasToday()) {
				setVotesForTheDay(0);
			}
		}
	}

	public void setVotesForTheDay(int count) {
		if(!VoteRoulette.USE_DATABASE) {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("votesToday", count);
			playerCfg.saveConfig();
		}
	}

	public int getVotesForTheDay() {
		int votesToday = 0;
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT count(player_id) FROM vr_votes WHERE YEAR(datetime) = YEAR(NOW()) AND MONTH(datetime) = MONTH(NOW()) AND DAY(datetime) = DAY(NOW()) AND player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				votesToday = rs.getInt("count(player_id)");
			} catch (Exception e) {e.printStackTrace();}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			votesToday = playerCfg.getConfig().getInt("votesToday", 0);
		}
		return votesToday;
	}

	public void saveUnclaimedReward(String rewardName) {
		if(!VoteRoulette.DISABLE_UNCLAIMED) {
			if(VoteRoulette.USE_DATABASE) {
				if(VoteRoulette.USE_DATABASE) {
					try {
						VoteRoulette.getVRDatabase().updateSQL("INSERT INTO vr_unclaimed_awards VALUES (0, \""+ rewardName +"\", 0, \"" + this.getIdentifier() +"\")");
					} catch (Exception e) {}
				} else {
					ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
					List<String> rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
					rewardsList.add(rewardName);
					playerCfg.getConfig().set("unclaimedRewards", rewardsList);
					playerCfg.saveConfig();
				}
			}
		}
	}

	public VoterStatSheet getStatSheet() {
		return new VoterStatSheet(this);
	}

	public void removeUnclaimedReward(String rewardName) {
		if(!VoteRoulette.DISABLE_UNCLAIMED) {
			if(VoteRoulette.USE_DATABASE) {
				try {
					VoteRoulette.getVRDatabase().updateSQL("DELETE FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_name = \"" + rewardName + "\" AND award_type = 0 LIMIT 1");
				} catch (Exception e) {e.printStackTrace();}
			} else {
				ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
				List<String> rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
				rewardsList.remove(rewardName);
				playerCfg.getConfig().set("unclaimedRewards", rewardsList);
				playerCfg.saveConfig();
			}
		}
	}

	public void removeUnclaimedRewards() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("DELETE FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_type = 0");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("unclaimedRewards", null);
			playerCfg.saveConfig();
		}
	}

	public List<Reward> getUnclaimedRewards() {
		List<String> rewardsList = new ArrayList<String>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT * FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_type = 0");
				while(rs.next()) {
					rewardsList.add(rs.getString("award_name"));
				}
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
		}
		List<Reward> rewards = new ArrayList<Reward>();
		for(String rewardName : rewardsList) {
			Reward reward = (Reward) VoteRoulette.getAwardManager().getAwardByName(rewardName, AwardType.REWARD);
			if (reward != null) {
				rewards.add(reward);
			} else {
				removeUnclaimedReward(rewardName);
			}
		}
		return rewards;
	}

	public int getUnclaimedRewardCount() {
		List<String> rewardsList = new ArrayList<String>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT count(unclaimed_id) as unclaimed_count FROM vr_unclaimed_awards WHERE award_type = 0 AND player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				return rs.getInt("unclaimed_count");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			rewardsList = playerCfg.getConfig().getStringList("unclaimedRewards");
		}
		return rewardsList.size();
	}

	public void saveUnclaimedMilestone(String milestoneName) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("INSERT INTO vr_unclaimed_awards VALUES (0, \""+ milestoneName +"\", 1, \"" + this.getIdentifier() +"\")");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			List<String> milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
			milestonesList.add(milestoneName);
			playerCfg.getConfig().set("unclaimedMilestones", milestonesList);
			playerCfg.saveConfig();
		}
	}

	public void saveUnclaimedAward(Award award) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("INSERT INTO vr_unclaimed_awards VALUES (0, \""+ award.getName() +"\", " + (award.getAwardType() == AwardType.REWARD ? 0 : 1)  + ", \"" + this.getIdentifier() +"\")");
			} catch (Exception e) {}
		} else {
			if(award.getAwardType() == AwardType.REWARD) {
				this.saveUnclaimedReward(award.getName());
			} else {
				this.saveUnclaimedMilestone(award.getName());
			}
		}
	}

	public void removeUnclaimedAward(Award award) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("DELETE FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_name = \"" + award.getName() + "\" AND award_type = " + (award.getAwardType() == AwardType.REWARD ? 0 : 1)  + " LIMIT 1");
			} catch (Exception e) {}
		} else {
			if(award.getAwardType() == AwardType.REWARD) {
				this.removeUnclaimedReward(award.getName());
			} else {
				this.removeUnclaimedMilestone(award.getName());
			}
		}
	}

	public void removeUnclaimedMilestone(String milestoneName) {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("DELETE FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_name = \"" + milestoneName + "\" AND award_type = 1 LIMIT 1");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			List<String> milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
			milestonesList.remove(milestoneName);
			playerCfg.getConfig().set("unclaimedMilestones", milestonesList);
			playerCfg.saveConfig();
		}
	}

	public void removeUnclaimedMilestones() {
		if(VoteRoulette.USE_DATABASE) {
			try {
				VoteRoulette.getVRDatabase().updateSQL("DELETE FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_type = 1");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			playerCfg.getConfig().set("unclaimedMilestones", null);
			playerCfg.saveConfig();
		}
	}

	public int getUnclaimedMilestoneCount() {
		List<String> milestonesList = new ArrayList<String>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT count(unclaimed_id) as unclaimed_count FROM vr_unclaimed_awards WHERE award_type = 1 AND player_id = \"" + this.getIdentifier() + "\"");
				rs.first();
				return rs.getInt("unclaimed_count");
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
		}
		return milestonesList.size();
	}

	public List<Milestone> getUnclaimedMilestones() {
		List<String> milestonesList = new ArrayList<String>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT * FROM vr_unclaimed_awards WHERE player_id = \"" + this.getIdentifier() + "\" AND award_type = 1");
				while(rs.next()) {
					milestonesList.add(rs.getString("award_name"));
				}
			} catch (Exception e) {}
		} else {
			ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
			milestonesList = playerCfg.getConfig().getStringList("unclaimedMilestones");
		}
		List<Milestone> milestones = new ArrayList<Milestone>();
		for(String milestoneName : milestonesList) {
			Milestone milestone = (Milestone) VoteRoulette.getAwardManager().getAwardByName(milestoneName, AwardType.MILESTONE);
			if (milestone != null) {
				milestones.add(milestone);
			} else {
				removeUnclaimedMilestone(milestoneName);
			}
		}
		return milestones;
	}

	private void convertKeys() {
		//convert flat file to a new save structor
	}

	private void createFile(String path, String fileName) {
		(new File(path)).mkdirs();
		File file = new File(path + File.separator + fileName);
		if (!file.exists()) {
			try {
				file.createNewFile();
				Utils.debugMessage("Created new player file: \"" + fileName + "\".");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ConfigAccessor playerCfg = new ConfigAccessor(getFilePath());
		double version = playerCfg.getConfig().getDouble("config-version", 1.0);
		if(version == 1.0) {
			convertKeys();
		}
	}
}
