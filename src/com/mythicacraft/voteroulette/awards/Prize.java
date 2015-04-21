package com.mythicacraft.voteroulette.awards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public interface Prize {

	public String getIdentifier();

	public boolean loadFromConfig(ConfigurationSection configSection);

	public String loadFromConfigFailedMessage();

	public boolean isEmpty();

	public ConfigurationSection getConfigSave();

	public String getDescription();

	public boolean administerPrize(Player player);

	public String getAdministrationFailedMessage();

	public ItemStack[] getGUIIcons();

	/* Award Creator methods */

	public boolean isAwardCreatorCompatible();

	public String[] getAwardCreatorDirections();

	public String[] getAwardCreatorDescription();

	public boolean handleAwardCreatorInput(String inputText);

	public String getContentsLoadACFailedMessage();

}
