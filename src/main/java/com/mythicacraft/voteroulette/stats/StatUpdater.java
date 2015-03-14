package com.mythicacraft.voteroulette.stats;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.*;


class StatUpdater implements Runnable {

	VoterManager vm = VoteRoulette.getVoterManager();

	StatUpdater() {
	}

	@Override
	public void run() {
		ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
		List<VoteStat> stats = new ArrayList<VoteStat>();
		if(VoteRoulette.USE_DATABASE)  {

		} else {
			String pluginFolder = Bukkit.getPluginManager().getPlugin("VoteRoulette").getDataFolder().getAbsolutePath();
			String filePath = "";
			if(VoteRoulette.USE_UUIDS) {
				filePath = "data" + File.separator + "playerdata";
			} else {
				filePath = "data" + File.separator + "players";
			}
			File[] files = new File(pluginFolder + File.separator + filePath).listFiles();
			if(files != null && files.length != 0) {
				for (File file : files) {
					if (file.isFile()) {
						if(file.isHidden()) continue;
						if(file.getName().endsWith(".yml")) {
							String fileID = file.getName();
							ConfigAccessor playerCfg = new ConfigAccessor(filePath + File.separator + fileID);
							Voter voter;
							if(VoteRoulette.USE_UUIDS) {
								UUID uuid = UUID.fromString(fileID.replace(".yml", ""));
								String playerName = playerCfg.getConfig().getString("name", "");
								voter = vm.getVoter(uuid, playerName);
							} else {
								voter = vm.getVoter(fileID.replace(".yml", ""));
							}
							if(voter.isReal()) {
								stats.add(new VoteStat(voter));
							}
						}
					}
				}
			}
		}

		Collections.sort(stats, new Comparator<VoteStat>(){
			public int compare(VoteStat v1, VoteStat v2) {
				return v2.getLifetimeVotes() - v1.getLifetimeVotes();
			}
		});

		//clear stats
		statsData.getConfig().set("vote-totals.lifetime", null);
		statsData.saveConfig();

		//add stats for top timetime votes
		int count = 0;
		for(VoteStat stat : stats) {
			if(count != 10) {
				statsData.getConfig().set("vote-totals.lifetime." + stat.getPlayerName(), stat.getLifetimeVotes());
				count++;
			} else {
				break;
			}
		}

		//add stats for top longest streaks
		count = 0;
		statsData.saveConfig();
		Collections.sort(stats, new Comparator<VoteStat>(){
			public int compare(VoteStat v1, VoteStat v2) {
				return v2.getLongestVoteStreak() - v1.getLongestVoteStreak();
			}
		});

		statsData.getConfig().set("vote-streaks.longest", null);
		statsData.saveConfig();
		for(VoteStat stat : stats) {
			if(count != 10) {
				statsData.getConfig().set("vote-streaks.longest." + stat.getPlayerName(), stat.getLongestVoteStreak());
				count++;
			} else {
				break;
			}
		}
		statsData.saveConfig();
	}
}
