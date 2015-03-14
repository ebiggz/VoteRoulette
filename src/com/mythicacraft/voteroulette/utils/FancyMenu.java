package com.mythicacraft.voteroulette.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mythicacraft.voteroulette.utils.InteractiveMessageAPI.FormattedText;
import com.mythicacraft.voteroulette.utils.InteractiveMessageAPI.InteractiveMessage;
import com.mythicacraft.voteroulette.utils.InteractiveMessageAPI.InteractiveMessageElement;
import com.mythicacraft.voteroulette.utils.InteractiveMessageAPI.InteractiveMessageElement.ClickEvent;
import com.mythicacraft.voteroulette.utils.InteractiveMessageAPI.InteractiveMessageElement.HoverEvent;


public class FancyMenu {

	private List<InteractiveMessage> commands = new ArrayList<InteractiveMessage>();
	private String commandLabel;
	private String header;


	private InteractiveMessageElement previous;
	private InteractiveMessageElement next;
	private InteractiveMessageElement previousAndNext;

	static String tellRawRun = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"%commandList%\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%command%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"%description%\"}}]}";
	static String tellRawSuggest = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"%commandList%\",\"color\":\"green\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"%command%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"%description%\"}}]}";
	static String tellRawPrevious = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"\",\"color\":\"gold\",\"strikethrough\":\"true\"},{\"text\":\"[\",\"color\":\"gold\"},{\"text\":\"Previous Page\",\"color\":\"yellow\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%prevcommand%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click me!\"}},{\"text\":\"]\",\"color\":\"gold\"}]}";
	static String tellRawNext = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"\",\"color\":\"gold\",\"strikethrough\":\"true\"},{\"text\":\"[\",\"color\":\"gold\"},{\"text\":\"Next Page\",\"color\":\"yellow\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%nextcommand%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click me!\"}},{\"text\":\"]\",\"color\":\"gold\"}]}";
	static String tellRawPreviousNext = "tellraw %player% {\"text\":\"\",\"extra\":[{\"text\":\"\",\"color\":\"gold\",\"strikethrough\":\"true\"},{\"text\":\"[\",\"color\":\"gold\"},{\"text\":\"Previous Page\",\"color\":\"yellow\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%prevcommand%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click me!\"}},{\"text\":\" | \",\"color\":\"gold\"},{\"text\":\"Next Page\",\"color\":\"yellow\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%nextcommand%\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click me!\"}},{\"text\":\"]\",\"color\":\"gold\"}]}";

	public FancyMenu(String commandLabel, String header) {
		this.commandLabel = commandLabel;
		this.header = header;

	}
	public void addEntry(String text, String hoverText, ClickEvent clickAction) {
		InteractiveMessage newCmd = new InteractiveMessage();
		newCmd.addElement(
				new InteractiveMessageElement(
						new FormattedText(text, ChatColor.GREEN),
						(hoverText != null) ? HoverEvent.SHOW_TEXT : HoverEvent.NONE,
								new FormattedText(hoverText),
								clickAction,
								text));
		commands.add(newCmd);
	}

	public void showClickableCommandList(CommandSender sender, String commandLabel, String header, String[] dataList, int pageNumber) {
		int totalPages = (int) Math.ceil(dataList.length/7.0);
		if(pageNumber > totalPages) {
			sender.sendMessage(ChatColor.RED + "That's not a valid page number!");
			return;
		}
		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "-----" + ChatColor.GOLD + "[" + ChatColor.YELLOW + header + ChatColor.GOLD + " | " + ChatColor.YELLOW + "Page " + pageNumber + "/" + totalPages + ChatColor.GOLD + "]");
		int count = 0 + (7*(pageNumber-1));
		while(count < 7 * pageNumber && count < dataList.length) {
			String entry = dataList[count];
			if(entry.startsWith("TELLRAW")) {
				//remove tellraw prefix and split data
				entry = entry.replaceFirst("TELLRAW ", "").trim();
				String[] entryData = entry.split(">>");

				//get tellraw type
				String command;
				if(entryData[0].equals("run")) {
					command = tellRawRun;
				} else {
					command = tellRawSuggest;
				}

				//build command
				command = command.replace("%player%", sender.getName()).replace("%commandList%", entryData[1]).replace("%command%", entryData[2]).replace("%description%", entryData[3]);

				//run command
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
			} else {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', entry));
			}
			count++;
		}
		String nextCmd = "/" + commandLabel + " " + Integer.toString(pageNumber+1);
		String prevCmd = "/" + commandLabel + " " + Integer.toString(pageNumber-1);
		if(totalPages == 1) return;
		if(pageNumber == 1) {
			String command = tellRawNext;
			command = command.replace("%player%", sender.getName()).replace("%nextcommand%", nextCmd);
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
		}
		else if(pageNumber < totalPages) {
			String command = tellRawPreviousNext;
			command = command.replace("%player%", sender.getName()).replace("%nextcommand%", nextCmd).replace("%prevcommand%", prevCmd);
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
		}
		else {
			String command = tellRawPrevious;
			command = command.replace("%player%", sender.getName()).replace("%prevcommand%", prevCmd);
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
		}
	}
}
