package com.mythicacraft.voteroulette.cmdexecutors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mythicacraft.voteroulette.Reward;
import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.listeners.VoteListener;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Paginate;

public class Commands implements CommandExecutor {

	Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");
	RewardManager rm = VoteRoulette.getRewardManager();

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		ConfigAccessor playCfg = new ConfigAccessor("players.yml");
		ConfigAccessor localCfg = new ConfigAccessor("localizations.yml");
		String playername = sender.getName();
		if(commandLabel.equalsIgnoreCase("debugvote")) {
			VoteListener.processVote((Player) sender, null);
		}
		if(commandLabel.equalsIgnoreCase("vr") || commandLabel.equalsIgnoreCase("voteroulette")) {
			if(args.length == 0 || (args.length == 1 && (args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help")))) {
				//send help menu
			}

			if(args.length >= 1) {
				String lifetimeVotes, voteCycle = null;
				if(args[0].equalsIgnoreCase("stats")) {
					if(playCfg.getConfig().contains(playername)) {
						lifetimeVotes = playCfg.getConfig().getString(playername + ".lifetimeVotes");
						if(plugin.getConfig().getBoolean("giveRewardsOnThreshold") == true) {
							voteCycle = playCfg.getConfig().getString(playername + ".voteCycle");
						}
						String message = "Lifetime Votes: " + lifetimeVotes;
						if(voteCycle != null) {
							message = message + " Current Vote Cycle: " + voteCycle;
						}
						sender.sendMessage(message);
					}
					//send vote totals, how many votes till next milestone, how many votes in threshold
				}
				if(args[0].equalsIgnoreCase("milestones")) {
					//show qualifing milestones
				}
				if(args[0].equalsIgnoreCase("rewards")) {

					Reward[] rewards = rm.getQualifiedRewards((Player) sender);
					String message = "";

					for(int i = 0; i < rewards.length; i++) {
						if(rewards[i].isEmpty()) continue;
						message = message + "Reward #" + Integer.toString(i+1) + ": " + rewards[i].getName() + "\nCurrency: " + Double.toString(rewards[i].getCurrency()) + "\nXp Levels: " + Integer.toString(rewards[i].getXpLevels()) + "\n";
						if(rewards[i].hasCurrency()) {
							message = message + "Currency: " + Double.toString(rewards[i].getCurrency()) + "\n";
						}
						if(rewards[i].hasXpLevels()) {
							message = message + "Xp Levels: " + Integer.toString(rewards[i].getXpLevels()) + "\n";
						}
						if(rewards[i].hasItems()) {
							message = message + "items sheit" + "\n";
						}
					}

					Paginate rewardPag = new Paginate(message);

					if(args.length >= 2) {
						try {
							int pageNumber = Integer.parseInt(args[1]);
							if(pageNumber <= rewardPag.pageTotal()) {
								rewardPag.sendPage(pageNumber, sender);
							} else {
								sender.sendMessage("Not a valid page number!");
							}
						} catch (Exception e) {
							sender.sendMessage("Not a valid page number!");
						}
					} else {
						rewardPag.sendPage(1, sender);
					}
					//show qualifying rewards
				}
				if(args[0].equalsIgnoreCase("reload")) {
					plugin.reloadConfig();
					playCfg.reloadConfig();
					localCfg.reloadConfig();
					sender.sendMessage("Reload complete!");
					//reload configs
				}
			}
		}
		//commands
		return true;
	}
}

