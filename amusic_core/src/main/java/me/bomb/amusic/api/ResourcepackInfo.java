package me.bomb.amusic.api;

import java.util.UUID;

import me.bomb.amusic.resourcepack.SoundInfo;

public interface ResourcepackInfo {
	
	public int getPacksize();

	public String getPackname();
	
	public SoundInfo[] getSounds();
	
	public byte[] getSha1();
	
	public byte[] getSha256();
	
	public UUID getBhea();
	
	public UUID getBres();
	
	public byte[] getCustomdata();
}
