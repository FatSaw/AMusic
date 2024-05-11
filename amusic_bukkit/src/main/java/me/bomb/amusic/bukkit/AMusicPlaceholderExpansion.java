package me.bomb.amusic.bukkit;

import org.bukkit.OfflinePlayer;

import me.bomb.amusic.PositionTracker;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class AMusicPlaceholderExpansion extends PlaceholderExpansion {
	private final PositionTracker positiontracker;
	protected AMusicPlaceholderExpansion(PositionTracker positiontracker) {
		this.positiontracker = positiontracker;
	}

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
		return "0.14";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		if (params.equalsIgnoreCase("playingsoundsize.hour")) {
			byte hour = (byte) (positiontracker.getPlayingSizeF(player.getUniqueId()) >> 12);
			hour &= 0x07;
			return Byte.toString(hour);
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.hour")) {
			byte hour = (byte) (positiontracker.getPlayingRemainF(player.getUniqueId()) >> 12);
			hour &= 0x07;
			return Byte.toString(hour);
		}
		String placeholder = null;
		if (params.equalsIgnoreCase("playingsoundname")) {
			placeholder = positiontracker.getPlaying(player.getUniqueId());
			return placeholder == null ? "" : placeholder;
		}

		if (params.equalsIgnoreCase("playingsoundsize.total")) {
			placeholder = Short.toString(positiontracker.getPlayingSize(player.getUniqueId()));
			return placeholder == null ? "" : placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.total")) {
			placeholder = Short.toString(positiontracker.getPlayingRemain(player.getUniqueId()));
			return placeholder == null ? "" : placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundsize.sec")) {
			byte sec = (byte) positiontracker.getPlayingSizeF(player.getUniqueId());
			sec &= 0x3F;
			placeholder = Byte.toString(sec);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.sec")) {
			byte sec = (byte) positiontracker.getPlayingRemainF(player.getUniqueId());
			sec &= 0x3F;
			placeholder = Byte.toString(sec);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundsize.min")) {
			byte min = (byte) (positiontracker.getPlayingSizeF(player.getUniqueId()) >> 6);
			min &= 0x3F;
			placeholder = Byte.toString(min);
			if (placeholder.length() == 1) {
				placeholder = "0".concat(placeholder);
			}
			return placeholder;
		}
		if (params.equalsIgnoreCase("playingsoundremainsize.min")) {
			byte min = (byte) (positiontracker.getPlayingRemainF(player.getUniqueId()) >> 6);
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
