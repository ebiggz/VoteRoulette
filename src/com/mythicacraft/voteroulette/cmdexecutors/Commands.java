package com.mythicacraft.voteroulette.cmdexecutors;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mythicacraft.voteroulette.Milestone;
import com.mythicacraft.voteroulette.PlayerManager;
import com.mythicacraft.voteroulette.Reward;
import com.mythicacraft.voteroulette.RewardManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.listeners.VoteProcessor;
import com.mythicacraft.voteroulette.utils.Paginate;
import com.mythicacraft.voteroulette.utils.Utils;

public class Commands implements CommandExecutor {

	private static VoteRoulette plugin;

	public Commands(VoteRoulette instance) {
		plugin = instance;
	}

	RewardManager rm = VoteRoulette.getRewardManager();
	PlayerManager pm = VoteRoulette.getPlayerManager();

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		String playername = sender.getName();

		if(commandLabel.equalsIgnoreCase("vote") || commandLabel.equalsIgnoreCase("votelinks") || commandLabel.equalsIgnoreCase("votesites")) {
			if(sender.hasPermission("voteroulette.votecommand")) {
				for(String website: plugin.VOTE_WEBSITES) {
					sender.sendMessage(website);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
			}
		}
		else if(commandLabel.equalsIgnoreCase("vr") || commandLabel.equalsIgnoreCase("voteroulette") || commandLabel.equalsIgnoreCase("vtr")) {
			if(args.length == 0) {
				sender.sendMessage(plugin.BASE_CMD_NOTIFICATION);
			}
			else if(args.length >= 1) {
				if(args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("help")) {
					Paginate helpMenuPag = new Paginate(Utils.helpMenu(sender), "Help Menu", commandLabel + " " + args[0]);
					if(args.length >= 2) {
						try {
							int pageNumber = Integer.parseInt(args[1]);
							if(pageNumber <= helpMenuPag.pageTotal()) {
								helpMenuPag.sendPage(pageNumber, sender);
							} else {
								sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid page number!");
							}
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid page number!");
						}
					} else {
						helpMenuPag.sendPage(1, sender);
					}
				}
				else if(args[0].equalsIgnoreCase("lastvote")) {
					if(args.length == 1) {
						if(!sender.hasPermission("voteroulette.lastvote")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}
						if(pm.playerHasLastVoteTimeStamp(playername)) {
							String timeSince = Utils.getTimeSinceString(pm.getPlayerLastVoteTimeStamp(playername));
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] Your last vote was " + timeSince + " ago.");
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] A last vote time stamp for you has not been saved yet!");
						}
					}
					else if(args.length == 2) {
						if(!sender.hasPermission("voteroulette.lastvoteothers")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}
						String otherPlayer = Utils.completeName(args[1]);
						if(otherPlayer == null) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Couldn't find a player by the name: " + args[1]);
							return true;
						}
						if(pm.playerHasLastVoteTimeStamp(otherPlayer)) {
							String timeSince = Utils.getTimeSinceString(pm.getPlayerLastVoteTimeStamp(otherPlayer));
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] " + otherPlayer + "'s last vote was " + timeSince + " ago.");
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] A last vote time stamp for " + otherPlayer + " has not been saved yet!");
						}
					}
				}
				else if(args[0].equalsIgnoreCase("forcevote")) {
					if(!sender.hasPermission("voteroulette.forcevote")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					if(args.length == 1) {
						sender.sendMessage(ChatColor.RED + " [VoteRoulette] Please include a players name to force a vote to. /vr forcevote <playername>");
						return true;
					}
					if(args.length > 1) {
						String otherPlayer = Utils.completeName(args[1]);
						if(otherPlayer == null) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Couldn't find a player by the name: " + args[1]);
							return true;
						}
						sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You forced a vote to " + ChatColor.YELLOW + otherPlayer + ChatColor.AQUA +  ". Player will receive a reward/milestone if applicable.");
						pm.incrementPlayerVoteTotals(otherPlayer);
						new Thread(new VoteProcessor(otherPlayer, plugin, true, null)).start();
					}
				}

				else if(args[0].equalsIgnoreCase("remind") || args[0].equalsIgnoreCase("reminder") || args[0].equalsIgnoreCase("broadcast")) {
					if(!sender.hasPermission("voteroulette.remind")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					Bukkit.broadcastMessage(plugin.PERIODIC_REMINDER);
				}

				else if(args[0].equalsIgnoreCase("stats")) {
					int lifetimeVotes, voteCycle;
					if(args.length == 1) {
						if (!(sender instanceof Player)) {
							sender.sendMessage("[VoteRoulette] This command can't be ran by the console!");
							return true;
						}
						if(!sender.hasPermission("voteroulette.viewstats")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}

						lifetimeVotes = pm.getPlayerLifetimeVotes(playername);
						String message = ChatColor.BLUE + "Lifetime Votes: " + ChatColor.AQUA + lifetimeVotes;
						if(plugin.REWARDS_ON_THRESHOLD) {
							voteCycle = pm.getPlayerCurrentVoteCycle(playername);
							message = message + ChatColor.BLUE + "\nCurrent Vote Cycle: " + ChatColor.AQUA + voteCycle;
						}
						sender.sendMessage(ChatColor.GOLD + "-----" + ChatColor.GREEN + "Voting Stats For: " + ChatColor.YELLOW + sender.getName() + ChatColor.GOLD + "-----"); //header of page with current and total pages
						sender.sendMessage(message);

					}
					else if(args.length == 2) {
						if(!sender.hasPermission("voteroulette.viewotherstats")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}

						String otherPlayer = Utils.completeName(args[1]);
						if(otherPlayer == null) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Couldn't find a player by the name: " + args[1]);
							return true;
						}

						lifetimeVotes = pm.getPlayerLifetimeVotes(otherPlayer);
						String message = ChatColor.BLUE + "Lifetime/Total Votes: " + ChatColor.AQUA + lifetimeVotes;
						if(plugin.REWARDS_ON_THRESHOLD) {
							voteCycle = pm.getPlayerCurrentVoteCycle(otherPlayer);
							message = message + ChatColor.BLUE + "\nCurrent Vote Cycle: " + ChatColor.AQUA + voteCycle;
						}
						sender.sendMessage(ChatColor.GOLD + "-----" + ChatColor.GREEN + "Voting Stats For: " + ChatColor.YELLOW + otherPlayer + ChatColor.GOLD + "-----"); //header of page with current and total pages
						sender.sendMessage(message);
					}
					else if(args.length == 4) {
						//  /vr stats player settotal ##
						if(!sender.hasPermission("voteroulette.editstats")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}

						String otherPlayer = Utils.completeName(args[1]);
						if(otherPlayer == null) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Couldn't find a player by the name: " + args[1]);
							return true;
						}

						int voteNumber;
						try {
							voteNumber = Integer.parseInt(args[3]);
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] " + args[3] + " is not a valid number!");
							return true;
						}

						if(args[2].equalsIgnoreCase("settotal")) {
							pm.setPlayerLifetimeVotes(otherPlayer, voteNumber);
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You set " + ChatColor.YELLOW + otherPlayer + "'s " + ChatColor.AQUA +  "total votes to " + ChatColor.YELLOW + voteNumber + ChatColor.AQUA + "!");
						}
						else if(args[2].equalsIgnoreCase("setcycle")) {
							pm.setPlayerCurrentVoteCycle(otherPlayer, voteNumber);
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You set " + ChatColor.YELLOW + otherPlayer + "'s " + ChatColor.AQUA +  "current vote cycle to " + ChatColor.YELLOW + voteNumber + ChatColor.AQUA + "!");
						}
					}
					else if(args.length > 4) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] Invalid stats command! /vr stats");
					}
				}
				//send vote totals, how many votes till next milestone, how many votes in threshold
				else if(args[0].equalsIgnoreCase("milestones")) {
					//show qualifying milestones
					if(!sender.hasPermission("voteroulette.viewmilestones")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}

					Milestone[] milestones = rm.getQualifiedMilestones(sender.getName());
					String message = "";
					for(int i = 0; i < milestones.length; i++) {
						message += ChatColor.YELLOW + "Milestone #" + Integer.toString(i+1) + ": " + ChatColor.AQUA + milestones[i].getName() + "\n";
						message += ChatColor.GOLD + "Votes: " + ChatColor.DARK_AQUA + milestones[i].getVotes() + "\n";
						message += ChatColor.GOLD + "Recurring: " + ChatColor.DARK_AQUA +  milestones[i].isRecurring() + "\n";
						if(milestones[i].hasDescription()) {
							message += ChatColor.GOLD + "Description: " + ChatColor.DARK_AQUA + milestones[i].getDescription() + "\n";
						}
						if(milestones[i].hasCurrency()) {
							String currency = Double.toString(milestones[i].getCurrency());
							if(currency.length() < 4) {
								if(currency.length() > 2) {
									currency += "0";
								} else {
									currency += "00";
								}
							}
							message += ChatColor.GOLD + "Currency: " + ChatColor.DARK_AQUA + currency + "\n";
						}
						if(milestones[i].hasXpLevels()) {
							message += ChatColor.GOLD + "XP Levels: " + ChatColor.DARK_AQUA + Integer.toString(milestones[i].getXpLevels()) + "\n";
						}
						if(milestones[i].hasItems()) {
							message += ChatColor.GOLD + "Items: " + ChatColor.DARK_AQUA + Utils.getItemListString(milestones[i].getItems()) + "\n";
						}
						if(milestones[i].hasWorlds()) {
							message += ChatColor.GOLD + "Worlds: " + ChatColor.DARK_AQUA + Utils.worldsString(milestones[i].getWorlds()) + "\n";
						}
						if(milestones[i].hasChance()) {
							message += ChatColor.GOLD + "Chance: "  + ChatColor.DARK_AQUA + milestones[i].getChanceMin() + " in " + milestones[i].getChanceMax() + "\n";
						}
						if(milestones[i].hasReroll()) {
							message += ChatColor.GOLD + "Reroll For: "  + ChatColor.DARK_AQUA + milestones[i].getReroll().replace("ANY", "Any Reward") + "\n";
						}
					}
					Paginate milestonePag = new Paginate(message, "Milestones", commandLabel + " milestones");

					if(args.length >= 2) {
						try {
							int pageNumber = Integer.parseInt(args[1]);
							if(pageNumber <= milestonePag.pageTotal()) {
								milestonePag.sendPage(pageNumber, sender);
							} else {
								sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid page number!");
							}
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid page number!");
						}
					} else {
						milestonePag.sendPage(1, sender);
					}
				}
				else if(args[0].equalsIgnoreCase("rewards")) {

					if(!sender.hasPermission("voteroulette.viewrewards")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}

					Reward[] rewards = rm.getQualifiedRewards(sender.getName());
					String message = "";

					for(int i = 0; i < rewards.length; i++) {
						if(rewards[i].isEmpty()) continue;
						message = message + ChatColor.YELLOW + "Reward #" + Integer.toString(i+1) + ": " + ChatColor.AQUA + rewards[i].getName() + "\n";
						if(rewards[i].hasDescription()) {
							message += ChatColor.GOLD + "Description: " + ChatColor.DARK_AQUA + rewards[i].getDescription() + "\n";
						}
						if(rewards[i].hasCurrency()) {
							String currency = Double.toString(rewards[i].getCurrency());
							if(currency.length() < 4) {
								if(currency.length() > 2) {
									currency += "0";
								} else {
									currency += "00";
								}
							}
							message += ChatColor.GOLD + "Currency: " + ChatColor.DARK_AQUA + currency + "\n";
						}
						if(rewards[i].hasXpLevels()) {
							message += ChatColor.GOLD + "XP Levels: " + ChatColor.DARK_AQUA + Integer.toString(rewards[i].getXpLevels()) + "\n";
						}
						if(rewards[i].hasItems()) {
							message += ChatColor.GOLD + "Items: " + ChatColor.DARK_AQUA + Utils.getItemListString(rewards[i].getItems()) + "\n";
						}
						if(rewards[i].hasWorlds()) {
							message += ChatColor.GOLD + "Worlds: " + ChatColor.DARK_AQUA + Utils.worldsString(rewards[i].getWorlds()) + "\n";
						}
						if(rewards[i].hasChance()) {
							message += ChatColor.GOLD + "Chance: "  + ChatColor.DARK_AQUA + rewards[i].getChanceMin() + " in " + rewards[i].getChanceMax() + "\n";
						}
						if(rewards[i].hasReroll()) {
							message += ChatColor.GOLD + "Reroll For: "  + ChatColor.DARK_AQUA + rewards[i].getReroll().replace("ANY", "Any Reward") + "\n";
						}
					}

					Paginate rewardPag = new Paginate(message, "Rewards", commandLabel + " rewards");

					if(args.length >= 2) {
						try {
							int pageNumber = Integer.parseInt(args[1]);
							if(pageNumber <= rewardPag.pageTotal()) {
								rewardPag.sendPage(pageNumber, sender);
							} else {
								sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid page number!");
							}
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid page number!");
						}
					} else {
						rewardPag.sendPage(1, sender);
					}
					//show qualifying rewards
				}
				else if(args[0].equalsIgnoreCase("reload")) {
					if(!sender.hasPermission("voteroulette.admin")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					plugin.reloadConfigs();
					sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] Reload complete!");
					//reload configs
				}
				else if(args[0].equalsIgnoreCase("claim")) {
					if (!(sender instanceof Player)) {
						sender.sendMessage("[VoteRoulette] This command can't be used in the console!");
						return true;
					}

					int unclaimedRewardsCount = pm.getUnclaimedRewardCount(playername);
					int unclaimedMilestonesCount = pm.getUnclaimedMilestoneCount(playername);
					if(args.length == 1) {
						if(unclaimedRewardsCount > 0) {
							sender.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "reward").replace("%amount%", Integer.toString(unclaimedRewardsCount)).replace("%command%", "/vr claim rewards"));
						} else {
							sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "reward"));
						}
						if(unclaimedMilestonesCount > 0) {
							sender.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "milestone").replace("%amount%", Integer.toString(unclaimedMilestonesCount)).replace("%command%", "/vr claim milestones"));
						} else {
							sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "milestone"));
						}
					}
					else if(args.length >= 2) {
						if(args[1].equalsIgnoreCase("rewards")) {

							if(unclaimedRewardsCount == 0) {
								sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "reward"));
								return true;
							}
							List<Reward> unclaimedRewards = pm.getUnclaimedRewards(playername);
							if(args.length == 2) {
								String[] rewardMessages = new String[unclaimedRewards.size()];
								int count = 0;
								for(Reward reward: unclaimedRewards) {
									rewardMessages[count] = ChatColor.YELLOW + Integer.toString(count+1) + ") " + ChatColor.GOLD + "Name: \"" + ChatColor.YELLOW + reward.getName() + ChatColor.GOLD + "\", Required Slots: " + ChatColor.YELLOW + reward.getRequiredSlots();
									count++;
								}
								sender.sendMessage(ChatColor.AQUA + "-----[Unclaimed Rewards]-----");
								sender.sendMessage(rewardMessages);
								sender.sendMessage(ChatColor.AQUA + "Type " + ChatColor.YELLOW + "/vr claim rewards #" + ChatColor.AQUA + " or " + ChatColor.YELLOW + "/vr claim rewards all");

							}
							else if(args.length == 3) {
								if(args[2].equalsIgnoreCase("all")) {
									if(sender.hasPermission("voteroulette.claimall")) {
										for(Reward reward : unclaimedRewards) {
											pm.removeUnclaimedReward(playername, reward.getName());
											rm.administerRewardContents(reward, sender.getName());
										}
									} else {
										sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
									}
								} else {
									try {
										int rewardNumber = Integer.parseInt(args[2]);
										if(rewardNumber <= unclaimedRewards.size()) {
											Reward reward = unclaimedRewards.get(rewardNumber-1);
											pm.removeUnclaimedReward(playername, reward.getName());
											rm.administerRewardContents(reward, sender.getName());
										} else {
											sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid reward number!");
										}
									} catch (Exception e) {
										sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid reward number!");
									}
								}
							}
						}
						else if(args[1].equalsIgnoreCase("milestones")) {

							if(unclaimedMilestonesCount == 0) {
								sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "milestone"));
								return true;
							}
							List<Milestone> unclaimedMilestones = pm.getUnclaimedMilestones(playername);
							if(args.length == 2) {
								String[] milestoneMessages = new String[unclaimedMilestones.size()];
								int count = 0;
								for(Milestone milestone: unclaimedMilestones) {
									milestoneMessages[count] = ChatColor.YELLOW + Integer.toString(count+1) + ") " + ChatColor.GOLD + "Name: \"" + ChatColor.YELLOW + milestone.getName() + ChatColor.GOLD + "\", Required Slots: " + ChatColor.YELLOW + milestone.getRequiredSlots();
									count++;
								}
								sender.sendMessage(ChatColor.AQUA + "-----[Unclaimed Milestones]-----");
								sender.sendMessage(milestoneMessages);
								sender.sendMessage(ChatColor.AQUA + "Type " + ChatColor.YELLOW + "/vr claim milestones #" + ChatColor.AQUA + " or " + ChatColor.YELLOW + "/vr claim milestones all");

							}
							else if(args.length == 3) {
								if(sender.hasPermission("voteroulette.claimall")) {
									if(args[2].equalsIgnoreCase("all")) {
										for(Milestone milestone : unclaimedMilestones) {
											pm.removeUnclaimedMilestone(playername, milestone.getName());
											rm.administerMilestoneContents(milestone, sender.getName());
										}
									} else {
										sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
									}
								} else {
									try {
										int rewardNumber = Integer.parseInt(args[2]);
										if(rewardNumber <= unclaimedMilestones.size()) {
											Milestone milestone = unclaimedMilestones.get(rewardNumber-1);
											pm.removeUnclaimedMilestone(playername, milestone.getName());
											rm.administerMilestoneContents(milestone, sender.getName());
										} else {
											sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid milestone number!");
										}
									} catch (Exception e) {
										sender.sendMessage(ChatColor.RED + "[VoteRoulette] Not a valid milestone number!");
									}
								}
							}
						} else {
							sender.sendMessage(plugin.BASE_CMD_NOTIFICATION);
						}
					}
				}
				else {
					sender.sendMessage(plugin.BASE_CMD_NOTIFICATION);
				}
			}
		}
		//commands
		return true;
	}
}

