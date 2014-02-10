package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.mythicacraft.voteroulette.utils.Utils;

public class Reward {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private double currency = 0;
	private int xpLevels = 0;
	private int chance = 0;
	private ArrayList<ItemStack> items = new ArrayList<ItemStack>();
	private String[] permGroups;
	private String name;
	private List<String> commands = new ArrayList<String>();
	private List<String> worlds = new ArrayList<String>();
	private String message;

	@SuppressWarnings("deprecation")
	Reward(String name, ConfigurationSection cs) {
		this.setName(name);
		if(cs.contains("currency")) {
			if(!VoteRoulette.hasEconPlugin()) {
				log.warning("[VoteRoulette] Reward \"" + name + "\" contains currency settings but Vault is not installed or there is no economy plugin, Skipping currency.");
			} else {
				try {
					String currency = cs.getString("currency");
					this.currency = Double.parseDouble(currency);
				} catch (Exception e) {
					log.warning("[VoteRoulette] Invalid currency format for reward: " + name + ", Skipping currency.");
				}
			}
		}
		if(cs.contains("xpLevels")) {
			try {
				String xp = cs.getString("xpLevels");
				this.xpLevels = Integer.parseInt(xp);
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid xpLevel format for reward: " + name + ", Skipping xpLevels.");
			}
		}
		if(cs.contains("chance")) {
			try {
				String chanceStr = cs.getString("chance").replace("%", "");
				this.chance = Integer.parseInt(chanceStr);
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid chance format for reward: " + name + ", Skipping chance.");
			}
		}
		if(cs.contains("commands")) {
			try {
				commands = cs.getStringList("commands");
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading commands for reward:" + name + ", Skipping commands.");
			}
		}
		if(cs.contains("worlds")) {
			try {
				String worldsStr = cs.getString("worlds");
				String[] worldArray = worldsStr.split(",");
				for(String worldName: worldArray) {
					worlds.add(worldName.trim());
				}
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading worlds for reward:" + name + ", Skipping worlds.");
			}
		}
		if(cs.contains("message")) {
			try {
				message = ChatColor.translateAlternateColorCodes('&', cs.getString("message"));
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading custom message for reward:" + name + ", Skipping message.");
			}
		}
		if(cs.contains("items")) {
			ConfigurationSection items = cs.getConfigurationSection("items");
			if(items != null) {
				for(String itemID : items.getKeys(false)) {
					int id;
					try {
						id = Integer.parseInt(itemID);
					} catch (Exception e) {
						System.out.println("[VoteRoulette] \"" + itemID + "\" is not a valid itemID, Skipping!");
						continue;
					}
					ConfigurationSection itemData = items.getConfigurationSection(itemID);
					ItemStack item;
					ItemMeta itemMeta;
					if(itemData != null) {
						if(itemData.contains("dataID")) {
							String dataIDStr = itemData.getString("dataID");
							byte dataID;
							try {
								dataID = Byte.parseByte(dataIDStr);
							} catch (Exception e) {
								dataID = 1;
								System.out.println("[VoteRoulette] \"" + dataIDStr + "\" is not a valid dataID, Defaulting to 0!");
							}
							item = new ItemStack(Material.getMaterial(id), 1, dataID);
						} else {
							item = new ItemStack(Material.getMaterial(id), 1);
						}
						itemMeta = item.getItemMeta();
						if(itemData.contains("amount")) {
							int amount = itemData.getInt("amount");
							item.setAmount(amount);
						}
						if(itemData.contains("armorColor")) {
							String colorStr = itemData.getString("armorColor").trim();
							String testForInt = colorStr.substring(0, 1);
							Color color = null;
							if(testForInt.matches("[0-9]")) {
								String[] colorValues = colorStr.split(",");
								if(colorValues.length < 3 || colorValues.length > 3) {
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
								if(newColor != null) {
									color = newColor;
								} else {
									System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Invalid color name.");
								}
							}
							if(color == null) {
								System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Invalid color format.");
							}
							else if(item.getType() == Material.LEATHER_BOOTS || item.getType() == Material.LEATHER_CHESTPLATE || item.getType() == Material.LEATHER_HELMET || item.getType() == Material.LEATHER_LEGGINGS) {
								LeatherArmorMeta wim = (LeatherArmorMeta) itemMeta;
								wim.setColor(color);
								itemMeta = wim;
							} else {
								System.out.println("[VoteRoulette] Couldn't add the color for the item: " + itemID + "! Item not leather armor.");
							}
						}
						if(itemData.contains("enchants")) {
							String[] tmp = itemData.getString("enchants").split(",");
							for (String enchantName : tmp) {
								String level = "1";
								if (enchantName.equals("")) continue;
								if (enchantName.contains("(")) {
									String[] enchantAndLevel = enchantName.split("\\(");
									enchantName = enchantAndLevel[0].trim();
									level = enchantAndLevel[1].replace(")", "").trim();
								}
								try {
									Enchantment enchant = Utils.getEnchantEnumFromName(enchantName);
									int iLevel = Integer.parseInt(level);
									if(enchant == null) {
										System.out.println("[VoteRoulette] Couldn't find enchant with the name \"" + enchantName + "\" for the item: " + itemID + "!");
										continue;
									}
									itemMeta.addEnchant(enchant, iLevel, true);
								} catch(Exception e) {
									System.out.println("[VoteRoulette] Invalid enchant level for \"" + enchantName + "\" for the item: " + itemID + "!");
								}
							}
						}
						if(itemData.contains("name")) {
							String customName = itemData.getString("name");
							itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
						}
						if(itemData.contains("lore")) {
							List<String> lore = new ArrayList<String>();
							lore = itemData.getStringList("lore");
							if(lore == null || lore.isEmpty()) {
								String loreStr = itemData.getString("lore");
								if(loreStr.isEmpty()) {
									System.out.println("[VoteRoulette] The lore for item \"" + itemID + "\" is empty or formatted incorrectly!");
								} else {
									String[] loreLines = loreStr.split(",");
									for(String loreLine: loreLines) {
										lore.add(loreLine.trim());
									}
									itemMeta.setLore(lore);
								}
							} else {
								itemMeta.setLore(lore);
							}
						}
						item.setItemMeta(itemMeta);
						this.items.add(item);
					}
				}
			}
		}
		if(cs.contains("permGroups")) {
			if(!VoteRoulette.hasPermPlugin()) {
				log.warning("[VoteRoulette] Reward \"" + name + "\" contains perm group settings but Vault is not installed or there is no permission plugin, Skipping perm groups.");
			} else {
				permGroups = cs.getString("permGroups").split(",");
				for(int i = 0; i < permGroups.length; i++) {
					permGroups[i] = permGroups[i].trim();
				}
			}
		}
	}

	public double getCurrency() {
		return currency;
	}

	public void setCurrency(int currency) {
		this.currency = currency;
	}

	public int getXpLevels() {
		return xpLevels;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean hasPermissionGroups() {
		if(permGroups == null) return false;
		return true;
	}

	public boolean hasItems() {
		if(items.isEmpty()) return false;
		return true;
	}

	public boolean hasCurrency() {
		if(currency == 0) return false;
		return true;
	}

	public boolean hasXpLevels() {
		if(xpLevels == 0) return false;
		return true;
	}

	public ItemStack[] getItems() {
		ItemStack[] itemStacks = new ItemStack[items.size()];
		for(int i = 0; i < items.size();i++) {
			itemStacks[i] = items.get(i);
		}
		return itemStacks;
	}

	public int getRequiredSlots() {
		int totalSlots = 0;
		for(int i = 0; i < items.size(); i++) {
			int itemSlots = items.get(i).getAmount()/64;
			if(items.get(i).getAmount() % 64 != 0) {
				itemSlots = itemSlots + 1;
			}
			totalSlots = totalSlots + itemSlots;
		}
		return totalSlots;
	}

	public boolean isEmpty() {
		if(currency == 0 && xpLevels == 0 && items.isEmpty() && commands.isEmpty()) {
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
		if(commands == null || commands.isEmpty()) return false;
		return true;
	}

	public boolean hasWorlds() {
		if(worlds == null || worlds.isEmpty()) return false;
		return true;
	}

	public List<String> getWorlds() {
		return worlds;
	}

	public boolean hasMessage() {
		if(message == null || message.length() == 0) return false;
		return true;
	}

	public String getMessage() {
		return message;
	}

	public void updateLoreAndCustomNames(String playerName) {
		for(ItemStack item: items) {
			ItemMeta im = item.getItemMeta();
			if(im.hasLore()) {
				List<String> oldLore = im.getLore();
				List<String> newLore = new ArrayList<String>();
				for(String line: oldLore) {
					newLore.add(line.replace("%player%", playerName));
				}
				im.setLore(newLore);
			}
			if(im.hasDisplayName()) {
				im.setDisplayName(im.getDisplayName().replace("%player%", playerName));
			}
			item.setItemMeta(im);
		}
	}

	public int getChance() {
		return chance;
	}

	public void setChance(int chance) {
		this.chance = chance;
	}

	public boolean hasChance() {
		if(chance > 0) return true;
		return false;
	}
}
