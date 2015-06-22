package com.mythicacraft.voteroulette.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
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
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.Voter.Stat;
import com.mythicacraft.voteroulette.api.PlayerEarnedAwardEvent;
import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.DelayedCommand;
import com.mythicacraft.voteroulette.awards.ItemPrize;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.awards.Reward.VoteStreakModifier;
import com.mythicacraft.voteroulette.stats.BoardReset;
import com.mythicacraft.voteroulette.stats.VoterStat;
import com.mythicacraft.voteroulette.stats.VoterStatSheet.StatType;
import com.mythicacraft.voteroulette.utils.InteractiveMessageAPI.InteractiveMessageElement.ClickEvent;

public class Utils {

	/**
	 * TODO: Tame this beast
	 */

	private static VoteRoulette plugin;

	public Utils(VoteRoulette instance) {
		plugin = instance;
	}

	public static void debugMessage(String message) {
		if (VoteRoulette.DEBUG) {
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
				im.setDisplayName(im.getDisplayName().replace("%player%", playerName));
			}
			if (item.getType() == Material.SKULL_ITEM && item.getDurability() == 3 /* playerhead */) {
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

	public static ItemStack[] updateLoreAndCustomNamesForItemPrizes(
			Voter voter, List<ItemPrize> items) {
		String playerName = voter.getPlayerName();
		List<ItemStack> calcItems = new ArrayList<ItemStack>();
		for (ItemPrize itemP : items) { // go through all itemprize
			// create lore for each itemprize
			ItemMeta im = itemP.getItemMeta().clone();
			if (im.hasLore()) {
				List<String> oldLore = im.getLore();
				List<String> newLore = new ArrayList<String>();
				for (String line : oldLore) {
					newLore.add(line.replace("%player%", playerName));
				}
				im.setLore(newLore);
			}
			if (itemP.hasVariableAmount()) {
				List<String> lore = im.getLore();
				if (lore == null) {
					lore = new ArrayList<String>();
				}
				lore.add(" ");
				lore.add(ChatColor.GRAY + "This item has a variable amount.");
				lore.add(ChatColor.GRAY + "At this moment, you would get:");
				lore.add(ChatColor.GRAY + Integer.toString(itemP.getCalculatedAmount(voter)));
				if (VoteRoulette.SHOW_VARIABLE_AMOUNT_EXPRESSION) {
					lore.add(ChatColor.GRAY + "From:");
					lore.add(ChatColor.GRAY + itemP.getFormattedVariableString(voter));
					lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + itemP.getAmountExpression());
				}
				im.setLore(lore);
			}
			if (im.hasDisplayName()) {
				im.setDisplayName(im.getDisplayName().replace("%player%", playerName));
			}
			if (itemP.getType() == Material.SKULL_ITEM && itemP.getDurability() == 3 /* playerhead */) {
				SkullMeta sim = (SkullMeta) im;
				if (sim.hasOwner()) {
					sim.setOwner(sim.getOwner().replace("%player%", playerName));
					im = sim;
				}
			}
			for (ItemStack item : itemP.getCalculatedItem(voter)) { // get
																	// calcumated
																	// itemstacks
																	// for each
																	// itemstack
																	// in
																	// itemprize
				// clone item, apply lore, add to calcitems list
				ItemStack itemS = item.clone();
				itemS.setItemMeta(im);
				calcItems.add(itemS);
			}
		}
		ItemStack[] itemStacks = new ItemStack[calcItems.size()];
		calcItems.toArray(itemStacks);
		return itemStacks;
	}

	public static void playerEarnAward(Voter voter, Award award, String website) {
		PlayerEarnedAwardEvent event = new PlayerEarnedAwardEvent(voter.getPlayerName(), award);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			VoteRoulette.getAwardManager().administerAwardContents(event.getAward(), voter);
			if (!(website.equals("forcevote") || website.equals("reroll"))) {
				Utils.broadcastMessageToServer(Utils.getServerMessageWithAward(event.getAward(), voter, website), event.getPlayerName());
			}
		} else {
			Utils.debugMessage("Event stopped by another pluggin. Cancelling award process.");
		}
	}

	@SuppressWarnings("deprecation")
	public static void showTopScoreboard(final Player player, StatType stat) {
		Utils.debugMessage("opening top 10 in scorebord.");
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		Scoreboard board = manager.getNewScoreboard();
		board.registerNewObjective("TopVotes", "dummy");
		Objective objective = board.getObjective("TopVotes");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		if (stat == StatType.TOTAL_VOTES) {
			Utils.debugMessage("stat type: total");
			objective.setDisplayName(ChatColor.AQUA + plugin.TOTAL_VOTES_DEF);
			List<VoterStat> topStats = VoteRoulette.getStatsManager().getTopLifetimeVotes();
			if (topStats == null || topStats.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Error: stats are empty.");
				return;
			}
			Utils.debugMessage("creating scorebord");
			int count = 0;
			for (VoterStat vs : topStats) {
				if (count > 10)
					break;
				String name = vs.getPlayerName();
				if (name.length() > 16) {
					name = name.substring(0, 11) + "...";
				}
				Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
				score.setScore(vs.getStatCount());
				count++;
			}
		}
		if (stat == StatType.LONGEST_VOTE_STREAKS) {
			Utils.debugMessage("stat type: streak");
			objective.setDisplayName(ChatColor.AQUA + plugin.LONGEST_VOTE_STREAK_DEF);
			List<VoterStat> topStats = VoteRoulette.getStatsManager().getTopLongestVotestreaks();
			if (topStats == null || topStats.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Error: stats are empty.");
				return;
			}
			Utils.debugMessage("creating scorebord");
			int count = 0;
			for (VoterStat vs : topStats) {
				if (count > 10)
					break;
				String name = vs.getPlayerName();
				if (name.length() > 16) {
					name = name.substring(0, 11) + "...";
				}
				Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
				score.setScore(vs.getStatCount());
				count++;
			}
		}
		if (stat == StatType.CURRENT_MONTH_VOTES) {
			Utils.debugMessage("stat type: current month");
			objective.setDisplayName(ChatColor.AQUA + plugin.CURRENT_MONTHS_VOTES_DEF);
			List<VoterStat> topStats = VoteRoulette.getStatsManager().getTopCurrentMonthVotes();
			if (topStats == null || topStats.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Error: stats are empty.");
				return;
			}
			Utils.debugMessage("creating scorebord");
			int count = 0;
			for (VoterStat vs : topStats) {
				if (count > 10)
					break;
				String name = vs.getPlayerName();
				if (name.length() > 16) {
					name = name.substring(0, 11) + "...";
				}
				Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
				score.setScore(vs.getStatCount());
				count++;
			}
		}
		if (stat == StatType.PREVIOUS_MONTH_VOTES) {
			Utils.debugMessage("stat type: previous month");
			objective.setDisplayName(ChatColor.AQUA + plugin.PREVIOUS_MONTHS_VOTES_DEF);
			List<VoterStat> topStats = VoteRoulette.getStatsManager().getTopPreviousMonthVotes();
			if (topStats == null || topStats.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Error: stats are empty.");
				return;
			}
			Utils.debugMessage("creating scorebord");
			int count = 0;
			for (VoterStat vs : topStats) {
				if (count > 10)
					break;
				String name = vs.getPlayerName();
				if (name.length() > 16) {
					name = name.substring(0, 11) + "...";
				}
				Score score = objective.getScore(Bukkit.getOfflinePlayer(name));
				score.setScore(vs.getStatCount());
				count++;
			}
		}
		Utils.debugMessage("showing scoreboard for 5 secs");
		player.setScoreboard(board);
		// cancel button selection after 5 seconds
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(plugin, new BoardReset(player, board), 300L);
	}

	public static void showTopInChat(Player player, StatType stat) {
		List<VoterStat> topStats = null;
		String typeText = "";
		if (stat == StatType.TOTAL_VOTES) {
			topStats = VoteRoulette.getStatsManager().getTopLifetimeVotes();
			typeText = plugin.TOTAL_VOTES_DEF;
		}
		if (stat == StatType.LONGEST_VOTE_STREAKS) {
			topStats = VoteRoulette.getStatsManager().getTopLongestVotestreaks();
			typeText = plugin.LONGEST_VOTE_STREAK_DEF;
		}
		if (stat == StatType.CURRENT_MONTH_VOTES) {
			topStats = VoteRoulette.getStatsManager().getTopCurrentMonthVotes();
			typeText = plugin.CURRENT_MONTHS_VOTES_DEF;
		}
		if (stat == StatType.PREVIOUS_MONTH_VOTES) {
			topStats = VoteRoulette.getStatsManager().getTopPreviousMonthVotes();
			typeText = plugin.PREVIOUS_MONTHS_VOTES_DEF;
		}
		if (topStats == null || topStats.isEmpty()) {
			player.sendMessage(ChatColor.RED + "Error: stats are empty.");
			return;
		}
		Collections.sort(topStats, new Comparator<VoterStat>() {
			public int compare(VoterStat v1, VoterStat v2) {
				return v2.getStatCount() - v1.getStatCount();
			}
		});
		if (topStats != null) {
			player.sendMessage(ChatColor.DARK_AQUA + "------[" + ChatColor.GREEN + typeText + ChatColor.DARK_AQUA + "]------");
			int count = 1;
			String topNumber = "";
			int counter = 0;
			for (VoterStat vs : topStats) {
				if (counter > 10)
					break;
				String statCount = Integer.toString(vs.getStatCount());
				if (count == 1) {
					topNumber = statCount;
				}
				int lengthDif = topNumber.length() - statCount.length();
				String message = "";
				for (int i = 0; i < lengthDif; i++) {
					message += " ";
				}
				message += ChatColor.GOLD + statCount + " " + ChatColor.WHITE + vs.getPlayerName();
				player.sendMessage(message);
				count++;
				counter++;
			}
		}
	}

	/*
	 * public static String unclaimedMessage(Voter voter) { int
	 * unclaimedRewardsCount = voter.getUnclaimedRewardCount(); int
	 * unclaimedMilestonesCount = voter.getUnclaimedMilestoneCount();
	 * if(unclaimedRewardsCount > 0) {
	 * sender.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%",
	 * plugin.REWARDS_PURAL_DEF.toLowerCase()).replace("%amount%",
	 * Integer.toString(unclaimedRewardsCount)).replace("%command%", "/" +
	 * plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF.toLowerCase() + " " +
	 * plugin.REWARDS_PURAL_DEF.toLowerCase())); } else {
	 * sender.sendMessage(plugin
	 * .NO_UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%",
	 * plugin.REWARDS_PURAL_DEF.toLowerCase())); } if(unclaimedMilestonesCount >
	 * 0) {
	 * sender.sendMessage(plugin.UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%",
	 * plugin.MILESTONE_PURAL_DEF.toLowerCase()).replace("%amount%",
	 * Integer.toString(unclaimedMilestonesCount)).replace("%command%", "/" +
	 * plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF.toLowerCase() + " " +
	 * plugin.MILESTONE_PURAL_DEF.toLowerCase())); } else {
	 * sender.sendMessage(plugin
	 * .NO_UNCLAIMED_AWARDS_NOTIFICATION.replace("%type%",
	 * plugin.MILESTONE_PURAL_DEF.toLowerCase())); } }
	 */

	public static void sendMessageToPlayer(String message, String playerName) {
		Player player = Bukkit.getPlayerExact(playerName);
		if (player == null)
			return;
		player.sendMessage(message);
	}

	@SuppressWarnings("unchecked")
	public static void broadcastMessageToServer(String message,
			String exemptPlayer) {
		if (plugin.BROADCAST_TO_SERVER) {
			if (plugin.ONLY_BROADCAST_ONLINE && !Utils.playerIsOnline(exemptPlayer))
				return;
			if (plugin.USE_BROADCAST_COOLDOWN) {
				if (VoteRoulette.cooldownPlayers.contains(exemptPlayer)) {
					Utils.debugMessage(exemptPlayer + " is in broadcast cooldown.");
					return;
				} else {
					Utils.debugMessage(exemptPlayer + " is not in broadcast cooldown.");
				}
			}
			try {
				if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
					for (Player player : ((Collection<? extends Player>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]))) {
						if (player.getName().equals(exemptPlayer))
							continue;
						player.sendMessage(message);
					}
				else
					for (Player player : ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]))) {
						if (player.getName().equals(exemptPlayer))
							continue;
						player.sendMessage(message);
					}
			} catch (NoSuchMethodException ex) {
			} // can never happen
			catch (InvocationTargetException ex) {
			} // can also never happen
			catch (IllegalAccessException ex) {
			} // can still never happen

			if (plugin.USE_BROADCAST_COOLDOWN) {
				if (!VoteRoulette.cooldownPlayers.contains(exemptPlayer)) {
					VoteRoulette.cooldownPlayers.add(exemptPlayer);
					Utils.debugMessage("Put " + exemptPlayer + " in broadcast cooldown.");
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

	public static String getServerMessageWithAward(Award award, Voter voter,
			String website) {
		String message = getServerAwardMessage(plugin.SERVER_BROADCAST_MESSAGE, award, voter);
		message = message.replace("%site%", website);
		return message;
	}

	public static String getServerAwardMessage(String awardMessage,
			Award award, Voter voter) {
		awardMessage = awardMessage.replace("%name%", award.getName());
		awardMessage = awardMessage.replace("%player%", voter.getPlayerName());
		awardMessage = awardMessage.replace("%server%", Bukkit.getServerName());

		if (award.getAwardType() == AwardType.MILESTONE) {
			awardMessage = awardMessage.replace("%type%", plugin.MILESTONE_DEF.toLowerCase());
		} else {
			awardMessage = awardMessage.replace("%type%", plugin.REWARD_DEF.toLowerCase());
		}
		awardMessage = awardMessage.replace("%prizes%", getAwardPrizesString(award, voter));
		return awardMessage;
	}

	public static String getAwardMessage(String awardMessage, Award award,
			Voter voter) {
		if (!award.hasMessage()) {
			awardMessage = awardMessage.replace("%name%", award.getName());
			awardMessage = awardMessage.replace("%player%", voter.getPlayerName());
			awardMessage = awardMessage.replace("%server%", Bukkit.getServerName());

			if (award.getAwardType() == AwardType.MILESTONE) {
				awardMessage = awardMessage.replace("%type%", plugin.MILESTONE_DEF.toLowerCase());
			} else {
				awardMessage = awardMessage.replace("%type%", plugin.REWARD_DEF.toLowerCase());
			}
			awardMessage = awardMessage.replace("%prizes%", getAwardPrizesString(award, voter));
			return awardMessage;
		} else {
			return Utils.transcribeColorCodes(award.getMessage().replace("%player%", voter.getPlayerName()));
		}
	}

	public static List<Award> convertRewardListToAward(List<Reward> rewards) {
		List<Award> awards = new ArrayList<Award>();
		for (Reward reward : rewards) {
			awards.add(reward);
		}
		return awards;
	}

	public static List<Award> convertMilestoneListToAward(
			List<Milestone> milestones) {
		List<Award> awards = new ArrayList<Award>();
		for (Milestone milestone : milestones) {
			awards.add(milestone);
		}
		return awards;
	}

	public static String getSummarizedAwardsMessage(List<Award> awards,
			Voter voter) {

		HashMap<Award, Integer> awardCounts = new HashMap<Award, Integer>();
		for (Award award : awards) {
			if (!awardCounts.containsKey(award)) {
				awardCounts.put(award, 1);
			} else {
				awardCounts.put(award, awardCounts.get(award) + 1);
			}
		}
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		for (Award award : awardCounts.keySet()) {
			int count = awardCounts.get(award);
			if (count > 1) {
				sb.append(award.getName() + "(x" + count + ")");
			} else {
				sb.append(award.getName());
			}
			int lastIndex = awardCounts.size() - 1;
			if (counter < lastIndex - 1) {
				sb.append(", ");
			}
			if (counter == lastIndex - 1) {
				sb.append(", " + plugin.AND_DEF + " ");
			}
			counter++;
		}
		String awardsListStr = sb.toString();
		String summerizeMessage = plugin.PLAYER_AWARDS_SUMMARY_MESSAGE;

		summerizeMessage = summerizeMessage.replace("%names%", awardsListStr).replace("%player%", voter.getPlayerName()).replace("%server%", Bukkit.getServerName()).replace("%prizes%", getSummarizedAwardPrizesString(awards, voter));

		if (awards.get(0) instanceof Milestone) {
			if (awards.size() > 1) {
				summerizeMessage = summerizeMessage.replace("%type%", plugin.MILESTONE_PURAL_DEF.toLowerCase());
			} else {
				summerizeMessage = summerizeMessage.replace("%type%", plugin.MILESTONE_DEF.toLowerCase());
			}
		} else {
			if (awards.size() > 0) {
				summerizeMessage = summerizeMessage.replace("%type%", plugin.REWARDS_PURAL_DEF.toLowerCase());
			} else {
				summerizeMessage = summerizeMessage.replace("%type%", plugin.REWARD_DEF.toLowerCase());
			}
		}
		return summerizeMessage;
	}

	private static String getSummarizedAwardPrizesString(List<Award> awards,
			Voter voter) {
		Inventory tempInv = Bukkit.createInventory(null, 999);
		double totalMoney = 0.0;
		int totalXp = 0;
		StringBuilder sb = new StringBuilder();
		for (Award award : awards) {
			if (award.hasDescription()) {
				sb.append(award.getDescription());
			} else {
				if (award.hasCurrency()) {
					totalMoney += award.getCurrency();
				}
				if (award.hasXpLevels()) {
					totalXp += award.getXpLevels();
				}
				if (award.hasItems()) {
					for (ItemStack item : award.getItems(voter)) {
						tempInv.addItem(item);
					}
				}
			}
		}
		if (totalMoney > 0) {
			sb.append(plugin.CURRENCY_SYMBOL + totalMoney);
		}
		if (totalXp > 0) {
			if (totalMoney > 0) {
				sb.append(", ");
			}
			sb.append(totalXp + " " + plugin.XPLEVELS_DEF.toLowerCase());
		}
		if (tempInv.getContents().length > 0) {
			if (totalMoney > 0 || totalXp > 0) {
				sb.append(", ");
			}
			sb.append(Utils.getItemListSentance(stripNullIndexes(tempInv.getContents())));
		}
		return sb.toString();
	}

	private static ItemStack[] stripNullIndexes(ItemStack[] items) {
		List<ItemStack> cleanItemList = new ArrayList<ItemStack>();
		for (ItemStack item : items) {
			if (item == null)
				continue;
			cleanItemList.add(item);
		}
		ItemStack[] cleanItemArray = new ItemStack[cleanItemList.size()];
		cleanItemList.toArray(cleanItemArray);
		return cleanItemArray;
	}

	private static String getAwardPrizesString(Award award, Voter voter) {
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
				sb.append(award.getXpLevels() + " " + plugin.XPLEVELS_DEF.toLowerCase());
			}
			if (award.hasItems()) {
				if (award.hasCurrency() || award.hasXpLevels()) {
					sb.append(", ");
				}
				sb.append(Utils.getItemListSentance(award.getItems(voter)));
			}
		}
		return sb.toString();
	}

	public static List<String> getBlacklistPlayers() {
		List<String> blacklistStr = plugin.getConfig().getStringList("blacklistedPlayers");
		return blacklistStr;
	}

	public static String searchCacheForName(UUID id) {
		ConfigAccessor uuidCache = new ConfigAccessor("data" + File.separator + "UUIDCache.yml");
		if (uuidCache.getConfig().contains(id.toString())) {
			return uuidCache.getConfig().getString(id.toString());
		}
		return null;
	}

	public static UUID searchCacheForID(String playerName) {
		ConfigAccessor uuidCache = new ConfigAccessor("data" + File.separator + "UUIDCache.yml");
		if (uuidCache.getConfig().contains(playerName)) {
			return UUID.fromString(uuidCache.getConfig().getString(playerName));
		}
		return null;
	}

	public static void saveKnownNameUUID(String playerName, UUID id) {
		ConfigAccessor uuidCache = new ConfigAccessor("data" + File.separator + "UUIDCache.yml");
		uuidCache.getConfig().set(id.toString(), playerName);
		uuidCache.getConfig().set(playerName, id.toString());
		uuidCache.saveConfig();
	}

	public static void saveKnownWebsite(String website) {
		if (website.equalsIgnoreCase("forcevote"))
			return;
		ConfigAccessor websiteFile = new ConfigAccessor("data" + File.separator + "known websites.yml");
		List<String> websites = websiteFile.getConfig().getStringList("known-websites");
		if (websites != null) {
			if (!websites.contains(website)) {
				websites.add(website);
				websiteFile.getConfig().set("known-websites", websites);
				websiteFile.saveConfig();
			}
		}
	}

	public static String getKnownWebsites() {
		ConfigAccessor websiteFile = new ConfigAccessor("data" + File.separator + "known websites.yml");
		List<String> websites = websiteFile.getConfig().getStringList("known-websites");
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
		message = message.replace("%black%", "&0").replace("%darkblue%", "&1").replace("%darkgreen%", "&2").replace("%darkaqua%", "&3").replace("%darkred%", "&4").replace("%purple%", "&5").replace("%gold%", "&6").replace("%grey%", "&7").replace("%darkgrey%", "&8").replace("%blue%", "&9").replace("%green%", "&a").replace("%aqua%", "&b").replace("%red%", "&c").replace("%pink%", "&d").replace("%yellow%", "&e").replace("%white%", "&f").replace("%bold%", "&l").replace("%strikethrough%", "&m").replace("%underline%", "&n").replace("%italic%", "&o").replace("%reset%", "&r").replace("%magic%", "&k");
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
		List<String> blacklistStr = plugin.getConfig().getStringList("blacklistedWorlds");
		if (blacklistStr.contains(worldName))
			return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	public static boolean playerIsOnline(String playerName) {
		try {
			if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
				for (Player player : ((Collection<? extends Player>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]))) {
					if (player.getName().equals(playerName))
						return true;
				}
			else
				for (Player player : ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]))) {
					if (player.getName().equals(playerName))
						return true;
				}
		} catch (NoSuchMethodException ex) {
		} // can never happen
		catch (InvocationTargetException ex) {
		} // can also never happen
		catch (IllegalAccessException ex) {
		} // can still never happen
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

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
			String itemName = is.getType().toString().toLowerCase().replace("_", " ");
			if (itemName.equals("monster egg")) {
				SpawnEgg egg = (SpawnEgg) is.getData();
				itemName = egg.getSpawnedType().toString().toLowerCase().replace("_", " ") + " egg";
			}
			if (itemName.equals("potion")) {
				if (is.getAmount() > 1) {
					itemName += "s";
				}
				Potion potion = Potion.fromItemStack(is);
				itemName += " of " + potion.getType().toString().toLowerCase().replace("_", " ");
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
					} else if (itemName.contains("beef")) {
						plural = "";
					} else if (itemName.contains("lapiz")) {
						plural = "";
					} else if (itemName.contains("potato")) {
						plural = "es";
					} else if (itemName.contains("glass")) {
						plural = "";
					} else if (itemName.contains("grass")) {
						plural = "";
					} else if (itemName.contains("dirt")) {
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
			if (is.getItemMeta().hasEnchants() || (useStorage && esm.hasStoredEnchants())) {
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
					sb.append(getNameFromEnchant(enchant) + " " + Integer.toString(level) + ", ");
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
			if (is == null || is.getType() == Material.AIR)
				continue;
			sb.append(is.getAmount() + " ");
			String itemName = is.getType().toString().toLowerCase().replace("_", " ");
			if (itemName.equals("monster egg")) {
				SpawnEgg egg = (SpawnEgg) is.getData();
				itemName = egg.getSpawnedType().toString().toLowerCase().replace("_", " ") + " egg";
			}
			if (itemName.equals("potion")) {
				if (is.getAmount() > 1) {
					itemName += "s";
				}
				Potion potion = Potion.fromItemStack(is);
				itemName += " of " + potion.getType().toString().toLowerCase().replace("_", " ");
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
					} else if (itemName.contains("beef")) {
						plural = "";
					} else if (itemName.contains("lapiz")) {
						plural = "";
					} else if (itemName.contains("glass")) {
						plural = "";
					} else if (itemName.contains("grass")) {
						plural = "";
					} else if (itemName.contains("dirt")) {
						plural = "";
					} else if (itemName.contains("potato")) {
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
			if (is.getItemMeta().hasEnchants() || (useStorage && esm.hasStoredEnchants())) {
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
					sb.append(getNameFromEnchant(enchant) + " " + Integer.toString(level) + ", ");
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
		} else if (name.equalsIgnoreCase("silk touch")) {
			return Enchantment.SILK_TOUCH;
		} else if (name.equalsIgnoreCase("sharpness")) {
			return Enchantment.DAMAGE_ALL;
		} else if (name.equalsIgnoreCase("bane of arthropods")) {
			return Enchantment.DAMAGE_ARTHROPODS;
		} else if (name.equalsIgnoreCase("smite")) {
			return Enchantment.DAMAGE_UNDEAD;
		} else if (name.equalsIgnoreCase("knockback")) {
			return Enchantment.KNOCKBACK;
		} else if (name.equalsIgnoreCase("protection")) {
			return Enchantment.PROTECTION_ENVIRONMENTAL;
		} else if (name.equalsIgnoreCase("fire protection")) {
			return Enchantment.PROTECTION_FIRE;
		} else if (name.equalsIgnoreCase("blast protection")) {
			return Enchantment.PROTECTION_EXPLOSIONS;
		} else if (name.equalsIgnoreCase("projectile protection")) {
			return Enchantment.PROTECTION_PROJECTILE;
		} else if (name.equalsIgnoreCase("feather falling")) {
			return Enchantment.PROTECTION_FALL;
		} else if (name.equalsIgnoreCase("respiration")) {
			return Enchantment.OXYGEN;
		} else if (name.equalsIgnoreCase("aqua affinity")) {
			return Enchantment.WATER_WORKER;
		} else if (name.equalsIgnoreCase("thorns")) {
			return Enchantment.THORNS;
		} else if (name.equalsIgnoreCase("fire aspect")) {
			return Enchantment.FIRE_ASPECT;
		} else if (name.equalsIgnoreCase("efficiency")) {
			return Enchantment.DIG_SPEED;
		} else if (name.equalsIgnoreCase("unbreaking")) {
			return Enchantment.DURABILITY;
		} else if (name.equalsIgnoreCase("fortune")) {
			return Enchantment.LOOT_BONUS_BLOCKS;
		} else if (name.equalsIgnoreCase("power")) {
			return Enchantment.ARROW_DAMAGE;
		} else if (name.equalsIgnoreCase("punch")) {
			return Enchantment.ARROW_KNOCKBACK;
		} else if (name.equalsIgnoreCase("flame")) {
			return Enchantment.ARROW_FIRE;
		} else if (name.equalsIgnoreCase("infinity")) {
			return Enchantment.ARROW_INFINITE;
		} else if (name.equalsIgnoreCase("luck of the sea")) {
			return Enchantment.LUCK;
		} else if (name.equalsIgnoreCase("lure")) {
			return Enchantment.LURE;
		} else if (name.equalsIgnoreCase("depth strider")) {
			return Enchantment.DEPTH_STRIDER;
		} else {
			return null;
		}
	}

	public static String getNameFromEnchant(Enchantment enchant) {
		String name = "";
		if (enchant.equals(Enchantment.LOOT_BONUS_MOBS)) {
			name = "looting";
		} else if (enchant.equals(Enchantment.SILK_TOUCH)) {
			name = "silk touch";
		} else if (enchant.equals(Enchantment.DAMAGE_ALL)) {
			name = "sharpness";
		} else if (enchant.equals(Enchantment.DAMAGE_ARTHROPODS)) {
			name = "bane of arthropods";
		} else if (enchant.equals(Enchantment.DAMAGE_UNDEAD)) {
			name = "smite";
		} else if (enchant.equals(Enchantment.KNOCKBACK)) {
			name = "knockback";
		} else if (enchant.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) {
			name = "protection";
		} else if (enchant.equals(Enchantment.PROTECTION_EXPLOSIONS)) {
			name = "blast protection";
		} else if (enchant.equals(Enchantment.PROTECTION_FALL)) {
			name = "feather falling";
		} else if (enchant.equals(Enchantment.PROTECTION_FIRE)) {
			name = "fire protection";
		} else if (enchant.equals(Enchantment.PROTECTION_PROJECTILE)) {
			name = "projectile protection";
		} else if (enchant.equals(Enchantment.OXYGEN)) {
			name = "respiration";
		} else if (enchant.equals(Enchantment.WATER_WORKER)) {
			name = "aqua affinity";
		} else if (enchant.equals(Enchantment.THORNS)) {
			name = "thorns";
		} else if (enchant.equals(Enchantment.FIRE_ASPECT)) {
			name = "fire aspect";
		} else if (enchant.equals(Enchantment.DIG_SPEED)) {
			name = "efficiency";
		} else if (enchant.equals(Enchantment.DURABILITY)) {
			name = "unbreaking";
		} else if (enchant.equals(Enchantment.LOOT_BONUS_BLOCKS)) {
			name = "fortune";
		} else if (enchant.equals(Enchantment.ARROW_DAMAGE)) {
			name = "power";
		} else if (enchant.equals(Enchantment.ARROW_FIRE)) {
			name = "flame";
		} else if (enchant.equals(Enchantment.ARROW_INFINITE)) {
			name = "infinity";
		} else if (enchant.equals(Enchantment.ARROW_KNOCKBACK)) {
			name = "punch";
		} else if (enchant.equals(Enchantment.LUCK)) {
			name = "luck of the sea";
		} else if (enchant.equals(Enchantment.LURE)) {
			name = "lure";
		} else if (enchant.equals(Enchantment.DEPTH_STRIDER)) {
			name = "depth strider";
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

	@SuppressWarnings("unchecked")
	public static String completeName(String playername) {
		try {
			if (Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).getReturnType() == Collection.class)
				for (Player onlinePlayer : ((Collection<? extends Player>) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]))) {
					if (onlinePlayer.getName().toLowerCase().startsWith(playername.toLowerCase())) {
						return onlinePlayer.getName();
					}
				}
			else
				for (Player onlinePlayer : ((Player[]) Bukkit.class.getMethod("getOnlinePlayers", new Class<?>[0]).invoke(null, new Object[0]))) {
					if (onlinePlayer.getName().toLowerCase().startsWith(playername.toLowerCase())) {
						return onlinePlayer.getName();
					}
				}
		} catch (NoSuchMethodException ex) {
		} // can never happen
		catch (InvocationTargetException ex) {
		} // can also never happen
		catch (IllegalAccessException ex) {
		} // can still never happen

		OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
		for (int i = 0; i < offlinePlayers.length; i++) {
			if (offlinePlayers[i].getName().toLowerCase().startsWith(playername.toLowerCase())) {
				return offlinePlayers[i].getName();
			}
		}
		return null;
	}

	public static String helpMenu(CommandSender player) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " ?" + ChatColor.GRAY + " - This help menu.\n");
		if (player.hasPermission("voteroulette.votecommand")) {
			sb.append(ChatColor.AQUA + "/vote" + ChatColor.GRAY + " - Get the links to vote on.\n");
		}
		if (player.hasPermission("voteroulette.createawards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "create" + ChatColor.GRAY + " - Create a new award.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "editreward [#]" + ChatColor.GRAY + " - Edit a reward.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "editmilestone [#]" + ChatColor.GRAY + " - Edit a milestone.\n");
		}
		if (player.hasPermission("voteroulette.deleteawards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "deletereward [#]" + ChatColor.GRAY + " - Delete a reward.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "deletemilestone [#]" + ChatColor.GRAY + " - Delete a milestone.\n");
		}
		if (player.hasPermission("voteroulette.edititems")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "setname [text]" + ChatColor.GRAY + " - Set a custom name for item in hand.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "setlore [text]" + ChatColor.GRAY + " - Set lore for item in hand.\n");
		}
		if (player.hasPermission("voteroulette.colors")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + "colors" + ChatColor.GRAY + " - See the colorcodes.\n");
		}
		if (player.hasPermission("voteroulette.viewrewards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + ChatColor.GRAY + " - See rewards you are eligible to get.\n");
		}
		if (player.hasPermission("voteroulette.viewmilestones")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase() + ChatColor.GRAY + " - See milestones you are eligible to get.\n");
		}
		if (player.hasPermission("voteroulette.viewallawards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " [" + plugin.REWARDS_PURAL_DEF.toLowerCase() + "/" + plugin.MILESTONE_PURAL_DEF.toLowerCase() + "]" + " -a" + ChatColor.GRAY + " - See all the awards, regardless of if you are eligable.\n");
		}
		if (player.hasPermission("voteroulette.top10")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + "10 " + plugin.TOTAL_DEF.toLowerCase().replace(" ", "") + ChatColor.GRAY + " - See the top 10 players for total votes.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + "10 " + plugin.VOTE_STREAK_DEF.toLowerCase().replace(" ", "") + ChatColor.GRAY + " - See the top 10 players for consecutive days voting.\n");
		}
		if (player.hasPermission("voteroulette.lastvote")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.LASTVOTE_DEF.toLowerCase() + ChatColor.GRAY + " - Shows how long ago your last vote was.\n");
		}
		if (player.hasPermission("voteroulette.lastvoteothers")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.LASTVOTE_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "]" + ChatColor.GRAY + " - Shows how long ago the given players last vote was.\n");
		}
		if (player.hasPermission("voteroulette.viewstats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + ChatColor.GRAY + " - See your voting stats.\n");
		}
		if (player.hasPermission("voteroulette.viewotherstats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "]" + ChatColor.GRAY + " - See the stats of another player.\n");
		}
		if (player.hasPermission("voteroulette.editstats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETTOTAL_DEF + " [#]" + ChatColor.GRAY + " - Set a players total votes.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETCYCLE_DEF + " [#]" + ChatColor.GRAY + " - Set a players current vote cycle.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETSTREAK_DEF + " [#]" + ChatColor.GRAY + " - Set a players current vote streak.\n");
		}
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + ChatColor.GRAY + " - Tells you if you have any unclaimed rewards or\n milestones you received while offline.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + ChatColor.GRAY + " - Lists any of your unclaimed rewards.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + " [#/" + plugin.ALL_DEF + "]" + ChatColor.GRAY + " - Gives you the reward with the given # or all of them.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase() + ChatColor.GRAY + " - Lists any of your unclaimed milestones.\n");
		sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF + " [#/" + plugin.ALL_DEF + "]" + ChatColor.GRAY + " - Gives you the milestone with the given # or all of them.\n");
		if (player.hasPermission("voteroulette.forcevote")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEVOTE_DEF + " [" + plugin.PLAYER_DEF + "]" + ChatColor.GRAY + " - Make it as if the given player just voted, this will update their stats and give them an applicable reward/milestone.\n");
		}
		if (player.hasPermission("voteroulette.forceawards")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEREWARD_DEF + "  " + "[reward#] " + "[" + plugin.PLAYER_DEF.toLowerCase() + "]" + ChatColor.GRAY + " - Award a player the given reward. The number corresponds with the full rewards list.\n");
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEMILESTONE_DEF + "  " + "[milestone#] " + "[" + plugin.PLAYER_DEF.toLowerCase() + "]" + ChatColor.GRAY + " - Award a player the given milestone. The number corresponds with the full milestones list.\n");
		}
		if (player.hasPermission("voteroulette.wipestats")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.WIPESTATS_DEF + " [" + plugin.PLAYER_DEF + "/" + plugin.ALL_DEF + "] [" + plugin.STATS_DEF + "/" + plugin.ALL_DEF + "]" + ChatColor.GRAY + " - Wipes the given stat (or all stats) of a particular player (or all of them).\n");
		}
		if (player.hasPermission("voteroulette.admin")) {
			sb.append(ChatColor.AQUA + "/" + plugin.DEFAULT_ALIAS + " " + plugin.RELOAD_DEF + ChatColor.GRAY + " - Reloads the config file.\n");
		}
		return sb.toString().toLowerCase();
	}

	public static FancyMenu fancyHelpMenu(CommandSender player,
			String commandLabel) {
		FancyMenu fancyMenu = new FancyMenu("VoteRoulette Commands", commandLabel);
		fancyMenu.addText(ChatColor.GRAY + "" + ChatColor.ITALIC + "(Hover over a " + ChatColor.GREEN + "" + ChatColor.ITALIC + "command" + ChatColor.GRAY + "" + ChatColor.ITALIC + " for info, click to run it.)");
		fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " ?", "This help menu.", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " ?");

		if (player.hasPermission("voteroulette.votecommand")) {
			fancyMenu.addCommand("/vote", "View the links to vote on.", ClickEvent.RUN_COMMAND, "/vote");
		}
		if (player.hasPermission("voteroulette.createawards")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "create", "Create a new award", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "create");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "editreward [#]", "Edit a reward", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "editreward ");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "editmilestone [#]", "Edit a milestone", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "editmilestone ");
		}
		if (player.hasPermission("voteroulette.deleteawards")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "deletereward [#]", "Delete a reward", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "deletereward ");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "deletemilestone [#]", "Delete a milestone", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "deletemilestone ");
		}
		if (player.hasPermission("voteroulette.edititems")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "setname [text]", "Set a custom name for item in hand", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "setname ");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "setlore [text]", "Set a custom lore for item in hand", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "setlore ");
		}
		if (player.hasPermission("voteroulette.colors")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + "colors", "See the colorcodes", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + "colors");
		}
		if (player.hasPermission("voteroulette.viewrewards")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF.toLowerCase(), "See rewards you are eligible to get", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF.toLowerCase());
		}
		if (player.hasPermission("voteroulette.viewmilestones")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase(), "See milestones you are eligible to get", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase());
		}
		if (player.hasPermission("voteroulette.viewallawards")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + " -a", "See all the rewards, regardless of if you are eligable.", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + " -a");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase() + " -a", "See all the milestones, regardless of if you are eligable.", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase() + " -a");
		}
		if (player.hasPermission("voteroulette.top10")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + "10 " + plugin.TOTAL_DEF.toLowerCase().replace(" ", ""), "See the top 10 players for lifetime votes", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + "10 " + plugin.TOTAL_DEF.toLowerCase().replace(" ", ""));
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + "10 " + plugin.VOTE_STREAK_DEF.toLowerCase().replace(" ", ""), "See the top 10 players for consecutive days voting", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.TOP_DEF + "10 " + plugin.VOTE_STREAK_DEF.toLowerCase().replace(" ", ""));
		}
		if (player.hasPermission("voteroulette.lastvote")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.LASTVOTE_DEF.toLowerCase(), "See how long ago your last vote was", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.LASTVOTE_DEF.toLowerCase());
		}
		if (player.hasPermission("voteroulette.lastvoteothers")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.LASTVOTE_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "]", "Shows how long ago the given players last vote was", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.LASTVOTE_DEF.toLowerCase() + " ");
		}
		if (player.hasPermission("voteroulette.viewstats")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase(), "See your voting stats", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase());
		}
		if (player.hasPermission("voteroulette.viewotherstats")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "]", "See the stats of another player", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " ");
		}
		if (player.hasPermission("voteroulette.editstats")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETTOTAL_DEF + " [#]", "Set a players total votes", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETTOTAL_DEF + " [#]");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETCYCLE_DEF + " [#]", "Set a players current vote cycle", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETCYCLE_DEF + " [#]");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETSTREAK_DEF + " [#]", "Set a players current vote streak", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.STATS_DEF.toLowerCase() + " [" + plugin.PLAYER_DEF.toLowerCase() + "] " + plugin.SETSTREAK_DEF + " [#]");
		}
		fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF, "Tells you if you have any unclaimed rewards or\nmilestones you received while offline", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF);
		fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF.toLowerCase(), "Lists any of your unclaimed rewards", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF.toLowerCase());
		fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + " [#/" + plugin.ALL_DEF + "]", "Claims the reward with the given # or all of them", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.REWARDS_PURAL_DEF.toLowerCase() + " [#/" + plugin.ALL_DEF + "]");
		fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase(), "Lists any of your unclaimed milestones", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase());
		fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase() + " [#/" + plugin.ALL_DEF + "]", "Claims the milestone with the given # or all of them", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.CLAIM_DEF + " " + plugin.MILESTONE_PURAL_DEF.toLowerCase() + " [#/" + plugin.ALL_DEF + "]");
		if (player.hasPermission("voteroulette.forcevote")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEVOTE_DEF + " [" + plugin.PLAYER_DEF + "]", "Make it as if the given player just voted,\nthis will update their stats and give\n them an applicable reward/milestone.", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEVOTE_DEF + " [" + plugin.PLAYER_DEF + "]");
		}
		if (player.hasPermission("voteroulette.forceawards")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEREWARD_DEF + "  " + "[reward#] " + "[" + plugin.PLAYER_DEF.toLowerCase() + "]", "Award a player the given reward.\nThe number corresponds with the full rewards list.", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEREWARD_DEF + "  " + "[reward#] " + "[" + plugin.PLAYER_DEF.toLowerCase() + "]");
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEMILESTONE_DEF + "  " + "[milestone#] " + "[" + plugin.PLAYER_DEF.toLowerCase() + "]", "Award a player the given milestone.\nThe number corresponds with the full milestones list.", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.FORCEMILESTONE_DEF + "  " + "[reward#] " + "[" + plugin.PLAYER_DEF.toLowerCase() + "]");
		}
		if (player.hasPermission("voteroulette.wipestats")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.WIPESTATS_DEF + " [" + plugin.PLAYER_DEF + "/" + plugin.ALL_DEF + "] [" + plugin.STATS_DEF + "/" + plugin.ALL_DEF + "]", "Wipes the given stat (or all stats)\nof a particular player (or all of them)", ClickEvent.SUGGEST_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.WIPESTATS_DEF + " [" + plugin.PLAYER_DEF + "/" + plugin.ALL_DEF + "] [" + plugin.STATS_DEF + "/" + plugin.ALL_DEF + "]");
		}
		if (player.hasPermission("voteroulette.admin")) {
			fancyMenu.addCommand("/" + plugin.DEFAULT_ALIAS + " " + plugin.RELOAD_DEF, "Reloads the config files", ClickEvent.RUN_COMMAND, "/" + plugin.DEFAULT_ALIAS + " " + plugin.RELOAD_DEF);
		}
		return fancyMenu;
	}

	public static void showAwardGUI(Award award, Voter voter, int awardNumber) {
		Utils.showAwardGUI(award, voter, awardNumber, false);
	}

	public static void showAwardGUI(Award award, Voter voter, int awardNumber,
			boolean showPlayers) {
		Player p = voter.getPlayer();
		p.closeInventory();
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
		String name = ChatColor.DARK_BLUE + award.getName() + " " + ChatColor.BLACK + awardType;
		if (name.length() > 32) {
			name = name.substring(0, 28) + "...";
		}
		int multOf9 = 9;
		int req = award.getRequiredSlots(voter);
		if (award.hasCurrency()) {
			req++;
		}
		if (award.hasCommands()) {
			req++;
		}
		if (award.hasXpLevels()) {
			req++;
		}
		if ((award.getAwardType() == AwardType.REWARD && (reward.hasWebsites() || reward.hasVoteStreak())) || award.hasChance() || award.hasWorlds() || award.hasReroll() || award.hasDescription() || ((plugin.SHOW_COMMANDS_IN_AWARD || showPlayers) && (award.hasPlayers() || award.hasPermissionGroups())) || award.getAwardType() == AwardType.MILESTONE) {
			req++;
		}
		while (req > multOf9) {
			multOf9 += 9;
		}
		Inventory i = Bukkit.createInventory(p, multOf9, name);
		ItemStack[] items = Utils.updateLoreAndCustomNamesForItemPrizes(voter, award.getItemPrizes());
		for (ItemStack item : items) {
			i.addItem(item);
		}
		if (award.hasXpLevels()) {
			ItemStack xp = new ItemStack(Material.EXP_BOTTLE);
			ItemMeta itemMeta = xp.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW + Integer.toString(award.getXpLevels()) + ChatColor.RESET + " " + plugin.XPLEVELS_DEF);
			xp.setItemMeta(itemMeta);
			i.addItem(xp);
		}
		if (award.hasCurrency()) {
			ItemStack xp = new ItemStack(Material.GOLD_INGOT);
			ItemMeta itemMeta = xp.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW + Double.toString(award.getCurrency()) + ChatColor.RESET + " " + plugin.CURRENCY_PURAL_DEF);
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
							command = command.substring(0, index + 2) + "/" + command.substring(index + 2, command.length());
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
		if ((award.getAwardType() == AwardType.REWARD && (reward.hasWebsites() || reward.hasVoteStreak())) || award.hasChance() || award.hasWorlds() || award.hasReroll() || award.hasDescription() || ((plugin.SHOW_COMMANDS_IN_AWARD || showPlayers) && (award.hasPlayers() || award.hasPermissionGroups())) || award.getAwardType() == AwardType.MILESTONE) {
			ItemStack sign = new ItemStack(Material.SIGN);
			ItemMeta itemMeta = sign.getItemMeta();
			itemMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "Details");
			List<String> lore = new ArrayList<String>();
			if (award.getAwardType() == AwardType.MILESTONE) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.VOTES_DEF + ": " + ChatColor.DARK_AQUA);
				if (milestone.isRecurring()) {
					sb.append(plugin.EVERY_DEF + " ");
				}
				sb.append(milestone.getVotes());
				lore.add(sb.toString());
			}
			if (award.getAwardType() == AwardType.REWARD && reward.hasVoteStreak()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.VOTE_STREAK_DEF + ": " + ChatColor.DARK_AQUA + reward.getVoteStreak());
				if (reward.hasVoteStreakModifier()) {
					VoteStreakModifier vsm = reward.getVoteStreakModifier();
					sb.append(" " + vsm.toString().toLowerCase().replace("_", " "));
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
				sb.append(ChatColor.GOLD + plugin.CHANCE_DEF + ": " + ChatColor.DARK_AQUA);
				sb.append(award.getChanceMin() + " in " + award.getChanceMax());
				lore.add(sb.toString());
			}
			if (award.hasReroll()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + "Reroll: " + ChatColor.DARK_AQUA);
				sb.append(award.getRerollString());
				lore.add(sb.toString());
			}
			if (award.hasWorlds()) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.WORLDS_DEF + ": " + ChatColor.DARK_AQUA);
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
					sb.append(ChatColor.GOLD + "PermGroups: " + ChatColor.DARK_AQUA);
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
					sb.append(ChatColor.GOLD + "Players: " + ChatColor.DARK_AQUA);
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
			if ((award.getAwardType() == AwardType.REWARD && reward.hasWebsites())) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD + plugin.WEBSITES_DEF + ": " + ChatColor.DARK_AQUA);
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
				sb.append(ChatColor.GOLD + "Description: " + ChatColor.DARK_AQUA + award.getDescription());
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
		} else if (input.equalsIgnoreCase("currentvotecycle")) {
			return Stat.CURRENT_VOTE_CYCLE;
		} else if (input.equalsIgnoreCase("votecycle")) {
			return Stat.CURRENT_VOTE_CYCLE;
		} else if (input.equalsIgnoreCase("votestreak")) {
			return Stat.CURRENT_VOTE_STREAK;
		} else if (input.equalsIgnoreCase("currentvotestreak")) {
			return Stat.CURRENT_VOTE_STREAK;
		} else if (input.equalsIgnoreCase("longestvotestreak")) {
			return Stat.LONGEST_VOTE_STREAK;
		} else if (input.equalsIgnoreCase("lifetimevotes")) {
			return Stat.LIFETIME_VOTES;
		} else if (input.equalsIgnoreCase("totalvotes")) {
			return Stat.LIFETIME_VOTES;
		} else if (input.equalsIgnoreCase("lastvote")) {
			return Stat.LAST_VOTE;
		} else if (input.equalsIgnoreCase("unclaimedrewards")) {
			return Stat.UNCLAIMED_REWARDS;
		} else if (input.equalsIgnoreCase("unclaimedmilestones")) {
			return Stat.UNCLAIMED_MILSTONES;
		} else {
			return null;
		}

	}

	public static void randomFireWork(Location loc) {
		Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);

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

		FireworkEffect effect = FireworkEffect.builder().flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type).trail(r.nextBoolean()).build();

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
