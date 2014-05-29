package com.mythicacraft.voteroulette.awardcreator;

import java.util.Stack;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.Milestone;


public class AwardCreator {

	private Player player;
	private AwardType awardType = null;
	private Award award;
	private AwardCreationStage currentStage;
	private Stack<AwardCreationStage> previousStages = new Stack<AwardCreationStage>();
	private boolean paused = false;

	public AwardCreator(Player player) {
		this.player = player;
	}


	public enum AwardCreationStage {
		CHOOSE_AWARD, NAME, SET_VOTES, SET_RECURRING, CHOOSE_PRIZE, ADD_ITEMS, ADD_XP, CHOOSE_XP_OPTION, ADD_CURRENCY, EDIT_COMMANDS, ADD_COMMAND, REMOVE_COMMAND, CHOOSE_OPTION, ADD_CHANCE, SET_PRIORITY, ADD_REROLL, ADD_DESCRIPTION, ADD_MESSAGE, ADD_PERMGROUPS, ADD_PLAYERS, ADD_WEBSITES, ADD_WORLDS, FINAL_CONFIRMATION
	}

	public void goToStage(AwardCreationStage stage) {
		this.goToStage(stage, false);
	}

	public void goToStage(AwardCreationStage stage, boolean isGoingBack) {
		if(currentStage != null) {
			if(!isGoingBack) {
				previousStages.push(currentStage);
			}
		}
		currentStage = stage;
		switch(stage) {
		case CHOOSE_AWARD:
			player.sendMessage(ChatColor.YELLOW + "* What type of award do you want to create?");
			player.sendMessage(ChatColor.GOLD + "Reward" + ChatColor.GRAY + " - Considered for players every time they vote.");
			player.sendMessage(ChatColor.GOLD + "Milestone" + ChatColor.GRAY + " - Given to players after they reach a set amount of votes.");
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "reward" + ChatColor.YELLOW + " or " + ChatColor.GOLD + "milestone" + ChatColor.YELLOW + ", or "+ ChatColor.GOLD + "help" + ChatColor.YELLOW + " for help, " + ChatColor.GOLD + "cancel" + ChatColor.YELLOW + " to quit.");
			//player.sendMessage(ChatColor.GRAY + "(or " + ChatColor.GOLD + "help" + ChatColor.GRAY + " for help, " + ChatColor.GOLD + "cancel" + ChatColor.GRAY + " to quit)");
			break;
		case ADD_CHANCE:
			player.sendMessage(ChatColor.YELLOW + "* Please enter the chance you want this " + awardType.toString().toLowerCase() + " to have:");
			player.sendMessage(ChatColor.GRAY + "- Chance can be formatted as #% or #/#. Example: 50% or 1/100");
			player.sendMessage(ChatColor.YELLOW + "(or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.)");
			break;
		case EDIT_COMMANDS:
			if(!award.hasCommands()) {
				player.sendMessage(ChatColor.YELLOW + "* Type " + ChatColor.GOLD + "add" + ChatColor.YELLOW + " to add a new command or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.");
			} else {
				player.sendMessage(ChatColor.YELLOW + "* Type " + ChatColor.GOLD + "add" + ChatColor.YELLOW + " to add a new command, " + ChatColor.GOLD + "remove" + ChatColor.YELLOW + " to remove a command, or " + ChatColor.GOLD + "next" + ChatColor.YELLOW + " to continue.");
				player.sendMessage(ChatColor.GRAY + "Current commands:");
				int count = 1;
				for(String command : award.getCommands()) {
					if(command.contains("(")) {
						int index = command.indexOf(") ");
						if(command.charAt(index+2) != '/') {
							command = command.substring(0, index+2) + "/" + command.substring(index+2, command.length());
						}
					} else {
						if(!command.contains("/")) {
							command = "/" + command;
						}
					}
					player.sendMessage(ChatColor.GRAY + "" + count + ChatColor.GRAY + ") " + command);
					count++;
				}
			}
			break;
		case ADD_CURRENCY:
			player.sendMessage(ChatColor.YELLOW + "* Please type the amount of currency you want this " + awardType.toString().toLowerCase() + " to give:");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_DESCRIPTION:
			player.sendMessage(ChatColor.YELLOW + "* Please type a description for this " + awardType.toString().toLowerCase() + " that players can see when viewing awards:");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_ITEMS:
			player.sendMessage(ChatColor.YELLOW + "* To add items to this " + awardType.toString().toLowerCase() + ", the items must be in your inventory. Please make your inventory reflect what you want the award to give and type " + ChatColor.GOLD + "add" + ChatColor.YELLOW + " when you are ready or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to return to the previous step.");
			break;
		case ADD_MESSAGE:
			player.sendMessage(ChatColor.YELLOW + "* Please type the message that will be soon when a player wins this " + awardType.toString().toLowerCase() + ":");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_PERMGROUPS:
			player.sendMessage(ChatColor.YELLOW + "* Please type the permission group names (seperated by commas) that are eligible for this " + awardType.toString().toLowerCase() + ":");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_PLAYERS:
			player.sendMessage(ChatColor.YELLOW + "* Please type the player names (seperated by commas) that are eligible for this " + awardType.toString().toLowerCase() + ":");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_REROLL:
			player.sendMessage(ChatColor.YELLOW + "* Please type a name of another Reward to be rerolled for when a player wins this " + awardType.toString().toLowerCase() + ":");
			player.sendMessage(ChatColor.GRAY + "- Include \"(#%)\" at the end of the name to set a custom chance");
			player.sendMessage(ChatColor.GRAY + "- Use \"ANY\" instead of a name to have a Reward chosen at random.");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_WEBSITES:
			player.sendMessage(ChatColor.YELLOW + "* Please type the websites (seperated by commas) that this " + awardType.toString().toLowerCase() + " is eligible for:");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_WORLDS:
			player.sendMessage(ChatColor.YELLOW + "* Please type the world names (seperated by commas) that this " + awardType.toString().toLowerCase() + " can be claimed in:");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case ADD_XP:
			player.sendMessage(ChatColor.YELLOW + "* Please type the number of XP levels you want this " + awardType.toString().toLowerCase() + " to give:");
			player.sendMessage(ChatColor.YELLOW + "(or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step)");
			break;
		case CHOOSE_OPTION:
			if(award.hasOptions()) {
				player.sendMessage(ChatColor.YELLOW + "* Do you want to add another award option for " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + "?");
				player.sendMessage(ChatColor.GRAY + "Options: " + ChatColor.GOLD + "permgroups" + ChatColor.GRAY + ", " + ChatColor.GOLD + "players" + ChatColor.GRAY + ", " + ChatColor.GOLD + "worlds" + ChatColor.GRAY + ", " + ChatColor.GOLD + "websites" + ChatColor.GRAY + ", " + ChatColor.GOLD + "chance" + ChatColor.GRAY + ", " + ChatColor.GOLD + "reroll" + ChatColor.GRAY + ", " + ChatColor.GOLD + "description" + ChatColor.GRAY + ", or " + ChatColor.GOLD + "message" + ChatColor.GRAY + ".");
				player.sendMessage(ChatColor.GRAY + "(Type " + ChatColor.GOLD + "preview" + ChatColor.GRAY + " to see the award.");
				player.sendMessage(ChatColor.YELLOW + "Type an option, " + ChatColor.GOLD + "next" + ChatColor.YELLOW + " to continue, or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.");
				return;
			}
			player.sendMessage(ChatColor.YELLOW + "* Do you want to add an award option for " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + "?");
			player.sendMessage(ChatColor.GRAY + "Options: " + ChatColor.GOLD + "permgroups" + ChatColor.GRAY + ", " + ChatColor.GOLD + "players" + ChatColor.GRAY + ", " + ChatColor.GOLD + "worlds" + ChatColor.GRAY + ", " + ChatColor.GOLD + "websites" + ChatColor.GRAY + ", " + ChatColor.GOLD + "chance" + ChatColor.GRAY + ", " + ChatColor.GOLD + "reroll" + ChatColor.GRAY + ", " + ChatColor.GOLD + "description" + ChatColor.GRAY + ", or " + ChatColor.GOLD + "message" + ChatColor.GRAY + ".");
			player.sendMessage(ChatColor.YELLOW + "Type an option, " + ChatColor.GOLD + "next" + ChatColor.YELLOW + " to continue, or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.");
			break;
		case CHOOSE_PRIZE:

			if(award != null) {
				if(!award.isEmpty()) {
					player.sendMessage(ChatColor.YELLOW + "* Do you want to add another prize type for " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + "?");
					player.sendMessage(ChatColor.GRAY + "Prizes: " + ChatColor.GOLD + "items" + ChatColor.GRAY + ", " + ChatColor.GOLD + "xp" + ChatColor.GRAY + ", " + ChatColor.GOLD + "currency" + ChatColor.GRAY + ", or " + ChatColor.GOLD + "commands" + ChatColor.GRAY + ".");
					player.sendMessage(ChatColor.YELLOW + "Enter a prize type, "  + ChatColor.GOLD + "preview" + ChatColor.YELLOW + " to see the award, "+ ChatColor.GOLD + "next" + ChatColor.YELLOW + " to continue, or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back.");
					return;
				}
			}
			player.sendMessage(ChatColor.YELLOW + "* Choose a prize type to add for " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + ":");
			player.sendMessage(ChatColor.GRAY + "Prizes: " + ChatColor.GOLD + "items" + ChatColor.GRAY + ", " + ChatColor.GOLD + "xp" + ChatColor.GRAY + ", " + ChatColor.GOLD + "currency" + ChatColor.GRAY + ", or " + ChatColor.GOLD + "commands" + ChatColor.GRAY + ".");
			player.sendMessage(ChatColor.YELLOW + "Enter a prize type or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.");
			break;
		case FINAL_CONFIRMATION:
			player.sendMessage(ChatColor.YELLOW + "* Confirm the creation of " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + ":");
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "save" + ChatColor.YELLOW + " to finish, " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back, or " + ChatColor.GOLD + "preview" + ChatColor.YELLOW + " to see the award.");
			break;
		case NAME:
			player.sendMessage(ChatColor.YELLOW + "* Please type a name for this new " + awardType.toString().toLowerCase() + ":");
			break;
		case SET_PRIORITY:
			break;
		case SET_RECURRING:
			Milestone ms = (Milestone) award;
			player.sendMessage(ChatColor.YELLOW + "* Do you want " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + " to be recurring?");
			player.sendMessage(ChatColor.GRAY + "Meaning the Milestone would occur every " + ms.getVotes() + " votes. (" + ms.getVotes() + ", " + (ms.getVotes() + ms.getVotes()) + ", " + (ms.getVotes() + ms.getVotes()*2) + ", etc)");
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "yes" + ChatColor.YELLOW + " or " + ChatColor.GOLD + "no" + ChatColor.YELLOW + ", or type " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back.");
			break;
		case SET_VOTES:
			player.sendMessage(ChatColor.YELLOW + "* Enter the number of votes required to reach " + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + ":");
			player.sendMessage(ChatColor.GRAY + "(or type back to go back a step)");
			break;
		case CHOOSE_XP_OPTION:
			break;
		case ADD_COMMAND:
			player.sendMessage(ChatColor.YELLOW + "* Type out the command to add or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.");
			player.sendMessage(ChatColor.GRAY + "- To insert the voting players name, use the %player% varible.");
			player.sendMessage(ChatColor.GRAY + "- To add a delay, include the delay amount in seconds in parentheses at the beginning of the command.");
			player.sendMessage(ChatColor.GRAY + "- Example: (60) gamemode creative %player%");
			break;
		case REMOVE_COMMAND:
			player.sendMessage(ChatColor.YELLOW + "* Type the " + ChatColor.GOLD + "#" + ChatColor.YELLOW + " of the command you wish to remove or " + ChatColor.GOLD + "back" + ChatColor.YELLOW + " to go back a step.");
			player.sendMessage(ChatColor.GRAY + "Current commands:");
			int count = 1;
			for(String command : award.getCommands()) {
				if(command.contains("(")) {
					int index = command.indexOf(") ");
					if(command.charAt(index+2) != '/') {
						command = command.substring(0, index+2) + "/" + command.substring(index+2, command.length());
					}
				} else {
					if(!command.contains("/")) {
						command = "/" + command;
					}
				}
				player.sendMessage(ChatColor.GOLD + "" + count + ChatColor.GRAY + ") " + command);
				count++;
			}
			break;
		default:
			break;
		}
		//player.sendMessage(ChatColor.GRAY + "(Type " + ChatColor.GOLD + "cancel" + ChatColor.GRAY + " to exit the Award Creator)");
	}

	public Stack<AwardCreationStage> getPreviousStages() {
		return this.previousStages;
	}

	public AwardCreationStage getCurrentStage() {
		return this.currentStage;
	}

	public Award getAward() {
		return this.award;
	}

	public void setAward(Award award) {
		this.award = award;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public AwardType getAwardType() {
		return this.awardType;
	}
	public void setAwardType(AwardType type) {
		this.awardType = type;
	}

}
