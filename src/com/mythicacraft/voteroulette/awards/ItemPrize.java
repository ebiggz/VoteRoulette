package com.mythicacraft.voteroulette.awards;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.Voter;
import com.mythicacraft.voteroulette.stats.VoterStatSheet;
import com.mythicacraft.voteroulette.utils.Expression;


public class ItemPrize extends ItemStack {

	private String amountExpression;

	public ItemPrize(Material material, int amount, short dataID) {
		super(material, amount, dataID);
	}

	public ItemPrize(ItemStack is) {
		super(is);
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

	public void setAmount(String amountEx) {
		if(amountEx.length() == 1){
			try {
				this.setAmount(Integer.parseInt(amountEx));
				return;
			} catch (Exception e) {}
		}
		this.setAmountExpression(amountEx);
	}

	public String getStringAmount() {
		if(hasVariableAmount()) {
			return getAmountExpression();
		} else {
			return Integer.toString(this.getAmount());
		}
	}

	public boolean hasVariableAmount() {
		return (amountExpression != null && !amountExpression.isEmpty());
	}

	public String getFormattedVariableString(Voter voter) {
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
			a = a.replace(randTag, Integer.toString(random));
		}
		return a;
	}

	public int getCalculatedAmount(Voter voter) {
		Expression ex = new Expression(getFormattedVariableString(voter));
		return ex.setRoundingMode(RoundingMode.DOWN).eval().intValue();
	}

	public ItemStack[] getCalculatedItem(Voter voter) {
		ItemStack item = this.clone();
		if(!hasVariableAmount()) return new ItemStack[]{item};
		int sum = this.getCalculatedAmount(voter);
		if(sum < 1) {
			item.setAmount(1);
		} else {
			if(VoteRoulette.HAS_ITEM_LIMIT) {
				if(sum > VoteRoulette.VARIABLE_ITEM_LIMIT) {
					item.setAmount(VoteRoulette.VARIABLE_ITEM_LIMIT);
					return new ItemStack[]{item};
				}
			}
			if(sum > 64) {
				List<ItemStack> items = new ArrayList<ItemStack>();
				while(sum > 64) {
					ItemStack newItem = item.clone();
					newItem.setAmount(64);
					items.add(newItem);
					sum -= 64;
				}
				if(sum > 0) {
					item.setAmount(sum);
					items.add(item);
				}
				ItemStack[] array = new ItemStack[items.size()];
				items.toArray(array);
				return array;
			} else {
				item.setAmount(sum);
			}
		}
		return new ItemStack[]{item};
	}
}
