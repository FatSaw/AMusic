package me.bomb.amusic;

import java.util.UUID;

public final class Playing {
	protected final UUID soundhash;
	protected final short currenttrack, maxid;
	protected volatile short remaining;

	protected Playing(UUID soundhash, short currenttrack, short maxid, short remaining) {
		this.soundhash = soundhash;
		this.currenttrack = currenttrack;
		this.maxid = maxid;
		this.remaining = remaining;
	}
}
