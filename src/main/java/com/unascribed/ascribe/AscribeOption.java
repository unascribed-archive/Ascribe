package com.unascribed.ascribe;

import java.util.Locale;

public enum AscribeOption {
	SYNC_ATTACKED_AT_YAW("Since Minecraft 1.3, the attackedAtYaw field is not synced. Due to this,"
			+ " when taking damage, your camera will always shake to the right. If the field is"
			+ " synced properly, the camera will shake in the opposite direction of the source of"
			+ " the hit."),
	GAMERULE_DO_DOWNFALL("Adds a gamerule, doDownfall, which is on by default. If disabled, it can't"
			+ " rain. Why this isn't in vanilla is beyond me.");
	private final String category;
	private final String key;
	private final String comment;
	private AscribeOption(String comment) {
		String[] split = name().split("_");
		StringBuilder sb = new StringBuilder(split[1].toLowerCase(Locale.ENGLISH));
		for (int i = 2; i < split.length; i++) {
			String str = split[i].toLowerCase(Locale.ENGLISH);
			str = Character.toUpperCase(str.charAt(0)) + str.substring(1);
			sb.append(str);
		}
		this.category = split[0].toLowerCase();
		this.key = sb.toString();
		this.comment = comment;
	}
	
	public String getCategory() {
		return category;
	}
	public String getKey() {
		return key;
	}
	public String getComment() {
		return comment;
	}
}
