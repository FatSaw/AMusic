package me.bomb.amusic;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class AMusicPlaceholderExpansion extends PlaceholderExpansion {

	@Override
	public String getAuthor() {
		return "Bomb";
	}

	@Override
	public String getIdentifier() {
		return "amusic";
	}

	@Override
	public String getVersion() {
		return "0.8";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		if (params.equalsIgnoreCase("playingsoundsize.hour")) {
			byte hour = (byte) (PositionTracker.getPlayingSizeF(player.getUniqueId()) >> 12);
			hour &= 0x07;
			return Byte.toString(hour);
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.hour")) {
			byte hour = (byte) (PositionTracker.getPlayingRemainF(player.getUniqueId()) >> 12);
			hour &= 0x07;
			return Byte.toString(hour);
		}
		String placeholder = null;
		if (params.equalsIgnoreCase("playingsoundname")) {
			placeholder = PositionTracker.getPlaying(player.getUniqueId());
			return placeholder == null ? "" : placeholder;
		}

		if (params.equalsIgnoreCase("playingsoundsize.total")) {
			placeholder = Short.toString(PositionTracker.getPlayingSize(player.getUniqueId()));
			return placeholder == null ? "" : placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.total")) {
			placeholder = Short.toString(PositionTracker.getPlayingRemain(player.getUniqueId()));
			return placeholder == null ? "" : placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundsize.sec")) {
			byte sec = (byte) PositionTracker.getPlayingSizeF(player.getUniqueId());
			sec &= 0x3F;
			placeholder = Byte.toString(sec);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.sec")) {
			short formated = PositionTracker.getPlayingRemainF(player.getUniqueId());
			if (formated == -1) {

			}
			byte sec = (byte) formated;
			sec &= 0x3F;
			placeholder = Byte.toString(sec);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundsize.min")) {
			byte min = (byte) (PositionTracker.getPlayingSizeF(player.getUniqueId()) >> 6);
			min &= 0x3F;
			placeholder = Byte.toString(min);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.min")) {
			byte min = (byte) (PositionTracker.getPlayingRemainF(player.getUniqueId()) >> 6);
			min &= 0x3F;
			placeholder = Byte.toString(min);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		return null;
	}

}
