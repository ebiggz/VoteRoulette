package com.mythicacraft.voteroulette.awards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public interface Prize {

	public String getPrizeIdentifier();

	public boolean loadContentsFromFile(ConfigurationSection configSection);

	public String getContentsLoadConfigFailedMessage();

	public boolean hasContents();

	public ConfigurationSection createContentsSave();

	public String getContentsDescription();

	public boolean administerPrize(Player player);

	public String getFailedAdministrationMessage();

	public ItemStack[] getGUIIcons();

	/* Award Creator methods */

	public boolean isAwardCreatorCompatible();

	public String[] getAwardCreatorPrizeDirections();

	public String[] getAwardCreatorContentsDescription();

	public boolean loadContentsfromAwardCreatorEntry(String entryText);

	public String getContentsLoadACFailedMessage();

}
