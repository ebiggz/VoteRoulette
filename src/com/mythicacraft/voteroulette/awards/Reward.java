package com.mythicacraft.voteroulette.awards;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;


public class Reward extends Award{

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private List<String> websites = new ArrayList<String>();
	private int voteStreak = 0;
	private VoteStreakModifier vsModifier = VoteStreakModifier.NONE;

	public Reward(String name, ConfigurationSection cs) {

		super(name, cs, AwardType.REWARD);

		if(cs.contains("websites")) {
			try {
				String websitesStr = cs.getString("websites");
				String[] websitesArray = websitesStr.split(",");
				for(String website : websitesArray) {
					websites.add(website.trim());
				}
			} catch (Exception e) {
				log.warning("[VoteRoulette] Error loading websites for reward:" + name + ", Skipping websites.");
			}
		}
		if(cs.contains("voteStreak")) {
			String voteStringInfo  = cs.getString("voteStreak").toLowerCase();
			Pattern p = Pattern.compile("\\d+");
			Matcher m = p.matcher(voteStringInfo);
			if(m.find()) {
				try {
					voteStreak = Integer.parseInt(m.group());
					if(voteStringInfo.contains("or more")) {
						vsModifier = VoteStreakModifier.OR_MORE;
					}
					else if(voteStringInfo.contains("or less")) {
						vsModifier = VoteStreakModifier.OR_LESS;
					}
				} catch (Exception e) {
					log.warning("[VoteRoulette] Error loading vote streak settings for reward:" + name + ", Skipping vote streak.");
				}
			}
		}
	}

	public Reward(String name) {
		super(name, AwardType.REWARD);
	}

	public enum VoteStreakModifier {
		OR_LESS, OR_MORE, NONE
	}

	public boolean hasWebsites() {
		if(websites == null || websites.isEmpty()) return false;
		return true;
	}

	public List<String> getWebsites() {
		return websites;
	}

	public void setWebsites(List<String> websites) {
		this.websites = websites;
	}

	public void setVoteStreak(int streak) {
		voteStreak = streak;
	}

	public int getVoteStreak() {
		return voteStreak;
	}

	public boolean hasOptions() {
		if(this.hasAwardOptions() || this.hasVoteStreak() || this.hasWebsites()) return true;
		return false;
	}

	public boolean hasVoteStreakModifier() {
		return vsModifier != VoteStreakModifier.NONE;
	}

	public void setVoteStreakModifier(VoteStreakModifier VSM) {
		vsModifier = VSM;
	}

	public VoteStreakModifier getVoteStreakModifier() {
		return vsModifier;
	}

	public boolean hasVoteStreak() {
		return voteStreak > 0;
	}
}
