package me.bomb.amusic.packedinfo;

import java.util.UUID;

public final class SoundInfo {
	public final String name;
	public final UUID hash;
	public final short length;
	public final byte split;
	
	public SoundInfo(String name, UUID hash, short length, byte split) {
		this.name = name;
		this.hash = hash;
		this.length = length;
		this.split = split;
	}
}
