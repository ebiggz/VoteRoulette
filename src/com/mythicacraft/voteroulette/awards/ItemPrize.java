package com.mythicacraft.voteroulette.awards;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.stats.VoterStatSheet;
import com.mythicacraft.voteroulette.utils.Expression;


public class ItemPrize extends ItemStack {

	private String amountExpression;

	public ItemPrize(Material material, int amount, short dataID) {
		super(material, amount, dataID);
	}

	public ItemPrize(Material material, int amount) {
		super(material, amount);
	}

	public ItemPrize(Material material, String amountExpression, short dataID) {
		super(material, 1, dataID);
		this.amountExpression = amountExpression;
	}

	public ItemPrize(Material material, String amountExpression) {
		super(material, 1);
		this.amountExpression = amountExpression;
	}

	public String getAmountExpression() {
		return amountExpression;
	}

	public void setAmountExpression(String amountExpression) {
		this.amountExpression = amountExpression;
	}

	public boolean hasVariableAmount() {
		return (amountExpression != null && !amountExpression.isEmpty());
	}

	public ItemStack getCalculatedItem(Voter voter) {
		ItemStack item = this.clone();
		if(!hasVariableAmount()) return item;
		VoterStatSheet statsheet = voter.getStatSheet();
		String a = getAmountExpression()
				.replace("%lifetime%", Integer.toString(statsheet.getLifetimeVotes()))
				.replace("%currentstreak%", Integer.toString(statsheet.getCurrentVoteStreak()))
				.replace("%longeststreak%", Integer.toString(statsheet.getLongestVoteStreak()))
				.replace("%currentmonth%", Integer.toString(statsheet.getCurrentMonthVotes()))
				.replace("%previousmonth%", Integer.toString(statsheet.getPreviousMonthVotes()))
				.replace("%cycle%", Integer.toString(statsheet.getCurrentCycle()));

		Pattern r = Pattern.compile("[R-r][A-a][N-n][D-d][O-o][M-m]\\((\\d+)\\)");
		Pattern n = Pattern.compile("\\d+");
		Matcher m = r.matcher(a);
		if (m.find( )) {
			String randTag = m.group(0);
			m = n.matcher(randTag);
			m.find();
			int random = 1 + (int)(Math.random() * ((Integer.parseInt(m.group(0)) - 1) + 1));
			a.replace(randTag, Integer.toString(random));
		}
		Expression ex = new Expression(a);
		item.setAmount(ex.eval().intValue());
		return item;
	}
}
