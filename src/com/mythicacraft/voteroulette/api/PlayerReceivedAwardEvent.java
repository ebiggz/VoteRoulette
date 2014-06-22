package com.mythicacraft.voteroulette.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mythicacraft.voteroulette.awards.Award;

public class PlayerReceivedAwardEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private Award award;

	public PlayerReceivedAwardEvent(Player player, Award award) {
		this.player = player;
		this.award = award;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Player getPlayer() {
		return player;
	}

	public Award getAward() {
		return award;
	}
}
