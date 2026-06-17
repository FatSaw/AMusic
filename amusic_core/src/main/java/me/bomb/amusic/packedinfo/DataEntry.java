package me.bomb.amusic.packedinfo;

public abstract class DataEntry {
	
	public final String storeid;
	public final int size;
	public final String name;
	public final SoundInfo[] sounds;
	public final byte[] sha1;
	public final byte[] sha256;

	protected DataEntry(String storeid, int size, String name, SoundInfo[] sounds, byte[] sha1, byte[] sha256) {
		this.storeid = storeid;
		this.size = size;
		this.name = name;
		this.sounds = sounds;
		this.sha1 = sha1;
		this.sha256 = sha256;
	}
	
	/**
	 * Get resourcepack
	 * @return resourcepack byte array null if signature invalid
	 */
	public abstract byte[] getPack();
	
}