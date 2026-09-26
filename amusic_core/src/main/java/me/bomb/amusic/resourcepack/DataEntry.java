package me.bomb.amusic.resourcepack;

public abstract class DataEntry {
	
	public final String storeid;
	public final ResourcepackInfoImpl info;

	protected DataEntry(String storeid, ResourcepackInfoImpl info) {
		this.storeid = storeid;
		this.info = info;
	}
	
	/**
	 * Get resourcepack
	 * @return resourcepack byte array null if signature invalid
	 */
	public abstract byte[] getPack();
	
}