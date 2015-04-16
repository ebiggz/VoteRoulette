package com.mythicacraft.voteroulette.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.mythicacraft.voteroulette.VoteRoulette;
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

	public FancyMenu(String header, String commandLabel) {
		this.commandLabel = commandLabel;
		this.header = header;

		previous = new InteractiveMessageElement(
				new FormattedText("Previous", ChatColor.YELLOW),
				HoverEvent.SHOW_TEXT,
				new FormattedText("Click for previous page"),
				ClickEvent.RUN_COMMAND,
				"");

		next = new InteractiveMessageElement(
				new FormattedText("Next", ChatColor.YELLOW),
				HoverEvent.SHOW_TEXT,
				new FormattedText("Click for next page"),
				ClickEvent.RUN_COMMAND,
				"");

	}
	public void addCommand(String text, String hoverText, ClickEvent clickAction, String command) {
		InteractiveMessage newCmd = new InteractiveMessage();
		newCmd.addElement(
				new InteractiveMessageElement(
						new FormattedText(text, ChatColor.AQUA),
						(hoverText != null) ? HoverEvent.SHOW_TEXT : HoverEvent.NONE,
								new FormattedText(hoverText),
								clickAction,
								command));
		commands.add(newCmd);
	}

	public void addText(String text) {
		commands.add(new InteractiveMessage().addElement(text));
	}

	public int getTotalPages() {
		return (int) Math.ceil(commands.size()/7.0);
	}

	public void sendPage(int pageNumber, CommandSender sender) {
		int totalPages = this.getTotalPages();
		if(pageNumber > totalPages) {
			sender.sendMessage(VoteRoulette.getInstance().INVALID_NUMBER_NOTIFICATION);
			return;
		}
		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "-----" + ChatColor.GOLD + "[" + ChatColor.YELLOW + header + ChatColor.GOLD + " | " + ChatColor.YELLOW + "Page " + pageNumber + "/" + totalPages + ChatColor.GOLD + "]");
		int count = 0 + (7*(pageNumber-1));
		while(count < 7 * pageNumber && count < commands.size()) {
			InteractiveMessage command = commands.get(count);
			command.sendTo(sender);
			count++;
		}

		if(totalPages <= 1) return;

		this.next.setCommand("/" + commandLabel + " " + Integer.toString(pageNumber+1));
		this.previous.setCommand("/" + commandLabel + " " + Integer.toString(pageNumber-1));

		InteractiveMessage navButtons = new InteractiveMessage();
		navButtons.addElement(new FormattedText("-----").setColor(ChatColor.GOLD).setStrikethrough(true)).addElement("[", ChatColor.GOLD);
		if(pageNumber == 1) {
			navButtons.addElement(next);
		}
		else if(pageNumber < totalPages) {
			navButtons.addElement(previous);
			navButtons.addElement(" | ", ChatColor.GOLD);
			navButtons.addElement(next);
		}
		else {
			navButtons.addElement(previous);
		}
		navButtons.addElement("]", ChatColor.GOLD);
		navButtons.sendTo(sender);
	}
}
