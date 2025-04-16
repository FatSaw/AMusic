package me.bomb.amusic.packedinfo;

public abstract class DataEntry {
	
	public int size;
	public final String name;
	public SoundInfo[] sounds;
	public byte[] sha1;

	protected DataEntry(int size, String name, SoundInfo[] sounds, byte[] sha1) {
		this.size = size;
		this.name = name;
		this.sounds = sounds;
		this.sha1 = sha1;
	}
	
	/**
	 * Get resourcepack
	 * @return resourcepack byte array null if signature invalid
	 */
	public abstract byte[] getPack();
	
}