package com.mythicacraft.voteroulette.awards;

public class RerollEntry {
	private String rewardName;
	private int min;
	private int max;

	public RerollEntry(String rewardName, int min, int max) {
		this.rewardName = rewardName;
		this.min = min;
		this.max = max;
	}

	public String getRewardName() {
		return rewardName;
	}

	public boolean hasCustomChance() {
		return max > 0 && min > 0;
	}

	public int getChanceMin() {
		return min;
	}

	public int getChanceMax() {
		return max;
	}
}