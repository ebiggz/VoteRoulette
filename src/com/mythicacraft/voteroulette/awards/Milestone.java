package com.mythicacraft.voteroulette.awards;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;

public class Milestone extends Award {

	private static final Logger log = Logger.getLogger("VoteRoulette");
	private int votes = 0;
	private boolean recurring = false;
	private int priority = 10;

	public Milestone(String name, ConfigurationSection cs) {

		super(name, cs, AwardType.MILESTONE);

		String votes = cs.getString("votes");
		try {
			this.votes = Integer.parseInt(votes);
		} catch (Exception e) {
			log.warning("[VoteRoulette] Milestone \"" + name
			        + "\" votes format invalid! Ignoring Milestone...");
			try {
				this.finalize();
			} catch (Throwable t) {
			}
			return;
		}
		if (cs.contains("recurring")) {
			try {
				boolean tmp = Boolean.parseBoolean(cs.getString("recurring"));
				this.recurring = tmp;
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid recurring format for milestone: "
				        + name + ", Recurring defaulting to false...");
			}
		}
		if (cs.contains("priority")) {
			try {
				String prior = cs.getString("priority");
				this.priority = Integer.parseInt(prior);
				if (this.priority < 1) {
					log.warning("[VoteRoulette] Invalid priority format for milestone: "
					        + name
					        + ", Priority can't be less than 1! Setting priorty to default of 10...");
					this.priority = 10;
				}
			} catch (Exception e) {
				log.warning("[VoteRoulette] Invalid priority format for milestone: "
				        + name + ", Setting priorty to default of 10...");
			}
		}
	}

	public Milestone(String name) {
		super(name, AwardType.MILESTONE);
	}

	public int getVotes() {
		return votes;
	}

	public void setVotes(int votes) {
		this.votes = votes;
	}

	public boolean hasOptions() {
		if (this.hasAwardOptions() || this.getPriority() != 10)
			return true;
		return false;
	}

	public boolean isRecurring() {
		return recurring;
	}

	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
