package com.mythicacraft.voteroulette;

import java.util.UUID;




public class VoterManager {

	@SuppressWarnings("unused")
	private VoteRoulette plugin;

	VoterManager(VoteRoulette instance) {
		plugin = instance;
	}

	public Voter getVoter(String playerName) {
		return new Voter(playerName);
	}

	public Voter getVoter(UUID uuid, String playerName) {
		if(uuid == null) return null;
		return new Voter(uuid, playerName);
	}
}
