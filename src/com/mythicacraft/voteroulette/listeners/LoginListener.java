package com.mythicacraft.voteroulette.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.VoterManager;
import com.mythicacraft.voteroulette.awards.DelayedCommand;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.utils.Utils;

public class LoginListener implements Listener {

	private static VoteRoulette plugin;

	public LoginListener(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {

		VoterManager vm = VoteRoulette.getVoterManager();
		Player player = event.getPlayer();
		Voter voter = vm.getVoter(player.getName());

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.runTaskLaterAsynchronously(plugin, new Runnable() {

			private Player player;
			private Voter voter;

			@Override
			public void run() {
				if (!VoteRoulette.DISABLE_UNCLAIMED) {
					int unclaimedRewardsCount = voter.getUnclaimedRewardCount();
					int unclaimedMilestonesCount = voter
					        .getUnclaimedMilestoneCount();

					if (unclaimedRewardsCount > 0) {
						if (VoteRoulette.AUTO_CLAIM) {
							List<Reward> unclaimedRewards = voter
							        .getUnclaimedRewards();
							for (Reward reward : unclaimedRewards) {
								voter.removeUnclaimedReward(reward.getName());
								VoteRoulette.getAwardManager()
								        .administerAwardContents(reward,
								                player.getName());
							}
						} else {
							player.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION
							        .replace(
							                "%type%",
							                plugin.REWARDS_PURAL_DEF
							                        .toLowerCase())
							        .replace(
							                "%amount%",
							                Integer.toString(unclaimedRewardsCount))
							        .replace(
							                "%command%",
							                "/"
							                        + plugin.DEFAULT_ALIAS
							                        + " "
							                        + plugin.CLAIM_DEF
							                                .toLowerCase()
							                        + " "
							                        + plugin.REWARDS_PURAL_DEF
							                                .toLowerCase()));
						}
					}

					if (unclaimedMilestonesCount > 0) {
						if (VoteRoulette.AUTO_CLAIM) {
							List<Milestone> unclaimedMilestones = voter
							        .getUnclaimedMilestones();
							for (Milestone milestone : unclaimedMilestones) {
								voter.removeUnclaimedMilestone(milestone
								        .getName());
								VoteRoulette.getAwardManager()
								        .administerAwardContents(milestone,
								                player.getName());
							}
						} else {
							player.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION
							        .replace(
							                "%type%",
							                plugin.MILESTONE_PURAL_DEF
							                        .toLowerCase())
							        .replace(
							                "%amount%",
							                Integer.toString(unclaimedMilestonesCount))
							        .replace(
							                "%command%",
							                "/"
							                        + plugin.DEFAULT_ALIAS
							                        + " "
							                        + plugin.CLAIM_DEF
							                                .toLowerCase()
							                        + " "
							                        + plugin.MILESTONE_PURAL_DEF
							                                .toLowerCase()));
						}
					}
				}

				if (plugin.USE_TWENTYFOUR_REMINDER) {
					if (voter.hasntVotedInADay()) {
						player.sendMessage(plugin.TWENTYFOUR_REMINDER.replace(
						        "%player%", player.getName()));
						if (!VoteRoulette.notifiedPlayers.contains(player)) {
							VoteRoulette.notifiedPlayers.add(player);
						}
					}
				}

				if (player.hasPermission("voteroulette.admin")) {
					if (plugin.hasUpdate()) {
						player.sendMessage(ChatColor.AQUA
						        + "[VoteRoulette] There is a new version available to download! Visit "
						        + ChatColor.YELLOW
						        + "http://dev.bukkit.org/bukkit-plugins/voteroulette/");
					}
				}
			}

			private Runnable init(Player player, Voter voter) {
				this.player = player;
				this.voter = voter;
				return this;
			}
		}.init(player, voter), 20L);

	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		if (VoteRoulette.notifiedPlayers.contains(event.getPlayer())) {
			VoteRoulette.notifiedPlayers.remove(event.getPlayer());
		}
		List<DelayedCommand> playerDelayedCmds = Utils
		        .getPlayerDelayedCmds(event.getPlayer().getName());
		for (DelayedCommand dCmd : playerDelayedCmds) {
			if (dCmd.shouldRunOnLogOff()) {
				dCmd.run();
				dCmd.cancel();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void invClick(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player))
			return;
		Player p = (Player) e.getWhoClicked();
		if (VoteRoulette.lookingAtRewards.containsKey(p)) {
			e.setCancelled(true);
		}
		if (VoteRoulette.lookingAtMilestones.containsKey(p)) {
			e.setCancelled(true);
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void invClose(InventoryCloseEvent e) {
		if (!(e.getPlayer() instanceof Player))
			return;
		if (VoteRoulette.lookingAtRewards.containsKey(e.getPlayer())) {
			VoteRoulette.lookingAtRewards.remove(e.getPlayer());
		}
		if (VoteRoulette.lookingAtMilestones.containsKey(e.getPlayer())) {
			VoteRoulette.lookingAtMilestones.remove(e.getPlayer());
		}
	}

	/*
	 * @EventHandler(priority=EventPriority.HIGHEST) public void
	 * onCommand(PlayerCommandPreprocessEvent e) {
	 * if(Bukkit.getCommandAliases()) { e.getMessage() }
	 */
}
