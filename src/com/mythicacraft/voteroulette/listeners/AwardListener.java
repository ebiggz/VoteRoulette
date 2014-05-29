package com.mythicacraft.voteroulette.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.api.PlayerReceivedAwardEvent;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.utils.Utils;




public class AwardListener implements Listener {

	private VoteRoulette plugin;

	public AwardListener(VoteRoulette plugin) {
		this.plugin = plugin;
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onJoin(PlayerReceivedAwardEvent event) {
		if(event.getAward().getAwardType() == AwardType.MILESTONE && plugin.FIREWORK_ON_MILESTONE) {
			Location fireworkLoc = event.getPlayer().getLocation();
			fireworkLoc.setY(fireworkLoc.getY()+2);
			Utils.randomFireWork(fireworkLoc);
		}
	}
}
