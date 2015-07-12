package com.mythicacraft.voteroulette.awards;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.utils.Utils;

public class Award {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private double currency = 0;
	private int xpLevels = 0;
	private int chanceMin;
	private int chanceMax = 100;
	private boolean hasChance = false;
	private ArrayList<ItemPrize> items = new ArrayList<>();
	private String[] permGroups;
	private String[] players;
	private String name;
	private List<String> commands = new ArrayList<String>();
	private List<String> worlds = new ArrayList<String>();
	private String message;
	private String description;
	private List<RerollEntry> rerollEntries = new ArrayList<RerollEntry>();
	private RerollType rerollType = RerollType.NONE;
	private String rerollString;
	private AwardType type;

	protected Award(String name, ConfigurationSection cs, AwardType type) {
		this.setName(name);
		this.setAwardType(type);
		if (cs.contains("currency")) {
			if (!VoteRoulette.hasEconPlugin()) {
				log.warning("[VoteRoulette] Reward \"" + name + "\" contains currency settings but Vault is not installed or there is no economy plugin, Skipping currency.");
			} else {
				try {
					String currency = cs.getString("currency");
					currency = currency.replace("$", "");
					this.currency = Double.parseDouble(currency);
				} catch (Exception e) {
					log.warning("[VoteRoulette] Invalid currency format for " + type.toString().toLowerCase() + ": " + name + ", Skipping currency.");
				}
			}
		}
		if (cs.contains("xpLevels")) {
			try {
				String xp = cs.getString("xpLevels");
				this.xpLevels = Integer.parseInt(xp);
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid xpLevel format for reward: " + name + ", Skipping xpLevels.");
			}
		}
		if (cs.contains("chance")) {
			try {
				String chanceStr = cs.getString("chance").replace("%", "");
				if (chanceStr.contains("/")) {
					String[] chances = chanceStr.split("/");
					this.chanceMin = Integer.parseInt(chances[0]);
					this.chanceMax = Integer.parseInt(chances[1]);
					this.hasChance = true;
				} else {
					this.chanceMin = Integer.parseInt(chanceStr);
					this.hasChance = true;
				}
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid chance format for reward: " + name + ", Skipping chance.");
			}
		}
		if (cs.contains("commands")) {
			try {
				commands = cs.getStringList("commands");
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading commands for reward:" + name + ", Skipping commands.");
			}
		}
		if (cs.contains("worlds")) {
			try {
				String worldsStr = cs.getString("worlds");
				String[] worldArray = worldsStr.split(",");
				for (String worldName : worldArray) {
					worlds.add(worldName.trim());
				}
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading worlds for reward:" + name + ", Skipping worlds.");
			}
		}
		if (cs.contains("message")) {
			try {
				message = Utils.transcribeColorCodes(cs.getString("message"));
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading custom message for reward:" + name + ", Skipping message.");
			}
		}
		if (cs.contains("description")) {
			try {
				description = Utils.transcribeColorCodes(cs.getString("description"));
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading custom description for reward:" + name + ", Skipping description.");
			}
		}
		if (cs.contains("reroll")) {
			try {
				String rerollData = cs.getString("reroll");
				if (rerollData.startsWith("ALL")) {
					if (rerollData.contains("[")) {
						rerollData = rerollData.replace("ALL", "").replace("[", "").replace("]", "").trim();
						String[] rewardsData = rerollData.split(",");
						for (String rewardData : rewardsData) {
							String[] parsedRerollData = this.parseRerollRewardData(rewardData);
							this.rerollEntries.add(new RerollEntry(parsedRerollData[0], Integer.parseInt(parsedRerollData[1]), Integer.parseInt(parsedRerollData[2])));
						}
					}
					this.rerollType = RerollType.ALL;
				} else if (rerollData.startsWith("RANDOM") || rerollData.startsWith("ANY")) {
					if (rerollData.contains("[")) {
						rerollData = rerollData.replace("RANDOM", "").replace("ANY", "").replace("[", "").replace("]", "").trim();
						String[] rewardsData = rerollData.split(",");
						for (String rewardData : rewardsData) {
							String[] parsedRerollData = this.parseRerollRewardData(rewardData);
							this.rerollEntries.add(new RerollEntry(parsedRerollData[0], Integer.parseInt(parsedRerollData[1]), Integer.parseInt(parsedRerollData[2])));
						}
					}
					this.rerollType = RerollType.RANDOM;
				} else {
					String[] parsedRerollData = this.parseRerollRewardData(rerollData);
					this.rerollEntries.add(new RerollEntry(parsedRerollData[0], Integer.parseInt(parsedRerollData[1]), Integer.parseInt(parsedRerollData[2])));
					this.rerollType = RerollType.SINGLE;
				}
				this.rerollString = cs.getString("reroll");
			} catch (Exception e) {
				Utils.debugMessage(e.getMessage());
				if(VoteRoulette.DEBUG) {
					e.printStackTrace();
				}
				log.warning("[VoteRoulette] Error loading reroll settings for reward:" + name + ", Skipping reroll.");
			}
		}
		if (cs.contains("items")) {
			ConfigurationSection items = cs.getConfigurationSection("items");
			if (items != null) {
				for (String itemID : items.getKeys(false)) {
					int id;
					try {
						id = Integer.parseInt(itemID);
					} catch (Exception e) {
						System.out.println("[VoteRoulette] \"" + itemID + "\" is not a valid itemID, Skipping!");
						continue;
					}
					ConfigurationSection itemData = items.getConfigurationSection(itemID);
					if (itemData != null) {
						if (itemData.contains("multiple")) {
							ConfigurationSection multipleData = itemData.getConfigurationSection("multiple");
							for (String mItemID : multipleData.getKeys(false)) {
								ItemPrize mItem = parseConfigItemData(id, multipleData.getConfigurationSection(mItemID));
								if (mItem != null) {
									this.items.add(mItem);
								}
							}
						} else {
							ItemPrize item = parseConfigItemData(id, itemData);
							if (item != null) {
								this.items.add(item);
							}
						}
					}
				}
			}
		}
		if (cs.contains("permGroups")) {
			if (!VoteRoulette.hasPermPlugin()) {
				log.warning("[VoteRoulette] Reward \"" + name + "\" contains perm group settings but Vault is not installed or there is no permission plugin, Skipping perm groups.");
			} else {
				permGroups = cs.getString("permGroups").split(",");
				for (int i = 0; i < permGroups.length; i++) {
					permGroups[i] = permGroups[i].trim();
				}
			}
		}
		if (cs.contains("players")) {
			players = cs.getString("players").split(",");
			for (int i = 0; i < players.length; i++) {
				players[i] = players[i].trim();
			}
		}
		if (!this.hasCurrency() && !this.hasItems() && !this.hasXpLevels() && !this.hasCommands()) {
			log.warning("[VoteRoulette] The Reward \"" + this.getName() + "\" appears to be empty. Check your config settings!");
		}
	}

	protected Award(String name, AwardType type) {
		this.name = name;
		this.type = type;
	}

	public enum AwardType {
		REWARD, MILESTONE, VOTESTREAK
	}

	public double getCurrency() {
		return currency;
	}

	public void setCurrency(double currency) {
		this.currency = currency;
	}

	public int getXpLevels() {
		return xpLevels;
	}

	public void clearItems() {
		items.clear();
	}

	public void addItem(ItemStack item) {
		items.add(new ItemPrize(item));
	}

	public void addItemPrize(ItemPrize item) {
		items.add(item);
	}

	public void setXpLevels(int xpLevels) {
		this.xpLevels = xpLevels;
	}

	public String[] getPermGroups() {
		return permGroups;
	}

	public void setPermGroups(String[] permGroups) {
		this.permGroups = permGroups;
	}

	public String[] getPlayers() {
		return players;
	}

	public void setPlayers(String[] players) {
		this.players = players;
	}

	public boolean hasPlayers() {
		if (players == null)
			return false;
		return true;
	}

	public boolean containsPlayer(String playerName) {
		for (String player : players) {
			if (player.equals(playerName))
				return true;
		}
		return false;
	}

	public void setWorlds(List<String> worlds) {
		this.worlds = worlds;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasPermissionGroups() {
		if (permGroups == null)
			return false;
		return true;
	}

	public boolean containsPermGroup(String permGroup) {
		for (String group : permGroups) {
			if (group.equals(permGroup))
				return true;
		}
		return false;
	}

	public boolean hasItems() {
		if (items.isEmpty())
			return false;
		return true;
	}

	public boolean hasCurrency() {
		if (currency == 0)
			return false;
		return true;
	}

	public boolean hasXpLevels() {
		if (xpLevels == 0)
			return false;
		return true;
	}

	public boolean hasAwardOptions() {
		if (this.hasChance || this.hasWorlds() || this.hasPermissionGroups() || this.hasReroll() || this.hasPlayers() || this.hasMessage() || this.hasDescription()) {
			return true;
		}
		return false;
	}

	public ItemStack[] getItems(Voter voter) {
		List<ItemStack> calcItems = new ArrayList<ItemStack>();
		for (ItemPrize itemP : items) {
			for (ItemStack item : itemP.getCalculatedItem(voter)) {
				calcItems.add(item);
			}
		}
		ItemStack[] itemStacks = new ItemStack[calcItems.size()];
		calcItems.toArray(itemStacks);
		return itemStacks;
	}

	public List<ItemPrize> getItemPrizes() {
		return items;
	}

	public int getRequiredSlots(Voter voter) {
		int totalSlots = 0;
		ItemStack[] items = this.getItems(voter);
		for (int i = 0; i < items.length; i++) {
			int itemSlots = items[i].getAmount() / 64;
			if (items[i].getAmount() % 64 != 0) {
				itemSlots = itemSlots + 1;
			}
			totalSlots = totalSlots + itemSlots;
		}
		return totalSlots;
	}

	public boolean isEmpty() {
		if (currency == 0 && xpLevels == 0 && items.isEmpty() && commands.isEmpty()) {
			return true;
		}
		return false;
	}

	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}

	public boolean hasCommands() {
		if (commands == null || commands.isEmpty())
			return false;
		return true;
	}

	public boolean hasWorlds() {
		if (worlds == null || worlds.isEmpty())
			return false;
		return true;
	}

	public List<String> getWorlds() {
		return worlds;
	}

	public boolean hasMessage() {
		if (message == null || message.length() == 0)
			return false;
		return true;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean hasDescription() {
		if (description == null || description.length() == 0)
			return false;
		return true;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean hasReroll() {
		return rerollType != RerollType.NONE;
	}

	public RerollType getRerollType() {
		return rerollType;
	}

	public List<RerollEntry> getRerollEntries() {
		return rerollEntries;
	}

	public String getRerollString() {
		return rerollString;
	}

	public void setReroll(String rerollData) {
		this.rerollEntries.clear();
		this.chanceMax = 0;
		this.chanceMin = 0;
		this.rerollType = RerollType.NONE;
		if (rerollData == null)
			return;
		try {
			this.rerollString = rerollData;
			if (rerollData.startsWith("ALL")) {
				if (rerollData.contains("[")) {
					rerollData = rerollData.replace("[", "").replace("]", "").trim();
					String[] rewardsData = rerollData.split(",");
					for (String rewardData : rewardsData) {
						String[] parsedRerollData = this.parseRerollRewardData(rewardData);
						this.rerollEntries.add(new RerollEntry(parsedRerollData[0], Integer.parseInt(parsedRerollData[1]), Integer.parseInt(parsedRerollData[2])));
					}
				}
				this.rerollType = RerollType.ALL;
			} else if (rerollData.startsWith("RANDOM") || rerollData.startsWith("ANY")) {
				if (rerollData.contains("[")) {
					rerollData = rerollData.replace("[", "").replace("]", "").trim();
					String[] rewardsData = rerollData.split(",");
					for (String rewardData : rewardsData) {
						String[] parsedRerollData = this.parseRerollRewardData(rewardData);
						this.rerollEntries.add(new RerollEntry(parsedRerollData[0], Integer.parseInt(parsedRerollData[1]), Integer.parseInt(parsedRerollData[2])));
					}
				}
				this.rerollType = RerollType.RANDOM;
			} else {
				String[] parsedRerollData = this.parseRerollRewardData(rerollData);
				this.rerollEntries.add(new RerollEntry(parsedRerollData[0], Integer.parseInt(parsedRerollData[1]), Integer.parseInt(parsedRerollData[2])));
				this.rerollType = RerollType.SINGLE;
			}
		} catch (Exception e) {
			log.warning("[VoteRoulette] Error loading reroll settings for reward:" + name + ", Skipping reroll.");
		}
	}

	public int getChanceMin() {
		return chanceMin;
	}

	public int getChanceMax() {
		return chanceMax;
	}

	public void setChanceMin(int chanceMin) {
		this.chanceMin = chanceMin;
		this.hasChance = true;
	}

	public void setChanceMax(int chanceMax) {
		this.chanceMax = chanceMax;
	}

	public boolean hasChance() {
		return hasChance;
	}

	public void setHasChance(boolean hasChance) {
		this.hasChance = hasChance;
	}

	@SuppressWarnings("deprecation")
	private ItemPrize parseConfigItemData(int itemID,
			ConfigurationSection itemData) {
		ItemPrize item = null;
		ItemMeta itemMeta;
		if (itemData != null) {
			if (itemData.contains("dataID")) {
				String dataIDStr = itemData.getString("dataID");
				short dataID;
				try {
					dataID = Short.parseShort(dataIDStr);
				} catch (Exception e) {
					dataID = 1;
					log.warning("[VoteRoulette] \"" + dataIDStr + "\" is not a valid dataID, Defaulting to 1!");
				}
				try {
					item = new ItemPrize(Material.getMaterial(itemID), 1, dataID);
				} catch (Exception e) {
					log.warning("[VoteRoulette] \"" + itemID + "\" is not a recognized itemID, skipping item!");
					return null;
				}
			} else {
				try {
					item = new ItemPrize(Material.getMaterial(itemID), 1);
				} catch (Exception e) {
					log.warning("[VoteRoulette] \"" + itemID + "\" is not a recognized itemID, skipping item!");
					return null;
				}
			}
			itemMeta = item.getItemMeta();
			if (itemData.contains("amount")) {
				item.setAmount(itemData.getString("amount"));
			}
			if (itemData.contains("armorColor")) {
				String colorStr = itemData.getString("armorColor").trim();
				String testForInt = colorStr.substring(0, 1);
				Color color = null;
				if (testForInt.matches("[0-9]")) {
					String[] colorValues = colorStr.split(",");
					if (colorValues.length < 3 || colorValues.length > 3) {
						System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Invalid amount of numbers.");
					} else {
						int red, green, blue;
						try {
							red = Integer.parseInt(colorValues[0].trim());
							green = Integer.parseInt(colorValues[1].trim());
							blue = Integer.parseInt(colorValues[2].trim());
							color = Color.fromRGB(red, green, blue);
						} catch (Exception e) {
							System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Invalid number format.");
						}
					}
				} else {
					Color newColor = Utils.getColorEnumFromName(colorStr);
					if (newColor != null) {
						color = newColor;
					} else {
						System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Invalid color name.");
					}
				}
				if (color == null) {
					System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Invalid color format.");
				} else if (item.getType() == Material.LEATHER_BOOTS || item.getType() == Material.LEATHER_CHESTPLATE || item.getType() == Material.LEATHER_HELMET || item.getType() == Material.LEATHER_LEGGINGS) {
					LeatherArmorMeta wim = (LeatherArmorMeta) itemMeta;
					wim.setColor(color);
					itemMeta = wim;
				} else {
					System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Item not leather armor.");
				}
			}
			if (itemData.contains("skullOwner")) {
				String skullOwner = itemData.getString("skullOwner");
				if (item.getType() == Material.SKULL_ITEM && item.getDurability() == 3 /* playerhead */) {
					SkullMeta sim = (SkullMeta) itemMeta;
					sim.setOwner(skullOwner);
					itemMeta = sim;
				} else {
					System.out.println("[VoteRoulette] Couldn't add skullOwner for the item: " + itemID + ":" + item.getDurability() + "! Item not a player head.");
				}
			}
			if (itemData.contains("enchants")) {
				boolean useStorage = false;
				EnchantmentStorageMeta esm = null;
				if (item.getType() == Material.ENCHANTED_BOOK) {
					esm = (EnchantmentStorageMeta) itemMeta;
					useStorage = true;
				}
				String[] tmp = itemData.getString("enchants").split(",");
				for (String enchantName : tmp) {
					String level = "1";
					if (enchantName.equals(""))
						continue;
					if (enchantName.contains("(")) {
						String[] enchantAndLevel = enchantName.split("\\(");
						enchantName = enchantAndLevel[0].trim();
						level = enchantAndLevel[1].replace(")", "").trim();
					}
					try {
						Enchantment enchant = Utils.getEnchantEnumFromName(enchantName);
						int iLevel = Integer.parseInt(level);
						if (enchant == null) {
							System.out.println("[VoteRoulette] Couldn't find enchant with the name \"" + enchantName + "\" for the item: " + itemID + "!");
							continue;
						}
						if (useStorage) {
							esm.addStoredEnchant(enchant, iLevel, true);
						} else {
							itemMeta.addEnchant(enchant, iLevel, true);
						}
					} catch (Exception e) {
						System.out.println("[VoteRoulette] Invalid enchant level for \"" + enchantName + "\" for the item: " + itemID + "!");
					}
					if (useStorage) {
						itemMeta = esm;
					}
				}
			}
			if (itemData.contains("potionEffects")) {
				if (item.getType() == Material.POTION) {
					PotionMeta pim = (PotionMeta) itemMeta;

					String[] tmp = itemData.getString("enchants").split(",");
					for (String potionEffectRaw : tmp) {
						if (potionEffectRaw.equals(""))
							continue;
						if (!potionEffectRaw.contains("(") || !potionEffectRaw.contains("/"))
							continue;

						String name, amplifier, duration;
						String[] effectAndLevel = potionEffectRaw.split("\\(");
						name = effectAndLevel[0].trim();
						amplifier = effectAndLevel[1].replace(")", "").trim();
						duration = effectAndLevel[2].replace(")", "").trim();

						try {
							PotionEffect pe = new PotionEffect(PotionEffectType.getByName(name), Integer.parseInt(duration), Integer.parseInt(amplifier));
							pim.addCustomEffect(pe, true);
						} catch (Exception e) {
							System.out.println("[VoteRoulette] Invalid potion effect for \"" + name + "\" for the item: " + itemID + "!");
						}
					}
				}
			}
			if (itemData.contains("name")) {
				String customName = itemData.getString("name");
				itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
			}
			if (itemData.contains("lore")) {
				List<String> lore = new ArrayList<String>();
				lore = itemData.getStringList("lore");
				if (lore == null || lore.isEmpty()) {
					String loreStr = itemData.getString("lore");
					if (loreStr.isEmpty()) {
						System.out.println("[VoteRoulette] The lore for item \"" + itemID + "\" is empty or formatted incorrectly!");
					} else {
						String[] loreLines = loreStr.split(",");
						for (String loreLine : loreLines) {
							lore.add(loreLine.trim());
						}
						for (int i = 0; i < lore.size(); i++) {
							lore.set(i, Utils.transcribeColorCodes(lore.get(i)));
						}
						itemMeta.setLore(lore);
					}
				} else {
					for (int i = 0; i < lore.size(); i++) {
						lore.set(i, Utils.transcribeColorCodes(lore.get(i)));
					}
					itemMeta.setLore(lore);
				}
			}
			item.setItemMeta(itemMeta);
		}
		return item;
	}

	public AwardType getAwardType() {
		return type;
	}

	public void setAwardType(AwardType type) {
		this.type = type;
	}

	public String typeString() {
		if (type == AwardType.REWARD) {
			return "Reward";
		}
		if (type == AwardType.MILESTONE) {
			return "Milestone";
		}
		return "";
	}

	public enum RerollType {
		NONE, SINGLE, RANDOM, ALL
	}

	public String[] parseRerollRewardData(String rerollData) {
		String rewardName, rerollChanceMin, rerollChanceMax;
		if (rerollData.contains("(")) {
			String[] split = rerollData.split("\\(");
			rewardName = split[0].trim();
			split[1] = split[1].replace("%", "").trim().replace(")", "");
			if (split[1].contains("/")) {
				String[] chanceData = split[1].split("/");
				rerollChanceMin = chanceData[0].trim();
				rerollChanceMax = chanceData[1].trim();
			} else {
				rerollChanceMin = split[1].trim();
				rerollChanceMax = "100";
			}
		} else {
			rewardName = rerollData.trim();
			rerollChanceMin = "0";
			rerollChanceMax = "0";
		}
		return new String[] { rewardName, rerollChanceMin, rerollChanceMax };
	}
}
