package com.mythicacraft.voteroulette.utils.database;

import java.io.File;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;


public class DatabaseImporter extends Thread {

	VoterManager vm = VoteRoulette.getVoterManager();

	private int currentFile = 0;
	private int totalFiles = 0;

	private String pluginFolder = Bukkit.getPluginManager().getPlugin("VoteRoulette").getDataFolder().getAbsolutePath();
	private String filePath = "";
	private File[] files;

	public DatabaseImporter() {
		if(VoteRoulette.USE_DATABASE)  {
			if(VoteRoulette.USE_UUIDS) {
				filePath = "data" + File.separator + "playerdata";
			} else {
				filePath = "data" + File.separator + "players";
			}
			File folderPath = new File(pluginFolder + File.separator + filePath);
			if(!folderPath.exists()) {
				folderPath.mkdirs();
			}
			files = folderPath.listFiles();
			if(files == null) {
				totalFiles = 0;
			} else {
				totalFiles = files.length;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		if(files != null && files.length != 0) {
			int previousPercent = -1;
			for (File file : files) {
				int percent = (currentFile * 100) / totalFiles;
				if(previousPercent != percent && (percent % 10) == 0) {
					Bukkit.getLogger().info("[VoteRoulette] Importing stats to database: " + Integer.toString(percent) + "%");
					previousPercent = percent;
				}
				if (file.isFile()) {
					if(file.isHidden()) continue;
					if(file.getName().endsWith(".yml")) {
						String voterID = file.getName().replace(".yml", "");
						ConfigAccessor playerCfg = new ConfigAccessor(filePath + File.separator + voterID);
						Voter voter;
						if(VoteRoulette.USE_UUIDS) {
							UUID uuid = UUID.fromString(voterID);
							String playerName = playerCfg.getConfig().getString("name", "");
							voter = vm.getVoter(uuid, playerName);
						} else {
							voter = vm.getVoter(voterID);
						}
						if(voter.isReal()) {
							HashMap<String, Object> data = voter.getFlatFileData();
							try {
								ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT EXISTS(SELECT 1 FROM vr_voters WHERE player_id = \""+ voterID +"\") as exists_check");
								rs.first();
								int exists = rs.getInt("exists_check");
								if(exists == 0) {
									VoteRoulette.getVRDatabase().updateSQL("INSERT INTO vr_voters VALUES (\""+ voterID +"\",\"" + (String)data.get("player_name") +"\"," + ((Integer)data.get("current_cycle")).toString() + "," + ((Integer)data.get("lifetime_votes")).toString() + "," + ((Integer)data.get("longest_vote_streak")).toString() + "," + ((Integer)data.get("current_vote_streak")).toString() + ")");
								} else {
									VoteRoulette.getVRDatabase().updateSQL("UPDATE vr_voters SET player_name = \"" + (String)data.get("player_name") + "\", current_cycle = " + ((Integer)data.get("current_cycle")).toString() + ", lifetime_votes = " + ((Integer)data.get("lifetime_votes")).toString() + ", longest_vote_streak = " + ((Integer)data.get("longest_vote_streak")).toString() + ", current_vote_streak = " + ((Integer)data.get("current_vote_streak")).toString() + " WHERE player_id = \"" + voter.getIdentifier() + "\"");
								}
								List<String> unclaimedRewards = (List<String>) data.get("unclaimed_rewards");
								if(unclaimedRewards != null && !unclaimedRewards.isEmpty()) {
									StringBuilder sb = new StringBuilder();
									sb.append("INSERT INTO vr_unclaimed_awards VALUES ");
									for(String reward : unclaimedRewards) {
										sb.append("(0, \""+ reward +"\", 0, \"" + voter.getIdentifier() +"\"),");
									}
									sb.deleteCharAt(sb.lastIndexOf(","));
									VoteRoulette.getVRDatabase().updateSQL(sb.toString());
								}
								List<String> unclaimedMilestones = (List<String>) data.get("unclaimed_milestones");
								if(unclaimedMilestones != null && !unclaimedMilestones.isEmpty()) {
									StringBuilder sb = new StringBuilder();
									sb.append("INSERT INTO vr_unclaimed_awards VALUES ");
									for(String milestone : unclaimedMilestones) {
										sb.append("(0, \""+ milestone +"\", 1, \"" + voter.getIdentifier() +"\"),");
									}
									sb.deleteCharAt(sb.lastIndexOf(","));
									VoteRoulette.getVRDatabase().updateSQL(sb.toString());
								}
							} catch (Exception e) {}
						}
					}
				}
				currentFile++;
			}
			Bukkit.getLogger().info("[VoteRoulette] Finsihed importing to database!");
		}
	}
}