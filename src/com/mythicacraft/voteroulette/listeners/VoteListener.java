package com.mythicacraft.voteroulette.listeners;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.voteroulette.VoteHandler;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {

	ConfigAccessor playerCfg = new ConfigAccessor("players.yml");

	private VoteRoulette plugin;

	public VoteListener(VoteRoulette plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		Player p = plugin.getServer().getPlayerExact(vote.getUsername());
		updatePlayerVoteTotals(vote.getUsername());
		VoteHandler.processVote(p);
	}

	void updatePlayerVoteTotals(String playername) {
		if(playerCfg.getConfig().contains(playername)) {
			int currentCycle = playerCfg.getConfig().getInt(playername + ".currentCycle");
			int lifetimeVotes = playerCfg.getConfig().getInt(playername + ".lifetimeVotes");
			lifetimeVotes = lifetimeVotes + 1;
			currentCycle = currentCycle + 1;
			playerCfg.getConfig().set(playername + ".currentCycle", currentCycle);
			playerCfg.getConfig().set(playername + ".lifetimeVotes", lifetimeVotes);
			playerCfg.saveConfig();
		} else {
			playerCfg.getConfig().addDefault(playername + ".currentCycle", 1);
			playerCfg.getConfig().addDefault(playername + ".lifetimeVotes", 1);
			playerCfg.saveConfig();
		}
	}
}

