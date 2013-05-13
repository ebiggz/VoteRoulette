package com.mythicacraft.votifierlistener.listeners;


import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.votifierlistener.VotifierListener;
import com.mythicacraft.votifierlistener.utils.ConfigAccessor;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {

	ConfigAccessor playerCfg = new ConfigAccessor("players.yml");

	private VotifierListener plugin;

	public VoteListener(VotifierListener plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void onVotifierEvent(VotifierEvent event) {
		Vote vote = event.getVote();
		processVote(vote);
	}

	void processVote(Vote vote) {
		String playername = vote.getUsername();
		boolean doRewardsOnThreshold = plugin.getConfig().getBoolean("doRewardsOnThreshold");
		int voteThreshold = plugin.getConfig().getInt("voteThreshold");
		if(playerCfg.getConfig().contains(playername)) {
			int currentCycle = playerCfg.getConfig().getInt(playername + ".currentCycle");
			int lifetimeVotes = playerCfg.getConfig().getInt(playername + ".lifetimeVotes");
			lifetimeVotes = lifetimeVotes + 1;
			currentCycle = currentCycle + 1;
			if(doRewardsOnThreshold && currentCycle >= voteThreshold) {
				//do rewards
				currentCycle = 0;
			} else {
				//do rewards
			}
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
