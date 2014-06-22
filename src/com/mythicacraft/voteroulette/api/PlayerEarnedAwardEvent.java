package com.mythicacraft.voteroulette.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mythicacraft.voteroulette.awards.Award;

public class PlayerEarnedAwardEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private String playerName;
	private Award award;

	private boolean cancelled;

	public PlayerEarnedAwardEvent(String playerName, Award award) {
		this.playerName = playerName;
		this.award = award;
	}

	public Award getAward() {
		return award;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public String getPlayerName() {
		return playerName;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setAward(Award award) {
		this.award = award;
	}

	public void setCanceled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}
}
