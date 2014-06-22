package com.mythicacraft.voteroulette.awardcreator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.awardcreator.AwardCreator.AwardCreationStage;
import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.awards.Reward.VoteStreakModifier;
import com.mythicacraft.voteroulette.utils.Utils;

public class ACListener implements Listener {

	private static VoteRoulette plugin;

	public ACListener(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (VoteRoulette.inAwardCreator.containsKey(player)) {
			if (VoteRoulette.inAwardCreator.get(player).isPaused())
				return;

			AwardCreator ac = VoteRoulette.inAwardCreator.get(player);
			AwardCreationStage stage = ac.getCurrentStage();
			if (stage == AwardCreationStage.ADD_COMMAND) {

				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.runTaskAsynchronously(plugin, new Runnable() {

					private AwardCreator ac;
					private String message;

					@Override
					public void run() {
						ac.getAward().getCommands()
						        .add(message.replace("/", ""));
						ac.goToStage(AwardCreationStage.EDIT_COMMANDS);
					}

					private Runnable init(AwardCreator ac, String message) {
						this.ac = ac;
						this.message = message;
						return this;
					}
				}.init(ac, event.getMessage()));

				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (VoteRoulette.inAwardCreator.containsKey(player)) {
			if (VoteRoulette.inAwardCreator.get(player).isPaused())
				return;
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.runTaskLaterAsynchronously(plugin, new Runnable() {

				private Player player;
				private String message;

				@Override
				public void run() {
					processChat(player, message);
				}

				private Runnable init(Player player, String message) {
					this.player = player;
					this.message = message;
					return this;
				}
			}.init(player, event.getMessage()), 1);
			event.setCancelled(true);
		}
	}

	private void processChat(Player player, String message) {
		if (VoteRoulette.inAwardCreator.containsKey(player)) {
			message = message.trim();
			if (message.equalsIgnoreCase("cancel")
			        || message.equalsIgnoreCase("quit")
			        || message.equalsIgnoreCase("stop")
			        || message.equalsIgnoreCase("exit")) {
				VoteRoulette.inAwardCreator.remove(player);
				player.sendMessage(ChatColor.YELLOW
				        + "* "
				        + message.replaceFirst(Character.toString(message
				                .charAt(0)),
				                Character.toString(message.charAt(0))
				                        .toUpperCase())
				        + "ing award creation...");
				player.sendMessage(ChatColor.YELLOW + "[VoteRoulette] "
				        + ChatColor.AQUA + "Exited the Award Creator!");
				return;
			}

			if (message.equalsIgnoreCase("help")
			        || message.equalsIgnoreCase("?")) {
				player.sendMessage(ChatColor.AQUA + "[Award Creator Help]");
				player.sendMessage(ChatColor.AQUA
				        + "- "
				        + ChatColor.YELLOW
				        + "The Award Creator allows you to create awards entirely in-game!");
				player.sendMessage(ChatColor.AQUA
				        + "- "
				        + ChatColor.YELLOW
				        + "Type your entries straight into chat, your input wont be shown to other players!");
				player.sendMessage(ChatColor.AQUA
				        + "- "
				        + ChatColor.YELLOW
				        + "Text in "
				        + ChatColor.GOLD
				        + "orange"
				        + ChatColor.YELLOW
				        + " indicate trigger words that tell Award Creator to do something.");
				player.sendMessage(ChatColor.AQUA + "- " + ChatColor.YELLOW
				        + "At anytime you can type: " + ChatColor.GOLD
				        + "cancel" + ChatColor.YELLOW + " to quit the AC, "
				        + ChatColor.GOLD + "pause" + ChatColor.YELLOW
				        + " to temporarly leave the AC (So you can chat), and "
				        + ChatColor.GOLD + "current" + ChatColor.YELLOW
				        + " to see your current step again.");
				player.sendMessage(ChatColor.AQUA + "- " + ChatColor.YELLOW
				        + "When adding an award prize or option, you can type "
				        + ChatColor.GOLD + "clear" + ChatColor.YELLOW
				        + " to clear the previous setting.");
				return;
			}

			AwardCreator ac = VoteRoulette.inAwardCreator.get(player);
			AwardCreationStage stage = ac.getCurrentStage();

			if (message.equalsIgnoreCase("pause")) {
				player.sendMessage(ChatColor.AQUA
				        + "*"
				        + ChatColor.YELLOW
				        + " Paused award creation. Type \"/vr create\" to resume.");

				player.sendMessage(ChatColor.YELLOW + "[VoteRoulette] "
				        + ChatColor.AQUA + "Exited the Award Creator!");
				ac.setPaused(true);
				return;
			}

			if (message.equalsIgnoreCase("current")
			        || message.equalsIgnoreCase("step")) {
				ac.goToStage(ac.getCurrentStage(), true);
				return;
			}

			if (message.equalsIgnoreCase("preview")) {
				if (ac.getAward() == null) {
					player.sendMessage(ChatColor.RED + "No award to preview!");
					return;
				} else
					if (ac.getAward().isEmpty()) {
						player.sendMessage(ChatColor.RED + "Award is empty!");
						return;
					}
				Utils.showAwardGUI(ac.getAward(), player, 1, true);
				return;
			}

			player.sendMessage(ChatColor.ITALIC + message);

			if (message.equalsIgnoreCase("back")) {
				if (ac.getPreviousStages() != null) {
					if (!ac.getPreviousStages().isEmpty()) {
						ac.goToStage(ac.getPreviousStages().pop(), true);
						return;
					} else {
						player.sendMessage(ChatColor.RED
						        + "There is not a previous step to go back too!");
						return;
					}
				}
				player.sendMessage(ChatColor.RED
				        + "There is not a previous step to go back too!");
				return;
			}

			AwardType awardType = ac.getAwardType();

			switch (stage) {
			case CHOOSE_AWARD:
				if (message.equalsIgnoreCase("reward")) {
					ac.setAwardType(AwardType.REWARD);
				} else
					if (message.equalsIgnoreCase("milestone")) {
						ac.setAwardType(AwardType.MILESTONE);
					} else {
						player.sendMessage(ChatColor.RED
						        + "That is not a recongized award type!");
						return;
					}
				ac.goToStage(AwardCreationStage.NAME);
				break;
			case ADD_CHANCE:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setHasChance(false);
					ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
				} else {
					message = message.replace("%", "");
					if (message.contains("/")) {
						String[] split = message.split("/");
						int count = 1;
						int min = 0;
						int max = 0;
						for (String chanceOption : split) {
							try {
								if (count == 1) {
									min = Integer.parseInt(chanceOption);
								} else {
									max = Integer.parseInt(chanceOption);
								}
							} catch (Exception e) {
								player.sendMessage(ChatColor.RED
								        + "That is not a recognized chance format. Please type chance as \"#%\" or \"#/#\".");
								return;
							}
							count++;
						}
						ac.getAward().setChanceMin(min);
						ac.getAward().setChanceMax(max);
						ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
					} else {
						try {
							ac.getAward().setChanceMin(
							        Integer.parseInt(message));
							ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED
							        + "That is not a recognized chance format. Please type chance as \"#%\" or \"#/#\".");
							return;
						}
					}
				}
				break;
			case EDIT_COMMANDS:
				if (message.equalsIgnoreCase("add")) {
					ac.goToStage(AwardCreationStage.ADD_COMMAND);
				} else
					if (message.equalsIgnoreCase("remove")) {
						if (!ac.getAward().hasCommands()) {
							player.sendMessage(ChatColor.RED
							        + "There are no commands to remove yet!");
							return;
						}
						ac.goToStage(AwardCreationStage.REMOVE_COMMAND);
					} else
						if (message.equalsIgnoreCase("next")) {
							ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
						} else {
							player.sendMessage(ChatColor.RED
							        + "Unrecongized input! Type add, remove, or next.");
						}
				break;
			case ADD_COMMAND:
				ac.getAward().getCommands().add(message.replace("/", ""));
				ac.goToStage(AwardCreationStage.EDIT_COMMANDS);
				break;
			case REMOVE_COMMAND:
				try {
					int number = Integer.parseInt(message);
					ac.getAward().getCommands().remove(number - 1);
					ac.goToStage(AwardCreationStage.EDIT_COMMANDS);
				} catch (Exception e) {
					player.sendMessage(ChatColor.RED
					        + "That is not a valid command number to remove. Please try again or type back.");
				}
				break;
			case ADD_CURRENCY:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setCurrency(0);
					ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
				} else {
					try {
						double money = Double.parseDouble(message);
						ac.getAward().setCurrency(money);
						ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
					} catch (Exception e) {
						player.sendMessage(ChatColor.RED
						        + "That is not a recognized number. Please try again.");
					}
				}
				break;
			case ADD_DESCRIPTION:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setDescription(null);
				} else {
					ac.getAward().setDescription(message);
				}
				ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				break;
			case ADD_ITEMS:
				if (message.equalsIgnoreCase("add")) {
					ac.getAward().clearItems();
					ItemStack[] items = player.getInventory().getContents();
					for (ItemStack item : items) {
						if (item != null) {
							if (item.getType() != Material.AIR) {
								ac.getAward().addItem(item);
							}
						}
					}
					ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
				} else
					if (message.equalsIgnoreCase("clear")) {
						ac.getAward().clearItems();
						ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
					} else {
						player.sendMessage(ChatColor.RED
						        + "Unrecognized input! Type \"add\" to add your inventory as award items or \"back\" to go back to previous step.");
					}
				break;
			case ADD_MESSAGE:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setMessage(null);
				} else {
					ac.getAward().setMessage(message);
				}
				ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				break;
			case ADD_PERMGROUPS:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setPermGroups(null);
				} else {
					String[] permGroups = message.split(",");
					for (int i = 0; i < permGroups.length; i++) {
						permGroups[i] = permGroups[i].trim();
					}
					ac.getAward().setPermGroups(permGroups);
				}
				ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				break;
			case ADD_PLAYERS:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setPlayers(null);
				} else {
					String[] players = message.split(",");
					for (int i = 0; i < players.length; i++) {
						players[i] = players[i].trim();
					}
					ac.getAward().setPlayers(players);
				}
				ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				break;
			case ADD_REROLL:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setReroll(null);
				} else {
					ac.getAward().setReroll(message);
				}
				ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				break;
			case ADD_WEBSITES:
				Reward reward = (Reward) ac.getAward();
				if (message.equalsIgnoreCase("clear")) {
					reward.getWebsites().clear();
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				} else {
					List<String> websites = new ArrayList<String>();
					String[] websiteArray = message.split(",");
					for (String website : websiteArray) {
						websites.add(website.trim());
					}
					reward.setWebsites(websites);
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				}
				break;
			case ADD_VOTESTREAK:
				Reward r = (Reward) ac.getAward();
				if (message.equalsIgnoreCase("clear")) {
					r.setVoteStreak(0);
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				} else {
					String voteStringInfo = message.toLowerCase();
					Pattern p = Pattern.compile("\\d+");
					Matcher m = p.matcher(voteStringInfo);
					if (m.find()) {
						try {
							r.setVoteStreak(Integer.parseInt(m.group()));
							if (voteStringInfo.contains("or more")) {
								r.setVoteStreakModifier(VoteStreakModifier.OR_MORE);
							} else
								if (voteStringInfo.contains("or less")) {
									r.setVoteStreakModifier(VoteStreakModifier.OR_LESS);
								}
							ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED
							        + "Unrecognized input!");
						}
					}
				}
				break;
			case ADD_WORLDS:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().getWorlds().clear();
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				} else {
					List<String> worlds = new ArrayList<String>();
					String[] worldArray = message.split(",");
					for (String worldName : worldArray) {
						worlds.add(worldName.trim());
					}
					ac.getAward().setWorlds(worlds);
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				}
				break;
			case ADD_XP:
				if (message.equalsIgnoreCase("clear")) {
					ac.getAward().setXpLevels(0);
					ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
				} else {
					try {
						int levels = Integer.parseInt(message);
						ac.getAward().setXpLevels(levels);
						ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
					} catch (Exception e) {
						player.sendMessage(ChatColor.RED
						        + "That is not a recognized number. Please try again.");
					}
				}
				break;
			case CHOOSE_OPTION:
				if (message.equalsIgnoreCase("permgroups")) {
					ac.goToStage(AwardCreationStage.ADD_PERMGROUPS);
				} else
					if (message.equalsIgnoreCase("players")) {
						ac.goToStage(AwardCreationStage.ADD_PLAYERS);
					} else
						if (message.equalsIgnoreCase("worlds")) {
							ac.goToStage(AwardCreationStage.ADD_WORLDS);
						} else
							if (message.equalsIgnoreCase("websites")
							        && ac.getAwardType() == AwardType.REWARD) {
								ac.goToStage(AwardCreationStage.ADD_WEBSITES);
							} else
								if (message.equalsIgnoreCase("votestreak")
								        && ac.getAwardType() == AwardType.REWARD) {
									ac.goToStage(AwardCreationStage.ADD_VOTESTREAK);
								} else
									if (message.equalsIgnoreCase("priority")
									        && ac.getAwardType() == AwardType.MILESTONE) {
										ac.goToStage(AwardCreationStage.SET_PRIORITY);
									} else
										if (message
										        .equalsIgnoreCase("recurring")
										        && ac.getAwardType() == AwardType.MILESTONE) {
											ac.goToStage(AwardCreationStage.SET_RECURRING);
										} else
											if (message
											        .equalsIgnoreCase("votes")
											        && ac.getAwardType() == AwardType.MILESTONE) {
												ac.goToStage(AwardCreationStage.SET_VOTES);
											} else
												if (message
												        .equalsIgnoreCase("chance")) {
													ac.goToStage(AwardCreationStage.ADD_CHANCE);
												} else
													if (message
													        .equalsIgnoreCase("reroll")) {
														ac.goToStage(AwardCreationStage.ADD_REROLL);
													} else
														if (message
														        .equalsIgnoreCase("description")) {
															ac.goToStage(AwardCreationStage.ADD_DESCRIPTION);
														} else
															if (message
															        .equalsIgnoreCase("message")) {
																ac.goToStage(AwardCreationStage.ADD_MESSAGE);
															} else
																if (message
																        .equalsIgnoreCase("back")) {
																	ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
																} else
																	if (message
																	        .equalsIgnoreCase("rename")) {
																		ac.goToStage(AwardCreationStage.NAME);
																	} else
																		if (message
																		        .equalsIgnoreCase("next")) {
																			ac.goToStage(AwardCreationStage.FINAL_CONFIRMATION);
																		} else {
																			player.sendMessage(ChatColor.RED
																			        + "Unrecognized award option!");
																		}
				break;
			case CHOOSE_PRIZE:
				if (ac.getAward() != null) {
					if (!ac.getAward().isEmpty()) {
						if (message.equalsIgnoreCase("next")
						        || message.equalsIgnoreCase("continue")) {
							ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
							return;
						}
					}
				}
				if (message.equalsIgnoreCase("xp")) {
					ac.goToStage(AwardCreationStage.ADD_XP);
					return;
				} else
					if (message.equalsIgnoreCase("currency")) {
						ac.goToStage(AwardCreationStage.ADD_CURRENCY);
						return;
					} else
						if (message.equalsIgnoreCase("items")) {
							ac.goToStage(AwardCreationStage.ADD_ITEMS);
							return;
						} else
							if (message.equalsIgnoreCase("commands")) {
								ac.goToStage(AwardCreationStage.EDIT_COMMANDS);
								return;
							}
				player.sendMessage(ChatColor.RED
				        + "That is not a recognized prize type. Please type items, xp, currency, or commands.");
				break;
			case FINAL_CONFIRMATION:
				if (message.equalsIgnoreCase("finish")
				        || message.equalsIgnoreCase("save")
				        || message.equalsIgnoreCase("done")) {
					if (ac.hasOrigAward()) {
						Award a = ac.getOrigAward();
						VoteRoulette.getAwardManager().deleteAwardFromFile(a);
						if (a.getAwardType() == AwardType.REWARD) {
							VoteRoulette.getAwardManager().getRewards()
							        .remove(ac.getOrigIndex());
						} else {
							VoteRoulette.getAwardManager().getMilestones()
							        .remove(ac.getOrigIndex());
						}
					}
					VoteRoulette.getAwardManager().saveAwardToFile(
					        ac.getAward());
					VoteRoulette.getAwardManager().addAward(ac.getAward());
					player.sendMessage(ChatColor.YELLOW
					        + "* Successfully created and saved the "
					        + ac.getAwardType().toString().toLowerCase() + " "
					        + ChatColor.AQUA + ac.getAward().getName()
					        + ChatColor.YELLOW + "!");
					VoteRoulette.inAwardCreator.remove(player);
					player.sendMessage(ChatColor.YELLOW + "[VoteRoulette] "
					        + ChatColor.AQUA + "Exited the Award Creator!");
				}
				break;
			case NAME:
				Award award = VoteRoulette.getAwardManager().getAwardByName(
				        message, awardType);
				if (award != null) {
					player.sendMessage(ChatColor.RED + "There is already a "
					        + awardType.toString().toLowerCase()
					        + " with this name!");
					player.sendMessage(ChatColor.RED
					        + "Please enter a different name.");
					return;
				}
				if (awardType == AwardType.REWARD) {
					if (ac.getAward() == null) {
						ac.setAward(new Reward(message));
					} else {
						ac.getAward().setName(message);
					}
					if (ac.getPreviousStages().peek() == AwardCreationStage.CHOOSE_OPTION) {
						ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
					} else {
						ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
					}
					return;
				} else
					if (awardType == AwardType.MILESTONE) {
						if (ac.getAward() == null) {
							ac.setAward(new Milestone(message));
						} else {
							ac.getAward().setName(message);
						}
						if (ac.getPreviousStages().peek() == AwardCreationStage.CHOOSE_OPTION) {
							ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
						} else {
							ac.goToStage(AwardCreationStage.SET_VOTES);
						}
						return;
					}
				break;
			case SET_PRIORITY:
				Milestone ms = (Milestone) ac.getAward();
				if (message.equalsIgnoreCase("clear")) {
					ms.setPriority(10);
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				} else {
					try {
						int priority = Integer.parseInt(message);
						if (priority < 1) {
							player.sendMessage(ChatColor.RED
							        + "Priority can't be less than 1!");
							return;
						}
						ms.setPriority(priority);
						ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
					} catch (Exception e) {
						player.sendMessage(ChatColor.RED
						        + "That is not a recognized number. Please try again.");
					}
				}
				break;
			case SET_RECURRING:
				Milestone ms1 = (Milestone) ac.getAward();
				if (message.equalsIgnoreCase("yes")) {
					ms1.setRecurring(true);
				} else
					if (message.equalsIgnoreCase("no")) {
						ms1.setRecurring(false);
					} else {
						player.sendMessage(ChatColor.RED
						        + "That is not a recognized input. Please try again.");
						return;
					}
				if (ac.getPreviousStages().peek() == AwardCreationStage.CHOOSE_OPTION) {
					ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
				} else {
					ac.goToStage(AwardCreationStage.CHOOSE_PRIZE);
				}
				break;
			case SET_VOTES:
				try {
					int votes = Integer.parseInt(message);
					if (votes < 1) {
						player.sendMessage(ChatColor.RED
						        + "The vote count cannot be less than 1!");
					}
					Milestone ms2 = (Milestone) ac.getAward();
					ms2.setVotes(votes);
					if (ac.getPreviousStages().peek() == AwardCreationStage.CHOOSE_OPTION) {
						ac.goToStage(AwardCreationStage.CHOOSE_OPTION);
					} else {
						ac.goToStage(AwardCreationStage.SET_RECURRING);
					}
				} catch (Exception e) {
					player.sendMessage(ChatColor.RED
					        + "That is not a recognized number. Please try again.");
				}
				break;
			case CHOOSE_XP_OPTION:
				break;
			default:
				break;
			}
			// player.sendMessage(ChatColor.RED +
			// "Unrecognized input! Type \"help\" or \"cancel\" to leave the Award Creator.");
		}
	}
}
