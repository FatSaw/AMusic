package me.bomb.amusic.resourcepack;

public class RamDataEntry extends DataEntry {

	private final byte[] pack;
	
	protected RamDataEntry(String storeid, ResourcepackInfoImpl info, byte[] pack) {
		super(storeid, info);
		this.pack = pack;
	}

	@Override
	public byte[] getPack() {
		return this.pack;
	}

}
