package com.mythicacraft.voteroulette;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class Reward {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private double currency = 0;
	private int xpLevels = 0;
	private ArrayList<ItemStack> items = new ArrayList<ItemStack>();
	private String[] permGroups;
	private String name;

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
		if(cs.contains("items")) {
			String[] tmp = cs.getString("items").split(",");
			for (String item : tmp) {
				String quant = "1";
				if (item.equals("")) continue;
				if (item.contains("(")) {
					String[] itemAndQuant = item.split("\\(");
					item = itemAndQuant[0].trim();
					quant = itemAndQuant[1].replace(")", "").trim();
				}
				try {
					int iItem = Integer.parseInt(item);
					int iQuant = Integer.parseInt(quant);
					items.add(new ItemStack(Material.getMaterial(iItem), iQuant));
				} catch (Exception e) {
					log.warning("[VoteRoulette] Invalid item formatting for reward: " + item + ", Skipping.");
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
				itemSlots++;
			}
			totalSlots =+ itemSlots;
		}
		return totalSlots;
	}

	public boolean isEmpty() {
		if(currency == 0 && xpLevels == 0 && items.isEmpty()) {
			return true;
		}
		return false;
	}
}
