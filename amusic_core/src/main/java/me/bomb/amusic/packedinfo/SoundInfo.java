package me.bomb.amusic.packedinfo;

public final class SoundInfo {
	public final String name;
	public final short length;
	public final byte split;
	
	public SoundInfo(String name, short length, byte split) {
		this.name = name;
		this.length = length;
		this.split = split;
	}
}
