package com.mythicacraft.voteroulette.utils.InteractiveMessageAPI;


public class InteractiveMessageElement {

	private FormattedText text;
	private HoverEvent hoverEventType;
	private FormattedText hoverText;
	private ClickEvent clickEventType;
	private String command;


	/**
	 * Create an InteractiveMessageElement with a string of text
	 * <p>
	 *     Note: This will auto-create an element with FormattedText without hover or click events.
	 * </p>
	 * @param text the text to add
	 */
	public InteractiveMessageElement(String text) {
		this(text, HoverEvent.NONE, null, ClickEvent.NONE, null);
	}

	/**
	 * Create an InteractiveMessageElement with FormattedText
	 * <p>
	 *     Note: This will auto-create an element without hover or click events.
	 * </p>
	 * @param text the FormattedText to add
	 */
	public InteractiveMessageElement(FormattedText text) {
		this(text, HoverEvent.NONE, null, ClickEvent.NONE, null);
	}

	/**
	 * Create an InteractiveMessageElement with Hover and Click events
	 * <p>
	 *     Note: This will auto-create an element without hover or click events.
	 * </p>
	 * @param textString the text to show (auto creates a FormattedText object)
	 * @param hoverEventType enum the type of hover event
	 * @param hoverTextString if there is a hover event, the text to show. If none, can be null
	 * @param clickEventType enum the type of click event
	 * @param command the command for the click event. If none, can be null
	 */
	public InteractiveMessageElement(String textString, HoverEvent hoverEventType, String hoverTextString, ClickEvent clickEventType, String command) {
		this(new FormattedText(textString), hoverEventType, new FormattedText(hoverTextString), clickEventType, command);
	}

	/**
	 * Create an InteractiveMessageElement with with Hover and Click events
	 * <p>
	 *     Note: This will auto-create an element without hover or click events.
	 * </p>
	 * @param text the FormattedText
	 * @param hoverEventType enum the type of hover event
	 * @param hoverTextString if there is a hover event, the text to show. If none, can be null
	 * @param clickEventType enum the type of click event
	 * @param command the command for the click event. If none, can be null
	 */
	public InteractiveMessageElement(FormattedText text, HoverEvent hoverEventType, FormattedText hoverText, ClickEvent clickEventType, String command) {
		this.text = text;
		this.hoverEventType = hoverEventType;
		this.hoverText = hoverText;
		this.clickEventType = clickEventType;
		this.command = command;
	}


	public enum HoverEvent {
		SHOW_TEXT, NONE
	}

	public enum ClickEvent {
		SUGGEST_COMMAND, RUN_COMMAND, NONE
	}

	public FormattedText getMainText() {
		return text;
	}

	public HoverEvent getHoverEventType() {
		return hoverEventType;
	}

	public boolean hasHoverEvent() {
		return hoverEventType != HoverEvent.NONE;
	}

	public FormattedText getHoverText() {
		return hoverText;
	}

	public ClickEvent getClickEventType() {
		return clickEventType;
	}

	public boolean hasClickEvent() {
		return clickEventType != ClickEvent.NONE;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
}
