package me.bomb.amusic.packedinfo;

public class RamDataEntry extends DataEntry {

	private final byte[] pack;
	
	protected RamDataEntry(String storeid, ResourcepackInfo info, byte[] pack) {
		super(storeid, info);
		this.pack = pack;
	}

	@Override
	public byte[] getPack() {
		return this.pack;
	}

}
