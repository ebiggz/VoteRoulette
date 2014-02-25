package com.mythicacraft.voteroulette;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;


public class DelayedCommand extends BukkitRunnable {


	private String command;
	private String player;
	boolean runOnLogOff;

	public DelayedCommand(String command, String playerName, boolean runOnLogOff) {
		this.command = command;
		this.player = playerName;
		this.runOnLogOff = runOnLogOff;
	}

	@Override
	public void run() {
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player));
		if(VoteRoulette.delayedCommands.contains(this)) {
			VoteRoulette.delayedCommands.remove(this);
		}
	}

	public String getCommand() {
		return command;
	}

	public String getPlayer() {
		return player;
	}

	public boolean shouldRunOnLogOff() {
		return runOnLogOff;
	}
}
