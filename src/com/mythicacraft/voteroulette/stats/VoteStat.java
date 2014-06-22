package com.mythicacraft.voteroulette.stats;

import com.mythicacraft.voteroulette.Voter;

public class VoteStat {

	private Voter voter;
	private int lifetimeVotes;
	private int monthVotes;
	private int longestVoteStreak;
	private int currentVoteStreak;

	public VoteStat(Voter voter) {
		this.voter = voter;
		lifetimeVotes = voter.getLifetimeVotes();
		longestVoteStreak = voter.getLongestVoteStreak();
		currentVoteStreak = voter.getCurrentVoteStreak();
	}

	public enum StatType {
		TOTAL_VOTES, MONTH_VOTES, LONGEST_VOTE_STREAKS
	}

	public int getLifetimeVotes() {
		return lifetimeVotes;
	}

	public void setLifetimeVotes(int lifetimeVotes) {
		this.lifetimeVotes = lifetimeVotes;
	}

	public int getLongestVoteStreak() {
		return longestVoteStreak;
	}

	public void setLongestVoteStreak(int longestVoteStreak) {
		this.longestVoteStreak = longestVoteStreak;
	}

	public int getCurrentVoteStreak() {
		return currentVoteStreak;
	}

	public void setCurrentVoteStreak(int currentVoteStreak) {
		this.currentVoteStreak = currentVoteStreak;
	}

	public int getMonthVotes() {
		return monthVotes;
	}

	public void setMonthVotes(int monthVotes) {
		this.monthVotes = monthVotes;
	}

	public String getPlayerName() {
		return voter.getPlayerName();
	}
}
