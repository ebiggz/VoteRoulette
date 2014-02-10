package com.mythicacraft.voteroulette.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mythicacraft.voteroulette.PlayerManager;
import com.mythicacraft.voteroulette.VoteRoulette;


public class LoginListener implements Listener {

	private static VoteRoulette plugin;

	public LoginListener(VoteRoulette instance) {
		plugin = instance;
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		PlayerManager pm = VoteRoulette.getPlayerManager();
		Player player = event.getPlayer();

		int unclaimedRewards = pm.getUnclaimedRewardCount(player.getName());
		int unclaimedMilestones = pm.getUnclaimedMilestoneCount(player.getName());

		if(unclaimedRewards > 0) {
			player.sendMessage(ChatColor.AQUA + "[VoteRoulette] You have " + ChatColor.YELLOW + unclaimedRewards + ChatColor.AQUA + " unclaimed reward(s)! Type " + ChatColor.YELLOW + "/vr claim rewards" + ChatColor.AQUA + " to see them.");
		}
		if(unclaimedMilestones > 0) {
			player.sendMessage(ChatColor.AQUA + "[VoteRoulette] You have " + ChatColor.YELLOW + unclaimedMilestones + ChatColor.AQUA + " unclaimed milestone(s)! Type " + ChatColor.YELLOW + "/vr claim milestones" + ChatColor.AQUA + " to see them.");
		}

		if(plugin.USE_TWENTYFOUR_REMINDER) {
			if(pm.playerHasntVotedInADay(event.getPlayer().getName())) {
				player.sendMessage(ChatColor.AQUA + "[VoteRoulette] " + ChatColor.RESET + plugin.TWENTYFOUR_REMINDER.replace("%player%", event.getPlayer().getName()));
				if(!VoteRoulette.notifiedPlayers.contains(event.getPlayer())) {
					VoteRoulette.notifiedPlayers.add(event.getPlayer());
				}
			}
		}
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		if(VoteRoulette.notifiedPlayers.contains(event.getPlayer())) {
			VoteRoulette.notifiedPlayers.remove(event.getPlayer());
		}
	}
}
