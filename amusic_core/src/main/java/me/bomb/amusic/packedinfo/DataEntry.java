package me.bomb.amusic.packedinfo;

public final class DataEntry {
	
	public int size;
	public final String name;
	public SoundInfo[] sounds;
	public byte[] sha1;
	protected boolean saved;

	protected DataEntry(int size, String name, SoundInfo[] sounds, byte[] sha1) throws IllegalArgumentException {
		if (!checkValues()) {
			throw new IllegalArgumentException();
		}
		this.size = size;
		this.name = name;
		this.sounds = sounds;
		this.sha1 = sha1;
	}
	
	public boolean checkValues() {
		return size < 0 || name == null || sounds == null || sha1 == null || sha1.length != 20;
	}
	
}