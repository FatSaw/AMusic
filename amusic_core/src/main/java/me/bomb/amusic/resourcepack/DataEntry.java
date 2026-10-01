package me.bomb.amusic.resourcepack;

import java.io.IOException;
import java.nio.channels.SocketChannel;

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
	
	/**
	 * Send resourcepack to channel
	 */
	public abstract void sendTo(SocketChannel channel) throws IOException;
	
	/**
	 * Get length of resourcepack
	 */
	public abstract int getLength();
	
}