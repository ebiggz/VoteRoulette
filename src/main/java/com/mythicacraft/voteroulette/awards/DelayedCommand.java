package com.mythicacraft.voteroulette.awards;

import com.mythicacraft.voteroulette.VoteRoulette;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;


public class DelayedCommand extends BukkitRunnable {


	private String command;
	private String player;
	boolean runOnLogOff;
	boolean runOnShutdown;

	public DelayedCommand(String command, String playerName, boolean runOnLogOff, boolean runOnShutdown) {
		this.command = command;
		this.player = playerName;
		this.runOnLogOff = runOnLogOff;
		this.runOnShutdown = runOnShutdown;
	}

	@Override
	public void run() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.runTask(VoteRoulette.getPlugin(), new Runnable() {
			@Override
			public void run() {
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player));
				if(VoteRoulette.delayedCommands.contains(this)) {
					VoteRoulette.delayedCommands.remove(this);
				}
			}
		});
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

	public boolean shouldRunOnShutdown() {
		return runOnShutdown;
	}
}
