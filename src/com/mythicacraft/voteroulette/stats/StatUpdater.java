package com.mythicacraft.voteroulette.stats;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;


public class StatUpdater extends Thread {

	VoterManager vm = VoteRoulette.getVoterManager();

	private int currentFile = 0;
	private int totalFiles = 0;

	private ConfigAccessor statsData = new ConfigAccessor("data" + File.separator + "stats.yml");
	private String pluginFolder = Bukkit.getPluginManager().getPlugin("VoteRoulette").getDataFolder().getAbsolutePath();
	private String filePath = "";
	private File[] files;

	public StatUpdater() {
		if(!VoteRoulette.USE_DATABASE)  {
			if(VoteRoulette.USE_UUIDS) {
				filePath = "data" + File.separator + "playerdata";
			} else {
				filePath = "data" + File.separator + "players";
			}
			files = new File(pluginFolder + File.separator + filePath).listFiles();
			totalFiles = files.length;
		}
	}

	@Override
	public void run() {
		List<VoteStat> stats = new ArrayList<VoteStat>();
		if(VoteRoulette.USE_DATABASE)  {

		} else {
			if(files != null && files.length != 0) {
				int previousPercent = -1;
				for (File file : files) {
					int percent = (currentFile * 100) / totalFiles;
					if(previousPercent != percent && (percent % 10) == 0) {
						Bukkit.getLogger().info("[VoteRoulette] Updating stats: " + Integer.toString(percent) + "%");
						previousPercent = percent;
					}
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
					currentFile++;
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
			if(count != 20) {
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
			if(count != 20) {
				statsData.getConfig().set("vote-streaks.longest." + stat.getPlayerName(), stat.getLongestVoteStreak());
				count++;
			} else {
				break;
			}
		}
		statsData.saveConfig();
		Bukkit.getLogger().info("[VoteRoulette] ...Stats update completed!");
	}
}
