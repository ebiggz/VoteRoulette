package com.mythicacraft.voteroulette.cmdexecutors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.mythicacraft.voteroulette.Milestone;
import com.mythicacraft.voteroulette.Reward;
import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.listeners.VoteHandler;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Paginate;
import com.mythicacraft.voteroulette.utils.Utils;

public class Commands implements CommandExecutor {

	private static VoteRoulette plugin;

	public Commands(VoteRoulette instance) {
		plugin = instance;
	}

	RewardManager rm = VoteRoulette.getRewardManager();

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		ConfigAccessor playCfg = new ConfigAccessor("players.yml");
		String playername = sender.getName();
		if(commandLabel.equalsIgnoreCase("debugvote")) {
			VoteHandler.updatePlayerVoteTotals(playername);
			VoteHandler.processVote((Player) sender, null);
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
						if(plugin.REWARDS_ON_THRESHOLD) {
							voteCycle = playCfg.getConfig().getString(playername + ".voteCycle");
						}
						String message = ChatColor.BLUE + "Lifetime Votes: " + ChatColor.AQUA + lifetimeVotes;
						if(voteCycle != null) {
							message = message + ChatColor.BLUE + " Current Vote Cycle: " + ChatColor.AQUA + voteCycle;
						}
						sender.sendMessage(ChatColor.GOLD + "-----" + ChatColor.DARK_GREEN + "Voting Stats For: " + sender.getName() + ChatColor.GOLD + "-----"); //header of page with current and total pages
						sender.sendMessage(message);
					}
					//send vote totals, how many votes till next milestone, how many votes in threshold
				}
				if(args[0].equalsIgnoreCase("milestones")) {
					//show qualifing milestones
					Milestone[] milestones = rm.getQualifiedMilestones((Player) sender);
					String message = "";

					for(int i = 0; i < milestones.length; i++) {
						message = message + ChatColor.BLUE + "Milestone #" + Integer.toString(i+1) + ": " + ChatColor.AQUA + milestones[i].getName() + "\n";
						message += ChatColor.GOLD + "    Vote Threshold: " + ChatColor.AQUA + milestones[i].getVotes() + "\n";
						message += ChatColor.GOLD + "    Recurring: " + ChatColor.AQUA +  milestones[i].isRecurring() + "\n";
						if(milestones[i].hasCurrency()) {
							String currency = Double.toString(milestones[i].getCurrency());
							if(currency.length() < 4) {
								if(currency.length() > 2) {
									currency += "0";
								} else {
									currency += "00";
								}
							}
							message += ChatColor.GOLD + "    Currency: " + ChatColor.AQUA + currency + "\n";
						}
						if(milestones[i].hasXpLevels()) {
							message += ChatColor.GOLD + "    XP Levels: " + ChatColor.AQUA + Integer.toString(milestones[i].getXpLevels()) + "\n";
						}
						if(milestones[i].hasItems()) {
							message += ChatColor.GOLD + "    Items: " + ChatColor.DARK_AQUA + Utils.getItemListString(milestones[i].getItems()) + "\n";
						}
					}
				}
				if(args[0].equalsIgnoreCase("rewards")) {

					Reward[] rewards = rm.getQualifiedRewards((Player) sender);
					String message = "";

					for(int i = 0; i < rewards.length; i++) {
						if(rewards[i].isEmpty()) continue;
						message = message + ChatColor.BLUE + "Reward #" + Integer.toString(i+1) + ": " + ChatColor.AQUA + rewards[i].getName() + "\n";
						if(rewards[i].hasCurrency()) {
							String currency = Double.toString(rewards[i].getCurrency());
							if(currency.length() < 4) {
								if(currency.length() > 2) {
									currency += "0";
								} else {
									currency += "00";
								}
							}
							message += ChatColor.GOLD + "    Currency: " + ChatColor.AQUA + currency + "\n";
						}
						if(rewards[i].hasXpLevels()) {
							message += ChatColor.GOLD + "    XP Levels: " + ChatColor.AQUA + Integer.toString(rewards[i].getXpLevels()) + "\n";
						}
						if(rewards[i].hasItems()) {
							message += ChatColor.GOLD + "    Items: " + ChatColor.DARK_AQUA + Utils.getItemListString(rewards[i].getItems()) + "\n";
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
					if(!sender.isOp()) return true;
					plugin.reloadConfigs();
					sender.sendMessage("Reload complete!");
					//reload configs
				}
				if(args[0].equalsIgnoreCase("claim")) {
					ConfigurationSection playUnclaimR = playCfg.getConfig().getConfigurationSection(playername + ".unclaimedRewards");
					if(playUnclaimR != null) {
						//give rewards
					} else {
						sender.sendMessage("You do not have unclaimed rewards!");
					}
				}
			}
		}
		//commands
		return true;
	}
}

