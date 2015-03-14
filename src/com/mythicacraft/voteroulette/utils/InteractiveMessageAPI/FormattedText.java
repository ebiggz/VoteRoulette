package com.mythicacraft.voteroulette.utils.InteractiveMessageAPI;

import org.bukkit.ChatColor;


public class FormattedText {

	private String text;
	private ChatColor color;
	private boolean isBold = false;
	private boolean isItalic = false;
	private boolean isStrikethrough = false;
	private boolean isUnderlined = false;
	private boolean isObfuscated = false;

	public FormattedText(String text) {
		this(text, null);
	}

	public FormattedText(String text, ChatColor color) {
		if(text != null) this.text = ChatColor.translateAlternateColorCodes('&', text);
		this.color = color;
	}

	public String getString() {
		return text;
	}

	public FormattedText setString(String text) {
		if(text != null) this.text = ChatColor.translateAlternateColorCodes('&', text);
		return this;
	}

	public boolean hasColor() {
		return (color != null);
	}

	public ChatColor getColor() {
		return color;
	}

	public FormattedText setColor(ChatColor color) {
		this.color = color;
		return this;
	}

	public boolean hasFormat() {
		return (isBold || isItalic || isStrikethrough || isUnderlined || isObfuscated);
	}

	public boolean isBold() {
		return isBold;
	}

	public FormattedText setBold(boolean isBold) {
		this.isBold = isBold;
		return this;
	}

	public boolean isItalic() {
		return isItalic;
	}

	public FormattedText setItalic(boolean isItalic) {
		this.isItalic = isItalic;
		return this;
	}

	public boolean isStrikethrough() {
		return isStrikethrough;
	}

	public FormattedText setStrikethrough(boolean isStrikethrough) {
		this.isStrikethrough = isStrikethrough;
		return this;
	}

	public boolean isUnderlined() {
		return isUnderlined;
	}

	public FormattedText setUnderlined(boolean isUnderlined) {
		this.isUnderlined = isUnderlined;
		return this;
	}

	public boolean isObfuscated() {
		return isObfuscated();
	}

	public FormattedText setObfuscated(boolean isObfuscated) {
		this.isObfuscated = isObfuscated;
		return this;
	}


	public String getJSONString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\"text\": \"" + text.replace("\"", "\\\"") + "\"");
		if(hasColor()) {
			sb.append(",");
			sb.append("\"color\": \"" + color.name().toLowerCase() + "\"");
		}
		if(hasFormat()) {
			if(isBold) {
				sb.append(",");
				sb.append("\"bold\": \"true\"");
			}
			if(isItalic) {
				sb.append(",");
				sb.append("\"italic\": \"true\"");
			}
			if(isStrikethrough) {
				sb.append(",");
				sb.append("\"strikethrough\": \"true\"");
			}
			if(isUnderlined) {
				sb.append(",");
				sb.append("\"underlined\": \"true\"");
			}
			if(isObfuscated) {
				sb.append(",");
				sb.append("\"obfuscated\": \"true\"");
			}
		}
		return sb.toString();
	}
}
