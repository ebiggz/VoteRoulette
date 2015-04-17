package com.mythicacraft.voteroulette.utils.InteractiveMessageAPI;

import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class InteractiveMessage {

	private Queue<InteractiveMessageElement> elements = new LinkedList<InteractiveMessageElement>();

	public InteractiveMessage() {}
	public InteractiveMessage(InteractiveMessageElement element) {
		elements.add(element);
	}
	/**
	 * Add a new element to the InteractiveMessage with the given text.
	 * <p>
	 *     Note: This will auto-create an InteractiveMessageElement without hover or click events.
	 * </p>
	 * @param text the text to add
	 * @return this InteractiveMessage object to enable daisy chaining
	 */
	public InteractiveMessage addElement(String text) {
		elements.add(new InteractiveMessageElement(text));
		return this;
	}

	/**
	 * Add a new element to the InteractiveMessage with the given text and color.
	 * <p>
	 *     Note: This will auto-create an InteractiveMessageElement without hover or click events.
	 * </p>
	 * @param text the text to add
	 * @param color the color for the text
	 * @return this InteractiveMessage object to enable daisy chaining
	 */
	public InteractiveMessage addElement(String text, ChatColor color) {
		elements.add(new InteractiveMessageElement(new FormattedText(text, color)));
		return this;
	}

	/**
	 * Add a new element to the InteractiveMessage with FormattedText.
	 * <p>
	 *     Note: This will auto-create an InteractiveMessageElement without hover or click events.
	 * </p>
	 * @param text the formatted text to add
	 * @return this InteractiveMessage object to enable daisy chaining
	 */
	public InteractiveMessage addElement(FormattedText text) {
		elements.add(new InteractiveMessageElement(text));
		return this;
	}

	/**
	 * Add a new element to the InteractiveMessage with the InteractiveMessageElement.
	 * @param element the element to add
	 * @return this InteractiveMessage object to enable daisy chaining
	 */
	public InteractiveMessage addElement(InteractiveMessageElement element) {
		elements.add(element);
		return this;
	}

	/**
	 * Sends the interactive message to a player
	 * @param player the player to send the message to.
	 */
	public void sendTo(Player player) {
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), this.getFormattedCommand(player.getName()));
	}

	/**
	 * Sends the interactive message to a CommandSender
	 * @param sender the CommandSender to send the message to. Must be a Player and not a console.
	 */
	public void sendTo(CommandSender sender) {
		if(!(sender instanceof Player)) return;
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), this.getFormattedCommand(sender.getName()));
	}

	/**
	 * Creates the JSON formatted /tellraw command
	 * @param string the players name to send the message to.
	 * @return the formatted command
	 */
	private String getFormattedCommand(String playerName) {
		StringBuilder sb = new StringBuilder();
		sb.append("tellraw " + playerName + " {\"text\":\"\",\"extra\":[");
		int count = elements.size();
		for(InteractiveMessageElement element : elements) {
			sb.append("{");
			sb.append(element.getMainText().getJSONString());
			if(element.hasClickEvent()) {
				sb.append(",");
				sb.append("\"clickEvent\": {");
				sb.append("\"action\": \"" + element.getClickEventType().toString().toLowerCase() + "\",");
				sb.append("\"value\": \"" + element.getCommand() + "\"");
				sb.append("}");
			}
			if(element.hasHoverEvent()) {
				sb.append(",");
				sb.append("\"hoverEvent\": {");
				sb.append("\"action\": \"" + element.getHoverEventType().toString().toLowerCase() + "\",");
				sb.append("\"value\": {");
				sb.append("\"text\": \"\",");
				sb.append("\"extra\": [{");
				sb.append(element.getHoverText().getJSONString());
				sb.append("}]}}");
			}
			sb.append("}");
			count--;
			if(count > 0) {
				sb.append(",");
			}
		}
		sb.append("]}");
		return sb.toString();
	}
}
