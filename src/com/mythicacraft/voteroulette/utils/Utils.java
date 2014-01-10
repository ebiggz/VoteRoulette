package com.mythicacraft.voteroulette.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;


public class Utils {

	static Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");

	public static List<String> getBlacklistPlayers() {
		List<String> blacklistStr = plugin.getConfig().getStringList("blacklistedPlayers");
		return blacklistStr;
	}

	public static boolean playerIsBlacklisted(String playerName) {
		if(getBlacklistPlayers().contains(playerName)) return true;
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
	public static String getItemListString(ItemStack[] items) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for(int i = 0; i < items.length; i++) {
			ItemStack is = items[i];
			sb.append(is.getAmount() + " ");
			String itemName = is.getType().toString().toLowerCase().replace("_", " ");
			sb.append(itemName);
			if(is.getAmount() > 1) {
				String plural = "s";
				if(itemName.endsWith("ch")) {
					plural = "es";
				}
				else if(itemName.contains("beef")) {
					plural = "";
				}
				else if(itemName.contains("lapiz")) {
					plural = "";
				}
				else if(itemName.contains("potato")) {
					plural = "es";
				}
				sb.append(plural);
			}
			if(is.getItemMeta().hasEnchants()) {
				sb.append("(with ");
				Map<Enchantment, Integer> enchants = is.getItemMeta().getEnchants();
				Set<Enchantment> enchantKeys = enchants.keySet();
				for(Enchantment enchant : enchantKeys) {
					int level = enchants.get(enchant);
					sb.append(getNameFromEnchant(enchant) + " " + Integer.toString(level) + ", ");
				}
				sb.delete(sb.length()-2, sb.length());
				sb.append(")");
			}
			int lastIndex = items.length-1;
			if(i < lastIndex-1) {
				sb.append(", ");
			}
			if(i == lastIndex-1) {
				sb.append(", and ");
			}
			if(count % 2 == 0) {
				sb.append(ChatColor.AQUA);
			} else {
				sb.append(ChatColor.DARK_AQUA);
			}
			count++;
		}
		return sb.toString();
	}
	public static String getItemListSentance(ItemStack[] items) {
		StringBuilder sb = new StringBuilder();
		//int count = 0;
		for(int i = 0; i < items.length; i++) {
			ItemStack is = items[i];
			sb.append(is.getAmount() + " ");
			String itemName = is.getType().toString().toLowerCase().replace("_", " ");
			sb.append(itemName);
			if(is.getAmount() > 1) {
				String plural = "s";
				if(itemName.endsWith("ch")) {
					plural = "es";
				}
				else if(itemName.contains("beef")) {
					plural = "";
				}
				else if(itemName.contains("lapiz")) {
					plural = "";
				}
				else if(itemName.contains("potato")) {
					plural = "es";
				}
				sb.append(plural);
			}
			if(is.getItemMeta().hasEnchants()) {
				sb.append("(with ");
				Map<Enchantment, Integer> enchants = is.getItemMeta().getEnchants();
				Set<Enchantment> enchantKeys = enchants.keySet();
				for(Enchantment enchant : enchantKeys) {
					int level = enchants.get(enchant);
					sb.append(getNameFromEnchant(enchant) + " " + Integer.toString(level) + ", ");
				}
				sb.delete(sb.length()-2, sb.length());
				sb.append(")");
			}
			int lastIndex = items.length-1;
			if(i < lastIndex-1) {
				sb.append(", ");
			}
			if(i == lastIndex-1) {
				sb.append(", and ");
			}
		}
		return sb.toString();
	}
	public static Enchantment getEnchantEnumFromName(String name) {
		switch(name.toLowerCase()) {
		case "looting":
			return Enchantment.LOOT_BONUS_MOBS;
		case "silk touch":
			return Enchantment.SILK_TOUCH;
		case "sharpness":
			return Enchantment.DAMAGE_ALL;
		case "bane of arthropods":
			return Enchantment.DAMAGE_ARTHROPODS;
		case "smite":
			return Enchantment.DAMAGE_UNDEAD;
		case "knockback":
			return Enchantment.KNOCKBACK;
		case "protection":
			return Enchantment.PROTECTION_ENVIRONMENTAL;
		case "fire protection":
			return Enchantment.PROTECTION_FIRE;
		case "blast protection":
			return Enchantment.PROTECTION_EXPLOSIONS;
		case "projectile protection":
			return Enchantment.PROTECTION_PROJECTILE;
		case "feather falling":
			return Enchantment.PROTECTION_FALL;
		case "respiration":
			return Enchantment.OXYGEN;
		case "aqua affinity":
			return Enchantment.WATER_WORKER;
		case "thorns":
			return Enchantment.THORNS;
		case "fire aspect":
			return Enchantment.FIRE_ASPECT;
		case "efficiency":
			return Enchantment.DIG_SPEED;
		case "unbreaking":
			return Enchantment.DURABILITY;
		case "fortune":
			return Enchantment.LOOT_BONUS_BLOCKS;
		case "power":
			return Enchantment.ARROW_DAMAGE;
		case "punch":
			return Enchantment.ARROW_KNOCKBACK;
		case "flame":
			return Enchantment.ARROW_FIRE;
		case "infinity":
			return Enchantment.ARROW_INFINITE;
		case "luck of the sea":
			return Enchantment.LUCK;
		case "lure":
			return Enchantment.LURE;
		default:
			return null;
		}
	}
	public static String getNameFromEnchant(Enchantment enchant) {
		String name = "";
		if(enchant == Enchantment.LOOT_BONUS_MOBS) {
			name = "looting";
		}
		else if(enchant == Enchantment.SILK_TOUCH) {
			name = "silk touch";
		}
		else if(enchant == Enchantment.DAMAGE_ALL) {
			name = "sharpness";
		}
		else if(enchant == Enchantment.DAMAGE_ARTHROPODS) {
			name = "bane of arthropods";
		}
		else if(enchant == Enchantment.DAMAGE_UNDEAD) {
			name = "smite";
		}
		else if(enchant == Enchantment.KNOCKBACK) {
			name = "knockback";
		}
		else if(enchant == Enchantment.PROTECTION_ENVIRONMENTAL) {
			name = "protection";
		}
		else if(enchant == Enchantment.PROTECTION_EXPLOSIONS) {
			name = "blast protection";
		}
		else if(enchant == Enchantment.PROTECTION_FALL) {
			name = "feather falling";
		}
		else if(enchant == Enchantment.PROTECTION_FIRE) {
			name = "fire protection";
		}
		else if(enchant == Enchantment.PROTECTION_PROJECTILE) {
			name = "projectile protection";
		}
		else if(enchant == Enchantment.OXYGEN) {
			name = "respiration";
		}
		else if(enchant == Enchantment.WATER_WORKER) {
			name = "aqua affinity";
		}
		else if(enchant == Enchantment.THORNS) {
			name = "thorns";
		}
		else if(enchant == Enchantment.FIRE_ASPECT) {
			name = "fire aspect";
		}
		else if(enchant == Enchantment.DIG_SPEED) {
			name = "efficiency";
		}
		else if(enchant == Enchantment.DURABILITY) {
			name = "unbreaking";
		}
		else if(enchant == Enchantment.LOOT_BONUS_BLOCKS) {
			name = "fortune";
		}
		else if(enchant == Enchantment.ARROW_DAMAGE) {
			name = "power";
		}
		else if(enchant == Enchantment.ARROW_FIRE) {
			name = "flame";
		}
		else if(enchant == Enchantment.ARROW_INFINITE) {
			name = "infinity";
		}
		else if(enchant == Enchantment.ARROW_KNOCKBACK) {
			name = "punch";
		}
		else if(enchant == Enchantment.LUCK) {
			name = "luck of the sea";
		}
		else if(enchant == Enchantment.LURE) {
			name = "lure";
		}
		return name;
	}

	public static String completeName(String playername) {
		Player[] onlinePlayers = Bukkit.getOnlinePlayers();
		for(int i = 0; i < onlinePlayers.length; i++) {
			if(onlinePlayers[i].getName().toLowerCase().startsWith(playername.toLowerCase())) {
				return onlinePlayers[i].getName();
			}
		}
		OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
		for(int i = 0; i < offlinePlayers.length; i++) {
			if(offlinePlayers[i].getName().toLowerCase().startsWith(playername.toLowerCase())) {
				return offlinePlayers[i].getName();
			}
		}
		return null;
	}

	public static String helpMenu(CommandSender player) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.AQUA + "/vr ?" + ChatColor.GRAY + " - This help menu.\n");
		if(player.hasPermission("voteroulette.viewrewards")) {
			sb.append(ChatColor.AQUA + "/vr rewards" + ChatColor.GRAY + " - See rewards you are eligible to get.\n");
		}
		if(player.hasPermission("voteroulette.viewmilestones")) {
			sb.append(ChatColor.AQUA + "/vr milestones" + ChatColor.GRAY + " - See milestones you are eligible to get.\n");
		}
		if(player.hasPermission("voteroulette.viewstats")) {
			sb.append(ChatColor.AQUA + "/vr stats" + ChatColor.GRAY + " - See your voting stats.\n");
		}
		if(player.hasPermission("voteroulette.viewotherstats")) {
			sb.append(ChatColor.AQUA + "/vr stats [player]" + ChatColor.GRAY + " - See the stats of another player.\n");
		}
		if(player.hasPermission("voteroulette.editstats")) {
			sb.append(ChatColor.AQUA + "/vr stats [player] settotal [#]" + ChatColor.GRAY + " - Set a players total votes.\n");
			sb.append(ChatColor.AQUA + "/vr stats [player] setcycle [#]" + ChatColor.GRAY + " - Set a players current vote cycle.\n");
		}
		sb.append(ChatColor.AQUA + "/vr claim" + ChatColor.GRAY + " - Tells you if you have any unclaimed rewards or\n milestones you received while offline.\n");
		sb.append(ChatColor.AQUA + "/vr claim rewards" + ChatColor.GRAY + " - Lists any of your unclaimed rewards.\n");
		sb.append(ChatColor.AQUA + "/vr claim rewards [#/all]" + ChatColor.GRAY + " - Gives you the reward with the given # or all of them.\n");
		sb.append(ChatColor.AQUA + "/vr claim milestones" + ChatColor.GRAY + " - Lists any of your unclaimed milestones.\n");
		sb.append(ChatColor.AQUA + "/vr claim milestones [#/all]" + ChatColor.GRAY + " - Gives you the milestone with the given # or all of them.\n");
		if(player.hasPermission("voteroulette.forcevote")) {
			sb.append(ChatColor.AQUA + "/vr forcevote [player]" + ChatColor.GRAY + " - Make it as if the given player just voted, this will update their stats and give them an applicable reward/milestone.\n");
		}
		if(player.hasPermission("voteroulette.admin")) {
			sb.append(ChatColor.AQUA + "/vr reload" + ChatColor.GRAY + " - Reloads the config file.\n");
		}
		return sb.toString();
	}
}
