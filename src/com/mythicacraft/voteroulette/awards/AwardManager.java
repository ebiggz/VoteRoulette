package com.mythicacraft.voteroulette.awards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.api.PlayerEarnedAwardEvent;
import com.mythicacraft.voteroulette.api.PlayerReceivedAwardEvent;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.Reward.VoteStreakModifier;
import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;

@SuppressWarnings("deprecation")
public class AwardManager {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private ArrayList<Reward> rewards = new ArrayList<Reward>();
	private ArrayList<Milestone> milestones = new ArrayList<Milestone>();
	private List<DelayedCommand> delayedCommands = new ArrayList<DelayedCommand>();
	private Reward defaultReward = null;
	private VoterManager vm;

	private static VoteRoulette plugin;

	public AwardManager(VoteRoulette instance) {
		plugin = instance;
		vm = VoteRoulette.getVoterManager();
	}

	/**
	 * Award Methods
	 **/

	public void administerAllUnclaimedAwards(Voter voter, AwardType awardType) {
		Player player = voter.getPlayer();
		if(player == null || !player.isOnline()) return;

		if(Utils.worldIsBlacklisted(player.getWorld().getName())) {
			Utils.debugMessage(player.getName() + " is in blacklisted world. Saving as unclaimed.");
			player.sendMessage(plugin.BLACKLISTED_WORLD_NOTIFICATION.replace("%type%", awardType.toString().toLowerCase()));
			return;
		}

		List<Award> awards;
		if(awardType == AwardType.REWARD) {
			awards = Utils.convertRewardListToAward(voter.getUnclaimedRewards());
		} else {
			awards = Utils.convertMilestoneListToAward(voter.getUnclaimedMilestones());
		}

		List<Award> worldFilter = new ArrayList<Award>();
		for(Award award : awards) {
			if(!award.hasWorlds()) continue;
			if(!award.getWorlds().contains(player.getWorld().getName())) {
				worldFilter.add(award);
			}
		}
		for(Award award : worldFilter) {
			awards.remove(award);
		}

		List<Award> successful = new ArrayList<Award> ();
		for(Award award : awards) {
			voter.removeUnclaimedAward(award);
			if(VoteRoulette.getAwardManager().administerAwardContents(award, voter, true)) successful.add(award);
		}
		if(successful.size() > 0) {
			player.sendMessage(Utils.getSummarizedAwardsMessage(successful, voter));
		}
		if(worldFilter.size() > 0) {
			List<String> worldNames = new ArrayList<String>();
			StringBuilder sb = new StringBuilder();
			for(Award award : worldFilter) {
				for(String world : award.getWorlds()) {
					if(worldNames.contains(world)) continue;
					worldNames.add(world);
				}
			}
			for(String world : worldNames) {
				sb.append(world + ", ");
			}
			if(sb.length() > 2) {
				sb.delete(sb.length()-2, sb.length()-1);
			}

			player.sendMessage(plugin.WRONG_AWARD_WORLD_AUTOCLAIM_NOTIFICATION
					.replace("%type%", awardType == AwardType.REWARD ? plugin.REWARDS_PURAL_DEF.toLowerCase() : plugin.MILESTONE_PURAL_DEF.toLowerCase())
					.replace("%worlds%", sb.toString()));
		}
	}
	public boolean administerAwardContents(Award award, Voter voter) {
		return administerAwardContents(award, voter, false);
	}

	public boolean administerAwardContents(Award award, Voter voter, boolean muteMessage) {

		Utils.debugMessage("Starting award prize administering ...");

		if(!voter.isReal()) {
			plugin.getLogger().warning("VoteRoulette could not find a UUID for a player! Rewards will not be given and stats will not update. Maybe there is a connectivity issue with Mojang's UUID server or the player isn't using a legitamte copy of Minecraft?");
			return false;
		}

		String playerName = voter.getPlayerName();

		Player player = Bukkit.getPlayerExact(playerName);

		if(player == null) {
			Utils.debugMessage(playerName + " isnt online. Saving as unclaimed.");
			voter.saveUnclaimedAward(award);
			return false;
		}

		String worldName = player.getWorld().getName();

		if(Utils.worldIsBlacklisted(worldName)) {
			Utils.debugMessage(playerName + " is in blacklisted world. Saving as unclaimed.");
			voter.saveUnclaimedAward(award);
			player.sendMessage(plugin.BLACKLISTED_WORLD_NOTIFICATION.replace("%type%", "award"));
			return false;
		}
		if(award.hasWorlds()) {
			if(!award.getWorlds().contains(worldName)) {
				Utils.debugMessage(playerName + " isn't in specific award world. Saving as unclaimed.");
				voter.saveUnclaimedAward(award);
				player.sendMessage(plugin.WRONG_AWARD_WORLD_NOTIFICATION.replace("%type%", "award").replace("%name%", award.getName()).replace("%worlds%", Utils.worldsString(award.getWorlds())));
				return false;
			}
		}
		if(award.hasItems()) {
			Utils.debugMessage("Award contains items");
			ItemStack[] items = Utils.updateLoreAndCustomNames(player.getName(), award.getItems(voter));
			if(award.getRequiredSlots(voter) <= Utils.getPlayerOpenInvSlots(player)) {
				Inventory inv = player.getInventory();
				for(int i = 0; i < items.length; i++) {
					inv.addItem(items[i]);
				}
			} else {
				Utils.debugMessage(playerName + " doesnt have space in inventory. Saving as unclaimed.");
				if(VoteRoulette.DISABLE_UNCLAIMED || VoteRoulette.DISABLE_INVENTORY_PROT) {

					BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
					scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {

						private Player player;
						private ItemStack[] items;

						@Override
						public void run() {
							for(int i = 0; i < items.length; i++) {
								player.getWorld().dropItemNaturally(player.getLocation(), items[i]);
							}
						}
						private Runnable init(Player player, ItemStack[] items){
							this.player = player;
							this.items = items;
							return this;
						}
					}.init(player, items), 1L);

				} else {
					voter.saveUnclaimedAward(award);
					player.sendMessage(plugin.INVENTORY_FULL_NOTIFICATION.replace("%type%", "award").replace("%name%", award.getName()).replace("%slots%", Integer.toString(award.getRequiredSlots(voter))));
					return false;
				}
			}
		}
		if(award.hasCurrency()) {
			Utils.debugMessage("Award contains curreny, depositing in bank");
			VoteRoulette.economy.depositPlayer(playerName, award.getCurrency());
		}
		if(award.hasXpLevels()) {
			Utils.debugMessage("Award contains xp, giving it.");
			player.giveExpLevels(award.getXpLevels());
		}
		if(award.hasCommands()) {
			Utils.debugMessage("Award has commands, running them");
			for(String command : award.getCommands()) {
				if(command.startsWith("/")) {
					command = command.replaceFirst("/", "");
				}
				if(command.startsWith("(")) {
					command = command.replaceFirst("\\(", "");
					String[] delayedCommandData = command.split("\\)");
					String delayedCommand = delayedCommandData[1].trim();
					if(delayedCommand.startsWith("/")) {
						delayedCommand = delayedCommand.replaceFirst("/", "");
					}
					int delay = 1;
					boolean runOnLogOff = false;
					boolean runOnShutdown = false;
					if(delayedCommandData[0].contains("/")) {
						String[] delayedCommandOptions = delayedCommandData[0].split("/");
						for(String cmdOption: delayedCommandOptions) {
							if(cmdOption.trim().equalsIgnoreCase("logoff") || cmdOption.trim().equalsIgnoreCase("log off")) {
								runOnLogOff = true;
							}
							else if(cmdOption.trim().equalsIgnoreCase("shutdown")) {
								runOnShutdown = true;
							}
							else {
								try {
									delay = Integer.parseInt(cmdOption.trim());
								} catch (Exception e) {
									log.warning("[VoteRoulette] Error parsing delay for command in award: " + award.getName());
								}
							}
						}
					} else {
						delay = Integer.parseInt(delayedCommandData[0].trim());
					}
					DelayedCommand dc = new DelayedCommand(delayedCommand, delay, playerName, runOnLogOff, runOnShutdown);
					dc.runTaskLater(plugin, delay*20);
					VoteRoulette.delayedCommands.add(dc);
				} else {
					BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
					scheduler.runTask(plugin, new Runnable() {

						private String command;
						private String playerName;

						@Override
						public void run() {
							Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", playerName));
						}

						private Runnable init(String command, String playerName){
							this.command = command;
							this.playerName = playerName;
							return this;
						}
					}.init(command, player.getName()));
				}
			}
		}
		if(plugin.MESSAGE_PLAYER) {
			if(!muteMessage) {
				if(award.hasMessage()) {
					player.sendMessage(Utils.transcribeColorCodes(award.getMessage().replace("%player%", player.getName())));
				} else {
					player.sendMessage(Utils.getAwardMessage(plugin.PLAYER_VOTE_MESSAGE, award, voter));
				}
			}
		}
		if(award.hasReroll()) {
			Utils.debugMessage("Award has reroll settings, rolling");
			player.sendMessage(plugin.REROLL_NOTIFICATION.replace("%type%", "award").replace("%name%", award.getName()));
			if(!rerollReward(award.getReroll(), player, award, voter)) {
				log.warning("[VoteRoulette] There was an error when doing the reroll settings in award " + award.getName() + " for the player " + player.getName() + ", was the reroll award spelled correctly?");
			}
		}

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.runTask(plugin, new Runnable() {
			private Player player;
			private Award award;
			@Override
			public void run() {
				Bukkit.getServer().getPluginManager().callEvent(new PlayerReceivedAwardEvent(player, award));
			}
			private Runnable init(Player player, Award award){
				this.player = player;
				this.award = award;
				return this;
			}
		}.init(player, award));
		return true;
	}

	public Award getAwardByName(String awardName, AwardType awardType) {
		if(awardType == AwardType.REWARD) {
			for(Reward reward: rewards) {
				if(reward.getName().equals(awardName)) return reward;
			}
			return null;
		} else {
			for(Milestone milestone: milestones) {
				if(milestone.getName().equals(awardName)) return milestone;
			}
			return null;
		}
	}

	public boolean awardsContainChance(Award[] awards) {
		for(Award award : awards) {
			if(award.hasChance()) return true;
		}
		return false;
	}

	/**
	 * Reward Specific
	 **/

	public void addAward(Award award) {
		if(award.getAwardType() == AwardType.REWARD) {
			rewards.add((Reward) award);
		} else {
			milestones.add((Milestone) award);
		}
	}

	public void addReward(Reward reward) {
		rewards.add(reward);
	}

	public void removeAward(Award award) {
		if(award.getAwardType() == AwardType.REWARD) {
			rewards.remove((Reward) award);
		} else {
			milestones.remove((Milestone) award);
		}
	}

	public ArrayList<Reward> getRewards() {
		return rewards;
	}

	public void clearRewards() {
		rewards.clear();
	}

	public boolean hasDefaultReward() {
		if(defaultReward == null) return false;
		return true;
	}

	public Reward getDefaultReward() {
		return defaultReward;
	}

	public void setDefaultReward(Reward defaultReward) {
		this.defaultReward = defaultReward;
	}


	public Reward[] getQualifiedRewards(Voter voter, boolean skipFilters) {

		String playerName = voter.getPlayerName();

		ArrayList<Reward> qualifiedRewards = new ArrayList<Reward>();

		//iterate through all rewards
		for(Reward reward: rewards) {

			//check if reward has specific players set
			if(reward.hasPlayers()) {
				if(reward.containsPlayer(playerName)) {
					qualifiedRewards.add(reward);
					continue;
				}
				if(VoteRoulette.hasPermPlugin()) {
					if(reward.hasPermissionGroups()) {
						if(plugin.ONLY_PRIMARY_GROUP) {
							String primaryGroup = VoteRoulette.permission.getPrimaryGroup("", playerName);
							if(primaryGroup != null) {
								if(reward.containsPermGroup(primaryGroup)) {
									qualifiedRewards.add(reward);
								}
							} else {
								System.out.println("[VoteRoulette] Warning! Could not get the primary group for player: " + playerName);
							}
						} else {
							String[] groups = reward.getPermGroups();
							for(String group: groups) {
								if(VoteRoulette.permission.playerInGroup("",playerName, group)) {
									qualifiedRewards.add(reward);
									break;
								}
							}
						}
					}
				}
			}

			//check if reward has specific perm groups set
			else if(VoteRoulette.hasPermPlugin()) {
				if(reward.hasPermissionGroups()) {
					if(plugin.ONLY_PRIMARY_GROUP) {
						String primaryGroup = VoteRoulette.permission.getPrimaryGroup("", playerName);
						if(primaryGroup != null) {
							if(reward.containsPermGroup(primaryGroup)) {
								qualifiedRewards.add(reward);
							}
						} else {
							System.out.println("[VoteRoulette] Warning! Could not get the primary group for player: " + playerName);
						}
					} else {
						String[] groups = reward.getPermGroups();
						for(String group: groups) {
							if(VoteRoulette.permission.playerInGroup("",playerName, group)) {
								qualifiedRewards.add(reward);
								break;
							}
						}
					}
				} else {
					qualifiedRewards.add(reward);
				}
			} else {
				qualifiedRewards.add(reward);
			}
		}

		Reward[] rewardsArray;
		rewardsArray = new Reward[qualifiedRewards.size()];
		qualifiedRewards.toArray(rewardsArray);

		//filter out rewards not for the world player is standing in
		if(plugin.CONSIDER_REWARDS_FOR_CURRENT_WORLD && !skipFilters) {
			OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
			ArrayList<Reward> worldFilteredRewards = new ArrayList<Reward>();
			if(op.isOnline()) {
				String worldName = op.getPlayer().getWorld().getName();
				for(Reward reward : rewardsArray) {
					if(reward.hasWorlds()) {
						if(reward.getWorlds().contains(worldName)) {
							worldFilteredRewards.add(reward);
						}
					} else {
						worldFilteredRewards.add(reward);
					}
				}
				if(!worldFilteredRewards.isEmpty()) {
					rewardsArray = new Reward[worldFilteredRewards.size()];
					worldFilteredRewards.toArray(rewardsArray);
				}
			}
		}

		// filter out rewards that have votestreak settings that the player doesn't meet
		if(!skipFilters) {
			int playerVoteStreak = voter.getCurrentVoteStreak();
			ArrayList<Reward> qualifiedVoteStreaks = new ArrayList<Reward>();
			ArrayList<Reward> nonVoteStreakRewards = new ArrayList<Reward>();
			for(Reward reward : rewardsArray) {
				if(!reward.hasVoteStreak()) {
					nonVoteStreakRewards.add(reward);
					continue;
				}
				if(reward.hasVoteStreakModifier()) {
					VoteStreakModifier vsm = reward.getVoteStreakModifier();
					if(vsm == VoteStreakModifier.OR_LESS) {
						if(playerVoteStreak <= reward.getVoteStreak()) {
							qualifiedVoteStreaks.add(reward);
						}
					}
					else if(vsm == VoteStreakModifier.OR_MORE) {
						if(playerVoteStreak >= reward.getVoteStreak()) {
							qualifiedVoteStreaks.add(reward);
						}
					}
				} else {
					if(playerVoteStreak == reward.getVoteStreak()) {
						qualifiedVoteStreaks.add(reward);
					}
				}
			}
			if(!qualifiedVoteStreaks.isEmpty()) {
				if(plugin.PRIORITIZE_VOTESTREAKS) {
					rewardsArray = new Reward[qualifiedVoteStreaks.size()];
					qualifiedVoteStreaks.toArray(rewardsArray);
				} else {
					nonVoteStreakRewards.addAll(qualifiedVoteStreaks);
					rewardsArray = new Reward[nonVoteStreakRewards.size()];
					nonVoteStreakRewards.toArray(rewardsArray);
				}
			} else {
				rewardsArray = new Reward[nonVoteStreakRewards.size()];
				nonVoteStreakRewards.toArray(rewardsArray);
			}
		}
		return rewardsArray;
	}

	private boolean rerollReward(String reroll, Player player, Award origAward, Voter voter) {
		Utils.debugMessage("Entered reroll processing...");
		int chanceMin = 0;
		int chanceMax = 100;
		boolean useCustomChance = false;
		String name;
		if(reroll.contains("(") && reroll.contains(")") && reroll.endsWith(")")) {
			String[] rerollData = reroll.split("\\(");
			name = rerollData[0].trim();
			rerollData[1] = rerollData[1].replace("%", "").trim().replace(")", "");
			if(rerollData[1].contains("/")) {
				String[] chanceData = rerollData[1].split("/");
				try {
					chanceMin = Integer.parseInt(chanceData[0].trim());
					chanceMax = Integer.parseInt(chanceData[1].trim());
				} catch (Exception e) {
					return false;
				}
			} else {
				try {
					chanceMin = Integer.parseInt(rerollData[1].trim());
				} catch (Exception e) {
					return false;
				}
			}
			useCustomChance = true;
		} else {
			name = reroll;
		}

		Award award;
		if(name.equals("ANY")) {
			Utils.debugMessage("Has ANY modifier");
			Random rand = new Random();
			if(origAward.getAwardType() == AwardType.REWARD) {
				Utils.debugMessage("Original award is a reward, ignoring original");
				ArrayList<Reward> tempRewards = new ArrayList<Reward>();
				Reward origReward = (Reward) origAward;
				for(Reward reward : rewards) {
					if(reward == origReward) continue;
					tempRewards.add(reward);
				}
				Utils.debugMessage("there are " + tempRewards.size() + " eligible rewards, choosing random");
				award = tempRewards.get(rand.nextInt(tempRewards.size()));
			} else {
				award = rewards.get(rand.nextInt(rewards.size()));
				Utils.debugMessage("there are " + rewards.size() + " eligible rewards, choosing a random one...");
			}
		} else {
			award = getAwardByName(name, AwardType.REWARD);
		}

		if(award == null) return false;

		Utils.debugMessage("Chose: " + award.getName());

		if(!useCustomChance) {
			if(award.hasChance()) {
				Utils.debugMessage("\"" + award.getName() + "\" has chance (" + award.getChanceMin() + "/" + award.getChanceMax() + "), doing a chance check...");
				chanceMin = award.getChanceMin();
				chanceMax = award.getChanceMax();
			} else {
				chanceMin = 100;
			}
		}

		int random = 1 + (int)(Math.random() * ((chanceMax - 1) + 1));

		if(random <= chanceMin) {
			Utils.debugMessage("Reroll success (" + random + "), administering to player...");
			PlayerEarnedAwardEvent event = new PlayerEarnedAwardEvent(player.getName(), award);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				if(event.getPlayerName() == voter.getPlayerName()) {
					administerAwardContents(event.getAward(), voter);
				} else {
					administerAwardContents(event.getAward(), vm.getVoter(event.getPlayerName()));
				}
			}
		} else {
			Utils.debugMessage("Reroll failed (" + random + ").");
			player.sendMessage(plugin.REROLL_FAILED_NOTIFICATION);
		}
		Utils.debugMessage("Exited reroll processing");
		return true;
	}

	/**
	 * Milestone Specific
	 **/

	public ArrayList<Milestone> getMilestones() {
		return milestones;
	}

	public void addMilestone(Milestone milestone) {
		milestones.add(milestone);
	}

	public void clearMilestones() {
		milestones.clear();
	}

	public Milestone[] getQualifiedMilestones(String playerName) {

		ArrayList<Milestone> qualifiedMilestones = new ArrayList<Milestone>();

		//iterate through all rewards
		for(Milestone milestone: milestones) {

			//check if reward has specific players set
			if(milestone.hasPlayers()) {
				if(milestone.containsPlayer(playerName)) {
					qualifiedMilestones.add(milestone);
					continue;
				}
				if(VoteRoulette.hasPermPlugin()) {
					if(milestone.hasPermissionGroups()) {
						if(plugin.ONLY_PRIMARY_GROUP) {
							String primaryGroup = VoteRoulette.permission.getPrimaryGroup("", playerName);
							if(primaryGroup != null) {
								if(milestone.containsPermGroup(primaryGroup)) {
									qualifiedMilestones.add(milestone);
								}
							} else {
								System.out.println("[VoteRoulette] Warning! Could not get the primary group for player: " + playerName);
							}
						} else {
							String[] groups = milestone.getPermGroups();
							for(String group: groups) {
								if(VoteRoulette.permission.playerInGroup("",playerName, group)) {
									qualifiedMilestones.add(milestone);
									break;
								}
							}
						}
					}
				}
			}

			//check if reward has specific perm groups set
			else if(VoteRoulette.hasPermPlugin()) {
				if(milestone.hasPermissionGroups()) {
					if(plugin.ONLY_PRIMARY_GROUP) {
						String primaryGroup = VoteRoulette.permission.getPrimaryGroup("", playerName);
						if(primaryGroup != null) {
							if(milestone.containsPermGroup(primaryGroup)) {
								qualifiedMilestones.add(milestone);
							}
						} else {
							System.out.println("[VoteRoulette] Warning! Could not get the primary group for player: " + playerName);
						}
					} else {
						String[] groups = milestone.getPermGroups();
						for(String group: groups) {
							if(VoteRoulette.permission.playerInGroup("",playerName, group)) {
								qualifiedMilestones.add(milestone);
								break;
							}
						}
					}
				} else {
					qualifiedMilestones.add(milestone);
				}
			} else {
				qualifiedMilestones.add(milestone);
			}
		}

		Milestone[] milestonesArray;
		milestonesArray = new Milestone[qualifiedMilestones.size()];
		qualifiedMilestones.toArray(milestonesArray);

		//filter out rewards not for the world player is standing in
		if(plugin.CONSIDER_MILESTONES_FOR_CURRENT_WORLD) {
			OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
			ArrayList<Milestone> worldFilteredMilestones = new ArrayList<Milestone>();
			if(op.isOnline()) {
				String worldName = op.getPlayer().getWorld().getName();
				for(Milestone milestone : milestonesArray) {
					if(milestone.hasWorlds()) {
						if(milestone.getWorlds().contains(worldName)) {
							worldFilteredMilestones.add(milestone);
						}
					} else {
						worldFilteredMilestones.add(milestone);
					}
				}
				if(!worldFilteredMilestones.isEmpty()) {
					milestonesArray = new Milestone[worldFilteredMilestones.size()];
					worldFilteredMilestones.toArray(milestonesArray);
				}
			}
		}
		return milestonesArray;
	}


	public Milestone[] getReachedMilestones(Voter voter) {


		String playerName = voter.getPlayerName();

		List<Milestone> reachedMils = new ArrayList<Milestone>();
		Milestone[] playerMS = getQualifiedMilestones(playerName);
		int playerVotes = voter.getLifetimeVotes();

		for(int i = 0; i < playerMS.length; i++) {
			int milVotes = playerMS[i].getVotes();
			if(playerMS[i].isRecurring()) {
				if (playerVotes % milVotes == 0) {
					reachedMils.add(playerMS[i]);
					continue;
				}
			}
			if(milVotes == playerVotes) {
				reachedMils.add(playerMS[i]);
			}
		}

		Collections.sort(reachedMils, new Comparator<Milestone>(){
			public int compare(Milestone m1, Milestone m2) {
				return m1.getPriority() - m2.getPriority();
			}
		});

		Milestone[] milestonesArray = new Milestone[reachedMils.size()];
		reachedMils.toArray(milestonesArray);

		return milestonesArray;
	}

	public void deleteAwardFromFile(Award award) {
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");
		String awardType;
		if(award.getAwardType() == AwardType.REWARD) {
			awardType = "Rewards";
		} else {
			awardType = "Milestones";
		}
		String awardPath = awardType + "." + award.getName();
		awardsData.getConfig().set(awardPath, null);
		awardsData.saveConfig();
	}

	public void saveAwardToFile(Award award) {
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");
		String awardType;
		if(award.getAwardType() == AwardType.REWARD) {
			awardType = "Rewards";
		} else {
			awardType = "Milestones";
		}
		String awardPath = awardType + "." + award.getName() + ".";
		if(award.hasItems()) {
			List<Material> handledItems = new ArrayList<Material>();

			for(ItemPrize item: award.getItemPrizes()) {
				if(handledItems.contains(item.getType())) continue;
				ItemPrize[] multiples = this.getMultiples(item, award.getItemPrizes());
				if(multiples != null) {
					handledItems.add(item.getType());
					int count = 1;
					String itemPath = awardPath + "items." + item.getData().getItemTypeId() + ".multiple.";
					for(ItemPrize i : multiples) {
						this.saveItemToPath(itemPath + count + ".", i);
						count++;
					}
				} else {
					String itemPath = awardPath + "items." + item.getData().getItemTypeId() + ".";
					this.saveItemToPath(itemPath, item);
				}
			}
		}
		if(award.hasXpLevels()) {
			awardsData.getConfig().set(awardPath + "xpLevels", award.getXpLevels());
		}
		if(award.hasCurrency()) {
			awardsData.getConfig().set(awardPath + "currency", award.getCurrency());
		}
		if(award.hasCommands()) {
			awardsData.getConfig().set(awardPath + "commands", award.getCommands());
		}
		if(award.hasChance()) {
			awardsData.getConfig().set(awardPath + "chance", award.getChanceMin() + "/" + award.getChanceMax());
		}
		if(award.hasMessage()) {
			awardsData.getConfig().set(awardPath + "message", award.getMessage());
		}
		if(award.hasDescription()) {
			awardsData.getConfig().set(awardPath + "description", award.getDescription());
		}
		if(award.hasPermissionGroups()) {
			String[] array = award.getPermGroups();
			StringBuilder sb = new StringBuilder();
			for(String entry : array) {
				sb.append(entry);
				sb.append(", ");
			}
			sb.delete(sb.length()-2, sb.length());
			awardsData.getConfig().set(awardPath + "permGroups", sb.toString());
		}
		if(award.hasPlayers()) {
			String[] array = award.getPlayers();
			StringBuilder sb = new StringBuilder();
			for(String entry : array) {
				sb.append(entry);
				sb.append(", ");
			}
			sb.delete(sb.length()-2, sb.length());
			awardsData.getConfig().set(awardPath + "players", sb.toString());
		}
		if(award.hasWorlds()) {
			List<String> array = award.getWorlds();
			StringBuilder sb = new StringBuilder();
			for(String entry : array) {
				sb.append(entry);
				sb.append(", ");
			}
			sb.delete(sb.length()-2, sb.length());
			awardsData.getConfig().set(awardPath + "worlds", sb.toString());
		}
		if(award.hasReroll()) {
			awardsData.getConfig().set(awardPath + "reroll", award.getReroll());
		}
		if(award.getAwardType() == AwardType.REWARD) {
			Reward r = (Reward) award;
			if(r.hasWebsites()) {
				List<String> array = r.getWebsites();
				StringBuilder sb = new StringBuilder();
				for(String entry : array) {
					sb.append(entry);
					sb.append(", ");
				}
				sb.delete(sb.length()-2, sb.length());
				awardsData.getConfig().set(awardPath + "websites", sb.toString());
			}
			if(r.hasVoteStreak()) {
				awardsData.getConfig().set(awardPath + "voteStreak", r.getVoteStreak());
			}
		}
		else if(award.getAwardType() == AwardType.MILESTONE) {
			Milestone ms = (Milestone) award;
			awardsData.getConfig().set(awardPath + "votes", ms.getVotes());
			if(ms.isRecurring()) {
				awardsData.getConfig().set(awardPath + "recurring", true);
			}
			if(ms.getPriority() != 10) {
				awardsData.getConfig().set(awardPath + "priority", ms.getPriority());
			}

		}
		awardsData.saveConfig();
	}

	void saveItemToPath(String itemPath, ItemPrize item) {
		ConfigAccessor awardsData = new ConfigAccessor("awards.yml");
		awardsData.getConfig().set(itemPath + "amount", item.getStringAmount());
		if(item.getData().getData() != 0) {
			awardsData.getConfig().set(itemPath + "dataID", item.getData().getData());
			if(item.getType() == Material.POTION) {
				awardsData.getConfig().set(itemPath + "dataID", item.getDurability());
			}
		}
		if(item.hasItemMeta()) {
			ItemMeta im = item.getItemMeta();
			if(im.hasDisplayName()) {
				awardsData.getConfig().set(itemPath + "name", im.getDisplayName());
			}
			if(im.hasLore()) {
				awardsData.getConfig().set(itemPath + "lore", im.getLore());
			}
			if(im.hasEnchants()) {
				Map<Enchantment, Integer> enchants = im.getEnchants();
				Set<Enchantment> keys = enchants.keySet();
				StringBuilder sb = new StringBuilder();
				for(Enchantment key : keys) {
					int level = enchants.get(key);
					sb.append(Utils.getNameFromEnchant(key) + "(" + level + ")");
					sb.append(", ");
				}
				sb.delete(sb.length()-2, sb.length());
				awardsData.getConfig().set(itemPath + "enchants", sb.toString());
			} else if(im instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta esm = (EnchantmentStorageMeta) im;
				if(esm.hasStoredEnchants()) {
					Map<Enchantment, Integer> enchants = esm.getStoredEnchants();
					Set<Enchantment> keys = enchants.keySet();
					StringBuilder sb = new StringBuilder();
					for(Enchantment key : keys) {
						int level = enchants.get(key);
						sb.append(Utils.getNameFromEnchant(key) + "(" + level + ")");
						sb.append(", ");
					}
					sb.delete(sb.length()-2, sb.length());
					awardsData.getConfig().set(itemPath + "enchants", sb.toString());
				}
			}
			if(im instanceof LeatherArmorMeta) {
				LeatherArmorMeta wim = (LeatherArmorMeta) im;
				String rgb = wim.getColor().getRed() + "," + wim.getColor().getGreen() + "," + wim.getColor().getBlue();
				if(!rgb.equalsIgnoreCase("160,101,64")) {
					awardsData.getConfig().set(itemPath + "armorColor", rgb);
				}
			}
			if(im instanceof SkullMeta) {
				SkullMeta sim = (SkullMeta) im;
				if(sim.hasOwner()) {
					awardsData.getConfig().set(itemPath + "skullOwner", sim.getOwner());
				}
			}
		}
		awardsData.saveConfig();
	}

	ItemPrize[] getMultiples(ItemPrize item, List<ItemPrize> items) {
		ItemPrize[] returnedItems = null;
		List<ItemPrize> matches = new ArrayList<ItemPrize>();
		for(ItemPrize i : items) {
			if(i == item) continue;
			if(i.getType() == item.getType()) {
				matches.add(i);
			}
		}
		if(!matches.isEmpty()) {
			returnedItems = new ItemPrize[matches.size()];
			matches.toArray(returnedItems);
		}
		return returnedItems;
	}

	/*
	 * Delayed Commands
	 */

	public List<DelayedCommand> getDelayedCommands() {
		return delayedCommands;
	}
}
