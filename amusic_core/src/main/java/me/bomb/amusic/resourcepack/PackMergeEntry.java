package me.bomb.amusic.resourcepack;

public interface PackMergeEntry {
	
	public byte[] getMergePack();
	
	public int entryCount();
	
	public int cdSize();
	
	public int cdOffset();
	
	public int commentLength();
	
}
