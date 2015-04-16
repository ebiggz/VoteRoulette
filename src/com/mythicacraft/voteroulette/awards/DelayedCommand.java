package com.mythicacraft.voteroulette.awards;

import java.util.Calendar;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.mythicacraft.voteroulette.VoteRoulette;


public class DelayedCommand extends BukkitRunnable {


	private String command;
	private String player = "";
	private boolean runOnLogOff;
	private boolean runOnShutdown;
	private int delay;
	private Date startTime;

	public DelayedCommand(String command, int delay, String playerName, boolean runOnLogOff, boolean runOnShutdown) {
		this.command = command;
		this.delay = delay;
		this.player = playerName;
		this.runOnLogOff = runOnLogOff;
		this.runOnShutdown = runOnShutdown;
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
		return player != null ? player : " ";
	}

	public boolean shouldRunOnLogOff() {
		return runOnLogOff;
	}

	public boolean shouldRunOnShutdown() {
		return runOnShutdown;
	}

	public long getSecondsRemaining() {
		Date now = Calendar.getInstance().getTime();
		long seconds = (now.getTime() - startTime.getTime())/1000;
		return delay - seconds;
	}

	public void handleShutdown() {
		if(shouldRunOnShutdown()) {
			this.run();
		} else {
			if(VoteRoulette.USE_DATABASE) {

			} else {

			}
		}

	}

	@Override
	public BukkitTask runTaskLater(Plugin plugin, long delay) {
		this.startTime = Calendar.getInstance().getTime();
		return super.runTaskLater(plugin, delay);
	}
}
