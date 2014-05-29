package com.mythicacraft.voteroulette.stats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;


public class BoardReset implements Runnable{

	Player user;
	Scoreboard board;
	public BoardReset(Player user, Scoreboard board) {
		this.user = user;
		this.board = board;
	}

	@Override
	public void run() {
		Scoreboard emptyBoard = Bukkit.getScoreboardManager().getNewScoreboard();
		if(user.getScoreboard() == board) {
			user.setScoreboard(emptyBoard);
		}
	}

}
