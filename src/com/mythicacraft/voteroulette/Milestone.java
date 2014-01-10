package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.mythicacraft.voteroulette.utils.Utils;


public class Milestone {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private int votes = 0;
	private double currency = 0;
	private int xpLevels = 0;
	private ArrayList<ItemStack> items = new ArrayList<ItemStack>();
	private String[] permGroups;
	private int priority = 10;
	private String name;
	private boolean recurring = false;
	private List<String> commands = new ArrayList<String>();
	private String message;

	@SuppressWarnings("deprecation")
	Milestone(String name, ConfigurationSection cs) {
		this.setName(name);
		String votes = cs.getString("votes");
		try {
			this.votes = Integer.parseInt(votes);
		} catch (Exception e) {
			log.warning("[VoteRoulette] Milestone \"" + name + "\" votes format invalid! Ignoring Milestone...");
			try {
				this.finalize();
			}
			catch (Throwable t) {
			}
			return;
		}
		if(cs.contains("currency")) {
			if(!VoteRoulette.hasEconPlugin()) {
				log.warning("[VoteRoulette] Reward \"" + name + "\" contains currency settings but Vault is not installed or there is no economy plugin, Skipping currency...");
			} else {
				try {
					String currency = cs.getString("currency");
					this.currency = Double.parseDouble(currency);
				} catch (Exception e) {
					log.warning("[VoteRoulette] Invalid currency format for reward: " + name + ", Skipping currency...");
				}
			}
		}
		if(cs.contains("xpLevels")) {
			try {
				String xp = cs.getString("xpLevels");
				this.xpLevels = Integer.parseInt(xp);
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid xpLevel format for milestone: " + name + ", Skipping xpLevels...");
			}
		}
		if(cs.contains("recurring")) {
			try {
				boolean tmp = Boolean.parseBoolean(cs.getString("recurring"));
				this.recurring = tmp;
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid recurring format for milestone: " + name + ", Recurring defaulting to false...");
			}
		}
		if(cs.contains("commands")) {
			try {
				commands = cs.getStringList("commands");
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading commands for reward:" + name + ", Skipping commands.");
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
					ItemStack item = new ItemStack(Material.getMaterial(id), 1);
					ItemMeta itemMeta = item.getItemMeta();
					if(itemData != null) {
						if(itemData.contains("amount")) {
							int amount = itemData.getInt("amount");
							item.setAmount(amount);
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
							itemMeta.setDisplayName(customName);
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
				log.warning("[VoteRoulette] Reward \"" + name + "\" contains perm group settings but Vault is not installed or there is no permission plugin, Skipping perm groups...");
			} else {
				permGroups = cs.getString("permGroups").split(",");
				for(int i = 0; i < permGroups.length; i++) {
					permGroups[i] = permGroups[i].trim();
				}
			}
		}
		if(cs.contains("priority")) {
			try {
				String prior = cs.getString("priority");
				this.priority = Integer.parseInt(prior);
				if(this.priority < 1) {
					log.warning("[VoteRoulette] Invalid priority format for milestone: " + name + ", Priority can't be less than 1! Setting priorty to default of 10...");
					this.priority = 10;
				}
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid priority format for milestone: " + name + ", Setting priorty to default of 10...");
			}
		}
		if(!cs.contains("priority")) {
			log.warning("[VoteRoulette] No priority format for milestone: " + name + " found, Setting priorty to default of 10...");
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

	public int getVotes() {
		return votes;
	}

	public void setVotes(int votes) {
		this.votes = votes;
	}

	public boolean isRecurring() {
		return recurring;
	}

	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
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

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
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

	public boolean hasMessage() {
		if(message == null || message.length() == 0) return false;
		return true;
	}

	public String getMessage() {
		return message;
	}
}
