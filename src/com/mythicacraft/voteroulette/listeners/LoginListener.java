package com.mythicacraft.voteroulette.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.mythicacraft.voteroulette.Milestone;
import com.mythicacraft.voteroulette.Reward;
import com.mythicacraft.voteroulette.VoteRoulette;


public class LoginListener implements Listener {

	@EventHandler (priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		List<Reward> unclaimedRewards = VoteRoulette.getPlayerManager().getUnclaimedRewards(event.getPlayer().getName());
		List<Milestone> unclaimedMilestones = VoteRoulette.getPlayerManager().getUnclaimedMilestones(event.getPlayer().getName());
		if(unclaimedRewards.size() > 0) {
			event.getPlayer().sendMessage(ChatColor.AQUA + "[VoteRoulette] You have " + ChatColor.YELLOW + unclaimedRewards.size() + ChatColor.AQUA + " unclaimed reward(s)! Type " + ChatColor.YELLOW + "/vr claim rewards" + ChatColor.AQUA + " to see them.");
		}
		if(unclaimedMilestones.size() > 0) {
			event.getPlayer().sendMessage(ChatColor.AQUA + "[VoteRoulette] You have " + ChatColor.YELLOW + unclaimedMilestones.size() + ChatColor.AQUA + " unclaimed milestone(s)! Type " + ChatColor.YELLOW + "/vr claim milestones" + ChatColor.AQUA + " to see them.");
		}
	}
}
