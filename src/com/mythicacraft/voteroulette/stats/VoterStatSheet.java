package com.mythicacraft.voteroulette.stats;

import java.sql.ResultSet;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;


public class VoterStatSheet {

	private Voter voter;
	private int lifetimeVotes = 0;
	private int currentMonthVotes = 0;
	private int previousMonthVotes = 0;
	private int longestVoteStreak = 0;
	private int currentVoteStreak = 0;
	private int currentCycle = 0;


	public VoterStatSheet(Voter voter) {
		this.voter = voter;
		if(VoteRoulette.USE_DATABASE) {
			try {
				ResultSet rs = VoteRoulette.getVRDatabase().querySQL("SELECT vr_voters.current_cycle, vr_voters.lifetime_votes, vr_voters.current_vote_streak, vr_voters.longest_vote_streak, COUNT(CASE WHEN (vr_votes.player_id = \""+voter.getIdentifier()+"\" AND YEAR(vr_votes.datetime) = YEAR(NOW()) AND MONTH(vr_votes.datetime) = MONTH(NOW())) THEN vr_votes.datetime END) as current_month_votes, COUNT(CASE WHEN (vr_votes.player_id = \""+voter.getIdentifier()+"\" AND YEAR(vr_votes.datetime) = YEAR(NOW() - INTERVAL 1 MONTH) AND MONTH(vr_votes.datetime) = MONTH(NOW() - INTERVAL 1 MONTH)) THEN vr_votes.datetime END) as previous_month_votes FROM vr_votes, vr_voters WHERE vr_voters.player_id = \""+voter.getIdentifier()+"\"");
				rs.first();
				longestVoteStreak = rs.getInt("longest_vote_streak");
				currentVoteStreak = rs.getInt("current_vote_streak");
				lifetimeVotes = rs.getInt("lifetime_votes");
				currentMonthVotes = rs.getInt("current_month_votes");
				previousMonthVotes  = rs.getInt("previous_month_votes");
				currentCycle = rs.getInt("current_cycle");
			} catch (Exception e) {}
		} else {
			lifetimeVotes = voter.getLifetimeVotes();
			longestVoteStreak = voter.getLongestVoteStreak();
			currentVoteStreak = voter.getCurrentVoteStreak();
			currentCycle = voter.getCurrentVoteCycle();
		}
	}

	public enum StatType {
		TOTAL_VOTES, CURRENT_MONTH_VOTES, PREVIOUS_MONTH_VOTES, LONGEST_VOTE_STREAKS, CURRENT_VOTE_STREAK, CURRENT_CYCLE
	}

	public int getLifetimeVotes() {
		return lifetimeVotes;
	}

	public int getCurrentMonthVotes() {
		return currentMonthVotes;
	}

	public int getPreviousMonthVotes() {
		return previousMonthVotes;
	}

	public int getLongestVoteStreak() {
		return longestVoteStreak;
	}

	public int getCurrentVoteStreak() {
		return currentVoteStreak;
	}

	public int getCurrentCycle() {
		return currentCycle;
	}

	public String getPlayerName() {
		return voter.getPlayerName();
	}
}