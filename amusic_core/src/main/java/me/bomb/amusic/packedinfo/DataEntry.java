package me.bomb.amusic.packedinfo;

public abstract class DataEntry {
	
	public final String storeid;
	public final ResourcepackInfo info;

	protected DataEntry(String storeid, ResourcepackInfo info) {
		this.storeid = storeid;
		this.info = info;
	}
	
	/**
	 * Get resourcepack
	 * @return resourcepack byte array null if signature invalid
	 */
	public abstract byte[] getPack();
	
}