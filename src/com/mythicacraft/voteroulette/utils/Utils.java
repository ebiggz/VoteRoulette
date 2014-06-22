package com.mythicacraft.voteroulette.utils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.Potion;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter.Stat;
import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.DelayedCommand;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.awards.Reward.VoteStreakModifier;
import com.mythicacraft.voteroulette.stats.BoardReset;
import com.mythicacraft.voteroulette.stats.VoteStat.StatType;
import com.mythicacraft.voteroulette.stats.VoterStat;

public class Utils {

	/**
	 * TODO: Tame this beast
	 */

	private static VoteRoulette plugin;

	public Utils(VoteRoulette instance) {
		plugin = instance;
	}

	public static void debugMessage(String message) {
		if (plugin.DEBUG) {
			plugin.getLogger().info("DEBUG: " + message);
		}
	}

	public static ItemStack[] updateLoreAndCustomNames(String playerName,
	        ItemStack[] items) {
		ItemStack[] itemsClone = items.clone();
		for (ItemStack item : itemsClone) {
			ItemMeta im = item.getItemMeta();
			if (im.hasLore()) {
				List<String> oldLore = im.getLore();
				List<String> newLore = new ArrayList<String>();
				for (String line : oldLore) {
					newLore.add(line.replace("%player%", playerName));
				}
				im.setLore(newLore);
			}
			if (im.hasDisplayName()) {
				im.setDisplayName(im.getDisplayName().replace("%player%",
				        playerName));
			}
			if (item.getType() == Material.SKULL_ITEM
			        && item.getDurability() == 3 /* playerhead */) {
				SkullMeta sim = (SkullMeta) im;
				if (sim.hasOwner()) {
					sim.setOwner(sim.getOwner().replace("%player%", playerName));
					im = sim;
				}
			}
			item.setItemMeta(im);
		}
		return itemsClone;
	}

	@SuppressWarnings("deprecation")
	public static void showTopScoreboard(final Player player, StatType stat) {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		Scoreboard board = manager.getNewScoreboard();
		board.registerNewObjective("TopVotes", "dummy");
		Objective objective = board.getObjective("TopVotes");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		if (stat == StatType.TOTAL_VOTES) {
			objective.setDisplayName(ChatColor.AQUA + plugin.TOTAL_VOTES_DEF);
			List<VoterStat> topStats = VoteRoulette.getStatsManager()
			        .getTopLifetimeVotes();
			if (topStats == null) {
				player.sendMessage(ChatColor.RED + "Error: stats are empty.");
				return;
			}
			for (VoterStat vs : topStats) {
				String name = vs.getPlayerName();
				if (name.length() > 16) {
					name = name.substring(0, 11) + "...";
				}
				Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
				score.setScore(vs.getStatCount());
			}
		}
		if (stat == StatType.LONGEST_VOTE_STREAKS) {
			objective.setDisplayName(ChatColor.AQUA
			        + plugin.LONGEST_VOTE_STREAK_DEF);
			List<VoterStat> topStats = VoteRoulette.getStatsManager()
			        .getTopLongestVotestreaks();
			if (topStats == null) {
				player.sendMessage(ChatColor.RED + "Error: stats are empty.");
				return;
			}
			for (VoterStat vs : topStats) {
				String name = vs.getPlayerName();
				if (name.length() > 16) {
					name = name.substring(0, 11) + "...";
				}
				Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
				score.setScore(vs.getStatCount());
			}
		}
		player.setScoreboard(board);
		// cancel button selection after 5 seconds
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(plugin,
		        new BoardReset(player, board), 300L);
	}

	public static void showTopInChat(Player player, StatType stat) {
		List<VoterStat> topStats = null;
		if (stat == StatType.TOTAL_VOTES) {
			topStats = VoteRoulette.getStatsManager().getTopLifetimeVotes();
		}
		if (stat == StatType.LONGEST_VOTE_STREAKS) {
			topStats = VoteRoulette.getStatsManager()
			        .getTopLongestVotestreaks();
		}
		if (topStats == null) {
			player.sendMessage(ChatColor.RED + "Error: stats are empty.");
			return;
		}
		Collections.sort(topStats, new Comparator<VoterStat>() {

			@Override
			public int compare(VoterStat v1, VoterStat v2) {
				return v2.getStatCount() - v1.getStatCount();
			}
		});
		if (topStats != null) {
			int count = 1;
			String topNumber = "";
			for (VoterStat vs : topStats) {
				String statCount = Integer.toString(vs.getStatCount());
				if (count == 1) {
					topNumber = statCount;
				}
				int lengthDif = topNumber.length() - statCount.length();
				String message = "";
				for (int i = 0; i < lengthDif; i++) {
					message += " ";
				}
				message += ChatColor.GOLD + statCount + " " + ChatColor.WHITE
				        + vs.getPlayerName();

				player.sendMessage(message);
				count++;
			}
		}
	}

	public static void sendMessageToPlayer(String message, String playerName) {
		@SuppressWarnings("deprecation")
		Player player = Bukkit.getPlayerExact(playerName);
		if (player == null)
			return;
		player.sendMessage(message);
	}

	public static void broadcastMessageToServer(String message,
	        String exemptPlayer) {
		if (plugin.BROADCAST_TO_SERVER) {
			if (plugin.ONLY_BROADCAST_ONLINE
			        && !Utils.playerIsOnline(exemptPlayer))
				return;
			if (plugin.USE_BROADCAST_COOLDOWN) {
				if (VoteRoulette.cooldownPlayers.contains(exemptPlayer)) {
					Utils.debugMessage(exemptPlayer
					        + " is in broadcast cooldown.");
					return;
				} else {
					Utils.debugMessage(exemptPlayer
					        + " is not in broadcast cooldown.");
				}
			}
			Player[] onlinePlayers = Bukkit.getOnlinePlayers();
			for (Player player : onlinePlayers) {
				if (player.getName().equals(exemptPlayer))
					continue;
				player.sendMessage(message);
			}
			if (plugin.USE_BROADCAST_COOLDOWN) {
				if (!VoteRoulette.cooldownPlayers.contains(exemptPlayer)) {
					VoteRoulette.cooldownPlayers.add(exemptPlayer);
					Utils.debugMessage("Put " + exemptPlayer
					        + " in broadcast cooldown.");
				}
				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {

					private String playerName;

					@Override
					public void run() {
						if (VoteRoulette.cooldownPlayers.contains(playerName)) {
							VoteRoulette.cooldownPlayers.remove(playerName);
						}
					}

					private Runnable init(String playerName) {
						this.playerName = playerName;
						return this;
					}
				}.init(exemptPlayer), plugin.BROADCAST_COOLDOWN * 60 * 20);
			}
		}
		if (plugin.LOG_TO_CONSOLE) {
			plugin.getLogger().info(message);
		}
	}

	public static String getServerMessageWithAward(Award award,
	        String playerName, String website) {
		String message = getServerAwardMessage(plugin.SERVER_BROADCAST_MESSAGE,
		        award, playerName);
		message = message.replace("%site%", website);
		return message;
	}

	public static String getServerAwardMessage(String awardMessage,
	        Award award, String playerName) {
		awardMessage = awardMessage.replace("%name%", award.getName());
		awardMessage = awardMessage.replace("%player%", playerName);
		awardMessage = awardMessage.replace("%server%", Bukkit.getServerName());

		if (award.getAwardType() == AwardType.MILESTONE) {
			awardMessage = awardMessage.replace("%type%",
			        plugin.MILESTONE_DEF.toLowerCase());
		} else {
			awardMessage = awardMessage.replace("%type%",
			        plugin.REWARD_DEF.toLowerCase());
		}
		awardMessage = awardMessage.replace("%prizes%",
		        getAwardPrizesString(award));
		return awardMessage;
	}

	public static String getAwardMessage(String awardMessage, Award award,
	        String playerName) {
		if (!award.hasMessage()) {
			awardMessage = awardMessage.replace("%name%", award.getName());
			awardMessage = awardMessage.replace("%player%", playerName);
			awardMessage = awardMessage.replace("%server%",
			        Bukkit.getServerName());

			if (award.getAwardType() == AwardType.MILESTONE) {
				awardMessage = awardMessage.replace("%type%",
				        plugin.MILESTONE_DEF.toLowerCase());
			} else {
				awardMessage = awardMessage.replace("%type%",
				        plugin.REWARD_DEF.toLowerCase());
			}
			awardMessage = awardMessage.replace("%prizes%",
			        getAwardPrizesString(award));
			return awardMessage;
		} else {
			return Utils.transcribeColorCodes(award.getMessage().replace(
			        "%player%", playerName));
		}
	}

	private static String getAwardPrizesString(Award award) {
		StringBuilder sb = new StringBuilder();
		if (award.hasDescription()) {
			sb.append(award.getDescription());
		} else {
			if (award.hasCurrency()) {
				sb.append(plugin.CURRENCY_SYMBOL + award.getCurrency());
			}
			if (award.hasXpLevels()) {
				if (award.hasCurrency()) {
					sb.append(", ");
				}
				sb.append(award.getXpLevels() + " "
				        + plugin.XPLEVELS_DEF.toLowerCase());
			}
			if (award.hasItems()) {
				if (award.hasCurrency() || award.hasXpLevels()) {
					sb.append(", ");
				}
				sb.append(Utils.getItemListSentance(award.getItems()));
			}
		}
		return sb.toString();
	}

	public static List<String> getBlacklistPlayers() {
		List<String> blacklistStr = plugin.getConfig().getStringList(
		        "blacklistedPlayers");
		return blacklistStr;
	}

	public static void saveKnownWebsite(String website) {
		if (website.equalsIgnoreCase("forcevote"))
			return;
		ConfigAccessor websiteFile = new ConfigAccessor("data" + File.separator
		        + "known websites.yml");
		List<String> websites = websiteFile.getConfig().getStringList(
		        "known-websites");
		if (websites != null) {
			if (!websites.contains(website)) {
				websites.add(website);
				websiteFile.getConfig().set("known-websites", websites);
				websiteFile.saveConfig();
			}
		}
	}

	public static String getKnownWebsites() {
		ConfigAccessor websiteFile = new ConfigAccessor("data" + File.separator
		        + "known websites.yml");
		List<String> websites = websiteFile.getConfig().getStringList(
		        "known-websites");
		if (websites != null && !websites.isEmpty()) {
			return Utils.concatListToString(websites);
		} else {
			return "none";
		}
	}

	public static boolean playerIsBlacklisted(String playerName) {
		if (getBlacklistPlayers().contains(playerName))
			return true;
		return false;
	}

	public static String transcribeColorCodes(String message) {
		message = message.replace("%black%", "&0").replace("%darkblue%", "&1")
		        .replace("%darkgreen%", "&2").replace("%darkaqua%", "&3")
		        .replace("%darkred%", "&4").replace("%purple%", "&5")
		        .replace("%gold%", "&6").replace("%grey%", "&7")
		        .replace("%darkgrey%", "&8").replace("%blue%", "&9")
		        .replace("%green%", "&a").replace("%aqua%", "&b")
		        .replace("%red%", "&c").replace("%pink%", "&d")
		        .replace("%yellow%", "&e").replace("%white%", "&f")
		        .replace("%bold%", "&l").replace("%strikethrough%", "&m")
		        .replace("%underline%", "&n").replace("%italic%", "&o")
		        .replace("%reset%", "&r").replace("%magic%", "&k");
		message = ChatColor.translateAlternateColorCodes('&', message);
		return message;
	}

	public static List<DelayedCommand> getPlayerDelayedCmds(String playerName) {
		List<DelayedCommand> playerDCs = new ArrayList<DelayedCommand>();
		for (DelayedCommand dCmd : VoteRoulette.delayedCommands) {
			if (playerName.equals(dCmd.getPlayer())) {
				playerDCs.add(dCmd);
			}
		}
		return playerDCs;
	}

	public static boolean worldIsBlacklisted(String worldName) {
		List<String> blacklistStr = plugin.getConfig().getStringList(
		        "blacklistedWorlds");
		if (blacklistStr.contains(worldName))
			return true;
		return false;
	}

	public static boolean playerIsOnline(String playerName) {
		Player[] onlinePlayers = Bukkit.getOnlinePlayers();
		for (Player player : onlinePlayers) {
			if (player.getName().equals(playerName))
				return true;
		}
		return false;
	}

	public static String timeString(int totalMinutes) {

		String timeStr = "";

		int totalMins = totalMinutes;
		int totalHours = totalMins / 60;
		int totalDays = totalHours / 24;
		int remainingMins = totalMins % 60;
		int remainingHours = totalHours % 24;

		if (totalDays > 0) {
			timeStr += Integer.toString(totalDays) + " ";
			if (totalDays > 1) {
				timeStr += plugin.DAY_PLURAL_DEF;
			} else {
				timeStr += plugin.DAY_DEF;
			}
		}
		if (totalHours > 0) {
			int hours = totalHours;
			if (totalDays > 0) {
				hours = remainingHours;
				if (remainingHours > 0) {
					if (remainingMins > 0) {
						timeStr += ", ";
					} else {
						timeStr += " " + plugin.AND_DEF + " ";
					}
					timeStr += Integer.toString(hours) + " ";
					if (hours > 1) {
						timeStr += plugin.HOUR_PLURAL_DEF;
					} else {
						timeStr += plugin.HOUR_DEF;
					}
				}
			} else {
				timeStr += Integer.toString(hours) + " ";
				if (hours > 1) {
					timeStr += plugin.HOUR_PLURAL_DEF;
				} else {
					timeStr += plugin.HOUR_DEF;
				}
			}
		}
		if (totalMins > 0) {
			if (totalDays > 0) {
				if (remainingMins > 0) {
					if (remainingHours > 0) {
						timeStr += ", " + plugin.AND_DEF + " ";
					} else {
						timeStr += " " + plugin.AND_DEF + " ";
					}
				}
			} else {
				if (totalHours > 0) {
					if (remainingMins > 0) {
						timeStr += " " + plugin.AND_DEF + " ";
					}
				}
			}
			int mins = totalMins;
			if (totalDays > 0 || totalHours > 0) {
				mins = remainingMins;
			}
			if (mins > 0) {
				timeStr += Integer.toString(mins) + " ";
				if (mins > 1) {
					timeStr += plugin.MINUTE_PLURAL_DEF;
				} else {
					timeStr += plugin.MINUTE_DEF;
				}
			}
		}
		if (totalMins < 1) {
			timeStr = "0 " + plugin.MINUTE_PLURAL_DEF;
		}
		return timeStr;
	}

	public static int compareTimeToNow(String time) throws ParseException {

		String currentTime = getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a",
		        Locale.ENGLISH);
		Date date1 = sdf.parse(time);
		Date date2 = sdf.parse(currentTime);

		Long differnceInMills = date2.getTime() - date1.getTime();

		long timeInMinutes = differnceInMills / 60000;
		int totalMinutes = (int) timeInMinutes;

		return totalMinutes;
	}

	public static String getTimeSinceString(String time) {
		try {
			int totalMins = compareTimeToNow(time);
			return timeString(totalMins);
		} catch (ParseException e) {
		}
		return "";
	}

	public static String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a");
		String time = sdf.format(cal.getTime());
		return time;
	}

	public static String worldsString(List<String> worlds) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < worlds.size(); i++) {
			sb.append(worlds.get(i));
			int lastIndex = worlds.size() - 1;
			if (i < lastIndex - 1) {
				sb.append(", ");
			}
			if (i == lastIndex - 1) {
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
		for (int i = 0; i < items.length; i++) {
			ItemStack is = items[i];
			sb.append(is.getAmount() + " ");
			String itemName = is.getType().toString().toLowerCase()
			        .replace("_", " ");
			if (itemName.equals("monster egg")) {
				SpawnEgg egg = (SpawnEgg) is.getData();
				itemName = egg.getSpawnedType().toString().toLowerCase()
				        .replace("_", " ")
				        + " egg";
			}
			if (itemName.equals("potion")) {
				if (is.getAmount() > 1) {
					itemName += "s";
				}
				Potion potion = Potion.fromItemStack(is);
				itemName += " of "
				        + potion.getType().toString().toLowerCase()
				                .replace("_", " ");
				if (potion.getLevel() > 1) {
					itemName += " II";
				}
				if (potion.hasExtendedDuration()) {
					itemName += " (extended)";
				}
			}
			sb.append(itemName);
			if (!itemName.startsWith("potion")) {
				if (is.getAmount() > 1) {
					String plural = "s";
					if (itemName.endsWith("ch")) {
						plural = "es";
					} else
						if (itemName.contains("beef")) {
							plural = "";
						} else
							if (itemName.contains("lapiz")) {
								plural = "";
							} else
								if (itemName.contains("potato")) {
									plural = "es";
								} else
									if (itemName.contains("glass")) {
										plural = "";
									} else
										if (itemName.contains("grass")) {
											plural = "";
										} else
											if (itemName.contains("dirt")) {
												plural = "";
											}
					sb.append(plural);
				}
			}
			EnchantmentStorageMeta esm = null;
			boolean useStorage = false;
			if (is.getType() == Material.ENCHANTED_BOOK) {
				esm = (EnchantmentStorageMeta) is.getItemMeta();
				useStorage = true;
			}
			if (is.getItemMeta().hasEnchants()
			        || (useStorage && esm.hasStoredEnchants())) {
				sb.append("(with ");
				Map<Enchantment, Integer> enchants = null;
				if (useStorage) {
					enchants = esm.getStoredEnchants();
				} else {
					enchants = is.getItemMeta().getEnchants();
				}
				Set<Enchantment> enchantKeys = enchants.keySet();
				for (Enchantment enchant : enchantKeys) {
					int level = enchants.get(enchant);
					sb.append(getNameFromEnchant(enchant) + " "
					        + Integer.toString(level) + ", ");
				}
				sb.delete(sb.length() - 2, sb.length());
				sb.append(")");
			}
			int lastIndex = items.length - 1;
			if (i < lastIndex - 1) {
				sb.append(", ");
			}
			if (i == lastIndex - 1) {
				sb.append(", " + plugin.AND_DEF + " ");
			}
			if (count % 2 == 0) {
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
		// int count = 0;
		for (int i = 0; i < items.length; i++) {
			ItemStack is = items[i];
			sb.append(is.getAmount() + " ");
			String itemName = is.getType().toString().toLowerCase()
			        .replace("_", " ");
			if (itemName.equals("monster egg")) {
				SpawnEgg egg = (SpawnEgg) is.getData();
				itemName = egg.getSpawnedType().toString().toLowerCase()
				        .replace("_", " ")
				        + " egg";
			}
			if (itemName.equals("potion")) {
				if (is.getAmount() > 1) {
					itemName += "s";
				}
				Potion potion = Potion.fromItemStack(is);
				itemName += " of "
				        + potion.getType().toString().toLowerCase()
				                .replace("_", " ");
				if (potion.getLevel() > 1) {
					itemName += " II";
				}
				if (potion.hasExtendedDuration()) {
					itemName += " (extended)";
				}
			}
			sb.append(itemName);
			if (!itemName.startsWith("potion")) {
				if (is.getAmount() > 1) {
					String plural = "s";
					if (itemName.endsWith("ch")) {
						plural = "es";
					} else
						if (itemName.contains("beef")) {
							plural = "";
						} else
							if (itemName.contains("lapiz")) {
								plural = "";
							} else
								if (itemName.contains("glass")) {
									plural = "";
								} else
									if (itemName.contains("grass")) {
										plural = "";
									} else
										if (itemName.contains("dirt")) {
											plural = "";
										} else
											if (itemName.contains("potato")) {
												plural = "es";
											}
					sb.append(plural);
				}
			}
			EnchantmentStorageMeta esm = null;
			boolean useStorage = false;
			if (is.getType() == Material.ENCHANTED_BOOK) {
				esm = (EnchantmentStorageMeta) is.getItemMeta();
				useStorage = true;
			}
			if (is.getItemMeta().hasEnchants()
			        || (useStorage && esm.hasStoredEnchants())) {
				sb.append("(with ");
				Map<Enchantment, Integer> enchants = null;
				if (useStorage) {
					enchants = esm.getStoredEnchants();
				} else {
					enchants = is.getItemMeta().getEnchants();
				}
				Set<Enchantment> enchantKeys = enchants.keySet();
				for (Enchantment enchant : enchantKeys) {
					int level = enchants.get(enchant);
					sb.append(getNameFromEnchant(enchant) + " "
					        + Integer.toString(level) + ", ");
				}
				sb.delete(sb.length() - 2, sb.length());
				sb.append(")");
			}
			int lastIndex = items.length - 1;
			if (i < lastIndex - 1) {
				sb.append(", ");
			}
			if (i == lastIndex - 1) {
				sb.append(", " + plugin.AND_DEF + " ");
			}
		}
		return sb.toString();
	}

	public static Enchantment getEnchantEnumFromName(String name) {
		if (name.equalsIgnoreCase("looting")) {
			return Enchantment.LOOT_BONUS_MOBS;
		}
		if (name.equalsIgnoreCase("silk touch")) {
			return Enchantment.SILK_TOUCH;
		}
		if (name.equalsIgnoreCase("sharpness")) {
			return Enchantment.DAMAGE_ALL;
		}
		if (name.equalsIgnoreCase("bane of arthropods")) {
			return Enchantment.DAMAGE_ARTHROPODS;
		}
		if (name.equalsIgnoreCase("smite")) {
			return Enchantment.DAMAGE_UNDEAD;
		}
		if (name.equalsIgnoreCase("knockback")) {
			return Enchantment.KNOCKBACK;
		}
		if (name.equalsIgnoreCase("protection")) {
			return Enchantment.PROTECTION_ENVIRONMENTAL;
		}
		if (name.equalsIgnoreCase("fire protection")) {
			return Enchantment.PROTECTION_FIRE;
		}
		if (name.equalsIgnoreCase("blast protection")) {
			return Enchantment.PROTECTION_EXPLOSIONS;
		}
		if (name.equalsIgnoreCase("projectile protection")) {
			return Enchantment.PROTECTION_PROJECTILE;
		}
		if (name.equalsIgnoreCase("feather falling")) {
			return Enchantment.PROTECTION_FALL;
		}
		if (name.equalsIgnoreCase("respiration")) {
			return Enchantment.OXYGEN;
		}
		if (name.equalsIgnoreCase("aqua affinity")) {
			return Enchantment.WATER_WORKER;
		}
		if (name.equalsIgnoreCase("thorns")) {
			return Enchantment.THORNS;
		}
		if (name.equalsIgnoreCase("fire aspect")) {
			return Enchantment.FIRE_ASPECT;
		}
		if (name.equalsIgnoreCase("efficiency")) {
			return Enchantment.DIG_SPEED;
		}
		if (name.equalsIgnoreCase("unbreaking")) {
			return Enchantment.DURABILITY;
		}
		if (name.equalsIgnoreCase("fortune")) {
			return Enchantment.LOOT_BONUS_BLOCKS;
		}
		if (name.equalsIgnoreCase("power")) {
			return Enchantment.ARROW_DAMAGE;
		}
		if (name.equalsIgnoreCase("punch")) {
			return Enchantment.ARROW_KNOCKBACK;
		}
		if (name.equalsIgnoreCase("flame")) {
			return Enchantment.ARROW_FIRE;
		}
		if (name.equalsIgnoreCase("infinity")) {
			return Enchantment.ARROW_INFINITE;
		}
		if (name.equalsIgnoreCase("luck of the sea")) {
			return Enchantment.LUCK;
		}
		if (name.equalsIgnoreCase("lure")) {
			return Enchantment.LURE;
		} else {
			return null;
		}
	}

	public static String getNameFromEnchant(Enchantment enchant) {
		String name = "";
		if (enchant.equals(Enchantment.LOOT_BONUS_MOBS)) {
			name = "looting";
		} else
			if (enchant.equals(Enchantment.SILK_TOUCH)) {
				name = "silk touch";
			} else
				if (enchant.equals(Enchantment.DAMAGE_ALL)) {
					name = "sharpness";
				} else
					if (enchant.equals(Enchantment.DAMAGE_ARTHROPODS)) {
						name = "bane of arthropods";
					} else
						if (enchant.equals(Enchantment.DAMAGE_UNDEAD)) {
							name = "smite";
						} else
							if (enchant.equals(Enchantment.KNOCKBACK)) {
								name = "knockback";
							} else
								if (enchant
								        .equals(Enchantment.PROTECTION_ENVIRONMENTAL)) {
									name = "protection";
								} else
									if (enchant
									        .equals(Enchantment.PROTECTION_EXPLOSIONS)) {
										name = "blast protection";
									} else
										if (enchant
										        .equals(Enchantment.PROTECTION_FALL)) {
											name = "feather falling";
										} else
											if (enchant
											        .equals(Enchantment.PROTECTION_FIRE)) {
												name = "fire protection";
											} else
												if (enchant
												        .equals(Enchantment.PROTECTION_PROJECTILE)) {
													name = "projectile protection";
												} else
													if (enchant
													        .equals(Enchantment.OXYGEN)) {
														name = "respiration";
													} else
														if (enchant
														        .equals(Enchantment.WATER_WORKER)) {
															name = "aqua affinity";
														} else
															if (enchant
															        .equals(Enchantment.THORNS)) {
																name = "thorns";
															} else
																if (enchant
																        .equals(Enchantment.FIRE_ASPECT)) {
																	name = "fire aspect";
																} else
																	if (enchant
																	        .equals(Enchantment.DIG_SPEED)) {
																		name = "efficiency";
																	} else
																		if (enchant
																		        .equals(Enchantment.DURABILITY)) {
																			name = "unbreaking";
																		} else
																			if (enchant
																			        .equals(Enchantment.LOOT_BONUS_BLOCKS)) {
																				name = "fortune";
																			} else
																				if (enchant
																				        .equals(Enchantment.ARROW_DAMAGE)) {
																					name = "power";
																				} else
																					if (enchant
																					        .equals(Enchantment.ARROW_FIRE)) {
																						name = "flame";
																					} else
																						if (enchant
																						        .equals(Enchantment.ARROW_INFINITE)) {
																							name = "infinity";
																						} else
																							if (enchant
																							        .equals(Enchantment.ARROW_KNOCKBACK)) {
																								name = "punch";
																							} else
																								if (enchant
																								        .equals(Enchantment.LUCK)) {
																									name = "luck of the sea";
																								} else
																									if (enchant
																									        .equals(Enchantment.LURE)) {
																										name = "lure";
																									}
		return name;
	}

	public static Color getColorEnumFromName(String name) {
		if (name.equalsIgnoreCase("aqua")) {
			return Color.AQUA;
		}
		if (name.equalsIgnoreCase("black")) {
			return Color.BLACK;
		}
		if (name.equalsIgnoreCase("blue")) {
			return Color.BLUE;
		}
		if (name.equalsIgnoreCase("fuchsia")) {
			return Color.FUCHSIA;
		}
		if (name.equalsIgnoreCase("gray")) {
			return Color.GRAY;
		}
		if (name.equalsIgnoreCase("green")) {
			return Color.GREEN;
		}
		if (name.equalsIgnoreCase("lime")) {
			return Color.LIME;
		}
		if (name.equalsIgnoreCase("maroon")) {
			return Color.MAROON;
		}
		if (name.equalsIgnoreCase("navy")) {
			return Color.NAVY;
		}
		if (name.equalsIgnoreCase("olive")) {
			return Color.OLIVE;
		}
		if (name.equalsIgnoreCase("orange")) {
			return Color.ORANGE;
		}
		if (name.equalsIgnoreCase("purple")) {
			return Color.PURPLE;
		}
		if (name.equalsIgnoreCase("red")) {
			return Color.RED;
		}
		if (name.equalsIgnoreCase("silver")) {
			return Color.SILVER;
		}
		if (name.equalsIgnoreCase("teal")) {
			return Color.TEAL;
		}
		if (name.equalsIgnoreCase("white")) {
			return Color.WHITE;
		}
		if (name.equalsIgnoreCase("yellow")) {
			return Color.YELLOW;
		} else {
			return null;
		}
	}

	public static String completeName(String playername) {
		Player[] onlinePlayers = Bukkit.getOnlinePlayers();
		for (int i = 0; i < onlinePlayers.length; i++) {
			if (onlinePlayers[i].getName().toLowerCase()
			        .startsWith(playername.toLowerCase())) {
				return onlinePlayers[i].getName();
			}
		}
		OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
		for (int i = 0; i < offlinePlayers.length; i++) {
			if (offlinePlayers[i].getName().toLowerCase()
			        .startsWith(playername.toLowerCase())) {
				return offlinePlayers[i].getName();
			}
		}
		return null;
	}

	public static String helpMenu(CommandSender player) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " ?"
		        + ChatColor.GRAY + " - This help menu.\n");
		if (player.hasPermission("voteroulette.votecommand")) {
			sb.append(ChatColor.AQUA + "/vote" + ChatColor.GRAY
			        + " - Get the links to vote on.\n");
		}
		if (player.hasPermission("voteroulette.createawards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "create" + ChatColor.GRAY + " - Create a new award.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "edit" + ChatColor.GRAY + " - Edit a current award.\n");
		}
		if (player.hasPermission("voteroulette.deleteawards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "deletereward [#]" + ChatColor.GRAY
			        + " - Delete a reward.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "deletemilestone [#]" + ChatColor.GRAY
			        + " - Delete a milestone.\n");
		}
		if (player.hasPermission("voteroulette.edititems")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "setname [text]" + ChatColor.GRAY
			        + " - Set a custom name for item in hand.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "setlore [text]" + ChatColor.GRAY
			        + " - Set lore for item in hand.\n");
		}
		if (player.hasPermission("voteroulette.colors")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + "colors" + ChatColor.GRAY + " - See the colorcodes.\n");
		}
		if (player.hasPermission("voteroulette.viewrewards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.REWARDS_PURAL_DEF.toLowerCase() + ChatColor.GRAY
			        + " - See rewards you are eligible to get.\n");
		}
		if (player.hasPermission("voteroulette.viewmilestones")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.MILESTONE_PURAL_DEF.toLowerCase() + ChatColor.GRAY
			        + " - See milestones you are eligible to get.\n");
		}
		if (player.hasPermission("voteroulette.viewallawards")) {
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " ["
			        + plugin.REWARDS_PURAL_DEF.toLowerCase()
			        + "/"
			        + plugin.MILESTONE_PURAL_DEF.toLowerCase()
			        + "]"
			        + " -a"
			        + ChatColor.GRAY
			        + " - See all the awards, regardless of if you are eligable.\n");
		}
		if (player.hasPermission("voteroulette.top10")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.TOP_DEF + "10 "
			        + plugin.TOTAL_DEF.toLowerCase().replace(" ", "")
			        + ChatColor.GRAY
			        + " - See the top 10 players for total votes.\n");
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " "
			        + plugin.TOP_DEF
			        + "10 "
			        + plugin.VOTE_STREAK_DEF.toLowerCase().replace(" ", "")
			        + ChatColor.GRAY
			        + " - See the top 10 players for consecutive days voting.\n");
		}
		if (player.hasPermission("voteroulette.lastvote")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.LASTVOTE_DEF.toLowerCase() + ChatColor.GRAY
			        + " - Shows how long ago your last vote was.\n");
		}
		if (player.hasPermission("voteroulette.lastvoteothers")) {
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " "
			        + plugin.LASTVOTE_DEF.toLowerCase()
			        + " ["
			        + plugin.PLAYER_DEF.toLowerCase()
			        + "]"
			        + ChatColor.GRAY
			        + " - Shows how long ago the given players last vote was.\n");
		}
		if (player.hasPermission("voteroulette.viewstats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.STATS_DEF.toLowerCase() + ChatColor.GRAY
			        + " - See your voting stats.\n");
		}
		if (player.hasPermission("voteroulette.viewotherstats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.STATS_DEF.toLowerCase() + " ["
			        + plugin.PLAYER_DEF.toLowerCase() + "]" + ChatColor.GRAY
			        + " - See the stats of another player.\n");
		}
		if (player.hasPermission("voteroulette.editstats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.STATS_DEF.toLowerCase() + " ["
			        + plugin.PLAYER_DEF.toLowerCase() + "] "
			        + plugin.SETTOTAL_DEF + " [#]" + ChatColor.GRAY
			        + " - Set a players total votes.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.STATS_DEF.toLowerCase() + " ["
			        + plugin.PLAYER_DEF.toLowerCase() + "] "
			        + plugin.SETCYCLE_DEF + " [#]" + ChatColor.GRAY
			        + " - Set a players current vote cycle.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.STATS_DEF.toLowerCase() + " ["
			        + plugin.PLAYER_DEF.toLowerCase() + "] "
			        + plugin.SETSTREAK_DEF + " [#]" + ChatColor.GRAY
			        + " - Set a players current vote streak.\n");
		}
		sb.append(ChatColor.AQUA
		        + "/"
		        + plugin.DEFAULT_ALIAS
		        + " "
		        + plugin.CLAIM_DEF
		        + ChatColor.GRAY
		        + " - Tells you if you have any unclaimed rewards or\n milestones you received while offline.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
		        + plugin.CLAIM_DEF + " "
		        + plugin.REWARDS_PURAL_DEF.toLowerCase() + ChatColor.GRAY
		        + " - Lists any of your unclaimed rewards.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
		        + plugin.CLAIM_DEF + " "
		        + plugin.REWARDS_PURAL_DEF.toLowerCase() + " [#/"
		        + plugin.ALL_DEF + "]" + ChatColor.GRAY
		        + " - Gives you the reward with the given # or all of them.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
		        + plugin.CLAIM_DEF + " "
		        + plugin.MILESTONE_PURAL_DEF.toLowerCase() + ChatColor.GRAY
		        + " - Lists any of your unclaimed milestones.\n");
		sb.append(ChatColor.AQUA
		        + "/"
		        + plugin.DEFAULT_ALIAS
		        + " "
		        + plugin.CLAIM_DEF
		        + " "
		        + plugin.MILESTONE_PURAL_DEF
		        + " [#/"
		        + plugin.ALL_DEF
		        + "]"
		        + ChatColor.GRAY
		        + " - Gives you the milestone with the given # or all of them.\n");
		if (player.hasPermission("voteroulette.forcevote")) {
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " "
			        + plugin.FORCEVOTE_DEF
			        + " ["
			        + plugin.PLAYER_DEF
			        + "]"
			        + ChatColor.GRAY
			        + " - Make it as if the given player just voted, this will update their stats and give them an applicable reward/milestone.\n");
		}
		if (player.hasPermission("voteroulette.forceawards")) {
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " "
			        + plugin.FORCEREWARD_DEF
			        + "  "
			        + "[reward#] "
			        + "["
			        + plugin.PLAYER_DEF.toLowerCase()
			        + "]"
			        + ChatColor.GRAY
			        + " - Award a player the given reward. The number corresponds with the full rewards list.\n");
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " "
			        + plugin.FORCEMILESTONE_DEF
			        + "  "
			        + "[milestone#] "
			        + "["
			        + plugin.PLAYER_DEF.toLowerCase()
			        + "]"
			        + ChatColor.GRAY
			        + " - Award a player the given milestone. The number corresponds with the full milestones list.\n");
		}
		if (player.hasPermission("voteroulette.wipestats")) {
			sb.append(ChatColor.AQUA
			        + "/"
			        + plugin.DEFAULT_ALIAS
			        + " "
			        + plugin.WIPESTATS_DEF
			        + " ["
			        + plugin.PLAYER_DEF
			        + "/"
			        + plugin.ALL_DEF
			        + "] ["
			        + plugin.STATS_DEF
			        + "/"
			        + plugin.ALL_DEF
			        + "]"
			        + ChatColor.GRAY
			        + " - Wipes the given stat (or all stats) of a particular player (or all of them).\n");
		}
		if (player.hasPermission("voteroulette.admin")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " "
			        + plugin.RELOAD_DEF + ChatColor.GRAY
			        + " - Reloads the config file.\n");
		}
		return sb.toString().toLowerCase();
	}

	public static void showAwardGUI(Award award, Player p, int awardNumber) {
		Utils.showAwardGUI(award, p, awardNumber, false);
	}

	public static void showAwardGUI(Award award, Player p, int awardNumber,
	        boolean showPlayers) {
		Reward reward = null;
		Milestone milestone = null;
		String awardType = "";
		if (award.getAwardType() == AwardType.REWARD) {
			reward = (Reward) award;
			awardType = plugin.REWARD_DEF;
		}
		if (award.getAwardType() == AwardType.MILESTONE) {
			milestone = (Milestone) award;
			awardType = plugin.MILESTONE_DEF;
		}
		String name = ChatColor.DARK_BLUE + award.getName() + " "
		        + ChatColor.BLACK + awardType;
		if (name.length() > 32) {
			name = name.substring(0, 28) + "...";
		}
		int multOf9 = 9;
		int req = award.getRequiredSlots();
		if (award.hasCurrency()) {
			req++;
		}
		if (award.hasCommands()) {
			req++;
		}
		if (award.hasXpLevels()) {
			req++;
		}
		if ((award.getAwardType() == AwardType.REWARD && (reward.hasWebsites() || reward
		        .hasVoteStreak()))
		        || award.hasChance()
		        || award.hasWorlds()
		        || award.hasReroll()
		        || award.hasDescription()
		        || ((plugin.SHOW_COMMANDS_IN_AWARD || showPlayers) && (award
		                .hasPlayers() || award.hasPermissionGroups()))
		        || award.getAwardType() == AwardType.MILESTONE) {
			req++;
		}
		while (req > multOf9) {
			multOf9 += 9;
		}
		Inventory i = Bukkit.createInventory(p, multOf9, name);
		ItemStack[] items = Utils.updateLoreAndCustomNames(p.getName(),
		        award.getItems());
		for (ItemStack item : items) {
			i.addItem(item);
		}
		if (award.hasXpLevels()) {
			ItemStack xp = new ItemStack(Material.EXP_BOTTLE);
			ItemMeta itemMeta = xp.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW
			        + Integer.toString(award.getXpLevels()) + ChatColor.RESET
			        + " " + plugin.XPLEVELS_DEF);
			xp.setItemMeta(itemMeta);
			i.addItem(xp);
		}
		if (award.hasCurrency()) {
			ItemStack xp = new ItemStack(Material.GOLD_INGOT);
			ItemMeta itemMeta = xp.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW
			        + Double.toString(award.getCurrency()) + ChatColor.RESET
			        + " " + plugin.CURRENCY_PURAL_DEF);
			xp.setItemMeta(itemMeta);
			i.addItem(xp);
		}
		if (award.hasCommands()) {
			ItemStack paper = new ItemStack(Material.COMMAND);
			ItemMeta itemMeta = paper.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW + "Runs commands.");
			List<String> lore = new ArrayList<String>();
			if (plugin.SHOW_COMMANDS_IN_AWARD) {
				List<String> commands = award.getCommands();
				for (String command : commands) {
					if (command.contains("(")) {
						int index = command.indexOf(") ");
						if (command.charAt(index + 2) != '/') {
							command = command.substring(0, index + 2)
							        + "/"
							        + command.substring(index + 2,
							                command.length());
						}
					} else {
						if (!command.contains("/")) {
							command = "/" + command;
						}
					}
					lore.add(ChatColor.GRAY + command);
				}
			}
			itemMeta.setLore(lore);
			paper.setItemMeta(itemMeta);
			i.addItem(paper);
		}
		if ((award.getAwardType() == AwardType.REWARD && (reward.hasWebsites() || reward
		        .hasVoteStreak()))
		        || award.hasChance()
		        || award.hasWorlds()
		        || award.hasReroll()
		        || award.hasDescription()
		        || ((plugin.SHOW_COMMANDS_IN_AWARD || showPlayers) && (award
		                .hasPlayers() || award.hasPermissionGroups()))
		        || award.getAwardType() == AwardType.MILESTONE) {
			ItemStack sign = new ItemStack(Material.SIGN);
			ItemMeta itemMeta = sign.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE
			        + "Details");
			List<String> lore = new ArrayList<String>();
			if (award.getAwardType() == AwardType.MILESTONE) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.VOTES_DEF + ": "
				        + ChatColor.DARK_AQUA);
				if (milestone.isRecurring()) {
					sb.append(plugin.EVERY_DEF + " ");
				}
				sb.append(milestone.getVotes());
				lore.add(sb.toString());
			}
			if (award.getAwardType() == AwardType.REWARD
			        && reward.hasVoteStreak()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.VOTE_STREAK_DEF + ": "
				        + ChatColor.DARK_AQUA + reward.getVoteStreak());
				if (reward.hasVoteStreakModifier()) {
					VoteStreakModifier vsm = reward.getVoteStreakModifier();
					sb.append(" "
					        + vsm.toString().toLowerCase().replace("_", " "));
				}
				sb.append(" ");
				if (reward.getVoteStreak() > 1) {
					sb.append(plugin.DAY_PLURAL_DEF);
				} else {
					sb.append(plugin.DAY_DEF);
				}
				lore.add(sb.toString());
			}
			if (award.hasChance()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.CHANCE_DEF + ": "
				        + ChatColor.DARK_AQUA);
				sb.append(award.getChanceMin() + " in " + award.getChanceMax());
				lore.add(sb.toString());
			}
			if (award.hasReroll()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + "Reroll: " + ChatColor.DARK_AQUA);
				sb.append(award.getReroll());
				lore.add(sb.toString());
			}
			if (award.hasWorlds()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.WORLDS_DEF + ": "
				        + ChatColor.DARK_AQUA);
				sb.append(Utils.worldsString(award.getWorlds()));
				String worldStr = sb.toString();
				String[] words = worldStr.split(" ");
				String tempStr = "";
				for (String word : words) {
					tempStr += ChatColor.DARK_AQUA + word + " ";
					if (tempStr.length() > 25) {
						lore.add(tempStr);
						tempStr = "";
					}
				}
				lore.add(tempStr);
			}
			if (plugin.SHOW_PLAYER_AND_GROUPS || showPlayers) {
				if (award.hasPermissionGroups()) {
					StringBuilder sb = new StringBuilder();
					sb.append(ChatColor.GOLD + "PermGroups: "
					        + ChatColor.DARK_AQUA);
					for (String website : award.getPermGroups()) {
						sb.append(website + ", ");
					}
					sb.delete(sb.length() - 2, sb.length() - 1);
					String worldStr = sb.toString();
					String[] words = worldStr.split(" ");
					String tempStr = "";
					for (String word : words) {
						tempStr += ChatColor.DARK_AQUA + word + " ";
						if (tempStr.length() > 25) {
							lore.add(tempStr);
							tempStr = "";
						}
					}
					lore.add(tempStr);
				}
				if (award.hasPlayers()) {
					StringBuilder sb = new StringBuilder();
					sb.append(ChatColor.GOLD + "Players: "
					        + ChatColor.DARK_AQUA);
					for (String website : award.getPlayers()) {
						sb.append(website + ", ");
					}
					sb.delete(sb.length() - 2, sb.length() - 1);
					String worldStr = sb.toString();
					String[] words = worldStr.split(" ");
					String tempStr = "";
					for (String word : words) {
						tempStr += ChatColor.DARK_AQUA + word + " ";
						if (tempStr.length() > 25) {
							lore.add(tempStr);
							tempStr = "";
						}
					}
					lore.add(tempStr);
				}
			}
			if ((award.getAwardType() == AwardType.REWARD && reward
			        .hasWebsites())) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.WEBSITES_DEF + ": "
				        + ChatColor.DARK_AQUA);
				for (String website : reward.getWebsites()) {
					sb.append(website + ", ");
				}
				sb.delete(sb.length() - 2, sb.length() - 1);
				String worldStr = sb.toString();
				String[] words = worldStr.split(" ");
				String tempStr = "";
				for (String word : words) {
					tempStr += ChatColor.DARK_AQUA + word + " ";
					if (tempStr.length() > 25) {
						lore.add(tempStr);
						tempStr = "";
					}
				}
				lore.add(tempStr);
			}
			if (award.hasDescription()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + "Description: "
				        + ChatColor.DARK_AQUA + award.getDescription());
				String[] words = sb.toString().split(" ");
				String tempStr = "";
				for (String word : words) {
					tempStr += ChatColor.DARK_AQUA + word + " ";
					if (tempStr.length() > 25) {
						lore.add(tempStr);
						tempStr = "";
					}
				}
				lore.add(tempStr);
			}
			itemMeta.setLore(lore);
			sign.setItemMeta(itemMeta);
			i.addItem(sign);

		}
		if (award.getAwardType() == AwardType.REWARD) {
			VoteRoulette.lookingAtRewards.put(p, awardNumber);
		} else {
			VoteRoulette.lookingAtMilestones.put(p, awardNumber);
		}
		p.openInventory(i);
	}

	public static Stat getVoteStatFromStr(String input) {
		if (input.equalsIgnoreCase("all")) {
			return Stat.ALL;
		} else
			if (input.equalsIgnoreCase("currentvotecycle")) {
				return Stat.CURRENT_VOTE_CYCLE;
			} else
				if (input.equalsIgnoreCase("votecycle")) {
					return Stat.CURRENT_VOTE_CYCLE;
				} else
					if (input.equalsIgnoreCase("votestreak")) {
						return Stat.CURRENT_VOTE_STREAK;
					} else
						if (input.equalsIgnoreCase("currentvotestreak")) {
							return Stat.CURRENT_VOTE_STREAK;
						} else
							if (input.equalsIgnoreCase("longestvotestreak")) {
								return Stat.LONGEST_VOTE_STREAK;
							} else
								if (input.equalsIgnoreCase("lifetimevotes")) {
									return Stat.LIFETIME_VOTES;
								} else
									if (input.equalsIgnoreCase("totalvotes")) {
										return Stat.LIFETIME_VOTES;
									} else
										if (input.equalsIgnoreCase("lastvote")) {
											return Stat.LAST_VOTE;
										} else
											if (input
											        .equalsIgnoreCase("unclaimedrewards")) {
												return Stat.UNCLAIMED_REWARDS;
											} else
												if (input
												        .equalsIgnoreCase("unclaimedmilestones")) {
													return Stat.UNCLAIMED_MILSTONES;
												} else {
													return null;
												}

	}

	public static void randomFireWork(Location loc) {
		Firework fw = (Firework) loc.getWorld().spawnEntity(loc,
		        EntityType.FIREWORK);

		FireworkMeta fwm = fw.getFireworkMeta();

		Random r = new Random();

		int rt = r.nextInt(4) + 1;
		FireworkEffect.Type type = FireworkEffect.Type.BALL;
		if (rt == 1)
			type = FireworkEffect.Type.BALL;
		if (rt == 2)
			type = FireworkEffect.Type.BALL_LARGE;
		if (rt == 3)
			type = FireworkEffect.Type.BURST;
		if (rt == 4)
			type = FireworkEffect.Type.CREEPER;
		if (rt == 5)
			type = FireworkEffect.Type.STAR;

		int r1i = r.nextInt(17) + 1;
		int r2i = r.nextInt(17) + 1;
		Color c1 = getColor(r1i);
		Color c2 = getColor(r2i);

		FireworkEffect effect = FireworkEffect.builder()
		        .flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type)
		        .trail(r.nextBoolean()).build();

		fwm.addEffect(effect);

		int rp = r.nextInt(2) + 1;
		fwm.setPower(rp);

		fw.setFireworkMeta(fwm);
	}

	private static Color getColor(int i) {
		Color c = null;
		if (i == 1) {
			c = Color.AQUA;
		}
		if (i == 2) {
			c = Color.BLACK;
		}
		if (i == 3) {
			c = Color.BLUE;
		}
		if (i == 4) {
			c = Color.FUCHSIA;
		}
		if (i == 5) {
			c = Color.GRAY;
		}
		if (i == 6) {
			c = Color.GREEN;
		}
		if (i == 7) {
			c = Color.LIME;
		}
		if (i == 8) {
			c = Color.MAROON;
		}
		if (i == 9) {
			c = Color.NAVY;
		}
		if (i == 10) {
			c = Color.OLIVE;
		}
		if (i == 11) {
			c = Color.ORANGE;
		}
		if (i == 12) {
			c = Color.PURPLE;
		}
		if (i == 13) {
			c = Color.RED;
		}
		if (i == 14) {
			c = Color.SILVER;
		}
		if (i == 15) {
			c = Color.TEAL;
		}
		if (i == 16) {
			c = Color.WHITE;
		}
		if (i == 17) {
			c = Color.YELLOW;
		}
		return c;
	}

	public static String getFancyLink(String input) {
		Pattern p = Pattern.compile("\\{(.+)>(.+)\\}");
		Matcher m = p.matcher(input);
		if (m.find()) {
			return m.group();
		}
		return "";
	}

	public static boolean awardHasOptions(Award award) {
		if (award.getAwardType() == AwardType.REWARD) {
			Reward reward = (Reward) award;
			if (reward.hasOptions())
				return true;
			return false;
		} else {
			Milestone milestone = (Milestone) award;
			if (milestone.hasOptions())
				return true;
			return false;
		}
	}

	public static String concatListToString(List<String> strings) {
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string + ", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		return sb.toString();
	}
}
