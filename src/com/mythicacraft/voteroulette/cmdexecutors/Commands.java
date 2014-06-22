package com.mythicacraft.voteroulette.cmdexecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.Voter.Stat;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.awardcreator.AwardCreator;
import com.mythicacraft.voteroulette.awardcreator.AwardCreator.AwardCreationStage;
import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.AwardManager;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.awards.Reward.VoteStreakModifier;
import com.mythicacraft.voteroulette.stats.VoteStat.StatType;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Paginate;
import com.mythicacraft.voteroulette.utils.Utils;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class Commands implements CommandExecutor {

	/**
	 * TODO: Clean this guy up... split into separate classes?
	 */

	private static VoteRoulette plugin;

	AwardManager rm = VoteRoulette.getAwardManager();

	VoterManager vm = VoteRoulette.getVoterManager();

	public Commands(VoteRoulette instance) {
		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		String playername = sender.getName();
		Voter voter = null;
		if (sender instanceof Player) {
			voter = vm.getVoter(playername);
		}

		if (commandLabel.equalsIgnoreCase("vote") || commandLabel
		        .equalsIgnoreCase("votelinks") || commandLabel
		        .equalsIgnoreCase("votesites") || commandLabel
		        .equalsIgnoreCase(plugin.VOTE_DEF.toLowerCase())) {
			if (sender.hasPermission("voteroulette.votecommand")) {
				if (plugin.USE_FANCY_LINKS) {
					if (plugin.isOn1dot7) {
						for (String line : plugin.VOTE_WEBSITES) {
							String fancyLink = Utils.getFancyLink(line);
							if (!fancyLink.isEmpty()) {
								String tellRaw = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"%before%\",\"color\":\"white\"},{\"text\":\"%site%\",\"color\":\"white\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"%link%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"" + ChatColor.YELLOW + Utils
								        .transcribeColorCodes(plugin.FANCY_LINK_POPUP) + "\"}},{\"text\":\"%after%\",\"color\":\"white\"}]}";
								line = line.replace(fancyLink, "%split%");
								String[] beforeAndAfter = line.split("%split%");
								if (beforeAndAfter.length != 0) {
									int count = 1;
									for (String s : beforeAndAfter) {
										if (count == 1) {
											tellRaw = tellRaw.replace(
											        "%before%", s);
										} else if (count == 2) {
											tellRaw = tellRaw.replace(
											        "%after%", s);
										}
										count++;
									}
								}
								tellRaw = tellRaw.replace("%before%", "")
								        .replace("%after%", "");
								String[] linkData = fancyLink.replace("{", "")
								        .replace("}", "").split(">");
								if (linkData.length > 0) {
									tellRaw = tellRaw.replace("%site%",
									        linkData[0].trim()).replace(
									        "%link%", linkData[1].trim());
								}
								tellRaw = tellRaw.replace("%player%",
								        sender.getName());
								Bukkit.getServer().dispatchCommand(
								        Bukkit.getServer().getConsoleSender(),
								        tellRaw);
							} else {
								sender.sendMessage(line);
							}
						}
					} else {
						for (String website : plugin.VOTE_WEBSITES) {
							sender.sendMessage(website);
						}
					}

				} else {
					for (String website : plugin.VOTE_WEBSITES) {
						sender.sendMessage(website);
					}
				}
			} else {
				sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
			}
		} else if (commandLabel.equalsIgnoreCase("vr") || commandLabel
		        .equalsIgnoreCase("voteroulette") || commandLabel
		        .equalsIgnoreCase("vtr")) {
			if (args.length == 0) {
				sender.sendMessage(plugin.BASE_CMD_NOTIFICATION);
			} else if (args.length >= 1) {
				if (args[0].equalsIgnoreCase("setname")) {
					if (sender.hasPermission("voteroulette.edititems")) {

						Player p = (Player) sender;
						ItemStack itemInHand = p.getItemInHand();
						ItemMeta im = itemInHand.getItemMeta();

						if ((itemInHand != null) && (itemInHand.getType() != Material.AIR)) {
							if (args.length == 1) {
								im.setDisplayName(null);
								sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You have cleared the custom name of this " + ChatColor.YELLOW + itemInHand
								        .getType().toString().toLowerCase()
								        .replace("_", " ") + ChatColor.BLUE + ".");
							} else {
								// combine args
								StringBuilder sb = new StringBuilder();
								for (int i = 1; i < args.length; i++) {
									sb.append(args[i] + " ");
								}
								sb.delete(sb.length() - 1, sb.length());
								// set name
								String name = sb.toString();
								im.setDisplayName(Utils
								        .transcribeColorCodes(name));
								sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You have set the custom name of this " + ChatColor.YELLOW + itemInHand
								        .getType().toString().toLowerCase()
								        .replace("_", "") + ChatColor.BLUE + ".");
							}
							itemInHand.setItemMeta(im);
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] You must be holding an item to set it's name!");
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				} else if (args[0].equalsIgnoreCase("setlore")) {
					if (sender.hasPermission("voteroulette.edititems")) {
						Player p = (Player) sender;
						ItemStack itemInHand = p.getItemInHand();

						if ((itemInHand != null) && (itemInHand.getType() != Material.AIR)) {
							ItemMeta im = itemInHand.getItemMeta();
							if (args.length == 1) {
								im.setLore(null);
								sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You have cleared the custom lore of this " + ChatColor.YELLOW + itemInHand
								        .getType().toString().toLowerCase()
								        .replace("_", "") + ChatColor.BLUE + ".");
							} else {
								// combine args
								StringBuilder sb = new StringBuilder();
								for (int i = 1; i < args.length; i++) {
									sb.append(args[i] + " ");
								}
								sb.delete(sb.length() - 1, sb.length());
								// set name
								String loreStr = Utils.transcribeColorCodes(sb
								        .toString());
								String[] loreArray = loreStr.split("/");
								im.setLore(Arrays.asList(loreArray));
								sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You have set the custom lore of this " + ChatColor.YELLOW + itemInHand
								        .getType().toString().toLowerCase()
								        .replace("_", " ") + ChatColor.BLUE + ".");
							}
							itemInHand.setItemMeta(im);
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] You must be holding an item to set it's lore!");
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				} else if (args[0].equalsIgnoreCase("colors")) {
					if (sender.hasPermission("voteroulette.colors")) {
						sender.sendMessage(ChatColor.WHITE + "Colors:" + ChatColor.BLACK + " &0" + ChatColor.DARK_BLUE + " &1" + ChatColor.DARK_GREEN + " &2" + ChatColor.DARK_AQUA + " &3" + ChatColor.DARK_RED + " &4" + ChatColor.DARK_PURPLE + " &5" + ChatColor.GOLD + " &6" + ChatColor.GRAY + " &7" + ChatColor.DARK_GRAY + " &8" + ChatColor.BLUE + " &9" + ChatColor.GREEN + " &a" + ChatColor.AQUA + " &b" + ChatColor.RED + " &c" + ChatColor.LIGHT_PURPLE + " &d" + ChatColor.YELLOW + " &e" + ChatColor.WHITE + " &f");
						sender.sendMessage(ChatColor.WHITE + "Formats:" + ChatColor.BOLD + " &l" + ChatColor.RESET + " " + ChatColor.STRIKETHROUGH + "&m" + ChatColor.RESET + " " + ChatColor.UNDERLINE + "&n" + ChatColor.RESET + " " + ChatColor.ITALIC + "&o" + ChatColor.RESET + " &r(reset)");
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				} else if (args[0].equalsIgnoreCase("create")) {
					if (!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] You must be in-game to run this command!");
						return true;
					}
					if (sender.hasPermission("voteroulette.createawards")) {
						Player player = (Player) sender;
						if (VoteRoulette.inAwardCreator.containsKey(player)) {
							AwardCreator ac = VoteRoulette.inAwardCreator
							        .get(player);
							if (ac.isPaused()) {
								sender.sendMessage(ChatColor.YELLOW + "[VoteRoulette] " + ChatColor.AQUA + "Entered the Award Creator!");
								ac.setPaused(false);
								sender.sendMessage(ChatColor.GRAY + "(You have resumed the creation of a previous award)");
								ac.goToStage(ac.getCurrentStage(), true);
							} else {
								sender.sendMessage(ChatColor.RED + "You are already in the Award Creator. Type cancel to quit or current to see your current step.");
							}
						} else {
							sender.sendMessage(ChatColor.YELLOW + "[VoteRoulette] " + ChatColor.AQUA + "Entered the Award Creator!");
							AwardCreator ac = new AwardCreator(player);
							VoteRoulette.inAwardCreator.put(player, ac);
							ac.goToStage(AwardCreationStage.CHOOSE_AWARD);
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("editreward")) {
					if (sender.hasPermission("voteroulette.createawards")) {
						if (args.length >= 2) {
							int rewardIndex;
							try {
								Player player = (Player) sender;
								rewardIndex = Integer.parseInt(args[1]) - 1;
								Reward reward = rm.getRewards()
								        .get(rewardIndex);
								if (VoteRoulette.inAwardCreator
								        .containsKey(player)) {
									sender.sendMessage(ChatColor.RED + "You are already in the Award Creator. Type cancel to quit or current to see your current step.");
									return true;
								}
								AwardCreator ac = new AwardCreator(player);
								ac.setAward(reward);
								ac.setAwardType(AwardType.REWARD);
								ac.setOrigAward(reward, rewardIndex);
								VoteRoulette.inAwardCreator.put(player, ac);

								sender.sendMessage(ChatColor.YELLOW + "[VoteRoulette] " + ChatColor.AQUA + "Entered the Award Creator!");
								sender.sendMessage(ChatColor.GRAY + "(You are editting a previous reward)");
								ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);

							} catch (Exception e) {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] /vr editreward [#]. Get # from \"/vr rewards -a\"");
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("deletereward")) {
					if (sender.hasPermission("voteroulette.deleteawards")) {
						if (args.length >= 2) {
							int rewardIndex;
							try {
								rewardIndex = Integer.parseInt(args[1]) - 1;
								Reward reward = rm.getRewards()
								        .get(rewardIndex);
								rm.deleteAwardFromFile(reward);
								rm.getRewards().remove(rewardIndex);
								sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You successfully deleted the Reward " + ChatColor.YELLOW + reward
								        .getName() + ChatColor.YELLOW + "!");
							} catch (Exception e) {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] /vr deletereward [#]. Get # from \"/vr rewards -a\"");
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("editmilestone")) {
					if (sender.hasPermission("voteroulette.createawards")) {
						if (args.length >= 2) {
							int milestoneIndex;
							try {
								Player player = (Player) sender;
								milestoneIndex = Integer.parseInt(args[1]) - 1;
								Milestone milestone = rm.getMilestones().get(
								        milestoneIndex);
								if (VoteRoulette.inAwardCreator
								        .containsKey(player)) {
									sender.sendMessage(ChatColor.RED + "You are already in the Award Creator. Type cancel to quit or current to see your current step.");
									return true;
								}
								AwardCreator ac = new AwardCreator(player);
								ac.setAward(milestone);
								ac.setAwardType(AwardType.MILESTONE);
								ac.setOrigAward(milestone, milestoneIndex);
								VoteRoulette.inAwardCreator.put(player, ac);
								sender.sendMessage(ChatColor.YELLOW + "[VoteRoulette] " + ChatColor.AQUA + "Entered the Award Creator!");
								sender.sendMessage(ChatColor.GRAY + "(You are editting a previous milestone)");
								ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);

							} catch (Exception e) {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] /vr editmilestone [#]. Get # from \"/vr rewards -a\"");
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("deletemilestone")) {
					if (sender.hasPermission("voteroulette.deleteawards")) {
						if (args.length >= 2) {
							int milestoneIndex;
							try {
								milestoneIndex = Integer.parseInt(args[1]) - 1;
								Milestone milestone = rm.getMilestones().get(
								        milestoneIndex);
								rm.deleteAwardFromFile(milestone);
								rm.getMilestones().remove(milestoneIndex);
								sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You successfully deleted the Milestone " + ChatColor.YELLOW + milestone
								        .getName() + ChatColor.YELLOW + "!");
							} catch (Exception e) {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						} else {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] /vr deletemilestone [#]. Get # from \"/vr milestones -a\"");
						}
					} else {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
					}
					return true;
				} else if (args[0].equalsIgnoreCase(plugin.TOP_DEF + "10") || args[0]
				        .equalsIgnoreCase(plugin.TOP_DEF)) {
					if (!sender.hasPermission("voteroulette.top10")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					if (args.length == 1) {
						sender.sendMessage(ChatColor.DARK_AQUA + "------[" + ChatColor.GREEN + "Top Commands" + ChatColor.DARK_AQUA + "]------");
						sender.sendMessage(ChatColor.GOLD + "Total Votes: " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + " " + plugin.TOTAL_DEF
						        .toLowerCase().replace(" ", ""));
						sender.sendMessage(ChatColor.GOLD + "Consecutive Days: " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + " " + plugin.STREAK_DEF
						        .toLowerCase().replace(" ", ""));
					} else {
						if (args[1].equalsIgnoreCase(plugin.VOTE_STREAK_DEF
						        .replace(" ", "")) || args[1]
						        .equalsIgnoreCase("votestreaks") || args[1]
						        .equalsIgnoreCase(plugin.STREAK_DEF)) {
							sender.sendMessage(plugin.TOP_10_CMD.replace(
							        "%stat%", plugin.LONGEST_VOTE_STREAK_DEF));
							if (plugin.USE_SCOREBOARD && (sender instanceof Player)) {
								Utils.showTopScoreboard((Player) sender,
								        StatType.LONGEST_VOTE_STREAKS);
							} else {
								Utils.showTopInChat((Player) sender,
								        StatType.LONGEST_VOTE_STREAKS);
							}
						} else if (args[1].equalsIgnoreCase(plugin.TOTAL_DEF
						        .replace(" ", "")) || args[1]
						        .equalsIgnoreCase("lifetime")) {
							sender.sendMessage(plugin.TOP_10_CMD.replace(
							        "%stat%", plugin.TOTAL_VOTES_DEF));
							if (plugin.USE_SCOREBOARD && (sender instanceof Player)) {
								Utils.showTopScoreboard((Player) sender,
								        StatType.TOTAL_VOTES);
							} else {
								Utils.showTopInChat((Player) sender,
								        StatType.TOTAL_VOTES);
							}
						}

					}
				} else if (args[0].equalsIgnoreCase("?") || args[0]
				        .equalsIgnoreCase("help")) {
					Paginate helpMenuPag = new Paginate(Utils.helpMenu(sender),
					        "Help Menu", commandLabel + " " + args[0]);
					if (args.length >= 2) {
						try {
							int pageNumber = Integer.parseInt(args[1]);
							if (pageNumber <= helpMenuPag.pageTotal()) {
								helpMenuPag.sendPage(pageNumber, sender);
							} else {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						} catch (Exception e) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
						}
					} else {
						helpMenuPag.sendPage(1, sender);
					}
				} else if (args[0].equalsIgnoreCase(plugin.LASTVOTE_DEF)) {
					if (args.length == 1) {
						if (!(sender instanceof Player)) {
							sender.sendMessage("[VoteRoulette] This command can't be ran by the console!");
							return true;
						}
						if (!sender.hasPermission("voteroulette.lastvote")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}
						if (voter.hasLastVoteTimeStamp()) {
							String timeSince = Utils.getTimeSinceString(voter
							        .getLastVoteTimeStamp());
							sender.sendMessage(plugin.LAST_VOTE_SELF_CMD
							        .replace("%time%", timeSince));
						} else {
							sender.sendMessage(plugin.LAST_VOTE_NONE_NOTIFICATION);
						}
					} else if (args.length == 2) {
						if (!sender
						        .hasPermission("voteroulette.lastvoteothers")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}
						String otherPlayer = Utils.completeName(args[1]);
						if (otherPlayer == null) {
							sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
							        .replace("%player%", args[1]));
							return true;
						}
						Voter otherVoter = vm.getVoter(otherPlayer);
						if (otherVoter.hasLastVoteTimeStamp()) {
							String timeSince = Utils
							        .getTimeSinceString(otherVoter
							                .getLastVoteTimeStamp());
							sender.sendMessage(plugin.LAST_VOTE_OTHER_CMD
							        .replace("%player%", otherPlayer).replace(
							                "%time%", timeSince));
						} else {
							sender.sendMessage(plugin.LAST_VOTE_NONE_NOTIFICATION);
						}
					}
				} else if (args[0].equalsIgnoreCase(plugin.FORCEVOTE_DEF)) {
					if (!sender.hasPermission("voteroulette.forcevote")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					if (args.length == 1) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] /" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEVOTE_DEF + " [" + plugin.PLAYER_DEF
						        .toLowerCase() + "]");
						return true;
					}
					if (args.length > 1) {
						String otherPlayer = Utils.completeName(args[1]);
						if (otherPlayer == null) {
							sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
							        .replace("%player%", args[1]));
							return true;
						}
						sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You forced a vote to " + ChatColor.YELLOW + otherPlayer + ChatColor.AQUA + ".");
						Vote forceVote = new Vote();
						forceVote.setAddress("1.2.3.4");
						forceVote.setServiceName("forcevote");
						if (args.length > 2) {
							forceVote.setServiceName(args[2]);
						}
						Date date = new Date();
						forceVote.setTimeStamp(String.valueOf(date.getTime()));
						forceVote.setUsername(otherPlayer);
						Bukkit.getPluginManager().callEvent(
						        new VotifierEvent(forceVote));
					}
				}

				else if (args[0].equalsIgnoreCase(plugin.FORCEREWARD_DEF
				        .toLowerCase())) {
					if (!sender.hasPermission("voteroulette.forceawards")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					if (args.length < 3) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] /" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEREWARD_DEF + " " + "[reward#] " + "[" + plugin.PLAYER_DEF
						        .toLowerCase() + "]");
						return true;
					}
					if (args.length >= 3) {
						String otherPlayer = Utils.completeName(args[2]);
						if (otherPlayer == null) {
							sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
							        .replace("%player%", args[2]));
							return true;
						}
						int rewardIndex;
						try {
							rewardIndex = Integer.parseInt(args[1]) - 1;
						} catch (Exception e) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							return true;
						}
						ArrayList<Reward> rewards = rm.getRewards();
						if ((rewardIndex >= rewards.size()) || (rewardIndex < 0)) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							return true;
						}
						Reward reward = rewards.get(rewardIndex);
						sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You forced the reward " + ChatColor.YELLOW + reward
						        .getName() + ChatColor.AQUA + " to " + ChatColor.YELLOW + otherPlayer + ChatColor.AQUA + ".");
						rm.administerAwardContents(reward, otherPlayer);
					}
				}

				else if (args[0].equalsIgnoreCase("forcemilestone")) {
					if (!sender.hasPermission("voteroulette.forceawards")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					if (args.length < 3) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] /" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEMILESTONE_DEF + " " + "[milestone#] " + "[" + plugin.PLAYER_DEF
						        .toLowerCase() + "]");
						return true;
					}
					if (args.length >= 3) {
						String otherPlayer = Utils.completeName(args[2]);
						if (otherPlayer == null) {
							sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
							        .replace("%player%", args[2]));
							return true;
						}
						int milestoneIndex;
						try {
							milestoneIndex = Integer.parseInt(args[1]) - 1;
						} catch (Exception e) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							return true;
						}
						ArrayList<Milestone> milestones = rm.getMilestones();
						if ((milestoneIndex >= milestones.size()) || (milestoneIndex < 0)) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							return true;
						}
						Milestone milestone = milestones.get(milestoneIndex);
						sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You forced the milestone " + ChatColor.YELLOW + milestone
						        .getName() + ChatColor.AQUA + " to " + ChatColor.YELLOW + otherPlayer + ChatColor.AQUA + ".");
						rm.administerAwardContents(milestone, otherPlayer);
					}
				}

				else if (args[0].equalsIgnoreCase(plugin.REMIND_DEF) || args[0]
				        .equalsIgnoreCase("reminder") || args[0]
				        .equalsIgnoreCase("broadcast")) {
					if (!sender.hasPermission("voteroulette.remind")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					Bukkit.broadcastMessage(plugin.PERIODIC_REMINDER);
				}

				else if (args[0].equalsIgnoreCase(plugin.STATS_DEF)) {
					int lifetimeVotes, voteCycle, voteStreak, longestVoteStreak;
					if (args.length == 1) {
						if (!(sender instanceof Player)) {
							sender.sendMessage("[VoteRoulette] This command can't be ran by the console!");
							return true;
						}
						if (!sender.hasPermission("voteroulette.viewstats")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}

						lifetimeVotes = voter.getLifetimeVotes();
						String message = ChatColor.YELLOW + plugin.TOTAL_VOTES_DEF + ": " + ChatColor.AQUA + lifetimeVotes;
						if (plugin.REWARDS_ON_THRESHOLD) {
							voteCycle = voter.getCurrentVoteCycle();
							message = message + ChatColor.YELLOW + "\n" + plugin.VOTE_CYCLE_DEF + ": " + ChatColor.AQUA + voteCycle;
						}
						voteStreak = voter.getCurrentVoteStreak();
						message = message + ChatColor.YELLOW + "\n" + plugin.CURRENT_VOTE_STREAK_DEF + ": " + ChatColor.AQUA + voteStreak;

						longestVoteStreak = voter.getLongestVoteStreak();
						message = message + ChatColor.YELLOW + "\n" + plugin.LONGEST_VOTE_STREAK_DEF + ": " + ChatColor.AQUA + longestVoteStreak;
						sender.sendMessage(ChatColor.GOLD + "-----" + ChatColor.GREEN + plugin.VOTE_DEF + " " + plugin.STATS_DEF + ": " + ChatColor.YELLOW + sender
						        .getName() + ChatColor.GOLD + "-----"); // header
						                                                // of
						                                                // page
						                                                // with
						                                                // current
						                                                // and
						                                                // total
						                                                // pages
						sender.sendMessage(message);

					} else if (args.length == 2) {
						if (!sender
						        .hasPermission("voteroulette.viewotherstats")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}

						String otherPlayer = Utils.completeName(args[1]);
						if (otherPlayer == null) {
							sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
							        .replace("%player%", args[1]));
							return true;
						}
						Voter otherVoter = vm.getVoter(otherPlayer);
						lifetimeVotes = otherVoter.getLifetimeVotes();
						String message = ChatColor.YELLOW + plugin.TOTAL_VOTES_DEF + ": " + ChatColor.AQUA + lifetimeVotes;
						if (plugin.REWARDS_ON_THRESHOLD) {
							voteCycle = otherVoter.getCurrentVoteCycle();
							message = message + ChatColor.YELLOW + "\n" + plugin.VOTE_CYCLE_DEF + ": " + ChatColor.AQUA + voteCycle;
						}
						voteStreak = otherVoter.getCurrentVoteStreak();
						message = message + ChatColor.YELLOW + "\n" + plugin.CURRENT_VOTE_STREAK_DEF + ": " + ChatColor.AQUA + voteStreak;

						longestVoteStreak = otherVoter.getLongestVoteStreak();
						message = message + ChatColor.YELLOW + "\n" + plugin.LONGEST_VOTE_STREAK_DEF + ": " + ChatColor.AQUA + longestVoteStreak;
						sender.sendMessage(ChatColor.GOLD + "-----" + ChatColor.GREEN + plugin.VOTE_DEF + " " + plugin.STATS_DEF + ": " + ChatColor.YELLOW + otherPlayer + ChatColor.GOLD + "-----"); // header
						                                                                                                                                                                              // of
						                                                                                                                                                                              // page
						                                                                                                                                                                              // with
						                                                                                                                                                                              // current
						                                                                                                                                                                              // and
						                                                                                                                                                                              // total
						                                                                                                                                                                              // pages
						sender.sendMessage(message);
					} else if (args.length == 4) {
						// /vr
						// stats
						// player
						// settotal
						// ##
						if (!sender.hasPermission("voteroulette.editstats")) {
							sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
							return true;
						}

						String otherPlayer = Utils.completeName(args[1]);

						if (otherPlayer == null) {
							sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
							        .replace("%player%", args[1]));
							return true;
						}
						Voter otherVoter = vm.getVoter(otherPlayer);
						int voteNumber;
						try {
							voteNumber = Integer.parseInt(args[3]);
						} catch (Exception e) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							return true;
						}

						if (args[2].equalsIgnoreCase(plugin.SETTOTAL_DEF)) {
							otherVoter.setLifetimeVotes(voteNumber);
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You set " + ChatColor.YELLOW + otherPlayer + "'s " + ChatColor.AQUA + "total votes to " + ChatColor.YELLOW + voteNumber + ChatColor.AQUA + "!");
						} else if (args[2]
						        .equalsIgnoreCase(plugin.SETCYCLE_DEF)) {
							otherVoter.setCurrentVoteCycle(voteNumber);
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You set " + ChatColor.YELLOW + otherPlayer + "'s " + ChatColor.AQUA + "current vote cycle to " + ChatColor.YELLOW + voteNumber + ChatColor.AQUA + "!");
						} else if (args[2]
						        .equalsIgnoreCase(plugin.SETSTREAK_DEF)) {
							otherVoter.setCurrentVoteStreak(voteNumber);
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] You set " + ChatColor.YELLOW + otherPlayer + "'s " + ChatColor.AQUA + "vote streak to " + ChatColor.YELLOW + voteNumber + ChatColor.AQUA + "!");
						}
						VoteRoulette.getStatsManager().updateStats();
					} else if (args.length > 4) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] /" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF);
					}
				}
				// send vote totals, how
				// many votes till next
				// milestone, how many
				// votes in threshold
				else if (args[0].equalsIgnoreCase(plugin.MILESTONE_PURAL_DEF)) {
					// show
					// qualifying
					// milestones
					boolean seeingAll = false;
					if (!sender.hasPermission("voteroulette.viewmilestones")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}

					Milestone[] milestones = rm.getQualifiedMilestones(sender
					        .getName());

					if (!(sender instanceof Player)) {
						milestones = new Milestone[rm.getMilestones().size()];
						rm.getMilestones().toArray(milestones);
						seeingAll = true;
					}

					if (args.length > 1) {
						if (args[args.length - 1].equalsIgnoreCase("-a")) {
							if (sender
							        .hasPermission("voteroulette.viewallawards")) {
								ArrayList<Milestone> ms = rm.getMilestones();
								milestones = new Milestone[ms.size()];
								ms.toArray(milestones);
								seeingAll = true;
							}
						}
					}

					String message = "";
					if (!plugin.GUI_FOR_AWARDS || !(sender instanceof Player)) {
						for (int i = 0; i < milestones.length; i++) {
							message += ChatColor.YELLOW + plugin.MILESTONE_DEF + " #" + Integer
							        .toString(i + 1) + ": " + ChatColor.AQUA + milestones[i]
							        .getName() + "\n";
							message += ChatColor.GOLD + plugin.VOTES_DEF + ": " + ChatColor.DARK_AQUA + milestones[i]
							        .getVotes() + "\n";
							message += ChatColor.GOLD + "Recurring: " + ChatColor.DARK_AQUA + milestones[i]
							        .isRecurring() + "\n";
							if (milestones[i].hasDescription()) {
								message += ChatColor.GOLD + "Description: " + ChatColor.DARK_AQUA + milestones[i]
								        .getDescription() + "\n";
							}
							if (milestones[i].hasCurrency()) {
								String currency = Double.toString(milestones[i]
								        .getCurrency());
								if (currency.length() < 4) {
									if (currency.length() > 2) {
										currency += "0";
									} else {
										currency += "00";
									}
								}
								message += ChatColor.GOLD + plugin.CURRENCY_PURAL_DEF + ": " + ChatColor.DARK_AQUA + currency + "\n";
							}
							if (milestones[i].hasXpLevels()) {
								message += ChatColor.GOLD + plugin.XPLEVELS_DEF + ": " + ChatColor.DARK_AQUA + Integer
								        .toString(milestones[i].getXpLevels()) + "\n";
							}
							if (milestones[i].hasItems()) {
								message += ChatColor.GOLD + plugin.ITEM_PLURAL_DEF + ": " + ChatColor.DARK_AQUA + Utils
								        .getItemListString(milestones[i]
								                .getItems()) + "\n";
							}
							if (milestones[i].hasWorlds()) {
								message += ChatColor.GOLD + plugin.WORLDS_DEF + ": " + ChatColor.DARK_AQUA + Utils
								        .worldsString(milestones[i].getWorlds()) + "\n";
							}
							if (milestones[i].hasChance()) {
								message += ChatColor.GOLD + plugin.CHANCE_DEF + ": " + ChatColor.DARK_AQUA + milestones[i]
								        .getChanceMin() + " in " + milestones[i]
								        .getChanceMax() + "\n";
							}
							if (milestones[i].hasReroll()) {
								message += ChatColor.GOLD + "Reroll For: " + ChatColor.DARK_AQUA + milestones[i]
								        .getReroll().replace("ANY",
								                "Any Reward") + "\n";
							}
						}
					} else {
						if (plugin.isOn1dot7 && (args.length == 1)) {
							showClickableAwardList(sender, AwardType.MILESTONE,
							        milestones, 1, seeingAll);
							return true;
						}
						if (plugin.isOn1dot7 && (args.length == 2)) {
							if (args[1].equalsIgnoreCase("-a")) {
								showClickableAwardList(sender,
								        AwardType.MILESTONE, milestones, 1,
								        seeingAll);
								return true;
							}
						}
						message = message + ChatColor.GOLD + "Type " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF
						        .toLowerCase() + " see [#]" + ChatColor.GOLD + " to see details.\n";
						for (int i = 0; i < milestones.length; i++) {
							message += ChatColor.YELLOW + Integer
							        .toString(i + 1) + ") " + ChatColor.AQUA + milestones[i]
							        .getName() + "\n";
						}
					}
					Paginate milestonePag = new Paginate(message,
					        plugin.MILESTONE_PURAL_DEF,
					        commandLabel + " " + plugin.MILESTONE_PURAL_DEF
					                .toLowerCase());
					if (args.length >= 2) {
						if (plugin.GUI_FOR_AWARDS && (sender instanceof Player)) {
							if (args[1].equalsIgnoreCase("see")) {
								if (args.length < 3) {
									sender.sendMessage(ChatColor.RED + "[VoteRoulette] /" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF
									        .toLowerCase() + " see [#]");
								} else {
									try {
										int milestoneNumber = Integer
										        .parseInt(args[2]);
										if (args.length == 4) {
											if (args[3].equalsIgnoreCase("-a")) {
												ArrayList<Milestone> milestonesAL = rm
												        .getMilestones();
												milestones = new Milestone[milestonesAL
												        .size()];
												milestonesAL
												        .toArray(milestones);
											}
										}
										if (milestoneNumber > milestones.length) {
											sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
											return true;
										} else {

											Milestone milestone = milestones[milestoneNumber - 1];
											Player p = (Player) sender;
											Utils.showAwardGUI(milestone, p,
											        milestoneNumber - 1);
											return true;

										}
									} catch (Exception e) {
										sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
										return true;
									}
								}
							}
						} else {
							try {
								int pageNumber = Integer.parseInt(args[1]);
								if (plugin.isOn1dot7 && (args.length >= 2) && plugin.GUI_FOR_AWARDS && (sender instanceof Player)) {
									showClickableAwardList(sender,
									        AwardType.MILESTONE, milestones,
									        pageNumber, seeingAll);
									return true;
								}
								if (pageNumber <= milestonePag.pageTotal()) {
									milestonePag.sendPage(pageNumber, sender);
								} else {
									sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
								}
							} catch (Exception e) {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						}
					} else {
						milestonePag.sendPage(1, sender);
					}
				} else if (args[0].equalsIgnoreCase(plugin.REWARDS_PURAL_DEF)) {

					if (!sender.hasPermission("voteroulette.viewrewards")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}

					boolean seeingAll = false;

					Reward[] rewards = rm.getQualifiedRewards(sender.getName(),
					        true);

					if (!(sender instanceof Player)) {
						rewards = new Reward[rm.getRewards().size()];
						rm.getRewards().toArray(rewards);
						seeingAll = true;
					}

					if (args.length > 1) {
						if (args[args.length - 1].equalsIgnoreCase("-a")) {
							if (sender
							        .hasPermission("voteroulette.viewallawards")) {
								ArrayList<Reward> rs = rm.getRewards();
								rewards = new Reward[rs.size()];
								rs.toArray(rewards);
								seeingAll = true;
							}
						}
					}

					String message = "";

					if (!plugin.GUI_FOR_AWARDS || !(sender instanceof Player)) {
						for (int i = 0; i < rewards.length; i++) {
							if (rewards[i].isEmpty()) {
								continue;
							}
							message = message + ChatColor.YELLOW + plugin.RELOAD_DEF + " #" + Integer
							        .toString(i + 1) + ": " + ChatColor.AQUA + rewards[i]
							        .getName() + "\n";
							if (rewards[i].hasDescription()) {
								message += ChatColor.GOLD + "Description: " + ChatColor.DARK_AQUA + rewards[i]
								        .getDescription() + "\n";
							}
							if (rewards[i].hasCurrency()) {
								String currency = Double.toString(rewards[i]
								        .getCurrency());
								if (currency.length() < 4) {
									if (currency.length() > 2) {
										currency += "0";
									} else {
										currency += "00";
									}
								}
								message += ChatColor.GOLD + plugin.CURRENCY_PURAL_DEF + ": " + ChatColor.DARK_AQUA + currency + "\n";
							}
							if (rewards[i].hasVoteStreak()) {
								message += ChatColor.GOLD + plugin.VOTE_STREAK_DEF + ": " + ChatColor.DARK_AQUA + Integer
								        .toString(rewards[i].getVoteStreak());
								if (rewards[i].hasVoteStreakModifier()) {
									VoteStreakModifier vsm = rewards[i]
									        .getVoteStreakModifier();
									message += " " + vsm.toString()
									        .toLowerCase().replace("_", " ") + " ";
									if (rewards[i].getVoteStreak() > 1) {
										message += plugin.DAY_PLURAL_DEF;
									} else {
										message += plugin.DAY_DEF;
									}
								}
								message += "\n";
							}
							if (rewards[i].hasXpLevels()) {
								message += ChatColor.GOLD + plugin.XPLEVELS_DEF + ": " + ChatColor.DARK_AQUA + Integer
								        .toString(rewards[i].getXpLevels()) + "\n";
							}
							if (rewards[i].hasItems()) {
								message += ChatColor.GOLD + plugin.ITEM_DEF + ": " + ChatColor.DARK_AQUA + Utils
								        .getItemListString(rewards[i]
								                .getItems()) + "\n";
							}
							if (rewards[i].hasWorlds()) {
								message += ChatColor.GOLD + plugin.WORLDS_DEF + ": " + ChatColor.DARK_AQUA + Utils
								        .worldsString(rewards[i].getWorlds()) + "\n";
							}
							if (rewards[i].hasChance()) {
								message += ChatColor.GOLD + plugin.CHANCE_DEF + ": " + ChatColor.DARK_AQUA + rewards[i]
								        .getChanceMin() + " in " + rewards[i]
								        .getChanceMax() + "\n";
							}
							if (rewards[i].hasReroll()) {
								message += ChatColor.GOLD + "Reroll For: " + ChatColor.DARK_AQUA + rewards[i]
								        .getReroll().replace("ANY",
								                "Any Reward") + "\n";
							}
						}
					} else {
						if (plugin.isOn1dot7 && (args.length == 1)) {
							showClickableAwardList(sender, AwardType.REWARD,
							        rewards, 1, seeingAll);
							return true;
						}
						if (plugin.isOn1dot7 && (args.length == 2)) {
							if (args[1].equalsIgnoreCase("-a")) {
								showClickableAwardList(sender,
								        AwardType.REWARD, rewards, 1, seeingAll);
								return true;
							}
						}
						message = message + ChatColor.GOLD + "Type " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF
						        .toLowerCase() + " see [#]" + ChatColor.GOLD + " to see details.\n";
						for (int i = 0; i < rewards.length; i++) {
							if (rewards[i].isEmpty()) {
								continue;
							}
							message = message + ChatColor.YELLOW + Integer
							        .toString(i + 1) + ") " + ChatColor.AQUA + rewards[i]
							        .getName() + "\n";
						}
					}

					Paginate rewardPag = new Paginate(message,
					        plugin.REWARDS_PURAL_DEF,
					        commandLabel + " " + plugin.REWARDS_PURAL_DEF);

					if (args.length >= 2) {
						if (plugin.GUI_FOR_AWARDS && (sender instanceof Player)) {
							if (args[1].equalsIgnoreCase("see")) {
								if (args.length < 3) {
									sender.sendMessage(ChatColor.RED + "[VoteRoulette] /" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF
									        .toLowerCase() + " see [#]");
								} else {
									try {
										int rewardNumber = Integer
										        .parseInt(args[2]);
										if (args.length == 4) {
											if (args[3].equalsIgnoreCase("-a")) {
												ArrayList<Reward> rewardsAL = rm
												        .getRewards();
												rewards = new Reward[rewardsAL
												        .size()];
												rewardsAL.toArray(rewards);
											}
										}
										if (rewardNumber > rewards.length) {
											sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
											return true;
										} else {
											Reward reward = rewards[rewardNumber - 1];
											Player p = (Player) sender;
											Utils.showAwardGUI(reward, p,
											        rewardNumber - 1);
											return true;

										}
									} catch (Exception e) {
										sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
										return true;
									}
								}
							}
						}
						try {
							int pageNumber = Integer.parseInt(args[1]);
							if (plugin.isOn1dot7 && (args.length >= 2) && plugin.GUI_FOR_AWARDS && (sender instanceof Player)) {
								showClickableAwardList(sender,
								        AwardType.REWARD, rewards, pageNumber,
								        seeingAll);
								return true;
							}
							message = message + ChatColor.GOLD + "Type " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF
							        .toLowerCase() + " see [#]";
							if (seeingAll) {
								message += " -a";
							}
							message += "" + ChatColor.GOLD + " to see details.\n";

							for (int i = 0; i < rewards.length; i++) {
								if (rewards[i].isEmpty()) {
									continue;
								}
								message = message + ChatColor.YELLOW + Integer
								        .toString(i + 1) + ") " + ChatColor.AQUA + rewards[i]
								        .getName() + "\n";
							}

							if (pageNumber <= rewardPag.pageTotal()) {
								rewardPag.sendPage(pageNumber, sender);
							} else {
								sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
							}
						} catch (Exception e) {
							sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
						}
					} else {
						rewardPag.sendPage(1, sender);
					}
					// show
					// qualifying
					// rewards
				} else if (args[0].equalsIgnoreCase(plugin.RELOAD_DEF)) {
					if (!sender.hasPermission("voteroulette.admin")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					plugin.loadAllFilesAndData();
					sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] Reload complete!");
					// reload
					// configs
				} else if (args[0].equalsIgnoreCase(plugin.WIPESTATS_DEF)) {
					if (!sender.hasPermission("voteroulette.wipestats")) {
						sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
						return true;
					}
					if (args.length == 1) {
						sender.sendMessage(ChatColor.RED + "[VoteRoulette] Please include a players name to wipe or type \"all\" to wipe everyones.");
						return true;
					}
					if (args.length >= 2) {
						Stat stat;
						if (args.length == 2) {
							stat = Stat.ALL;
						} else {
							stat = Utils.getVoteStatFromStr(args[2]);
						}
						if (stat == null) {
							sender.sendMessage(ChatColor.RED + "[VoteRoulette] Unrecognized stat type! Available stats: votecycle, votestreak, longestvotestreak, totalvotes, lastvote, unclaimedrewards, unclaimedmilestones, all");
							return true;
						}
						if (args[1].equalsIgnoreCase("all")) {
							File[] files = new File(
							        plugin.getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + "playerdata")
							        .listFiles();
							for (File file : files) {
								if (file.isFile()) {
									if (file.getName().endsWith(".yml")) {
										String uuid = file.getName();
										ConfigAccessor playerCfg = new ConfigAccessor(
										        "data" + File.separator + "playerdata" + File.separator + uuid);
										Voter voterObj = vm.getVoter(playerCfg
										        .getConfig().getString("name",
										                ""));
										if (voterObj.isReal()) {
											voterObj.wipeStat(stat);
										}
									}
								}
							}

							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] Wiped everyones stat: " + stat
							        .toString().toLowerCase().replace("_", " "));
						} else {
							String otherPlayer = Utils.completeName(args[1]);
							if (otherPlayer == null) {
								sender.sendMessage(plugin.CANT_FIND_PLAYER_NOTIFICATION
								        .replace("%player%", args[1]));
								return true;
							}
							vm.getVoter(otherPlayer).wipeStat(stat);
							sender.sendMessage(ChatColor.AQUA + "[VoteRoulette] Wiped " + otherPlayer + " stat: " + stat
							        .toString().toLowerCase().replace("_", " "));
						}
						VoteRoulette.getStatsManager().updateStats();
					}
					// reload
					// configs
				} else if (args[0].equalsIgnoreCase(plugin.CLAIM_DEF)) {
					if (!(sender instanceof Player)) {
						sender.sendMessage("[VoteRoulette] This command can't be used in the console!");
						return true;
					}

					int unclaimedRewardsCount = voter.getUnclaimedRewardCount();
					int unclaimedMilestonesCount = voter
					        .getUnclaimedMilestoneCount();
					if (args.length == 1) {
						if (unclaimedRewardsCount > 0) {
							sender.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION
							        .replace(
							                "%type%",
							                plugin.REWARDS_PURAL_DEF
							                        .toLowerCase())
							        .replace(
							                "%amount%",
							                Integer.toString(unclaimedRewardsCount))
							        .replace(
							                "%command%",
							                "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF
							                        .toLowerCase()));
						} else {
							sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION
							        .replace("%type%", plugin.REWARDS_PURAL_DEF
							                .toLowerCase()));
						}
						if (unclaimedMilestonesCount > 0) {
							sender.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION
							        .replace(
							                "%type%",
							                plugin.MILESTONE_PURAL_DEF
							                        .toLowerCase())
							        .replace(
							                "%amount%",
							                Integer.toString(unclaimedMilestonesCount))
							        .replace(
							                "%command%",
							                "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF
							                        .toLowerCase()));
						} else {
							sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION
							        .replace("%type%",
							                plugin.MILESTONE_PURAL_DEF
							                        .toLowerCase()));
						}
					} else if (args.length >= 2) {
						if (args[1].equalsIgnoreCase(plugin.REWARDS_PURAL_DEF)) {

							if (unclaimedRewardsCount == 0) {
								sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION
								        .replace("%type%",
								                plugin.REWARDS_PURAL_DEF
								                        .toLowerCase()));
								return true;
							}
							List<Reward> unclaimedRewards = voter
							        .getUnclaimedRewards();
							if (args.length == 2) {
								String[] rewardMessages = new String[unclaimedRewards
								        .size()];
								int count = 0;
								for (Reward reward : unclaimedRewards) {
									rewardMessages[count] = ChatColor.YELLOW + Integer
									        .toString(count + 1) + ") " + ChatColor.GOLD + "Name: \"" + ChatColor.YELLOW + reward
									        .getName() + ChatColor.GOLD + "\", Required Slots: " + ChatColor.YELLOW + reward
									        .getRequiredSlots();
									count++;
								}
								sender.sendMessage(ChatColor.AQUA + "-----[Unclaimed " + plugin.REWARDS_PURAL_DEF + "]-----");
								sender.sendMessage(rewardMessages);
								sender.sendMessage(ChatColor.AQUA + "Type " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF
								        .toLowerCase() + " " + plugin.REWARDS_PURAL_DEF
								        .toLowerCase() + " #" + ChatColor.AQUA + " or " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF
								        .toLowerCase() + " " + plugin.REWARDS_PURAL_DEF
								        .toLowerCase() + " " + plugin.ALL_DEF
								        .toLowerCase());

							} else if (args.length == 3) {
								if (args[2].equalsIgnoreCase(plugin.ALL_DEF)) {
									if (sender
									        .hasPermission("voteroulette.claimall")) {
										for (Reward reward : unclaimedRewards) {
											voter.removeUnclaimedReward(reward
											        .getName());
											rm.administerAwardContents(reward,
											        sender.getName());
										}
									} else {
										sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
									}
								} else {
									try {
										int rewardNumber = Integer
										        .parseInt(args[2]);
										if (rewardNumber <= unclaimedRewards
										        .size()) {
											Reward reward = unclaimedRewards
											        .get(rewardNumber - 1);
											voter.removeUnclaimedReward(reward
											        .getName());
											rm.administerAwardContents(reward,
											        sender.getName());
										} else {
											sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
										}
									} catch (Exception e) {
										sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
									}
								}
							}
						} else if (args[1]
						        .equalsIgnoreCase(plugin.MILESTONE_PURAL_DEF)) {

							if (unclaimedMilestonesCount == 0) {
								sender.sendMessage(plugin.NO_UNCLAIMED_AWARDS_NOTIFICATION
								        .replace("%type%",
								                plugin.MILESTONE_PURAL_DEF
								                        .toLowerCase()));
								return true;
							}
							List<Milestone> unclaimedMilestones = voter
							        .getUnclaimedMilestones();
							if (args.length == 2) {
								String[] milestoneMessages = new String[unclaimedMilestones
								        .size()];
								int count = 0;
								for (Milestone milestone : unclaimedMilestones) {
									milestoneMessages[count] = ChatColor.YELLOW + Integer
									        .toString(count + 1) + ") " + ChatColor.GOLD + "Name: \"" + ChatColor.YELLOW + milestone
									        .getName() + ChatColor.GOLD + "\", Required Slots: " + ChatColor.YELLOW + milestone
									        .getRequiredSlots();
									count++;
								}
								sender.sendMessage(ChatColor.AQUA + "-----[Unclaimed " + plugin.MILESTONE_PURAL_DEF + "]-----");
								sender.sendMessage(milestoneMessages);
								sender.sendMessage(ChatColor.AQUA + "Type " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF
								        .toLowerCase() + " " + plugin.MILESTONE_PURAL_DEF
								        .toLowerCase() + " #" + ChatColor.AQUA + " or " + ChatColor.YELLOW + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF
								        .toLowerCase() + " " + plugin.MILESTONE_PURAL_DEF
								        .toLowerCase() + " " + plugin.ALL_DEF
								        .toLowerCase());

							} else if (args.length == 3) {
								if (args[2].equalsIgnoreCase(plugin.ALL_DEF)) {
									if (sender
									        .hasPermission("voteroulette.claimall")) {
										for (Milestone milestone : unclaimedMilestones) {
											voter.removeUnclaimedMilestone(milestone
											        .getName());
											rm.administerAwardContents(
											        milestone, sender.getName());
										}
									} else {
										sender.sendMessage(plugin.NO_PERM_NOTIFICATION);
									}
								} else {
									try {
										int rewardNumber = Integer
										        .parseInt(args[2]);
										if (rewardNumber <= unclaimedMilestones
										        .size()) {
											Milestone milestone = unclaimedMilestones
											        .get(rewardNumber - 1);
											voter.removeUnclaimedMilestone(milestone
											        .getName());
											rm.administerAwardContents(
											        milestone, sender.getName());
										} else {
											sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
										}
									} catch (Exception e) {
										sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
									}
								}
							}
						} else {
							sender.sendMessage(plugin.BASE_CMD_NOTIFICATION);
						}
					}
				} else {
					sender.sendMessage(plugin.BASE_CMD_NOTIFICATION);
				}
			}
		}
		// commands
		return true;
	}

	void showClickableAwardList(CommandSender sender, AwardType type, Award[] awards, int pageNumber, boolean seeingAll) {
		int totalPages = (int) Math.ceil(awards.length / 7.0);
		if (pageNumber > totalPages) {
			sender.sendMessage(plugin.INVALID_NUMBER_NOTIFICATION);
			return;
		}
		String typeStr;
		if (type == AwardType.REWARD) {
			typeStr = plugin.REWARDS_PURAL_DEF;
		} else {
			typeStr = plugin.MILESTONE_PURAL_DEF;
		}
		sender.sendMessage(ChatColor.GOLD + "-----" + ChatColor.GREEN + typeStr + ChatColor.GOLD + " | " + ChatColor.GREEN + "Page " + pageNumber + "/" + totalPages + ChatColor.GOLD + "-----");
		if (pageNumber == 1) {
			sender.sendMessage(ChatColor.GOLD + "Click on a name to see it's contents.");
		}
		int count = 0 + (7 * (pageNumber - 1));
		while ((count < (7 * pageNumber)) && (count < awards.length)) {
			String tellRaw = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"%num%) \",\"color\":\"yellow\"},{\"text\":\"%name%\",\"color\":\"aqua\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%command%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click to see contents.\"}}]}";
			String command = "/" + plugin.DEFAULT_ALIAS + " " + typeStr
			        .toLowerCase() + " see " + Integer.toString(count + 1);
			if (seeingAll) {
				command += " -a";
			}
			tellRaw = tellRaw.replace("%player%", sender.getName())
			        .replace("%num%", Integer.toString(count + 1))
			        .replace("%name%", awards[count].getName())
			        .replace("%command%", command);
			Bukkit.getServer().dispatchCommand(
			        Bukkit.getServer().getConsoleSender(), tellRaw);
			count++;
		}
		if (pageNumber < totalPages) {
			sender.sendMessage(ChatColor.GOLD + "Type " + "\"/" + plugin.DEFAULT_ALIAS + " " + typeStr
			        .toLowerCase() + " " + Integer.toString(pageNumber + 1) + "\" for the next page.");
		}
	}
}
