package com.mythicacraft.voteroulette;




public class VoterManager {

	@SuppressWarnings("unused")
	private VoteRoulette plugin;

	VoterManager(VoteRoulette instance) {
		plugin = instance;
	}

	public Voter getVoter(String playerName) {
		return new Voter(playerName);
	}

}
