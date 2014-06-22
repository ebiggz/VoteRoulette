package com.mythicacraft.voteroulette.awardcreator;

import java.util.Arrays;
import java.util.Stack;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mythicacraft.voteroulette.awards.Award;
import com.mythicacraft.voteroulette.awards.Award.AwardType;
import com.mythicacraft.voteroulette.awards.Milestone;
import com.mythicacraft.voteroulette.awards.Reward;
import com.mythicacraft.voteroulette.utils.Utils;

public class AwardCreator {

	private Player player;
	private AwardType awardType = null;
	private Award award;
	private Award origAward;
	private int origAwardIndex;
	private AwardCreationStage currentStage;
	private Stack<AwardCreationStage> previousStages = new Stack<AwardCreationStage>();
	private boolean paused = false;

	public AwardCreator(Player player) {
		this.player = player;
	}

	public enum AwardCreationStage {
		CHOOSE_AWARD, NAME, SET_VOTES, SET_RECURRING, CHOOSE_PRIZE, ADD_ITEMS, ADD_XP, CHOOSE_XP_OPTION, ADD_CURRENCY, EDIT_COMMANDS, ADD_COMMAND, REMOVE_COMMAND, CHOOSE_OPTION, ADD_CHANCE, SET_PRIORITY, ADD_VOTESTREAK, ADD_REROLL, ADD_DESCRIPTION, ADD_MESSAGE, ADD_PERMGROUPS, ADD_PLAYERS, ADD_WEBSITES, ADD_WORLDS, FINAL_CONFIRMATION
	}

	public void goToStage(AwardCreationStage stage) {
		this.goToStage(stage, false);
	}

	public boolean hasOrigAward() {
		if (origAward != null) {
			return true;
		}
		return false;
	}

	public void setOrigAward(Award award, int index) {
		this.origAward = award;
		this.origAwardIndex = index;
	}

	public Award getOrigAward() {
		return origAward;
	}

	public int getOrigIndex() {
		return origAwardIndex;
	}

	public void goToStage(AwardCreationStage stage, boolean isGoingBack) {
		if (currentStage != null) {
			if (!isGoingBack) {
				previousStages.push(currentStage);
			}
		}
		currentStage = stage;
		switch (stage) {
		case CHOOSE_AWARD:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " What type of award do you want to create?");
			player.sendMessage(ChatColor.GOLD + "Reward" + ChatColor.GRAY
			        + " - Considered for players every time they vote.");
			player.sendMessage(ChatColor.GOLD
			        + "Milestone"
			        + ChatColor.GRAY
			        + " - Given to players after they reach a set amount of votes.");
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD
			        + "reward" + ChatColor.YELLOW + " or " + ChatColor.GOLD
			        + "milestone" + ChatColor.YELLOW + ", or " + ChatColor.GOLD
			        + "help" + ChatColor.YELLOW + " for help, "
			        + ChatColor.GOLD + "cancel" + ChatColor.YELLOW
			        + " to quit.");
			// player.sendMessage(ChatColor.GRAY + "(or " + ChatColor.GOLD +
			// "help" + ChatColor.GRAY + " for help, " + ChatColor.GOLD +
			// "cancel" + ChatColor.GRAY + " to quit)");
			break;
		case ADD_CHANCE:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Please enter the chance you want this "
			        + awardType.toString().toLowerCase() + " to have, or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			player.sendMessage(ChatColor.GRAY
			        + "- Chance can be formatted as #% or #/#. Example: 50% or 1/100");
			if (award.hasChance()) {
				player.sendMessage(ChatColor.GRAY + "Current chance: "
				        + award.getChanceMin() + "/" + award.getChanceMax());
			}
			break;
		case EDIT_COMMANDS:
			if (!award.hasCommands()) {
				player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
				        + " Type " + ChatColor.GOLD + "add" + ChatColor.YELLOW
				        + " to add a new command or " + ChatColor.GOLD + "back"
				        + ChatColor.YELLOW + " to go back.");
			} else {
				player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
				        + " Type " + ChatColor.GOLD + "add" + ChatColor.YELLOW
				        + " to add a new command, " + ChatColor.GOLD + "remove"
				        + ChatColor.YELLOW + " to remove a command, or "
				        + ChatColor.GOLD + "next" + ChatColor.YELLOW
				        + " to continue.");
				player.sendMessage(ChatColor.GRAY + "Current commands:");
				int count = 1;
				for (String command : award.getCommands()) {
					if (command.contains("(")) {
						int index = command.indexOf(") ");
						if (command.charAt(index + 2) != '/') {
							command = command.substring(0, index + 2)
							        + "/"
							        + command.substring(index + 2,
							                command.length());
						}
					} else {
						if (!command.contains("/")) {
							command = "/" + command;
						}
					}
					player.sendMessage(ChatColor.GRAY + "" + count
					        + ChatColor.GRAY + ") " + command);
					count++;
				}
			}
			break;
		case ADD_CURRENCY:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Please type the amount of currency you want this "
			        + awardType.toString().toLowerCase() + " to give, or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			if (award.hasCurrency()) {
				player.sendMessage(ChatColor.GRAY + "Current currency: "
				        + award.getCurrency());
			}
			break;
		case ADD_DESCRIPTION:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Please type a description for this "
			        + awardType.toString().toLowerCase()
			        + " that players can see when viewing awards, or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			if (award.hasDescription()) {
				player.sendMessage(ChatColor.GRAY + "Current description: "
				        + award.getDescription());
			}
			break;
		case ADD_ITEMS:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " To add items to this "
			        + awardType.toString().toLowerCase()
			        + ", the items must be in your inventory. Make sure your inventory reflects what you want the award to give.");
			player.sendMessage(ChatColor.GRAY
			        + " - You can use the commands /vr setname and /vr setlore to customize items with colored names and lore.");
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD
			        + "add" + ChatColor.YELLOW + " when you are ready, "
			        + ChatColor.GOLD + "clear" + ChatColor.YELLOW
			        + " to clear previous items, or " + ChatColor.GOLD + "back"
			        + ChatColor.YELLOW + " to go back.");
			break;
		case ADD_MESSAGE:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " Please type the message that will be shown when a player wins this "
			        + awardType.toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			if (award.hasMessage()) {
				player.sendMessage(ChatColor.GRAY + "Current message: "
				        + award.getMessage());
			}
			break;
		case ADD_PERMGROUPS:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " Please type the permission group names (seperated by commas) that are eligible for this "
			        + awardType.toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			if (award.hasPermissionGroups()) {
				player.sendMessage(ChatColor.GRAY
				        + "Current groups: "
				        + Utils.concatListToString(Arrays.asList(award
				                .getPermGroups())));
			}
			break;
		case ADD_PLAYERS:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " Please type the player names (seperated by commas) that are eligible for this "
			        + awardType.toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			if (award.hasPlayers()) {
				player.sendMessage(ChatColor.GRAY
				        + "Current players: "
				        + Utils.concatListToString(Arrays.asList(award
				                .getPlayers())));
			}
			break;
		case ADD_REROLL:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " Please type a name of another Reward to be rerolled for when a player wins this "
			        + awardType.toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			player.sendMessage(ChatColor.GRAY
			        + "- Include \"(#%)\" at the end of the name to set a custom chance");
			player.sendMessage(ChatColor.GRAY
			        + "- Use \"ANY\" instead of a name to have a Reward chosen at random.");
			if (award.hasReroll()) {
				player.sendMessage(ChatColor.GRAY + "Current reroll: "
				        + award.getReroll());
			}
			break;
		case ADD_WEBSITES:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " Please type the website identifiers (seperated by commas) that this "
			        + awardType.toString().toLowerCase()
			        + " is eligible for, or " + ChatColor.GOLD + "back"
			        + ChatColor.YELLOW + " to go back:");
			Reward r = (Reward) award;
			player.sendMessage(ChatColor.GRAY + "Known websites: "
			        + Utils.getKnownWebsites());
			if (r.hasWebsites()) {
				player.sendMessage(ChatColor.GRAY + "Current websites: "
				        + Utils.concatListToString(r.getWebsites()));
			}
			break;
		case ADD_WORLDS:
			player.sendMessage(ChatColor.AQUA
			        + "*"
			        + ChatColor.YELLOW
			        + " Please type the world names (seperated by commas) that this "
			        + awardType.toString().toLowerCase()
			        + " can be claimed in, or " + ChatColor.GOLD + "back"
			        + ChatColor.YELLOW + " to go back:");
			if (award.hasWorlds()) {
				player.sendMessage(ChatColor.GRAY + "Current worlds: "
				        + Utils.concatListToString(award.getWorlds()));
			}
			break;
		case ADD_XP:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Please type the number of XP levels you want this "
			        + awardType.toString().toLowerCase() + " to give, or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			if (award.hasXpLevels()) {
				player.sendMessage(ChatColor.GRAY + "Current XP: "
				        + award.getXpLevels());
			}
			break;
		case CHOOSE_OPTION:

			if (Utils.awardHasOptions(award)) {
				player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
				        + " Do you want to add/change another "
				        + award.getAwardType().toString().toLowerCase()
				        + " option for " + ChatColor.AQUA + award.getName()
				        + ChatColor.YELLOW + "?");
			} else {
				player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
				        + " Do you want to add/change a "
				        + award.getAwardType().toString().toLowerCase()
				        + " option for " + ChatColor.AQUA + award.getName()
				        + ChatColor.YELLOW + "?");
			}
			if (award.getAwardType() == AwardType.REWARD) {
				player.sendMessage(ChatColor.GRAY + "Options: "
				        + ChatColor.GOLD + "permgroups" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "players" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "worlds" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "websites" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "votestreak" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "rename" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "chance" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "reroll" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "description" + ChatColor.GRAY
				        + ", or " + ChatColor.GOLD + "message" + ChatColor.GRAY
				        + ".");
			} else {
				player.sendMessage(ChatColor.GRAY + "Options: "
				        + ChatColor.GOLD + "permgroups" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "players" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "worlds" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "votecount" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "recurring" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "priority" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "rename" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "chance" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "reroll" + ChatColor.GRAY + ", "
				        + ChatColor.GOLD + "description" + ChatColor.GRAY
				        + ", or " + ChatColor.GOLD + "message" + ChatColor.GRAY
				        + ".");
			}
			player.sendMessage(ChatColor.YELLOW + "Type an option or type "
			        + ChatColor.GOLD + "next" + ChatColor.YELLOW
			        + " to continue, " + ChatColor.GOLD + "preview"
			        + ChatColor.YELLOW + " to see the "
			        + award.getAwardType().toString().toLowerCase() + ", "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back.");
			break;
		case CHOOSE_PRIZE:
			if (award != null) {
				if (!award.isEmpty()) {
					player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
					        + " Do you want to add another prize type for "
					        + ChatColor.AQUA + award.getName()
					        + ChatColor.YELLOW + "?");
					player.sendMessage(ChatColor.GRAY + "Prizes: "
					        + ChatColor.GOLD + "items" + ChatColor.GRAY + ", "
					        + ChatColor.GOLD + "xp" + ChatColor.GRAY + ", "
					        + ChatColor.GOLD + "currency" + ChatColor.GRAY
					        + ", or " + ChatColor.GOLD + "commands"
					        + ChatColor.GRAY + ".");
					player.sendMessage(ChatColor.YELLOW
					        + "Enter a prize type or type " + ChatColor.GOLD
					        + "next" + ChatColor.YELLOW + " to continue, "
					        + ChatColor.GOLD + "preview" + ChatColor.YELLOW
					        + " to see the "
					        + award.getAwardType().toString().toLowerCase()
					        + ", or " + ChatColor.GOLD + "back"
					        + ChatColor.YELLOW + " to go back.");
					return;
				}
			}
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Choose a prize type to add for " + ChatColor.AQUA
			        + award.getName() + ChatColor.YELLOW + ":");
			player.sendMessage(ChatColor.GRAY + "Prizes: " + ChatColor.GOLD
			        + "items" + ChatColor.GRAY + ", " + ChatColor.GOLD + "xp"
			        + ChatColor.GRAY + ", " + ChatColor.GOLD + "currency"
			        + ChatColor.GRAY + ", or " + ChatColor.GOLD + "commands"
			        + ChatColor.GRAY + ".");
			player.sendMessage(ChatColor.YELLOW + "Enter a prize type or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back.");
			break;
		case FINAL_CONFIRMATION:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Confirm the creation of "
			        + award.getAwardType().toString().toLowerCase() + " "
			        + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + ":");
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD
			        + "save" + ChatColor.YELLOW + " to finish, "
			        + ChatColor.GOLD + "preview" + ChatColor.YELLOW
			        + " to see the "
			        + award.getAwardType().toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back.");
			break;
		case NAME:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Please type a name for this new "
			        + awardType.toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			break;
		case SET_PRIORITY:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Please enter the priority for this "
			        + awardType.toString().toLowerCase() + ", or "
			        + ChatColor.GOLD + "back" + ChatColor.YELLOW
			        + " to go back:");
			player.sendMessage(ChatColor.GRAY
			        + "- Priority is used when multiple Milestones happen at the same time.");
			player.sendMessage(ChatColor.GRAY
			        + "- Can be any number, 1 being the highest. (Think first priority)");
			player.sendMessage(ChatColor.GRAY
			        + "- If no priority is set, 10 is used.");
			Milestone m = (Milestone) award;
			if (m.getPriority() != 10) {
				player.sendMessage(ChatColor.GRAY + "Current priority: "
				        + m.getPriority());
			}
			break;
		case SET_RECURRING:
			Milestone ms = (Milestone) award;
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Do you want " + ChatColor.AQUA + award.getName()
			        + ChatColor.YELLOW + " to be recurring?");
			player.sendMessage(ChatColor.GRAY
			        + " - Meaning the Milestone would occur every "
			        + ms.getVotes() + " votes. (" + ms.getVotes() + ", "
			        + (ms.getVotes() + ms.getVotes()) + ", "
			        + (ms.getVotes() + ms.getVotes() * 2) + ", etc)");
			if (ms.isRecurring()) {
				player.sendMessage(ChatColor.GRAY
				        + "Current recurring setting: true");
			}
			player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD
			        + "yes" + ChatColor.YELLOW + " or " + ChatColor.GOLD + "no"
			        + ChatColor.YELLOW + ", or type " + ChatColor.GOLD + "back"
			        + ChatColor.YELLOW + " to go back.");
			break;
		case SET_VOTES:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Enter the number of votes required to reach "
			        + ChatColor.AQUA + award.getName() + ChatColor.YELLOW + ":");
			Milestone milestone = (Milestone) award;
			if (milestone.getVotes() != 0) {
				player.sendMessage(ChatColor.GRAY + "Current votes: "
				        + milestone.getVotes());
			}
			break;
		case CHOOSE_XP_OPTION:
			break;
		case ADD_COMMAND:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Type out the command to add or " + ChatColor.GOLD
			        + "back" + ChatColor.YELLOW + " to go back.");
			player.sendMessage(ChatColor.GRAY
			        + "- To insert the voting players name, use the %player% varible.");
			player.sendMessage(ChatColor.GRAY
			        + "- To add a delay, include the delay amount in seconds in parentheses at the beginning of the command.");
			player.sendMessage(ChatColor.GRAY
			        + "- Example: (60) gamemode creative %player%");
			break;
		case REMOVE_COMMAND:
			player.sendMessage(ChatColor.AQUA + "*" + ChatColor.YELLOW
			        + " Type the " + ChatColor.GOLD + "#" + ChatColor.YELLOW
			        + " of the command you wish to remove or " + ChatColor.GOLD
			        + "back" + ChatColor.YELLOW + " to go back.");
			player.sendMessage(ChatColor.GRAY + "Current commands:");
			int count = 1;
			for (String command : award.getCommands()) {
				if (command.contains("(")) {
					int index = command.indexOf(") ");
					if (command.charAt(index + 2) != '/') {
						command = command.substring(0, index + 2)
						        + "/"
						        + command
						                .substring(index + 2, command.length());
					}
				} else {
					if (!command.contains("/")) {
						command = "/" + command;
					}
				}
				player.sendMessage(ChatColor.GOLD + "" + count + ChatColor.GRAY
				        + ") " + command);
				count++;
			}
			break;
		default:
			break;
		}
		// player.sendMessage(ChatColor.GRAY + "(Type " + ChatColor.GOLD +
		// "cancel" + ChatColor.GRAY + " to exit the Award Creator)");
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
