package com.mythicacraft.voteroulette.stats;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.Voter.Stat;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.database.DatabaseImporter;


public class StatManager {

	protected StatManager() { /*exists to block instantiation*/ }
	private static StatManager instance = null;
	public static StatManager getInstance() {
		if(instance == null) {
			instance = new StatManager();
		}
		return instance;
	}

	public List<VoterStat> getTopLifetimeVotes() {
		List<VoterStat> topStats = new ArrayList<VoterStat>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT player_name, lifetime_votes FROM vr_voters ORDER BY lifetime_votes DESC LIMIT 10");
				while(rs.next()) {
					topStats.add(new VoterStat(rs.getString("player_name"), rs.getInt("lifetime_votes")));
				}
			} catch (Exception e) {}
		}
		else {
			ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
			ConfigurationSection cs = statsData.getConfig().getConfigurationSection("vote-totals.lifetime");
			if(cs == null) {
				return topStats;
			}
			Set<String> keys = cs.getKeys(false);
			if(keys == null) {
				return topStats;
			}
			for(String key : keys) {
				topStats.add(new VoterStat(key, cs.getInt(key)));
			}
		}
		return topStats;
	}

	public List<VoterStat> getTopCurrentMonthVotes() {
		List<VoterStat> topStats = new ArrayList<VoterStat>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT vr_voters.player_name, count(vr_votes.player_id) as vote_count FROM vr_votes INNER JOIN vr_voters ON vr_votes.player_id = vr_voters.player_id WHERE YEAR(vr_votes.datetime) = YEAR(NOW()) AND MONTH(vr_votes.datetime) = MONTH(NOW()) GROUP BY vr_votes.player_id ORDER BY 2 DESC");
				while(rs.next()) {
					topStats.add(new VoterStat(rs.getString("player_name"), rs.getInt("vote_count")));
				}
			} catch (Exception e) {}
		}
		return topStats;
	}

	public List<VoterStat> getTopPreviousMonthVotes() {
		List<VoterStat> topStats = new ArrayList<VoterStat>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT vr_voters.player_name, count(vr_votes.player_id) as vote_count FROM vr_votes INNER JOIN vr_voters ON vr_votes.player_id = vr_voters.player_id WHERE YEAR(vr_votes.datetime) = YEAR(CURRENT_DATE - INTERVAL 1 MONTH) AND MONTH(vr_votes.datetime) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) GROUP BY vr_votes.player_id ORDER BY 2 DESC");
				while(rs.next()) {
					topStats.add(new VoterStat(rs.getString("player_name"), rs.getInt("vote_count")));
				}
			} catch (Exception e) {}
		}
		return topStats;
	}

	public List<VoterStat> getTopLongestVotestreaks() {
		List<VoterStat> topStats = new ArrayList<VoterStat>();
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT player_name, longest_vote_streak FROM vr_voters ORDER BY longest_vote_streak DESC LIMIT 10");
				while(rs.next()) {
					topStats.add(new VoterStat(rs.getString("player_name"), rs.getInt("longest_vote_streak")));
				}
			} catch (Exception e) {}
		}
		else {
			ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
			ConfigurationSection cs = statsData.getConfig().getConfigurationSection("vote-streaks.longest");
			if(cs == null) {
				return topStats;
			}
			Set<String> keys = cs.getKeys(false);
			if(keys == null) {
				return topStats;
			}
			for(String key : keys) {
				topStats.add(new VoterStat(key, cs.getInt(key)));
			}
		}
		return topStats;
	}

	public void updateAllStats() {
		if(VoteRoulette.USE_DATABASE) return;
		Bukkit.getLogger().info("[VoteRoulette] All stats update started. You may experience lag...");
		new StatUpdater().start();
	}

	public void importStatsToDatabase() {
		if(!VoteRoulette.USE_DATABASE) return;
		Bukkit.getLogger().info("[VoteRoulette] Stats import started. You may experience lag...");
		new DatabaseImporter().start();
	}

	public void wipeStats(Stat stat) {
		Bukkit.getLogger().info("[VoteRoulette] " + stat.toString().replace("_", " ") + " stats wipe started. You may experience lag...");
		new StatWiper(stat).run();
	}

	public void updateStatsWithPlayer(String playerName) {
		if(VoteRoulette.USE_DATABASE) return;

		ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
		Voter voter = VoteRoulette.getVoterManager().getVoter(playerName);

		//longest votestreak
		List<VoterStat> longestVotestreaks = this.getTopLongestVotestreaks();
		boolean hasStat = false;
		if(longestVotestreaks != null) {
			for(VoterStat vs : longestVotestreaks) {
				if(vs.getPlayerName().equals(voter.getPlayerName())) {
					vs.setStatCount(voter.getLongestVoteStreak());
					hasStat = true;
					break;
				}
			}
		} else {
			longestVotestreaks = new ArrayList<VoterStat>();
		}
		if(!hasStat) {
			longestVotestreaks.add(new VoterStat(voter.getPlayerName(), voter.getLongestVoteStreak()));
		}
		Collections.sort(longestVotestreaks, new Comparator<VoterStat>(){
			public int compare(VoterStat v1, VoterStat v2) {
				return v2.getStatCount() - v1.getStatCount();
			}
		});
		statsData.getConfig().set("vote-streaks.longest", null);
		statsData.saveConfig();
		int count = 0;
		for(VoterStat stat : longestVotestreaks) {
			if(count != 20) {
				statsData.getConfig().set("vote-streaks.longest." + stat.getPlayerName(), stat.getStatCount());
				count++;
			} else {
				break;
			}
		}
		statsData.saveConfig();

		//lifetime votes
		List<VoterStat> lifetimeVotes = this.getTopLifetimeVotes();
		boolean hasStat2 = false;
		if(lifetimeVotes != null) {
			for(VoterStat vs : lifetimeVotes) {
				if(vs.getPlayerName().equals(voter.getPlayerName())) {
					vs.setStatCount(voter.getLifetimeVotes());
					hasStat2 = true;
					break;
				}
			}
		} else {
			lifetimeVotes = new ArrayList<VoterStat>();
		}
		if(!hasStat2) {
			lifetimeVotes.add(new VoterStat(voter.getPlayerName(), voter.getLifetimeVotes()));
		}
		Collections.sort(lifetimeVotes, new Comparator<VoterStat>(){
			public int compare(VoterStat v1, VoterStat v2) {
				return v2.getStatCount() - v1.getStatCount();
			}
		});
		statsData.getConfig().set("vote-totals.lifetime", null);
		statsData.saveConfig();
		int count2 = 0;
		for(VoterStat stat : lifetimeVotes) {
			if(count2 != 20) {
				statsData.getConfig().set("vote-totals.lifetime." + stat.getPlayerName(), stat.getStatCount());
				count++;
			} else {
				break;
			}
		}
		statsData.saveConfig();
	}
}
