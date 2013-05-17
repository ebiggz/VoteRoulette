package com.mythicacraft.voteroulette.cmdexecutors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mythicacraft.voteroulette.listeners.VoteListener;

public class Commands implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(commandLabel.equalsIgnoreCase("debugvote")) {
			VoteListener.processVote((Player) sender, null);
		}
		if(commandLabel.equalsIgnoreCase("vr") || commandLabel.equalsIgnoreCase("voteroulette")) {


		}
		//commands
		return true;
	}
}

