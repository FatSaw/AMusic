package me.bomb.amusic;

public final class Playing {
	protected final short currenttrack, maxid;
	protected volatile short remaining;

	protected Playing(short currenttrack, short maxid, short remaining) {
		this.currenttrack = currenttrack;
		this.maxid = maxid;
		this.remaining = remaining;
	}
}
