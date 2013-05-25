package com.mythicacraft.voteroulette.utils;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;


public class Utils {

	static Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");

	public static Player[] getBlacklistPlayers() {
		List<?> blacklistStr = plugin.getConfig().getList("blacklistedPlayers");
		Player[] blacklistPlayers = new Player[blacklistStr.size()];
		for(int i = 0; i < blacklistStr.size(); i++) {
			Player p = plugin.getServer().getPlayerExact((String) blacklistStr.get(i));
			blacklistPlayers[i] = p;
		}
		return blacklistPlayers;
	}

	public static boolean playerIsBlacklisted(Player player) {
		Player[] blacklistPlayers = getBlacklistPlayers();
		for(int i = 0; i < blacklistPlayers.length; i++) {
			if(player == blacklistPlayers[i]) {
				return true;
			}
		}
		return false;
	}

	public static int getPlayerOpenInvSlots(Player player) {
		Inventory inv = player.getInventory();
		ItemStack[] contents = inv.getContents();
		int count = 0;
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] == null)
				count++;
		}
		return count;
	}

}
