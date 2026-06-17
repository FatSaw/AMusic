package me.bomb.amusic.packedinfo;

public class RamDataEntry extends DataEntry {

	private final byte[] pack;
	
	protected RamDataEntry(String storeid, int size, String name, SoundInfo[] sounds, byte[] sha1, byte[] sha256, byte[] pack) {
		super(storeid, size, name, sounds, sha1, sha256);
		this.pack = pack;
	}

	@Override
	public byte[] getPack() {
		return this.pack;
	}

}
