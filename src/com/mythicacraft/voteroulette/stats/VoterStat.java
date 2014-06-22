package com.mythicacraft.voteroulette.stats;

public class VoterStat {

	private String playerName;
	private int count;

	public VoterStat(String playerName, int statCount) {
		this.playerName = playerName;
		this.count = statCount;
	}

	public String getPlayerName() {
		return playerName;
	}

	public int getStatCount() {
		return count;
	}

}
