package com.mythicacraft.voteroulette.listeners;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mythicacraft.voteroulette.DelayedCommand;
import com.mythicacraft.voteroulette.PlayerManager;
import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.Utils;


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
			player.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "reward").replace("%amount%", Integer.toString(unclaimedRewards)).replace("%command%", "/vr claim rewards"));
		}
		if(unclaimedMilestones > 0) {
			player.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%", "milestone").replace("%amount%", Integer.toString(unclaimedMilestones)).replace("%command%", "/vr claim milestones"));
		}

		if(plugin.USE_TWENTYFOUR_REMINDER) {
			if(pm.playerHasntVotedInADay(event.getPlayer().getName())) {
				player.sendMessage(plugin.TWENTYFOUR_REMINDER.replace("%player%", event.getPlayer().getName()));
				if(!VoteRoulette.notifiedPlayers.contains(event.getPlayer())) {
					VoteRoulette.notifiedPlayers.add(event.getPlayer());
				}
			}
		}

		if(player.hasPermission("voteroulette.admin")) {
			if(plugin.hasUpdate()) {
				player.sendMessage(ChatColor.AQUA + "[VoteRoulette] There is a new version available to download! Visit " + ChatColor.YELLOW + "http://dev.bukkit.org/bukkit-plugins/voteroulette/");
			}
		}
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		if(VoteRoulette.notifiedPlayers.contains(event.getPlayer())) {
			VoteRoulette.notifiedPlayers.remove(event.getPlayer());
		}
		List<DelayedCommand> playerDelayedCmds = Utils.getPlayerDelayedCmds(event.getPlayer().getName());
		for(DelayedCommand dCmd : playerDelayedCmds) {
			if(dCmd.shouldRunOnLogOff()) {
				dCmd.run();
				dCmd.cancel();
			}
		}
	}
}
