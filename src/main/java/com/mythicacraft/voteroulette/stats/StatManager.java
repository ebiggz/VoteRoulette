package com.mythicacraft.voteroulette.stats;

import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class StatManager {

	private static StatManager Instance = new StatManager();
	private StatUpdater su;

	private StatManager() {
		su = new StatUpdater();
	}
	public static synchronized StatManager getInstance() {
		return Instance;
	}

	public List<VoterStat> getTopLifetimeVotes() {
		List<VoterStat> topStats = new ArrayList<VoterStat>();
		ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
		ConfigurationSection cs = statsData.getConfig().getConfigurationSection("vote-totals.lifetime");
		if(cs == null) {
			return null;
		}
		Set<String> keys = cs.getKeys(false);
		if(keys == null) {
			return null;
		}
		for(String key : keys) {
			topStats.add(new VoterStat(key, cs.getInt(key)));
		}
		return topStats;
	}

	public List<VoterStat> getTopLongestVotestreaks() {
		List<VoterStat> topStats = new ArrayList<VoterStat>();
		ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
		ConfigurationSection cs = statsData.getConfig().getConfigurationSection("vote-streaks.longest");
		if(cs == null) {
			return null;
		}
		Set<String> keys = cs.getKeys(false);
		if(keys == null) {
			return null;
		}
		for(String key : keys) {
			topStats.add(new VoterStat(key, cs.getInt(key)));
		}
		return topStats;
	}

	public void updateStats() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("VoteRoulette"), new Runnable() {
			@Override
			public void run() {
				Utils.debugMessage("Top stats update thread initiated.");
				su.run();
			}
		});
	}
}
