package com.mythicacraft.voteroulette.listeners;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.voteroulette.PlayerManager;
import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {

	static ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");
	static FileConfiguration playerData = playerCfg.getConfig();
	static RewardManager rm = VoteRoulette.getRewardManager();
	static PlayerManager pm = VoteRoulette.getPlayerManager();

	private static VoteRoulette plugin;

	public VoteListener(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {

		Vote vote = event.getVote();

		new Thread(new VoteProcessor(vote.getUsername(), plugin, false)).start();

		pm.savePlayerLastVoteTimeStamp(vote.getUsername());
		pm.incrementPlayerVoteTotals(vote.getUsername());

		String voteMessage = plugin.SERVER_BROADCAST_MESSAGE;
		voteMessage = voteMessage.replace("%player%", vote.getUsername()).replace("%server%", Bukkit.getServerName()).replace("%site%", vote.getServiceName());

		if(plugin.LOG_TO_CONSOLE) {
			System.out.println(voteMessage);
		}

		if(plugin.BROADCAST_TO_SERVER) {
			if(plugin.ONLY_BROADCAST_ONLINE && !Utils.playerIsOnline(vote.getUsername())) return;
			Player[] onlinePlayers = Bukkit.getOnlinePlayers();
			for(Player player : onlinePlayers) {
				if(player.getName().equals(vote.getUsername())) continue;
				player.sendMessage(voteMessage);
			}
		}
	}
}

