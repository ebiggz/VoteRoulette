package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

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

	public boolean playerHasRewards(Player player) {
		Reward[] qualRewards = getQualifiedRewards(player);
		if(qualRewards.length == 0) {
			System.out.println("no qualified rewards");
			return false;
		}
		for(int i = 0; i < qualRewards.length; i++) {
			System.out.println(qualRewards[i].getName());
		}
		return true;
	}

	public Reward[] getQualifiedRewards(Player player) {
		ArrayList<Reward> qualifiedRewards = new ArrayList<Reward>();
		Reward[] rewardsArray;
		if(VoteRoulette.hasPermPlugin()) {
			for(int i = 0; i < rewards.size(); i++) {
				System.out.println("Checking for: " + rewards.get(i).getName());
				if(rewards.get(i).hasPermissionGroups()) {
					String[] permGroups = rewards.get(i).getPermGroups();
					for(int j = 0; j < permGroups.length; j++) {
						if(VoteRoulette.permission.playerInGroup(player, permGroups[j])) {
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

	public void giveDefaultReward(Player player) {
		if(defaultReward == null) {
			log.warning("[VoteRoulette] Player earned the default reward but there's no default reward set to give!");
			return;
		}
	}

	public void giveRandomReward(Player player) {
		Reward[] qualRewards = this.getQualifiedRewards(player);
		Random rand = new Random();
		int randNum = rand.nextInt(qualRewards.length);
		administerRewardContents(qualRewards[randNum], player);
	}

	public void administerRewardContents(Reward reward, Player player) {
		ConfigAccessor playerCfg = new ConfigAccessor("players.yml");
		String playername = player.getName();
		if(reward.hasCurrency()) {
			VoteRoulette.economy.depositPlayer(playername, reward.getCurrency());
		}
		if(reward.hasItems()) {
			if(reward.getRequiredSlots() <= Utils.getPlayerOpenInvSlots(player)) {
				Inventory inv = player.getInventory();
				ItemStack[] items = reward.getItems();
				for(int i = 0; i < items.length; i++) {
					inv.addItem(items[i]);
				}
			} else {
				List<ItemStack> items = Arrays.asList(reward.getItems());
				playerCfg.getConfig().addDefault(playername + ".unclaimedRewards." + reward.getName(), items);
				playerCfg.saveConfig();
				player.sendMessage(ChatColor.RED + "You not have the required space for the items in this reward. Please type \"/vr claim\" once you have cleared room in your inventory.");
			}
		}
		if(reward.hasXpLevels()) {
			player.giveExpLevels(reward.getXpLevels());
		}
		player.sendMessage(getPlayerRewardMessage(reward));
	}

	//milestone methods

	void addMilestone(Milestone milestone) {
		milestones.add(milestone);
	}

	void clearMilestones() {
		milestones.clear();
	}

	public void giveDefaultMilestone(Player player) {
		System.out.println("Sending milestone");

	}

	public void giveRandomMilestone(Player player) {
		System.out.println("Sending random milestone");
	}


	public boolean playerHasMilestones(Player player) {
		if(getQualifiedMilestones(player).length == 0) {
			return false;
		}
		return true;
	}

	public Milestone[] getQualifiedMilestones(Player player) {
		ArrayList<Milestone> qualifiedMilestones = new ArrayList<Milestone>();
		Milestone[] milestonesArray;
		if(VoteRoulette.hasPermPlugin()) {
			for(int i = 0; i < milestones.size(); i++) {
				if(milestones.get(i).hasPermissionGroups()) {
					String[] permGroups = milestones.get(i).getPermGroups();
					for(int j = 0; j < permGroups.length; j++) {
						if(VoteRoulette.permission.playerInGroup(player, permGroups[j])) {
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

	public boolean playerReachedMilestone(Player player) {

		String playername = player.getName();
		ConfigAccessor playerCfg = new ConfigAccessor("players.yml");

		if(!playerHasMilestones(player)) return false;
		if(!playerCfg.getConfig().contains(playername)) return false;

		Milestone[] playerMS = getQualifiedMilestones(player);
		int playerVotes = playerCfg.getConfig().getInt(playername + ".lifetimeVotes");

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

	private String getPlayerRewardMessage(Reward reward) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.GREEN + "You've just received the reward \"" + ChatColor.BLUE + reward.getName() + ChatColor.GREEN + "\" which gave you ");
		if(reward.hasCurrency()) {
			sb.append(ChatColor.AQUA + "$" + reward.getCurrency());
		}
		if(reward.hasXpLevels()) {
			if(reward.hasCurrency()) {
				sb.append( ChatColor.GREEN + ", ");
			}
			sb.append(ChatColor.AQUA + "" + reward.getXpLevels() + " XP levels");
		}
		if(reward.hasItems()) {
			if(reward.hasCurrency() || reward.hasXpLevels()) {
				sb.append( ChatColor.GREEN + ", ");
			}
			sb.append(ChatColor.DARK_AQUA + Utils.getItemListString(reward.getItems()));
		}
		sb.append(".");
		return sb.toString();
	}
}
