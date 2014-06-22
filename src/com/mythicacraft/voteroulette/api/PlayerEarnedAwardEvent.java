package com.mythicacraft.voteroulette.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mythicacraft.voteroulette.awards.Award;

public class PlayerEarnedAwardEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private String playerName;
	private Award award;
	private boolean cancelled;

	public PlayerEarnedAwardEvent(String playerName, Award award) {
		this.playerName = playerName;
		this.award = award;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public Award getAward() {
		return award;
	}

	public void setAward(Award award) {
		this.award = award;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCanceled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
