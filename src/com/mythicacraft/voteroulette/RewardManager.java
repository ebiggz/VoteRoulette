package com.mythicacraft.voteroulette;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mythicacraft.voteroulette.utils.ConfigAccessor;
import com.mythicacraft.voteroulette.utils.Utils;

public class RewardManager {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private ArrayList<Reward> rewards = new ArrayList<Reward>();
	private ArrayList<Milestone> milestones = new ArrayList<Milestone>();
	private Reward defaultReward = null;
	private PlayerManager pm = VoteRoulette.getPlayerManager();

	private static VoteRoulette plugin;

	RewardManager(VoteRoulette instance) {
		plugin = instance;
	}

	//reward methods
	void addReward(Reward reward) {
		rewards.add(reward);
	}

	void clearRewards() {
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

	public boolean playerHasRewards(String playerName) {
		Reward[] qualRewards = getQualifiedRewards(playerName);
		if(qualRewards.length == 0) {
			return false;
		}
		return true;
	}

	public Reward[] getQualifiedRewards(String playerName) {
		ArrayList<Reward> qualifiedRewards = new ArrayList<Reward>();
		Reward[] rewardsArray;
		if(VoteRoulette.hasPermPlugin()) {
			for(int i = 0; i < rewards.size(); i++) {
				if(rewards.get(i).hasPermissionGroups()) {
					String[] permGroups = rewards.get(i).getPermGroups();
					for(int j = 0; j < permGroups.length; j++) {
						if(VoteRoulette.permission.playerInGroup("",playerName, permGroups[j])) {
							qualifiedRewards.add(rewards.get(i));
							break;
						}
					}
					continue;
				}
				qualifiedRewards.add(rewards.get(i));
			}
			rewardsArray = new Reward[qualifiedRewards.size()];
			qualifiedRewards.toArray(rewardsArray);
			return rewardsArray;
		}
		rewardsArray = new Reward[rewards.size()];
		rewards.toArray(rewardsArray);
		return rewardsArray;
	}

	public void giveDefaultReward(String playerName) {
		if(defaultReward == null) {
			log.warning("[VoteRoulette] Player earned the default reward but there's no default reward set to give!");
			return;
		}
		Player player = Bukkit.getPlayerExact(playerName);
		if(player != null) {
			administerRewardContents(defaultReward, player);
		} else {
			//save as unclaimed
			pm.saveUnclaimedReward(playerName, defaultReward.getName());
		}
	}

	public void giveRandomReward(String playerName) {
		Reward[] qualRewards = this.getQualifiedRewards(playerName);
		Random rand = new Random();
		Reward reward = qualRewards[rand.nextInt(qualRewards.length)];
		Player player = Bukkit.getPlayerExact(playerName);
		if(player != null) {
			administerRewardContents(reward, player);
		} else {
			//save as unclaimed
			pm.saveUnclaimedReward(playerName, reward.getName());
		}
	}

	public void administerRewardContents(Reward reward, Player player) {
		String playerName = player.getName();
		if(reward.hasItems()) {
			if(reward.getRequiredSlots() <= Utils.getPlayerOpenInvSlots(player)) {
				Inventory inv = player.getInventory();
				ItemStack[] items = reward.getItems();
				for(int i = 0; i < items.length; i++) {
					inv.addItem(items[i]);
				}
			} else {
				pm.saveUnclaimedReward(player.getName(), reward.getName());
				player.sendMessage(ChatColor.RED + "You don't have the required space in your inventory for this reward (" + reward.getRequiredSlots() + " slots). Please type \"/vr claim\" once you have cleared enough room in your inventory.");
				return;
			}
		}
		if(reward.hasCurrency()) {
			VoteRoulette.economy.depositPlayer(playerName, reward.getCurrency());
		}
		if(reward.hasXpLevels()) {
			player.giveExpLevels(reward.getXpLevels());
		}
		if(reward.hasCommands()) {
			for(String command : reward.getCommands()) {
				if(command.startsWith("/")) {
					command = command.replaceFirst("/", "");
				}
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player.getName()));
			}
		}
		if(plugin.MESSAGE_PLAYER) {
			if(reward.hasMessage()) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', reward.getMessage().replace("%player%", player.getName())));
			} else {
				player.sendMessage(getRewardMessage(player, reward));
			}
		}
		if(plugin.LOG_TO_CONSOLE) {
			System.out.println("[VoteRoulette] " + player.getName() + " just earned the reward: " + reward.getName());
		}
	}

	/*
	 * 
	 * milestone methods
	 * 
	 */

	void addMilestone(Milestone milestone) {
		milestones.add(milestone);
	}

	void clearMilestones() {
		milestones.clear();
	}

	public void giveHighestPriorityMilestone(String playerName) {
		List<Milestone> reachedMils = getReachedMilestones(playerName);
		Player player = Bukkit.getPlayerExact(playerName);
		if(player != null) {
			administerMilestoneContents(reachedMils.get(0), player);
		} else {
			pm.saveUnclaimedMilestone(playerName, reachedMils.get(0).getName());
		}
	}

	public void giveRandomMilestone(String playerName) {
		List<Milestone> reachedMils = getReachedMilestones(playerName);
		Random rand = new Random();
		Milestone milestone = reachedMils.get(rand.nextInt(reachedMils.size()));
		Player player = Bukkit.getPlayerExact(playerName);
		if(player != null) {
			administerMilestoneContents(milestone, player);
		} else {
			pm.saveUnclaimedMilestone(playerName, milestone.getName());
		}
	}

	public void administerMilestoneContents(Milestone milestone, Player player) {
		String playerName = player.getName();
		if(milestone.hasItems()) {
			if(milestone.getRequiredSlots() <= Utils.getPlayerOpenInvSlots(player)) {
				Inventory inv = player.getInventory();
				ItemStack[] items = milestone.getItems();
				for(int i = 0; i < items.length; i++) {
					inv.addItem(items[i]);
				}
			} else {
				pm.saveUnclaimedMilestone(player.getName(), milestone.getName());
				player.sendMessage(ChatColor.RED + "You don't have the required space in your inventory for this milestone (" + milestone.getRequiredSlots() + " slots). Please type \"/vr claim\" once you have cleared enough room in your inventory.");
				return;
			}
		}
		if(milestone.hasCurrency()) {
			VoteRoulette.economy.depositPlayer(playerName, milestone.getCurrency());
		}
		if(milestone.hasXpLevels()) {
			player.giveExpLevels(milestone.getXpLevels());
		}
		if(milestone.hasCommands()) {
			for(String command : milestone.getCommands()) {
				if(command.startsWith("/")) {
					command = command.replaceFirst("/", "");
				}
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("%player%", player.getName()));
			}
		}
		if(plugin.MESSAGE_PLAYER) {
			if(milestone.hasMessage()) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', milestone.getMessage().replace("%player%", player.getName())));
			} else {
				player.sendMessage(getMilestoneMessage(player, milestone));
			}
		}
		if(plugin.LOG_TO_CONSOLE) {
			System.out.println("[VoteRoulette] " + player.getName() + " just earned the milestone: " + milestone.getName());
		}
	}


	public boolean playerHasMilestones(String playerName) {
		if(getQualifiedMilestones(playerName).length == 0) {
			return false;
		}
		return true;
	}

	public Milestone[] getQualifiedMilestones(String playerName) {
		ArrayList<Milestone> qualifiedMilestones = new ArrayList<Milestone>();
		Milestone[] milestonesArray;
		if(VoteRoulette.hasPermPlugin()) {
			for(int i = 0; i < milestones.size(); i++) {
				if(milestones.get(i).hasPermissionGroups()) {
					String[] permGroups = milestones.get(i).getPermGroups();
					for(int j = 0; j < permGroups.length; j++) {
						if(VoteRoulette.permission.playerInGroup("",playerName, permGroups[j])) {
							qualifiedMilestones.add(milestones.get(i));
							break;
						}
					}
					continue;
				}
				qualifiedMilestones.add(milestones.get(i));
			}
			milestonesArray = new Milestone[qualifiedMilestones.size()];
			qualifiedMilestones.toArray(milestonesArray);
			return milestonesArray;
		}
		milestonesArray = new Milestone[milestones.size()];
		milestones.toArray(milestonesArray);
		return milestonesArray;
	}

	public boolean playerReachedMilestone(String playerName) {

		ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");

		if(!playerHasMilestones(playerName)) return false;
		if(!playerCfg.getConfig().contains(playerName)) return false;

		Milestone[] playerMS = getQualifiedMilestones(playerName);
		int playerVotes = playerCfg.getConfig().getInt(playerName + ".lifetimeVotes");

		for(int i = 0; i < playerMS.length; i++) {
			int milVotes = playerMS[i].getVotes();
			if(playerMS[i].isRecurring()) {
				if (playerVotes % milVotes == 0) {
					return true;
				}
			}
			if(milVotes == playerVotes) {
				return true;
			}
		}
		return false;
	}

	public List<Milestone> getReachedMilestones(String playerName) {

		ConfigAccessor playerCfg = new ConfigAccessor("data" + File.separator + "players.yml");

		List<Milestone> reachedMils = new ArrayList<Milestone>();
		Milestone[] playerMS = getQualifiedMilestones(playerName);
		int playerVotes = playerCfg.getConfig().getInt(playerName + ".lifetimeVotes");

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

		return reachedMils;
	}

	private String getRewardMessage(Player player, Reward reward) {
		String rewardMessage = plugin.PLAYER_VOTE_MESSAGE;
		rewardMessage = rewardMessage.replace("%name%", reward.getName());
		rewardMessage = rewardMessage.replace("%player%", player.getName());
		rewardMessage = rewardMessage.replace("%server%", Bukkit.getServerName());
		rewardMessage = rewardMessage.replace("%type%", "reward");
		rewardMessage = rewardMessage.replace("%prizes%", getRewardPrizes(reward));
		return rewardMessage;
	}

	private String getRewardPrizes(Reward reward) {
		StringBuilder sb = new StringBuilder();
		if(reward.hasCurrency()) {
			sb.append("$" + reward.getCurrency());
		}
		if(reward.hasXpLevels()) {
			if(reward.hasCurrency()) {
				sb.append(", ");
			}
			sb.append(reward.getXpLevels() + " xp levels");
		}
		if(reward.hasItems()) {
			if(reward.hasCurrency() || reward.hasXpLevels()) {
				sb.append(", ");
			}
			sb.append(Utils.getItemListSentance(reward.getItems()));
		}
		return sb.toString();
	}

	private String getMilestoneMessage(Player player, Milestone milestone) {
		String milestoneMessage = plugin.PLAYER_VOTE_MESSAGE;
		milestoneMessage = milestoneMessage.replace("%name%", milestone.getName());
		milestoneMessage = milestoneMessage.replace("%player%", player.getName());
		milestoneMessage = milestoneMessage.replace("%server%", Bukkit.getServerName());
		milestoneMessage = milestoneMessage.replace("%type%", "milestone");
		milestoneMessage = milestoneMessage.replace("%prizes%", getMilstonePrizes(milestone));
		return milestoneMessage;
	}

	private String getMilstonePrizes(Milestone milestone) {
		StringBuilder sb = new StringBuilder();
		if(milestone.hasCurrency()) {
			sb.append("$" + milestone.getCurrency());
		}
		if(milestone.hasXpLevels()) {
			if(milestone.hasCurrency()) {
				sb.append(", ");
			}
			sb.append(milestone.getXpLevels() + " xp levels");
		}
		if(milestone.hasItems()) {
			if(milestone.hasCurrency() || milestone.hasXpLevels()) {
				sb.append(", ");
			}
			sb.append(Utils.getItemListSentance(milestone.getItems()));
		}
		return sb.toString();
	}
}
