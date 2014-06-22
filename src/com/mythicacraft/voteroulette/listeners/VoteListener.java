package com.mythicacraft.voteroulette.listeners;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.awards.AwardManager;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {

	static ConfigAccessor playerCfg = new ConfigAccessor(
	        "data" + File.separator + "players.yml");
	static FileConfiguration playerData = playerCfg.getConfig();
	static AwardManager rm = VoteRoulette.getAwardManager();
	static VoterManager vm = VoteRoulette.getVoterManager();

	private static VoteRoulette plugin;

	public VoteListener(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {

		Vote vote = event.getVote();

		Utils.debugMessage("Recieved vote from Votifier. Username: \"" + vote
		        .getUsername() + "\", Website: \"" + vote.getServiceName() + "\"");

		if (!vote.getUsername().trim().isEmpty()) {
			new Thread(new VoteProcessor(vote.getUsername(), plugin, false,
			        vote.getServiceName())).start();
		}

		Utils.saveKnownWebsite(vote.getServiceName());

	}
}
