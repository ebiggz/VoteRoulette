package com.mythicacraft.voteroulette.stats;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.Voter.Stat;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;


public class StatWiper implements Runnable {

	VoterManager vm = VoteRoulette.getVoterManager();

	private int currentFile = 0;
	private int totalFiles = 0;

	private String pluginFolder = Bukkit.getPluginManager().getPlugin("VoteRoulette").getDataFolder().getAbsolutePath();
	private String filePath = "";
	private File[] files;
	private Stat stat;

	public StatWiper(Stat stat) {
		if(!VoteRoulette.USE_DATABASE)  {
			if(VoteRoulette.USE_UUIDS) {
				filePath = "data" + File.separator + "playerdata";
			} else {
				filePath = "data" + File.separator + "players";
			}
			files = new File(pluginFolder + File.separator + filePath).listFiles();
			totalFiles = files.length;
		}
		this.stat = stat;
	}

	@Override
	public void run() {
		if(files != null && files.length != 0) {
			int previousPercent = -1;
			for (File file : files) {
				int percent = (currentFile * 100) / totalFiles;
				if(previousPercent != percent && (percent % 10) == 0) {
					Bukkit.getLogger().info("[VoteRoulette] Wiping stats: " + Integer.toString(percent) + "%");
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
							voter.wipeStat(stat);
						}
					}
				}
				currentFile++;
			}
		}
	}
}
