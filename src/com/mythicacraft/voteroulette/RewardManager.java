package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

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

	public Reward[] getQualifiedRewards(Player player) {
		ArrayList<Reward> qualifiedRewards = new ArrayList<Reward>();
		Reward[] rewardsArray;
		if(VoteRoulette.hasPermPlugin()) {
			System.out.println("server has vault and plugin plugin");
			for(int i = 0; i < rewards.size(); i++) {
				System.out.println("Checking for:" + rewards.get(i).getName());
				if(rewards.get(i).hasPermissionGroups()) {
					String[] permGroups = rewards.get(i).getPermGroups();
					for(int j = 0; j < permGroups.length; j++) {
						if(VoteRoulette.permission.playerInGroup(player, permGroups[j])) {
							qualifiedRewards.add(rewards.get(i));
							break;
						}
						System.out.println("player isnt in group: " + permGroups[j]);
					}
					continue;
				}
				System.out.println(rewards.get(i).getName() + " doesnt have a permgroup section, adding");
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

	public boolean playerHasMilestones(Player player) {
		if(getQualifiedMilestones(player).length == 0) {
			return false;
		}
		return true;
	}

	public void sendReward(Player player) {
		if(defaultReward == null ) {
			log.warning("[VoteRoulette] No default reward set to send!");
			return;
		}
		System.out.println("Sending default reward: " + defaultReward.getName());
	}

	public void sendMilestone(Player player) {
		System.out.println("Sending milestone");

	}

	public void sendRandReward(Player player) {
		Reward[] qualRewards = this.getQualifiedRewards(player);
		Random rand = new Random();
		int randNum = rand.nextInt(qualRewards.length);
		System.out.println("amount of rewards: " + qualRewards.length + ", Looking in index: " + randNum);
		System.out.println("Sending random reward: " + qualRewards[randNum].getName() + " to " + player.getName());
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
				playerCfg.getConfig().addDefault(playername + ".unclaimedRewards", "test");
				playerCfg.getConfig().addDefault(playername + ".unclaimedRewards", "test2");
			}
		}
		if(reward.hasXpLevels()) {
			player.giveExpLevels(reward.getXpLevels());
		}
	}

	public void sendRandMilestone(Player player) {
		System.out.println("Sending random milestone");

	}

	public boolean playerReachedMilestone(Player player) {

		String playername = player.getName();
		ConfigAccessor playerCfg = new ConfigAccessor("players.yml");

		if(playerHasMilestones(player) == false) return false;
		if(playerCfg.getConfig().contains(playername) == false) return false;

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

	public Reward getDefaultReward() {
		return defaultReward;
	}

	public boolean hasDefaultReward() {
		System.out.println("Checking for default reward");
		if(defaultReward == null) return false;
		System.out.println("Theres a reward!");
		return true;
	}

	public void setDefaultReward(Reward defaultReward) {
		this.defaultReward = defaultReward;
	}

	void addReward(Reward reward) {
		rewards.add(reward);
	}

	void clearRewards() {
		rewards.clear();
	}

	void addMilestone(Milestone milestone) {
		milestones.add(milestone);
	}

	void clearMilestones() {
		milestones.clear();
	}
}
