package me.bomb.amusic.packedinfo;

import java.util.UUID;

public class RamDataEntry extends DataEntry {

	private final byte[] pack;
	
	protected RamDataEntry(String storeid, int size, String name, SoundInfo[] sounds, byte[] sha1, byte[] sha256, UUID bhea, UUID bres, byte[] pack) {
		super(storeid, size, name, sounds, sha1, sha256, bhea, bres);
		this.pack = pack;
	}

	@Override
	public byte[] getPack() {
		return this.pack;
	}

}
