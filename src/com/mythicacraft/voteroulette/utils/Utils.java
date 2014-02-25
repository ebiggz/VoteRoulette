package com.mythicacraft.voteroulette.utils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.mythicacraft.voteroulette.DelayedCommand;
import com.mythicacraft.voteroulette.VoteRoulette;


public class Utils {

	static Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("VoteRoulette");

	public static List<String> getBlacklistPlayers() {
		List<String> blacklistStr = plugin.getConfig().getStringList("blacklistedPlayers");
		return blacklistStr;
	}

	public static void saveKnownWebsite(String website) {
		ConfigAccessor websiteFile = new ConfigAccessor("data" + File.separator + "known websites.yml");
		List<String> websites = websiteFile.getConfig().getStringList("known-websites");
		if(websites != null) {
			if(!websites.contains(website)) {
				websites.add(website);
				websiteFile.getConfig().set("known-websites", websites);
				websiteFile.saveConfig();
			}
		}
	}

	public static boolean playerIsBlacklisted(String playerName) {
		if(getBlacklistPlayers().contains(playerName)) return true;
		return false;
	}

	public static String transcribeColorCodes(String message) {
		message = message.replace("%black%", "&0").replace("%darkblue%", "&1").replace("%darkgreen%", "&2").replace("%darkaqua%", "&3").replace("%darkred%", "&4").replace("%purple%", "&5").replace("%gold%", "&6").replace("%grey%", "&7").replace("%darkgrey%", "&8").replace("%blue%", "&9").replace("%green%", "&a").replace("%aqua%", "&b").replace("%red%", "&c").replace("%pink%", "&d").replace("%yellow%", "&e").replace("%white%", "&f").replace("%bold%", "&l").replace("%strikethrough%", "&m").replace("%underline%", "&n").replace("%italic%", "&o").replace("%reset%", "&r").replace("%magic%", "&k");
		message = ChatColor.translateAlternateColorCodes('&', message);
		return message;
	}

	public static List<DelayedCommand> getPlayerDelayedCmds(String playerName) {
		List<DelayedCommand> playerDCs = new ArrayList<DelayedCommand>();
		for(DelayedCommand dCmd : VoteRoulette.delayedCommands) {
			if(playerName.equals(dCmd.getPlayer())) {
				playerDCs.add(dCmd);
			}
		}
		return playerDCs;
	}

	public static boolean worldIsBlacklisted(String worldName) {
		List<String> blacklistStr = plugin.getConfig().getStringList("blacklistedWorlds");
		if(blacklistStr.contains(worldName)) return true;
		return false;
	}

	public static boolean playerIsOnline(String playerName) {
		Player[] onlinePlayers = Bukkit.getOnlinePlayers();
		for(Player player : onlinePlayers) {
			if(player.getName().equals(playerName)) return true;
		}
		return false;
	}

	public static String timeString(int totalMinutes) {

		String timeStr = "";

		int totalMins = totalMinutes;
		int totalHours = totalMins/60;
		int totalDays = totalHours/24;
		int remainingMins = totalMins % 60;
		int remainingHours = totalHours % 24;

		if(totalDays > 0) {
			timeStr += Integer.toString(totalDays) + " day";
			if(totalDays > 1) {
				timeStr += "s";
			}
		}
		if(totalHours > 0) {
			int hours = totalHours;
			if(totalDays > 0) {
				hours = remainingHours;
				if(remainingHours > 0) {
					if(remainingMins > 0) {
						timeStr += ", ";
					} else {
						timeStr += " and ";
					}
					timeStr += Integer.toString(hours) + " hour";
					if(hours > 1) {
						timeStr += "s";
					}
				}
			} else {
				timeStr += Integer.toString(hours) + " hour";
				if(hours > 1) {
					timeStr += "s";
				}
			}
		}
		if(totalMins > 0) {
			if(totalDays > 0) {
				if(remainingMins > 0) {
					if(remainingHours > 0) {
						timeStr += ", and ";
					} else {
						timeStr += " and ";
					}
				}
			} else {
				if(totalHours > 0) {
					if(remainingMins > 0) {
						timeStr += " and ";
					}
				}
			}
			int mins = totalMins;
			if(totalDays > 0 || totalHours > 0) {
				mins = remainingMins;
			}
			if(mins > 0) {
				timeStr += Integer.toString(mins) + " minute";
				if(mins > 1) {
					timeStr += "s";
				}
			}
		}
		if(totalMins < 1) {
			timeStr = "less than a minute";
		}
		return timeStr;
	}

	public static int compareTimeToNow(String time) throws ParseException {

		String currentTime = getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH);
		Date date1 = sdf.parse(time);
		Date date2 = sdf.parse(currentTime);

		Long differnceInMills = date2.getTime() - date1.getTime();

		long timeInMinutes = differnceInMills/60000;
		int totalMinutes = (int) timeInMinutes;

		return totalMinutes;
	}

	public static String getTimeSinceString(String time) {
		try {
			int totalMins = compareTimeToNow(time);
			return timeString(totalMins);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH);
		String time = sdf.format(cal.getTime());
		return time;
	}

	public static String worldsString(List<String> worlds) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < worlds.size(); i++) {
			sb.append(worlds.get(i));
			int lastIndex = worlds.size()-1;
			if(i < lastIndex-1) {
				sb.append(", ");
			}
			if(i == lastIndex-1) {
				sb.append(", and ");
			}
		}
		return sb.toString();
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
				else if(itemName.contains("glass")) {
					plural = "";
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
		if(name.equalsIgnoreCase("looting")) {
			return Enchantment.LOOT_BONUS_MOBS;
		}
		if(name.equalsIgnoreCase("silk touch")) {
			return Enchantment.SILK_TOUCH;
		}
		if(name.equalsIgnoreCase("sharpness")) {
			return Enchantment.DAMAGE_ALL;
		}
		if(name.equalsIgnoreCase("bane of arthropods")) {
			return Enchantment.DAMAGE_ARTHROPODS;
		}
		if(name.equalsIgnoreCase("smite")) {
			return Enchantment.DAMAGE_UNDEAD;
		}
		if(name.equalsIgnoreCase("knockback")) {
			return Enchantment.KNOCKBACK;
		}
		if(name.equalsIgnoreCase("protection")) {
			return Enchantment.PROTECTION_ENVIRONMENTAL;
		}
		if(name.equalsIgnoreCase("fire protection")) {
			return Enchantment.PROTECTION_FIRE;
		}
		if(name.equalsIgnoreCase("blast protection")) {
			return Enchantment.PROTECTION_EXPLOSIONS;
		}
		if(name.equalsIgnoreCase("projectile protection")) {
			return Enchantment.PROTECTION_PROJECTILE;
		}
		if(name.equalsIgnoreCase("feather falling")) {
			return Enchantment.PROTECTION_FALL;
		}
		if(name.equalsIgnoreCase("respiration")) {
			return Enchantment.OXYGEN;
		}
		if(name.equalsIgnoreCase("aqua affinity")) {
			return Enchantment.WATER_WORKER;
		}
		if(name.equalsIgnoreCase("thorns")) {
			return Enchantment.THORNS;
		}
		if(name.equalsIgnoreCase("fire aspect")) {
			return Enchantment.FIRE_ASPECT;
		}
		if(name.equalsIgnoreCase("efficiency")) {
			return Enchantment.DIG_SPEED;
		}
		if(name.equalsIgnoreCase("unbreaking")) {
			return Enchantment.DURABILITY;
		}
		if(name.equalsIgnoreCase("fortune")) {
			return Enchantment.LOOT_BONUS_BLOCKS;
		}
		if(name.equalsIgnoreCase("power")) {
			return Enchantment.ARROW_DAMAGE;
		}
		if(name.equalsIgnoreCase("punch")) {
			return Enchantment.ARROW_KNOCKBACK;
		}
		if(name.equalsIgnoreCase("flame")) {
			return Enchantment.ARROW_FIRE;
		}
		if(name.equalsIgnoreCase("infinity")) {
			return Enchantment.ARROW_INFINITE;
		}
		if(name.equalsIgnoreCase("luck of the sea")) {
			return Enchantment.LUCK;
		}
		if(name.equalsIgnoreCase("lure")) {
			return Enchantment.LURE;
		}
		else {
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

	public static Color getColorEnumFromName(String name) {
		if(name.equalsIgnoreCase("aqua")) {
			return Color.AQUA;
		}
		if(name.equalsIgnoreCase("black")) {
			return Color.BLACK;
		}
		if(name.equalsIgnoreCase("blue")) {
			return Color.BLUE;
		}
		if(name.equalsIgnoreCase("fuchsia")) {
			return Color.FUCHSIA;
		}
		if(name.equalsIgnoreCase("gray")) {
			return Color.GRAY;
		}
		if(name.equalsIgnoreCase("green")) {
			return Color.GREEN;
		}
		if(name.equalsIgnoreCase("lime")) {
			return Color.LIME;
		}
		if(name.equalsIgnoreCase("maroon")) {
			return Color.MAROON;
		}
		if(name.equalsIgnoreCase("navy")) {
			return Color.NAVY;
		}
		if(name.equalsIgnoreCase("olive")) {
			return Color.OLIVE;
		}
		if(name.equalsIgnoreCase("orange")) {
			return Color.ORANGE;
		}
		if(name.equalsIgnoreCase("purple")) {
			return Color.PURPLE;
		}
		if(name.equalsIgnoreCase("red")) {
			return Color.RED;
		}
		if(name.equalsIgnoreCase("silver")) {
			return Color.SILVER;
		}
		if(name.equalsIgnoreCase("teal")) {
			return Color.TEAL;
		}
		if(name.equalsIgnoreCase("white")) {
			return Color.WHITE;
		}
		if(name.equalsIgnoreCase("yellow")) {
			return Color.YELLOW;
		}
		else {
			return null;
		}
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
		if(player.hasPermission("voteroulette.votecommand")) {
			sb.append(ChatColor.AQUA + "/vote" + ChatColor.GRAY + " - Get the links to vote on.\n");
		}
		if(player.hasPermission("voteroulette.viewrewards")) {
			sb.append(ChatColor.AQUA + "/vr rewards" + ChatColor.GRAY + " - See rewards you are eligible to get.\n");
		}
		if(player.hasPermission("voteroulette.viewmilestones")) {
			sb.append(ChatColor.AQUA + "/vr milestones" + ChatColor.GRAY + " - See milestones you are eligible to get.\n");
		}
		if(player.hasPermission("voteroulette.lastvote")) {
			sb.append(ChatColor.AQUA + "/vr lastvote" + ChatColor.GRAY + " - Shows how long ago your last vote was.\n");
		}
		if(player.hasPermission("voteroulette.lastvoteothers")) {
			sb.append(ChatColor.AQUA + "/vr lastvote [player]" + ChatColor.GRAY + " - Shows how long ago the given players last vote was.\n");
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
